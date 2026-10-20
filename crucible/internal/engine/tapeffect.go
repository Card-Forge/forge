// Tap: CR 701.21, M6's own next script-driven effect after Destroy -- 577
// of the corpus's real (AB|DB)$ Tap lines once the corpus's own dominant
// shape (818 of 1,395, naming ETB$) is set aside: that shape is CR 614's own
// "enters the battlefield tapped" replacement effect, already resolved
// end to end by replacement.go's own tapAbilityResolvesTap/
// replacementTapsOnMove, never reaching Registry.Resolve or this file at
// all (a ReplaceWith$ target's own sub-ability runs by hand, the identical
// "not back through Resolve itself" reasoning drawReplaced's own doc
// comment gives, replacement.go). Of the 577 remaining, 413 name ValidTgts$
// and 151 name Defined$ instead -- SpellAbilityEffect.getTargetCards(sa)'s
// own either/or contract, targetedOrDefinedCards (defined.go).
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/TapEffect.java's
// resolve. Not ported: TriggerType.TapAll (2 real corpus T: lines, not
// worth building -- taptype.go's own identical reasoning for the same
// trigger, reached from a cost-paid tap rather than a script-driven one
// there).

package engine

//enginelint:allow id card game ability defined condition control zone parts amount effecthelpers resolvechoice

import "fmt"

// tapUnresolvedParams names TapEffect's own params this port does not
// evaluate. Every one fails the whole line loudly (PORT-8/GO-7):
// Tapper$
// (2) -- a tap attributed to a player other than the activator, CR 701.21's
// own "tapped by" distinction nothing downstream reads yet; Condition$/
// ConditionDefined$ (0/7) -- condition.go's own subAbilityConditionMet
// would otherwise silently no-op a card naming either, dealDamageEffect's
// own identical reasoning; ETB$ (0 reaching here -- the file doc comment
// above has the reason) is listed anyway, a defensive reject in case a real
// line somehow combines it with a shape that reaches this path some other
// way, rather than silently tapping-without-a-trigger the wrong permanent.
var tapUnresolvedParams = [...]string{
	"Tapper", "ETB",
}

// tapEffect resolves Mode$/DB$/AB$ Tap. ConditionPresent$/ConditionCompare$/
// ConditionCheckSVar$/ConditionSVarCompare$ are resolved through
// subAbilityConditionMet (condition.go), the identical way every other M6
// effect's own does.
type tapEffect struct{}

func (tapEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range tapUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Tap: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	var cards []CardID
	if spec, ok := a.Params.Param("CardChoices"); ok {
		// TapEffect.java:37-47: the activator picks ChoiceAmount$ (1) battlefield
		// permanents matching CardChoices$, none required with AnyNumber$.
		var err error
		if cards, err = tapChoice(g, a, controller, spec); err != nil {
			return err
		}
	} else {
		var err error
		if cards, err = targetedOrDefinedCards(source, a.Params, a.refs()); err != nil {
			return fmt.Errorf("engine: Tap: %w", err)
		}
	}
	_, remTapped := a.Params.Param("RememberTapped")
	_, alwaysRem := a.Params.Param("AlwaysRemember")
	for _, id := range cards {
		c := g.Card(id)
		// A phased-out permanent is skipped (TapEffect.java:56).
		if c.Zone != Battlefield || c.IsPhasedOut() {
			continue
		}
		wasUntapped := !c.Tapped
		if remTapped && wasUntapped || alwaysRem {
			g.Card(a.Source).Memory.Remember(CardEntity(id))
		}
		if wasUntapped {
			c.Tapped = true
			g.checkTapsTriggers(controller, id, c.Controller(), false)
		}
	}
	return nil
}

// tapChoice is TapEffect's CardChoices$ pick (TapEffect.java:37-47).
func tapChoice(g *Game, a *Ability, controller PlayerController, spec string) ([]CardID, error) {
	n := 1
	if raw, ok := a.Params.Param("ChoiceAmount"); ok {
		var ok bool
		if n, ok = resolveNamedAmount(g, a.Amounts, g.Card(a.Source), raw); !ok {
			return nil, fmt.Errorf("engine: Tap: ChoiceAmount$ %q is not resolvable", raw)
		}
	}
	lo := n
	if _, anyNumber := a.Params.Param("AnyNumber"); anyNumber {
		lo = 0
	}
	offer, err := choiceCandidates(g, a, "Tap", []ZoneType{Battlefield}, spec)
	if err != nil {
		return nil, err
	}
	hi := min(n, len(offer))
	lo = max(min(lo, hi), 0)
	if hi <= 0 {
		return nil, nil
	}
	options := make([]EntityID, len(offer))
	for i, id := range offer {
		options[i] = CardEntity(id)
	}
	picked := controller.ChooseEntitiesForEffect(g, a.Controller, a.Source, options, lo, hi)
	if err := checkChoice(picked, options, lo, hi); err != nil {
		return nil, fmt.Errorf("engine: Tap: %w", err)
	}
	cards := make([]CardID, 0, len(picked))
	for _, e := range picked {
		if id, ok := e.AsCard(); ok {
			cards = append(cards, id)
		}
	}
	return cards, nil
}
