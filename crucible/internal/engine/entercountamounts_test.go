package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// An entering permanent's "enters with a counter for each ..." amounts count
// the state before it arrived (Count$LastStateBattlefield), draft notes read as
// none and Count$ExactManaCost counts distinct mana costs.
func TestEnterCounterAmountsReadTheLastState(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name  string
		svar  string
		setup func(t *testing.T, g *engine.Game, p engine.PlayerID)
		want  int
	}{
		{"creatures in play before it", "Count$LastStateBattlefield Creature.YouCtrl",
			func(t *testing.T, g *engine.Game, p engine.PlayerID) {
				g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
				g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			}, 2},
		{"creature cards in the graveyard", "Count$LastStateGraveyard Creature.YouCtrl",
			func(t *testing.T, g *engine.Game, p engine.PlayerID) {
				g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
			}, 1},
		{"draft notes", "Count$DraftNotesCount.Some Card", func(*testing.T, *engine.Game, engine.PlayerID) {}, 0},
		{"different mana costs in the graveyard", "Count$ExactManaCostGraveyard Creature.YouOwn",
			func(t *testing.T, g *engine.Game, p engine.PlayerID) {
				g.NewCard(creatureDefCost(t, "A", "G"), p, engine.Graveyard)
				g.NewCard(creatureDefCost(t, "B", "G"), p, engine.Graveyard)
				g.NewCard(creatureDefCost(t, "C", "1 U"), p, engine.Graveyard)
			}, 2},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			tc.setup(t, g, p)
			def := scriptDef(t, "Counter Counter", "Creature Elf",
				"K:etbCounter:P1P1:X:no Condition:enters with counters", "SVar:X:"+tc.svar)
			card := g.NewCard(def, p, engine.Hand)
			if err := castEntrant(t, g, p, card, engine.NewScriptedController()); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Card(card).Counters.Count(engine.P1P1); got != tc.want {
				t.Errorf("+1/+1 counters = %d, want %d", got, tc.want)
			}
		})
	}
}

// A convoked permanent enters with a counter for each creature that convoked
// it (Count$Convoked, Defined$ Convoked).
func TestConvokedCreaturesAreRememberedOnTheCard(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	def := scriptDef(t, "Convoking Elf", "Creature Elf",
		"K:Convoke", "K:etbCounter:P1P1:X:no Condition:enters with counters", "SVar:X:Convoked$Amount")
	def.Faces[0].ManaCost = creatureDefCost(t, "x", "2").Faces[0].ManaCost
	card := g.NewCard(def, p, engine.Hand)
	var picks []engine.CardID
	for range 2 {
		picks = append(picks, g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield))
	}
	c := engine.NewScriptedController()
	c.QueueCardChoice(picks)
	if err := castEntrant(t, g, p, card, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(card).Counters.Count(engine.P1P1); got != 2 {
		t.Errorf("+1/+1 counters = %d, want 2 (one per convoking creature)", got)
	}
}

// Count$DifferentCounterKinds_<valid> counts the kinds of counter the matching
// permanents carry between them (Perrie, the Pulverizer).
func TestDifferentCounterKindsAmount(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.Card(a).Counters.Add(engine.P1P1, 2)
	g.Card(b).Counters.Add(engine.P1P1, 1)
	g.Card(b).Counters.Add("SHIELD", 1)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(a)},
		"DB$ Pump | ValidTgts$ Creature | NumAtt$ +X | Duration$ Permanent",
		"X", "Count$DifferentCounterKinds_Permanent.YouCtrl"); err != nil {
		t.Fatal(err)
	}
	sba(g)
	// 1/1 with two +1/+1 counters is 3/3, and two kinds of counter add 2
	if got, _ := g.Card(a).Power(); got != 5 {
		t.Errorf("power = %d, want 5", got)
	}
}

// A Pump naming "You & Valid ..." pumps the player and the cards the valid
// string names: the player half names no card and the card half no player.
func TestPumpDefinedJoinsAPlayerAndValidCards(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
		"DB$ Pump | Defined$ You & Valid Creature.YouCtrl | KW$ Hexproof | Duration$ Permanent"); err != nil {
		t.Fatal(err)
	}
	sba(g)
	if !g.Card(mine).HasKeyword("Hexproof") {
		t.Error("the creature did not gain hexproof")
	}
	if !g.Player(p).HasKeyword("Hexproof") {
		t.Error("the player did not gain hexproof")
	}
}
