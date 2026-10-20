package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// SpellAbilityCondition's Condition$ player-state values
// (SpellAbilityCondition.java:266-272) gate any effect that calls
// subAbilityConditionMet: Hellbent (no cards in hand), Threshold (seven cards
// in the graveyard), Delirium (four card types there), Metalcraft (three
// artifacts).
func TestConditionValuesGateAnAbility(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		value string
		fill  func(t *testing.T, g *engine.Game, p engine.PlayerID)
	}{
		{"Hellbent", nil},
		{"Threshold", func(t *testing.T, g *engine.Game, p engine.PlayerID) {
			for range 7 {
				g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
			}
		}},
		{"Metalcraft", func(t *testing.T, g *engine.Game, p engine.PlayerID) {
			for range 3 {
				g.NewCard(scriptDef(t, "Test Rock", "Artifact"), p, engine.Battlefield)
			}
		}},
	} {
		line := "DB$ Pump | Defined$ Self | NumAtt$ +3 | Condition$ " + tc.value
		g, p, _ := newTwoPlayerGame(t)
		if tc.fill == nil {
			g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand)
		} else {
			// Not enough yet.
			g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
		}
		host, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, line)
		if err != nil {
			t.Fatalf("%s: %v", tc.value, err)
		}
		if got, _ := g.Card(host).Power(); got != 1 {
			t.Errorf("%s unmet: power = %d, want 1", tc.value, got)
		}

		g, p, _ = newTwoPlayerGame(t)
		if tc.fill != nil {
			tc.fill(t, g, p)
		}
		host, err = resolveNow(t, g, p, engine.NewScriptedController(), nil, line)
		if err != nil {
			t.Fatalf("%s: %v", tc.value, err)
		}
		if got, _ := g.Card(host).Power(); got != 4 {
			t.Errorf("%s met: power = %d, want 4", tc.value, got)
		}
	}
}
