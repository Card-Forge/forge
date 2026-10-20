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

// The Animate-granted "You may spend mana as though it were mana of any color
// to cast this spell" (Undercity Plunder's SpendAnyMana, EffectZone$ Stack)
// lets off-color mana pay the card it was granted to; the same static printed
// without EffectZone$ on a card in hand is not active there and discounts
// nothing.
func TestManaConvertEffectZoneStackGrantAppliesToItsOwnCast(t *testing.T) {
	t.Parallel()

	const grant = "DB$ Animate | Defined$ Targeted | staticAbilities$ SpendAnyMana | Duration$ Perpetual"
	const spend = "Mode$ ManaConvert | EffectZone$ Stack | ValidPlayer$ You | ValidCard$ Card.Self | ValidSA$ Spell | ManaConversion$ AnyType->AnyColor"

	t.Run("granted Stack static", func(t *testing.T) {
		t.Parallel()

		g, p, _ := newTwoPlayerGame(t)
		spell := g.NewCard(creatureDefCost(t, "Red One", "R"), p, engine.Hand)
		if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(spell)},
			grant, "SpendAnyMana", spend); err != nil {
			t.Fatal(err)
		}
		sba(g)
		g.Player(p).ManaPool.Add(mana.Green, 1)
		c := engine.NewScriptedController()
		c.QueuePayGeneric(mana.ShardG)
		if !g.CastSpell(p, spell, c) {
			t.Error("the granted spell could not be cast with green mana for {R}")
		}
	})
	t.Run("printed default-zone static on a card in hand", func(t *testing.T) {
		t.Parallel()

		g, p, _ := newTwoPlayerGame(t)
		def := creatureDefCost(t, "Red One", "R")
		def.Faces[0].Statics = append(def.Faces[0].Statics,
			scriptDef(t, "x", "Enchantment", "S:Mode$ ManaConvert | ValidPlayer$ You | ValidCard$ Card.Self | ManaConversion$ AnyType->AnyColor").Faces[0].Statics...)
		spell := g.NewCard(def, p, engine.Hand)
		g.Player(p).ManaPool.Add(mana.Green, 1)
		c := engine.NewScriptedController()
		c.QueuePayGeneric(mana.ShardG)
		if g.CastSpell(p, spell, c) {
			t.Error("a static that is only active on the battlefield discounted its own cast from hand")
		}
	})
}
