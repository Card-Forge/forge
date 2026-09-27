package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// These tests cover AddHiddenKeyword$ (StaticAbilityContinuous.java's
// RULES-layer hidden keyword grant): a whole keyword line block and attack
// legality read exactly the way they read a printed one, recomputed every
// state-based-action pass.

// A real Lure on an attacker: every creature able to block it must.
func TestAddHiddenKeywordLureOnEnchantedAttacker(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	lure := g.NewCard(corpusCard(t, "Lure"), a, engine.Battlefield)
	g.Attach(lure, attacker)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	declareAttacking(t, g, attacker)

	wantIllegal(t, tryBlocks(g), "CR 509.1c")
	if err := tryBlocks(g, engine.Block{Blocker: blocker, Attacker: attacker}); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
}

// A hidden keyword is not in the card's keyword list: nothing that reads
// keywords (HasKeyword, KeywordLines) sees it -- the one difference from
// an AddKeyword$ grant.
func TestAddHiddenKeywordIsNotAKeywordLine(t *testing.T) {
	t.Parallel()
	g, a, _ := combatGame(t)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	lure := g.NewCard(corpusCard(t, "Lure"), a, engine.Battlefield)
	g.Attach(lure, attacker)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if lines := g.Card(attacker).KeywordLines(); len(lines) != 0 {
		t.Errorf("KeywordLines = %q, want none: a hidden keyword is not a keyword line", lines)
	}
}

// The Effect-card shape (Barrage of Boulders' KWPump, AffectedZone$
// Battlefield written out): every matching creature can't block, a
// non-matching one still can -- and once the source is gone, the next
// check drops the grant.
func TestAddHiddenKeywordCantBlockBlanket(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	g.NewCard(continuousDef(t, "Boulders",
		"Mode$ Continuous | AffectedZone$ Battlefield | Affected$ Creature.withoutFlying | AddHiddenKeyword$ CARDNAME can't block."),
		b, engine.Battlefield)
	grounded := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	flyer := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Flying"), b, engine.Battlefield)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	declareAttacking(t, g, attacker)

	wantIllegal(t, tryBlocks(g, engine.Block{Blocker: grounded, Attacker: attacker}), "CR 509.1b")
	if err := tryBlocks(g, engine.Block{Blocker: flyer, Attacker: attacker}); err != nil {
		t.Fatalf("flyer's block: %v", err)
	}

	g2, a2, b2 := combatGame(t)
	attacker2 := g2.NewCard(creatureDefPT(t, "2", "2"), a2, engine.Battlefield)
	source2 := g2.NewCard(continuousDef(t, "Boulders",
		"Mode$ Continuous | AffectedZone$ Battlefield | Affected$ Creature.withoutFlying | AddHiddenKeyword$ CARDNAME can't block."),
		b2, engine.Battlefield)
	grounded2 := g2.NewCard(creatureDefPT(t, "2", "2"), b2, engine.Battlefield)
	engine.CheckStateBasedActions(g2, engine.NewScriptedController())
	g2.Move(source2, engine.Graveyard, b2)
	engine.CheckStateBasedActions(g2, engine.NewScriptedController())
	declareAttacking(t, g2, attacker2)
	if err := tryBlocks(g2, engine.Block{Blocker: grounded2, Attacker: attacker2}); err != nil {
		t.Fatalf("block after the source left: %v", err)
	}
}

// "CARDNAME can't attack or block." (AffectedDefined$ Self): the attack half
// is StaticAbilityCantAttackBlock.cantAttack's own keyword check.
func TestAddHiddenKeywordCantAttackOrBlockStopsAttacking(t *testing.T) {
	t.Parallel()
	g, a, _ := combatGame(t)
	pacified := g.NewCard(copyTestDef(t, "Pacified Elf", "Creature Elf", "2", "2",
		"S:Mode$ Continuous | AffectedDefined$ Self | AddHiddenKeyword$ CARDNAME can't attack or block."),
		a, engine.Battlefield)
	// A second, eligible creature: with none, the controller is never asked.
	g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{pacified})
	_, err := g.DeclareCombatAttackers(ac)
	wantIllegal(t, err, "CR 508.1a")
}

// A line naming a keyword nothing in this port reads is skipped whole, not
// half-applied: "doesn't untap" beside "can't block" grants neither.
func TestAddHiddenKeywordUnreadLineIsSkippedWhole(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	g.NewCard(continuousDef(t, "Half Read",
		"Mode$ Continuous | Affected$ Creature | AddHiddenKeyword$ CARDNAME can't block. & This card doesn't untap during your next untap step."),
		b, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	declareAttacking(t, g, attacker)

	if err := tryBlocks(g, engine.Block{Blocker: blocker, Attacker: attacker}); err != nil {
		t.Fatalf("block: %v -- a line with an unread keyword must grant nothing", err)
	}
}
