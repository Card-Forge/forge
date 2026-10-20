// SpellAbilityEffect.addUntilCommand and checkValidDuration: the one table
// that turns a Duration$ into the command lists the ending command waits on.
//
// Ported from forge-game/src/main/java/forge/game/ability/SpellAbilityEffect.
// java:951-1062 and the Phase command lists (phase/Phase.java:51-140,
// PhaseHandler.java:252-260, :363, :485-490, :515-518). Java registers
// anonymous GameCommand closures; each one here is a cardCommand record
// (controlcommands.go), so the game state stays data.

package engine

//enginelint:allow id card game zone player ability control controlcommands phase

import (
	"fmt"
	"strings"
)

// untilWhen names the Phase command list a phaseCommand sits on, and which of
// that Phase's runs reaches it.
type untilWhen uint8

const (
	// untilTurnStart is Game.getCleanup().addUntil(player): a turn handover
	// that makes Player active (PhaseHandler.java:518).
	untilTurnStart untilWhen = iota
	// untilUpkeepBegin is getUpkeep().addUntil(player): Player's upkeep
	// begins (:259).
	untilUpkeepBegin
	// untilUpkeepEnd is getUpkeep().addUntilEnd/registerUntilEnd(player):
	// Player's upkeep ends (:485-486).
	untilUpkeepEnd
	// untilUntapEnd is getUntap().addUntilEnd(player): Player's untap step
	// ends (:490).
	untilUntapEnd
	// untilEndStepBegin is getEndOfTurn().addUntil(player): Player's end step
	// begins (:363).
	untilEndStepBegin
	// untilEndStepAt is getEndOfTurn().addAt: the next end step of any turn
	// begins (:366).
	untilEndStepAt
	// untilEndCombat is getEndOfCombat().registerUntilEnd(player): Player's
	// next end of combat after the turn it was made in is over
	// (PhaseHandler.java:409, :1267).
	untilEndCombat
	// untilCleanupDefer is getEndOfTurn().addUntil(() -> addUntil(cmd)): the
	// command moves to the until-end-of-turn list at this cleanup and runs at
	// the next one.
	untilCleanupDefer
	// untilSBAChecked is Game.addSBACheckedCommand: after the next
	// state-based-action check (GameAction.java:1667).
	untilSBAChecked
)

// phaseCommand is one pending GameCommand on a Phase list. Player is the list
// key (NoPlayer for the unkeyed untilEndStepAt). Armed is a command Java put
// on registerMap (registerUntilEnd): the phase's end this turn does not run
// it, only moves it to the until-end list (registerUntilEndCommand).
type phaseCommand struct {
	cardCommand
	When   untilWhen
	Player PlayerID
	Armed  bool
}

// untilDurations is every Duration$ addUntilCommand has a branch for, those
// refused included. A value outside it ends at cleanup, Java's final else
// (EndOfTurn.addUntil), so "End", "EndOfTurn" and "UntilEndOfTurn" all do.
var untilDurations = map[string]bool{
	"UntilEndOfCombat": true, "UntilEndOfCombatYourNextTurn": true, "UntilYourNextUpkeep": true,
	"UntilTheEndOfYourNextUpkeep": true, "UntilTheEndOfYourNextUntap": true, "UntilNextEndStep": true,
	"UntilYourNextEndStep": true, "UntilYourNextTurn": true, "UntilTheEndOfYourNextTurn": true,
	"UntilTheEndOfTargetedNextTurn": true, "ThisTurnAndNextTurn": true, "UntilStateBasedActionChecked": true,
	"UntilHostLeavesPlay": true, "UntilHostLeavesPlayOrEOT": true, "UntilHostLeavesPlayOrEndOfCombat": true,
	"UntilLoseControlOfHost": true, "AsLongAsControl": true, "AsLongAsInPlay": true, "UntilUntaps": true,
	"UntilTargetedUntaps": true, "UntilUnattached": true, "UntilFacedown": true,
}

// endsAtCleanup reports whether a Duration$ d ends at cleanup in Java: none
// given, or one addUntilCommand's table does not name (its final else).
// Permanent and Perpetual never reach addUntilCommand and are not asked.
func endsAtCleanup(d string) bool { return !untilDurations[d] }

// durationRefused is the Duration$ values addUntilCommand names that this port
// cannot register: the error says which, never a silent end-of-turn (GO-7).
func durationRefused(api, d string) error {
	switch d {
	case "UntilTheEndOfTargetedNextTurn", "UntilFacedown":
		return fmt.Errorf("engine: %s: Duration$ %q not resolvable yet", api, d)
	}
	return nil
}

// checkValidDuration is SpellAbilityEffect.checkValidDuration (:1036-1062):
// false means the effect does not apply at all -- the host a host-bound
// duration hangs on is gone, phased out, not the activator's, or an
// UntilUntaps host that is not tapped.
func (g *Game) checkValidDuration(a *Ability, d string) bool {
	host := g.Card(a.Source)
	if strings.HasPrefix(d, "UntilHostLeavesPlay") || d == "UntilLoseControlOfHost" || d == "UntilUntaps" ||
		d == "AsLongAsControl" || d == "AsLongAsInPlay" {
		if host.Zone != Battlefield && host.Zone != Stack {
			return false
		}
	}
	if (d == "AsLongAsControl" || d == "AsLongAsInPlay") && host.IsPhasedOut() {
		return false
	}
	if (d == "UntilLoseControlOfHost" || d == "AsLongAsControl") && host.Controller() != a.Controller {
		return false
	}
	if d == "UntilUntaps" && !host.Tapped {
		return false
	}
	if d == "UntilTargetedUntaps" {
		tgt, ok := firstCardTarget(a)
		if !ok || !g.Card(tgt).Tapped || g.Card(tgt).IsPhasedOut() {
			return false
		}
	}
	return true
}

// firstCardTarget is sa.getSATargetingCard().getTargetCard() for the cases
// that read it: the ability's first card target.
func firstCardTarget(a *Ability) (CardID, bool) {
	for _, e := range a.Targets {
		if id, ok := e.AsCard(); ok {
			return id, true
		}
	}
	return NoCard, false
}

// addUntilCommand is SpellAbilityEffect.addUntilCommand (:954-1031): cmd goes
// on the lists d names, with a.Controller as the player a "your" duration is
// keyed to. A d that ends at cleanup (endsAtCleanup) puts it on the
// until-end-of-turn list; callers whose record already ends at cleanup on its
// own skip the call instead. It errors on a Duration$ it cannot register.
func (g *Game) addUntilCommand(a *Ability, d string, cmd cardCommand) error {
	return g.addUntilCommandFor(a, d, cmd, a.Controller)
}

// addUntilCommandFor is addUntilCommand with the player a "your" duration is
// keyed to given: an Effect's owner (EffectEffect.java:319).
func (g *Game) addUntilCommandFor(a *Ability, d string, cmd cardCommand, you PlayerID) error {
	if err := durationRefused(a.Params.Name, d); err != nil {
		return err
	}
	host := g.Card(a.Source)
	cmd.Host = host.ID
	phase := func(when untilWhen, p PlayerID, armed bool) {
		g.phaseCmds = append(g.phaseCmds, phaseCommand{cardCommand: cmd, When: when, Player: p, Armed: armed})
	}
	switch d {
	case "UntilEndOfCombat":
		g.endOfCombatCmds = append(g.endOfCombatCmds, cmd)
	case "UntilYourNextUpkeep":
		phase(untilUpkeepBegin, you, false)
	case "UntilTheEndOfYourNextUpkeep":
		phase(untilUpkeepEnd, you, g.activePhase == Upkeep)
	case "UntilEndOfCombatYourNextTurn":
		phase(untilEndCombat, you, true)
	case "ThisTurnAndNextTurn":
		phase(untilCleanupDefer, NoPlayer, false)
	case "UntilStateBasedActionChecked":
		phase(untilSBAChecked, NoPlayer, false)
	case "UntilTheEndOfYourNextUntap":
		phase(untilUntapEnd, you, false)
	case "UntilNextEndStep":
		phase(untilEndStepAt, NoPlayer, false)
	case "UntilYourNextEndStep":
		phase(untilEndStepBegin, you, false)
	case "UntilYourNextTurn":
		phase(untilTurnStart, you, false)
	case "UntilTheEndOfYourNextTurn":
		g.endOfNextTurnCmds = append(g.endOfNextTurnCmds, playerCommand{
			cardCommand: cmd, Player: you, Armed: g.activePlayer == you,
		})
	case "UntilHostLeavesPlay":
		host.leavesPlayCmds = append(host.leavesPlayCmds, cmd)
	case "UntilHostLeavesPlayOrEOT":
		host.leavesPlayCmds = append(host.leavesPlayCmds, cmd)
		g.endOfTurnCmds = append(g.endOfTurnCmds, cmd)
	case "UntilHostLeavesPlayOrEndOfCombat":
		host.leavesPlayCmds = append(host.leavesPlayCmds, cmd)
		g.endOfCombatCmds = append(g.endOfCombatCmds, cmd)
	case "UntilLoseControlOfHost":
		host.leavesPlayCmds = append(host.leavesPlayCmds, cmd)
		host.changeControllerCmds = append(host.changeControllerCmds, cmd)
	case "AsLongAsControl":
		host.leavesPlayCmds = append(host.leavesPlayCmds, cmd)
		host.changeControllerCmds = append(host.changeControllerCmds, cmd)
		host.phaseOutCmds = append(host.phaseOutCmds, cmd)
	case "AsLongAsInPlay":
		host.leavesPlayCmds = append(host.leavesPlayCmds, cmd)
		host.phaseOutCmds = append(host.phaseOutCmds, cmd)
	case "UntilUntaps":
		host.leavesPlayCmds = append(host.leavesPlayCmds, cmd)
		host.untapCmds = append(host.untapCmds, cmd)
		host.phaseOutCmds = append(host.phaseOutCmds, cmd)
	case "UntilTargetedUntaps":
		id, ok := firstCardTarget(a)
		if !ok {
			return fmt.Errorf("engine: %s: Duration$ UntilTargetedUntaps without a card target", a.Params.Name)
		}
		tgt := g.Card(id)
		tgt.leavesPlayCmds = append(tgt.leavesPlayCmds, cmd)
		tgt.untapCmds = append(tgt.untapCmds, cmd)
	case "UntilUnattached":
		host.leavesPlayCmds = append(host.leavesPlayCmds, cmd)
		host.unattachCmds = append(host.unattachCmds, cmd)
		host.phaseOutCmds = append(host.phaseOutCmds, cmd)
	default:
		g.endOfTurnCmds = append(g.endOfTurnCmds, cmd)
	}
	return nil
}

// runPhaseCommands runs and drops the commands on the list when names for
// player, as Phase.executeUntil(p)/executeAt() do. An armed command is
// skipped: it waits for registerUntilEndCommand to disarm it.
func (g *Game) runPhaseCommands(controller PlayerController, when untilWhen, player PlayerID) {
	var due []cardCommand
	kept := g.phaseCmds[:0:0]
	for _, p := range g.phaseCmds {
		if p.When != when || (p.Player != NoPlayer && p.Player != player) || p.Armed {
			kept = append(kept, p)
			continue
		}
		due = append(due, p.cardCommand)
	}
	g.phaseCmds = kept
	g.runCommands(controller, due)
}

// disarmPhaseCommands is Phase.registerUntilEndCommand(p): what was
// registered during this phase now waits for its end the next time.
func (g *Game) disarmPhaseCommands(when untilWhen, player PlayerID) {
	for i := range g.phaseCmds {
		if p := &g.phaseCmds[i]; p.When == when && p.Player == player {
			p.Armed = false
		}
	}
}

// endPhaseCommands is a phase's end for player: executeUntilEndOfPhase, then
// registerUntilEndCommand (PhaseHandler.java:485-486).
func (g *Game) endPhaseCommands(controller PlayerController, when untilWhen, player PlayerID) {
	g.runPhaseCommands(controller, when, player)
	g.disarmPhaseCommands(when, player)
}
