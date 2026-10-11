package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// triggerWatcherDef is an Enchantment carrying one T: line and the SVars
// it names, for the designation trigger modes (BecomeMonarch,
// TakesInitiative, DungeonCompleted).
func triggerWatcherDef(t *testing.T, name, trigger string, svars ...string) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Enchantment")
	raw.Faces[0].Triggers = []string{trigger}
	for i := 0; i+1 < len(svars); i += 2 {
		raw.Faces[0].SVars.Set(svars[i], svars[i+1])
	}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// namedIn is the cards named name in p's zone z.
func namedIn(g *engine.Game, z engine.ZoneType, p engine.PlayerID, name string) []engine.CardID {
	var out []engine.CardID
	for _, id := range g.Zone(z, p).Cards() {
		if c := g.Card(id); c.Def != nil && c.Def.Name == name {
			out = append(out, id)
		}
	}
	return out
}

// TestBecomeMonarchDrawsAtTheMonarchsEndStep proves the dominant shape
// (51 of 64 corpus lines, a bare "DB$ BecomeMonarch"): the activator becomes
// the monarch, "The Monarch" enters their Command zone as an effect card,
// and its Phase trigger draws them a card at the beginning of their end
// step -- and not at the opponent's (ValidPlayer$ You).
func TestBecomeMonarchDrawsAtTheMonarchsEndStep(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ BecomeMonarch")
	if g.Monarch() != p {
		t.Fatalf("Monarch = %v, want %v", g.Monarch(), p)
	}
	cards := namedIn(g, engine.Command, p, "The Monarch")
	if len(cards) != 1 || !g.Card(cards[0]).IsEffect {
		t.Fatalf("p's Command zone = %v, want one The Monarch effect card", g.Zone(engine.Command, p).Cards())
	}

	top := libraryCards(t, g, p, 1)[0]
	otherTop := libraryCards(t, g, other, 1)[0]
	c := engine.NewScriptedController()
	g.SetTurnState(1, p, engine.Main2)
	g.AdvancePhase(c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(top).Zone != engine.Hand {
		t.Errorf("monarch's top card zone = %v, want Hand after their end step", g.Card(top).Zone)
	}

	g.SetTurnState(2, other, engine.Main2)
	g.AdvancePhase(c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(otherTop).Zone != engine.Library {
		t.Errorf("opponent's top card zone = %v, want Library -- the monarch draws only at their own end step", g.Card(otherTop).Zone)
	}

	endTurn(g, 2, other)
	if len(namedIn(g, engine.Command, p, "The Monarch")) != 1 {
		t.Error("The Monarch left the Command zone at cleanup, want it to stay: its duration is the designation")
	}
}

// TestMonarchPassesToTheControllerOfACreatureDealingCombatDamage proves
// The Monarch's second trigger (CR 724.2): combat damage dealt to the
// monarch by a creature makes that creature's controller -- read when the
// damage was dealt (Defined$ TriggeredSourceController) -- the monarch; the
// old monarch's card leaves the Command zone and the new one's enters.
func TestMonarchPassesToTheControllerOfACreatureDealingCombatDamage(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ BecomeMonarch")
	old := namedIn(g, engine.Command, p, "The Monarch")[0]

	g.SetTurnState(2, other, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)
	g.DealCombatDamage(c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	if g.Monarch() != other {
		t.Fatalf("Monarch = %v, want the attacker's controller %v", g.Monarch(), other)
	}
	if z := g.Card(old).Zone; z != engine.None {
		t.Errorf("old monarch's card zone = %v, want None", z)
	}
	if len(namedIn(g, engine.Command, other, "The Monarch")) != 1 {
		t.Error("new monarch has no The Monarch card in their Command zone")
	}
}

// TestBecomeMonarchTargetsAndReusesEachPlayersCard proves ValidTgts$ (4
// corpus lines) and the one-card-per-player shape Player.monarchEffect
// has: taking the monarchy back returns the same card to the Command zone.
func TestBecomeMonarchTargetsAndReusesEachPlayersCard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ BecomeMonarch")
	mine := namedIn(g, engine.Command, p, "The Monarch")[0]

	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	resolveLine(t, g, p, c, "DB$ BecomeMonarch | ValidTgts$ Opponent")
	if g.Monarch() != other {
		t.Fatalf("Monarch = %v, want the targeted opponent", g.Monarch())
	}
	if g.Card(mine).Zone != engine.None {
		t.Errorf("p's card zone = %v, want None", g.Card(mine).Zone)
	}

	resolveLine(t, g, p, c, "DB$ BecomeMonarch | Defined$ You")
	if got := namedIn(g, engine.Command, p, "The Monarch"); len(got) != 1 || got[0] != mine {
		t.Errorf("p's Command zone monarch cards = %v, want the same card %v back", got, mine)
	}
	if len(namedIn(g, engine.Command, other, "The Monarch")) != 0 {
		t.Error("opponent still holds a The Monarch card")
	}

	// Becoming the monarch again while already the monarch changes nothing.
	resolveLine(t, g, p, c, "DB$ BecomeMonarch")
	if n := len(g.Zone(engine.Command, p).Cards()); n != 1 {
		t.Errorf("p's Command zone holds %d cards, want 1", n)
	}
}

// TestCantBecomeMonarchStaticStopsIt proves Jared Carthalion's shape: an
// Effect card carrying Mode$ CantBecomeMonarch | ValidPlayer$ You keeps its
// controller from becoming the monarch, while an opponent still can.
func TestCantBecomeMonarchStaticStopsIt(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ Effect | StaticAbilities$ STCant",
		"STCant", "Mode$ CantBecomeMonarch | ValidPlayer$ You | Description$ You can't become the monarch this turn.")
	resolveLine(t, g, p, c, "DB$ BecomeMonarch")
	if g.Monarch() != engine.NoPlayer {
		t.Fatalf("Monarch = %v, want nobody", g.Monarch())
	}
	resolveLine(t, g, p, c, "DB$ BecomeMonarch | Defined$ Opponent")
	if g.Monarch() != other {
		t.Errorf("Monarch = %v, want the opponent, whom the static does not name", g.Monarch())
	}
}

// TestBecomeMonarchTriggerMode proves Mode$ BecomeMonarch: ValidPlayer$
// against the new monarch, BeginTurn$ against who was the monarch as the
// turn began, and Defined$ TriggeredPlayer naming the new monarch.
func TestBecomeMonarchTriggerMode(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	g.NewCard(triggerWatcherDef(t, "Mine",
		"Mode$ BecomeMonarch | ValidPlayer$ You | TriggerZones$ Battlefield | Execute$ TrigGain",
		"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Drain",
		"Mode$ BecomeMonarch | ValidPlayer$ Opponent | BeginTurn$ You | TriggerZones$ Battlefield | Execute$ TrigDrain",
		"TrigDrain", "DB$ LoseLife | Defined$ TriggeredPlayer | LifeAmount$ 2"), p, engine.Battlefield)

	resolveLine(t, g, p, c, "DB$ BecomeMonarch")
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("p life = %d, want 21 (ValidPlayer$ You fires once)", got)
	}

	// Nobody was the monarch as this turn began: BeginTurn$ You fails.
	resolveLine(t, g, p, c, "DB$ BecomeMonarch | Defined$ Opponent")
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("opponent life = %d, want 20 -- BeginTurn$ You needs p to have been monarch at turn start", got)
	}

	// A new turn begins with p as the monarch; now the drain fires.
	resolveLine(t, g, p, c, "DB$ BecomeMonarch")
	g.SetTurnState(1, p, engine.Cleanup)
	g.AdvancePhase(c)
	g.SetTurnState(2, other, engine.Main1)
	resolveLine(t, g, other, c, "DB$ BecomeMonarch")
	if got := g.Player(other).Life; got != 18 {
		t.Errorf("opponent life = %d, want 18 -- TriggeredPlayer loses 2", got)
	}
}

// TestMonarchPassesWhenTheMonarchLoses proves CR 724.4 (Game.onPlayerLost):
// a non-active monarch who loses passes the monarchy to the active player;
// an active monarch who loses passes it to the next player in turn order.
func TestMonarchPassesWhenTheMonarchLoses(t *testing.T) {
	t.Parallel()

	g := engine.NewGame(nil, javarand.New(1), []string{"a", "b", "c"})
	a, b, cc := g.Players()[0], g.Players()[1], g.Players()[2]
	for _, pid := range g.Players() {
		g.Player(pid).Life = 20
	}
	g.SetTurnState(1, a, engine.Main1)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(b)})
	resolveLine(t, g, a, c, "DB$ BecomeMonarch | ValidTgts$ Player")

	g.Player(b).Life = 0
	engine.CheckStateBasedActions(g, c)
	if g.Over() || g.Monarch() != a {
		t.Fatalf("over=%v monarch=%v, want the game going on with the active player %v as monarch", g.Over(), g.Monarch(), a)
	}

	g.Player(a).Life = 0
	engine.CheckStateBasedActions(g, c)
	if g.Monarch() != cc {
		t.Errorf("Monarch = %v, want %v, next in turn order after the active player who lost", g.Monarch(), cc)
	}
}

// TestMonarchSurvivesClone proves Game.Clone copies the designation and
// leaves the clone independent.
func TestMonarchSurvivesClone(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ BecomeMonarch")
	clone := g.Clone()
	if clone.Monarch() != p {
		t.Fatalf("clone Monarch = %v, want %v", clone.Monarch(), p)
	}
	resolveLine(t, clone, p, c, "DB$ BecomeMonarch | Defined$ Opponent")
	if g.Monarch() != p || clone.Monarch() != other {
		t.Errorf("monarch original=%v clone=%v, want %v and %v", g.Monarch(), clone.Monarch(), p, other)
	}
}

// TestSetMonarchRestoresTheDesignation proves the fixture loader's
// SetMonarch: the designation and its card move with no trigger, and
// NoPlayer clears both.
func TestSetMonarchRestoresTheDesignation(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Mine",
		"Mode$ BecomeMonarch | ValidPlayer$ Player | Execute$ TrigGain",
		"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	g.SetMonarch(p)
	g.SetMonarch(other)
	card := namedIn(g, engine.Command, other, "The Monarch")
	if g.Monarch() != other || len(card) != 1 || !g.IsDesignationCard(card[0]) {
		t.Fatalf("monarch=%v cards=%v, want other holding The Monarch", g.Monarch(), card)
	}
	if len(namedIn(g, engine.Command, p, "The Monarch")) != 0 || g.Player(p).Life != 20 {
		t.Errorf("p still holds a card or a trigger ran (life %d)", g.Player(p).Life)
	}
	g.SetMonarch(engine.NoPlayer)
	if g.Monarch() != engine.NoPlayer || g.Card(card[0]).Zone != engine.None {
		t.Errorf("after clearing: monarch=%v card zone=%v, want nobody and None", g.Monarch(), g.Card(card[0]).Zone)
	}
	if g.IsDesignationCard(engine.NoCard) {
		t.Error("IsDesignationCard(NoCard) = true")
	}
}

// TestBecomeMonarchFailsClosed proves the rejected shapes error before
// acting: Defined$ TriggeredTarget, a Defined$
// Triggered* no trigger recorded, and a CantBecomeMonarch static carrying a
// param it cannot read.
func TestBecomeMonarchFailsClosed(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"DB$ BecomeMonarch | Defined$ TriggeredTarget",
		"DB$ BecomeMonarch | Defined$ TriggeredSourceController",
		"DB$ BecomeMonarch | Defined$ TriggeredSource",
		"DB$ BecomeMonarch | Defined$ TriggeredPlayer",
	} {
		g, p, _ := newTwoPlayerGame(t)
		if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, line); err == nil {
			t.Errorf("%q: err = nil, want an error", line)
		}
		if g.Monarch() != engine.NoPlayer {
			t.Errorf("%q: Monarch = %v, want nobody", line, g.Monarch())
		}
	}

	g, p, _ := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Effect | StaticAbilities$ STCant",
		"STCant", "Mode$ CantBecomeMonarch | ValidPlayer$ You | IsPresent$ Creature | Description$ x")
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ BecomeMonarch")
	if err == nil || !strings.Contains(err.Error(), "IsPresent") {
		t.Errorf("err = %v, want the static's IsPresent$ rejected", err)
	}
}

// TestCantBecomeMonarchStaticShapes proves the static's other two readings:
// no ValidPlayer$ stops every player, and a ValidPlayer$ this port cannot
// read is an error rather than a guess.
func TestCantBecomeMonarchStaticShapes(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ Effect | StaticAbilities$ STCant", "STCant", "Mode$ CantBecomeMonarch | Description$ Nobody.")
	resolveLine(t, g, p, c, "DB$ BecomeMonarch | Defined$ Player")
	if g.Monarch() != engine.NoPlayer {
		t.Errorf("Monarch = %v, want nobody -- a ValidPlayer$-less static names every player", g.Monarch())
	}

	g, p, _ = newTwoPlayerGame(t)
	resolveLine(t, g, p, c, "DB$ Effect | StaticAbilities$ STCant", "STCant", "Mode$ CantBecomeMonarch | ValidPlayer$ Player.Chosen")
	if _, err := resolveNow(t, g, p, c, nil, "DB$ BecomeMonarch"); err == nil {
		t.Error("err = nil, want an unreadable ValidPlayer$ rejected")
	}
}

// TestBecomeMonarchSkipsAPlayerWhoHasLost proves BecomeMonarchEffect's
// isInGame check: a targeted player who has lost is passed over.
func TestBecomeMonarchSkipsAPlayerWhoHasLost(t *testing.T) {
	t.Parallel()

	g := engine.NewGame(nil, javarand.New(1), []string{"a", "b", "c"})
	a, b := g.Players()[0], g.Players()[1]
	for _, pid := range g.Players() {
		g.Player(pid).Life = 20
	}
	g.SetTurnState(1, a, engine.Main1)
	g.Player(b).Lost = true
	c := engine.NewScriptedController()
	if err := resolveTargeting(t, g, a, c, []engine.EntityID{engine.PlayerEntity(b)}, "DB$ BecomeMonarch | ValidTgts$ Player"); err != nil {
		t.Fatalf("BecomeMonarch: %v", err)
	}
	if g.Monarch() != engine.NoPlayer {
		t.Errorf("Monarch = %v, want nobody -- the target has lost", g.Monarch())
	}
}
