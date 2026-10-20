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

// etbConniveTriggerDefParams builds a *compile.Card whose own "when
// CARDNAME enters" trigger runs DB$ Connive with the given params string
// appended -- etbDestroyTriggerDefParams' own shape (destroyeffect_test.go).
func etbConniveTriggerDefParams(t *testing.T, name, extraParams string) *compile.Card {
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
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigConnive",
	}
	raw.Faces[0].SVars.Set("TrigConnive", "DB$ Connive | "+extraParams)

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBConnive casts def for p on controller c and resolves the stack --
// castETBDestroy's own shape (destroyeffect_test.go).
func castETBConnive(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, c *engine.ScriptedController) (engine.CardID, error) {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return creature, g.ResolveStack(engine.NewRegistry(), c)
}

// TestConniveEffectDrawDiscardThenCounterOnNonlandDiscard proves the
// corpus's own dominant real shape: Defined$ Self draws a card, discards a
// card, and gets a +1/+1 counter because the discarded card was a nonland.
func TestConniveEffectDrawDiscardThenCounterOnNonlandDiscard(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	libraryCards(t, g, p, 3)
	extra := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand)

	c := engine.NewScriptedController()
	c.QueueDiscardChoice([]engine.CardID{extra})
	host, err := castETBConnive(t, g, p, etbConniveTriggerDefParams(t, "Test Connive Self", "Defined$ Self"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(host).Counters.Count(engine.P1P1); n != 1 {
		t.Errorf("host P1P1 = %d, want 1", n)
	}
	if g.Card(extra).Zone != engine.Graveyard {
		t.Errorf("extra zone = %v, want Graveyard", g.Card(extra).Zone)
	}
}

// TestConniveEffectNoCounterWhenDiscardedCardIsLand proves the counter is
// conditioned on the discard being a nonland card -- a discarded land grants
// nothing.
func TestConniveEffectNoCounterWhenDiscardedCardIsLand(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	libraryCards(t, g, p, 3)
	land := g.NewCard(landDef(t, "Test Land", "Basic Land Plains"), p, engine.Hand)

	c := engine.NewScriptedController()
	c.QueueDiscardChoice([]engine.CardID{land})
	host, err := castETBConnive(t, g, p, etbConniveTriggerDefParams(t, "Test Connive Land", "Defined$ Self"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(host).Counters.Count(engine.P1P1); n != 0 {
		t.Errorf("host P1P1 = %d, want 0 (discarded card was a land)", n)
	}
	if g.Card(land).Zone != engine.Graveyard {
		t.Errorf("land zone = %v, want Graveyard", g.Card(land).Zone)
	}
}

// TestConniveEffectValidTgtsMakesTheChosenCreatureConnive proves an ability
// naming ValidTgts$ reads its own chosen target directly
// (targetedOrDefinedCards, defined.go) rather than through Defined$.
func TestConniveEffectValidTgtsMakesTheChosenCreatureConnive(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	libraryCards(t, g, p, 3)
	other := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	extra := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand)

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(other)})
	c.QueueDiscardChoice([]engine.CardID{extra})
	_, err := castETBConnive(t, g, p, etbConniveTriggerDefParams(t, "Test Connive Tgt", "ValidTgts$ Creature.Other"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(other).Counters.Count(engine.P1P1); n != 1 {
		t.Errorf("other P1P1 = %d, want 1", n)
	}
}

// TestConniveEffectRejectsUnresolvedParam proves conniveUnresolvedParams'
// own fail-loud contract (PORT-8/GO-7).
func TestConniveEffectRejectsUnresolvedParam(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	c := engine.NewScriptedController()
	_, err := castETBConnive(t, g, p, etbConniveTriggerDefParams(t, "Test Connive Condition", "Defined$ Self | Condition$ Teamwork"), c)
	if err == nil {
		t.Fatal("ResolveStack succeeded, want an error for unresolved Condition$")
	}
}
