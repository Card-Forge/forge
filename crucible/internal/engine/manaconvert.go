// Mode$ ManaConvert (StaticAbilityManaConvert.manaConvert) for casting a
// spell: a static that lets its player spend mana as though it were mana of
// any color ("AnyType->AnyColor") or of any type ("AnyType->AnyType") on the
// spells its ValidCard$ and ValidSA$ name. Java builds a ManaConversionMatrix
// the payment reads; here the same effect rewrites the cost the way a
// MayPlayIgnoreColor$/MayPlayIgnoreType$ grant does (anyColorCost,
// anyTypeCost), so the payment code stays one path.
//
// Not read: the per-color conversions (White->Red, C->AnyColor, ...), the
// Optional$ choice and activated-ability costs; a line carrying one of those is
// skipped and the card pays as printed (game-state.md, "Not ported yet").

//enginelint:allow ability id zone card game valid player continuous staticability zonemove cantbecast saspec castspell castoptions

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// manaConvertParams are the params a ManaConvert line may carry that this
// port evaluates; Optional$, AffectedZone$ and the rest make the line unread.
var manaConvertParams = map[string]bool{
	"mode": true, "validplayer": true, "validcard": true, "validsa": true, "manaconversion": true,
	"effectzone": true, "condition": true, "description": true, "secondary": true,
}

// manaConversionKind is what a ManaConversion$ value does to a spell's cost:
// anyColor and anyType are the two shapes read, none anything else.
type manaConversionKind uint8

const (
	conversionNone manaConversionKind = iota
	conversionAnyColor
	conversionAnyType
)

// readManaConversion is the cost rewrite s's ManaConversion$ asks for.
func readManaConversion(s *compile.Ability) manaConversionKind {
	v, _ := s.Param("ManaConversion")
	switch strings.TrimSpace(v) {
	case "AnyType->AnyColor":
		return conversionAnyColor
	case "AnyType->AnyType":
		return conversionAnyType
	}
	return conversionNone
}

// manaConvertSupported reports whether a static line is a ManaConvert this
// port reads for a spell's cost: only evaluated params and a conversion
// readManaConversion knows. animate.go grants only such lines, so a grant is
// never inert.
func manaConvertSupported(s *compile.Ability) bool {
	if readManaConversion(s) == conversionNone {
		return false
	}
	for _, p := range s.Params {
		if !manaConvertParams[strings.ToLower(p.Key)] {
			return false
		}
	}
	return true
}

// manaConvertForSpell is the ManaConvert statics' verdict on pid casting card:
// mana of any type pays the whole cost (anyType), or a colored part
// (anyColor). AnyType wins when both apply.
func (g *Game) manaConvertForSpell(pid PlayerID, card CardID) (anyType, anyColor bool) {
	c := g.Card(card)
	spell := &Ability{Source: card, Controller: pid, spell: true}
	for _, host := range g.staticHostsWith(card) {
		h := g.Card(host)
		if h.Def == nil {
			continue
		}
		for _, face := range h.traitFaces() {
			for _, s := range face.Statics {
				if !strings.EqualFold(s.Name, "ManaConvert") || !manaConvertSupported(s) ||
					!g.manaConvertApplies(pid, c, h, s, spell) {
					continue
				}
				if readManaConversion(s) == conversionAnyType {
					anyType = true
				} else {
					anyColor = true
				}
			}
		}
	}
	return anyType, anyColor
}

// manaConvertApplies is StaticAbilityManaConvert.checkManaConvert for a spell:
// ValidPlayer$, ValidCard$ and ValidSA$ each match when absent, and the line's
// Condition$ holds. A spec this port cannot recognize leaves the line unread.
func (g *Game) manaConvertApplies(pid PlayerID, c, host *Card, s *compile.Ability, spell *Ability) bool {
	if !g.staticConditionsMet(host, s) {
		return false
	}
	if spec, ok := s.Param("ValidPlayer"); ok {
		matched, recognized := matchesPlayerSpec(g, pid, host.Controller(), host.ID, spec)
		if !recognized || !matched {
			return false
		}
	}
	if spec, ok := s.Param("ValidCard"); ok && !Matches(g, c, valid.Parse(spec), host.Controller(), host.ID) {
		return false
	}
	if spec, ok := s.Param("ValidSA"); ok {
		matched, recognized := g.spellAbilityMatches(spell, spec, host, host.Controller(), host.abilityAmounts(s))
		return recognized && matched
	}
	return true
}
