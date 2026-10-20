// RevealHand: CR 701.15 -- 50 real (AB|DB)$ RevealHand lines. Revealing
// itself has nothing to change: this port's own engine is already
// omniscient (no hidden-information model for any zone, GameState's own
// full-visibility fixture format), so Java's own game.getAction().reveal
// call -- an information-only UI/log event -- has no state-changing
// counterpart to port. What DOES change state is RememberRevealed$ (16
// real lines) and RememberRevealedPlayer$ (3), both plain Memory.Remember
// (memory.go) the identical way sacrificeAllEffect's/pumpAllEffect's/
// millEffect's own already are -- the reason this file exists at all rather
// than reducing to a silent no-op. Look$ (23, who does the looking) changes
// nothing this port tracks either way and is not read.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/RevealHandEffect.java's
// resolve.

package engine

//enginelint:allow id card game ability defined condition control valid zone parts

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// revealHandUnresolvedParams names RevealHandEffect's own params this port
// does not evaluate. Every one fails the whole line loudly (PORT-8/GO-7):
// Optional$ (2) -- an interactive "would you like to reveal" confirm, the
// identical gap Sacrifice's/Discard's/Mill's own Optional$ already
// document; ImprintRevealed$ (1) -- Card.ChosenMap/imprint storage, a
// separate mechanism from Memory.Remember this port does not have;
// Condition$/ConditionDefined$ (0/0, defensive) -- condition.go's own
// subAbilityConditionMet would otherwise silently no-op a card naming
// either without ConditionPresent$ alongside it, dealDamageEffect's own
// identical reasoning.
var revealHandUnresolvedParams = [...]string{
	"Optional", "ImprintRevealed"}

// revealHandEffect resolves Mode$/DB$/AB$ RevealHand. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type revealHandEffect struct{}

func (revealHandEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range revealHandUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: RevealHand: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: RevealHand: %w", err)
	}

	_, remCards := a.Params.Param("RememberRevealed")
	_, remPlayer := a.Params.Param("RememberRevealedPlayer")
	if !remCards && !remPlayer {
		return nil
	}

	var spec valid.Spec
	hasType := false
	if revealType, ok := a.Params.Param("RevealType"); ok {
		spec = valid.Parse(revealType)
		hasType = true
	}

	for _, pid := range players {
		if remCards {
			for _, cid := range g.Zone(Hand, pid).Cards() {
				if hasType && !Matches(g, g.Card(cid), spec, a.Controller, a.Source) {
					continue
				}
				source.Memory.Remember(CardEntity(cid))
			}
		}
		if remPlayer {
			source.Memory.Remember(PlayerEntity(pid))
		}
	}
	return nil
}
