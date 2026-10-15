// Delayed and reflexive triggers: triggered abilities an effect creates
// rather than a card's own T: line (CR 603.7, 603.12).
//
// Ported from forge-game/src/main/java/forge/game/trigger/
// TriggerHandler.java's delayed-trigger lists (registerDelayedTrigger,
// registerThisTurnDelayedTrigger, registerPlayerDefinedDelayedTrigger,
// clearThisTurnDelayedTrigger, handlePlayerDefinedDelTriggers) and the
// delayed half of runWaitingTrigger.

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// delayedTrigger is one registered delayed trigger: the DelayedTrigger
// line itself (Mode$, Phase$, ValidPlayer$, Execute$), who created it and
// what it remembered. A delayed trigger fires once and is gone.
type delayedTrigger struct {
	Trigger    *compile.Ability
	Host       CardID
	Controller PlayerID
	Amounts    map[string]expr.Amount
	Remembered []EntityID
	// ThisTurn drops the trigger when the next turn begins
	// (clearThisTurnDelayedTrigger) if it has not fired.
	ThisTurn bool
	// AtCleanup holds the trigger inactive until this turn's cleanup
	// (NextTurn$/UpcomingTurn$, Java's getCleanup().addUntil).
	AtCleanup bool
	// ForPlayer holds the trigger inactive until that player's next turn
	// begins (DelayedTriggerDefinedPlayer$).
	ForPlayer PlayerID
	// HostTransforms is the host's transform count as the trigger was made.
	HostTransforms int
}

func (d *delayedTrigger) active() bool { return !d.AtCleanup && d.ForPlayer == NoPlayer }

// activateCleanupDelayedTriggers is the cleanup-time registration of
// NextTurn$/UpcomingTurn$ triggers. Java runs it from the cleanup phase's
// end, after handleNextTurn has already cleared the ending turn's ThisTurn
// triggers, so a NextTurn$ trigger is live for exactly the turn that
// follows -- AdvancePhase calls it right after delayedTriggersOnNextTurn.
func (g *Game) activateCleanupDelayedTriggers() {
	for i := range g.delayed {
		g.delayed[i].AtCleanup = false
	}
}

// delayedTriggersOnNextTurn is handleNextTurn's delayed-trigger half:
// ThisTurn triggers that never fired are dropped, then triggers waiting on
// the incoming active player's turn become live.
func (g *Game) delayedTriggersOnNextTurn(next PlayerID) {
	kept := g.delayed[:0]
	for _, d := range g.delayed {
		if d.ThisTurn && d.active() {
			continue
		}
		if d.ForPlayer == next {
			d.ForPlayer = NoPlayer
		}
		kept = append(kept, d)
	}
	g.delayed = kept
}

// delayedPhaseTriggerMatches collects every live delayed Mode$ Phase
// trigger the current step satisfies, removing each one it collects --
// runWaitingTrigger's `delayedTriggers.remove(deltrig)` before
// runSingleTrigger. A spawned trigger has no zone check (TriggerHandler
// skips zonesCheck when getSpawningAbility() != null), and one whose
// controller has left the game is dropped.
func (g *Game) delayedPhaseTriggerMatches() []Ability {
	var matches []Ability
	kept := g.delayed[:0]
	for _, d := range g.delayed {
		if g.Player(d.Controller).Lost {
			continue
		}
		mode, _ := d.Trigger.Param("Mode")
		if !d.active() || !strings.EqualFold(mode, "Phase") || !phaseTriggerMatches(d.Trigger, "Phase", g.activePhase) {
			kept = append(kept, d)
			continue
		}
		if validPlayer, ok := d.Trigger.Param("ValidPlayer"); ok {
			matched, recognized := matchesPlayerSpec(g, g.activePlayer, d.Controller, d.Host, validPlayer)
			if !recognized || !matched {
				kept = append(kept, d)
				continue
			}
		}
		sub, api, optional, ok := triggerEffectAPI(g, g.Card(d.Host), d.Amounts, d.Trigger)
		if !ok {
			kept = append(kept, d)
			continue
		}
		// TriggerPhase's AbilityKey.Player, the turn's player
		// (PhaseHandler.java:438), as for a card's own Mode$ Phase line.
		matches = append(matches, Ability{
			API: api, Source: d.Host, Controller: d.Controller, Params: sub,
			Amounts: d.Amounts, Optional: optional, TriggerRemembered: d.Remembered,
			triggered:      triggeredObjects{player: g.activePlayer},
			hostTransforms: d.HostTransforms, hasHostTransforms: true,
		})
	}
	g.delayed = kept
	return matches
}

// delayedChangesControllerMatches collects every live delayed Mode$
// ChangesController trigger (TriggerChangesController.performTest) that
// card's change from original satisfies, removing each one it collects.
// ValidCard$ Card.IsTriggerRemembered names a card the trigger remembered
// (RememberObjects$, 3 of 4 real lines: Ray of Command, Magus of the Unseen,
// Stolen Uniform); any other ValidCard$ is a valid string against the
// trigger's own host (Card.StrictlySelf, Seraph and Krovikan Vampire).
// ValidOriginalController$ is a player spec relative to the trigger's
// controller. The card is recorded as TriggeredCard and the original
// controller as TriggeredOriginalController.
func (g *Game) delayedChangesControllerMatches(card CardID, original PlayerID) []Ability {
	var matches []Ability
	kept := g.delayed[:0]
	for _, d := range g.delayed {
		if !d.active() || g.Player(d.Controller).Lost || !delayedModeIs(d, "ChangesController") ||
			!g.delayedChangesControllerSatisfied(d, card, original) {
			kept = append(kept, d)
			continue
		}
		sub, api, optional, ok := triggerEffectAPI(g, g.Card(d.Host), d.Amounts, d.Trigger)
		if !ok {
			kept = append(kept, d)
			continue
		}
		matches = append(matches, Ability{
			API: api, Source: d.Host, Controller: d.Controller, Params: sub,
			Amounts: d.Amounts, Optional: optional, TriggerRemembered: d.Remembered,
			triggered:      triggeredObjects{card: card, originalController: original},
			hostTransforms: d.HostTransforms, hasHostTransforms: true,
		})
	}
	g.delayed = kept
	return matches
}

// delayedModeIs reports whether d's DelayedTrigger line names Mode$ mode.
func delayedModeIs(d delayedTrigger, mode string) bool {
	m, _ := d.Trigger.Param("Mode")
	return strings.EqualFold(m, mode)
}

// delayedChangesControllerSatisfied is performTest for one delayed trigger.
func (g *Game) delayedChangesControllerSatisfied(d delayedTrigger, card CardID, original PlayerID) bool {
	if validCard, ok := d.Trigger.Param("ValidCard"); ok {
		if validCard == "Card.IsTriggerRemembered" {
			if !containsEntity(d.Remembered, CardEntity(card)) {
				return false
			}
		} else if !Matches(g, g.Card(card), valid.Parse(validCard), d.Controller, d.Host) {
			return false
		}
	}
	if spec, ok := d.Trigger.Param("ValidOriginalController"); ok {
		matched, recognized := matchesPlayerSpec(g, original, d.Controller, d.Host, spec)
		if !recognized || !matched {
			return false
		}
	}
	return g.delayedPresentMatches(d)
}

// delayedPresentMatches is Trigger.meetsCommonRequirements' IsPresent$ for a
// delayed trigger, at the moment it would fire: at least one battlefield
// permanent matches the valid string, read with the trigger's controller as
// "You". Card.IsTriggerRemembered (CardProperty.java:163) is a member of the
// trigger's RememberObjects$ list, which Matches cannot see, so it is
// applied here and dropped from the string (Stolen Uniform's "if it's
// attached to a creature you control"). Java checks again as the trigger
// resolves; this port checks once, as it fires.
func (g *Game) delayedPresentMatches(d delayedTrigger) bool {
	spec, ok := d.Trigger.Param("IsPresent")
	if !ok {
		return true
	}
	remembered := false
	if strings.Contains(spec, "IsTriggerRemembered") {
		remembered = true
		spec = strings.NewReplacer(".IsTriggerRemembered+", ".", "+IsTriggerRemembered", "", ".IsTriggerRemembered", "").Replace(spec)
	}
	parsed := valid.Parse(spec)
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if remembered && !containsEntity(d.Remembered, CardEntity(id)) {
				continue
			}
			if Matches(g, g.Card(id), parsed, d.Controller, d.Host) {
				return true
			}
		}
	}
	return false
}

// delayedLeftBattlefieldMatches collects every live delayed trigger watching
// left leave the battlefield for dest, removing each one it collects: a
// Mode$ ChangesZone trigger from the battlefield to dest, or (dest Exile) a
// Mode$ Exiled one, whose ValidCard$ Card.IsTriggerRemembered names left --
// the one shape registered today, Earthbend's return (earthbendeffect.go).
// Identity is the CardID, as Java's Card.equals compares ids.
func (g *Game) delayedLeftBattlefieldMatches(left CardID, dest ZoneType) []Ability {
	var matches []Ability
	kept := g.delayed[:0]
	for _, d := range g.delayed {
		if !d.active() || g.Player(d.Controller).Lost || !g.delayedWatchesLeaving(d, left, dest) {
			kept = append(kept, d)
			continue
		}
		sub, api, optional, ok := triggerEffectAPI(g, g.Card(d.Host), d.Amounts, d.Trigger)
		if !ok {
			kept = append(kept, d)
			continue
		}
		// TriggerChangesZone.java:192-196 (Origin$ Battlefield: Card is the
		// pre-move LKI) and TriggerExiled.java:103 (the pre-move object,
		// GameAction.java:933-942) record AbilityKey.Card, the card that moved
		// -- one CardID here, the one a card's own Dies trigger records too
		// (checkDiesTriggers), with its battlefield controller and owner frozen
		// the same way for Defined$ TriggeredCardController/Owner.
		lki := g.Card(left)
		if snap := g.LKI(left); snap != nil {
			lki = snap
		}
		objs := changesZoneCardObjects(d.Trigger, left, lki)
		if strings.EqualFold(d.Trigger.Name, "Exiled") {
			objs = objs.withCardLKI(lki)
		}
		matches = append(matches, Ability{
			API: api, Source: d.Host, Controller: d.Controller, Params: sub,
			Amounts: d.Amounts, Optional: optional, TriggerRemembered: d.Remembered,
			triggered:      objs,
			hostTransforms: d.HostTransforms, hasHostTransforms: true,
		})
	}
	g.delayed = kept
	return matches
}

// delayedWatchesLeaving reports whether d is a battlefield-leaving delayed
// trigger for left going to dest. ValidCard$ Card.IsTriggerRemembered names a
// card d remembered; any other ValidCard$ is a valid string tested against
// left as it was on the battlefield (Seraph's Card.StrictlySelf, with the
// trigger's host as the source). Destination$ Any accepts every dest this
// is called for (the graveyard and exile paths).
func (g *Game) delayedWatchesLeaving(d delayedTrigger, left CardID, dest ZoneType) bool {
	t := d.Trigger
	if o, _ := t.Param("Origin"); o != "Battlefield" {
		return false
	}
	mode, ok := t.Param("Mode")
	if !ok {
		mode = t.Name
	}
	switch {
	case strings.EqualFold(mode, "Exiled"):
		if dest != Exile {
			return false
		}
	case strings.EqualFold(mode, "ChangesZone"):
		// An absent Destination$ accepts every destination
		// (TriggerChangesZone.java:85): Animate Dead's delayed "leaves the
		// battlefield" names an Origin$ only.
		want, has := t.Param("Destination")
		if has && !strings.EqualFold(want, "Any") {
			if z, ok := ZoneByName(want); !ok || z != dest {
				return false
			}
		}
	default:
		return false
	}
	v, _ := t.Param("ValidCard")
	if v != "Card.IsTriggerRemembered" {
		c := g.Card(left)
		if snap := g.LKI(left); snap != nil {
			c = snap
		}
		return v != "" && Matches(g, c, valid.Parse(v), d.Controller, d.Host)
	}
	for _, e := range d.Remembered {
		if id, ok := e.AsCard(); ok && id == left {
			return true
		}
	}
	return false
}
