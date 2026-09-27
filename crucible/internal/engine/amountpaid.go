// Count$Valid family amounts: how many cards a valid string matches across
// one or more zones, or -- when the valid string carries a `$`-suffixed
// property (Tarmogoyf's own `Count$ValidGraveyard Card$CardTypes`) -- a
// measurement over the matched cards instead of their count: the handlePaid
// subset this port can evaluate.
//
// Ported from forge-game/src/main/java/forge/game/ability/AbilityUtils.java's
// xCount Valid branch (:1942, :2869) and handlePaid (:3675).

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// validFamilyValue is xCount's own Valid branch: the cards count.Valid
// matches in the zones count.Head names (validCountZones, amount.go), then
// either their number or, when count.DistinctProperty is set, handlePaid
// over them (paidMeasure). The zone list is every player's own zone of that
// type, the identical game.getCardsIn(zones) Java walks; the valid string's
// own YouCtrl/YouOwn/OppOwn properties do the narrowing.
func validFamilyValue(g *Game, sourceController PlayerID, source CardID, count expr.Count) (int, bool) {
	zones, ok := validCountZones(count.Head)
	if !ok {
		return 0, false
	}
	if count.DistinctProperty == "" {
		return countValid(g, zones, count.Valid, sourceController, source), true
	}
	measure, ok := paidMeasure(count.DistinctProperty)
	if !ok {
		return 0, false
	}
	var cards []CardID
	for _, z := range zones {
		for _, pid := range g.Players() {
			for _, id := range g.Zone(z, pid).Cards() {
				if Matches(g, g.Card(id), count.Valid, sourceController, source) {
					cards = append(cards, id)
				}
			}
		}
	}
	// handlePaid's own first line: an empty list is 0 whatever the
	// property -- before any property-specific branch runs.
	if len(cards) == 0 {
		return 0, true
	}
	return measure(g, cards), true
}

// paidMeasure is handlePaid's own dispatch on def, for the properties this
// port can measure, checked in handlePaid's own order so a name one earlier
// branch would catch never reaches a later one:
//
//   - Amount...: how many cards.
//   - Colors: how many colors among the cards, together
//     (CardUtil.getColorsFromCards).
//   - CardTypes/CardTypesPermanent...: distinct core types among the cards
//     (countCardTypesFromList), permanent types only for the second.
//   - Greatest/Least/Different<per-card>, or a bare <per-card>: the max, min,
//     distinct-value count or sum of a per-card xCount (perCardMeasure).
//
// false for everything else: DifferentCardNames (CardLists.
// getDifferentNamesCount's own Spy Kit and no-name rules, not ported),
// CreatureType/PlaneswalkerType/LandType/AllTypes (each needs the subtype
// vocabulary -- a *cardtype.Registry -- to tell a creature type from any
// other subtype), DifferentColorPair, TapPowerValue and a Valid-filtered
// sub-list. A property this does not recognize must not fall back to a
// plain match count: that would be a real but wrong number (GO-7).
func paidMeasure(def string) (func(*Game, []CardID) int, bool) {
	switch {
	case strings.HasPrefix(def, "Amount"):
		return func(_ *Game, cards []CardID) int { return len(cards) }, true
	case strings.HasPrefix(def, "TapPowerValue"), strings.HasPrefix(def, "DifferentCardNames"),
		def == "DifferentColorPair", strings.HasPrefix(def, "Valid"), strings.HasPrefix(def, "AllTypes"):
		return nil, false
	case def == "Colors":
		return func(g *Game, cards []CardID) int {
			var colors mana.Colors
			for _, id := range cards {
				colors |= g.Card(id).Colors()
			}
			return colors.Count()
		}, true
	case strings.HasPrefix(def, "CardTypes"):
		permanent := strings.HasPrefix(def, "CardTypesPermanent")
		return func(g *Game, cards []CardID) int {
			var seen cardtype.Line
			for _, id := range cards {
				seen = seen.Union(g.Card(id).Type())
			}
			n := 0
			for _, t := range seen.CoreTypes() {
				if !permanent || t.IsPermanent() {
					n++
				}
			}
			return n
		}, true
	case strings.HasPrefix(def, "CreatureType"), strings.HasPrefix(def, "PlaneswalkerType"),
		strings.HasPrefix(def, "LandType"):
		return nil, false
	}

	fold, perCard := sumFold, def
	switch {
	case strings.HasPrefix(def, "Least"):
		fold, perCard = minFold, strings.TrimPrefix(def, "Least")
	case strings.HasPrefix(def, "Greatest"):
		fold, perCard = maxFold, strings.TrimPrefix(def, "Greatest")
	case strings.HasPrefix(def, "Different"):
		fold, perCard = distinctFold, strings.TrimPrefix(def, "Different")
	}
	measure, ok := perCardMeasure(perCard)
	if !ok {
		return nil, false
	}
	return func(g *Game, cards []CardID) int {
		values := make([]int, len(cards))
		for i, id := range cards {
			values[i] = measure(g.Card(id))
		}
		return fold(values)
	}, true
}

// perCardMeasure is the per-card xCount handlePaid's own fallthrough calls
// for each matched card, for the heads whose value is fixed while a
// continuous effect is being folded: CardManaCost (Card.getCMC, the printed
// mana value, Card.CMC here) and CardCounters.<TYPE>|ALL (counters on that
// card). CardPower/CardToughness are refused on purpose: another
// permanent's power is itself mid-rebuild while Layer 7 is being applied
// (applyContinuousPT, continuous.go), so reading it there would see a
// half-folded value rather than the real one.
func perCardMeasure(head string) (func(*Card) int, bool) {
	if head == "CardManaCost" {
		return func(c *Card) int { return c.CMC() }, true
	}
	if kind, ok := strings.CutPrefix(head, "CardCounters."); ok && kind != "" && !strings.Contains(kind, ".") {
		if kind == "ALL" {
			return func(c *Card) int { return c.Counters.Total() }, true
		}
		ct := CounterType(strings.ToUpper(kind))
		return func(c *Card) int { return c.Counters.Count(ct) }, true
	}
	return nil, false
}

func sumFold(values []int) int {
	n := 0
	for _, v := range values {
		n += v
	}
	return n
}

func minFold(values []int) int {
	n := values[0]
	for _, v := range values[1:] {
		n = min(n, v)
	}
	return n
}

func maxFold(values []int) int {
	n := values[0]
	for _, v := range values[1:] {
		n = max(n, v)
	}
	return n
}

// distinctFold is IntStream.distinct().count().
func distinctFold(values []int) int {
	seen := make(map[int]struct{}, len(values))
	for _, v := range values {
		seen[v] = struct{}{}
	}
	return len(seen)
}
