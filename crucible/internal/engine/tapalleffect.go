// TapAll: CR 701.21, tapEffect's own batch-scan sibling -- 61 real
// (AB|DB)$ TapAll lines, all naming ValidCards$ (a battlefield scan, not a
// target). Neither ValidTgts$ nor Defined$ present (53 of 61) scans every
// player's battlefield; either present (8) scans only the named/targeted
// player's own battlefield instead -- TapAllEffect.java's own
// `!sa.usesTargeting() && !sa.hasParam("Defined") ? game.getCardsIn(Battlefield)
// : getTargetPlayers(sa).getCardsIn(Battlefield)`, ported as
// targetedOrDefinedPlayers (defined.go) called only on that second branch: its
// own "neither present" default ("You", not "every player") does not match
// TapAllEffect.java's own here, so this file guards the branch itself rather
// than leaning on that default.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/TapAllEffect.java's
// resolve. Not ported: TriggerType.TapAll (the batch trigger, not the
// per-card one) -- tapEffect.go's own identical reasoning, checkTapsTriggers
// already firing per card below covers every real corpus T: line that
// exists.

package engine

//enginelint:allow id card game ability defined condition control zone parts valid trigger

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// tapAllUnresolvedParams names TapAllEffect's own params this port does not
// evaluate. Every one fails the whole line loudly (PORT-8/GO-7):
// TapperController$ (3) -- tapEffect's own identical Tapper$ gap, "tapped
// by" attribution nothing downstream reads yet; Ultimate$/Planeswalker$
// (2/2) -- a planeswalker-ultimate-specific flag and an unclear-semantics
// restriction on a resolving line, neither worth guessing at; Condition$/
// ConditionDefined$ (0/0, defensive) -- condition.go's own
// subAbilityConditionMet would otherwise silently no-op a card naming
// either without ConditionPresent$ alongside it, dealDamageEffect's own
// identical reasoning.
var tapAllUnresolvedParams = [...]string{
	"TapperController", "Ultimate", "Planeswalker"}

// tapAllEffect resolves Mode$/DB$/AB$ TapAll. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type tapAllEffect struct{}

func (tapAllEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range tapAllUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: TapAll: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	validCards, ok := a.Params.Param("ValidCards")
	if !ok {
		return fmt.Errorf("engine: TapAll: ValidCards$ missing")
	}
	spec := valid.Parse(validCards)

	_, hasValidTgts := a.Params.Param("ValidTgts")
	_, hasDefined := a.Params.Param("Defined")
	players := g.Players()
	if hasValidTgts || hasDefined {
		var err error
		players, err = targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
		if err != nil {
			return fmt.Errorf("engine: TapAll: %w", err)
		}
	}

	_, remTapped := a.Params.Param("RememberTapped")
	for _, pid := range players {
		for _, cid := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(cid)
			if c.Tapped || !Matches(g, c, spec, a.Controller, a.Source) {
				continue
			}
			c.Tapped = true
			if remTapped {
				source.Memory.Remember(CardEntity(cid))
			}
			g.checkTapsTriggers(controller, cid, c.Controller(), false)
		}
	}
	return nil
}
