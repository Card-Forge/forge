package engine

//enginelint:allow id card game ability defined condition control parts zone

import "fmt"

// exchangeControlUnresolvedParams: target-selection restrictions targeting.go
// does not enforce, and Charm's ModeCost$ linkage. TargetsWithSharedCardType$,
// TargetsWithSharedTypes$, TargetsWithRelatedProperty$,
// TargetsWithDefinedController$, TargetingPlayer$ and TargetsAtRandom$ are
// enforced when the targets are chosen (targeting.go), so they no longer
// refuse here.
var exchangeControlUnresolvedParams = [...]string{
	"TargetsWithSameCardType", "TargetsWithDifferentControllers",
	"ModeCost",
	"Condition", "SorcerySpeed",
}

// exchangeControlEffect is ControlExchangeEffect.java: two permanents trade
// controllers. With targets, the first target is object1 and either the
// Defined$ card or the second target is object2; without targets, Defined$
// names both (object2 first, as Java reads it). Both must still be on the
// battlefield; Optional$ asks first; RememberExchanged$ remembers both.
type exchangeControlEffect struct{}

func (exchangeControlEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range exchangeControlUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: ExchangeControl: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	var targets []CardID
	for _, e := range a.Targets {
		if id, ok := e.AsCard(); ok {
			targets = append(targets, id)
		}
	}
	_, usesTargeting := a.Params.Param("ValidTgts")
	object1, object2 := NoCard, NoCard
	if usesTargeting && len(targets) > 0 {
		object1 = targets[0]
	}
	if spec, ok := a.Params.Param("Defined"); ok {
		cards, err := definedCards(source, spec, a.refs())
		if err != nil {
			return fmt.Errorf("engine: ExchangeControl: %w", err)
		}
		if len(cards) > 0 {
			object2 = cards[0]
		}
		if len(cards) > 1 && !usesTargeting {
			object1 = cards[1]
		}
	} else if len(targets) > 1 {
		object2 = targets[1]
	}
	if object1 == NoCard || object2 == NoCard ||
		g.Card(object1).Zone != Battlefield || g.Card(object2).Zone != Battlefield {
		return nil
	}
	if _, ok := a.Params.Param("Optional"); ok && !controller.ConfirmEffect(g, a.Controller, a.Source) {
		return nil
	}
	p1, p2 := g.Card(object1).Controller(), g.Card(object2).Controller()
	g.changeController(controller, object2, p1)
	g.changeController(controller, object1, p2)
	if _, ok := a.Params.Param("RememberExchanged"); ok {
		source.Memory.Remember(CardEntity(object1))
		source.Memory.Remember(CardEntity(object2))
	}
	return nil
}
