package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// etbMultiplyCounterTriggerDefParams builds a *compile.Card whose own "when
// CARDNAME enters" trigger runs DB$ MultiplyCounter with the given params
// string appended -- etbDestroyTriggerDefParams' own shape
// (destroyeffect_test.go).
func etbMultiplyCounterTriggerDefParams(t *testing.T, name, extraParams string) *compile.Card {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "1", "1"
	raw.Faces[0].ManaCost = mana.MustParse("G")
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigMultiplyCounter",
	}
	raw.Faces[0].SVars.Set("TrigMultiplyCounter", "DB$ MultiplyCounter | "+extraParams)

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBMultiplyCounterSelf builds the host via def, gives it the counters
// pre set (still legal while it sits in Hand -- Counters is a plain
// per-card field, not zone-gated, removecountereffect_test.go's own
// identical castETBRemoveCounterSelf idiom), then casts it and resolves the
// stack.
func castETBMultiplyCounterSelf(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, preset func(*engine.Card)) (engine.CardID, error) {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	preset(g.Card(creature))
	c := engine.NewScriptedController()
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return creature, g.ResolveStack(engine.NewRegistry(), c)
}

// TestMultiplyCounterEffectDefinedSelfDoublesTheNamedKind proves the
// corpus's own dominant real shape: Defined$ Self doubles a single literal
// CounterType$'s own current count on the host.
func TestMultiplyCounterEffectDefinedSelfDoublesTheNamedKind(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	def := etbMultiplyCounterTriggerDefParams(t, "Test Multiply Self", "Defined$ Self | CounterType$ P1P1")
	host, err := castETBMultiplyCounterSelf(t, g, p, def, func(c *engine.Card) { c.Counters.Add(engine.P1P1, 3) })
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(host).Counters.Count(engine.P1P1); n != 6 {
		t.Errorf("host P1P1 = %d, want 6 (3 doubled)", n)
	}
}

// TestMultiplyCounterEffectValidTgtsMultipliesChosenCreature proves an
// ability naming ValidTgts$ reads its own chosen target directly
// (targetedOrDefinedCards, defined.go) rather than through Defined$, and a
// literal Multiplier$ past the default 2 scales correctly.
func TestMultiplyCounterEffectValidTgtsMultipliesChosenCreature(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	other := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.Card(other).Counters.Add(engine.P1P1, 2)

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(other)})
	def := etbMultiplyCounterTriggerDefParams(t, "Test Multiply Tgt", "ValidTgts$ Creature | CounterType$ P1P1 | Multiplier$ 3")
	g.Player(p).ManaPool.Add(mana.Green, 1)
	host := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, host, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(other).Counters.Count(engine.P1P1); n != 6 {
		t.Errorf("other P1P1 = %d, want 6 (2 * 3)", n)
	}
}

// TestMultiplyCounterEffectNoCounterTypeDoublesEveryKind proves
// CounterType$'s own absence doubles every kind the target carries at
// once, CountersMultiplyEffect.java's own `getCounterType(sa)` returning
// null shape.
func TestMultiplyCounterEffectNoCounterTypeDoublesEveryKind(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	def := etbMultiplyCounterTriggerDefParams(t, "Test Multiply AllKinds", "Defined$ Self")
	host, err := castETBMultiplyCounterSelf(t, g, p, def, func(c *engine.Card) {
		c.Counters.Add(engine.P1P1, 2)
		c.Counters.Add(engine.Stun, 1)
	})
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(host).Counters.Count(engine.P1P1); n != 4 {
		t.Errorf("host P1P1 = %d, want 4 (2 doubled)", n)
	}
	if n := g.Card(host).Counters.Count(engine.Stun); n != 2 {
		t.Errorf("host Stun = %d, want 2 (1 doubled)", n)
	}
}

// TestMultiplyCounterEffectRejectsUnresolvedParam proves
// multiplyCounterUnresolvedParams' own fail-loud contract (PORT-8/GO-7).
func TestMultiplyCounterEffectRejectsUnresolvedParam(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	def := etbMultiplyCounterTriggerDefParams(t, "Test Multiply Condition", "Defined$ Self | CounterType$ P1P1 | Condition$ Teamwork")
	_, err := castETBMultiplyCounterSelf(t, g, p, def, func(c *engine.Card) { c.Counters.Add(engine.P1P1, 1) })
	if err == nil {
		t.Fatal("ResolveStack succeeded, want an error for unresolved Condition$")
	}
}
