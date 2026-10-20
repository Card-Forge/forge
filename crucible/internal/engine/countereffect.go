package engine

//enginelint:allow game ability control effecthelpers card condition parts zone id destroyeffect

import "fmt"

// counterEffect is CounterEffect.java: each targeted spell (a card on the
// stack) is countered -- unless it can't be -- and removed from the stack, its
// card going to Destination$: Graveyard (the default), Exile, Hand,
// TopOfLibrary or BottomOfLibrary. Each targeted activated or triggered
// ability (StackItemEntity, ADR-0041) is removed from the stack and nothing
// moves (CounterEffect.java:270-272); DestroyPermanent$ then destroys its host
// (:105-107). RememberCountered$ and RememberForCounter$ remember the card --
// an ability's host -- on the host of this ability (the latter even if it
// could not be countered). Optional$ lets the activator stop first. A Layer$
// CantHappen Counter replacement (counterCantHappen, replacementevents.go)
// stops the counter, and a ReplaceWith$ one replaces a spell's (counterReplaced).
// TargetValidTargeting$ is a targeting restriction (canTargetStackItem), read
// as targets are chosen and again at resolution. Defined$ spells, any other
// Counter replacement and a CantBeCountered static are not resolved.
type counterEffect struct{}

func (counterEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Counter", "Defined", "DestinationChoice", "RememberCounteredCMC",
		"RememberCounteredSA", "RememberSplicedOntoCounteredSpell", "ConditionWouldDestroy"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	// A targeted Counter resolves under any TargetType$: targeting offers a
	// spell as its card and an ability as its stack item
	// (stackAbilityCandidates), each countered its own way below. An
	// untargeted one must name Spell: it has no other way to its spell.
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
		// Ward on an ability: that stack item, countered as any targeted
		// ability is (counterAbility).
		return counterAbility(g, a, controller, StackItemEntity(a.wardItem))
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
		if _, isItem := t.AsStackItem(); isItem {
			if hasParam(a, "Optional") && !controller.ConfirmEffect(g, a.Controller, a.Source) {
				return nil
			}
			if err := counterAbility(g, a, controller, t); err != nil {
				return err
			}
			continue
		}
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
		cant, err := g.counterCantHappen(id, causeSpell)
		if err != nil {
			return err
		}
		if cant {
			continue
		}
		// Event$ Counter with a ReplaceWith$ ability (Guile): the spell is
		// not countered, the ability acts on it instead.
		if g.counterReplaced(controller, a, id, causeSpell) {
			continue
		}
		// CounterEffect.removeFromStack's game.getStack().remove(si)
		// (CounterEffect.java:258): the spell's own item leaves the stack, and
		// nothing else -- a cast trigger whose source is the spell's card stays
		// and still resolves (MagicStack.remove(Card), MagicStack.java:799-805,
		// keeps abilities the same way).
		g.removeSpellFromStack(id)
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

// counterAbility is CounterEffect.resolve's loop body (CounterEffect.java:
// 72-114) for t, an activated or triggered ability on the stack: RememberForCounter$
// remembers its host card whether or not it is still there; an ability that
// already left the stack is skipped (getInstanceMatchingSpellAbilityID's null,
// :87-90); otherwise a Counter replacement that stops it (counterCantHappen
// with its host as Affected, :252-257) leaves it, and it is removed from the
// stack by its own stack item alone -- a cast trigger whose source is a spell's
// card stays with the spell -- moving no card (:270-272). DestroyPermanent$
// then destroys its host (:105-107) and RememberCountered$ remembers the host
// (:109-110). An ability is always counterable: isCounterableBy is asked of
// a spell only (:83).
func counterAbility(g *Game, a *Ability, controller PlayerController, t EntityID) error {
	ref, known := g.stackTargetOf(t, a.stackTargets)
	if hasParam(a, "RememberForCounter") {
		if !known {
			return fmt.Errorf("engine: Counter: RememberForCounter$ for an ability no longer on the stack not resolvable yet")
		}
		g.Card(a.Source).Memory.Remember(CardEntity(ref.host))
	}
	id, _ := t.AsStackItem()
	item, onStack := g.stackItem(id)
	if !onStack {
		return nil
	}
	host, kind := item.Source, item.causeKind()
	cant, err := g.counterCantHappen(host, kind)
	if err != nil {
		return err
	}
	if cant || g.counterReplaced(controller, a, host, kind) {
		return nil
	}
	g.removeStackItem(id)
	if hasParam(a, "DestroyPermanent") {
		// GameAction.destroy with regeneration allowed: the steps destroyEffect
		// runs per card (destroyeffect.go).
		if c := g.Card(host); c.Zone == Battlefield && canBeDestroyed(c) &&
			!g.destroyInstead(controller, host) && !g.regenerate(controller, host) {
			g.moveToGraveyard(controller, host)
			g.checkDiesTriggers(controller, host)
			g.checkChangesZoneAllTriggers(controller, []CardID{host}, Battlefield, Graveyard)
		}
	}
	if hasParam(a, "RememberCountered") {
		g.Card(a.Source).Memory.Remember(CardEntity(host))
	}
	return nil
}

// removeStackItem takes the item whose ID is id off the stack, and nothing
// else: MagicStack.remove(SpellAbilityStackInstance).
func (g *Game) removeStackItem(id StackItemID) {
	kept := g.stack[:0]
	for _, s := range g.stack {
		if s.ID != id {
			kept = append(kept, s)
		}
	}
	clear(g.stack[len(kept):])
	g.stack = kept
}
