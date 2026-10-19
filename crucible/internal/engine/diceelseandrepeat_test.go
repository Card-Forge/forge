package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// An "Else:SVar" pair inside ResultSubAbilities$ is the roll's Else ability
// (AbilityFactory.java:265-271 keys each pair's ability by its key;
// RollDiceEffect.java:459-481 resolves "Else" when no number matched), the
// shape Sword of Dungeons & Dragons writes.
func TestRollDiceResultElsePairResolvesWhenNoKeyMatches(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		results string
		want    int
	}{
		{"2:DBOne,Else:DBTwo", 22},
		{"1:DBOne,Else:DBTwo", 21},
		{"2:DBOne", 20},
	} {
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
			"DB$ RollDice | Sides$ 1 | ResultSubAbilities$ "+tc.results,
			"DBOne", "DB$ GainLife | Defined$ You | LifeAmount$ 1",
			"DBTwo", "DB$ GainLife | Defined$ You | LifeAmount$ 2")
		if err != nil {
			t.Fatalf("%s: %v", tc.results, err)
		}
		if got := g.Player(p).Life; got != tc.want {
			t.Errorf("%s: life = %d, want %d", tc.results, got, tc.want)
		}
	}
}

// NoteDoubles$ binds Doubles to 1 on a double (RollDiceEffect.java:526-533);
// without one the SVar stays unset, which calculateAmount reads as 0
// (AbilityUtils.java:415-423), Celebr-8000's "if you rolled doubles".
func TestRollDiceNoteDoublesReadsZeroWithoutADouble(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		dice string
		want int
	}{
		{"2", 23},
		{"1", 20},
	} {
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
			"DB$ RollDice | Amount$ "+tc.dice+" | Sides$ 1 | NoteDoubles$ True | SubAbility$ DBDouble",
			"DBDouble", "DB$ GainLife | Defined$ You | LifeAmount$ 3 | ConditionCheckSVar$ Doubles")
		if err != nil {
			t.Fatalf("%s dice: %v", tc.dice, err)
		}
		if got := g.Player(p).Life; got != tc.want {
			t.Errorf("%s dice: life = %d, want %d", tc.dice, got, tc.want)
		}
	}
}

// RepeatCheckSVar$ is first read after the first repetition
// (RepeatEffect.java:39-52): an SVar the repeated chain stores before then is
// read as stored, even when its script value ("Number$ 1", with Sword of
// Dungeons & Dragons' space) would not parse.
func TestRepeatCheckSVarIsReadAfterTheFirstRepetition(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
		"DB$ Repeat | RepeatSubAbility$ DBStore | RepeatCheckSVar$ Check | RepeatSVarCompare$ GT0",
		"DBStore", "DB$ StoreSVar | SVar$ Check | Type$ Number | Expression$ 0 | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 1",
		"Check", "Number$ 1")
	if err != nil {
		t.Fatalf("Repeat: %v", err)
	}
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("life = %d, want 21: one repetition, then the stored 0 stops it", got)
	}
}

// Defined$ TargetedAndYou is the targeted players, then the activator, each
// once (AbilityUtils.java:970-975); nothing targeting a player names nobody.
func TestDefinedTargetedAndYou(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ GainLife | Defined$ TargetedAndYou | LifeAmount$ 1"); err != nil {
		t.Fatalf("targeting the opponent: %v", err)
	}
	if g.Player(p).Life != 21 || g.Player(other).Life != 21 {
		t.Errorf("life = %d/%d, want 21/21", g.Player(p).Life, g.Player(other).Life)
	}
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.PlayerEntity(p)},
		"DB$ GainLife | Defined$ TargetedAndYou | LifeAmount$ 1"); err != nil {
		t.Fatalf("targeting itself: %v", err)
	}
	if g.Player(p).Life != 22 {
		t.Errorf("life = %d, want 22: the activator gains once", g.Player(p).Life)
	}
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
		"DB$ GainLife | Defined$ TargetedAndYou | LifeAmount$ 1"); err != nil {
		t.Fatalf("no targets: %v", err)
	}
	if g.Player(p).Life != 22 || g.Player(other).Life != 21 {
		t.Errorf("life = %d/%d, want 22/21: no player targeted names nobody", g.Player(p).Life, g.Player(other).Life)
	}
}
