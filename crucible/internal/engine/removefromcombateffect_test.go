package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// etbRemoveFromCombatTriggerDefParams builds a *compile.Card whose own
// "when CARDNAME enters" trigger runs DB$ RemoveFromCombat with the given
// params string appended -- etbDestroyTriggerDefParams' own shape
// (destroyeffect_test.go).
func etbRemoveFromCombatTriggerDefParams(t *testing.T, name, extraParams string) *compile.Card {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "1", "1"
	raw.Faces[0].ManaCost = mana.MustParse("G")
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigRemoveFromCombat",
	}
	raw.Faces[0].SVars.Set("TrigRemoveFromCombat", "DB$ RemoveFromCombat | "+extraParams)

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBRemoveFromCombat casts def for p on controller c and resolves the
// stack -- castETBDestroy's own shape (destroyeffect_test.go).
func castETBRemoveFromCombat(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, c *engine.ScriptedController) (engine.CardID, error) {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return creature, g.ResolveStack(engine.NewRegistry(), c)
}

// TestRemoveFromCombatEffectRemovesAnAttacker proves the corpus's own
// dominant real shape: a ValidTgts$ target that is currently attacking stops
// attacking without moving or destroying it.
func TestRemoveFromCombatEffectRemovesAnAttacker(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)
	if len(g.Attackers()) != 1 {
		t.Fatalf("setup: Attackers() = %v, want 1 attacker", g.Attackers())
	}

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(attacker)})
	_, err := castETBRemoveFromCombat(t, g, p, etbRemoveFromCombatTriggerDefParams(t, "Test RemoveFromCombat Attacker", "ValidTgts$ Creature"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if len(g.Attackers()) != 0 {
		t.Errorf("Attackers() = %v, want none", g.Attackers())
	}
	if g.Card(attacker).Zone != engine.Battlefield {
		t.Errorf("attacker zone = %v, want Battlefield (RemoveFromCombat does not move the card)", g.Card(attacker).Zone)
	}
}

// TestRemoveFromCombatEffectRemovesABlocker proves the mirror shape: a
// blocker leaving combat drops every Block naming it, leaving the attacker
// itself untouched.
func TestRemoveFromCombatEffectRemovesABlocker(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attacker}})
	declareBlockers(t, g, bc)
	if len(g.Blocks()) != 1 {
		t.Fatalf("setup: Blocks() = %v, want 1 block", g.Blocks())
	}

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(blocker)})
	_, err := castETBRemoveFromCombat(t, g, p, etbRemoveFromCombatTriggerDefParams(t, "Test RemoveFromCombat Blocker", "ValidTgts$ Creature"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if len(g.Blocks()) != 0 {
		t.Errorf("Blocks() = %v, want none", g.Blocks())
	}
	if len(g.Attackers()) != 1 {
		t.Errorf("Attackers() = %v, want the attacker still attacking", g.Attackers())
	}
}

// TestRemoveFromCombatEffectBlockerLeavesAttackerBlocked proves CR
// 506.4/509.1h: an attacker whose only blocker is removed from combat
// stays blocked (Java's AttackingBand keeps its blocked flag,
// Combat.removeFromCombat) and deals no combat damage to the player.
func TestRemoveFromCombatEffectBlockerLeavesAttackerBlocked(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attacker}})
	declareBlockers(t, g, bc)

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(blocker)})
	if _, err := castETBRemoveFromCombat(t, g, p, etbRemoveFromCombatTriggerDefParams(t, "Test RemoveFromCombat Only Blocker", "ValidTgts$ Creature"), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !engine.Matches(g, g.Card(attacker), valid.Parse("Card.blocked"), p, engine.NoCard) {
		t.Error("the attacker is no longer blocked after its only blocker left combat")
	}
	g.DealCombatDamage(engine.NewScriptedController())
	if life := g.Player(other).Life; life != 20 {
		t.Errorf("defender life = %d, want 20 (a blocked attacker deals no damage to the player)", life)
	}
}

// TestRemoveFromCombatEffectRejectsUnresolvedParam proves
// removeFromCombatUnresolvedParams' own fail-loud contract (PORT-8/GO-7).
func TestRemoveFromCombatEffectRejectsUnresolvedParam(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	c := engine.NewScriptedController()
	_, err := castETBRemoveFromCombat(t, g, p, etbRemoveFromCombatTriggerDefParams(t, "Test RemoveFromCombat Unblock", "Defined$ Self | UnblockCreaturesBlockedOnlyBy$ Self"), c)
	if err == nil {
		t.Fatal("ResolveStack succeeded, want an error for unresolved UnblockCreaturesBlockedOnlyBy$")
	}
}
