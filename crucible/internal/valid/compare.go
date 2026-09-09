// Ported from forge-game/src/main/java/forge/game/card/CardProperty.java:1423
// (the power/toughness/cmc branch) and forge/util/Expressions.java (compare).

package valid

import "strings"

// Compare is a property that measures something and compares it to an operand:
// `powerGE1` is "power greater than or equal to 1".
type Compare struct {
	// Field is what is measured, spelled as the script spells it.
	Field string
	// Operator is one of LT, LE, EQ, GE, GT, NE, M2.
	Operator string
	// Operand is what it is compared against: a number, `X`, `Chosen`, or an
	// SVar name. Resolving it needs a game, so it stays text.
	Operand string
}

// compareFields are the measured fields, with the offset at which the operand
// begins.
//
// The offsets are Java's, hardcoded per field as substring() calls. They are
// not derivable from the field name: `totalPT` is written `totalPT_` with an
// underscore, which is why its operand starts two characters later than the
// name length suggests. Order matters, longest prefix first, so `basePower` is
// not read as `power`.
var compareFields = []struct {
	name   string
	offset int
}{
	{"baseToughness", 15},
	{"basePower", 11},
	{"toughness", 11},
	{"totalPT", 10},
	{"numColors", 11},
	{"numTypes", 10},
	{"power", 7},
	{"cmc", 5},
}

// compareOperators are checked in Java's order, and by containment rather than
// position: Expressions.compare takes the whole property string and asks
// whether it contains each operator in turn. Reproduced exactly, because the
// order is what decides a token that could match twice.
var compareOperators = []string{"LT", "LE", "EQ", "GE", "GT", "NE", "M2"}

// parseCompare splits a numeric comparison property into its parts.
//
// A property that names no measured field is not a comparison, and neither is
// one whose operator Expressions would not find -- Java would call
// Expressions.compare anyway and get false for every object, which is a
// property that matches nothing rather than a parse error.
func parseCompare(name string) (Compare, bool) {
	for _, field := range compareFields {
		if !strings.HasPrefix(name, field.name) {
			continue
		}
		if len(name) < field.offset {
			return Compare{}, false
		}
		operator, ok := operatorIn(name)
		if !ok {
			return Compare{}, false
		}
		return Compare{Field: field.name, Operator: operator, Operand: name[field.offset:]}, true
	}
	return Compare{}, false
}

// operatorIn returns the operator Expressions.compare would use.
func operatorIn(property string) (string, bool) {
	for _, operator := range compareOperators {
		if strings.Contains(property, operator) {
			return operator, true
		}
	}
	return "", false
}
