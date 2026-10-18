// Count$Valid family amounts and handlePaid: how many cards a valid string
// matches across one or more zones, or -- when the valid string carries a
// `$`-suffixed property (Tarmogoyf's own `Count$ValidGraveyard Card$CardTypes`)
// -- a measurement over the matched cards instead of their count; and the same
// measurement over any other card list an amount names (Remembered$,
// Sacrificed$, Targeted$, ...).
//
// Ported from forge-game/src/main/java/forge/game/ability/AbilityUtils.java's
// xCount Valid branch (:1951-1979, :2884-2894) and handlePaid (:3742-3851).

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// validFamilyValue is xCount's own Valid branch: the cards count.Valid
// matches in the zones count.Head names (validCountZones, amount.go), every
// card in the game for ValidAll (cardsInGame), or the
// source alone for ValidSelf (`lparts[0].endsWith("Self")`), then either their
// number or, when count.DistinctProperty is set, handlePaid over them. The
// zone list is every player's own zone of that type, the identical
// game.getCardsIn(zones) Java walks; the valid string's own YouCtrl/YouOwn/
// OppOwn properties do the narrowing. The operator is the caller's: xCount
// applies it once, to handlePaid's result.
func validFamilyValue(g *Game, sourceController PlayerID, source CardID, count expr.Count) (int, bool) {
	var pool []CardID
	switch {
	case strings.Contains(count.Head, "All"):
		// lparts[0].contains("All"): Game.getCardsInGame (Game.java:761-789).
		pool = g.cardsInGame()
	case strings.HasSuffix(count.Head, "Self"):
		if source == NoCard {
			return 0, false
		}
		pool = []CardID{source}
	default:
		zones, ok := validCountZones(count.Head)
		if !ok {
			return 0, false
		}
		if count.DistinctProperty == "" {
			return countValid(g, zones, count.Valid, sourceController, source), true
		}
		for _, z := range zones {
			for _, pid := range g.Players() {
				pool = append(pool, g.Zone(z, pid).Cards()...)
			}
		}
	}
	var cards []*Card
	for _, id := range pool {
		if Matches(g, g.Card(id), count.Valid, sourceController, source) {
			cards = append(cards, g.Card(id))
		}
	}
	if count.DistinctProperty == "" {
		return len(cards), true
	}
	return handlePaid(g, nil, sourceController, source, cards, count.DistinctProperty, nil, 0)
}

// handlePaid is AbilityUtils.handlePaid over cards: def the property, op the
// operator written after it (nil for none), applied where Java applies it --
// to the count for an empty list, Amount, a Valid filter's count, CardTypes and
// DifferentCardNames; to each card's own value for the per-card measures, so
// `Sacrificed$CardPower/Plus.1` over two cards adds two; and nowhere for the
// properties Java matches with equals, which an operator would therefore turn
// into a different property (refused). Checked in handlePaid's own order so a
// name one earlier branch would catch never reaches a later one.
//
// Not measured, each false (GO-7): TapPowerValue (CardLists.getTotalPower's
// tap-for-power replacements), Sticker*, AllTypes, and CreatureType/
// PlaneswalkerType/LandType (each needs the subtype vocabulary to tell a
// creature type from any other subtype). A property this does not recognize
// must not fall back to a plain match count: that would be a real but wrong
// number. CardPower and CardToughness are read only outside a layer pass
// (Game.layering): inside one another permanent's power is itself mid-rebuild.
func handlePaid(g *Game, amounts map[string]expr.Amount, sourceController PlayerID, source CardID, cards []*Card, def string, op *expr.Op, depth int) (int, bool) {
	math := func(n int) (int, bool) {
		if op == nil {
			return n, true
		}
		return applyOperator(g, amounts, sourceController, source, n, op, depth)
	}
	if len(cards) == 0 {
		return math(0)
	}
	switch {
	case strings.HasPrefix(def, "Amount"):
		return math(len(cards))
	case strings.HasPrefix(def, "TapPowerValue"):
		return 0, false
	case def == "Colors":
		if op != nil {
			return 0, false
		}
		var colors mana.Colors
		for _, c := range cards {
			colors |= c.Colors()
		}
		return colors.Count(), true
	case strings.HasPrefix(def, "DifferentCardNames"):
		ids := make([]CardID, len(cards))
		for i, c := range cards {
			ids[i] = c.ID
		}
		return math(differentNamesCount(g, ids))
	case def == "DifferentColorPair":
		if op != nil {
			return 0, false
		}
		var pairs []mana.Colors
		for _, c := range cards {
			if col := c.Colors(); col.Count() == 2 && !containsColors(pairs, col) {
				pairs = append(pairs, col)
			}
		}
		return len(pairs), true
	case strings.HasPrefix(def, "Valid"):
		// The shortcut filter (AbilityUtils.java:3773-3782): the cards the
		// valid string matches, "You" being the source's controller, then
		// either a nested property over them or their number.
		spec, nested, hasNested := strings.Cut(def, "$")
		restriction, isValid := strings.CutPrefix(spec, "Valid ")
		if !isValid || source == NoCard {
			return 0, false
		}
		parsed := valid.Parse(restriction)
		host := g.Card(source)
		var filtered []*Card
		for _, c := range cards {
			if Matches(g, c, parsed, host.Controller(), source) {
				filtered = append(filtered, c)
			}
		}
		if hasNested {
			return handlePaid(g, amounts, sourceController, source, filtered, nested, op, depth)
		}
		return math(len(filtered))
	case strings.HasPrefix(def, "StickerPower"), strings.HasPrefix(def, "StickerToughness"),
		strings.HasPrefix(def, "AllTypes"):
		return 0, false
	case strings.HasPrefix(def, "CardTypes"):
		permanent := strings.HasPrefix(def, "CardTypesPermanent")
		var seen cardtype.Line
		for _, c := range cards {
			seen = seen.Union(c.Type())
		}
		n := 0
		for _, t := range seen.CoreTypes() {
			if !permanent || t.IsPermanent() {
				n++
			}
		}
		return math(n)
	case strings.HasPrefix(def, "CreatureType"), strings.HasPrefix(def, "PlaneswalkerType"),
		strings.HasPrefix(def, "LandType"):
		return 0, false
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
	measure, ok := cardMeasure(g, perCard)
	if !ok {
		return 0, false
	}
	values := make([]int, len(cards))
	for i, c := range cards {
		v, ok := measure(c)
		if !ok {
			return 0, false
		}
		if values[i], ok = math(v); !ok {
			return 0, false
		}
	}
	return fold(values), true
}

// cardMeasure is the per-card xCount handlePaid's own fallthrough calls:
// liveCardMeasure outside a layer pass. Inside one a battlefield permanent's
// power and toughness are mid-rebuild and unresolved; a card elsewhere (an
// exiled card a CDA measures) changes only by its own characteristic-defining
// ability, and is read.
func cardMeasure(g *Game, head string) (func(*Card) (int, bool), bool) {
	live, ok := liveCardMeasure(head)
	if !ok || g.layering == 0 {
		return live, ok
	}
	if m, fixed := perCardMeasure(head); fixed {
		return func(c *Card) (int, bool) { return m(c), true }, true
	}
	return func(c *Card) (int, bool) {
		if c.Zone == Battlefield {
			return 0, false
		}
		return live(c)
	}, true
}

func containsColors(list []mana.Colors, c mana.Colors) bool {
	for _, v := range list {
		if v == c {
			return true
		}
	}
	return false
}

// differentNamesCount is CardLists.getDifferentNamesCount (CardLists.java:498-518):
// the distinct names among cards. A card with no name counts for nothing; the
// cards that have every non-legendary creature name (Spy Kit) are counted last,
// only if they share a name with none already counted.
func differentNamesCount(g *Game, cards []CardID) int {
	var plain, spy []CardID
	for _, id := range cards {
		c := g.Card(id)
		list := &plain
		if c.HasNonLegendaryCreatureNames {
			list = &spy
		}
		if c.Name() == "" && !c.HasNonLegendaryCreatureNames {
			continue
		}
		dup := false
		for _, kept := range *list {
			if g.sharesNameWith(id, kept) {
				dup = true
				break
			}
		}
		if !dup {
			*list = append(*list, id)
		}
	}
	for _, id := range spy {
		dup := false
		for _, kept := range plain {
			if g.sharesNameWith(id, kept) {
				dup = true
				break
			}
		}
		if !dup {
			plain = append(plain, id)
		}
	}
	return len(plain)
}

// sharesNameWith is Card.sharesNameWith(Card) (Card.java:5830-5851): two cards
// that both have every non-legendary creature name share one; an empty name
// shares nothing; a name that is some non-legendary creature's shares with a
// card that has all of those names; otherwise equal names.
func (g *Game) sharesNameWith(a, b CardID) bool {
	ca, cb := g.Card(a), g.Card(b)
	if ca.HasNonLegendaryCreatureNames && cb.HasNonLegendaryCreatureNames {
		return true
	}
	name := ca.Name()
	if name == "" {
		return false
	}
	if cb.HasNonLegendaryCreatureNames && g.isNonLegendaryCreatureName(name) {
		return true
	}
	return name == cb.Name()
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
	if head == "ExcessDamageReceivedThisTurn" {
		// xCount per card (AbilityUtils.java:2406-2408): Card.logExcessDamage's
		// running total.
		return func(c *Card) int { return c.Damage.ExcessReceived }, true
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
