// MoveCounter: CR 121.5, "move N counters of a kind from one object to
// another" -- 30 real (AB|DB)$ MoveCounter lines, every one Source$ Self
// (15) or another definedCards-resolvable value (9, 6 of them a value
// (Creature/Permanent/ParentTarget) this port's own definedCards has no
// case for and so fails loudly rather than guessing), no line naming
// ValidSource$/ValidDefined$ at all -- CountersMoveEffect.java's own "many
// sources to one destination"/"one source to many destinations" branches,
// each needing a chooseCardsForEffect/chooseNumber interactive step this
// port's own PlayerController has no hook for, simply do not exist in the
// real corpus this port has read. The remaining shape -- one Source$ card,
// one or more ValidTgts$/Defined$ destinations (targetedOrDefinedCards,
// defined.go, default "Self" the identical way Java's own
// getDefinedCardsOrTargeted(sa) with no override does) -- resolves outright.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/CountersMoveEffect.java's
// resolve, trimmed hard: that file's own two-target shape (TargetMin$ 2, the
// FIRST target itself the source rather than Source$ naming it) is not
// ported -- see moveCounterUnresolvedParams below.

package engine

//enginelint:allow id card game ability defined condition control amount parts event

import (
	"fmt"
	"strings"
)

// moveCounterUnresolvedParams names CountersMoveEffect's own params this
// port does not evaluate. Every one fails the whole line loudly (PORT-8/
// GO-7): TargetMin$/TargetMax$ (7/7, always literal 2 in the real corpus) --
// the two-target-source shape (the ability's own FIRST target is the
// source, not Source$), a distinct branch this file does not resolve;
// ValidSource$/ValidDefined$ (0/0, defensive) -- the "many sources to one
// destination"/"one source to many destinations" branches, above, listed
// even at zero real lines rather than silently misreading either as a
// plain Source$ value; NonZero$ (1) -- CounterNum$ Any's own "at least one"
// floor, moot on its own since CounterNum$ Any already fails below.
var moveCounterUnresolvedParams = [...]string{
	"TargetMin", "TargetMax", "ValidSource", "ValidDefined", "NonZero",
}

// moveCounterEffect resolves Mode$/DB$/AB$ MoveCounter for the one-source,
// one-or-more-destinations shape.
type moveCounterEffect struct{}

func (moveCounterEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range moveCounterUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: MoveCounter: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	counterTypeRaw, ok := a.Params.Param("CounterType")
	if !ok {
		return fmt.Errorf("engine: MoveCounter: CounterType$ missing")
	}
	var counterType CounterType
	allKinds := false
	switch {
	case strings.EqualFold(counterTypeRaw, "All"):
		allKinds = true
	case strings.Contains(counterTypeRaw, ","), strings.EqualFold(counterTypeRaw, "Any"), strings.EqualFold(counterTypeRaw, "EachNotOn"):
		return fmt.Errorf("engine: MoveCounter: CounterType$ %q not resolvable yet", counterTypeRaw)
	default:
		counterType = CounterType(strings.ToUpper(counterTypeRaw))
	}

	counterNumParam, ok := a.Params.Param("CounterNum")
	if !ok {
		counterNumParam = "1"
	}
	if strings.EqualFold(counterNumParam, "Any") {
		return fmt.Errorf("engine: MoveCounter: CounterNum$ %q not resolvable yet", counterNumParam)
	}
	allAmount := strings.EqualFold(counterNumParam, "All")
	var amount int
	if !allAmount {
		amount, ok = resolveNamedAmount(g, a.Amounts, source, counterNumParam)
		if !ok {
			return fmt.Errorf("engine: MoveCounter: CounterNum$ %q is not resolvable", counterNumParam)
		}
	}

	sourceParam, ok := a.Params.Param("Source")
	if !ok {
		sourceParam = "Self"
	}
	srcCards, err := definedCards(source, sourceParam, a.refs())
	if err != nil {
		return fmt.Errorf("engine: MoveCounter: Source$: %w", err)
	}
	if len(srcCards) == 0 {
		return nil
	}
	src := srcCards[0]
	if !g.Card(src).Counters.Any() {
		return nil
	}

	destCards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: MoveCounter: %w", err)
	}

	for _, dest := range destCards {
		if dest == src {
			continue
		}
		moveCounters(g, controller, a.Controller, a.Source, src, dest, counterType, allKinds, allAmount, amount)
	}
	return nil
}

// moveCounters is CountersMoveEffect.resolve's own per-destination body:
// allKinds moves every kind src currently carries (each capped at its own
// count, CountersRemoveEffect's own identical per-kind independence);
// otherwise only counterType moves. placer, the activator, puts them on dest
// (CountersMoveEffect.java:86, :160).
func moveCounters(g *Game, controller PlayerController, placer PlayerID, source, src, dest CardID, counterType CounterType, allKinds, allAmount bool, amount int) {
	srcCounters := &g.Card(src).Counters
	if allKinds {
		for _, kind := range srcCounters.Kinds() {
			moveOneCounterKind(g, controller, placer, source, src, dest, srcCounters, kind, allAmount, amount)
		}
		return
	}
	moveOneCounterKind(g, controller, placer, source, src, dest, srcCounters, counterType, allAmount, amount)
}

func moveOneCounterKind(g *Game, controller PlayerController, placer PlayerID, source, src, dest CardID, srcCounters *Counters, kind CounterType, allAmount bool, amount int) {
	cmax := srcCounters.Count(kind)
	if cmax <= 0 {
		return
	}
	n := amount
	if allAmount || n > cmax {
		n = cmax
	}
	if n <= 0 {
		return
	}
	srcCounters.Add(kind, -n)
	emitCounterChanged(g, source, CardEntity(src), kind, -n)
	g.addCardCounters(controller, placer, source, dest, kind, n)
}
