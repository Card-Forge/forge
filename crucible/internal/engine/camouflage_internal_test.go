package engine

// package engine, not engine_test (TEST-2's "invariant not observable from
// outside" row): a DeclareBlocker replacement's two replacing objects,
// Player (Defined$ ReplacedPlayer, who declares) and DefendingPlayer
// (Defined$ ReplacedDefendingPlayer, whose creatures block), are always the
// same player through the public API -- nothing in this port sets Java's
// Player.getDeclaresBlockers() redirect (Odric, Master Tactician), so
// declareBlockersReplaced records the defender for both. A swap of the two
// reads would pass every engine_test test. This file builds the replacing
// event by hand with two distinct players to hold each keyword, and
// Camouflage's own declarer/defender split, to its own value (ADR-0035).

import (
	"slices"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// camouflageRecorder answers every ChooseCardsForEffect with nothing and
// records who was asked and what was offered.
type camouflageRecorder struct {
	*ScriptedController
	deciders []PlayerID
	offers   [][]CardID
}

func (r *camouflageRecorder) ChooseCardsForEffect(_ *Game, p PlayerID, _ CardID, options []CardID, _, _ int) []CardID {
	r.deciders = append(r.deciders, p)
	r.offers = append(r.offers, append([]CardID(nil), options...))
	return nil
}

func TestReplacedPlayerAndReplacedDefendingPlayerReadTheirOwnObjects(t *testing.T) {
	t.Parallel()
	g := NewGame(nil, javarand.New(1), []string{"a", "b"})
	a, b := g.Players()[0], g.Players()[1]
	host := g.NewCard(targetCandidatesTestCreature(t), a, Battlefield)
	attacker := g.NewCard(targetCandidatesTestCreature(t), a, Battlefield)
	defending := g.NewCard(targetCandidatesTestCreature(t), b, Battlefield)
	g.SetTurnState(1, a, Main1)
	ac := NewScriptedController()
	ac.QueueAttackers([]CardID{attacker})
	if _, err := g.DeclareCombatAttackers(ac); err != nil {
		t.Fatalf("DeclareCombatAttackers: %v", err)
	}

	// Declarer a, defender b: the two replacing objects told apart.
	ev := &replacementEvent{result: replacementReplaced, player: a, defendingPlayer: b}
	ab := Ability{API: APICamouflage, Source: host, Controller: b, replacing: ev,
		Params: &compile.Ability{Name: "Camouflage", Params: []vocab.Param{
			{Key: "Defined", Value: "ReplacedPlayer"}, {Key: "Defender", Value: "ReplacedDefendingPlayer"},
		}}}

	for _, tc := range []struct {
		defined string
		want    PlayerID
	}{
		{"ReplacedPlayer", a},
		{"ReplacedDefendingPlayer", b},
	} {
		got, err := definedPlayers(g, ab.Controller, host, tc.defined, ab.refs())
		if err != nil || !slices.Equal(got, []PlayerID{tc.want}) {
			t.Errorf("Defined$ %s = %v, %v; want [%v]", tc.defined, got, err, tc.want)
		}
	}

	rec := &camouflageRecorder{ScriptedController: NewScriptedController()}
	if err := (camouflageEffect{}).Resolve(g, &ab, rec); err != nil {
		t.Fatalf("Resolve: %v", err)
	}
	if !slices.Equal(rec.deciders, []PlayerID{a}) {
		t.Errorf("deciders = %v, want the declarer %v", rec.deciders, a)
	}
	if len(rec.offers) != 1 || !slices.Equal(rec.offers[0], []CardID{defending}) {
		t.Errorf("offers = %v, want the defender's creature %v", rec.offers, defending)
	}
}
