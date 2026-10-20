package engine

//enginelint:allow ability game control effecthelpers card condition parts

// chooseDirectionEffect is ChooseDirectionEffect.java: the activator picks
// left (clockwise) or right -- chooseBinary with LeftOrRight, true for
// left -- and the host records it (setChosenDirection).
type chooseDirectionEffect struct{}

func (chooseDirectionEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "ChooseDirection"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if controller.ChooseBinary(g, a.Controller, a.Source, LeftOrRight) {
		source.Memory.SetChosenDirection("Left")
	} else {
		source.Memory.SetChosenDirection("Right")
	}
	return nil
}
