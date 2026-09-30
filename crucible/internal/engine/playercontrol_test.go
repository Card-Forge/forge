package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These tests cover ControlPlayer (CR 800.4b's "controls another player",
// ADR-0030): the grant waits for the target's next turn (or, with Combat$,
// their next beginning of combat), the revoke for the boundary after, and
// the redirect is a stack -- the newest grant in force decides, and control
// falls back to the next-most-recent one as the newest ends.

// controlState is who controls pid now, as the two public readers see it.
type controlState struct {
	controller engine.PlayerID
	controlled bool
}

func controlOf(g *engine.Game, pid engine.PlayerID) controlState {
	return controlState{g.ControllingPlayer(pid), g.IsControlled(pid)}
}

func wantControl(t *testing.T, g *engine.Game, pid, controller engine.PlayerID) {
	t.Helper()
	want := controlState{controller, controller != engine.NoPlayer && controller != pid}
	if got := controlOf(g, pid); got != want {
		t.Errorf("turn %d %v: control of %v = %+v, want %+v", g.Turn(), g.ActivePhase(), pid, got, want)
	}
}

// newThreePlayerGame is newTwoPlayerGame with a third seat, for a grant
// by someone other than the two players it concerns.
func newThreePlayerGame(t *testing.T) (*engine.Game, engine.PlayerID, engine.PlayerID, engine.PlayerID) {
	t.Helper()
	g := newGame(t, "a", "b", "c")
	ids := g.Players()
	for _, pid := range ids {
		g.Player(pid).Life = 20
	}
	g.SetTurnState(1, ids[0], engine.Main1)
	return g, ids[0], ids[1], ids[2]
}

// advanceToPhase walks g until turn and phase are reached, first giving
// every player a library to draw from so nobody loses to an empty one
// (CR 704.5b) on the way -- a lost controller gains nothing.
func advanceToPhase(t *testing.T, g *engine.Game, c engine.PlayerController, turn int, phase engine.PhaseType) {
	t.Helper()
	for _, pid := range g.Players() {
		for g.Zone(engine.Library, pid).Len() < 8 {
			g.NewCard(nil, pid, engine.Library)
		}
	}
	advanceUntil(t, g, c, func() bool { return g.Turn() == turn && g.ActivePhase() == phase })
}

// TestControlPlayerLastsForTheTargetsNextTurn proves Mindslaver's shape:
// nothing changes as the ability resolves or for the rest of the caster's
// turn; the target is controlled from the start of their next turn through
// its cleanup, and free again as the turn after begins.
func TestControlPlayerLastsForTheTargetsNextTurn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	resolveLine(t, g, p, c, "DB$ ControlPlayer | ValidTgts$ Player")

	wantControl(t, g, other, engine.NoPlayer)
	advanceToPhase(t, g, c, 1, engine.Cleanup)
	wantControl(t, g, other, engine.NoPlayer)

	advanceToPhase(t, g, c, 2, engine.Untap)
	wantControl(t, g, other, p)
	wantControl(t, g, p, engine.NoPlayer)
	advanceToPhase(t, g, c, 2, engine.Cleanup)
	wantControl(t, g, other, p)

	advanceToPhase(t, g, c, 3, engine.Untap)
	wantControl(t, g, other, engine.NoPlayer)
}

// TestControlPlayerDuringTheTargetsOwnTurnWaitsForTheirNextTurn proves an
// instant-speed Mindslaver aimed at the active player: "next turn" is the
// target's next one, not the rest of the current one.
func TestControlPlayerDuringTheTargetsOwnTurnWaitsForTheirNextTurn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, other, engine.Main1)
	c := engine.NewScriptedController()
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)}, "DB$ ControlPlayer | ValidTgts$ Player"); err != nil {
		t.Fatal(err)
	}

	advanceToPhase(t, g, c, 1, engine.Cleanup)
	wantControl(t, g, other, engine.NoPlayer)
	advanceToPhase(t, g, c, 2, engine.Main1)
	wantControl(t, g, other, engine.NoPlayer)
	advanceToPhase(t, g, c, 3, engine.Untap)
	wantControl(t, g, other, p)
	advanceToPhase(t, g, c, 4, engine.Untap)
	wantControl(t, g, other, engine.NoPlayer)
}

// TestControlPlayerCombatLastsForTheTargetsNextCombat proves Combat$ (Secret
// of Bloodbending's unkicked half): control begins at the target's next
// beginning of combat -- not the caster's own -- and ends with that combat.
func TestControlPlayerCombatLastsForTheTargetsNextCombat(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	resolveLine(t, g, p, c, "DB$ ControlPlayer | ValidTgts$ Opponent | Combat$ True")

	advanceToPhase(t, g, c, 1, engine.CombatBegin)
	wantControl(t, g, other, engine.NoPlayer)
	advanceToPhase(t, g, c, 2, engine.Main1)
	wantControl(t, g, other, engine.NoPlayer)
	advanceToPhase(t, g, c, 2, engine.CombatBegin)
	wantControl(t, g, other, p)
	advanceToPhase(t, g, c, 2, engine.CombatDamage)
	wantControl(t, g, other, p)
	advanceToPhase(t, g, c, 2, engine.CombatEnd)
	wantControl(t, g, other, engine.NoPlayer)
	advanceToPhase(t, g, c, 4, engine.CombatBegin)
	wantControl(t, g, other, engine.NoPlayer)
}

// TestControlPlayerCombatDuringTheTargetsTurnTakesThisCombat proves the
// begin-of-combat grant is keyed to the target's combat, whichever turn it
// comes in: cast in the target's own precombat main phase, it takes this
// turn's combat.
func TestControlPlayerCombatDuringTheTargetsTurnTakesThisCombat(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, other, engine.Main1)
	c := engine.NewScriptedController()
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)}, "DB$ ControlPlayer | ValidTgts$ Opponent | Combat$ True"); err != nil {
		t.Fatal(err)
	}
	advanceToPhase(t, g, c, 1, engine.CombatBegin)
	wantControl(t, g, other, p)
	advanceToPhase(t, g, c, 1, engine.Main2)
	wantControl(t, g, other, engine.NoPlayer)
}

// TestEndCombatPhaseRevokesCombatControl proves the end-of-combat revoke
// rides PhaseHandler.endCombat itself, so an effect ending combat early
// (EndCombatPhase) ends the control with it.
func TestEndCombatPhaseRevokesCombatControl(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, other, engine.Main1)
	c := engine.NewScriptedController()
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)}, "DB$ ControlPlayer | ValidTgts$ Opponent | Combat$ True"); err != nil {
		t.Fatal(err)
	}
	advanceToPhase(t, g, c, 1, engine.DeclareAttackers)
	wantControl(t, g, other, p)
	if _, err := resolveNow(t, g, other, c, nil, "DB$ EndCombatPhase"); err != nil {
		t.Fatal(err)
	}
	wantControl(t, g, other, engine.NoPlayer)
}

// TestControlPlayerNestedGrantsRevertToTheNextMostRecent proves CR 800.4b's
// stack: a turn-long grant and a newer combat-only one over the same
// player. The newer one decides during combat; as it ends, control reverts
// to the older grant, not to the player themself.
func TestControlPlayerNestedGrantsRevertToTheNextMostRecent(t *testing.T) {
	t.Parallel()

	g, p, other, third := newThreePlayerGame(t)
	c := engine.NewScriptedController()
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)}, "DB$ ControlPlayer | ValidTgts$ Player"); err != nil {
		t.Fatal(err)
	}
	if _, err := resolveNow(t, g, third, c, []engine.EntityID{engine.PlayerEntity(other)}, "DB$ ControlPlayer | ValidTgts$ Player | Combat$ True"); err != nil {
		t.Fatal(err)
	}

	advanceToPhase(t, g, c, 2, engine.Main1)
	wantControl(t, g, other, p)
	advanceToPhase(t, g, c, 2, engine.CombatBegin)
	wantControl(t, g, other, third)
	advanceToPhase(t, g, c, 2, engine.CombatEnd)
	wantControl(t, g, other, p)
	advanceToPhase(t, g, c, 3, engine.Untap)
	wantControl(t, g, other, engine.NoPlayer)
}

// TestControlPlayerMutualPairRevokesBeforeGranting proves Cruel
// Entertainment's shape -- each of two players controls the other during
// the other's next turn -- and the ordering it depends on
// (PhaseHandler.java:515-518, "do this first for ControlPlayer"): as the
// second turn begins, the first grant's revoke runs before the second
// grant, and the second grant's own new revoke waits for the next turn
// rather than undoing it at once.
func TestControlPlayerMutualPairRevokesBeforeGranting(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ ControlPlayer | Defined$ Opponent | SubAbility$ DBOther",
		"DBOther", "DB$ ControlPlayer | Defined$ You | Controller$ Opponent")

	advanceToPhase(t, g, c, 1, engine.Cleanup)
	wantControl(t, g, p, engine.NoPlayer)
	wantControl(t, g, other, engine.NoPlayer)

	advanceToPhase(t, g, c, 2, engine.Untap)
	wantControl(t, g, other, p)
	wantControl(t, g, p, engine.NoPlayer)

	advanceToPhase(t, g, c, 3, engine.Untap)
	wantControl(t, g, other, engine.NoPlayer)
	wantControl(t, g, p, other)
	advanceToPhase(t, g, c, 3, engine.Cleanup)
	wantControl(t, g, p, other)

	advanceToPhase(t, g, c, 4, engine.Untap)
	wantControl(t, g, p, engine.NoPlayer)
	wantControl(t, g, other, engine.NoPlayer)
}

// TestControlPlayerOverYourselfIsNotControl proves Player.isControlled's
// self check: a Mindslaver aimed at its own controller records the grant
// but nobody else is deciding.
func TestControlPlayerOverYourselfIsNotControl(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
	resolveLine(t, g, p, c, "DB$ ControlPlayer | ValidTgts$ Player")
	advanceToPhase(t, g, c, 3, engine.Untap)
	if got := controlOf(g, p); got != (controlState{p, false}) {
		t.Errorf("control of p = %+v, want p over themself, not controlled", got)
	}
}

// TestControlPlayerGrantSkippedForAControllerWhoLeft proves CR 800.4b's
// guard in the grant (ControlPlayerEffect.java:36): a controller who has
// left the game before the target's turn gains nothing.
func TestControlPlayerGrantSkippedForAControllerWhoLeft(t *testing.T) {
	t.Parallel()

	g, _, other, third := newThreePlayerGame(t)
	c := engine.NewScriptedController()
	if _, err := resolveNow(t, g, third, c, []engine.EntityID{engine.PlayerEntity(other)}, "DB$ ControlPlayer | ValidTgts$ Player"); err != nil {
		t.Fatal(err)
	}
	if _, err := resolveNow(t, g, third, c, nil, "DB$ LosesGame | Defined$ You"); err != nil {
		t.Fatal(err)
	}
	advanceToPhase(t, g, c, 2, engine.Untap)
	wantControl(t, g, other, engine.NoPlayer)
}

// TestControllerLeavingTheGameFreesTheirMindslaves proves Game.onPlayerLost's
// "free any mindslaves" (Game.java:1014-1017): a controller who loses
// mid-turn stops controlling at once, and an older grant by someone still in
// the game decides again.
func TestControllerLeavingTheGameFreesTheirMindslaves(t *testing.T) {
	t.Parallel()

	g, p, other, third := newThreePlayerGame(t)
	c := engine.NewScriptedController()
	for _, controller := range []engine.PlayerID{p, third} {
		if _, err := resolveNow(t, g, controller, c, []engine.EntityID{engine.PlayerEntity(other)}, "DB$ ControlPlayer | ValidTgts$ Player"); err != nil {
			t.Fatal(err)
		}
	}
	advanceToPhase(t, g, c, 2, engine.Main1)
	wantControl(t, g, other, third)
	if _, err := resolveNow(t, g, third, c, nil, "DB$ LosesGame | Defined$ You"); err != nil {
		t.Fatal(err)
	}
	engine.CheckStateBasedActions(g, c)
	// third's grant is gone; p's older one, still in force, decides again.
	wantControl(t, g, other, p)
	advanceToPhase(t, g, c, 3, engine.Main1)
	wantControl(t, g, other, engine.NoPlayer)
}

// TestControlPlayerSurvivesCloneIndependently proves Game.Clone copies both
// the pending schedule and the redirect stack: the clone's turn advancing
// changes nothing in the original.
func TestControlPlayerSurvivesCloneIndependently(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	resolveLine(t, g, p, c, "DB$ ControlPlayer | ValidTgts$ Player")

	pending := g.Clone()
	advanceToPhase(t, pending, c, 2, engine.Untap)
	wantControl(t, pending, other, p)
	wantControl(t, g, other, engine.NoPlayer)

	advanceToPhase(t, g, c, 2, engine.Main1)
	granted := g.Clone()
	advanceToPhase(t, granted, c, 3, engine.Untap)
	wantControl(t, granted, other, engine.NoPlayer)
	wantControl(t, g, other, p)
}

// TestControlPlayerRejectsUnportedShapes proves the corpus params this
// port cannot resolve fail loudly rather than being skipped: Secret of
// Bloodbending's Condition$ OptionalCost pair and Cruel Entertainment's
// TargetUnique$ and Controller$ ParentTarget/Player.Chosen, plus a
// Controller$ naming nobody (Java's .get(0) on an empty list).
func TestControlPlayerRejectsUnportedShapes(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ line, want string }{
		{"DB$ ControlPlayer | ValidTgts$ Opponent | Condition$ OptionalCost | ConditionOptionalPaid$ False | Combat$ True", "Condition$ not resolvable yet"},
		{"DB$ ControlPlayer | Defined$ Opponent | ConditionOptionalPaid$ True", "ConditionOptionalPaid$ not resolvable yet"},
		{"DB$ ControlPlayer | ValidTgts$ Player | TargetUnique$ True | Controller$ Player.Chosen", "TargetUnique$ not resolvable yet"},
		{"DB$ ControlPlayer | Defined$ Opponent | Controller$ ParentTarget", "Controller$"},
		{"DB$ ControlPlayer | Defined$ Opponent | Controller$ Player.Chosen", "Controller$"},
		{"DB$ ControlPlayer | Defined$ Opponent | Controller$ ChosenPlayer", "names no player"},
	} {
		g, p, other := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: err = %v, want %q", tc.line, err, tc.want)
		}
		advanceToPhase(t, g, engine.NewScriptedController(), 2, engine.Main1)
		wantControl(t, g, other, engine.NoPlayer)
	}
}

// TestSorinMarkovUltimateControlsTheTarget activates the real Sorin
// Markov's -7 loyalty ability, the planeswalker host of the same line.
func TestSorinMarkovUltimateControlsTheTarget(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	sorin := g.NewCard(corpusCard(t, "Sorin Markov"), p, engine.Battlefield)
	g.Card(sorin).Counters.Add(engine.CounterType("LOYALTY"), 7)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if !g.ActivateAbility(p, sorin, 2, c) {
		t.Fatal("Sorin Markov's -7 did not activate")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	advanceToPhase(t, g, c, 2, engine.Upkeep)
	wantControl(t, g, other, p)
}

// TestMindslaverControlsTheTargetFromTheRealScript activates the real
// corpus Mindslaver ({4}, {T}, sacrifice: "You control target player during
// that player's next turn").
func TestMindslaverControlsTheTargetFromTheRealScript(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	slaver := g.NewCard(corpusCard(t, "Mindslaver"), p, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.Green, 4)
	c := engine.NewScriptedController()
	for i := 0; i < 4; i++ {
		c.QueuePayGeneric(mana.ShardG)
	}
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if !g.ActivateAbility(p, slaver, 0, c) {
		t.Fatal("Mindslaver did not activate")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if z := g.Card(slaver).Zone; z != engine.Graveyard {
		t.Errorf("Mindslaver in %v, want Graveyard (sacrificed)", z)
	}
	advanceToPhase(t, g, c, 2, engine.Untap)
	wantControl(t, g, other, p)
	advanceToPhase(t, g, c, 3, engine.Untap)
	wantControl(t, g, other, engine.NoPlayer)
}
