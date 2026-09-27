package engine

//enginelint:allow id card game ability condition control trigger defined

import "fmt"

// chaosEnsuesEffect is ChaosEnsuesEffect.java: CR 311.7's "chaos ensues".
// Without Defined$, every chaos ability live where its TriggerZones$ allows
// fires -- the active plane's own, in practice. With Defined$, chaos ensues
// for those cards only, from whatever zone they are in, a revealed plane
// still in the planar deck included (checkChaosEnsuesOnTriggers).
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/ChaosEnsuesEffect.java's
// resolve: the Planechase gate (:33-35), the Defined$ affected set
// (:41-57), the trigger run (:59). The zone widening and restore around the
// run (:45-49, :61-70) needs no state here: checkChaosEnsuesOnTriggers
// skips the TriggerZones$ gate for the affected card instead.
type chaosEnsuesEffect struct{}

func (chaosEnsuesEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if !g.planechaseActive {
		// Not a Planechase game: nothing happens (ChaosEnsuesEffect.java:33).
		return nil
	}
	activator := a.Controller
	defined, ok := a.Params.Param("Defined")
	if !ok {
		g.checkChaosEnsuesTriggers(c, activator)
		return g.TakePendingError()
	}
	affected, err := chaosEnsuesAffected(g, source, defined, a.refs())
	if err != nil {
		return err
	}
	if affected == NoCard {
		// No Defined$ card has a chaos ability: nothing triggers, not even
		// the active plane's (ChaosEnsuesEffect.java:54-56).
		return nil
	}
	g.checkChaosEnsuesOnTriggers(c, activator, affected)
	return g.TakePendingError()
}

// chaosEnsuesAffected is ChaosEnsuesEffect.java:42-52's affected list: the
// Defined$ cards carrying a chaos ability, NoCard for none. More than one
// distinct card is refused: TriggerChaosEnsues.performTest's Iterable branch
// (TriggerChaosEnsues.java:43-48) fires a trigger only when every affected
// card is its host, so two distinct planes would fire neither -- CR 311.7
// speaks only of chaos ensuing for "a particular object", and no real line
// reaches the case (the one Defined$ line, The Fertile Lands of Saulvinia's
// Defined$ Remembered, remembers DigUntil's single found plane).
func chaosEnsuesAffected(g *Game, source *Card, defined string, refs abilityRefs) (CardID, error) {
	cards, err := definedCards(source, defined, refs)
	if err != nil {
		return NoCard, err
	}
	affected := NoCard
	for _, id := range cards {
		if !hasChaosEnsuesTrigger(g.Card(id)) {
			continue
		}
		if affected != NoCard && affected != id {
			return NoCard, fmt.Errorf("engine: ChaosEnsues: Defined$ %s naming more than one plane not resolvable yet", defined)
		}
		affected = id
	}
	return affected, nil
}
