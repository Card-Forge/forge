// Cost parts past those unlesscost.go first read, shared by every payer of an
// unlessCost (a triggered AB$'s Cost$, an optional cost, an UnlessCost$):
// SubCounter and RemoveAnyCounter on the source itself, Discard<N/Type>,
// ExiledMoveToGrave, Forage, ExileFromHand<N/All>, ExileCtrlOrGrave and
// ChooseCreatureType, each ported from its forge-game/.../cost/Cost*.java.

package engine

//enginelint:allow id zone card game player ability control valid parts discardeffect sacrificeeffect exilefromgrave exile zonemove removecountereffect staticability chosencosts targeting triggerpack trigger unlesscost amount taptype

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/cost"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// triggeredCostShape reads a triggered AB$'s Cost$ before anyone is asked to
// pay it. A cost naming X -- a mana X or a part of amount X -- whose SVar X is
// Count$xPaid (or absent) has its X announced as it is paid
// (PlaySpellAbility.announceValuesLikeX, :766-782, run for the wrapped ability
// by WrappedAbility.java:440): announce is true, and uc is the shape with X
// standing for 1, for its Mandatory flag only. Any other X is the card's own
// amount (Champion of Wits' Draw<X/You>, X its power), evaluated here the way
// CostPart.getAbilityAmount reads it (costWithX). A cost without X has its
// SVar-named amounts expanded (expandUnlessCost). A shape this port does not
// pay is an error (GO-7).
func (g *Game) triggeredCostShape(a *Ability, text string, controller PlayerController) (uc unlessCost, announce bool, err error) {
	if costHasX(text) {
		if amountIsXPaid(a.Amounts) {
			uc, ok := parseUnlessCostX(text, 1, true)
			if !ok {
				return unlessCost{}, false, fmt.Errorf("engine: triggered AB$ Cost$ %q not resolvable yet", text)
			}
			if _, has := a.Params.Param("XMin"); has {
				return unlessCost{}, false, fmt.Errorf("engine: triggered AB$ Cost$ %q: XMin$ not resolvable yet", text)
			}
			if raw, has := a.Params.Param("XMax"); has {
				if _, known := resolveNamedAmount(g, a.Amounts, g.Card(a.Source), raw); !known {
					return unlessCost{}, false, fmt.Errorf("engine: triggered AB$ Cost$ %q: XMax$ %q not resolvable yet", text, raw)
				}
			}
			return uc, true, nil
		}
		x, known := resolveNamedAmount(g, a.Amounts, g.Card(a.Source), "X")
		if !known {
			return unlessCost{}, false, fmt.Errorf("engine: triggered AB$ Cost$ %q: SVar X not resolvable yet", text)
		}
		uc, ok := costWithX(text, max(x, 0))
		if !ok {
			return unlessCost{}, false, fmt.Errorf("engine: triggered AB$ Cost$ %q not resolvable yet", text)
		}
		return uc, false, nil
	}
	expanded, err := g.expandUnlessCost(a, text, controller)
	if err != nil {
		return unlessCost{}, false, err
	}
	uc, ok := parseUnlessCost(expanded)
	if !ok {
		return unlessCost{}, false, fmt.Errorf("engine: triggered AB$ Cost$ %q not resolvable yet", text)
	}
	return uc, false, nil
}

// announceTriggeredX announces the X of a triggered AB$'s Cost$ and returns
// the cost with that X in place. The controller names it (ChoosePayX, asked
// with the cost's mana); AbilityUtils.getAnnouncementBounds caps it at XMax$
// (:3058-3060), and PlayerControllerHuman.announceRequirements at what the
// non-mana parts can pay (:376-381, nonManaXMax). The value becomes the
// ability's xManaCostPaid, which its Count$xPaid -- and every sub-ability's --
// reads from here on. shape is triggeredCostShape's, whose mana (X kept) is
// what ChoosePayX is shown. ok is false when the controller declines (a
// negative answer, Java's null).
func (g *Game) announceTriggeredX(controller PlayerController, a *Ability, text string, shape unlessCost) (uc unlessCost, ok bool) {
	n := controller.ChoosePayX(g, a.Controller, shape.mana)
	if n < 0 {
		return unlessCost{}, false
	}
	if raw, has := a.Params.Param("XMax"); has {
		// triggeredCostShape checked it resolves.
		limit, _ := resolveNamedAmount(g, a.Amounts, g.Card(a.Source), raw)
		n = min(n, max(limit, 0))
	}
	if costNamesX(text) {
		n = min(n, g.nonManaXMax(a.Controller, a.Source, text))
	}
	a.xManaCostPaid, a.hasXManaCostPaid = n, true
	g.xctx = xContext{value: n, has: true, source: a.Source}
	// The shape parsed with X standing for 1, so it parses with any X.
	uc, _ = costWithX(text, n)
	return uc, true
}

// costWithX is text with X standing for x: every part of amount X costs x (a
// part of X 0 pays nothing, CostPart.getAbilityAmount), and every X mana
// symbol is x generic mana.
func costWithX(text string, x int) (unlessCost, bool) {
	uc, ok := parseUnlessCostX(text, x, true)
	if !ok || !uc.hasMana || uc.mana.CountX() == 0 {
		return uc, ok
	}
	var shards []mana.Shard
	for _, s := range uc.mana.Shards() {
		if !s.IsX() {
			shards = append(shards, s)
		}
	}
	uc.mana = mana.FromShards(shards, uc.mana.Generic()+x*uc.mana.CountX())
	return uc, true
}

// costHasX reports whether a Cost$ text names X: a mana X symbol or a part of
// amount X (Cost.hasXInAnyCostPart, Cost.java:1101-1111).
func costHasX(text string) bool {
	for _, tok := range cost.Parse(text).Mana {
		if tok == "X" {
			return true
		}
	}
	return costNamesX(text)
}

// amountIsXPaid reports whether amounts' X is Count$xPaid or absent: the only
// X PlaySpellAbility.announceValuesLikeX announces (:768-771). Any other X (a
// count of targets, of lands) is computed, never asked.
func amountIsXPaid(amounts map[string]expr.Amount) bool {
	amt, ok := amounts["x"]
	return !ok || (amt.Kind == expr.Expression && amt.Head == "Count" && strings.EqualFold(amt.Body, "xPaid"))
}

// forageExileN is how many graveyard cards Forage exiles (CostForage.java:
// 19-30); the other way to pay it is one Food sacrificed.
const forageExileN = 3

// extraPartsPayable is CostPart.canPay for the parts this file owns.
func (g *Game) extraPartsPayable(pid PlayerID, source CardID, uc unlessCost) bool {
	if uc.subCounterN > 0 {
		// CostRemoveCounter.canPay's payCostFromSource branch (:163-165).
		c := g.Card(source)
		if c.IsPhasedOut() || c.Counters.Count(uc.subCounterType) < uc.subCounterN {
			return false
		}
	}
	if uc.discardTypeN > 0 && len(g.costCandidates(pid, source, false, Hand, uc.discardTypeSpec)) < uc.discardTypeN {
		return false
	}
	if uc.movedToGraveN > 0 && len(g.exiledToGraveCandidates(pid, source, uc.movedToGraveSpec)) < uc.movedToGraveN {
		return false
	}
	if uc.forage && len(g.Zone(Graveyard, pid).Cards()) < forageExileN && len(g.forageFoods(pid)) == 0 {
		return false
	}
	if uc.exileEitherN > 0 && len(g.exileEitherCandidates(pid, source, uc.exileEitherSpec)) < uc.exileEitherN {
		return false
	}
	return true
}

// exiledToGraveCandidates is CostExiledMoveToGrave.getMaxAmountX's list
// (:48-55): every exiled card of the game, in seat order, matching spec from
// the payer's point of view.
func (g *Game) exiledToGraveCandidates(pid PlayerID, source CardID, spec string) []CardID {
	parsed := valid.Parse(strings.ReplaceAll(spec, ";", ","))
	var out []CardID
	for _, p := range g.Players() {
		for _, id := range g.Zone(Exile, p).Cards() {
			if Matches(g, g.Card(id), parsed, pid, source) {
				out = append(out, id)
			}
		}
	}
	return out
}

// forageFoods is the Foods pid may sacrifice for Forage (CostForage.java:25):
// pid's Food permanents that can be sacrificed.
func (g *Game) forageFoods(pid PlayerID) []CardID {
	var out []CardID
	for _, id := range g.Zone(Battlefield, pid).Cards() {
		c := g.Card(id)
		if !c.IsPhasedOut() && c.Type().HasSubtype("Food") && !g.cantSacrifice(c, false, nil) {
			out = append(out, id)
		}
	}
	return out
}

// exileEitherCandidates is ExileCtrlOrGrave's list (Cost.java:487-492):
// permanents pid controls, then cards in pid's graveyard, matching spec.
func (g *Game) exileEitherCandidates(pid PlayerID, source CardID, spec string) []CardID {
	return append(g.costCandidates(pid, source, false, Battlefield, spec), g.costCandidates(pid, source, false, Graveyard, spec)...)
}

// payExtraParts pays the parts this file owns, after unlessPayable said they
// can be. Each exiled card is marked as exiled with the source
// (SpellAbilityEffect.handleExiledWith, called by CostExile.java:294 and
// CostForage.java:52), and recorded as the cost's "Exiled" paid list in rec
// when there is one.
func (g *Game) payExtraParts(controller PlayerController, a *Ability, pid PlayerID, uc unlessCost, rec *paidLists) {
	if uc.subCounterN > 0 {
		c := g.Card(a.Source)
		removeCounters(g, a.Source, CardEntity(a.Source), &c.Counters, uc.subCounterType, false, false, uc.subCounterN)
	}
	if uc.discardTypeN > 0 {
		options := g.costCandidates(pid, a.Source, false, Hand, uc.discardTypeSpec)
		chosen := controller.ChooseCardsToDiscard(g, pid, options, uc.discardTypeN)
		if !isSubset(chosen, options) || len(chosen) != uc.discardTypeN {
			chosen = options[:uc.discardTypeN]
		}
		discardCards(g, controller, chosen, pid)
		if rec != nil {
			rec.discarded = append(rec.discarded, chosen...)
		}
	}
	if uc.movedToGraveN > 0 {
		options := g.exiledToGraveCandidates(pid, a.Source, uc.movedToGraveSpec)
		for _, id := range g.chosenUnlessCards(controller, pid, a.Source, options, uc.movedToGraveN, uc.movedToGraveN) {
			// CostExiledMoveToGrave.doPayment (:88-92): to its owner's graveyard.
			g.moveByEffect(controller, id, Graveyard, 0, NoPlayer, false)
		}
	}
	if uc.exileHandAll {
		hand := handWithout(g.Zone(Hand, pid).Cards(), a.Source)
		exiled := g.exileCostCards(controller, a.Source, hand)
		if rec != nil {
			rec.exiled = append(rec.exiled, exiled...)
		}
	}
	if uc.exileEitherN > 0 {
		options := g.exileEitherCandidates(pid, a.Source, uc.exileEitherSpec)
		picks := g.chosenUnlessCards(controller, pid, a.Source, options, uc.exileEitherN, uc.exileEitherN)
		exiled := g.exileCostCards(controller, a.Source, picks)
		if rec != nil {
			rec.exiled = append(rec.exiled, exiled...)
		}
	}
	if uc.forage {
		g.payForage(controller, a, pid)
	}
}

// payChoiceParts pays the parts paid first (paymentOrder 5, CostPart.java:210,
// in script order): ChooseCreatureType names the host's chosen type, then a
// spell's own Reveal, RevealOrChoose or Behold has its payer pick the cards,
// recorded as the cast's "Revealed" list -- the cards and each frozen as it
// was paid (CostPartWithList.java:136-138), what Revealed$ and Count$Presence_
// read. Another payer's reveal changes nothing this port reads, so it asks
// nothing. false when the type cannot be named or too few cards of it remain
// to behold (HumanCostDecision.java:1015-1017): the payment is declined.
func (g *Game) payChoiceParts(controller PlayerController, a *Ability, pid PlayerID, uc unlessCost) bool {
	if uc.chooseCreatureType && !g.chooseCostCreatureType(controller, pid, a.Source) {
		return false
	}
	if uc.revealN == 0 || !a.spell {
		return true
	}
	candidates := g.revealCandidates(pid, a.Source, uc)
	if len(candidates) < uc.revealN {
		return false
	}
	revealed := g.chosenUnlessCards(controller, pid, a.Source, candidates, uc.revealN, uc.revealN)
	rec := &g.Card(a.Source).castOpt
	rec.revealed = append(append([]CardID(nil), rec.revealed...), revealed...)
	lki := append([]*Card(nil), rec.revealedLKI...)
	for _, id := range revealed {
		lki = append(lki, g.Card(id).frozenCopy())
	}
	rec.revealedLKI = lki
	return true
}

// exileCostCards exiles ids for a cost of source, from whatever zone each is
// in, and marks each as exiled with source (CostExile.doPayment, :290-296). It
// returns the cards it exiled.
func (g *Game) exileCostCards(controller PlayerController, source CardID, ids []CardID) []CardID {
	var out []CardID
	for _, id := range ids {
		// Every cost part picks from the graveyard, the hand or the
		// battlefield.
		switch g.Card(id).Zone {
		case Graveyard:
			exileFromGraveyard(g, controller, id)
		case Hand:
			exileFromHand(g, id)
		default:
			exileCards(g, controller, []CardID{id})
		}
		if g.Card(id).Zone == Exile {
			g.markExiledWith(id, source)
			out = append(out, id)
		}
	}
	return out
}

// forageOptions are the two ways to forage, as ChooseOption shows them.
var forageOptions = []string{"Sacrifice a Food", "Exile three cards from your graveyard"}

// payForage is CostForage's payment (HumanCostDecision.java:619-646,
// CostForage.java:43-65): the payer sacrifices a Food or exiles three cards
// from their graveyard -- asked through ChooseOption when both are possible --
// then Mode$ Forage triggers run for the payer (:67-70).
func (g *Game) payForage(controller PlayerController, a *Ability, pid PlayerID) {
	foods := g.forageFoods(pid)
	grave := g.Zone(Graveyard, pid).Cards()
	sacrifice := len(foods) > 0
	if sacrifice && len(grave) >= forageExileN {
		sacrifice = controller.ChooseOption(g, pid, a.Source, forageOptions) != 1
	}
	if sacrifice {
		food := g.chosenUnlessCards(controller, pid, a.Source, foods, 1, 1)
		sacrificeCardsFor(g, controller, a, food, false)
	} else {
		picks := g.chosenUnlessCards(controller, pid, a.Source, append([]CardID(nil), grave...), forageExileN, forageExileN)
		g.exileCostCards(controller, a.Source, picks)
	}
	g.pushTriggeredAbilities(controller, g.playerEventMatches("Forage", forageTriggerParams, pid))
}

// forageTriggerParams are the params a Mode$ Forage trigger may carry
// (TriggerForage.performTest reads ValidPlayer$ only, :17-23).
var forageTriggerParams = effectEventParams("validplayer")

// beholdSharesType is CostBehold.canPay's ChosenType branch (:22-30): the
// creature type is not chosen yet, so the cost can be paid when some card in
// the payer's hand or on their battlefield shares a creature type with at
// least n of those cards, itself included. A spell being cast is left out of
// the hand: Java has it on the stack by then.
func (g *Game) beholdSharesType(pid PlayerID, source CardID, n int) bool {
	var list []*Card
	for _, z := range []ZoneType{Hand, Battlefield} {
		for _, id := range g.Zone(z, pid).Cards() {
			if z == Hand && id == source {
				continue
			}
			list = append(list, g.Card(id))
		}
	}
	for _, c := range list {
		count := 0
		for _, other := range list {
			if g.sharesCreatureType(c, other) {
				count++
			}
		}
		if count >= n {
			return true
		}
	}
	return false
}

// chooseCostCreatureType is CostChooseCreatureType's payment
// (HumanCostDecision.java:53-59, CostChooseCreatureType.java:56-58): the payer
// names one of every creature type and the host records it as its chosen
// type. False when there is no type to name or the answer is out of range.
func (g *Game) chooseCostCreatureType(controller PlayerController, pid PlayerID, host CardID) bool {
	reg := g.db.Types()
	if reg == nil {
		return false
	}
	types := reg.Members(cardtype.CategoryCreature)
	if len(types) == 0 {
		return false
	}
	i := controller.ChooseOption(g, pid, host, types)
	if i < 0 || i >= len(types) {
		return false
	}
	g.Card(host).Memory.SetChosenType(types[i], false)
	return true
}
