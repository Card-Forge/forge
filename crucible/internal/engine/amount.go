// Amount resolution: turning a compile.Face's own SVar-defined amounts
// (compile.Face.Amounts) into an int, the slice of AbilityUtils.calculateAmount
// this port can evaluate without a full port of it -- the "Valid" family of
// Count$ heads (CardLists.getValidCardCount against a zone this port already
// models), reusing Matches (valid.go) the identical way every other
// valid-string check in this port already does.
//
// Ported from forge-game/src/main/java/forge/game/ability/AbilityUtils.java's
// xCount/calculateAmount, the branch at line ~3424 ("count valid cards on the
// battlefield" / "count valid cards in any specified zone/s").

package engine

import (
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// maxAmountDepth bounds SVar-reference recursion (a `SVar:X:Y` chain) --
// defensive, not a real corpus need: the one real chain this port's own
// corpus-frequency research found (Roiling Horror's Y -> Z) carries an
// operator at each hop, which resolveAmount already refuses before it would
// ever recurse into it. A card whose own SVar chain is malformed enough to
// cycle fails to resolve rather than looping forever (GO-7).
const maxAmountDepth = 4

// resolveAmount evaluates amt to an int, when it is one of the shapes this
// port can compute: a plain Literal (Value already carries its own sign,
// [expr.Amount]'s own doc comment); a Reference to another SVar this same
// face defines, looked up in amounts and resolved in turn, one level of
// indirection at a time; or an Expression whose outer Head is "Count" with
// no operator suffix (Op == nil) and whose inner Count$ head is one of the
// "Valid" family (amt.Count, parsed at load, expr.IsValidHead) -- Count$Valid <spec>
// (battlefield) or Count$Valid<Zone>[,<Zone>...] <spec> (one or more other
// zones), counting cards each spec matches (countValid, below).
//
// Every other shape -- an operator suffix, a non-Count expression head
// (SVar$, PlayerCountOpponents, the eighty-some others
// AbilityUtils.calculateAmount itself dispatches on), a Count head outside
// the Valid family (xPaid, CardCounters, Devotion, ...), or a Valid family
// argument itself carrying a `$`-suffixed distinct-value operator
// (Tarmogoyf's own `Count$ValidGraveyard Card$CardTypes`,
// [expr.Count.DistinctProperty]'s own doc comment) -- reports false rather
// than a wrong number (GO-7): every real corpus caller of this (ptParam,
// continuous.go) already treats an unresolved amount as "skip this
// dimension," not "apply zero." The DistinctProperty case matters
// specifically because count.Valid itself still parses to something that
// looks usable (`Card`, matching every object) -- without the explicit
// check below, this would silently measure the wrong thing (a plain match
// count) rather than skip.
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
		if depth >= maxAmountDepth {
			return 0, false
		}
		next, ok := amounts[strings.ToLower(amt.Name)]
		if !ok {
			return 0, false
		}
		n, ok := resolveAmountDepth(g, amounts, sourceController, source, next, depth+1)
		if !ok {
			return 0, false
		}
		if amt.Negative {
			n = -n
		}
		return n, true

	case expr.Expression:
		if amt.Op != nil || !strings.EqualFold(amt.Head, "Count") {
			return 0, false
		}
		count := amt.Count
		if count == nil || !expr.IsValidHead(count.Head) {
			return 0, false
		}
		if count.DistinctProperty != "" {
			return 0, false
		}
		zones, ok := validCountZones(count.Head)
		if !ok {
			return 0, false
		}
		n := countValid(g, zones, count.Valid, sourceController, source)
		if amt.Negative {
			n = -n
		}
		return n, true
	}
	return 0, false
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
