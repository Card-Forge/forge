package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TargetsAtRandom$ (TargetRestrictions.java:202-204) has the game pick the
// targets: TargetSelection.java:123-137 draws TargetMin$ of them (or, with
// RandomNumTargets$, Aggregates.randomInt between TargetMin$ -- at least 1 --
// and the lesser of TargetMax$ and the candidates) one at a time with
// Aggregates.random over the remaining candidates, which draws nothing for a
// single one. Nobody is asked: no targets are queued on the controller.

// randomTapGame is a two-player game where the opponent controls n 1/1s, in
// creation order, and p's ETB trigger taps line's random targets.
func randomTapGame(t *testing.T, n int) (*engine.Game, engine.PlayerID, []engine.CardID) {
	t.Helper()
	g, p, other := newTwoPlayerGame(t)
	var opp []engine.CardID
	for range n {
		opp = append(opp, g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield))
	}
	return g, p, opp
}

// tapped is which of ids are tapped.
func tapped(g *engine.Game, ids []engine.CardID) []engine.CardID {
	var out []engine.CardID
	for _, id := range ids {
		if g.Card(id).Tapped {
			out = append(out, id)
		}
	}
	return out
}

func TestTargetsAtRandomDrawsLikeJava(t *testing.T) {
	t.Parallel()

	t.Run("one of three", func(t *testing.T) {
		t.Parallel()
		g, p, opp := randomTapGame(t, 3)
		want := *g.Rand()
		pick := opp[want.Int32n(3)]
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Polka", "DB$ Tap | ValidTgts$ Creature.OppCtrl | TargetsAtRandom$ True"),
			engine.NewScriptedController()); err != nil {
			t.Fatal(err)
		}
		if got := tapped(g, opp); len(got) != 1 || got[0] != pick {
			t.Errorf("tapped %v, want %v (nextInt(3) over the candidates)", got, pick)
		}
		if *g.Rand() != want {
			t.Error("the random pick drew more or less than one nextInt(3)")
		}
	})

	t.Run("a single candidate draws nothing", func(t *testing.T) {
		t.Parallel()
		g, p, opp := randomTapGame(t, 1)
		before := *g.Rand()
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Polka", "DB$ Tap | ValidTgts$ Creature.OppCtrl | TargetsAtRandom$ True"),
			engine.NewScriptedController()); err != nil {
			t.Fatal(err)
		}
		if got := tapped(g, opp); len(got) != 1 {
			t.Errorf("tapped %v, want the one candidate", got)
		}
		if *g.Rand() != before {
			t.Error("a single candidate drew from the RNG")
		}
	})

	t.Run("a random number of targets", func(t *testing.T) {
		t.Parallel()
		g, p, opp := randomTapGame(t, 3)
		want := *g.Rand()
		num := int(want.Int32n(3)) + 1 // randomInt(1, min(3, TargetMax$ 3))
		left := append([]engine.CardID(nil), opp...)
		var picks []engine.CardID
		for range num {
			i := 0
			if len(left) > 1 {
				i = int(want.Int32n(int32(len(left))))
			}
			picks = append(picks, left[i])
			left = append(left[:i:i], left[i+1:]...)
		}
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Catapult",
			"DB$ Tap | ValidTgts$ Creature.OppCtrl | TargetMin$ 0 | TargetMax$ 3 | TargetsAtRandom$ True | RandomNumTargets$ True"),
			engine.NewScriptedController()); err != nil {
			t.Fatal(err)
		}
		if got := tapped(g, opp); len(got) != len(picks) {
			t.Errorf("tapped %v, want %d random targets %v", got, len(picks), picks)
		}
		for _, id := range picks {
			if !g.Card(id).Tapped {
				t.Errorf("random target %v not tapped", id)
			}
		}
		if *g.Rand() != want {
			t.Error("the draws differ from randomInt then one Aggregates.random per target")
		}
	})

	t.Run("a SubAbility link", func(t *testing.T) {
		t.Parallel()
		g, p, opp := randomTapGame(t, 2)
		want := *g.Rand()
		pick := opp[want.Int32n(2)]
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Struggle", "DB$ GainLife | Defined$ You | LifeAmount$ 1 | SubAbility$ DBTap",
			"DBTap", "DB$ Tap | ValidTgts$ Creature.OppCtrl | TargetsAtRandom$ True"), engine.NewScriptedController()); err != nil {
			t.Fatal(err)
		}
		if got := tapped(g, opp); len(got) != 1 || got[0] != pick {
			t.Errorf("tapped %v, want %v", got, pick)
		}
	})
}

// A random count whose lower bound (TargetMin$) exceeds the candidates makes
// Java's nextInt throw; here the ability fails when it resolves, on its root
// or on the link that names it.
func TestTargetsAtRandomImpossibleCountFails(t *testing.T) {
	t.Parallel()

	for name, def := range map[string][]string{
		"root": {"DB$ Tap | ValidTgts$ Creature.OppCtrl | TargetMin$ 2 | TargetMax$ 2 | TargetsAtRandom$ True | RandomNumTargets$ True"},
		"link": {"DB$ GainLife | Defined$ You | LifeAmount$ 1 | SubAbility$ DBTap",
			"DBTap", "DB$ Tap | ValidTgts$ Creature.OppCtrl | TargetMin$ 2 | TargetMax$ 2 | TargetsAtRandom$ True | RandomNumTargets$ True"},
	} {
		t.Run(name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := randomTapGame(t, 1)
			_, err := castETBChain(t, g, p, etbChainDef(t, "Bad", def[0], def[1:]...), engine.NewScriptedController())
			if err == nil || !strings.Contains(err.Error(), "RandomNumTargets$") {
				t.Errorf("ResolveStack = %v, want an error naming RandomNumTargets$", err)
			}
		})
	}
}
