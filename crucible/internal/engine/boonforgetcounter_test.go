package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Boon$ (EffectEffect.java:272-274, TriggerHandler.java:541-546): the effect
// card leaves the Command zone with its first trigger, or after BoonAmount$
// firings of the trigger's ability.
func TestBoonEffectLeavesAfterItsTriggersFired(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		amount string
		draws  int
		life   int
		gone   bool
	}{{"", 1, 22, true}, {"BoonAmount$ 2 |", 1, 22, false}, {"BoonAmount$ 2 |", 2, 24, true}} {
		g, p, _ := newTwoPlayerGame(t)
		libraryCards(t, g, p, 5)
		c := engine.NewScriptedController()
		resolveLine(t, g, p, c, "DB$ Effect | Boon$ True | Duration$ Permanent | Triggers$ TrigGain",
			"TrigGain", "Mode$ Drawn | ValidCard$ Card.YouCtrl | "+tc.amount+" TriggerZones$ Command | Execute$ DBGain",
			"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 2")
		if n := len(commandEffects(g, p)); n != 1 {
			t.Fatalf("%q: %d effects after resolving, want 1", tc.amount, n)
		}
		for range tc.draws {
			g.DrawCards(p, 1, c)
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatal(err)
			}
		}
		if got := g.Player(p).Life; got != tc.life {
			t.Errorf("%q after %d draws: life = %d, want %d", tc.amount, tc.draws, got, tc.life)
		}
		if gone := len(commandEffects(g, p)) == 0; gone != tc.gone {
			t.Errorf("%q after %d draws: effect gone = %v, want %v", tc.amount, tc.draws, gone, tc.gone)
		}
	}
}

// ForgetCounter$ (:86-98, :597-612): the effect remembers only the cards that
// carry the counter, forgets one when its last counter goes, and leaves once it
// remembers nothing; ImprintOnHost$ has the host imprint the effect card.
func TestEffectForgetCounterAndImprintOnHost(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	with := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	without := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.Card(with).Counters.Add(engine.P1P1, 2)
	c := engine.NewScriptedController()
	src := resolveLine(t, g, p, c, "DB$ Effect | Duration$ Permanent | RememberObjects$ Valid Creature | ForgetCounter$ P1P1 | ImprintOnHost$ True")
	effects := commandEffects(g, p)
	if len(effects) != 1 {
		t.Fatalf("%d effects, want 1", len(effects))
	}
	if n := len(g.Card(src).Memory.Imprinted()); n != 1 {
		t.Errorf("host imprinted %d cards, want the effect", n)
	}
	remembered := g.Card(effects[0]).Memory.Remembered()
	if len(remembered) != 1 || remembered[0] != engine.CardEntity(with) {
		t.Fatalf("effect remembers %v, want only the card with counters %v (not %v)", remembered, with, without)
	}
	resolveLine(t, g, p, c, "DB$ RemoveCounter | Defined$ Valid Creature.counters_GE1_P1P1 | CounterType$ P1P1 | CounterNum$ All")
	if n := len(commandEffects(g, p)); n != 0 {
		t.Errorf("%d effects after the last counter went, want 0", n)
	}

	// No remembered card has the counter: no effect at all.
	g, p, _ = newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Effect | Duration$ Permanent | RememberObjects$ Valid Creature | ForgetCounter$ P1P1")
	if n := len(commandEffects(g, p)); n != 0 {
		t.Errorf("%d effects with nothing to watch, want 0", n)
	}
}

// Effect's Abilities$ (EffectEffect.java:104-111): the effect card carries the
// activated abilities, usable from the Command zone (an emblem's).
func TestEffectAbilitiesAreActivatableFromTheCommandZone(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ Effect | Duration$ Permanent | Abilities$ ABGain",
		"ABGain", "AB$ GainLife | Cost$ 0 | Defined$ You | LifeAmount$ 3 | ActivationZone$ Command")
	effects := commandEffects(g, p)
	if len(effects) != 1 {
		t.Fatalf("%d effects, want 1", len(effects))
	}
	if !activateAndResolve(t, g, p, effects[0], c) {
		t.Fatal("the emblem's ability was not activated")
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23", got)
	}
}
