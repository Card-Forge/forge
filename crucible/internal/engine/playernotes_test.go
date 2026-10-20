package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Pump NoteCards$ + NoteCardsFor$ notes a card on the player, and the
// NotedFor<key> property reads it back: only the noted card matches, until
// ClearNotedCardsFor$ wipes the key again.
func TestPumpNoteCardsIsReadByNotedFor(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	noted := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	unnoted := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(noted)},
		"DB$ Pump | Defined$ You | NoteCards$ Targeted | NoteCardsFor$ Stargate"); err != nil {
		t.Fatal(err)
	}
	const boost = "DB$ PumpAll | ValidCards$ Creature.NotedForStargate | NumAtt$ 2 | Duration$ Permanent"
	if _, err := resolveNow(t, g, p, c, nil, boost); err != nil {
		t.Fatal(err)
	}
	sba(g)
	if got, _ := g.Card(noted).Power(); got != 3 {
		t.Errorf("noted creature power = %d, want 3", got)
	}
	if got, _ := g.Card(unnoted).Power(); got != 1 {
		t.Errorf("unnoted creature power = %d, want 1", got)
	}
	if _, err := resolveNow(t, g, p, c, nil, "DB$ Pump | Defined$ You | ClearNotedCardsFor$ Stargate"); err != nil {
		t.Fatal(err)
	}
	if _, err := resolveNow(t, g, p, c, nil, boost); err != nil {
		t.Fatal(err)
	}
	sba(g)
	if got, _ := g.Card(noted).Power(); got != 3 {
		t.Errorf("after clearing the notes, power = %d, want 3 still", got)
	}
}
