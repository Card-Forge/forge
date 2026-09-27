// Ported from forge-game/src/main/java/forge/game/ability/AbilityUtils.java
// (xCount) and the Valid heads it reads.

package expr

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// Count is the body of a `Count$` expression: what is being counted.
type Count struct {
	// Head is the name alone, without the `.` parameters that may follow it.
	Head string
	// Parameters are the `.`-separated qualifiers on a simple head:
	// `TypeInYourGraveyard.Creature` counts creatures.
	Parameters []string
	// Valid is the embedded valid string, parsed. Set only when the head is one
	// of the Valid family.
	Valid valid.Spec
	// Argument is a space-separated argument that is not a valid string:
	// `Compare Y GE1`, `xColorPaid B`.
	Argument string
	// DistinctProperty is set when a Valid family argument itself carries a
	// `$`-suffixed operator -- `Count$ValidGraveyard Card$CardTypes`
	// (Tarmogoyf's own toughness SVar), where `Card$CardTypes` is not itself
	// a valid string at all: `xCount`'s own `paidparts = l[0].split("\\$", 2)`
	// cuts the whole "head argument" string on the FIRST `$`, so the actual
	// valid string is only the part before it (`Card`, which matches every
	// object -- Forge's own universal base) and the part after
	// (`CardTypes`) names a distinct-value count over whatever that valid
	// string matched (`handlePaid`, AbilityUtils.java:3719 --
	// `countCardTypesFromList`, counting DISTINCT card types among the
	// matches, not the matches themselves). Valid, above, is still set from
	// the part before the `$` -- parsing it as a normal valid string is
	// correct, just not sufficient on its own -- so a caller that ignores
	// DistinctProperty and evaluates Valid alone gets a real but wrong
	// answer (Tarmogoyf's own graveyard-card COUNT, not its distinct-type
	// count) rather than a skip. 350 real corpus lines carry one of 23
	// distinct properties (GreatestCardPower 67, GreatestCardManaCost 61,
	// CardPower 43, Colors 32, ... -- port-log/game-state/
	// layer7a-cda-amounts.md); the engine's own evaluator (paidMeasure,
	// engine/amountpaid.go) measures the subset it can and reports every
	// other property unresolved (GO-7) rather than silently measuring
	// Valid's own match count instead.
	DistinctProperty string
}

// ParseCount reads the body of a `Count$` expression, which is what is left
// after the head and the operator have been cut off.
//
// A head may take an argument after a space, and for the whole Valid family --
// `Valid`, `ValidHand`, `ValidGraveyard`, `ValidLibrary`, `ValidExile` and the
// rest -- that argument is a whole valid string. 172 corpus heads take one, so
// this is a family rather than the single `Count$Valid ` case, and a scanner
// that stops at whitespace loses the half that matters.
func ParseCount(body string) Count {
	// A leading space is written by 30 corpus values -- `Count$ 2`, `Count$ X`
	// -- and the head is what follows it.
	body = strings.TrimLeft(body, " ")

	if head, argument, ok := strings.Cut(body, " "); ok {
		if IsValidHead(head) {
			spec, distinct, hasDistinct := strings.Cut(argument, "$")
			c := Count{Head: head, Valid: valid.Parse(spec)}
			if hasDistinct {
				c.DistinctProperty = distinct
			}
			return c
		}
		return Count{Head: head, Argument: argument}
	}

	name, rest, ok := strings.Cut(body, ".")
	count := Count{Head: name}
	if ok {
		count.Parameters = strings.Split(rest, ".")
	}
	return count
}

// IsValidHead reports whether a count head's argument is a valid string.
//
// A prefix rule, like the one for param keys: the family grows with the zones
// Forge can count in, and a fixed list would go stale silently while a prefix
// fails loudly if a head ever starts with `Valid` without taking one.
func IsValidHead(head string) bool { return strings.HasPrefix(head, "Valid") }
