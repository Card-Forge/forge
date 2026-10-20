package engine

//enginelint:allow ability control game effecthelpers card condition defined id zone parts event

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// blightEffect is BlightEffect.java: each target player chooses a creature
// they control, if any, and puts Num$ (default 1) -1/-1 counters on it.
type blightEffect struct{}

func (blightEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Blight"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	n, err := optionalAmount(g, a, "Blight", "Num", 1)
	if err != nil {
		return err
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Blight: %w", err)
	}
	for _, p := range players {
		var options []CardID
		for _, id := range g.Zone(Battlefield, p).Cards() {
			if g.Card(id).Type().Has(cardtype.Creature) {
				options = append(options, id)
			}
		}
		if len(options) == 0 {
			continue
		}
		chosen := controller.ChooseCardsForEffect(g, p, a.Source, options, 1, 1)
		if err := checkChoice(chosen, options, 1, 1); err != nil {
			return fmt.Errorf("engine: Blight: %w", err)
		}
		// The blighting player places (BlightEffect.java:45, :54).
		g.addCardCounters(controller, p, a.Source, chosen[0], M1M1, n)
	}
	return nil
}
