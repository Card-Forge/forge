package engine

//enginelint:allow ability card condition control defined effecthelpers game id zone zonemove

import "fmt"

// exchangeZoneEffect is ZoneExchangeEffect.java: the Object$ card (default
// the host) in Zone1$ (default Battlefield), owned by the activator, trades
// places with a card of theirs in Zone2$ (default Hand) matching
// ValidExchange$ (default Card) that they choose -- optional unless
// Mandatory$. Object moves first, then the chosen card; ChangesZoneAll fires
// for each half.
//
// Type$ (both cards must share a type, with an Aura's attachment carried
// over, ZoneExchangeEffect.java:57-92) fails closed; no real line names it.
// The one real line (Darkpact, Zone1$ Ante, Object$ ParentTarget) is an ante
// sorcery this port cannot cast.
type exchangeZoneEffect struct{}

func (exchangeZoneEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "ExchangeZone", "Type"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	zone1, err := exchangeZoneParam(a, "Zone1", Battlefield)
	if err != nil {
		return err
	}
	zone2, err := exchangeZoneParam(a, "Zone2", Hand)
	if err != nil {
		return err
	}
	object1 := a.Source
	if raw, ok := a.Params.Param("Object"); ok {
		cards, err := definedCards(source, raw, a.refs())
		if err != nil {
			return fmt.Errorf("engine: ExchangeZone: %w", err)
		}
		if len(cards) == 0 {
			return nil
		}
		object1 = cards[0]
	}
	p := a.Controller
	if c := g.Card(object1); c.Zone != zone1 || c.Owner != p {
		return nil
	}
	filter, ok := a.Params.Param("ValidExchange")
	if !ok {
		filter = "Card"
	}
	var list []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(zone2, pid).Cards() {
			if zone2 == Battlefield && g.Card(id).Controller() != p || zone2 != Battlefield && pid != p {
				continue
			}
			list = append(list, id)
		}
	}
	list = filterValid(g, list, filter, p, a.Source)
	if len(list) == 0 {
		return nil
	}
	lo := 0
	if hasParam(a, "Mandatory") {
		lo = 1
	}
	picked := controller.ChooseCardsForEffect(g, p, a.Source, list, lo, 1)
	if err := checkChoice(picked, list, lo, 1); err != nil {
		return fmt.Errorf("engine: ExchangeZone: %w", err)
	}
	if len(picked) == 0 {
		return nil
	}
	object2 := picked[0]
	g.moveByEffect(controller, object1, zone2, 0, NoPlayer, false)
	g.moveByEffect(controller, object2, zone1, 0, NoPlayer, false)
	g.checkChangesZoneAllTriggers(controller, []CardID{object1}, zone1, g.Card(object1).Zone)
	g.checkChangesZoneAllTriggers(controller, []CardID{object2}, zone2, g.Card(object2).Zone)
	return nil
}

// exchangeZoneParam reads a zone param, def when absent.
func exchangeZoneParam(a *Ability, key string, def ZoneType) (ZoneType, error) {
	raw, ok := a.Params.Param(key)
	if !ok {
		return def, nil
	}
	z, ok := ZoneByName(raw)
	if !ok {
		return 0, fmt.Errorf("engine: ExchangeZone: %s$ %q not resolvable yet", key, raw)
	}
	return z, nil
}
