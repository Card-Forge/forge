package mana_test

import (
	"errors"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/mana"
)

// A table test rather than scenarios: the parser is a pure function over a
// large input space, which is the case TEST-2 sends here.
func TestParseCost(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name    string
		in      string
		want    string // canonical printed form
		cmc     int
		colors  string
		generic int
	}{
		{"empty cost is zero", "0", "{0}", 0, "C", 0},
		{"land has no cost at all", "no cost", "no cost", 0, "C", 0},
		{"single colour", "R", "{R}", 1, "R", 0},
		{"generic plus colour", "2 W W", "{2}{W}{W}", 4, "W", 2},
		{"generic tokens add up", "1 2 U", "{3}{U}", 4, "U", 3},
		{"two-digit generic", "16", "{16}", 16, "C", 16},
		{"x counts zero and prints first", "X R", "{X}{R}", 1, "R", 0},
		{"x with generic prints before it", "1 X R", "{X}{1}{R}", 2, "R", 1},
		{"hybrid", "1 BG G", "{1}{B/G}{G}", 3, "BG", 1},
		{"monocoloured hybrid counts two", "2/B 2/R 2/G", "{2/B}{2/R}{2/G}", 6, "BRG", 0},
		{"slash is optional", "2B", "{2/B}", 2, "B", 0},
		{"phyrexian", "2 GWP", "{2}{G/W/P}", 3, "WG", 2},
		{"letter order inside a symbol is free", "2 R PRG G", "{2}{R}{R/G/P}{G}", 5, "RG", 2},
		{"colorless symbol is not a colour", "1 C", "{1}{C}", 2, "C", 1},
		{"snow", "S", "{S}", 1, "C", 0},
		{"braced input", "{2}{W}{W}", "{2}{W}{W}", 4, "W", 2},
		{"braced input with x", "{X}{1}{R}", "{X}{1}{R}", 2, "R", 1},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			got, err := mana.Parse(tt.in)
			if err != nil {
				t.Fatalf("Parse(%q) failed: %v", tt.in, err)
			}
			if got.String() != tt.want {
				t.Errorf("Parse(%q).String() = %q, want %q", tt.in, got.String(), tt.want)
			}
			if got.CMC() != tt.cmc {
				t.Errorf("Parse(%q).CMC() = %d, want %d", tt.in, got.CMC(), tt.cmc)
			}
			if got.Colors().String() != tt.colors {
				t.Errorf("Parse(%q).Colors() = %s, want %s", tt.in, got.Colors(), tt.colors)
			}
			if got.Generic() != tt.generic {
				t.Errorf("Parse(%q).Generic() = %d, want %d", tt.in, got.Generic(), tt.generic)
			}

			// The property the corpus gate rests on: what this package prints,
			// it can read back.
			again, err := mana.Parse(got.String())
			if err != nil {
				t.Fatalf("Parse(%q) -- reparsing own output -- failed: %v", got, err)
			}
			if !again.Equal(got) {
				t.Errorf("Parse(%q) round trip = %q, want %q", tt.in, again, got)
			}
		})
	}
}

func TestParseRejectsUnknownSymbols(t *testing.T) {
	t.Parallel()

	// Java maps every one of these to a silently generic symbol. Crucible
	// fails the load instead, so a typo in a card script cannot become a
	// playable card with the wrong cost (GO-7).
	for _, in := range []string{"Q", "2 Q", "W Y", "{W", "no  cost", "nocost", "W}"} {
		if got, err := mana.Parse(in); err == nil {
			t.Errorf("Parse(%q) = %q, want an error", in, got)
		} else if !errors.Is(err, mana.ErrBadManaCost) {
			t.Errorf("Parse(%q) error = %v, want one wrapping ErrBadManaCost", in, err)
		} else if !strings.Contains(err.Error(), in) {
			t.Errorf("Parse(%q) error = %v, want the offending cost named in it", in, err)
		}
	}
}

func TestCostEqualIgnoresShardOrder(t *testing.T) {
	t.Parallel()

	tests := []struct {
		a, b string
		want bool
	}{
		{"X R", "R X", true},
		{"1 W U", "1 U W", true},
		{"2 W", "1 1 W", true},
		{"W W", "W", false},
		{"2 W", "2 U", false},
		{"0", "no cost", false},
		{"no cost", "no cost", true},
	}
	for _, tt := range tests {
		a, b := mana.MustParse(tt.a), mana.MustParse(tt.b)
		if got := a.Equal(b); got != tt.want {
			t.Errorf("Parse(%q).Equal(Parse(%q)) = %v, want %v", tt.a, tt.b, got, tt.want)
		}
		if got := b.Equal(a); got != tt.want {
			t.Errorf("Parse(%q).Equal(Parse(%q)) = %v, want %v -- Equal is not symmetric", tt.b, tt.a, got, tt.want)
		}
	}
}

func TestCostCounts(t *testing.T) {
	t.Parallel()

	c := mana.MustParse("2 X X W/P W")
	if got, want := c.CountX(), 2; got != want {
		t.Errorf("CountX() = %d, want %d", got, want)
	}
	if got, want := c.Count(mana.ShardGeneric), 2; got != want {
		t.Errorf("Count(ShardGeneric) = %d, want %d -- generic lives in the number, not the shards", got, want)
	}
	if got, want := c.Count(mana.ShardW), 1; got != want {
		t.Errorf("Count(ShardW) = %d, want %d", got, want)
	}
	if !c.HasPhyrexian() {
		t.Errorf("HasPhyrexian() = false for %q, want true", c)
	}
	if got, want := c.CMC(), 4; got != want {
		t.Errorf("CMC() = %d, want %d -- X counts zero", got, want)
	}
}

func TestNoCostIsNotZero(t *testing.T) {
	t.Parallel()

	// A land has no cost; a Black Lotus-shaped {0} has a cost of nothing. The
	// difference decides whether a card can be cast at all, so the two must not
	// collapse into one value.
	none, zero := mana.NoCost(), mana.GenericCost(0)
	if !none.IsNoCost() || none.IsZero() {
		t.Errorf("NoCost(): IsNoCost = %v, IsZero = %v, want true and false", none.IsNoCost(), none.IsZero())
	}
	if zero.IsNoCost() || !zero.IsZero() {
		t.Errorf("GenericCost(0): IsNoCost = %v, IsZero = %v, want false and true", zero.IsNoCost(), zero.IsZero())
	}
	if none.Equal(zero) {
		t.Errorf("NoCost().Equal(GenericCost(0)) = true, want false")
	}
}

func TestShardProperties(t *testing.T) {
	t.Parallel()

	tests := []struct {
		symbol    string
		shard     mana.Shard
		cmc       int
		colors    string
		phyrexian bool
		snow      bool
		x         bool
	}{
		{"W", mana.ShardW, 1, "W", false, false, false},
		{"C", mana.ShardC, 1, "C", false, false, false},
		{"W/U", mana.ShardWU, 1, "WU", false, false, false},
		{"2/W", mana.ShardW2, 2, "W", false, false, false},
		{"G/P", mana.ShardGP, 1, "G", true, false, false},
		{"U/R/P", mana.ShardURP, 1, "UR", true, false, false},
		{"S", mana.ShardSnow, 1, "C", false, true, false},
		{"X", mana.ShardX, 0, "C", false, false, true},
	}
	for _, tt := range tests {
		got, err := mana.ParseShard(tt.symbol)
		if err != nil {
			t.Fatalf("ParseShard(%q) failed: %v", tt.symbol, err)
		}
		if got != tt.shard {
			t.Errorf("ParseShard(%q) = %v, want %v", tt.symbol, got, tt.shard)
		}
		if got.Symbol() != tt.symbol {
			t.Errorf("ParseShard(%q).Symbol() = %q, want %q", tt.symbol, got.Symbol(), tt.symbol)
		}
		if got.String() != "{"+tt.symbol+"}" {
			t.Errorf("ParseShard(%q).String() = %q, want %q", tt.symbol, got.String(), "{"+tt.symbol+"}")
		}
		if got.CMC() != tt.cmc {
			t.Errorf("%v.CMC() = %d, want %d", got, got.CMC(), tt.cmc)
		}
		if got.Colors().String() != tt.colors {
			t.Errorf("%v.Colors() = %s, want %s", got, got.Colors(), tt.colors)
		}
		if got.IsPhyrexian() != tt.phyrexian || got.IsSnow() != tt.snow || got.IsX() != tt.x {
			t.Errorf("%v: phyrexian = %v, snow = %v, x = %v; want %v, %v, %v",
				got, got.IsPhyrexian(), got.IsSnow(), got.IsX(), tt.phyrexian, tt.snow, tt.x)
		}
	}
}

// Shard order is the order Java's mana solver will consume: the symbols with
// the fewest ways to be paid come first. A reordering here would change which
// payment the solver finds, so it is pinned.
func TestShardDeclarationOrderMatchesJava(t *testing.T) {
	t.Parallel()

	order := []mana.Shard{
		mana.ShardW, mana.ShardU, mana.ShardB, mana.ShardR, mana.ShardG, mana.ShardC,
		mana.ShardWU, mana.ShardWB, mana.ShardUB, mana.ShardUR, mana.ShardBR,
		mana.ShardBG, mana.ShardRW, mana.ShardRG, mana.ShardGW, mana.ShardGU,
		mana.ShardW2, mana.ShardU2, mana.ShardB2, mana.ShardR2, mana.ShardG2,
		mana.ShardCW, mana.ShardCU, mana.ShardCB, mana.ShardCR, mana.ShardCG,
		mana.ShardSnow, mana.ShardGeneric,
		mana.ShardWP, mana.ShardUP, mana.ShardBP, mana.ShardRP, mana.ShardGP,
		mana.ShardBGP, mana.ShardBRP, mana.ShardGUP, mana.ShardGWP, mana.ShardRGP,
		mana.ShardRWP, mana.ShardUBP, mana.ShardURP, mana.ShardWBP, mana.ShardWUP,
		mana.ShardX, mana.ShardColoredX,
	}
	for i, s := range order {
		if int(s) != i {
			t.Errorf("%v has index %d, want %d -- ManaCostShard declaration order changed", s, s, i)
		}
	}
}

func TestColorsSet(t *testing.T) {
	t.Parallel()

	tests := []struct {
		colors mana.Colors
		want   string
		count  int
	}{
		{0, "C", 0},
		{mana.White, "W", 1},
		{mana.Green | mana.White, "WG", 2},
		{mana.AllColors, "WUBRG", 5},
	}
	for _, tt := range tests {
		if got := tt.colors.String(); got != tt.want {
			t.Errorf("Colors(%d).String() = %q, want %q", tt.colors, got, tt.want)
		}
		if got := tt.colors.Count(); got != tt.count {
			t.Errorf("%s.Count() = %d, want %d", tt.want, got, tt.count)
		}
	}

	wu := mana.White | mana.Blue
	if !wu.Has(mana.White) || wu.Has(mana.Black) {
		t.Errorf("WU.Has(W) = %v, WU.Has(B) = %v, want true and false", wu.Has(mana.White), wu.Has(mana.Black))
	}
	if !wu.HasAny(mana.Blue|mana.Red) || wu.HasAny(mana.Green) {
		t.Errorf("WU.HasAny(UR) = %v, WU.HasAny(G) = %v, want true and false",
			wu.HasAny(mana.Blue|mana.Red), wu.HasAny(mana.Green))
	}
	if !mana.Colors(0).IsColorless() || mana.White.IsColorless() {
		t.Errorf("IsColorless: empty = %v, W = %v, want true and false",
			mana.Colors(0).IsColorless(), mana.White.IsColorless())
	}
	if _, ok := mana.ColorFromLetter('C'); ok {
		t.Errorf("ColorFromLetter('C') reported a colour; {C} is a mana type, not one of WUBRG")
	}
}

func TestShardClassification(t *testing.T) {
	t.Parallel()

	tests := []struct {
		shard      mana.Shard
		colorless  bool
		or2generic bool
		generic    bool
	}{
		{mana.ShardW, false, false, false},
		{mana.ShardC, true, false, false},
		{mana.ShardCW, true, false, false},
		{mana.ShardW2, false, true, false},
		{mana.ShardGeneric, false, false, true},
		{mana.ShardX, false, false, false},
	}
	for _, tt := range tests {
		if got := tt.shard.IsColorless(); got != tt.colorless {
			t.Errorf("%v.IsColorless() = %v, want %v", tt.shard, got, tt.colorless)
		}
		if got := tt.shard.IsOr2Generic(); got != tt.or2generic {
			t.Errorf("%v.IsOr2Generic() = %v, want %v", tt.shard, got, tt.or2generic)
		}
		if got := tt.shard.IsGeneric(); got != tt.generic {
			t.Errorf("%v.IsGeneric() = %v, want %v", tt.shard, got, tt.generic)
		}
	}
}

func TestParseShardRejectsUnknownCombinations(t *testing.T) {
	t.Parallel()

	// Three colours with no Phyrexian half is not a symbol Magic prints, so no
	// shard carries those atoms and the lookup has to fail rather than pick
	// something close.
	for _, in := range []string{"WUB", "", "{}", "WX"} {
		if got, err := mana.ParseShard(in); err == nil {
			t.Errorf("ParseShard(%q) = %v, want an error", in, got)
		} else if !errors.Is(err, mana.ErrUnknownShard) {
			t.Errorf("ParseShard(%q) error = %v, want one wrapping ErrUnknownShard", in, err)
		}
	}
}

func TestCostShardsAreInWrittenOrder(t *testing.T) {
	t.Parallel()

	got := mana.MustParse("1 G W/U").Shards()
	want := []mana.Shard{mana.ShardG, mana.ShardWU}
	if len(got) != len(want) {
		t.Fatalf("Shards() = %v, want %v", got, want)
	}
	for i := range want {
		if got[i] != want[i] {
			t.Fatalf("Shards() = %v, want %v", got, want)
		}
	}

	if mana.MustParse("2 W").HasPhyrexian() {
		t.Error("HasPhyrexian() = true for {2}{W}, want false")
	}
}

func TestMustParsePanicsOnBadInput(t *testing.T) {
	t.Parallel()

	// MustParse is for costs written in this repository, where a bad cost is a
	// bug in the caller rather than in card data.
	defer func() {
		if recover() == nil {
			t.Error("MustParse(\"Q\") did not panic")
		}
	}()
	mana.MustParse("Q")
}

func TestColorFromLetter(t *testing.T) {
	t.Parallel()

	for letter, want := range map[byte]mana.Colors{
		'W': mana.White,
		'U': mana.Blue,
		'B': mana.Black,
		'R': mana.Red,
		'G': mana.Green,
	} {
		got, ok := mana.ColorFromLetter(letter)
		if !ok || got != want {
			t.Errorf("ColorFromLetter(%q) = %v, %v; want %v, true", letter, got, ok, want)
		}
	}
	if _, ok := mana.ColorFromLetter('S'); ok {
		t.Error("ColorFromLetter('S') reported a colour; snow is not one")
	}
}

func TestCostWithReducedGenericPrintsTheReduction(t *testing.T) {
	t.Parallel()

	// Only reachable once an effect has reduced a cost below zero, which needs
	// an X spell for the reduction to mean anything. Pinned here because the
	// printed form is what the oracle diff compares.
	cost, err := mana.Parse("X -3")
	if err != nil {
		t.Fatalf("Parse failed: %v", err)
	}
	if got, want := cost.String(), "{X} -3"; got != want {
		t.Errorf("Parse(%q).String() = %q, want %q", "X -3", got, want)
	}
	if got, want := cost.CMC(), -3; got != want {
		t.Errorf("CMC() = %d, want %d", got, want)
	}
}
