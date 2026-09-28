package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Real UnlockDoor effect lines, verbatim but for their descriptions.
const (
	// keybearerUnlock is Ghostly Keybearer's TrigUnlock.
	keybearerUnlock = "DB$ UnlockDoor | Mode$ Unlock | ValidTgts$ Room.YouCtrl | TgtPrompt$ Choose target Room you control | TargetMin$ 0 | TargetMax$ 1"
	// dancersUnlock is Ghostly Dancers' DBUnlock.
	dancersUnlock = "DB$ UnlockDoor | Mode$ Unlock | Choices$ Room.YouCtrl+!FullyUnlocked | IsPresent$ Room.YouCtrl+!FullyUnlocked"
	// marinaLockOrUnlock is Marina Vendrell's ability, as a DB$ line.
	marinaLockOrUnlock = "DB$ UnlockDoor | Mode$ LockOrUnlock | ValidTgts$ Room.YouCtrl | TgtPrompt$ Choose target Room you control"
)

// fullRoom casts testRoomDef's left door and unlocks its right one: a fully
// unlocked Room, with p's life at 131 (both doors and the FullyUnlock
// watcher, which stays on the battlefield).
func fullRoom(t *testing.T, g *engine.Game, p engine.PlayerID) engine.CardID {
	t.Helper()
	g.NewCard(fullyUnlockWatcher(t), p, engine.Battlefield)
	room := castRoom(t, g, p, testRoomDef(t), engine.DoorLeft)
	c := engine.NewScriptedController()
	g.Player(p).ManaPool.Add(mana.Blue, 3)
	queueBlueGeneric(c, 2)
	if !g.UnlockDoor(p, room, engine.DoorRight, c) {
		t.Fatal("UnlockDoor failed paying the right door's cost")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	return room
}

// TestUnlockDoorEffectUnlockTheOneLockedDoor proves Ghostly Keybearer's
// shape: the targeted Room's one locked door unlocks without a question
// and fires its own trigger.
func TestUnlockDoorEffectUnlockTheOneLockedDoor(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	room := castRoom(t, g, p, testRoomDef(t), engine.DoorLeft)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(room)}, keybearerUnlock); err != nil {
		t.Fatalf("resolve: %v", err)
	}
	if !g.Card(room).DoorUnlocked(engine.DoorRight) || g.Player(p).Life != 31 {
		t.Errorf("right door unlocked %v life %d, want true and 31", g.Card(room).DoorUnlocked(engine.DoorRight), g.Player(p).Life)
	}
}

// TestUnlockDoorEffectUnlockAsksWhichLockedDoor proves Mode$ Unlock on a
// Room with both doors locked asks which one (chooseSingleCardState).
func TestUnlockDoorEffectUnlockAsksWhichLockedDoor(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	room := g.NewCard(testRoomDef(t), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueRoomDoor(engine.DoorRight)
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(room)}, keybearerUnlock); err != nil {
		t.Fatalf("resolve: %v", err)
	}
	rc := g.Card(room)
	if !rc.DoorUnlocked(engine.DoorRight) || rc.DoorUnlocked(engine.DoorLeft) || g.Player(p).Life != 30 {
		t.Errorf("unlocked %v life %d, want [RightSplit] and 30", rc.UnlockedDoors(), g.Player(p).Life)
	}
}

// TestUnlockDoorEffectChoicesSkipsFullyUnlockedRooms proves Ghostly
// Dancers' shape: Choices$ ...+!FullyUnlocked offers only Rooms with a
// locked door (the FullyUnlocked property), and the picked one unlocks.
func TestUnlockDoorEffectChoicesSkipsFullyUnlockedRooms(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	full := fullRoom(t, g, p)
	half := castRoom(t, g, p, testRoomDef(t), engine.DoorRight)
	rec := &offerRecorder{ScriptedController: engine.NewScriptedController()}
	rec.QueueCardChoice([]engine.CardID{half})
	if err := resolveWith(t, g, p, rec, dancersUnlock); err != nil {
		t.Fatalf("resolve: %v", err)
	}
	if len(rec.offers) != 1 || len(rec.offers[0]) != 1 || rec.offers[0][0] != half {
		t.Errorf("offered %v, want only %v: %v is fully unlocked", rec.offers, half, full)
	}
	if !g.Card(half).DoorUnlocked(engine.DoorLeft) {
		t.Error("the picked Room's locked door stayed locked")
	}
}

// TestUnlockDoorEffectChoicesWithNothingToPick proves the Choices$ branch
// does nothing when no Room matches (chooseSingleEntityForEffect over an
// empty list returns null).
func TestUnlockDoorEffectChoicesWithNothingToPick(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	fullRoom(t, g, p)
	if err := resolveWith(t, g, p, engine.NewScriptedController(), dancersUnlock); err != nil {
		t.Fatalf("resolve: %v", err)
	}
}

// TestUnlockDoorEffectLockOrUnlock proves Marina Vendrell's three cases:
// a fully unlocked Room locks the chosen door, a half-locked one flips the
// chosen door either way, and a fully locked one unlocks the chosen door.
func TestUnlockDoorEffectLockOrUnlock(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	room := fullRoom(t, g, p)
	target := []engine.EntityID{engine.CardEntity(room)}
	step := func(d engine.Door) {
		t.Helper()
		c := engine.NewScriptedController()
		c.QueueRoomDoor(d)
		if _, err := resolveNow(t, g, p, c, target, marinaLockOrUnlock); err != nil {
			t.Fatalf("resolve: %v", err)
		}
	}
	rc := g.Card(room)
	step(engine.DoorLeft)
	if got := rc.UnlockedDoors(); len(got) != 1 || got[0] != engine.DoorRight || rc.Def.Name != "Right Hall" {
		t.Fatalf("after locking left: unlocked %v name %q, want [RightSplit] Right Hall", got, rc.Def.Name)
	}
	step(engine.DoorRight)
	if len(rc.UnlockedDoors()) != 0 || rc.Def.Name != "" {
		t.Fatalf("after locking right: unlocked %v name %q, want an empty room", rc.UnlockedDoors(), rc.Def.Name)
	}
	life := g.Player(p).Life
	step(engine.DoorLeft)
	if got := rc.UnlockedDoors(); len(got) != 1 || got[0] != engine.DoorLeft || g.Player(p).Life != life+1 {
		t.Fatalf("after unlocking left: unlocked %v life %d, want [LeftSplit] and %d", got, g.Player(p).Life, life+1)
	}
	step(engine.DoorRight)
	if len(rc.LockedDoors()) != 0 || g.Player(p).Life != life+111 {
		t.Errorf("after unlocking right: locked %v life %d, want none and %d (right door and FullyUnlock)",
			rc.LockedDoors(), g.Player(p).Life, life+111)
	}
}

// TestUnlockDoorEffectNothingToUnlockOrConditionUnmet proves Mode$ Unlock
// on a fully unlocked Room does nothing and asks nothing (Java's
// chooseSingleCardState over no states returns null), and an unmet
// Condition skips the effect.
func TestUnlockDoorEffectNothingToUnlockOrConditionUnmet(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	room := fullRoom(t, g, p)
	target := []engine.EntityID{engine.CardEntity(room)}
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), target, keybearerUnlock); err != nil {
		t.Fatalf("resolve: %v", err)
	}
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), target, marinaLockOrUnlock+" | ConditionPlayerTurn$ False"); err != nil {
		t.Fatalf("resolve: %v", err)
	}
	if len(g.Card(room).LockedDoors()) != 0 {
		t.Errorf("locked %v, want both doors still unlocked", g.Card(room).LockedDoors())
	}
}

// TestRoomDoorNames proves the CardStateName spellings GameState writes
// after UnlockedRoom: read back, case-insensitively.
func TestRoomDoorNames(t *testing.T) {
	t.Parallel()

	for _, d := range []engine.Door{engine.DoorLeft, engine.DoorRight} {
		if got, ok := engine.DoorByName(strings.ToLower(d.String())); !ok || got != d {
			t.Errorf("DoorByName(%q) = %v %v, want %v", strings.ToLower(d.String()), got, ok, d)
		}
	}
	if _, ok := engine.DoorByName("Original"); ok {
		t.Error("DoorByName read Original as a door")
	}
}

// TestUnlockDoorEffectRejects proves the unported default Mode$ ThisDoor
// and an answer outside the offered doors are errors (GO-7), and a target
// that is not a Room is left alone.
func TestUnlockDoorEffectRejects(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	room := g.NewCard(testRoomDef(t), p, engine.Battlefield)
	target := []engine.EntityID{engine.CardEntity(room)}
	for _, line := range []string{
		"DB$ UnlockDoor | ValidTgts$ Room.YouCtrl",
		"DB$ UnlockDoor | Mode$ ThisDoor | ValidTgts$ Room.YouCtrl",
	} {
		if _, err := resolveNow(t, g, p, engine.NewScriptedController(), target, line); err == nil ||
			!strings.Contains(err.Error(), "Mode$ ThisDoor not resolvable yet") {
			t.Errorf("%q: err %v, want Mode$ ThisDoor not resolvable yet", line, err)
		}
	}
	c := engine.NewScriptedController()
	c.QueueRoomDoor(engine.Door(5))
	if _, err := resolveNow(t, g, p, c, target, marinaLockOrUnlock); err == nil || !strings.Contains(err.Error(), "controller chose door") {
		t.Errorf("err %v, want the out-of-range door refused", err)
	}
	creature := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(creature)}, marinaLockOrUnlock); err != nil {
		t.Errorf("a non-Room target: %v, want nothing done", err)
	}
}
