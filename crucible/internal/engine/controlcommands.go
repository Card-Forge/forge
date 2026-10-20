// Card and phase commands: Java's GameCommand lists, as data.
//
// Ported from forge-game/src/main/java/forge/game/card/Card.java:3562-3640
// (add*Command / run*Commands), SpellAbilityEffect.java:949-1040
// (addUntilCommand, checkValidDuration), ControlGainEffect.java:137-215
// (getLoseControlCommand and its registrations) and GameAction.java:994-1033
// (controllerChangeZoneCorrection's runChangeControllerCommands).
//
// Java registers anonymous closures. A closure would be code in the game
// state, so each one this port needs is a cardCommand record naming what it
// does and to which card (ADR-0030's "data only" shape, scheduledaction.go):
// two kinds exist, ControlGainEffect's lose-control command and an Effect
// card's "this effect ends" command.

package engine

//enginelint:allow id card game zone player control ability amount valid untilcommand

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/expr"
)

// cardCommandKind is which Java closure a cardCommand stands for.
type cardCommandKind uint8

const (
	// commandLoseControl is ControlGainEffect.getLoseControlCommand: the
	// control change made at Timestamp on Target ends.
	commandLoseControl cardCommandKind = iota
	// commandEndEffect is addUntilCommand's closure for an Effect card: the
	// effect card Target leaves the Command zone.
	commandEndEffect
	// commandEndPump, commandEndAnimate and commandEndGoad are the closures
	// PumpEffect.applyPump, AnimateEffectBase and GoadEffect hand to
	// addUntilCommand: the effect applied to Target at Timestamp ends.
	commandEndPump
	commandEndAnimate
	commandEndGoad
	// commandEndCopy is CloneEffect's unclone command: the copy effect made at
	// Timestamp on Target ends.
	commandEndCopy
	// commandReturnExiled is changeZoneUntilCommand's closure for one card
	// (SpellAbilityEffect.java:853-907, Duration$ UntilHostLeavesPlay): Target,
	// still the object it became at zone stamp Timestamp, goes back to Zone.
	// Running it only queues it on Game.pendingReturns: the move needs a
	// controller, which a leaves-play hook inside Game.Move does not have
	// (returnUntilCards).
	commandReturnExiled
)

// cardCommand is one pending GameCommand. Host is the card whose command
// list or phase list it sits on (ControlGainEffect's source). Zone is the
// zone a commandReturnExiled card returns to.
type cardCommand struct {
	Kind      cardCommandKind
	Target    CardID
	Timestamp uint64
	Host      CardID
	Zone      ZoneType
}

// playerCommand is a command that waits for a player's next turn to end
// (Phase.addUntilEnd/registerUntilEnd): Armed marks one registered during
// that player's own turn, which survives that turn's cleanup
// (registerUntilEndCommand).
type playerCommand struct {
	cardCommand
	Player PlayerID
	Armed  bool
}

// removeTempController is Card.removeTempController(long): the control
// change made at ts ends. It reports whether there was one.
func (c *Card) removeTempController(ts uint64) bool {
	for i, e := range c.tempControllers {
		if e.Timestamp == ts {
			c.tempControllers = append(c.tempControllers[:i:i], c.tempControllers[i+1:]...)
			return true
		}
	}
	return false
}

// runCommand is GameCommand.run for one record. controller may be nil when
// the caller has no decision-maker to hand (a zone change, a phase out): a
// lose-control command then ends the control change and leaves
// controllerChangeZoneCorrection to the next state-based pass, which Java
// runs the same way (GameAction.java:1205-1207). Only the moment the
// permanent changes battlefield lists differs, and nothing resolves between.
func (g *Game) runCommand(controller PlayerController, cmd cardCommand) {
	switch cmd.Kind {
	case commandLoseControl:
		c := g.Card(cmd.Target)
		// StaticAbilityCantGainControl.cantGainControl(c) cannot be asked:
		// no CantGainControl static is evaluated yet (controlspelleffect.go
		// refuses the same).
		if c.Zone == Battlefield {
			c.removeTempController(cmd.Timestamp)
			if controller != nil {
				g.correctControllerZone(controller, cmd.Target)
			}
		}
	case commandEndEffect:
		if e := g.Card(cmd.Target); e.Zone == Command && e.IsEffect {
			g.exileEffect(cmd.Target)
		}
	case commandEndPump:
		kept := g.pumps[:0]
		for _, p := range g.pumps {
			if p.Card != cmd.Target || p.Timestamp != cmd.Timestamp {
				kept = append(kept, p)
			}
		}
		g.pumps = kept
	case commandEndAnimate:
		kept := g.animates[:0]
		for _, r := range g.animates {
			if r.Card != cmd.Target || r.Timestamp != cmd.Timestamp {
				kept = append(kept, r)
			}
		}
		g.animates = kept
	case commandEndCopy:
		if c := g.Card(cmd.Target); c.Zone == Battlefield {
			for _, e := range c.copies {
				if e.Timestamp == cmd.Timestamp {
					g.uncopy(cmd.Target, e)
					break
				}
			}
		}
	case commandEndGoad:
		c := g.Card(cmd.Target)
		kept := c.goadedBy[:0]
		for _, gd := range c.goadedBy {
			if gd.Timestamp != cmd.Timestamp {
				kept = append(kept, gd)
			}
		}
		c.goadedBy = kept
	case commandReturnExiled:
		g.pendingReturns = append(g.pendingReturns, cmd)
	}
}

// returnUntilCards runs the commandReturnExiled records waiting on
// Game.pendingReturns: each card still the object it was exiled as (Java's
// equalsWithGameTimestamp, SpellAbilityEffect.java:874-877) moves back to the
// zone it came from -- the battlefield under its owner's control, a hand to
// its owner's hand -- with no cause (:901), and the cards moved together fire
// one ChangesZoneAll per zone pair (Game.untilHostLeavesPlayTriggerList,
// CardZoneTable.java:136-140). It reports whether a card moved.
//
// Java runs the move inside the host's own zone change. Here a leaves-play
// hook has no controller (effectCardsSeeMove, inside Game.Move), so the
// records wait for the first caller holding one: the end of moveByEffect --
// the same effect's next step already sees the card back, as in Java -- or
// the next state-based pass, before any player gets priority. Between the
// two nothing a player can do happens; only the order of the host's own
// leaves trigger against the returned card's enter trigger on the stack can
// differ.
func (g *Game) returnUntilCards(controller PlayerController) bool {
	if controller == nil || len(g.pendingReturns) == 0 {
		return false
	}
	due := g.pendingReturns
	g.pendingReturns = nil
	type batch struct {
		from, to ZoneType
		cards    []CardID
	}
	var batches []batch
	for _, cmd := range due {
		c := g.Card(cmd.Target)
		if c.zoneStamp != cmd.Timestamp {
			continue
		}
		from := c.Zone
		g.moveByEffect(controller, cmd.Target, cmd.Zone, 0, NoPlayer, false)
		to := g.Card(cmd.Target).Zone
		if to == from {
			continue
		}
		i := 0
		for i < len(batches) && (batches[i].from != from || batches[i].to != to) {
			i++
		}
		if i == len(batches) {
			batches = append(batches, batch{from: from, to: to})
		}
		batches[i].cards = append(batches[i].cards, cmd.Target)
	}
	for _, b := range batches {
		g.checkChangesZoneAllTriggers(controller, b.cards, b.from, b.to)
	}
	return len(batches) > 0
}

// runCommands runs cmds in order, the loop shared by every run*Commands.
func (g *Game) runCommands(controller PlayerController, cmds []cardCommand) {
	for _, cmd := range cmds {
		g.runCommand(controller, cmd)
	}
}

// runLeavesPlayCommands is Card.runLeavesPlayCommands: id left the
// battlefield (GameAction.java:533, 960). The list is cleared first so a
// command that moves cards cannot run it twice.
func (g *Game) runLeavesPlayCommands(controller PlayerController, id CardID) {
	c := g.Card(id)
	cmds := c.leavesPlayCmds
	// The other lists belong to the object that left: Java's zone change
	// makes a new Card with empty ones (CR 400.7).
	c.leavesPlayCmds, c.untapCmds, c.changeControllerCmds, c.phaseOutCmds = nil, nil, nil, nil
	c.unattachCmds, c.staticCmds = nil, nil
	g.runCommands(controller, cmds)
}

// runUnattachCommands is Card.runUnattachCommands: id stopped being attached
// (Card.java:3995). The list is cleared first.
func (g *Game) runUnattachCommands(controller PlayerController, id CardID) {
	c := g.Card(id)
	cmds := c.unattachCmds
	c.unattachCmds = nil
	g.runCommands(controller, cmds)
}

// staticCheck is one entry of Card.staticCommandList (ControlGainEffect.java:
// 191-195): the host's SVar named Left, evaluated with Affected as the
// source, is compared with Compare's operand evaluated on the host; when the
// comparison holds Cmd runs and the entry goes.
type staticCheck struct {
	Amounts  map[string]expr.Amount
	Left     string
	Compare  string
	Affected CardID
	Cmd      cardCommand
}

// staticCheckHolds is the comparison of GameAction.java:1187-1191.
func (g *Game) staticCheckHolds(host *Card, sc staticCheck) bool {
	amt, ok := sc.Amounts[strings.ToLower(sc.Left)]
	if !ok || len(sc.Compare) < 3 {
		return false
	}
	affected := g.Card(sc.Affected)
	left, ok := resolveAmount(g, sc.Amounts, affected.Controller(), affected.ID, amt)
	if !ok {
		return false
	}
	right, ok := resolveNamedAmount(g, sc.Amounts, host, sc.Compare[2:])
	if !ok {
		return false
	}
	return compareOp(left, sc.Compare[:2], right)
}

// runStaticCommands is the staticCommandList loop of GameAction.
// checkStaticAbilities (java:1180-1198), run right after the layers: every
// card with an entry whose comparison holds runs its command and drops the
// entry. It reports whether any ran.
func (g *Game) runStaticCommands(controller PlayerController) bool {
	ran := false
	for i := range g.cards {
		host := &g.cards[i]
		if len(host.staticCmds) == 0 {
			continue
		}
		var due []cardCommand
		kept := host.staticCmds[:0:0]
		for _, sc := range host.staticCmds {
			if g.staticCheckHolds(host, sc) {
				due = append(due, sc.Cmd)
			} else {
				kept = append(kept, sc)
			}
		}
		host.staticCmds = kept
		g.runCommands(controller, due)
		ran = ran || len(due) > 0
	}
	return ran
}

// runUntapCommands is Card.runUntapCommands, run as id untaps
// (Card.java:4724). Callers check the card was tapped: Card.untap returns
// early otherwise.
func (g *Game) runUntapCommands(controller PlayerController, id CardID) {
	c := g.Card(id)
	cmds := c.untapCmds
	c.untapCmds = nil
	g.runCommands(controller, cmds)
}

// runChangeControllerCommands is Card.runChangeControllerCommands: id is
// about to change controller (GameAction.java:1023).
func (g *Game) runChangeControllerCommands(controller PlayerController, id CardID) {
	c := g.Card(id)
	cmds := c.changeControllerCmds
	c.changeControllerCmds = nil
	g.runCommands(controller, cmds)
}

// runPhaseOutCommands is Card.runPhaseOutCommands (CR 702.26f), run as id
// phases out (Card.java:5650).
func (g *Game) runPhaseOutCommands(controller PlayerController, id CardID) {
	c := g.Card(id)
	cmds := c.phaseOutCmds
	c.phaseOutCmds = nil
	g.runCommands(controller, cmds)
}

// runEndOfTurnCommands is EndOfTurn.executeUntil() at cleanup
// (PhaseHandler.java:406) followed by executeUntilEndOfPhase(playerTurn) and
// registerUntilEndCommand(playerTurn) (:407-408): the until-end-of-turn
// list, then the active player's until-the-end-of-your-next-turn commands
// registered before this turn, then the ones registered during it armed.
func (g *Game) runEndOfTurnCommands(controller PlayerController) {
	cmds := g.endOfTurnCmds
	g.endOfTurnCmds = nil
	g.runCommands(controller, cmds)

	var due []cardCommand
	kept := g.endOfNextTurnCmds[:0:0]
	for _, p := range g.endOfNextTurnCmds {
		switch {
		case p.Player != g.activePlayer:
			kept = append(kept, p)
		case p.Armed:
			p.Armed = false
			kept = append(kept, p)
		default:
			due = append(due, p.cardCommand)
		}
	}
	g.endOfNextTurnCmds = kept
	g.runCommands(controller, due)

	// :409: what an end-of-combat "your next turn" duration registered waits
	// for that turn's end of combat now; and ThisTurnAndNextTurn's outer
	// command moves its inner one onto the list for the next cleanup.
	g.disarmPhaseCommands(untilEndCombat, g.activePlayer)
	var deferred []phaseCommand
	rest := g.phaseCmds[:0:0]
	for _, pc := range g.phaseCmds {
		if pc.When == untilCleanupDefer {
			deferred = append(deferred, pc)
			continue
		}
		rest = append(rest, pc)
	}
	g.phaseCmds = rest
	for _, pc := range deferred {
		g.endOfTurnCmds = append(g.endOfTurnCmds, pc.cardCommand)
	}
}

// runEndOfCombatCommands is EndOfCombat.executeUntil() then
// executeUntilEndOfPhase(playerTurn) (PhaseHandler.endCombat, java:1266-1267):
// the until-end-of-combat list, then the active player's registered durations.
func (g *Game) runEndOfCombatCommands(controller PlayerController) {
	cmds := g.endOfCombatCmds
	g.endOfCombatCmds = nil
	g.runCommands(controller, cmds)
	g.runPhaseCommands(controller, untilEndCombat, g.activePlayer)
}

// loseControlTokens are the LoseControl$ values ControlGainEffect registers
// a command for, in the order its resolve checks them.
var loseControlTokens = map[string]bool{
	"LeavesPlay": true, "Untap": true, "LoseControl": true, "EOT": true,
	"EndOfCombat": true, "UntilTheEndOfYourNextTurn": true,
	"StaticCommandCheck": true, "UntilSourceUnattached": true,
}

// parseLoseControl splits LoseControl$ on "," as ControlGainEffect.resolve
// does. A value this port cannot register is an error, never silently
// dropped (GO-7), and so is a token whose inputs are missing:
// StaticCommandCheck without its StaticCommandCheckSVar$ and
// StaticCommandSVarCompare$ (Java reads a null SVar there), and
// UntilSourceUnattached on an ability no Mode$ Attached trigger put on the
// stack (Java reads a null triggering Source).
func parseLoseControl(a *Ability, raw string) ([]string, error) {
	tokens := make([]string, 0, 4)
	for _, tok := range strings.Split(raw, ",") {
		if !loseControlTokens[tok] {
			return nil, fmt.Errorf("engine: GainControl: LoseControl$ %q not resolvable yet", tok)
		}
		tokens = append(tokens, tok)
	}
	if hasToken(tokens, "StaticCommandCheck") {
		name, ok := a.Params.Param("StaticCommandCheckSVar")
		cmp, ok2 := a.Params.Param("StaticCommandSVarCompare")
		if _, known := a.Amounts[strings.ToLower(name)]; !ok || !ok2 || !known || len(cmp) < 3 {
			return nil, fmt.Errorf("engine: GainControl: LoseControl$ StaticCommandCheck needs StaticCommandCheckSVar$ naming an SVar and StaticCommandSVarCompare$")
		}
	}
	if hasToken(tokens, "UntilSourceUnattached") {
		if _, ok := a.triggered.source.AsCard(); !ok {
			return nil, fmt.Errorf("engine: GainControl: LoseControl$ UntilSourceUnattached needs the attachment of a Mode$ Attached trigger")
		}
	}
	return tokens, nil
}

func hasToken(tokens []string, want string) bool {
	for _, t := range tokens {
		if t == want {
			return true
		}
	}
	return false
}

// registerLoseControl is the `if (lose != null)` block of
// ControlGainEffect.resolve for one target: the lose-control command goes on
// the host's lists and the game's phase lists, as each token says.
func (g *Game) registerLoseControl(host *Card, tokens []string, target CardID, ts uint64, a *Ability) {
	activator := a.Controller
	cmd := cardCommand{Kind: commandLoseControl, Target: target, Timestamp: ts, Host: host.ID}
	if hasToken(tokens, "LeavesPlay") && host.ID != target {
		// Only return control if host and target are different cards.
		host.leavesPlayCmds = append(host.leavesPlayCmds, cmd)
	}
	if hasToken(tokens, "Untap") {
		host.untapCmds = append(host.untapCmds, cmd)
	}
	if hasToken(tokens, "LoseControl") {
		host.changeControllerCmds = append(host.changeControllerCmds, cmd)
	}
	if hasToken(tokens, "EOT") {
		g.endOfTurnCmds = append(g.endOfTurnCmds, cmd)
	}
	if hasToken(tokens, "EndOfCombat") {
		g.endOfCombatCmds = append(g.endOfCombatCmds, cmd)
	}
	if hasToken(tokens, "StaticCommandCheck") {
		left, _ := a.Params.Param("StaticCommandCheckSVar")
		cmp, _ := a.Params.Param("StaticCommandSVarCompare")
		host.staticCmds = append(host.staticCmds, staticCheck{
			Amounts: a.Amounts, Left: left, Compare: cmp, Affected: target, Cmd: cmd,
		})
	}
	if hasToken(tokens, "UntilSourceUnattached") {
		attachment, _ := a.triggered.source.AsCard()
		at := g.Card(attachment)
		at.leavesPlayCmds = append(at.leavesPlayCmds, cmd)
		at.phaseOutCmds = append(at.phaseOutCmds, cmd)
		at.unattachCmds = append(at.unattachCmds, cmd)
	}
	if hasToken(tokens, "UntilTheEndOfYourNextTurn") {
		g.endOfNextTurnCmds = append(g.endOfNextTurnCmds, playerCommand{
			cardCommand: cmd, Player: activator,
			// registerUntilEnd during the activator's own turn, addUntilEnd
			// otherwise (ControlGainEffect.java:196-200).
			Armed: g.activePlayer == activator,
		})
	}
}
