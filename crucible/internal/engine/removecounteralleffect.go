// RemoveCounterAll: CR 122.1, removeCounterEffect's own battlefield-sweep
// sibling and PutCounterAll's own mirror image -- 19 real (AB|DB)$
// RemoveCounterAll lines, every one naming both ValidCards$ and
// CounterType$; CounterNum$ is a resolvable amount (6) or absent,
// defaulting to 1, or AllCounters$ (12) overrides it entirely with the
// target's own current count of that one kind, computed per target the
// identical way removeCounterEffect's own CounterNum$ All does -- a
// distinct literal param here rather than a CounterNum$ value,
// CountersRemoveAllEffect.java's own `sa.hasParam("AllCounters")` check. No real
// line names ValidTgts$ or AllCounterTypes$ at all: every sweep here is
// unfiltered by controller and names exactly one counter kind.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/CountersRemoveAllEffect.java's
// resolve. Reuses removeCounters (removecountereffect.go) outright for the
// per-card body -- the first M6 pack whose own "All" sibling shares its
// per-entity write path with its own singular effect's file directly,
// rather than each effect owning a private copy.

package engine

//enginelint:allow id card game ability condition control zone valid amount removecountereffect parts

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// removeCounterAllUnresolvedParams names CountersRemoveAllEffect's own
// params past ValidCards$/CounterType$/CounterNum$/AllCounters$ this port
// does not evaluate. Every one fails the whole line loudly: ValidZone$ (3)
// -- putCounterAllEffect's own identical Battlefield-only-sweep gap;
// AllCounterTypes$ (0, defensive) -- removes every kind at once,
// CountersRemoveEffect's/removeCounterEffect's own CounterType$ All shape
// for the single-target effect, listed here even at zero real lines rather
// than silently ignoring a card that does combine it with a sweep some day;
// Condition$/ConditionDefined$ (0/0, defensive) -- condition.go's own
// subAbilityConditionMet would otherwise silently no-op a card naming
// either without ConditionPresent$ alongside it, dealDamageEffect's own
// identical reasoning.
var removeCounterAllUnresolvedParams = [...]string{
	"ValidZone", "AllCounterTypes"}

// removeCounterAllEffect resolves Mode$/DB$/AB$ RemoveCounterAll.
// ConditionPresent$/ConditionCompare$/ConditionCheckSVar$/
// ConditionSVarCompare$ are resolved through subAbilityConditionMet
// (condition.go), the identical way every other M6 effect's own does.
type removeCounterAllEffect struct{}

func (removeCounterAllEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range removeCounterAllUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: RemoveCounterAll: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	validCards, ok := a.Params.Param("ValidCards")
	if !ok {
		return fmt.Errorf("engine: RemoveCounterAll: ValidCards$ missing")
	}
	spec := valid.Parse(validCards)

	counterTypeRaw, ok := a.Params.Param("CounterType")
	if !ok {
		return fmt.Errorf("engine: RemoveCounterAll: CounterType$ missing")
	}
	if strings.Contains(counterTypeRaw, ",") || strings.EqualFold(counterTypeRaw, "Any") || strings.EqualFold(counterTypeRaw, "All") {
		return fmt.Errorf("engine: RemoveCounterAll: CounterType$ %q not resolvable yet", counterTypeRaw)
	}
	counterType := CounterType(strings.ToUpper(counterTypeRaw))

	_, allAmount := a.Params.Param("AllCounters")
	var amount int
	if !allAmount {
		counterNumParam, ok := a.Params.Param("CounterNum")
		if !ok {
			counterNumParam = "1"
		}
		amount, ok = resolveNamedAmount(g, a.Amounts, source, counterNumParam)
		if !ok {
			return fmt.Errorf("engine: RemoveCounterAll: CounterNum$ %q is not resolvable", counterNumParam)
		}
	}

	for _, pid := range g.Players() {
		for _, cid := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(cid)
			if !Matches(g, c, spec, a.Controller, a.Source) {
				continue
			}
			removeCounters(g, a.Source, CardEntity(cid), &c.Counters, counterType, false, allAmount, amount)
		}
	}
	return nil
}
