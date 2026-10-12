package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

const gainThreeSVar = "SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 3"

// Mode$ Cycled | ValidCard$ Card.Self (78 real lines): the card fires from
// the graveyard once its Cycling ability has been activated and the discard
// paid (Player.addCycled).
func TestCycledTriggersFromTheGraveyardOnTheCardsOwnCycling(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).ManaPool.AddColorless(1)
	card := g.NewCard(scriptDef(t, "Test Cycler", "Creature Elf", "K:Cycling:1",
		"T:Mode$ Cycled | ValidCard$ Card.Self | Execute$ TrigGain", gainThreeSVar), p, engine.Hand)
	g.NewCard(corpusCard(t, "Island"), p, engine.Library)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardC)
	if !g.ActivateAbility(p, card, 0, c) {
		t.Fatal("cycling failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23: the cycled card's Cycled trigger did not fire", got)
	}
	if g.Card(card).Zone != engine.Graveyard {
		t.Errorf("cycled card zone = %v, want Graveyard", g.Card(card).Zone)
	}
}

// A battlefield watcher with ValidCard$ Card and FirstTime$ (Lightning Rift's
// shape): it sees any player's cycling, and FirstTime$ only the first of the
// turn.
func TestCycledWatcherFirstTimeFiresOncePerTurn(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.NewCard(scriptDef(t, "Test Watcher", "Enchantment",
		"T:Mode$ Cycled | ValidCard$ Card.YouOwn | FirstTime$ True | TriggerZones$ Battlefield | Execute$ TrigGain", gainThreeSVar), p, engine.Battlefield)
	c := engine.NewScriptedController()
	for i := 0; i < 2; i++ {
		g.Player(p).ManaPool.AddColorless(1)
		card := g.NewCard(scriptDef(t, "Test Cycler", "Creature Elf", "K:Cycling:1"), p, engine.Hand)
		g.NewCard(corpusCard(t, "Island"), p, engine.Library)
		c.QueuePayGeneric(mana.ShardC)
		if !g.ActivateAbility(p, card, 0, c) {
			t.Fatalf("cycling %d failed", i)
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23: FirstTime$ fires for the first cycling only", got)
	}
}

// Mode$ AbilityCast | ValidActivatingPlayer$ You | ValidSA$ Activated.
// !ManaAbility: a watcher fires when its controller activates a non-mana
// ability, and not for the opponent's.
func TestAbilityCastWatcherSeesItsControllersActivations(t *testing.T) {
	t.Parallel()

	for _, mine := range []bool{true, false} {
		g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
		g.SetTurnState(1, p, engine.Main1)
		g.NewCard(scriptDef(t, "Test Watcher", "Enchantment",
			"T:Mode$ AbilityCast | ValidActivatingPlayer$ You | ValidSA$ Activated.!ManaAbility | TriggerZones$ Battlefield | Execute$ TrigGain", gainThreeSVar), p, engine.Battlefield)
		owner := p
		if !mine {
			owner = other
		}
		pinger := g.NewCard(creatureDefWithAbility(t, "Test Pinger", "AB$ GainLife | Cost$ 0 | Defined$ You | LifeAmount$ 1"), owner, engine.Battlefield)
		g.Card(pinger).SummonSick = false
		sba(g)
		c := engine.NewScriptedController()
		if !g.ActivateAbility(owner, pinger, 0, c) {
			t.Fatalf("mine %v: activation failed", mine)
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		want := 20
		if mine {
			want = 24
		}
		if got := g.Player(p).Life; got != want {
			t.Errorf("mine %v: watcher's controller life = %d, want %d", mine, got, want)
		}
	}
}

// ValidSA$ Activated.Loyalty against an ability with a Planeswalker$ marker,
// and Activated.hasTapCost: the SpellAbility properties the 44 AbilityCast
// lines name.
func TestAbilityCastValidSAProperties(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		spec, abilityText string
		want              bool
	}{
		{"Activated.hasTapCost", "AB$ GainLife | Cost$ T | Defined$ You | LifeAmount$ 1", true},
		{"Activated.hasTapCost", "AB$ GainLife | Cost$ 0 | Defined$ You | LifeAmount$ 1", false},
		{"Activated.!hasTapCost", "AB$ GainLife | Cost$ 0 | Defined$ You | LifeAmount$ 1", true},
		{"Triggered", "AB$ GainLife | Cost$ 0 | Defined$ You | LifeAmount$ 1", false},
		{"Spell", "AB$ GainLife | Cost$ 0 | Defined$ You | LifeAmount$ 1", false},
	} {
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		g.SetTurnState(1, p, engine.Main1)
		g.NewCard(scriptDef(t, "Test Watcher", "Enchantment",
			"T:Mode$ AbilityCast | ValidSA$ "+tc.spec+" | TriggerZones$ Battlefield | Execute$ TrigGain", gainThreeSVar), p, engine.Battlefield)
		src := g.NewCard(creatureDefWithAbility(t, "Test Source", tc.abilityText), p, engine.Battlefield)
		g.Card(src).SummonSick = false
		sba(g)
		c := engine.NewScriptedController()
		if !g.ActivateAbility(p, src, 0, c) {
			t.Fatalf("%s: activation failed", tc.spec)
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		gained := g.Player(p).Life - 20
		if tc.want && gained < 3 || !tc.want && gained >= 3 {
			t.Errorf("ValidSA$ %s on %q: life gained %d, want trigger %v", tc.spec, tc.abilityText, gained, tc.want)
		}
	}
}

// Mode$ AttackerUnblocked | ValidCard$ Card.Self (37 real lines): fires for
// an attacker nobody blocked, and not for a blocked one.
func TestAttackerUnblockedFiresOnlyForUnblockedAttackers(t *testing.T) {
	t.Parallel()

	for _, blocked := range []bool{false, true} {
		g := newGame(t, "a", "b")
		a, b := g.Players()[0], g.Players()[1]
		g.SetTurnState(1, a, engine.Main1)
		attacker := g.NewCard(scriptDef(t, "Test Attacker", "Creature Elf",
			"T:Mode$ AttackerUnblocked | ValidCard$ Card.Self | Execute$ TrigGain", gainThreeSVar), a, engine.Battlefield)
		blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
		g.Card(attacker).SummonSick = false
		sba(g)
		ac := engine.NewScriptedController()
		ac.QueueAttackers([]engine.CardID{attacker})
		declareAttackers(t, g, ac)
		bc := engine.NewScriptedController()
		if blocked {
			bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attacker}})
		} else {
			bc.QueueBlocks(nil)
		}
		declareBlockers(t, g, bc)
		if err := g.ResolveStack(engine.NewRegistry(), bc); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		want := 23
		if blocked {
			want = 20
		}
		if got := g.Player(a).Life; got != want {
			t.Errorf("blocked %v: life = %d, want %d", blocked, got, want)
		}
	}
}

// Mode$ SpellCastOrCopy (31 real lines) fires for a cast spell, like
// SpellCast, with ValidActivatingPlayer$.
func TestSpellCastOrCopyFiresOnACast(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.NewCard(scriptDef(t, "Test Watcher", "Enchantment",
		"T:Mode$ SpellCastOrCopy | ValidCard$ Card | ValidActivatingPlayer$ You | TriggerZones$ Battlefield | Execute$ TrigGain", gainThreeSVar), p, engine.Battlefield)
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	g.Player(p).ManaPool.AddColorless(1)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardC)
	if !g.CastSpell(p, bears, c) {
		t.Fatal("CastSpell failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23", got)
	}
}

// Mode$ DiscardedAll | ValidPlayer$ You (22 real lines): one trigger for a
// batch of discards, with TriggerCount$Amount the batch size; FirstTime$ only
// for the first batch of the turn; and a plain Discarded watcher is
// untouched.
func TestDiscardedAllFiresOncePerBatchWithItsAmount(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.NewCard(scriptDef(t, "Test Watcher", "Enchantment",
		"T:Mode$ DiscardedAll | ValidPlayer$ You | TriggerZones$ Battlefield | Execute$ TrigGainN",
		"SVar:TrigGainN:DB$ GainLife | Defined$ You | LifeAmount$ X", "SVar:X:TriggerCount$Amount"), p, engine.Battlefield)
	for range 3 {
		g.NewCard(corpusCard(t, "Island"), p, engine.Hand)
	}
	c := engine.NewScriptedController()
	hand := g.Zone(engine.Hand, p).Cards()
	c.QueueDiscardChoice([]engine.CardID{hand[0], hand[1]})
	c.QueueCardOrder([]engine.CardID{hand[0], hand[1]})
	if err := resolveWith(t, g, p, c, "DB$ Discard | Defined$ You | NumCards$ 2 | Mode$ TgtChoose"); err != nil {
		t.Fatalf("resolve: %v", err)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22: one DiscardedAll trigger for a batch of two", got)
	}
}

// Trigger.checkActivationLimit: "ActivationLimit$ 1" is once a turn for every
// mode, here an ability-cast watcher seeing two activations; and it resets at
// the next turn (Card.resetActivationsPerTurn).
func TestTriggerActivationLimitFiresOnceATurn(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.NewCard(scriptDef(t, "Test Watcher", "Enchantment",
		"T:Mode$ AbilityCast | ValidActivatingPlayer$ You | ActivationLimit$ 1 | TriggerZones$ Battlefield | Execute$ TrigGain", gainThreeSVar), p, engine.Battlefield)
	pinger := g.NewCard(creatureDefWithAbility(t, "Test Pinger", "AB$ GainLife | Cost$ 0 | Defined$ You | LifeAmount$ 0"), p, engine.Battlefield)
	g.Card(pinger).SummonSick = false
	sba(g)
	c := engine.NewScriptedController()
	for i := 0; i < 2; i++ {
		if !g.ActivateAbility(p, pinger, 0, c) {
			t.Fatalf("activation %d failed", i)
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23: two activations, one trigger", got)
	}
}

// CR 603.2i: ResolvedLimit$ 1 counts resolutions. The trigger resolved for the
// first activation, so it does not trigger for the second.
func TestTriggerResolvedLimitCountsResolutions(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.NewCard(scriptDef(t, "Test Watcher", "Enchantment",
		"T:Mode$ AbilityCast | ValidActivatingPlayer$ You | ResolvedLimit$ 1 | TriggerZones$ Battlefield | Execute$ TrigGain", gainThreeSVar), p, engine.Battlefield)
	pinger := g.NewCard(creatureDefWithAbility(t, "Test Pinger", "AB$ GainLife | Cost$ 0 | Defined$ You | LifeAmount$ 0"), p, engine.Battlefield)
	g.Card(pinger).SummonSick = false
	sba(g)
	c := engine.NewScriptedController()
	for i := 0; i < 2; i++ {
		if !g.ActivateAbility(p, pinger, 0, c) {
			t.Fatalf("activation %d failed", i)
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23: two activations, the trigger resolved once", got)
	}
}

// CR 511.3 / PhaseHandler.onPhaseEnd: combat ends as the end of combat step
// ends, so a Mode$ Phase | Phase$ EndCombat trigger still sees who attacked
// (IsPresent$ Card.Self+attackedThisCombat, 14 real lines).
func TestEndOfCombatTriggerSeesTheCombat(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a := g.Players()[0]
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(scriptDef(t, "Test Attacker", "Creature Elf",
		"T:Mode$ Phase | Phase$ EndCombat | ValidPlayer$ Player | IsPresent$ Card.Self+attackedThisCombat | TriggerZones$ Battlefield | Execute$ TrigGain",
		gainThreeSVar), a, engine.Battlefield)
	g.Card(attacker).SummonSick = false
	sba(g)
	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)
	g.SetTurnState(1, a, engine.CombatDamage)
	g.AdvancePhase(bc) // -> CombatEnd: its trigger sees the combat
	if err := g.ResolveStack(engine.NewRegistry(), bc); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(a).Life; got != 23 {
		t.Errorf("life = %d, want 23: the end of combat trigger saw its attacker", got)
	}
}
