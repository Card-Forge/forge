package engine

//enginelint:allow card game ability defined condition control

import "fmt"

// lookAtEffect is LookAtEffect.java: the activator looks at the targeted or
// Defined$ cards (GameAction.revealTo). The engine is omniscient -- no
// player-visibility state exists to update -- so resolving it changes
// nothing; it still resolves the card list so an unresolvable Defined$ fails
// closed rather than passing silently.
type lookAtEffect struct{}

func (lookAtEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if _, err := targetedOrDefinedCards(source, a.Params, a.refs()); err != nil {
		return fmt.Errorf("engine: LookAt: %w", err)
	}
	return nil
}
