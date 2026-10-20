package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// PutCounter's per-object counter kinds (CountersPutEffect.java:300-470):
// CounterTypes$, SplitAmount$, ChosenFromList, ExistingCounter, UniqueType$ and
// UpTo$.
func TestPutCounterKindsPerObject(t *testing.T) {
	t.Parallel()

	victim := func() (*engine.Game, engine.PlayerID, engine.CardID) {
		g, p, _ := newTwoPlayerGame(t)
		return g, p, g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	}
	count := func(g *engine.Game, id engine.CardID, kind string) int {
		return g.Card(id).Counters.Count(engine.CounterType(kind))
	}

	// CounterTypes$ lists the kinds, each getting the amount.
	g, p, id := victim()
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(id)},
		"DB$ PutCounter | ValidTgts$ Creature | CounterTypes$ P1P1,Lifelink | CounterNum$ 2"); err != nil {
		t.Fatal(err)
	}
	if count(g, id, "P1P1") != 2 || count(g, id, "LIFELINK") != 2 {
		t.Errorf("CounterTypes$: P1P1 %d, LIFELINK %d, want 2 and 2", count(g, id, "P1P1"), count(g, id, "LIFELINK"))
	}

	// ChosenFromList: the activator picks one kind off TypeList$.
	g, p, id = victim()
	c := engine.NewScriptedController()
	c.QueueOptionNamed("VIGILANCE")
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(id)},
		"DB$ PutCounter | ValidTgts$ Creature | CounterTypes$ P1P1,ChosenFromList | TypeList$ Flying,Vigilance | CounterNum$ 1"); err != nil {
		t.Fatal(err)
	}
	if count(g, id, "VIGILANCE") != 1 || count(g, id, "FLYING") != 0 || count(g, id, "P1P1") != 1 {
		t.Errorf("ChosenFromList: vigilance %d, flying %d, p1p1 %d", count(g, id, "VIGILANCE"), count(g, id, "FLYING"), count(g, id, "P1P1"))
	}

	// SplitAmount$: all but the last kind choose a number, the last takes the rest.
	g, p, id = victim()
	c = engine.NewScriptedController()
	c.QueueNumberChoice(1)
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(id)},
		"DB$ PutCounter | ValidTgts$ Creature | CounterTypes$ P1P1,P0P2 | CounterNum$ 3 | SplitAmount$ True"); err != nil {
		t.Fatal(err)
	}
	if count(g, id, "P1P1") != 1 || count(g, id, "P0P2") != 2 {
		t.Errorf("SplitAmount$: P1P1 %d, P0P2 %d, want 1 and 2", count(g, id, "P1P1"), count(g, id, "P0P2"))
	}

	// ExistingCounter: only a kind the target already has; EachExistingCounter all.
	g, p, id = victim()
	g.Card(id).Counters.Add(engine.CounterType("STUN"), 1)
	g.Card(id).Counters.Add(engine.P1P1, 1)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(id)},
		"DB$ PutCounter | ValidTgts$ Creature | CounterType$ ExistingCounter | EachExistingCounter$ True | CounterNum$ 1"); err != nil {
		t.Fatal(err)
	}
	if count(g, id, "STUN") != 2 || count(g, id, "P1P1") != 2 {
		t.Errorf("EachExistingCounter$: STUN %d, P1P1 %d, want 2 and 2", count(g, id, "STUN"), count(g, id, "P1P1"))
	}
	g, p, id = victim()
	g.Card(id).Counters.Add(engine.CounterType("STUN"), 1)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(id)},
		"DB$ PutCounter | ValidTgts$ Creature | CounterType$ ExistingCounter | CounterNum$ 1"); err != nil {
		t.Fatal(err)
	}
	if count(g, id, "STUN") != 2 {
		t.Errorf("ExistingCounter: STUN %d, want 2", count(g, id, "STUN"))
	}

	// UpTo$: the activator names how many.
	g, p, id = victim()
	c = engine.NewScriptedController()
	c.QueueNumberChoice(1)
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(id)},
		"DB$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | CounterNum$ 3 | UpTo$ True"); err != nil {
		t.Fatal(err)
	}
	if count(g, id, "P1P1") != 1 {
		t.Errorf("UpTo$: P1P1 %d, want 1", count(g, id, "P1P1"))
	}
}

// A Mode$ CanAdapt static (StaticAbilityAdapt) lets a creature with +1/+1
// counters adapt anyway.
func TestCanAdaptLetsACreatureWithCountersAdapt(t *testing.T) {
	t.Parallel()

	for _, withStatic := range []bool{false, true} {
		g, p, _ := newTwoPlayerGame(t)
		if withStatic {
			g.NewCard(scriptDef(t, "Test Allower", "Enchantment", "S:Mode$ CanAdapt | ValidCard$ Creature"), p, engine.Battlefield)
		}
		adapter := g.NewCard(scriptDef(t, "Test Adapter", "Creature Elf", "A:AB$ PutCounter | Cost$ 0 | Defined$ Self | Adapt$ 2"), p, engine.Battlefield)
		g.Card(adapter).Counters.Add(engine.P1P1, 1)
		sba(g)
		activateAndResolve(t, g, p, adapter, engine.NewScriptedController())
		want := 1
		if withStatic {
			want = 3
		}
		if got := p1p1(g, adapter); got != want {
			t.Errorf("CanAdapt static %v: %d counters, want %d", withStatic, got, want)
		}
	}
}

// The valid properties greatestPower, MonoColor and YourTeamCtrl
// (CardProperty.java:182, :1211, CardStateProperty.java:56).
func TestValidPropertiesGreatestPowerMonoColorAndTeam(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	small := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	big := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	green := g.NewCard(scriptDef(t, "Test Green", "Creature Elf"), p, engine.Battlefield)
	g.Card(green).ColorMod.Add(engine.ColorEffect{Timestamp: 1, Colors: mana.Green, Overwrite: true})
	for _, tc := range []struct {
		spec string
		card engine.CardID
		want bool
	}{
		{"Creature.greatestPower", big, true},
		{"Creature.greatestPower", small, false},
		{"Creature.greatestPowerControlledByYou", small, true},
		{"Creature.greatestPowerControlledByYou", big, false},
		{"Creature.MonoColor", green, true},
		{"Creature.MonoColor", small, false},
		{"Creature.YourTeamCtrl", small, true},
		{"Creature.YourTeamCtrl", big, false},
	} {
		if got := engine.Matches(g, g.Card(tc.card), valid.Parse(tc.spec), p, engine.NoCard); got != tc.want {
			t.Errorf("%s on card %d = %v, want %v", tc.spec, tc.card, got, tc.want)
		}
	}
}
