package engine

//enginelint:allow ability animate card condition control controlcommands defined effecthelpers game id parts zone ateot

import "fmt"

// animateEffect is AnimateEffect.java: each targeted or Defined$ card
// (default Self) on the battlefield takes buildAnimate's characteristics
// until end of turn, or for good with Duration$ Permanent; with Triggers$
// and Duration$ Perpetual each one, in any zone, perpetually gains those
// triggers instead (grantPerpetualTriggers).
// RememberAnimated$ remembers each one on the host.
type animateEffect struct{}

func (animateEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, "Animate"); err != nil {
		return err
	}
	template, grants, err := buildAnimate(g, a, "Animate")
	if err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if d, ok := a.Params.Param("Duration"); ok && !g.checkValidDuration(a, d) {
		// AnimateEffect.java:34.
		return nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Animate: %w", err)
	}
	if hasParam(a, "Optional") && !controller.ConfirmEffect(g, a.Controller, a.Source) {
		// AnimateEffect.java:155-163: the activator may decline the lot.
		return nil
	}
	if hasParam(a, "RememberAnimated") {
		for _, id := range cards {
			if g.Card(id).Zone == Battlefield {
				source.Memory.Remember(CardEntity(id))
			}
		}
	}
	if grants != nil {
		g.grantPerpetualTriggers(cards, grants, a.Amounts)
	}
	if !template.empty() {
		if err := g.animateCards(a, template, cards); err != nil {
			return err
		}
	}
	// AnimateEffect.java:194-201: each animated card remembers RememberObjects$'
	// objects and imprints ImprintCards$' cards, read with the host as source.
	remember, hasRemember := a.Params.Param("RememberObjects")
	imprint, hasImprint := a.Params.Param("ImprintCards")
	if hasRemember || hasImprint {
		var objects []EntityID
		var imprinted []CardID
		if hasRemember {
			if objects, err = definedEntities(g, a.Controller, source, remember, a.refs()); err != nil {
				return fmt.Errorf("engine: Animate: RememberObjects$: %w", err)
			}
		}
		if hasImprint {
			if imprinted, err = definedCards(source, imprint, a.refs()); err != nil {
				return fmt.Errorf("engine: Animate: ImprintCards$: %w", err)
			}
		}
		for _, id := range cards {
			if c := g.Card(id); c.Zone == None || c.IsPhasedOut() {
				continue
			}
			for _, e := range objects {
				g.Card(id).Memory.Remember(e)
			}
			for _, im := range imprinted {
				g.Card(id).Memory.Imprint(im)
			}
		}
	}
	return registerAtEOT(g, a, cards)
}
