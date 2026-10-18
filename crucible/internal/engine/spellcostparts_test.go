package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// The cost parts optional and triggered costs read are also a spell's own
// Cost$ parts and an UnlessCost$'s: each is paid, and what a spell reads back
// is recorded on it.

// Caller of the Hunt's Cost$ 2 G ChooseCreatureType<1>: the type is named as
// the spell is paid (CostChooseCreatureType.java:56-58) and its */* counts the
// creatures of that type.
func TestCallerOfTheHuntNamesItsTypeAsItIsCast(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	caller := g.NewCard(corpusCard(t, "Caller of the Hunt"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 3)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardG)
	c.QueuePayGeneric(mana.ShardG)
	c.QueueOptionNamed("Bear")
	if !g.CastSpell(p, caller, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got, _ := g.Card(caller).Power(); got != 2 {
		t.Errorf("Caller of the Hunt's power = %d, want 2 (two Bears)", got)
	}
}

// A creature type the payer cannot name -- an answer out of range, or a game
// with no type vocabulary to offer -- declines the payment and the cast.
func TestChooseCreatureTypeCostThatCannotBeNamedFailsTheCast(t *testing.T) {
	t.Parallel()

	t.Run("answer out of range", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		caller := g.NewCard(corpusCard(t, "Caller of the Hunt"), p, engine.Hand)
		g.Player(p).ManaPool.Add(mana.Green, 3)
		c := engine.NewScriptedController()
		c.QueuePayGeneric(mana.ShardG)
		c.QueuePayGeneric(mana.ShardG)
		c.QueueOption(1 << 20)
		if g.CastSpell(p, caller, c) {
			t.Fatal("CastSpell succeeded with no creature type named")
		}
	})
	t.Run("no type vocabulary", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		spell := scriptDef(t, "Test Named Type", "Creature",
			"A:SP$ PermanentCreature | Cost$ G ChooseCreatureType<1>")
		spell.Faces[0].ManaCost = mana.MustParse("G")
		card := g.NewCard(spell, p, engine.Hand)
		g.Player(p).ManaPool.Add(mana.Green, 1)
		if g.CastSpell(p, card, engine.NewScriptedController()) {
			t.Fatal("CastSpell succeeded with no creature type to name")
		}
	})
}

// A mana ability's SubAbility$ that fails leaves its error pending; the mana is
// already in the pool (resolveManaSubAbility).
func TestManaAbilitySubAbilityErrorIsPending(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	rock := g.NewCard(scriptDef(t, "Test Rock", "Artifact",
		"A:AB$ Mana | Cost$ T | Produced$ G | SubAbility$ DBBad",
		"SVar:DBBad:DB$ ImmediateTrigger | RememberSVarAmount$ X | Execute$ DBGain",
		"SVar:DBGain:DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	if !g.ActivateManaAbility(p, rock, 0, engine.NewScriptedController()) {
		t.Fatalf("ActivateManaAbility failed: %v", g.TakePendingError())
	}
	if g.Player(p).ManaPool.Total() != 1 {
		t.Errorf("mana in pool = %d, want 1", g.Player(p).ManaPool.Total())
	}
	if err := g.TakePendingError(); err == nil {
		t.Error("no pending error for the failing sub-ability")
	}
}

// Out of timing, a spell whose optional costs cannot be read is not offered
// through them (castsWithFlashOptional reads no option it cannot pay).
func TestUnreadableOptionalCostGivesNoFlash(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, other, engine.Main1)
	def := scriptDef(t, "Test Unread Optional", "Creature",
		"S:Mode$ OptionalCost | EffectZone$ All | ValidCard$ Card.Self | ValidSA$ Spell | Cost$ Mill<1> | Description$ You may mill a card.")
	def.Faces[0].ManaCost = mana.MustParse("G")
	card := g.NewCard(def, p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	if g.CastSpell(p, card, engine.NewScriptedController()) {
		t.Fatal("CastSpell succeeded out of timing")
	}
}

// Monstrous Emergence's Cost$ 1 G RevealOrChoose<1/Creature>: the chosen
// creature is the spell's Revealed list, and Revealed$CardPower its damage.
func TestMonstrousEmergenceDealsTheChosenCreaturesPower(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	dragon := g.NewCard(corpusCard(t, "Shivan Dragon"), p, engine.Battlefield)
	theirs := g.NewCard(corpusCard(t, "Colossal Dreadmaw"), other, engine.Battlefield)
	spell := g.NewCard(corpusCard(t, "Monstrous Emergence"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardG)
	c.QueueCardChoice([]engine.CardID{dragon})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
	if !g.CastSpell(p, spell, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(theirs).Damage.Marked; got != 5 {
		t.Errorf("damage = %d, want 5", got)
	}
}

// Processor Assault's Cost$ 1 R ExiledMoveToGrave<1/Card.OppOwn>: the exiled
// card goes to its owner's graveyard as the spell is cast.
func TestProcessorAssaultPutsAnExiledCardIntoItsGraveyard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	exiled := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Exile)
	theirs := g.NewCard(corpusCard(t, "Colossal Dreadmaw"), other, engine.Battlefield)
	spell := g.NewCard(corpusCard(t, "Processor Assault"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Red, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardR)
	c.QueueCardChoice([]engine.CardID{exiled})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
	if !g.CastSpell(p, spell, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	if g.Card(exiled).Zone != engine.Graveyard {
		t.Errorf("the exiled card is in %v, want its owner's graveyard", g.Card(exiled).Zone)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(theirs).Damage.Marked; got != 5 {
		t.Errorf("damage = %d, want 5", got)
	}
}

// Fallow Wurm's "sacrifice it unless you discard a land card": UnlessCost$
// Discard<1/Land> is paid with the land, and the Wurm stays.
func TestFallowWurmKeptByDiscardingALand(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		pay  bool
		want engine.ZoneType
	}{
		{"land discarded", true, engine.Battlefield},
		{"declined", false, engine.Graveyard},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			forest := g.NewCard(corpusCard(t, "Forest"), p, engine.Hand)
			wurm := g.NewCard(corpusCard(t, "Fallow Wurm"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Green, 3)
			c := engine.NewScriptedController()
			c.QueuePayGeneric(mana.ShardG)
			c.QueuePayGeneric(mana.ShardG)
			c.QueueConfirmPayCost(tc.pay)
			c.QueueDiscardChoice([]engine.CardID{forest})
			if !g.CastSpell(p, wurm, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Card(wurm).Zone; got != tc.want {
				t.Errorf("Fallow Wurm is in %v, want %v", got, tc.want)
			}
		})
	}
}
