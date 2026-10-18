package engine

//enginelint:allow game control ability effecthelpers card condition defined id zone

import "fmt"

// removeFromGameEffect is RemoveFromGameEffect.java: each Defined$ (or
// targeted) card ceases to exist -- GameAction.ceaseToExist with skipTrig,
// so no leaves-the-battlefield trigger runs. The card moves to ZoneType
// None, the zone Java parks it in.
type removeFromGameEffect struct{}

func (removeFromGameEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectParams(a, "RemoveFromGame", "Condition"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: RemoveFromGame: %w", err)
	}
	for _, id := range cards {
		g.ceaseToExist(id)
	}
	return nil
}

// ceaseToExist is GameAction.ceaseToExist(c, true): id leaves whatever zone
// it is in -- the stack included, dropping the spell -- for ZoneType None,
// with no zone-change trigger. On the stack only the spell goes
// (GameAction.java:970-972 calls MagicStack.remove(Card), MagicStack.java:
// 799-805, which removes spell items alone): a triggered or activated ability
// whose source is the card stays and resolves on its own.
func (g *Game) ceaseToExist(id CardID) {
	c := g.Card(id)
	if c.Zone == None {
		return
	}
	if c.Zone == Stack {
		g.removeSpellFromStack(id)
	}
	if c.Zone == Battlefield {
		g.removeFromCombat(id)
	}
	g.Move(id, None, c.Owner)
}
