package engine

import (
	"fmt"
	"strings"
)

// libraryBottom is LibraryPosition$ -1: Java's moveToLibrary(c, -1), the
// library's own end, which Game.Move appends to.
const libraryBottom = -1

// enterCounters is a counter kind and count a permanent enters the battlefield
// with (ChangeZone's WithCountersType$/WithCountersAmount$).
type enterCounters struct {
	kind CounterType
	n    int
	// placer is the counter table's player key: ChangeZone's activator
	// (ChangeZoneEffect.java:617), a token's creator, MakeCard's player.
	// NoPlayer reads as the player the permanent enters under.
	placer PlayerID
}

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
//
// A host whose leaving runs Duration$ UntilHostLeavesPlay returns gets them
// carried out before this returns (returnUntilCards, controlcommands.go), so
// the rest of the effect sees those cards back, as Java's synchronous
// leaves-play command does.
func (g *Game) moveByEffect(controller PlayerController, id CardID, dest ZoneType, libPos int, newController PlayerID, tapped bool, enter ...enterCounters) CardID {
	melded := g.moveByEffectOnly(controller, id, dest, libPos, newController, tapped, enter...)
	g.returnUntilCards(controller)
	return melded
}

// moveByEffectOnly is moveByEffect without the until-host-leaves returns.
func (g *Game) moveByEffectOnly(controller PlayerController, id CardID, dest ZoneType, libPos int, newController PlayerID, tapped bool, enter ...enterCounters) CardID {
	c := g.Card(id)
	origin := c.Zone
	var melded CardID
	// A Moved replacement for leaving the battlefield (LeaveBattlefield$'s
	// effect card, Realmbreaker's "exile it instead") redirects any destination
	// but the graveyard, whose moves moveToGraveyard reads it for.
	var redirected *movedPost
	if origin == Battlefield && dest != Battlefield && dest != Graveyard {
		dest, redirected = g.movedDestination(id, dest)
	}
	switch dest {
	case Battlefield:
		if newController == NoPlayer {
			newController = c.Controller()
		}
		if g.entryReplaced(controller, id, origin, newController) {
			return NoCard
		}
		melded = g.Move(id, Battlefield, newController)
		c.controller = newController
		if tapped {
			c.Tapped = true
		}
		// WithCountersType$: the permanent enters with the counters, before its
		// own enter replacements and triggers see it. They join the entry's
		// counter table (Card.pendingEnter), which applyEnterCounters runs the
		// AddCounter replacements over once and places through
		// addCardCounters, CounterAdded included: Java passes them in the same
		// AbilityKey.CounterTable as the ETB$ counters (ChangeZoneEffect.java:
		// 614-620, TokenEffectBase.java:140-145, MakeCardEffect.java:179-183).
		for _, e := range enter {
			placer := e.placer
			if placer == NoPlayer {
				placer = newController
			}
			c.pendingEnter = append(append([]enterPile(nil), c.pendingEnter...), enterPile{ct: e.kind, n: e.n, source: id, placer: placer})
		}
		g.enterBattlefieldReplacements(controller, id, origin)
		g.checkETBTriggers(controller, id, origin)
	case Library:
		if libPos == 0 {
			melded = g.MoveToLibraryTop(id, c.Owner)
		} else {
			melded = g.Move(id, Library, c.Owner)
			if libPos > 0 {
				g.sinkInLibrary(id, c.Owner, libPos)
			}
		}
	case Graveyard:
		dest, melded = g.moveToGraveyard(controller, id)
	default:
		melded = g.Move(id, dest, c.Owner)
	}
	g.finishMoved(controller, id, c.Owner, redirected, melded)
	if origin == Graveyard && dest != Battlefield {
		// Where the card ended up, a Moved replacement included; the
		// graveyard itself fires nothing here.
		g.checkLeftGraveyardTriggers(controller, id, g.Card(id).Zone)
	}
	if origin == Hand || origin == Library {
		g.checkLeftZoneTriggers(controller, []CardID{id}, origin)
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
	case Library:
		g.checkLeftToTriggers(controller, id, Library)
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
	if raw == "All" {
		// ZoneType.listValueOf("All"), ZoneType.java:77-79.
		return []ZoneType{Battlefield, Hand, Graveyard, Exile, Stack, Library, Command}, nil
	}
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

// sinkInLibrary moves id, just put on the bottom of owner's library, up to
// index pos (counted from the top, clamped to the library size): Java's
// zone.add(card, index) for LibraryPosition$ N.
func (g *Game) sinkInLibrary(id CardID, owner PlayerID, pos int) {
	cards := g.Zone(Library, owner).cards
	i := cards.Len() - 1
	if i < 0 || cards.All()[i] != id {
		return
	}
	for ; i > pos; i-- {
		cards.Swap(i, i-1)
	}
}

// libraryPosition reads LibraryPosition$ (default 0, the top; -1 the bottom;
// N the Nth card down, ChangeZone and ChangeZoneAll only).
func libraryPosition(g *Game, a *Ability, host *Card) (int, error) {
	raw, ok := a.Params.Param("LibraryPosition")
	if !ok {
		return 0, nil
	}
	n, ok := resolveNamedAmount(g, a.Amounts, host, raw)
	if !ok || n < libraryBottom {
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
