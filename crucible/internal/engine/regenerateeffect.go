package engine

//enginelint:allow card game ability defined condition control zone

import "fmt"

// regenerateUnresolvedParams: RegenerationAbility$ (a follow-up ability when
// the shield is used), RememberObjects$ (extra remembered objects on the
// shield effect), and bare activation gates.
var regenerateUnresolvedParams = [...]string{
	"RegenerationAbility", "RememberObjects"}

// regenerateEffect is RegenerateEffect.java: each targeted or Defined$
// permanent (default Self) still on the battlefield gets one regeneration
// shield (Card.RegenShields) until end of turn; Game.regenerate spends it on
// the next destruction. Java builds one Regeneration effect card per
// resolution, each replacing one destruction per remembered permanent, so a
// counter is the same observable state. 262 real lines.
type regenerateEffect struct{}

func (regenerateEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range regenerateUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Regenerate: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Regenerate: %w", err)
	}
	for _, id := range cards {
		if c := g.Card(id); c.Zone == Battlefield {
			c.RegenShields++
		}
	}
	return nil
}
