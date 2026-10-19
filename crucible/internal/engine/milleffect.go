// Mill: CR 701.13, M6's own seventeenth script-driven effect -- 507 real
// (AB|DB)$ Mill lines, 317 naming Defined$ and 130 naming ValidTgts$
// instead -- SpellAbilityEffect.getTargetPlayers(sa)'s own either/or
// contract, targetedOrDefinedPlayers (defined.go). 0 real lines name
// Destination$ at all -- CR 701.13a's own default (the graveyard) is the
// whole real corpus, `ZoneType.smartValueOf(sa.getParam("Destination"))`'s
// own null-defaults-to-Graveyard branch never actually taken.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/MillEffect.java's
// resolve and GameAction.mill/Player.mill for the actual zone change.

package engine

//enginelint:allow id card game player ability defined condition control zone parts amount

import "fmt"

// millUnresolvedParams names MillEffect's own params this port does not
// evaluate. Every one fails the whole line loudly (PORT-8/GO-7): Destination$
// (0 real lines -- the file doc comment above has the reason) is listed
// anyway, a defensive reject rather than silently assuming Graveyard for an
// unrecognized destination; Optional$ (23) -- CR 701.13b's own "would you
// like to mill" confirm, this port's own PlayerController has no hook for
// it, the identical gap Sacrifice's/Discard's own Optional$ already
// document; Planeswalker$ (14) is not here: CR 606.3's own loyalty-ability
// marker is a cost-side gate (ActivateAbility/ActivateManaAbility,
// activateability.go), never a restriction on how the effect it pays for
// resolves, every other M6 effect's own identical reasoning; Ultimate$ (3)
// -- a planeswalker-ultimate-specific flag, its own further mechanic;
// ReduceCost$/
// ModeCost$ (1/1) -- Charm's own per-mode cost linkage and a cost-reduction
// effect neither one this file has anywhere to route through; Condition$/
// ConditionDefined$ (0/5) -- condition.go's own subAbilityConditionMet
// would otherwise silently no-op a card naming either, dealDamageEffect's
// own identical reasoning. ShowMilledCards$ (1) is not here: a reveal-dialog
// flag this port has no UI to show or hide, changing nothing about the
// actual zone change, the identical "cosmetic, ignored" treatment
// SpellDescription$/TgtPrompt$ already get everywhere.
var millUnresolvedParams = [...]string{
	"Destination", "Optional", "Ultimate", "ReduceCost",
	"ModeCost", "Condition"}

// millEffect resolves Mode$/DB$/AB$ Mill. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type millEffect struct{}

func (millEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range millUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Mill: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	numCards := 1
	if v, ok := a.Params.Param("NumCards"); ok {
		n, ok := resolveNamedAmount(g, a.Amounts, source, v)
		if !ok {
			return fmt.Errorf("engine: Mill: NumCards$ %q is not resolvable", v)
		}
		numCards = n
	}
	if numCards <= 0 {
		return nil
	}

	millers, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Mill: %w", err)
	}

	if _, ok := a.Params.Param("ForgetOtherRemembered"); ok {
		source.Memory.ClearRemembered()
	}
	_, remember := a.Params.Param("RememberMilled")
	_, imprint := a.Params.Param("Imprint")

	var milled []CardID
	for _, pid := range millers {
		// Event$ Mill: "they mill twice that many cards instead".
		pidCards, replaced := g.playerAmountReplaced(controller, "Mill", "Number", pid, numCards)
		if replaced || pidCards <= 0 {
			continue
		}
		lib := g.Zone(Library, pid).Cards()
		n := pidCards
		if n > len(lib) {
			n = len(lib)
		}
		// topN copies before Move mutates the zone out from under lib's own
		// slice -- scryEffect's own identical defensive copy (scryeffect.go).
		topN := append([]CardID(nil), lib[:n]...)
		for _, id := range topN {
			g.moveToGraveyard(controller, id)
			milled = append(milled, id)
			if remember {
				source.Memory.Remember(CardEntity(id))
			}
			if imprint {
				source.Memory.Imprint(id)
			}
		}
	}
	g.checkLeftZoneTriggers(controller, milled, Library)
	g.checkChangesZoneAllTriggers(controller, milled, Library, Graveyard)
	return nil
}
