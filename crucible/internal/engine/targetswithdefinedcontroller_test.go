package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// ChangeZone's TargetsWithDefinedController$ has two readers in Java: the
// targeting restriction (SpellAbility.canTarget, SpellAbility.java:1405-1416)
// and, for a Shuffle$ True move into a library, whose library is shuffled
// (ChangeZoneEffect.java:873-890) -- the defined players, even with no target
// chosen.

// TestGaeasBlessingWithNoCardsShufflesTheTargetPlayersLibrary is Gaea's
// Blessing with zero cards chosen: the ParentTarget player's library is
// shuffled, not the caster's, and the caster still draws.
func TestGaeasBlessingWithNoCardsShufflesTheTargetPlayersLibrary(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	libraryCards(t, g, other, 12)
	libraryCards(t, g, p, 12)
	theirs := slices.Clone(g.Zone(engine.Library, other).Cards())
	mine := slices.Clone(g.Zone(engine.Library, p).Cards())
	blessing := g.NewCard(corpusCard(t, "Gaea's Blessing"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 2)

	c := engine.NewScriptedController()
	c.QueueConfirmPayCost(false)
	c.QueuePayGeneric(mana.ShardG)
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueTargets(nil)
	if !g.CastSpell(p, blessing, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if slices.Equal(g.Zone(engine.Library, other).Cards(), theirs) {
		t.Error("target player's library is in its old order, want it shuffled")
	}
	if got := g.Zone(engine.Library, p).Cards(); !slices.Equal(got, mine[1:]) {
		t.Errorf("caster's library = %v, want the old order minus the drawn top card %v", got, mine[1:])
	}
}

// TestUpToTargetsWithNoCandidateStillResolves proves an "up to" choice
// (TargetMin$ 0) over an empty candidate pool still goes on the stack
// (TargetSelection.java:82, 113-120), resolves with no target rather than
// fizzling, and does not fall back to Defined$'s default Self.
func TestUpToTargetsWithNoCandidateStillResolves(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	host := resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Pump | ValidTgts$ Creature.OppCtrl | TargetMin$ 0 | NumAtt$ +2 | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 3")
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23: the ability must resolve with no target", got)
	}
	if pw, _ := g.Card(host).Power(); pw != 1 {
		t.Errorf("host power = %d, want its base 1: no target must not pump Self", pw)
	}
}

// TestChangeZoneTargetsWithDefinedControllerRestrictsTargets is Deadwood
// Treefolk's return shape with the controller spec written as You: a creature
// card in the opponent's graveyard is not a legal target, one in p's is, and
// it returns.
func TestChangeZoneTargetsWithDefinedControllerRestrictsTargets(t *testing.T) {
	t.Parallel()

	const line = "DB$ ChangeZone | Origin$ Graveyard | Destination$ Hand | ValidTgts$ Creature | TargetsWithDefinedController$ You"

	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(creatureDef(t), p, engine.Graveyard)
	g.NewCard(creatureDef(t), other, engine.Graveyard)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	resolveLine(t, g, p, c, line)
	if z := g.Card(mine).Zone; z != engine.Hand {
		t.Errorf("p's creature card is in %v, want Hand", z)
	}

	// Only the opponent's card: no legal target, so nothing returns.
	g2, p2, other2 := newTwoPlayerGame(t)
	theirs := g2.NewCard(creatureDef(t), other2, engine.Graveyard)
	c2 := engine.NewScriptedController()
	c2.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
	resolveLine(t, g2, p2, c2, line)
	if z := g2.Card(theirs).Zone; z != engine.Graveyard {
		t.Errorf("opponent's creature card is in %v, want it left in the Graveyard", z)
	}
}

// TestChangeZoneTargetsWithDefinedControllerUnknownSpecIsAnError proves a
// controller spec the shuffle cannot resolve fails the move loudly (GO-7)
// rather than shuffling nobody.
func TestChangeZoneTargetsWithDefinedControllerUnknownSpecIsAnError(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	card := g.NewCard(creatureDef(t), p, engine.Graveyard)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(card)},
		"DB$ ChangeZone | Origin$ Graveyard | Destination$ Library | Shuffle$ True | ValidTgts$ Card | TargetsWithDefinedController$ Bogus")
	if err == nil || !strings.Contains(err.Error(), "TargetsWithDefinedController$") {
		t.Errorf("ResolveStack = %v, want an error naming TargetsWithDefinedController$", err)
	}
}
