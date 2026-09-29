package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// TestMatchesNamedProperty covers CardProperty.java:62-68's named<Name>:
// the rest of the property is a card name, `;` standing for a `,` and `_`
// for a space -- the IsPresent2$ shape every Meld trigger gates on
// (gisela_the_broken_blade_brisela_voice_of_nightmares.txt).
func TestMatchesNamedProperty(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	bruna := g.NewCard(corpusCard(t, "Bruna, the Fading Light"), p, engine.Battlefield)
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)

	for _, tc := range []struct {
		spec string
		id   engine.CardID
		want bool
	}{
		{"Creature.namedBruna; the Fading Light", bruna, true},
		{"Creature.YouCtrl+YouOwn+namedBruna; the Fading Light", bruna, true},
		{"Creature.namedBruna;_the_Fading_Light", bruna, true},
		{"Creature.namedBruna; the Fading Light", bears, false},
		{"Creature.namedGrizzly Bears", bears, true},
		{"Creature.!namedGrizzly Bears", bears, false},
		{"Creature.named", bears, false},
	} {
		if got := engine.Matches(g, g.Card(tc.id), valid.Parse(tc.spec), p, engine.NoCard); got != tc.want {
			t.Errorf("%s on %s = %v, want %v", tc.spec, g.Card(tc.id).Def.Name, got, tc.want)
		}
	}
}

// TestMatchesNamedSplitHalfOffBattlefield covers Card.sharesNameWith's
// split-card branch (Card.java:5853-5860): off the battlefield a split
// card also has each half's name.
func TestMatchesNamedSplitHalfOffBattlefield(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	def := corpusCard(t, "Fire")
	if def.SplitType != carddb.SplitSplit {
		t.Fatalf("Fire // Ice split type = %v, want Split", def.SplitType)
	}
	fireIce := g.NewCard(def, p, engine.Hand)
	for _, name := range []string{def.Faces[0].Name, def.Faces[carddb.FaceAlternate].Name} {
		if !engine.Matches(g, g.Card(fireIce), valid.Parse("Card.named"+name), p, engine.NoCard) {
			t.Errorf("Card.named%s did not match Fire // Ice in hand", name)
		}
	}
	if engine.Matches(g, g.Card(fireIce), valid.Parse("Card.namedNot A Half"), p, engine.NoCard) {
		t.Error("Card.namedNot A Half matched Fire // Ice")
	}
}
