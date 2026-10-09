package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// stealUntil resolves a GainControl line for p against victim and returns the
// host that carries the command lists.
func stealUntil(t *testing.T, g *engine.Game, p engine.PlayerID, victim engine.CardID, line string, svars ...string) engine.CardID {
	t.Helper()
	host, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)}, line, svars...)
	if err != nil {
		t.Fatalf("%q: %v", line, err)
	}
	return host
}

// LoseControl$ EOT with AddKWs$ Haste: the keyword and the control both end
// at the cleanup step (ControlGainEffect.java:151-154, 183-186).
func TestGainControlEndOfTurnReturnsTheCreatureAndDropsTheKeywords(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	stealUntil(t, g, p, victim, "DB$ GainControl | ValidTgts$ Creature.OppCtrl | LoseControl$ EOT | AddKWs$ Haste")
	if got := g.Card(victim).Controller(); got != p {
		t.Fatalf("controller = %v, want the thief %v", got, p)
	}
	if !g.Card(victim).HasKeyword("Haste") {
		t.Error("the stolen creature lacks Haste")
	}
	endTurn(g, 1, p)
	if got := g.Card(victim).Controller(); got != other {
		t.Errorf("controller after cleanup = %v, want the owner %v", got, other)
	}
	if g.Card(victim).HasKeyword("Haste") {
		t.Error("Haste outlived the turn")
	}
}

// UntilTheEndOfYourNextTurn made during the activator's own turn survives
// that turn's cleanup (registerUntilEnd) and ends at the next one of theirs.
func TestGainControlUntilTheEndOfYourNextTurnSurvivesTheFirstCleanup(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	g.SetTurnState(1, p, engine.Main1)
	stealUntil(t, g, p, victim, "DB$ GainControl | ValidTgts$ Creature.OppCtrl | LoseControl$ UntilTheEndOfYourNextTurn")
	endTurn(g, 1, p)
	if got := g.Card(victim).Controller(); got != p {
		t.Fatalf("controller after this turn's cleanup = %v, want the thief %v", got, p)
	}
	endTurn(g, 3, p)
	if got := g.Card(victim).Controller(); got != other {
		t.Errorf("controller after the next own cleanup = %v, want the owner %v", got, other)
	}
}

// LoseControl$ LeavesPlay,LoseControl: the host dying and the host changing
// controller each end the hold, the second before the host moves.
func TestGainControlEndsWhenTheHostLeavesOrChangesController(t *testing.T) {
	t.Parallel()

	for _, how := range []string{"dies", "stolen"} {
		g, p, other := newTwoPlayerGame(t)
		victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		host := stealUntil(t, g, p, victim, "DB$ GainControl | ValidTgts$ Creature.OppCtrl | LoseControl$ LeavesPlay,LoseControl")
		if g.Card(victim).Controller() != p {
			t.Fatalf("%s: victim not stolen", how)
		}
		if how == "dies" {
			g.Move(host, engine.Graveyard, p)
			engine.CheckStateBasedActions(g, engine.NewScriptedController())
		} else {
			stealUntil(t, g, other, host, "DB$ GainControl | ValidTgts$ Creature.OppCtrl")
		}
		if got := g.Card(victim).Controller(); got != other {
			t.Errorf("%s: victim controller = %v, want the owner %v", how, got, other)
		}
	}
}

// LoseControl$ Untap: the hold lasts while the host stays tapped, and ends
// when the host untaps.
func TestGainControlUntapEndsWhenTheHostUntaps(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	host, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)},
		"DB$ Tap | Defined$ Self | SubAbility$ DBSteal",
		"DBSteal", "DB$ GainControl | ValidTgts$ Creature.OppCtrl | LoseControl$ Untap")
	if err != nil {
		t.Fatal(err)
	}
	if g.Card(victim).Controller() != p {
		t.Fatal("victim not stolen while the host is tapped")
	}
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(host)}, "DB$ Untap | ValidTgts$ Card"); err != nil {
		t.Fatal(err)
	}
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if got := g.Card(victim).Controller(); got != other {
		t.Errorf("controller after the host untapped = %v, want the owner %v", got, other)
	}
}

// A LoseControl$ value this port keeps no bookkeeping for is an error, never
// a hold that never ends (GO-7).
func TestGainControlRefusesAnUnknownLoseControlToken(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)},
		"DB$ GainControl | ValidTgts$ Creature.OppCtrl | LoseControl$ Bogus")
	if err == nil || !strings.Contains(err.Error(), "LoseControl$") {
		t.Errorf("err = %v, want a LoseControl$ error", err)
	}
}

// Duration$ AsLongAsControl on an Effect: the effect card goes when its host
// leaves the battlefield or another player gains control of the host.
func TestEffectAsLongAsControlEndsWithTheHostsControl(t *testing.T) {
	t.Parallel()

	for _, how := range []string{"dies", "stolen"} {
		g, p, other := newTwoPlayerGame(t)
		host, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ Effect | Duration$ AsLongAsControl | RememberObjects$ Self")
		if err != nil {
			t.Fatal(err)
		}
		if n := len(commandEffects(g, p)); n != 1 {
			t.Fatalf("%s: %d effect cards, want 1", how, n)
		}
		if how == "dies" {
			g.Move(host, engine.Graveyard, p)
		} else {
			stealUntil(t, g, other, host, "DB$ GainControl | ValidTgts$ Creature.OppCtrl")
		}
		if n := len(commandEffects(g, p)); n != 0 {
			t.Errorf("%s: %d effect cards left, want 0", how, n)
		}
	}
}

// LoseControl$ EndOfCombat ends when combat ends (EndOfCombat.executeUntil).
func TestGainControlEndOfCombatReturnsTheCreature(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	g.SetTurnState(1, p, engine.CombatDamage)
	stealUntil(t, g, p, victim, "DB$ GainControl | ValidTgts$ Creature.OppCtrl | LoseControl$ EndOfCombat")
	if g.Card(victim).Controller() != p {
		t.Fatal("victim not stolen")
	}
	g.AdvancePhase(engine.NewScriptedController()) // into Combat End
	g.AdvancePhase(engine.NewScriptedController()) // combat ends as the step is left
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if got := g.Card(victim).Controller(); got != other {
		t.Errorf("controller after combat = %v, want the owner %v", got, other)
	}
}

// Cleanup's ClearTriggered$ drops the delayed triggers its host registered
// (TriggerHandler.clearDelayedTrigger(card)): the Upkeep one never fires.
func TestCleanupClearTriggeredDropsTheHostsDelayedTriggers(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Untap)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
		"DB$ DelayedTrigger | Mode$ Phase | Phase$ Upkeep | Execute$ DBGain | SubAbility$ DBClean",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 5",
		"DBClean", "DB$ Cleanup | ClearTriggered$ True"); err != nil {
		t.Fatal(err)
	}
	c := engine.NewScriptedController()
	g.AdvancePhase(c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("life = %d, want 20 (the delayed trigger was cleared)", got)
	}
}

// A sub-ability naming ValidTgts$ with no legal target keeps the whole
// ability off the stack (CR 601.2c): the Draw never resolves.
func TestSubAbilityWithoutALegalTargetKeepsTheTriggerOffTheStack(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)
	def := etbChainDef(t, "Test Chain", "DB$ Draw | Defined$ You | NumCards$ 1 | SubAbility$ DBTap",
		"DBTap", "DB$ Tap | ValidTgts$ Creature.OppCtrl")
	if _, err := castETBChain(t, g, p, def, engine.NewScriptedController()); err != nil {
		t.Fatal(err)
	}
	if got := g.Zone(engine.Hand, p).Len(); got != 0 {
		t.Errorf("hand = %d, want 0 (the trigger never reached the stack)", got)
	}
}

// Attach with Object$ and no Choices$ (Stolen Uniform's shape): the named
// card attaches to the one host named.
func TestAttachObjectAttachesToTheNamedHost(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)

	// A creature "attached" this way falls off again at the next
	// state-based check (CR 704.5n), so only that the line resolves is
	// asserted; the control-stolen-uniform scenarios attach a real Equipment.
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(a)},
		"DB$ Attach | Object$ Self | ValidTgts$ Creature.OppCtrl"); err != nil {
		t.Fatal(err)
	}
	for _, line := range []string{
		"DB$ Attach | Object$ Self | ValidTgts$ Creature.OppCtrl",
		"DB$ Attach | Object$ Self | Chooser$ Opponent | ValidTgts$ Creature.OppCtrl",
	} {
		targets := []engine.EntityID{engine.CardEntity(a), engine.CardEntity(b)}
		if strings.Contains(line, "Chooser") {
			targets = targets[:1]
		}
		if _, err := resolveNow(t, g, p, engine.NewScriptedController(), targets, line); err == nil {
			t.Errorf("%q: resolved, want an error", line)
		}
	}
}

// Soulbond (BondEffect.java, GameAction.java:622-628, 1015-1020): a pair is
// made by the controller's choice and broken by the partner leaving or the
// Soulbond creature changing controller.
func TestBondPairsAndUnpairs(t *testing.T) {
	t.Parallel()

	for _, how := range []string{"partner leaves", "host stolen", "declined"} {
		g, p, other := newTwoPlayerGame(t)
		partner := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
		c := engine.NewScriptedController()
		if how == "declined" {
			c.QueueCardChoice(nil)
		} else {
			c.QueueCardChoice([]engine.CardID{partner})
		}
		host, err := resolveNow(t, g, p, c, nil, "DB$ Bond | Defined$ Self | ValidCards$ Creature.Other+YouCtrl")
		if err != nil {
			t.Fatalf("%s: %v", how, err)
		}
		paired := g.Card(host).IsPaired() && g.Card(host).PairedWith() == partner && g.Card(partner).PairedWith() == host
		if paired == (how == "declined") {
			t.Fatalf("%s: paired = %v", how, paired)
		}
		switch how {
		case "partner leaves":
			g.Move(partner, engine.Graveyard, p)
		case "host stolen":
			stealUntil(t, g, other, host, "DB$ GainControl | ValidTgts$ Creature.OppCtrl")
		}
		if g.Card(host).IsPaired() || g.Card(partner).IsPaired() {
			t.Errorf("%s: still paired", how)
		}
	}
}
