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

// Mode$ NoCleanupDamage, printed or granted by an Animate, keeps damage on the
// creature through the cleanup step (Switchgrass Grazer, Victory of the
// Pyrohammer); a creature without it loses its damage.
func TestNoCleanupDamageKeepsDamage(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name  string
		build func(t *testing.T, g *engine.Game, p engine.PlayerID) engine.CardID
		want  bool
	}{
		{"printed on the creature", func(t *testing.T, g *engine.Game, p engine.PlayerID) engine.CardID {
			def := creatureDefPT(t, "1", "9")
			def.Faces[0].Statics = append(def.Faces[0].Statics,
				scriptDef(t, "x", "Enchantment", "S:Mode$ NoCleanupDamage | ValidCard$ Card.Self").Faces[0].Statics...)
			return g.NewCard(def, p, engine.Battlefield)
		}, true},
		{"no static", func(t *testing.T, g *engine.Game, p engine.PlayerID) engine.CardID {
			return g.NewCard(creatureDefPT(t, "1", "9"), p, engine.Battlefield)
		}, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGame(t)
			creature := tc.build(t, g, p)
			g.Card(creature).Damage.Mark(2, false)
			advanceToPhase(t, g, engine.NewScriptedController(), 2, engine.Upkeep)
			if kept := g.Card(creature).Damage.Marked > 0; kept != tc.want {
				t.Errorf("damage kept through cleanup = %v, want %v", kept, tc.want)
			}
		})
	}
}
