package engine

//enginelint:allow ability control game effecthelpers card condition animate defined id zone

import "fmt"

// exchangePowerEffect is PowerExchangeEffect.java: two creatures -- the
// host and the one target, or the two targets -- exchange power. Each gets
// the other's current power as a Layer 7b setting under one timestamp,
// until end of turn unless Duration$ Permanent. Both must still be on the
// battlefield.
type exchangePowerEffect struct{}

func (exchangePowerEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectParams(a, "ExchangePower", "Condition", "BasePower"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	permanent, until, err := animateDuration(a, "ExchangePower")
	if err != nil {
		return err
	}
	if until != "" {
		d, _ := a.Params.Param("Duration")
		return fmt.Errorf("engine: ExchangePower: Duration$ %q not resolvable yet", d)
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ExchangePower: %w", err)
	}
	var c1, c2 CardID
	switch len(cards) {
	case 0:
		return nil
	case 1:
		c1, c2 = a.Source, cards[0]
	default:
		c1, c2 = cards[0], cards[1]
	}
	if g.Card(c1).Zone != Battlefield || g.Card(c2).Zone != Battlefield {
		return nil
	}
	p1, ok1 := g.Card(c1).Power()
	p2, ok2 := g.Card(c2).Power()
	if !ok1 || !ok2 {
		return fmt.Errorf("engine: ExchangePower: a power is not resolvable")
	}
	g.timestamp++
	ts := g.timestamp
	g.addAnimate(animateRecord{Card: c1, Timestamp: ts, Permanent: permanent, Power: p2, HasPower: true})
	g.addAnimate(animateRecord{Card: c2, Timestamp: ts, Permanent: permanent, Power: p1, HasPower: true})
	return nil
}
