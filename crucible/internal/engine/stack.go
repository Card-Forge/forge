// The stack: CR 405. One resolvable [Ability] at a time, last on, first off.
//
// Ported from forge-game/src/main/java/forge/game/zone/MagicStack.java
// (1,025 LOC), cut down to the container and CR 405.5/608's resolve loop.
// addSimultaneousStackEntry itself (ordering several triggers that became
// true at once) is ported too, as pushTriggeredAbilities (trigger.go) --
// every real trigger-check function collects its own matches and calls it
// once, rather than calling PushAbility inline as each match is found.
// undoStack still has nothing to undo (no PlayerController method reaches
// it). freezeStack/unfreezeStack -- holding a push arriving while another
// ability is still resolving, so it lands only once that one finishes --
// stays unbuilt too: PassPriority (priority.go, ADR-0019) is real
// interactive priority now, but everything it can push happens between
// resolutions (resolveTop, one per round), never during one, so nothing
// yet needs holding.

package engine

// PushAbility puts an ability on the stack (CR 405.1, 601.2i, 603.3b) and
// emits AbilityActivated. It does not itself decide who is entitled to push
// or in what order -- that is pushTriggeredAbilities' own job (trigger.go)
// for CR 603.3b's APNAP ordering, the same way Move does not itself decide
// whether a zone change is legal. A caller pushing one ability at a time,
// unconditionally (casting a spell, an activated ability once one exists)
// has no ordering question to answer and calls this directly.
func (g *Game) PushAbility(a Ability) {
	if !a.hasHostTransforms && a.Source != NoCard && int(a.Source) < len(g.cards) {
		a.hostTransforms, a.hasHostTransforms = g.Card(a.Source).Transforms, true
	}
	g.nextStackItemID++
	a.ID = g.nextStackItemID
	g.stampTargets(&a)
	g.stack = append(g.stack, a)
	g.sink.Emit(Event{Kind: AbilityActivated, Phase: g.activePhase, Active: g.activePlayer, Actor: a.Controller, Turn: uint16(g.turn), Source: a.Source})
}

// StackLen is how many abilities are waiting to resolve.
func (g *Game) StackLen() int { return len(g.stack) }

// StackTop is the ability that would resolve next, and whether the stack
// holds one -- CR 405's "top of the stack" is the most recently pushed item.
func (g *Game) StackTop() (Ability, bool) {
	if len(g.stack) == 0 {
		return Ability{}, false
	}
	return g.stack[len(g.stack)-1], true
}

// ResolveStack resolves the stack to empty against reg by calling resolveTop
// until it is: CR 405.5's "keep resolving while nothing responds," the
// shape this port needs whenever nothing is asking a player whether to
// respond in between (every existing caller -- module tests, actions.go's
// resolvestack verb). PassPriority (priority.go, ADR-0019) calls resolveTop
// directly instead, once per full pass-around: CR 117.4 only ever resolves
// the single object on top of the stack before priority is offered again
// (CR 117.3b), and looping this to empty from inside a priority round would
// remove the response window between items that is the entire point of
// interactive priority.
//
// A resolution's own error stops the loop immediately and reaches the
// caller unchanged (GO-7): a bad card fails its game, not the batch, and
// does not get to resolve whatever was left under it as if nothing
// happened.
//
// A static trigger's error still pending from before the call (a mana
// ability's tap has no error return, statictrigger.go) is returned first,
// before anything resolves (ADR-0020 decision 4).
//
// A RestartGame resolution stops the loop the way the turn driver stops
// (Restarted, driver.go): whatever the restart left on the stack belongs to
// the restarted game, resolved once ResumeAfterRestart has begun it, and
// calling this again before then is an error (ADR-0034).
func (g *Game) ResolveStack(reg *Registry, controller PlayerController) error {
	if err := g.TakePendingError(); err != nil {
		return err
	}
	if g.restarted {
		return errRestartPending
	}
	for len(g.stack) > 0 && !g.over && !g.restarted {
		if err := g.resolveTop(reg, controller); err != nil {
			return err
		}
	}
	return nil
}

// resolveTop pops the top ability (CR 608.2m -- it leaves the stack before
// its effect happens, so a resolving ability never sees itself still
// there), checks CR 608.2b's own fizzle condition, dispatches it if it did
// not fizzle, emits AbilityResolved, moves a spell's own source off the
// stack (ADR-0018), then checks state-based actions (CR 704.3) -- the same
// pairing beginPhase runs after a turn-based action. One call is one
// resolution, CR 117.4's own grain: ResolveStack loops it to empty,
// PassPriority (priority.go) calls it once per full pass-around.
func (g *Game) resolveTop(reg *Registry, controller PlayerController) error {
	n := len(g.stack) - 1
	a := g.stack[n]
	g.stack[n] = Ability{}
	g.stack = g.stack[:n]

	if g.targetsStillLegal(&a) {
		// Captured before Resolve, not read fresh after: EndTurn/
		// EndCombatPhase (and any future effect touching turn state
		// mid-resolution) can advance g.activePhase/g.activePlayer/g.turn
		// inside Resolve, and AbilityResolved must describe the context
		// the ability actually resolved in, not wherever the state jumped
		// to afterward (found auditing this call site for RestartGame,
		// docs/crucible/adr/0033-restartgame-mid-resolution-reset.md).
		phase, active, turn := g.activePhase, g.activePlayer, g.turn
		if err := reg.Resolve(g, &a, controller); err != nil {
			return err
		}
		g.sink.Emit(Event{Kind: AbilityResolved, Phase: phase, Active: active, Actor: a.Controller, Turn: uint16(turn), Source: a.Source})
		// A restart (ADR-0034) returns at once: Java's resolveStack is
		// followed straight by mainLoopStep's RestartedByKarn exit
		// (PhaseHandler.java:1150-1155), with no state-based action check
		// before the restarted game's opening hands. RestartGame refuses a
		// host on the stack (restartgameeffect.go), so there is no spell
		// card left to move.
		if g.restarted {
			return nil
		}
	}
	g.moveResolvedSpellToGraveyard(a)
	CheckStateBasedActions(g, controller)
	return nil
}

// moveResolvedSpellToGraveyard is ADR-0018's own post-resolution step:
// permanentEffect and attachEffect (castspell.go) already move a's own
// source card off the Stack zone as part of what they resolve into, so this
// is a no-op for both -- and for an activated or triggered ability, whose
// Source never enters the Stack zone at all (only the card it names as its
// own host, castable a-la-carte in castspell.go's two cast paths, ever
// does). An Instant or Sorcery's own effect never moves its own source
// (permanentEffect's/attachEffect's own Battlefield destination has no
// meaning for a card that never becomes a permanent), so this is the one
// place CR 608.2m's "the spell... is then put into its owner's graveyard"
// actually happens for it -- fizzled (CR 608.2b) or resolved, the same
// either way.
//
// ReplaceGraveyard$ (CR 614's own "goes to exile instead" redirect) has no
// resolver in this port yet (ADR-0018's own Decision, point 3) -- this
// always moves to the graveyard, never anywhere else. A copy of a spell
// (Card.IsCopiedSpell) ceases to exist instead, inside Move itself
// (ceaseCopiedSpell, game.go; MagicStack.removeCardFromStack's
// ceaseToExist, MagicStack.java:680-683).
func (g *Game) moveResolvedSpellToGraveyard(a Ability) {
	if a.Source == NoCard || int(a.Source) >= len(g.cards) {
		return
	}
	c := g.Card(a.Source)
	if c.Zone != Stack {
		return
	}
	g.Move(a.Source, Graveyard, c.Owner)
}

// stackItem is the item on the stack whose ID is id, and whether one is --
// MagicStack.getInstanceMatchingSpellAbilityID. Anything that resolved,
// fizzled or was countered is gone.
func (g *Game) stackItem(id StackItemID) (*Ability, bool) {
	if id == NoStackItem {
		return nil, false
	}
	for i := range g.stack {
		if g.stack[i].ID == id {
			return &g.stack[i], true
		}
	}
	return nil, false
}

// spellItemOf is the ID of the spell card is on the stack as, and whether
// there is one: the item marked spell whose Source is card, top first. A
// card in the Stack zone is one spell (CR 405.1), so at most one matches.
func (g *Game) spellItemOf(card CardID) (StackItemID, bool) {
	for i := len(g.stack) - 1; i >= 0; i-- {
		if g.stack[i].spell && g.stack[i].Source == card {
			return g.stack[i].ID, true
		}
	}
	return NoStackItem, false
}
