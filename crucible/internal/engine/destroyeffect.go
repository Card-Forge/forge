// Destroy: CR 701.7, M6's own next script-driven effect after the twelve
// castspell.go's own NewRegistry doc comment already lists -- 986 of the
// corpus's real (AB|DB)$ Destroy lines (excluding DestroyAll, a separate
// ApiType/effect class this port does not build), 782 of them naming
// ValidTgts$ and 205 naming Defined$ instead -- SpellAbilityEffect.
// getTargetCards(sa)'s own either/or contract, targetedOrDefinedCards
// (defined.go).
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/DestroyEffect.java's
// resolve/internalDestroy. Game.regenerate (regeneration.go) replaces the
// destruction unless NoRegen$ is set -- a regeneration shield or a
// permanent's own always-on "if this would be destroyed, regenerate it"
// replacement (ApiType.Regeneration), the two Destroy-type replacements
// this port models, both gated by Mode$ CantRegenerate first.
// TriggerType.Destroyed -- a rare corpus trigger mode ("whenever a permanent
// is destroyed," distinct from Mode$ Dies, which checkDiesTriggers already
// fires below for every real battlefield departure regardless of cause) this
// port's own trigger table has no entry for yet, the identical "an unbuilt
// Mode$ simply never fires" reasoning every other still-missing mode already
// has (game-state.md's own "Not ported yet").

package engine

//enginelint:allow id card game ability defined condition control zone parts

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// destroyUnresolvedParams names DestroyEffect's own params this port does
// not evaluate. Every one fails the whole line loudly rather than destroying
// the wrong permanent or silently dropping half of what it asked for
// (PORT-8/GO-7): a Condition$ value subAbilityConditionMet does not read
// (rejectUnreadCondition; Kicked and OptionalCost resolve); SorcerySpeed$ (9) -- a cost-restriction flag with
// no cost-payment site to attach to, sacrificeEffect's own identical
// reasoning; Ultimate$ (11) -- a planeswalker-ultimate-specific flag, its
// own further mechanic; ModeCost$ (7) -- Charm's own per-mode cost linkage,
// unrelated; RememberLKI$ (10) -- needs an LKI snapshot of the destroyed
// permanent taken before the move, this port's own Game.LKI is a whole-batch
// zone-change record (game-state.md), not wired to a single script-driven
// effect's own Remembered list yet.
//
// SubAbility$ chains through resolveSubAbility (subability.go,
// Registry.Resolve, effect.go) once this effect's own body finishes, the
// identical shape every other M6 effect already has. UnlessCost$/
// UnlessPayer$/UnlessResolveSubs$/UnlessSwitched$ no longer block either:
// resolveUnlessCost (effect.go) gates the whole ability before Registry.
// Resolve ever reaches it.
var destroyUnresolvedParams = [...]string{
	"SorcerySpeed", "Ultimate",
	"ModeCost", "RememberLKI",
}

// destroyEffect resolves Mode$/DB$/AB$ Destroy. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type destroyEffect struct{}

func (destroyEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range destroyUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Destroy: %s$ not resolvable yet", key)
		}
	}
	if err := rejectUnreadCondition(a, "Destroy"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Destroy: %w", err)
	}
	_, remember := a.Params.Param("RememberDestroyed")
	_, noRegen := a.Params.Param("NoRegen")
	var destroyed []CardID
	for _, id := range cards {
		c := g.Card(id)
		if c.Zone != Battlefield || !canBeDestroyed(c) {
			continue
		}
		if g.destroyInstead(controller, id) || (!noRegen && g.regenerate(controller, id)) {
			continue
		}
		if remember {
			g.Card(a.Source).Memory.Remember(CardEntity(id))
		}
		g.moveToGraveyard(controller, id)
		g.checkDiesTriggers(controller, id)
		destroyed = append(destroyed, id)
	}
	g.checkChangesZoneAllTriggers(controller, destroyed, Battlefield, Graveyard)
	return nil
}

// canBeDestroyed is Card.canBeDestroyed's own formula (Card.java:6815-6817),
// minus the isInPlay half every caller already checks: a phased-out
// permanent cannot be destroyed (CR 702.26b), and indestructible blocks
// destruction unless c is a creature at 0 or less toughness, CR 704.5f's own
// carve-out (destroyDamagedCreatures's own identical Indestructible/
// Toughness reasoning, action.go, applied here to an explicit Destroy rather
// than lethal damage).
func canBeDestroyed(c *Card) bool {
	if c.IsPhasedOut() {
		return false
	}
	if !c.HasKeyword("Indestructible") {
		return true
	}
	if !c.Type().Has(cardtype.Creature) {
		return false
	}
	t, ok := c.Toughness()
	return ok && t <= 0
}
