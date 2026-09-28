package fixture_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/fixture"
)

// roomDB is a database holding one vanilla Room, Left Hall // Right Hall,
// under its primary face's name.
func roomDB(t *testing.T) *compile.DB {
	t.Helper()
	typ := cardtype.ParseToken("Enchantment").Union(cardtype.ParseToken("Room"))
	raw := &carddb.Card{Filename: "left_hall_right_hall", SplitType: carddb.SplitSplit}
	raw.Faces[0] = carddb.Face{Present: true, Name: "Left Hall", Type: typ}
	raw.Faces[carddb.FaceAlternate] = carddb.Face{Present: true, Name: "Right Hall", Type: typ}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile room: %v", err)
	}
	return compile.NewDB(map[string]*compile.Card{"Left Hall": c})
}

// UnlockedRoom: (GameState.java:1419/:445) loads a battlefield Room's
// unlocked doors and dumps them back last, left then right, under the
// printed card's name.
func TestRoomUnlockedDoorsRoundTrip(t *testing.T) {
	t.Parallel()

	l := load(t, roomDB(t), "humanlife=20\nailife=20\nhumanbattlefield=Left Hall|Id:1|UnlockedRoom:RightSplit|UnlockedRoom:LeftSplit\n")
	room := l.CardByFixtureID[1]
	if got := l.Game.Card(room).UnlockedDoors(); len(got) != 2 {
		t.Fatalf("unlocked %v, want both doors", got)
	}
	var buf strings.Builder
	if err := fixture.Write(&buf, fixture.Dump(l)); err != nil {
		t.Fatalf("Write: %v", err)
	}
	if want := "Left Hall|Id:1|UnlockedRoom:LeftSplit|UnlockedRoom:RightSplit"; !strings.Contains(buf.String(), want) {
		t.Errorf("dump %q, want it to contain %q", buf.String(), want)
	}
}

// UnlockedRoom: off the battlefield is recorded unapplied, and a value that
// names no door is an error.
func TestRoomUnlockedDoorsOffTheBattlefieldOrMisspelled(t *testing.T) {
	t.Parallel()

	l := load(t, roomDB(t), "humanlife=20\nailife=20\nhumanhand=Left Hall|UnlockedRoom:LeftSplit\n")
	if len(l.Unapplied) != 1 || !strings.Contains(l.Unapplied[0], "UnlockedRoom:LeftSplit") {
		t.Errorf("unapplied %v, want the hand card's UnlockedRoom:", l.Unapplied)
	}
	st, err := fixture.Parse(strings.NewReader("humanlife=20\nailife=20\nhumanbattlefield=Left Hall|UnlockedRoom:Original\n"))
	if err != nil {
		t.Fatalf("Parse: %v", err)
	}
	if _, err := fixture.Load(st, roomDB(t), nil); err == nil {
		t.Error("UnlockedRoom:Original loaded")
	}
}

// The Room action and queue verbs parse doors in CardStateName spelling
// and reject anything else.
func TestRoomActionVerbs(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"queue action human cast 1 Original",
		"queue action human unlockdoor 1",
		"queue action human unlockdoor 1 Middle",
		"queue roomdoor Middle",
	} {
		l := load(t, roomDB(t), "humanlife=20\nailife=20\nhumanbattlefield=Left Hall|Id:1\n")
		if err := runActions(t, l, engine.NewScriptedController(), line+"\n"); err == nil {
			t.Errorf("%q did not error", line)
		}
	}
	l := load(t, roomDB(t), "humanlife=20\nailife=20\nhumanbattlefield=Left Hall|Id:1\n")
	c := engine.NewScriptedController()
	if err := runActions(t, l, c, "queue roomdoor RightSplit\nqueue action human unlockdoor 1 LeftSplit\n"); err != nil {
		t.Fatalf("runActions: %v", err)
	}
	if got := c.ChooseRoomDoor(l.Game, l.Game.Players()[0], l.CardByFixtureID[1], nil); got != engine.DoorRight {
		t.Errorf("queued door %v, want RightSplit", got)
	}
	if got := c.TakeAction(l.Game, l.Game.Players()[0]); got.Kind != engine.ActionUnlockDoor || got.Door != engine.DoorLeft {
		t.Errorf("queued action %+v, want unlockdoor LeftSplit", got)
	}
}
