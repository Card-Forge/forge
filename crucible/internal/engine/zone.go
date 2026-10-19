// Zones, and the ordered card lists they hold.

package engine

import "github.com/jczastkiewicz/crucible/pkg/collect"

// ZoneType is where a card is. Ported from
// forge-game/src/main/java/forge/game/zone/ZoneType.java, in its declaration
// order, because card scripts name these by string and the order is what a
// `ZoneType.listValueOf` comparison sees.
type ZoneType uint8

// The zones, in Java's declaration order.
const (
	Hand ZoneType = iota
	Library
	Graveyard
	Battlefield
	Exile
	Flashback
	Command
	Stack
	Sideboard
	Ante
	Merged
	SchemeDeck
	PlanarDeck
	AttractionDeck
	Junkyard
	ContraptionDeck
	StickerSheets
	Subgame
	ExtraHand
	None

	numZoneTypes = int(None) + 1
)

var zoneNames = [numZoneTypes]string{
	Hand: "Hand", Library: "Library", Graveyard: "Graveyard", Battlefield: "Battlefield",
	Exile: "Exile", Flashback: "Flashback", Command: "Command", Stack: "Stack",
	Sideboard: "Sideboard", Ante: "Ante", Merged: "Merged", SchemeDeck: "SchemeDeck",
	PlanarDeck: "PlanarDeck", AttractionDeck: "AttractionDeck", Junkyard: "Junkyard",
	ContraptionDeck: "ContraptionDeck", StickerSheets: "StickerSheets", Subgame: "Subgame", ExtraHand: "ExtraHand",
	None: "None",
}

// String returns the zone as a card script spells it.
func (z ZoneType) String() string {
	if int(z) >= numZoneTypes {
		return "None"
	}
	return zoneNames[z]
}

// ZoneByName looks a zone up by the name a script writes, and reports whether
// it is one. Matching is exact, not case-insensitive: Java's
// ZoneType.smartValueOf trims and then compares with compareToIgnoreCase, but
// every zone name in the corpus is already written in the enum's own case, so
// the only thing case-insensitive matching would add is letting a typo'd zone
// name silently resolve to the zone it did not ask for -- the same "matching
// exactly finds bugs that a looser match would hide" reasoning valid-strings.md's
// own "Nothing is trimmed, on purpose" makes for the parser one layer up.
func ZoneByName(name string) (ZoneType, bool) {
	for i, n := range zoneNames {
		if n == name {
			return ZoneType(i), true
		}
	}
	return None, false
}

// Zone is one player's copy of one zone, or the single shared copy for the
// zones that have no owner.
//
// Order is load-bearing everywhere: a library is a stack, a graveyard's order
// decides `CardsInGraveyard` counts and delirium, and the battlefield's order
// is the tiebreak Java uses when two triggers are otherwise simultaneous
// (GO-12).
type Zone struct {
	Type  ZoneType
	Owner PlayerID
	cards *collect.OrderedSet[CardID]
	// phasedOut is the subset of cards that is phased out (CR 702.26b), nil
	// until something in this zone first phases out. Only the battlefield
	// ever has one. Game.setPhasedOut is its only writer and keeps it in step
	// with each card's own Card.phasedOut (ADR-0021, decision 5); membership
	// is all it records, the order stays cards' own.
	phasedOut *collect.OrderedSet[CardID]
}

// Cards returns the zone's contents in order, without its phased-out
// permanents: CR 702.26b's "treated as though it does not exist", taken from
// the one place Java takes it too (PlayerZoneBattlefield.getCards(true),
// PlayerZoneBattlefield.java:76-100, ADR-0021). With nothing phased out --
// every zone but the battlefield, and the battlefield nearly always -- the
// slice is the zone's own, so a caller that needs to hold it past the next
// mutation copies it. Otherwise it is a fresh slice, in the same relative
// order (GO-12).
func (z *Zone) Cards() []CardID {
	all := z.cards.All()
	if z.phasedOut == nil || z.phasedOut.Len() == 0 {
		return all
	}
	out := make([]CardID, 0, len(all)-z.phasedOut.Len())
	for _, id := range all {
		if !z.phasedOut.Contains(id) {
			out = append(out, id)
		}
	}
	return out
}

// CardsIncludingPhasedOut is Cards with the phased-out permanents left in:
// Java's getCardsIncludePhasingIn / getCards(false). ADR-0021 names who may
// call it -- the untap step's phasing (Untap.java:202), Phases' own
// phase-in branch (PhasesEffect.java:47-48), the cleanup step's damage clear
// (PhaseHandler.java:400, Game.java:1236), the fixture dump and load
// (GameState.java:182, :223) -- and a new caller cites the Java opt-in it
// mirrors. The slice is the zone's own.
func (z *Zone) CardsIncludingPhasedOut() []CardID { return z.cards.All() }

// Len is how many cards the zone holds, phased-out ones included: Java's
// Zone.size() reads the unfiltered list.
func (z *Zone) Len() int { return z.cards.Len() }

// Contains reports whether the card is in this zone, phased out or not: a
// phased-out permanent never left the battlefield (CR 702.26d).
func (z *Zone) Contains(id CardID) bool { return z.cards.Contains(id) }

// remove takes id out of the zone, and out of its phased-out subset with
// it, so a card that leaves the battlefield phased out does not come back
// hidden.
func (z *Zone) remove(id CardID) {
	z.cards.Remove(id)
	if z.phasedOut != nil {
		z.phasedOut.Remove(id)
	}
}

// clone is the zone's half of Game.Clone. The phased-out subset is copied
// only when it holds something, so the common case allocates nothing more
// than it did before phasing existed.
func (z *Zone) clone() *Zone {
	out := &Zone{Type: z.Type, Owner: z.Owner, cards: z.cards.Clone()}
	if z.phasedOut != nil && z.phasedOut.Len() > 0 {
		out.phasedOut = z.phasedOut.Clone()
	}
	return out
}
