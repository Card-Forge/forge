package engine

// package engine, not engine_test (TEST-2's "invariant not observable from
// outside" row): these hold the player-property and UnlessCost$ halves of
// batch G to their own contract. No corpus card reaches them through a
// fixture: a Draw<N/Player.targetedBy> or Discard<N/Hand> UnlessCost$ sits
// behind a full ValidTgts$/UnlessSwitched$ chain (Perplex), and PlayerUID_<n> only
// appears once a chosen player is set, which no GameState key does.

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
)

func TestPlayerPropertyOpponentOfAndPlayerUID(t *testing.T) {
	t.Parallel()

	g := targetCandidatesTestGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]

	cases := []struct {
		spec      string
		cand      PlayerID
		wantMatch bool
		wantOK    bool
	}{
		{"Player.PlayerUID_2", b, true, true},
		{"Player.PlayerUID_2", a, false, true},
		{"Player.OpponentOf PlayerUID_1", b, true, true},
		{"Player.OpponentOf PlayerUID_1", a, false, true},
		{"Player.OpponentOf You", b, true, true},
		{"Player.OpponentOf Chosen", b, false, false},
		{"Player.PlayerUID_x", a, false, false},
	}
	for _, c := range cases {
		got, ok := matchesPlayerSpec(g, c.cand, a, NoCard, c.spec)
		if got != c.wantMatch || ok != c.wantOK {
			t.Errorf("matchesPlayerSpec(%q, %d) = %v, %v; want %v, %v", c.spec, c.cand, got, ok, c.wantMatch, c.wantOK)
		}
	}
}

func TestUnlessCostDrawTargetedByLimitsToTheAbilitysTargets(t *testing.T) {
	t.Parallel()

	g := targetCandidatesTestGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	src := g.NewCard(targetCandidatesTestCreature(t), a, Battlefield)

	uc, ok := parseUnlessCost("Draw<3/Player.targetedBy>")
	if !ok {
		t.Fatal("parseUnlessCost rejects Draw<3/Player.targetedBy>")
	}
	if got := g.unlessDrawers(a, src, uc); len(got) != 2 {
		t.Errorf("unowned cost: drawers = %v, want both players", got)
	}
	bound := uc.withTargets(&Ability{Targets: []EntityID{PlayerEntity(b)}})
	if got := g.unlessDrawers(a, src, bound); len(got) != 1 || got[0] != b {
		t.Errorf("bound to b: drawers = %v, want [b]", got)
	}
}

// Internal: withTriggered and unlessDrawers are unexported and no public
// ability constructor builds an unowned Draw cost (TEST-1).
func TestUnlessCostDrawActivatorLimitsToTheHostsController(t *testing.T) {
	t.Parallel()

	g := targetCandidatesTestGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	src := g.NewCard(targetCandidatesTestCreature(t), b, Battlefield)

	uc, ok := parseUnlessCost("Draw<1/Player.Activator>")
	if !ok {
		t.Fatal("parseUnlessCost rejects Draw<1/Player.Activator>")
	}
	bound, err := uc.withTriggered(g, &Ability{Controller: b, Source: src})
	if err != nil {
		t.Fatalf("withTriggered: %v", err)
	}
	if got := g.unlessDrawers(a, src, bound); len(got) != 1 || got[0] != b {
		t.Errorf("Activator drawers = %v, want [b]", got)
	}
	if _, ok := parseUnlessCost("Draw<1/Player.TriggeredPlayer>"); !ok {
		t.Error("parseUnlessCost rejects Draw<1/Player.TriggeredPlayer>")
	}
}

func TestUnlessCostDiscardHandIsAlwaysPayableAndEmptiesTheHand(t *testing.T) {
	t.Parallel()

	g := targetCandidatesTestGame(t, "a")
	p := g.Players()[0]
	src := g.NewCard(targetCandidatesTestCreature(t), p, Battlefield)
	g.NewCard(targetCandidatesTestCreature(t), p, Hand)
	g.NewCard(targetCandidatesTestCreature(t), p, Hand)

	uc, ok := parseUnlessCost("Discard<1/Hand>")
	if !ok || !uc.discardHand {
		t.Fatalf("parseUnlessCost(Discard<1/Hand>) = %+v, %v", uc, ok)
	}
	if !g.unlessPayable(p, src, uc) {
		t.Fatal("Discard<1/Hand> is not payable")
	}
	g.payUnlessParts(NewScriptedController(), &Ability{Source: src, Controller: p}, p, uc)
	if n := len(g.Zone(Hand, p).Cards()); n != 0 {
		t.Errorf("hand after paying = %d cards, want 0", n)
	}
	if n := len(g.Zone(Graveyard, p).Cards()); n != 2 {
		t.Errorf("graveyard after paying = %d cards, want 2", n)
	}
}

func TestTrimTargetSetPairwiseRestrictions(t *testing.T) {
	t.Parallel()

	g := targetCandidatesTestGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	c1 := g.NewCard(targetCandidatesTestCreature(t), a, Battlefield)
	c2 := g.NewCard(targetCandidatesTestCreature(t), a, Battlefield)
	c3 := g.NewCard(targetCandidatesTestCreature(t), b, Battlefield)
	all := []EntityID{CardEntity(c1), CardEntity(c2), CardEntity(c3)}

	for _, c := range []struct {
		param string
		want  int
	}{
		{"TargetsWithDifferentControllers", 2},
		{"TargetsWithDifferentNames", 1},
		{"TargetsWithDifferentCMC", 1},
		{"TargetsWithEqualToughness", 3},
		{"TargetsWithSameCardType", 3},
		{"MaxTotalTargetPower", 1},
	} {
		value := "True"
		if c.param == "MaxTotalTargetPower" {
			value = "3"
		}
		ab := &Ability{Source: c1, Params: &compile.Ability{Name: "ChangeZone", Params: []vocab.Param{{Key: c.param, Value: value}}}}
		if got := g.trimTargetSet(ab, all); len(got) != c.want {
			t.Errorf("%s: kept %d targets, want %d", c.param, len(got), c.want)
		}
	}
}
