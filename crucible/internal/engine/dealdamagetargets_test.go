package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// DealDamage's own ValidTgts$ shape (CR 115's "any target"): a real
// Lightning Bolt, cast at a creature, marks damage the same way combat
// already does -- dealPermanentDamage (combatdamage.go) shared by both
// callers.
func TestDealDamageValidTgtsAnyDamagesCreature(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).ManaPool.Add(mana.Red, 1)
	target := g.NewCard(creatureDefPT(t, "4", "4"), p, engine.Battlefield)
	bolt := g.NewCard(instantDefWithAbility(t, "Lightning Bolt", "R", "SP$ DealDamage | ValidTgts$ Any | NumDmg$ 3"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})

	if !g.CastSpell(p, bolt, c) {
		t.Fatal("CastSpell failed casting an Any-targeted Instant")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(target).Damage.Marked; got != 3 {
		t.Errorf("target damage = %d, want 3", got)
	}
}

// A planeswalker target loses loyalty counters, not marked damage (CR
// 120.3c) -- dealPermanentDamage's own type-gated branch, reached through
// ValidTgts$ Any for the first time outside combat.
func TestDealDamageValidTgtsAnyDamagesPlaneswalker(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).ManaPool.Add(mana.Red, 1)
	pw := g.NewCard(planeswalkerDefLoyalty(t, "5"), p, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 5)
	bolt := g.NewCard(instantDefWithAbility(t, "Lightning Bolt", "R", "SP$ DealDamage | ValidTgts$ Any | NumDmg$ 3"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(pw)})

	if !g.CastSpell(p, bolt, c) {
		t.Fatal("CastSpell failed casting an Any-targeted Instant at a planeswalker")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(pw).Counters.Count(engine.Loyalty); got != 2 {
		t.Errorf("loyalty = %d, want 2 (5 - 3)", got)
	}
}

// A planeswalker driven to zero loyalty dies to the state-based action
// (CR 704.5i) -- ResolveStack's own CheckStateBasedActions call, unchanged.
func TestDealDamageValidTgtsAnyKillsPlaneswalkerAtZeroLoyalty(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.Player(p).ManaPool.Add(mana.Red, 1)
	pw := g.NewCard(planeswalkerDefLoyalty(t, "3"), p, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 3)
	bolt := g.NewCard(instantDefWithAbility(t, "Lightning Bolt", "R", "SP$ DealDamage | ValidTgts$ Any | NumDmg$ 3"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(pw)})

	if !g.CastSpell(p, bolt, c) {
		t.Fatal("CastSpell failed casting an Any-targeted Instant at a planeswalker")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(pw).Zone != engine.Graveyard {
		t.Errorf("planeswalker zone = %v, want Graveyard", g.Card(pw).Zone)
	}
}

// A Battle target loses defense counters (CR 121.5), the identical
// type-gated branch as a planeswalker's own loyalty.
func TestDealDamageValidTgtsAnyDamagesBattle(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).ManaPool.Add(mana.Red, 1)
	battle := g.NewCard(battleDefDefense(t, "5"), p, engine.Battlefield)
	g.Card(battle).Counters.Add(engine.Defense, 5)
	bolt := g.NewCard(instantDefWithAbility(t, "Lightning Bolt", "R", "SP$ DealDamage | ValidTgts$ Any | NumDmg$ 3"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(battle)})

	if !g.CastSpell(p, bolt, c) {
		t.Fatal("CastSpell failed casting an Any-targeted Instant at a Battle")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(battle).Counters.Count(engine.Defense); got != 2 {
		t.Errorf("defense = %d, want 2 (5 - 3)", got)
	}
}

// A player target loses life -- the union fix (targetCandidates,
// targeting.go) means ValidTgts$ Any offers a player as a candidate at all,
// not just creatures/planeswalkers/Battles.
func TestDealDamageValidTgtsAnyDamagesPlayer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.Player(p).ManaPool.Add(mana.Red, 1)
	bolt := g.NewCard(instantDefWithAbility(t, "Lightning Bolt", "R", "SP$ DealDamage | ValidTgts$ Any | NumDmg$ 3"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})

	if !g.CastSpell(p, bolt, c) {
		t.Fatal("CastSpell failed casting an Any-targeted Instant at a player")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 17 {
		t.Errorf("life = %d, want 17 (20 - 3)", got)
	}
}

// ValidTgts$ Any reaches DealDamage through ActivateAbility too, not just a
// cast Instant -- pushTriggeredAbilities' own resolveTargets call, the
// identical path both share.
func TestDealDamageValidTgtsAnyThroughActivatedAbility(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	target := g.NewCard(creatureDefPT(t, "4", "4"), p, engine.Battlefield)
	source := g.NewCard(creatureDefWithAbility(t, "Prodigal Pyromancer", "AB$ DealDamage | Cost$ T | ValidTgts$ Any | NumDmg$ 1"), p, engine.Battlefield)
	g.Card(source).SummonSick = false
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})

	if !g.ActivateAbility(p, source, 0, c) {
		t.Fatal("ActivateAbility failed activating an Any-targeted ability")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(target).Damage.Marked; got != 1 {
		t.Errorf("target damage = %d, want 1", got)
	}
}

// A card target that left the battlefield between targeting and resolution
// is skipped without erroring (DamageDealEffect.java's own per-target
// liveness check, dealdamageeffect.go) -- a SpellCast trigger destroying
// the target before the DealDamage spell resolves.
func TestDealDamageValidTgtsAnySkipsTargetThatLeftBattlefield(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).ManaPool.Add(mana.Red, 1)
	target := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	bolt := g.NewCard(instantDefWithAbility(t, "Lightning Bolt", "R", "SP$ DealDamage | ValidTgts$ Any | NumDmg$ 3"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})

	if !g.CastSpell(p, bolt, c) {
		t.Fatal("CastSpell failed casting an Any-targeted Instant")
	}
	g.Move(target, engine.Graveyard, p)

	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(target).Damage.Marked; got != 0 {
		t.Errorf("damage marked on a card that already left the battlefield = %d, want 0", got)
	}
}
