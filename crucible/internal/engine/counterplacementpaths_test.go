package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Every counter placement goes through the counter table Java fills
// (GameEntityCounterTable.replaceCounterEffect): AddCounter replacements
// (Hardened Scales) see it, and Card.addCounterInternal fires CounterAdded
// (Card.java:1761-1832). These are the placements that used to skip one or
// the other.

// counterWatcher is an enchantment gaining its controller 1 life per counter
// of kind ct put on a creature (Mode$ CounterAdded fires once per counter).
func counterWatcher(t *testing.T, ct string) []string {
	t.Helper()
	return []string{
		"T:Mode$ CounterAdded | ValidCard$ Creature | CounterType$ " + ct + " | TriggerZones$ Battlefield | Execute$ TrigGain",
		"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 1",
	}
}

// CR 120.3d: wither damage is dealt as -1/-1 counters, placed by the source's
// controller (Card.java:6354): they fire CounterAdded.
func TestWitherDamageCountersFireCounterAdded(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(scriptDef(t, "M1M1 Watcher", "Enchantment", counterWatcher(t, "M1M1")...), p, engine.Battlefield)
	src := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Wither"), p, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "4", "4"), other, engine.Battlefield)
	def := etbChainDef(t, "Wither Bolt", "DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 2")
	sub := def.Faces[0].Triggers[0].Subs[0].Ability
	g.PushAbility(engine.Ability{API: engine.APIDealDamage, Source: src, Controller: p, Params: sub,
		Targets: []engine.EntityID{engine.CardEntity(victim)}})
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(victim).Counters.Count(engine.M1M1); got != 2 {
		t.Fatalf("victim has %d -1/-1 counters, want 2", got)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22 (one CounterAdded per counter)", got)
	}
}

// ChangeZone's WithCountersType$ counters join the entry's counter table:
// Hardened Scales adds one, and CounterAdded fires for each.
func TestChangeZoneEntryCountersAreReplacedAndFireCounterAdded(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Hardened Scales"), p, engine.Battlefield)
	g.NewCard(scriptDef(t, "P1P1 Watcher", "Enchantment", counterWatcher(t, "P1P1")...), p, engine.Battlefield)
	dead := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Graveyard)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(dead)})
	resolveLine(t, g, p, c, "DB$ ChangeZone | ValidTgts$ Creature.YouOwn | Origin$ Graveyard | Destination$ Battlefield | WithCountersType$ P1P1 | WithCountersAmount$ 2")
	if got := g.Card(dead).Counters.Count(engine.P1P1); got != 3 {
		t.Fatalf("Grizzly Bears has %d +1/+1 counters, want 3 (2 + Hardened Scales)", got)
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23", got)
	}
}

// MakeCard's WithCounter$ off the battlefield is placed by the making player
// through addCardCounters (MakeCardEffect.java:203-206).
func TestMakeCardCountersOffTheBattlefieldArePlaced(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	host := resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ MakeCard | Name$ Grizzly Bears | Zone$ Exile | WithCounter$ P1P1 | WithCounterNum$ 2 | RememberMade$ True")
	made := g.Card(host).Memory.Remembered()
	if len(made) != 1 {
		t.Fatalf("made %d cards, want 1", len(made))
	}
	id, _ := made[0].AsCard()
	if got := g.Card(id).Counters.Count(engine.P1P1); got != 2 {
		t.Errorf("made card has %d +1/+1 counters, want 2", got)
	}
}

// PutCounterAll runs the AddCounter replacements per card
// (CountersPutAllEffect.java:118).
func TestPutCounterAllIsReplacedByHardenedScales(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Hardened Scales"), p, engine.Battlefield)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ PutCounterAll | ValidCards$ Creature | CounterType$ P1P1 | CounterNum$ 1")
	if got := g.Card(mine).Counters.Count(engine.P1P1); got != 2 {
		t.Errorf("own creature has %d counters, want 2", got)
	}
	if got := g.Card(theirs).Counters.Count(engine.P1P1); got != 1 {
		t.Errorf("opposing creature has %d counters, want 1 (not a creature Hardened Scales' controller controls)", got)
	}
}

// MultiplyCounter runs them too (CountersMultiplyEffect.java:69), for a named
// kind and for every kind.
func TestMultiplyCounterIsReplacedByHardenedScales(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name, line       string
		wantP1P1, wantCh int
	}{
		{"one kind", "DB$ MultiplyCounter | ValidTgts$ Creature | CounterType$ P1P1", 5, 1},
		{"every kind", "DB$ MultiplyCounter | ValidTgts$ Creature", 5, 2},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			g.NewCard(corpusCard(t, "Hardened Scales"), p, engine.Battlefield)
			target := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
			g.Card(target).Counters.Add(engine.P1P1, 2)
			g.Card(target).Counters.Add(engine.CounterType("CHARGE"), 1)
			c := engine.NewScriptedController()
			c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
			resolveLine(t, g, p, c, tc.line)
			if got := g.Card(target).Counters.Count(engine.P1P1); got != tc.wantP1P1 {
				t.Errorf("+1/+1 counters = %d, want %d", got, tc.wantP1P1)
			}
			if got := g.Card(target).Counters.Count(engine.CounterType("CHARGE")); got != tc.wantCh {
				t.Errorf("charge counters = %d, want %d", got, tc.wantCh)
			}
		})
	}
}
