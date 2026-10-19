package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// The resolve-time card choice (Choices$, CardChoices$): the Chooser$ player
// picks from the cards the valid string names as the effect resolves, in place
// of Defined$ and the targets.

func TestPutCounterChoicesPutsCountersOnTheChosenCardOnly(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	first := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	second := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{second})
	_, err := resolveNow(t, g, p, c, nil, "DB$ PutCounter | Choices$ Creature.YouCtrl | CounterType$ P1P1 | CounterNum$ 2 | RememberCards$ True")
	if err != nil {
		t.Fatal(err)
	}
	for id, want := range map[engine.CardID]int{first: 0, second: 2, theirs: 0} {
		if got := g.Card(id).Counters.Count(engine.P1P1); got != want {
			t.Errorf("card %d has %d counters, want %d", id, got, want)
		}
	}

	// A card the valid string does not name is a bad answer, not a pick.
	g, p, other = newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	theirs = g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	c = engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{theirs})
	if _, err := resolveNow(t, g, p, c, nil, "DB$ PutCounter | Choices$ Creature.YouCtrl | CounterType$ P1P1"); err == nil ||
		!strings.Contains(err.Error(), "not offered") {
		t.Errorf("err = %v, want a not-offered error", err)
	}
}

func TestPutCounterChoicesDividedAsYouChoose(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{a, b})
	c.QueueNumberChoice(2)
	_, err := resolveNow(t, g, p, c, nil,
		"DB$ PutCounter | Choices$ Creature.YouCtrl | ChoiceAmount$ 2 | MinChoiceAmount$ 1 | DividedAsYouChoose$ 3 | CounterType$ P1P1 | CounterNum$ 3")
	if err != nil {
		t.Fatal(err)
	}
	// The first card gets the 2 asked for, the last takes what is left.
	if got := g.Card(a).Counters.Count(engine.P1P1); got != 2 {
		t.Errorf("first = %d, want 2", got)
	}
	if got := g.Card(b).Counters.Count(engine.P1P1); got != 1 {
		t.Errorf("second = %d, want 1", got)
	}
}

func TestPutCounterOptionalDeclined(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	if _, err := resolveNow(t, g, p, c, nil, "DB$ PutCounter | Defined$ Targeted | Choices$ Creature.YouCtrl | Optional$ True | CounterType$ P1P1"); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(a).Counters.Count(engine.P1P1); got != 0 {
		t.Errorf("counters = %d, want 0", got)
	}
}

func TestTapCardChoicesAnyNumber(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueEntityChoice([]engine.EntityID{engine.CardEntity(a)})
	host, err := resolveNow(t, g, p, c, nil,
		"DB$ Tap | CardChoices$ Creature.YouCtrl+untapped | AnyNumber$ True | ChoiceAmount$ 2 | RememberTapped$ True")
	if err != nil {
		t.Fatal(err)
	}
	if !g.Card(a).Tapped || g.Card(b).Tapped {
		t.Errorf("tapped a=%v b=%v, want only a", g.Card(a).Tapped, g.Card(b).Tapped)
	}
	if rem := g.Card(host).Memory.Remembered(); len(rem) != 1 || rem[0] != engine.CardEntity(a) {
		t.Error("the tapped creature was not remembered")
	}
}

func TestRemoveCounterChoicesPicksOneCard(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.Card(a).Counters.Add(engine.P1P1, 2)
	g.Card(b).Counters.Add(engine.P1P1, 2)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{b})
	if _, err := resolveNow(t, g, p, c, nil, "DB$ RemoveCounter | Choices$ Creature.YouCtrl+counters_GE1_P1P1 | CounterType$ P1P1 | CounterNum$ 1"); err != nil {
		t.Fatal(err)
	}
	if ga, gb := g.Card(a).Counters.Count(engine.P1P1), g.Card(b).Counters.Count(engine.P1P1); ga != 2 || gb != 1 {
		t.Errorf("counters a=%d b=%d, want 2 and 1", ga, gb)
	}
}

func TestGainControlChoicesTakesTheChosenPermanent(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	want := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{want})
	if _, err := resolveNow(t, g, p, c, nil, "DB$ GainControl | Choices$ Creature.OppCtrl"); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(want).Controller(); got != p {
		t.Errorf("controller = %v, want %v", got, p)
	}
}

// RememberRemoved$ remembers one (kind, index) pair per counter taken off a
// card, RememberAmount$ the total (CountersRemoveEffect.java:187-192, :217-219);
// the pairs of two cards collapse onto the indices of the larger, as Java's set
// does.
func TestRemoveCounterRemembersWhatItRemoved(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.Card(a).Counters.Add(engine.P1P1, 3)
	g.Card(b).Counters.Add(engine.P1P1, 1)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{a, b})
	host, err := resolveNow(t, g, p, c, nil,
		"DB$ RemoveCounter | Choices$ Creature.YouCtrl | ChoiceNum$ 2 | CounterType$ P1P1 | CounterNum$ All | RememberRemoved$ True | RememberAmount$ True")
	if err != nil {
		t.Fatal(err)
	}
	m := g.Card(host).Memory
	if got := m.RememberedCount(); got != 4 {
		t.Errorf("RememberedCount = %d, want 4 (3 pairs and the total)", got)
	}
	if nums := m.RememberedNumbers(); len(nums) != 1 || nums[0] != 4 {
		t.Errorf("RememberedNumbers = %v, want [4]", nums)
	}
}

func TestRemoveCounterOptionalDeclined(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.Card(a).Counters.Add(engine.P1P1, 2)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	if _, err := resolveNow(t, g, p, c, nil, "DB$ RemoveCounter | Choices$ Creature.YouCtrl | Optional$ True | CounterType$ P1P1"); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(a).Counters.Count(engine.P1P1); got != 2 {
		t.Errorf("counters = %d, want 2", got)
	}
}
