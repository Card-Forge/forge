// WinsGame: CR 104.1's own alt-win -- "an effect states that a player wins
// the game." 42 real (AB|DB)$ WinsGame lines, 41 naming Defined$
// (targetedOrDefinedPlayers, defined.go, default "You"). Player.Won
// (player.go) already existed for CR 104.2a's own "one player left
// standing" elimination win (CheckStateBasedActions, action.go) but nothing
// read it as an independent win condition until this file forced the
// question: CheckStateBasedActions now checks it first, above CR 104.2a's
// own elimination count, ending the game immediately the way CR 104.1 says
// an effect-driven win does rather than waiting for every opponent to also
// be eliminated (action.go's own doc comment on CheckStateBasedActions has
// the full reasoning, including CR 104.4a's own simultaneous-win draw).
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/GameWinEffect.java's
// resolve. checkGameOverCondition's own explicit call is not ported, the
// identical reasoning losesGameEffect's own doc comment gives.

package engine

//enginelint:allow id card game player ability defined condition control

import "fmt"

// winsGameEffect resolves Mode$/DB$/AB$ WinsGame. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type winsGameEffect struct{}

func (winsGameEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: WinsGame: %w", err)
	}
	for _, pid := range players {
		// Player.altWinBySpellEffect: nothing happens when the player cannot win.
		if !g.cantWin(pid) {
			g.Player(pid).Won = true
		}
	}
	return nil
}
