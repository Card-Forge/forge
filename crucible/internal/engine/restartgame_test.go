package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// karnUltimate and karnExile are Karn Liberated's own ability indexes
// (karn_liberated.txt: +4, -3, -14).
const (
	karnExile    = 1
	karnUltimate = 2
)

// karnGame is a two-player Main1 game on the real corpus with Karn
// Liberated on the battlefield of the seat-th player, loyal enough to
// activate everything, and eight Grizzly Bears in each library so the
// restarted game can deal full opening hands. It returns the game, Karn's
// controller, the other player and Karn.
func karnGame(t *testing.T, seat int) (*engine.Game, engine.PlayerID, engine.PlayerID, engine.CardID) {
	t.Helper()
	g, a, b := newTwoPlayerGameOn(t, scenarioDB(t))
	p, other := a, b
	if seat == 1 {
		p, other = b, a
	}
	g.SetTurnState(1, p, engine.Main1)
	karn := g.NewCard(corpusCard(t, "Karn Liberated"), p, engine.Battlefield)
	g.Card(karn).Counters.Add(engine.Loyalty, 30)
	for _, pid := range []engine.PlayerID{p, other} {
		for i := 0; i < 8; i++ {
			g.NewCard(corpusCard(t, "Grizzly Bears"), pid, engine.Library)
		}
	}
	return g, p, other, karn
}

// activateKarn activates Karn's index'th ability for p, targeting targets,
// and resolves the stack, then clears the once-per-turn loyalty mark so a
// test can activate again in the same turn.
func activateKarn(t *testing.T, g *engine.Game, p engine.PlayerID, karn engine.CardID, index int, c *engine.ScriptedController, targets ...engine.CardID) {
	t.Helper()
	if len(targets) > 0 {
		var ents []engine.EntityID
		for _, id := range targets {
			ents = append(ents, engine.CardEntity(id))
		}
		c.QueueTargets(ents)
	}
	if !g.ActivateAbility(p, karn, index, c) {
		t.Fatalf("activating Karn's ability %d failed", index)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("resolving Karn's ability %d: %v", index, err)
	}
	g.Card(karn).LoyaltyAbilityActivated = false
}

func inZone(g *engine.Game, id engine.CardID, z engine.ZoneType, owner engine.PlayerID) bool {
	c := g.Card(id)
	return c.Zone == z && c.ZoneOwner == owner
}

// TestRestartGameKarnLiberatedUltimate proves the one real corpus line end
// to end (karn_liberated.txt:7, ADR-0034): Karn's -3 exiles a permanent
// with Karn; the -14 shuffles every other card of every restart zone into
// its owner's library -- Karn itself, a card exiled by something else, and
// an instant and an Aura exiled with Karn (RestrictFromValid$'s Spell and
// Card.Aura alternatives) -- clears monarch, initiative, day/night, the
// Command zone, life and player counters, and marks the game restarted
// for Karn's controller; then SubAbility$ ReturnFromExile puts the exiled
// permanent onto the battlefield under Karn's controller against that
// reset state.
func TestRestartGameKarnLiberatedUltimate(t *testing.T) {
	t.Parallel()

	g, p, other, karn := karnGame(t, 0)
	c := engine.NewScriptedController()
	bear := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Battlefield)
	forest := g.NewCard(corpusCard(t, "Forest"), other, engine.Battlefield)
	hand := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	grave := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Graveyard)
	bolt := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Exile)
	aura := g.NewCard(corpusCard(t, "Pacifism"), other, engine.Exile)
	stranger := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Exile)
	g.SetExiledWith(bolt, karn)
	g.SetExiledWith(aura, karn)

	takeInitiativeNow(t, g, p, c)
	g.SetMonarch(other)
	if err := resolveWith(t, g, p, c, "DB$ DayTime | Value$ Day"); err != nil {
		t.Fatal(err)
	}
	g.Player(p).Life = 7
	g.Player(other).Counters.Add(engine.Poison, 3)
	g.Player(p).LandsPlayed = 1

	activateKarn(t, g, p, karn, karnExile, c, bear)
	if got := g.Card(bear).ExiledWith(); got != karn {
		t.Fatalf("Karn's -3 exiled the bear with %v, want Karn %v", got, karn)
	}
	activateKarn(t, g, p, karn, karnUltimate, c)

	if b := g.Card(bear); b.Zone != engine.Battlefield || b.Controller() != p {
		t.Errorf("bear in %v under %v, want returned to the battlefield under %v", b.Zone, b.Controller(), p)
	}
	for _, tc := range []struct {
		name  string
		id    engine.CardID
		owner engine.PlayerID
	}{
		{"Karn", karn, p}, {"forest", forest, other}, {"hand card", hand, p}, {"graveyard card", grave, other},
		{"instant exiled with Karn", bolt, p}, {"Aura exiled with Karn", aura, other},
		{"card exiled by something else", stranger, p},
	} {
		if !inZone(g, tc.id, engine.Library, tc.owner) {
			t.Errorf("%s in %v, want its owner's library", tc.name, g.Card(tc.id).Zone)
		}
	}
	for _, pid := range []engine.PlayerID{p, other} {
		for _, z := range []engine.ZoneType{engine.Hand, engine.Graveyard, engine.Exile, engine.Command} {
			if n := g.Zone(z, pid).Len(); n != 0 {
				t.Errorf("player %v's %v holds %d cards, want none", pid, z, n)
			}
		}
		if life := g.Player(pid).Life; life != 20 {
			t.Errorf("player %v life %d, want the starting 20", pid, life)
		}
	}
	if n := g.Zone(engine.Library, p).Len(); n != 8+4+2 {
		t.Errorf("p's library holds %d cards, want 14 (8 bears, Karn, two exiles, the bolt from hand, the TakeInitiative and DayTime test hosts)", n)
	}
	if g.Monarch() != engine.NoPlayer || g.Initiative() != engine.NoPlayer || g.DayTime() != engine.DayNeither {
		t.Errorf("monarch %v initiative %v daytime %v, want all cleared", g.Monarch(), g.Initiative(), g.DayTime())
	}
	if n := g.Player(other).Counters.Count(engine.Poison); n != 0 {
		t.Errorf("poison %d, want cleared", n)
	}
	if n := g.Player(p).LandsPlayed; n != 0 {
		t.Errorf("lands played %d, want reset", n)
	}
	if !g.Restarted() || g.RestartedBy() != p || g.Turn() != 0 || g.ActivePlayer() != p || g.StackLen() != 0 {
		t.Errorf("restarted %v by %v, turn %d, active %v, stack %d; want true, %v, 0, %v, 0",
			g.Restarted(), g.RestartedBy(), g.Turn(), g.ActivePlayer(), g.StackLen(), p, p)
	}
}

// TestRestartGameCarveOutWhicheverSeatActs proves RestrictFromValid$ is
// evaluated per player after earlier players' cards have already moved
// (RestartGameEffect.java:78-83): with Karn's controller in either seat,
// the opponent-owned permanents Karn exiled -- in the opponent's own exile
// zone -- stay in exile through the reset and come back, even when Karn
// was shuffled away before the opponent's exile was filtered.
func TestRestartGameCarveOutWhicheverSeatActs(t *testing.T) {
	t.Parallel()

	for _, seat := range []int{0, 1} {
		g, p, other, karn := karnGame(t, seat)
		c := engine.NewScriptedController()
		bear := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Battlefield)
		activateKarn(t, g, p, karn, karnExile, c, bear)
		activateKarn(t, g, p, karn, karnUltimate, c)
		if b := g.Card(bear); b.Zone != engine.Battlefield || b.Controller() != p || b.Owner != other {
			t.Errorf("seat %d: bear in %v under %v, want on the battlefield under Karn's controller", seat, b.Zone, b.Controller())
		}
		if !inZone(g, karn, engine.Library, p) {
			t.Errorf("seat %d: Karn in %v, want its owner's library", seat, g.Card(karn).Zone)
		}
	}
}

// TestRestartGameIgnoresAnEarlierKarnsExiles pins the object identity the
// carve-out reads: a card exiled with Karn before Karn left the battlefield
// and came back is not exiled with the Karn that restarts, so it is
// shuffled in like any other card; one the returned Karn exiled comes back.
func TestRestartGameIgnoresAnEarlierKarnsExiles(t *testing.T) {
	t.Parallel()

	g, p, other, karn := karnGame(t, 0)
	c := engine.NewScriptedController()
	old := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Exile)
	g.SetExiledWith(old, karn)
	g.Move(karn, engine.Hand, p)
	g.Move(karn, engine.Battlefield, p)
	g.Card(karn).Counters.Add(engine.Loyalty, 30)
	fresh := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Battlefield)
	activateKarn(t, g, p, karn, karnExile, c, fresh)
	activateKarn(t, g, p, karn, karnUltimate, c)

	if !inZone(g, old, engine.Library, other) {
		t.Errorf("card an earlier Karn exiled in %v, want its owner's library", g.Card(old).Zone)
	}
	if z := g.Card(fresh).Zone; z != engine.Battlefield {
		t.Errorf("card this Karn exiled in %v, want returned to the battlefield", z)
	}
}

// TestRestartGameStopsTheDriverUntilResumed proves ADR-0034's driver
// contract through each entry point: Run, Step and PassPriority return
// cleanly as the -14 resolves, with nothing left on the stack; each refuses
// to run again, as does ResolveStack, until ResumeAfterRestart deals new
// opening hands and begins Karn's controller's first turn, after which the
// game plays on.
func TestRestartGameStopsTheDriverUntilResumed(t *testing.T) {
	t.Parallel()

	reg := engine.NewRegistry()
	for _, entry := range []string{"Run", "Step", "PassPriority"} {
		g, p, other, karn := karnGame(t, 0)
		c := engine.NewScriptedController()
		c.QueueAction(p, engine.Action{Kind: engine.ActionActivate, Card: karn, AbilityIndex: karnUltimate})
		var err error
		switch entry {
		case "Run":
			g.SetTurnState(1, p, engine.Draw)
			err = g.Run(reg, c, 5)
		case "Step":
			g.SetTurnState(1, p, engine.Draw)
			err = g.Step(reg, c)
		case "PassPriority":
			err = g.PassPriority(reg, c)
		}
		if err != nil {
			t.Fatalf("%s: %v", entry, err)
		}
		if !g.Restarted() || g.StackLen() != 0 || g.Over() {
			t.Fatalf("%s: restarted %v, stack %d, over %v; want restarted, empty, not over", entry, g.Restarted(), g.StackLen(), g.Over())
		}
		for name, call := range map[string]func() error{
			"Run":          func() error { return g.Run(reg, c, 5) },
			"Step":         func() error { return g.Step(reg, c) },
			"PassPriority": func() error { return g.PassPriority(reg, c) },
			"ResolveStack": func() error { return g.ResolveStack(reg, c) },
		} {
			if err := call(); err == nil || !strings.Contains(err.Error(), "ResumeAfterRestart") {
				t.Errorf("%s then %s: err = %v, want the restart-pending error", entry, name, err)
			}
		}

		c.QueueKeepHand(true)
		c.QueueKeepHand(true)
		if err := g.ResumeAfterRestart(c); err != nil {
			t.Fatalf("%s: ResumeAfterRestart: %v", entry, err)
		}
		if g.Restarted() || g.Turn() != 1 || g.ActivePlayer() != p || g.ActivePhase() != engine.Untap {
			t.Errorf("%s: after resuming restarted %v turn %d active %v phase %v, want false, 1, %v, Untap",
				entry, g.Restarted(), g.Turn(), g.ActivePlayer(), g.ActivePhase(), p)
		}
		for _, pid := range []engine.PlayerID{p, other} {
			if n := g.Zone(engine.Hand, pid).Len(); n != 7 {
				t.Errorf("%s: player %v opening hand %d cards, want 7", entry, pid, n)
			}
		}
		if err := g.ResumeAfterRestart(c); err == nil {
			t.Errorf("%s: a second ResumeAfterRestart succeeded, want an error", entry)
		}
		if err := g.Run(reg, c, 1); err != nil {
			t.Errorf("%s: Run after resuming: %v", entry, err)
		}
		if g.Turn() != 1 || g.ActivePhase() != engine.Cleanup {
			t.Errorf("%s: after one more turn at turn %d phase %v, want turn 1's Cleanup", entry, g.Turn(), g.ActivePhase())
		}
	}
}

// TestRestartGameReturnedTriggerResolvesInTheNewGame proves what the
// restart leaves on the stack: a permanent ReturnFromExile brings back
// triggers on entering (Elvish Visionary's draw), and that trigger waits
// through the restart and resolves in the restarted game's first priority
// window, Karn's controller's upkeep.
func TestRestartGameReturnedTriggerResolvesInTheNewGame(t *testing.T) {
	t.Parallel()

	g, p, _, karn := karnGame(t, 0)
	c := engine.NewScriptedController()
	elf := g.NewCard(corpusCard(t, "Elvish Visionary"), p, engine.Exile)
	g.SetExiledWith(elf, karn)
	activateKarn(t, g, p, karn, karnUltimate, c)
	if g.Card(elf).Zone != engine.Battlefield || g.StackLen() != 1 {
		t.Fatalf("elf in %v with %d on the stack, want on the battlefield with its trigger waiting", g.Card(elf).Zone, g.StackLen())
	}
	c.QueueKeepHand(true)
	c.QueueKeepHand(true)
	if err := g.ResumeAfterRestart(c); err != nil {
		t.Fatal(err)
	}
	if err := g.Step(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if g.StackLen() != 0 || g.Zone(engine.Hand, p).Len() != 8 {
		t.Errorf("stack %d, hand %d; want the draw resolved into an eighth card", g.StackLen(), g.Zone(engine.Hand, p).Len())
	}
}

// TestRestartGameWithoutACarveOut proves RestrictFromZone$'s absence: every
// restart zone, exile included, goes to the library.
func TestRestartGameWithoutACarveOut(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	exiled := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Exile)
	host, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ RestartGame")
	if err != nil {
		t.Fatal(err)
	}
	if !inZone(g, exiled, engine.Library, p) || !inZone(g, host, engine.Library, p) {
		t.Errorf("exiled card in %v, host in %v, want both in the library", g.Card(exiled).Zone, g.Card(host).Zone)
	}
	if clone := g.Clone(); !clone.Restarted() || clone.RestartedBy() != p {
		t.Errorf("clone restarted %v by %v, want the original's restart carried", clone.Restarted(), clone.RestartedBy())
	}
}

// TestRestartGameClearsTheStackUnderIt proves MagicStack.reset
// (RestartGameEffect.java:53): an ability still waiting under the
// resolving RestartGame is discarded with the old game, never resolved --
// ResolveStack stops on the restart, and nothing is left to strand.
func TestRestartGameClearsTheStackUnderIt(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := etbChainDef(t, "Test Gain", "DB$ GainLife | Defined$ You | LifeAmount$ 5")
	gainHost := g.NewCard(def, p, engine.Battlefield)
	g.PushAbility(engine.Ability{API: engine.APIGainLife, Source: gainHost, Controller: p, Params: def.Faces[0].Triggers[0].Subs[0].Ability})
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ RestartGame"); err != nil {
		t.Fatal(err)
	}
	if g.StackLen() != 0 || g.Player(p).Life != 20 {
		t.Errorf("stack %d, life %d; want the waiting GainLife discarded unresolved (0, 20)", g.StackLen(), g.Player(p).Life)
	}
}

// TestRestartGameClearsDelayedTriggers proves
// TriggerHandler.clearDelayedTrigger (RestartGameEffect.java:40): a
// delayed "next upkeep" trigger made before the restart never fires in the
// restarted game's first upkeep. The same trigger without a restart does
// fire there -- the control that makes the absence mean something.
func TestRestartGameClearsDelayedTriggers(t *testing.T) {
	t.Parallel()

	reg := engine.NewRegistry()
	for _, restart := range []bool{false, true} {
		g, p, other := newTwoPlayerGame(t)
		libraryCards(t, g, p, 8)
		libraryCards(t, g, other, 8)
		c := engine.NewScriptedController()
		resolveLine(t, g, p, c, "DB$ DelayedTrigger | Mode$ Phase | Phase$ Upkeep | Execute$ DBGain",
			"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 5")
		want := 25
		if restart {
			if _, err := resolveNow(t, g, p, c, nil, "DB$ RestartGame"); err != nil {
				t.Fatal(err)
			}
			c.QueueKeepHand(true)
			c.QueueKeepHand(true)
			if err := g.ResumeAfterRestart(c); err != nil {
				t.Fatal(err)
			}
			want = 20
		} else {
			g.SetTurnState(1, p, engine.Untap)
		}
		if err := g.Step(reg, c); err != nil {
			t.Fatal(err)
		}
		if g.ActivePhase() != engine.Upkeep || g.Player(p).Life != want {
			t.Errorf("restart %v: phase %v life %d, want Upkeep and %d", restart, g.ActivePhase(), g.Player(p).Life, want)
		}
	}
}

// TestRestartGameClearsActiveControlGrant proves p.clearController()
// (RestartGameEffect.java:74, ADR-0030's ControlPlayer): a control grant
// already in force when the game restarts ends immediately, the same as
// every other per-player restart field this effect resets.
func TestRestartGameClearsActiveControlGrant(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	resolveLine(t, g, p, c, "DB$ ControlPlayer | ValidTgts$ Player")
	advanceToPhase(t, g, c, 2, engine.Untap)
	wantControl(t, g, other, p)

	if _, err := resolveNow(t, g, p, c, nil, "DB$ RestartGame"); err != nil {
		t.Fatal(err)
	}
	wantControl(t, g, other, engine.NoPlayer)
}

// TestRestartGamePreservesAPendingBeginCombatGrant reproduces a Forge bug
// (forge-java-defects.md, PORT-8): RestartGameEffect.java:47-51 clears the
// untap/upkeep/end-of-combat/end-of-turn/cleanup command lists but never
// game.getBeginOfCombat()'s, so a Combat$ ControlPlayer grant not yet
// active survives a restart and still fires at the target's own next
// combat, in the new game.
func TestRestartGamePreservesAPendingBeginCombatGrant(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	resolveLine(t, g, p, c, "DB$ ControlPlayer | ValidTgts$ Opponent | Combat$ True")
	wantControl(t, g, other, engine.NoPlayer)

	if _, err := resolveNow(t, g, p, c, nil, "DB$ RestartGame"); err != nil {
		t.Fatal(err)
	}
	c.QueueKeepHand(true)
	c.QueueKeepHand(true)
	if err := g.ResumeAfterRestart(c); err != nil {
		t.Fatal(err)
	}

	advanceToPhase(t, g, c, 2, engine.CombatBegin)
	wantControl(t, g, other, p)
}

// TestRestartGameRejectsWhatItCannotReset pins each fail-closed shape: an
// error before any card moves.
func TestRestartGameRejectsWhatItCannotReset(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, line, want string
		zone             engine.ZoneType
	}{
		{"zone outside restartZones", "DB$ RestartGame | RestrictFromZone$ Command", "RestrictFromZone$", engine.None},
		{"spell on the stack", "DB$ RestartGame", "stack", engine.Stack},
		{"commander or variant card", "DB$ RestartGame", "Command-zone", engine.Command},
		{"planechase", "DB$ RestartGame", "Planechase", engine.PlanarDeck},
		{"variant deck", "DB$ RestartGame", "AttractionDeck", engine.AttractionDeck},
	} {
		g, p, _ := newTwoPlayerGame(t)
		kept := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand)
		if tc.zone != engine.None {
			g.NewCard(creatureDefPT(t, "3", "3"), p, tc.zone)
		}
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want one naming %s", tc.name, err, tc.want)
		}
		if z := g.Card(kept).Zone; z != engine.Hand || g.Restarted() {
			t.Errorf("%s: hand card in %v, restarted %v -- the reset began before the rejection", tc.name, z, g.Restarted())
		}
	}
}
