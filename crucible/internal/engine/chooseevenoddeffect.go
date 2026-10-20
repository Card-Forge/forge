package engine

//enginelint:allow game control ability effecthelpers card condition defined player parts

import "fmt"

// chooseEvenOddEffect is ChooseEvenOddEffect.java: each target player (the
// activator by default) picks odd or even -- chooseBinary with
// OddsOrEvens, true for odd -- and the host records it (setChosenEvenOdd),
// the last pick winning.
type chooseEvenOddEffect struct{}

func (chooseEvenOddEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "ChooseEvenOdd"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ChooseEvenOdd: %w", err)
	}
	for _, p := range players {
		if g.Player(p).Lost {
			continue
		}
		if controller.ChooseBinary(g, p, a.Source, OddsOrEvens) {
			source.Memory.SetChosenEvenOdd("Odd")
		} else {
			source.Memory.SetChosenEvenOdd("Even")
		}
	}
	return nil
}
