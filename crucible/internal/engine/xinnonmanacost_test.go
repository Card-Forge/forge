package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// A spell whose Cost$ names X in a part past mana (Bond of Agony: "X B
// PayLife<X>") announces one X, pays it in both places, and reads it back as
// Count$xPaid (PlaySpellAbility.announceValuesLikeX, java:766-782). The answer
// is held to what the non-mana part can pay (PlayerControllerHuman:376-381):
// life for PayLife<X>.
func TestXAnnouncedForACostPartIsSharedAndBounded(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name               string
		life, ask          int
		wantYou, wantOther int
	}{
		{"X within the life total", 20, 3, 17, 17},
		{"X above the life total is held to it", 3, 9, 0, 17},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			g.Player(p).Life = tc.life
			bond := g.NewCard(corpusCard(t, "Bond of Agony"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Black, 30)
			c := engine.NewScriptedController()
			c.QueueConfirmPayCost(false)
			c.QueuePayX(tc.ask)
			for range min(tc.ask, tc.life) {
				c.QueuePayGeneric(mana.ShardB)
			}
			if !g.CastSpell(p, bond, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Player(p).Life; got != tc.wantYou {
				t.Errorf("caster has %d life, want %d", got, tc.wantYou)
			}
			if got := g.Player(other).Life; got != 20-min(tc.ask, tc.life) {
				t.Errorf("opponent has %d life, want %d", got, 20-min(tc.ask, tc.life))
			}
		})
	}
}
