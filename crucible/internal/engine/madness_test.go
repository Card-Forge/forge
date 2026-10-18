package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Madness (CR 702.35): "If you discard this card, discard it into exile.
// When you do, cast it for its madness cost or put it into your graveyard."
// keyword.Expand writes CardFactoryUtil's replacement (:2345-2362) and
// Mode$ Exiled trigger (:1386-1420); the discard replacement walk
// (replacementdiscard.go) runs the first, which fires the second.

// discardHand resolves "discard your hand" for p, as p's own effect.
func discardHand(t *testing.T, g *engine.Game, p engine.PlayerID, c *engine.ScriptedController) error {
	t.Helper()
	_, err := resolveNow(t, g, p, c, nil, "DB$ Discard | Defined$ You | Mode$ Hand")
	return err
}

// Cast for the madness cost: Fiery Temper, discarded, is exiled, then cast
// for {R} from exile, and goes to the graveyard once it resolves.
func TestMadnessCastsTheDiscardedCardForItsMadnessCost(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	temper := g.NewCard(corpusCard(t, "Fiery Temper"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Red, 1)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if err := discardHand(t, g, p, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 17 {
		t.Errorf("opponent life = %d, want 17 (Fiery Temper cast for its madness cost)", got)
	}
	if got := g.Player(p).ManaPool.Total(); got != 0 {
		t.Errorf("%d mana left, want the {R} madness cost paid", got)
	}
	if got := g.Card(temper).Zone; got != engine.Graveyard {
		t.Errorf("Fiery Temper is in %v, want the graveyard after resolving", got)
	}
}

// Declined: the card goes from exile to the graveyard, and the Discarded
// trigger still saw a discard.
func TestMadnessDeclinedPutsTheCardIntoTheGraveyard(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(scriptDef(t, "Discard Watcher", "Enchantment",
		"T:Mode$ Discarded | ValidCard$ Card.YouCtrl | TriggerZones$ Battlefield | Execute$ TrigGain",
		"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	temper := g.NewCard(corpusCard(t, "Fiery Temper"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	if err := discardHand(t, g, p, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(temper).Zone; got != engine.Graveyard {
		t.Errorf("Fiery Temper is in %v, want the graveyard", got)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("opponent life = %d, want 20 (not cast)", got)
	}
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("life = %d, want 21 (the discard still triggered)", got)
	}
}

// Count$Madness: From Under the Floorboards cast for {X}{B}{B} by madness
// makes X tokens and gains X life; cast from hand, three of each.
func TestCountMadnessReadsTheMadnessCast(t *testing.T) {
	t.Parallel()

	zombies := func(g *engine.Game, p engine.PlayerID) int {
		n := 0
		for _, id := range g.Zone(engine.Battlefield, p).Cards() {
			if g.Card(id).IsToken {
				n++
			}
		}
		return n
	}

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "From Under the Floorboards"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Black, 4)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)
	c.QueuePayX(2)
	c.QueuePayGeneric(mana.ShardB)
	c.QueuePayGeneric(mana.ShardB)
	if err := discardHand(t, g, p, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := zombies(g, p); got != 2 {
		t.Errorf("madness X=2: %d tokens, want 2", got)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("madness X=2: life %d, want 22", got)
	}

	g2, p2, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	floor := g2.NewCard(corpusCard(t, "From Under the Floorboards"), p2, engine.Hand)
	g2.Player(p2).ManaPool.Add(mana.Black, 5)
	c2 := engine.NewScriptedController()
	for range 3 {
		c2.QueuePayGeneric(mana.ShardB)
	}
	if !g2.CastSpell(p2, floor, c2) {
		t.Fatal("could not cast From Under the Floorboards")
	}
	if err := g2.ResolveStack(engine.NewRegistry(), c2); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := zombies(g2, p2); got != 3 {
		t.Errorf("cast from hand: %d tokens, want 3", got)
	}
}
