// Fight: CR 701.12, M6's own sixteenth script-driven effect -- 146 of the
// corpus's real (AB|DB)$ Fight lines, every one of them naming Defined$, 138
// of THOSE also naming ValidTgts$ (fight_with_fire.txt's own dominant real
// shape, "Defined$ Self | ValidTgts$ Creature" -- CARDNAME fights a chosen
// target) and 8 naming Defined$ alone (two specific creatures, e.g. both
// Remembered).
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/FightEffect.java's
// resolve/getFighters/dealDamage. Not ported: TriggerType.Fight/FightOnce --
// this port's own trigger table has no entry for either mode yet, the
// identical "an unbuilt Mode$ simply never fires" reasoning every other
// still-missing mode already has (game-state.md's own "Not ported yet").

package engine

//enginelint:allow id card game ability defined condition control zone combatdamage trigger excessdamage replacedying

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// fightUnresolvedParams names FightEffect's own params this port does not
// evaluate. Every one fails the whole line loudly (PORT-8/GO-7):
// Optional$ (2) -- an interactive "would you like to
// fight" confirm, the identical gap Sacrifice's/Discard's/Pump's own
// Optional$ already document (TargetsAtRandom$ is chosen when the targets
// are, randomTargets in targeting.go); Condition$/ConditionDefined$
// (0/0) -- condition.go's own subAbilityConditionMet would otherwise
// silently no-op a card naming either, dealDamageEffect's own identical
// reasoning.
var fightUnresolvedParams = [...]string{
	"Optional"}

// fightEffect resolves Mode$/DB$/AB$ Fight. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does. Once the fight's damage is dealt, ExcessSVar$
// binds the excess the ExcessSVarCondition$ fighters took (bindExcessSVar,
// excessdamage.go: Rhino's Rampage, The Last Agni Kai) and
// ReplaceDyingDefined$ makes its exile-instead effect (replaceDying,
// FightEffect.java:177: Mawloc, Unnatural Aggression). A fight with fewer
// than two fighters deals nothing and makes no effect (:60-62).
type fightEffect struct{}

func (fightEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range fightUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Fight: %s$ not resolvable yet", key)
		}
	}
	if err := checkReplaceDying(g, a, "Fight"); err != nil {
		return err
	}
	if err := checkExcessSVar(a, "Fight"); err != nil {
		return err
	}
	defaultExcessSVar(a)
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	fighters, err := fightFighters(g, source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Fight: %w", err)
	}
	if len(fighters) < 2 {
		return nil
	}
	fighterA, fighterB := g.Card(fighters[0]), g.Card(fighters[1])

	var table damageTable
	powerA, ok := fighterA.Power()
	if !ok {
		return fmt.Errorf("engine: Fight: %s's own power is not resolvable", fighterA.Def.Name)
	}
	// CR 701.12c: a creature fighting itself deals damage to itself equal to
	// twice its power, dealDamage's own `fighterA.equals(fighterB)` branch.
	if fighterA.ID == fighterB.ID {
		g.dealPermanentDamage(controller, fighterA.ID, fighterA.ID, powerA*2, fighterA.HasKeyword("Deathtouch"), false, &table)
	} else {
		powerB, ok := fighterB.Power()
		if !ok {
			return fmt.Errorf("engine: Fight: %s's own power is not resolvable", fighterB.Def.Name)
		}
		g.dealPermanentDamage(controller, fighterA.ID, fighterB.ID, powerA, fighterA.HasKeyword("Deathtouch"), false, &table)
		g.dealPermanentDamage(controller, fighterB.ID, fighterA.ID, powerB, fighterB.HasKeyword("Deathtouch"), false, &table)
	}
	bindExcessSVar(g, a, table)
	g.checkDamageTableTriggers(controller, table, false)
	return g.replaceDying(a, "Fight")
}

// fightFighters is getFighters's own real corpus shape: at most one card
// from ValidTgts$'s own chosen target (targetedOrDefinedCards's own first
// half, defined.go -- read directly here rather than through that helper,
// since Fight's own combining rule needs to know separately whether a
// target supplied a card, unlike every other targeted effect this port
// has) and Defined$'s own resolved cards, combined the identical way
// Java's own priority does: a lone Defined$ card pairs with an already-
// found target (fighter2 = the target, fighter1 = the Defined$ card,
// getFighters' own real corpus shape); two or more Defined$ cards with no
// target found yet become both fighters outright. CR 701.12b's own "no
// longer on the battlefield or no longer a creature" legality check
// (definedCards's own filter has no such notion) is applied here, to
// Defined$'s own resolved cards only -- Java's own identical asymmetry,
// since a freshly-chosen ValidTgts$ target was already legality-filtered by
// resolveTargets' own candidate walk (targeting.go) and re-checked by CR
// 608.2b's targetsStillLegal as the ability resolves.
func fightFighters(g *Game, host *Card, a *compile.Ability, refs abilityRefs) ([]CardID, error) {
	var fighter1, fighter2 CardID
	haveFighter1 := false

	if _, hasValidTgts := a.Param("ValidTgts"); hasValidTgts {
		for _, e := range refs.targets {
			if id, ok := e.AsCard(); ok {
				fighter1, haveFighter1 = id, true
				break
			}
		}
	}

	if defined, ok := a.Param("Defined"); ok {
		cards, err := definedCards(host, defined, refs)
		if err != nil {
			return nil, err
		}
		var live []CardID
		for _, id := range cards {
			c := g.Card(id)
			if c.Zone == Battlefield && c.Type().Has(cardtype.Creature) {
				live = append(live, id)
			}
		}
		switch {
		case len(live) > 1 && !haveFighter1:
			fighter1, fighter2 = live[0], live[1]
			haveFighter1 = true
		case len(live) > 0:
			fighter2 = fighter1
			fighter1 = live[0]
			haveFighter1 = true
		}
	} else if haveFighter1 {
		var cardTargets []CardID
		for _, e := range refs.targets {
			if id, ok := e.AsCard(); ok {
				cardTargets = append(cardTargets, id)
			}
		}
		if len(cardTargets) > 1 {
			fighter2 = cardTargets[1]
		}
	}

	if !haveFighter1 || fighter2 == NoCard {
		return nil, nil
	}
	return []CardID{fighter1, fighter2}, nil
}
