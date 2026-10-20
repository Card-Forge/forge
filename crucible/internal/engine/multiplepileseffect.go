package engine

//enginelint:allow control ability game effecthelpers card condition zone defined id player parts

import (
	"fmt"
	"strconv"
)

// multiplePilesEffect is MultiplePilesEffect.java: starting with the
// activator, each target player splits a pool -- DefinedCards$, or their
// Zone$ (default Battlefield) cards, matching ValidCards$ -- into Piles$
// piles, choosing each pile but the last from what remains. With
// RandomChosen$ one pile per player is picked at random and all picked
// piles are remembered on the host while ChosenPile$ resolves, then
// forgotten. Java walks the players' piles in hash order; this port walks
// them in the order they were made.
type multiplePilesEffect struct{}

func (multiplePilesEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "MultiplePiles"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	raw, _ := a.Params.Param("Piles")
	piles, err := strconv.Atoi(raw)
	if err != nil || piles < 1 {
		return fmt.Errorf("engine: MultiplePiles: Piles$ %q not resolvable", raw)
	}
	zone := Battlefield
	if raw, ok := a.Params.Param("Zone"); ok {
		z, ok := ZoneByName(raw)
		if !ok {
			return fmt.Errorf("engine: MultiplePiles: Zone$ %q not resolvable", raw)
		}
		zone = z
	}
	spec, _ := a.Params.Param("ValidCards")
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: MultiplePiles: %w", err)
	}
	players = rotateToFront(players, a.Controller)
	var record [][][]CardID
	for _, p := range players {
		if g.Player(p).Lost {
			continue
		}
		var pool []CardID
		if def, ok := a.Params.Param("DefinedCards"); ok {
			if pool, err = definedCards(source, def, a.refs()); err != nil {
				return fmt.Errorf("engine: MultiplePiles: %w", err)
			}
		} else {
			pool = append([]CardID(nil), g.Zone(zone, p).Cards()...)
		}
		if spec != "" {
			pool = filterValid(g, pool, spec, source.Controller(), a.Source)
		}
		var list [][]CardID
		for i := 1; i < piles; i++ {
			pile := controller.ChooseCardsForEffect(g, p, a.Source, pool, 0, len(pool))
			if err := checkChoice(pile, pool, 0, len(pool)); err != nil {
				return fmt.Errorf("engine: MultiplePiles: %w", err)
			}
			list = append(list, pile)
			pool = withoutCards(pool, pile)
		}
		list = append(list, pool)
		record = append(record, list)
	}
	if !hasParam(a, "RandomChosen") {
		return nil
	}
	for _, list := range record {
		for _, id := range list[g.randomIndex(len(list))] {
			source.Memory.Remember(CardEntity(id))
		}
	}
	if err := g.resolveAdditionalKey(a, controller, "ChosenPile"); err != nil {
		return err
	}
	source.Memory.ClearRemembered()
	return nil
}
