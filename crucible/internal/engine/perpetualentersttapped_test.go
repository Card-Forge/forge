package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// ReplacementHandlerTest#testPerpetualEntersTappedReplacementEffect: a card
// that perpetually gained "this permanent enters tapped" (Boareskyr
// Tollkeeper's Animate with Duration$ Perpetual and Replacements$) enters the
// battlefield tapped, once, from the grant row it kept while in hand.
func TestPerpetualEntersTappedReplacementTapsTheCardAsItEnters(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	toll := g.NewCard(corpusCard(t, "Boareskyr Tollkeeper"), p, engine.Hand)
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardW)
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueCardChoice([]engine.CardID{bears})
	if !g.CastSpell(p, toll, c) {
		t.Fatal("could not cast Boareskyr Tollkeeper")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	g.SetTurnState(2, other, engine.Main1)
	g.Player(other).ManaPool.Add(mana.Green, 2)
	oc := engine.NewScriptedController()
	oc.QueuePayGeneric(mana.ShardG)
	if !g.CastSpell(other, bears, oc) {
		t.Fatal("could not cast Grizzly Bears")
	}
	if err := g.ResolveStack(engine.NewRegistry(), oc); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(bears).Zone; got != engine.Battlefield {
		t.Fatalf("Grizzly Bears is in %v, want Battlefield", got)
	}
	if !g.Card(bears).Tapped {
		t.Error("the card with the perpetual enters-tapped replacement entered untapped")
	}
}

// A perpetual replacement of an event nothing reads off a granted card would
// be inert, so it is refused when granted (GO-7).
func TestPerpetualReplacementOfAnotherShapeFailsClosed(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	err := resolveWith(t, g, p, engine.NewScriptedController(),
		"DB$ Animate | Defined$ Self | Duration$ Perpetual | Replacements$ Rep",
		"Rep", "Event$ DamageDone | ValidSource$ Card.Self | ReplaceWith$ Exiled",
		"Exiled", "DB$ ReplaceEffect | VarName$ DamageAmount | VarValue$ ReplaceCount$DamageAmount/Plus.2")
	if err == nil {
		t.Error("granted a perpetual replacement no consumer reads")
	}
}

// A Permanent (not Perpetual) Animate granting "if this creature would die,
// exile it instead" (Can't Stay Away): the creature that is destroyed ends up
// in exile.
func TestAnimateGrantsADiesReplacement(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(bears)},
		"DB$ Animate | ValidTgts$ Creature | Replacements$ ReplaceDies | Duration$ Permanent",
		"ReplaceDies", "Event$ Moved | ActiveZones$ Battlefield | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Card.Self | ReplaceWith$ Exile",
		"Exile", "DB$ ChangeZone | Origin$ Battlefield | Destination$ Exile | Defined$ ReplacedCard"); err != nil {
		t.Fatal(err)
	}
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(bears)}, "DB$ Destroy | ValidTgts$ Creature"); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(bears).Zone; got != engine.Exile {
		t.Errorf("destroyed creature is in %v, want Exile", got)
	}
}
