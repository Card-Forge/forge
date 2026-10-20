package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Pump KWChoice$ has the activator pick one keyword for the target, and
// Optional$ lets them decline the whole pump (PumpEffect.java:280-301).
func TestPumpKeywordChoiceAndOptional(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name     string
		line     string
		confirm  bool
		option   int
		wantKW   string
		wantNone bool
	}{
		{"second keyword", "DB$ Pump | ValidTgts$ Creature | KWChoice$ Flying,Lifelink | Duration$ Permanent", true, 1, "Lifelink", false},
		{"first keyword", "DB$ Pump | ValidTgts$ Creature | KWChoice$ Flying,Lifelink | Duration$ Permanent", true, 0, "Flying", false},
		{"optional declined", "DB$ Pump | ValidTgts$ Creature | KW$ Flying | Optional$ True | Duration$ Permanent", false, 0, "Flying", true},
		{"optional accepted", "DB$ Pump | ValidTgts$ Creature | KW$ Flying | Optional$ True | Duration$ Permanent", true, 0, "Flying", false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGame(t)
			victim := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			c := engine.NewScriptedController()
			c.QueueConfirmEffect(tc.confirm)
			c.QueueOption(tc.option)
			if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(victim)}, tc.line); err != nil {
				t.Fatal(err)
			}
			sba(g)
			if got := g.Card(victim).HasKeyword(tc.wantKW); got == tc.wantNone {
				t.Errorf("%s granted = %v, want %v", tc.wantKW, got, !tc.wantNone)
			}
		})
	}
}

// Pump with TgtZone$ Graveyard targets a card there and PumpZone$ Graveyard
// pumps it: "target instant card in your graveyard gains flashback" (Flashback).
func TestPumpTargetsACardInTheGraveyard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	bolt := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Graveyard)
	if g.Card(bolt).HasKeyword("Flashback") {
		t.Fatal("the bolt has flashback before the grant")
	}
	c := engine.NewScriptedController()
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(bolt)},
		"DB$ Pump | ValidTgts$ Instant.YouOwn,Sorcery.YouOwn | TgtZone$ Graveyard | KW$ Flashback | PumpZone$ Graveyard"); err != nil {
		t.Fatal(err)
	}
	if !g.Card(bolt).HasKeyword("Flashback") {
		t.Fatal("the bolt did not gain flashback")
	}
	// the granted flashback casts it for its mana cost, and Move drops the
	// grant with the zone change
	cast := engine.NewScriptedController()
	cast.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	g.Player(p).ManaPool.Add(mana.Red, 1)
	if !g.CastSpell(p, bolt, cast) {
		t.Fatal("the bolt could not be cast from the graveyard with its granted flashback")
	}
}
