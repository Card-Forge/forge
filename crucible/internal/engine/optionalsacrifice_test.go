package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// An Optional$ Sacrifice without StrictAmount$ is "sacrifice up to Amount$":
// SacrificeEffect.java:138 sets minTargets to 0, so a player who confirmed
// still picks anywhere from none to Amount$ of the candidates (:148).
func TestOptionalSacrificeChoosesUpToAmount(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		pick int // how many of the two bears the player picks
	}{{"one of two", 1}, {"none", 0}, {"both", 2}} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g := newGame(t, "a", "b")
			p := g.Players()[0]
			g.SetTurnState(1, p, engine.Main1)
			bears := []engine.CardID{
				g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield),
				g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield),
			}
			c := engine.NewScriptedController()
			c.QueueConfirmEffect(true)
			c.QueueCardChoice(bears[:tc.pick])
			def := etbSacrificeTriggerDefParams(t, "Test Up To Two", "SacValid$ Creature.Other | Amount$ 2 | Optional$ True", nil)
			if _, err := castETBSacrifice(t, g, p, def, c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			for i, id := range bears {
				want := engine.Battlefield
				if i < tc.pick {
					want = engine.Graveyard
				}
				if got := g.Card(id).Zone; got != want {
					t.Errorf("bear %d is in %v, want %v", i, got, want)
				}
			}
		})
	}
}

// Optional$ with StrictAmount$ keeps minTargets at Amount$
// (SacrificeEffect.java:138): once confirmed, exactly Amount$ go, chosen
// through ChoosePermanentsToSacrifice.
func TestOptionalStrictSacrificeTakesExactlyAmount(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	a := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)
	c.QueueSacrificeChoice([]engine.CardID{a, b})
	def := etbSacrificeTriggerDefParams(t, "Test Strict Two", "SacValid$ Creature.Other | Amount$ 2 | Optional$ True | StrictAmount$ True", nil)
	if _, err := castETBSacrifice(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	for _, id := range []engine.CardID{a, b} {
		if got := g.Card(id).Zone; got != engine.Graveyard {
			t.Errorf("%v is in %v, want both sacrificed", id, got)
		}
	}
}
