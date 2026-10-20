// Static triggers: a T: line carrying Static$ True resolves at its trigger
// site, immediately, and never uses the stack (ADR-0020). CR 605.1b is the
// rules basis for the mana half -- a triggered mana ability ("whenever ...
// is tapped for mana, add ...") is never put on the stack -- and Forge
// treats every mode's Static$ line the same way
// (TriggerHandler.runSingleTriggerInternal, TriggerHandler.java:522-527:
// playTrigger instead of addSimultaneousStackEntry).

package engine

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// isStaticTrigger is Trigger.isStatic (Trigger.java:579-581): the param's
// presence alone decides, and every corpus value is "True".
func isStaticTrigger(t *compile.Ability) bool {
	_, ok := t.Param("Static")
	return ok
}

// resolveStaticTriggers resolves matches in order, each through the Game's
// own Registry (Registry.Resolve, so Optional$, UnlessCost$ and
// ErrUnimplemented behave as for a stacked ability). Nothing is pushed, no
// AbilityActivated or AbilityResolved event is emitted, and no state-based
// action check follows: Java's playTrigger runs the ability through
// playNoStack, with no stack entry and no SBA pass (PlayerControllerAi.java:
// 1377-1382, ComputerUtil.playNoStack).
//
// Order is the host walk's own, not APNAP: TriggerHandler.runWaitingTrigger
// runs static triggers in activeTriggers order (TriggerHandler.java:300-309)
// before any stacked trigger is ordered.
//
// A trigger site has no error return (TapLandForMana, ActivateManaAbility
// and Move report bool or nothing), so a failure is recorded on the Game
// and the nearest boundary that returns error hands it on (pendingErr,
// game.go; ADR-0020 decision 4, GO-7). The first error wins; the event's
// later static matches do not run, the same "stop at the first failure" a
// stack resolution gives.
//
// Targets and Charm modes are refused rather than chosen: no corpus static
// TapsForMana line needs either, and choosing them here would need
// pushTriggeredAbilities' own BecomesTarget firing without a stack item to
// name.
func (g *Game) resolveStaticTriggers(controller PlayerController, matches []Ability) {
	for i := range matches {
		if g.pendingErr != nil {
			return
		}
		a := &matches[i]
		if err := g.staticTriggerResolvable(a); err != nil {
			g.recordPendingError(err)
			return
		}
		if err := g.registry.Resolve(g, a, controller); err != nil {
			g.recordPendingError(fmt.Errorf("engine: static trigger of %q: %w", g.Card(a.Source).Def.Name, err))
			return
		}
		// TriggerHandler.java:539: regtrig.triggerRun() once the static
		// trigger has resolved (a stacked one is noted as it is pushed), so
		// its own resolution reads the runs before it (Count$ActivatedThisGame,
		// Telekinesis).
		if a.Params != nil {
			g.Card(a.Source).trigActs.note(a.Params)
		}
		g.afterEffectTrigger(a)
	}
}

// staticTriggerResolvable reports why a cannot resolve at its trigger site,
// nil when it can.
func (g *Game) staticTriggerResolvable(a *Ability) error {
	if g.registry == nil {
		return fmt.Errorf("engine: static trigger of %q: no Registry on this Game", g.Card(a.Source).Def.Name)
	}
	if a.API == APICharm {
		return fmt.Errorf("engine: static trigger of %q: Charm modes not resolvable yet", g.Card(a.Source).Def.Name)
	}
	if _, ok := a.Params.Param("ValidTgts"); ok {
		return fmt.Errorf("engine: static trigger of %q: ValidTgts$ not resolvable yet", g.Card(a.Source).Def.Name)
	}
	return nil
}
