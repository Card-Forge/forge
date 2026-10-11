package engine_test

import (
	"testing"
	"time"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// A Mode$ Phase trigger on the Cleanup step carrying Static$ True resolves
// inline (ADR-0020) and puts nothing on the stack, so CR 514.3a does not give
// priority and begin another Cleanup. Pushed on the stack instead, the trigger
// fires again on that next Cleanup, and the game never leaves the turn: Vodalian
// War Machine, Herald of Vengeance, Summoning Trap and Spider-Man 2099 hung
// every game they were in.
func TestStaticCleanupTriggerDoesNotRepeatTheCleanupStep(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	for range 5 {
		g.NewCard(corpusCard(t, "Forest"), p, engine.Library)
		g.NewCard(corpusCard(t, "Forest"), g.Players()[1], engine.Library)
	}
	g.NewCard(scriptDef(t, "Cleanup Counter", "Enchantment",
		"T:Mode$ Phase | Phase$ Cleanup | Static$ True | Execute$ Gain",
		"SVar:Gain:DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)

	done := make(chan error, 1)
	go func() { done <- g.Run(engine.NewRegistry(), engine.NewScriptedController(), 2) }()
	select {
	case err := <-done:
		if err != nil {
			t.Fatalf("Run: %v", err)
		}
	case <-time.After(30 * time.Second):
		t.Fatal("Run did not finish: the Cleanup step repeats forever")
	}
	// The trigger fired once per Cleanup of turns 1 and 2, not once per repeat.
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22 (20 plus one per Cleanup step)", got)
	}
}
