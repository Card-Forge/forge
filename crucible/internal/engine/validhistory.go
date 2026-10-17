// Valid properties that read the history of the turn: what a card blocked,
// was blocked by, dealt or was dealt this turn, when it entered its zone,
// where it was cast from and where it stands among the spells cast this turn
// (CardProperty.java). Each
// reads a record the engine already keeps on the card, its controller's
// player or the game; propertyMatches (valid.go) asks here before falling
// back to reading a property as a type.

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// turnHistoryMatches answers the turn-history properties. handled is false
// for any other name, which propertyMatches then reads on.
func turnHistoryMatches(g *Game, c *Card, name string, sourceController PlayerID, source CardID) (matched, handled bool) {
	switch {
	case strings.HasPrefix(name, "ThisTurnEnteredFrom"):
		// valid.go's own case, kept there.
		return false, false
	case strings.HasPrefix(name, "ThisTurnEntered"):
		m, known := thisTurnEnteredMatches(g, c, name, sourceController)
		return m && known, true
	case name == "EnteredSinceYourLastTurn":
		// CardProperty.java:951-954: the card's turn in its zone is later
		// than the source controller's last turn (Player.getLastTurnNr, 0
		// before the first). A card NewCard placed has no turn in its zone.
		return c.zoneEntered && c.zoneEntryTurn > g.Player(sourceController).lastTurnNr, true
	case strings.HasPrefix(name, "dealtDamageToYouThisTurn"):
		// CardProperty.java:1144-1147: the card's damage history holds damage
		// it dealt to the source controller this turn. The game's record of
		// player damage keeps the source as it stood; the same object is the
		// same CardID with the same zoneStamp, as Java's history is per object.
		for i := range g.playerDamageThisTurn {
			d := &g.playerDamageThisTurn[i]
			if d.target == sourceController && d.source.ID == c.ID && d.source.zoneStamp == c.zoneStamp {
				return true, true
			}
		}
		return false, true
	case strings.HasPrefix(name, "wasDealtDamageThisTurn"):
		// CardProperty.java:1156-1159: GameEntity.getAssignedDamage, the sum
		// of damageReceivedThisTurn (GameEntity.java:377-392). Every instance
		// dealt is one entry of Damage.Sources, cleared at cleanup and on
		// leaving the battlefield, kept by a regeneration (Damage.Heal).
		return len(c.Damage.Sources) > 0, true
	case strings.HasPrefix(name, "dealtDamageThisTurn"):
		// CardProperty.java:1177-1180: Card.getTotalDamageDoneBy
		// (Card.java:6104-6106), every damage the card dealt this turn after
		// prevention and replacement.
		return c.damageDealtThisTurn > 0, true
	case strings.HasPrefix(name, "blockedThisTurn"):
		// CardProperty.java:1199-1202 (the second branch at :1613-1614 is the
		// same test and never reached).
		return len(c.blockedThisTurn) > 0, true
	case strings.HasPrefix(name, "blockedByThisTurn"), strings.HasPrefix(name, "gotBlockedThisTurn"):
		// CardProperty.java:1207-1210 and :1615-1616: blocked by anything
		// this turn.
		return len(c.blockedByThisTurn) > 0, true
	case strings.HasPrefix(name, "blockedValidThisTurn "):
		return blockedValidMatches(g, c, c.blockedThisTurn, strings.TrimPrefix(name, "blockedValidThisTurn "), source), true
	case strings.HasPrefix(name, "blockedByValidThisTurn "):
		return blockedValidMatches(g, c, c.blockedByThisTurn, strings.TrimPrefix(name, "blockedByValidThisTurn "), source), true
	case strings.HasPrefix(name, "wasCastFrom"):
		m, known := castFromPropertyMatches(c, name, sourceController)
		return m && known, true
	case strings.HasPrefix(name, "SecondSpellCastThisTurn"):
		// CardProperty.java:924-931: the second spell cast this turn, by
		// anyone (CardUtil.getThisTurnCast("Card", ...)), is this very object.
		if len(g.castThisTurn) < 2 {
			return false, true
		}
		rec := g.castThisTurn[1]
		return rec.card == c.ID && rec.stamp == c.zoneStamp, true
	}
	return false, false
}

// castFromPropertyMatches is CardProperty.java:1929-1949,
// wasCastFrom[Your|Their]<Zone>[ByYou]: the card was cast (and is still that
// object) from <Zone>; Your is the source controller's zone, Their the card
// controller's; ByYou the source controller cast it. The zone a card is cast
// from is its owner's (hand, graveyard, library, exile, command), so the
// zone's player is the card's owner. known is false for a zone name the
// engine does not know.
func castFromPropertyMatches(c *Card, name string, sourceController PlayerID) (matched, known bool) {
	zone := strings.TrimPrefix(name, "wasCastFrom")
	zoneOwner := NoPlayer
	if rest, ok := strings.CutPrefix(zone, "Your"); ok {
		zone, zoneOwner = rest, sourceController
	} else if rest, ok := strings.CutPrefix(zone, "Their"); ok {
		zone, zoneOwner = rest, c.Controller()
	}
	byYou := strings.Contains(name, "ByYou")
	if byYou {
		zone, _, _ = strings.Cut(zone, "ByYou")
	}
	where, ok := ZoneByName(zone)
	if !ok {
		return false, false
	}
	switch {
	case !c.wasCast, c.castFrom != where:
		return false, true
	case zoneOwner != NoPlayer && c.Owner != zoneOwner:
		return false, true
	case byYou && c.castBy != sourceController:
		return false, true
	}
	return true, true
}

// thisTurnEnteredMatches is CardProperty.java:969-986: the card entered the
// zone it is in this turn (Card.enteredThisTurn, its turn in zone the current
// one, Card.java:2056-2058) and, for a ThisTurnEntered[Your]<Zone> name, that
// zone is the source controller's <Zone> (Zone.getCardsAddedThisTurn), owned
// by them when Your. The zone read is the card's current one: a card that
// entered the zone this turn and is still there is in Java's added list too.
// known is false for a zone name the engine does not know.
func thisTurnEnteredMatches(g *Game, c *Card, name string, sourceController PlayerID) (matched, known bool) {
	where, your, bare, known := thisTurnEnteredZone(name)
	if !known {
		return false, false
	}
	entered := c.zoneEntered && c.zoneEntryTurn == g.turn
	if bare {
		return entered, true
	}
	inZone := c.Zone == where && c.ZoneOwner == sourceController
	return entered && inZone && (!your || c.Owner == sourceController), true
}

// thisTurnEnteredZone reads a ThisTurnEntered[Your]<Zone> name: bare for
// the name alone, else the zone and whether Your is written. known is false
// when the rest names no zone.
func thisTurnEnteredZone(name string) (where ZoneType, your, bare, known bool) {
	rest := strings.TrimPrefix(name, "ThisTurnEntered")
	if rest == "" {
		return None, false, true, true
	}
	your = strings.Contains(name, "Your")
	if your {
		rest = strings.TrimPrefix(rest, "Your")
	}
	where, known = ZoneByName(rest)
	return where, your, false, known
}

// thisTurnEnteredKnown reports whether a ThisTurnEntered* name is one the
// evaluator answers: Clone's spec check refuses the rest (cloneSpec).
func thisTurnEnteredKnown(name string) bool {
	if strings.HasPrefix(name, "ThisTurnEnteredFrom_") {
		return true
	}
	_, _, _, known := thisTurnEnteredZone(name)
	return known
}

// blockedValidMatches is CardProperty.java:1617-1646's restriction half: one
// of the cards c blocked (or was blocked by) this turn matches spec, read with
// c's controller as the source controller. Java then also tries spec as a
// Defined$ list (Targeted, ParentTarget); that needs the ability in context,
// which this evaluator is not given, so those names match nothing here, as
// "AttachedTo"'s own Defined fallback does not (valid.go).
func blockedValidMatches(g *Game, c *Card, list []CardID, spec string, source CardID) bool {
	parsed := valid.Parse(spec)
	for _, id := range list {
		if Matches(g, g.Card(id), parsed, c.Controller(), source) {
			return true
		}
	}
	return false
}
