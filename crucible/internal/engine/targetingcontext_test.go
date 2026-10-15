package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// While a triggered ability's targets are chosen, it is the ability in
// context: Java hands that SpellAbility to every calculateAmount its target
// bounds and division read, so they see its triggering objects
// (TriggerObjectsAttackers$Amount) before it ever resolves.

// TestCaseOfTheMarketMeleeDividesTheAttackersCount proves Case of the Market
// Melee's solved trigger, its real script: TargetMax$ and DividedAsYouChoose$
// are both TriggerObjectsAttackers$Amount, read as the targets are chosen. Two
// attackers allow two targets and two damage; one target takes it all, two
// take one each.
func TestCaseOfTheMarketMeleeDividesTheAttackersCount(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name          string
		creature      bool
		wantLife      int
		wantCreatureD int
	}{
		{"one target", false, 18, 0},
		{"two targets", true, 19, 1},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			melee := g.NewCard(corpusCard(t, "Case of the Market Melee"), p, engine.Battlefield)
			g.Card(melee).Solved = true
			a1 := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			a2 := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			victim := g.NewCard(creatureDefPT(t, "0", "4"), other, engine.Battlefield)

			c := engine.NewScriptedController()
			c.QueueAttackers([]engine.CardID{a1, a2})
			chosen := []engine.EntityID{engine.PlayerEntity(other)}
			if tc.creature {
				chosen = append(chosen, engine.CardEntity(victim))
			}
			c.QueueTargets(chosen)
			declareAttackers(t, g, c)
			if got := g.StackLen(); got != 1 {
				t.Fatalf("StackLen() = %d, want 1 -- the solved trigger must go on the stack", got)
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := [2]int{g.Player(other).Life, g.Card(victim).Damage.Marked}; got != [2]int{tc.wantLife, tc.wantCreatureD} {
				t.Errorf("(defender life, creature damage) = %v, want [%d %d]", got, tc.wantLife, tc.wantCreatureD)
			}
		})
	}
}
