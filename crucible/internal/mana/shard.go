// Ported from forge-core/src/main/java/forge/card/mana/ManaCostShard.java and
// forge-core/src/main/java/forge/card/mana/ManaAtom.java.
// Deviations recorded in docs/crucible/porting/port-log/mana-cost.md.

package mana

import (
	"errors"
	"fmt"
	"strings"
)

// Atom bits describe what a shard can be paid with. A shard is the OR of its
// atoms, which is how one symbol expresses "red, or two life" ({R/P}) or
// "green, or two generic" ({2/G}).
//
// Bit positions match Java's ManaAtom so an atom mask can cross the oracle
// boundary unchanged. Bit 7 is skipped there because the colour half of the
// mask is narrowed to a byte in places; the gap is kept rather than closed.
const (
	atomWhite      = 1 << 0
	atomBlue       = 1 << 1
	atomBlack      = 1 << 2
	atomRed        = 1 << 3
	atomGreen      = 1 << 4
	atomColorless  = 1 << 5
	atomGeneric    = 1 << 6
	atomX          = 1 << 8
	atomOr2Generic = 1 << 9
	atomOr2Life    = 1 << 10
	atomSnow       = 1 << 11

	atomAllColors = atomWhite | atomBlue | atomBlack | atomRed | atomGreen
)

// Shard is one symbol of a mana cost: {W}, {2/U}, {G/P}, {S}, {X}, or the
// generic shard that stands for a number.
//
// Values are dense indices into an internal table, so a Shard is comparable,
// cheap to copy, and usable as an array index. The declaration order below is
// load-bearing and matches Java's enum: shards that offer the fewest ways to be
// paid come first, and the mana solver will walk them in that order so the most
// constrained symbol is satisfied first.
type Shard uint8

// The 45 shards, in Java's ManaCostShard declaration order.
const (
	// Pure colours, then the colorless symbol {C}.
	ShardW Shard = iota
	ShardU
	ShardB
	ShardR
	ShardG
	ShardC

	// Hybrid: either of two colours.
	ShardWU
	ShardWB
	ShardUB
	ShardUR
	ShardBR
	ShardBG
	ShardRW
	ShardRG
	ShardGW
	ShardGU

	// Monocoloured hybrid: one colour, or two generic.
	ShardW2
	ShardU2
	ShardB2
	ShardR2
	ShardG2

	// One colour, or one colorless.
	ShardCW
	ShardCU
	ShardCB
	ShardCR
	ShardCG

	// Snow, and the generic shard that carries a plain number.
	ShardSnow
	ShardGeneric

	// Phyrexian: a colour, or two life.
	ShardWP
	ShardUP
	ShardBP
	ShardRP
	ShardGP
	ShardBGP
	ShardBRP
	ShardGUP
	ShardGWP
	ShardRGP
	ShardRWP
	ShardUBP
	ShardURP
	ShardWBP
	ShardWUP

	// X, and the colour-restricted X of Emblazoned Golem.
	ShardX
	ShardColoredX

	numShards = int(ShardColoredX) + 1
)

type shardInfo struct {
	atoms  uint16
	symbol string // without braces, as written inside a card script
}

// shardTable is indexed by Shard and is never written after this declaration.
// Order matches the constants above, which matches Java.
var shardTable = [numShards]shardInfo{
	ShardW: {atomWhite, "W"},
	ShardU: {atomBlue, "U"},
	ShardB: {atomBlack, "B"},
	ShardR: {atomRed, "R"},
	ShardG: {atomGreen, "G"},
	ShardC: {atomColorless, "C"},

	ShardWU: {atomWhite | atomBlue, "W/U"},
	ShardWB: {atomWhite | atomBlack, "W/B"},
	ShardUB: {atomBlue | atomBlack, "U/B"},
	ShardUR: {atomBlue | atomRed, "U/R"},
	ShardBR: {atomBlack | atomRed, "B/R"},
	ShardBG: {atomBlack | atomGreen, "B/G"},
	ShardRW: {atomRed | atomWhite, "R/W"},
	ShardRG: {atomRed | atomGreen, "R/G"},
	ShardGW: {atomGreen | atomWhite, "G/W"},
	ShardGU: {atomGreen | atomBlue, "G/U"},

	ShardW2: {atomWhite | atomOr2Generic, "2/W"},
	ShardU2: {atomBlue | atomOr2Generic, "2/U"},
	ShardB2: {atomBlack | atomOr2Generic, "2/B"},
	ShardR2: {atomRed | atomOr2Generic, "2/R"},
	ShardG2: {atomGreen | atomOr2Generic, "2/G"},

	ShardCW: {atomWhite | atomColorless, "C/W"},
	ShardCU: {atomBlue | atomColorless, "C/U"},
	ShardCB: {atomBlack | atomColorless, "C/B"},
	ShardCR: {atomRed | atomColorless, "C/R"},
	ShardCG: {atomGreen | atomColorless, "C/G"},

	ShardSnow:    {atomSnow, "S"},
	ShardGeneric: {atomGeneric, "1"},

	ShardWP:  {atomWhite | atomOr2Life, "W/P"},
	ShardUP:  {atomBlue | atomOr2Life, "U/P"},
	ShardBP:  {atomBlack | atomOr2Life, "B/P"},
	ShardRP:  {atomRed | atomOr2Life, "R/P"},
	ShardGP:  {atomGreen | atomOr2Life, "G/P"},
	ShardBGP: {atomBlack | atomGreen | atomOr2Life, "B/G/P"},
	ShardBRP: {atomBlack | atomRed | atomOr2Life, "B/R/P"},
	ShardGUP: {atomGreen | atomBlue | atomOr2Life, "G/U/P"},
	ShardGWP: {atomGreen | atomWhite | atomOr2Life, "G/W/P"},
	ShardRGP: {atomRed | atomGreen | atomOr2Life, "R/G/P"},
	ShardRWP: {atomRed | atomWhite | atomOr2Life, "R/W/P"},
	ShardUBP: {atomBlue | atomBlack | atomOr2Life, "U/B/P"},
	ShardURP: {atomBlue | atomRed | atomOr2Life, "U/R/P"},
	ShardWBP: {atomWhite | atomBlack | atomOr2Life, "W/B/P"},
	ShardWUP: {atomWhite | atomBlue | atomOr2Life, "W/U/P"},

	ShardX:        {atomX, "X"},
	ShardColoredX: {atomAllColors | atomX, "1"},
}

// ErrUnknownShard reports a mana symbol the vocabulary does not contain. Card
// scripts are data, so a bad symbol is an error and not a panic (GO-7).
var ErrUnknownShard = errors.New("unknown mana symbol")

// ParseShard reads one symbol, written with or without braces: "W", "{W}",
// "2/U", "2U", "G/P". Slashes are separators only and carry no meaning, which
// is why the corpus spells the same shard both ways.
//
// Letter order inside a symbol does not matter, matching Java: the corpus
// contains "PRG" for what is canonically {R/G/P}.
func ParseShard(token string) (Shard, error) {
	return parseShardAtoms(strings.TrimSuffix(strings.TrimPrefix(token, "{"), "}"))
}

func parseShardAtoms(token string) (Shard, error) {
	if token == "" {
		return 0, fmt.Errorf("%w: empty symbol", ErrUnknownShard)
	}
	var atoms uint16
	for i := 0; i < len(token); i++ {
		switch c := token[i]; c {
		case 'W':
			atoms |= atomWhite
		case 'U':
			atoms |= atomBlue
		case 'B':
			atoms |= atomBlack
		case 'R':
			atoms |= atomRed
		case 'G':
			atoms |= atomGreen
		case 'C':
			atoms |= atomColorless
		case 'P':
			atoms |= atomOr2Life
		case 'S':
			atoms |= atomSnow
		case 'X':
			atoms |= atomX
		case '2':
			atoms |= atomOr2Generic
		case '/':
			// Separator between the halves of a hybrid symbol.
		default:
			if c >= '0' && c <= '9' {
				atoms |= atomGeneric
				continue
			}
			// Java maps an unrecognised character to no atom at all, which
			// turns a typo into a silently generic symbol. Crucible fails the
			// load instead, so the P2 vocabulary gate can be total.
			return 0, fmt.Errorf("%w: %q in symbol %q", ErrUnknownShard, c, token)
		}
	}

	// "2" and "12" reach here only when the caller has already ruled out a
	// plain number, but the atoms they produce still have to collapse to the
	// generic shard rather than to {2/W}-shaped nonsense.
	if atoms == atomOr2Generic || atoms == atomOr2Generic|atomGeneric {
		atoms = atomGeneric
	}
	return shardFromAtoms(atoms)
}

// shardFromAtoms returns the first shard whose atoms match exactly. First match
// wins, as in Java, so declaration order decides ties -- {1} before the coloured
// X of Emblazoned Golem, which shares its symbol.
func shardFromAtoms(atoms uint16) (Shard, error) {
	if atoms == 0 {
		return ShardGeneric, nil
	}
	for i, info := range shardTable {
		if info.atoms == atoms {
			return Shard(i), nil
		}
	}
	return 0, fmt.Errorf("%w: atom mask %#x", ErrUnknownShard, atoms)
}

// Symbol returns the shard without braces, as a card script writes it: "W",
// "2/U", "G/P".
func (s Shard) Symbol() string { return shardTable[s].symbol }

// String returns the shard in printed form, with braces: "{W}", "{2/U}".
func (s Shard) String() string { return "{" + shardTable[s].symbol + "}" }

// CMC returns the shard's contribution to mana value. X counts 0 and a
// monocoloured hybrid counts 2, per CR 202.3.
func (s Shard) CMC() int {
	switch atoms := shardTable[s].atoms; {
	case atoms&atomX != 0:
		return 0
	case atoms&atomOr2Generic != 0:
		return 2
	default:
		return 1
	}
}

// Colors returns the colours this shard can be paid with. Snow, generic, X and
// {C} are colourless and return the empty set.
func (s Shard) Colors() Colors { return Colors(shardTable[s].atoms & atomAllColors) }

// IsX reports whether the shard is an X symbol.
func (s Shard) IsX() bool { return shardTable[s].atoms&atomX != 0 }

// IsPhyrexian reports whether the shard can be paid with two life.
func (s Shard) IsPhyrexian() bool { return shardTable[s].atoms&atomOr2Life != 0 }

// IsSnow reports whether the shard requires snow mana.
func (s Shard) IsSnow() bool { return shardTable[s].atoms&atomSnow != 0 }

// IsColorless reports whether the shard accepts the colorless mana type {C}.
// A shard with no colours at all is not necessarily colorless in this sense:
// generic and X accept anything.
func (s Shard) IsColorless() bool { return shardTable[s].atoms&atomColorless != 0 }

// IsOr2Generic reports whether the shard is a monocoloured hybrid, {2/W}.
func (s Shard) IsOr2Generic() bool { return shardTable[s].atoms&atomOr2Generic != 0 }

// IsGeneric reports whether the shard is the plain number shard.
func (s Shard) IsGeneric() bool { return s == ShardGeneric }
