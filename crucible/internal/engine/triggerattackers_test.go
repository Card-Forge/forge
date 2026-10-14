package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// The attacker list a trigger records (AbilityKey.Attackers): Mode$
// AttackersDeclared and AttackersDeclaredOneTarget keep the attackers their
// ValidAttackers$ matched (TriggerAttackersDeclared.java:82-91), Mode$ Blocks
// the attacker the blocker blocks (TriggerBlocks.java:71). Defined$
// TriggeredAttackers and the TriggerObjectsAttackers$ amount head read it.

// attackAndResolve declares attackers for the active player p and resolves the
// triggers the declaration put on the stack.
func attackAndResolve(t *testing.T, g *engine.Game, attackers ...engine.CardID) {
	t.Helper()
	c := engine.NewScriptedController()
	c.QueueAttackers(attackers)
	declareAttackers(t, g, c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
}

// TestAttackersDeclaredRecordsTheAttackers proves Vrestin, Menoptra Leader's
// shape: Defined$ TriggeredAttackers names the creatures that attacked and no
// other, and TriggerObjectsAttackers$Amount (Varina, The Ur-Dragon) counts
// them.
func TestAttackersDeclaredRecordsTheAttackers(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a1 := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	a2 := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	home := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Attack Watcher",
		"Mode$ AttackersDeclared | AttackingPlayer$ You | TriggerZones$ Battlefield | Execute$ TrigPut",
		"TrigPut", "DB$ PutCounter | Defined$ TriggeredAttackers | CounterType$ P1P1 | CounterNum$ 1 | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ X",
		"X", "TriggerObjectsAttackers$Amount"), p, engine.Battlefield)

	attackAndResolve(t, g, a1, a2)
	if got, want := [3]int{p1p1(g, a1), p1p1(g, a2), p1p1(g, home)}, [3]int{1, 1, 0}; got != want {
		t.Errorf("+1/+1 counters (a1, a2, home) = %v, want %v -- only the attackers", got, want)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22 -- two attackers", got)
	}
}

// TestAttackersDeclaredRecordsOnlyTheValidAttackers proves the ValidAttackers$
// narrowing (TriggerAttackersDeclared.java:83-84; Grand Warlord Radha, Lulu):
// the recorded list is the matching attackers, so the amount and its
// GreatestCardPower measure (Aloy, Shriekwood Devourer) skip the rest.
func TestAttackersDeclaredRecordsOnlyTheValidAttackers(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		x    string
		want int
	}{
		{"TriggerObjectsAttackers$Amount", 1},
		{"TriggerObjectsAttackers$Amount/Twice", 2},
		{"TriggerObjectsAttackers$CardPower", 4},
		{"TriggerObjectsAttackers$GreatestCardPower", 4},
		{"TriggerObjectsAttackers$Valid Creature.powerGE2", 1},
	} {
		t.Run(tc.x, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			small := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			big := g.NewCard(creatureDefPT(t, "4", "4"), p, engine.Battlefield)
			g.NewCard(triggerWatcherDef(t, "Test Attack Watcher",
				"Mode$ AttackersDeclared | AttackingPlayer$ You | ValidAttackers$ Creature.powerGE3 | TriggerZones$ Battlefield | Execute$ TrigGain",
				"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ X",
				"X", tc.x), p, engine.Battlefield)

			attackAndResolve(t, g, small, big)
			if got := g.Player(p).Life - 20; got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
}

// TestAttackersDeclaredRecordedEmptyListReadsZero proves a recorded list
// with no member is 0, not unresolved: ValidAttackersAmount$ EQ0 fires with
// no attacker matching, and the amount's operator still applies.
func TestAttackersDeclaredRecordedEmptyListReadsZero(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Attack Watcher",
		"Mode$ AttackersDeclared | ValidAttackers$ Creature.powerGE5 | ValidAttackersAmount$ EQ0 | TriggerZones$ Battlefield | Execute$ TrigGain",
		"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ X",
		"X", "TriggerObjectsAttackers$Amount/Plus.3"), p, engine.Battlefield)

	attackAndResolve(t, g, a)
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23 -- 0 recorded attackers plus 3", got)
	}
}

// TestAttackersDeclaredOneTargetRecordsTheDefendersAttackers proves the
// per-defender mode (Yuriko, Frontier Warmonger): the attackers of the one
// defender the trigger fired for, through Defined$ TriggeredAttackers.
func TestAttackersDeclaredOneTargetRecordsTheDefendersAttackers(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Attack Watcher",
		"Mode$ AttackersDeclaredOneTarget | AttackedTarget$ Player | AttackingPlayer$ You | TriggerZones$ Battlefield | Execute$ TrigDamage",
		"TrigDamage", "DB$ DealDamage | Defined$ TriggeredAttackers | NumDmg$ 1 | SubAbility$ DBLose",
		"DBLose", "DB$ LoseLife | Defined$ Opponent | LifeAmount$ X",
		"X", "TriggerObjectsAttackers$Amount"), p, engine.Battlefield)

	attackAndResolve(t, g, a)
	if got := g.Card(a).Damage.Marked; got != 1 {
		t.Errorf("attacker damage = %d, want 1", got)
	}
	if got := g.Player(other).Life; got != 19 {
		t.Errorf("other life = %d, want 19 -- one attacker on that defender", got)
	}
}

// TestBlocksTriggerRecordsTheBlockedAttacker proves TriggerBlocks.java:71's
// AbilityKey.Attackers (Godsend's "one of those creatures"): the attacker the
// blocker blocks.
func TestBlocksTriggerRecordsTheBlockedAttacker(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	attacker := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	bystander := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Block Watcher",
		"Mode$ Blocks | ValidCard$ Creature.YouCtrl | Execute$ TrigDestroy",
		"TrigDestroy", "DB$ Destroy | Defined$ TriggeredAttackers"), other, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker, bystander})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attacker}})
	declareBlockers(t, g, bc)
	if err := g.ResolveStack(engine.NewRegistry(), bc); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := [2]engine.ZoneType{g.Card(attacker).Zone, g.Card(bystander).Zone}; got != [2]engine.ZoneType{engine.Graveyard, engine.Battlefield} {
		t.Errorf("zones (blocked, unblocked) = %v, want the blocked attacker destroyed", got)
	}
}

// TestUnrecordedAttackersStayUnresolved proves the GO-7 side: a Phase
// trigger records no attackers, so both readers fail loudly instead of
// reading Java's default empty list.
func TestUnrecordedAttackersStayUnresolved(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, exec, want string
	}{
		{"Defined", "DB$ PutCounter | Defined$ TriggeredAttackers | CounterType$ P1P1 | CounterNum$ 1",
			`"TriggeredAttackers": the trigger recorded no attackers`},
		{"amount", "DB$ GainLife | Defined$ You | LifeAmount$ X", "LifeAmount"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			g.NewCard(triggerWatcherDef(t, "Test Upkeep Watcher",
				"Mode$ Phase | Phase$ Upkeep | TriggerZones$ Battlefield | Execute$ TrigDo",
				"TrigDo", tc.exec, "X", "TriggerObjectsAttackers$Amount"), p, engine.Battlefield)
			g.SetTurnState(2, other, engine.Untap)

			c := engine.NewScriptedController()
			g.AdvancePhase(c)
			err := g.ResolveStack(engine.NewRegistry(), c)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Fatalf("ResolveStack error = %v, want one naming %s", err, tc.want)
			}
		})
	}
}
