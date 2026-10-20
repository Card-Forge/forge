// Shuffle: CR 701.20, M6's own next script-driven effect -- 54 real
// (AB|DB)$ Shuffle lines, 42 naming Defined$ and 3 naming ValidTgts$
// instead -- SpellAbilityEffect.getTargetPlayers(sa)'s own either/or
// contract, targetedOrDefinedPlayers (defined.go), millEffect's own first
// caller (milleffect.go) reused wholesale here.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/ShuffleEffect.java's
// resolve. Game.Shuffle (game.go) already exists -- mulligan.go's own real
// caller for London's own "shuffle the cards put back" step -- so this file
// is the thinnest M6 effect yet: read the player list, call it once per
// player.

package engine

//enginelint:allow id card game ability defined condition control zone

import "fmt"

// shuffleUnresolvedParams names ShuffleEffect's own params this port does
// not evaluate. Every one fails the whole line loudly (PORT-8/GO-7):
// Optional$ (1) -- an interactive "would you like to shuffle" confirm, the
// identical gap Sacrifice's/Discard's/Mill's own Optional$ already
// document; Condition$/ConditionDefined$ (0/1) -- condition.go's own
// subAbilityConditionMet would otherwise silently no-op a card naming
// either, dealDamageEffect's own identical reasoning.
var shuffleUnresolvedParams = [...]string{
	"Optional"}

// shuffleEffect resolves Mode$/DB$/AB$ Shuffle. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type shuffleEffect struct{}

func (shuffleEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range shuffleUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Shuffle: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Shuffle: %w", err)
	}
	for _, pid := range players {
		g.Shuffle(Library, pid)
	}
	return nil
}
