// A card's own Mode$ AlternativeCost static (CR 118.9): "you may pay <cost>
// rather than pay this spell's mana cost" -- the Bringers, Fireblast, Daze,
// Force of Will. Java builds one more SpellAbility per static whose cost
// replaces the mana cost (StaticAbilityAlternativeCost.alternativeCosts,
// copyWithManaCostReplaced), and marks it OptionalCost.AltCost when the
// static is the card's own (:82-87), which Count$AltCost reads
// (AbilityUtils.java:2064-2066).
//
// Ported here for the self-hosted spell shape: ValidSA$ Spell.Self (or
// ValidSA$ Spell with ValidCard$ Card.Self), cast from the hand, conditions
// read as any static's are (staticConditionsMet), the cost one
// parseUnlessCost reads -- its mana in place of the mana cost, its other
// parts paid with it, as Escape's are. A line naming a param this port does
// not read (XAlternative$, Announce$, ManaRestriction$, Named$, CostDesc$, a
// ConvertedManaCost cost) is not offered, never cast at the wrong price
// (GO-7). Statics granting an alternative cost to other spells or to
// activated abilities (Fist of Suns, Jodah, the equip and cycling ones) are
// not ported.

package engine

//enginelint:allow id zone card game ability control staticability unlesscost castoptions

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// altCostParams are the params a self-hosted Mode$ AlternativeCost line may
// carry here: its shape, its cost and the conditions staticConditionsMet
// reads.
var altCostParams = map[string]bool{
	"mode": true, "validsa": true, "validcard": true, "validplayer": true, "effectzone": true, "cost": true,
	"description": true, "stackdescription": true, "secondary": true, "condition": true,
	"ispresent": true, "presentcompare": true, "presentzone": true, "presentplayer": true, "presentdefined": true,
	"ispresent2": true, "presentcompare2": true, "presentzone2": true, "presentplayer2": true, "presentdefined2": true,
	"checksvar": true, "svarcompare": true,
}

// isSelfSpellAltCost reports whether s is an AlternativeCost line for the
// spell of its own host, cast by its controller.
func isSelfSpellAltCost(s *compile.Ability) bool {
	if !strings.EqualFold(s.Name, "AlternativeCost") {
		return false
	}
	sa, _ := s.Param("ValidSA")
	card, hasCard := s.Param("ValidCard")
	selfSpell := sa == "Spell.Self" && !hasCard
	spellOfSelf := sa == "Spell" && card == "Card.Self"
	if !selfSpell && !spellOfSelf {
		return false
	}
	player, hasPlayer := s.Param("ValidPlayer")
	return !hasPlayer || player == "You"
}

// altCostOptions are the ways c's own AlternativeCost statics let pid cast it
// from the hand, in script order: each line whose conditions hold and whose
// cost pid can pay past its mana.
func (g *Game) altCostOptions(pid PlayerID, c *Card) []castOption {
	if c.Def == nil || c.Controller() != pid {
		return nil
	}
	var out []castOption
	for _, face := range c.traitFaces() {
		for _, s := range face.Statics {
			if !isSelfSpellAltCost(s) || !paramsResolvable(s, altCostParams) || !g.staticConditionsMet(c, s) {
				continue
			}
			text, _ := s.Param("Cost")
			if strings.Contains(text, "ConvertedManaCost") {
				continue
			}
			uc, ok := parseUnlessCost(text)
			if !ok {
				continue
			}
			alt := mana.MustParse("0")
			if uc.hasMana {
				alt = uc.mana
			}
			uc.hasMana, uc.mana, uc.manaTokens = false, mana.Cost{}, nil
			o := castOption{hasAlt: true, alt: alt, altStatic: true}
			if hasNonManaParts(uc) {
				if !g.unlessPayable(pid, c.ID, uc) {
					continue
				}
				o.altExtra, o.hasAltExtra = uc, true
			}
			o.altText = text
			out = append(out, o)
		}
	}
	return out
}

// hasNonManaParts reports whether uc, its mana taken out, still asks for
// something.
func hasNonManaParts(uc unlessCost) bool {
	return len(uc.parsed.Parts) > 0
}
