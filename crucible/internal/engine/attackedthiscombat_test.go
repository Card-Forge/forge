package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// This file covers the two cast-time CheckSVar$ operands the "cast this spell
// only during ..." instants read: the BeenAttackedThisCombat player property
// (PlayerProperty.java:477-483, Assassin's Blade's "only if you've been
// attacked this step") and Count$FinishedUpkeepsThisTurn (AbilityUtils.java:
// 2213-2216, Reset's "after their upkeep step"), and the combat card
// properties their targets name.

// beenAttacked is the SVar every "only if you've been attacked this step"
// instant names.
const beenAttacked = "PlayerCountPropertyYou$HasPropertyBeenAttackedThisCombat"

// attackedStepInstant is Assassin's Blade's restriction on a plain effect:
// cast only during the declare attackers step and only if the caster has
// been attacked this combat.
func attackedStepInstant(t *testing.T) func(g *engine.Game, caster engine.PlayerID) bool {
	t.Helper()
	def := copyTestDef(t, "Test Attacked Step", "Instant", "", "", "Cost:B",
		"A:SP$ GainLife | Defined$ You | LifeAmount$ 1 | CheckSVar$ X | ActivationPhases$ Declare Attackers",
		"SVar:X:"+beenAttacked)
	return func(g *engine.Game, caster engine.PlayerID) bool {
		g.Player(caster).ManaPool.Add(mana.Black, 1)
		return g.CastSpell(caster, g.NewCard(def, caster, engine.Hand), engine.NewScriptedController())
	}
}

// declareLoneAttacker makes attacker's controller the active player in its declare
// attackers step and declares attacker, at target when one is given.
func declareLoneAttacker(t *testing.T, g *engine.Game, attacker engine.CardID, target ...engine.EntityID) {
	t.Helper()
	g.SetTurnState(2, g.Card(attacker).Controller(), engine.DeclareAttackers)
	g.Card(attacker).SummonSick = false
	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	for _, e := range target {
		ac.QueueAttackTarget(e)
	}
	declareAttackers(t, g, ac)
}

func TestAttackedStepInstantNeedsTheCasterToHaveBeenAttacked(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		// attack: who declares an attack (0 the caster, 1 the opponent, -1 nobody).
		attack int
		want   bool
	}{
		{"caster attacked", 1, true},
		{"caster attacking", 0, false},
		{"no attack declared", -1, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGame(t)
			cast := attackedStepInstant(t)
			switch tc.attack {
			case 0:
				declareLoneAttacker(t, g, g.NewCard(creatureDef(t), p, engine.Battlefield))
			case 1:
				declareLoneAttacker(t, g, g.NewCard(creatureDef(t), other, engine.Battlefield))
			default:
				g.SetTurnState(2, other, engine.DeclareAttackers)
			}
			if got := cast(g, p); got != tc.want {
				t.Errorf("CastSpell = %v, want %v", got, tc.want)
			}
		})
	}
}

// TestAttackedStepInstantStillNeedsItsStep proves the CheckSVar$ adds to the
// ActivationPhases$ window rather than replacing it: attacked, but past the
// declare attackers step, the cast is refused.
func TestAttackedStepInstantStillNeedsItsStep(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	cast := attackedStepInstant(t)
	declareLoneAttacker(t, g, g.NewCard(creatureDef(t), other, engine.Battlefield))
	g.SetTurnState(2, other, engine.DeclareBlockers)
	if cast(g, p) {
		t.Error("CastSpell in declare blockers succeeded for a declare-attackers-only spell")
	}
}

func TestBeenAttackedThisCombatRecordsTheDeclaration(t *testing.T) {
	t.Parallel()

	t.Run("a planeswalker attacked is not its controller attacked", func(t *testing.T) {
		t.Parallel()

		g, p, other := newTwoPlayerGame(t)
		pw := g.NewCard(planeswalkerDefLoyalty(t, "3"), p, engine.Battlefield)
		declareLoneAttacker(t, g, g.NewCard(creatureDef(t), other, engine.Battlefield), engine.CardEntity(pw))
		if n := measure(t, g, p, beenAttacked); n != 0 {
			t.Errorf("BeenAttackedThisCombat = %d, want 0", n)
		}
	})

	t.Run("an attacker removed from combat still counts", func(t *testing.T) {
		t.Parallel()

		g, p, other := newTwoPlayerGame(t)
		attacker := g.NewCard(creatureDef(t), other, engine.Battlefield)
		declareLoneAttacker(t, g, attacker)
		remover := g.NewCard(copyTestDef(t, "Test Remover", "Artifact", "", "",
			"A:AB$ RemoveFromCombat | Cost$ 0 | ValidTgts$ Creature | Defined$ Targeted"), p, engine.Battlefield)
		if err := activate(g, p, engine.NewScriptedController(), remover, attacker); err != nil {
			t.Fatal(err)
		}
		if len(g.Attackers()) != 0 {
			t.Fatal("attacker still attacking after RemoveFromCombat")
		}
		if n := measure(t, g, p, beenAttacked); n != 1 {
			t.Errorf("BeenAttackedThisCombat = %d, want 1 (Java records the declaration)", n)
		}
	})

	t.Run("cleared as combat ends", func(t *testing.T) {
		t.Parallel()

		g, p, other := newTwoPlayerGame(t)
		declareLoneAttacker(t, g, g.NewCard(creatureDef(t), other, engine.Battlefield))
		g.SetTurnState(2, other, engine.CombatEnd)
		if n := measure(t, g, p, beenAttacked); n != 1 {
			t.Fatalf("BeenAttackedThisCombat in end of combat = %d, want 1", n)
		}
		g.AdvancePhase(engine.NewScriptedController())
		if n := measure(t, g, p, beenAttacked); n != 0 {
			t.Errorf("BeenAttackedThisCombat in %v = %d, want 0", g.ActivePhase(), n)
		}
	})

	t.Run("the clone keeps its own record", func(t *testing.T) {
		t.Parallel()

		g, p, other := newTwoPlayerGame(t)
		declareLoneAttacker(t, g, g.NewCard(creatureDef(t), other, engine.Battlefield))
		clone := g.Clone()
		g.SetTurnState(2, other, engine.CombatEnd)
		g.AdvancePhase(engine.NewScriptedController())
		if n := measure(t, clone, p, beenAttacked); n != 1 {
			t.Errorf("clone's BeenAttackedThisCombat = %d, want 1 after the original's combat ended", n)
		}
	})
}

// TestCombatTargetPropertiesReadTheAttack covers the card properties the
// combat-step instants target with: attackingYou (Flash Foliage),
// DefendingPlayerCtrl (Blaze of Glory, False Orders) and attackedYouThisTurn
// (Jabari's Influence).
func TestCombatTargetPropertiesReadTheAttack(t *testing.T) {
	t.Parallel()

	matches := func(g *engine.Game, id engine.CardID, spec string, you engine.PlayerID) bool {
		return engine.Matches(g, g.Card(id), valid.Parse(spec), you, engine.NoCard)
	}

	t.Run("attackingYou is the player, not their planeswalker", func(t *testing.T) {
		t.Parallel()

		g, p, other := newTwoPlayerGame(t)
		atMe := g.NewCard(creatureDef(t), other, engine.Battlefield)
		declareLoneAttacker(t, g, atMe)
		if !matches(g, atMe, "Creature.attackingYou", p) {
			t.Error("an attacker at p is not attackingYou for p")
		}
		if matches(g, atMe, "Creature.attackingYou", other) {
			t.Error("an attacker at p is attackingYou for its own controller")
		}

		g2, p2, other2 := newTwoPlayerGame(t)
		pw := g2.NewCard(planeswalkerDefLoyalty(t, "3"), p2, engine.Battlefield)
		atPW := g2.NewCard(creatureDef(t), other2, engine.Battlefield)
		declareLoneAttacker(t, g2, atPW, engine.CardEntity(pw))
		if matches(g2, atPW, "Creature.attackingYou", p2) {
			t.Error("an attacker at p's planeswalker is attackingYou for p")
		}
	})

	t.Run("DefendingPlayerCtrl", func(t *testing.T) {
		t.Parallel()

		g, p, other := newTwoPlayerGame(t)
		mine := g.NewCard(creatureDef(t), p, engine.Battlefield)
		attacker := g.NewCard(creatureDef(t), other, engine.Battlefield)
		if matches(g, mine, "Creature.DefendingPlayerCtrl", p) {
			t.Error("DefendingPlayerCtrl matched with no attack declared")
		}
		declareLoneAttacker(t, g, attacker)
		if !matches(g, mine, "Creature.DefendingPlayerCtrl", other) {
			t.Error("the attacked player's creature is not DefendingPlayerCtrl")
		}
		if matches(g, attacker, "Creature.DefendingPlayerCtrl", p) {
			t.Error("the attacking player's creature is DefendingPlayerCtrl")
		}

		g2, p2, other2 := newTwoPlayerGame(t)
		pw := g2.NewCard(planeswalkerDefLoyalty(t, "3"), p2, engine.Battlefield)
		mine2 := g2.NewCard(creatureDef(t), p2, engine.Battlefield)
		declareLoneAttacker(t, g2, g2.NewCard(creatureDef(t), other2, engine.Battlefield), engine.CardEntity(pw))
		if !matches(g2, mine2, "Creature.DefendingPlayer", other2) {
			t.Error("a planeswalker's controller is not the defending player")
		}
	})

	t.Run("attackedYouThisTurn outlives the combat, not the turn", func(t *testing.T) {
		t.Parallel()

		g, p, other := newTwoPlayerGame(t)
		attacker := g.NewCard(creatureDef(t), other, engine.Battlefield)
		declareLoneAttacker(t, g, attacker)
		g.SetTurnState(2, other, engine.CombatEnd)
		c := engine.NewScriptedController()
		g.AdvancePhase(c) // Main2: combat is over
		if !matches(g, attacker, "Creature.attackedYouThisTurn", p) {
			t.Error("the attacker is not attackedYouThisTurn for p after combat")
		}
		if matches(g, attacker, "Creature.attackedYouThisTurn", other) {
			t.Error("the attacker attackedYouThisTurn its own controller")
		}
		clone := g.Clone()
		for g.ActivePhase() != engine.Untap {
			g.AdvancePhase(c)
		}
		if matches(g, attacker, "Creature.attackedYouThisTurn", p) {
			t.Error("attackedYouThisTurn survived the cleanup")
		}
		if !matches(clone, attacker, "Creature.attackedYouThisTurn", p) {
			t.Error("the clone lost its record when the original's turn ended")
		}
	})
}

func TestFinishedUpkeepsThisTurnCountsUpkeepsPast(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	g.SetTurnState(1, p, engine.Untap)
	finished := func() int { return measure(t, g, p, "Count$FinishedUpkeepsThisTurn") }

	if n := finished(); n != 0 {
		t.Errorf("before the upkeep: %d, want 0", n)
	}
	g.AdvancePhase(c) // Upkeep
	if n := finished(); n != 0 {
		t.Errorf("during the upkeep: %d, want 0 (the one in progress is not finished)", n)
	}
	g.AdvancePhase(c) // Draw
	if n := finished(); n != 1 {
		t.Errorf("after the upkeep: %d, want 1", n)
	}
	for g.ActivePhase() != engine.Untap {
		g.AdvancePhase(c)
	}
	if n := finished(); n != 0 {
		t.Errorf("the next turn's untap step: %d, want 0", n)
	}
	clone := g.Clone()
	g.AdvancePhase(c)
	g.AdvancePhase(c)
	if n := measure(t, clone, p, "Count$FinishedUpkeepsThisTurn"); n != 0 {
		t.Errorf("clone after the original's upkeep: %d, want 0", n)
	}
}

// TestAfterUpkeepInstantWaitsForTheUpkeepToEnd is Reset's restriction:
// OpponentTurn$ and CheckSVar$ Count$FinishedUpkeepsThisTurn.
func TestAfterUpkeepInstantWaitsForTheUpkeepToEnd(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		phase engine.PhaseType
		want  bool
	}{
		{engine.Upkeep, false},
		{engine.Draw, true},
	} {
		t.Run(tc.phase.String(), func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGame(t)
			c := engine.NewScriptedController()
			g.SetTurnState(2, other, engine.Untap)
			for g.ActivePhase() != tc.phase {
				g.AdvancePhase(c)
			}
			def := copyTestDef(t, "Test After Upkeep", "Instant", "", "", "Cost:U",
				"A:SP$ GainLife | Defined$ You | LifeAmount$ 1 | OpponentTurn$ True | CheckSVar$ Count$FinishedUpkeepsThisTurn")
			g.Player(p).ManaPool.Add(mana.Blue, 1)
			if got := g.CastSpell(p, g.NewCard(def, p, engine.Hand), c); got != tc.want {
				t.Errorf("CastSpell in the opponent's %v = %v, want %v", tc.phase, got, tc.want)
			}
		})
	}
}
