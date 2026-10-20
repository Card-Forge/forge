package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// RemoveCounter reads its targets (ValidTgts$) or the host when it names
// nothing, and CounterType$ Any / UpTo$ ask the activator how many and of what
// (CountersRemoveEffect.java:84-275).
func TestRemoveCounterTargetedHostAndPrompts(t *testing.T) {
	t.Parallel()

	// A targeted line.
	g, p, _ := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.Card(victim).Counters.Add(engine.P1P1, 3)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)},
		"DB$ RemoveCounter | ValidTgts$ Creature | CounterType$ P1P1 | CounterNum$ 2"); err != nil {
		t.Fatal(err)
	}
	if got := p1p1(g, victim); got != 1 {
		t.Errorf("targeted: %d counters left, want 1", got)
	}

	// No Defined$: the host itself.
	g, p, _ = newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	def := scriptDef(t, "Test Counter Host", "Creature Elf", "A:AB$ RemoveCounter | Cost$ 0 | CounterType$ P1P1")
	host := g.NewCard(def, p, engine.Battlefield)
	g.Card(host).Counters.Add(engine.P1P1, 2)
	activateAndResolve(t, g, p, host, c)
	if got := p1p1(g, host); got != 1 {
		t.Errorf("host: %d counters left, want 1", got)
	}

	// UpTo$: the activator names how many.
	g, p, _ = newTwoPlayerGame(t)
	victim = g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.Card(victim).Counters.Add(engine.P1P1, 3)
	c = engine.NewScriptedController()
	c.QueueNumberChoice(2)
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(victim)},
		"DB$ RemoveCounter | ValidTgts$ Creature | CounterType$ P1P1 | CounterNum$ 3 | UpTo$ True"); err != nil {
		t.Fatal(err)
	}
	if got := p1p1(g, victim); got != 1 {
		t.Errorf("up to: %d counters left, want 1", got)
	}

	// CounterType$ Any: a kind, then how many of it.
	g, p, _ = newTwoPlayerGame(t)
	victim = g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.Card(victim).Counters.Add(engine.P1P1, 2)
	g.Card(victim).Counters.Add(engine.CounterType("FLOOD"), 2)
	c = engine.NewScriptedController()
	c.QueueOptionNamed("FLOOD")
	c.QueueNumberChoice(2)
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(victim)},
		"DB$ RemoveCounter | ValidTgts$ Creature | CounterType$ Any | CounterNum$ 2"); err != nil {
		t.Fatal(err)
	}
	if got, want := g.Card(victim).Counters.Count(engine.CounterType("FLOOD")), 0; got != want {
		t.Errorf("any: %d FLOOD counters left, want %d", got, want)
	}
	if got := p1p1(g, victim); got != 2 {
		t.Errorf("any: %d +1/+1 counters left, want 2", got)
	}
}
