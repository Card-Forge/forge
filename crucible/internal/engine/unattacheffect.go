// Unattach: CR 704.5m's own voluntary trigger -- detaching an Aura,
// Equipment or Fortification without moving or destroying it. 7 real
// (AB|DB)$ Unattach lines, all naming Defined$ alone -- no real line names
// ValidTgts$ at all, so targetedOrDefinedCards (defined.go) still applies
// (its own "no ValidTgts$" branch reads Defined$ exactly the way this file
// needs, default "Self").
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/UnattachEffect.java's
// resolve. Game.Unattach (game.go) already exists -- cleanupDanglingAttachments'
// own real caller (action.go) for CR 704.5m's own state-based half -- so this
// file is a target-resolution loop around one already-built call, shuffleEffect's
// own identical shape (shuffleeffect.go). Java's own timestamp-staleness
// re-check (game.getCardState/equalsWithGameTimestamp) is not ported:
// targetedOrDefinedCards never returns a card the game itself no longer
// tracks, multiplyCounterEffect's own identical reasoning.

package engine

//enginelint:allow id card game ability defined condition control zone

import "fmt"

// unattachEffect resolves Mode$/DB$/AB$ Unattach. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type unattachEffect struct{}

func (unattachEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Unattach: %w", err)
	}
	for _, id := range cards {
		c := g.Card(id)
		if c.Zone != Battlefield {
			continue
		}
		if _, attached := c.AttachedTo(); attached {
			g.Unattach(id)
		}
	}
	return nil
}
