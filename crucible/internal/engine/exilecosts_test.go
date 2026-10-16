package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// The exile cost parts CostExile reads past the graveyard: Exile<N/Type>
// (permanents the payer controls, its battlefield default) and
// ExileFromHand<N/Type> (cards from the hand, never the source itself).

// Lunar Hatchling escapes for {4}{G}{U}, a land it exiles from the
// battlefield and five other cards from the graveyard; without a land the
// escape is not offered.
func TestEscapeExilesALandFromTheBattlefield(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	hatchling := g.NewCard(corpusCard(t, "Lunar Hatchling"), p, engine.Graveyard)
	fodder := addN(t, g, p, engine.Graveyard, 5, "Artifact")
	addMana := func() {
		g.Player(p).ManaPool.Add(mana.Green, 5)
		g.Player(p).ManaPool.Add(mana.Blue, 1)
	}
	addMana()
	if g.CastSpell(p, hatchling, engine.NewScriptedController()) {
		t.Fatal("escaped with no land to exile")
	}
	land := g.NewCard(corpusCard(t, "Forest"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	for range 4 {
		c.QueuePayGeneric(mana.ShardG)
	}
	c.QueueCardChoice(fodder)
	c.QueueCardChoice([]engine.CardID{land})
	castThenResolve(t, g, p, hatchling, c)
	if got := g.Card(hatchling).Zone; got != engine.Battlefield {
		t.Fatalf("Lunar Hatchling in %v, want Battlefield", got)
	}
	if got := g.Card(land).Zone; got != engine.Exile {
		t.Errorf("the Forest in %v, want Exile", got)
	}
	for _, id := range fodder {
		if got := g.Card(id).Zone; got != engine.Exile {
			t.Errorf("escape fodder in %v, want Exile", got)
		}
	}
}

// An UnlessCost$ with an ExileFromHand part: the payer exiles a matching card
// from their hand to stop the effect; without one the payment fails and the
// effect happens.
func TestUnlessCostExilesFromHand(t *testing.T) {
	t.Parallel()

	for _, hasCard := range []bool{true, false} {
		g, p, other := newTwoPlayerGame(t)
		var card []engine.CardID
		if hasCard {
			card = addN(t, g, other, engine.Hand, 1, "Artifact")
		}
		c := engine.NewScriptedController()
		c.QueueConfirmPayCost(true)
		if hasCard {
			c.QueueCardChoice(card)
		}
		resolveLine(t, g, p, c, "DB$ LoseLife | Defined$ Opponent | LifeAmount$ 3 | UnlessCost$ ExileFromHand<1/Artifact> | UnlessPayer$ Opponent")
		want := 17
		if hasCard {
			want = 20
			if got := g.Card(card[0]).Zone; got != engine.Exile {
				t.Errorf("the paid card in %v, want Exile", got)
			}
		}
		if got := g.Player(other).Life; got != want {
			t.Errorf("hasCard=%v: opponent at %d, want %d", hasCard, got, want)
		}
	}
}
