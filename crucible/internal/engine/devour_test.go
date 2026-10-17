package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Devour (CR 702.82, CardFactoryUtil.java:2211-2238): as the permanent enters,
// its controller may sacrifice any number of other creatures (or the devour
// type); it enters with N +1/+1 counters for each, and remembers what it
// devoured for Count$Devoured, HasDevoured and Mode$ Devoured.

// castDevourer casts the corpus card name for p with enough green mana,
// confirming the devour and choosing food (nil declines).
func castDevourer(t *testing.T, g *engine.Game, p engine.PlayerID, name string, generic int, food []engine.CardID, confirm bool) engine.CardID {
	t.Helper()
	card := g.NewCard(corpusCard(t, name), p, engine.Hand)
	// The green pays the generic part and a green pip; one of each other
	// color covers the card's other pips.
	g.Player(p).ManaPool.Add(mana.Green, generic+1)
	for _, col := range []mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red} {
		g.Player(p).ManaPool.Add(col, 1)
	}
	c := engine.NewScriptedController()
	for range generic {
		c.QueuePayGeneric(mana.ShardG)
	}
	c.QueueConfirmEffect(confirm)
	if confirm {
		c.QueueCardChoice(food)
	}
	castThenResolve(t, g, p, card, c)
	if err := g.TakePendingError(); err != nil {
		t.Fatalf("pending error: %v", err)
	}
	return card
}

// Skullmulcher (Devour 1) enters with a counter per creature devoured and
// draws that many: the player may devour fewer than all (Java's minimum is 0
// under Optional$), or decline.
func TestDevourSacrificesAnyNumberAndCountsThem(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name    string
		confirm bool
		eat     int
	}{
		{"both", true, 2},
		{"one of two", true, 1},
		{"none chosen", true, 0},
		{"declined", false, 0},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			g.SetTurnState(1, p, engine.Main1)
			food := addN(t, g, p, engine.Battlefield, 2, "Creature Elf")
			addN(t, g, p, engine.Library, 5, "Artifact")
			hand := len(g.Zone(engine.Hand, p).Cards())
			mulcher := castDevourer(t, g, p, "Skullmulcher", 4, food[:tc.eat], tc.confirm)
			if got := g.Card(mulcher).Counters.Count(engine.P1P1); got != tc.eat {
				t.Errorf("%d +1/+1 counters, want %d", got, tc.eat)
			}
			if got := len(g.Zone(engine.Hand, p).Cards()) - hand; got != tc.eat {
				t.Errorf("drew %d, want %d", got, tc.eat)
			}
			for i, id := range food {
				want := engine.Battlefield
				if i < tc.eat {
					want = engine.Graveyard
				}
				if got := g.Card(id).Zone; got != want {
					t.Errorf("food %d in %v, want %v", i, got, want)
				}
			}
		})
	}
}

// Devour 2 puts two counters per creature; Count$Devoured <valid> counts only
// the devoured permanents the valid string names (Voracious Dragon's Goblins).
func TestDevourMultiplierAndDevouredValid(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	food := addN(t, g, p, engine.Battlefield, 2, "Creature Elf")
	chomper := castDevourer(t, g, p, "Marrow Chomper", 3, food, true)
	if got := g.Card(chomper).Counters.Count(engine.P1P1); got != 4 {
		t.Errorf("Marrow Chomper has %d counters, want 4: Devour 2, two creatures", got)
	}
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life %d, want 24: 2 life for each creature devoured", got)
	}

	g2, p2, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g2.SetTurnState(1, p2, engine.Main1)
	goblin := g2.NewCard(corpusCard(t, "Goblin Piker"), p2, engine.Battlefield)
	elf := addN(t, g2, p2, engine.Battlefield, 1, "Creature Elf")[0]
	dragon := g2.NewCard(corpusCard(t, "Voracious Dragon"), p2, engine.Hand)
	g2.Player(p2).ManaPool.Add(mana.Red, 5)
	c := engine.NewScriptedController()
	for range 3 {
		c.QueuePayGeneric(mana.ShardR)
	}
	c.QueueConfirmEffect(true)
	c.QueueCardChoice([]engine.CardID{goblin, elf})
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	castThenResolve(t, g2, p2, dragon, c)
	if err := g2.TakePendingError(); err != nil {
		t.Fatalf("pending error: %v", err)
	}
	if got := g2.Player(other).Life; got != 18 {
		t.Errorf("opponent at %d, want 18: twice the one Goblin devoured", got)
	}
}

// Hellkite Hatchling has flying and trample only once it devoured something
// (HasDevoured); the record ends when it leaves the battlefield.
func TestHasDevouredGatesAStatic(t *testing.T) {
	t.Parallel()

	for _, eat := range []bool{true, false} {
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		g.SetTurnState(1, p, engine.Main1)
		food := addN(t, g, p, engine.Battlefield, 1, "Creature Elf")
		var pick []engine.CardID
		if eat {
			pick = food
		}
		hatchling := castDevourer(t, g, p, "Hellkite Hatchling", 2, pick, true)
		engine.CheckStateBasedActions(g, engine.NewScriptedController())
		if got := g.Card(hatchling).HasKeyword("Flying"); got != eat {
			t.Errorf("devoured=%v: flying = %v", eat, got)
		}
		if !eat {
			continue
		}
		g.Move(hatchling, engine.Hand, p)
		g.Move(hatchling, engine.Battlefield, p)
		engine.CheckStateBasedActions(g, engine.NewScriptedController())
		if g.Card(hatchling).HasKeyword("Flying") {
			t.Error("a Hatchling that left and came back still remembers what it devoured")
		}
	}
}

// Kresh the Bloodbraided Avatar (Mode$ Devoured, TriggerDevoured) makes an
// X/X Ooze for each creature devoured, X its power as it last was.
func TestDevouredTriggerReadsTheFood(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.NewCard(corpusCard(t, "Kresh the Bloodbraided Avatar"), p, engine.Command)
	addN(t, g, p, engine.Library, 2, "Artifact")
	food := g.NewCard(creatureDefPT(t, "3", "1"), p, engine.Battlefield)
	castDevourer(t, g, p, "Skullmulcher", 4, []engine.CardID{food}, true)
	var oozes []engine.CardID
	for _, id := range g.Zone(engine.Battlefield, p).Cards() {
		if strings.Contains(g.Card(id).Type().String(), "Ooze") {
			oozes = append(oozes, id)
		}
	}
	if len(oozes) != 1 {
		t.Fatalf("%d Oozes, want 1", len(oozes))
	}
	if power, _ := g.Card(oozes[0]).Power(); power != 3 {
		t.Errorf("Ooze power %d, want 3: the devoured creature's", power)
	}
}

// A devour answer outside the offer is an error, not a sacrifice.
func TestDevourRejectsABadAnswer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	addN(t, g, p, engine.Battlefield, 1, "Creature Elf")
	theirs := addN(t, g, other, engine.Battlefield, 1, "Creature Elf")
	card := g.NewCard(corpusCard(t, "Skullmulcher"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 5)
	c := engine.NewScriptedController()
	for range 4 {
		c.QueuePayGeneric(mana.ShardG)
	}
	c.QueueConfirmEffect(true)
	c.QueueCardChoice(theirs)
	if !g.CastSpell(p, card, c) {
		t.Fatal("cast failed")
	}
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil {
		err = g.TakePendingError()
	}
	if err == nil || !strings.Contains(err.Error(), "optional answer") {
		t.Errorf("err = %v, want the devour answer refused", err)
	}
	if got := g.Card(theirs[0]).Zone; got != engine.Battlefield {
		t.Errorf("the opponent's creature in %v, want Battlefield", got)
	}
}
