// Ported from forge-core/src/main/java/forge/card/CardFace.java (parsePT and
// setPtText).
// Deviations recorded in docs/crucible/porting/port-log/card-rules-reader.md.

package carddb

import (
	"math"
	"strconv"
	"strings"
)

// PTUnset is the integer power or toughness of a face that has no `PT:` line.
//
// Java initialises both fields to Integer.MAX_VALUE and overwrites them only
// when a script sets P/T, so callers see a sentinel rather than zero -- and a
// 0/0 creature is a real card. The value is part of the ported behaviour, not a
// Go convenience.
const PTUnset = math.MaxInt32

// IntPower is the face's power as a number, with a characteristic-defining `*`
// counted as zero.
func (f *Face) IntPower() int {
	n, _ := parsePT(f.Power)
	return n
}

// IntToughness is the face's toughness as a number, on the same terms as
// [Face.IntPower].
func (f *Face) IntToughness() int {
	n, _ := parsePT(f.Toughness)
	return n
}

// parsePT turns a written power or toughness into the number Forge computes
// with: `*` becomes zero and the sign binding it is removed with it, so `1+*`
// is 1, `*+1` is 1 and `7-*` is 7.
//
// The four replacements run in sequence, not as one pass, because that is what
// Java's chained String.replace does and the two differ on a value holding more
// than one `*`. Reproduced rather than tidied: this number is the base every
// later layer modifies, so a different one would put the whole engine off the
// oracle by a point.
//
// An empty value means the script set no P/T at all, which is [PTUnset]. A
// value that survives normalisation without being a number is an error, which
// is Java throwing out of setPtText; no card in the corpus does it.
func parsePT(value string) (int, error) {
	if value == "" {
		return PTUnset, nil
	}
	normalized := value
	if strings.Contains(normalized, "*") {
		normalized = strings.ReplaceAll(normalized, "+*", "")
		normalized = strings.ReplaceAll(normalized, "-*", "")
		normalized = strings.ReplaceAll(normalized, "*+", "")
		normalized = strings.ReplaceAll(normalized, "*", "0")
	}
	n, err := strconv.Atoi(normalized)
	if err != nil {
		return PTUnset, err
	}
	return n, nil
}
