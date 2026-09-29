package engine

//enginelint:allow game control ability effecthelpers card condition id zone valid defined

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// removeFromMatchEffect is RemoveFromMatchEffect.java: every card in the
// game matching RemoveType$ (IncludeSideboard$ adds sideboards), or else
// the Defined$/targeted cards, ceases to exist. Java also drops them from
// the rest of the match and, with RemoveFromInventory$, from the player's
// collection -- bookkeeping outside a single game, which is all this port
// plays.
//
// The RemoveType$ scan walks every CardID ever allocated rather than a
// zone's own Cards() set, so a melded secondary (ADR-0032) -- Zone ==
// Battlefield, but deliberately absent from that zone's own collection --
// is skipped by its own Melded flag here instead of by never being offered.
// Java's own scan (Game.forEachCardInGame, Game.java:757-786) walks each
// zone's own getCards(), which the identical PlayerZoneBattlefield.addToMelded
// mechanism already excludes it from (rules review on the merged Meld
// commit).
type removeFromMatchEffect struct{}

func (removeFromMatchEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectParams(a, "RemoveFromMatch", "Condition"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	var cards []CardID
	if spec, ok := a.Params.Param("RemoveType"); ok {
		parsed := valid.Parse(spec)
		sideboard := hasParam(a, "IncludeSideboard")
		for i := 1; i < len(g.cards); i++ {
			c := &g.cards[i]
			if c.Zone == None || (c.Zone == Sideboard && !sideboard) || c.Melded {
				continue
			}
			if Matches(g, c, parsed, a.Controller, a.Source) {
				cards = append(cards, CardID(i))
			}
		}
	} else {
		var err error
		if cards, err = targetedOrDefinedCards(source, a.Params, a.refs()); err != nil {
			return fmt.Errorf("engine: RemoveFromMatch: %w", err)
		}
	}
	for _, id := range cards {
		g.ceaseToExist(id)
	}
	return nil
}
