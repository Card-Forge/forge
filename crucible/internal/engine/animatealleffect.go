package engine

//enginelint:allow defined ability animate card condition control effecthelpers game id zone zonemove

import "fmt"

// animateAllEffect is AnimateAllEffect.java: every card of Zone$ (the
// battlefield by default) matching ValidCards$ takes buildAnimate's
// characteristics; ValidTgts$/Defined$ scope it to those players' cards.
type animateAllEffect struct{}

func (animateAllEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectParams(a, "AnimateAll", "RememberAnimated"); err != nil {
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
	// AnimateAllEffect.java:136-140: a targeting or Defined$ line reads the cards
	// of the targeted or defined players only.
	players := g.Players()
	if _, targeting := a.Params.Param("ValidTgts"); targeting {
		players = nil
		for _, e := range a.refs().targets {
			if pid, ok := e.AsPlayer(); ok {
				players = append(players, pid)
			}
		}
	} else if defined, ok := a.Params.Param("Defined"); ok {
		var err error
		if players, err = definedPlayers(g, a.Controller, a.Source, defined, a.refs()); err != nil {
			return fmt.Errorf("engine: AnimateAll: %w", err)
		}
	}
	var cards []CardID
	for _, z := range zones {
		for _, p := range players {
			cards = append(cards, g.Zone(z, p).Cards()...)
		}
	}
	if spec != "" {
		cards = filterValid(g, cards, spec, a.Controller, a.Source)
	}
	if grants != nil {
		g.grantPerpetualTriggers(cards, grants, a.Amounts)
	}
	if !template.empty() {
		if err := g.animateCards(a, template, cards); err != nil {
			return err
		}
	}
	if err := g.leaveBattlefieldFor(a, "AnimateAll", cards); err != nil {
		return err
	}
	return g.incorporateInto(a, "AnimateAll", cards)
}
