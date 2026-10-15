// PutCounterAll: CR 121.1, putCounterEffect's own battlefield-sweep sibling
// -- 285 real (AB|DB)$ PutCounterAll lines, every one naming both
// ValidCards$ and CounterType$; CounterNum$ is a resolvable amount (275) or
// absent, defaulting to 1 the identical way putCounterEffect's own does. 8
// name ValidTgts$ too (a targeted player, CardLists.filterControlledBy
// narrowing the sweep to permanents that player controls) -- 277 sweep
// every player's battlefield unfiltered.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/CountersPutAllEffect.java's
// resolve, trimmed the identical way putcountereffect.go's own is: that
// file also handles AmountByChosenMap$'s own ChosenMap-indexed amount, and a second
// ValidCards2$/CounterType2$/CounterNum2$ pass -- each its own further
// mechanic this port has nowhere to route through yet, so each fails the
// whole line loudly (PORT-8/GO-7) rather than resolving the first pass and
// silently dropping the second.

package engine

//enginelint:allow id card game player ability defined condition control zone valid amount event parts replacement

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// putCounterAllUnresolvedParams names CountersPutAllEffect's own params
// past ValidCards$/CounterType$/CounterNum$/ValidTgts$ this port does not
// evaluate. Every one fails the whole line loudly: ValidZone$ (2) -- this
// file's own sweep is Battlefield-only, CountersPutAllEffect.java's own
// default too, but a card naming a different zone explicitly must not
// silently sweep the wrong one; ValidCards2$/
// CounterType2$/CounterNum2$ (7/13/6) -- the second ValidCards$/CounterType$/
// CounterNum$ pass, above; AmountByChosenMap$ (1) -- a ChosenMap-indexed
// amount, this port's own Card.ChosenMap gap; TargetUnique$ (2) -- an extra
// battlefield-ownership filter this file has nowhere to route through,
// damageAllEffect's own identical gap; IsCurse$ (5) -- a display-only flag
// on a Curse-subtype enchantment's own SpellDescription, never read by
// resolve itself, listed anyway rather than silently assuming so;
// Ultimate$/ModeCost$ (3/1) -- tapAllEffect's/untapAllEffect's own
// identical gaps; Condition$/ConditionDefined$/ConditionPlayerTurn$/
// ConditionPhases$ (2/5/1/1) -- ConditionZone$'s/ConditionPlayerTurn$'s own
// identical putCounterEffect gap, SpellAbilityCondition shapes
// subAbilityConditionMet does not cover.
var putCounterAllUnresolvedParams = [...]string{
	"ValidZone", "ValidCards2", "CounterType2", "CounterNum2",
	"AmountByChosenMap", "TargetUnique", "IsCurse",
	"Ultimate", "ModeCost",
	"Condition", "ConditionPlayerTurn", "ConditionPhases",
}

// putCounterAllEffect resolves Mode$/DB$/AB$ PutCounterAll. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type putCounterAllEffect struct{}

func (putCounterAllEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range putCounterAllUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: PutCounterAll: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	validCards, ok := a.Params.Param("ValidCards")
	if !ok {
		return fmt.Errorf("engine: PutCounterAll: ValidCards$ missing")
	}
	spec := valid.Parse(validCards)

	counterTypeRaw, ok := a.Params.Param("CounterType")
	if !ok {
		return fmt.Errorf("engine: PutCounterAll: CounterType$ missing")
	}
	if strings.Contains(counterTypeRaw, ",") || strings.EqualFold(counterTypeRaw, "Any") {
		return fmt.Errorf("engine: PutCounterAll: CounterType$ %q not resolvable yet", counterTypeRaw)
	}
	counterType := CounterType(strings.ToUpper(counterTypeRaw))

	counterNumParam, ok := a.Params.Param("CounterNum")
	if !ok {
		counterNumParam = "1"
	}
	amount, ok := resolveNamedAmount(g, a.Amounts, source, counterNumParam)
	if !ok {
		return fmt.Errorf("engine: PutCounterAll: CounterNum$ %q is not resolvable", counterNumParam)
	}
	if amount <= 0 {
		return nil
	}

	players := g.Players()
	if _, ok := a.Params.Param("ValidTgts"); ok {
		var err error
		players, err = targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
		if err != nil {
			return fmt.Errorf("engine: PutCounterAll: %w", err)
		}
	}

	placerOf, err := putCounterAllPlacer(g, a)
	if err != nil {
		return err
	}
	for _, pid := range players {
		for _, cid := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(cid)
			if !Matches(g, c, spec, a.Controller, a.Source) {
				continue
			}
			// The counter table's replaceCounterEffect runs the AddCounter
			// replacements (Hardened Scales) and Mode$ CantPutCounter first
			// (CountersPutAllEffect.java:92, :118).
			placer := placerOf(c)
			if n := g.countersReplaced(controller, placer, CardEntity(cid), counterType, amount); n > 0 {
				g.addCardCounters(controller, placer, a.Source, cid, counterType, n)
			}
		}
	}
	return nil
}

// putCounterAllPlacer is who puts the counters on each card
// (CountersPutAllEffect.java:65-84): the activator, each card's own controller
// or owner for Placer$ Controller/Owner (Agitator Ant's "each player may put"),
// or the first player any other Placer$ names -- Java's `.get(0)`, which throws
// on an empty list; here that is an error before anything is placed (GO-7).
func putCounterAllPlacer(g *Game, a *Ability) (func(*Card) PlayerID, error) {
	spec, ok := a.Params.Param("Placer")
	switch {
	case !ok:
		return func(*Card) PlayerID { return a.Controller }, nil
	case spec == "Controller":
		return func(c *Card) PlayerID { return c.Controller() }, nil
	case spec == "Owner":
		return func(c *Card) PlayerID { return c.Owner }, nil
	}
	players, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
	if err != nil {
		return nil, fmt.Errorf("engine: PutCounterAll: Placer$: %w", err)
	}
	if len(players) == 0 {
		return nil, fmt.Errorf("engine: PutCounterAll: Placer$ %q names no player", spec)
	}
	return func(*Card) PlayerID { return players[0] }, nil
}
