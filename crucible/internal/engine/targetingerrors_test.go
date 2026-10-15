package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// A target question this port cannot ask -- a TargetingPlayer$ naming no
// single player, a TargetsWithDefinedController$ it cannot resolve, a target
// zone it cannot scan -- never leaves the ability off the stack as if it had no
// legal target, and never lets it resolve on an empty target set: the ability
// goes on the stack and fails when it resolves, before any part of it acts
// (GO-7). Java asks the activator to pick among several targeting players
// (SpellAbility.java:2150-2154); that pick is a decision this port does not
// ask yet.

// TestTargetQuestionItCannotAskFailsTheTriggerLoudly covers each shape on an
// ETB trigger whose root would otherwise gain life.
func TestTargetQuestionItCannotAskFailsTheTriggerLoudly(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, line, want string
	}{
		{"several targeting players",
			"DB$ GainLife | ValidTgts$ Player | TargetingPlayer$ Player | LifeAmount$ 3",
			`TargetingPlayer$ "Player" names 2 players`},
		{"targeting player not recorded",
			"DB$ GainLife | ValidTgts$ Player | TargetingPlayer$ TriggeredPlayer | LifeAmount$ 3",
			"TargetingPlayer$"},
		{"defined controller not recorded",
			"DB$ Pump | ValidTgts$ Creature | TargetsWithDefinedController$ TriggeredPlayer | NumAtt$ 1 | SubAbility$ DBGain",
			"TargetsWithDefinedController$"},
		{"target zone not scanned",
			"DB$ ChangeZone | ValidTgts$ Card | Origin$ Library | Destination$ Hand | SubAbility$ DBGain",
			"targeting a card in Library"},
	} {
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		_, err := castETBChain(t, g, p, etbChainDef(t, "Test Target Question", tc.line,
			"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 3"), engine.NewScriptedController())
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want %q", tc.name, err, tc.want)
		}
		if got := [2]int{g.Player(p).Life, g.Player(other).Life}; got != [2]int{20, 20} {
			t.Errorf("%s: life (p, other) = %v, want both 20: nothing resolved", tc.name, got)
		}
	}
}

// TestLinkTargetQuestionItCannotAskFailsTheRootBeforeItActs proves a
// SubAbility$ link's unaskable target question fails the whole ability before
// the root's own body runs, naming the link.
func TestLinkTargetQuestionItCannotAskFailsTheRootBeforeItActs(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	_, err := castETBChain(t, g, p, etbChainDef(t, "Test Link Question",
		"DB$ GainLife | Defined$ You | LifeAmount$ 3 | SubAbility$ DBPump",
		"DBPump", "DB$ Pump | ValidTgts$ Creature | TargetingPlayer$ Player | NumAtt$ 1"), engine.NewScriptedController())
	if err == nil || !strings.Contains(err.Error(), "SubAbility$ DBPump (Pump)") || !strings.Contains(err.Error(), "TargetingPlayer$") {
		t.Fatalf("err = %v, want the link's TargetingPlayer$ error", err)
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("p life = %d, want 20: the root did not act", got)
	}
}
