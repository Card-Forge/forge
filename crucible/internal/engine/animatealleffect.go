package engine

//enginelint:allow ability animate card condition control effecthelpers game id zone zonemove

import "fmt"

// animateAllEffect is AnimateAllEffect.java: every card of Zone$ (the
// battlefield by default) matching ValidCards$ takes buildAnimate's
// characteristics. The player-scoped ValidTgts$/Defined$ form fails closed.
type animateAllEffect struct{}

func (animateAllEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectParams(a, "AnimateAll", "ValidTgts", "Defined", "RememberAnimated"); err != nil {
		return err
	}
	if err := rejectUnreadCondition(a, "AnimateAll"); err != nil {
		return err
	}
	template, grants, err := buildAnimate(g, a, "AnimateAll")
	if err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	spec, _ := a.Params.Param("ValidCards")
	// Zone$ is a comma list (Battlefield by default), game.getCardsIn(z): each
	// zone across every player in seat order, in the order the list names them.
	zones := []ZoneType{Battlefield}
	if raw, ok := a.Params.Param("Zone"); ok {
		var err error
		if zones, err = parseZoneList(raw); err != nil {
			return fmt.Errorf("engine: AnimateAll: Zone$: %w", err)
		}
	}
	var cards []CardID
	for _, z := range zones {
		for _, p := range g.Players() {
			cards = append(cards, g.Zone(z, p).Cards()...)
		}
	}
	if spec != "" {
		cards = filterValid(g, cards, spec, a.Controller, a.Source)
	}
	if grants != nil {
		g.grantPerpetualTriggers(cards, grants, a.Amounts)
	}
	if template.empty() {
		return nil
	}
	return g.animateCards(a, template, cards)
}
