package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// "Can block an additional creature" (CombatUtil.canBlockMoreCreatures,
// CombatUtil.java:495-501): one blocker may take several attackers once a
// Mode$ Continuous CanBlockAmount$/CanBlockAny$ static or a Pump grants it.

func twoAttackerBlock(t *testing.T, blockerLines ...string) (g *engine.Game, a, b engine.PlayerID, blocker engine.CardID, attackers [2]engine.CardID, hostPump func(line string)) {
	t.Helper()
	g, a, b = newTwoPlayerGame(t)
	attackers[0] = g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	attackers[1] = g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	blocker = g.NewCard(scriptDef(t, "Test Wall", "Creature Wall", blockerLines...), b, engine.Battlefield)
	hostPump = func(line string) {
		if _, err := resolveNow(t, g, b, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(blocker)}, line); err != nil {
			t.Fatal(err)
		}
		sba(g)
	}
	return
}

func declareDoubleBlock(t *testing.T, g *engine.Game, a engine.PlayerID, blocker engine.CardID, attackers [2]engine.CardID) error {
	t.Helper()
	g.SetTurnState(1, a, engine.Main1)
	ac := engine.NewScriptedController()
	ac.QueueAttackers(attackers[:])
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attackers[0]}, {Blocker: blocker, Attacker: attackers[1]}})
	_, err := g.DeclareCombatBlockers(bc)
	return err
}

func TestCanBlockAnAdditionalCreatureFromAStatic(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		line string
		ok   bool
	}{
		{"no static", "", false},
		{"one more", "S:Mode$ Continuous | AffectedDefined$ Self | CanBlockAmount$ 1", true},
		{"any number", "S:Mode$ Continuous | AffectedDefined$ Self | CanBlockAny$ True", true},
	} {
		var lines []string
		if tc.line != "" {
			lines = append(lines, tc.line)
		}
		g, a, _, blocker, attackers, _ := twoAttackerBlock(t, lines...)
		sba(g)
		if err := declareDoubleBlock(t, g, a, blocker, attackers); (err == nil) != tc.ok {
			t.Errorf("%s: err = %v, want ok=%v", tc.name, err, tc.ok)
		}
	}
}

func TestPumpCanBlockAmountLetsABlockerTakeTwo(t *testing.T) {
	t.Parallel()

	g, a, _, blocker, attackers, pump := twoAttackerBlock(t)
	pump("DB$ Pump | ValidTgts$ Creature | CanBlockAmount$ 1")
	if err := declareDoubleBlock(t, g, a, blocker, attackers); err != nil {
		t.Errorf("pumped blocker: %v", err)
	}
}

// UnblockCreaturesBlockedOnlyBy$ (RemoveFromCombatEffect.java:47-65): the
// attacker a removed blocker held alone is unblocked, not "blocked".
func TestRemoveFromCombatUnblocksWhatItBlockedAlone(t *testing.T) {
	t.Parallel()

	g, a, b := newTwoPlayerGame(t)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	g.SetTurnState(1, a, engine.Main1)
	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attacker}})
	declareBlockers(t, g, bc)
	if _, err := resolveNow(t, g, b, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(blocker)},
		"DB$ RemoveFromCombat | ValidTgts$ Creature | UnblockCreaturesBlockedOnlyBy$ Targeted"); err != nil {
		t.Fatal(err)
	}
	for _, blk := range g.Blocks() {
		if blk.Attacker == attacker {
			t.Errorf("the attacker is still blocked by %v", blk.Blocker)
		}
	}
}

// Attach Move$ with Object$ and Choices$ (AttachEffect.java:93-103, Aura
// Graft): the new host is a permanent other than the one the Aura is on.
func TestAttachMoveOffersOnlyOtherHosts(t *testing.T) {
	t.Parallel()

	for _, pick := range []string{"same", "other"} {
		g, p, _ := newTwoPlayerGame(t)
		first := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		second := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		aura := g.NewCard(auraDefWithEnchant(t, "Creature"), p, engine.Battlefield)
		g.Attach(aura, first)
		c := engine.NewScriptedController()
		chosen := second
		if pick == "same" {
			chosen = first
		}
		c.QueueCardChoice([]engine.CardID{chosen})
		_, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(aura)},
			"DB$ Attach | ValidTgts$ Aura | Object$ Targeted | Choices$ Creature | Move$ True")
		if (err == nil) != (pick == "other") {
			t.Errorf("%s host: err = %v", pick, err)
		}
		if host, _ := g.Card(aura).AttachedTo(); pick == "other" && host != second {
			t.Errorf("the Aura is on %v, want %v", host, second)
		}
	}
}
