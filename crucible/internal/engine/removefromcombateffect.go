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
// params this port does not evaluate. Every one fails the whole line loudly
// (PORT-8/GO-7): UnblockCreaturesBlockedOnlyBy$ (3) -- a further
// "unblock what this attacker was blocking alone" mechanic distinct from
// removing the named card itself from combat; Condition$/ConditionDefined$
// (0/1) -- condition.go's own subAbilityConditionMet would otherwise
// silently no-op a card naming either without ConditionPresent$ alongside
// it, dealDamageEffect's own identical reasoning.
var removeFromCombatUnresolvedParams = [...]string{
	"UnblockCreaturesBlockedOnlyBy",
	"Condition"}

// removeFromCombatEffect resolves Mode$/DB$/AB$ RemoveFromCombat.
// ConditionPresent$/ConditionCompare$/ConditionCheckSVar$/
// ConditionSVarCompare$ are resolved through subAbilityConditionMet
// (condition.go), the identical way every other M6 effect's own does.
type removeFromCombatEffect struct{}

func (removeFromCombatEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	for _, key := range removeFromCombatUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: RemoveFromCombat: %s$ not resolvable yet", key)
		}
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
		g.removeFromCombat(id)
		if remember {
			source.Memory.Remember(CardEntity(id))
		}
	}
	return nil
}
