package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// "Enters with your choice of a deathtouch or lifelink counter", "with your
// choice of two different counters" and "with up to two counters": the kind and
// amount are chosen as the card enters (CountersPutEffect.java:292-314,
// :620-629), and Defined$ defaults to the entering card.
func TestEntersWithAChoiceOfCounters(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name   string
		put    string
		option []int
		number int
		want   map[engine.CounterType]int
	}{
		{"one of a list", "DB$ PutCounter | ETB$ True | CounterType$ Deathtouch,Lifelink", []int{1}, 0,
			map[engine.CounterType]int{"LIFELINK": 1}},
		{"two different", "DB$ PutCounter | ETB$ True | CounterType$ Menace,Deathtouch,Lifelink | ChooseDifferent$ 2", []int{2, 0}, 0,
			map[engine.CounterType]int{"LIFELINK": 1, "MENACE": 1}},
		{"up to two", "DB$ PutCounter | ETB$ True | Defined$ Self | CounterType$ P1P1 | CounterNum$ 2 | UpTo$ True", nil, 1,
			map[engine.CounterType]int{"P1P1": 1}},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			def := scriptDef(t, "Counter Keeper", "Creature Elf",
				"K:ETBReplacement:Other:Counters", "SVar:Counters:"+tc.put)
			card := g.NewCard(def, p, engine.Hand)
			c := engine.NewScriptedController()
			for _, o := range tc.option {
				c.QueueOption(o)
			}
			if tc.number > 0 {
				c.QueueNumberChoice(tc.number)
			}
			if err := castEntrant(t, g, p, card, c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			for ct, n := range tc.want {
				if got := g.Card(card).Counters.Count(ct); got != n {
					t.Errorf("%s counters = %d, want %d", ct, got, n)
				}
			}
			if got := len(g.Card(card).Counters.Kinds()); got != len(tc.want) {
				t.Errorf("kinds = %v, want %d", g.Card(card).Counters.Kinds(), len(tc.want))
			}
		})
	}
}

// Outside an entry, a PutCounter naming a CounterType$ list has its placer pick
// once, and ChooseDifferent$ picks several kinds for each object.
func TestPutCounterChoosesFromAKindList(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name   string
		line   string
		option []int
		want   map[engine.CounterType]int
	}{
		{"one pick", "DB$ PutCounter | ValidTgts$ Creature | CounterType$ Flying,Lifelink,P1P1 | CounterNum$ 2", []int{2},
			map[engine.CounterType]int{"P1P1": 2}},
		{"different picks", "DB$ PutCounter | ValidTgts$ Creature | CounterType$ Vigilance,Reach,Trample | ChooseDifferent$ 2", []int{1, 1},
			map[engine.CounterType]int{"REACH": 1, "TRAMPLE": 1}},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGame(t)
			victim := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			c := engine.NewScriptedController()
			for _, o := range tc.option {
				c.QueueOption(o)
			}
			if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(victim)}, tc.line); err != nil {
				t.Fatal(err)
			}
			for ct, n := range tc.want {
				if got := g.Card(victim).Counters.Count(ct); got != n {
					t.Errorf("%s counters = %d, want %d", ct, got, n)
				}
			}
			if got := len(g.Card(victim).Counters.Kinds()); got != len(tc.want) {
				t.Errorf("kinds = %v, want %d", g.Card(victim).Counters.Kinds(), len(tc.want))
			}
		})
	}
}
