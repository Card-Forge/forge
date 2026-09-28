package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// sameControllerPumpLine is a two-target TargetsWithSameController$ ability
// shaped like Sorrow's Path's own targeting (TargetMin$/TargetMax$ 2,
// opponents' creatures), with a Pump payload whose result is easy to read.
const sameControllerPumpLine = "AB$ Pump | Cost$ T | ValidTgts$ Creature.OppCtrl | TargetMin$ 2 | TargetMax$ 2 | " +
	"TargetsWithSameController$ True | NumAtt$ 1"

// sameControllerGame seats three players: a activates, b controls two
// creatures, c controls one.
func sameControllerGame(t *testing.T) (g *engine.Game, a engine.PlayerID, source, b1, b2, c1 engine.CardID) {
	t.Helper()
	g = newGame(t, "a", "b", "c")
	players := g.Players()
	a = players[0]
	g.SetTurnState(1, a, engine.Main1)
	for _, pid := range players {
		g.Player(pid).Life = 20
	}
	source = g.NewCard(creatureDefWithAbility(t, "Test Same Controller", sameControllerPumpLine), a, engine.Battlefield)
	b1 = g.NewCard(creatureDefPT(t, "2", "2"), players[1], engine.Battlefield)
	b2 = g.NewCard(creatureDefPT(t, "2", "2"), players[1], engine.Battlefield)
	c1 = g.NewCard(creatureDefPT(t, "2", "2"), players[2], engine.Battlefield)
	return g, a, source, b1, b2, c1
}

// TestTargetsWithSameControllerDropsLoneCandidate proves
// CardLists.getTargetableCards' pre-filter (CardLists.java:201-217): c's
// only creature has no same-controller partner, so it is never offered.
func TestTargetsWithSameControllerDropsLoneCandidate(t *testing.T) {
	t.Parallel()

	g, a, source, b1, b2, _ := sameControllerGame(t)
	rec := &targetOfferRecorder{ScriptedController: engine.NewScriptedController()}
	rec.QueueTargets([]engine.EntityID{engine.CardEntity(b1), engine.CardEntity(b2)})
	if !g.ActivateAbility(a, source, 0, rec) {
		t.Fatal("ActivateAbility returned false")
	}
	want := []engine.EntityID{engine.CardEntity(b1), engine.CardEntity(b2)}
	if len(rec.offers) != 1 || !equalEntities(rec.offers[0], want) {
		t.Fatalf("offers = %v, want one offer of %v", rec.offers, want)
	}
	if err := g.ResolveStack(engine.NewRegistry(), rec); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	for _, id := range []engine.CardID{b1, b2} {
		if pw, _ := g.Card(id).Power(); pw != 3 {
			t.Errorf("card %d power = %d, want 3", id, pw)
		}
	}
}

// TestTargetsWithSameControllerMixedAnswerFizzles proves a chosen set
// spanning two controllers is not repaired: at resolution each card target
// has a partner with another controller (SpellAbility.java:1543-1549), so
// every target is illegal and nothing is pumped.
func TestTargetsWithSameControllerMixedAnswerFizzles(t *testing.T) {
	t.Parallel()

	g, a, source, b1, _, c1 := sameControllerGame(t)
	// c1 was filtered out of the offer; ScriptedController returns what it
	// is told, standing in for a controller that ignores the offer.
	sc := engine.NewScriptedController()
	sc.QueueTargets([]engine.EntityID{engine.CardEntity(b1), engine.CardEntity(c1)})
	if !g.ActivateAbility(a, source, 0, sc) {
		t.Fatal("ActivateAbility returned false")
	}
	if err := g.ResolveStack(engine.NewRegistry(), sc); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	for _, id := range []engine.CardID{b1, c1} {
		if pw, _ := g.Card(id).Power(); pw != 2 {
			t.Errorf("card %d power = %d, want 2 (the ability fizzled)", id, pw)
		}
	}
}

// TestTargetsWithSameControllerFizzlesOnControlChange proves the fizzle-time
// half (SpellAbility.java:1543 runs with fizzleCheck true too): once one
// target changes controller, each target has a partner with a different
// controller, both are illegal (MagicStack.hasFizzled removes after the
// loop), and nothing is pumped.
func TestTargetsWithSameControllerFizzlesOnControlChange(t *testing.T) {
	t.Parallel()

	g, a, source, b1, b2, _ := sameControllerGame(t)
	sc := engine.NewScriptedController()
	sc.QueueTargets([]engine.EntityID{engine.CardEntity(b1), engine.CardEntity(b2)})
	if !g.ActivateAbility(a, source, 0, sc) {
		t.Fatal("ActivateAbility returned false")
	}
	// A Layer 2 GainControl$ static, controlled by c, takes b2 while the
	// ability waits on the stack.
	stealer := g.NewCard(continuousDef(t, "Test Steal", "Mode$ Continuous | Affected$ Card.IsRemembered | GainControl$ You"),
		g.Players()[2], engine.Battlefield)
	g.Card(stealer).Memory.Remember(engine.CardEntity(b2))
	engine.CheckStateBasedActions(g, sc)
	if g.Card(b2).Controller() != g.Players()[2] {
		t.Fatalf("setup: b2 controller = %d, want c", g.Card(b2).Controller())
	}

	if err := g.ResolveStack(engine.NewRegistry(), sc); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(b1).Power(); pw != 2 {
		t.Errorf("b1 power = %d, want 2 (the ability fizzled)", pw)
	}
}

// equalEntities reports whether two entity lists hold the same IDs in the
// same order.
func equalEntities(x, y []engine.EntityID) bool {
	if len(x) != len(y) {
		return false
	}
	for i := range x {
		if x[i] != y[i] {
			return false
		}
	}
	return true
}
