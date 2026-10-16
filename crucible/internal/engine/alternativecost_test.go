package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// A card's own Mode$ AlternativeCost static (StaticAbilityAlternativeCost,
// CR 118.9) is a second way to cast it from the hand: its cost in place of
// the mana cost, recorded as OptionalCost.AltCost for Count$AltCost.

// Bringer of Green Zenith's Twilight cast for {W}{U}{B}{R}{G} has X 5 and
// enters with twice that many counters; cast for its mana cost with X 2, it
// enters with two.
func TestAlternativeCostRecordsTheCast(t *testing.T) {
	t.Parallel()

	for _, alt := range []bool{true, false} {
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		g.SetTurnState(1, p, engine.Main1)
		bringer := g.NewCard(corpusCard(t, "Bringer of Green Zenith's Twilight"), p, engine.Hand)
		c := engine.NewScriptedController()
		want := 2
		if alt {
			for _, col := range []mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red, mana.Green} {
				g.Player(p).ManaPool.Add(col, 1)
			}
			c.QueueOption(1)
			want = 10
		} else {
			g.Player(p).ManaPool.Add(mana.Green, 4)
			c.QueueOption(0)
			c.QueuePayX(2)
			c.QueuePayGeneric(mana.ShardG)
			c.QueuePayGeneric(mana.ShardG)
		}
		castThenResolve(t, g, p, bringer, c)
		if err := g.TakePendingError(); err != nil {
			t.Fatalf("alt=%v: pending error: %v", alt, err)
		}
		if got := g.Card(bringer).Counters.Count(engine.P1P1); got != want {
			t.Errorf("alt=%v: %d +1/+1 counters, want %d", alt, got, want)
		}
		if got := g.Player(p).ManaPool.Total(); got != 0 {
			t.Errorf("alt=%v: %d mana left, want 0", alt, got)
		}
	}
}

// Fireblast's alternative cost is two Mountains sacrificed: offered only with
// two to sacrifice, and paid once the targets are chosen.
func TestAlternativeCostPaysItsOtherParts(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	first := g.NewCard(corpusCard(t, "Mountain"), p, engine.Battlefield)
	blast := g.NewCard(corpusCard(t, "Fireblast"), p, engine.Hand)
	// One Mountain: only the mana cost is offered (no option asked), and
	// there is no mana to pay it.
	c0 := engine.NewScriptedController()
	c0.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	for range 4 {
		c0.QueuePayGeneric(mana.ShardR)
	}
	if g.CastSpell(p, blast, c0) {
		t.Fatal("cast Fireblast with one Mountain and no mana")
	}
	second := g.NewCard(corpusCard(t, "Mountain"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueOption(1)
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueSacrificeChoice([]engine.CardID{first, second})
	castThenResolve(t, g, p, blast, c)
	if got := g.Player(other).Life; got != 16 {
		t.Errorf("opponent at %d, want 16", got)
	}
	for _, id := range []engine.CardID{first, second} {
		if got := g.Card(id).Zone; got != engine.Graveyard {
			t.Errorf("Mountain in %v, want Graveyard", got)
		}
	}
}

// Snuff Out's alternative cost needs a Swamp (IsPresent$): without one only
// the mana cost is offered.
func TestAlternativeCostReadsItsCondition(t *testing.T) {
	t.Parallel()

	for _, swamp := range []bool{true, false} {
		g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
		g.SetTurnState(1, p, engine.Main1)
		if swamp {
			g.NewCard(corpusCard(t, "Swamp"), p, engine.Battlefield)
		}
		victim := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Battlefield)
		snuff := g.NewCard(corpusCard(t, "Snuff Out"), p, engine.Hand)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
		if swamp {
			c.QueueOption(1)
		} else {
			g.Player(p).ManaPool.Add(mana.Black, 4)
			for range 3 {
				c.QueuePayGeneric(mana.ShardB)
			}
		}
		castThenResolve(t, g, p, snuff, c)
		if got := g.Card(victim).Zone; got != engine.Graveyard {
			t.Errorf("swamp=%v: the Bears in %v, want Graveyard", swamp, got)
		}
		wantLife := 20
		if swamp {
			wantLife = 16
		}
		if got := g.Player(p).Life; got != wantLife {
			t.Errorf("swamp=%v: life %d, want %d", swamp, got, wantLife)
		}
	}
}

// Force of Will's alternative cost exiles another blue card from the hand
// and pays 1 life; the spell never exiles itself.
func TestAlternativeCostExilesFromHand(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, other, engine.Main1)
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Hand)
	g.Player(other).ManaPool.Add(mana.Green, 2)
	oc := engine.NewScriptedController()
	oc.QueuePayGeneric(mana.ShardG)
	if !g.CastSpell(other, bears, oc) {
		t.Fatal("the opponent could not cast Grizzly Bears")
	}
	force := g.NewCard(corpusCard(t, "Force of Will"), p, engine.Hand)
	c0 := engine.NewScriptedController()
	c0.QueueTargets([]engine.EntityID{engine.CardEntity(bears)})
	for range 3 {
		c0.QueuePayGeneric(mana.ShardU)
	}
	if g.CastSpell(p, force, c0) {
		t.Fatal("cast Force of Will with no other blue card and no mana")
	}
	pitch := g.NewCard(corpusCard(t, "Island"), p, engine.Hand)
	blue := g.NewCard(corpusCard(t, "Counterspell"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueOption(1)
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bears)})
	c.QueueCardChoice([]engine.CardID{blue})
	castThenResolve(t, g, p, force, c)
	if got := g.Card(bears).Zone; got != engine.Graveyard {
		t.Errorf("Grizzly Bears in %v, want Graveyard: countered", got)
	}
	if got := g.Card(blue).Zone; got != engine.Exile {
		t.Errorf("Counterspell in %v, want Exile", got)
	}
	if got := g.Card(pitch).Zone; got != engine.Hand {
		t.Errorf("Island in %v, want Hand: not blue", got)
	}
	if got := g.Player(p).Life; got != 19 {
		t.Errorf("life %d, want 19", got)
	}
}
