package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestCastSpellWardEachInstanceFiresIndependently is CR 702.21g: a
// permanent with two or more Ward instances has each fire on its own --
// protectionEach's own "check every line" precedent (ADR-0027's
// implementing pack), reused for checkWardTriggers (trigger.go, ADR-0028).
// Tomakul Honor Guard's own printed Ward:1 plus a second, granted Ward:2
// (Card.KeywordMod, the identical Layer 6 shape a real continuous grant
// would use) push two separate Counter-shaped abilities onto the stack
// above the spell that targeted it, not one.
func TestCastSpellWardEachInstanceFiresIndependently(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	target := g.NewCard(creatureDefPTKeywords(t, "3", "1", "Ward:1"), p, engine.Battlefield)
	g.Card(target).KeywordMod.Add(engine.KeywordEffect{AddKeywords: []string{"Ward:2"}})
	spell := g.NewCard(instantDefWithAbility(t, "Test Bolt", "0", "SP$ DealDamage | ValidTgts$ Creature | NumDmg$ 3"), opp, engine.Hand)
	g.SetTurnState(1, p, engine.Main1)

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
	if !g.CastSpell(opp, spell, c) {
		t.Fatal("CastSpell failed")
	}
	if got := g.StackLen(); got != 3 {
		t.Errorf("StackLen() = %d, want 3 (the spell plus two independent Ward triggers)", got)
	}
}

// TestCastSpellWardDoesNotFireForANonManaCost proves ADR-0028 Decision
// point 2's own scope boundary: a Ward line whose cost is not pure mana
// (PayLife<2>, Discard<1/Card>, ... -- Not ported yet, game-state.md)
// never reaches the stack at all, rather than reaching it and erroring at
// resolution.
func TestCastSpellWardDoesNotFireForANonManaCost(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	target := g.NewCard(creatureDefPTKeywords(t, "3", "1", "Ward:PayLife<2>"), p, engine.Battlefield)
	spell := g.NewCard(instantDefWithAbility(t, "Test Bolt", "0", "SP$ DealDamage | ValidTgts$ Creature | NumDmg$ 3"), opp, engine.Hand)
	g.SetTurnState(1, p, engine.Main1)

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
	if !g.CastSpell(opp, spell, c) {
		t.Fatal("CastSpell failed")
	}
	if got := g.StackLen(); got != 1 {
		t.Errorf("StackLen() = %d, want 1 (the spell alone, PayLife<2> not resolvable yet)", got)
	}
}

// TestCastSpellWardDoesNotMarkTheSpellAsATarget is the regression this
// pack's own review pass found: checkWardTriggers used to put the
// targeting spell on the built Counter Ability's Targets, which ran it
// through pushTriggeredAbilities' own post-push checkBecomesTargetTriggers
// scan and wrongly set Card.BecameTargetThisTurn on the spell's own card --
// a card that was never, itself, a chosen target of anything (ability.go's
// wardCounters doc comment). Bolt's own card must stay untouched.
func TestCastSpellWardDoesNotMarkTheSpellAsATarget(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	target := g.NewCard(creatureDefPTKeywords(t, "3", "1", "Ward:1"), p, engine.Battlefield)
	spell := g.NewCard(instantDefWithAbility(t, "Test Bolt", "0", "SP$ DealDamage | ValidTgts$ Creature | NumDmg$ 3"), opp, engine.Hand)
	g.SetTurnState(1, p, engine.Main1)

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
	if !g.CastSpell(opp, spell, c) {
		t.Fatal("CastSpell failed")
	}
	if g.Card(spell).BecameTargetThisTurn {
		t.Error("BecameTargetThisTurn = true on the spell's own card, want false: Ward's own Counter never targets the spell it counters (Defined$ TriggeredSourceSA, wardCounters)")
	}
}
