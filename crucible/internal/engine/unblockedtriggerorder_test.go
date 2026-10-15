package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// PhaseHandler.java:746 runs Combat.fireTriggersForUnblockedAttackers before
// BlockersDeclared, Blocks and AttackerBlocked (:748-820): the unblocked
// attacker's trigger is put on the stack first, so it resolves after the
// blocked attacker's.
func TestUnblockedTriggersGoOnTheStackBeforeBlockedOnes(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	blockedOne := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	free := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	unblockedWatcher := g.NewCard(triggerWatcherDef(t, "Unblocked Watcher",
		"Mode$ AttackerUnblocked | ValidCard$ Creature | Execute$ TrigA",
		"TrigA", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	blockedWatcher := g.NewCard(triggerWatcherDef(t, "Blocked Watcher",
		"Mode$ AttackerBlocked | ValidCard$ Creature | Execute$ TrigB",
		"TrigB", "DB$ GainLife | Defined$ You | LifeAmount$ 10"), p, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{blockedOne, free})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: blockedOne}})
	declareBlockers(t, g, bc)

	if n := g.StackLen(); n != 2 {
		t.Fatalf("stack holds %d items, want 2", n)
	}
	top, _ := g.StackTop()
	if top.Source != blockedWatcher {
		t.Errorf("top of the stack is %v's trigger, want the blocked watcher's (%v); the unblocked one (%v) goes under it",
			top.Source, blockedWatcher, unblockedWatcher)
	}
}
