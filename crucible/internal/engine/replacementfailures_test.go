package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestReplacementChoiceOutOfRangeIsARecordedControllerError proves an answer
// outside the candidates ChooseReplacementEffect was offered records a pending
// error (GO-7) and falls back to the first candidate.
func TestReplacementChoiceOutOfRangeIsARecordedControllerError(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	for range 3 {
		g.NewCard(nil, p, engine.Library)
	}
	g.NewCard(replacementEnchantmentDefWithSVar(t, "Test Twice One",
		"Event$ Draw | ActiveZones$ Battlefield | ValidPlayer$ You | ReplaceWith$ Two | Description$ One.",
		"Two", "DB$ Draw | Defined$ You | NumCards$ 2"), p, engine.Battlefield)
	g.NewCard(replacementEnchantmentDefWithSVar(t, "Test Twice Two",
		"Event$ Draw | ActiveZones$ Battlefield | ValidPlayer$ You | ReplaceWith$ Two | Description$ Two.",
		"Two", "DB$ Draw | Defined$ You | NumCards$ 2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueReplacementEffect(9)
	g.DrawCards(p, 1, c)
	if g.TakePendingError() == nil {
		t.Error("an out-of-range replacement choice recorded no pending error")
	}
}

// TestReplacementAbilityErrorsAreRecorded proves a ReplaceWith$ ability the
// Registry refuses (ChangeZone's Duration$ UntilYourNextTurn is unresolved) records a pending
// error for the face-change, Destroy and Counter dispatches instead of
// applying half of it.
func TestReplacementAbilityErrorsAreRecorded(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(replacementEnchantmentDefWithSVar(t, "Test Refused Destroy",
		"Event$ Destroy | ActiveZones$ Battlefield | ReplaceWith$ Rm", "Rm", "DB$ ChangeZone | Defined$ ReplacedCard | Destination$ Exile | Duration$ UntilYourNextTurn"), p, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	def := etbChainDef(t, "Destroyer", "DB$ Destroy | Defined$ Targeted | ValidTgts$ Creature")
	host := g.NewCard(def, p, engine.Battlefield)
	sub := def.Faces[0].Triggers[0].Subs[0].Ability
	g.PushAbility(engine.Ability{API: engine.APIDestroy, Source: host, Controller: p, Params: sub,
		Targets: []engine.EntityID{engine.CardEntity(victim)}})
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err == nil {
		t.Error("no pending error for the refused ReplaceWith$ ability")
	}
	if g.Card(victim).Zone != engine.Graveyard {
		t.Errorf("victim zone = %v, want Graveyard: the refused replacement must not apply", g.Card(victim).Zone)
	}
}
