package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// "As CARDNAME enters, choose ..." (K:ETBReplacement:Other:<SVar>) runs its
// chain on the entering card before it lands (ADR-0022,
// CardFactoryUtil.java:515-543): the choice is on the permanent, made once.

// castEntrant casts card for p on c and resolves the stack.
func castEntrant(t *testing.T, g *engine.Game, p engine.PlayerID, card engine.CardID, c *engine.ScriptedController) error {
	t.Helper()
	if !g.CastSpell(p, card, c) {
		t.Fatalf("could not cast %q", g.Card(card).Def.Name)
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

// Iona, Shield of Emeria: the color is chosen once, as it enters, and kept on
// the permanent.
func TestAsEntersChooseColorIsOnThePermanent(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	iona := g.NewCard(corpusCard(t, "Iona, Shield of Emeria"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 9)
	c := engine.NewScriptedController()
	for range 6 {
		c.QueuePayGeneric(mana.ShardW)
	}
	c.QueueColorChoice(mana.Red)
	if err := castEntrant(t, g, p, iona, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(iona).Zone; got != engine.Battlefield {
		t.Fatalf("Iona is in %v, want the battlefield", got)
	}
	if got := g.Card(iona).Memory.ChosenColors(); got != mana.Red {
		t.Errorf("chosen colors = %v, want red", got)
	}
}

// The chosen number is there when the card's own ETB trigger resolves.
func TestAsEntersChoiceIsMadeBeforeTheEnterTrigger(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	def := scriptDef(t, "Number Keeper", "Creature Elf",
		"K:ETBReplacement:Other:ChooseNum",
		"SVar:ChooseNum:DB$ ChooseNumber | Defined$ You | Max$ 5",
		"T:Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigGain",
		"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ X",
		"SVar:X:Count$ChosenNumber")
	keeper := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueNumberChoice(4)
	if err := castEntrant(t, g, p, keeper, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life = %d, want 24 (the chosen 4 gained)", got)
	}
}

// A chain of effects on other objects runs before the move too: "as this
// enters, mill two".
func TestAsEntersMillRunsBeforeTheMove(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	for range 3 {
		g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Library)
	}
	def := scriptDef(t, "Mill Entrant", "Creature Elf",
		"K:ETBReplacement:Other:DBMill",
		"SVar:DBMill:DB$ Mill | NumCards$ 2 | Defined$ You")
	entrant := g.NewCard(def, p, engine.Hand)
	if err := castEntrant(t, g, p, entrant, engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := len(g.Zone(engine.Graveyard, p).Cards()); got != 2 {
		t.Errorf("graveyard holds %d cards, want 2 milled", got)
	}
}

// An as-enters chain no entry path runs is refused, not skipped (GO-7): an
// AlterAttribute would write onto the card that Game.Move then rebuilds.
func TestAsEntersUnresolvableChainIsAnError(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	def := scriptDef(t, "Prepared Entrant", "Creature Elf",
		"K:ETBReplacement:Other:DBPrepare",
		"SVar:DBPrepare:DB$ AlterAttribute | Attributes$ Prepared")
	entrant := g.NewCard(def, p, engine.Hand)
	err := castEntrant(t, g, p, entrant, engine.NewScriptedController())
	if err == nil || !strings.Contains(err.Error(), "as-it-enters replacement ReplaceWith$ AlterAttribute") {
		t.Errorf("err = %v, want the AlterAttribute chain refused", err)
	}
}

// Phylactery Lich: the chosen artifact gets its phylactery counter as the
// Lich enters (a PutCounter onto Defined$ ChosenCard runs before the move).
func TestAsEntersChosenCardGetsItsCounter(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	ring := g.NewCard(corpusCard(t, "Sol Ring"), p, engine.Battlefield)
	lich := g.NewCard(corpusCard(t, "Phylactery Lich"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Black, 3)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{ring})
	if err := castEntrant(t, g, p, lich, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(ring).Counters.Count(engine.CounterType("PHYLACTERY")); got != 1 {
		t.Errorf("Sol Ring has %d phylactery counters, want 1", got)
	}
	if got := g.Card(lich).Zone; got != engine.Battlefield {
		t.Errorf("Phylactery Lich is in %v, want the battlefield", got)
	}
}

// A choice chain that goes on to change the entering card itself (an
// Animate) is refused whole; a DelayedTrigger in the chain is not walked into
// and runs before the move.
func TestAsEntersChainShapes(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	animated := g.NewCard(scriptDef(t, "Animated Entrant", "Creature Elf",
		"K:ETBReplacement:Other:ChooseColor",
		"SVar:ChooseColor:DB$ ChooseColor | Defined$ You | SubAbility$ DBAnimate",
		"SVar:DBAnimate:DB$ Animate | Defined$ Self | Power$ 5 | Duration$ Permanent"), p, engine.Hand)
	err := castEntrant(t, g, p, animated, engine.NewScriptedController())
	if err == nil || !strings.Contains(err.Error(), "as-it-enters replacement ReplaceWith$ ChooseColor") {
		t.Errorf("err = %v, want the Animate chain refused", err)
	}

	g2, p2, other2 := newTwoPlayerGameOn(t, scenarioDB(t))
	victim := g2.NewCard(corpusCard(t, "Grizzly Bears"), other2, engine.Battlefield)
	delayed := g2.NewCard(scriptDef(t, "Delayed Entrant", "Creature Elf",
		"K:ETBReplacement:Other:ChooseC",
		"SVar:ChooseC:DB$ ChooseCard | Defined$ You | Choices$ Creature.YouDontCtrl | Mandatory$ True | SubAbility$ DBDelay",
		"SVar:DBDelay:DB$ DelayedTrigger | Mode$ ChangesZone | RememberObjects$ ChosenCard | ValidCard$ Card.IsTriggerRemembered | Origin$ Battlefield | Destination$ Graveyard | Execute$ TrigGain",
		"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 3"), p2, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{victim})
	if err := castEntrant(t, g2, p2, delayed, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g2.Card(delayed).Memory.Chosen(); len(got) != 1 || got[0] != victim {
		t.Errorf("chosen = %v, want the opposing Grizzly Bears", got)
	}
}

// Rainbow Knights' ChooseColor carries Random$, which ChooseColor does not
// resolve: the entry is refused rather than the protection silently missing.
func TestRainbowKnightsRandomColorIsRefused(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	knights := g.NewCard(corpusCard(t, "Rainbow Knights"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 2)
	err := castEntrant(t, g, p, knights, engine.NewScriptedController())
	if err == nil || !strings.Contains(err.Error(), "Random$") {
		t.Errorf("err = %v, want ChooseColor's Random$ refused", err)
	}
}
