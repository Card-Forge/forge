// Interactive priority: CR 117. ADR-0019.
//
// PassPriority is the standalone entry a test or the `passpriority` fixture
// verb calls, gated by givesPriority. The turn driver (driver.go, ADR-0026)
// calls priorityRound directly, gated by each step's own grant (beginStep,
// turn.go) instead. AdvancePhase never opens a round: it is bookkeeping.

package engine

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/mana"
)

// actionError is PassPriority's own GO-7 stop: a queued Action that
// CastSpell/ActivateAbility declined. Naming the player, the card and (for
// an activate) the ability index is enough to find the fixture line that
// queued it -- the same information a scenario's own actions.log already
// carries for the verb that produced it.
type actionError struct {
	pid   PlayerID
	kind  string
	card  CardID
	index int
}

func (e *actionError) Error() string {
	if e.kind == "activate" {
		return fmt.Sprintf("engine: player %d could not %s card %d ability %d", e.pid, e.kind, e.card, e.index)
	}
	return fmt.Sprintf("engine: player %d could not %s card %d", e.pid, e.kind, e.card)
}

// Action is a player's answer to being offered priority: cast a spell
// (Kind == ActionCast, Card names the hand card CastSpell would take),
// activate an ability (Kind == ActionActivate, Card and AbilityIndex are
// ActivateAbility's own arguments), play a land (ActionPlayLand), or
// activate a mana ability -- a basic land's intrinsic one (ActionTapForMana,
// Color is the one color it taps for) or a scripted one
// (ActionManaAbility, AbilityIndex as ActivateManaAbility takes it), or
// unlock a Room's locked door (ActionUnlockDoor). Door is the half a Room
// card is cast as (ActionCast; DoorLeft for every other card) or the door
// ActionUnlockDoor unlocks. The zero value, ActionPass, is a pass -- GO-8's
// typed-struct shape, not a map or a sentinel string.
type Action struct {
	Kind         ActionKind
	Card         CardID
	AbilityIndex int
	Color        mana.Colors
	Door         Door
}

// ActionKind is Action's own discriminant.
type ActionKind uint8

const (
	// ActionPass is the zero value: decline priority.
	ActionPass ActionKind = iota
	// ActionCast casts Card via CastSpell.
	ActionCast
	// ActionActivate activates Card's AbilityIndex'th ability via
	// ActivateAbility.
	ActionActivate
	// ActionPlayLand plays Card via PlayLand: a special action (CR 116.2a,
	// 305.1), after which the player keeps priority (CR 116.3). ADR-0026.
	ActionPlayLand
	// ActionTapForMana taps the basic land Card for Color via
	// TapLandForMana (CR 605.3a: no stack, priority kept). ADR-0026.
	ActionTapForMana
	// ActionManaAbility activates Card's AbilityIndex'th mana ability via
	// ActivateManaAbility (CR 605.3a). ADR-0026.
	ActionManaAbility
	// ActionUnlockDoor unlocks Door of the Room Card via UnlockDoor: a
	// special action (CR 116.2, 709.5), after which the player keeps priority.
	ActionUnlockDoor
)

// canActSorcerySpeed is CR 307.1's own timing restriction (Player.java's
// canCastSorcery, forge-game/src/main/java/forge/game/player/Player.java:
// 2512-2515): pid's own turn, a main phase, an empty stack. CastSpell
// applies it to everything except an Instant; ActivateAbility applies it to
// a loyalty ability (Planeswalker$) or one explicitly marked
// SorcerySpeed$ -- every other activated ability is instant speed by
// default, the same default Java's own canCastTiming has
// (SpellAbility.java:2607-2610).
func (g *Game) canActSorcerySpeed(pid PlayerID) bool {
	return pid == g.activePlayer && (g.activePhase == Main1 || g.activePhase == Main2) && len(g.stack) == 0
}

// givesPriority reports whether phase offers priority at all -- ported from
// PhaseHandler.onPhaseBegin's own per-phase givePriorityToPlayer sets
// (PhaseHandler.java:240-451). Untap never does (CR 502.4). Cleanup only
// does when a state-based action found something to do or the stack is
// non-empty (CR 514.3a) -- checked here, not cached, since it can only be
// known by actually running the check. The no-attackers/no-damage
// combat-step cases (PhaseHandler.java:305-344) need a real per-step body
// this port's own Combat does not have yet (turn.go's header) and are
// deferred, not silently wrong: nothing calls PassPriority from those steps
// until the turn structure wires it in. Every other phase grants it
// unconditionally.
func (g *Game) givesPriority(phase PhaseType, controller PlayerController) bool {
	switch phase {
	case Untap:
		return false
	case Cleanup:
		_, performed := checkStateBasedActions(g, controller)
		return performed || len(g.stack) != 0
	default:
		return true
	}
}

// PassPriority runs one CR 117 priority round against reg to completion:
// each live player, starting with the active player, is asked TakeAction in
// turn order; a pass moves priority to the next live player; an action
// applies it and hands priority back to the actor (CR 117.3c). Once
// priority has gone all the way around with nothing but passes, either the
// stack is empty (the round ends) or resolveTop (stack.go) resolves the one
// object on top of it (CR 117.4) and a fresh round starts with the active
// player (CR 117.3b), the active player's own next-in-line if they have
// since lost.
//
// Returns nil without asking anyone if the current phase does not grant
// priority (givesPriority).
//
// A static trigger's pending error (statictrigger.go) is returned at entry
// and after each applied action, before anyone is asked again (ADR-0020
// decision 4).
func (g *Game) PassPriority(reg *Registry, controller PlayerController) error {
	if err := g.TakePendingError(); err != nil {
		return err
	}
	if g.restarted {
		return errRestartPending
	}
	if !g.givesPriority(g.activePhase, controller) {
		return nil
	}
	return g.priorityRound(reg, controller)
}

// priorityRound is PassPriority's loop, without its entry checks: the turn
// driver (driver.go) takes pending errors itself and gates on each step's
// own grant, never on givesPriority (ADR-0026 Decision 3).
func (g *Game) priorityRound(reg *Registry, controller PlayerController) error {
	holder := g.activePlayer
	if g.Player(holder).Lost {
		holder = g.nextPlayerAfter(holder)
	}
	for !g.over {
		CheckStateBasedActions(g, controller)
		if g.over {
			return nil
		}

		passed := 0
		for passed < g.livePlayerCount() && !g.over {
			a := controller.TakeAction(g, holder)
			if a.Kind == ActionPass {
				holder = g.nextPlayerAfter(holder)
				passed++
				continue
			}
			if err := g.applyAction(holder, a, controller); err != nil {
				return err
			}
			if err := g.TakePendingError(); err != nil {
				return err
			}
			CheckStateBasedActions(g, controller)
			if g.over {
				return nil
			}
			// CR 117.3c: the actor keeps priority, unless losing the game
			// they were just about to hold it in took them out of it too.
			if g.Player(holder).Lost {
				holder = g.nextPlayerAfter(holder)
			}
			passed = 0
		}
		if g.over {
			return nil
		}
		if len(g.stack) == 0 {
			return nil
		}
		if err := g.resolveTop(reg, controller); err != nil {
			return err
		}
		// A RestartGame resolution ends the round (ADR-0034): nobody in
		// the old game is asked for anything more.
		if g.restarted {
			return nil
		}
		holder = g.activePlayer
		if g.Player(holder).Lost {
			holder = g.nextPlayerAfter(holder)
		}
	}
	return nil
}

// livePlayerCount is how many players have not yet lost -- recomputed on
// every check rather than cached, since a state-based action inside the
// loop can remove one mid-round.
func (g *Game) livePlayerCount() int {
	n := 0
	for _, pid := range g.Players() {
		if !g.Player(pid).Lost {
			n++
		}
	}
	return n
}

// applyAction turns a non-pass Action into the CastSpell/ActivateAbility
// call it names. Both already carry their own CR 307.1 timing check
// (castspell.go, activateability.go) and their existing "false means
// declined" contract is unchanged for every other caller -- PassPriority is
// the one caller asking on a controller's behalf, so a false here is not an
// ordinary decline: the oracle should never queue an action it cannot
// legally take, so a false becomes an error naming what failed (GO-7).
func (g *Game) applyAction(pid PlayerID, a Action, controller PlayerController) error {
	switch a.Kind {
	case ActionCast:
		cast := false
		if a.Door == DoorLeft {
			cast = g.CastSpell(pid, a.Card, controller)
		} else {
			cast = g.CastRoomDoor(pid, a.Card, a.Door, controller)
		}
		if !cast {
			return &actionError{pid: pid, kind: "cast", card: a.Card}
		}
	case ActionUnlockDoor:
		if !g.UnlockDoor(pid, a.Card, a.Door, controller) {
			return &actionError{pid: pid, kind: fmt.Sprintf("unlock door %s of", a.Door), card: a.Card}
		}
	case ActionActivate:
		if !g.ActivateAbility(pid, a.Card, a.AbilityIndex, controller) {
			return &actionError{pid: pid, kind: "activate", card: a.Card, index: a.AbilityIndex}
		}
	case ActionPlayLand:
		if !g.PlayLand(pid, a.Card, controller) {
			return &actionError{pid: pid, kind: "play land", card: a.Card}
		}
	case ActionTapForMana:
		// TapLandForMana panics on a Colors value with more than one bit
		// (manaability.go): an invariant for its internal callers, but here
		// the value came from a controller, so it is that controller's error
		// (GO-7).
		if a.Color == 0 || a.Color&(a.Color-1) != 0 {
			return &actionError{pid: pid, kind: fmt.Sprintf("tap for mana of colors %v with", a.Color), card: a.Card}
		}
		if !g.TapLandForMana(pid, a.Card, a.Color, controller) {
			return &actionError{pid: pid, kind: "tap for mana", card: a.Card}
		}
	case ActionManaAbility:
		if !g.ActivateManaAbility(pid, a.Card, a.AbilityIndex, controller) {
			return &actionError{pid: pid, kind: "activate", card: a.Card, index: a.AbilityIndex}
		}
	default:
		return &actionError{pid: pid, kind: fmt.Sprintf("take an action of unknown kind %d for", a.Kind), card: a.Card}
	}
	return nil
}
