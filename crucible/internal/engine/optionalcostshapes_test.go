package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// Dragon's Fire's RevealOrChoose<1/Dragon> (a CostReveal over the hand and the
// battlefield, Cost.java:522-526): the damage is the revealed or chosen
// Dragon's power through Count$OptionalGenericCostPaid.X.3, X Revealed$CardPower
// -- the power as it was paid (the Revealed list holds LKI copies,
// CostPartWithList.java:136-138), so +1/+1 counters added after the cast do
// not count. Declined, it deals 3.
func TestDragonsFireRevealsOrChoosesADragon(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name   string
		zone   engine.ZoneType
		pay    bool
		damage int
	}{
		{"chosen on the battlefield", engine.Battlefield, true, 5},
		{"revealed from hand", engine.Hand, true, 5},
		{"declined", engine.Battlefield, false, 3},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			dragon := g.NewCard(corpusCard(t, "Shivan Dragon"), p, tc.zone)
			theirs := g.NewCard(corpusCard(t, "Colossal Dreadmaw"), other, engine.Battlefield)
			fire := g.NewCard(corpusCard(t, "Dragon's Fire"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Red, 2)
			c := engine.NewScriptedController()
			c.QueueConfirmPayCost(tc.pay)
			c.QueuePayGeneric(mana.ShardR)
			c.QueueCardChoice([]engine.CardID{dragon})
			c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
			if !g.CastSpell(p, fire, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			if tc.zone == engine.Battlefield {
				g.Card(dragon).Counters.Add(engine.P1P1, 2)
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Card(theirs).Damage.Marked; got != tc.damage {
				t.Errorf("damage = %d, want %d", got, tc.damage)
			}
		})
	}
}

// Celestial Reunion's ChooseCreatureType<1> Behold<2/Creature.ChosenType>: it is
// offered when two of the caster's cards share a creature type (CostBehold.java:
// 22-30); the type is chosen (CostChooseCreatureType.java:56-58), two cards of
// it beheld, and a found creature of that type enters the battlefield instead
// of the hand. Declined, it goes to the hand.
func TestCelestialReunionChoosesATypeAndBeholds(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		pay  bool
		want engine.ZoneType
	}{
		{"paid", true, engine.Battlefield},
		{"declined", false, engine.Hand},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			bears := []engine.CardID{
				g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield),
				g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield),
			}
			found := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Library)
			reunion := g.NewCard(corpusCard(t, "Celestial Reunion"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Green, 3)
			c := engine.NewScriptedController()
			c.QueueConfirmPayCost(tc.pay)
			c.QueuePayX(2)
			c.QueuePayGeneric(mana.ShardG)
			c.QueuePayGeneric(mana.ShardG)
			if tc.pay {
				c.QueueOptionNamed("Bear")
				c.QueueCardChoice(bears)
			}
			c.QueueCardChoice([]engine.CardID{found})
			if !g.CastSpell(p, reunion, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Card(found).Zone; got != tc.want {
				t.Errorf("the found creature is in %v, want %v", got, tc.want)
			}
		})
	}
}

// With no two cards sharing a creature type, Celestial Reunion's optional cost
// is not offered (an empty ConfirmPayCost queue would panic), and choosing a
// type with too few cards declines the payment and the cast.
func TestCelestialReunionBeholdNeedsSharedType(t *testing.T) {
	t.Parallel()

	t.Run("not offered", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
		g.NewCard(corpusCard(t, "Shivan Dragon"), p, engine.Battlefield)
		found := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Library)
		reunion := g.NewCard(corpusCard(t, "Celestial Reunion"), p, engine.Hand)
		g.Player(p).ManaPool.Add(mana.Green, 3)
		c := engine.NewScriptedController()
		c.QueuePayX(2)
		c.QueuePayGeneric(mana.ShardG)
		c.QueuePayGeneric(mana.ShardG)
		c.QueueCardChoice([]engine.CardID{found})
		if !g.CastSpell(p, reunion, c) {
			t.Fatalf("CastSpell failed: %v", g.TakePendingError())
		}
	})
	t.Run("wrong type named", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
		g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
		reunion := g.NewCard(corpusCard(t, "Celestial Reunion"), p, engine.Hand)
		g.Player(p).ManaPool.Add(mana.Green, 3)
		c := engine.NewScriptedController()
		c.QueueConfirmPayCost(true)
		c.QueuePayX(2)
		c.QueuePayGeneric(mana.ShardG)
		c.QueuePayGeneric(mana.ShardG)
		c.QueueOptionNamed("Dragon")
		if g.CastSpell(p, reunion, c) {
			t.Fatal("CastSpell succeeded with a type no two cards share")
		}
		if g.Card(reunion).Zone == engine.Stack {
			t.Error("the spell is on the stack")
		}
	})
}

// Redemptor Dreadnought's optional ExileFromGrave<1/Creature> exiles the card
// with the spell, and the permanent the spell becomes keeps the link (CR
// 607.2q, GameAction.java:480-481): Card.ExiledWithSource names it from the
// battlefield. Declined, nothing is exiled with it.
func TestRedemptorDreadnoughtKeepsItsExiledCard(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		pay  bool
	}{
		{"paid", true},
		{"declined", false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			fallen := g.NewCard(corpusCard(t, "Colossal Dreadmaw"), p, engine.Graveyard)
			dread := g.NewCard(corpusCard(t, "Redemptor Dreadnought"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Black, 5)
			c := engine.NewScriptedController()
			c.QueueConfirmPayCost(tc.pay)
			c.QueueCardChoice([]engine.CardID{fallen})
			for range 5 {
				c.QueuePayGeneric(mana.ShardB)
			}
			if !g.CastSpell(p, dread, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if g.Card(dread).Zone != engine.Battlefield {
				t.Fatalf("Redemptor Dreadnought is in %v, want the battlefield", g.Card(dread).Zone)
			}
			linked := engine.Matches(g, g.Card(fallen), valid.Parse("Card.ExiledWithSource"), p, dread)
			if linked != tc.pay {
				t.Errorf("exiled with the permanent = %v, want %v", linked, tc.pay)
			}
			if wantZone := map[bool]engine.ZoneType{true: engine.Exile, false: engine.Graveyard}[tc.pay]; g.Card(fallen).Zone != wantZone {
				t.Errorf("the creature card is in %v, want %v", g.Card(fallen).Zone, wantZone)
			}
		})
	}
}

// Quantum Reduction's CastWithFlash ValidSA$ Spell.Teamwork: on an opponent's
// turn it can be cast only by paying Teamwork (SpellAbility.canPlay(true),
// SpellAbility.java:467-491, rechecked once the costs are chosen,
// PlaySpellAbility.java:107-113). Declining Teamwork, or having no creature to
// tap for it, leaves it in hand.
func TestQuantumReductionFlashNeedsTeamwork(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name       string
		creature   bool
		pay        bool
		wantCast   bool
		wantTapped bool
	}{
		{"teamwork paid", true, true, true, true},
		{"teamwork declined", true, false, false, false},
		{"nothing to tap", false, false, false, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			g.SetTurnState(1, other, engine.Main1)
			theirs := g.NewCard(corpusCard(t, "Colossal Dreadmaw"), other, engine.Battlefield)
			var bears engine.CardID
			if tc.creature {
				bears = g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
			}
			aura := g.NewCard(corpusCard(t, "Quantum Reduction"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Blue, 2)
			c := engine.NewScriptedController()
			if tc.creature {
				c.QueueConfirmPayCost(tc.pay)
				c.QueueCardChoice([]engine.CardID{bears})
			}
			c.QueueEnchantTarget(theirs)
			c.QueuePayGeneric(mana.ShardU)
			if got := g.CastSpell(p, aura, c); got != tc.wantCast {
				t.Fatalf("CastSpell = %v, want %v (%v)", got, tc.wantCast, g.TakePendingError())
			}
			if tc.creature && g.Card(bears).Tapped != tc.wantTapped {
				t.Errorf("Grizzly Bears tapped = %v, want %v", g.Card(bears).Tapped, tc.wantTapped)
			}
			if !tc.wantCast {
				if g.Card(aura).Zone != engine.Hand {
					t.Errorf("Quantum Reduction is in %v, want the hand", g.Card(aura).Zone)
				}
				return
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got, _ := g.Card(theirs).Power(); got != 1 {
				t.Errorf("enchanted creature's power = %d, want 1 (6 - 5)", got)
			}
		})
	}
}

// Cryptex's "{T}, Collect evidence 3: Add one mana of any color. Put an unlock
// counter on Cryptex.": CollectEvidence<3> as an activation cost
// (CostCollectEvidence.java:42-80) and the mana ability's SubAbility$, resolved
// with it. Too little mana value in the graveyard and it cannot be activated.
func TestCryptexCollectsEvidenceForMana(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name     string
		card     string
		want     bool
		counters int
	}{
		{"enough evidence", "Shivan Dragon", true, 1},
		{"too little evidence", "Grizzly Bears", false, 0},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			cryptex := g.NewCard(corpusCard(t, "Cryptex"), p, engine.Battlefield)
			evidence := g.NewCard(corpusCard(t, tc.card), p, engine.Graveyard)
			c := engine.NewScriptedController()
			c.QueueManaColor(mana.Red)
			c.QueueCardChoice([]engine.CardID{evidence})
			if got := g.ActivateManaAbility(p, cryptex, 0, c); got != tc.want {
				t.Fatalf("ActivateManaAbility = %v, want %v (%v)", got, tc.want, g.TakePendingError())
			}
			if err := g.TakePendingError(); err != nil {
				t.Fatalf("pending error: %v", err)
			}
			if got := g.Card(cryptex).Counters.Count(engine.CounterType("UNLOCK")); got != tc.counters {
				t.Errorf("unlock counters = %d, want %d", got, tc.counters)
			}
			if tc.want {
				if g.Card(evidence).Zone != engine.Exile {
					t.Errorf("the evidence is in %v, want exile", g.Card(evidence).Zone)
				}
				if got := g.Player(p).ManaPool.Total(); got != 1 {
					t.Errorf("mana in pool = %d, want 1", got)
				}
				if !g.Card(cryptex).Tapped {
					t.Error("Cryptex is untapped")
				}
			}
		})
	}
}
