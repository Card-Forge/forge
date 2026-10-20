package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// AtEOTTrig$ (SpellAbilityEffect.addSelfTrigger, :498-526): the copy gets
// "at the beginning of the end step, sacrifice it"; AddKeywords$ is an
// "except" of the copy (CardFactory.getCloneStates).
func TestCopyPermanentAtEOTTrigSacrificesTheHastyCopy(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	target := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	copier := g.NewCard(scriptDef(t, "Test Kindle", "Artifact",
		"A:AB$ CopyPermanent | Cost$ 0 | ValidTgts$ Creature.YouCtrl | AddKeywords$ Haste | AtEOTTrig$ Sacrifice | RememberTokens$ True"),
		p, engine.Battlefield)
	c := engine.NewScriptedController()
	mustActivate(t, g, p, c, copier, target)
	made, _ := g.Card(copier).Memory.Remembered()[0].AsCard()
	if !g.Card(made).HasKeyword("Haste") {
		t.Error("the copy lacks the except's Haste")
	}
	if g.Card(target).HasKeyword("Haste") {
		t.Error("the original gained Haste: the except wrote through the shared definition")
	}
	advanceToPhase(t, g, c, 1, engine.Cleanup)
	if z := g.Card(made).Zone; z == engine.Battlefield {
		t.Errorf("the copy is still on the battlefield after the end step")
	}
	if z := g.Card(target).Zone; z != engine.Battlefield {
		t.Errorf("the original is in %v, want Battlefield", z)
	}
}

// Effect's AtEOT$ acts on the host (EffectEffect.java:312-314), Pump's even
// when the line pumps nothing (PumpEffect.java:485), and AtEOTCondition$ gates
// the body on an SVar of the host (SpellAbilityEffect.java:486-489).
func TestAtEOTReachesTheHostFromEffectAndEmptyPump(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, line string
		svars      []string
		gone       bool
	}{
		{"effect", "DB$ Effect | StaticAbilities$ Unblockable | AtEOT$ Sacrifice", []string{"Unblockable", "Mode$ CantBlockBy | ValidAttacker$ Creature.IsRemembered"}, true},
		{"empty pump", "DB$ Pump | Defined$ Self | NumAtt$ 0 | AtEOT$ Sacrifice", nil, true},
		{"condition holds", "DB$ Pump | Defined$ Self | NumAtt$ 1 | AtEOT$ Sacrifice | AtEOTCondition$ Y", []string{"Y", "Number$1"}, true},
		{"condition fails", "DB$ Pump | Defined$ Self | NumAtt$ 1 | AtEOT$ Sacrifice | AtEOTCondition$ Y", []string{"Y", "Number$0"}, false},
	} {
		g, p, _ := newTwoPlayerGame(t)
		c := engine.NewScriptedController()
		host, err := resolveNow(t, g, p, c, nil, tc.line, tc.svars...)
		if err != nil {
			t.Fatalf("%s: %v", tc.name, err)
		}
		advanceToPhase(t, g, c, 1, engine.Cleanup)
		if gone := g.Card(host).Zone != engine.Battlefield; gone != tc.gone {
			t.Errorf("%s: host left the battlefield = %v, want %v", tc.name, gone, tc.gone)
		}
	}
}

// Populate$ (CopyPermanentEffect.java:312-316) copies a creature token its
// controller picks; a non-token creature is not offered.
func TestCopyPermanentPopulateCopiesAChosenCreatureToken(t *testing.T) {
	t.Parallel()

	g, p, _ := newTokenGame(t)
	g.NewCard(creatureDefPT(t, "4", "4"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	src := resolveLine(t, g, p, c,
		"DB$ Token | TokenScript$ w_1_1_soldier | TokenPower$ 5 | TokenToughness$ 5 | RememberTokens$ True")
	tok, _ := g.Card(src).Memory.Remembered()[0].AsCard()
	c.QueueCardChoice([]engine.CardID{tok})
	made := resolveLine(t, g, p, c, "DB$ CopyPermanent | Populate$ True | RememberTokens$ True")
	copyID, ok := g.Card(made).Memory.Remembered()[0].AsCard()
	if !ok || copyID == tok || !g.Card(copyID).IsToken {
		t.Fatalf("populate made %v (ok %v), want a new token", copyID, ok)
	}
}
