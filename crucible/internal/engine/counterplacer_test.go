package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// A counter is put on a card by a player: Card.addCounterInternal's `Player
// source` (Card.java:1761), AbilityKey.Source of the CounterAdded and
// CounterAddedOnce triggers (Card.java:1805). Their ValidSource$ -- "You" on
// every corpus line ("Whenever you put a +1/+1 counter on a creature") --
// matches that player, never the card the counters came from. PutCounter's
// placer is its activator unless Placer$ names another
// (CountersPutEffect.java:583-586); PutCounterAll's may be each card's own
// controller or owner (CountersPutAllEffect.java:65-84).

// addYouPlaceWatcher gives owner an Enchantment that gains its controller 1
// life for each placement of +1/+1 counters that controller puts on a
// creature.
func addYouPlaceWatcher(t *testing.T, g *engine.Game, owner engine.PlayerID) {
	t.Helper()
	g.NewCard(triggerWatcherDef(t, "Test Placer Watcher",
		"Mode$ CounterAddedOnce | ValidCard$ Creature | ValidSource$ You | CounterType$ P1P1 | TriggerZones$ Battlefield | Execute$ TrigGain",
		"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), owner, engine.Battlefield)
}

// TestCounterAddedValidSourceMatchesThePlacer proves ValidSource$ You reads
// the player putting the counters: p's watcher fires when p's ability puts the
// counter on p's creature, and not when other's does.
func TestCounterAddedValidSourceMatchesThePlacer(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name     string
		byOther  bool
		wantLife int
	}{
		{"p places", false, 21},
		{"other places", true, 20},
	} {
		g, p, other := newTwoPlayerGame(t)
		addYouPlaceWatcher(t, g, p)
		bear := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		who := p
		if tc.byOther {
			who = other
		}
		_, err := resolveNow(t, g, who, engine.NewScriptedController(), entities(bear),
			"DB$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | CounterNum$ 1")
		if err != nil {
			t.Fatalf("%s: ResolveStack: %v", tc.name, err)
		}
		if n := g.Card(bear).Counters.Count(engine.P1P1); n != 1 {
			t.Fatalf("%s: bear counters = %d, want 1", tc.name, n)
		}
		if got := g.Player(p).Life; got != tc.wantLife {
			t.Errorf("%s: p life = %d, want %d", tc.name, got, tc.wantLife)
		}
	}
}

// TestPutCounterPlacerNamesWhoPuts proves Placer$ (Quicksilver Fountain's and
// Alexios' "that player puts"): p resolves the line, other places, so other's
// watcher fires and p's does not.
func TestPutCounterPlacerNamesWhoPuts(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	addYouPlaceWatcher(t, g, p)
	addYouPlaceWatcher(t, g, other)
	bear := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), entities(bear),
		"DB$ PutCounter | ValidTgts$ Creature | Placer$ Opponent | CounterType$ P1P1 | CounterNum$ 2")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(bear).Counters.Count(engine.P1P1); n != 2 {
		t.Fatalf("bear counters = %d, want 2", n)
	}
	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{20, 21}; got != want {
		t.Errorf("life (p, other) = %v, want %v -- the opponent placed the counters", got, want)
	}
}

// TestPutCounterPlacerItCannotNameFailsBeforePlacing proves Placer$ naming
// nobody (Java's `.get(0)` on an empty list) or a key the trigger did not
// record fails the line before any counter is placed (GO-7).
func TestPutCounterPlacerItCannotNameFailsBeforePlacing(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ placer, want string }{
		{"TargetedPlayer", "names no player"},
		{"TriggeredPlayer", "Placer$"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		_, err := castETBChain(t, g, p, etbChainDef(t, "Test Placer",
			"DB$ PutCounter | Defined$ Self | Placer$ "+tc.placer+" | CounterType$ P1P1 | CounterNum$ 1"), engine.NewScriptedController())
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("Placer$ %s: err = %v, want %q", tc.placer, err, tc.want)
		}
		for _, id := range g.Zone(engine.Battlefield, p).Cards() {
			if n := g.Card(id).Counters.Count(engine.P1P1); n != 0 {
				t.Errorf("Placer$ %s: a counter was placed", tc.placer)
			}
		}
	}
}

// TestPutCounterAllPlacerPerCard proves PutCounterAll's per-card placer: under
// Placer$ Controller each creature's controller puts its counter, under
// Placer$ Owner each creature's owner does (a creature p borrowed is other's
// to place on), and with no Placer$ the activator places every one.
func TestPutCounterAllPlacerPerCard(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, placer string
		want         [2]int // p, other life: one point per placement each made
	}{
		// The non-Elf creatures: p's mine and the borrowed one (other's
		// own), other's theirs. The abilities' hosts are Elves.
		{"activator", "", [2]int{23, 20}},
		{"controller", " | Placer$ Controller", [2]int{22, 21}},
		{"owner", " | Placer$ Owner", [2]int{21, 22}},
		{"defined", " | Placer$ Opponent", [2]int{20, 23}},
	} {
		g, p, other := newTwoPlayerGame(t)
		addYouPlaceWatcher(t, g, p)
		addYouPlaceWatcher(t, g, other)
		g.NewCard(scriptDef(t, "Test Beast", "Creature"), p, engine.Battlefield)
		g.NewCard(scriptDef(t, "Test Beast", "Creature"), other, engine.Battlefield)
		borrowed := g.NewCard(scriptDef(t, "Test Beast", "Creature"), other, engine.Battlefield)
		stealCreature(t, g, p, other, borrowed)

		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
			"DB$ PutCounterAll | ValidCards$ Creature.nonElf | CounterType$ P1P1 | CounterNum$ 1"+tc.placer)
		if err != nil {
			t.Fatalf("%s: ResolveStack: %v", tc.name, err)
		}
		if got := [2]int{g.Player(p).Life, g.Player(other).Life}; got != tc.want {
			t.Errorf("%s: life (p, other) = %v, want %v", tc.name, got, tc.want)
		}
	}
}

// TestPutCounterAllPlacerItCannotNameFails is
// TestPutCounterPlacerItCannotNameFailsBeforePlacing for PutCounterAll.
func TestPutCounterAllPlacerItCannotNameFails(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ placer, want string }{
		{"TargetedPlayer", "names no player"},
		{"TriggeredPlayer", "Placer$"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		bear := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		_, err := castETBChain(t, g, p, etbChainDef(t, "Test Placer All",
			"DB$ PutCounterAll | ValidCards$ Creature | Placer$ "+tc.placer+" | CounterType$ P1P1 | CounterNum$ 1"), engine.NewScriptedController())
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("Placer$ %s: err = %v, want %q", tc.placer, err, tc.want)
		}
		if n := g.Card(bear).Counters.Count(engine.P1P1); n != 0 {
			t.Errorf("Placer$ %s: a counter was placed", tc.placer)
		}
	}
}
