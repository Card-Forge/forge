// Amount resolution: turning a compile.Face's own SVar-defined amounts
// (compile.Face.Amounts) into an int -- the slice of
// AbilityUtils.calculateAmount/xCount this port can evaluate against state it
// already models. What resolves: plain and SVar-referenced numbers, doXMath's
// arithmetic suffixes, the Number$/SVar$ heads, the Count$Valid family
// (CardLists.getValidCardCount, reusing Matches from valid.go) with the
// handlePaid distinct-value subset in amountpaid.go, and the host-, player- and
// zone-measuring Count$ heads in amountheads.go.
//
// Ported from forge-game/src/main/java/forge/game/ability/AbilityUtils.java's
// calculateAmount (:367), xCount (:1566), doXMath (:3206), playerXCount
// (:3288) and handlePaid (:3675).

package engine

import (
	"math"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// maxAmountDepth bounds SVar-reference recursion (a `SVar:X:Y` chain, an
// SVar$ head, an operator's own SVar operand) -- defensive, not a real corpus
// need: the deepest real chain a CDA writes (Roiling Horror's X -> Y -> Z,
// Tarmogoyf's Y -> SVar$X -> X) is three hops. A card whose own SVar chain is
// malformed enough to cycle fails to resolve rather than looping forever
// (GO-7).
const maxAmountDepth = 6

// resolveAmount evaluates amt to an int, when it is one of the shapes this
// port can compute: a plain Literal (Value already carries its own sign,
// [expr.Amount]'s own doc comment); a Reference to an SVar, looked up in
// source's runtime SVars first and amounts second, resolved in turn one level
// of indirection at a time (namedAmount); or an Expression whose head
// expressionValue evaluates, with its own doXMath suffix (applyOperator) and
// leading `-` applied after, in calculateAmount's own order (the multiplier
// last, `val * multiplier`).
//
// Every other shape -- a context-prefixed head (`CastSA>`, `Spawner>`,
// `TriggeredSpellAbility>`: adjustTriggerContext switches which ability the
// measurement is taken against, which a static ability has none of), one of
// the eighty-some other heads calculateAmount/xCount dispatch on, or an SVar
// operand that itself does not resolve -- reports false rather than a wrong
// number (GO-7): every caller already treats an unresolved amount as "skip
// this dimension" (ptParam, continuous.go) or as an error naming the value,
// never as "apply zero" -- which is what Java itself would print to stderr
// and do.
//
// sourceController/source are the pairing Matches itself always takes: for
// a static ability, host's own controller and id (AbilityUtils.xCount's own
// `player = ctb.getHostCard().getController()` when no activating
// SpellAbility exists, which is always true for a Mode$ Continuous line).
func resolveAmount(g *Game, amounts map[string]expr.Amount, sourceController PlayerID, source CardID, amt expr.Amount) (int, bool) {
	return resolveAmountDepth(g, amounts, sourceController, source, amt, 0)
}

func resolveAmountDepth(g *Game, amounts map[string]expr.Amount, sourceController PlayerID, source CardID, amt expr.Amount, depth int) (int, bool) {
	switch amt.Kind {
	case expr.Literal:
		return amt.Value, true

	case expr.Reference:
		n, ok := namedAmount(g, amounts, sourceController, source, amt.Name, depth)
		if !ok {
			return 0, false
		}
		if amt.Negative {
			n = -n
		}
		return n, true

	case expr.Expression:
		if amt.Context != "" {
			return 0, false
		}
		n, ok := expressionValue(g, amounts, sourceController, source, amt, depth)
		if !ok {
			return 0, false
		}
		if amt.Op != nil {
			if n, ok = applyOperator(g, amounts, sourceController, source, n, amt.Op, depth); !ok {
				return 0, false
			}
		}
		if amt.Negative {
			n = -n
		}
		return n, true
	}
	return 0, false
}

// namedAmount is calculateAmount's own SVar lookup for a bare name:
// ability.getSVar, then card.getSVar -- a runtime SVar an effect stored on
// source (Card.svars, StoreSVar's own setSVar) shadowing the script's own
// SVar of that name, the same order resolveNamedAmount already uses for the
// outermost name. depth counts the hop so a cycle ends (maxAmountDepth).
func namedAmount(g *Game, amounts map[string]expr.Amount, sourceController PlayerID, source CardID, name string, depth int) (int, bool) {
	if depth >= maxAmountDepth {
		return 0, false
	}
	key := strings.ToLower(name)
	if source != NoCard {
		if n, ok := g.Card(source).svars[key]; ok {
			return n, true
		}
	}
	next, ok := amounts[key]
	if !ok {
		return 0, false
	}
	return resolveAmountDepth(g, amounts, sourceController, source, next, depth+1)
}

// expressionValue is calculateAmount's own head dispatch, before any
// operator: `Count$` (xCount, countValue in amountheads.go), `Number$N` (a
// plain number, the one head carrying no measurement at all), `SVar$Name`
// (the named SVar, resolved -- calculateAmount's own
// `calculateAmount(card, l[0], ability)`) and `PlayerCount<Players>$`
// (playerCountValue, amountheads.go). Head is matched as written: Java tests
// calcX[0].startsWith for each, and no corpus head extends one of these four
// names into another.
func expressionValue(g *Game, amounts map[string]expr.Amount, sourceController PlayerID, source CardID, amt expr.Amount, depth int) (int, bool) {
	switch {
	case amt.Count != nil:
		return countValue(g, sourceController, source, *amt.Count)
	case amt.Head == "Number":
		return amt.Value, amt.Numeric
	case amt.Head == "SVar":
		return namedAmount(g, amounts, sourceController, source, amt.Body, depth)
	case strings.HasPrefix(amt.Head, "PlayerCount"):
		// playerXCount's own Highest/Lowest branch hands the whole
		// "HighestX/Op" string down to playerXProperty, which applies the
		// operator per player, then applies it again to the result -- a
		// double application no corpus line exercises. Refused rather than
		// guessed at either way (GO-7).
		if amt.Op != nil {
			return 0, false
		}
		return playerCountValue(g, sourceController, strings.TrimPrefix(amt.Head, "PlayerCount"), amt.Body)
	}
	return 0, false
}

// applyOperator is doXMath: op's own arithmetic applied to n. The operator
// is matched by containment in doXMath's own order (expr.Operator); a name
// matching none leaves n unchanged, doXMath's own final else.
//
// The operand (doXMath's secondaryNum) is read only by the operators that
// use it, and is 0 when the suffix carries none or carries more than one
// `.`-separated part (`s.length == 2` is the only case Java parses). An
// integer operand was read at load (expr.Op.Numeric); anything else is an
// SVar name, resolved like any other (namedAmount) -- Java's own
// `calculateAmount(c, s[1], ctb)` fallback. false for an operand that does
// not resolve, and for Mod by zero (Java throws ArithmeticException there,
// which no card script can be allowed to do to a batch, GO-7).
func applyOperator(g *Game, amounts map[string]expr.Amount, sourceController PlayerID, source CardID, n int, op *expr.Op, depth int) (int, bool) {
	name, ok := expr.Operator(op.Name)
	if !ok {
		return n, true
	}
	secondary := func() (int, bool) {
		switch {
		case op.Numeric:
			return op.Value, true
		case op.Operand == "" || strings.Contains(op.Operand, "."):
			return 0, true
		}
		return namedAmount(g, amounts, sourceController, source, op.Operand, depth)
	}
	switch name {
	case "Twice":
		return n * 2, true
	case "Thrice":
		return n * 3, true
	case "HalfUp":
		return int(math.Ceil(float64(n) / 2)), true
	case "HalfDown":
		return int(math.Floor(float64(n) / 2)), true
	case "ThirdUp":
		return int(math.Ceil(float64(n) / 3)), true
	case "ThirdDown":
		return int(math.Floor(float64(n) / 3)), true
	case "Negative":
		return -n, true
	case "Abs":
		if n < 0 {
			return -n, true
		}
		return n, true
	}
	m, ok := secondary()
	if !ok {
		return 0, false
	}
	switch name {
	case "Plus":
		return n + m, true
	case "NMinus":
		return m - n, true
	case "Minus":
		return n - m, true
	case "Times":
		return n * m, true
	case "Pow":
		return int(math.Pow(float64(n), float64(m))), true
	case "DivideEvenlyUp":
		if m == 0 {
			return 0, true
		}
		if n%m == 0 {
			return n / m, true
		}
		return n/m + 1, true
	case "DivideEvenlyDown":
		if m == 0 {
			return 0, true
		}
		return n / m, true
	case "Mod":
		if m == 0 {
			return 0, false
		}
		return n % m, true
	case "LimitMax":
		return min(n, m), true
	case "LimitMin":
		return max(n, m), true
	}
	return n, true
}

// validZoneNames maps a Count$Valid<Zone> head's own zone suffix (the part
// after "Valid") to the ZoneType it names -- CardFactoryUtil's own
// ZoneType.listValueOf, the single-zone subset this port's own ZoneType
// (zone.go) already covers. A bare "Valid" (empty suffix) is Battlefield,
// Java's own default when the corpus writes no suffix at all (l[0].startsWith
// ("Valid ")): 1,973 of the corpus's 2,804 real Count$Valid* lines.
var validZoneNames = map[string]ZoneType{
	"":            Battlefield,
	"Battlefield": Battlefield,
	"Graveyard":   Graveyard,
	"Hand":        Hand,
	"Library":     Library,
	"Exile":       Exile,
	"Command":     Command,
	"Stack":       Stack,
	"Sideboard":   Sideboard,
}

// validCountZones splits a Count$Valid<Zone1>,<Zone2>... head's own zone
// suffix on "," (ZoneType.listValueOf's own comma-list contract, 40-some
// real corpus lines naming more than one) and maps each to a ZoneType via
// validZoneNames. false the moment any one name is unrecognized ("All",
// "Self" -- 8 real lines together, neither a zone name at all) -- the same
// "skip the whole line rather than count only some of the zones it names"
// contract every other partial-corpus-shape gap in this port already has.
func validCountZones(head string) ([]ZoneType, bool) {
	suffix := strings.TrimPrefix(head, "Valid")
	if suffix == "" {
		return []ZoneType{Battlefield}, true
	}
	names := strings.Split(suffix, ",")
	zones := make([]ZoneType, 0, len(names))
	for _, name := range names {
		z, ok := validZoneNames[name]
		if !ok {
			return nil, false
		}
		zones = append(zones, z)
	}
	return zones, true
}

// countValid is CardLists.getValidCardCount, ported: how many cards across
// every one of zones, every player's own, spec (valid.go's own Matches)
// accepts -- spec's own YouCtrl/YouOwn/OppCtrl properties (already evaluated
// relative to sourceController) are what narrow "every player's" down to
// "yours" or "an opponent's" when the corpus spec itself says so, the
// identical division CardLists.getValidCardCount leaves to the restriction
// string rather than the zone lookup.
func countValid(g *Game, zones []ZoneType, spec valid.Spec, sourceController PlayerID, source CardID) int {
	n := 0
	for _, z := range zones {
		for _, pid := range g.Players() {
			for _, id := range g.Zone(z, pid).Cards() {
				if Matches(g, g.Card(id), spec, sourceController, source) {
					n++
				}
			}
		}
	}
	return n
}

// resolveNamedAmount is AbilityUtils.calculateAmount's own two cases: a
// plain base-10 integer, or the name of an SVar amounts defines, resolved
// via resolveAmount above. Shared by ptParam (continuous.go, a continuous
// effect's own numeric params) and trigger.go's own
// triggerCommonRequirementsMet (CheckSVar$'s own value, and both halves of
// every *Compare$ operand there) -- neither owns this outright, so it lives
// here alongside resolveAmount itself rather than in either.
func resolveNamedAmount(g *Game, amounts map[string]expr.Amount, host *Card, value string) (int, bool) {
	if n, err := strconv.Atoi(value); err == nil {
		return n, true
	}
	if n, ok := host.svars[strings.ToLower(value)]; ok {
		return n, true
	}
	amt, ok := amounts[strings.ToLower(value)]
	if !ok {
		return 0, false
	}
	return resolveAmount(g, amounts, host.Controller(), host.ID, amt)
}
