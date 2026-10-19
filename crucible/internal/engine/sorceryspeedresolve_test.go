package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestSorcerySpeedParamDoesNotBlockResolution proves SorcerySpeed$ is a
// timing restriction ActivateAbility enforces (activateability.go), never a
// resolve-time param an effect refuses: the line resolves once it is on the
// stack.
func TestSorcerySpeedParamDoesNotBlockResolution(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Mill | Defined$ You | NumCards$ 1 | SorcerySpeed$ True | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 3")
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23: the SorcerySpeed$ line did not resolve", got)
	}
}
