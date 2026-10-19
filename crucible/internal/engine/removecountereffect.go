// RemoveCounter: CR 122.1, PutCounter's own mirror-image effect -- 199 real
// (AB|DB)$ RemoveCounter lines, all naming Defined$ (112) -- the identical
// Defined$ Self/Enchanted/Equipped/You shape putCounterEffect already
// resolves, ValidTgts$ (36) deferred the same way (putcountereffect.go's
// own doc comment, "this port's own targeting gap" -- not yet extended to
// this file either). CounterType$ is a single literal name (171 of 201, the
// identical scope putCounterType already has) or the literal "All" (15,
// every kind the target carries, CounterNum$ ignored when it is);
// CounterNum$ is a resolvable amount (134) or the literal "All" (55, the
// target's own current count of that one kind, computed per target since a
// SVar-free literal cannot answer "how many are there" up front the way
// putCounterEffect's own CounterNum$ always can).
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/CountersRemoveEffect.java's
// resolve, trimmed hard: that file also handles Choices$'s own interactive
// battlefield-wide pick, UpTo$'s own chooseNumber prompt, and CounterType$
// Any's own chooseCounterType prompt -- three PlayerController hooks this
// port does not have, so each fails the whole line loudly (PORT-8/GO-7)
// rather than resolving two of three params and guessing at the rest.

package engine

//enginelint:allow id card game player ability defined condition control parts event amount putcountereffect resolvechoice

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// removeCounterUnresolvedParams names CountersRemoveEffect's own params
// this port does not evaluate. Every one fails the whole line loudly:
// ValidTgts$/TargetMin$/TargetMax$ (36/3/3) -- this port's own targeting
// gap, putCounterEffect's own identical scope; Choices$/ChoiceOptional$/
// TgtZone$ (9/5/4) -- an interactive battlefield-wide pick, no
// PlayerController hook; UpTo$ (5) -- chooseNumber's own interactive
// prompt, the identical gap;
// Optional$ (2) -- an interactive "would you like to remove" confirm, the
// identical gap Sacrifice's/Discard's/Mill's own Optional$ already
// document; RememberRemoved$/RememberAmount$ (28/13) -- CountersRemoveEffect.
// java's own `source.addRemembered(Pair.of(counterType, i))`/
// `source.addRemembered(totalRemoved)` remember a (type, index) pair and a
// raw integer respectively, neither an EntityID Memory.Remember (memory.go)
// has anywhere to put, and the Java source's own "TODO might need to be
// more specific" on the first suggests this is not even a clean shape in
// the oracle itself; Condition$/ConditionDefined$ (0/2) -- condition.go's
// own subAbilityConditionMet would otherwise silently no-op a card naming
// either, dealDamageEffect's own identical reasoning.
var removeCounterUnresolvedParams = [...]string{
	"ValidTgts", "TargetMin", "TargetMax",
	"TgtZone", "UpTo",
	"Condition"}

// removeCounterEffect resolves Mode$/DB$/AB$ RemoveCounter for the Defined$
// Self/Enchanted/Equipped/You shape. ConditionPresent$/ConditionCompare$/
// ConditionCheckSVar$/ConditionSVarCompare$ are resolved through
// subAbilityConditionMet (condition.go), the identical way every other M6
// effect's own does. This port has no `canRemoveCounters` check
// (CountersRemoveEffect.java's own guard against a static ability
// forbidding removal) -- no such static ability is built anywhere in this
// port, so the check would always pass, a real, narrow simplification.
type removeCounterEffect struct{}

func (removeCounterEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range removeCounterUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: RemoveCounter: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	if _, ok := a.Params.Param("Optional"); ok && !controller.ConfirmEffect(g, a.Controller, a.Source) {
		// CountersRemoveEffect.java:84-88.
		return nil
	}
	counterType, allKinds, err := removeCounterType(a.Params)
	if err != nil {
		return err
	}

	counterNumParam, ok := a.Params.Param("CounterNum")
	if !ok {
		counterNumParam = "1"
	}
	allAmount := strings.EqualFold(counterNumParam, "All")
	var amount int
	if !allAmount && !allKinds {
		amount, ok = resolveNamedAmount(g, a.Amounts, source, counterNumParam)
		if !ok {
			return fmt.Errorf("engine: RemoveCounter: CounterNum$ %q is not resolvable", counterNumParam)
		}
	}

	var cards []CardID
	var players []PlayerID
	if spec, ok := a.Params.Param("Choices"); ok {
		// CountersRemoveEffect.java:107-137: the activator picks the cards.
		if cards, err = removeCounterChoice(g, a, controller, spec); err != nil {
			return err
		}
	} else {
		defined, _ := a.Params.Param("Defined")
		cards, players, err = definedCounterTargets(g, a.Controller, source, defined, a.refs())
		if err != nil {
			return fmt.Errorf("engine: RemoveCounter: %w", err)
		}
	}

	// RememberRemoved$ remembers a (kind, index) pair per counter taken off a
	// card of a literal kind, and RememberAmount$ the total removed from
	// anything (CountersRemoveEffect.java:187-192, :217-219).
	_, rememberRemoved := a.Params.Param("RememberRemoved")
	_, rememberAmount := a.Params.Param("RememberAmount")
	total := 0
	for _, cid := range cards {
		removed := removeCounters(g, a.Source, CardEntity(cid), &g.Card(cid).Counters, counterType, allKinds, allAmount, amount)
		total += removed
		if rememberRemoved && !allKinds {
			for i := range removed {
				source.Memory.RememberRemovedCounter(counterType, i)
			}
		}
	}
	for _, pid := range players {
		total += removeCounters(g, a.Source, PlayerEntity(pid), &g.Player(pid).Counters, counterType, allKinds, allAmount, amount)
	}
	if rememberAmount && total > 0 {
		source.Memory.RememberNumber(total)
	}
	return nil
}

// removeCounters is CountersRemoveEffect.resolve's own per-entity body,
// shared between the card and the player loop above the identical way
// putCounterEffect's own single loop body already is (both write through
// the same Counters type, card.go's own doc comment on Player.Counters).
// allKinds ignores amount/allAmount entirely and empties every kind the
// entity carries, CounterType$ All's own real shape; otherwise allAmount
// reads the entity's own current count of counterType first (CounterNum$
// All, computed per entity since it can differ target to target).
func removeCounters(g *Game, source CardID, target EntityID, counters *Counters, counterType CounterType, allKinds, allAmount bool, amount int) (total int) {
	if allKinds {
		for _, kind := range counters.Kinds() {
			n := counters.Count(kind)
			counters.Add(kind, -n)
			emitCounterChanged(g, source, target, kind, -n)
			total += n
		}
		return total
	}
	n := amount
	if allAmount {
		n = counters.Count(counterType)
	}
	if n <= 0 {
		return 0
	}
	// Card.subtractCounter's delta is what was there less what is left
	// (Card.java:1910-1911): "remove two" from one removes one, from none
	// removes nothing.
	removed := counters.Count(counterType) - counters.Add(counterType, -n)
	if removed > 0 {
		emitCounterChanged(g, source, target, counterType, -removed)
	}
	return max(removed, 0)
}

// removeCounterType reads CounterType$: a single literal name (uppercased,
// putCounterType's own identical canonicalization, putcountereffect.go), or
// the literal "All" (allKinds true, amount ignored entirely). A
// comma-separated list and the literal "Any" (an interactive
// chooseCounterType prompt) fail loudly, putCounterType's own identical
// rejection.
func removeCounterType(a *compile.Ability) (t CounterType, allKinds bool, err error) {
	raw, ok := a.Param("CounterType")
	if !ok {
		return "", false, fmt.Errorf("engine: RemoveCounter: CounterType$ missing")
	}
	if strings.EqualFold(raw, "All") {
		return "", true, nil
	}
	if strings.Contains(raw, ",") || strings.EqualFold(raw, "Any") {
		return "", false, fmt.Errorf("engine: RemoveCounter: CounterType$ %q not resolvable yet", raw)
	}
	return CounterType(strings.ToUpper(raw)), false, nil
}

// removeCounterChoice is the Choices$ pick (CountersRemoveEffect.java:107-137):
// one card of ChoiceZone$ matching Choices$ by default, ChoiceNum$ of them, or
// any number with ChoiceOptional$; the effect does nothing when fewer than the
// minimum exist (:128-130). The activator answers whoever Chooser$ names
// (the class never reads it).
func removeCounterChoice(g *Game, a *Ability, controller PlayerController, spec string) ([]CardID, error) {
	zones, err := choiceZones(a, "RemoveCounter")
	if err != nil {
		return nil, err
	}
	offer, err := choiceCandidates(g, a, "RemoveCounter", zones, spec)
	if err != nil {
		return nil, err
	}
	lo, hi := 1, 1
	if _, ok := a.Params.Param("ChoiceOptional"); ok {
		lo, hi = 0, len(offer)
	}
	if raw, ok := a.Params.Param("ChoiceNum"); ok {
		n, ok := resolveNamedAmount(g, a.Amounts, g.Card(a.Source), raw)
		if !ok {
			return nil, fmt.Errorf("engine: RemoveCounter: ChoiceNum$ %q is not resolvable", raw)
		}
		lo, hi = n, n
	}
	if len(offer) < lo {
		return nil, nil
	}
	return askChoiceCards(g, controller, a, "RemoveCounter", a.Controller, offer, lo, hi)
}
