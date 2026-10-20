package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// The damage prevention keywords: "Prevent all damage that would be dealt by
// CARDNAME." stops the card's damage (to a player here), the same keyword on
// another card changes nothing (CardFactoryUtil.java:2541-2570).
func TestPreventDamageKeywordStopsTheCardsDamage(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name     string
		keyword  string
		wantLife int
	}{
		{"no keyword", "K:Flying", 17},
		{"dealt by", "K:Prevent all damage that would be dealt by CARDNAME.", 20},
		{"dealt to is not dealt by", "K:Prevent all damage that would be dealt to CARDNAME.", 17},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGame(t)
			burner := g.NewCard(scriptDef(t, "Burner", "Creature Elf", tc.keyword,
				"A:AB$ DealDamage | Cost$ 0 | Defined$ You | NumDmg$ 3"), p, engine.Battlefield)
			c := engine.NewScriptedController()
			if !g.ActivateAbility(p, burner, 0, c) {
				t.Fatal("ActivateAbility refused")
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatal(err)
			}
			if got := g.Player(p).Life; got != tc.wantLife {
				t.Errorf("life = %d, want %d", got, tc.wantLife)
			}
		})
	}
}

// Dromoka's Command, mode one: an Effect targeting a spell on the stack grants
// it "Prevent all damage that would be dealt by CARDNAME." for as long as it
// stays there, so the Lightning Bolt deals nothing.
func TestEffectPreventsDamageOfATargetedSpell(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	bolt := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	g.Player(p).ManaPool.Add(mana.Red, 1)
	if !g.CastSpell(p, bolt, c) {
		t.Fatal("could not cast the bolt")
	}
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(bolt)},
		"DB$ Effect | ValidTgts$ Instant,Sorcery | TgtZone$ Stack | StaticAbilities$ PreventDmg | ExileOnMoved$ Stack | RememberObjects$ TargetedSource",
		"PreventDmg", "Mode$ Continuous | AffectedZone$ Stack | Affected$ Card.IsRemembered | AddKeyword$ Prevent all damage that would be dealt by CARDNAME."); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("the bolt's target has %d life, want 20 (damage prevented)", got)
	}
}
