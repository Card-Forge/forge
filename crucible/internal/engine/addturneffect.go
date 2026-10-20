package engine

//enginelint:allow card game ability defined condition control amount

import "fmt"

// addTurnUnresolvedParams: an extra turn carrying a delayed trigger
// (ExtraTurnDelayedTrigger$), a skipped untap step (SkipUntap$) or an
// Archenemy scheme restriction (NoSchemes$).
var addTurnUnresolvedParams = [...]string{
	"ExtraTurnDelayedTrigger", "ExtraTurnDelayedTriggerExecute", "SkipUntap", "NoSchemes",
	"Ultimate",
}

// addTurnEffect is AddTurnEffect.java: each target player (default You)
// takes NumTurns$ extra turns after this one (Game.addExtraTurn, turn.go).
// Extra turns stack: the most recently added is taken first (CR 500.7).
type addTurnEffect struct{}

func (addTurnEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range addTurnUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: AddTurn: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	raw, _ := a.Params.Param("NumTurns")
	n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
	if !ok {
		return fmt.Errorf("engine: AddTurn: NumTurns$ %q not resolvable yet", raw)
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: AddTurn: %w", err)
	}
	for _, p := range players {
		for i := 0; i < n; i++ {
			g.addExtraTurn(p)
		}
	}
	return nil
}
