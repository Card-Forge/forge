package engine

//enginelint:allow card game player ability defined condition control effecthelpers mana

import "fmt"

// drainManaEffect is DrainManaEffect.java: each target player (default You)
// empties their mana pool; with DrainMana$ the activator gets it all.
// RememberDrainedMana$ remembers an Integer, which Memory cannot hold, so
// that line fails closed. Mana burn (StaticAbilityUnspentMana) has no
// static in this port to switch it on.
type drainManaEffect struct{}

func (drainManaEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range [...]string{"RememberDrainedMana"} {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: DrainMana: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: DrainMana: %w", err)
	}
	var drained Pool
	for _, p := range players {
		if g.Player(p).Lost {
			continue
		}
		drained.merge(g.Player(p).ManaPool)
		g.Player(p).ManaPool.Empty()
	}
	if hasParam(a, "DrainMana") {
		g.Player(a.Controller).ManaPool.merge(drained)
	}
	return nil
}
