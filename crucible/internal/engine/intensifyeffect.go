package engine

//enginelint:allow game ability control effecthelpers card condition id zone valid defined

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// intensifyEffect is IntensifyEffect.java: each card -- every card in the
// game matching AllDefined$, else the Defined$ (default Self) or targeted
// cards -- has its intensity raised by Amount$ (default 1).
//
// AllDefined$'s scan walks every CardID ever allocated, so it must skip a
// melded secondary (ADR-0032) explicitly by its own Melded flag the same way
// removeFromMatchEffect does: Java's own scan reads each zone's own
// getCards(), which already excludes it (removefrommatcheffect.go's own doc
// comment has the citation).
type intensifyEffect struct{}

func (intensifyEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectParams(a, "Intensify", "Condition"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	n, err := optionalAmount(g, a, "Intensify", "Amount", 1)
	if err != nil {
		return err
	}
	var cards []CardID
	if spec, ok := a.Params.Param("AllDefined"); ok {
		parsed := valid.Parse(spec)
		for i := 1; i < len(g.cards); i++ {
			id := CardID(i)
			if g.cards[i].Zone == None || g.cards[i].Melded {
				continue
			}
			if Matches(g, g.Card(id), parsed, a.Controller, a.Source) {
				cards = append(cards, id)
			}
		}
	} else {
		cards, err = targetedOrDefinedCards(source, a.Params, a.refs())
		if err != nil {
			return fmt.Errorf("engine: Intensify: %w", err)
		}
	}
	for _, id := range cards {
		g.Card(id).Intensity += n
	}
	return nil
}
