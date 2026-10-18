package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// A mana ability's SubAbility$ resolves with it, at once (AbilityUtils.resolve's
// sub-abilities, CR 605.3a): Adarkar Wastes' "deals 1 damage to you".
func TestPainLandDealsDamageAsItMakesMana(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	land := g.NewCard(corpusCard(t, "Adarkar Wastes"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueManaColor(mana.Blue)
	// Index 1: the {W}/{U} ability; index 0 adds {C}.
	if !g.ActivateManaAbility(p, land, 1, c) {
		t.Fatalf("ActivateManaAbility failed: %v", g.TakePendingError())
	}
	if err := g.TakePendingError(); err != nil {
		t.Fatalf("pending error: %v", err)
	}
	if got := g.Player(p).Life; got != 19 {
		t.Errorf("life = %d, want 19", got)
	}
	if got := g.Player(p).ManaPool.Total(); got != 1 {
		t.Errorf("mana in pool = %d, want 1", got)
	}
}

// A targeted link in the chain has its target chosen with the ability, before
// anything is paid: Chandra, Dressed to Kill's +1 deals 1 damage to the target.
func TestManaAbilitySubAbilityTargetIsChosenFirst(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	chandra := g.NewCard(corpusCard(t, "Chandra, Dressed to Kill"), p, engine.Battlefield)
	g.Card(chandra).Counters.Add(engine.Loyalty, 3)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if !g.ActivateManaAbility(p, chandra, 0, c) {
		t.Fatalf("ActivateManaAbility failed: %v", g.TakePendingError())
	}
	if err := g.TakePendingError(); err != nil {
		t.Fatalf("pending error: %v", err)
	}
	if got := g.Player(other).Life; got != 19 {
		t.Errorf("opponent's life = %d, want 19", got)
	}
	if got := g.Player(p).ManaPool.Total(); got != 1 {
		t.Errorf("mana in pool = %d, want 1", got)
	}
}
