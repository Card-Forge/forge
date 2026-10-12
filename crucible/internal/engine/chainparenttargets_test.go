package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// A sub-ability's TargetsWithDefinedController$ ParentTarget is read while the
// spell is cast, when the link's targets are chosen (CR 601.2c), so the link
// already sees the targets its parent chose (SpellAbility.getParentTargetingPlayer).
// Dwell on the Past targets a player, then up to four cards in that player's
// graveyard. Before, the link could not name the parent's player at cast time
// and the spell could not be cast at all.
func TestChainLinkSeesItsParentsTargetsWhileCasting(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Graveyard)
	dwell := g.NewCard(corpusCard(t, "Dwell on the Past"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 1)

	c := engine.NewScriptedController()
	c.QueueConfirmPayCost(false)
	c.QueuePayGeneric(mana.ShardG)
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueTargets(nil)
	if !g.CastSpell(p, dwell, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
}
