package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestActivateAbilityActivationPhasesRestrictsTiming proves
// SpellAbilityRestriction.checkTimingRestrictions' ActivationPhases$ check
// (SpellAbilityRestriction.java:294-297): an ability limited to the declare
// blockers step declines in Main1 and in the combat damage step, with no
// cost paid, and activates in the declare blockers step itself.
func TestActivateAbilityActivationPhasesRestrictsTiming(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		phase engine.PhaseType
		want  bool
	}{
		{engine.Main1, false},
		{engine.CombatDamage, false},
		{engine.DeclareBlockers, true},
	} {
		t.Run(tc.phase.String(), func(t *testing.T) {
			t.Parallel()

			g := newGame(t, "a", "b")
			p := g.Players()[0]
			g.SetTurnState(1, p, tc.phase)
			g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
			def := creatureDefWithAbility(t, "Test Blockers Only",
				"AB$ Pump | Cost$ T | ActivationPhases$ Declare Blockers | Defined$ Self | NumAtt$ 1")
			creature := g.NewCard(def, p, engine.Battlefield)

			got := g.ActivateAbility(p, creature, 0, engine.NewScriptedController())
			if got != tc.want {
				t.Fatalf("ActivateAbility in %v = %v, want %v", tc.phase, got, tc.want)
			}
			if g.Card(creature).Tapped != tc.want {
				t.Errorf("Tapped = %v, want %v (a declined activation pays no cost)", g.Card(creature).Tapped, tc.want)
			}
		})
	}
}

// TestActivateAbilityActivationPhasesRange proves the "A->B" range form
// (PhaseType.parseRange) is inclusive at both ends and excludes steps
// outside it.
func TestActivateAbilityActivationPhasesRange(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		phase engine.PhaseType
		want  bool
	}{
		{engine.Upkeep, true},
		{engine.CombatBegin, true},
		{engine.DeclareAttackers, false},
	} {
		t.Run(tc.phase.String(), func(t *testing.T) {
			t.Parallel()

			g := newGame(t, "a", "b")
			p := g.Players()[0]
			g.SetTurnState(1, p, tc.phase)
			g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
			def := creatureDefWithAbility(t, "Test Upkeep To Combat",
				"AB$ Pump | Cost$ T | ActivationPhases$ Upkeep->BeginCombat | Defined$ Self | NumAtt$ 1")
			creature := g.NewCard(def, p, engine.Battlefield)

			if got := g.ActivateAbility(p, creature, 0, engine.NewScriptedController()); got != tc.want {
				t.Errorf("ActivateAbility in %v = %v, want %v", tc.phase, got, tc.want)
			}
		})
	}
}

// TestActivateAbilityActivationPhasesUnreadableDeclines proves an
// ActivationPhases$ naming no phase refuses the activation rather than
// ignoring the restriction (GO-7).
func TestActivateAbilityActivationPhasesUnreadableDeclines(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	def := creatureDefWithAbility(t, "Test Bad Phase",
		"AB$ Pump | Cost$ T | ActivationPhases$ Teatime | Defined$ Self | NumAtt$ 1")
	creature := g.NewCard(def, p, engine.Battlefield)

	if g.ActivateAbility(p, creature, 0, engine.NewScriptedController()) {
		t.Error("ActivateAbility accepted an unreadable ActivationPhases$")
	}
}

// TestActivateManaAbilityActivationPhasesRestrictsTiming proves
// ActivateManaAbility (activatemanaability.go) checks ActivationPhases$ the
// identical way ActivateAbility does, checked first: a mana ability
// restricted to Main1 declines outside it, pays no cost, and adds no mana.
func TestActivateManaAbilityActivationPhasesRestrictsTiming(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		phase engine.PhaseType
		want  bool
	}{
		{engine.Upkeep, false},
		{engine.Main1, true},
	} {
		t.Run(tc.phase.String(), func(t *testing.T) {
			t.Parallel()

			g := newGame(t, "a", "b")
			p := g.Players()[0]
			g.SetTurnState(1, p, tc.phase)
			g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
			def := creatureDefWithAbility(t, "Test Mana Main Phase Only",
				"AB$ Mana | Cost$ T | ActivationPhases$ Main1 | Produced$ G")
			dork := g.NewCard(def, p, engine.Battlefield)

			c := engine.NewScriptedController()
			if got := g.ActivateManaAbility(p, dork, 0, c); got != tc.want {
				t.Fatalf("ActivateManaAbility in %v = %v, want %v", tc.phase, got, tc.want)
			}
			if g.Card(dork).Tapped != tc.want {
				t.Errorf("Tapped = %v, want %v (a declined activation pays no cost)", g.Card(dork).Tapped, tc.want)
			}
			if got, want := g.Player(p).ManaPool.Breakdown()[4] > 0, tc.want; got != want {
				t.Errorf("green in pool = %v, want %v", got, want)
			}
		})
	}
}
