package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// canDeclare reports whether attacker is accepted as an attacker for a's turn
// (CR 508.1a): the controller is offered only eligible creatures.
func canDeclare(g *engine.Game, attacker engine.CardID) bool {
	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	attackers, err := g.DeclareCombatAttackers(ac)
	return err == nil && len(attackers) == 1
}

// CR 702.3b: a creature with defender can't attack, and a CanAttackDefender
// static lifts that (Mode$ CanAttackDefender, "as though it didn't have
// defender").
func TestDefenderCannotAttackUnlessCanAttackDefender(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		line string
		want bool
	}{
		{"defender", "", false},
		{"lifted for creatures you control", "S:Mode$ CanAttackDefender | ValidCard$ Creature.YouCtrl", true},
		{"lifted for another creature only", "S:Mode$ CanAttackDefender | ValidCard$ Creature.OppCtrl", false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, a, _ := combatGame(t)
			if tc.line != "" {
				g.NewCard(scriptDef(t, "Test Rolling Stones", "Enchantment", tc.line), a, engine.Battlefield)
			}
			wall := g.NewCard(creatureDefPTKeywords(t, "0", "4", "Defender"), a, engine.Battlefield)
			if got := canDeclare(g, wall); got != tc.want {
				t.Errorf("a defender can be declared as an attacker = %v, want %v", got, tc.want)
			}
		})
	}
}

// Mode$ CantAttack names the creatures (ValidCard$) and, with Target$, the
// defenders they can't attack: Pacifism-style "enchanted creature can't
// attack", and "creatures can't attack you" (Ghostly Prison's Target$ You).
func TestCantAttackStatics(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name       string
		line       string
		staticSide string // "own" puts the static under the attacker's controller, "opp" under the defender's
		want       bool
	}{
		{"all creatures", "S:Mode$ CantAttack | ValidCard$ Creature", "opp", false},
		{"only withoutFlying", "S:Mode$ CantAttack | ValidCard$ Creature.withoutFlying", "opp", false},
		{"only withFlying", "S:Mode$ CantAttack | ValidCard$ Creature.withFlying", "opp", true},
		{"creatures can't attack you", "S:Mode$ CantAttack | ValidCard$ Creature | Target$ You", "opp", false},
		{"creatures can't attack you, static on the attacker's side", "S:Mode$ CantAttack | ValidCard$ Creature | Target$ You", "own", true},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, a, b := combatGame(t)
			side := b
			if tc.staticSide == "own" {
				side = a
			}
			g.NewCard(scriptDef(t, "Test Prison", "Enchantment", tc.line), side, engine.Battlefield)
			bear := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
			if got := canDeclare(g, bear); got != tc.want {
				t.Errorf("a ground creature can attack = %v, want %v", got, tc.want)
			}
		})
	}
}

// With no creature able to attack, the controller is not asked at all.
func TestCreatureThatCannotAttackAnyoneIsNotEligible(t *testing.T) {
	t.Parallel()

	g, a, b := combatGame(t)
	g.NewCard(scriptDef(t, "Test Prison", "Enchantment", "S:Mode$ CantAttack | ValidCard$ Creature"), b, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	attackers, err := g.DeclareCombatAttackers(engine.NewScriptedController())
	if err != nil || len(attackers) != 0 {
		t.Errorf("DeclareCombatAttackers = %v, %v; want no attackers and no error", attackers, err)
	}
}

// Mode$ CantBlock (141 cards): "CARDNAME can't block", Pacifism's enchanted
// creature, Threshold-conditioned lines.
func TestCantBlockStatics(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		line string
		want bool
	}{
		{"no static", "", true},
		{"the creature itself", "S:Mode$ CantBlock | ValidCard$ Card.Self", false},
		{"every creature", "S:Mode$ CantBlock | ValidCard$ Creature", false},
		{"only white creatures", "S:Mode$ CantBlock | ValidCard$ Creature.White", true},
		{"a condition that fails", "S:Mode$ CantBlock | ValidCard$ Card.Self | Condition$ Threshold", true},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, a, b := combatGame(t)
			attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
			def := scriptDef(t, "Test Blocker", "Creature Elf")
			if tc.line != "" {
				def.Faces[0].Statics = append(def.Faces[0].Statics, scriptDef(t, "x", "Enchantment", tc.line).Faces[0].Statics...)
			}
			blocker := g.NewCard(def, b, engine.Battlefield)
			if got := g.CanBlock(attacker, blocker); got != tc.want {
				t.Errorf("CanBlock = %v, want %v", got, tc.want)
			}
		})
	}
}

// An Animate granting staticAbilities$ Mode$ CantBlock gives the animated
// creature the static, read like a printed one (Traumatic Prank).
func TestAnimateGrantsACantBlockStatic(t *testing.T) {
	t.Parallel()

	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	if !g.CanBlock(attacker, blocker) {
		t.Fatal("the blocker cannot block before the grant")
	}
	if _, err := resolveNow(t, g, b, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(blocker)},
		"DB$ Animate | ValidTgts$ Creature | staticAbilities$ NoBlock | Duration$ Permanent",
		"NoBlock", "Mode$ CantBlock | ValidCard$ Card.Self"); err != nil {
		t.Fatal(err)
	}
	if g.CanBlock(attacker, blocker) {
		t.Error("the blocker can still block after the grant")
	}
}
