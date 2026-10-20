package engine

//enginelint:allow game ability control effecthelpers card condition defined player

import "fmt"

// changeSpeedEffect is ChangeSpeedEffect.java: Mode$ Increase (the default)
// raises each target player's speed by one, capped at 4 (Player.
// increaseSpeed); Mode$ Decrease lowers it, never below 1 (decreaseSpeed).
// Java's speed effect card -- the once-a-turn increase when an opponent
// loses life -- belongs to "Start your engines!", not to this effect.
type changeSpeedEffect struct{}

func (changeSpeedEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "ChangeSpeed"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	mode, ok := a.Params.Param("Mode")
	if !ok {
		mode = "Increase"
	}
	if mode != "Increase" && mode != "Decrease" {
		return fmt.Errorf("engine: ChangeSpeed: Mode$ %q not resolvable", mode)
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ChangeSpeed: %w", err)
	}
	for _, pid := range players {
		p := g.Player(pid)
		if p.Lost {
			continue
		}
		switch {
		case mode == "Increase" && p.Speed < 4:
			p.Speed++
		case mode == "Decrease" && p.Speed > 1:
			p.Speed--
		}
	}
	return nil
}
