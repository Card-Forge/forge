package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// A triggered AB$'s Cost$ is paid as the ability resolves (WrappedAbility.java:
// 440): these tests put a watcher on the battlefield whose "another creature
// enters" trigger carries the cost under test, then cast a 1/1 to set it off.
// Every answer is queued first: ResolveStack resolves the 1/1 and the trigger.

// costWatcherDef is a 1/1 whose trigger is the AB$ line body, with extra
// script lines (SVars) after it.
func costWatcherDef(t *testing.T, body string, extra ...string) *compile.Card {
	t.Helper()
	lines := append([]string{
		"T:Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Creature.Other+YouCtrl | TriggerZones$ Battlefield | Execute$ TrigCost | TriggerDescription$ Whenever another creature you control enters, you may pay a cost.",
		"SVar:TrigCost:" + body,
	}, extra...)
	return scriptDef(t, "Test Cost Watcher", "Creature", lines...)
}

// placeWatcher puts body's watcher onto the battlefield under p.
func placeWatcher(t *testing.T, g *engine.Game, p engine.PlayerID, body string, extra ...string) engine.CardID {
	t.Helper()
	return g.NewCard(costWatcherDef(t, body, extra...), p, engine.Battlefield)
}

// fireWatcher casts a {W} 1/1 for p and resolves the stack: the 1/1, then the
// watcher's trigger.
func fireWatcher(t *testing.T, g *engine.Game, p engine.PlayerID, c *engine.ScriptedController) error {
	t.Helper()
	bait := scriptDef(t, "Test Bait", "Creature")
	bait.Faces[0].ManaCost = mana.MustParse("W")
	card := g.NewCard(bait, p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, card, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

// mustFireWatcher is fireWatcher failing the test on an error.
func mustFireWatcher(t *testing.T, g *engine.Game, p engine.PlayerID, c *engine.ScriptedController) {
	t.Helper()
	if err := fireWatcher(t, g, p, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
}

// Cost$ X W with SVar X:Count$xPaid: X is announced as the cost is paid
// (PlaySpellAbility.announceValuesLikeX, :766-782), capped by XMax$
// (AbilityUtils.getAnnouncementBounds, :3058-3060), and the ability and its
// sub-ability read it. A declined trigger pays nothing and does nothing.
func TestTriggeredCostAnnouncesX(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name         string
		xmax         string
		confirm      bool
		x, generic   int
		mine, theirs int
		poolLeft     int
	}{
		{"paid", "", true, 3, 3, 23, 17, 0},
		{"declined", "", false, 0, 0, 20, 20, 4},
		{"capped by XMax", " | XMax$ 2", true, 5, 2, 22, 18, 1},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			placeWatcher(t, g, p, "AB$ GainLife | Cost$ X W | Defined$ You | LifeAmount$ X | SubAbility$ DBTheirs"+tc.xmax,
				"SVar:DBTheirs:DB$ LoseLife | Defined$ Opponent | LifeAmount$ X",
				"SVar:X:Count$xPaid")
			g.Player(p).ManaPool.Add(mana.White, 4)
			c := engine.NewScriptedController()
			c.QueueConfirmOptionalTrigger(tc.confirm)
			if tc.confirm {
				c.QueuePayX(tc.x)
				for range tc.generic {
					c.QueuePayGeneric(mana.ShardW)
				}
			}
			mustFireWatcher(t, g, p, c)
			if got := g.Player(p).Life; got != tc.mine {
				t.Errorf("controller's life = %d, want %d", got, tc.mine)
			}
			if got := g.Player(other).Life; got != tc.theirs {
				t.Errorf("opponent's life = %d, want %d", got, tc.theirs)
			}
			if got := g.Player(p).ManaPool.Total(); got != tc.poolLeft {
				t.Errorf("mana left = %d, want %d", got, tc.poolLeft)
			}
		})
	}
}

// An X the card computes (Champion of Wits' Draw<X/You>, X its power) is never
// announced: CostPart.getAbilityAmount reads the SVar as the cost is paid.
func TestTriggeredCostReadsComputedX(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	placeWatcher(t, g, p, "AB$ GainLife | Cost$ PayLife<X> | Defined$ You | LifeAmount$ 5", "SVar:X:Count$CardPower")
	c := engine.NewScriptedController()
	c.QueueConfirmOptionalTrigger(true)
	mustFireWatcher(t, g, p, c)
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life = %d, want 24 (paid 1 for power 1, gained 5)", got)
	}
}

// A part of amount X is bounded by what it can pay (PlayerControllerHuman.
// announceRequirements, :376-381): two +1/+1 counters cap SubCounter<X/P1P1>
// at 2 whatever is announced, and X 0 pays nothing.
func TestTriggeredCostBoundsXByItsParts(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name     string
		counters int
		announce int
		life     int
	}{
		{"capped", 2, 5, 22},
		{"zero", 0, 3, 20},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			watcher := placeWatcher(t, g, p, "AB$ GainLife | Cost$ SubCounter<X/P1P1> | Defined$ You | LifeAmount$ X", "SVar:X:Count$xPaid")
			g.Card(watcher).Counters.Add(engine.P1P1, tc.counters)
			c := engine.NewScriptedController()
			c.QueueConfirmOptionalTrigger(true)
			c.QueuePayX(tc.announce)
			mustFireWatcher(t, g, p, c)
			if got := g.Player(p).Life; got != tc.life {
				t.Errorf("life = %d, want %d", got, tc.life)
			}
			if got := g.Card(watcher).Counters.Count(engine.P1P1); got != 0 {
				t.Errorf("+1/+1 counters left = %d, want 0", got)
			}
		})
	}
}

// A reflexive trigger reads the X of the ability that spawned it
// (AbilityUtils.java:1649-1656): Isareth's "you may pay {X}. When you do".
func TestImmediateTriggerReadsSpawningX(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	placeWatcher(t, g, p, "AB$ ImmediateTrigger | Cost$ X | Execute$ TrigGain | TriggerDescription$ When you do, gain X life.",
		"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ X",
		"SVar:X:Count$xPaid")
	g.Player(p).ManaPool.Add(mana.Green, 2)
	c := engine.NewScriptedController()
	c.QueueConfirmOptionalTrigger(true)
	c.QueuePayX(2)
	c.QueuePayGeneric(mana.ShardG)
	c.QueuePayGeneric(mana.ShardG)
	mustFireWatcher(t, g, p, c)
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22", got)
	}
}

// SubCounter<N/Type> and RemoveAnyCounter<N/Any/CARDNAME> take counters off the
// source itself (CostRemoveCounter.java:163-165, CostRemoveAnyCounter); without
// them the cost cannot be paid and the ability does nothing.
func TestTriggeredCostRemovesCountersFromItself(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, cost string
		kind       engine.CounterType
		have, left int
		life       int
	}{
		{"oil counter", "SubCounter<1/OIL>", engine.CounterType("OIL"), 2, 1, 25},
		{"keyword counter", "SubCounter<1/Menace>", engine.CounterType("MENACE"), 1, 0, 25},
		{"none to remove", "SubCounter<1/OIL>", engine.CounterType("OIL"), 0, 0, 20},
		{"any counter", "RemoveAnyCounter<1/Any/CARDNAME/this creature>", engine.P1P1, 1, 0, 25},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			watcher := placeWatcher(t, g, p, "AB$ GainLife | Cost$ "+tc.cost+" | Defined$ You | LifeAmount$ 5")
			g.Card(watcher).Counters.Add(tc.kind, tc.have)
			c := engine.NewScriptedController()
			c.QueueConfirmOptionalTrigger(true)
			c.QueueCardChoice([]engine.CardID{watcher})
			mustFireWatcher(t, g, p, c)
			if got := g.Player(p).Life; got != tc.life {
				t.Errorf("life = %d, want %d", got, tc.life)
			}
			if got := g.Card(watcher).Counters.Count(tc.kind); got != tc.left {
				t.Errorf("%s counters left = %d, want %d", tc.kind, got, tc.left)
			}
		})
	}
}

// ExiledMoveToGrave<1/Card.OppOwn> (CostExiledMoveToGrave.java:48-92) puts an
// exiled card an opponent owns into its owner's graveyard; the caster's own
// exiled card does not count.
func TestTriggeredCostMovesExiledCardToGraveyard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	mine := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Exile)
	theirs := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Exile)
	placeWatcher(t, g, p, "AB$ GainLife | Cost$ ExiledMoveToGrave<1/Card.OppOwn/card an opponent owns> | Defined$ You | LifeAmount$ 5")
	c := engine.NewScriptedController()
	c.QueueConfirmOptionalTrigger(true)
	c.QueueCardChoice([]engine.CardID{theirs})
	mustFireWatcher(t, g, p, c)
	if got := g.Card(theirs).Zone; got != engine.Graveyard || g.Card(theirs).ZoneOwner != other {
		t.Errorf("the opponent's card is in %v of %d, want their graveyard", got, g.Card(theirs).ZoneOwner)
	}
	if g.Card(mine).Zone != engine.Exile {
		t.Errorf("the caster's own exiled card moved to %v", g.Card(mine).Zone)
	}
	if got := g.Player(p).Life; got != 25 {
		t.Errorf("life = %d, want 25", got)
	}
}

// Forage (CostForage.java:19-70) sacrifices a Food or exiles three graveyard
// cards, the payer's choice when both are possible (HumanCostDecision.java:
// 619-646), then fires Mode$ Forage.
func TestTriggeredCostForages(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name      string
		food      bool
		graveyard int
		option    int
		foodGone  bool
		exiled    int
	}{
		{"food only", true, 0, -1, true, 0},
		{"graveyard only", false, 3, -1, false, 3},
		{"both, exile chosen", true, 4, 1, false, 3},
		{"both, food chosen", true, 4, 0, true, 0},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			g.NewCard(scriptDef(t, "Test Forage Watcher", "Enchantment",
				"T:Mode$ Forage | ValidPlayer$ You | TriggerZones$ Battlefield | Execute$ TrigGain | TriggerDescription$ Whenever you forage, gain 1 life.",
				"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
			var food engine.CardID
			if tc.food {
				food = g.NewCard(corpusCard(t, "Golden Egg"), p, engine.Battlefield)
			}
			grave := make([]engine.CardID, 0, tc.graveyard)
			for range tc.graveyard {
				grave = append(grave, g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Graveyard))
			}
			placeWatcher(t, g, p, "AB$ GainLife | Cost$ Forage | Defined$ You | LifeAmount$ 5")
			c := engine.NewScriptedController()
			c.QueueConfirmOptionalTrigger(true)
			if tc.option >= 0 {
				c.QueueOption(tc.option)
			}
			if tc.foodGone {
				c.QueueCardChoice([]engine.CardID{food})
			} else {
				c.QueueCardChoice(grave[:3])
			}
			mustFireWatcher(t, g, p, c)
			if got := g.Player(p).Life; got != 26 {
				t.Errorf("life = %d, want 26 (5 from the ability, 1 from the forage trigger)", got)
			}
			if tc.food && (g.Card(food).Zone != engine.Battlefield) != tc.foodGone {
				t.Errorf("Food in %v, want sacrificed = %v", g.Card(food).Zone, tc.foodGone)
			}
			if got := len(g.Zone(engine.Exile, p).Cards()); got != tc.exiled {
				t.Errorf("exiled cards = %d, want %d", got, tc.exiled)
			}
		})
	}
}

// Discard<N/Type> discards cards of the type; Discard<0/Hand> the whole hand
// (CostDiscard's "Hand" type, Vaultguard Trooper).
func TestTriggeredCostDiscards(t *testing.T) {
	t.Parallel()

	t.Run("typed", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		land := g.NewCard(corpusCard(t, "Forest"), p, engine.Hand)
		bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Hand)
		placeWatcher(t, g, p, "AB$ GainLife | Cost$ Discard<1/Creature> | Defined$ You | LifeAmount$ 5")
		c := engine.NewScriptedController()
		c.QueueConfirmOptionalTrigger(true)
		c.QueueDiscardChoice([]engine.CardID{bears})
		mustFireWatcher(t, g, p, c)
		if g.Card(bears).Zone != engine.Graveyard || g.Card(land).Zone != engine.Hand {
			t.Errorf("creature in %v, land in %v; want the creature discarded", g.Card(bears).Zone, g.Card(land).Zone)
		}
		if got := g.Player(p).Life; got != 25 {
			t.Errorf("life = %d, want 25", got)
		}
	})
	t.Run("whole hand", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		g.NewCard(corpusCard(t, "Forest"), p, engine.Hand)
		g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Hand)
		placeWatcher(t, g, p, "AB$ GainLife | Cost$ Discard<0/Hand> | Defined$ You | LifeAmount$ 5")
		c := engine.NewScriptedController()
		c.QueueConfirmOptionalTrigger(true)
		mustFireWatcher(t, g, p, c)
		if got := len(g.Zone(engine.Hand, p).Cards()); got != 0 {
			t.Errorf("hand = %d cards, want 0", got)
		}
		if got := g.Player(p).Life; got != 25 {
			t.Errorf("life = %d, want 25", got)
		}
	})
}

// ExileCtrlOrGrave<1/Artifact.Other> exiles an artifact from the battlefield or
// the graveyard, ExileFromHand<1/All> the whole hand; each exiled card is
// exiled with the source (CostExile.java:290-296).
func TestTriggeredCostExiles(t *testing.T) {
	t.Parallel()

	t.Run("controlled or graveyard", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		relic := g.NewCard(corpusCard(t, "Ornithopter"), p, engine.Battlefield)
		watcher := placeWatcher(t, g, p, "AB$ GainLife | Cost$ ExileCtrlOrGrave<1/Artifact.Other> | Defined$ You | LifeAmount$ 5")
		c := engine.NewScriptedController()
		c.QueueConfirmOptionalTrigger(true)
		c.QueueCardChoice([]engine.CardID{relic})
		mustFireWatcher(t, g, p, c)
		if g.Card(relic).Zone != engine.Exile {
			t.Fatalf("the artifact is in %v, want exile", g.Card(relic).Zone)
		}
		if !engine.Matches(g, g.Card(relic), valid.Parse("Card.ExiledWithSource"), p, watcher) {
			t.Error("the exiled artifact is not exiled with the source")
		}
	})
	t.Run("whole hand", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		g.NewCard(corpusCard(t, "Forest"), p, engine.Hand)
		g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Hand)
		placeWatcher(t, g, p, "AB$ GainLife | Cost$ ExileFromHand<1/All> | Defined$ You | LifeAmount$ 5")
		c := engine.NewScriptedController()
		c.QueueConfirmOptionalTrigger(true)
		mustFireWatcher(t, g, p, c)
		if got := len(g.Zone(engine.Exile, p).Cards()); got != 2 {
			t.Errorf("exiled = %d cards, want 2", got)
		}
	})
}

// A cost part with nothing to pay it with (CostPart.canPay false) is not paid,
// and the ability does nothing.
func TestTriggeredCostUnpayablePartsDoNothing(t *testing.T) {
	t.Parallel()

	for _, cost := range []string{
		"Discard<1/Creature>",
		"ExiledMoveToGrave<1/Card.OppOwn/card an opponent owns>",
		"Forage",
		"ExileCtrlOrGrave<1/Artifact.Other>",
		"RemoveAnyCounter<1/Any/CARDNAME/this creature>",
	} {
		t.Run(cost, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			placeWatcher(t, g, p, "AB$ GainLife | Cost$ "+cost+" | Defined$ You | LifeAmount$ 5")
			c := engine.NewScriptedController()
			c.QueueConfirmOptionalTrigger(true)
			mustFireWatcher(t, g, p, c)
			if got := g.Player(p).Life; got != 20 {
				t.Errorf("life = %d, want 20", got)
			}
		})
	}
}

// The shapes payTriggeredCost does not read fail the resolution loudly rather
// than resolving for free (GO-7).
func TestTriggeredCostUnreadShapesFail(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, body, svar, want string
	}{
		{"Mill", "AB$ GainLife | Cost$ Mill<4> | Defined$ You | LifeAmount$ 5", "SVar:Y:1", "Mill<4>"},
		{"XMin", "AB$ GainLife | Cost$ X | XMin$ 1 | Defined$ You | LifeAmount$ X", "SVar:X:Count$xPaid", "XMin$"},
		{"XMax", "AB$ GainLife | Cost$ X | XMax$ Nope | Defined$ You | LifeAmount$ X", "SVar:X:Count$xPaid", "XMax$"},
		{"announced X shape", "AB$ GainLife | Cost$ X Mill<4> | Defined$ You | LifeAmount$ X", "SVar:X:Count$xPaid", "not resolvable"},
		{"computed X", "AB$ GainLife | Cost$ PayLife<X> | Defined$ You | LifeAmount$ 1", "SVar:X:TriggeredPlayersTargets$Amount", "SVar X"},
		{"computed X shape", "AB$ GainLife | Cost$ PayLife<X> Mill<4> | Defined$ You | LifeAmount$ 1", "SVar:X:Count$CardPower", "not resolvable"},
		{"NumTimes", "AB$ GainLife | Cost$ Mana<1 W\\NumTimes> | Announce$ NumTimes | Defined$ You | LifeAmount$ 1", "SVar:NumTimes:Number$0", "not resolvable"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			placeWatcher(t, g, p, tc.body, tc.svar)
			// Nothing is asked: an empty queue would panic.
			c := engine.NewScriptedController()
			err := fireWatcher(t, g, p, c)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Errorf("ResolveStack = %v, want an error naming %q", err, tc.want)
			}
			if got := g.Player(p).Life; got != 20 {
				t.Errorf("life = %d, want 20 (nothing resolved)", got)
			}
		})
	}
}
