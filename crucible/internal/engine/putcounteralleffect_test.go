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

// etbPutCounterAllTriggerDefParams builds a *compile.Card whose own "when
// CARDNAME enters" trigger runs DB$ PutCounterAll with the given params
// string appended -- etbDestroyTriggerDefParams' own shape
// (destroyeffect_test.go).
func etbPutCounterAllTriggerDefParams(t *testing.T, name, extraParams string) *compile.Card {
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
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigPutCounterAll",
	}
	raw.Faces[0].SVars.Set("TrigPutCounterAll", "DB$ PutCounterAll | "+extraParams)

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBPutCounterAll casts def for p on controller c and resolves the
// stack -- castETBDestroy's own shape (destroyeffect_test.go).
func castETBPutCounterAll(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, c *engine.ScriptedController) (engine.CardID, error) {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return creature, g.ResolveStack(engine.NewRegistry(), c)
}

// TestPutCounterAllEffectSweepsEveryPlayersBattlefield proves the corpus's
// own dominant real shape: ValidCards$ alone, no ValidTgts$, sweeps every
// player's matching permanents, not just the activator's own.
func TestPutCounterAllEffectSweepsEveryPlayersBattlefield(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	_, err := castETBPutCounterAll(t, g, p, etbPutCounterAllTriggerDefParams(t, "Test PutCounterAll Sweep", "ValidCards$ Creature.StrictlyOther | CounterType$ P1P1 | CounterNum$ 2"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(mine).Counters.Count(engine.P1P1); n != 2 {
		t.Errorf("mine P1P1 = %d, want 2", n)
	}
	if n := g.Card(theirs).Counters.Count(engine.P1P1); n != 2 {
		t.Errorf("theirs P1P1 = %d, want 2", n)
	}
}

// TestPutCounterAllEffectValidTgtsScopesToTargetedPlayer proves an ability
// naming ValidTgts$ narrows the sweep to permanents the chosen player
// controls (targetedOrDefinedPlayers, defined.go), leaving every OTHER
// player's matching permanents untouched.
func TestPutCounterAllEffectValidTgtsScopesToTargetedPlayer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	_, err := castETBPutCounterAll(t, g, p, etbPutCounterAllTriggerDefParams(t, "Test PutCounterAll Tgt", "ValidCards$ Creature.StrictlyOther | CounterType$ P1P1 | ValidTgts$ Player"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(theirs).Counters.Count(engine.P1P1); n != 1 {
		t.Errorf("theirs P1P1 = %d, want 1", n)
	}
	if n := g.Card(mine).Counters.Count(engine.P1P1); n != 0 {
		t.Errorf("mine P1P1 = %d, want 0 -- only the targeted player's permanents receive counters", n)
	}
}

// TestPutCounterAllEffectCounterNumDefaultsToOne proves CounterNum$'s own
// default -- absent, it adds exactly 1, putCounterEffect's own identical
// default.
func TestPutCounterAllEffectCounterNumDefaultsToOne(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	c := engine.NewScriptedController()
	_, err := castETBPutCounterAll(t, g, p, etbPutCounterAllTriggerDefParams(t, "Test PutCounterAll Default", "ValidCards$ Creature.StrictlyOther | CounterType$ P1P1"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(mine).Counters.Count(engine.P1P1); n != 1 {
		t.Errorf("mine P1P1 = %d, want 1 (CounterNum$ default)", n)
	}
}

// TestPutCounterAllEffectRejectsUnresolvedParam proves
// putCounterAllUnresolvedParams' own fail-loud contract (PORT-8/GO-7).
func TestPutCounterAllEffectRejectsUnresolvedParam(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	c := engine.NewScriptedController()
	_, err := castETBPutCounterAll(t, g, p, etbPutCounterAllTriggerDefParams(t, "Test PutCounterAll ValidZone", "ValidCards$ Creature | CounterType$ P1P1 | ValidZone$ Graveyard"), c)
	if err == nil {
		t.Fatal("ResolveStack succeeded, want an error for unresolved ValidZone$")
	}
}
