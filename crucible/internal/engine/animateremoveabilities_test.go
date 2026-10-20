package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Animate's RemoveAllAbilities$ (AnimateEffectBase.java:80-82, :222-226): the
// animated card loses every printed trait and every keyword for as long as the
// record lasts, and gets them back when it ends.
func TestAnimateRemoveAllAbilitiesEndsAtCleanup(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	creature := g.NewCard(scriptDef(t, "Winged Healer", "Creature Cleric",
		"K:Flying", "A:AB$ GainLife | Cost$ 1 | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	stealUntil(t, g, p, creature, "DB$ Animate | ValidTgts$ Creature | Power$ 0 | Toughness$ 1 | RemoveAllAbilities$ True")
	sba(g)
	if g.Card(creature).HasKeyword("Flying") {
		t.Error("the animated creature kept Flying")
	}
	if canActivateGrant(t, g, p, creature) {
		t.Error("the animated creature kept its printed ability")
	}
	if pw, _ := g.Card(creature).Power(); pw != 0 {
		t.Errorf("power = %d, want 0", pw)
	}
	endTurn(g, 1, p)
	g.SetTurnState(2, p, engine.Main1)
	sba(g)
	if !g.Card(creature).HasKeyword("Flying") {
		t.Error("Flying did not come back at cleanup")
	}
	if !canActivateGrant(t, g, p, creature) {
		t.Error("the printed ability did not come back at cleanup")
	}
}

// RemoveNonManaAbilities$ (:83-85) keeps the mana abilities but drops the
// keywords too, and a Permanent record never ends.
func TestAnimateRemoveNonManaAbilitiesKeepsManaAbilities(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	creature := g.NewCard(scriptDef(t, "Mana Healer", "Creature Cleric",
		"K:Flying", "A:AB$ Mana | Cost$ T | Produced$ G",
		"A:AB$ GainLife | Cost$ 1 | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	stealUntil(t, g, p, creature, "DB$ Animate | ValidTgts$ Creature | RemoveNonManaAbilities$ True | Duration$ Permanent")
	sba(g)
	if g.Card(creature).HasKeyword("Flying") {
		t.Error("the animated creature kept Flying")
	}
	g.Player(p).ManaPool.AddColorless(1)
	c := engine.NewScriptedController()
	if g.ActivateAbility(p, creature, 1, c) {
		t.Error("the non-mana ability survived")
	}
	if !g.ActivateManaAbility(p, creature, 0, c) {
		t.Error("the mana ability was removed")
	}
	endTurn(g, 1, p)
	sba(g)
	if g.Card(creature).HasKeyword("Flying") {
		t.Error("a Permanent removal ended at cleanup")
	}
}
