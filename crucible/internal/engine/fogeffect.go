package engine

//enginelint:allow card game ability condition control

// fogEffect is FogEffect.java: all combat damage is prevented for the rest
// of the turn. Java builds an effect card holding an "Event$ DamageDone |
// IsCombat$ True | Prevent$ True" replacement, exiled at end of turn; this
// port keeps the same effect as Game.combatDamagePrevented, read first by
// damagePrevented/damagePreventedPlayer (replacement.go) and cleared at
// cleanup. 15 real lines. A Condition$ value subAbilityConditionMet does not
// read is refused (rejectUnreadCondition).
type fogEffect struct{}

func (fogEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, "Fog"); err != nil {
		return err
	}
	if !subAbilityConditionMet(g, g.Card(a.Source), a.Amounts, a.Params) {
		return nil
	}
	g.combatDamagePrevented = true
	return nil
}
