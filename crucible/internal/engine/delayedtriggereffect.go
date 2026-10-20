package engine

//enginelint:allow ability additional card condition control defined delayedtrigger effecthelpers game

import (
	"fmt"
	"strings"
)

// delayedTriggerEffect is DelayedTriggerEffect.java: registers the line
// itself as a delayed trigger (CR 603.7) whose Execute$ runs once, when it
// next triggers, controlled by this ability's controller. ThisTurn$ lets it
// lapse at the next turn; NextTurn$ and UpcomingTurn$ hold it until this
// turn's cleanup (the former then lapsing after that turn);
// DelayedTriggerDefinedPlayer$ holds it until that player's next turn.
// RememberObjects$ is what Defined$ DelayTriggerRemembered reads.
//
// Only Mode$ Phase (380 of 461 real lines) fires: the port's other trigger
// checks walk card scripts, not this registry, so any other Mode$ fails
// closed here rather than registering a trigger that never fires.
// RememberNumber$/RememberSVarAmount$ remember an Integer and Static$
// triggers resolve off the stack; both fail closed.
type delayedTriggerEffect struct{}

func (delayedTriggerEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "DelayedTrigger", "RememberNumber", "RememberSVarAmount", "Static",
		"ValidTgts"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	mode, _ := a.Params.Param("Mode")
	switch {
	case strings.EqualFold(mode, "Phase"):
	case strings.EqualFold(mode, "ChangesZone"):
		// A battlefield-leaving watch (Seraph, Krovikan Vampire): matched by
		// delayedWatchesLeaving, which the dies, exile, bounce and library
		// paths call (delayedLeftBattlefieldMatches).
		if o, _ := a.Params.Param("Origin"); o != "Battlefield" {
			return fmt.Errorf("engine: DelayedTrigger: Mode$ ChangesZone with Origin$ %q not resolvable yet", o)
		}
	case strings.EqualFold(mode, "ChangesController"):
		// IsPresent$ (Stolen Uniform) is read by delayedPresentMatches,
		// which knows Card.IsTriggerRemembered; its modifiers are not.
		if err := rejectParams(a, "DelayedTrigger", "PresentCompare", "PresentZone", "PresentPlayer",
			"PresentDefined", "IsPresent2"); err != nil {
			return err
		}
	default:
		return fmt.Errorf("engine: DelayedTrigger: Mode$ %q not resolvable yet", mode)
	}
	if len(additionalAbilities(a.Params, "Execute")) == 0 {
		return fmt.Errorf("engine: DelayedTrigger: no Execute$")
	}
	d := delayedTrigger{
		Trigger: a.Params, Host: a.Source, Controller: a.Controller, Amounts: a.Amounts,
		ThisTurn:       hasParam(a, "ThisTurn") || hasParam(a, "NextTurn"),
		HostTransforms: source.Transforms,
	}
	if raw, ok := a.Params.Param("RememberObjects"); ok {
		for _, def := range strings.Split(raw, " & ") {
			es, err := definedEntities(g, a.Controller, source, def, a.refs())
			if err != nil {
				return fmt.Errorf("engine: DelayedTrigger: %w", err)
			}
			d.Remembered = append(d.Remembered, es...)
		}
	}
	switch {
	case hasParam(a, "DelayedTriggerDefinedPlayer"):
		raw, _ := a.Params.Param("DelayedTriggerDefinedPlayer")
		ps, err := definedPlayers(g, a.Controller, a.Source, raw, a.refs())
		if err != nil {
			return fmt.Errorf("engine: DelayedTrigger: %w", err)
		}
		if len(ps) == 0 {
			return nil
		}
		d.ForPlayer = ps[0]
	case hasParam(a, "NextTurn") || hasParam(a, "UpcomingTurn"):
		d.AtCleanup = true
	}
	g.delayed = append(g.delayed, d)
	return nil
}
