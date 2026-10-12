package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestTurnFaceUpReplaceWithLines proves faceChangeReplaced's skips and its
// outcomes: a ValidCard$ the card does not match or failing requirements
// leave the host untouched, a ReplacementResult$ Updated line applies, and a
// ReplaceWith$ ability the Registry refuses records a pending error.
func TestTurnFaceUpReplaceWithLines(t *testing.T) {
	t.Parallel()

	const counter = "DB$ PutCounter | Defined$ Self | CounterType$ STUDY | CounterNum$ 1"
	for name, tc := range map[string]struct {
		extra, sub string
		wantStudy  int
		wantError  bool
	}{
		"applies":            {"", counter, 1, false},
		"updated":            {"ReplacementResult$ Updated | ", counter, 1, false},
		"ValidCard mismatch": {"ValidCard$ Card.Token | ", counter, 0, false},
		"requirements fail":  {"IsPresent$ Card.Nonexistent | PresentCompare$ GE1 | ", counter, 0, false},
		"refused ability":    {"", "DB$ ChangeZone | Defined$ ReplacedCard | Destination$ Exile | Duration$ UntilYourNextTurn", 0, true},
	} {
		t.Run(name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGame(t)
			watcher := g.NewCard(replacementEnchantmentDefWithSVar(t, "Test Flip Watcher",
				"Event$ TurnFaceUp | ActiveZones$ Battlefield | "+tc.extra+"ReplaceWith$ Rm", "Rm", tc.sub), p, engine.Battlefield)
			top := g.NewCard(creatureDefPT(t, "5", "5"), p, engine.Library)
			resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Manifest")
			def := etbChainDef(t, "Turner", "DB$ SetState | ValidTgts$ Creature | Mode$ TurnFaceUp")
			host := g.NewCard(def, p, engine.Battlefield)
			sub := def.Faces[0].Triggers[0].Subs[0].Ability
			g.PushAbility(engine.Ability{API: engine.APISetState, Source: host, Controller: p, Params: sub,
				Targets: []engine.EntityID{engine.CardEntity(top)}})
			err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController())
			if (err != nil) != tc.wantError {
				t.Errorf("ResolveStack error = %v, want error %v", err, tc.wantError)
			}
			if g.Card(top).IsFaceDown() {
				t.Error("still face down")
			}
			if got := g.Card(watcher).Counters.Count("STUDY"); got != tc.wantStudy {
				t.Errorf("study counters = %d, want %d", got, tc.wantStudy)
			}
		})
	}
}
