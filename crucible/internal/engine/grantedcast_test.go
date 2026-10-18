package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These port GrantedCastTest: a MayPlay$ grant that only changes how a card is
// cast (MayPlayDontGrantZonePermissions$) never reaches a card the caster
// could not otherwise cast, and one that grants the zone (Heist, Mnemonic
// Betrayal) still lets a second, cost-only grant ride on top of it.

// castFree casts card for pid choosing the option at pick, with an empty pool.
func castFree(t *testing.T, g *engine.Game, pid engine.PlayerID, card engine.CardID, pick int, targets ...engine.EntityID) bool {
	t.Helper()
	c := engine.NewScriptedController()
	c.QueueOption(pick)
	if len(targets) > 0 {
		c.QueueTargets(targets)
	}
	return g.CastSpell(pid, card, c)
}

// As Foretold ("once each turn, pay {0} for a spell of mana value X or less,
// X the time counters on it"): it discounts my own card in hand, and does not
// reach a card in an opponent's hand.
func TestACostReductionAloneDoesNotReachAnotherHand(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	af := g.NewCard(corpusCard(t, "As Foretold"), p, engine.Battlefield)
	g.Card(af).Counters.Add(engine.CounterType("TIME"), 5)
	mine := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	theirs := g.NewCard(corpusCard(t, "Lightning Bolt"), other, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Red, 3)
	sba(g)

	if castFree(t, g, p, theirs, 0, engine.PlayerEntity(other)) {
		t.Fatal("cast a card out of the opponent's hand")
	}
	// Option 0 is the normal cast, option 1 the {0} one: with the mana in the
	// pool the free one still costs nothing.
	before := g.Player(p).ManaPool.Breakdown()
	if !castFree(t, g, p, mine, 1, engine.PlayerEntity(other)) {
		t.Fatal("could not cast my own Bolt for {0} under As Foretold")
	}
	if g.Player(p).ManaPool.Breakdown() != before {
		t.Error("the {0} cast spent mana")
	}
}

// MayPlayLimit$ 1: As Foretold's discount works once each turn.
func TestMayPlayLimitAllowsOneDiscountedCastPerTurn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	af := g.NewCard(corpusCard(t, "As Foretold"), p, engine.Battlefield)
	g.Card(af).Counters.Add(engine.CounterType("TIME"), 5)
	first := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	second := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	sba(g)

	if !castFree(t, g, p, first, 1, engine.PlayerEntity(other)) {
		t.Fatal("the first discounted cast failed")
	}
	// Only the normal cast is left, so there is nothing to choose between and
	// with no mana it fails (no option is asked).
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if g.CastSpell(p, second, c) {
		t.Error("a second cast went through As Foretold's once-a-turn discount")
	}
}

// Weftwalking: the first spell each player casts on their turn may be cast
// without paying its mana cost -- by the active player, whoever controls the
// enchantment, and only for their own cards.
func TestWeftwalkingFreeCastReachesOnlyTheActivePlayersCards(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Weftwalking"), other, engine.Battlefield)
	mine := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	theirs := g.NewCard(corpusCard(t, "Lightning Bolt"), other, engine.Hand)
	sba(g)

	if castFree(t, g, p, theirs, 1, engine.PlayerEntity(other)) {
		t.Error("cast a card out of the opponent's hand")
	}
	if !castFree(t, g, p, mine, 1, engine.PlayerEntity(other)) {
		t.Fatal("could not cast my own Bolt free under Weftwalking")
	}
}

// Weftwalking's grant is for the first spell only (Count$ThisTurnCast_...
// EQ0): after one spell has been cast this turn the free option is gone.
func TestWeftwalkingIsForTheFirstSpellOnly(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Weftwalking"), other, engine.Battlefield)
	first := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	second := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	sba(g)

	if !castFree(t, g, p, first, 1, engine.PlayerEntity(other)) {
		t.Fatal("the first spell could not be cast free")
	}
	sba(g)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if g.CastSpell(p, second, c) {
		t.Error("the second spell of the turn was cast free")
	}
}

// heistABolt has p heist other's library, which holds one nonland card (a
// Lightning Bolt) under some lands, so the pick is known; it returns the card,
// now in exile.
func heistABolt(t *testing.T, g *engine.Game, p, other engine.PlayerID) engine.CardID {
	t.Helper()
	for range 3 {
		g.NewCard(corpusCard(t, "Mountain"), other, engine.Library)
	}
	bolt := g.NewCard(corpusCard(t, "Lightning Bolt"), other, engine.Library)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{bolt})
	if err := resolveTargeting(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)}, "DB$ Heist | ValidTgts$ Opponent"); err != nil {
		t.Fatalf("Heist: %v", err)
	}
	if g.Card(bolt).Zone != engine.Exile {
		t.Fatalf("the heisted Bolt is in %v, want Exile", g.Card(bolt).Zone)
	}
	sba(g)
	return bolt
}

// Heist grants the exile zone and lets the heister spend mana of any type;
// Grenzo's once-a-turn {0} rides on top of it as a second option
// (aCostReductionRidesOnTopOfAGrant).
func TestAGrenzoDiscountRidesOnTopOfAHeistGrant(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Grenzo, Crooked Jailer"), p, engine.Battlefield)
	heisted := heistABolt(t, g, p, other)

	// Two options: Grenzo's {0} (a MayPlay$ static, listed first) and Heist's
	// any-type cast. Picking the first spends no mana at all.
	if !castFree(t, g, p, heisted, 0, engine.PlayerEntity(other)) {
		t.Fatal("could not cast the heisted card through Grenzo's {0}")
	}
}

// A Heist grant on its own still lets the heister spend any type of mana: a
// Bolt ({R}) is paid with blue mana.
func TestAHeistGrantLetsTheHeisterSpendAnyTypeOfMana(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	heisted := heistABolt(t, g, p, other)
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueuePayGeneric(mana.ShardU)
	if !g.CastSpell(p, heisted, c) {
		t.Fatal("could not cast the heisted Bolt with blue mana")
	}
}

// Mnemonic Betrayal exiles every card in the opponents' graveyards and grants
// (an Effect card, MayPlay$ with MayPlayIgnoreType$) the right to cast them
// this turn spending any type of mana: a grant that reaches into another
// player's zone still works (aGrantThatReachesTheZoneStillWorks).
func TestAGrantThatReachesTheZoneStillWorks(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	prize := g.NewCard(corpusCard(t, "Lightning Bolt"), other, engine.Graveyard)
	betrayal := g.NewCard(corpusCard(t, "Mnemonic Betrayal"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	g.Player(p).ManaPool.Add(mana.Black, 1)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardG)
	if !g.CastSpell(p, betrayal, c) {
		t.Fatal("could not cast Mnemonic Betrayal")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(prize).Zone; got != engine.Exile {
		t.Fatalf("the opponent's Bolt is in %v, want Exile", got)
	}
	sba(g)

	g.Player(p).ManaPool.Add(mana.Blue, 1)
	cast := engine.NewScriptedController()
	cast.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	cast.QueuePayGeneric(mana.ShardU)
	if !g.CastSpell(p, prize, cast) {
		t.Fatal("could not cast the exiled Bolt with blue mana")
	}
}

// Two limited grants are two options: a cast through one uses up only that
// static, so two As Foretolds allow two discounted casts in a turn.
func TestTwoLimitedGrantsAllowTwoDiscountedCasts(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	for range 2 {
		af := g.NewCard(corpusCard(t, "As Foretold"), p, engine.Battlefield)
		g.Card(af).Counters.Add(engine.CounterType("TIME"), 5)
	}
	first := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	second := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	sba(g)
	// Options: the normal cast, then each As Foretold's {0}.
	if !castFree(t, g, p, first, 1, engine.PlayerEntity(other)) {
		t.Fatal("the first discounted cast failed")
	}
	if !castFree(t, g, p, second, 1, engine.PlayerEntity(other)) {
		t.Error("the second As Foretold did not allow a second discounted cast")
	}
}
