package engine

//enginelint:allow ability card condition control effecthelpers game

import "fmt"

// losePerpetualEffect is LosePerpetualEffect.java: a granted trigger that,
// having fired, perpetually loses itself -- Racketeer Boss's "When you cast
// this spell, create a Treasure token and this spell perpetually loses this
// ability." Java acts only when the ability runs under a trigger
// (sa.getTrigger() != null, LosePerpetualEffect.java:20), finds the host's
// changedCardTraits row whose triggers hold that exact trigger (:22-27) and
// removes the row and its perpetual record by timestamp (:29-31). Here the
// trigger's grant row id rides on the ability (triggeredObjects.grant,
// stamped by every trigger scan through triggerFaces) and the row is dropped
// from the host's overlay (grantedTriggers, card.go) -- by id, so a second
// grant of the same compiled SVar on the same card survives. A printed
// trigger, or no trigger at all, carries no row id: nothing is removed and
// nothing fails, as in Java, whose toRemove stays 0.
//
// ConditionDefined$ Remembered (Pass the Torch's "if you do") is resolved by
// isPresentMatches (trigger.go); any other ConditionDefined$, and Condition$,
// are refused rather than read as never met (GO-7).
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/LosePerpetualEffect.java's
// resolve (LosePerpetualEffect.java:14-32).
type losePerpetualEffect struct{}

func (losePerpetualEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectParams(a, "LosePerpetual", "Condition"); err != nil {
		return err
	}
	if d, ok := a.Params.Param("ConditionDefined"); ok && d != "Remembered" {
		return fmt.Errorf("engine: LosePerpetual: ConditionDefined$ %q not resolvable yet", d)
	}
	host := g.Card(a.Source)
	if !subAbilityConditionMet(g, host, a.Amounts, a.Params) {
		return nil
	}
	if a.triggered.grant == 0 {
		return nil
	}
	if grants, found := host.withoutGrant(a.triggered.grant); found {
		host.grants = grants
	}
	return nil
}
