// A permanent leaving the battlefield for its owner's hand or library: the
// "leaves the battlefield" ChangesZone triggers of CR 603.6d at those two
// destinations, checkDiesTriggers' and checkExiledTriggers' sibling, plus the
// live delayed ChangesZone watches registered for the card (a delayed
// "when it leaves the battlefield" naming Destination$ Any, Hand or Library).
//
// Ported from forge-game's TriggerChangesZone.performTest and the zone-change
// run in GameAction.changeZone; this port keeps one function per destination
// family, so a bounce and a tuck share this file and its dies/exile
// siblings stay where they were.

package engine

//enginelint:allow id card game ability control trigger statictrigger valid zone

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// isLeftToTrigger reports whether t is CR 603.6d's "leaves the battlefield"
// shape restricted to destination dest: Mode$ ChangesZone, Origin$ naming (or
// not restricting away) the battlefield, Destination$ naming (or not
// restricting away) dest.
func isLeftToTrigger(t *compile.Ability, dest ZoneType) bool {
	return strings.EqualFold(t.Name, "ChangesZone") &&
		hasZoneOrAny(t, "Origin", Battlefield) &&
		hasZoneOrAny(t, "Destination", dest) &&
		changesZoneResolvable(t)
}

// checkLeftToTriggers fires the leaves-the-battlefield triggers of left, a
// permanent that just moved to dest (Hand or Library): its own Card.Self
// triggers, the triggers of every permanent still on the battlefield and the
// live delayed watches. left's last-known information is read for the
// dying-state fields, as checkDiesTriggers does.
func (g *Game) checkLeftToTriggers(controller PlayerController, left CardID, dest ZoneType) {
	var matches []Ability
	c := g.Card(left)
	if snap := g.LKI(left); snap != nil {
		c = snap
	}
	if c.Def != nil {
		for face := range c.triggerFaces {
			for _, t := range face.Triggers {
				if !isLeftToTrigger(t, dest) {
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
					matches = append(matches, Ability{API: api, Source: left, Controller: c.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional, triggered: face.objects(triggeredObjects{card: left}), staticTrigger: isStaticTrigger(t)})
				}
			}
		}
	}
	matches = append(matches, g.otherLeftToTriggerMatches(left, dest)...)
	matches = append(matches, g.delayedLeftBattlefieldMatches(left, dest)...)
	g.pushTriggeredAbilities(controller, matches)
}

// checkLeftGraveyardTriggers fires the ChangesZone triggers naming a
// graveyard origin (Krovikan Vampire's "Origin$ Graveyard | Destination$ Any |
// ValidCard$ Card.IsRemembered", Murktide Regent's "an instant or sorcery card
// leaves your graveyard") for card, which just moved from its owner's
// graveyard to dest -- by a resolving effect (moveByEffect), a cost
// (exileFromGraveyard, Delve's costAssist.settle) or a cast
// (checkSpellCastTriggers). Java runs ChangesZone for every zone change but a
// same-zone one (GameAction.java:153, :601-613); these are the graveyard-origin
// sites. dest is never the battlefield: the enters-the-battlefield walk
// (checkETBTriggers) already reads Origin$ there, so calling both would fire a
// Destination$ Any line twice.
func (g *Game) checkLeftGraveyardTriggers(controller PlayerController, card CardID, dest ZoneType) {
	g.pushTriggeredAbilities(controller, g.leftGraveyardMatches(card, dest))
}

// noteGraveyardExit records card, which just left from, in castExits when it
// left a graveyard during a cast (holdingCastExits, game.go); reports whether
// it did, so the caller does not fire the triggers itself.
func (g *Game) noteGraveyardExit(card CardID, from ZoneType) bool {
	if from != Graveyard || !g.holdingCastExits {
		return false
	}
	g.castExits = append(g.castExits, card)
	return true
}

// castExitMatches takes the held graveyard exits' trigger matches, in the
// order the cards left, and empties the hold.
func (g *Game) castExitMatches() []Ability {
	var matches []Ability
	for _, id := range g.castExits {
		matches = append(matches, g.leftGraveyardMatches(id, g.Card(id).Zone)...)
	}
	g.castExits = nil
	return matches
}

// leftGraveyardMatches is checkLeftGraveyardTriggers' collection half, for a
// caller that pushes the matches in a wider batch (a spell cast from a
// graveyard, whose SpellCast triggers Java holds alongside,
// TriggerHandler.java:264). Every trigger is gated by its TriggerZones$
// against its host's zone (TriggerReplacementBase.zonesCheck,
// TriggerReplacementBase.java:61-65, phaseTriggerZoneMatches):
//
//   - the moved card's own, in dest (Golgari Brownscale's "When CARDNAME is
//     put into your hand from your graveyard", no TriggerZones$, so active
//     wherever the card is);
//   - every other card's in the zones phaseTriggerZones walks, the moved card
//     skipped there (it may now sit in exile): the battlefield watchers plus
//     a graveyard one such as Ereboss Titan's TriggerZones$ Graveyard.
//
// ValidCard$ is matched against card's current state: CR 603.10a's look back
// (TriggerChangesZone.java:121-124 reads CardLKI for a graveyard origin) has
// no graveyard snapshot here, and Move only clears continuous-effect grants,
// which no corpus Origin$ Graveyard ValidCard$ reads.
func (g *Game) leftGraveyardMatches(card CardID, dest ZoneType) []Ability {
	if dest == Battlefield || dest == Graveyard {
		return nil
	}
	c := g.Card(card)
	matches := g.appendLeftGraveyardMatches(nil, c, dest, c, dest)
	for _, pid := range g.Players() {
		for _, z := range phaseTriggerZones {
			for _, watcher := range g.Zone(z, pid).Cards() {
				if watcher == card {
					continue
				}
				matches = g.appendLeftGraveyardMatches(matches, g.Card(watcher), z, c, dest)
			}
		}
	}
	return matches
}

// appendLeftGraveyardMatches appends host's (sitting in zone) graveyard-origin
// ChangesZone triggers that fire for moved going to dest.
func (g *Game) appendLeftGraveyardMatches(matches []Ability, host *Card, zone ZoneType, moved *Card, dest ZoneType) []Ability {
	if host.Def == nil {
		return matches
	}
	for face := range host.triggerFaces {
		for _, t := range face.Triggers {
			if !strings.EqualFold(t.Name, "ChangesZone") || !hasZoneOrAny(t, "Origin", Graveyard) ||
				!hasZoneOrAny(t, "Destination", dest) || !changesZoneResolvable(t) || !phaseTriggerZoneMatches(host, t, zone) {
				continue
			}
			validCard, ok := t.Param("ValidCard")
			if !ok || !Matches(g, moved, valid.Parse(validCard), host.Controller(), host.ID) {
				continue
			}
			if sub, api, optional, ok := triggerEffectAPI(g, host, face.Amounts, t); ok {
				matches = append(matches, Ability{API: api, Source: host.ID, Controller: host.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional, triggered: face.objects(triggeredObjects{card: moved.ID}), staticTrigger: isStaticTrigger(t)})
			}
		}
	}
	return matches
}

// otherLeftToTriggerMatches is checkLeftToTriggers' wider half: every
// permanent still on the battlefield gets its own Triggers walked against
// left. No entered == left skip is needed: left is already in dest.
func (g *Game) otherLeftToTriggerMatches(left CardID, dest ZoneType) []Ability {
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
					if !isLeftToTrigger(t, dest) {
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
						matches = append(matches, Ability{API: api, Source: watcher, Controller: w.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional, triggered: face.objects(triggeredObjects{card: left}), staticTrigger: isStaticTrigger(t)})
					}
				}
			}
		}
	}
	return matches
}
