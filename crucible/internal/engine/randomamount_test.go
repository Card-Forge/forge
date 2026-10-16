package engine_test

import (
	"slices"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Count$Random.<min>.<max> (AbilityUtils.java:2782-2787) draws a number from
// min to max, both ends included, on every read, from the game's seeded
// generator.

// randomGains resolves "gain X life" n times in one game, X a Count$Random
// body, and answers each gain in order.
func randomGains(t *testing.T, x string, n int, svars ...string) []int {
	t.Helper()
	g, p, _ := newTwoPlayerGame(t)
	var gains []int
	for range n {
		before := g.Player(p).Life
		resolveLine(t, g, p, engine.NewScriptedController(), "DB$ GainLife | Defined$ You | LifeAmount$ X",
			append([]string{"X", x}, svars...)...)
		gains = append(gains, g.Player(p).Life-before)
	}
	return gains
}

func TestCountRandomDrawsWithinItsBoundsOnEveryRead(t *testing.T) {
	t.Parallel()

	gains := randomGains(t, "Count$Random.0.2", 40)
	seen := map[int]bool{}
	for _, n := range gains {
		if n < 0 || n > 2 {
			t.Fatalf("gained %d, want 0 to 2", n)
		}
		seen[n] = true
	}
	if len(seen) != 3 {
		t.Errorf("40 reads drew %v, want each of 0, 1 and 2", gains)
	}
	if again := randomGains(t, "Count$Random.0.2", 40); !slices.Equal(gains, again) {
		t.Errorf("the same seed drew %v then %v, want the same sequence", gains, again)
	}
	for _, n := range randomGains(t, "Count$Random.3.3", 3) {
		if n != 3 {
			t.Errorf("Count$Random.3.3 gained %d, want 3", n)
		}
	}
	// A bound is itself an amount: here the SVar Y, two cards in hand.
	setupHand := func(g *engine.Game, p, _ engine.PlayerID) { addN(t, g, p, engine.Hand, 2, "Artifact") }
	if got := gainedFrom(t, setupHand, "Count$Random.Y.Y", "Y", "Count$CardsInYourHand"); got != 2 {
		t.Errorf("Count$Random.Y.Y gained %d, want 2", got)
	}
	if got := gainedFrom(t, nil, "Count$Random.1.3/Times.10"); got < 10 || got > 30 || got%10 != 0 {
		t.Errorf("Count$Random.1.3/Times.10 gained %d, want 10, 20 or 30", got)
	}
	for _, bad := range []string{"Count$Random.2.1", "Count$Random.0", "Count$Random.0.Nope"} {
		if err := gainErr(t, nil, bad); err == nil {
			t.Errorf("%s resolved, want an error", bad)
		}
	}
}

// Ashuza's Breath draws once per creature: each takes 0 to 2 damage.
func TestAshuzasBreathDrawsPerCreature(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	var creatures []engine.CardID
	for range 6 {
		creatures = append(creatures, g.NewCard(creatureDefPT(t, "5", "5"), other, engine.Battlefield))
	}
	spell := g.NewCard(corpusCard(t, "Ashuza's Breath"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Red, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardR)
	castThenResolve(t, g, p, spell, c)
	total := 0
	for _, id := range creatures {
		d := g.Card(id).Damage.Marked
		if d < 0 || d > 2 {
			t.Errorf("a creature took %d damage, want 0 to 2", d)
		}
		total += d
	}
	if total == 0 {
		t.Error("six creatures took no damage between them: the draw looks constant")
	}
}
