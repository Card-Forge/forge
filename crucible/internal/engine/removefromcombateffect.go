// RemoveFromCombat: CR 506.4 -- 24 real (AB|DB)$ RemoveFromCombat lines, 23
// naming Defined$ and 4 ValidTgts$ (targetedOrDefinedCards, defined.go,
// default "Self"). Game.removeFromCombat (combat.go) is the actual CR
// 506.4 mechanic, shared with whatever future caller needs it (a Battle
// losing its last defender, first/second strike interactions, ... --
// combat.go's own doc comment); this file is a target-resolution loop
// around that one call.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/RemoveFromCombatEffect.java's
// resolve. Java's own timestamp-staleness re-check
// (game.getCardState/equalsWithGameTimestamp) is not ported, multiplyCounterEffect's
// own identical reasoning; saveLKI is not ported either -- this port's own
// Game.LKI (the removed port log's "Last-known-information lands") already covers
// the one subset the real corpus's own dies triggers read, and a card
// merely leaving combat, still on the battlefield, needs no LKI copy at all.

package engine

//enginelint:allow id card game ability defined condition control parts combat zone

import "fmt"

// removeFromCombatUnresolvedParams names RemoveFromCombatEffect's own
// params this port does not evaluate: none now. UnblockCreaturesBlockedOnlyBy$
// is unblockAttackersBlockedOnlyBy.

// removeFromCombatEffect resolves Mode$/DB$/AB$ RemoveFromCombat.
// ConditionPresent$/ConditionCompare$/ConditionCheckSVar$/
// ConditionSVarCompare$ are resolved through subAbilityConditionMet
// (condition.go), the identical way every other M6 effect's own does.
type removeFromCombatEffect struct{}

func (removeFromCombatEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	_, remember := a.Params.Param("RememberRemovedFromCombat")
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: RemoveFromCombat: %w", err)
	}
	for _, id := range cards {
		if g.Card(id).Zone != Battlefield {
			continue
		}
		if spec, ok := a.Params.Param("UnblockCreaturesBlockedOnlyBy"); ok {
			// RemoveFromCombatEffect.java:47-65: the attackers the named creature
			// alone was blocking become unblocked (Ydwen Efreet, False Orders).
			named, err := definedCards(source, spec, a.refs())
			if err != nil {
				return fmt.Errorf("engine: RemoveFromCombat: UnblockCreaturesBlockedOnlyBy$: %w", err)
			}
			if len(named) > 0 {
				g.unblockAttackersBlockedOnlyBy(named[0])
			}
		}
		g.removeFromCombat(id)
		if remember {
			source.Memory.Remember(CardEntity(id))
		}
	}
	return nil
}
