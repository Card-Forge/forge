package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// "As this enters, choose Mirran or Phyrexian" (Mirrodin Besieged, the
// Sieges): a GenericChoice with SetChosenMode$ runs before the move, records
// the chosen ability's description on the card (ChooseGenericEffect.java:
// 120-122), and the statics keyed on Card.ChosenMode<X> grant that mode's
// trigger only.
func TestSiegeChosenModeGrantsThatModesTrigger(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name  string
		pick  int
		mode  string
		myrs  int
		other string
	}{
		{"Mirran", 0, "Mirran", 1, "Phyrexian"},
		{"Phyrexian", 1, "Phyrexian", 0, "Mirran"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			siege := g.NewCard(corpusCard(t, "Mirrodin Besieged"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Blue, 3)
			c := engine.NewScriptedController()
			c.QueuePayGeneric(mana.ShardU)
			c.QueuePayGeneric(mana.ShardU)
			c.QueueAbilityChoice([]int{tc.pick})
			if err := castEntrant(t, g, p, siege, c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Card(siege).Memory.ChosenMode(); got != tc.mode {
				t.Fatalf("chosen mode = %q, want %q", got, tc.mode)
			}
			if !engine.Matches(g, g.Card(siege), valid.Parse("Card.ChosenMode"+tc.mode), p, siege) ||
				engine.Matches(g, g.Card(siege), valid.Parse("Card.ChosenMode"+tc.other), p, siege) {
				t.Errorf("Card.ChosenMode%s does not read the %s choice alone", tc.mode, tc.mode)
			}

			ring := g.NewCard(corpusCard(t, "Sol Ring"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Blue, 1)
			cast := engine.NewScriptedController()
			cast.QueuePayGeneric(mana.ShardU)
			if !g.CastSpell(p, ring, cast) {
				t.Fatal("could not cast Sol Ring")
			}
			if err := g.ResolveStack(engine.NewRegistry(), cast); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			myrs := 0
			for _, id := range g.Zone(engine.Battlefield, p).Cards() {
				if g.Card(id).IsToken {
					myrs++
				}
			}
			if myrs != tc.myrs {
				t.Errorf("Myr tokens = %d, want %d", myrs, tc.myrs)
			}
		})
	}
}

// Archangel of Strife's choices are Pumps that note cards for players
// (NoteCards$), which the entry chain does not run: refused, not skipped.
func TestArchangelOfStrifeChoiceStaysRefused(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	angel := g.NewCard(corpusCard(t, "Archangel of Strife"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 7)
	c := engine.NewScriptedController()
	for range 5 {
		c.QueuePayGeneric(mana.ShardW)
	}
	if err := castEntrant(t, g, p, angel, c); err == nil {
		t.Error("Archangel of Strife's war-or-peace choice resolved; its NoteCards$ Pumps are not ported")
	}
}
