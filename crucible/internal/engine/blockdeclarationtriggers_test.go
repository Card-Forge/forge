package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// The once-per-declare-blockers-step trigger modes: Mode$ BlockersDeclared
// ("whenever one or more creatures block", PhaseHandler.java:748-762) and Mode$
// AttackerBlockedOnce ("whenever one or more creatures you control become
// blocked", PhaseHandler.java:816-820). Each fires once for the whole
// declaration, however many blocks it holds.

// blockAndResolve has p attack with attackers, the defender declare blocks,
// and resolves what the declarations put on the stack with c.
func blockAndResolve(t *testing.T, g *engine.Game, c *engine.ScriptedController, attackers []engine.CardID, blocks []engine.Block) {
	t.Helper()
	c.QueueAttackers(attackers)
	declareAttackers(t, g, c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack after attackers: %v", err)
	}
	c.QueueBlocks(blocks)
	declareBlockers(t, g, c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack after blockers: %v", err)
	}
}

// TestBlockersDeclaredRecordsEveryBlockerAndBlockedAttacker proves the trigger
// fires once (each object gets one counter, not one per block) and records
// AbilityKey.Blockers and AbilityKey.Attackers: the two blockers and the two
// attackers they block, not the unblocked one.
func TestBlockersDeclaredRecordsEveryBlockerAndBlockedAttacker(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a1 := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	a2 := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	free := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	b1 := g.NewCard(creatureDefPT(t, "1", "4"), other, engine.Battlefield)
	b2 := g.NewCard(creatureDefPT(t, "1", "4"), other, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Block Watcher",
		"Mode$ BlockersDeclared | TriggerZones$ Battlefield | Execute$ TrigBlockers",
		"TrigBlockers", "DB$ PutCounter | Defined$ TriggeredBlockers | CounterType$ P1P1 | CounterNum$ 1 | SubAbility$ DBAttackers",
		"DBAttackers", "DB$ PutCounter | Defined$ TriggeredAttackers | CounterType$ P1P1 | CounterNum$ 1"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	blockAndResolve(t, g, c, []engine.CardID{a1, a2, free},
		[]engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})
	got := [5]int{p1p1(g, b1), p1p1(g, b2), p1p1(g, a1), p1p1(g, a2), p1p1(g, free)}
	if want := [5]int{1, 1, 1, 1, 0}; got != want {
		t.Errorf("+1/+1 counters (b1, b2, a1, a2, unblocked) = %v, want %v", got, want)
	}
}

// TestBlockersDeclaredNeedsABlock proves the `!declaredBlockers.isEmpty()`
// guard: a declaration with no block fires nothing.
func TestBlockersDeclaredNeedsABlock(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Block Watcher",
		"Mode$ BlockersDeclared | TriggerZones$ Battlefield | Execute$ TrigGain",
		"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 5"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{a})
	declareAttackers(t, g, c)
	c.QueueBlocks(nil)
	declareBlockers(t, g, c)
	if got := g.StackLen(); got != 0 {
		t.Errorf("StackLen() = %d, want 0 -- no creature blocked", got)
	}
}

// TestTideOfWarSacrificesOneSideOfTheBlocks proves Tide of War's real script:
// the coin flip sacrifices the blockers on a win (Defined$ TriggeredBlockers)
// and the blocked attackers on a loss (Defined$ TriggeredAttackers), never the
// unblocked attacker. The game's seed fixes the flip, so the two calls land on
// opposite sides.
func TestTideOfWarSacrificesOneSideOfTheBlocks(t *testing.T) {
	t.Parallel()

	outcome := map[bool]string{}
	for _, heads := range []bool{true, false} {
		g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
		blocked := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
		free := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
		blocker := g.NewCard(creatureDefPT(t, "1", "4"), other, engine.Battlefield)
		g.NewCard(corpusCard(t, "Tide of War"), other, engine.Battlefield)

		c := engine.NewScriptedController()
		c.QueueCoinCall(heads)
		blockAndResolve(t, g, c, []engine.CardID{blocked, free}, []engine.Block{{Blocker: blocker, Attacker: blocked}})
		if got := g.Card(free).Zone; got != engine.Battlefield {
			t.Fatalf("call %v: unblocked attacker zone = %v, want Battlefield", heads, got)
		}
		switch [2]engine.ZoneType{g.Card(blocker).Zone, g.Card(blocked).Zone} {
		case [2]engine.ZoneType{engine.Graveyard, engine.Battlefield}:
			outcome[heads] = "blockers"
		case [2]engine.ZoneType{engine.Battlefield, engine.Graveyard}:
			outcome[heads] = "attackers"
		default:
			t.Fatalf("call %v: zones (blocker, blocked) = %v, %v, want exactly one side sacrificed",
				heads, g.Card(blocker).Zone, g.Card(blocked).Zone)
		}
	}
	if outcome[true] == outcome[false] {
		t.Errorf("both calls sacrificed the %s -- a won and a lost flip must pick different sides", outcome[true])
	}
}

// TestAttackerBlockedOnceFiresOncePerDeclaration proves Big Spender's real
// script: two of its controller's attackers become blocked, one Treasure.
func TestAttackerBlockedOnceFiresOncePerDeclaration(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Big Spender"), p, engine.Battlefield)
	a1 := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	a2 := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	b1 := g.NewCard(creatureDefPT(t, "1", "4"), other, engine.Battlefield)
	b2 := g.NewCard(creatureDefPT(t, "1", "4"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	blockAndResolve(t, g, c, []engine.CardID{a1, a2},
		[]engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})
	if got := len(namedIn(g, engine.Battlefield, p, "Treasure Token")); got != 1 {
		t.Errorf("Treasures = %d, want 1 -- one trigger for the whole declaration", got)
	}
}

// TestAttackerBlockedOnceValidCardMatchesTheBlockedAttackers proves ValidCard$
// is matched against the blocked attackers: Big Spender on the defending side
// sees none of its controller's creatures blocked, and an unblocked attack
// fires nothing at all.
func TestAttackerBlockedOnceValidCardMatchesTheBlockedAttackers(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name      string
		defending bool
		block     bool
	}{
		{"spender defends", true, true},
		{"nothing blocked", false, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			owner := p
			if tc.defending {
				owner = other
			}
			g.NewCard(corpusCard(t, "Big Spender"), owner, engine.Battlefield)
			a := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
			b := g.NewCard(creatureDefPT(t, "1", "4"), other, engine.Battlefield)
			var blocks []engine.Block
			if tc.block {
				blocks = []engine.Block{{Blocker: b, Attacker: a}}
			}

			c := engine.NewScriptedController()
			blockAndResolve(t, g, c, []engine.CardID{a}, blocks)
			if got := len(namedIn(g, engine.Battlefield, owner, "Treasure Token")); got != 0 {
				t.Errorf("Treasures = %d, want 0", got)
			}
		})
	}
}

// TestAttackerBlockedOnceRecordsTheBlockedAttackers proves AbilityKey.Attackers
// is every blocked attacker and no unblocked one.
func TestAttackerBlockedOnceRecordsTheBlockedAttackers(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a1 := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	free := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	a2 := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	b1 := g.NewCard(creatureDefPT(t, "1", "4"), other, engine.Battlefield)
	b2 := g.NewCard(creatureDefPT(t, "1", "4"), other, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Blocked Watcher",
		"Mode$ AttackerBlockedOnce | TriggerZones$ Battlefield | Execute$ TrigPut",
		"TrigPut", "DB$ PutCounter | Defined$ TriggeredAttackers | CounterType$ P1P1 | CounterNum$ 1"), p, engine.Battlefield)

	c := engine.NewScriptedController()
	blockAndResolve(t, g, c, []engine.CardID{a1, free, a2},
		[]engine.Block{{Blocker: b1, Attacker: a2}, {Blocker: b2, Attacker: a1}})
	if got, want := [3]int{p1p1(g, a1), p1p1(g, a2), p1p1(g, free)}, [3]int{1, 1, 0}; got != want {
		t.Errorf("+1/+1 counters (a1, a2, unblocked) = %v, want %v", got, want)
	}
}

// TestUnrecordedBlockersStayUnresolved proves Defined$ TriggeredBlockers
// fails loudly under a trigger mode that recorded no blockers (GO-7).
func TestUnrecordedBlockersStayUnresolved(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Upkeep Watcher",
		"Mode$ Phase | Phase$ Upkeep | TriggerZones$ Battlefield | Execute$ TrigSac",
		"TrigSac", "DB$ SacrificeAll | Defined$ TriggeredBlockers"), p, engine.Battlefield)
	g.SetTurnState(2, other, engine.Untap)

	c := engine.NewScriptedController()
	g.AdvancePhase(c)
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "recorded no blockers") {
		t.Fatalf("ResolveStack error = %v, want one naming the unrecorded blockers", err)
	}
}
