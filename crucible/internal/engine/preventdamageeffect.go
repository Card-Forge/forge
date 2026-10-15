package engine

//enginelint:allow control ability game effecthelpers card condition amount id defined zone

import "fmt"

// preventDamageEffect is DamagePreventEffect.java: each targeted (or
// Defined$) player, and each targeted card still on the battlefield, gets a
// shield preventing the next Amount$ damage that would be dealt to it this
// turn. Java builds a command-zone effect per target holding a DamageDone
// replacement with PreventionEffect$ NextN, which drops away at end of turn
// and, for a card, when it leaves the battlefield; this port keeps the same
// shield as a preventShield record (applyPreventShields). A
// DividedAsYouChoose$ target gets a shield of its own share, announced with
// the targets (DamagePreventEffect.java:105, perTargetAmounts):
// Remedy's "prevent the next 5 damage ... to any number of targets, divided
// as you choose". CardChoices$/PlayerChoices$ and PreventionSubAbility$ are
// not resolved.
type preventDamageEffect struct{}

func (preventDamageEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectParams(a, "PreventDamage", "Condition",
		"CardChoices", "PlayerChoices", "PreventionSubAbility", "ShieldEffectTarget"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	raw, ok := a.Params.Param("Amount")
	if !ok {
		return fmt.Errorf("engine: PreventDamage: Amount$ missing")
	}
	n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
	if !ok {
		return fmt.Errorf("engine: PreventDamage: Amount$ %q not resolvable", raw)
	}
	var targets []EntityID
	if hasParam(a, "ValidTgts") {
		targets = a.Targets
	} else {
		def, ok := a.Params.Param("Defined")
		if !ok {
			def = "Self"
		}
		var err error
		targets, err = definedEntities(g, a.Controller, source, def, a.refs())
		if err != nil {
			return fmt.Errorf("engine: PreventDamage: %w", err)
		}
	}
	amounts, err := a.perTargetAmounts("PreventDamage", targets, n)
	if err != nil {
		return err
	}
	for i, t := range targets {
		if id, isCard := t.AsCard(); isCard && g.Card(id).Zone != Battlefield {
			continue
		}
		g.preventShields = append(g.preventShields, preventShield{Target: t, Remaining: amounts[i]})
	}
	// Radiance$ (DamagePreventEffect.java:102, :115-119).
	for _, id := range g.radianceCards(a) {
		g.preventShields = append(g.preventShields, preventShield{Target: CardEntity(id), Remaining: n})
	}
	return nil
}

// preventShield is one "prevent the next N damage" shield: what is left of
// it, and what it protects.
type preventShield struct {
	Target    EntityID
	Remaining int
}

// applyPreventShields spends target's shields, oldest first, against amount
// damage, answering what is left. A spent shield is removed.
func (g *Game) applyPreventShields(target EntityID, amount int) int {
	kept := g.preventShields[:0]
	for _, s := range g.preventShields {
		if s.Target == target && amount > 0 {
			used := s.Remaining
			if used > amount {
				used = amount
			}
			amount -= used
			s.Remaining -= used
		}
		if s.Remaining > 0 {
			kept = append(kept, s)
		}
	}
	g.preventShields = kept
	return amount
}

// dropPreventShields removes every shield on card id (its effect's
// forget-on-moved trigger, when it leaves the battlefield).
func (g *Game) dropPreventShields(id CardID) {
	kept := g.preventShields[:0]
	for _, s := range g.preventShields {
		if s.Target != CardEntity(id) {
			kept = append(kept, s)
		}
	}
	g.preventShields = kept
}
