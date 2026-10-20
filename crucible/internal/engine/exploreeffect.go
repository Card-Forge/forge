package engine

//enginelint:allow id card game ability defined condition control amount parts event zone

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// exploreEffect is ExploreEffect.java (CR 701.44): each exploring permanent
// (targetedOrDefinedCards, default Self) Num$ times (default 1) has its
// controller reveal the top card of their library. A land goes to hand;
// otherwise the controller may put it into the graveyard (ConfirmEffect)
// and the explorer, if still on the battlefield, gets a +1/+1 counter.
//
// Not ported: the Explore replacement family and the per-player "explored this
// turn" count -- neither exists yet. Triggered objects: the Explorer is the
// triggered card; Defined$ TriggeredExplored is not recorded (no corpus line).
type exploreEffect struct{}

func (exploreEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	amount := 1
	if raw, ok := a.Params.Param("Num"); ok {
		n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
		if !ok {
			return fmt.Errorf("engine: Explore: Num$ %q not resolvable yet", raw)
		}
		amount = n
	}
	explorers, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Explore: %w", err)
	}
	explorers, err = g.orderCardsByTheirOwners(controller, explorers, Battlefield)
	if err != nil {
		return fmt.Errorf("engine: Explore: %w", err)
	}
	for _, id := range explorers {
		c := g.Card(id)
		pl := c.Controller()
		for i := 0; i < amount; i++ {
			revealedLand := false
			top := NoCard
			if library := g.Zone(Library, pl).Cards(); len(library) > 0 {
				top = library[0]
				if g.Card(top).Type().Has(cardtype.Land) {
					g.moveByEffect(controller, top, Hand, 0, NoPlayer, false)
					g.checkChangesZoneAllTriggers(controller, []CardID{top}, Library, Hand)
					revealedLand = true
				} else if controller.ConfirmEffect(g, pl, a.Source) {
					g.moveByEffect(controller, top, Graveyard, 0, NoPlayer, false)
					g.checkChangesZoneAllTriggers(controller, []CardID{top}, Library, Graveyard)
				}
			}
			if !revealedLand && c.Zone == Battlefield {
				// The explorer's controller places (ExploreEffect.java:63, :98).
				g.addCardCounters(controller, pl, a.Source, id, P1P1, 1)
			}
			g.pushTriggeredAbilities(controller, g.exploresMatches(id, top))
		}
	}
	return nil
}
