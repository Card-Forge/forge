package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// ChangeZone AttachedTo$ puts an Aura onto the battlefield attached to a
// host its controller picks among the legal ones; with none legal the Aura
// stays where it is (ChangeZoneEffect.java:632-655).
func TestChangeZoneAttachedToPicksAHost(t *testing.T) {
	t.Parallel()

	const line = "DB$ ChangeZone | Origin$ Graveyard | Destination$ Battlefield | ValidTgts$ Card.Aura | AttachedTo$ Creature.YouCtrl"
	t.Run("picks the chosen creature", func(t *testing.T) {
		t.Parallel()

		g, p, _ := newTwoPlayerGame(t)
		first := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
		second := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		aura := g.NewCard(auraDefWithEnchant(t, "Creature"), p, engine.Graveyard)
		c := engine.NewScriptedController()
		c.QueueCardChoice([]engine.CardID{second})
		if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(aura)}, line); err != nil {
			t.Fatal(err)
		}
		if got := g.Card(aura).Zone; got != engine.Battlefield {
			t.Fatalf("aura zone = %v, want the battlefield", got)
		}
		if host, ok := g.Card(aura).AttachedTo(); !ok || host != second {
			t.Errorf("aura attached to %v (%v), want the second creature %v (first %v)", host, ok, second, first)
		}
	})
	t.Run("no legal host leaves the aura", func(t *testing.T) {
		t.Parallel()

		g, p, _ := newTwoPlayerGame(t)
		aura := g.NewCard(auraDefWithEnchant(t, "Creature"), p, engine.Graveyard)
		// no creature of the opponent's exists to attach to
		none := "DB$ ChangeZone | Origin$ Graveyard | Destination$ Battlefield | ValidTgts$ Card.Aura | AttachedTo$ Creature.OppCtrl"
		if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(aura)}, none); err != nil {
			t.Fatal(err)
		}
		if got := g.Card(aura).Zone; got != engine.Graveyard {
			t.Errorf("aura zone = %v, want it still in the graveyard", got)
		}
	})
}
