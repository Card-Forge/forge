package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// activateAndResolve activates ability 0 of card for p and resolves the stack.
func activateAndResolve(t *testing.T, g *engine.Game, p engine.PlayerID, card engine.CardID, c *engine.ScriptedController) bool {
	t.Helper()
	if !g.ActivateAbility(p, card, 0, c) {
		return false
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	return true
}

// TestMonstrosityPutsCountersOnceAndFiresBecomeMonstrous proves Monstrosity$ N
// (CountersPutEffect.java:489, :543-548): N +1/+1 counters, the creature
// becomes monstrous and Mode$ BecomeMonstrous fires; a second activation does
// nothing.
func TestMonstrosityPutsCountersOnceAndFiresBecomeMonstrous(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := scriptDef(t, "Test Monster", "Creature Elf",
		"A:AB$ PutCounter | Cost$ 0 | Defined$ Self | Monstrosity$ 3",
		"T:Mode$ BecomeMonstrous | ValidCard$ Card.Self | TriggerZones$ Battlefield | Execute$ TrigGain",
		"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 5")
	monster := g.NewCard(def, p, engine.Battlefield)
	c := engine.NewScriptedController()
	if !activateAndResolve(t, g, p, monster, c) {
		t.Fatal("first activation was declined")
	}
	if !activateAndResolve(t, g, p, monster, c) {
		t.Fatal("second activation was declined")
	}
	if got := p1p1(g, monster); got != 3 {
		t.Errorf("counters = %d, want 3: the second Monstrosity does nothing", got)
	}
	if got := g.Player(p).Life; got != 25 {
		t.Errorf("life = %d, want 25: BecomeMonstrous fires once", got)
	}
}

// TestAdaptSkipsACreatureWithCounters proves Adapt$ N (CountersPutEffect.java:
// 485-487): N counters only while the creature has none.
func TestAdaptSkipsACreatureWithCounters(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := scriptDef(t, "Test Adapter", "Creature Elf", "A:AB$ PutCounter | Cost$ 0 | Defined$ Self | Adapt$ 2")
	adapter := g.NewCard(def, p, engine.Battlefield)
	c := engine.NewScriptedController()
	activateAndResolve(t, g, p, adapter, c)
	activateAndResolve(t, g, p, adapter, c)
	if got := p1p1(g, adapter); got != 2 {
		t.Errorf("counters = %d, want 2: the second Adapt finds counters", got)
	}
}

// TestSupportPutsACounterOnUpToNOtherCreatures proves Support$ N
// (CountersPutEffect.java:779-800): up to N target creatures other than a
// permanent host, one counter each.
func TestSupportPutsACounterOnUpToNOtherCreatures(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := scriptDef(t, "Test Supporter", "Creature Elf", "A:AB$ PutCounter | Cost$ 0 | Support$ 2")
	host := g.NewCard(def, p, engine.Battlefield)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.CardEntity(b)})
	activateAndResolve(t, g, p, host, c)
	if p1p1(g, a) != 1 || p1p1(g, b) != 1 || p1p1(g, host) != 0 {
		t.Errorf("counters = %d/%d on the targets and %d on the host, want 1/1 and 0", p1p1(g, a), p1p1(g, b), p1p1(g, host))
	}
}

// TestExhaustLimitsAnAbilityToOneActivationPerGame proves Exhaust$ True
// (SpellAbilityRestriction.java:465-471).
func TestExhaustLimitsAnAbilityToOneActivationPerGame(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := creatureDefWithAbility(t, "Test Exhaust", "AB$ GainLife | Cost$ 0 | Defined$ You | LifeAmount$ 1 | Exhaust$ True")
	creature := g.NewCard(def, p, engine.Battlefield)
	if got := activateN(g, p, creature, 3); got != 1 {
		t.Errorf("activations = %d, want 1", got)
	}
	for g.ActivePhase() != engine.Cleanup {
		g.AdvancePhase(engine.NewScriptedController())
	}
	if got := activateN(g, p, creature, 3); got != 0 {
		t.Errorf("activations after cleanup = %d, want 0", got)
	}
}

// TestBolsterOffersOnlyTheControllersLeastToughnessCreatures proves Bolster$ N
// (CountersPutEffect.java:750-756): a Choices$ over
// Creature.leastToughnessControlledByYou, so a tie is the controller's pick, a
// sturdier creature or an opponent's smaller one is not offered
// (CardProperty.java:1247-1262).
func TestBolsterOffersOnlyTheControllersLeastToughnessCreatures(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	// resolveNow's host is a 1/1 of p's: it ties with tied2.
	tied2 := g.NewCard(creatureDefPT(t, "3", "1"), p, engine.Battlefield)
	sturdy := g.NewCard(creatureDefPT(t, "2", "4"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "1", "0"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{tied2})
	host, err := resolveNow(t, g, p, c, nil, "DB$ PutCounter | Bolster$ 2")
	if err != nil {
		t.Fatal(err)
	}
	for id, want := range map[engine.CardID]int{host: 0, tied2: 2, sturdy: 0, theirs: 0} {
		if got := p1p1(g, id); got != want {
			t.Errorf("card %d has %d counters, want %d", id, got, want)
		}
	}

	// The sturdy creature is not a legal pick.
	g, p, _ = newTwoPlayerGame(t)
	sturdy = g.NewCard(creatureDefPT(t, "2", "4"), p, engine.Battlefield)
	c = engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{sturdy})
	if _, err := resolveNow(t, g, p, c, nil, "DB$ PutCounter | Bolster$ 1"); err == nil {
		t.Error("choosing a creature with more toughness than the least was accepted")
	}
}
