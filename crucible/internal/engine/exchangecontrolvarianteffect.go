package engine

//enginelint:allow game ability control effecthelpers card condition defined zone id valid

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// exchangeControlVariantEffect is ControlExchangeVariantEffect.java: with
// exactly two target players, the activator chooses any number of the
// first player's Type$ (default Card) cards in Zone$ (default Battlefield)
// and then as many of the second player's, and the two groups change
// controller -- one timestamp, lasting indefinitely.
type exchangeControlVariantEffect struct{}

func (exchangeControlVariantEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "ExchangeControlVariant"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ExchangeControlVariant: %w", err)
	}
	if len(players) != 2 {
		return nil
	}
	zone := Battlefield
	if raw, ok := a.Params.Param("Zone"); ok {
		z, ok := ZoneByName(raw)
		if !ok || z != Battlefield {
			return fmt.Errorf("engine: ExchangeControlVariant: Zone$ %q not resolvable", raw)
		}
		zone = z
	}
	spec, ok := a.Params.Param("Type")
	if !ok {
		spec = "Card"
	}
	parsed := valid.Parse(spec)
	list := func(p PlayerID) []CardID {
		var out []CardID
		for _, id := range g.Zone(zone, p).Cards() {
			if Matches(g, g.Card(id), parsed, a.Controller, a.Source) {
				out = append(out, id)
			}
		}
		return out
	}
	list1, list2 := list(players[0]), list(players[1])
	most := min(len(list1), len(list2))
	chosen1 := controller.ChooseCardsForEffect(g, a.Controller, a.Source, list1, 0, most)
	if err := checkChoice(chosen1, list1, 0, most); err != nil {
		return fmt.Errorf("engine: ExchangeControlVariant: %w", err)
	}
	n := len(chosen1)
	chosen2 := controller.ChooseCardsForEffect(g, a.Controller, a.Source, list2, n, n)
	if err := checkChoice(chosen2, list2, n, n); err != nil {
		return fmt.Errorf("engine: ExchangeControlVariant: %w", err)
	}
	g.timestamp++
	ts := g.timestamp
	for _, id := range chosen1 {
		g.changeControllerAt(controller, id, players[1], ts)
	}
	for _, id := range chosen2 {
		g.changeControllerAt(controller, id, players[0], ts)
	}
	return nil
}
