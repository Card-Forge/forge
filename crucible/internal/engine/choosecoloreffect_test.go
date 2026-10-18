package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// TestChooseColorEffectRecordsChosenColor proves the dominant shape (112 of
// 122 real lines name Defined$ alone): one color, recorded on the host.
func TestChooseColorEffectRecordsChosenColor(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueColorChoice(mana.Red)
	host, err := castETBChain(t, g, p, etbChainDef(t, "Test ChooseColor", "DB$ ChooseColor | Defined$ You"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(host).Memory.ChosenColors(); got != mana.Red {
		t.Errorf("ChosenColors = %v, want R", got)
	}
}

// TestChooseColorEffectTwoColors proves TwoColors$ asks for exactly two.
func TestChooseColorEffectTwoColors(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueColorChoice(mana.White | mana.Blue)
	host, err := castETBChain(t, g, p, etbChainDef(t, "Test ChooseColor Two", "DB$ ChooseColor | Defined$ You | TwoColors$ True"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(host).Memory.ChosenColors(); got != mana.White|mana.Blue {
		t.Errorf("ChosenColors = %v, want WU", got)
	}
}

// TestChooseColorEffectRejectsExcludedColor proves Exclude$ removes a color
// from the offer, so choosing it is an error (GO-7).
func TestChooseColorEffectRejectsExcludedColor(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueColorChoice(mana.White)
	def := etbChainDef(t, "Test ChooseColor Exclude", "DB$ ChooseColor | Defined$ You | Exclude$ white")
	if _, err := castETBChain(t, g, p, def, c); err == nil {
		t.Fatal("ResolveStack succeeded, want an error for an excluded color")
	}
}

// TestChooseColorEffectRejectsRandomWithChoices proves Random$ or Exclude$
// beside Choices$ fails closed: Java's Arrays.asList list cannot shrink.
func TestChooseColorEffectRejectsRandomWithChoices(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"DB$ ChooseColor | Defined$ You | Random$ True | Choices$ white,blue",
		"DB$ ChooseColor | Defined$ You | Choices$ white,blue | Exclude$ white",
	} {
		g, p, _ := newTwoPlayerGame(t)
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Test ChooseColor Choices", line), engine.NewScriptedController()); err == nil {
			t.Errorf("%q: ResolveStack succeeded, want an error", line)
		}
	}
}

// TestChooseColorEffectChoicesOrColors proves Choices$ restricts the offer
// and OrColors$ allows more than one of it.
func TestChooseColorEffectChoicesOrColors(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueColorChoice(mana.Red | mana.Green)
	host, err := castETBChain(t, g, p, etbChainDef(t, "Test ChooseColor Or", "DB$ ChooseColor | Defined$ You | Choices$ red,Green | OrColors$ True"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(host).Memory.ChosenColors(); got != mana.Red|mana.Green {
		t.Errorf("ChosenColors = %v, want RG", got)
	}
}

// TestChooseColorEffectUpToNoneRecordsNothing proves UpTo$ allows zero,
// and a colorless answer ends the effect without recording.
func TestChooseColorEffectUpToNoneRecordsNothing(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueColorChoice(0)
	host, err := castETBChain(t, g, p, etbChainDef(t, "Test ChooseColor UpTo", "DB$ ChooseColor | Defined$ You | UpTo$ True"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(host).Memory.ChosenColors(); got != 0 {
		t.Errorf("ChosenColors = %v, want none", got)
	}
}

// TestChooseColorEffectRejectsUnknownColorName proves Choices$/Exclude$
// with a name outside MagicColor's five fail closed.
func TestChooseColorEffectRejectsUnknownColorName(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"DB$ ChooseColor | Defined$ You | Choices$ purple",
		"DB$ ChooseColor | Defined$ You | Exclude$ purple",
	} {
		g, p, _ := newTwoPlayerGame(t)
		c := engine.NewScriptedController()
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Test ChooseColor Name", line), c); err == nil {
			t.Errorf("%q: ResolveStack succeeded, want an error", line)
		}
	}
}
