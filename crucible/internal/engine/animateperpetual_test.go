package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Animate/AnimateAll staticAbilities$ (AnimateEffect.java:129-133) and the
// zones the grants reach: a Perpetual record survives every zone change
// (GameAction.java:265-266), an ordinary one is gone with the object.

// Zone$ Hand with a granted self ReduceCost: the {2}{G} creature in hand is
// cast for {1}{G} (CostAdjustment reads the card's own statics, EffectZone$
// All), and the grant stays when it is cast.
func TestAnimateAllHandStaticReducesTheCastCost(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		have int
		cast bool
	}{{1, false}, {2, true}} {
		g, p, _ := newTwoPlayerGame(t)
		card := g.NewCard(creatureDefCost(t, "Costly", "2 G"), p, engine.Hand)
		resolveLine(t, g, p, engine.NewScriptedController(),
			"DB$ AnimateAll | ValidCards$ Card.YouOwn | Zone$ Hand | staticAbilities$ Cheaper | Duration$ Perpetual",
			"Cheaper", "Mode$ ReduceCost | ValidCard$ Card.Self | Type$ Spell | Amount$ 1 | EffectZone$ All")
		if got := castWithGreen(g, p, card, tc.have); got != tc.cast {
			t.Errorf("casting with %d mana = %v, want %v", tc.have, got, tc.cast)
		}
	}
}

// Perpetual Power$/Toughness$ on a card in hand shows once it is on the
// battlefield; the same grant without Perpetual does not outlive the hand.
func TestAnimatePerpetualOutlivesTheZoneChange(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		duration string
		want     int
	}{{"Perpetual", 5}, {"Permanent", 1}} {
		g, p, _ := newTwoPlayerGame(t)
		card := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand)
		if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(card)},
			"DB$ Animate | ValidTgts$ Card | TgtZone$ Hand | Power$ 5 | Toughness$ 5 | Duration$ "+tc.duration); err != nil {
			t.Fatalf("%s: %v", tc.duration, err)
		}
		g.Move(card, engine.Battlefield, p)
		sba(g)
		if got, _ := g.Card(card).Power(); got != tc.want {
			t.Errorf("%s: power on the battlefield = %d, want %d", tc.duration, got, tc.want)
		}
	}
}

// A granted Continuous static works through the same trait row as a card's own.
func TestAnimateStaticAbilitiesGrantsAContinuousStatic(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)},
		"DB$ Animate | ValidTgts$ Creature | staticAbilities$ Boost | Duration$ Permanent",
		"Boost", "Mode$ Continuous | Affected$ Card.Self | AddPower$ 2"); err != nil {
		t.Fatal(err)
	}
	sba(g)
	if got, _ := g.Card(victim).Power(); got != 3 {
		t.Errorf("power = %d, want 3", got)
	}
}

// A mode nothing reads is refused rather than granted inert.
func TestAnimateStaticAbilitiesRefusesAModeNothingReads(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)},
		"DB$ Animate | ValidTgts$ Creature | staticAbilities$ Spend | Duration$ Permanent",
		"Spend", "Mode$ ManaConvert | ValidCard$ Card.Self | ManaConversion$ AnyType->AnyColor")
	if err == nil {
		t.Error("a ManaConvert grant was accepted, want a refusal")
	}
}

// Optional$ lets the activator decline the whole animation
// (AnimateEffect.java:155-163).
func TestAnimateOptionalCanBeDeclined(t *testing.T) {
	t.Parallel()

	for _, confirm := range []bool{true, false} {
		g, p, _ := newTwoPlayerGame(t)
		victim := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
		c := engine.NewScriptedController()
		c.QueueConfirmEffect(confirm)
		if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(victim)},
			"DB$ Animate | ValidTgts$ Creature | Power$ 4 | Optional$ True | Duration$ Permanent"); err != nil {
			t.Fatal(err)
		}
		sba(g)
		want := 1
		if confirm {
			want = 4
		}
		if got, _ := g.Card(victim).Power(); got != want {
			t.Errorf("confirm %v: power = %d, want %d", confirm, got, want)
		}
	}
}

// Defined$ Valid <spec> (AbilityUtils.java:287-303) reaches every effect that
// reads its cards through the ability's refs: a Pump of every creature you
// control.
func TestDefinedValidReachesPump(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
		"DB$ Pump | Defined$ Valid Creature.YouCtrl | NumAtt$ +2"); err != nil {
		t.Fatal(err)
	}
	for id, want := range map[engine.CardID]int{mine: 3, theirs: 1} {
		if got, _ := g.Card(id).Power(); got != want {
			t.Errorf("card %d: power = %d, want %d", id, got, want)
		}
	}
}
