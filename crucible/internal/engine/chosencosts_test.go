package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Sac<1/Creature> (Bloodthrone Vampire): the controller picks the creature to
// sacrifice before anything is paid; the ability then resolves.
func TestSacrificeATypeToActivate(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	vampire := g.NewCard(corpusCard(t, "Bloodthrone Vampire"), p, engine.Battlefield)
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueSacrificeChoice([]engine.CardID{bears})
	if !g.ActivateAbility(p, vampire, 0, c) {
		t.Fatal("could not activate with a creature to sacrifice")
	}
	if got := g.Card(bears).Zone; got != engine.Graveyard {
		t.Errorf("the sacrificed creature is in %v, want Graveyard", got)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := powerOf(t, g, vampire); got != 3 {
		t.Errorf("Vampire power = %d, want 3 after +2/+2", got)
	}
}

// A pick that is not one of the candidates pays nothing and refuses the
// activation.
func TestChosenCostRefusesAnIllegalPick(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	vampire := g.NewCard(corpusCard(t, "Bloodthrone Vampire"), p, engine.Battlefield)
	theirs := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueSacrificeChoice([]engine.CardID{theirs})
	if g.ActivateAbility(p, vampire, 0, c) {
		t.Fatal("activated by sacrificing the opponent's creature")
	}
	if got := g.Card(theirs).Zone; got != engine.Battlefield {
		t.Errorf("the opponent's creature is in %v, want Battlefield", got)
	}
}

// A mana ability with a chosen sacrifice (Phyrexian Altar).
func TestManaAbilitySacrificesAChosenCreature(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	altar := g.NewCard(corpusCard(t, "Phyrexian Altar"), p, engine.Battlefield)
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueSacrificeChoice([]engine.CardID{bears})
	c.QueueManaColor(mana.Red)
	if !g.ActivateManaAbility(p, altar, 0, c) {
		t.Fatal("could not activate the Altar")
	}
	if got := g.Card(bears).Zone; got != engine.Graveyard {
		t.Errorf("the sacrificed creature is in %v, want Graveyard", got)
	}
	if got := g.Player(p).ManaPool.Total(); got != 1 {
		t.Errorf("mana in pool = %d, want 1", got)
	}
}

// Discard<1/Land> (Borborygmos Enraged): the discard must be a land.
func TestDiscardATypeToActivate(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.Player(other).Life = 20
	borb := g.NewCard(corpusCard(t, "Borborygmos Enraged"), p, engine.Battlefield)
	spell := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	c := engine.NewScriptedController()
	if g.ActivateAbility(p, borb, 0, c) {
		t.Fatal("activated with no land in hand")
	}
	land := g.NewCard(corpusCard(t, "Forest"), p, engine.Hand)
	c.QueueDiscardChoice([]engine.CardID{land})
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if !g.ActivateAbility(p, borb, 0, c) {
		t.Fatal("could not activate with a land to discard")
	}
	if got := g.Card(land).Zone; got != engine.Graveyard {
		t.Errorf("the discarded land is in %v, want Graveyard", got)
	}
	if got := g.Card(spell).Zone; got != engine.Hand {
		t.Errorf("the spell is in %v, want Hand: it was not the card chosen", got)
	}
}

// ExileFromGrave<1/Creature> (Balduvian Dead): a creature card in the
// graveyard is exiled as the cost.
func TestExileATypeFromYourGraveyardToActivate(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	dead := g.NewCard(corpusCard(t, "Balduvian Dead"), p, engine.Battlefield)
	bolt := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Graveyard)
	creature := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Graveyard)
	g.Player(p).ManaPool.Add(mana.Red, 3)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{bolt})
	queueXPayGeneric(c, mana.ShardR, 3)
	if g.ActivateAbility(p, dead, 0, c) {
		t.Fatal("activated by exiling a card that is not a creature")
	}
	c.QueueCardChoice([]engine.CardID{creature})
	if !g.ActivateAbility(p, dead, 0, c) {
		t.Fatal("could not activate with a creature card to exile")
	}
	if got := g.Card(creature).Zone; got != engine.Exile {
		t.Errorf("the creature card is in %v, want Exile", got)
	}
}

// Sacrificed$CardPower (Bloodshot Cyclops): the sacrificed creature's power,
// read from its last-known information once it is gone.
func TestSacrificedAmountIsTheSacrificedCreaturesPower(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.Player(other).Life = 20
	cyclops := g.NewCard(corpusCard(t, "Bloodshot Cyclops"), p, engine.Battlefield)
	g.Card(cyclops).SummonSick = false
	fodder := g.NewCard(creatureDefPT(t, "5", "5"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueSacrificeChoice([]engine.CardID{fodder})
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if !g.ActivateAbility(p, cyclops, 0, c) {
		t.Fatal("could not activate the Cyclops")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 15 {
		t.Errorf("opponent life = %d, want 15: damage equal to the sacrificed 5/5's power", got)
	}
}

// A cost that sacrifices the source itself records it too: Sacrificed$ reads
// the card's last-known power.
func TestSelfSacrificeCostIsRecordedForSacrificedAmount(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.Player(other).Life = 20
	raw := &carddb.Card{Filename: "self-flinger"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Self Flinger"
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "3", "3"
	raw.Faces[0].Abilities = []string{"AB$ DealDamage | Cost$ Sac<1/CARDNAME> | ValidTgts$ Player | NumDmg$ X"}
	raw.Faces[0].SVars.Set("X", "Sacrificed$CardPower")
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	flinger := g.NewCard(def, p, engine.Battlefield)
	g.Card(flinger).SummonSick = false
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if !g.ActivateAbility(p, flinger, 0, c) {
		t.Fatal("could not activate")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 17 {
		t.Errorf("opponent life = %d, want 17: the sacrificed 3/3's power", got)
	}
}

// flingerDef is an instant dealing X damage to a target player, X its own
// Sacrificed$ amount, with cost as its A:SP$ Cost$ (empty for none).
func flingerDef(t *testing.T, cost, x string) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: "test-flinger"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Test Flinger"
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Instant")
	line := "SP$ DealDamage | ValidTgts$ Player | NumDmg$ X"
	if cost != "" {
		raw.Faces[0].ManaCost = mana.MustParse("R")
		line = "SP$ DealDamage | Cost$ " + cost + " | ValidTgts$ Player | NumDmg$ X"
	}
	raw.Faces[0].Abilities = []string{line}
	raw.Faces[0].SVars.Set("X", x)
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	return def
}

// A spell whose cost sacrificed nothing has Java's null paid list
// (SpellAbility.getPaidList): the amount reads 0 and the operator is not
// applied (AbilityUtils.java:715-729).
func TestSpellPaidListWithNothingSacrificedReadsZero(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.Player(other).Life = 20
	spell := g.NewCard(flingerDef(t, "", "Sacrificed$Amount/Plus.3"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if !g.CastSpell(p, spell, c) {
		t.Fatal("cast failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("opponent life = %d, want 20: nothing was sacrificed", got)
	}
}

// Fling's shape: the creature a spell's additional cost sacrificed is that
// spell's Sacrificed$ list, its power read from last-known information.
func TestSpellAdditionalCostSacrificeIsItsPaidList(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.Player(other).Life = 20
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	spell := g.NewCard(flingerDef(t, "R Sac<1/Creature>", "Sacrificed$CardPower/Plus.1"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Red, 1)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueSacrificeChoice([]engine.CardID{bears})
	if !g.CastSpell(p, spell, c) {
		t.Fatal("cast failed")
	}
	if z := g.Card(bears).Zone; z != engine.Graveyard {
		t.Fatalf("Bears in %v, want Graveyard: the cost sacrifices it", z)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 17 {
		t.Errorf("opponent life = %d, want 17: the Bears' power 2, plus 1", got)
	}
}
