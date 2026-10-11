package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Mode$ OptionalCost (GameActionUtil.getOptionalCostValues): Cinder Strike's
// "you may blight 1" is offered as the spell is cast; paying it puts a -1/-1
// counter on a creature its caster controls and Count$OptionalGenericCostPaid
// doubles the damage.
func TestOptionalBlightChangesCinderStrikeDamage(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name    string
		pay     bool
		damage  int
		counter int
	}{
		{"paid", true, 4, 1},
		{"declined", false, 2, 0},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			mine := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
			theirs := g.NewCard(corpusCard(t, "Colossal Dreadmaw"), other, engine.Battlefield)
			strike := g.NewCard(corpusCard(t, "Cinder Strike"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Red, 1)
			c := engine.NewScriptedController()
			c.QueueConfirmPayCost(tc.pay)
			c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
			c.QueueCardChoice([]engine.CardID{mine})
			if !g.CastSpell(p, strike, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Card(theirs).Damage.Marked; got != tc.damage {
				t.Errorf("damage = %d, want %d", got, tc.damage)
			}
			if got := g.Card(mine).Counters.Count(engine.M1M1); got != tc.counter {
				t.Errorf("-1/-1 counters on the blighted creature = %d, want %d", got, tc.counter)
			}
		})
	}
}

// An optional cost its caster cannot pay is not offered: Voltage Surge with no
// artifact to sacrifice asks nothing (an empty ConfirmPayCost queue would
// panic) and deals 2; with one, paying sacrifices it for 4.
func TestOptionalSacrificeOfferedOnlyWhenPayable(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name     string
		artifact bool
		damage   int
	}{
		{"no artifact", false, 2},
		{"artifact sacrificed", true, 4},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			theirs := g.NewCard(corpusCard(t, "Colossal Dreadmaw"), other, engine.Battlefield)
			surge := g.NewCard(corpusCard(t, "Voltage Surge"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Red, 1)
			c := engine.NewScriptedController()
			var relic engine.CardID
			if tc.artifact {
				relic = g.NewCard(corpusCard(t, "Ornithopter"), p, engine.Battlefield)
				c.QueueConfirmPayCost(true)
				c.QueueSacrificeChoice([]engine.CardID{relic})
			}
			c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
			if !g.CastSpell(p, surge, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Card(theirs).Damage.Marked; got != tc.damage {
				t.Errorf("damage = %d, want %d", got, tc.damage)
			}
			if tc.artifact && g.Card(relic).Zone != engine.Graveyard {
				t.Errorf("the sacrificed artifact is in %v, want the graveyard", g.Card(relic).Zone)
			}
		})
	}
}

// Condition$ OptionalCost gates a spell's sub-abilities on its own optional
// cost (SpellAbilityCondition.java:283-284); ConditionOptionalPaid$ False is
// the unpaid branch, and Count$OptionalGenericCostPaid feeds a third gain.
func TestOptionalCostConditionGatesSubAbilities(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name         string
		pay          bool
		mine, theirs int
	}{
		{"paid", true, 20 - 1 + 5 + 2, 20},
		{"declined", false, 20 + 1, 20 + 3},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGame(t)
			def := scriptDef(t, "Test Optional Life", "Instant",
				"S:Mode$ OptionalCost | EffectZone$ All | ValidCard$ Card.Self | ValidSA$ Spell | Cost$ PayLife<1> | Description$ You may pay 1 life.",
				"A:SP$ GainLife | Defined$ You | LifeAmount$ 5 | Condition$ OptionalCost | SubAbility$ DBTheirs",
				"SVar:DBTheirs:DB$ GainLife | Defined$ Opponent | LifeAmount$ 3 | Condition$ OptionalCost | ConditionOptionalPaid$ False | SubAbility$ DBCount",
				"SVar:DBCount:DB$ GainLife | Defined$ You | LifeAmount$ X",
				"SVar:X:Count$OptionalGenericCostPaid.2.1")
			def.Faces[0].ManaCost = mana.MustParse("W")
			spell := g.NewCard(def, p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.White, 1)
			c := engine.NewScriptedController()
			c.QueueConfirmPayCost(tc.pay)
			if !g.CastSpell(p, spell, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Player(p).Life; got != tc.mine {
				t.Errorf("caster's life = %d, want %d", got, tc.mine)
			}
			if got := g.Player(other).Life; got != tc.theirs {
				t.Errorf("opponent's life = %d, want %d", got, tc.theirs)
			}
		})
	}
}

// CollectEvidence as an optional cost records what it exiled: the spell's
// Collected$Amount and Defined$ Collected read it as it resolves, and a copy
// of the game made with the spell on the stack keeps its own record.
func TestCollectedEvidenceIsReadBackBySpell(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	def := scriptDef(t, "Test Evidence", "Sorcery",
		"S:Mode$ OptionalCost | EffectZone$ All | ValidCard$ Card.Self | ValidSA$ Spell | Cost$ CollectEvidence<6> | Description$ You may collect evidence 6.",
		"A:SP$ GainLife | Defined$ You | LifeAmount$ X | SubAbility$ DBTheirs",
		"SVar:DBTheirs:DB$ GainLife | Defined$ Opponent | LifeAmount$ 4 | ConditionDefined$ Collected | ConditionPresent$ Card",
		"SVar:X:Collected$Amount")
	def.Faces[0].ManaCost = mana.MustParse("G")
	spell := g.NewCard(def, p, engine.Hand)
	dragons := []engine.CardID{
		g.NewCard(corpusCard(t, "Shivan Dragon"), p, engine.Graveyard),
		g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Graveyard),
	}
	g.Player(p).ManaPool.Add(mana.Green, 1)
	c := engine.NewScriptedController()
	c.QueueConfirmPayCost(true)
	c.QueueCardChoice(dragons)
	if !g.CastSpell(p, spell, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	for _, id := range dragons {
		if g.Card(id).Zone != engine.Exile {
			t.Fatalf("collected card %d is in %v, want exile", id, g.Card(id).Zone)
		}
	}
	copied := g.Clone()
	for _, game := range []*engine.Game{g, copied} {
		if err := game.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if got := game.Player(p).Life; got != 22 {
			t.Errorf("caster's life = %d, want 22 (two cards collected)", got)
		}
		if got := game.Player(other).Life; got != 24 {
			t.Errorf("opponent's life = %d, want 24 (evidence was collected)", got)
		}
	}
}

// Vitu-Ghazi Inspector reads its cast's evidence from the battlefield through
// CastSA>Collected$Amount: its enters trigger fires only when it was paid.
func TestCastSAContextReadsPermanentsEvidence(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		pay  bool
		life int
	}{
		{"collected", true, 22},
		{"not collected", false, 20},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			inspector := g.NewCard(corpusCard(t, "Vitu-Ghazi Inspector"), p, engine.Hand)
			dragon := g.NewCard(corpusCard(t, "Shivan Dragon"), p, engine.Graveyard)
			g.Player(p).ManaPool.Add(mana.Green, 2)
			c := engine.NewScriptedController()
			c.QueuePayGeneric(mana.ShardG)
			c.QueueConfirmPayCost(tc.pay)
			if tc.pay {
				c.QueueCardChoice([]engine.CardID{dragon})
				c.QueueTargets([]engine.EntityID{engine.CardEntity(inspector)})
			}
			if !g.CastSpell(p, inspector, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			for range 2 {
				if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
					t.Fatalf("ResolveStack: %v", err)
				}
			}
			if got := g.Player(p).Life; got != tc.life {
				t.Errorf("life = %d, want %d", got, tc.life)
			}
		})
	}
}

// A Defiler's ReduceColor$ line (CostAdjustment.java:238-242): paying 2 life
// as the green permanent spell is cast takes {G} off its cost, so Grizzly
// Bears costs {1}.
func TestDefilerLifePaymentReducesColoredMana(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Defiler of Vigor"), p, engine.Battlefield)
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Red, 1)
	c := engine.NewScriptedController()
	c.QueueConfirmPayCost(true)
	c.QueuePayGeneric(mana.ShardR)
	if !g.CastSpell(p, bears, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	if got := g.Player(p).Life; got != 18 {
		t.Errorf("life = %d, want 18", got)
	}
}

// Bite Down on Crime's own ReduceCost line reads Count$OptionalGenericCostPaid
// with Relative$ True: collecting evidence makes it cost {2} less.
func TestOptionalCostFeedsOwnCostReduction(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	mine := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	theirs := g.NewCard(corpusCard(t, "Colossal Dreadmaw"), other, engine.Battlefield)
	dragon := g.NewCard(corpusCard(t, "Shivan Dragon"), p, engine.Graveyard)
	bite := g.NewCard(corpusCard(t, "Bite Down on Crime"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 2)
	c := engine.NewScriptedController()
	c.QueueConfirmPayCost(true)
	c.QueueCardChoice([]engine.CardID{dragon})
	c.QueuePayGeneric(mana.ShardG)
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
	if !g.CastSpell(p, bite, c) {
		t.Fatalf("CastSpell with {G}{G} and evidence failed: %v", g.TakePendingError())
	}
	if g.Card(dragon).Zone != engine.Exile {
		t.Errorf("the evidence is in %v, want exile", g.Card(dragon).Zone)
	}
}

// Waterbend as an optional cost: its {N} joins the cast's cost and the caster
// may tap artifacts and creatures for it. Katara's enters trigger discards
// unless it was paid, read through CastSA>Count$OptionalGenericCostPaid.
func TestWaterbendOptionalCostReadByPermanent(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name    string
		pay     bool
		hand    int
		tapped  bool
		blacks  int
		generic int
	}{
		{"waterbent", true, 2, true, 4, 3},
		{"declined", false, 1, false, 4, 3},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			libraryCards(t, g, p, 3)
			bears := []engine.CardID{
				g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield),
				g.NewCard(corpusCard(t, "Ornithopter"), p, engine.Battlefield),
			}
			kept := g.NewCard(corpusCard(t, "Shivan Dragon"), p, engine.Hand)
			katara := g.NewCard(corpusCard(t, "Katara, Seeking Revenge"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Black, tc.blacks)
			c := engine.NewScriptedController()
			c.QueueConfirmPayCost(tc.pay)
			if tc.pay {
				c.QueueCardChoice(bears)
			} else {
				c.QueueDiscardChoice([]engine.CardID{kept})
			}
			for range tc.generic {
				c.QueuePayGeneric(mana.ShardB)
			}
			c.QueueHybridManaColor(mana.Black)
			if !g.CastSpell(p, katara, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			for range 2 {
				if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
					t.Fatalf("ResolveStack: %v", err)
				}
			}
			if got := len(g.Zone(engine.Hand, p).Cards()); got != tc.hand {
				t.Errorf("hand = %d cards, want %d", got, tc.hand)
			}
			if got := g.Card(bears[0]).Tapped; got != tc.tapped {
				t.Errorf("waterbending creature tapped = %v, want %v", got, tc.tapped)
			}
		})
	}
}

// Teamwork (CostTeamwork, GameActionUtil.java:489-493) is the keyword half of
// the same list: paying it taps creatures with total power 2 and
// Count$Teamwork makes Repulsor Blast hit the creature's controller too.
func TestTeamworkTapsCreaturesAndIsReadBack(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		pay  bool
		life int
	}{
		{"teamwork", true, 18},
		{"declined", false, 20},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
			theirs := g.NewCard(corpusCard(t, "Colossal Dreadmaw"), other, engine.Battlefield)
			blast := g.NewCard(corpusCard(t, "Repulsor Blast"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Red, 4)
			c := engine.NewScriptedController()
			c.QueueConfirmPayCost(tc.pay)
			c.QueueCardChoice([]engine.CardID{bears})
			for range 3 {
				c.QueuePayGeneric(mana.ShardR)
			}
			c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
			if !g.CastSpell(p, blast, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			if g.Card(bears).Tapped != tc.pay {
				t.Errorf("Grizzly Bears tapped = %v, want %v", g.Card(bears).Tapped, tc.pay)
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Player(other).Life; got != tc.life {
				t.Errorf("opponent's life = %d, want %d", got, tc.life)
			}
		})
	}
}

// Count$Presence_Dragon (AbilityUtils.java:2180-2192): a Dragon revealed for
// the optional cost, or one its caster controls, picks the first branch.
func TestPresenceCountsRevealedOrControlledDragon(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name               string
		inHand, controlled bool
		pay                bool
		life               int
	}{
		{"revealed from hand", true, false, true, 24},
		{"controlled, not revealed", false, true, false, 24},
		{"in hand, declined", true, false, false, 21},
		{"no Dragon", false, false, false, 21},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			def := scriptDef(t, "Test Presence", "Instant",
				"S:Mode$ OptionalCost | EffectZone$ All | ValidCard$ Card.Self | ValidSA$ Spell | Cost$ Reveal<1/Dragon> | Description$ You may reveal a Dragon.",
				"A:SP$ GainLife | Defined$ You | LifeAmount$ X",
				"SVar:X:Count$Presence_Dragon.4.1")
			def.Faces[0].ManaCost = mana.MustParse("W")
			spell := g.NewCard(def, p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.White, 1)
			c := engine.NewScriptedController()
			if tc.inHand {
				dragon := g.NewCard(corpusCard(t, "Shivan Dragon"), p, engine.Hand)
				c.QueueConfirmPayCost(tc.pay)
				c.QueueCardChoice([]engine.CardID{dragon})
			}
			if tc.controlled {
				g.NewCard(corpusCard(t, "Shivan Dragon"), p, engine.Battlefield)
			}
			if !g.CastSpell(p, spell, c) {
				t.Fatalf("CastSpell failed: %v", g.TakePendingError())
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Player(p).Life; got != tc.life {
				t.Errorf("life = %d, want %d", got, tc.life)
			}
		})
	}
}

// The OptionalCost shapes this port cannot pay are refused at cast time, with
// the reason left for TakePendingError, rather than cast without the choice.
func TestOptionalCostUnportedShapesRefuseTheCast(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ card, want string }{
		{"Dragon's Fire", "RevealOrChoose"},
		{"Celestial Reunion", "ChooseCreatureType"},
		{"Redemptor Dreadnought", "exiled-with link"},
	} {
		t.Run(tc.card, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			card := g.NewCard(corpusCard(t, tc.card), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Red, 6)
			if g.CastSpell(p, card, engine.NewScriptedController()) {
				t.Fatal("CastSpell succeeded, want it refused")
			}
			if err := g.TakePendingError(); err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Errorf("pending error = %v, want one naming %q", err, tc.want)
			}
			if g.Card(card).Zone != engine.Hand {
				t.Errorf("the card is in %v, want it left in hand", g.Card(card).Zone)
			}
		})
	}
}
