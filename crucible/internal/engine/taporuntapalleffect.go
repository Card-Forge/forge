package engine

//enginelint:allow game control ability effecthelpers card condition id zone valid defined

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// tapOrUntapAllEffect is TapOrUntapAllEffect.java: the activator chooses
// once -- chooseBinary with TapOrUntap, true to tap -- and every ValidCards$
// permanent (or every targeted card) is tapped or untapped accordingly. With
// targeting or Defined$ the cards are narrowed to those the target players
// control.
type tapOrUntapAllEffect struct{}

func (tapOrUntapAllEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "TapOrUntapAll"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	var cards []CardID
	if spec, ok := a.Params.Param("ValidCards"); ok {
		parsed := valid.Parse(spec)
		for _, pid := range g.Players() {
			for _, id := range g.Zone(Battlefield, pid).Cards() {
				if Matches(g, g.Card(id), parsed, a.Controller, a.Source) {
					cards = append(cards, id)
				}
			}
		}
	} else {
		var err error
		cards, err = targetedOrDefinedCards(source, a.Params, a.refs())
		if err != nil {
			return fmt.Errorf("engine: TapOrUntapAll: %w", err)
		}
	}
	if hasParam(a, "ValidTgts") || hasParam(a, "Defined") {
		players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
		if err != nil {
			return fmt.Errorf("engine: TapOrUntapAll: %w", err)
		}
		var kept []CardID
		for _, id := range cards {
			for _, p := range players {
				if g.Card(id).Controller() == p {
					kept = append(kept, id)
					break
				}
			}
		}
		cards = kept
	}
	toTap := controller.ChooseBinary(g, a.Controller, a.Source, TapOrUntap)
	for _, id := range cards {
		c := g.Card(id)
		if c.Zone != Battlefield {
			continue
		}
		switch {
		case toTap && !c.Tapped:
			c.Tapped = true
			g.checkTapsTriggers(controller, id, a.Controller, false)
		case !toTap && c.Tapped:
			g.runUntapCommands(controller, id)
			c.Tapped = false
			g.checkUntapsTriggers(controller, id)
		}
	}
	return nil
}
