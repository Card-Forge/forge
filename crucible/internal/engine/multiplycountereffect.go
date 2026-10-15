// MultiplyCounter: CR 121.5, PutCounter's own doubling sibling -- 53 real
// (AB|DB)$ MultiplyCounter lines, 38 naming Defined$ (Self dominant, 10;
// Targeted, 13; TriggeredAttackerLKICopy/TriggeredSourceLKICopy, 8
// combined, deferred -- this port's own trigger-context Defined$ gap,
// defined.go's own doc comment) and 14 naming ValidTgts$ instead --
// SpellAbilityEffect.getTargetEntities(sa)'s own either/or contract, ported
// as targetedOrDefinedCards (defined.go) since every real line names a card
// spec on both sides, never a player -- the one "Defined$ You" line
// (doubling every kind of counter a PLAYER has, poison/energy) fails
// loudly instead of silently reading it as a card, definedCards' own
// unresolved-Defined$-value error already covering it without a special
// case here. CounterType$ present (41) names a single literal kind; absent
// (12) doubles every kind the target carries -- CountersMultiplyEffect.
// java's own `getCounterType(sa)` returning null for "double each kind of
// counter," a distinct shape from removeCounterEffect's own literal
// CounterType$ All (this API's own real corpus never spells "All" here).
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/CountersMultiplyEffect.java's
// resolve, trimmed hard: that file also re-reads each target through
// Game.getCardState to guard a timestamp-stale LKI copy (getTargetEntities'
// own possible dangling reference into a card that has since left the
// battlefield or changed state) -- this port's own targetedOrDefinedCards
// never returns a card the game itself no longer tracks, so the guard has
// nothing to protect against here and is not ported.

package engine

//enginelint:allow id card game ability defined condition control parts event replacement

import (
	"fmt"
	"strconv"
	"strings"
)

// multiplyCounterUnresolvedParams names CountersMultiplyEffect's own params
// this port does not evaluate. Every one fails the whole line loudly
// (PORT-8/GO-7): Condition$/ConditionDefined$ (0/1) -- condition.go's own
// subAbilityConditionMet would otherwise silently no-op a card naming
// either without ConditionPresent$ alongside it, dealDamageEffect's own
// identical reasoning.
var multiplyCounterUnresolvedParams = [...]string{
	"Condition"}

// multiplyCounterEffect resolves Mode$/DB$/AB$ MultiplyCounter.
// ConditionPresent$/ConditionCompare$/ConditionCheckSVar$/
// ConditionSVarCompare$ are resolved through subAbilityConditionMet
// (condition.go), the identical way every other M6 effect's own does.
type multiplyCounterEffect struct{}

func (multiplyCounterEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range multiplyCounterUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: MultiplyCounter: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	multiplierRaw, ok := a.Params.Param("Multiplier")
	if !ok {
		multiplierRaw = "2"
	}
	multiplier, err := strconv.Atoi(multiplierRaw)
	if err != nil {
		return fmt.Errorf("engine: MultiplyCounter: Multiplier$ %q is not resolvable", multiplierRaw)
	}
	factor := multiplier - 1

	var counterType CounterType
	allKinds := true
	if raw, ok := a.Params.Param("CounterType"); ok {
		if strings.Contains(raw, ",") || strings.EqualFold(raw, "Any") {
			return fmt.Errorf("engine: MultiplyCounter: CounterType$ %q not resolvable yet", raw)
		}
		counterType = CounterType(strings.ToUpper(raw))
		allKinds = false
	}

	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: MultiplyCounter: %w", err)
	}

	for _, cid := range cards {
		c := g.Card(cid)
		if allKinds {
			// Every amount is read before any is placed: Java fills one
			// counter table and places it at the end (CountersMultiplyEffect.
			// java:63-69).
			kinds := c.Counters.Kinds()
			amounts := make([]int, len(kinds))
			for i, kind := range kinds {
				amounts[i] = c.Counters.Count(kind) * factor
			}
			for i, kind := range kinds {
				g.multiplyPlace(controller, a, cid, kind, amounts[i])
			}
			continue
		}
		g.multiplyPlace(controller, a, cid, counterType, c.Counters.Count(counterType)*factor)
	}
	return nil
}

// multiplyPlace puts n more ct counters on cid for a: the activator places
// them (CountersMultiplyEffect.java:43, :62-65), and the counter table's
// replaceCounterEffect (:69) runs the AddCounter replacements (Hardened Scales)
// and Mode$ CantPutCounter first.
func (g *Game) multiplyPlace(controller PlayerController, a *Ability, cid CardID, ct CounterType, n int) {
	if n <= 0 {
		return
	}
	if n = g.countersReplaced(controller, a.Controller, CardEntity(cid), ct, n); n > 0 {
		g.addCardCounters(controller, a.Controller, a.Source, cid, ct, n)
	}
}
