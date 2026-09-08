// Ported from forge-core/src/main/java/forge/card/MagicColor.java and
// forge-core/src/main/java/forge/card/ColorSet.java.
// Deviations recorded in docs/crucible/porting/port-log/mana-cost.md.

package mana

import "math/bits"

// Colors is a set of the five colours, held as a bitmask in WUBRG order.
//
// The zero value is the empty set, which is what "colorless" means for a colour
// identity: a card with no coloured mana symbols has no colours. That is the
// same choice Java makes in MagicColor, where COLORLESS is 0.
//
// The colorless mana *symbol* {C} is a different thing and is deliberately not
// representable here. It is a way to pay a cost, not a colour, and it lives in
// the shard's atom mask instead (see [Shard.IsColorless]). Java's two constants
// disagree on this exact point -- MagicColor.COLORLESS is 0 while
// ManaAtom.COLORLESS is 1<<5 -- and every comparison between them has to be
// adjusted by hand.
type Colors uint8

// The five colours. Bit positions match Java's MagicColor, so a mask crossing
// the oracle boundary needs no translation.
const (
	White Colors = 1 << iota
	Blue
	Black
	Red
	Green
)

// AllColors is WUBRG: every colour, and nothing else.
const AllColors = White | Blue | Black | Red | Green

// colorLetters is indexed by bit position, so iteration is in WUBRG order --
// the order Magic prints colours in, and the order Forge sorts sets by.
var colorLetters = [5]byte{'W', 'U', 'B', 'R', 'G'}

// Has reports whether c contains every colour in want. The empty set is
// contained in everything, so Has(0) is always true.
func (c Colors) Has(want Colors) bool { return c&want == want }

// HasAny reports whether c and want overlap.
func (c Colors) HasAny(want Colors) bool { return c&want != 0 }

// Count returns the number of colours in the set.
func (c Colors) Count() int { return bits.OnesCount8(uint8(c & AllColors)) }

// IsColorless reports whether the set is empty.
func (c Colors) IsColorless() bool { return c&AllColors == 0 }

// String returns the colour letters in WUBRG order, or "C" for the empty set.
// The names match Forge's ColorSet enum, whose colorless member is also C.
func (c Colors) String() string {
	if c.IsColorless() {
		return "C"
	}
	out := make([]byte, 0, 5)
	for i, letter := range colorLetters {
		if c&(1<<uint(i)) != 0 {
			out = append(out, letter)
		}
	}
	return string(out)
}

// ColorFromLetter maps a single mana letter to its colour. The second result is
// false for any letter that is not one of WUBRG, including 'C', which is a mana
// type rather than a colour.
func ColorFromLetter(ch byte) (Colors, bool) {
	switch ch {
	case 'W':
		return White, true
	case 'U':
		return Blue, true
	case 'B':
		return Black, true
	case 'R':
		return Red, true
	case 'G':
		return Green, true
	}
	return 0, false
}
