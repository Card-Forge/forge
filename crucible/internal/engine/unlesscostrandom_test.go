package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestUnlessCostDiscardRandom proves Discard<1/Random> (CostDiscard's Random
// type; Balduvian Horde): the payer discards one hand card they do not choose,
// and with an empty hand the cost cannot be paid.
func TestUnlessCostDiscardRandom(t *testing.T) {
	t.Parallel()

	t.Run("paid", func(t *testing.T) {
		t.Parallel()
		g, p, c, cast := unlessCostGame(t, "Discard<1/Random>")
		held := g.NewCard(creatureDef(t), p, engine.Hand)
		c.QueueConfirmPayCost(true)
		creature := cast()
		if g.Card(creature).Zone != engine.Battlefield || g.Card(held).Zone != engine.Graveyard {
			t.Errorf("creature %v, hand card %v, want Battlefield and Graveyard", g.Card(creature).Zone, g.Card(held).Zone)
		}
	})
	t.Run("empty hand", func(t *testing.T) {
		t.Parallel()
		g, _, c, cast := unlessCostGame(t, "Discard<1/Random>")
		c.QueueConfirmPayCost(true)
		if creature := cast(); g.Card(creature).Zone != engine.Graveyard {
			t.Errorf("creature zone = %v, want Graveyard: nothing to discard", g.Card(creature).Zone)
		}
	})
}

// TestUnlessCostExileGraveyardAll proves ExileFromGrave<1/All> (Grip of
// Amnesia): the whole graveyard is exiled, and an empty one pays it too
// (CostExile.java:178-179).
func TestUnlessCostExileGraveyardAll(t *testing.T) {
	t.Parallel()

	t.Run("exiles everything", func(t *testing.T) {
		t.Parallel()
		g, p, c, cast := unlessCostGame(t, "ExileFromGrave<1/All>")
		a := g.NewCard(creatureDef(t), p, engine.Graveyard)
		b := g.NewCard(creatureDef(t), p, engine.Graveyard)
		c.QueueConfirmPayCost(true)
		creature := cast()
		if g.Card(creature).Zone != engine.Battlefield || g.Card(a).Zone != engine.Exile || g.Card(b).Zone != engine.Exile {
			t.Errorf("creature %v, graveyard %v and %v, want Battlefield and Exile twice", g.Card(creature).Zone, g.Card(a).Zone, g.Card(b).Zone)
		}
	})
	t.Run("empty graveyard still pays", func(t *testing.T) {
		t.Parallel()
		g, _, c, cast := unlessCostGame(t, "ExileFromGrave<1/All>")
		c.QueueConfirmPayCost(true)
		if creature := cast(); g.Card(creature).Zone != engine.Battlefield {
			t.Errorf("creature zone = %v, want Battlefield: exiling nothing pays", g.Card(creature).Zone)
		}
	})
}

// TestUnlessCostExileTopGraveyardCreature proves
// ExileFromGrave<1/Card.TopGraveyardCreature> (Barrow Ghoul): only the last
// creature of the graveyard qualifies.
func TestUnlessCostExileTopGraveyardCreature(t *testing.T) {
	t.Parallel()

	g, p, c, cast := unlessCostGame(t, "ExileFromGrave<1/Card.TopGraveyardCreature>")
	first := g.NewCard(creatureDef(t), p, engine.Graveyard)
	top := g.NewCard(creatureDef(t), p, engine.Graveyard)
	c.QueueConfirmPayCost(true)
	c.QueueCardChoice([]engine.CardID{first})
	creature := cast()
	if g.Card(creature).Zone != engine.Battlefield || g.Card(top).Zone != engine.Exile || g.Card(first).Zone != engine.Graveyard {
		t.Errorf("creature %v, top %v, first %v, want Battlefield, Exile and Graveyard", g.Card(creature).Zone, g.Card(top).Zone, g.Card(first).Zone)
	}
}
