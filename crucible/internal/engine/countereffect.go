package engine

//enginelint:allow game ability control effecthelpers card condition parts zone id

import "fmt"

// counterEffect is CounterEffect.java: each targeted spell (TargetType$
// Spell, a card on the stack) is countered -- unless it can't be -- and
// removed from the stack, its card going to Destination$: Graveyard (the
// default), Exile, Hand, TopOfLibrary or BottomOfLibrary. RememberCountered$
// and RememberForCounter$ remember the card on the host (the latter even if
// it could not be countered). Optional$ lets the activator stop first.
// Countering an ability, Defined$ spells, a Counter replacement effect and
// a CantBeCountered static are not resolved.
type counterEffect struct{}

func (counterEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "Counter", "Condition", "Defined", "DestinationChoice", "RememberCounteredCMC",
		"RememberCounteredSA", "RememberSplicedOntoCounteredSpell", "ConditionWouldDestroy", "DestroyPermanent",
		"TargetValidTargeting"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if tt, _ := a.Params.Param("TargetType"); tt != "Spell" {
		return fmt.Errorf("engine: Counter: TargetType$ %q not resolvable yet", tt)
	}
	if battlefieldReplacementEvent(g, "Counter") || battlefieldStaticMode(g, "CantBeCountered") {
		return fmt.Errorf("engine: Counter: Counter replacements or CantBeCountered statics not resolvable yet")
	}
	dest, ok := a.Params.Param("Destination")
	if !ok {
		dest = "Graveyard"
	}
	targets := a.Targets
	if a.wardCounters != NoEntity {
		// checkWardTriggers' own Defined$ TriggeredSourceSA stand-in
		// (ability.go's wardCounters doc comment, ADR-0028): the spell
		// this Ward-built Counter counters, kept off Targets so it never
		// ran through BecomesTarget or the CR 608.2b fizzle check.
		targets = append(append([]EntityID(nil), targets...), a.wardCounters)
	}
	for _, t := range targets {
		id, isCard := t.AsCard()
		if !isCard {
			continue
		}
		if hasParam(a, "Optional") && !controller.ConfirmEffect(g, a.Controller, a.Source) {
			return nil
		}
		if hasParam(a, "RememberForCounter") {
			source.Memory.Remember(t)
		}
		c := g.Card(id)
		if c.Zone != Stack || c.HasKeyword("This spell can't be countered.") {
			continue
		}
		kept := g.stack[:0]
		for _, s := range g.stack {
			if s.Source != id {
				kept = append(kept, s)
			}
		}
		g.stack = kept
		switch dest {
		case "Graveyard":
			g.moveByEffect(controller, id, Graveyard, 0, NoPlayer, false)
		case "Exile":
			g.moveByEffect(controller, id, Exile, 0, NoPlayer, false)
		case "Hand":
			g.moveByEffect(controller, id, Hand, 0, NoPlayer, false)
		case "TopOfLibrary":
			g.moveByEffect(controller, id, Library, 0, NoPlayer, false)
		case "BottomOfLibrary":
			g.moveByEffect(controller, id, Library, -1, NoPlayer, false)
		default:
			return fmt.Errorf("engine: Counter: Destination$ %q not resolvable", dest)
		}
		if hasParam(a, "RememberCountered") {
			source.Memory.Remember(t)
		}
	}
	return nil
}
