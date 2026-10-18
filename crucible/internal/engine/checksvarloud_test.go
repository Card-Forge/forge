package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// A CheckSVar condition this port cannot evaluate fails the line
// (checkSVarMatches records a pending error) instead of reading as unmet: an
// SVar that does not resolve on either side, a compare string with no
// operand, and a second SVar (CardTraitBase.java:470-488).
func TestCheckSVarUnresolvableFailsLoudly(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name  string
		line  string
		svars []string
		want  string
	}{
		{"left side", "DB$ GainLife | Defined$ You | LifeAmount$ 1 | ConditionCheckSVar$ X", nil, `ConditionCheckSVar$ "X" is not resolvable`},
		{"right side", "DB$ GainLife | Defined$ You | LifeAmount$ 1 | ConditionCheckSVar$ X | ConditionSVarCompare$ GEY",
			[]string{"X", "Number$1"}, `ConditionSVarCompare$ "GEY" is not resolvable`},
		{"no operand", "DB$ GainLife | Defined$ You | LifeAmount$ 1 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE",
			[]string{"X", "Number$1"}, "is not a comparison"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line, tc.svars...)
			if err == nil {
				err = g.TakePendingError()
			}
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Errorf("err = %v, want one containing %q", err, tc.want)
			}
			if got := g.Player(p).Life; got != 20 {
				t.Errorf("life = %d, want 20 (the line did not run)", got)
			}
		})
	}
}

// A trigger's CheckSecondSVar$ (CardTraitBase.java:480-487) is not ported:
// the trigger that reaches it fails the game instead of never firing.
func TestCheckSecondSVarFailsLoudly(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(watcherDef(t, "Second Watcher", "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Creature.Other | CheckSVar$ 1 | CheckSecondSVar$ 1 | TriggerZones$ Battlefield",
		"DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	_, err := resolveFromZone(t, g, p, engine.Graveyard, engine.NewScriptedController(), nil,
		"DB$ ChangeZone | Defined$ Self | Origin$ Graveyard | Destination$ Battlefield")
	if err == nil {
		err = g.TakePendingError()
	}
	if err == nil || !strings.Contains(err.Error(), "CheckSecondSVar$") {
		t.Errorf("err = %v, want one naming CheckSecondSVar$", err)
	}
}

// Raid's Count$AttackersDeclared (AbilityUtils.java:2511-2528): the distinct
// creatures the controller attacked with this turn.
func TestCountAttackersDeclaredReadsTheTurnsAttackers(t *testing.T) {
	t.Parallel()

	const raid = "DB$ GainLife | Defined$ You | LifeAmount$ 1 | ConditionCheckSVar$ RaidTest | ConditionSVarCompare$ GE1"

	t.Run("no attack", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, raid, "RaidTest", "Count$AttackersDeclared"); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if got := g.Player(p).Life; got != 20 {
			t.Errorf("life = %d, want 20 (no attack this turn)", got)
		}
	})
	t.Run("attacked", func(t *testing.T) {
		t.Parallel()
		g, _, other, _ := attackingCombat(t)
		if _, err := resolveNow(t, g, other, engine.NewScriptedController(), nil,
			"DB$ GainLife | Defined$ You | LifeAmount$ X", "X", "Count$AttackersDeclared"); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if got := g.Player(other).Life; got != 21 {
			t.Errorf("life = %d, want 21 (one creature attacked)", got)
		}
	})
}

// PhaseType.smartValueOf accepts Java's constant name as well as the script
// name (PhaseType.java:98): ConditionPhases$ COMBAT_BEGIN is the beginning of
// combat step, and a range may name either spelling.
func TestPhaseRangeAcceptsJavaConstantNames(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		phases string
		at     engine.PhaseType
		gains  bool
	}{
		{"COMBAT_BEGIN", engine.CombatBegin, true},
		{"combat_begin", engine.CombatBegin, true},
		{"COMBAT_BEGIN", engine.Main1, false},
		{"MAIN1->COMBAT_DECLARE_ATTACKERS", engine.DeclareAttackers, true},
		{"Upkeep->END_OF_TURN", engine.Cleanup, false},
	} {
		g, p, _ := newTwoPlayerGame(t)
		g.SetTurnState(1, p, tc.at)
		if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
			"DB$ GainLife | Defined$ You | LifeAmount$ 1 | ConditionPhases$ "+tc.phases); err != nil {
			t.Fatalf("%s at %v: ResolveStack: %v", tc.phases, tc.at, err)
		}
		if got := g.Player(p).Life == 21; got != tc.gains {
			t.Errorf("%s at %v: gained = %v, want %v", tc.phases, tc.at, got, tc.gains)
		}
	}
}
