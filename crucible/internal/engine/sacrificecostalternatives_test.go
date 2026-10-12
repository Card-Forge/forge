package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// A Sac<1/Artifact;Creature> cost part names two types joined by ';', which
// means either (CostSacrifice.java:71 splits the type on ';'). Costly Plunder
// is castable when the only permanent it can sacrifice is an artifact.
func TestSacrificeCostTypesJoinedBySemicolonAreAlternatives(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	ring := g.NewCard(corpusCard(t, "Sol Ring"), p, engine.Battlefield)
	plunder := g.NewCard(corpusCard(t, "Costly Plunder"), p, engine.Hand)
	g.NewCard(corpusCard(t, "Island"), p, engine.Library)
	g.NewCard(corpusCard(t, "Island"), p, engine.Library)
	g.Player(p).ManaPool.Add(mana.Black, 2)
	c := engine.NewScriptedController()
	c.QueueConfirmPayCost(false)
	c.QueuePayGeneric(mana.ShardB)
	c.QueueSacrificeChoice([]engine.CardID{ring})
	if !g.CastSpell(p, plunder, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	if got := g.Card(ring).Zone; got != engine.Graveyard {
		t.Errorf("Sol Ring is in %v, want the graveyard", got)
	}
}
