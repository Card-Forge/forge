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

// etbLosesGameTriggerDefParams builds a *compile.Card whose own "when
// CARDNAME enters" trigger runs DB$ LosesGame with the given params string
// appended -- etbDestroyTriggerDefParams' own shape (destroyeffect_test.go).
func etbLosesGameTriggerDefParams(t *testing.T, name, extraParams string) *compile.Card {
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
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigLosesGame",
	}
	raw.Faces[0].SVars.Set("TrigLosesGame", "DB$ LosesGame | "+extraParams)

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBLosesGame casts def for p on controller c and resolves the stack
// -- castETBDestroy's own shape (destroyeffect_test.go).
func castETBLosesGame(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, c *engine.ScriptedController) (engine.CardID, error) {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return creature, g.ResolveStack(engine.NewRegistry(), c)
}

// TestLosesGameEffectSetsLostAndTheSurvivorWins proves Player.Lost already
// existed for CheckStateBasedActions (action.go) to read: setting it is
// enough for the very next state-based-action pass to declare the other
// player the winner and end the game, CR 104.2a's own elimination count.
func TestLosesGameEffectSetsLostAndTheSurvivorWins(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	c := engine.NewScriptedController()
	_, err := castETBLosesGame(t, g, p, etbLosesGameTriggerDefParams(t, "Test LosesGame You", "Defined$ You"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !g.Player(p).Lost {
		t.Error("p Lost = false, want true")
	}
	if !g.Over() {
		t.Error("Over() = false, want true")
	}
	if !g.Player(other).Won {
		t.Error("other Won = false, want true")
	}
}

// TestLosesGameEffectValidTgtsScopesToChosenPlayer proves an ability naming
// ValidTgts$ reads its own chosen player target directly
// (targetedOrDefinedPlayers, defined.go).
func TestLosesGameEffectValidTgtsScopesToChosenPlayer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	_, err := castETBLosesGame(t, g, p, etbLosesGameTriggerDefParams(t, "Test LosesGame Tgt", "ValidTgts$ Player"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !g.Player(other).Lost {
		t.Error("other Lost = false, want true")
	}
	if g.Player(p).Lost {
		t.Error("p Lost = true, want false")
	}
}

// TestLosesGameEffectRejectsUnresolvedParam proves losesGameUnresolvedParams'
// own fail-loud contract (PORT-8/GO-7).
func TestLosesGameEffectRejectsUnresolvedParam(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	c := engine.NewScriptedController()
	_, err := castETBLosesGame(t, g, p, etbLosesGameTriggerDefParams(t, "Test LosesGame Condition", "Defined$ You | Condition$ Teamwork"), c)
	if err == nil {
		t.Fatal("ResolveStack succeeded, want an error for unresolved Condition$")
	}
}
