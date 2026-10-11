package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Defined$ ParentTargetedController names the controller of the card a parent
// ability targeted (AbilityUtils.java:987-993). Draconic Roar's second
// DealDamage uses it to hit the creature's controller, and only with a Dragon
// (ConditionCheckSVar$ Count$Presence_Dragon.1.0).
func TestParentTargetedControllerIsTheTargetsController(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name   string
		dragon bool
		life   int
	}{
		{"with a Dragon", true, 17},
		{"without a Dragon", false, 20},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			victim := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Battlefield)
			if tc.dragon {
				g.NewCard(corpusCard(t, "Shivan Dragon"), p, engine.Battlefield)
			}
			roar := g.NewCard(corpusCard(t, "Draconic Roar"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Red, 2)
			c := engine.NewScriptedController()
			c.QueueConfirmPayCost(false)
			c.QueuePayGeneric(mana.ShardR)
			c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
			if !g.CastSpell(p, roar, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Player(other).Life; got != tc.life {
				t.Errorf("the creature's controller has %d life, want %d", got, tc.life)
			}
		})
	}
}
