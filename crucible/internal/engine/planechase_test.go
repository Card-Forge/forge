package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// planarDef is a Plane or Phenomenon card named name carrying triggers and
// replacements, each trigger's Execute$ naming one of svars' SVars.
func planarDef(t *testing.T, name, typeLine string, triggers, replacements []string, svars ...string) *compile.Card {
	t.Helper()
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, typeLine)
	raw.Faces[0].Triggers = triggers
	raw.Faces[0].Replacements = replacements
	for i := 0; i+1 < len(svars); i += 2 {
		raw.Faces[0].SVars.Set(svars[i], svars[i+1])
	}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// plainPlane is a Plane with no abilities.
func plainPlane(t *testing.T, name string) *compile.Card {
	t.Helper()
	return planarDef(t, name, "Plane", nil, nil)
}

// planarDeck seats defs as p's planar deck, first on top.
func planarDeck(g *engine.Game, p engine.PlayerID, defs ...*compile.Card) []engine.CardID {
	ids := make([]engine.CardID, len(defs))
	for i, d := range defs {
		ids[i] = g.NewCard(d, p, engine.PlanarDeck)
	}
	return ids
}

// faceUpPlane puts plane face up in p's Command zone as the active plane.
func faceUpPlane(g *engine.Game, p engine.PlayerID, def *compile.Card) engine.CardID {
	id := g.NewCard(def, p, engine.Command)
	g.SetActivePlane(id)
	return id
}

func zoneOf(t *testing.T, g *engine.Game, id engine.CardID) []engine.CardID {
	t.Helper()
	c := g.Card(id)
	return g.Zone(c.Zone, c.ZoneOwner).Cards()
}

// TestPlaneswalkIsANoOpOutsidePlanechase proves PlaneswalkEffect.java:23:
// with no planar deck seated the game is not a Planechase game and nothing
// happens -- Optional$ is not even asked.
func TestPlaneswalkIsANoOpOutsidePlanechase(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Planeswalk | Optional$ True")
	if g.PlanechaseActive() {
		t.Error("PlanechaseActive with no planar deck")
	}
	if got := g.ActivePlane(); got != engine.NoCard {
		t.Errorf("ActivePlane = %v, want NoCard", got)
	}
}

// TestPlaneswalkReplacementOutsidePlanechaseIsInert proves the Planechase
// gate runs before the replacement check (PlaneswalkEffect.java:23 before
// :32): Susan Foreman beside TARDIS in a normal game neither errors nor
// moves anything.
func TestPlaneswalkReplacementOutsidePlanechaseIsInert(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(planarDef(t, "Susan Test", "Creature Elf", nil, []string{
		"Event$ Planeswalk | ActiveZones$ Battlefield | ValidPlayer$ You | ReplaceWith$ Scry",
	}, "Scry", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)
	resolveLine(t, g, p, c, "DB$ Planeswalk | Optional$ True")
	if got := g.ActivePlane(); got != engine.NoCard {
		t.Errorf("ActivePlane = %v, want NoCard", got)
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("life = %d, want 20", got)
	}
}

// TestPlaneswalkTurnsTheTopPlaneFaceUp proves the first planeswalk of a game
// with no active plane yet: the top of the activator's planar deck moves to
// their Command zone and becomes the active plane.
func TestPlaneswalkTurnsTheTopPlaneFaceUp(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	deck := planarDeck(g, p, plainPlane(t, "Top"), plainPlane(t, "Second"))
	if !g.PlanechaseActive() {
		t.Fatal("PlanechaseActive false after seating a planar deck")
	}
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Planeswalk")
	if got := g.ActivePlane(); got != deck[0] {
		t.Fatalf("ActivePlane = %v, want top %v", got, deck[0])
	}
	if c := g.Card(deck[0]); c.Zone != engine.Command || c.ZoneOwner != p {
		t.Errorf("top plane in %v of %v, want p's Command", c.Zone, c.ZoneOwner)
	}
	if got := g.Zone(engine.PlanarDeck, p).Cards(); len(got) != 1 || got[0] != deck[1] {
		t.Errorf("planar deck = %v, want [%v]", got, deck[1])
	}
}

// TestPlaneswalkAwayFiresFromBeforeLeavingAndToAfterArriving proves the
// leave-then-walk order (Player.java:2669-2679, 2649-2664): the old plane's
// "when you planeswalk away from" fires while it is still in the Command
// zone -- its TriggerZones$ Command would miss it otherwise -- then goes to
// the bottom of the planar deck, and the new plane's "when you planeswalk
// to" fires from the Command zone. Neither fires the other's mode.
func TestPlaneswalkAwayFiresFromBeforeLeavingAndToAfterArriving(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	old := faceUpPlane(g, p, planarDef(t, "Old", "Plane", []string{
		"Mode$ PlaneswalkedFrom | ValidCard$ Plane.Self | TriggerZones$ Command | Execute$ Away",
		"Mode$ PlaneswalkedTo | ValidCard$ Card.Self | Execute$ Here",
	}, nil,
		"Away", "DB$ GainLife | Defined$ You | LifeAmount$ 1",
		"Here", "DB$ GainLife | Defined$ You | LifeAmount$ 100"))
	deck := planarDeck(g, p, planarDef(t, "New", "Plane", []string{
		"Mode$ PlaneswalkedTo | ValidCard$ Card.Self | Execute$ Here",
		"Mode$ PlaneswalkedFrom | ValidCard$ Plane.Self | Execute$ Away",
	}, nil,
		"Here", "DB$ GainLife | Defined$ You | LifeAmount$ 10",
		"Away", "DB$ GainLife | Defined$ You | LifeAmount$ 1000"),
		plainPlane(t, "Under"))

	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Planeswalk")
	if got := g.Player(p).Life; got != 31 {
		t.Errorf("life = %d, want 31 (away 1 + here 10)", got)
	}
	if got := g.ActivePlane(); got != deck[0] {
		t.Errorf("ActivePlane = %v, want %v", got, deck[0])
	}
	want := []engine.CardID{deck[1], old}
	got := g.Zone(engine.PlanarDeck, p).Cards()
	if len(got) != 2 || got[0] != want[0] || got[1] != want[1] {
		t.Errorf("planar deck = %v, want %v (old plane on the bottom)", got, want)
	}
}

// TestPlaneswalkReturnsAnotherPlayersPlaneToItsOwner proves
// PlaneswalkEffect.java:39-43's every-player loop: the active plane belongs
// to the other player's Command zone, so it is theirs that leaves -- to the
// bottom of its owner's planar deck -- and its away trigger is theirs to
// control; the activator walks to the top of their own deck.
func TestPlaneswalkReturnsAnotherPlayersPlaneToItsOwner(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	theirs := faceUpPlane(g, other, planarDef(t, "Theirs", "Plane", []string{
		"Mode$ PlaneswalkedFrom | ValidCard$ Plane.Self | Execute$ Away",
	}, nil, "Away", "DB$ GainLife | Defined$ You | LifeAmount$ 5"))
	planarDeck(g, other, plainPlane(t, "Their Next"))
	mine := planarDeck(g, p, plainPlane(t, "Mine"))

	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Planeswalk")
	if got := g.ActivePlane(); got != mine[0] {
		t.Errorf("ActivePlane = %v, want p's own top %v", got, mine[0])
	}
	if c := g.Card(theirs); c.Zone != engine.PlanarDeck || c.ZoneOwner != other {
		t.Errorf("their plane in %v of %v, want other's PlanarDeck", c.Zone, c.ZoneOwner)
	}
	if d := zoneOf(t, g, theirs); d[len(d)-1] != theirs {
		t.Errorf("their deck = %v, want %v on the bottom", d, theirs)
	}
	if got := g.Player(other).Life; got != 25 {
		t.Errorf("other's life = %d, want 25 (their plane's away trigger)", got)
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("p's life = %d, want 20", got)
	}
}

// TestPlaneswalkedFromWithoutValidCardFiresOncePerPlayer proves
// leaveCurrentPlane's unconditional runTrigger (Player.java:2670-2672): a
// line with no ValidCard$ fires for every player in the game, plane or
// not, while a ValidCard$ line never matches a player's empty plane list.
func TestPlaneswalkedFromWithoutValidCardFiresOncePerPlayer(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	faceUpPlane(g, p, plainPlane(t, "Here"))
	planarDeck(g, p, plainPlane(t, "Next"))
	g.NewCard(planarDef(t, "Watcher", "Creature Elf", []string{
		"Mode$ PlaneswalkedFrom | Execute$ Any",
		"Mode$ PlaneswalkedFrom | ValidCard$ Plane | Execute$ Plane",
	}, nil,
		"Any", "DB$ GainLife | Defined$ You | LifeAmount$ 1",
		"Plane", "DB$ GainLife | Defined$ You | LifeAmount$ 100"), p, engine.Battlefield)

	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Planeswalk")
	if got := g.Player(p).Life; got != 122 {
		t.Errorf("life = %d, want 122 (no-ValidCard$ line twice, ValidCard$ Plane once)", got)
	}
}

// TestPlanesInThePlanarDeckDoNotTrigger proves the host walk covers the
// Battlefield and Command zones only: a face-down plane in the planar deck
// is not an active trigger source, whatever its TriggerZones$.
func TestPlanesInThePlanarDeckDoNotTrigger(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	planarDeck(g, p, plainPlane(t, "Top"), planarDef(t, "Buried", "Plane", []string{
		"Mode$ PlaneswalkedTo | Execute$ Gain",
	}, nil, "Gain", "DB$ GainLife | Defined$ You | LifeAmount$ 7"))

	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Planeswalk")
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("life = %d, want 20", got)
	}
}

// TestEncounteringAPhenomenonWalksOnToTheNextPlane proves the corpus's
// dominant chain: a phenomenon's "when you encounter" trigger ends in its
// own DB$ Planeswalk, which sends the phenomenon to the bottom and turns up
// the next plane.
func TestEncounteringAPhenomenonWalksOnToTheNextPlane(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	deck := planarDeck(g, p, planarDef(t, "Mutual Test", "Phenomenon", []string{
		"Mode$ PlaneswalkedTo | ValidCard$ Card.Self | Execute$ Epiphany",
	}, nil,
		"Epiphany", "DB$ GainLife | Defined$ You | LifeAmount$ 4 | SubAbility$ PWAway",
		"PWAway", "DB$ Planeswalk"),
		plainPlane(t, "Destination"))

	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Planeswalk")
	if got := g.ActivePlane(); got != deck[1] {
		t.Errorf("ActivePlane = %v, want the plane after the phenomenon %v", got, deck[1])
	}
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life = %d, want 24", got)
	}
	if got := g.Zone(engine.PlanarDeck, p).Cards(); len(got) != 1 || got[0] != deck[0] {
		t.Errorf("planar deck = %v, want [%v]", got, deck[0])
	}
}

// TestStaticPlaneswalkedFromResolvesInline proves a Static$ True line runs at
// the trigger site (ADR-0020), before the stack: Chaotic Aether's effect
// card exiles itself as a player planeswalks away from a plane.
func TestStaticPlaneswalkedFromResolvesInline(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	faceUpPlane(g, p, plainPlane(t, "Here"))
	planarDeck(g, p, plainPlane(t, "There"))
	eff := g.NewCard(planarDef(t, "Aether Effect", "Effect", []string{
		"Mode$ PlaneswalkedFrom | ValidCard$ Plane | Execute$ ExileSelf | Static$ True",
	}, nil, "ExileSelf", "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile"), p, engine.Command)
	g.Card(eff).IsEffect = true

	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Planeswalk")
	// An effect card leaving the Command zone ceases to exist.
	if z := g.Card(eff).Zone; z == engine.Command {
		t.Errorf("effect still in %v, want it exiled", z)
	}
}

// TestPlaneswalkOptionalAsksTheActivator proves Optional$
// (PlaneswalkEffect.java:27-30): a declined confirmation leaves every plane
// where it is; an accepted one walks.
func TestPlaneswalkOptionalAsksTheActivator(t *testing.T) {
	t.Parallel()

	for _, confirm := range []bool{false, true} {
		g, p, _ := newTwoPlayerGame(t)
		here := faceUpPlane(g, p, plainPlane(t, "Here"))
		deck := planarDeck(g, p, plainPlane(t, "There"))
		c := engine.NewScriptedController()
		c.QueueConfirmEffect(confirm)
		resolveLine(t, g, p, c, "DB$ Planeswalk | Optional$ True")
		want := here
		if confirm {
			want = deck[0]
		}
		if got := g.ActivePlane(); got != want {
			t.Errorf("confirm=%v: ActivePlane = %v, want %v", confirm, got, want)
		}
	}
}

// TestPlaneswalkAcceptsCause proves the planar die's own synthetic line
// (Player.java:3272) resolves: Cause$ only feeds a Planeswalk replacement.
func TestPlaneswalkAcceptsCause(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	deck := planarDeck(g, p, plainPlane(t, "There"))
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Planeswalk | Cause$ PlanarDie")
	if got := g.ActivePlane(); got != deck[0] {
		t.Errorf("ActivePlane = %v, want %v", got, deck[0])
	}
}

// TestPlaneswalkRejectsUnportedShapes proves Defined$ and
// DontPlaneswalkAway$ -- both needing more than one concurrent active plane
// -- and a live Event$ Planeswalk replacement are refused before anything
// moves, as is a planeswalk with nothing left to walk to.
func TestPlaneswalkRejectsUnportedShapes(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, line, want string
		replacement      bool
		emptyDeck        bool
	}{
		{"defined", "DB$ Planeswalk | Defined$ Remembered", "Defined$ not resolvable yet", false, false},
		{"dont walk away", "DB$ Planeswalk | DontPlaneswalkAway$ True", "DontPlaneswalkAway$ not resolvable yet", false, false},
		{"replacement", "DB$ Planeswalk", "Event$ Planeswalk replacement not resolvable yet", true, false},
		{"empty deck", "DB$ Planeswalk", "planar deck is empty", false, true},
	} {
		g, p, other := newTwoPlayerGame(t)
		here := faceUpPlane(g, other, plainPlane(t, "Here"))
		if tc.emptyDeck {
			planarDeck(g, other, plainPlane(t, "Theirs"))
		} else {
			planarDeck(g, p, plainPlane(t, "There"))
		}
		if tc.replacement {
			g.NewCard(planarDef(t, "Susan Test", "Creature Elf", nil, []string{
				"Event$ Planeswalk | ActiveZones$ Battlefield | ValidPlayer$ You | ReplaceWith$ Scry",
			}, "Scry", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), other, engine.Battlefield)
		}
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want %q", tc.name, err, tc.want)
		}
		if got := g.ActivePlane(); got != here {
			t.Errorf("%s: ActivePlane = %v, want unchanged %v", tc.name, got, here)
		}
		if z := g.Card(here).Zone; z != engine.Command {
			t.Errorf("%s: active plane moved to %v", tc.name, z)
		}
	}
}

// TestPlaneswalkDeckRefilledByTheLeavingPlane proves the empty-deck refusal
// counts the activator's own leaving plane: it goes to the bottom first, so
// it is also the top card walked straight back to.
func TestPlaneswalkDeckRefilledByTheLeavingPlane(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	only := faceUpPlane(g, p, plainPlane(t, "Only"))
	g.NewCard(plainPlane(t, "Seat"), p, engine.PlanarDeck) // marks the game Planechase
	g.Move(g.Zone(engine.PlanarDeck, p).Cards()[0], engine.Exile, p)

	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Planeswalk")
	if got := g.ActivePlane(); got != only {
		t.Errorf("ActivePlane = %v, want %v walked back to", got, only)
	}
	if c := g.Card(only); c.Zone != engine.Command {
		t.Errorf("plane in %v, want Command", c.Zone)
	}
}

// TestClonePlanechaseState proves Game.Clone carries the active plane and
// the Planechase flag, and the copy moves independently.
func TestClonePlanechaseState(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	here := faceUpPlane(g, p, plainPlane(t, "Here"))
	deck := planarDeck(g, p, plainPlane(t, "There"))
	cl := g.Clone()
	if !cl.PlanechaseActive() || cl.ActivePlane() != here {
		t.Fatalf("clone: PlanechaseActive=%v ActivePlane=%v, want true/%v", cl.PlanechaseActive(), cl.ActivePlane(), here)
	}
	resolveLine(t, cl, p, engine.NewScriptedController(), "DB$ Planeswalk")
	if cl.ActivePlane() != deck[0] || g.ActivePlane() != here {
		t.Errorf("clone walked to %v, original at %v; want %v and %v", cl.ActivePlane(), g.ActivePlane(), deck[0], here)
	}
}

// TestPlaneswalkSkipsPlayersWhoHaveLost proves the leave loop walks
// game.getPlayers(), which no longer holds a player who has lost: a
// no-ValidCard$ away trigger fires once per player still in the game.
func TestPlaneswalkSkipsPlayersWhoHaveLost(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b", "c")
	ps := g.Players()
	p := ps[0]
	g.SetTurnState(1, p, engine.Main1)
	for _, id := range ps {
		g.Player(id).Life = 20
	}
	g.Player(ps[2]).Lost = true
	planarDeck(g, p, plainPlane(t, "There"))
	g.NewCard(planarDef(t, "Watcher", "Creature Elf", []string{
		"Mode$ PlaneswalkedFrom | Execute$ Any",
	}, nil, "Any", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)

	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Planeswalk")
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22 (two players still in the game)", got)
	}
}

// TestPlaneswalkStopsOnAStaticTriggerError proves a Static$ True line that
// cannot resolve fails the planeswalk at its trigger site rather than
// being dropped (GO-7), on either side of the walk.
func TestPlaneswalkStopsOnAStaticTriggerError(t *testing.T) {
	t.Parallel()

	for _, mode := range []string{"PlaneswalkedFrom", "PlaneswalkedTo"} {
		g, p, other := newTwoPlayerGame(t)
		planarDeck(g, p, plainPlane(t, "There"))
		eff := g.NewCard(planarDef(t, "Bad Effect", "Effect", []string{
			"Mode$ " + mode + " | Execute$ Hit | Static$ True",
		}, nil, "Hit", "DB$ DealDamage | ValidTgts$ Player | NumDmg$ 1"), other, engine.Command)
		g.Card(eff).IsEffect = true
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ Planeswalk")
		if err == nil || !strings.Contains(err.Error(), "ValidTgts$ not resolvable yet") {
			t.Errorf("%s: err = %v, want the static trigger's refusal", mode, err)
		}
	}
}
