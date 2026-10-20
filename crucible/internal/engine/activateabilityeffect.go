package engine

//enginelint:allow game control ability effecthelpers card condition defined player zone manaability

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// activateAbilityEffect is ActivateAbilityEffect.java with ManaAbility$
// True: each target player, for each Type$ (default Card) permanent they
// control, activates one of its mana abilities they can -- a basic land
// type's intrinsic one (CR 305.6) or a scripted Mana ability -- choosing
// among several. A non-mana activation is not resolved.
type activateAbilityEffect struct{}

// manaChoice is one mana ability a permanent offers: an intrinsic basic
// land color, or a scripted ability's index.
type manaChoice struct {
	color mana.Colors
	index int
}

func (activateAbilityEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "ActivateAbility"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if !hasParam(a, "ManaAbility") {
		return fmt.Errorf("engine: ActivateAbility: only ManaAbility$ is resolvable yet")
	}
	spec, ok := a.Params.Param("Type")
	if !ok {
		spec = "Card"
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ActivateAbility: %w", err)
	}
	for _, p := range players {
		if g.Player(p).Lost {
			continue
		}
		for _, id := range filterValid(g, g.Zone(Battlefield, p).Cards(), spec, a.Controller, a.Source) {
			choices := manaChoices(g.Card(id))
			if len(choices) == 0 {
				continue
			}
			i := 0
			if len(choices) > 1 {
				names := make([]string, len(choices))
				for j, ch := range choices {
					if ch.index < 0 {
						names[j] = ch.color.String()
					} else {
						names[j] = fmt.Sprintf("ability %d", ch.index)
					}
				}
				i = controller.ChooseOption(g, p, id, names)
				if i < 0 || i >= len(choices) {
					return fmt.Errorf("engine: ActivateAbility: choice %d out of range", i)
				}
			}
			if ch := choices[i]; ch.index < 0 {
				g.TapLandForMana(p, id, ch.color, controller)
			} else {
				g.ActivateManaAbility(p, id, ch.index, controller)
			}
			if g.pendingErr != nil {
				return nil
			}
		}
	}
	return nil
}

// manaChoices lists the mana abilities c offers while untapped: one per
// basic land type it has, then each scripted Mana ability.
func manaChoices(c *Card) []manaChoice {
	if c.Tapped || c.isDetained() {
		return nil
	}
	var out []manaChoice
	for _, color := range []mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red, mana.Green} {
		if c.Type().HasSubtype(basicLandType[color]) {
			out = append(out, manaChoice{color: color, index: -1})
		}
	}
	if c.Def == nil {
		return out
	}
	for i := range c.abilityCount() {
		ab, _, ok := c.abilityAt(i)
		if !ok {
			continue
		}
		if ab.Record == compile.Activated && ab.Name == "Mana" {
			out = append(out, manaChoice{index: i})
		}
	}
	return out
}
