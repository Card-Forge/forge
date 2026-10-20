package engine

//enginelint:allow game ability control effecthelpers card condition defined valid

import "fmt"

// becomesBlockedEffect is BecomesBlockedEffect.java: outside combat nothing
// happens. Inside, each Defined$ or targeted attacker becomes blocked
// (Combat.setBlocked) even though no creature blocks it (CR 509.1h). An
// attacker that was not already blocked this combat fires its "becomes
// blocked" triggers with no blockers.
type becomesBlockedEffect struct{}

func (becomesBlockedEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "BecomesBlocked"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if !g.activePhase.IsCombat() {
		return nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: BecomesBlocked: %w", err)
	}
	for _, id := range cards {
		if !g.combat.isAttacking(id) {
			continue
		}
		wasBlocked := g.combat.isBlocked(id)
		if !containsCard(g.combat.ForcedBlocked, id) {
			g.combat.ForcedBlocked = append(g.combat.ForcedBlocked, id)
		}
		if !wasBlocked {
			g.checkAttackerBlockedTriggers(controller, id, nil)
		}
	}
	return nil
}
