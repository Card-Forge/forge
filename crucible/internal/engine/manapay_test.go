package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// A two-colour hybrid shard pays from either color the pool has -- CR
// 601.2h, resolved by asking the controller rather than picking one
// automatically.
func TestPayManaCostResolvesTwoColorHybridFromController(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	c := engine.NewScriptedController()
	c.QueueHybridManaColor(mana.Blue)

	if !g.PayManaCost(p, mana.MustParse("W/U"), c) {
		t.Fatal("PayManaCost failed paying {W/U} with a blue mana in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}

// The controller's answer decides which color is spent -- choosing white
// when the pool holds white but not blue must fail, not silently fall back
// to whatever the pool actually has.
func TestPayManaCostFailsWhenTheChosenColorIsNotInThePool(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.White, 1)
	c := engine.NewScriptedController()
	c.QueueHybridManaColor(mana.Blue)

	if g.PayManaCost(p, mana.MustParse("W/U"), c) {
		t.Fatal("PayManaCost succeeded choosing blue with only white in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 1 {
		t.Errorf("pool total after a failed payment = %d, want 1 (unchanged)", got)
	}
}

// A cost mixing a plain shard and a hybrid shard resolves the hybrid and
// pays the plain shard the same call -- PayManaCost is one payment, not a
// per-shard loop the caller has to drive.
func TestPayManaCostResolvesHybridAlongsidePlainShards(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.Red, 1)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	c := engine.NewScriptedController()
	c.QueueHybridManaColor(mana.Green)

	if !g.PayManaCost(p, mana.MustParse("R G/W"), c) {
		t.Fatal("PayManaCost failed paying {R}{G/W} with red and green in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}

// A monocoloured hybrid ({2/W}) resolved to color spends exactly the one
// colored pip, not two generic on top of it -- CR 601.2h's choice is between
// the two payment shapes, not both at once.
func TestPayManaCostResolvesMonocoloredHybridWithColor(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.White, 1)
	c := engine.NewScriptedController()
	c.QueuePayMonocoloredHybrid(true)

	if !g.PayManaCost(p, mana.MustParse("2/W"), c) {
		t.Fatal("PayManaCost failed paying {2/W} with color chosen and a white mana in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}

// The same shard resolved to generic instead spends its CMC (2) worth of
// any mana and none of the color -- the controller's answer picks the
// payment shape, not just a tiebreak between two pools that both work.
func TestPayManaCostResolvesMonocoloredHybridWithGeneric(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.AddColorless(2)
	c := engine.NewScriptedController()
	c.QueuePayMonocoloredHybrid(false)
	c.QueuePayGeneric(mana.ShardC)
	c.QueuePayGeneric(mana.ShardC)

	if !g.PayManaCost(p, mana.MustParse("2/W"), c) {
		t.Fatal("PayManaCost failed paying {2/W} with generic chosen and two colorless mana in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}

// Choosing color with none in the pool fails outright -- PayManaCost does
// not fall back to the generic shape the controller declined.
func TestPayManaCostFailsWhenMonocoloredHybridChosenColorNotInPool(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.AddColorless(2)
	c := engine.NewScriptedController()
	c.QueuePayMonocoloredHybrid(true)

	if g.PayManaCost(p, mana.MustParse("2/W"), c) {
		t.Fatal("PayManaCost succeeded choosing color with no white in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 2 {
		t.Errorf("pool total after a failed payment = %d, want 2 (unchanged)", got)
	}
}

// A colourless hybrid ({C/W}) resolved to color spends the colored pip, not
// {C} -- the same substitution shape as a two-colour hybrid, just with only
// one color on offer.
func TestPayManaCostResolvesColorlessHybridWithColor(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.White, 1)
	c := engine.NewScriptedController()
	c.QueuePayColorlessHybrid(true)

	if !g.PayManaCost(p, mana.MustParse("C/W"), c) {
		t.Fatal("PayManaCost failed paying {C/W} with color chosen and a white mana in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}

// The same shard resolved to colorless spends {C}, not generic -- a
// colourless hybrid's other side is a specific mana type
// (ChoosePayColorlessHybrid's own doc comment), unlike a monocoloured
// hybrid's generic side.
func TestPayManaCostResolvesColorlessHybridWithColorless(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.AddColorless(1)
	c := engine.NewScriptedController()
	c.QueuePayColorlessHybrid(false)

	if !g.PayManaCost(p, mana.MustParse("C/W"), c) {
		t.Fatal("PayManaCost failed paying {C/W} with colorless chosen and one colorless mana in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}

// Choosing color with none in the pool fails outright -- PayManaCost does
// not fall back to {C} the controller declined, even if {C} is available.
func TestPayManaCostFailsWhenColorlessHybridChosenColorNotInPool(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.AddColorless(1)
	c := engine.NewScriptedController()
	c.QueuePayColorlessHybrid(true)

	if g.PayManaCost(p, mana.MustParse("C/W"), c) {
		t.Fatal("PayManaCost succeeded choosing color with no white in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 1 {
		t.Errorf("pool total after a failed payment = %d, want 1 (unchanged)", got)
	}
}

// A Phyrexian shard ({W/P}) resolved to color spends the colored pip and
// costs no life.
func TestPayManaCostResolvesPhyrexianWithColor(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).Life = 20
	g.Player(p).ManaPool.Add(mana.White, 1)
	c := engine.NewScriptedController()
	c.QueuePayPhyrexian(true)

	if !g.PayManaCost(p, mana.MustParse("W/P"), c) {
		t.Fatal("PayManaCost failed paying {W/P} with color chosen and a white mana in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("life = %d, want unchanged at 20 -- color was chosen, not life", g.Player(p).Life)
	}
}

// The same shard resolved to life costs 2 life and spends no mana at all,
// and fires LifeChanged the same way combat damage does (CounterChanged's
// own "wired at every real call site" discipline, the removed port log's "Events,
// wired").
func TestPayManaCostResolvesPhyrexianWithLife(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).Life = 20
	c := engine.NewScriptedController()
	c.QueuePayPhyrexian(false)
	var sink recordingSink
	g.SetSink(&sink)

	if !g.PayManaCost(p, mana.MustParse("W/P"), c) {
		t.Fatal("PayManaCost failed paying {W/P} with life chosen and no mana in the pool")
	}
	if g.Player(p).Life != 18 {
		t.Errorf("life = %d, want 18 after paying 2 life", g.Player(p).Life)
	}
	var sawLife bool
	for _, e := range sink.events {
		if e.Kind == engine.LifeChanged && e.Amount == -2 && e.Target == engine.PlayerEntity(p) {
			sawLife = true
		}
	}
	if !sawLife {
		t.Errorf("events = %v, want a LifeChanged for -2 targeting %v", sink.events, p)
	}
}

// Choosing color with none in the pool fails outright -- PayManaCost does
// not fall back to the 2-life option the controller declined.
func TestPayManaCostFailsWhenPhyrexianChosenColorNotInPool(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).Life = 20
	c := engine.NewScriptedController()
	c.QueuePayPhyrexian(true)

	if g.PayManaCost(p, mana.MustParse("W/P"), c) {
		t.Fatal("PayManaCost succeeded choosing color with no white in the pool")
	}
	if g.Player(p).Life != 20 {
		t.Errorf("life = %d, want unchanged at 20 -- color was chosen, so no life should be lost either", g.Player(p).Life)
	}
}

// A Phyrexian shard resolved to life alongside another shard the pool
// cannot cover must not cost life at all -- Pay's own "never spends part of
// a cost it cannot finish paying" guarantee extends to the life side of a
// payment PayManaCost adds on top.
func TestPayManaCostDoesNotChargeLifeWhenAnUnrelatedShardFails(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).Life = 20
	c := engine.NewScriptedController()
	c.QueuePayPhyrexian(false)

	if g.PayManaCost(p, mana.MustParse("W/P U"), c) {
		t.Fatal("PayManaCost succeeded with no blue mana in the pool for the plain {U} shard")
	}
	if g.Player(p).Life != 20 {
		t.Errorf("life = %d, want unchanged at 20 -- the payment failed on the other shard", g.Player(p).Life)
	}
}

// A hybrid Phyrexian shard ({B/G/P}) resolved to either of its two colours
// spends that colour's pip and costs no life.
func TestPayManaCostResolvesHybridPhyrexianWithEitherColor(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).Life = 20
	g.Player(p).ManaPool.Add(mana.Green, 1)
	c := engine.NewScriptedController()
	c.QueuePayHybridPhyrexian(mana.Green)

	if !g.PayManaCost(p, mana.MustParse("B/G/P"), c) {
		t.Fatal("PayManaCost failed paying {B/G/P} with green chosen and a green mana in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("life = %d, want unchanged at 20 -- a colour was chosen, not life", g.Player(p).Life)
	}
}

// The same shard resolved to life (the zero mana.Colors) costs 2 life and
// spends no mana at all -- the same "zero means life" convention as the
// return value's own doc comment.
func TestPayManaCostResolvesHybridPhyrexianWithLife(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).Life = 20
	c := engine.NewScriptedController()
	c.QueuePayHybridPhyrexian(0)

	if !g.PayManaCost(p, mana.MustParse("B/G/P"), c) {
		t.Fatal("PayManaCost failed paying {B/G/P} with life chosen and no mana in the pool")
	}
	if g.Player(p).Life != 18 {
		t.Errorf("life = %d, want 18 after paying 2 life", g.Player(p).Life)
	}
}

// Choosing a colour with none of it in the pool fails outright -- no
// fallback to the other colour or to life.
func TestPayManaCostFailsWhenHybridPhyrexianChosenColorNotInPool(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).Life = 20
	g.Player(p).ManaPool.Add(mana.Black, 1)
	c := engine.NewScriptedController()
	c.QueuePayHybridPhyrexian(mana.Green)

	if g.PayManaCost(p, mana.MustParse("B/G/P"), c) {
		t.Fatal("PayManaCost succeeded choosing green with only black in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 1 {
		t.Errorf("pool total after a failed payment = %d, want 1 (unchanged)", got)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("life = %d, want unchanged at 20", g.Player(p).Life)
	}
}

// A plain generic cost is now a real per-unit choice (CR 106.6), not the
// pool's own fixed colorless-first fallback order (Pool.Pay's doc comment,
// mana.go) -- choosing white leaves the colorless mana untouched.
func TestPayManaCostResolvesGenericFromControllerChoice(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.White, 1)
	g.Player(p).ManaPool.AddColorless(1)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardW)

	if !g.PayManaCost(p, mana.MustParse("1"), c) {
		t.Fatal("PayManaCost failed paying {1} with white chosen and both a white and a colorless mana in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 1 {
		t.Errorf("pool total after payment = %d, want 1 (the untouched colorless mana)", got)
	}
}

// Choosing a mana type the pool does not have fails outright -- PayManaCost
// does not fall back to whatever else the pool holds.
func TestPayManaCostFailsWhenChosenGenericShardNotInPool(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.White, 1)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardU)

	if g.PayManaCost(p, mana.MustParse("1"), c) {
		t.Fatal("PayManaCost succeeded choosing blue for generic with no blue in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 1 {
		t.Errorf("pool total after a failed payment = %d, want 1 (unchanged)", got)
	}
}

// A cost mixing a colored pip and more than one unit of generic pays the pip
// first, then asks ChoosePayGeneric once per unit of generic still owed --
// each unit is its own independent choice, not one combined answer for the
// whole amount.
func TestPayManaCostResolvesMultipleGenericUnitsIndependently(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.White, 1)
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	g.Player(p).ManaPool.Add(mana.Black, 1)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardU)
	c.QueuePayGeneric(mana.ShardB)

	if !g.PayManaCost(p, mana.MustParse("2 W"), c) {
		t.Fatal("PayManaCost failed paying {2}{W} with white for the pip and blue+black chosen for generic")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}

// {X R}: ChoosePayX answers X once, folded into the generic amount before
// the generic-resolution loop asks ChoosePayGeneric once per unit owed --
// the same "X announced ahead of everything else" order CR 601.2b puts it
// in.
func TestPayManaCostResolvesXFromControllerChoice(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.Red, 1)
	g.Player(p).ManaPool.Add(mana.White, 3)
	c := engine.NewScriptedController()
	c.QueuePayX(3)
	c.QueuePayGeneric(mana.ShardW)
	c.QueuePayGeneric(mana.ShardW)
	c.QueuePayGeneric(mana.ShardW)

	if !g.PayManaCost(p, mana.MustParse("X R"), c) {
		t.Fatal("PayManaCost failed paying {X}{R} with X=3 against a pool of 1 red and 3 white")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}

// {X X}: two X symbols in the same cost stand for one chosen value each,
// not two independent ones -- ChoosePayX is asked exactly once regardless
// of how many X symbols the cost carries (CR 601.2b/107.3f).
func TestPayManaCostFoldsEveryXSymbolToTheSameChosenValue(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.White, 4)
	c := engine.NewScriptedController()
	c.QueuePayX(2)
	c.QueuePayGeneric(mana.ShardW)
	c.QueuePayGeneric(mana.ShardW)
	c.QueuePayGeneric(mana.ShardW)
	c.QueuePayGeneric(mana.ShardW)

	if !g.PayManaCost(p, mana.MustParse("X X"), c) {
		t.Fatal("PayManaCost failed paying {X}{X} with X=2 (4 generic total) against a pool of 4 white")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}

// X=0 is a legal answer (CR 601.2b's "non-negative integer") and asks
// ChoosePayGeneric zero times -- paying {X}{R} with X=0 is exactly paying
// {R}.
func TestPayManaCostXZeroAsksNoGeneric(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.Red, 1)
	c := engine.NewScriptedController()
	c.QueuePayX(0)

	if !g.PayManaCost(p, mana.MustParse("X R"), c) {
		t.Fatal("PayManaCost failed paying {X}{R} with X=0 against a pool of 1 red")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}

// A negative X is not a legal answer (CR 601.2b) and is not re-checked by
// asking Pay to fail on it -- PayManaCost declines the payment itself,
// before ChoosePayGeneric is ever asked or the pool is touched.
func TestPayManaCostFailsWhenXIsNegative(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.Red, 1)
	c := engine.NewScriptedController()
	c.QueuePayX(-1)

	if g.PayManaCost(p, mana.MustParse("X R"), c) {
		t.Fatal("PayManaCost succeeded with a negative X")
	}
	if got := g.Player(p).ManaPool.Total(); got != 1 {
		t.Errorf("pool total after a failed payment = %d, want 1 (unchanged)", got)
	}
}

// {S}: ChoosePaySnow answers which color's snow mana pays the symbol --
// plain mana of that color, however much is floating, does not substitute.
func TestPayManaCostResolvesSnowFromControllerChoice(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.AddSnow(mana.White, 1)
	c := engine.NewScriptedController()
	c.QueuePaySnow(mana.ShardW)

	if !g.PayManaCost(p, mana.MustParse("S"), c) {
		t.Fatal("PayManaCost failed paying {S} with one snow white in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}

// {S}{S}: two snow symbols in the same cost are two independent questions,
// not one value reused -- unlike X, each can be paid with a different
// color's snow mana.
func TestPayManaCostResolvesTwoSnowSymbolsIndependently(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.AddSnow(mana.White, 1)
	g.Player(p).ManaPool.AddSnow(mana.Blue, 1)
	c := engine.NewScriptedController()
	c.QueuePaySnow(mana.ShardW)
	c.QueuePaySnow(mana.ShardU)

	if !g.PayManaCost(p, mana.MustParse("S S"), c) {
		t.Fatal("PayManaCost failed paying {S}{S} with snow white and snow blue in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}

// Plain white does not substitute for {S}'s own requirement, however much
// of it is floating -- choosing white when the pool holds only plain white
// fails, the same as choosing a hybrid color the pool does not have.
func TestPayManaCostFailsWhenChosenSnowColorHasNoSnowMana(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.Add(mana.White, 5)
	c := engine.NewScriptedController()
	c.QueuePaySnow(mana.ShardW)

	if g.PayManaCost(p, mana.MustParse("S"), c) {
		t.Fatal("PayManaCost succeeded paying {S} with only plain white in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 5 {
		t.Errorf("pool total after a failed payment = %d, want 5 (unchanged)", got)
	}
}

// A cost mixing a snow symbol and a plain pip pays both from the same
// payment: the snow answer spends only the snow bucket, the plain pip
// spends (or falls back to) the ordinary one.
func TestPayManaCostResolvesSnowAlongsidePlainShards(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.Player(p).ManaPool.AddSnow(mana.White, 1)
	g.Player(p).ManaPool.Add(mana.Red, 1)
	c := engine.NewScriptedController()
	c.QueuePaySnow(mana.ShardW)

	if !g.PayManaCost(p, mana.MustParse("S R"), c) {
		t.Fatal("PayManaCost failed paying {S}{R} with snow white and plain red in the pool")
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("pool total after payment = %d, want 0", got)
	}
}
