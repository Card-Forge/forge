package engine

//enginelint:allow ability airbendeffect card condition control defined effecthelpers game id player random zone zonemove

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// heistEffect is HeistEffect.java (Alchemy): Num$ times (default 1), three
// nonland cards are picked at random from the target player's library
// (Aggregates.random's reservoir on the game's stream), the first Defined$
// player (default You) chooses one, and it is exiled face down. The heister
// may play the heisted cards while they stay exiled, spending mana of any
// type (ExilePlayGrant, AnyManaType); ChangesZoneAll fires once at the end.
//
// A face-down exiled card has no characteristics at all (Card.turnFaceDown's
// blank FaceDown state), unlike a face-down permanent's 2/2; it turns face up
// when it leaves exile (Game.Move). Java also lets the heister look at it
// (addMayLookFaceDownExile) -- this port has no hidden-information model to
// record that in. Card.canExiledBy is not checked: nothing in this port
// models a CantExile static yet.
type heistEffect struct{}

func (heistEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "Heist", "Condition", "ConditionDefined"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	def, ok := a.Params.Param("Defined")
	if !ok {
		def = "You"
	}
	heisters, err := definedPlayers(g, a.Controller, a.Source, def, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Heist: %w", err)
	}
	var victims []PlayerID
	for _, e := range a.Targets {
		if pid, ok := e.AsPlayer(); ok {
			victims = append(victims, pid)
		}
	}
	if len(heisters) == 0 || len(victims) == 0 {
		return nil
	}
	heister, victim := heisters[0], victims[0]
	num, err := optionalAmount(g, a, "Heist", "Num", 1)
	if err != nil {
		return err
	}

	var heisted []CardID
	for i := 0; i < num; i++ {
		var pool []CardID
		for _, id := range g.Zone(Library, victim).Cards() {
			if !g.Card(id).Type().Has(cardtype.Land) {
				pool = append(pool, id)
			}
		}
		var choices []CardID
		for _, idx := range g.randomSample(len(pool), 3) {
			choices = append(choices, pool[idx])
		}
		if len(choices) == 0 {
			continue
		}
		picked := controller.ChooseCardsForEffect(g, heister, a.Source, choices, 1, 1)
		if err := checkChoice(picked, choices, 1, 1); err != nil {
			return fmt.Errorf("engine: Heist: %w", err)
		}
		id := picked[0]
		melded := g.moveByEffect(controller, id, Exile, 0, NoPlayer, false)
		g.markExiledWith(id, a.Source)
		if melded != NoCard {
			g.markExiledWith(melded, a.Source)
		}
		c := g.Card(id)
		if c.faceUpDef == nil {
			c.faceUpDef = c.Def
		}
		c.Def = &compile.Card{}
		heisted = append(heisted, id)
		g.exileGrants = append(g.exileGrants, ExilePlayGrant{
			Card: id, Timestamp: c.Timestamp, Player: heister, AnyManaType: true,
		})
	}
	g.checkChangesZoneAllTriggers(controller, heisted, Library, Exile)
	return nil
}
