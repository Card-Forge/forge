// The turn driver: whole steps, turns and games played through real
// priority rounds. ADR-0026.
//
// Ported from PhaseHandler.mainGameLoop/mainLoopStep
// (forge-game/src/main/java/forge/game/phase/PhaseHandler.java:1032-1160):
// each step's turn-based actions and phase triggers (beginStep, turn.go),
// then a priority round if the step grants one (priorityRound,
// priority.go), then the next step. AdvancePhase stays the bookkeeping walk
// fixtures use; only this file drives a step.

package engine

import "errors"

// errRunBeforeStartTurn is Run's own GO-7 stop: a game with no turn begun
// has no active player or step to drive.
var errRunBeforeStartTurn = errors.New("engine: Run called before StartTurn")

// errNoRestart is ResumeAfterRestart called on a game nothing restarted.
var errNoRestart = errors.New("engine: ResumeAfterRestart called on a game that was not restarted")

// Step leaves the current step and plays out the next one: its turn-based
// actions -- combat's declarations and damage included -- and phase
// triggers (beginStep, driven), then a priority round if the step grants
// one. A Cleanup that grants priority is followed by another Cleanup in the
// same call (CR 514.3a, PhaseHandler.java:156-158), until one grants none.
//
// The current step's own priority window is taken as already played: StartTurn
// begins Untap, which grants none, so StartTurn followed by Step or Run
// plays a whole game.
//
// A Cleanup that EndTurn begins during resolution (endturneffect.go) is not
// repeated: its state-based result is discarded, ADR-0026 Decision 5's named
// gap. Only a Cleanup this call began itself repeats.
//
// A RestartGame resolution (restartgameeffect.go) returns at once with
// Restarted true, the way Java's mainLoopStep returns on
// GameStage.RestartedByKarn (PhaseHandler.java:1150-1155); calling Step
// again before ResumeAfterRestart is an error (ADR-0034).
func (g *Game) Step(reg *Registry, controller PlayerController) error {
	if g.over {
		return nil
	}
	if g.restarted {
		return errRestartPending
	}
	priority, err := g.advanceStep(controller, true)
	for {
		if err != nil {
			return err
		}
		if err := g.TakePendingError(); err != nil {
			return err
		}
		if g.over || !priority {
			return nil
		}
		began := g.activePhase
		if err := g.priorityRound(reg, controller); err != nil {
			return err
		}
		if g.over || g.restarted || began != Cleanup || g.activePhase != Cleanup {
			return nil
		}
		priority, err = g.beginStep(controller, true)
	}
}

// Run steps until the game is over or turn maxTurns's Cleanup has
// finished, checked at the Cleanup-to-Untap boundary so a capped game
// always stops after a whole turn (the P7 gate's turn cap). A capped game
// returns nil with Over() still false: whether that is a draw is the
// caller's decision (M8), not a rules outcome. Java has no cap
// (PhaseHandler.java:1032-1037).
//
// A restarted game (Restarted) also returns nil, Over() false, the second
// reason Java's mainGameLoop leaves its loop (PhaseHandler.java:1034); the
// caller runs ResumeAfterRestart and calls Run again (ADR-0034).
//
// There is no cap on actions within one priority round: ScriptedController
// is finite, and a controller that never passes is M7's to guard against,
// where Java put its own (PhaseHandler.java:1102-1105).
func (g *Game) Run(reg *Registry, controller PlayerController, maxTurns int) error {
	if g.restarted {
		return errRestartPending
	}
	if g.turn == 0 {
		return errRunBeforeStartTurn
	}
	for !g.over && !g.restarted {
		if g.activePhase == Cleanup && g.turn >= maxTurns {
			return nil
		}
		if err := g.Step(reg, controller); err != nil {
			return err
		}
	}
	return nil
}

// Restarted reports whether a RestartGame effect has restarted this game
// and the caller has not yet resumed it (GameStage.RestartedByKarn,
// ADR-0034). Step, Run, PassPriority and ResolveStack return as soon as it
// turns true, and refuse to run while it stays true.
//
// The caller's contract on seeing it: call ResumeAfterRestart, which deals
// the restarted game's opening hands, runs its mulligans and begins its
// first turn, then drive the game again. A caller that never checks it
// sees a game that stops advancing with Over() false -- the ADR's own
// named cost.
func (g *Game) Restarted() bool { return g.restarted }

// RestartedBy is the player whose RestartGame effect restarted the game,
// who takes the restarted game's first turn; NoPlayer while Restarted is
// false.
func (g *Game) RestartedBy() PlayerID { return g.restartedBy }

// ResumeAfterRestart is the rest of GameAction.startGame's
// do-while(RestartedByKarn) loop body (GameAction.java:2326-2381) for a
// restarted game: each live player draws an opening hand from the library
// the restart already shuffled -- no coin flip, no starting-player choice
// and no second shuffle, since Java's loop carries `first` over as the
// activator (GameAction.java:2380) and draws with drawCards alone
// (:2341) -- then London mulligans starting with the activator, then the
// activator's first turn (PhaseHandler.startFirstTurn). DealOpeningHands
// is the wrong entry point here: it flips a coin, asks
// ChooseStartingPlayer and shuffles again, three random-stream and
// controller calls Java's restart does not make.
//
// Not ported, same as a normal game start (DealOpeningHands):
// runPreOpeningHandActions/runOpeningHandActions and the NewGame trigger.
// The restart itself refuses any Command-zone card a NewGame trigger could
// be on (restartgameeffect.go), so skipping the trigger drops nothing.
func (g *Game) ResumeAfterRestart(controller PlayerController) error {
	if !g.restarted {
		return errNoRestart
	}
	first := g.restartedBy
	for _, pid := range g.Players() {
		if !g.Player(pid).Lost {
			drawOpeningHand(g, pid)
		}
	}
	PerformMulligans(g, controller, first)
	g.restarted, g.restartedBy = false, NoPlayer
	if g.over {
		return nil
	}
	g.StartTurn(first, controller)
	return nil
}
