package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Defined$ wrapping another Defined$: a "Base.Prop" filter keeps the base's
// cards that match Card.Prop, and "A & B" joins both lists, each card once.
func TestDefinedCardsFilterAndJoin(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name      string
		defined   string
		wantPower int
	}{
		{"filter keeps the creature", "Targeted.Creature", 3},
		{"filter drops everything", "Targeted.Land", 1},
		{"join lists a card once", "Targeted.Creature & Targeted", 3},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGame(t)
			creature := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			// a non-creature second target, so the filter has something to drop
			other := g.NewCard(artifactDefT(t), p, engine.Battlefield)
			if _, err := resolveNow(t, g, p, engine.NewScriptedController(),
				[]engine.EntityID{engine.CardEntity(creature), engine.CardEntity(other)},
				"DB$ Pump | Defined$ "+tc.defined+" | NumAtt$ +2 | Duration$ Permanent"); err != nil {
				t.Fatal(err)
			}
			if got, _ := g.Card(creature).Power(); got != tc.wantPower {
				t.Errorf("creature power = %d, want %d", got, tc.wantPower)
			}
		})
	}
}
