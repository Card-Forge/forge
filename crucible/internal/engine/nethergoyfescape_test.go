package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Nethergoyf's escape: {2}{B} and exile any number of other cards from your
// graveyard with four or more card types among them
// (ExileFromGrave<X/Card.Other+withTypesGE4>). CostExile.canPay counts the
// types among every valid card (CostExile.java:215-228); the payer picks one
// or more of them with four or more types (HumanCostDecision.java:323-336).

// nethergoyfGraveyard puts Nethergoyf and the named cards in p's graveyard and
// gives p {2}{B}.
func nethergoyfGraveyard(t *testing.T, names ...string) (*engine.Game, engine.PlayerID, engine.CardID, []engine.CardID) {
	t.Helper()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	goyf := g.NewCard(corpusCard(t, "Nethergoyf"), p, engine.Graveyard)
	var others []engine.CardID
	for _, n := range names {
		others = append(others, g.NewCard(corpusCard(t, n), p, engine.Graveyard))
	}
	g.Player(p).ManaPool.Add(mana.Black, 3)
	return g, p, goyf, others
}

func TestNethergoyfEscapesExilingFourCardTypes(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		pick int // how many of the five cards the payer picks
		want int // how many end up exiled
	}{
		{"exactly the four types", 4, 4},
		{"short of four types is replaced by every card", 2, 5},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, goyf, others := nethergoyfGraveyard(t, "Grizzly Bears", "Forest", "Lightning Bolt", "Sol Ring", "Llanowar Elves")
			c := engine.NewScriptedController()
			c.QueuePayGeneric(mana.ShardB)
			c.QueuePayGeneric(mana.ShardB)
			c.QueueCardChoice(others[:tc.pick])
			if !g.CastSpell(p, goyf, c) {
				t.Fatal("Nethergoyf's escape was not cast")
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Card(goyf).Zone; got != engine.Battlefield {
				t.Errorf("Nethergoyf is in %v, want the battlefield", got)
			}
			if got := len(g.Zone(engine.Exile, p).Cards()); got != tc.want {
				t.Errorf("%d cards exiled, want %d", got, tc.want)
			}
		})
	}
}

// Three card types in the graveyard: the escape cannot be paid, so it is not
// offered.
func TestNethergoyfEscapeNeedsFourCardTypes(t *testing.T) {
	t.Parallel()
	g, p, goyf, _ := nethergoyfGraveyard(t, "Grizzly Bears", "Forest", "Lightning Bolt")
	if g.CastSpell(p, goyf, engine.NewScriptedController()) {
		t.Fatal("Nethergoyf escaped with three card types among the other cards")
	}
	if got := g.Card(goyf).Zone; got != engine.Graveyard {
		t.Errorf("Nethergoyf is in %v, want the graveyard", got)
	}
}
