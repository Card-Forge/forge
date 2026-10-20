package engine

//enginelint:allow id card game ability defined condition control parts effecthelpers

import "fmt"

// choosePlayerUnresolvedParams are ChoosePlayerEffect.java's params this
// port cannot honour yet: Protect$ (battle protector reassignment),
// Optional$, and the ChooseSubAbility$/CantChooseSubAbility$
// AdditionalAbility dispatch no effect in this port has yet. Random$ picks
// with Aggregates.random over Choices$ (ChoosePlayerEffect.java:47-48);
// Secretly$ writes the same chosen-player slot, only without updating the
// view (Card.setSecretChosenPlayer, Card.java:2127-2129).
var choosePlayerUnresolvedParams = [...]string{
	"Protect", "Optional",
	"ChooseSubAbility", "CantChooseSubAbility",
	"OrOtherConditionSVarCompare",
}

// choosePlayerEffect is ChoosePlayerEffect.java: each chooser
// (getTargetPlayers -- Defined$/ValidTgts$, default You) picks one player
// out of Choices$ (a Defined$-style player list, default every player) and
// the host records it (Memory.SetChosenPlayer, read back by Defined$
// ChosenPlayer). An empty choice list records nothing. 127 real lines.
type choosePlayerEffect struct{}

func (choosePlayerEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range choosePlayerUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: ChoosePlayer: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	choosers, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ChoosePlayer: %w", err)
	}
	choicesParam, ok := a.Params.Param("Choices")
	if !ok {
		choicesParam = "Player"
	}
	choices, err := definedPlayers(g, a.Controller, a.Source, choicesParam, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ChoosePlayer: Choices$: %w", err)
	}
	if len(choices) == 0 {
		return nil
	}
	_, random := a.Params.Param("Random")
	for _, pid := range choosers {
		var chosen PlayerID
		if random {
			chosen = choices[g.randomIndex(len(choices))]
		} else {
			chosen = controller.ChoosePlayerForEffect(g, pid, a.Source, choices)
			if err := checkChoice([]PlayerID{chosen}, choices, 1, 1); err != nil {
				return fmt.Errorf("engine: ChoosePlayer: %w", err)
			}
		}
		source.Memory.SetChosenPlayer(chosen)
		if _, ok := a.Params.Param("ForgetOtherRemembered"); ok {
			source.Memory.ClearRemembered()
		}
		if _, ok := a.Params.Param("RememberChosen"); ok {
			source.Memory.Remember(PlayerEntity(chosen))
		}
	}
	return nil
}
