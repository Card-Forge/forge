package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// LeaveBattlefield$ Exile (Animate, Pump, ChangeZone) makes the card go to
// exile instead of anywhere else it would leave the battlefield to: dying and
// being returned to hand alike (SpellAbilityEffect.java:618-650).
func TestLeaveBattlefieldExilesInsteadOfAnyDestination(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, grant, leave string
		want               engine.ZoneType
	}{
		{"animate then destroy", "DB$ Animate | ValidTgts$ Creature | Power$ 3 | Duration$ Permanent | LeaveBattlefield$ Exile", "DB$ Destroy | ValidTgts$ Creature", engine.Exile},
		{"pump then bounce", "DB$ Pump | ValidTgts$ Creature | NumAtt$ 1 | LeaveBattlefield$ Exile", "DB$ ChangeZone | ValidTgts$ Creature | Origin$ Battlefield | Destination$ Hand", engine.Exile},
		{"no grant bounces normally", "DB$ Pump | ValidTgts$ Creature | NumAtt$ 1", "DB$ ChangeZone | ValidTgts$ Creature | Origin$ Battlefield | Destination$ Hand", engine.Hand},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGame(t)
			victim := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			c := engine.NewScriptedController()
			targets := []engine.EntityID{engine.CardEntity(victim)}
			if _, err := resolveNow(t, g, p, c, targets, tc.grant); err != nil {
				t.Fatal(err)
			}
			if _, err := resolveNow(t, g, p, c, targets, tc.leave); err != nil {
				t.Fatal(err)
			}
			if got := g.Card(victim).Zone; got != tc.want {
				t.Errorf("the card is in %v, want %v", got, tc.want)
			}
		})
	}
}
