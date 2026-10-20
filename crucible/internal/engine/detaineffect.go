package engine

//enginelint:allow control ability game effecthelpers card condition defined id

import "fmt"

// detainEffect is DetainEffect.java (CR 701.35): each targeted permanent is
// detained by the activator until that player's next turn -- it can't attack
// or block, and its activated abilities can't be activated
// (Card.detain, removed by a Cleanup.addUntil command for the activator).
type detainEffect struct{}

func (detainEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Detain"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Detain: %w", err)
	}
	for _, id := range cards {
		c := g.Card(id)
		c.detainedBy = append(c.detainedBy, a.Controller)
	}
	return nil
}

// isDetained is Card.isDetained: some player's detain still holds.
func (c *Card) isDetained() bool { return len(c.detainedBy) > 0 }

// endDetains lifts every detain p made, as p's turn begins.
func (g *Game) endDetains(p PlayerID) {
	for i := range g.cards {
		c := &g.cards[i]
		if len(c.detainedBy) == 0 {
			continue
		}
		kept := c.detainedBy[:0]
		for _, d := range c.detainedBy {
			if d != p {
				kept = append(kept, d)
			}
		}
		c.detainedBy = kept
	}
}
