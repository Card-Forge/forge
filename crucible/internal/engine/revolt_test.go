package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Revolt (CardTraitBase.java:323-330) gates the replacements that carry it as
// much as the triggers: Greenwheel Liberator's etbCounter extra and Aether
// Revolt's damage replacement read the same turn record.

// Greenwheel Liberator enters with two +1/+1 counters only once a permanent its
// controller controlled left the battlefield this turn.
func TestRevoltGatesAnEntersWithCountersReplacement(t *testing.T) {
	t.Parallel()

	for _, revolt := range []bool{true, false} {
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		g.SetTurnState(1, p, engine.Main1)
		if revolt {
			g.Move(addN(t, g, p, engine.Battlefield, 1, "Artifact")[0], engine.Graveyard, p)
		}
		liberator := g.NewCard(corpusCard(t, "Greenwheel Liberator"), p, engine.Hand)
		g.Player(p).ManaPool.Add(mana.Green, 2)
		c := engine.NewScriptedController()
		c.QueuePayGeneric(mana.ShardG)
		castThenResolve(t, g, p, liberator, c)
		if err := g.TakePendingError(); err != nil {
			t.Fatalf("revolt=%v: pending error: %v", revolt, err)
		}
		want := 0
		if revolt {
			want = 2
		}
		if got := g.Card(liberator).Counters.Count(engine.P1P1); got != want {
			t.Errorf("revolt=%v: %d +1/+1 counters, want %d", revolt, got, want)
		}
	}
}

// Aether Revolt adds 2 to noncombat damage a source its controller controls
// deals to an opponent, but only with revolt.
func TestRevoltGatesADamageReplacement(t *testing.T) {
	t.Parallel()

	for _, revolt := range []bool{true, false} {
		g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
		g.SetTurnState(1, p, engine.Main1)
		g.NewCard(corpusCard(t, "Aether Revolt"), p, engine.Battlefield)
		if revolt {
			g.Move(addN(t, g, p, engine.Battlefield, 1, "Artifact")[0], engine.Graveyard, p)
		}
		resolveLine(t, g, p, engine.NewScriptedController(), "DB$ DealDamage | Defined$ Opponent | NumDmg$ 1")
		want := 19
		if revolt {
			want = 17
		}
		if got := g.Player(other).Life; got != want {
			t.Errorf("revolt=%v: opponent at %d life, want %d", revolt, got, want)
		}
	}
}
