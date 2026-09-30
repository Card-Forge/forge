package engine

//enginelint:allow game ability control condition defined effecthelpers player id scheduledaction

import "fmt"

// controlPlayerEffect is ControlPlayerEffect.java: the Controller$ player
// (the activator by default) controls each target player during that
// player's next turn (CR 800.4b's "controls another player"), or with
// Combat$ during their next combat phase only.
//
// Nothing changes as the ability resolves. Java adds one command per target
// to a future phase boundary -- getCleanup().addUntil(pTarget, ...), or
// getBeginOfCombat() with Combat$ -- that grants control when that player's
// turn (or combat) begins and schedules the revoke for the next cleanup (or
// end of combat). This port queues the same pair as data (scheduledAction,
// scheduledaction.go, ADR-0030). The grant only redirects whose brain
// decides for the target (Game.ControllingPlayer); the target stays the
// acting player everywhere else.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/ControlPlayerEffect.java's resolve.
type controlPlayerEffect struct{}

func (controlPlayerEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	// Condition$ OptionalCost/ConditionOptionalPaid$ (Secret of
	// Bloodbending) needs the optional-cost record this port does not keep;
	// subAbilityConditionMet would skip the line silently. TargetUnique$
	// (Cruel Entertainment) is a target-set rule targeting.go does not
	// enforce. Both fail loudly instead (PORT-8, GO-7).
	if err := rejectParams(a, "ControlPlayer", "Condition", "ConditionOptionalPaid", "TargetUnique"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	spec, ok := a.Params.Param("Controller")
	if !ok {
		spec = "You"
	}
	controllers, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ControlPlayer: Controller$: %w", err)
	}
	if len(controllers) == 0 {
		// Java's getDefinedPlayers(...).get(0) throws here.
		return fmt.Errorf("engine: ControlPlayer: Controller$ %q names no player", spec)
	}
	controller := controllers[0]
	targets, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ControlPlayer: %w", err)
	}
	_, combat := a.Params.Param("Combat")
	at := boundaryCleanup
	if combat {
		at = boundaryBeginCombat
	}
	for _, target := range targets {
		g.schedule(scheduledAction{
			Kind: scheduledControlGrant, At: at, ForPlayer: target,
			Target: target, Controller: controller, UntilEndOfCombat: combat,
		})
	}
	return nil
}
