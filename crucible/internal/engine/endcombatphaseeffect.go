package engine

//enginelint:allow game ability control effecthelpers card condition action phase

// endCombatPhaseEffect is EndCombatPhaseEffect.java: outside combat it does
// nothing. Inside, every spell on the stack is exiled and every ability
// removed, combat ends, state-based actions are checked, and the game moves
// straight past the end-of-combat step (PhaseHandler.
// endCombatPhaseByEffect sets COMBAT_END, then advances).
type endCombatPhaseEffect struct{}

func (endCombatPhaseEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "EndCombatPhase"); err != nil {
		return err
	}
	if !subAbilityConditionMet(g, g.Card(a.Source), a.Amounts, a.Params) {
		return nil
	}
	if !g.activePhase.IsCombat() {
		return nil
	}
	g.exileStack(controller)
	g.endCombat()
	CheckStateBasedActions(g, controller)
	g.activePhase = CombatEnd
	g.AdvancePhase(controller)
	return nil
}
