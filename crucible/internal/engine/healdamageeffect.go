package engine

//enginelint:allow card game ability defined condition control parts

import "fmt"

// healDamageEffect is HealDamageEffect.java: all damage marked on each
// targeted or Defined$ card (default Self) is removed (Card.healDamage).
type healDamageEffect struct{}

func (healDamageEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: HealDamage: %w", err)
	}
	for _, id := range cards {
		g.Card(id).Damage.Heal()
	}
	return nil
}
