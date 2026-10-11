package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Mode$ DrawFromBottom (Lantern of Undersight) makes its controller draw the
// last card of the library instead of the first, and only its controller: the
// static's ValidPlayer$ is You (StaticAbilityDrawFromBottom.java).
func TestDrawFromBottomDrawsTheLastCard(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name    string
		lantern bool
		forOpp  bool
		wantEnd bool
	}{
		{"no Lantern draws the top", false, false, false},
		{"Lantern draws the bottom", true, false, true},
		{"the opponent's Lantern does not change our draw", true, true, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			first := g.NewCard(corpusCard(t, "Forest"), p, engine.Library)
			g.NewCard(corpusCard(t, "Island"), p, engine.Library)
			last := g.NewCard(corpusCard(t, "Mountain"), p, engine.Library)
			if tc.lantern {
				owner := p
				if tc.forOpp {
					owner = other
				}
				g.NewCard(corpusCard(t, "Lantern of Undersight"), owner, engine.Battlefield)
			}

			g.DrawCards(p, 1, engine.NewScriptedController())

			want := first
			if tc.wantEnd {
				want = last
			}
			hand := g.Zone(engine.Hand, p).Cards()
			if len(hand) != 1 || hand[0] != want {
				t.Errorf("hand = %v, want [%v]", hand, want)
			}
		})
	}
}
