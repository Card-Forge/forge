package engine

import (
	"fmt"
	"strings"
)

// libraryBottom is LibraryPosition$ -1: Java's moveToLibrary(c, -1), the
// library's own end, which Game.Move appends to.
const libraryBottom = -1

// moveByEffect is GameAction.moveTo as an effect resolution drives it: id
// goes to dest, and the zone-change consequences this port models fire. A
// permanent entering the battlefield goes to newController's side
// (NoPlayer keeps its current controller -- its owner, for any card not
// already on the battlefield), is tapped first when tapped is set (Java's
// setTapped before moveToPlay), then runs enterBattlefieldReplacements and the ETB
// triggers exactly as permanentEffect does. A library destination honours
// libPos: 0 is the top, libraryBottom the bottom. A card leaving the
// battlefield fires the dies/exiled/returned trigger matching its
// destination. The batch-level ChangesZoneAll check is the caller's.
//
// Returns the melded partner [Game.Move]/[Game.MoveToLibraryTop] unmelded
// along with id, NoCard if id was not a melded permanent -- a caller
// marking id with markExiledWith on a dest == Exile move checks this to
// mark the partner too (ChangeZoneEffect.java's own handleExiledWith(meld,
// sa) call alongside its primary one).
func (g *Game) moveByEffect(controller PlayerController, id CardID, dest ZoneType, libPos int, newController PlayerID, tapped bool) CardID {
	c := g.Card(id)
	origin := c.Zone
	var melded CardID
	switch dest {
	case Battlefield:
		if newController == NoPlayer {
			newController = c.Controller()
		}
		melded = g.Move(id, Battlefield, newController)
		c.controller = newController
		if tapped {
			c.Tapped = true
		}
		g.enterBattlefieldReplacements(controller, id, origin)
		g.checkETBTriggers(controller, id, origin)
	case Library:
		if libPos == 0 {
			melded = g.MoveToLibraryTop(id, c.Owner)
		} else {
			melded = g.Move(id, Library, c.Owner)
		}
	default:
		melded = g.Move(id, dest, c.Owner)
	}
	if origin != Battlefield {
		return melded
	}
	switch dest {
	case Graveyard:
		g.checkDiesTriggers(controller, id)
	case Exile:
		g.checkExiledTriggers(controller, id)
	case Hand:
		g.checkReturnedTriggers(controller, id)
	}
	return melded
}

// orderCardsByTheirOwners is GameActionUtil.orderCardsByTheirOwners (CR
// 613.7m): cards moving together into dest are split per deciding player --
// the controller for the battlefield, the owner everywhere else -- and each
// player, in APNAP order, orders their own group of two or more. A single
// card needs no decision.
func (g *Game) orderCardsByTheirOwners(controller PlayerController, cards []CardID, dest ZoneType) ([]CardID, error) {
	if len(cards) <= 1 {
		return cards, nil
	}
	var out []CardID
	for _, pid := range g.playersInAPNAPOrder() {
		var group []CardID
		for _, id := range cards {
			c := g.Card(id)
			decider := c.Owner
			if dest == Battlefield {
				decider = c.Controller()
			}
			if decider == pid {
				group = append(group, id)
			}
		}
		if len(group) > 1 {
			ordered := controller.OrderCardsForZone(g, pid, group, dest)
			if err := checkChoice(ordered, group, len(group), len(group)); err != nil {
				return nil, fmt.Errorf("ordering cards into %v: %w", dest, err)
			}
			group = ordered
		}
		out = append(out, group...)
	}
	return out, nil
}

// parseZoneList reads a comma-separated zone list (Origin$, ChoiceZone$).
func parseZoneList(raw string) ([]ZoneType, error) {
	var zones []ZoneType
	for _, name := range strings.Split(raw, ",") {
		z, ok := ZoneByName(strings.TrimSpace(name))
		if !ok {
			return nil, fmt.Errorf("zone %q not resolvable yet", name)
		}
		zones = append(zones, z)
	}
	return zones, nil
}

// zoneIn reports whether z is one of zones.
func zoneIn(z ZoneType, zones []ZoneType) bool {
	for _, x := range zones {
		if x == z {
			return true
		}
	}
	return false
}

// libraryPosition reads LibraryPosition$ (default 0, the top). Only the top
// and the bottom (-1) are resolvable: Game has no insert-at-depth move yet.
func libraryPosition(g *Game, a *Ability, host *Card) (int, error) {
	raw, ok := a.Params.Param("LibraryPosition")
	if !ok {
		return 0, nil
	}
	n, ok := resolveNamedAmount(g, a.Amounts, host, raw)
	if !ok || (n != 0 && n != libraryBottom) {
		return 0, fmt.Errorf("LibraryPosition$ %q not resolvable yet", raw)
	}
	return n, nil
}

// markExiledWith is SpellAbilityEffect.handleExiledWith
// (SpellAbilityEffect.java:1087-1116) for an ability of host that has just
// moved id: a nontoken card that ended up in exile is marked as exiled with
// host's object (hostObjectStamp, game.go), read back by the
// ExiledWithSource valid property (valid.go). A card a replacement sent
// elsewhere is not marked. Called only by the effects Java calls it from:
// ChangeZone, ChangeZoneAll, Dig, DigUntil, Heist and Airbend (ADR-0034).
func (g *Game) markExiledWith(id, host CardID) {
	c := g.Card(id)
	if c.Zone != Exile || c.IsToken || host == NoCard {
		return
	}
	stamp, listed := g.hostObjectStamp(host)
	c.exiledWith = exiledWithMark{host: host, stamp: stamp, listed: listed}
}
