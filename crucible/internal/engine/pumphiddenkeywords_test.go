package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Pump/PumpAll KW$ "HIDDEN " lines: PumpEffect.applyPump (PumpEffect.java:48-73)
// puts them on the card as hidden extrinsic keywords, read only by
// Card.hasKeyword's exact-text shortcut, removed with the pump at cleanup, and
// -- for "doesn't untap during your next untap step" -- by the untap step that
// read it (Untap.java:157-161).

// TestPumpHiddenCantBlockLastsUntilCleanup is Mugging's DBPumpCurse shape: the
// target can't block for the rest of the turn, and can again after cleanup.
func TestPumpHiddenCantBlockLastsUntilCleanup(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	attacker := g.NewCard(creatureDef(t), p, engine.Battlefield)
	blocker := g.NewCard(creatureDef(t), other, engine.Battlefield)
	if !g.CanBlock(attacker, blocker) {
		t.Fatal("blocker cannot block before the pump")
	}
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(blocker)},
		"DB$ Pump | ValidTgts$ Creature | IsCurse$ True | KW$ HIDDEN CARDNAME can't block."); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(blocker).HasKeyword("CARDNAME can't block.") {
		t.Error("the hidden line is in the keyword list, want it hidden")
	}
	if g.CanBlock(attacker, blocker) {
		t.Error("blocker can block, want the pumped \"can't block\" to stop it")
	}

	g.SetTurnState(1, p, engine.EndOfTurn)
	g.AdvancePhase(engine.NewScriptedController()) // -> Cleanup
	if !g.CanBlock(attacker, blocker) {
		t.Error("blocker still cannot block after cleanup, want the pump worn off")
	}
}

// TestPumpHiddenDoesNotUntapSkipsTheControllersNextUntapStep is Frost Breath's
// TrigPump (Duration$ Permanent): the opponent's creature stays tapped through
// its controller's next untap step and untaps at the one after. The pumping
// player's own untap step neither untaps it nor ends the effect.
func TestPumpHiddenDoesNotUntapSkipsTheControllersNextUntapStep(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	frozen := g.NewCard(creatureDef(t), other, engine.Battlefield)
	g.Card(frozen).Tapped = true
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(frozen)},
		"DB$ Pump | ValidTgts$ Creature | Duration$ Permanent | KW$ HIDDEN This card doesn't untap during your next untap step."); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	c := engine.NewScriptedController()
	g.StartTurn(p, c)
	if !g.Card(frozen).Tapped {
		t.Fatal("untapped during its non-controller's untap step")
	}

	clone := g.Clone()
	g.StartTurn(other, c)
	if !g.Card(frozen).Tapped {
		t.Fatal("untapped during its controller's next untap step, want it to stay tapped")
	}
	g.StartTurn(other, c)
	if g.Card(frozen).Tapped {
		t.Error("still tapped at the untap step after, want the effect used up")
	}

	// The clone taken before the untap step shares no pump state: the
	// effect is still waiting there.
	clone.StartTurn(other, c)
	if !clone.Card(frozen).Tapped {
		t.Error("clone untapped: the original's untap step ended the clone's effect too")
	}
}

// TestPumpHiddenDoesNotUntapKeepsTheRestOfThePump proves the untap step takes
// only the untap line off a record: the same Permanent pump's +1/+0 stays.
func TestPumpHiddenDoesNotUntapKeepsTheRestOfThePump(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	frozen := g.NewCard(creatureDef(t), other, engine.Battlefield)
	g.Card(frozen).Tapped = true
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(frozen)},
		"DB$ Pump | ValidTgts$ Creature | NumAtt$ +1 | Duration$ Permanent | KW$ HIDDEN This card doesn't untap during your next untap step."); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	c := engine.NewScriptedController()
	g.StartTurn(other, c)
	g.StartTurn(other, c)
	if g.Card(frozen).Tapped {
		t.Error("still tapped at the second untap step")
	}
	if pw, _ := g.Card(frozen).Power(); pw != 3 {
		t.Errorf("power = %d, want 3: the +1/+0 is Permanent", pw)
	}
}

// TestPumpAllHiddenAndPlainKeywordsTogether is Goblin War Cry's PumpAll shape
// with a plain keyword beside the hidden line: every matching creature gets
// both, the others neither.
func TestPumpAllHiddenAndPlainKeywordsTogether(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(creatureDef(t), p, engine.Battlefield)
	theirs := g.NewCard(creatureDef(t), other, engine.Battlefield)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ PumpAll | ValidCards$ Creature.OppCtrl | KW$ Trample & HIDDEN CARDNAME can't block.")
	if !g.Card(theirs).HasKeyword("Trample") || g.CanBlock(mine, theirs) {
		t.Errorf("opponent's creature: trample=%v canBlock=%v, want true/false",
			g.Card(theirs).HasKeyword("Trample"), g.CanBlock(mine, theirs))
	}
	if g.Card(mine).HasKeyword("Trample") || !g.CanBlock(theirs, mine) {
		t.Error("p's own creature was pumped, want ValidCards$ Creature.OppCtrl only")
	}
}

// TestPumpHiddenCantAttackLeavesItOutOfTheAttack is Change of Heart: the
// target is no eligible attacker this turn.
func TestPumpHiddenCantAttackLeavesItOutOfTheAttack(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	stopped := g.NewCard(creatureDef(t), p, engine.Battlefield)
	free := g.NewCard(creatureDef(t), p, engine.Battlefield)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(stopped)},
		"DB$ Pump | ValidTgts$ Creature | KW$ HIDDEN CARDNAME can't attack."); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	g.Card(stopped).SummonSick, g.Card(free).SummonSick = false, false
	g.SetTurnState(1, p, engine.DeclareAttackers)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{stopped})
	if _, err := g.DeclareCombatAttackers(c); err == nil || !strings.Contains(err.Error(), "not an eligible attacker") {
		t.Errorf("DeclareCombatAttackers = %v, want the pumped creature refused", err)
	}
	c.QueueAttackers([]engine.CardID{free})
	attackers, err := g.DeclareCombatAttackers(c)
	if err != nil || !slices.Equal(attackers, []engine.CardID{free}) {
		t.Errorf("DeclareCombatAttackers = %v, %v, want the other creature to attack", attackers, err)
	}
}

// TestPumpHiddenRejectsWhatNothingReads covers the KW$ shapes that stay loud
// errors (GO-7): a hidden line no rule here reads, a hidden line given to a
// player, and a plain keyword carrying a dynamic marker with no DefinedKW$.
func TestPumpHiddenRejectsWhatNothingReads(t *testing.T) {
	t.Parallel()

	cases := []struct {
		name, line, want string
		player           bool
	}{
		// _____ _____ _____ Trespasser (b/____________trespasser.txt:7): no
		// Java code reads "Unblockable" any more.
		{"unread", "DB$ Pump | Defined$ Self | KW$ HIDDEN Unblockable", `hidden keyword "Unblockable"`, false},
		{"player", "DB$ Pump | ValidTgts$ Player | KW$ HIDDEN CARDNAME can't block.", "on a player", true},
		{"dynamic", "DB$ Pump | Defined$ Self | KW$ Protection:Card.ChosenColor:chosenColor", "ChosenColor", false},
		{"pumpall", "DB$ PumpAll | ValidCards$ Creature | KW$ HIDDEN Unblockable", `PumpAll: KW$ hidden keyword "Unblockable"`, false},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			var targets []engine.EntityID
			if tc.player {
				targets = []engine.EntityID{engine.PlayerEntity(other)}
			}
			_, err := resolveNow(t, g, p, engine.NewScriptedController(), targets, tc.line)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Errorf("ResolveStack = %v, want an error containing %q", err, tc.want)
			}
		})
	}
}
