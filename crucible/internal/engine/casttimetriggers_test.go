package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// A trigger a spell's cost fires waits until the spell is on the stack and
// goes above it: Java freezes the stack while the cost is paid
// (PlaySpellAbility.java:682) and runs the waiting triggers once the spell is
// added (TriggerHandler.java:264, MagicStack.add), so the dies trigger of a
// creature sacrificed to cast a spell resolves before the spell.

// fodderWithDiesDraw is a 1/1 whose dies trigger draws its controller a card.
func fodderWithDiesDraw(t *testing.T) []string {
	t.Helper()
	return []string{
		"T:Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Card.Self | Execute$ TrigDraw",
		"SVar:TrigDraw:DB$ Draw | Defined$ You | NumCards$ 1",
	}
}

// Emerge: the creature sacrificed for the emerge cost dies while Adipose
// Offspring is cast; its dies trigger is on top of the spell.
func TestEmergeSacrificeDiesTriggerGoesAboveTheSpell(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Library)
	fodder := g.NewCard(scriptDef(t, "Dying Fodder", "Creature Elf", fodderWithDiesDraw(t)...), p, engine.Battlefield)
	offspring := g.NewCard(corpusCard(t, "Adipose Offspring"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 6)
	c := engine.NewScriptedController()
	c.QueueOption(1)
	c.QueueSacrificeChoice([]engine.CardID{fodder})
	for range 5 {
		c.QueuePayGeneric(mana.ShardW)
	}
	if !g.CastSpell(p, offspring, c) {
		t.Fatal("could not emerge Adipose Offspring")
	}
	if got := g.StackLen(); got != 2 {
		t.Fatalf("stack holds %d items, want the spell and the dies trigger", got)
	}
	top, _ := g.StackTop()
	if top.Source != fodder {
		t.Errorf("top of the stack is from card %d, want the sacrificed creature's dies trigger (%d) above the spell", top.Source, fodder)
	}
}

// Bargain: the artifact creature sacrificed for the optional cost dies while
// the spell is cast; its dies trigger goes above the spell.
func TestBargainSacrificeDiesTriggerGoesAboveTheSpell(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	def := spellDefWith(t, "Test Bargain Gain", "Sorcery", "G", "SP$ GainLife | Defined$ You | LifeAmount$ 1")
	def.Faces[0].Keywords = append(def.Faces[0].Keywords, "Bargain")
	spell := g.NewCard(def, p, engine.Hand)
	fodder := g.NewCard(scriptDef(t, "Dying Golem", "Artifact Creature Golem", fodderWithDiesDraw(t)...), p, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	c := engine.NewScriptedController()
	c.QueueConfirmPayCost(true)
	c.QueueSacrificeChoice([]engine.CardID{fodder})
	if !g.CastSpell(p, spell, c) {
		t.Fatal("could not cast the bargain spell")
	}
	top, _ := g.StackTop()
	if g.StackLen() != 2 || top.Source != fodder {
		t.Errorf("stack len %d, top from card %d; want the dies trigger of %d above the spell", g.StackLen(), top.Source, fodder)
	}
}

// A trigger hosted by the spell itself (Cascade's "when you cast this
// spell") leaves its card on the stack when it resolves: the card is the
// spell still waiting there, and goes nowhere until it resolves itself.
func TestCastTriggerDoesNotMoveItsSpellOffTheStack(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(scriptDef(t, "Graveyard Watcher", "Enchantment",
		"T:Mode$ ChangesZone | Destination$ Graveyard | ValidCard$ Creature.YouOwn | TriggerZones$ Battlefield | Execute$ TrigGain",
		"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	elf := g.NewCard(corpusCard(t, "Bloodbraid Elf"), p, engine.Hand)
	g.NewCard(corpusCard(t, "Forest"), p, engine.Library)
	g.Player(p).ManaPool.Add(mana.Red, 2)
	g.Player(p).ManaPool.Add(mana.Green, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardR)
	c.QueuePayGeneric(mana.ShardG)
	if !g.CastSpell(p, elf, c) {
		t.Fatal("could not cast Bloodbraid Elf")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(elf).Zone; got != engine.Battlefield {
		t.Errorf("Bloodbraid Elf is in %v, want the battlefield", got)
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("life = %d, want 20: the Elf never went to the graveyard", got)
	}
}

// An additional cost (Bone Splinters' Sac<1/Creature>): the same order.
func TestAdditionalCostSacrificeDiesTriggerGoesAboveTheSpell(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Library)
	fodder := g.NewCard(scriptDef(t, "Dying Fodder", "Creature Elf", fodderWithDiesDraw(t)...), p, engine.Battlefield)
	victim := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Battlefield)
	splinters := g.NewCard(corpusCard(t, "Bone Splinters"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Black, 1)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	c.QueueSacrificeChoice([]engine.CardID{fodder})
	if !g.CastSpell(p, splinters, c) {
		t.Fatal("could not cast Bone Splinters")
	}
	top, _ := g.StackTop()
	if g.StackLen() != 2 || top.Source != fodder {
		t.Fatalf("stack len %d, top from card %d; want the dies trigger of %d above the spell", g.StackLen(), top.Source, fodder)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := len(g.Zone(engine.Hand, p).Cards()); got != 1 {
		t.Errorf("hand holds %d cards, want the dies trigger's draw", got)
	}
	if got := g.Card(victim).Zone; got != engine.Graveyard {
		t.Errorf("the target is in %v, want destroyed", got)
	}
}
