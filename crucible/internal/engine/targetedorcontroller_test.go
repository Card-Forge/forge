package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestDefinedTargetedOrControllerNamesThePlayerOrTheController proves
// Defined$ TargetedOrController (AbilityUtils.java:933-935, Pulse of the
// Forge's "that player or that planeswalker's controller", Urza's
// DBDiscard1U): a targeted player is named, and a targeted card names its
// controller.
func TestDefinedTargetedOrControllerNamesThePlayerOrTheController(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name   string
		walker bool
	}{
		{"player", false},
		{"planeswalker", true},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			target := engine.PlayerEntity(other)
			if tc.walker {
				target = engine.CardEntity(g.NewCard(planeswalkerDefLoyalty(t, "5"), other, engine.Battlefield))
			}
			if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{target},
				"DB$ GainLife | Defined$ TargetedOrController | LifeAmount$ 3"); err != nil {
				t.Fatal(err)
			}
			if got := g.Player(other).Life; got != 23 {
				t.Errorf("opponent life = %d, want 23", got)
			}
			if got := g.Player(p).Life; got != 20 {
				t.Errorf("caster life = %d, want 20", got)
			}
		})
	}
}
