package engine

//enginelint:allow card game player ability defined condition control amount

import "fmt"

// skipTurnEffect is SkipTurnEffect.java: each target player (default You)
// skips their next NumTurns$ turns. Java installs a BeginTurn replacement
// that counts itself down; Player.TurnsToSkip is that count, spent by
// nextActivePlayer (turn.go). Two SkipTurn resolutions add up, as two Java
// effect cards would each skip their own turns. 13 real lines.
type skipTurnEffect struct{}

func (skipTurnEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	raw, _ := a.Params.Param("NumTurns")
	n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
	if !ok {
		return fmt.Errorf("engine: SkipTurn: NumTurns$ %q not resolvable yet", raw)
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: SkipTurn: %w", err)
	}
	for _, p := range players {
		g.Player(p).TurnsToSkip += n
	}
	return nil
}
