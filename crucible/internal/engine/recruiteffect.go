package engine

//enginelint:allow control game ability effecthelpers card condition defined zone id discardeffect token

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// recruitEffect is RecruitEffect.java: each target player (the activator by
// default) draws a card, then discards a card of their choice; if the
// discard was a nonland card they create a 1/1 white Human Soldier token
// (w_1_1_human_soldier).
type recruitEffect struct{}

func (recruitEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Recruit"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Recruit: %w", err)
	}
	for _, p := range players {
		g.DrawCards(p, 1, controller)
		hand := append([]CardID(nil), g.Zone(Hand, p).Cards()...)
		if len(hand) == 0 {
			continue
		}
		chosen := controller.ChooseCardsToDiscard(g, p, hand, 1)
		if err := checkChoice(chosen, hand, 1, 1); err != nil {
			return fmt.Errorf("engine: Recruit: %w", err)
		}
		nonland := !g.Card(chosen[0]).Type().Has(cardtype.Land)
		discardCards(g, controller, chosen, p)
		if !nonland {
			continue
		}
		def, err := tokenScript(g, "w_1_1_human_soldier")
		if err != nil {
			return fmt.Errorf("engine: Recruit: %w", err)
		}
		id := g.createToken(controller, tokenSpec{Def: def, Owner: p})
		g.checkChangesZoneAllTriggers(controller, []CardID{id}, None, Battlefield)
	}
	return nil
}
