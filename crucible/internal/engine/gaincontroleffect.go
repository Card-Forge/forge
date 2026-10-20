package engine

//enginelint:allow id card game ability defined condition control parts zone continuous controlcommands resolvechoice

import "fmt"

// gainControlUnresolvedParams are ControlGainEffect.java's params this port
// cannot honour yet: the sweep shape (AllValid$). The target-selection restrictions (TargetsForEachPlayer$,
// MaxTotalTargetCMC$, TargetsWithControllerProperty$, TargetingPlayer$,
// TargetingPlayerControls$ and the random pick, TargetsAtRandom$) are
// targeting.go's.
var gainControlUnresolvedParams = [...]string{
	"AllValid", "Ultimate",
}

// gainControlEffect is ControlGainEffect.java's permanent shape: the
// NewController$ player (default the activator) gains control of each
// targeted or Defined$ permanent (default Self) with no end date --
// Card.addTempController, a timestamped entry Controller merges with
// Layer 2's continuous effects (Card.tempControllers). Untap$ untaps it;
// Optional$ asks per permanent. A permanent that changes controller is
// removed from combat and is summoning sick again (CR 506.4, 302.6).
type gainControlEffect struct{}

func (gainControlEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range gainControlUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: GainControl: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	newController := a.Controller
	if spec, ok := a.Params.Param("NewController"); ok {
		players, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
		if err != nil {
			return fmt.Errorf("engine: GainControl: NewController$: %w", err)
		}
		if len(players) > 0 {
			newController = players[0]
		}
	}
	var cards []CardID
	if spec, ok := a.Params.Param("Choices"); ok {
		// ControlGainEffect.java:106-120: Chooser$ (the activator) picks one
		// battlefield permanent, never optionally; none to pick ends the effect.
		chooser, err := choiceChooser(g, a, "GainControl")
		if err != nil {
			return err
		}
		offer, err := choiceCandidates(g, a, "GainControl", []ZoneType{Battlefield}, spec)
		if err != nil {
			return err
		}
		if cards, err = askChoiceCards(g, controller, a, "GainControl", chooser, offer, 1, 1); err != nil || len(cards) == 0 {
			return err
		}
	} else {
		var err error
		if cards, err = targetedOrDefinedCards(source, a.Params, a.refs()); err != nil {
			return fmt.Errorf("engine: GainControl: %w", err)
		}
	}
	_, untap := a.Params.Param("Untap")
	_, optional := a.Params.Param("Optional")
	_, remember := a.Params.Param("RememberControlled")
	_, forget := a.Params.Param("ForgetControlled")
	var keywords []string
	if _, ok := a.Params.Param("AddKWs"); ok {
		var ok bool
		if keywords, ok = keywordTokens(a.Params, "AddKWs"); !ok {
			return fmt.Errorf("engine: GainControl: AddKWs$ not resolvable yet")
		}
	}
	var lose []string
	if raw, ok := a.Params.Param("LoseControl"); ok {
		var err error
		if lose, err = parseLoseControl(a, raw); err != nil {
			return err
		}
		// "Check for lose control criteria right away"
		// (ControlGainEffect.java:127-135).
		if hasToken(lose, "LeavesPlay") && source.Zone != Battlefield {
			return nil
		}
		if hasToken(lose, "LoseControl") && source.Controller() != a.Controller {
			return nil
		}
		if hasToken(lose, "Untap") && !source.Tapped {
			return nil
		}
	}
	for _, id := range cards {
		c := g.Card(id)
		// canBeControlledBy's isInGame half; the CantGainControl static
		// half is not evaluated (controlspelleffect.go refuses it).
		if c.Zone != Battlefield || g.Player(newController).Lost || c.IsPhasedOut() {
			continue
		}
		if optional && !controller.ConfirmEffect(g, a.Controller, a.Source) {
			continue
		}
		g.timestamp++
		ts := g.timestamp
		c.tempControllers = append(c.tempControllers, ControlEffect{Timestamp: ts, Controller: newController})
		if untap && c.Tapped {
			g.runUntapCommands(controller, id)
			c.Tapped = false
			g.checkUntapsTriggers(controller, id)
		}
		if len(keywords) > 0 {
			// addChangedCardKeywords at ts, removed by a command at end of
			// turn (ControlGainEffect.java:151-154, 223-235): a Pump-shaped
			// record that is gone at the next cleanup.
			g.pumps = append(g.pumps, pumpRecord{Card: id, Timestamp: ts, Keywords: keywords})
		}
		if remember {
			source.Memory.Remember(CardEntity(id))
		}
		if forget {
			source.Memory.Forget(CardEntity(id))
		}
		if lose != nil {
			g.registerLoseControl(source, lose, id, ts, a)
		}
		g.correctControllerZone(controller, id)
	}
	return nil
}

// changeController gives id a new timestamped controller (Java's
// addTempController plus controllerChangeZoneCorrection): when that is a
// real change the permanent moves to its controller's battlefield list,
// leaves combat and is summoning sick again (correctControllerZone).
func (g *Game) changeController(controller PlayerController, id CardID, to PlayerID) {
	g.timestamp++
	g.changeControllerAt(controller, id, to, g.timestamp)
}

// changeControllerAt is changeController under a timestamp the caller took
// -- several permanents changing controller in one event share it.
func (g *Game) changeControllerAt(controller PlayerController, id CardID, to PlayerID, ts uint64) {
	c := g.Card(id)
	before := c.Controller()
	c.tempControllers = append(c.tempControllers, ControlEffect{Timestamp: ts, Controller: to})
	if c.Zone == Battlefield {
		g.correctControllerZone(controller, id)
	} else if c.Controller() != before {
		// A spell on the stack (ControlSpell) has no battlefield list to
		// move between; it only takes the flags.
		c.SummonSick = true
		g.removeFromCombat(id)
		g.loseRingBearer(id)
	}
}

// correctControllerZone is GameAction.controllerChangeZoneCorrection
// (GameAction.java:994-1033, ADR-0037): a battlefield permanent whose
// Controller() is no longer the owner of the list it sits in moves to the end
// of its controller's battlefield list, so Zone(Battlefield, pid) is the
// permanents pid controls. The card keeps its Timestamp and zoneStamp (CR
// 400.7: the same object) and fires neither a ZoneChanged event nor a
// ChangesZone trigger -- Java suppresses the latter -- and a phased-out card
// stays phased out. The permanent leaves combat, is summoning sick under its
// new controller and stops being its old controller's Ring-bearer, for a
// Layer 2 change as for a one-shot one, then Mode$ ChangesController triggers
// fire (checkChangesControllerTriggers). Before the move, as
// GameAction.java:1008-1022 orders it, a paired creature is unpaired from its
// partner (CR 702.95e) and the permanent's change-controller commands run and
// clear (runChangeControllerCommands, controlcommands.go): LoseControl$
// LoseControl, Duration$ AsLongAsControl/UntilLoseControlOfHost. A command that
// ends another permanent's control change corrects that permanent's zone
// first, so its own ChangesController trigger fires before this one's.
func (g *Game) correctControllerZone(controller PlayerController, id CardID) {
	c := g.Card(id)
	if c.Zone != Battlefield {
		return
	}
	to := c.Controller()
	if to == c.ZoneOwner {
		return
	}
	original := c.ZoneOwner
	g.unpair(id)
	g.runChangeControllerCommands(controller, id)
	g.Zone(Battlefield, c.ZoneOwner).remove(id)
	c.ZoneOwner = to
	g.Zone(Battlefield, to).cards.Add(id)
	if c.phasedOut != NoPlayer {
		g.setPhasedOut(id, c.phasedOut)
	}
	c.SummonSick = true
	c.cameUnderControl = true
	g.removeFromCombat(id)
	g.loseRingBearer(id)
	g.checkChangesControllerTriggers(controller, id, original)
}

// correctControllerZones runs correctControllerZone over every battlefield
// phased-in permanent whose controller differs from its list's owner, in
// Java's walk order (each player's list in turn, p.getCardsIn(Battlefield)
// skipping phased-out cards: a permanent that phased out with its Aura is not
// re-homed while the Aura's static is off). It collects first and moves after, so
// no list is changed while it is being read. CheckStateBasedActions runs it
// right after applyContinuousControl.
func (g *Game) correctControllerZones(controller PlayerController) {
	var moved []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if g.Card(id).Controller() != pid {
				moved = append(moved, id)
			}
		}
	}
	for _, id := range moved {
		g.correctControllerZone(controller, id)
	}
}
