package engine

//enginelint:allow ability game control effecthelpers card condition zone defined player id

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/pkg/collect"
)

// reorderZoneEffect is ReorderZoneEffect.java: each target player's Zone$
// is reordered -- at random with Random$ (Collections.shuffle on the game's
// random, the same draw Game.Shuffle takes), otherwise in the order that
// player chooses (orderMoveToZoneList). Zone.setCards replaces the order
// without a zone change, so no timestamp moves.
type reorderZoneEffect struct{}

func (reorderZoneEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "ReorderZone"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	raw, ok := a.Params.Param("Zone")
	if !ok {
		return fmt.Errorf("engine: ReorderZone: Zone$ missing")
	}
	zone, ok := ZoneByName(raw)
	if !ok {
		return fmt.Errorf("engine: ReorderZone: Zone$ %q not resolvable", raw)
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ReorderZone: %w", err)
	}
	_, random := a.Params.Param("Random")
	for _, p := range players {
		if g.Player(p).Lost {
			continue
		}
		if random {
			g.Shuffle(zone, p)
			continue
		}
		cards := append([]CardID(nil), g.Zone(zone, p).Cards()...)
		if len(cards) == 0 {
			continue
		}
		ordered := controller.OrderCardsForZone(g, p, cards, zone)
		if err := checkChoice(ordered, cards, len(cards), len(cards)); err != nil {
			return fmt.Errorf("engine: ReorderZone: %w", err)
		}
		g.setZoneOrder(zone, p, ordered)
	}
	return nil
}

// setZoneOrder is Zone.setCards: the zone's cards become ids, in that order,
// with no zone change and so no new timestamps. ids must hold exactly the
// zone's cards.
func (g *Game) setZoneOrder(kind ZoneType, owner PlayerID, ids []CardID) {
	z := g.Zone(kind, owner)
	set := collect.NewOrderedSet[CardID](len(ids))
	for _, id := range ids {
		set.Add(id)
	}
	z.cards = set
}
