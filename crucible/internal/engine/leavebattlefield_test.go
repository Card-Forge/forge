package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
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

// Animate Incorporate$ adds a mana cost and its colors to the card for good
// (PerpetualIncorporate): the mana value and colors change, in any zone.
func TestIncorporateAddsCostAndColors(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	card := g.NewCard(creatureDefCost(t, "Plain", "2"), p, engine.Hand)
	if got := g.Card(card).CMC(); got != 2 {
		t.Fatalf("mana value before = %d, want 2", got)
	}
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(card)},
		"DB$ Animate | Defined$ Targeted | Incorporate$ 1 G | Duration$ Perpetual"); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(card).CMC(); got != 4 {
		t.Errorf("mana value after = %d, want 4", got)
	}
	if !g.Card(card).Colors().Has(mana.Green) {
		t.Errorf("colors = %v, want green", g.Card(card).Colors())
	}
	g.Move(card, engine.Graveyard, p)
	if got := g.Card(card).CMC(); got != 4 {
		t.Errorf("mana value after a zone change = %d, want it kept", got)
	}
}

// A Perpetual Animate's ManaCost$ replaces the card's mana cost for good and
// leaves its colors (Thought Partition); without Perpetual it does nothing.
func TestAnimateManaCostIsPerpetualOnly(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		duration string
		want     int
	}{{"Perpetual", 5}, {"Permanent", 2}} {
		g, p, _ := newTwoPlayerGame(t)
		card := g.NewCard(creatureDefCost(t, "Plain", "2"), p, engine.Hand)
		if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(card)},
			"DB$ Animate | Defined$ Targeted | ManaCost$ 5 | Duration$ "+tc.duration); err != nil {
			t.Fatal(err)
		}
		if got := g.Card(card).CMC(); got != tc.want {
			t.Errorf("%s: mana value = %d, want %d", tc.duration, got, tc.want)
		}
	}
}
