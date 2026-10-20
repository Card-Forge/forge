package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// addUntilCommand's Duration$ table (SpellAbilityEffect.java:954-1031): each
// duration puts the ending command on its own Phase or card list. The tests
// drive the real steps (SetTurnState runs no phase hooks) and read a Pump's
// +3 power through the card.

const untilPump = "DB$ Pump | ValidTgts$ Creature | NumAtt$ +3 | Duration$ "

// pumpedUntil resolves the Pump above for p against a fresh 2/2 on p's side and
// reports its power as turn and phase are reached in turn.
func pumpedUntil(t *testing.T, duration string, checks ...struct {
	turn  int
	phase engine.PhaseType
	power int
}) engine.CardID {
	t.Helper()
	g, p, _ := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(victim)}, untilPump+duration); err != nil {
		t.Fatalf("%s: %v", duration, err)
	}
	if got, _ := g.Card(victim).Power(); got != 5 {
		t.Fatalf("%s: power = %d right after resolving, want 5", duration, got)
	}
	for _, ck := range checks {
		advanceToPhase(t, g, c, ck.turn, ck.phase)
		if got, _ := g.Card(victim).Power(); got != ck.power {
			t.Errorf("%s: power at turn %d %v = %d, want %d", duration, ck.turn, ck.phase, got, ck.power)
		}
	}
	return victim
}

type untilCheck = struct {
	turn  int
	phase engine.PhaseType
	power int
}

func TestUntilEndOfCombatEndsWithCombat(t *testing.T) {
	t.Parallel()
	pumpedUntil(t, "UntilEndOfCombat",
		untilCheck{1, engine.CombatEnd, 5}, untilCheck{1, engine.Main2, 2})
}

func TestUntilYourNextEndStepEndsAsYourEndStepBegins(t *testing.T) {
	t.Parallel()
	pumpedUntil(t, "UntilYourNextEndStep",
		untilCheck{1, engine.Main2, 5}, untilCheck{1, engine.EndOfTurn, 2})
}

func TestUntilYourNextUpkeepSkipsTheOpponentsUpkeep(t *testing.T) {
	t.Parallel()
	pumpedUntil(t, "UntilYourNextUpkeep",
		untilCheck{1, engine.Cleanup, 5}, untilCheck{2, engine.Upkeep, 5}, untilCheck{3, engine.Upkeep, 2})
}

func TestUntilTheEndOfYourNextUntapEndsAfterYourUntapStep(t *testing.T) {
	t.Parallel()
	pumpedUntil(t, "UntilTheEndOfYourNextUntap",
		untilCheck{3, engine.Untap, 5}, untilCheck{3, engine.Upkeep, 2})
}

// Created in your own main phase, "until the end of your next upkeep" is the
// next turn's upkeep, not one that already passed (addUntilEnd).
func TestUntilTheEndOfYourNextUpkeepWaitsForTheNextOne(t *testing.T) {
	t.Parallel()
	pumpedUntil(t, "UntilTheEndOfYourNextUpkeep",
		untilCheck{3, engine.Upkeep, 5}, untilCheck{3, engine.Draw, 2})
}

// A host-bound duration ends as the host leaves the battlefield.
func TestAsLongAsInPlayEndsWhenTheHostLeaves(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	host, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(victim)}, untilPump+"AsLongAsInPlay")
	if err != nil {
		t.Fatal(err)
	}
	advanceToPhase(t, g, c, 2, engine.Main1)
	if got, _ := g.Card(victim).Power(); got != 5 {
		t.Fatalf("power = %d after the turn changed, want 5", got)
	}
	g.Move(host, engine.Graveyard, p)
	sba(g)
	if got, _ := g.Card(victim).Power(); got != 2 {
		t.Errorf("power = %d after the host left, want 2", got)
	}
}

// UntilUntaps: checkValidDuration makes a line whose host is untapped do
// nothing; a tapped host's pump ends as it untaps.
func TestUntilUntapsEndsAsTheHostUntaps(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	host := g.NewCard(scriptDef(t, "Test Tapper", "Creature Elf", "K:Haste",
		"A:AB$ Pump | Cost$ T | Defined$ Self | NumAtt$ +3 | Duration$ UntilUntaps"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	if !activateAndResolve(t, g, p, host, c) {
		t.Fatal("the pump was not activated")
	}
	if got, _ := g.Card(host).Power(); got != 4 {
		t.Fatalf("power = %d, want 4", got)
	}
	advanceToPhase(t, g, c, 2, engine.Main1)
	if got, _ := g.Card(host).Power(); got != 4 {
		t.Errorf("power = %d in the opponent's turn, want 4: the host is still tapped", got)
	}
	advanceToPhase(t, g, c, 3, engine.Upkeep)
	if got, _ := g.Card(host).Power(); got != 1 {
		t.Errorf("power = %d after the untap step, want 1", got)
	}

	// An untapped host: the effect does not apply at all.
	g, p, _ = newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)}, untilPump+"UntilUntaps"); err != nil {
		t.Fatal(err)
	}
	if got, _ := g.Card(victim).Power(); got != 2 {
		t.Errorf("power = %d from an untapped host's line, want 2", got)
	}
}

// A duration Java names that this port cannot register is refused, not
// treated as end of turn; one it does not name ends at cleanup.
func TestUnregisteredDurationsAreRefusedAndUnknownOnesEndAtCleanup(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)}, untilPump+"UntilFacedown")
	if err == nil || !strings.Contains(err.Error(), "UntilFacedown") {
		t.Errorf("err = %v, want the UntilFacedown refusal", err)
	}
	pumpedUntil(t, "NotADuration", untilCheck{1, engine.Main2, 5}, untilCheck{2, engine.Main1, 2})
}

// Effect rides the same table: the effect card goes as the duration says.
func TestEffectUntilYourNextEndStepLeavesTheCommandZone(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ Effect | RememberObjects$ Self | StaticAbilities$ Unblockable | Duration$ UntilYourNextEndStep",
		"Unblockable", "Mode$ CantBlockBy | ValidAttacker$ Creature.IsRemembered")
	advanceToPhase(t, g, c, 1, engine.Main2)
	if n := len(commandEffects(g, p)); n != 1 {
		t.Fatalf("command zone holds %d effects before the end step, want 1", n)
	}
	advanceToPhase(t, g, c, 1, engine.EndOfTurn)
	if n := len(commandEffects(g, p)); n != 0 {
		t.Errorf("command zone holds %d effects after the end step began, want 0", n)
	}
}

// Animate's Duration$ UntilYourNextTurn ends as the activator's next turn
// begins, the cleanup list's run (PhaseHandler.java:518).
func TestAnimateUntilYourNextTurnEndsAtTheirNextTurn(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(victim)},
		"DB$ Animate | ValidTgts$ Creature | Power$ 0 | Toughness$ 1 | Duration$ UntilYourNextTurn"); err != nil {
		t.Fatal(err)
	}
	for _, step := range []struct {
		turn  int
		phase engine.PhaseType
		want  int
	}{{1, engine.Cleanup, 0}, {2, engine.Main1, 0}, {3, engine.Untap, 2}} {
		advanceToPhase(t, g, c, step.turn, step.phase)
		if got, _ := g.Card(victim).Power(); got != step.want {
			t.Errorf("power at turn %d %v = %d, want %d", step.turn, step.phase, got, step.want)
		}
	}
}

// PumpAll's AtEOT$ (PumpAllEffect.java:98-100): the pumped cards are
// remembered by an end-step trigger that acts on them.
func TestPumpAllAtEOTActsOnEveryPumpedCard(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ PumpAll | ValidCards$ Creature.powerEQ2 | NumAtt$ +1 | AtEOT$ Exile")
	advanceToPhase(t, g, c, 1, engine.Cleanup)
	for _, id := range []engine.CardID{a, b} {
		if z := g.Card(id).Zone; z != engine.Exile {
			t.Errorf("creature %v is in %v, want Exile", id, z)
		}
	}
}

// ChangeZoneAll's Duration$ UntilHostLeavesPlay (ChangeZoneAllEffect.java:206,
// changeZoneUntilCommand): the sweep's cards return as the host leaves.
func TestChangeZoneAllUntilHostLeavesPlayReturnsTheCards(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host, err := resolveNow(t, g, p, c, nil,
		"DB$ ChangeZoneAll | ChangeType$ Creature.OppCtrl | Origin$ Battlefield | Destination$ Exile | RandomOrder$ True | Duration$ UntilHostLeavesPlay")
	if err != nil {
		t.Fatal(err)
	}
	for _, id := range []engine.CardID{a, b} {
		if z := g.Card(id).Zone; z != engine.Exile {
			t.Fatalf("creature %v is in %v, want Exile", id, z)
		}
	}
	g.Move(host, engine.Graveyard, p)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	sba(g)
	for _, id := range []engine.CardID{a, b} {
		if z := g.Card(id).Zone; z != engine.Battlefield {
			t.Errorf("creature %v is in %v after the host left, want Battlefield", id, z)
		}
	}
}

// The Effect durations that ride the remaining lists: after the next
// state-based-action check, this turn and next, and the end of combat of the
// owner's next turn (SpellAbilityEffect.java:985-998, GameAction.java:1667).
func TestEffectDurationsOnTheRemainingLists(t *testing.T) {
	t.Parallel()

	const effect = "DB$ Effect | RememberObjects$ Self | StaticAbilities$ Unblockable | Duration$ "
	const static = "Mode$ CantBlockBy | ValidAttacker$ Creature.IsRemembered"
	for _, tc := range []struct {
		duration string
		// present lists where the effect card must still be, gone where not.
		present, gone []struct {
			turn  int
			phase engine.PhaseType
		}
	}{
		{"UntilStateBasedActionChecked", nil, []struct {
			turn  int
			phase engine.PhaseType
		}{{1, engine.Main1}}},
		{"ThisTurnAndNextTurn", []struct {
			turn  int
			phase engine.PhaseType
		}{{1, engine.Main2}, {2, engine.Main1}}, []struct {
			turn  int
			phase engine.PhaseType
		}{{3, engine.Untap}}},
		{"UntilEndOfCombatYourNextTurn", []struct {
			turn  int
			phase engine.PhaseType
		}{{1, engine.CombatEnd}, {3, engine.CombatBegin}}, []struct {
			turn  int
			phase engine.PhaseType
		}{{3, engine.Main2}}},
	} {
		g, p, _ := newTwoPlayerGame(t)
		c := engine.NewScriptedController()
		resolveLine(t, g, p, c, effect+tc.duration, "Unblockable", static)
		sba(g)
		for _, w := range tc.present {
			advanceToPhase(t, g, c, w.turn, w.phase)
			if n := len(commandEffects(g, p)); n != 1 {
				t.Errorf("%s: %d effects at turn %d %v, want 1", tc.duration, n, w.turn, w.phase)
			}
		}
		for _, w := range tc.gone {
			if w.turn > 1 || w.phase != engine.Main1 {
				advanceToPhase(t, g, c, w.turn, w.phase)
			}
			if n := len(commandEffects(g, p)); n != 0 {
				t.Errorf("%s: %d effects at turn %d %v, want 0", tc.duration, n, w.turn, w.phase)
			}
		}
	}
}

// Clone rides the table too: UntilNextEndStep ends the copy as the next end
// step begins (SpellAbilityEffect.java:975-976, EndOfTurn.addAt).
func TestCloneUntilNextEndStepEndsTheCopy(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | Duration$ UntilNextEndStep")
	mustActivate(t, g, p, c, host, giant)
	if !g.Card(host).IsCopy() {
		t.Fatal("the host did not become a copy")
	}
	advanceToPhase(t, g, c, 1, engine.Main2)
	if !g.Card(host).IsCopy() {
		t.Error("the copy ended before the end step")
	}
	advanceToPhase(t, g, c, 1, engine.EndOfTurn)
	if g.Card(host).IsCopy() {
		t.Error("the copy outlived the end step's beginning")
	}
}
