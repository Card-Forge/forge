package engine

//enginelint:allow game ability control effecthelpers card condition parts zone id

import "fmt"

// counterEffect is CounterEffect.java: each targeted spell (TargetType$
// Spell, a card on the stack) is countered -- unless it can't be -- and
// removed from the stack, its card going to Destination$: Graveyard (the
// default), Exile, Hand, TopOfLibrary or BottomOfLibrary. RememberCountered$
// and RememberForCounter$ remember the card on the host (the latter even if
// it could not be countered). Optional$ lets the activator stop first.
// A Layer$ CantHappen Counter replacement (counterCantHappen,
// replacementevents.go) stops the counter, and a ReplaceWith$ one replaces it
// (counterReplaced). Countering an ability, Defined$
// spells, any other Counter replacement and a CantBeCountered static are
// not resolved.
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
	// A targeted Counter resolves under any TargetType$: targeting offers a
	// spell as its card and fails the ability before it gets here when an
	// activated or triggered ability would be a legal target
	// (stackAbilityCandidates), so every target is a spell -- countered the
	// same way under "Spell" or "Spell,Activated". An untargeted one must
	// name Spell: it has no other way to its spell.
	if tt, ok := a.Params.Param("TargetType"); !ok || tt != "Spell" && !hasParam(a, "ValidTgts") {
		return fmt.Errorf("engine: Counter: TargetType$ %q not resolvable yet", tt)
	}
	if battlefieldStaticMode(g, "CantBeCountered") {
		return fmt.Errorf("engine: Counter: CantBeCountered statics not resolvable yet")
	}
	dest, ok := a.Params.Param("Destination")
	if !ok {
		dest = "Graveyard"
	}
	if a.wardItem != 0 {
		// Ward on an ability: remove that stack item and nothing else. An
		// ability is not a card to move (CounterEffect.removeFromStack:
		// 270-272), and a host with several stack items loses only this one.
		kept := g.stack[:0]
		for _, s := range g.stack {
			if s.ID != a.wardItem {
				kept = append(kept, s)
			}
		}
		g.stack = kept
		return nil
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
		// CounterEffect.removeFromStack's cantHappenCheck over Event$ Counter.
		cant, err := g.counterCantHappen(id)
		if err != nil {
			return err
		}
		if cant {
			continue
		}
		// Event$ Counter with a ReplaceWith$ ability (Guile): the spell is
		// not countered, the ability acts on it instead.
		if g.counterReplaced(controller, a, id) {
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
