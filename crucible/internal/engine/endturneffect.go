package engine

//enginelint:allow control ability game effecthelpers card condition defined action phase zone id

import "fmt"

// endTurnEffect is EndTurnEffect.java and CR 723: with Optional$ the first
// Defined$ player (the activator by default) may decline. Otherwise every
// spell on the stack is exiled and every ability on it removed, combat
// ends, state-based actions are checked, and the turn skips to its cleanup
// step (PhaseHandler.endTurnByEffect: queued extra phases are dropped and
// cleanup begins at once). The next AdvancePhase moves on to the next turn.
// A Cleanup whose state-based actions did something or that put a trigger on
// the stack is repeated by Step (CR 514.3a, Game.endTurnCleanup).
type endTurnEffect struct{}

func (endTurnEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "EndTurn"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if hasParam(a, "Optional") {
		ender := a.Controller
		if def, ok := a.Params.Param("Defined"); ok {
			players, err := definedPlayers(g, a.Controller, a.Source, def, a.refs())
			if err != nil {
				return fmt.Errorf("engine: EndTurn: %w", err)
			}
			if len(players) > 0 {
				ender = players[0]
			}
		}
		if !controller.ConfirmEffect(g, ender, a.Source) {
			return nil
		}
	}
	g.exileStack(controller)
	g.endCombat()
	CheckStateBasedActions(g, controller)
	g.extraPhases = [numPhaseTypes][]PhaseType{}
	g.activePhase = Cleanup
	priority, err := g.beginStep(controller, false)
	if err != nil {
		return fmt.Errorf("engine: EndTurn: %w", err)
	}
	g.endTurnCleanup = priority
	return nil
}

// exileStack is EndTurn's and EndCombatPhase's shared first step: every
// spell on the stack is exiled (its card moves to exile, firing the
// exiled triggers) and the stack is emptied of abilities too.
func (g *Game) exileStack(controller PlayerController) {
	stack := g.stack
	g.stack = nil
	for i := len(stack) - 1; i >= 0; i-- {
		id := stack[i].Source
		if g.Card(id).Zone == Stack {
			g.moveByEffect(controller, id, Exile, 0, NoPlayer, false)
		}
	}
}
