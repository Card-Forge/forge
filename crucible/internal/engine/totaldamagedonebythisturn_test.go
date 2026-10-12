package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Count$TotalDamageDoneByThisTurn is the damage the host dealt this turn after
// prevention (Card.getTotalDamageDoneBy). Brightflame's life gain reads it:
// X damage to the target and to each other creature sharing a color is gained
// as life, so two green Bears hit for 2 each give 4.
func TestTotalDamageDoneByThisTurnCountsEveryRecipient(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	target := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Battlefield)
	g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Battlefield)
	flame := g.NewCard(corpusCard(t, "Brightflame"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Red, 4)
	g.Player(p).ManaPool.Add(mana.White, 2)
	c := engine.NewScriptedController()
	c.QueueConfirmPayCost(false)
	c.QueuePayX(2)
	c.QueuePayGeneric(mana.ShardR)
	c.QueuePayGeneric(mana.ShardR)
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
	if !g.CastSpell(p, flame, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("caster has %d life, want 24 (20 + 2 damage to each of two Bears)", got)
	}
}
