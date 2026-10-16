// Emerge (CR 702.119): an alternative cost -- sacrifice a creature (or, with
// "Emerge from <type>", a permanent of that type) and pay the emerge cost
// reduced by the sacrificed permanent's mana value. Ported from
// CardFactoryUtil.java:2808-2821 (the alternative spell: the emerge cost in
// place of the mana cost, IsPresent$ <type>.YouCtrl+CanBeSacrificedBy),
// CostAdjustment.adjustCostByEmerge (CostAdjustment.java:404-422: the player
// picks the permanent as the cost is computed, its mana value comes off the
// generic part) and PlaySpellAbility.java:484-489, :548-553 (no pick cancels
// the cast; the permanent is sacrificed once the mana is paid, and the spell
// keeps it, as last known, for Count$Emerged and Emerged$).

package engine

//enginelint:allow id zone card game ability valid control staticability sacrificeeffect taptype amount castoptions

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// emergeCost is c's Emerge keyword: its mana cost and the valid string of what
// it sacrifices (Emerge.getValidType: Creature unless the keyword names a
// type). ok is false without the keyword, or for a cost that is not plain
// mana or names an X (not offered, never half-paid, GO-7).
func emergeCost(c *Card) (mc mana.Cost, validType string, ok bool) {
	for _, line := range c.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "Emerge" {
			continue
		}
		args := k.Args()
		if len(args) < 1 || args[0] == "" {
			return mana.Cost{}, "", false
		}
		mc, err := mana.Parse(args[0])
		if err != nil || mc.CountX() > 0 || mc.IsNoCost() {
			return mana.Cost{}, "", false
		}
		validType = "Creature"
		if len(args) > 1 && args[1] != "" {
			validType = args[1]
		}
		return mc, validType, true
	}
	return mana.Cost{}, "", false
}

// emergeCandidates is what pid may sacrifice to emerge card: their permanents
// of validType that a CantSacrifice static does not protect from a cost
// (CardPredicates.canBeSacrificedBy(sa, false)).
func (g *Game) emergeCandidates(pid PlayerID, card CardID, validType string) []CardID {
	spec := valid.Parse(validType)
	cause := &Ability{Source: card, Controller: pid, spell: true}
	var out []CardID
	for _, id := range g.Zone(Battlefield, pid).Cards() {
		c := g.Card(id)
		if c.IsPhasedOut() || !Matches(g, c, spec, pid, card) || g.cantSacrifice(c, false, cause) {
			continue
		}
		out = append(out, id)
	}
	return out
}

// emergeOption is the Emerge way to cast c from pid's hand, when the keyword
// is there and something can be sacrificed for it (the alternative spell's
// IsPresent$ restriction).
func (g *Game) emergeOption(pid PlayerID, c *Card) (castOption, bool) {
	mc, validType, ok := emergeCost(c)
	if !ok || len(g.emergeCandidates(pid, c.ID, validType)) == 0 {
		return castOption{}, false
	}
	return castOption{hasAlt: true, alt: mc, emerge: validType}, true
}

// chooseEmergeSacrifice asks pid which permanent to sacrifice for the emerge
// cast of card (choosePermanentsToSacrifice(sa, 0, 1, ...)). ok is false when
// nothing is left to sacrifice or the answer is not one of the candidates:
// with no pick Java cancels the cast (PlaySpellAbility.java:485-488).
func (g *Game) chooseEmergeSacrifice(controller PlayerController, pid PlayerID, card CardID, validType string) (CardID, bool) {
	candidates := g.emergeCandidates(pid, card, validType)
	if len(candidates) == 0 {
		return NoCard, false
	}
	picked := controller.ChoosePermanentsToSacrifice(g, pid, candidates, 1)
	if len(picked) != 1 || !isSubset(picked, candidates) {
		return NoCard, false
	}
	return picked[0], true
}

// reducedByEmerge is total with the emerged permanent's mana value taken off
// its generic part, never below zero (ManaCostBeingPaid.decreaseGenericMana).
func reducedByEmerge(g *Game, total mana.Cost, emerged CardID) mana.Cost {
	if emerged == NoCard {
		return total
	}
	return mana.FromShards(total.Shards(), max(total.Generic()-g.Card(emerged).CMC(), 0))
}

// payEmergeSacrifice sacrifices emerged as part of casting card, once the mana
// is paid, and records it on the spell (SpellAbility.setSacrificedAsEmerge
// with the last-known copy).
func (g *Game) payEmergeSacrifice(controller PlayerController, pid PlayerID, card, emerged CardID) {
	sacrificeCardsFor(g, controller, &Ability{Source: card, Controller: pid, spell: true}, []CardID{emerged}, false)
	g.Card(card).castOpt.emerged = emerged
}

// castEmerged is getCastSA() != null && getCastSA().isEmerge(): c was cast for
// its Emerge cost and still carries that cast.
func (c *Card) castEmerged() bool { return c.wasCast && c.castOpt.emerged != NoCard }

// emergedCards is Card.getEmerged (Card.java:1226-1231): the permanent c's
// cast sacrificed for Emerge, as it last was, or none.
func (g *Game) emergedCards(c *Card) []*Card {
	if !c.castEmerged() {
		return nil
	}
	return g.cardsOf([]CardID{c.castOpt.emerged}, true)
}

// emergeLabel is the cast option's text.
func emergeLabel(name string, alt mana.Cost, validType string) string {
	what := "a creature"
	if !strings.EqualFold(validType, "Creature") {
		what = "a " + strings.ToLower(validType)
	}
	return "Cast " + name + " for its emerge cost " + alt.String() + ", sacrificing " + what
}
