package engine_test

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// unlockGainTrigger is the real "When you unlock this door" shape (29 of
// 30 Mode$ UnlockDoor lines), with an Execute$ that makes it observable.
const unlockGainTrigger = "Mode$ UnlockDoor | ValidPlayer$ You | ValidCard$ Card.Self | ThisDoor$ True | Execute$ TrigGain"

// testRoomDef is a Room whose left door (cost U) gains 1 life and whose
// right door (cost 2 U) gains 10 life when unlocked -- enough to tell which
// door's trigger fired.
func testRoomDef(t *testing.T) *compile.Card {
	t.Helper()
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n[EnchantmentTypes]\nRoom\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "test_room", SplitType: carddb.SplitSplit}
	for i, half := range []struct{ name, cost, gain string }{{"Left Hall", "U", "1"}, {"Right Hall", "2 U", "10"}} {
		f := &raw.Faces[[]int{0, carddb.FaceAlternate}[i]]
		f.Present = true
		f.Name = half.name
		f.Type = cardtype.Parse(reg, "Enchantment Room")
		f.ManaCost = mana.MustParse(half.cost)
		f.Triggers = []string{unlockGainTrigger}
		f.SVars.Set("TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ "+half.gain)
	}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile room: %v", err)
	}
	return c
}

// realRoomDef compiles a Room script from the Forge tree.
func realRoomDef(t *testing.T, file string) *compile.Card {
	t.Helper()
	root := scenarioRepoRoot(t)
	f, err := os.Open(filepath.Join(root, "forge-gui", "res", "lists", "TypeLists.txt"))
	if err != nil {
		t.Fatalf("open TypeLists: %v", err)
	}
	reg, err := cardtype.LoadRegistry(f)
	_ = f.Close()
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw, err := os.ReadFile(filepath.Join(root, "forge-gui", "res", "cardsfolder", file[:1], file+".txt"))
	if err != nil {
		t.Fatalf("read %s: %v", file, err)
	}
	parsed, err := carddb.ParseScript(reg, file, raw)
	if err != nil {
		t.Fatalf("parse %s: %v", file, err)
	}
	c, err := compile.Compile(parsed)
	if err != nil {
		t.Fatalf("compile %s: %v", file, err)
	}
	return c
}

// castRoom casts room's door d for p with exactly its cost in blue mana and
// resolves the stack, unlock trigger included.
func castRoom(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, d engine.Door) engine.CardID {
	t.Helper()
	c := engine.NewScriptedController()
	queueBlueGeneric(c, def.Faces[d].ManaCost.Generic())
	g.Player(p).ManaPool.Add(mana.Blue, def.Faces[d].ManaCost.CMC())
	room := g.NewCard(def, p, engine.Hand)
	if !g.CastRoomDoor(p, room, d, c) {
		t.Fatalf("CastRoomDoor(%s) failed with exactly enough mana", d)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	return room
}

// queueBlueGeneric answers n generic-mana questions with blue.
func queueBlueGeneric(c *engine.ScriptedController, n int) {
	for i := 0; i < n; i++ {
		c.QueuePayGeneric(mana.ShardU)
	}
}

// fullyUnlockWatcher is a permanent with the real Mode$ FullyUnlock shape
// (Entity Tracker's "whenever you fully unlock a Room"), gaining 100 life.
func fullyUnlockWatcher(t *testing.T) *compile.Card {
	t.Helper()
	return bendWatcherDef(t, "Mode$ FullyUnlock | ValidCard$ Card.Room | ValidPlayer$ You | Secondary$ True | TriggerZones$ Battlefield", "100")
}

// TestRoomRealScriptCompilesAsTwoDoors proves a real Room script
// (AlternateMode:Split) compiles to two Room faces, and that as a spell and
// as a permanent it takes the characteristics of its cast half only (CR
// 709.3): name, mana value, and only that half's traits.
func TestRoomRealScriptCompilesAsTwoDoors(t *testing.T) {
	t.Parallel()

	def := realRoomDef(t, "bottomless_pool_locker_room")
	if def.SplitType != carddb.SplitSplit || def.Faces[engine.DoorLeft].Name != "Bottomless Pool" ||
		def.Faces[engine.DoorRight].Name != "Locker Room" {
		t.Fatalf("compiled %v %q/%q, want a split Bottomless Pool // Locker Room", def.SplitType,
			def.Faces[engine.DoorLeft].Name, def.Faces[engine.DoorRight].Name)
	}
	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	room := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, room, c) {
		t.Fatal("CastSpell failed casting Bottomless Pool with U")
	}
	if got := g.Card(room).Def.Name; got != "Bottomless Pool" {
		t.Errorf("spell name %q, want the cast half's", got)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(victim).Zone; got != engine.Hand {
		t.Errorf("victim in %v, want Hand: Bottomless Pool's unlock trigger returns it", got)
	}
	rc := g.Card(room)
	if got := rc.UnlockedDoors(); len(got) != 1 || got[0] != engine.DoorLeft {
		t.Errorf("unlocked %v, want [LeftSplit]", got)
	}
	if rc.Def.Name != "Bottomless Pool" || rc.CMC() != 1 || rc.PrintedDef() != def {
		t.Errorf("permanent %q mana value %d, want Bottomless Pool 1 over the printed card", rc.Def.Name, rc.CMC())
	}
}

// TestRoomCastRightDoorUnlocksOnlyThatDoor proves casting the right half
// pays its cost, and only its own "when you unlock this door" fires.
func TestRoomCastRightDoorUnlocksOnlyThatDoor(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	room := castRoom(t, g, p, testRoomDef(t), engine.DoorRight)
	if got := g.Player(p).Life; got != 30 {
		t.Errorf("life %d, want 30: the right door's trigger alone", got)
	}
	rc := g.Card(room)
	if got := rc.LockedDoors(); len(got) != 1 || got[0] != engine.DoorLeft {
		t.Errorf("locked %v, want [LeftSplit]", got)
	}
	if rc.Def.Name != "Right Hall" || rc.CMC() != 3 {
		t.Errorf("permanent %q mana value %d, want Right Hall 3", rc.Def.Name, rc.CMC())
	}
}

// TestRoomUnlockSpecialActionFiresThatDoorAndFullyUnlock proves the
// special action: paying the locked door's cost unlocks it without the
// stack, only that door's trigger fires (not the already unlocked one's
// again), FullyUnlock fires once, and the Room then combines both halves.
func TestRoomUnlockSpecialActionFiresThatDoorAndFullyUnlock(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(fullyUnlockWatcher(t), p, engine.Battlefield)
	room := castRoom(t, g, p, testRoomDef(t), engine.DoorLeft)
	if got := g.Player(p).Life; got != 21 {
		t.Fatalf("life %d after casting the left door, want 21", got)
	}
	c := engine.NewScriptedController()
	queueBlueGeneric(c, 2)
	if g.UnlockDoor(p, room, engine.DoorRight, c) {
		t.Fatal("UnlockDoor succeeded with no mana")
	}
	if g.UnlockDoor(p, room, engine.DoorLeft, c) {
		t.Fatal("UnlockDoor succeeded on an unlocked door")
	}
	g.Player(p).ManaPool.Add(mana.Blue, 3)
	queueBlueGeneric(c, 2)
	if !g.UnlockDoor(p, room, engine.DoorRight, c) {
		t.Fatal("UnlockDoor failed paying the right door's cost")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 131 {
		t.Errorf("life %d, want 131: right door (+10) and FullyUnlock (+100), not the left door again", got)
	}
	rc := g.Card(room)
	if rc.Def.Name != "Left Hall // Right Hall" || rc.CMC() != 4 || len(rc.LockedDoors()) != 0 {
		t.Errorf("permanent %q mana value %d locked %v, want both halves combined", rc.Def.Name, rc.CMC(), rc.LockedDoors())
	}
}

// TestRoomUnlockSpecialActionIsSorcerySpeedForItsController proves the
// action's timing and control gates (Card.java:7414).
func TestRoomUnlockSpecialActionIsSorcerySpeedForItsController(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	room := castRoom(t, g, p, testRoomDef(t), engine.DoorLeft)
	c := engine.NewScriptedController()
	g.Player(other).ManaPool.Add(mana.Blue, 3)
	if g.UnlockDoor(other, room, engine.DoorRight, c) {
		t.Error("an opponent unlocked the Room's door")
	}
	g.Player(p).ManaPool.Add(mana.Blue, 3)
	g.SetTurnState(1, p, engine.Upkeep)
	if g.UnlockDoor(p, room, engine.DoorRight, c) {
		t.Error("UnlockDoor succeeded outside a main phase")
	}
	if g.Card(room).DoorUnlocked(engine.DoorRight) {
		t.Error("a refused UnlockDoor unlocked the door")
	}
}

// TestRoomUnlockRefusedUnderAnUnlockCostStatic proves the special action
// fails closed while Inquisitive Glimmer's "Unlock costs you pay cost {1}
// less" is on the battlefield: no cost-changing static is applied, so
// charging the printed cost would be wrong (GO-7). LoadUnlockedDoor refuses
// a value that names no door.
func TestRoomUnlockRefusedUnderAnUnlockCostStatic(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	room := g.NewCard(testRoomDef(t), p, engine.Battlefield)
	if g.LoadUnlockedDoor(room, engine.Door(5)) || len(g.Card(room).UnlockedDoors()) != 0 {
		t.Fatal("LoadUnlockedDoor accepted a door that does not exist")
	}
	g.NewCard(copyTestDef(t, "Glimmer", "Creature Elf", "2", "2",
		"S:Mode$ ReduceCost | ValidSpell$ Static.Unlock | Activator$ You | Amount$ 1"), p, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	if g.UnlockDoor(p, room, engine.DoorLeft, engine.NewScriptedController()) {
		t.Error("UnlockDoor paid the printed cost under an unlock cost reduction")
	}
}

// TestRoomUnlockCostStaticOnlyAppliesToItsOwnActivator proves Activator$ is
// checked against unlockCostModified's own caller, not every player's
// battlefield indiscriminately: an opponent's Glimmer (Activator$ You, that
// opponent) does not touch your own unlock. Before this fix, one player's
// Glimmer refused every player's legal unlock (rules-review finding on the
// merged commit).
func TestRoomUnlockCostStaticOnlyAppliesToItsOwnActivator(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	room := g.NewCard(testRoomDef(t), p, engine.Battlefield)
	g.NewCard(copyTestDef(t, "Glimmer", "Creature Elf", "2", "2",
		"S:Mode$ ReduceCost | ValidSpell$ Static.Unlock | Activator$ You | Amount$ 1"), other, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	if !g.UnlockDoor(p, room, engine.DoorLeft, engine.NewScriptedController()) {
		t.Error("an opponent's Glimmer refused a legal unlock it does not apply to")
	}
}

// TestCastRoomDoorRejectsAnInvalidDoor proves a Door outside
// DoorLeft/DoorRight is refused before it ever reaches doorView's own
// printed.Faces[d] index -- applyAction (priority.go) hands CastRoomDoor an
// unvalidated controller answer, and an out-of-range Door there panicked
// before this fix (rules-review finding on the merged commit; GO-7).
func TestCastRoomDoorRejectsAnInvalidDoor(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	room := g.NewCard(testRoomDef(t), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Blue, 3)
	for _, d := range []engine.Door{2, 6, 200} {
		if g.CastRoomDoor(p, room, d, engine.NewScriptedController()) {
			t.Errorf("CastRoomDoor(%d) succeeded, want refused", d)
		}
	}
}

// TestRoomEnteringUncastHasBothDoorsLocked proves GameAction.java:148: a
// Room put onto the battlefield without being cast is an unnamed
// Enchantment Room with no mana cost and no abilities, and it becomes its
// printed card again once it leaves.
func TestRoomEnteringUncastHasBothDoorsLocked(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := testRoomDef(t)
	room := g.NewCard(def, p, engine.Graveyard)
	g.Move(room, engine.Battlefield, p)
	rc := g.Card(room)
	if rc.Def.Name != "" || rc.CMC() != 0 || !rc.Type().HasSubtype("Room") || !rc.Type().Has(cardtype.Enchantment) {
		t.Errorf("empty room %q mana value %d type %v, want an unnamed 0-cost Enchantment Room", rc.Def.Name, rc.CMC(), rc.Type())
	}
	if got := rc.LockedDoors(); len(got) != 2 || got[0] != engine.DoorLeft || got[1] != engine.DoorRight {
		t.Errorf("locked %v, want [LeftSplit RightSplit]", got)
	}
	for _, f := range rc.Def.Faces {
		if len(f.Triggers) != 0 {
			t.Fatalf("an empty room has triggers %v", f.Triggers)
		}
	}
	g.Move(room, engine.Graveyard, p)
	if rc.Def != def || rc.UnlockedDoors() != nil || rc.IsRoomPermanent() {
		t.Errorf("left the battlefield as %q, want the printed card with no door state", rc.Def.Name)
	}
}

// TestRoomSpellLeavingTheStackIsItsPrintedCard proves a Room spell that
// does not resolve (countered, here moved straight to the graveyard) is
// the whole split card again, and that a Room cast with too little mana is
// never cast as a half.
func TestRoomSpellLeavingTheStackIsItsPrintedCard(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := testRoomDef(t)
	c := engine.NewScriptedController()
	room := g.NewCard(def, p, engine.Hand)
	queueBlueGeneric(c, 2)
	if g.CastRoomDoor(p, room, engine.DoorRight, c) {
		t.Fatal("cast the right door with no mana")
	}
	if g.Card(room).Def != def {
		t.Fatal("a failed cast left the card as one of its halves")
	}
	if g.CastRoomDoor(p, g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand), engine.DoorRight, c) {
		t.Fatal("CastRoomDoor cast a card that is not a Room")
	}
	g.Player(p).ManaPool.Add(mana.Blue, 3)
	queueBlueGeneric(c, 2)
	if !g.CastRoomDoor(p, room, engine.DoorRight, c) {
		t.Fatal("CastRoomDoor failed with enough mana")
	}
	if got := g.Card(room).Def.Name; got != "Right Hall" {
		t.Errorf("spell %q, want Right Hall", got)
	}
	g.Move(room, engine.Graveyard, p)
	if g.Card(room).Def != def || g.Card(room).PrintedDef() != def {
		t.Errorf("countered Room is %q, want its printed card", g.Card(room).Def.Name)
	}
}

// TestRoomCopiesCopyBothHalvesNotTheDoors proves a Room permanent's
// copiable values are its whole printed card (CardFactory.java:535-542): a
// token copy of a half-unlocked Room enters with both doors locked and can
// unlock either. A permanent becoming a copy of a Room is rejected: its own
// door state (CloneEffect.java:146) is not modeled.
func TestRoomCopiesCopyBothHalvesNotTheDoors(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := testRoomDef(t)
	room := castRoom(t, g, p, def, engine.DoorRight)
	c := engine.NewScriptedController()
	populate := g.NewCard(copyTestDef(t, "Populate", "Artifact", "", "",
		"A:AB$ CopyPermanent | ValidTgts$ Enchantment | RememberTokens$ True"), p, engine.Battlefield)
	mustActivate(t, g, p, c, populate, room)
	made, _ := g.Card(populate).Memory.Remembered()[0].AsCard()
	tc := g.Card(made)
	if tc.PrintedDef() != def || len(tc.LockedDoors()) != 2 || tc.Def.Name != "" {
		t.Fatalf("token copy %q locked %v, want an empty Room over the printed card", tc.Def.Name, tc.LockedDoors())
	}
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	if !g.UnlockDoor(p, made, engine.DoorLeft, c) {
		t.Error("the token copy's left door could not be unlocked")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	host := shifter(t, g, p, "Clone | ValidTgts$ Enchantment")
	if err := activate(g, p, c, host, room); err == nil || !strings.Contains(err.Error(), "copy of a Room not resolvable yet") {
		t.Errorf("Clone of a Room: err %v, want it rejected", err)
	}
}
