package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// A trigger carrying Static$ True resolves at once, ahead of the stack, for
// every trigger mode (TriggerHandler.runWaitingTrigger, TriggerHandler.java:
// 300-309), not only for ChangesZone and TapsForMana: the watcher's life gain
// has happened by the time the action returns, with nothing of the watcher's on
// the stack. SpellCast (17 real lines), Drawn (5), LandPlayed (5), BecomeMonarch
// and the other modes share the one path.
func TestStaticTriggerResolvesInlineForEveryMode(t *testing.T) {
	t.Parallel()

	const gain = "DB$ GainLife | Defined$ You | LifeAmount$ 5"
	for _, tc := range []struct {
		name    string
		trigger string
		act     func(g *engine.Game, p engine.PlayerID, c engine.PlayerController)
		stack   int
	}{
		{"SpellCast", "Mode$ SpellCast | ValidCard$ Card | Static$ True | TriggerZones$ Battlefield | Execute$ Gain",
			func(g *engine.Game, p engine.PlayerID, c engine.PlayerController) {
				g.Player(p).ManaPool.Add(mana.Red, 1)
				spell := g.NewCard(gainInstant(t, "Gain One", "R", "1"), p, engine.Hand)
				if !g.CastSpell(p, spell, c) {
					t.Fatalf("CastSpell failed: %v", g.TakePendingError())
				}
			}, 1},
		{"Drawn", "Mode$ Drawn | ValidCard$ Card.YouCtrl | Static$ True | TriggerZones$ Battlefield | Execute$ Gain",
			func(g *engine.Game, p engine.PlayerID, c engine.PlayerController) {
				g.NewCard(corpusCard(t, "Forest"), p, engine.Library)
				g.DrawCards(p, 1, c)
			}, 0},
		{"LandPlayed", "Mode$ LandPlayed | ValidCard$ Land.YouCtrl | Static$ True | TriggerZones$ Battlefield | Execute$ Gain",
			func(g *engine.Game, p engine.PlayerID, c engine.PlayerController) {
				if !g.PlayLand(p, g.NewCard(corpusCard(t, "Forest"), p, engine.Hand), c) {
					t.Fatalf("PlayLand failed: %v", g.TakePendingError())
				}
			}, 0},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGame(t)
			g.NewCard(triggerWatcherDef(t, "Static Watcher", tc.trigger, "Gain", gain), p, engine.Battlefield)
			c := engine.NewScriptedController()

			tc.act(g, p, c)

			if got := g.Player(p).Life; got != 25 {
				t.Errorf("life = %d, want 25: the Static$ True trigger resolves inline", got)
			}
			if got := g.StackLen(); got != tc.stack {
				t.Errorf("stack holds %d items, want %d (only the cast spell, never the trigger)", got, tc.stack)
			}
		})
	}
}
