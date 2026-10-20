package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Mode$ ManaConvert AnyType->AnyColor lets green mana pay a {R} creature spell,
// for the player and the spells the line's ValidPlayer$ and ValidCard$ name; a
// line with a conversion or param this port does not read changes nothing.
func TestManaConvertSpendsAnyColorOnNamedSpells(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		line string
		want bool
	}{
		{"none", "S:Mode$ Continuous | Affected$ Card.Self | AddPower$ 0", false},
		{"any color for you", "S:Mode$ ManaConvert | ValidPlayer$ You | ManaConversion$ AnyType->AnyColor", true},
		{"any color for everyone", "S:Mode$ ManaConvert | ManaConversion$ AnyType->AnyColor", true},
		{"any type for creature spells", "S:Mode$ ManaConvert | ValidPlayer$ You | ValidCard$ Creature.YouCtrl | ValidSA$ Spell | ManaConversion$ AnyType->AnyType", true},
		{"planeswalker spells only", "S:Mode$ ManaConvert | ValidPlayer$ You | ValidCard$ Planeswalker.YouCtrl | ValidSA$ Spell | ManaConversion$ AnyType->AnyColor", false},
		{"opponents only", "S:Mode$ ManaConvert | ValidPlayer$ Opponent | ManaConversion$ AnyType->AnyColor", false},
		{"activated abilities only", "S:Mode$ ManaConvert | ValidPlayer$ You | ValidSA$ Activated | ManaConversion$ AnyType->AnyColor", false},
		{"one color to another is unread", "S:Mode$ ManaConvert | ValidPlayer$ You | ManaConversion$ Green->Red", false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGame(t)
			g.NewCard(scriptDef(t, "Test Rule", "Enchantment", tc.line), p, engine.Battlefield)
			spell := g.NewCard(creatureDefCost(t, "Red One", "R"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Green, 1)
			c := engine.NewScriptedController()
			c.QueuePayGeneric(mana.ShardG)
			if got := g.CastSpell(p, spell, c); got != tc.want {
				t.Errorf("casting {R} with green = %v, want %v", got, tc.want)
			}
		})
	}
}
