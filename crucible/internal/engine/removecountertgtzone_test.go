package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// RemoveCounter reads TgtZone$ as a targeting zone: a card in exile with time
// counters is a legal target ("Permanent or suspended card").
func TestRemoveCounterTargetsACardInExile(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	exiled := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Exile)
	g.Card(exiled).Counters.Add("TIME", 3)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(exiled)})
	def := etbChainDef(t, "Test Time Eater",
		"DB$ RemoveCounter | ValidTgts$ Card.inZoneExile | TgtZone$ Exile | CounterType$ TIME | CounterNum$ 1")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(exiled).Counters.Count("TIME"); got != 2 {
		t.Errorf("time counters = %d, want 2", got)
	}
}
