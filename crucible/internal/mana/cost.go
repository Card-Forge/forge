// Package mana holds the mana cost value types: colours, shards, and the cost
// a card script writes on its ManaCost line.
//
// Everything here is parsed once, at load, and shared read-only by every game
// (PORT-2). Java re-parses the same strings per card instance per game, which
// is the single largest waste in its engine and the first thing this port
// removes.
//
// Ported from forge-core/src/main/java/forge/card/mana/ManaCost.java and
// forge-core/src/main/java/forge/card/mana/ManaCostParser.java.
// Deviations recorded in docs/crucible/porting/port-log/mana-cost.md.
package mana

import (
	"errors"
	"fmt"
	"strconv"
	"strings"
)

// NoCostText is what a card script writes for a card that has no mana cost at
// all -- a land, or the back face of a transforming card. It is not the same as
// {0}: a zero cost is a cost that has been paid, and a card with no cost cannot
// be cast.
const NoCostText = "no cost"

// Cost is a mana cost: a generic amount plus an ordered run of shards.
//
// The zero value is {0}. A Cost is immutable once parsed; every method has a
// value receiver and none of them writes to the shard slice.
//
// Shard order is kept because it decides how the cost prints, and printed costs
// are diffed against the Java oracle. Order does not decide equality -- see
// [Cost.Equal].
type Cost struct {
	generic int
	shards  []Shard
	noCost  bool
}

// ErrBadManaCost reports a cost string the parser could not read. A card script
// is data, so this is an error and never a panic (GO-7).
var ErrBadManaCost = errors.New("bad mana cost")

// NoCost returns the cost of a card that has none.
func NoCost() Cost { return Cost{noCost: true} }

// GenericCost returns a cost of n generic mana and nothing else.
func GenericCost(n int) Cost { return Cost{generic: n} }

// Parse reads a mana cost.
//
// Both spellings the project has to handle are accepted: the space-separated
// form a card script writes on its ManaCost line ("2 W W", "X R", "2/B 2/R"),
// and the braced form this package prints ("{2}{W}{W}"). Java can only read the
// first and only write the second, so a cost cannot survive a round trip
// through it. Accepting both is what makes Parse(c.String()) == c hold, which
// is the property the corpus gate and the parser fuzzer both check.
//
// The literal "no cost" returns [NoCost]. Java handles that string one layer up,
// in the card-script reader, leaving its own parser unable to read a value its
// own corpus contains.
func Parse(text string) (Cost, error) {
	text = strings.TrimSpace(text)
	if text == NoCostText {
		return NoCost(), nil
	}

	tokens, err := tokenize(text)
	if err != nil {
		return Cost{}, fmt.Errorf("%w %q: %w", ErrBadManaCost, text, err)
	}

	var out Cost
	for _, tok := range tokens {
		if n, err := strconv.Atoi(tok); err == nil {
			out.generic += n
			continue
		}
		shard, err := parseShardAtoms(tok)
		if err != nil {
			return Cost{}, fmt.Errorf("%w %q: %w", ErrBadManaCost, text, err)
		}
		// A generic shard carries no information of its own: the number it
		// stands for is already in the generic total. Java drops it here too.
		if shard == ShardGeneric {
			continue
		}
		out.shards = append(out.shards, shard)
	}
	return out, nil
}

// MustParse is Parse for costs written in this repository -- test tables and
// constants -- where a parse failure is a bug in the caller, not in card data.
func MustParse(text string) Cost {
	c, err := Parse(text)
	if err != nil {
		panic(err)
	}
	return c
}

// tokenize splits a cost into symbols. A braced group is one symbol whatever it
// contains; anything else is split on whitespace.
func tokenize(text string) ([]string, error) {
	var out []string
	for i := 0; i < len(text); {
		switch text[i] {
		case ' ', '\t':
			i++
		case '{':
			end := strings.IndexByte(text[i:], '}')
			if end < 0 {
				return nil, fmt.Errorf("unclosed %q at offset %d", "{", i)
			}
			out = append(out, text[i+1:i+end])
			i += end + 1
		default:
			j := i
			for j < len(text) && text[j] != ' ' && text[j] != '\t' && text[j] != '{' {
				j++
			}
			out = append(out, text[i:j])
			i = j
		}
	}
	return out, nil
}

// IsNoCost reports whether the card has no mana cost at all.
func (c Cost) IsNoCost() bool { return c.noCost }

// IsZero reports whether the cost is {0} -- payable, and free.
func (c Cost) IsZero() bool { return !c.noCost && c.generic == 0 && len(c.shards) == 0 }

// Generic returns the generic portion. It can be negative once cost reduction
// has been applied, which is why it is not a uint.
func (c Cost) Generic() int { return c.generic }

// Shards returns the shards in the order they were written. The slice aliases
// the cost's storage; callers must not modify it.
func (c Cost) Shards() []Shard { return c.shards }

// CMC returns mana value: every shard's contribution plus the generic amount.
// X counts as zero (CR 202.3b).
func (c Cost) CMC() int {
	sum := c.generic
	for _, s := range c.shards {
		sum += s.CMC()
	}
	return sum
}

// Colors returns the colours appearing in the cost, which for most cards is
// also the card's colour.
func (c Cost) Colors() Colors {
	var out Colors
	for _, s := range c.shards {
		out |= s.Colors()
	}
	return out
}

// Count returns how many times a shard appears. Asking for [ShardGeneric]
// returns the generic amount, since that is where the number lives.
func (c Cost) Count(want Shard) int {
	if want == ShardGeneric {
		return c.generic
	}
	n := 0
	for _, s := range c.shards {
		if s == want {
			n++
		}
	}
	return n
}

// CountX returns the number of X symbols in the cost.
func (c Cost) CountX() int { return c.Count(ShardX) }

// HasPhyrexian reports whether any shard can be paid with life.
func (c Cost) HasPhyrexian() bool {
	for _, s := range c.shards {
		if s.IsPhyrexian() {
			return true
		}
	}
	return false
}

// Equal reports whether two costs demand the same payment.
//
// Shards are compared as a multiset, not as a sequence: {X}{R} and {R}{X} are
// one cost written two ways, and both print identically. Java never answers
// this question -- ManaCost does not override equals -- so there is no
// behaviour to match, only a definition to choose.
func (c Cost) Equal(other Cost) bool {
	if c.noCost != other.noCost || c.generic != other.generic || len(c.shards) != len(other.shards) {
		return false
	}
	var counts [numShards]int
	for _, s := range c.shards {
		counts[s]++
	}
	for _, s := range other.shards {
		counts[s]--
		if counts[s] < 0 {
			return false
		}
	}
	return true
}

// String prints the cost in the form the oracle dump uses: "{2}{W}{W}", "{X}{R}",
// or "no cost".
//
// X symbols print before the generic amount, every other shard after it, which
// is the order Java's StringBuilder produces.
func (c Cost) String() string {
	if c.noCost {
		return NoCostText
	}
	if len(c.shards) == 0 {
		return "{" + strconv.Itoa(c.generic) + "}"
	}

	var head, tail strings.Builder
	for _, s := range c.shards {
		if s == ShardX {
			// Java inserts each X at position 0, so a second X lands before the
			// first. Every X prints the same, so the reversal is invisible.
			head.WriteString(s.String())
			continue
		}
		tail.WriteString(s.String())
	}

	var b strings.Builder
	b.WriteString(head.String())
	if c.generic > 0 {
		b.WriteString("{")
		b.WriteString(strconv.Itoa(c.generic))
		b.WriteString("}")
	}
	b.WriteString(tail.String())
	// A cost reduced below zero shows the reduction, which only happens to X
	// spells and only after an effect has touched the cost.
	if c.generic < 0 {
		b.WriteString(" ")
		b.WriteString(strconv.Itoa(c.generic))
	}
	return b.String()
}
