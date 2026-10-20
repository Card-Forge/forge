package engine

//enginelint:allow ability control game effecthelpers card condition id defined additional parts

import "fmt"

// assignGroupEffect is AssignGroupEffect.java: the Chooser$ (the activator
// by default) assigns each Defined$ or targeted object -- player or card --
// to one of the Choices$ abilities. Then each ability with anything
// assigned resolves, in Choices$ order, with its group added to the host's
// remembered objects for the duration.
type assignGroupEffect struct{}

func (assignGroupEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "AssignGroup"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	def, _ := a.Params.Param("Defined")
	var objects []EntityID
	if def == "" {
		objects = a.Targets
	} else {
		var err error
		objects, err = definedEntities(g, a.Controller, source, def, a.refs())
		if err != nil {
			return fmt.Errorf("engine: AssignGroup: %w", err)
		}
	}
	chooser := a.Controller
	if raw, ok := a.Params.Param("Chooser"); ok {
		players, err := definedPlayers(g, a.Controller, a.Source, raw, a.refs())
		if err != nil || len(players) == 0 {
			return fmt.Errorf("engine: AssignGroup: Chooser$ %q not resolvable", raw)
		}
		chooser = players[0]
	}
	choices := additionalAbilities(a.Params, "Choices")
	if len(choices) == 0 {
		return nil
	}
	names := make([]string, len(choices))
	all := make([]int, len(choices))
	for i, c := range choices {
		names[i], all[i] = c.SVar, i
	}
	groups := make([][]EntityID, len(choices))
	for _, obj := range objects {
		picked := controller.ChooseAbilitiesForEffect(g, chooser, a.Source, names, 1)
		if err := checkChoice(picked, all, 1, 1); err != nil {
			return fmt.Errorf("engine: AssignGroup: %w", err)
		}
		groups[picked[0]] = append(groups[picked[0]], obj)
	}
	for i, group := range groups {
		if len(group) == 0 {
			continue
		}
		added := rememberAll(&source.Memory, group)
		if err := g.resolveAdditional(a, controller, choices[i]); err != nil {
			return err
		}
		forgetAll(&source.Memory, added)
	}
	return nil
}
