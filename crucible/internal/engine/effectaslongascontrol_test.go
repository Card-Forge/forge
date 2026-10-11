package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Icefall Regent: its ETB taps an opponent's creature and an Effect card
// (ReplacementEffects$ DontUntap, Duration$ AsLongAsControl) keeps it tapped
// through its controller's untap steps while the Regent's caster controls the
// Regent (SpellAbilityEffect.java:1010-1013).
func TestEffectAsLongAsControlIcefallRegentHoldsTheCreatureTapped(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	regent := g.NewCard(corpusCard(t, "Icefall Regent"), p, engine.Hand)
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.Blue, 5)

	c := engine.NewScriptedController()
	for range 3 {
		c.QueuePayGeneric(mana.ShardU)
	}
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bears)})
	if !g.CastSpell(p, regent, c) {
		t.Fatal("CastSpell failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if !g.Card(bears).Tapped {
		t.Fatal("the ETB did not tap the Bears")
	}
	g.StartTurn(other, engine.NewScriptedController())
	if !g.Card(bears).Tapped {
		t.Error("the Bears untapped while the Regent is controlled")
	}
	g.Move(regent, engine.Graveyard, p)
	g.StartTurn(other, engine.NewScriptedController())
	if g.Card(bears).Tapped {
		t.Error("the Bears stayed tapped after the Regent left")
	}
}
