package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestChoosePlayerEffectChosenPlayerFeedsDefined proves ChoosePlayer's
// write/read pair: the pick is recorded on the host, and a SubAbility$
// naming Defined$ ChosenPlayer (defined.go) acts on that player alone.
func TestChoosePlayerEffectChosenPlayerFeedsDefined(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueuePlayerChoice(other)
	def := etbChainDef(t, "Test ChoosePlayer",
		"DB$ ChoosePlayer | Defined$ You | Choices$ Player.Opponent | SubAbility$ DBLose",
		"DBLose", "DB$ LoseLife | Defined$ ChosenPlayer | LifeAmount$ 3")
	host, err := castETBChain(t, g, p, def, c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 17 {
		t.Errorf("chosen player life = %d, want 17", got)
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("chooser life = %d, want 20", got)
	}
	if got := g.Card(host).Memory.ChosenPlayer(); got != other {
		t.Errorf("ChosenPlayer = %v, want %v", got, other)
	}
}

// TestChoosePlayerEffectRejectsUnofferedAnswer proves a player outside
// Choices$ is an error (GO-7).
func TestChoosePlayerEffectRejectsUnofferedAnswer(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueuePlayerChoice(p)
	def := etbChainDef(t, "Test ChoosePlayer Bad", "DB$ ChoosePlayer | Defined$ You | Choices$ Player.Opponent")
	if _, err := castETBChain(t, g, p, def, c); err == nil {
		t.Fatal("ResolveStack succeeded, want an error for a player not in Choices$")
	}
}

// TestChoosePlayerEffectRejectsUnresolvedParam proves Protect$ fails closed.
func TestChoosePlayerEffectRejectsUnresolvedParam(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	def := etbChainDef(t, "Test ChoosePlayer Protect", "DB$ ChoosePlayer | Defined$ You | Choices$ Player | Protect$ True")
	if _, err := castETBChain(t, g, p, def, c); err == nil {
		t.Fatal("ResolveStack succeeded, want an error for unresolved Protect$")
	}
}

// TestChoosePlayerEffectRememberChosenFeedsDefinedRemembered proves
// RememberChosen$ and definedPlayers' plain "Remembered" case.
func TestChoosePlayerEffectRememberChosenFeedsDefinedRemembered(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueuePlayerChoice(other)
	def := etbChainDef(t, "Test ChoosePlayer Remember",
		"DB$ ChoosePlayer | Defined$ You | Choices$ Player.Opponent | ForgetOtherRemembered$ True | RememberChosen$ True | SubAbility$ DBLose",
		"DBLose", "DB$ LoseLife | Defined$ Remembered | LifeAmount$ 1")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 19 {
		t.Errorf("remembered player life = %d, want 19", got)
	}
}

// TestChoosePlayerEffectBadChoicesFails proves an unresolvable Choices$
// value fails closed.
func TestChoosePlayerEffectBadChoicesFails(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	def := etbChainDef(t, "Test ChoosePlayer BadChoices", "DB$ ChoosePlayer | Defined$ You | Choices$ TriggeredPlayer")
	if _, err := castETBChain(t, g, p, def, c); err == nil {
		t.Fatal("ResolveStack succeeded, want an error for unresolvable Choices$")
	}
}
