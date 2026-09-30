// Scheduled engine actions and the player-control redirect they drive
// (ADR-0030): CR 800.4b's "controls another player" as read-only engine
// state, granted and revoked at phase boundaries the way Java's
// Phase.addUntil commands are.
//
// Ported from forge-game/src/main/java/forge/game/phase/Phase.java (the
// untilMap/until lists), PhaseHandler.java:299-301/515-518/1261-1263 (where
// they run) and Player.java:2517-2566 (addController/removeController/
// getControllingPlayer/isControlled).

package engine

//enginelint:allow id game player

// scheduledBoundary is the phase boundary a scheduledAction waits for --
// which of Java's Phase objects the command was added to.
type scheduledBoundary uint8

const (
	// boundaryCleanup is Game.getCleanup(): the turn transition that ends a
	// cleanup step, where PhaseHandler.java:515-518 runs the cleanup
	// Phase's until lists once the next active player is known.
	boundaryCleanup scheduledBoundary = iota
	// boundaryBeginCombat is Game.getBeginOfCombat(): the start of a
	// beginning-of-combat step (PhaseHandler.java:301).
	boundaryBeginCombat
	// boundaryEndCombat is Game.getEndOfCombat(): combat ending
	// (PhaseHandler.endCombat, PhaseHandler.java:1261-1263).
	boundaryEndCombat
)

// scheduledKind is what a scheduledAction does when its boundary is
// reached.
type scheduledKind uint8

const (
	// scheduledControlGrant is ControlPlayerEffect.java:34-45's first
	// closure: Controller starts controlling Target (unless Controller has
	// left the game, CR 800.4b), and the matching revoke is scheduled.
	scheduledControlGrant scheduledKind = iota
	// scheduledControlRevoke is its second closure: the grant made at
	// Timestamp ends.
	scheduledControlRevoke
)

// scheduledAction is one engine action waiting on a phase boundary: data
// only, never a script line, so it stays apart from delayedTrigger (which
// carries a compile.Ability resolved through Registry.Resolve) and shares
// only its gating shape (ADR-0030, "Scheduling"). Java keeps one command
// list per Phase plus one per player; At picks the Phase and ForPlayer the
// player list, NoPlayer meaning the unkeyed list.
type scheduledAction struct {
	Kind scheduledKind
	// At is the boundary this action waits for.
	At scheduledBoundary
	// ForPlayer holds the action until the boundary is reached with that
	// player active (Phase.addUntil(Player, ...), executeUntil(playerTurn));
	// NoPlayer runs it at the first such boundary (Phase.addUntil(...),
	// executeUntil()).
	ForPlayer PlayerID
	// Target is the controlled player, Controller the controlling one.
	Target, Controller PlayerID
	// UntilEndOfCombat is ControlPlayer's Combat$: the grant's revoke
	// waits for end of combat rather than the next cleanup.
	UntilEndOfCombat bool
	// Timestamp names the grant a revoke ends.
	Timestamp uint64
}

// ControllingPlayer is Player.getControllingPlayer: the player making pid's
// decisions under the newest control grant in force, or NoPlayer when no
// grant is (pid decides for themselves). Every PlayerID the engine passes
// around stays pid throughout -- the redirect changes whose brain answers,
// not who acts (ADR-0030) -- so this is for a reader that must know who is
// deciding, such as a harness routing a different AI per seat.
func (g *Game) ControllingPlayer(pid PlayerID) PlayerID {
	grants := g.Player(pid).controlledBy
	if len(grants) == 0 {
		return NoPlayer
	}
	return grants[len(grants)-1].Controller
}

// IsControlled is Player.isControlled: another player controls pid now.
// A player controlling themselves (Mindslaver aimed at its own
// controller) is not controlled.
func (g *Game) IsControlled(pid PlayerID) bool {
	c := g.ControllingPlayer(pid)
	return c != NoPlayer && c != pid
}

// addControlGrant is Player.addController: a new grant, newest last. The
// slice stays in timestamp order because timestamps only grow.
func (g *Game) addControlGrant(target, controller PlayerID) uint64 {
	g.timestamp++
	p := g.Player(target)
	p.controlledBy = append(p.controlledBy, controlGrant{Timestamp: g.timestamp, Controller: controller})
	return g.timestamp
}

// removeControlGrant is Player.removeController(long): the grant made at ts
// ends, and control reverts to whichever grant is now newest (CR 800.4b's
// "next-most-recent"), or to the player themself.
func (g *Game) removeControlGrant(target PlayerID, ts uint64) {
	p := g.Player(target)
	for i, gr := range p.controlledBy {
		if gr.Timestamp == ts {
			p.controlledBy = append(p.controlledBy[:i:i], p.controlledBy[i+1:]...)
			return
		}
	}
}

// releaseControlBy is Game.onPlayerLost's "free any mindslaves"
// (Game.java:1014-1017): every grant naming leaver as controller ends.
func (g *Game) releaseControlBy(leaver PlayerID) {
	for _, pid := range g.Players() {
		p := g.Player(pid)
		kept := p.controlledBy[:0]
		for _, gr := range p.controlledBy {
			if gr.Controller != leaver {
				kept = append(kept, gr)
			}
		}
		p.controlledBy = kept
	}
}

// schedule queues a for its boundary.
func (g *Game) schedule(a scheduledAction) {
	g.scheduled = append(g.scheduled, a)
}

// runScheduledActions runs every action waiting on boundary b, with active
// the player whose turn it now is: first the unkeyed ones
// (Phase.executeUntil()), then those keyed to active
// (Phase.executeUntil(playerTurn)) -- PhaseHandler.java:515-518's order,
// "do this first for ControlPlayer". Each pass takes its batch out of the
// list before running any of it (Phase.excute's own copy), so an action
// scheduled while this boundary runs -- a grant's own revoke -- waits for
// the next boundary instead of firing at once.
//
// Java runs only the keyed list at begin of combat and only the unkeyed one
// at end of combat; every action queued for those two boundaries is keyed
// or unkeyed respectively (a grant is always keyed, its revoke never), so
// running both passes everywhere is the same thing.
//
// At the turn boundary an action keyed to a player who has left the game
// is dropped: PhaseHandler.handleMultiplayerEffects runs it as the turn
// passes that player's seat (CR 800.4m), and a grant over a player no
// longer in the game has nothing to observe it.
func (g *Game) runScheduledActions(b scheduledBoundary, active PlayerID) {
	if len(g.scheduled) == 0 {
		return
	}
	for _, a := range g.takeScheduled(b, func(a scheduledAction) bool { return a.ForPlayer == NoPlayer }) {
		g.runScheduled(a)
	}
	for _, a := range g.takeScheduled(b, func(a scheduledAction) bool { return a.ForPlayer == active }) {
		g.runScheduled(a)
	}
	if b == boundaryCleanup {
		g.takeScheduled(b, func(a scheduledAction) bool {
			return a.ForPlayer != NoPlayer && g.Player(a.ForPlayer).Lost
		})
	}
}

// takeScheduled removes and returns, in scheduling order, every action
// waiting on b that match selects.
func (g *Game) takeScheduled(b scheduledBoundary, match func(scheduledAction) bool) []scheduledAction {
	var batch []scheduledAction
	kept := g.scheduled[:0]
	for _, a := range g.scheduled {
		if a.At == b && match(a) {
			batch = append(batch, a)
			continue
		}
		kept = append(kept, a)
	}
	g.scheduled = kept
	return batch
}

// runScheduled performs one scheduled action.
func (g *Game) runScheduled(a scheduledAction) {
	switch a.Kind {
	case scheduledControlGrant:
		// CR 800.4b: a controller who has left the game gains nothing.
		if g.Player(a.Controller).Lost {
			return
		}
		ts := g.addControlGrant(a.Target, a.Controller)
		revokeAt := boundaryCleanup
		if a.UntilEndOfCombat {
			revokeAt = boundaryEndCombat
		}
		g.schedule(scheduledAction{Kind: scheduledControlRevoke, At: revokeAt, ForPlayer: NoPlayer, Target: a.Target, Timestamp: ts})
	case scheduledControlRevoke:
		g.removeControlGrant(a.Target, a.Timestamp)
	}
}
