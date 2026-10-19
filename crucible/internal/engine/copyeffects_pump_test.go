package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These tests cover Clone's PumpKeywords$/PumpDuration$ (CloneEffect.java:
// 153-156, TokenEffectBase.addPumpUntil), Embalm$/RemoveCost$
// (CardFactory.java:555, :613) and the ThisTurnEnteredFrom_<Zone> choice
// filter The Fourteenth Doctor writes.

// TestClonePumpKeywordsEndWithPumpDuration proves PumpKeywords$ with a
// PumpDuration$ (The Fourteenth Doctor): the copy gains haste, and the haste
// ends in the cleanup step.
func TestClonePumpKeywordsEndWithPumpDuration(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | PumpKeywords$ Haste & Menace | PumpDuration$ EOT")
	mustActivate(t, g, p, c, host, giant)

	h := g.Card(host)
	if !h.HasKeyword("Haste") || !h.HasKeyword("Menace") || !h.HasKeyword("Reach") {
		t.Errorf("keywords = %v, want the copied Reach plus the pumped Haste and Menace", h.KeywordLines())
	}
	advanceToCleanup(g, c)
	h = g.Card(host)
	if h.HasKeyword("Haste") || h.HasKeyword("Menace") {
		t.Errorf("keywords = %v after cleanup, want the pump gone", h.KeywordLines())
	}
	if h.Def.Name != "Copied Giant" {
		t.Errorf("host is %q after cleanup, want the permanent copy to stay", h.Def.Name)
	}
}

// TestClonePumpKeywordsWithoutDurationStayPastTheCopy pins a Java quirk
// (PORT-7): Loose in the Park writes Duration$ UntilEndOfTurn and
// PumpKeywords$ Haste but no PumpDuration$, and addPumpUntil registers no
// removal without one, so the haste outlives the copy. The script is a Forge
// bug against the Oracle text (card-script-defects.md, PORT-8).
func TestClonePumpKeywordsWithoutDurationStayPastTheCopy(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | Duration$ UntilEndOfTurn | PumpKeywords$ Haste")
	mustActivate(t, g, p, c, host, giant)
	if !g.Card(host).HasKeyword("Haste") {
		t.Fatal("the copy did not gain haste")
	}
	advanceToCleanup(g, c)
	h := g.Card(host)
	if h.IsCopy() || !h.HasKeyword("Haste") {
		t.Errorf("copy=%v haste=%v after cleanup, want the copy over and the haste kept", h.IsCopy(), h.HasKeyword("Haste"))
	}
}

// TestClonePumpDurationUntilYourNextTurnIsRefused proves the one
// PumpDuration$ this port cannot end fails the line before any copy is made
// (GO-7).
func TestClonePumpDurationUntilYourNextTurnIsRefused(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | PumpKeywords$ Haste | PumpDuration$ UntilYourNextTurn")
	err := activate(g, p, engine.NewScriptedController(), host, giant)
	if err == nil || !strings.Contains(err.Error(), "PumpDuration$") {
		t.Fatalf("err = %v, want a PumpDuration$ refusal", err)
	}
	if g.Card(host).IsCopy() {
		t.Error("a refused line still made the copy")
	}
}

// TestCloneChoiceFiltersByThisTurnEnteredFrom proves the Fourteenth Doctor
// shape: the choice lists only the cards in the graveyard that came from the
// library this turn, and a card seated there directly (no zone change) or
// one that came from the hand is not offered.
func TestCloneChoiceFiltersByThisTurnEnteredFrom(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	milled := g.NewCard(copyTestDef(t, "Milled Elf", "Creature Elf", "2", "2"), p, engine.Library)
	g.Move(milled, engine.Graveyard, p)
	discarded := g.NewCard(copyTestDef(t, "Discarded Elf", "Creature Elf", "3", "3"), p, engine.Hand)
	g.Move(discarded, engine.Graveyard, p)
	seated := g.NewCard(copyTestDef(t, "Seated Elf", "Creature Elf", "4", "4"), p, engine.Graveyard)

	sc := engine.NewScriptedController()
	sc.QueueCardChoice([]engine.CardID{milled})
	rec := &offerRecorder{ScriptedController: sc}
	host := shifter(t, g, p, "Clone | Choices$ Elf.YouOwn+ThisTurnEnteredFrom_Library | ChoiceZone$ Graveyard")
	mustActivate(t, g, p, rec, host)

	if len(rec.offers) != 1 || len(rec.offers[0]) != 1 || rec.offers[0][0] != milled {
		t.Fatalf("offered %v, want only the milled Elf %d (not %d, %d)", rec.offers, milled, discarded, seated)
	}
	if g.Card(host).Def.Name != "Milled Elf" {
		t.Errorf("host is %q, want a copy of Milled Elf", g.Card(host).Def.Name)
	}
}

// TestCloneRemoveCostKeepsTheColor proves RemoveCost$: the copy has no mana
// cost, so no mana value, but stays the color its cost gave it
// (a state's color is stored apart from its cost).
func TestCloneRemoveCostKeepsTheColor(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | RemoveCost$ True")
	mustActivate(t, g, p, c, host, giant)

	h := g.Card(host)
	if h.CMC() != 0 || !h.Colors().Has(mana.Green) {
		t.Errorf("CMC=%d colors=%v, want no cost and the green it had", h.CMC(), h.Colors())
	}
	if !h.Def.Faces[0].ManaCost.IsNoCost() {
		t.Errorf("mana cost = %v, want none", h.Def.Faces[0].ManaCost)
	}
	if !g.Card(giant).Def.Faces[0].ManaCost.Equal(mana.MustParse("2 G")) {
		t.Error("RemoveCost$ wrote through into the copied card's own definition")
	}
}

// TestCloneEmbalmGateSkipsEveryExceptForAHandCard proves Embalm$
// (Vizier of Many Faces): a card that is not an embalmed token takes the
// plain copy, none of the excepts the line lists. Embalm is not ported, so
// no card is ever embalmed.
func TestCloneEmbalmGateSkipsEveryExceptForAHandCard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p,
		"Clone | ValidTgts$ Creature | Embalm$ True | RemoveCost$ True | SetColor$ White | AddTypes$ Zombie | NewName$ Renamed")
	mustActivate(t, g, p, c, host, giant)

	h := g.Card(host)
	if h.Def.Name != "Copied Giant" || h.Type().HasSubtype("Zombie") || h.Colors() != mana.Green || h.CMC() != 3 {
		t.Errorf("host is %q %v colors %v CMC %d, want the plain green copy at 3", h.Def.Name, h.Type(), h.Colors(), h.CMC())
	}
}
