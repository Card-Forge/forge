package engine

//enginelint:allow ability game control effecthelpers condition card event id

// gameDrawnEffect is GameDrawEffect.java: every player draws the game
// intentionally and the game ends in a draw (CR 104.4a) -- no winner, no
// loser. Its ConditionPresent$/ConditionCheckSVar$ gates are the only params
// the corpus gives it.
type gameDrawnEffect struct{}

func (gameDrawnEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "GameDrawn"); err != nil {
		return err
	}
	if !subAbilityConditionMet(g, g.Card(a.Source), a.Amounts, a.Params) {
		return nil
	}
	g.over = true
	g.sink.Emit(Event{Kind: GameEnded, Active: g.activePlayer, Actor: NoPlayer, Turn: uint16(g.turn)})
	return nil
}
