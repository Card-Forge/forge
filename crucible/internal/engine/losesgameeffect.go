// LosesGame: CR 104.3a's own "an effect states that a player loses the
// game" -- 47 real (AB|DB)$ LosesGame lines, every one naming Defined$
// (targetedOrDefinedPlayers, defined.go, default "You"). Player.Lost
// (player.go) already exists -- CheckStateBasedActions (action.go) reads it
// unconditionally every pass regardless of why it became true (CR 704.5a's
// own 0-life check sets the identical field) -- so this file only has to
// set the flag; the very next state-based-action pass (ResolveStack's own
// caller, every ability resolution) declares a winner or a draw on its own.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/GameLossEffect.java's
// resolve. checkGameOverCondition's own explicit call is not ported: this
// port's CheckStateBasedActions already runs after every resolution
// (ResolveStack, stack.go), so nothing here has to force an extra pass.

package engine

//enginelint:allow id card game player ability defined condition control gameloss

import "fmt"

// losesGameEffect resolves Mode$/DB$/AB$ LosesGame. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type losesGameEffect struct{}

func (losesGameEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: LosesGame: %w", err)
	}
	for _, pid := range players {
		g.loseConditionMet(pid, lossSpellEffect)
	}
	return nil
}
