// DamageAll: CR 119, DealDamage's own "to every matching creature and/or
// player at once" sibling -- 307 real (AB|DB)$ DamageAll lines, 254 naming
// ValidCards$ and 126 naming ValidPlayers$ (91 of them naming both). 288 of
// the 307 name no DamageSource$ at all -- AbilityUtils.getDefinedCards's
// own `def == null ? "Self" : ...` default (definedCards's own identical
// "Self" case, defined.go), the same default DamageAllEffect.java's own
// resolve falls back to.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/DamageAllEffect.java's
// resolve. Reuses combat's own damage machinery the identical way
// dealDamageEffect/fightEffect already do: dealPermanentDamage/
// dealPlayerDamage (combatdamage.go) mark the damage and check CR 614's own
// prevention/replacement and CR 603's own "deals damage" trigger, one
// shared damageTable across every matched card and player so
// checkDamageDoneOnceTriggers fires once for the whole sweep rather than
// once per target -- Java's own single `game.getAction().dealDamage` batch
// call for the whole `damageMap`, ported as a loop over the identical two
// target lists instead of Java's own CardDamageTable object, dealDamageEffect's
// own multi-player loop the closer precedent than Fight's own two-call
// shape. Not ported: deathtouch. DamageAllEffect.java's own resolve never
// reads HasKeyword(Deathtouch) at all -- ported faithfully, not a gap: a
// sweeper's own damage source is overwhelmingly a spell or a planeswalker
// ability, not a creature, in the real corpus this port has read.

package engine

//enginelint:allow id card game ability defined condition control zone valid trigger amount effecthelpers damageresolveeffect replacedying

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// damageAllUnresolvedParams names DamageAllEffect's own params this port
// does not evaluate. Every one fails the whole line loudly (PORT-8/GO-7):
// ValidTgts$/TargetMin$/TargetMax$/TargetUnique$ (9/-/-/1) --
// getFirstTargetedPlayer's own extra battlefield-ownership filter, this
// file has nowhere to route it through;
// ModeCost$ (5) -- unclear semantics on a resolving (not triggering)
// line, not worth guessing at; Ultimate$ (4) -- a planeswalker-ultimate-
// specific flag, its own further mechanic; Remembered$/SVar$/XColor$/
// TriggeredSpellAbility$ (1 each) -- each its own further reference this
// file has no vocabulary for; Condition$/ConditionDefined$ (0/6) --
// condition.go's own subAbilityConditionMet would otherwise silently no-op
// a card naming either, dealDamageEffect's own identical reasoning.
var damageAllUnresolvedParams = [...]string{
	"ValidTgts", "TargetUnique", "ModeCost",
	"Ultimate", "Remembered", "SVar", "XColor", "TriggeredSpellAbility"}

// damageAllEffect resolves Mode$/DB$/AB$ DamageAll. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does. RememberDamaged$ has the host remember what
// its own damage reached (rememberDamaged) and ReplaceDyingDefined$ makes
// its exile-instead effect afterwards (replaceDying, replacedying.go).
type damageAllEffect struct{}

func (damageAllEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range damageAllUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: DamageAll: %s$ not resolvable yet", key)
		}
	}
	if err := checkReplaceDying(g, a, "DamageAll"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	numDmg, ok := a.Params.Param("NumDmg")
	if !ok {
		return fmt.Errorf("engine: DamageAll: NumDmg$ missing")
	}
	dmg, ok := resolveNamedAmount(g, a.Amounts, source, numDmg)
	if !ok {
		return fmt.Errorf("engine: DamageAll: NumDmg$ %q is not resolvable", numDmg)
	}
	if dmg <= 0 {
		// Nothing is dealt and nothing remembered; replaceDying still runs,
		// as DamageAllEffect.resolve reaches it whatever the amount.
		return g.replaceDying(a, "DamageAll")
	}

	damageSource, ok := a.Params.Param("DamageSource")
	if !ok {
		damageSource = "Self"
	}
	sources, err := definedCards(source, damageSource, a.refs())
	if err != nil {
		return fmt.Errorf("engine: DamageAll: %w", err)
	}
	if len(sources) == 0 {
		return nil
	}
	from := sources[0]

	var table damageTable
	if validCards, ok := a.Params.Param("ValidCards"); ok {
		spec := valid.Parse(validCards)
		for _, pid := range g.Players() {
			for _, cid := range g.Zone(Battlefield, pid).Cards() {
				if Matches(g, g.Card(cid), spec, source.Controller(), a.Source) {
					g.dealPermanentDamage(controller, from, cid, dmg, false, false, &table)
				}
			}
		}
	}
	if validPlayers, ok := a.Params.Param("ValidPlayers"); ok {
		players, err := definedPlayers(g, a.Controller, a.Source, validPlayers, a.refs())
		if err != nil {
			return fmt.Errorf("engine: DamageAll: ValidPlayers$: %w", err)
		}
		for _, pid := range players {
			g.dealPlayerDamage(controller, from, pid, dmg, false, &table)
		}
	}
	// GameAction.dealDamage's RememberDamaged$ (GameAction.java:2787-2794),
	// then replaceDying (DamageAllEffect.java:106): Anger of the Gods' "if a
	// creature dealt damage this way would die this turn, exile it instead".
	if hasParam(a, "RememberDamaged") {
		rememberDamaged(g, a.Source, table)
	}
	g.checkDamageTableTriggers(controller, table, false)
	return g.replaceDying(a, "DamageAll")
}
