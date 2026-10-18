package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// TestRemovingASpellKeepsItsCastTrigger proves the three ways a spell leaves
// the stack without resolving take the spell's own item and nothing else:
// Counter (CounterEffect.java:258, MagicStack.remove(si)), RemoveFromGame
// (GameAction.ceaseToExist, GameAction.java:970-972) and a Counter replaced by
// an Event$ Counter ReplaceWith$ exile (Guile; ChangeZoneEffect.java:577-579).
// The last two go through MagicStack.remove(Card) (MagicStack.java:799-805),
// which removes spell items only. The spell's own "when you cast this spell"
// trigger has the spell's card as its source; it stays on the stack and still
// resolves, so its controller gains 3 life.
func TestRemovingASpellKeepsItsCastTrigger(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name     string
		remover  string
		guile    bool
		wantZone engine.ZoneType
	}{
		{"counter", "DB$ Counter | TargetType$ Spell | ValidTgts$ Card", false, engine.Graveyard},
		{"remove from game", "DB$ RemoveFromGame | ValidTgts$ Card", false, engine.None},
		{"counter replaced by exile", "DB$ Counter | TargetType$ Spell | ValidTgts$ Card", true, engine.Exile},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGame(t)
			g.NewCard(watcherDef(t, "Remover", "Mode$ SpellCast | ValidActivatingPlayer$ Opponent | TriggerZones$ Battlefield",
				tc.remover), p, engine.Battlefield)
			if tc.guile {
				g.NewCard(replacementEnchantmentDefWithSVar(t, "Test Guile",
					"Event$ Counter | ActiveZones$ Battlefield | ValidSA$ Spell | ValidCause$ SpellAbility.YouCtrl | ReplaceWith$ Rm",
					"Rm", "DB$ ChangeZone | Defined$ ReplacedCard | Origin$ Stack | Destination$ Exile"), p, engine.Battlefield)
			}
			spell := g.NewCard(castSelfTriggerDef(t, ""), other, engine.Hand)
			g.SetTurnState(1, other, engine.Main1)
			g.Player(other).ManaPool.Add(mana.Green, 1)
			c := engine.NewScriptedController()
			c.QueueTargets([]engine.EntityID{engine.CardEntity(spell)})
			if !g.CastSpell(other, spell, c) {
				t.Fatal("cast failed")
			}
			// The spell, its own cast trigger (the active player's, pushed first)
			// and the remover's trigger above both.
			if got := g.StackLen(); got != 3 {
				t.Fatalf("stack = %d items after the cast, want 3", got)
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatal(err)
			}
			if got := g.Card(spell).Zone; got != tc.wantZone {
				t.Errorf("spell zone = %v, want %v", got, tc.wantZone)
			}
			if got := g.Player(other).Life; got != 23 {
				t.Errorf("caster life = %d, want 23: the spell's cast trigger must survive its spell's removal", got)
			}
		})
	}
}
