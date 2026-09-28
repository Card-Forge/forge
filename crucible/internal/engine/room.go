// Rooms (CR 709.5): a split enchantment card whose two halves are doors,
// each locked or unlocked while the card is a permanent. Only unlocked doors
// have characteristics and abilities; a Room with both doors locked is an
// unnamed "Enchantment Room" with no mana cost.
//
// Ported from forge-game/src/main/java/forge/game/card/Card.java's
// unlockRoom/lockRoom/updateRooms (:8006-:8064), CardUtil.java's
// getEmptyRoomCharacteristic (:218), GameAction.java's changeZone Room
// branches (:148, :571), TriggerUnlockDoor.java and TriggerFullyUnlock.java.

package engine

//enginelint:allow id zone card game parts ability control trigger valid manapay

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// Door is one half of a Room: Java's CardStateName.LeftSplit or RightSplit.
// Its value is the half's face index in the printed card (Faces[0] and
// Faces[carddb.FaceAlternate]).
type Door uint8

const (
	// DoorLeft is the primary face, CardStateName.LeftSplit.
	DoorLeft Door = 0
	// DoorRight is the ALTERNATE face, CardStateName.RightSplit.
	DoorRight Door = carddb.FaceAlternate
)

// String is Java's CardStateName spelling, the one GameState writes after
// UnlockedRoom: (GameState.java:447).
func (d Door) String() string {
	if d == DoorRight {
		return "RightSplit"
	}
	return "LeftSplit"
}

// DoorByName reads a CardStateName spelling back, case-insensitively as
// CardStateName.smartValueOf does.
func DoorByName(name string) (Door, bool) {
	switch {
	case strings.EqualFold(name, "LeftSplit"):
		return DoorLeft, true
	case strings.EqualFold(name, "RightSplit"):
		return DoorRight, true
	}
	return 0, false
}

// doorSet is Card.unlockedRooms: one bit per unlocked Door. Iterating it
// goes left then right, Java's EnumSet order.
type doorSet uint8

const allDoors = doorSet(1<<DoorLeft | 1<<DoorRight)

func (s doorSet) has(d Door) bool { return s&(1<<d) != 0 }

func (s doorSet) list() []Door {
	var out []Door
	for _, d := range [...]Door{DoorLeft, DoorRight} {
		if s.has(d) {
			out = append(out, d)
		}
	}
	return out
}

// isRoomDef reports whether def is a printed Room: a split card whose
// halves are Rooms (Card.isRoom reads the "Room" subtype; isSplitCard the
// split layout).
func isRoomDef(def *compile.Card) bool {
	return def != nil && def.SplitType == carddb.SplitSplit && def.Faces[0].Type.HasSubtype("Room") &&
		def.Faces[carddb.FaceAlternate].Type.HasSubtype("Room")
}

// IsRoomPermanent reports whether c is a Room with door state: on the
// battlefield (or on the stack, cast as one of its halves).
func (c *Card) IsRoomPermanent() bool { return c.roomDef != nil && c.Zone == Battlefield }

// DoorUnlocked reports whether d is an unlocked door of the Room permanent c.
func (c *Card) DoorUnlocked(d Door) bool { return c.IsRoomPermanent() && c.doors.has(d) }

// UnlockedDoors is Card.getUnlockedRooms, left then right; nil for anything
// that is not a Room permanent.
func (c *Card) UnlockedDoors() []Door {
	if !c.IsRoomPermanent() {
		return nil
	}
	return c.doors.list()
}

// LockedDoors is Card.getLockedRooms, left then right; nil for anything that
// is not a Room permanent. Java builds this set with Sets.newHashSet
// (Card.java:7991), whose iteration order is unspecified; this port fixes
// it left then right.
func (c *Card) LockedDoors() []Door {
	if !c.IsRoomPermanent() {
		return nil
	}
	return (allDoors &^ c.doors).list()
}

// PrintedDef is c's own printed definition: the whole split card for a Room
// that is a permanent or a spell (Def is then one view of it), otherwise
// UncopiedDef. Java's GameState writes a card under its paper card's name,
// which is what the fixture dumper reads this for.
func (c *Card) PrintedDef() *compile.Card {
	if c.roomDef != nil {
		return c.roomDef
	}
	return c.UncopiedDef()
}

// ownDefSlot is the field holding c's own characteristics: faceUpDef while
// face down, uncopiedDef under a copy effect, else Def. A Room's view goes
// there, so turning face up or a copy ending shows the Room's current doors
// (Card.java:894 calls updateRooms as a card turns face up).
func (c *Card) ownDefSlot() **compile.Card {
	switch {
	case c.faceUpDef != nil:
		return &c.faceUpDef
	case len(c.copies) > 0:
		return &c.uncopiedDef
	default:
		return &c.Def
	}
}

// enterRoom is GameAction.changeZone's Room branch (GameAction.java:148): a
// Room entering the battlefield enters with both doors locked. A cast half
// is unlocked after the move, by permanentEffect (GameAction.java:571).
func (c *Card) enterRoom() {
	slot := c.ownDefSlot()
	printed := c.roomDef
	if printed == nil {
		printed = *slot
	}
	if !isRoomDef(printed) {
		return
	}
	c.roomDef = printed
	c.doors = 0
	*slot = roomView(printed, 0)
}

// leaveRoom restores a Room's printed definition as it leaves the
// battlefield or the stack: Java's new object in the next zone has an empty
// unlockedRooms set and its Original state (GameAction.java:249). Callers
// run it after turnFaceUp and endCopiesOnLeave, so Def is the own slot.
func (c *Card) leaveRoom() {
	if c.roomDef == nil {
		return
	}
	c.Def = c.roomDef
	c.roomDef = nil
	c.doors = 0
	c.castDoor = DoorLeft
}

// castAsDoor makes a Room card in hand the spell of one of its halves:
// Card.setSplitStateToPlayAbility, CR 709.3 -- the spell has only that
// half's characteristics. undoCastAsDoor reverses it when the cast fails.
func (c *Card) castAsDoor(d Door) {
	c.roomDef = c.Def
	c.Def = doorView(c.roomDef, d)
	c.castDoor = d
}

func (c *Card) undoCastAsDoor() {
	c.Def = c.roomDef
	c.roomDef = nil
	c.castDoor = DoorLeft
}

// castRoomDoor reports the half a Room spell on the stack was cast as.
func (c *Card) castRoomDoor() (Door, bool) {
	if c.roomDef == nil || c.Zone != Stack {
		return 0, false
	}
	return c.castDoor, true
}

// refreshRoom is Card.updateRooms: c's own characteristics become the view
// of its unlocked doors.
func (c *Card) refreshRoom() {
	if c.roomDef == nil {
		return
	}
	*c.ownDefSlot() = roomView(c.roomDef, c.doors)
}

// doorView is one half alone, the characteristics of a spell cast as that
// half and of a Room with only that door unlocked (CardStateName.LeftSplit /
// RightSplit).
func doorView(printed *compile.Card, d Door) *compile.Card {
	v := &compile.Card{Filename: printed.Filename, Name: printed.Faces[d].Name}
	v.Faces[0] = printed.Faces[d]
	return v
}

// roomView is the Room permanent's characteristics for doors (Card.
// updateRooms' three cases):
//
//   - none unlocked: CardUtil.getEmptyRoomCharacteristic, an unnamed
//     Enchantment Room with no mana cost and no abilities;
//   - one: that half alone, in Faces[0];
//   - both: CardStateName.Original, whose name, mana cost, color and type
//     combine the halves (CardFactory.java:329-339) and whose traits are
//     both halves' (CardState.java:691, :720, :742). The halves keep their
//     own faces -- Faces[0] the left one with the combined characteristics,
//     Faces[carddb.FaceAlternate] the right one -- so each trait keeps its
//     own face's SVars.
//
// doorAtSlot maps a view face back to its door.
func roomView(printed *compile.Card, doors doorSet) *compile.Card {
	switch doors {
	case 0:
		v := &compile.Card{Filename: printed.Filename}
		v.Faces[0].Type = cardtype.ParseToken("Enchantment").Union(cardtype.ParseToken("Room"))
		v.Faces[0].ManaCost = mana.NoCost()
		return v
	case 1 << DoorLeft:
		return doorView(printed, DoorLeft)
	case 1 << DoorRight:
		return doorView(printed, DoorRight)
	}
	left, right := printed.Faces[DoorLeft], printed.Faces[DoorRight]
	v := &compile.Card{Filename: printed.Filename, Name: left.Name + " // " + right.Name}
	v.Faces[0] = left
	v.Faces[0].Name = v.Name
	v.Faces[0].Type = left.Type.Union(right.Type)
	shards := append(append([]mana.Shard(nil), left.ManaCost.Shards()...), right.ManaCost.Shards()...)
	v.Faces[0].ManaCost = mana.FromShards(shards, left.ManaCost.Generic()+right.ManaCost.Generic())
	if left.HasColors || right.HasColors {
		v.Faces[0].Colors = faceColors(left) | faceColors(right)
		v.Faces[0].HasColors = true
	}
	v.Faces[DoorRight] = right
	return v
}

// faceColors is a face's printed color: its Colors: override, else its mana
// cost's colors (Card.Colors' own rule).
func faceColors(f compile.Face) mana.Colors {
	if f.HasColors {
		return f.Colors
	}
	return f.ManaCost.Colors()
}

// doorAtSlot is the door whose traits sit in face slot of roomView(_, doors):
// the view's Faces[0] is the left door when it is unlocked, else the right
// one; Faces[carddb.FaceAlternate] is the right door of a fully unlocked
// Room.
func doorAtSlot(doors doorSet, slot int) Door {
	if slot == 0 && doors.has(DoorLeft) {
		return DoorLeft
	}
	return DoorRight
}

// unlockDoor is Card.unlockRoom (Card.java:8006): d of the Room permanent id
// unlocks for p, the Room's characteristics become those of its unlocked
// doors, and then Mode$ UnlockDoor triggers -- and, once both doors are
// unlocked, Mode$ FullyUnlock ones -- are collected and pushed together.
// Reports false, doing nothing, when id is not a Room permanent or d is
// already unlocked. Java also fires GameEventDoorChanged; this port has no
// such event kind (ADR-0013's schema).
func (g *Game) unlockDoor(controller PlayerController, id CardID, p PlayerID, d Door) bool {
	c := g.Card(id)
	if !c.IsRoomPermanent() || c.doors.has(d) {
		return false
	}
	c.doors |= 1 << d
	c.refreshRoom()
	matches := g.doorTriggerMatches("UnlockDoor", id, p, d)
	if c.doors == allDoors {
		matches = append(matches, g.doorTriggerMatches("FullyUnlock", id, p, d)...)
	}
	g.pushTriggeredAbilities(controller, matches)
	return true
}

// LoadUnlockedDoor unlocks d of the Room permanent id with no trigger: a
// fixture's UnlockedRoom: (GameState.java:1419), applied while GameState
// suppresses every trigger (GameState.java:617). Reports false, doing
// nothing, when id is not a Room permanent.
func (g *Game) LoadUnlockedDoor(id CardID, d Door) bool {
	c := g.Card(id)
	if (d != DoorLeft && d != DoorRight) || !c.IsRoomPermanent() {
		return false
	}
	c.doors |= 1 << d
	c.refreshRoom()
	return true
}

// lockDoor is Card.lockRoom (Card.java:8032): d of the Room permanent id
// locks again. No trigger mode watches a door locking.
func (g *Game) lockDoor(id CardID, d Door) bool {
	c := g.Card(id)
	if !c.IsRoomPermanent() || !c.doors.has(d) {
		return false
	}
	c.doors &^= 1 << d
	c.refreshRoom()
	return true
}

// doorTriggerMatches is TriggerUnlockDoor/TriggerFullyUnlock.performTest
// against every trigger host: ValidCard$ matches the Room, ValidPlayer$ the
// player who unlocked it, and UnlockDoor's ThisDoor$ requires the host be
// that Room and the trigger be printed on door d (the trigger's
// getCardStateName). A trigger with no TriggerZones$ is active on the
// battlefield only.
func (g *Game) doorTriggerMatches(mode string, room CardID, p PlayerID, d Door) []Ability {
	var matches []Ability
	rc := g.Card(room)
	// Every zone a host can sit in: the battlefield (every UnlockDoor line,
	// 16 of 17 FullyUnlock lines), an effect card's Command zone, and the
	// graveyard (1 FullyUnlock line, TriggerZones$ Graveyard).
	zones := [...]ZoneType{Battlefield, Command, Graveyard}
	for _, pid := range g.Players() {
		for _, z := range zones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for slot, face := range h.Def.Faces {
					for _, t := range face.Triggers {
						if !strings.EqualFold(t.Name, mode) || !doorTriggerZoneMatches(h, t, z) {
							continue
						}
						if v, ok := t.Param("ValidCard"); ok && !Matches(g, rc, valid.Parse(v), h.Controller(), host) {
							continue
						}
						if v, ok := t.Param("ValidPlayer"); ok {
							matched, recognized := matchesPlayerBase(p, h.Controller(), v)
							if !recognized || !matched {
								continue
							}
						}
						if mode == "UnlockDoor" && hasAnyParam(t, "ThisDoor") &&
							(host != room || doorAtSlot(h.doors, slot) != d) {
							continue
						}
						if sub, api, optional, ok := triggerEffectAPI(g, h, face.Amounts, t); ok {
							matches = append(matches, Ability{API: api, Source: host, Controller: h.Controller(), Params: sub,
								Amounts: face.Amounts, Optional: optional, triggered: triggeredObjects{card: room, player: p}})
						}
					}
				}
			}
		}
	}
	return matches
}

// doorTriggerZoneMatches is TriggerReplacementBase.zonesCheck for these two
// modes: an effect card's triggers live in the Command zone alone; a
// TriggerZones$ list names its zones; none means the battlefield.
func doorTriggerZoneMatches(h *Card, t *compile.Ability, zone ZoneType) bool {
	if h.IsEffect {
		return zone == Command
	}
	v, ok := t.Param("TriggerZones")
	if !ok {
		return zone == Battlefield
	}
	for _, name := range strings.Split(v, ",") {
		if z, ok := ZoneByName(name); ok && z == zone {
			return true
		}
	}
	return false
}

// UnlockDoor is the Room special action (CR 709.5, a special action under
// CR 116.2): pid pays the mana cost of a locked door of a Room they control,
// any time they could cast a sorcery, and that door unlocks -- no stack,
// and pid keeps priority. Java synthesizes it per locked door as `ST$
// UnlockDoor | Cost$ <that half's mana cost> | Unlock$ True`
// (CardFactoryUtil.java:124), offered while the Room is in play in one of
// its Room states (not face down, not a copy of something else), phased
// in, and its controller can cast a sorcery (Card.java:7414-7421);
// resolving it is UnlockDoorEffect's default Mode$ ThisDoor, unlocking the
// half the ability belongs to.
//
// Reports false, changing nothing, when the action is not legal or the
// cost could not be paid (PayManaCost's own contract).
func (g *Game) UnlockDoor(pid PlayerID, card CardID, d Door, controller PlayerController) bool {
	c := g.Card(card)
	if d != DoorLeft && d != DoorRight {
		return false
	}
	if !c.IsRoomPermanent() || c.doors.has(d) || c.Controller() != pid || c.IsFaceDown() || c.IsCopy() || c.IsPhasedOut() {
		return false
	}
	if !g.canActSorcerySpeed(pid) || unlockCostModified(g, pid) {
		return false
	}
	if _, paid := g.payManaCostX(pid, c.roomDef.Faces[d].ManaCost, controller); !paid {
		return false
	}
	g.unlockDoor(controller, card, pid, d)
	return true
}

// unlockCostModified reports whether a permanent's static changes what
// unlocking a door costs: `S:Mode$ ReduceCost | ValidSpell$ Static.Unlock`
// (Inquisitive Glimmer; SpellAbilityProperty.java:95 matches the
// synthesized ability's Unlock$ param). This port applies no cost-changing
// static, so UnlockDoor refuses rather than charging the printed cost
// (GO-7).
// unlockCostModified reports whether a live cost-changing static applies to
// unlocker's own unlock special action (Inquisitive Glimmer's own real line,
// "S:Mode$ ReduceCost | ValidSpell$ Static.Unlock | Activator$ You" --
// Activator$ restricts the static to its own controller's unlocks, not every
// player's). A static naming no Activator$ applies to everyone, Java's own
// default. Ignoring Activator$ here previously let one player's Glimmer
// abort every other player's legal unlock (rules-review finding on the
// merged commit).
func unlockCostModified(g *Game, unlocker PlayerID) bool {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(id)
			if c.Def == nil {
				continue
			}
			for _, face := range c.Def.Faces {
				for _, st := range face.Statics {
					mode, _ := st.Param("Mode")
					spell, _ := st.Param("ValidSpell")
					if (mode != "ReduceCost" && mode != "RaiseCost") || !strings.Contains(spell, "Unlock") {
						continue
					}
					if activator, ok := st.Param("Activator"); ok {
						matched, recognized := matchesPlayerSpec(g, unlocker, c.Controller(), id, activator)
						if !recognized || !matched {
							continue
						}
					}
					return true
				}
			}
		}
	}
	return false
}
