// Exiling a permanent as an activation cost: Exile<1/CARDNAME>, the identical
// self-reference shape Sac<1/CARDNAME> already has (ActivateAbility/
// ActivateManaAbility's own ActivationShape.SelfExile, internal/cost).
//
// Unlike sacrifice (CR 701.20, its own dedicated Mode$ Sacrificed trigger)
// or discard (CR 701.8, its own dedicated Mode$ Discarded trigger), exile has
// no dedicated corpus-relevant trigger mode of its own: Forge's own
// TriggerType.Exiled exists, but 3 real corpus T: lines name Mode$ Exiled at
// all -- not worth a fourth "own cause" trigger dispatch alongside
// checkSacrificedTriggers/checkDiscardedTriggers/checkTapsTriggers. What
// exile DOES share with every other way a permanent leaves the battlefield is
// CR 603.6d's own "leaves the battlefield" trigger family
// (TriggerChangesZone.java's own performTest, ported here the identical way
// checkDiesTriggers already ports it for the graveyard-destination case) --
// checkExiledTriggers, below, is that family's exile-destination sibling.

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// exileCards actually exiles each of ids -- GameAction.exile's own per-card
// loop (forge-game/src/main/java/forge/game/GameAction.java), mirroring
// sacrificeCards's own shape (sacrificeeffect.go) at the one real corpus
// need this cost primitive has: no RememberExiled$-equivalent to thread
// through (0 real Exile<1/CARDNAME> cost lines combine with one), so this
// takes no *Ability at all, unlike sacrificeCards's own signature. Fires
// checkExiledTriggers per card (Move, game.go, already freezes g.LKI on any
// battlefield-leaving move, not only a move to the graveyard, so the
// dying-state lookback checkDiesTriggers relies on works here for free) and
// checkChangesZoneAllTriggers once for the whole batch, CR 603.6d's own
// batched trigger, reused wholesale exactly as sacrificeCards's own trailing
// call already is.
func exileCards(g *Game, controller PlayerController, ids []CardID) {
	var exiled []CardID
	for _, id := range ids {
		c := g.Card(id)
		if c.Zone != Battlefield {
			continue
		}
		g.Move(id, Exile, c.Owner)
		g.checkExiledTriggers(controller, id)
		exiled = append(exiled, id)
	}
	g.checkChangesZoneAllTriggers(controller, exiled, Battlefield, Exile)
}

// isExiledTrigger reports whether t is CR 603.6d's "leaves the battlefield"
// shape restricted to the one destination checkExiledTriggers' own call
// sites ever reach, Exile -- isDiesTrigger's own exact sibling (trigger.go),
// Destination$ swapped from Graveyard to Exile. A line naming neither
// Destination$ Exile nor an unrestricted Destination$ (absent or "Any", CR
// 603.6c's own unqualified "leaves the battlefield," matched the identical
// way isDiesTrigger's own wildcard already is) does not match -- 35 real
// corpus lines name Destination$ with Exile in its list, most already
// permitting Battlefield as Origin$ too (absent, "Any", or a list naming it);
// every real "whenever ~ leaves the battlefield" line with no Destination$
// restriction at all is already counted in isDiesTrigger's own accounting
// and now correctly fires for an exile-caused departure too, not only a
// death.
func isExiledTrigger(t *compile.Ability) bool {
	return strings.EqualFold(t.Name, "ChangesZone") &&
		hasZoneOrAny(t, "Origin", Battlefield) &&
		hasZoneOrAny(t, "Destination", Exile) &&
		changesZoneResolvable(t)
}

// checkExiledTriggers is checkDiesTriggers' own exact structural sibling
// (trigger.go) -- own-card and other-watcher triggers both walked, g.LKI
// read for left's own dying-state fields the identical way, matches
// collected before pushTriggeredAbilities is called once -- with
// isExiledTrigger/Exile in every place checkDiesTriggers reads
// isDiesTrigger/Graveyard.
func (g *Game) checkExiledTriggers(controller PlayerController, left CardID) {
	var matches []Ability
	c := g.Card(left)
	if snap := g.LKI(left); snap != nil {
		c = snap
	}
	if c.Def != nil {
		for face := range c.triggerFaces {
			for _, t := range face.Triggers {
				if !isExiledTrigger(t) {
					continue
				}
				validCard, ok := t.Param("ValidCard")
				if !ok {
					continue
				}
				if !Matches(g, c, valid.Parse(validCard), c.Controller(), left) {
					continue
				}
				if sub, api, optional, ok := triggerEffectAPI(g, c, face.Amounts, t); ok {
					matches = append(matches, Ability{API: api, Source: left, Controller: c.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional, triggered: face.objects(triggeredObjects{})})
				}
			}
		}
	}
	matches = append(matches, g.otherExiledTriggerMatches(left)...)
	matches = append(matches, g.delayedLeftBattlefieldMatches(left, Exile)...)
	g.pushTriggeredAbilities(controller, matches)
}

// otherExiledTriggerMatches is otherDiesTriggerMatches' own exact sibling
// (trigger.go): every permanent still on the battlefield gets its own
// Triggers walked against left, the card that just left. No entered == left
// skip is needed for the identical reason otherDiesTriggerMatches' own has
// none: left is already in the exile zone by the time this runs.
func (g *Game) otherExiledTriggerMatches(left CardID) []Ability {
	var matches []Ability
	leaving := g.Card(left)
	if snap := g.LKI(left); snap != nil {
		leaving = snap
	}
	for _, pid := range g.Players() {
		for _, watcher := range g.Zone(Battlefield, pid).Cards() {
			w := g.Card(watcher)
			if w.Def == nil {
				continue
			}
			for face := range w.triggerFaces {
				for _, t := range face.Triggers {
					if !isExiledTrigger(t) {
						continue
					}
					validCard, ok := t.Param("ValidCard")
					if !ok {
						continue
					}
					if !Matches(g, leaving, valid.Parse(validCard), w.Controller(), watcher) {
						continue
					}
					if sub, api, optional, ok := triggerEffectAPI(g, w, face.Amounts, t); ok {
						matches = append(matches, Ability{API: api, Source: watcher, Controller: w.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional, triggered: face.objects(triggeredObjects{})})
					}
				}
			}
		}
	}
	return matches
}
