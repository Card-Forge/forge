// Abandon: an Archenemy ongoing scheme leaves the Command zone for the
// SchemeDeck (CR 904.9's own "if a scheme is abandoned"). 21 of the
// corpus's real DB$/AB$ Abandon lines, all on Ongoing Scheme cards; the
// dominant shape is a bare `DB$ Abandon`, always against its own host --
// AbandonEffect.java never reads a Defined$/ValidTgts$ param, so this port
// takes no target either.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/AbandonEffect.java's
// resolve. Optional$ asks controller.ConfirmEffect first (Java's
// confirmAction); RememberAbandoned$ remembers the host on itself before
// the move, the same as every other RememberX$ this port already has
// (changeZoneMemory, changezoneeffect.go). Java also does
// clearActiveTriggers/registerActiveTrigger around the move -- a cache
// invalidation this port needs nothing for, since checkAbandonedTriggers
// (trigger.go) reads each host's own current Zone through traitHosts
// rather than a cached registration.

package engine

//enginelint:allow card game ability condition control zone parts id

// abandonEffect resolves Mode$/DB$/AB$ Abandon. SubAbility$ chains through
// resolveSubAbility (subability.go) once this effect's own body finishes;
// UnlessCost$/UnlessPayer$/UnlessSwitched$/ConditionCheckSVar$/
// ConditionPresent$/ConditionCompare$ never reach here at all --
// resolveUnlessCost and subAbilityConditionMet (effect.go, condition.go)
// both gate the whole ability before Registry.Resolve does; a non-pure-mana
// UnlessCost$ (PayLife<3>/Discard<1/Card>/Sac<2/Creature>, this port's own 3
// real such Abandon lines) already fails loudly there too
// (resolveUnlessCost's own "not resolvable yet").
type abandonEffect struct{}

func (abandonEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if _, optional := a.Params.Param("Optional"); optional {
		if !controller.ConfirmEffect(g, a.Controller, a.Source) {
			return nil
		}
	}
	// Crucible addition, not Java parity: AbandonEffect.java removes/adds to
	// the two zones unconditionally, trusting that Abandon is only ever
	// resolved against a real Command-zone scheme. This guard makes that
	// same assumption an explicit no-op instead of a wrong move (GO-7) for
	// a host a SubAbility$ chain reached after something else already moved
	// it (TestAbandonEffectHostNotInCommandIsNoOp).
	if source.Zone != Command {
		return nil
	}
	if _, remember := a.Params.Param("RememberAbandoned"); remember {
		source.Memory.Remember(CardEntity(a.Source))
	}
	g.Move(a.Source, SchemeDeck, source.Owner)
	g.checkAbandonedTriggers(controller, a.Source)
	return nil
}
