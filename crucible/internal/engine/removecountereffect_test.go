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

// etbRemoveCounterTriggerDefParams builds a *compile.Card whose own "when
// CARDNAME enters" trigger runs DB$ RemoveCounter with the given params
// string appended -- etbPutCounterTriggerDefParams' own shape
// (putcountereffect_test.go).
func etbRemoveCounterTriggerDefParams(t *testing.T, name, extraParams string) *compile.Card {
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
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigRemove",
	}
	raw.Faces[0].SVars.Set("TrigRemove", "DB$ RemoveCounter | "+extraParams)

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBRemoveCounterSelf builds the host via def, gives it the counters
// pre set (still legal while it sits in Hand -- Counters is a plain
// per-card field, not zone-gated), then casts it and resolves the stack.
func castETBRemoveCounterSelf(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, preset func(*engine.Card)) (engine.CardID, error) {
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

// TestRemoveCounterEffectRemovesLiteralTypeFromSelf proves the corpus's own
// dominant real shape: a single literal CounterType$ and a resolvable
// CounterNum$ removed from Defined$ Self.
func TestRemoveCounterEffectRemovesLiteralTypeFromSelf(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	def := etbRemoveCounterTriggerDefParams(t, "Test Remove Literal", "CounterType$ P1P1 | CounterNum$ 2 | Defined$ Self")
	host, err := castETBRemoveCounterSelf(t, g, p, def, func(c *engine.Card) { c.Counters.Add(engine.P1P1, 3) })
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(host).Counters.Count(engine.P1P1); n != 1 {
		t.Errorf("host P1P1 count = %d, want 1 (3 - 2)", n)
	}
}

// TestRemoveCounterEffectCounterNumAllRemovesEntireCount proves CounterNum$
// All reads the target's own CURRENT count of that one kind, computed per
// target rather than a fixed literal.
func TestRemoveCounterEffectCounterNumAllRemovesEntireCount(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	def := etbRemoveCounterTriggerDefParams(t, "Test Remove All Num", "CounterType$ STUN | CounterNum$ All | Defined$ Self")
	host, err := castETBRemoveCounterSelf(t, g, p, def, func(c *engine.Card) { c.Counters.Add(engine.Stun, 5) })
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(host).Counters.Count(engine.Stun); n != 0 {
		t.Errorf("host Stun count = %d, want 0", n)
	}
}

// TestRemoveCounterEffectCounterTypeAllRemovesEveryKind proves CounterType$
// All empties every counter kind the target carries, ignoring CounterNum$
// entirely.
func TestRemoveCounterEffectCounterTypeAllRemovesEveryKind(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	def := etbRemoveCounterTriggerDefParams(t, "Test Remove All Type", "CounterType$ All | Defined$ Self")
	host, err := castETBRemoveCounterSelf(t, g, p, def, func(c *engine.Card) {
		c.Counters.Add(engine.P1P1, 2)
		c.Counters.Add(engine.Stun, 1)
	})
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(host).Counters; got.Any() {
		t.Errorf("host still has counters after CounterType$ All: P1P1=%d Stun=%d", got.Count(engine.P1P1), got.Count(engine.Stun))
	}
}

// TestRemoveCounterEffectRemovesFromPlayer proves definedCounterTargets'
// own player dispatch (putcountereffect.go) -- a Defined$ You line removes
// from the activating player's own counters, not a card's.
func TestRemoveCounterEffectRemovesFromPlayer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	g.Player(p).Counters.Add(engine.Energy, 3)

	def := etbRemoveCounterTriggerDefParams(t, "Test Remove Player", "CounterType$ ENERGY | CounterNum$ 1 | Defined$ You")
	_, err := castETBRemoveCounterSelf(t, g, p, def, func(*engine.Card) {})
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Player(p).Counters.Count(engine.Energy); n != 2 {
		t.Errorf("p Energy count = %d, want 2 (3 - 1)", n)
	}
}

// TestRemoveCounterEffectRejectsUnresolvedParam proves
// removeCounterUnresolvedParams' own fail-loud contract (PORT-8/GO-7).
func TestRemoveCounterEffectRejectsUnresolvedParam(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	def := etbRemoveCounterTriggerDefParams(t, "Test Remove UpTo", "CounterType$ P1P1 | CounterNum$ 1 | Defined$ Self | UpTo$ True")
	_, err := castETBRemoveCounterSelf(t, g, p, def, func(c *engine.Card) { c.Counters.Add(engine.P1P1, 1) })
	if err == nil {
		t.Fatal("ResolveStack succeeded, want an error for unresolved UpTo$")
	}
}
