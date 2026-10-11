package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Welcome to . . .'s chapter I line (TargetsForEachPlayer$, TargetMax$
// OneEach, Duration$ AsLongAsControl), resolved with its target chosen: the
// opponent's noncreature artifact becomes a 0/4 defender and goes back when
// the host leaves (SpellAbilityEffect.java:1010-1013).
func TestAnimateTargetsForEachPlayerLineAsLongAsControl(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	stone := g.NewCard(artifactDefManaCost(t, "2"), other, engine.Battlefield)
	host, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(stone)},
		"DB$ Animate | ValidTgts$ Artifact.nonCreature+OppCtrl | TargetMin$ 0 | TargetMax$ OneEach | TargetsForEachPlayer$ True | Defined$ Targeted | Power$ 0 | Toughness$ 4 | Types$ Creature,Artifact,Wall | Keywords$ Defender | Duration$ AsLongAsControl",
		"OneEach", "PlayerCountOpponents$Amount")
	if err != nil {
		t.Fatal(err)
	}
	sba(g)
	s := g.Card(stone)
	if tg, _ := s.Toughness(); tg != 4 || !s.HasKeyword("Defender") {
		t.Fatalf("toughness %d defender %v, want a 0/4 defender", tg, s.HasKeyword("Defender"))
	}
	g.Move(host, engine.Graveyard, p)
	sba(g)
	if g.Card(stone).HasKeyword("Defender") {
		t.Error("the animation outlived its host")
	}
}

// The same line as a pushed trigger, so resolveTargets reads TargetMax$
// OneEach and TargetsForEachPlayer$ itself: the opponent's artifact is
// offered and taken.
func TestAnimateTargetsForEachPlayerTriggerChoosesThePerPlayerTarget(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	stone := g.NewCard(artifactDefManaCost(t, "2"), other, engine.Battlefield)
	def := etbChainDef(t, "Test Jurassic",
		"DB$ Animate | ValidTgts$ Artifact.nonCreature+OppCtrl | TargetMin$ 0 | TargetMax$ OneEach | TargetsForEachPlayer$ True | Defined$ Targeted | Power$ 0 | Toughness$ 4 | Types$ Creature,Artifact,Wall | Keywords$ Defender | Duration$ AsLongAsControl",
		"OneEach", "PlayerCountOpponents$Amount")
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(stone)})
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatal(err)
	}
	sba(g)
	if !g.Card(stone).HasKeyword("Defender") {
		t.Error("the per-player target was not animated")
	}
}
