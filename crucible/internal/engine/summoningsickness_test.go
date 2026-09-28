package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// landDefWithAbility is a Land carrying one A: line.
func landDefWithAbility(t *testing.T, name, abilityText string) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Land")
	raw.Faces[0].Abilities = []string{abilityText}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// TestSummoningSicknessSparesNonCreatureTapAbility proves Card.isSick
// (Card.java:3651-3653, CR 302.6): a Land that entered this turn pays a
// {T} cost, for a stack ability and for a mana ability alike.
func TestSummoningSicknessSparesNonCreatureTapAbility(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	land := g.NewCard(landDefWithAbility(t, "Test Tap Land", "AB$ GainLife | Cost$ T | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	g.Card(land).SummonSick = true
	c := engine.NewScriptedController()
	if !g.ActivateAbility(p, land, 0, c) {
		t.Fatal("ActivateAbility declined a Land's {T} ability the turn it entered")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if life := g.Player(p).Life; life != 21 {
		t.Errorf("life = %d, want 21", life)
	}

	manaLand := g.NewCard(landDefWithAbility(t, "Test Mana Land", "AB$ Mana | Cost$ T | Produced$ C"), p, engine.Battlefield)
	g.Card(manaLand).SummonSick = true
	if !g.ActivateManaAbility(p, manaLand, 0, c) {
		t.Error("ActivateManaAbility declined a Land's {T} mana ability the turn it entered")
	}
}
