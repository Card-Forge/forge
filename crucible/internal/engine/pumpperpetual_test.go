package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Duration$ Perpetual (PumpEffect.java:37, :58-68; Card.addPerpetual): the
// boost and keywords sit on the card in any zone and survive cleanup and every
// zone change (GameAction.java:265-266), unlike a plain until-end-of-turn
// pump, which Move drops.
func TestPumpPerpetualStaysOnTheCardAcrossZonesAndTurns(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, line string
		targets    func(card engine.CardID) []engine.EntityID
	}{
		{"Pump", "DB$ Pump | ValidTgts$ Card | PumpZone$ Hand | NumAtt$ +2 | NumDef$ +1 | KW$ Flash | Duration$ Perpetual",
			func(c engine.CardID) []engine.EntityID { return []engine.EntityID{engine.CardEntity(c)} }},
		{"PumpAll", "DB$ PumpAll | ValidCards$ Creature | PumpZone$ Hand | NumAtt$ +2 | NumDef$ +1 | KW$ Flash | Duration$ Perpetual",
			func(engine.CardID) []engine.EntityID { return nil }},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			card := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Hand)
			if _, err := resolveNow(t, g, p, engine.NewScriptedController(), tc.targets(card), tc.line); err != nil {
				t.Fatal(err)
			}
			check := func(when string) {
				t.Helper()
				sba(g)
				pw, _ := g.Card(card).Power()
				tg, _ := g.Card(card).Toughness()
				if pw != 4 || tg != 3 || !g.Card(card).HasKeyword("Flash") {
					t.Errorf("%s: %d/%d flash=%v, want 4/3 with Flash", when, pw, tg, g.Card(card).HasKeyword("Flash"))
				}
			}
			check("in hand")
			endTurn(g, 1, p)
			check("after cleanup")
			g.Move(card, engine.Battlefield, p)
			check("on the battlefield")
			g.Move(card, engine.Graveyard, p)
			check("in the graveyard")
			g.Move(card, engine.Battlefield, p)
			check("back on the battlefield")
		})
	}
}

// A plain Duration$ Permanent pump does not outlive the battlefield.
func TestPumpPermanentDoesNotSurviveAZoneChange(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	card := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(card)},
		"DB$ Pump | ValidTgts$ Creature | NumAtt$ +2 | Duration$ Permanent"); err != nil {
		t.Fatal(err)
	}
	g.Move(card, engine.Graveyard, p)
	g.Move(card, engine.Battlefield, p)
	sba(g)
	if pw, _ := g.Card(card).Power(); pw != 2 {
		t.Errorf("power = %d, want 2", pw)
	}
}
