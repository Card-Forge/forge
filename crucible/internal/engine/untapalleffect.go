// UntapAll: CR 701.22, untapEffect's own batch-scan sibling and TapAll's
// own mirror image -- 105 real (AB|DB)$ UntapAll lines, all naming
// ValidCards$. Neither ValidTgts$ (1) nor Defined$ (3) present (101 of 105)
// scans every player's battlefield; either present scans only the named/
// targeted player's own battlefield instead -- UntapAllEffect.java's own
// identical `!sa.usesTargeting() && !sa.hasParam("Defined") ?
// game.getCardsIn(Battlefield) : getDefinedPlayersOrTargeted(sa).
// getCardsIn(Battlefield)`, ported the identical way tapalleffect.go's own
// is.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/UntapAllEffect.java's
// resolve. Not ported: TriggerType.UntapAll (the batch trigger) --
// checkUntapsTriggers already firing per card below covers every real
// corpus T: line, untapEffect.go's own identical reasoning for the same
// trigger reached from a script-driven Untap instead. ControllerUntaps$,
// the identical "who untaps attribution" TapperController$ leaves
// unresolved on TapAll, appears on zero real UntapAll lines -- not even
// listed below, nothing to guard against.

package engine

//enginelint:allow id card game ability defined condition control zone parts valid trigger

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// untapAllUnresolvedParams names UntapAllEffect's own params this port does
// not evaluate. Every one fails the whole line loudly (PORT-8/GO-7):
// Planeswalker$ (3) -- an unclear-semantics restriction on a resolving
// line, tapAllEffect's own identical gap; ModeCost$
// (2) -- a further activation-time restriction, unclear semantics on a resolving (not triggering) line;
// ConditionPlayerTurn$/ConditionManaSpent$ (1/1) -- SpellAbilityCondition's
// own shapes subAbilityConditionMet does not cover, putCounterEffect's own
// identical ConditionZone$/ConditionPlayerTurn$ gap; Condition$/
// ConditionDefined$ (0/1) -- condition.go's own subAbilityConditionMet
// would otherwise silently no-op a card naming either without
// ConditionPresent$ alongside it, dealDamageEffect's own identical
// reasoning.
var untapAllUnresolvedParams = [...]string{
	"Planeswalker", "ModeCost"}

// untapAllEffect resolves Mode$/DB$/AB$ UntapAll. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type untapAllEffect struct{}

func (untapAllEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range untapAllUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: UntapAll: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	validCards, ok := a.Params.Param("ValidCards")
	if !ok {
		return fmt.Errorf("engine: UntapAll: ValidCards$ missing")
	}
	spec := valid.Parse(validCards)

	_, hasValidTgts := a.Params.Param("ValidTgts")
	_, hasDefined := a.Params.Param("Defined")
	players := g.Players()
	if hasValidTgts || hasDefined {
		var err error
		players, err = targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
		if err != nil {
			return fmt.Errorf("engine: UntapAll: %w", err)
		}
	}

	_, remUntapped := a.Params.Param("RememberUntapped")
	for _, pid := range players {
		for _, cid := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(cid)
			if !c.Tapped || !Matches(g, c, spec, a.Controller, a.Source) {
				continue
			}
			g.runUntapCommands(controller, cid)
			c.Tapped = false
			if remUntapped {
				source.Memory.Remember(CardEntity(cid))
			}
			g.checkUntapsTriggers(controller, cid)
		}
	}
	return nil
}
