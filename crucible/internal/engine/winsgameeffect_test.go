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

// etbWinsGameTriggerDefParams builds a *compile.Card whose own "when
// CARDNAME enters" trigger runs DB$ WinsGame with the given params string
// appended -- etbDestroyTriggerDefParams' own shape (destroyeffect_test.go).
func etbWinsGameTriggerDefParams(t *testing.T, name, extraParams string) *compile.Card {
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
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigWinsGame",
	}
	raw.Faces[0].SVars.Set("TrigWinsGame", "DB$ WinsGame | "+extraParams)

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBWinsGame casts def for p on controller c and resolves the stack --
// castETBDestroy's own shape (destroyeffect_test.go).
func castETBWinsGame(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, c *engine.ScriptedController) (engine.CardID, error) {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return creature, g.ResolveStack(engine.NewRegistry(), c)
}

// TestWinsGameEffectEndsTheGameImmediatelyWithOpponentsStillAlive proves CR
// 104.1's own alt-win: a three-player game ends the moment one player wins,
// independent of CR 104.2a's own elimination count -- CheckStateBasedActions
// (action.go) checks Player.Won first, above the Lost-based count, exactly
// so an effect-driven win does not wait for every opponent to also be
// eliminated.
func TestWinsGameEffectEndsTheGameImmediatelyWithOpponentsStillAlive(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b", "c")
	p, second, third := g.Players()[0], g.Players()[1], g.Players()[2]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(second).Life, g.Player(third).Life = 20, 20, 20

	c := engine.NewScriptedController()
	_, err := castETBWinsGame(t, g, p, etbWinsGameTriggerDefParams(t, "Test WinsGame You", "Defined$ You"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !g.Player(p).Won {
		t.Error("p Won = false, want true")
	}
	if !g.Over() {
		t.Error("Over() = false, want true")
	}
	if g.Player(second).Lost || g.Player(third).Lost {
		t.Error("second/third Lost = true, want false -- an effect-driven win needs no elimination")
	}
}

// TestWinsGameEffectValidTgtsScopesToChosenPlayer proves an ability naming
// ValidTgts$ reads its own chosen player target directly
// (targetedOrDefinedPlayers, defined.go).
func TestWinsGameEffectValidTgtsScopesToChosenPlayer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	_, err := castETBWinsGame(t, g, p, etbWinsGameTriggerDefParams(t, "Test WinsGame Tgt", "ValidTgts$ Player"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !g.Player(other).Won {
		t.Error("other Won = false, want true")
	}
	if g.Player(p).Won {
		t.Error("p Won = true, want false")
	}
}

// TestWinsGameEffectRejectsUnresolvedParam proves winsGameUnresolvedParams'
// own fail-loud contract (PORT-8/GO-7).
func TestWinsGameEffectRejectsUnresolvedParam(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	c := engine.NewScriptedController()
	_, err := castETBWinsGame(t, g, p, etbWinsGameTriggerDefParams(t, "Test WinsGame Condition", "Defined$ You | Condition$ Teamwork"), c)
	if err == nil {
		t.Fatal("ResolveStack succeeded, want an error for unresolved Condition$")
	}
}
