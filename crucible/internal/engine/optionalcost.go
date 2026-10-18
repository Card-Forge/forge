// Optional additional costs a spell is cast with, past Kicker: Mode$
// OptionalCost statics and the Teamwork keyword. Java builds one list for the
// spell being cast (GameActionUtil.getOptionalCostValues, :409-535), the
// caster picks which to pay (PlayerController.chooseOptionalCosts,
// PlaySpellAbility.chooseOptionalAdditionalCosts :140-154), and each one
// picked joins the spell's cost (GameActionUtil.addOptionalCosts, :537-569) and
// is recorded on the cast SpellAbility as an OptionalCost kind.

//enginelint:allow id zone card game control valid player staticability cantbecast saspec unlesscost taptype ability chosencosts

package engine

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/cost"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// optionalCostParams are the params a Mode$ OptionalCost line may carry that
// getOptionalCostValues reads; a line naming another is refused (GO-7).
var optionalCostParams = map[string]bool{
	"mode": true, "effectzone": true, "validcard": true, "validsa": true, "activator": true,
	"cost": true, "reducecolor": true, "description": true,
}

// optionalCostOption is one entry of getOptionalCostValues' list: the cost, the
// kind it records, and the color a ReduceColor$ line reduces.
type optionalCostOption struct {
	// text is the cost as written, what ConfirmPayCost shows.
	text string
	uc   unlessCost
	// kind is optGeneric for a line without ReduceColor$, optTeamwork for the
	// keyword, 0 for a ReduceColor$ line (ReduceW..ReduceG, which no reader
	// asks about: they only reduce the spell's cost).
	kind optionalKinds
	// reduce is the ReduceColor$ shard (OptionalCost.getPip), for none.
	reduce    mana.Shard
	hasReduce bool
	// teamworkN is Teamwork<N>'s total power (CostTeamwork), 0 for any other.
	teamworkN int
}

// optionalChoice is what the optional costs a cast pays add to it: kinds for
// Card.castOpt, mana and Waterbend for the mana payment, the shards ReduceColor$
// lines take off (SpellAbility.getPipsToReduce), and the parts paid once the
// mana is.
type optionalChoice struct {
	kinds      optionalKinds
	mana       mana.Cost
	hasMana    bool
	waterbendN int
	reduce     []mana.Shard
	parts      []unlessCost
	teamworkN  int
}

// optionalCostOptions is getOptionalCostValues for pid casting card: every
// Mode$ OptionalCost line of the card itself or of a static-ability host
// (staticHostsWith) whose conditions hold and whose ValidCard$, ValidSA$ and
// Activator$ match, then the card's Teamwork keyword. A card on the
// battlefield is never cast, so it has none (GameActionUtil.java:420). A line
// that applies but carries a param or a Cost$ shape this port cannot pay is an
// error: the spell is not cast rather than cast without the choice (GO-7).
func (g *Game) optionalCostOptions(pid PlayerID, card CardID) ([]optionalCostOption, error) {
	c := g.Card(card)
	if c.Zone == Battlefield || c.Def == nil {
		return nil, nil
	}
	var out []optionalCostOption
	for _, host := range g.staticHostsWith(card) {
		h := g.Card(host)
		if h.Def == nil {
			continue
		}
		for _, face := range h.traitFaces() {
			for _, s := range face.Statics {
				if !strings.EqualFold(s.Name, "OptionalCost") || !g.staticConditionsMet(h, s) {
					continue
				}
				if v, ok := s.Param("ValidCard"); ok && !Matches(g, c, valid.Parse(v), h.Controller(), h.ID) {
					continue
				}
				if v, ok := s.Param("ValidSA"); ok {
					sa := &Ability{Source: card, Controller: pid, spell: true}
					matched, recognized := g.spellAbilityMatches(sa, v, h, h.Controller(), h.abilityAmounts(s))
					if !recognized {
						return nil, fmt.Errorf("engine: %q: OptionalCost ValidSA$ %q not resolvable yet", h.Def.Name, v)
					}
					if !matched {
						continue
					}
				}
				if v, ok := s.Param("Activator"); ok {
					matched, recognized := matchesPlayerSpec(g, pid, h.Controller(), h.ID, v)
					if !recognized {
						return nil, fmt.Errorf("engine: %q: OptionalCost Activator$ %q not resolvable yet", h.Def.Name, v)
					}
					if !matched {
						continue
					}
				}
				o, ok, err := optionalStaticOption(h, s.Param)
				if err != nil {
					return nil, err
				}
				if ok {
					if !paramsResolvable(s, optionalCostParams) {
						return nil, fmt.Errorf("engine: %q: OptionalCost: a param not resolvable yet", h.Def.Name)
					}
					out = append(out, o)
				}
			}
		}
	}
	for _, line := range c.KeywordLines() {
		if line == "Bargain" {
			// GameActionUtil.java:479-481: sacrifice an artifact, an enchantment
			// or a token as the spell is cast (OptionalCost.Bargain, CR 702.166).
			uc, ok := parseUnlessCost(bargainCost)
			if !ok {
				return nil, fmt.Errorf("engine: %q: Bargain cost %q not resolvable yet", c.Def.Name, bargainCost)
			}
			out = append(out, optionalCostOption{text: bargainCost, uc: uc, kind: optBargain})
			continue
		}
		k := keyword.Parse(line)
		if k.Name != "Teamwork" {
			continue
		}
		n, err := strconv.Atoi(k.Details)
		if err != nil || n <= 0 {
			return nil, fmt.Errorf("engine: %q: Teamwork %q not resolvable yet", c.Def.Name, k.Details)
		}
		out = append(out, optionalCostOption{text: "Teamwork<" + k.Details + ">", kind: optTeamwork, teamworkN: n})
	}
	return out, nil
}

// bargainCost is the Bargain keyword's optional cost, as GameActionUtil.java:480
// writes it.
const bargainCost = "Sac<1/Artifact;Enchantment;Card.token/artifact, enchantment or token>"

// optionalStaticOption reads one applying line's Cost$ and ReduceColor$. ok is
// false for a ReduceColor$ other than the five colors, which Java's if-chain
// adds nothing for (GameActionUtil.java:459-470).
func optionalStaticOption(h *Card, param func(string) (string, bool)) (optionalCostOption, bool, error) {
	text, ok := param("Cost")
	if !ok {
		return optionalCostOption{}, false, fmt.Errorf("engine: %q: OptionalCost without Cost$", h.Def.Name)
	}
	uc, ok := parseUnlessCost(text)
	if !ok {
		return optionalCostOption{}, false, fmt.Errorf("engine: %q: OptionalCost Cost$ %q not resolvable yet", h.Def.Name, text)
	}
	// A part that exiles marks the cards as exiled with the spell
	// (putSpellOnStack), and the permanent keeps the link (relinkExiledWith,
	// CR 607.2q): Redemptor Dreadnought's attack trigger reads it.
	o := optionalCostOption{text: text, uc: uc, kind: optGeneric}
	color, ok := param("ReduceColor")
	if !ok {
		return o, true, nil
	}
	shard, err := mana.ParseShard(color)
	if err != nil || shard.Colors().Count() != 1 || shard.IsPhyrexian() || shard.IsOr2Generic() {
		return optionalCostOption{}, false, nil
	}
	o.kind, o.reduce, o.hasReduce = 0, shard, true
	return o, true, nil
}

// castsWithFlashOptional is the second half of SpellAbility.canPlay
// (checkOptionalCosts) (SpellAbility.java:467-491) for timing: card cannot be
// cast now, but could be with one of its optional costs paid -- Quantum
// Reduction's CastWithFlash ValidSA$ Spell.Teamwork. Each option pid can pay
// is tried on its own (canPlayWithOptionalCost). The cast then begins, and
// once the costs are chosen PlaySpellAbility.java:107 asks canPlay again
// (castsWithFlashChosen through flashDecided): a cast without that cost fails.
func (g *Game) castsWithFlashOptional(pid PlayerID, card CardID) bool {
	options, err := g.optionalCostOptions(pid, card)
	if err != nil {
		return false
	}
	for _, o := range options {
		if o.kind == 0 || !g.optionalOptionPayable(pid, card, o) {
			continue
		}
		probe := &Ability{Source: card, Controller: pid, spell: true, probing: true, probeTeamwork: o.kind&optTeamwork != 0}
		if g.flashStatic(pid, card, probe, flashPlain) {
			return true
		}
	}
	return false
}

// chooseOptionalCosts is PlaySpellAbility.chooseOptionalAdditionalCosts for
// pid casting card: each option whose non-mana part pid can pay is offered
// through ConfirmPayCost, the question Kicker asks, in list order. A mana part
// is not checked here: like a kicker, a cost picked but not paid fails the
// cast (CostPayment).
func (g *Game) chooseOptionalCosts(controller PlayerController, pid PlayerID, card CardID) (optionalChoice, error) {
	options, err := g.optionalCostOptions(pid, card)
	if err != nil {
		return optionalChoice{}, err
	}
	var choice optionalChoice
	for _, o := range options {
		if !g.optionalOptionPayable(pid, card, o) || !controller.ConfirmPayCost(g, pid, cost.Parse(o.text), card) {
			continue
		}
		choice.add(o)
	}
	return choice, nil
}

// add records o as paid: GameActionUtil.addOptionalCosts' getPayCosts().add and
// addOptionalCost, which notes the pip a ReduceColor$ line reduces.
func (choice *optionalChoice) add(o optionalCostOption) {
	choice.kinds |= o.kind
	if o.hasReduce {
		choice.reduce = append(choice.reduce, o.reduce)
	}
	if o.teamworkN > 0 {
		choice.teamworkN = max(choice.teamworkN, o.teamworkN)
		return
	}
	if o.uc.hasMana {
		if choice.hasMana {
			choice.mana = mana.FromShards(append(append([]mana.Shard(nil), choice.mana.Shards()...), o.uc.mana.Shards()...),
				choice.mana.Generic()+o.uc.mana.Generic())
		} else {
			choice.mana, choice.hasMana = o.uc.mana, true
		}
		choice.waterbendN += o.uc.waterbendN
	}
	rest := o.uc
	rest.hasMana, rest.mana, rest.manaTokens, rest.waterbendN = false, mana.Cost{}, nil, 0
	if rest.hasParts() {
		choice.parts = append(choice.parts, rest)
	}
}

// hasParts reports whether uc names anything beyond mana.
func (uc unlessCost) hasParts() bool {
	return uc.lifeN > 0 || uc.energyN > 0 || uc.discardN > 0 || uc.discardHand || uc.sacN > 0 || uc.damageN > 0 ||
		uc.drawN > 0 || uc.returnN > 0 || uc.revealN > 0 || uc.addCounterN > 0 || uc.exileGraveN > 0 || uc.tapN > 0 ||
		uc.evidenceN > 0 || uc.youCounterN > 0 || uc.libN > 0 || uc.removeAnyN > 0 || uc.subCounterN > 0 ||
		uc.discardTypeN > 0 || uc.movedToGraveN > 0 || uc.forage || uc.exileHandAll || uc.exileEitherN > 0 ||
		uc.chooseCreatureType
}

// optionalOptionPayable is CostPart.canPay for o's non-mana part.
func (g *Game) optionalOptionPayable(pid PlayerID, card CardID, o optionalCostOption) bool {
	if o.teamworkN > 0 {
		return totalPower(g, g.teamworkCandidates(pid)) >= o.teamworkN
	}
	return g.unlessPayable(pid, card, o.uc)
}

// teamworkCandidates is what CostTeamwork may tap: pid's untapped creatures
// (CostTapType over Creature.YouCtrl; a tap cost that is not {T} lets a
// creature that came under control this turn tap too).
func (g *Game) teamworkCandidates(pid PlayerID) []CardID {
	var out []CardID
	for _, id := range g.Zone(Battlefield, pid).Cards() {
		c := g.Card(id)
		if !c.Tapped && c.Type().Has(cardtype.Creature) {
			out = append(out, id)
		}
	}
	return out
}

// optionalWithMana is base plus the mana the chosen optional costs add.
func (choice optionalChoice) withMana(base mana.Cost) mana.Cost {
	if !choice.hasMana || base.IsNoCost() {
		return base
	}
	return mana.FromShards(append(append([]mana.Shard(nil), base.Shards()...), choice.mana.Shards()...),
		base.Generic()+choice.mana.Generic())
}

// reduced is total less one shard per ReduceColor$ line paid, after the
// ReduceCost statics (CostAdjustment.java:238-242,
// ManaCostBeingPaid.decreaseShard): a shard of that color alone first, else a
// two-color hybrid holding it, else a {2/C}, else a colorless hybrid, else a
// Phyrexian one. The generic part is never touched: "this effect reduces only
// the amount of <color> mana you pay".
func (choice optionalChoice) reduced(total mana.Cost) mana.Cost {
	if len(choice.reduce) == 0 || total.IsNoCost() {
		return total
	}
	shards := append([]mana.Shard(nil), total.Shards()...)
	for _, pip := range choice.reduce {
		if i := pipToReduce(shards, pip); i >= 0 {
			shards = append(shards[:i], shards[i+1:]...)
		}
	}
	return mana.FromShards(shards, total.Generic())
}

// pipToReduce is the index of the shard decreaseShard(pip, 1) takes from
// shards, -1 for none.
func pipToReduce(shards []mana.Shard, pip mana.Shard) int {
	color := pip.Colors()
	tests := []func(s mana.Shard) bool{
		func(s mana.Shard) bool { return s == pip },
		func(s mana.Shard) bool { return s.Colors().Count() > 1 && s.Colors().Has(color) },
		func(s mana.Shard) bool { return s.IsOr2Generic() && s.Colors().Has(color) },
		func(s mana.Shard) bool { return s.IsColorless() && s.Colors().Has(color) },
		func(s mana.Shard) bool { return s.IsPhyrexian() && s.Colors().Has(color) },
	}
	for _, test := range tests {
		for i, s := range shards {
			if !s.IsX() && test(s) {
				return i
			}
		}
	}
	return -1
}

// optionalPartsPayable reports whether every non-mana part chosen can still be
// paid, checked before the mana is.
func (g *Game) optionalPartsPayable(pid PlayerID, card CardID, choice optionalChoice) bool {
	for _, uc := range choice.parts {
		if !g.unlessPayable(pid, card, uc) {
			return false
		}
	}
	return choice.teamworkN == 0 || totalPower(g, g.teamworkCandidates(pid)) >= choice.teamworkN
}

// payOptionalParts pays the chosen non-mana parts once the mana is paid, in
// the order they were chosen, and records what the cast SpellAbility's paid
// lists hold on the card: the cards collected as evidence ("Collected") and
// the cards revealed or beheld ("Revealed", which Count$Presence_ and
// Revealed$ read). Teamwork taps creatures with the total power it names; an
// answer that is not a subset of the candidates, repeats one, or falls short
// fails the cast with a pending error, as Sneak's return does.
func (g *Game) payOptionalParts(controller PlayerController, pid PlayerID, card CardID, choice optionalChoice) bool {
	a := &Ability{Source: card, Controller: pid, spell: true}
	for _, uc := range choice.parts {
		evidence := uc.evidenceN
		uc.evidenceN = 0
		var paid paidLists
		// The type, the revealed cards and the rest (payChoiceParts records
		// the reveal on the spell): a type with too few cards to behold
		// declines the payment and the cast.
		if !g.payUnlessPartsInto(controller, a, pid, uc, &paid) {
			return false
		}
		if len(paid.exiled) > 0 {
			g.Card(card).castOpt.exiled = append(append([]CardID(nil), g.Card(card).castOpt.exiled...), paid.exiled...)
		}
		if evidence > 0 {
			collected := g.collectEvidence(controller, pid, card, evidence)
			g.Card(card).castOpt.collected = append(append([]CardID(nil), g.Card(card).castOpt.collected...), collected...)
		}
	}
	if choice.teamworkN > 0 {
		candidates := g.teamworkCandidates(pid)
		picks := controller.ChooseCardsForEffect(g, pid, card, candidates, 1, len(candidates))
		if !isSubset(picks, candidates) || hasDuplicate(picks) || totalPower(g, picks) < choice.teamworkN {
			g.recordPendingError(fmt.Errorf("engine: Teamwork: tapped creatures %v cannot pay %d power", picks, choice.teamworkN))
			return false
		}
		tapChosenPermanents(g, controller, picks)
	}
	return true
}

// revealCandidates is the cards a Reveal or Behold part may show: pid's hand,
// and for Behold pid's battlefield too (CostBehold), the spell itself left
// out (unlessRevealable's count).
func (g *Game) revealCandidates(pid PlayerID, card CardID, uc unlessCost) []CardID {
	spec := valid.Parse(strings.ReplaceAll(uc.revealSpec, ";", ","))
	zones := []ZoneType{Hand}
	if uc.revealBattlefield {
		zones = append(zones, Battlefield)
	}
	var out []CardID
	for _, z := range zones {
		for _, id := range g.Zone(z, pid).Cards() {
			if id != card && Matches(g, g.Card(id), spec, pid, card) {
				out = append(out, id)
			}
		}
	}
	return out
}

// waterbendCast is CostAdjustment.adjustCostByWaterbend for a cast with a
// Waterbend part: the caster may tap untapped artifacts and creatures, each
// paying {1} of the generic part of the whole cost, at most the Waterbend
// amount (CostAdjustment.java:329-348). It returns the mana still to pay and
// what to tap once that is paid.
func (g *Game) waterbendCast(controller PlayerController, pid PlayerID, card CardID, total mana.Cost, n int) (mana.Cost, []CardID) {
	if n == 0 || total.IsNoCost() {
		return total, nil
	}
	return g.waterbendReduce(controller, pid, card, unlessCost{mana: total, hasMana: true, waterbendN: n})
}
