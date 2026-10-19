// UnlessCost$ past plain mana: the cost parts a player may pay to prevent an
// effect (AbilityUtils.handleUnlessCost, Ward), and with them a triggered AB$'s
// Cost$, a spell's own additional cost and an optional cost. PayLife<N>,
// PayEnergy<N>, DamageYou<N>, Draw<N/You>, Discard<N/Card>, Sac<N/Type> and
// Return<N/Type> beside mana tokens are the shapes that need no mid-payment
// decision beyond which cards; costparts.go holds the parts added since.

//enginelint:allow id zone card game player ability control event manapay discardeffect sacrificeeffect valid parts returncost combatdamage turn trigger taptype chosencosts exilefromgrave amount zonemove removecountereffect defined exile

package engine

import (
	"errors"
	"fmt"
	"slices"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/cost"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// unlessCost is an UnlessCost$ value this port can pay.
type unlessCost struct {
	parsed cost.Cost
	// mana is the cost's mana tokens; hasMana is false for none.
	mana    mana.Cost
	hasMana bool
	// lifeN, discardN and sacN are the PayLife, Discard<N/Card> and Sac<N/Type>
	// counts, 0 for no such part. sacSpec is the Sac part's valid string, or
	// "CARDNAME" for the source itself.
	lifeN, discardN, sacN int
	sacSpec               string
	// discardHand is Discard<N/Hand>: the whole hand, always payable.
	discardHand bool
	// energyN is PayEnergy<N>; mandatory is the Mandatory token, which makes
	// the cost unskippable (CostPart's isMandatory): nobody is asked.
	energyN   int
	mandatory bool
	// damageN is DamageYou<N> (the payer is dealt N noncombat damage by the
	// source), drawN is Draw<N/You>, and returnN/returnSpec are Return<N/Type>
	// (a valid string, or "CARDNAME" for the source itself); 0 for no such part.
	damageN, drawN, returnN int
	returnSpec              string
	// revealN/revealSpec are Reveal<N/Type>: N cards of the type in the payer's
	// hand (CostReveal.canPay). A spell's own reveal records the cards it
	// showed (payChoiceParts); any other payer's changes no state this port
	// tracks, so paying it only needs them to exist.
	revealN    int
	revealSpec string
	// revealBattlefield is Behold<N/Type> (CostBehold) or RevealOrChoose<N/Type>
	// (CostReveal over Hand,Battlefield, Cost.java:522-526): the cards may also
	// be permanents the payer controls. revealBehold tells Behold apart: its
	// canPay reads a ChosenType spec as "N cards sharing a creature type"
	// (CostBehold.java:19-32), a plain reveal never does.
	revealBattlefield bool
	revealBehold      bool
	// chooseCreatureType is ChooseCreatureType<1> (CostChooseCreatureType):
	// always payable (:51-53), the payer names a creature type and the host
	// records it as its chosen type (:56-58).
	chooseCreatureType bool
	// subCounterN/subCounterType are SubCounter<N/Type> on the source itself
	// (CostRemoveCounter's payCostFromSource shape, :151-165): N counters of
	// the kind removed from the source.
	subCounterN    int
	subCounterType CounterType
	// removeAnySelf is RemoveAnyCounter<N/Kind/CARDNAME|NICKNAME>: the counters
	// come off the source itself (HumanCostDecision.java:1046-1047).
	removeAnySelf bool
	// discardTypeN/discardTypeSpec are Discard<N/Type> for a valid type past
	// Card and Hand: N cards of the type from the payer's hand (CostDiscard).
	discardTypeN    int
	discardTypeSpec string
	// movedToGraveN/movedToGraveSpec are ExiledMoveToGrave<N/Type>: N exiled
	// cards of the type, any player's exile, put into their owners' graveyards
	// (CostExiledMoveToGrave.java:48-92).
	movedToGraveN    int
	movedToGraveSpec string
	// forage is Forage (CostForage.java:19-65): exile three cards from the
	// payer's graveyard or sacrifice a Food.
	forage bool
	// exileHandAll is ExileFromHand<N/All>: the payer's whole hand, always
	// payable (CostExile.java:178-180).
	exileHandAll bool
	// exileEitherN/exileEitherSpec are ExileCtrlOrGrave<N/Type>: N cards of the
	// type the payer controls on the battlefield or owns in the graveyard
	// (Cost.java:487-492).
	exileEitherN    int
	exileEitherSpec string
	// addCounterN/addCounterType are AddCounter<N/Type>: put N counters of a
	// kind on the source itself (CostPutCounter's CARDNAME shape, Fabricate).
	addCounterN    int
	addCounterType CounterType
	// addCounterSpec is the valid string of the card AddCounter<N/Type/Spec>
	// puts the counters on (the payer chooses one), or "" for the source
	// itself. Blight<N> is AddCounter<N/M1M1/Creature.YouCtrl>
	// (CostBlight extends CostPutCounter).
	addCounterSpec string
	// exileGraveN/exileGraveSpec are ExileFromGrave<N/Type>: N cards of the
	// type exiled from the payer's graveyard (CostExile).
	exileGraveN    int
	exileGraveSpec string
	// exileGraveAll is ExileFromGrave<N/All>: the payer's whole graveyard,
	// payable when empty (CostExile.canPay answers true for "All" before any
	// count, CostExile.java:178-179). exileGraveTop is
	// ExileFromGrave<1/Card.TopGraveyardCreature>: only the graveyard's last
	// creature qualifies (CardProperty.java:582-587).
	exileGraveAll, exileGraveTop bool
	// exileStackSelf is ExileFromStack<1/Card.Self> (CostExileFromStack): the
	// spell being resolved exiles itself, so it never reaches the graveyard.
	exileStackSelf bool
	// discardRandomN is Discard<N/Random>: N cards of the hand at random, no
	// choice (CostDiscard's "Random" type).
	discardRandomN int
	// exileTypesN/exileTypesSpec are ExileFromGrave<X/Type+withTypesGE<N>>:
	// any number of the type, at least one, exiled from the payer's graveyard
	// with N or more card types among them (Nethergoyf's escape). Only
	// escapeCost sets them (escapeTypesPart); parseUnlessCost never does.
	exileTypesN    int
	exileTypesSpec string
	// exileHandN/exileHandSpec are ExileFromHand<N/Type>: N cards of the type
	// exiled from the payer's hand, the source itself never among them
	// (CostExile from the hand; Allosaurus Rider's "do not count it",
	// CostExile.java:265-269). exileFieldN/exileFieldSpec are Exile<N/Type>:
	// N permanents of the type the payer controls, exiled (CostExile's
	// battlefield default). Both are the cost's "Exiled" paid list.
	exileHandN     int
	exileHandSpec  string
	exileFieldN    int
	exileFieldSpec string
	// tapN/tapSpec are tapXType<N/Type>: N untapped permanents of the type the
	// payer controls (CostTapType).
	tapN    int
	tapSpec string
	// waterbendN is Waterbend<N>: N generic mana the payer may pay by tapping
	// untapped artifacts and creatures instead (CostWaterbend, which
	// CostAdjustment.adjustCostByWaterbend reduces like Convoke). The mana is
	// in mana already.
	waterbendN int
	// evidenceN is CollectEvidence<N>: cards exiled from the payer's graveyard
	// with total mana value at least N (CostCollectEvidence).
	evidenceN int
	// youCounterN/youCounterType are AddCounterYou<N/Type>: the payer gets N
	// counters (CostPutCounterYou).
	youCounterN    int
	youCounterType CounterType
	// libN/libSpec/libPos are PutCardToLibFromGrave<N/Pos/Type>: N cards of the
	// type from the payer's graveyard on the top (libPos 0) or the bottom
	// (libraryBottom) of their library (CostPutCardToLib).
	libN    int
	libSpec string
	libPos  int
	// removeAnyN/removeAnyKind/removeAnySpec are RemoveAnyCounter<N/Kind/Type>:
	// N counters (of the kind, or "Any") removed from among the payer's
	// permanents of the type (CostRemoveAnyCounter).
	removeAnyN    int
	removeAnyKind string
	removeAnySpec string
	// drawSpec is the player spec of a Draw<N/Spec> that is not "You": every
	// player it names draws N (CostDraw.getPotentialPlayers), "" for You.
	drawSpec string
	// targeted, set by the ability that owns the cost (withTargets) when
	// drawSpec names Player.targetedBy, limits that Draw to the players the
	// ability itself targets; hasTargeted is false for a cost no ability
	// owns (a Ward, a replacement), where it names every matching player.
	targeted    []PlayerID
	hasTargeted bool
	// manaTokens are the cost's mana symbols, Waterbend's included, for times.
	manaTokens []string
}

// parseUnlessCost reads text into an unlessCost, false for a shape past
// mana, PayLife, PayEnergy, DamageYou<N>, Draw<N/You>, Discard<N/Card>,
// AddCounter<N/Type> on the source, one Sac, Return or Reveal part and the
// chosen-card parts below (ExileFromGrave, ExileFromHand, Exile from the
// battlefield, tapXType, ...); a Tap, an unannounced X or another part is
// its own further mechanic (GO-7).
func parseUnlessCost(text string) (unlessCost, bool) {
	return parseUnlessCostX(text, 0, false)
}

// parseUnlessCostX is parseUnlessCost for a cost whose parts may name the
// announced X (`Discard<X/Card>`, `Sac<X/Creature>`, `PayLife<X>`): hasX says
// X was announced as x, and a part of amount X then costs x. An X of 0 drops
// the part (CostPart.getAbilityAmount is 0, which pays nothing), and an X mana
// shard is kept: it is paid from the card's printed cost.
func parseUnlessCostX(text string, x int, hasX bool) (unlessCost, bool) {
	parsed := cost.Parse(text)
	if parsed.Tap || parsed.Untap || parsed.XMin != "" {
		return unlessCost{}, false
	}
	uc := unlessCost{parsed: parsed, mandatory: parsed.Mandatory}
	for _, p := range parsed.Parts {
		var n int
		switch {
		case p.Name == "Forage" && !uc.forage:
			uc.forage = true
			continue
		case p.Name == "Discard" && p.Field(1) == "Hand":
			// CostDiscard's "Hand" type discards the whole hand whatever the
			// amount (Vaultguard Trooper writes Discard<0/Hand>).
			uc.discardHand = true
			continue
		case p.Name == "ExileFromHand" && p.Field(1) == "All" && !uc.exileHandAll:
			uc.exileHandAll = true
			continue
		case p.Name == "ChooseCreatureType" && p.Field(0) == "1" && !uc.chooseCreatureType:
			uc.chooseCreatureType = true
			continue
		}
		if hasX && p.Field(0) == "X" {
			if x <= 0 {
				continue
			}
			n = x
		} else {
			var err error
			n, err = strconv.Atoi(p.Field(0))
			if err != nil || n < 0 {
				return unlessCost{}, false
			}
			if n == 0 {
				// An SVar amount that expanded to 0 pays nothing
				// (CostPart.getAbilityAmount is 0), like an announced X of 0.
				continue
			}
		}
		switch {
		case p.Name == "PayLife":
			uc.lifeN += n
		case p.Name == "PayEnergy":
			uc.energyN += n
		case p.Name == "Discard" && p.Field(1) == "Card":
			uc.discardN += n
		case p.Name == "Discard" && p.Field(1) == "Random" && uc.discardRandomN == 0:
			uc.discardRandomN = n
		case p.Name == "ExileFromGrave" && p.Field(1) == "All" && !uc.exileGraveAll && uc.exileGraveN == 0:
			uc.exileGraveAll = true
		case p.Name == "ExileFromGrave" && p.Field(1) == "Card.TopGraveyardCreature" && uc.exileGraveN == 0 && !uc.exileGraveAll:
			uc.exileGraveN, uc.exileGraveSpec, uc.exileGraveTop = n, "Creature", true
		case p.Name == "ExileFromStack" && n == 1 && p.Field(1) == "Card.Self" && !uc.exileStackSelf:
			uc.exileStackSelf = true
		case p.Name == "Discard" && uc.discardTypeN == 0 && chosenCardSpecResolvable(p.Field(1)):
			uc.discardTypeN, uc.discardTypeSpec = n, p.Field(1)
		case p.Name == "SubCounter" && uc.subCounterN == 0 && p.Field(1) != "" && (p.Field(2) == "" || p.Field(2) == "CARDNAME" || p.Field(2) == "NICKNAME"):
			uc.subCounterN, uc.subCounterType = n, CounterType(strings.ToUpper(p.Field(1)))
		case p.Name == "RemoveAnyCounter" && uc.removeAnyN == 0 && p.Field(1) != "" && (p.Field(2) == "CARDNAME" || p.Field(2) == "NICKNAME"):
			uc.removeAnyN, uc.removeAnyKind, uc.removeAnySelf = n, p.Field(1), true
		case p.Name == "ExiledMoveToGrave" && uc.movedToGraveN == 0 && chosenCardSpecResolvable(p.Field(1)):
			uc.movedToGraveN, uc.movedToGraveSpec = n, p.Field(1)
		case p.Name == "ExileCtrlOrGrave" && uc.exileEitherN == 0 && chosenCardSpecResolvable(p.Field(1)):
			uc.exileEitherN, uc.exileEitherSpec = n, p.Field(1)
		case p.Name == "RevealOrChoose" && uc.revealN == 0 && revealSpecResolvable(p.Field(1)):
			uc.revealN, uc.revealSpec, uc.revealBattlefield = n, p.Field(1), true
		case p.Name == "Sac" && uc.sacN == 0 && p.Field(1) != "":
			uc.sacN, uc.sacSpec = n, p.Field(1)
		case p.Name == "Return" && uc.returnN == 0 && p.Field(1) != "":
			uc.returnN, uc.returnSpec = n, p.Field(1)
		case p.Name == "Reveal" && uc.revealN == 0 && revealSpecResolvable(p.Field(1)):
			uc.revealN, uc.revealSpec = n, p.Field(1)
		case p.Name == "Behold" && uc.revealN == 0 && revealSpecResolvable(p.Field(1)):
			// CostBehold is a CostReveal over the hand and the battlefield.
			uc.revealN, uc.revealSpec, uc.revealBattlefield, uc.revealBehold = n, p.Field(1), true, true
		case p.Name == "AddCounter" && uc.addCounterN == 0 && p.Field(1) != "" && (p.Field(2) == "" || p.Field(2) == "CARDNAME"):
			uc.addCounterN, uc.addCounterType = n, CounterType(strings.ToUpper(p.Field(1)))
		case p.Name == "AddCounter" && uc.addCounterN == 0 && p.Field(1) != "" && chosenCardSpecResolvable(p.Field(2)):
			uc.addCounterN, uc.addCounterType, uc.addCounterSpec = n, CounterType(strings.ToUpper(p.Field(1))), p.Field(2)
		case p.Name == "Blight" && uc.addCounterN == 0:
			uc.addCounterN, uc.addCounterType, uc.addCounterSpec = n, M1M1, "Creature.YouCtrl"
		case p.Name == "ExileFromGrave" && uc.exileGraveN == 0 && chosenCardSpecResolvable(p.Field(1)):
			uc.exileGraveN, uc.exileGraveSpec = n, p.Field(1)
		case p.Name == "ExileFromHand" && uc.exileHandN == 0 && chosenCardSpecResolvable(p.Field(1)):
			uc.exileHandN, uc.exileHandSpec = n, p.Field(1)
		case p.Name == "Exile" && uc.exileFieldN == 0 && chosenCardSpecResolvable(p.Field(1)):
			uc.exileFieldN, uc.exileFieldSpec = n, p.Field(1)
		case p.Name == "tapXType" && uc.tapN == 0 && p.Field(0) != "Any" && p.Field(1) != "" && tapTypeResolvable(p.Field(1)):
			uc.tapN, uc.tapSpec = n, p.Field(1)
		case p.Name == "Waterbend" && uc.waterbendN == 0:
			uc.waterbendN = n
			parsed.Mana = append(append([]string(nil), parsed.Mana...), strconv.Itoa(n))
		case p.Name == "PutCardToLibFromGrave" && uc.libN == 0 && (p.Field(1) == "0" || p.Field(1) == "-1") && chosenCardSpecResolvable(p.Field(2)):
			uc.libN, uc.libSpec, uc.libPos = n, p.Field(2), 0
			if p.Field(1) == "-1" {
				uc.libPos = libraryBottom
			}
		case p.Name == "RemoveAnyCounter" && uc.removeAnyN == 0 && chosenCardSpecResolvable(p.Field(2)):
			uc.removeAnyN, uc.removeAnyKind, uc.removeAnySpec = n, p.Field(1), p.Field(2)
		case p.Name == "CollectEvidence" && uc.evidenceN == 0:
			uc.evidenceN = n
		case p.Name == "AddCounterYou" && uc.youCounterN == 0 && p.Field(1) != "":
			uc.youCounterN, uc.youCounterType = n, CounterType(strings.ToUpper(p.Field(1)))
		case p.Name == "DamageYou":
			uc.damageN += n
		case p.Name == "Draw" && p.Field(1) == "You":
			uc.drawN += n
		case p.Name == "Draw" && uc.drawN == 0 && drawSpecResolvable(p.Field(1)):
			uc.drawN, uc.drawSpec = n, p.Field(1)
		default:
			return unlessCost{}, false
		}
	}
	uc.manaTokens = parsed.Mana
	if len(parsed.Mana) > 0 {
		mc, err := mana.Parse(strings.Join(parsed.Mana, " "))
		if err != nil || (mc.CountX() > 0 && !hasX) {
			return unlessCost{}, false
		}
		uc.mana, uc.hasMana = mc, true
	}
	return uc, true
}

// withTargets binds a Draw<N/Player.targetedBy> part to the players a's own
// targets name (the spec's "targetedBy", AbilityUtils.getDefinedPlayers over
// the ability's targets), so the draw does not fall on every player.
func (uc unlessCost) withTargets(a *Ability) unlessCost {
	if !strings.Contains(uc.drawSpec, "targetedBy") {
		return uc
	}
	uc.hasTargeted = true
	uc.targeted = nil
	for _, t := range a.Targets {
		if pid, ok := t.AsPlayer(); ok {
			uc.targeted = append(uc.targeted, pid)
		}
	}
	return uc
}

// withTriggered binds a Draw<N/Player.Triggered...> or Player.Activator part
// to the players the ability's trigger objects or its host card's controller
// name (PlayerProperty.java:26-29, :440-443), so the draw does not fall on
// every player. An alternative list is not bound: no corpus line mixes them.
func (uc unlessCost) withTriggered(g *Game, a *Ability) (unlessCost, error) {
	_, prop, ok := strings.Cut(uc.drawSpec, ".")
	if !ok || strings.Contains(uc.drawSpec, ",") {
		return uc, nil
	}
	defined, bound := boundDrawProp(prop)
	if !bound {
		return uc, nil
	}
	players, err := definedPlayers(g, a.Controller, a.Source, defined, a.refs())
	if err != nil {
		return uc, fmt.Errorf("engine: UnlessCost$ Draw: %w", err)
	}
	uc.hasTargeted, uc.targeted = true, players
	return uc, nil
}

// unlessSacCandidates is what pid may sacrifice for the Sac part: the source
// itself for CARDNAME/NICKNAME (when pid controls it and it is not
// phased out, Card.canBeSacrificedBy), else pid's permanents matching the
// valid string.
func (g *Game) unlessSacCandidates(pid PlayerID, source CardID, uc unlessCost) []CardID {
	if uc.sacN == 0 {
		return nil
	}
	if uc.sacSpec == "CARDNAME" || uc.sacSpec == "NICKNAME" {
		c := g.Card(source)
		if c.Zone == Battlefield && !c.IsPhasedOut() && c.Controller() == pid {
			return []CardID{source}
		}
		return nil
	}
	spec := valid.Parse(strings.ReplaceAll(uc.sacSpec, ";", ","))
	var out []CardID
	for _, id := range g.Zone(Battlefield, pid).Cards() {
		if Matches(g, g.Card(id), spec, g.Card(source).Controller(), source) {
			out = append(out, id)
		}
	}
	return out
}

// unlessReturnCandidates is what pid may return to hand for the Return part:
// the source itself for CARDNAME/NICKNAME (CostReturn's self shape, when pid
// controls it on the battlefield), else pid's permanents matching the valid
// string (returnTypeCandidates, the activation-cost Return<N/Type> twin).
func (g *Game) unlessReturnCandidates(pid PlayerID, source CardID, uc unlessCost) []CardID {
	if uc.returnN == 0 {
		return nil
	}
	if uc.returnSpec == "CARDNAME" || uc.returnSpec == "NICKNAME" {
		c := g.Card(source)
		if c.Zone == Battlefield && c.Controller() == pid {
			return []CardID{source}
		}
		return nil
	}
	return returnTypeCandidates(g, pid, source, uc.returnSpec)
}

// revealSpecResolvable is false for CostReveal's own special types (the whole
// "Hand", "SameColor", the source itself), each a different payment.
func revealSpecResolvable(spec string) bool {
	switch spec {
	case "", "Hand", "SameColor", "CARDNAME", "NICKNAME":
		return false
	}
	return true
}

// unlessRevealable reports whether pid's hand holds the cards the Reveal part
// names: revealN cards matching revealSpec, an OR list when it carries ";"
// (the Cost syntax's own comma, as returnTypeCandidates reads it).
func (g *Game) unlessRevealable(pid PlayerID, source CardID, uc unlessCost) bool {
	if uc.revealN == 0 {
		return true
	}
	if uc.revealBehold && strings.HasSuffix(uc.revealSpec, "ChosenType") {
		return g.beholdSharesType(pid, source, uc.revealN)
	}
	spec := valid.Parse(strings.ReplaceAll(uc.revealSpec, ";", ","))
	n := 0
	zones := []ZoneType{Hand}
	if uc.revealBattlefield {
		zones = append(zones, Battlefield)
	}
	for _, z := range zones {
		for _, id := range g.Zone(z, pid).Cards() {
			// A Behold paid while casting: the spell is already on the stack, not in
			// the hand it is chosen from.
			if uc.revealBattlefield && id == source && z == Hand {
				continue
			}
			if Matches(g, g.Card(id), spec, pid, source) {
				n++
			}
		}
	}
	return n >= uc.revealN
}

// payUnlessCost pays uc for pid, reporting whether it was paid. Every part is
// checked payable first -- life at least PayLife (CR 119.4), enough cards to
// discard, enough permanents to sacrifice -- and the mana is paid next, which
// fails atomically (PayManaCost), so a cost that cannot be met leaves
// everything as it was. Then life is paid (the LifeChanged event
// loseLifeEffect emits), the controller chooses the discards and the
// sacrifices.
func (g *Game) payUnlessCost(controller PlayerController, a *Ability, pid PlayerID, uc unlessCost) bool {
	return g.payUnlessCostInto(controller, a, pid, uc, nil)
}

// payUnlessCostInto is payUnlessCost recording the cards its parts used up
// into rec when rec is not nil (payUnlessPartsInto).
func (g *Game) payUnlessCostInto(controller PlayerController, a *Ability, pid PlayerID, uc unlessCost, rec *paidLists) bool {
	if !g.unlessPayable(pid, a.Source, uc) {
		return false
	}
	mc, taps := uc.mana, []CardID(nil)
	if uc.waterbendN > 0 {
		mc, taps = g.waterbendReduce(controller, pid, a.Source, uc)
	}
	if uc.hasMana && !g.PayManaCost(pid, mc, controller) {
		return false
	}
	tapChosenPermanents(g, controller, taps)
	return g.payUnlessPartsInto(controller, a, pid, uc, rec)
}

// waterbendReduce is CostAdjustment.adjustCostByWaterbend: the payer may tap
// untapped artifacts and creatures, each paying one generic mana, at most
// Waterbend's N of them. It returns the mana still to pay and what to tap once
// that is paid. A pick that is not a subset of the candidates, or longer than
// the Waterbend part or the generic mana, is declined whole.
func (g *Game) waterbendReduce(controller PlayerController, pid PlayerID, source CardID, uc unlessCost) (mana.Cost, []CardID) {
	var candidates []CardID
	for _, id := range g.Zone(Battlefield, pid).Cards() {
		c := g.Card(id)
		if !c.Tapped && (c.Type().Has(cardtype.Artifact) || c.Type().Has(cardtype.Creature)) {
			candidates = append(candidates, id)
		}
	}
	generic := uc.mana.Generic()
	limit := min(uc.waterbendN, generic, len(candidates))
	if limit == 0 {
		return uc.mana, nil
	}
	picks := controller.ChooseCardsForEffect(g, pid, source, candidates, 0, limit)
	if !isSubset(picks, candidates) || hasDuplicate(picks) || len(picks) > limit {
		return uc.mana, nil
	}
	return mana.FromShards(uc.mana.Shards(), generic-len(picks)), picks
}

// unlessPayable reports whether every non-mana part of uc can be paid by pid:
// life, energy, cards to discard, permanents to sacrifice or return, a card to
// reveal and a draw (CostPart.canPay for each).
func (g *Game) unlessPayable(pid PlayerID, source CardID, uc unlessCost) bool {
	hand := handWithout(g.Zone(Hand, pid).Cards(), source)
	candidates := g.unlessSacCandidates(pid, source, uc)
	returnable := g.unlessReturnCandidates(pid, source, uc)
	return uc.lifeN <= g.Player(pid).Life && (uc.lifeN == 0 || !g.cantPayLife(pid, false, causeNone)) && uc.energyN <= g.Player(pid).Counters.Count(Energy) &&
		uc.discardN <= len(hand) && uc.discardRandomN <= len(hand) && uc.sacN <= len(candidates) && uc.returnN <= len(returnable) &&
		(uc.drawN == 0 || len(g.unlessDrawers(pid, source, uc)) > 0) && g.unlessRevealable(pid, source, uc) &&
		g.unlessCounterable(source, uc) && g.unlessCardsPayable(pid, source, uc) && g.extraPartsPayable(pid, source, uc)
}

// unlessCounterable is CostPutCounter.canPay for the source itself: it is on the
// battlefield and may receive the counters. An AddCounter naming a card type
// is unlessCardsPayable's.
func (g *Game) unlessCounterable(source CardID, uc unlessCost) bool {
	if uc.addCounterN == 0 || uc.addCounterSpec != "" {
		return true
	}
	return g.Card(source).Zone == Battlefield && !g.Card(source).IsPhasedOut() && !g.cantPutCounter(CardEntity(source), uc.addCounterType)
}

// unlessDrawers is CostDraw.getPotentialPlayers: the players the Draw part
// names that may draw its cards, in seating order. A Draw<N/You> names the
// payer.
func (g *Game) unlessDrawers(pid PlayerID, source CardID, uc unlessCost) []PlayerID {
	var out []PlayerID
	for _, cand := range g.Players() {
		if uc.drawSpec == "" && cand != pid {
			continue
		}
		if uc.drawSpec != "" && !drawSpecMatches(g, cand, pid, source, uc.drawSpec) {
			continue
		}
		if uc.hasTargeted && !slices.Contains(uc.targeted, cand) {
			continue
		}
		if !g.cantDrawAmount(cand, uc.drawN) {
			out = append(out, cand)
		}
	}
	return out
}

// drawSpecResolvable is whether every alternative of a Draw part's player spec
// is one drawSpecMatches reads: a base (You, Opponent, Player) with at most a
// property matchesPlayerSpec resolves, or Player.targetedBy, Player.Activator
// and Player.Triggered* (withTriggered binds those).
func drawSpecResolvable(spec string) bool {
	if spec == "" || spec == "You" {
		return false
	}
	for _, alt := range strings.Split(spec, ",") {
		base, prop, hasProp := strings.Cut(alt, ".")
		switch base {
		case "You", "Opponent", "Player":
		default:
			return false
		}
		if hasProp {
			switch prop {
			case "targetedBy", "You", "Opponent", "Other", "Active", "NonActive":
			default:
				if _, bound := boundDrawProp(prop); !bound {
					return false
				}
			}
		}
	}
	return true
}

// boundDrawProp is whether a Draw part's player property names players only
// the owning ability knows: PlayerProperty's Activator (the host card's
// controller, :26-29) or any "Triggered..." (:440-443, getDefinedPlayers over
// the trigger's recorded objects).
func boundDrawProp(prop string) (defined string, ok bool) {
	if prop == "Activator" {
		return "You", true
	}
	return prop, strings.HasPrefix(prop, "Triggered")
}

// drawSpecMatches is Player.isValid(spec, payer, source, ability) for a Draw
// part's player spec. Player.targetedBy names a player the ability targeted,
// which the payment is not handed, so it names every player here: a drawer
// outside the ability's targets cannot be told apart.
func drawSpecMatches(g *Game, cand, payer PlayerID, source CardID, spec string) bool {
	spec = strings.ReplaceAll(spec, ".targetedBy", "")
	if base, prop, ok := strings.Cut(spec, "."); ok {
		if _, bound := boundDrawProp(prop); bound {
			spec = base
		}
	}
	matched, _ := matchesPlayerSpec(g, cand, payer, source, spec)
	return matched
}

// chosenCardSpecResolvable is whether a cost part's valid string names cards
// valid.Parse and Matches evaluate (not the self tokens, not an empty field).
func chosenCardSpecResolvable(spec string) bool {
	switch spec {
	case "", "CARDNAME", "NICKNAME", "Hand", "All", "Any", "Random", "DifferentNames", "SameName", "LastDrawn":
		return false
	}
	return !strings.Contains(spec, "TopGraveyard")
}

// unlessCardsPayable is whether the chosen-card parts have their cards:
// graveyard cards to exile, permanents to tap, graveyard mana value for
// evidence, a card to put the counters on and a player who may get counters.
func (g *Game) unlessCardsPayable(pid PlayerID, source CardID, uc unlessCost) bool {
	if uc.exileStackSelf && g.Card(source).Zone != Stack {
		return false
	}
	if uc.exileGraveN > 0 && len(g.exileGraveOptions(pid, source, uc)) < uc.exileGraveN {
		return false
	}
	if uc.exileTypesN > 0 {
		// CostExile.canPay (CostExile.java:215-228): the card types among every
		// valid graveyard card reach N.
		options := g.costCandidates(pid, source, false, Graveyard, uc.exileTypesSpec)
		if len(options) == 0 || coreTypeCount(g, options) < uc.exileTypesN {
			return false
		}
	}
	if uc.exileHandN > 0 && len(g.costCandidates(pid, source, false, Hand, uc.exileHandSpec)) < uc.exileHandN {
		return false
	}
	if uc.exileFieldN > 0 && len(g.costCandidates(pid, source, false, Battlefield, uc.exileFieldSpec)) < uc.exileFieldN {
		return false
	}
	if uc.tapN > 0 && len(tapTypeCandidates(g, pid, source, false, uc.tapSpec)) < uc.tapN {
		return false
	}
	if uc.libN > 0 && len(g.costCandidates(pid, source, false, Graveyard, uc.libSpec)) < uc.libN {
		return false
	}
	if uc.removeAnyN > 0 && g.removableCounters(pid, source, uc) < uc.removeAnyN {
		return false
	}
	if uc.evidenceN > 0 && graveyardManaValue(g, g.Zone(Graveyard, pid).Cards()) < uc.evidenceN {
		return false
	}
	if uc.youCounterN > 0 && (g.Player(pid).Lost || g.cantPutCounter(PlayerEntity(pid), uc.youCounterType)) {
		return false
	}
	if uc.addCounterSpec != "" && len(g.unlessCounterTargets(pid, source, uc)) == 0 {
		return false
	}
	return true
}

// unlessCounterTargets is the cards AddCounter<N/Type/Spec> may put counters on
// (CostPutCounter.canPay: the valid cards that can receive the counters).
func (g *Game) unlessCounterTargets(pid PlayerID, source CardID, uc unlessCost) []CardID {
	var out []CardID
	for _, id := range g.costCandidates(pid, source, false, Battlefield, uc.addCounterSpec) {
		if !g.cantPutCounter(CardEntity(id), uc.addCounterType) {
			out = append(out, id)
		}
	}
	return out
}

// removableKinds is the counter kinds on c a RemoveAnyCounter<N/Kind/Type> part
// may remove: the named kind, or any kind for "Any" (CostRemoveAnyCounter
// .getMaxAmountX).
func removableKinds(c *Card, kind string) []CounterType {
	if !strings.EqualFold(kind, "Any") {
		ct := CounterType(strings.ToUpper(kind))
		if c.Counters.Count(ct) > 0 {
			return []CounterType{ct}
		}
		return nil
	}
	return c.Counters.Kinds()
}

// removeAnyCandidates is the permanents the RemoveAnyCounter part takes counters
// from: the source itself for CARDNAME/NICKNAME (HumanCostDecision.java:
// 1046-1047; CostRemoveAnyCounter's payCostFromSource), when it is a permanent
// that is not phased out, else pid's permanents of the type.
func (g *Game) removeAnyCandidates(pid PlayerID, source CardID, uc unlessCost) []CardID {
	if uc.removeAnySelf {
		if c := g.Card(source); c.Zone == Battlefield && !c.IsPhasedOut() {
			return []CardID{source}
		}
		return nil
	}
	return g.costCandidates(pid, source, false, Battlefield, uc.removeAnySpec)
}

// removableCounters is how many counters the RemoveAnyCounter part could remove:
// the sum over the payer's matching permanents (CostRemoveAnyCounter.canPay is
// the amount against it).
func (g *Game) removableCounters(pid PlayerID, source CardID, uc unlessCost) int {
	total := 0
	for _, id := range g.removeAnyCandidates(pid, source, uc) {
		c := g.Card(id)
		for _, ct := range removableKinds(c, uc.removeAnyKind) {
			total += c.Counters.Count(ct)
		}
	}
	return total
}

// removeAnyCounters removes the RemoveAnyCounter part's N counters one at a
// time: the payer picks a permanent that has one, and a kind when it has
// several (CostRemoveAnyCounter.payAsDecided applies the chosen counter table).
func (g *Game) removeAnyCounters(controller PlayerController, pid PlayerID, source CardID, uc unlessCost) {
	for range uc.removeAnyN {
		var options []CardID
		for _, id := range g.removeAnyCandidates(pid, source, uc) {
			if len(removableKinds(g.Card(id), uc.removeAnyKind)) > 0 {
				options = append(options, id)
			}
		}
		if len(options) == 0 {
			return
		}
		id := g.chosenUnlessCards(controller, pid, source, options, 1, 1)[0]
		c := g.Card(id)
		kinds := removableKinds(c, uc.removeAnyKind)
		kind := kinds[0]
		if len(kinds) > 1 {
			labels := make([]string, len(kinds))
			for i, k := range kinds {
				labels[i] = string(k)
			}
			if pick := controller.ChooseOption(g, pid, source, labels); pick > 0 && pick < len(kinds) {
				kind = kinds[pick]
			}
		}
		removeCounters(g, source, CardEntity(id), &c.Counters, kind, false, false, 1)
	}
}

// graveyardManaValue is the total mana value of ids (CardLists.getTotalCMC).
func graveyardManaValue(g *Game, ids []CardID) int {
	total := 0
	for _, id := range ids {
		total += g.Card(id).CMC()
	}
	return total
}

// payUnlessParts pays uc's non-mana parts, after unlessPayable said they can
// be: life (the LifeChanged event loseLifeEffect emits), energy, the controller's
// discards and sacrifices, returns, damage to the payer and draws.
func (g *Game) payUnlessParts(controller PlayerController, a *Ability, pid PlayerID, uc unlessCost) bool {
	return g.payUnlessPartsInto(controller, a, pid, uc, nil)
}

// payUnlessPartsInto is payUnlessParts recording what the cost sacrificed and
// discarded into rec, when rec is not nil: a spell's additional cost is that
// spell's own paid list (SpellAbility.getPaidList, read by Sacrificed$ and
// Discarded$), where an unless cost's payment is not. It reports false when a
// choice part could not be paid after all (payChoiceParts), before anything
// else is.
func (g *Game) payUnlessPartsInto(controller PlayerController, a *Ability, pid PlayerID, uc unlessCost, rec *paidLists) bool {
	if !g.payChoiceParts(controller, a, pid, uc) {
		return false
	}
	hand := handWithout(g.Zone(Hand, pid).Cards(), a.Source)
	candidates := g.unlessSacCandidates(pid, a.Source, uc)
	returnable := g.unlessReturnCandidates(pid, a.Source, uc)
	if uc.lifeN > 0 {
		g.Player(pid).Life -= uc.lifeN
		g.sink.Emit(Event{Kind: LifeChanged, Source: a.Source, Target: PlayerEntity(pid), Amount: -int32(uc.lifeN)})
	}
	if uc.energyN > 0 {
		g.Player(pid).Counters.Add(Energy, -uc.energyN)
		emitCounterChanged(g, a.Source, PlayerEntity(pid), Energy, -uc.energyN)
	}
	if uc.discardN > 0 {
		chosen := controller.ChooseCardsToDiscard(g, pid, hand, uc.discardN)
		discardCards(g, controller, chosen, pid)
		if rec != nil {
			for _, id := range chosen {
				if g.Card(id).Zone != Hand {
					rec.discarded = append(rec.discarded, id)
				}
			}
		}
	}
	if uc.discardRandomN > 0 {
		// Aggregates.random over the hand (PlaySpellAbility.java:335-339).
		var chosen []CardID
		for _, i := range g.randomSample(len(hand), uc.discardRandomN) {
			chosen = append(chosen, hand[i])
		}
		discardCards(g, controller, chosen, pid)
		if rec != nil {
			for _, id := range chosen {
				if g.Card(id).Zone != Hand {
					rec.discarded = append(rec.discarded, id)
				}
			}
		}
	}
	if uc.discardHand {
		// Discard<N/Hand> (CostDiscard's "Hand" type): the payer discards the
		// whole hand, no choice, and an empty hand pays it too.
		discardCards(g, controller, slices.Clone(g.Zone(Hand, pid).Cards()), pid)
	}
	if uc.sacN > 0 {
		sacrificed := sacrificeCardsFor(g, controller, a, controller.ChoosePermanentsToSacrifice(g, pid, candidates, uc.sacN), false)
		if rec != nil {
			rec.sacrificed = append(rec.sacrificed, sacrificed...)
		}
	}
	if uc.returnN > 0 {
		returnCards(g, controller, controller.ChoosePermanentsToReturn(g, pid, returnable, uc.returnN))
	}
	if uc.damageN > 0 {
		var table damageTable
		g.dealPlayerDamage(controller, a.Source, pid, uc.damageN, false, &table)
		g.checkDamageTableTriggers(controller, table, false)
	}
	if uc.drawN > 0 {
		for _, drawer := range g.unlessDrawers(pid, a.Source, uc) {
			g.DrawCards(drawer, uc.drawN, controller)
		}
	}
	if uc.exileGraveN > 0 {
		picks := g.chosenUnlessCards(controller, pid, a.Source, g.exileGraveOptions(pid, a.Source, uc), uc.exileGraveN, uc.exileGraveN)
		exiled := g.exileCostCards(controller, a.Source, picks)
		if rec != nil {
			rec.exiled = append(rec.exiled, exiled...)
		}
	}
	if uc.exileStackSelf && g.Card(a.Source).Zone == Stack {
		// CostExileFromStack.payAsDecided: the resolving spell leaves the stack
		// for exile, so moveResolvedSpellToGraveyard finds it gone.
		g.removeSpellFromStack(a.Source)
		g.Move(a.Source, Exile, g.Card(a.Source).Owner)
		g.markExiledWith(a.Source, a.Source)
		g.checkChangesZoneAllTriggers(controller, []CardID{a.Source}, Stack, Exile)
		if rec != nil {
			rec.exiled = append(rec.exiled, a.Source)
		}
	}
	if uc.exileGraveAll {
		exiled := g.exileCostCards(controller, a.Source, slices.Clone(g.Zone(Graveyard, pid).Cards()))
		if rec != nil {
			rec.exiled = append(rec.exiled, exiled...)
		}
	}
	if uc.exileTypesN > 0 {
		for _, id := range g.chosenTypesCards(controller, pid, a.Source, uc) {
			exileFromGraveyard(g, controller, id)
		}
	}
	if uc.exileHandN > 0 {
		picks := g.chosenUnlessCards(controller, pid, a.Source, g.costCandidates(pid, a.Source, false, Hand, uc.exileHandSpec), uc.exileHandN, uc.exileHandN)
		for _, id := range picks {
			exileFromHand(g, id)
			g.markExiledWith(id, a.Source)
		}
		if rec != nil {
			rec.exiled = append(rec.exiled, picks...)
		}
	}
	if uc.exileFieldN > 0 {
		picks := g.chosenUnlessCards(controller, pid, a.Source, g.costCandidates(pid, a.Source, false, Battlefield, uc.exileFieldSpec), uc.exileFieldN, uc.exileFieldN)
		exileCards(g, controller, picks)
		for _, id := range picks {
			g.markExiledWith(id, a.Source)
		}
		if rec != nil {
			rec.exiled = append(rec.exiled, picks...)
		}
	}
	if uc.libN > 0 {
		options := g.costCandidates(pid, a.Source, false, Graveyard, uc.libSpec)
		for _, id := range g.chosenUnlessCards(controller, pid, a.Source, options, uc.libN, uc.libN) {
			g.moveByEffect(controller, id, Library, uc.libPos, NoPlayer, false)
		}
	}
	if uc.removeAnyN > 0 {
		g.removeAnyCounters(controller, pid, a.Source, uc)
	}
	if uc.evidenceN > 0 {
		g.collectEvidence(controller, pid, a.Source, uc.evidenceN)
	}
	if uc.tapN > 0 {
		tapChosenPermanents(g, controller, controller.ChoosePermanentsToTap(g, pid, tapTypeCandidates(g, pid, a.Source, false, uc.tapSpec), uc.tapN))
	}
	if uc.youCounterN > 0 {
		if n := g.countersReplaced(controller, pid, PlayerEntity(pid), uc.youCounterType, uc.youCounterN); n > 0 {
			g.Player(pid).Counters.Add(uc.youCounterType, n)
			emitCounterChanged(g, a.Source, PlayerEntity(pid), uc.youCounterType, n)
		}
	}
	g.payExtraParts(controller, a, pid, uc, rec)
	if uc.addCounterN > 0 {
		target := a.Source
		if uc.addCounterSpec != "" {
			target = g.chosenUnlessCards(controller, pid, a.Source, g.unlessCounterTargets(pid, a.Source, uc), 1, 1)[0]
		}
		if n := g.countersReplaced(controller, pid, CardEntity(target), uc.addCounterType, uc.addCounterN); n > 0 {
			// The payer places (CostPutCounter.java:182).
			g.addCardCounters(controller, pid, a.Source, target, uc.addCounterType, n)
		}
	}
	return true
}

// chosenUnlessCards is the payer's pick of lo..hi of options for a cost part.
// An illegal pick (not a subset, duplicates, wrong count) is replaced by the
// first lo options: the cost was checked payable, and a payer cannot back out
// of a payment already half made.
func (g *Game) chosenUnlessCards(controller PlayerController, pid PlayerID, source CardID, options []CardID, lo, hi int) []CardID {
	picks := controller.ChooseCardsForEffect(g, pid, source, options, lo, hi)
	if !isSubset(picks, options) || hasDuplicate(picks) || len(picks) < lo || len(picks) > hi {
		return options[:lo]
	}
	return picks
}

// chosenTypesCards is the payer's pick for an exileTypesN part, the human
// decision of HumanCostDecision.visit(CostExile) (HumanCostDecision.java:
// 323-336): one or more of the valid graveyard cards with exileTypesN or more
// card types among them. A pick short of that is replaced by every valid card,
// which unlessCardsPayable said is enough, as collectEvidence does.
func (g *Game) chosenTypesCards(controller PlayerController, pid PlayerID, source CardID, uc unlessCost) []CardID {
	options := g.costCandidates(pid, source, false, Graveyard, uc.exileTypesSpec)
	picks := controller.ChooseCardsForEffect(g, pid, source, options, 1, len(options))
	if len(picks) == 0 || !isSubset(picks, options) || hasDuplicate(picks) || coreTypeCount(g, picks) < uc.exileTypesN {
		picks = options
	}
	return append([]CardID(nil), picks...)
}

// coreTypeCount is AbilityUtils.countCardTypesFromList(ids, false)
// (AbilityUtils.java:3955-3963): the distinct core types among the cards.
func coreTypeCount(g *Game, ids []CardID) int {
	var seen cardtype.Line
	for _, id := range ids {
		seen = seen.Union(g.Card(id).Type())
	}
	return len(seen.CoreTypes())
}

// escapeTypesPart splits Nethergoyf's ExileFromGrave<X/Type+withTypesGE<N>>
// out of an Escape cost: the rest of the text, the type without the suffix
// and N. ok is false for a cost without such a part. X is the number of cards
// exiled; Java announces it (PlaySpellAbility.java:766-778) but the human
// payment ignores it, choosing any number with N or more types
// (HumanCostDecision.java:323-336), and nothing else reads Nethergoyf's X.
func escapeTypesPart(text string) (rest, spec string, n int, ok bool) {
	for _, p := range cost.Parse(text).Parts {
		if p.Name != "ExileFromGrave" || p.Field(0) != "X" {
			continue
		}
		base, num, found := strings.Cut(p.Field(1), "+withTypesGE")
		if !found {
			continue
		}
		v, err := strconv.Atoi(num)
		if err != nil || v <= 0 || base == "" {
			return "", "", 0, false
		}
		return strings.TrimSpace(strings.Replace(text, p.Text, "", 1)), base, v, true
	}
	return "", "", 0, false
}

// collectEvidence is CostCollectEvidence.payAsDecided: the payer exiles cards
// from their graveyard with total mana value at least n, each marked as exiled
// with source (CostCollectEvidence.java:76-77). A pick short of n is replaced
// by the whole graveyard, which unlessCardsPayable said is enough. It returns
// the cards exiled, the cost's "Collected" paid list.
func (g *Game) collectEvidence(controller PlayerController, pid PlayerID, source CardID, n int) []CardID {
	grave := g.Zone(Graveyard, pid).Cards()
	picks := controller.ChooseCardsForEffect(g, pid, source, grave, 1, len(grave))
	if !isSubset(picks, grave) || hasDuplicate(picks) || graveyardManaValue(g, picks) < n {
		picks = grave
	}
	picks = g.exileCostCards(controller, source, append([]CardID(nil), picks...))
	g.checkCollectEvidenceTriggers(controller, pid)
	return picks
}

// handWithout is hand less source: a spell being cast cannot be discarded to pay
// for itself (CostDiscard leaves the host out while it is a spell).
func handWithout(hand []CardID, source CardID) []CardID {
	out := make([]CardID, 0, len(hand))
	for _, id := range hand {
		if id != source {
			out = append(out, id)
		}
	}
	return out
}

// times is uc paid n times over, Cost.mergeTo(cost, n, sa) for Cumulative
// upkeep: every amount multiplied, the mana repeated. False when the mana does
// not parse again.
func (uc unlessCost) times(n int) (unlessCost, bool) {
	if n > 1 && (uc.forage || uc.chooseCreatureType) {
		// A part with no amount to multiply: paying it n times is n separate
		// payments, which this merge does not model.
		return unlessCost{}, false
	}
	uc.lifeN *= n
	uc.discardN *= n
	uc.discardRandomN *= n
	uc.sacN *= n
	uc.energyN *= n
	uc.damageN *= n
	uc.drawN *= n
	uc.returnN *= n
	uc.revealN *= n
	uc.addCounterN *= n
	uc.exileGraveN *= n
	uc.exileHandN *= n
	uc.exileFieldN *= n
	uc.tapN *= n
	uc.waterbendN *= n
	uc.evidenceN *= n
	uc.libN *= n
	uc.removeAnyN *= n
	uc.youCounterN *= n
	uc.subCounterN *= n
	uc.discardTypeN *= n
	uc.movedToGraveN *= n
	uc.exileEitherN *= n
	if uc.hasMana {
		var tokens []string
		for range n {
			tokens = append(tokens, uc.manaTokens...)
		}
		uc.manaTokens = tokens
		mc, err := mana.Parse(strings.Join(tokens, " "))
		if err != nil {
			return unlessCost{}, false
		}
		uc.mana = mc
	}
	// What the payer is asked to confirm is the whole cost: the text n times
	// over, as Cost.mergeTo adds it.
	var repeated []string
	for range n {
		repeated = append(repeated, uc.parsed.Text)
	}
	uc.parsed = cost.Parse(strings.Join(repeated, " "))
	return uc, true
}

// upkeepCostPaid is the Echo$ and CumulativeUpkeep$ branches of
// SacrificeEffect.resolve: pay the cost or lose the permanent. proceed is true
// when the sacrifice goes ahead -- the cost was not paid and the permanent is
// still its activator's -- and false for an ability with neither param
// (ordinary sacrifice, proceed true), a paid cost, or a permanent that
// changed controller since the trigger.
func (g *Game) upkeepCostPaid(controller PlayerController, a *Ability, source *Card) (proceed bool, err error) {
	echo, isEcho := a.Params.Param("Echo")
	cumulative, isCumulative := a.Params.Param("CumulativeUpkeep")
	if !isEcho && !isCumulative {
		return true, nil
	}
	text, n := echo, 1
	if isCumulative {
		text = cumulative
		// One more AGE counter, through any AddCounter replacement, before
		// the cost is built from how many there are.
		if added := g.countersReplaced(controller, a.Controller, CardEntity(source.ID), Age, 1); added > 0 {
			// The activator places (SacrificeEffect.java:54).
			g.addCardCounters(controller, a.Controller, source.ID, source.ID, Age, added)
		}
		n = source.Counters.Count(Age)
		if n == 0 {
			// SacrificeEffect.java:57: no age counter, no cost to pay.
			return false, nil
		}
	}
	uc, ok := parseUnlessCost(text)
	if ok && n > 1 {
		uc, ok = uc.times(n)
	}
	if !ok {
		return false, fmt.Errorf("engine: Sacrifice: upkeep cost %q not resolvable yet", text)
	}
	paid := (uc.mandatory || controller.ConfirmPayCost(g, a.Controller, uc.parsed, a.Source)) &&
		g.payUnlessCost(controller, a, a.Controller, uc)
	return !paid && source.Controller() == a.Controller, nil
}

// errUnlessNoCost is calculateUnlessCost returning null: a DefinedCost_ naming
// no card. The ability then resolves without asking anyone to pay.
var errUnlessNoCost = errors.New("engine: UnlessCost$ names no cost")

// definedUnlessCost is AbilityUtils.calculateUnlessCost's DefinedCost_<Defined>
// [_Minus<N>|_Plus<N>] (AbilityUtils.java:1450-1471): the mana cost of the first
// card the defined text names, its generic part lowered or raised. UnlessUpTo$
// lets the ability's controller pick how much of a Minus reduction to take,
// 0 to N (AbilityUtils.java:1462-1464, chooseNumberForCostReduction), asked
// through controller.ChooseNumber before the payer is asked to pay; without a
// controller to ask (a Ward cost, which has no such param) it is not resolved
// (GO-7). text is the whole cost for the error, rest what follows
// "DefinedCost_".
func (g *Game) definedUnlessCost(a *Ability, host *Card, text, rest string, controller PlayerController) (string, error) {
	_, upTo := a.Params.Param("UnlessUpTo")
	if upTo && controller == nil {
		return "", fmt.Errorf("engine: UnlessCost$ %q: UnlessUpTo$ not resolvable yet", text)
	}
	def, modifier, hasModifier := strings.Cut(rest, "_")
	cards, err := definedCards(host, def, a.refs())
	if err != nil {
		return "", fmt.Errorf("engine: UnlessCost$ %q: %w", text, err)
	}
	if len(cards) == 0 {
		return "", errUnlessNoCost
	}
	mc := g.Card(cards[0]).Def.Faces[0].ManaCost
	if mc.IsNoCost() || mc.CountX() > 0 {
		return "", fmt.Errorf("engine: UnlessCost$ %q: the card's mana cost is not a plain cost", text)
	}
	generic := mc.Generic()
	if hasModifier {
		switch {
		case strings.HasPrefix(modifier, "Minus"):
			n, convErr := strconv.Atoi(strings.TrimPrefix(modifier, "Minus"))
			if convErr != nil {
				return "", fmt.Errorf("engine: UnlessCost$ %q: %w", text, convErr)
			}
			if upTo {
				n = controller.ChooseNumber(g, a.Controller, a.Source, 0, n)
			}
			generic = max(generic-n, 0)
		case strings.HasPrefix(modifier, "Plus"):
			n, convErr := strconv.Atoi(strings.TrimPrefix(modifier, "Plus"))
			if convErr != nil {
				return "", fmt.Errorf("engine: UnlessCost$ %q: %w", text, convErr)
			}
			generic += n
		default:
			return "", fmt.Errorf("engine: UnlessCost$ %q: modifier %q not resolvable", text, modifier)
		}
	}
	out := mana.FromShards(mc.Shards(), generic).String()
	return strings.NewReplacer("}{", " ", "{", "", "}", "").Replace(out), nil
}

// expandUnlessCost is AbilityUtils.calculateUnlessCost's SVar handling and the
// X of a cost part's amount, evaluated for the ability that asks for the cost:
//
//   - a cost that is only the name of an SVar (Y, Z; not "X") is that many
//     mana, generic or of the UnlessColor$ color;
//   - the mana symbol X is the amount of SVar X (PlaySpellAbility.payManaCost
//     reads the SVar named X, or XAlternative$);
//   - a part whose amount is a name (PayLife<X>, PayEnergy<X/...>,
//     DamageYou<X>) takes that SVar's amount.
//
// An SVar this port cannot evaluate is an error, never a free payment (GO-7).
// Text with none of these shapes is returned as written.
func (g *Game) expandUnlessCost(a *Ability, text string, controller PlayerController) (string, error) {
	text = strings.TrimSpace(text)
	host := g.Card(a.Source)
	if rest, ok := strings.CutPrefix(text, "DefinedCost_"); ok {
		return g.definedUnlessCost(a, host, text, rest, controller)
	}
	amount := func(name string) (int, error) {
		n, ok := resolveNamedAmount(g, a.Amounts, host, name)
		if !ok {
			return 0, fmt.Errorf("engine: UnlessCost$ %q: SVar %s not resolvable yet", text, name)
		}
		return max(n, 0), nil
	}
	if _, isSVar := a.Amounts[strings.ToLower(text)]; isSVar && text != "X" {
		n, err := amount(text)
		if err != nil {
			return "", err
		}
		color := "1"
		if c, ok := a.Params.Param("UnlessColor"); ok {
			color = c
		}
		return unlessManaOf(n, color)
	}
	var out strings.Builder
	depth, start := 0, 0
	flush := func(end int) error {
		seg := text[start:end]
		start = end
		// Outside <...> a segment is a space-separated token list.
		if depth == 0 {
			words := strings.Fields(seg)
			for i, w := range words {
				if w == "X" {
					n, err := amount("X")
					if err != nil {
						return err
					}
					words[i] = strconv.Itoa(n)
				}
			}
			out.WriteString(strings.Join(words, " "))
			if strings.HasSuffix(seg, " ") {
				out.WriteByte(' ')
			}
			return nil
		}
		out.WriteString(seg)
		return nil
	}
	for i := 0; i < len(text); i++ {
		switch text[i] {
		case '<':
			if depth == 0 {
				// Part name up to and including '<' is outside; the body follows.
				if err := flush(i + 1); err != nil {
					return "", err
				}
			}
			depth++
		case '>':
			depth--
			if depth == 0 {
				body := text[start:i]
				field0, rest, hasRest := strings.Cut(body, "/")
				if _, err := strconv.Atoi(field0); err != nil && field0 != "" {
					if _, isSVar := a.Amounts[strings.ToLower(field0)]; isSVar || field0 == "X" {
						n, err := amount(field0)
						if err != nil {
							return "", err
						}
						field0 = strconv.Itoa(n)
					}
				}
				out.WriteString(field0)
				if hasRest {
					out.WriteString("/" + rest)
				}
				start = i
			}
		}
	}
	depth = 0
	if err := flush(len(text)); err != nil {
		return "", err
	}
	return strings.TrimSpace(out.String()), nil
}

// unlessManaOf is n mana of color (UnlessColor$, a color name or letter; "1" is
// generic) as cost text.
func unlessManaOf(n int, color string) (string, error) {
	symbol := ""
	switch strings.ToLower(color) {
	case "1", "colorless":
		return strconv.Itoa(n), nil
	case "w", "white":
		symbol = "W"
	case "u", "blue":
		symbol = "U"
	case "b", "black":
		symbol = "B"
	case "r", "red":
		symbol = "R"
	case "g", "green":
		symbol = "G"
	default:
		return "", fmt.Errorf("engine: UnlessColor$ %q not resolvable", color)
	}
	if n == 0 {
		return "0", nil
	}
	return strings.TrimSpace(strings.Repeat(symbol+" ", n)), nil
}

// costNamesX reports whether a Cost$ text has a non-mana part whose amount is
// X (Cost.hasXInAnyCostPart, Cost.java:1101-1111, for the parts past mana).
func costNamesX(text string) bool {
	for _, p := range cost.Parse(text).Parts {
		if p.Field(0) == "X" {
			return true
		}
	}
	return false
}

// nonManaXMax is the largest X the Cost$ text's non-mana parts can pay for
// pid: Cost.getMaxForNonManaX (Cost.java:1118-1131), the minimum over the
// parts of what each can still supply (hand size for Discard<X>, creatures to
// sacrifice for Sac<X>, life for PayLife<X>). It asks unlessPayable for X = 1,
// 2, ... until the cost cannot be paid, so each part's own availability rules
// (a card in hand other than the source, a legal sacrifice) apply unchanged.
// Mana is left out: Java's cap does not count it either. A text with a part
// the cost model does not know has no maximum (0).
func (g *Game) nonManaXMax(pid PlayerID, source CardID, text string) int {
	const bound = 200
	best := 0
	for n := 1; n <= bound; n++ {
		uc, ok := parseUnlessCostX(text, n, true)
		if !ok {
			return 0
		}
		uc.hasMana = false
		if !g.unlessPayable(pid, source, uc) {
			break
		}
		best = n
	}
	return best
}

// exileGraveOptions are the graveyard cards an ExileFromGrave part of uc may
// exile: the last creature alone for TopGraveyardCreature, else every card of
// the spec.
func (g *Game) exileGraveOptions(pid PlayerID, source CardID, uc unlessCost) []CardID {
	options := g.costCandidates(pid, source, false, Graveyard, uc.exileGraveSpec)
	if uc.exileGraveTop && len(options) > 0 {
		return options[len(options)-1:]
	}
	return options
}
