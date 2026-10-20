package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestAtEOTSacrificesTokensAtTheNextEndStep proves SpellAbilityEffect.
// registerDelayedTrigger for Token: the tokens a line makes with AtEOT$
// Sacrifice stay until the end step and are then sacrificed.
func TestAtEOTSacrificesTokensAtTheNextEndStep(t *testing.T) {
	t.Parallel()

	g, p, other := newTokenGame(t)
	libraryCards(t, g, p, 5)
	libraryCards(t, g, other, 5)
	c := engine.NewScriptedController()
	def := etbChainDef(t, "Test Eot Maker",
		"DB$ Token | TokenAmount$ 2 | TokenScript$ w_1_1_soldier | TokenOwner$ You | AtEOT$ Sacrifice")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := len(tokensOn(g, p, "Soldier Token")); n != 2 {
		t.Fatalf("tokens before the end step = %d, want 2", n)
	}
	advanceUntil(t, g, c, func() bool { return g.ActivePhase() == engine.EndOfTurn })
	if n := len(tokensOn(g, p, "Soldier Token")); n != 0 {
		t.Errorf("tokens after the end step = %d, want 0", n)
	}
}

// TestAtEOTHandReturnsThePumpedCreature proves AtEOT$ Hand on Pump: the
// target is pumped now and returned to its owner's hand at the end step.
func TestAtEOTHandReturnsThePumpedCreature(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	libraryCards(t, g, p, 5)
	libraryCards(t, g, other, 5)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	def := etbChainDef(t, "Test Eot Pump",
		"DB$ Pump | ValidTgts$ Creature.YouCtrl | NumAtt$ +2 | AtEOT$ Hand")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(mine).Zone; z != engine.Battlefield {
		t.Fatalf("creature zone before the end step = %v, want Battlefield", z)
	}
	advanceUntil(t, g, c, func() bool { return g.ActivePhase() == engine.EndOfTurn })
	if z := g.Card(mine).Zone; z != engine.Hand {
		t.Errorf("creature zone after the end step = %v, want Hand", z)
	}
}
