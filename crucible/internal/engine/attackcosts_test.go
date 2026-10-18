package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These tests cover attack costs (CR 508.1d, 508.1h-j): Mode$
// CantAttackUnless statics, the real corpus cards that carry them, paid as
// attackers are declared (CombatUtil.checkPropagandaEffects,
// PhaseHandler.java:553-597).

// attackCostGame is a two-player game on the real corpus in a's declare
// attackers step, with each named card on b's battlefield.
func attackCostGame(t *testing.T, defenders ...string) (*engine.Game, engine.PlayerID, engine.PlayerID) {
	t.Helper()
	g, a, b := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, a, engine.DeclareAttackers)
	for _, name := range defenders {
		g.NewCard(corpusCard(t, name), b, engine.Battlefield)
	}
	return g, a, b
}

// payColorless queues n generic units paid from colorless mana and adds them
// to p's pool.
func payColorless(g *engine.Game, c *engine.ScriptedController, p engine.PlayerID, n int) {
	g.Player(p).ManaPool.AddColorless(n)
	for range n {
		c.QueuePayGeneric(mana.ShardC)
	}
}

// Propaganda: the attacker's controller is asked, pays {2} and the creature
// attacks, tapped.
func TestPropagandaTaxIsPaidAndTheCreatureAttacks(t *testing.T) {
	t.Parallel()
	g, a, _ := attackCostGame(t, "Propaganda")
	bear := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{bear})
	c.QueueConfirmPayCost(true)
	payColorless(g, c, a, 2)
	got := declareAttackers(t, g, c)

	if len(got) != 1 || got[0] != bear || !g.Card(bear).Tapped {
		t.Fatalf("attackers = %v, tapped %v; want the bear attacking, tapped", got, g.Card(bear).Tapped)
	}
	if n := g.Player(a).ManaPool.Total(); n != 0 {
		t.Errorf("pool after the tax = %d, want 0", n)
	}
}

// Declining the tax removes that attacker alone and untaps it; the other
// attacker, who paid, still attacks (PhaseHandler.java:580-584).
func TestDecliningAnAttackTaxDropsThatAttackerOnly(t *testing.T) {
	t.Parallel()
	g, a, _ := attackCostGame(t, "Ghostly Prison")
	payer := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	decliner := g.NewCard(creatureDefPT(t, "3", "3"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{payer, decliner})
	c.QueueConfirmPayCost(true)
	payColorless(g, c, a, 2)
	c.QueueConfirmPayCost(false)
	got := declareAttackers(t, g, c)

	if len(got) != 1 || got[0] != payer {
		t.Fatalf("attackers = %v, want [%v]", got, payer)
	}
	if g.Card(decliner).Tapped {
		t.Error("the attacker that declined its tax stayed tapped")
	}
	if attackers := g.Attackers(); len(attackers) != 1 || attackers[0] != payer {
		t.Errorf("Attackers() = %v, want [%v]", attackers, payer)
	}
}

// A tax its controller cannot pay (no mana) drops the attacker even on a yes.
func TestAnUnpayableAttackTaxDropsTheAttacker(t *testing.T) {
	t.Parallel()
	g, a, _ := attackCostGame(t, "Windborn Muse")
	bear := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{bear})
	c.QueueConfirmPayCost(true)
	// The payer still names a mana type per generic unit; the empty pool
	// then fails the payment.
	c.QueuePayGeneric(mana.ShardC)
	c.QueuePayGeneric(mana.ShardC)
	got := declareAttackers(t, g, c)

	if len(got) != 0 || g.Card(bear).Tapped {
		t.Fatalf("attackers = %v, tapped %v; want none, untapped", got, g.Card(bear).Tapped)
	}
}

// Two taxes on the same attack are summed into one cost
// (CombatUtil.java:291-298), and Sphere of Safety's X counts enchantments its
// controller controls: Propaganda and Sphere make {2}+{2}.
func TestStackedAttackTaxesAreSummed(t *testing.T) {
	t.Parallel()
	g, a, _ := attackCostGame(t, "Propaganda", "Sphere of Safety")
	bear := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{bear})
	c.QueueConfirmPayCost(true)
	payColorless(g, c, a, 4)
	got := declareAttackers(t, g, c)

	if len(got) != 1 || g.Player(a).ManaPool.Total() != 0 {
		t.Fatalf("attackers = %v, pool left %d; want the bear attacking for all 4", got, g.Player(a).ManaPool.Total())
	}
}

// Propaganda's Target$ You does not reach a planeswalker its controller
// controls (nobody is asked); Sphere of Safety's Planeswalker.YouCtrl does.
func TestAttackTaxFollowsItsTargetParam(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		static string
		taxed  bool
	}{{"Propaganda", false}, {"Sphere of Safety", true}} {
		t.Run(tc.static, func(t *testing.T) {
			t.Parallel()
			g, a, b := attackCostGame(t, tc.static)
			walker := g.NewCard(corpusCard(t, "Jace Beleren"), b, engine.Battlefield)
			bear := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

			c := engine.NewScriptedController()
			c.QueueAttackers([]engine.CardID{bear})
			c.QueueAttackTarget(engine.CardEntity(walker))
			if tc.taxed {
				c.QueueConfirmPayCost(false)
			}
			got := declareAttackers(t, g, c)
			if attacked := len(got) == 1; attacked == tc.taxed {
				t.Errorf("attackers = %v, want taxed %v", got, tc.taxed)
			}
		})
	}
}

// Brainwash's Creature.AttachedBy taxes the enchanted creature only.
func TestBrainwashTaxesOnlyTheEnchantedCreature(t *testing.T) {
	t.Parallel()
	g, a, b := attackCostGame(t)
	enchanted := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	free := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	aura := g.NewCard(corpusCard(t, "Brainwash"), b, engine.Battlefield)
	g.Attach(aura, enchanted)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{enchanted, free})
	c.QueueConfirmPayCost(false)
	got := declareAttackers(t, g, c)

	if len(got) != 1 || got[0] != free {
		t.Fatalf("attackers = %v, want only the unenchanted %v", got, free)
	}
}

// Leviathan's own Sac<2/Island>: a non-mana part, the same chosen-card
// payment an UnlessCost$ uses.
func TestLeviathanSacrificesTwoIslandsToAttack(t *testing.T) {
	t.Parallel()
	g, a, _ := attackCostGame(t)
	leviathan := g.NewCard(corpusCard(t, "Leviathan"), a, engine.Battlefield)
	i1 := g.NewCard(corpusCard(t, "Island"), a, engine.Battlefield)
	i2 := g.NewCard(corpusCard(t, "Island"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{leviathan})
	c.QueueConfirmPayCost(true)
	c.QueueSacrificeChoice([]engine.CardID{i1, i2})
	got := declareAttackers(t, g, c)

	if len(got) != 1 {
		t.Fatalf("attackers = %v, want Leviathan", got)
	}
	for _, id := range []engine.CardID{i1, i2} {
		if g.Card(id).Zone != engine.Graveyard {
			t.Errorf("Island %v in %v, want the graveyard", id, g.Card(id).Zone)
		}
	}
}

// An attacker sacrificed to pay another's cost leaves combat
// (Combat.removeAbsentCombatants): Dryad Arbor, a land creature, attacks
// beside Exalted Dragon and is the land the Dragon sacrifices, in either
// declaration order.
func TestAttackerSacrificedForAnAttackCostLeavesCombat(t *testing.T) {
	t.Parallel()
	for _, arborFirst := range []bool{true, false} {
		g, a, _ := attackCostGame(t)
		dragon := g.NewCard(corpusCard(t, "Exalted Dragon"), a, engine.Battlefield)
		arbor := g.NewCard(corpusCard(t, "Dryad Arbor"), a, engine.Battlefield)
		order := []engine.CardID{dragon, arbor}
		if arborFirst {
			order = []engine.CardID{arbor, dragon}
		}

		c := engine.NewScriptedController()
		c.QueueAttackers(order)
		c.QueueConfirmPayCost(true)
		c.QueueSacrificeChoice([]engine.CardID{arbor})
		got := declareAttackers(t, g, c)

		if len(got) != 1 || got[0] != dragon || g.Card(arbor).Zone != engine.Graveyard {
			t.Errorf("arbor first %v: attackers = %v, arbor in %v; want only the Dragon, arbor sacrificed", arborFirst, got, g.Card(arbor).Zone)
		}
		if attackers := g.Attackers(); len(attackers) != 1 || attackers[0] != dragon {
			t.Errorf("arbor first %v: Attackers() = %v, want [%v]", arborFirst, attackers, dragon)
		}
	}
}

// Hollow Warrior's tapXType<1/Creature.!attacking>: a vigilance attacker stays
// untapped but is attacking, so it cannot pay; with no other creature the
// Warrior stays home, and nobody is asked: the unpayable part fails the cost
// first (PlaySpellAbility.java:187-191).
func TestHollowWarriorCannotTapAnAttackerForItsCost(t *testing.T) {
	t.Parallel()
	g, a, _ := attackCostGame(t)
	warrior := g.NewCard(corpusCard(t, "Hollow Warrior"), a, engine.Battlefield)
	vigilant := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Vigilance"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{warrior, vigilant})
	got := declareAttackers(t, g, c)

	if len(got) != 1 || got[0] != vigilant {
		t.Fatalf("attackers = %v, want only the vigilance creature", got)
	}
	if g.Card(vigilant).Tapped {
		t.Error("the vigilance attacker was tapped for the cost")
	}
}

// With an untapped non-attacker, Hollow Warrior taps it and attacks.
func TestHollowWarriorTapsANonAttacker(t *testing.T) {
	t.Parallel()
	g, a, _ := attackCostGame(t)
	warrior := g.NewCard(corpusCard(t, "Hollow Warrior"), a, engine.Battlefield)
	helper := g.NewCard(creatureDefPT(t, "1", "1"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{warrior})
	c.QueueConfirmPayCost(true)
	c.QueueTapChoice([]engine.CardID{helper})
	got := declareAttackers(t, g, c)

	if len(got) != 1 || !g.Card(helper).Tapped {
		t.Fatalf("attackers = %v, helper tapped %v; want the Warrior attacking, helper tapped", got, g.Card(helper).Tapped)
	}
}

// Nils's RememberingAttacker$: X is the counters on the attacker itself.
func TestNilsTaxIsTheAttackersCounters(t *testing.T) {
	t.Parallel()
	g, a, b := attackCostGame(t, "Nils, Discipline Enforcer")
	bear := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	g.Card(bear).Counters.Add(engine.P1P1, 3)
	plain := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{bear, plain})
	c.QueueConfirmPayCost(true)
	payColorless(g, c, a, 3)
	got := declareAttackers(t, g, c)

	if len(got) != 2 || g.Player(a).ManaPool.Total() != 0 {
		t.Fatalf("attackers = %v, pool left %d; want both attacking, 3 paid", got, g.Player(a).ManaPool.Total())
	}
	if n := len(g.Card(g.Zone(engine.Battlefield, b).Cards()[0]).Memory.Remembered()); n != 0 {
		t.Errorf("Nils remembers %d objects after the evaluation, want 0", n)
	}
}

// A static whose condition does not hold taxes nothing: Archangel of Tithes
// tapped, Dain without an enduring story. Nobody is asked.
func TestAttackTaxConditions(t *testing.T) {
	t.Parallel()
	for _, name := range []string{"Archangel of Tithes", "Dáin, Lord of the Iron Hills"} {
		t.Run(name, func(t *testing.T) {
			t.Parallel()
			g, a, b := attackCostGame(t, name)
			g.Card(g.Zone(engine.Battlefield, b).Cards()[0]).Tapped = true
			bear := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

			c := engine.NewScriptedController()
			c.QueueAttackers([]engine.CardID{bear})
			if got := declareAttackers(t, g, c); len(got) != 1 {
				t.Fatalf("attackers = %v, want the bear untaxed", got)
			}
		})
	}
}

// Dain with an enduring story does tax.
func TestDainTaxesWithAnEnduringStory(t *testing.T) {
	t.Parallel()
	g, a, b := attackCostGame(t, "Dáin, Lord of the Iron Hills")
	g.Player(b).EnduringStory = true
	bear := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{bear})
	c.QueueConfirmPayCost(false)
	if got := declareAttackers(t, g, c); len(got) != 0 {
		t.Fatalf("attackers = %v, want none (tax declined)", got)
	}
}

// A creature that must attack may stay home when attacking costs something:
// collectLegalAttackers skips a costed pair (AttackConstraints.java:195-197),
// so the best attack leaves the requirement unmet too.
func TestMustAttackCreatureMayStayHomeUnderPropaganda(t *testing.T) {
	t.Parallel()
	g, a, _ := attackCostGame(t, "Propaganda")
	g.NewCard(combatCreatureDef(t, "Bloodrock Cyclops", []string{"Mode$ MustAttack | ValidCreature$ Card.Self"}, nil), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers(nil)
	if got := declareAttackers(t, g, c); len(got) != 0 {
		t.Fatalf("attackers = %v, want none", got)
	}
}

// Dropping an unpaid attacker can leave the rest illegal ("can't attack
// alone"): the declaration fails, the combat is undone and every attacker
// untaps, while the tax already paid stays paid (PhaseHandler.java:577).
func TestUnpaidTaxThatBreaksTheAttackFailsTheDeclaration(t *testing.T) {
	t.Parallel()
	g, a, _ := attackCostGame(t, "Propaganda")
	loner := g.NewCard(creatureDefPTKeywords(t, "2", "2", "CARDNAME can't attack alone."), a, engine.Battlefield)
	partner := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{loner, partner})
	c.QueueConfirmPayCost(true)
	payColorless(g, c, a, 2)
	c.QueueConfirmPayCost(false)
	_, err := g.DeclareCombatAttackers(c)
	wantIllegal(t, err, "CR 508.1c")

	if len(g.Attackers()) != 0 {
		t.Errorf("Attackers() = %v after a failed declaration, want none", g.Attackers())
	}
	if g.Card(loner).Tapped || g.Card(partner).Tapped {
		t.Error("an attacker stayed tapped after the failed declaration")
	}
	if n := g.Player(a).ManaPool.Total(); n != 0 {
		t.Errorf("pool = %d, want the paid tax kept spent (0)", n)
	}
}

// A CantAttackUnless line this port cannot evaluate fails the declaration
// before anything changes (GO-7): an unknown param, an unknown condition, two
// Sac parts the payment cannot hold, an SVar that does not resolve, no Cost$.
func TestUnresolvableAttackTaxIsAnError(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name, static, want string
		svars              []string
	}{
		{"param", "Mode$ CantAttackUnless | ValidCard$ Card.Self | Cost$ 2 | Bogus$ 1", "Bogus$ not resolvable", nil},
		{"condition", "Mode$ CantAttackUnless | ValidCard$ Card.Self | Cost$ 2 | Condition$ Bogus", "Condition$", nil},
		{"cost", "Mode$ CantAttackUnless | ValidCard$ Card.Self | Cost$ Sac<1/Land> Sac<1/Creature>", "not resolvable yet", nil},
		{"svar", "Mode$ CantAttackUnless | ValidCard$ Card.Self | Cost$ Q", "SVar not resolvable", []string{"Q", "Count$Bogus"}},
		{"nocost", "Mode$ CantAttackUnless | ValidCard$ Card.Self", "without Cost$", nil},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, a, _ := attackCostGame(t)
			id := g.NewCard(combatCreatureDef(t, "Taxed", []string{tc.static}, nil, tc.svars...), a, engine.Battlefield)

			c := engine.NewScriptedController()
			c.QueueAttackers([]engine.CardID{id})
			_, err := g.DeclareCombatAttackers(c)
			wantErrorContaining(t, err, tc.want)
			if g.Card(id).Tapped || len(g.Attackers()) != 0 {
				t.Error("the failed declaration changed the game")
			}
		})
	}
}
