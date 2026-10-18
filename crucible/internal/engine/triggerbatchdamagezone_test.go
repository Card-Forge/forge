package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// orderProbe is the pair of watcher effects these tests read the resolution
// order from: the non-active player's trigger sets the active player's life to
// 10, the active player's gains them 1. The non-active player's trigger on top
// (one APNAP pass, MagicStack.java:855-880) resolves first: 10, then 11. A
// pass per mode would put the active player's later-mode trigger on top: 21,
// then 10.
const (
	setOpponentTo10 = "DB$ SetLife | Defined$ Opponent | LifeAmount$ 10"
	gainOne         = "DB$ GainLife | Defined$ You | LifeAmount$ 1"
)

// A combat damage step is one event: the non-active player's DamageDone
// trigger (checked per exchange) and the active player's DamageDoneOnce
// trigger (checked off the step's table afterwards) reach the stack in one
// APNAP pass (dealCombatDamageStep's batch).
func TestCombatDamageTriggersShareOneAPNAPPass(t *testing.T) {
	t.Parallel()

	g, p, other, _ := attackingCombat(t)
	g.NewCard(watcherDef(t, "Damage Watcher", "Mode$ DamageDone | ValidSource$ Creature | ValidTarget$ Player | TriggerZones$ Battlefield",
		setOpponentTo10), p, engine.Battlefield)
	g.NewCard(watcherDef(t, "Once Watcher", "Mode$ DamageDoneOnce | ValidTarget$ Player | TriggerZones$ Battlefield",
		gainOne), other, engine.Battlefield)
	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)

	g.DealCombatDamage(engine.NewScriptedController())
	if got := g.StackLen(); got != 2 {
		t.Fatalf("stack = %d, want both watchers' triggers", got)
	}
	if top, _ := g.StackTop(); top.Controller != p {
		t.Errorf("top of the stack is %v's trigger, want the non-active player's (%v)", top.Controller, p)
	}
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 11 {
		t.Errorf("active player's life = %d, want 11 (set to 10, then gain 1)", got)
	}
}

// An effect's zone change is one event: the non-active player's dies
// trigger (checked per card) and the active player's ChangesZoneAll trigger
// (checked once the cards have moved) share one APNAP pass
// (resolveEffectBatched).
func TestEffectZoneChangeTriggersShareOneAPNAPPass(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, other, engine.Main1)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.NewCard(watcherDef(t, "Dies Watcher", "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Creature.Other | TriggerZones$ Battlefield",
		setOpponentTo10), p, engine.Battlefield)
	g.NewCard(watcherDef(t, "All Watcher", "Mode$ ChangesZoneAll | Origin$ Battlefield | Destination$ Graveyard | ValidCards$ Creature | TriggerZones$ Battlefield",
		gainOne), other, engine.Battlefield)

	if _, err := resolveNow(t, g, other, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)},
		"DB$ Destroy | ValidTgts$ Creature"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(victim).Zone; z != engine.Graveyard {
		t.Fatalf("victim zone = %v, want Graveyard", z)
	}
	if got := g.Player(other).Life; got != 11 {
		t.Errorf("active player's life = %d, want 11 (set to 10, then gain 1)", got)
	}
}

// State-based deaths are one event too (destroyDamagedCreatures' batch): the
// non-active player's dies trigger ends on top of the active player's
// ChangesZoneAll trigger.
func TestStateBasedDeathTriggersShareOneAPNAPPass(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, other, engine.Main1)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.NewCard(watcherDef(t, "Dies Watcher", "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Creature.Other | TriggerZones$ Battlefield",
		setOpponentTo10), p, engine.Battlefield)
	g.NewCard(watcherDef(t, "All Watcher", "Mode$ ChangesZoneAll | Origin$ Battlefield | Destination$ Graveyard | ValidCards$ Creature | TriggerZones$ Battlefield",
		gainOne), other, engine.Battlefield)
	g.Card(victim).Damage.Marked = 2

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if got := g.StackLen(); got != 2 {
		t.Fatalf("stack = %d, want both watchers' triggers", got)
	}
	if top, _ := g.StackTop(); top.Controller != p {
		t.Errorf("top of the stack is %v's trigger, want the non-active player's (%v)", top.Controller, p)
	}
}

// A zero-toughness death (destroyLethalToughness) batches the same way.
func TestZeroToughnessDeathTriggersShareOneAPNAPPass(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, other, engine.Main1)
	g.NewCard(creatureDefPT(t, "2", "0"), p, engine.Battlefield)
	g.NewCard(watcherDef(t, "Dies Watcher", "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Creature.Other | TriggerZones$ Battlefield",
		setOpponentTo10), p, engine.Battlefield)
	g.NewCard(watcherDef(t, "All Watcher", "Mode$ ChangesZoneAll | Origin$ Battlefield | Destination$ Graveyard | ValidCards$ Creature | TriggerZones$ Battlefield",
		gainOne), other, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if got := g.StackLen(); got != 2 {
		t.Fatalf("stack = %d, want both watchers' triggers", got)
	}
	if top, _ := g.StackTop(); top.Controller != p {
		t.Errorf("top of the stack is %v's trigger, want the non-active player's (%v)", top.Controller, p)
	}
}
