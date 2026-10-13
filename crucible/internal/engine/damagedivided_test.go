package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// arcLightning is Arc Lightning's own line: 3 damage divided as you choose
// among one, two, or three targets.
const arcLightning = "SP$ DealDamage | ValidTgts$ Any | NumDmg$ 3 | TargetMin$ 1 | TargetMax$ 3 | DividedAsYouChoose$ 3"

func marked(g *engine.Game, id engine.CardID) int { return g.Card(id).Damage.Marked }

// TestDealDamageDividedSplitsBetweenACreatureAndAPlayer proves
// DamageDealEffect.java:248: each target takes the share announced with it
// (CR 601.2d), a player target too, NumDmg$ unread for either.
func TestDealDamageDividedSplitsBetweenACreatureAndAPlayer(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	bear := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bear), engine.PlayerEntity(other)})
	c.QueueDivision([]int{2, 1})
	if err := castCounterSpell(t, g, p, c, arcLightning); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if marked(g, bear) != 2 || g.Player(other).Life != 19 {
		t.Errorf("creature damage = %d, other's life = %d, want 2 and 19", marked(g, bear), g.Player(other).Life)
	}
}

// TestDealDamageDividedOneTargetTakesItAll proves the forced division
// (PlayerControllerHuman.java:2495-2497): one target takes the whole amount,
// nothing asked (no division queued).
func TestDealDamageDividedOneTargetTakesItAll(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if err := castCounterSpell(t, g, p, c, arcLightning); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(other).Life != 17 {
		t.Errorf("other's life = %d, want 17", g.Player(other).Life)
	}
}

// TestDealDamageDividedDroppedTargetTakesItsShareWithIt proves CR 608.2b
// over a division (TargetChoices.java:71-82): the target gone before
// resolution takes its share with it, and the one left is dealt only its own
// share, not the whole amount.
func TestDealDamageDividedDroppedTargetTakesItsShareWithIt(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "4", "4"), other, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "4", "4"), other, engine.Battlefield)
	spell := g.NewCard(spellDefWith(t, "Forked Lightning", "Sorcery", "G",
		"SP$ DealDamage | ValidTgts$ Creature | NumDmg$ 4 | TargetMin$ 1 | TargetMax$ 3 | DividedAsYouChoose$ 4"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.CardEntity(b)})
	c.QueueDivision([]int{3, 1})
	g.Player(p).ManaPool.Add(mana.Green, 1)
	if !g.CastSpell(p, spell, c) {
		t.Fatal("CastSpell failed")
	}
	g.Move(a, engine.Graveyard, other)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if marked(g, b) != 1 {
		t.Errorf("damage on the target left = %d, want its own share 1", marked(g, b))
	}
}

// TestDealDamageDividedEachSourceDealsEachShare proves the per-source loop
// (DamageDealEffect.java:217-268): with two DamageSource$ cards each target
// is dealt its share by each of them.
func TestDealDamageDividedEachSourceDealsEachShare(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	a := g.NewCard(creatureDefPT(t, "9", "9"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.PlayerEntity(other)})
	c.QueueDivision([]int{2, 1})
	if err := castCounterSpell(t, g, p, c, arcLightning+" | DamageSource$ Valid Creature.YouCtrl+powerEQ1"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if marked(g, a) != 4 || g.Player(other).Life != 18 {
		t.Errorf("creature damage = %d, other's life = %d, want 4 and 18 (two sources, 2/1 each)", marked(g, a), g.Player(other).Life)
	}
}

// TestDealDamageDividedShareMeetsExcessDamage proves ExcessDamage$ reads the
// target's own share (internalDamageDeal, DamageDealEffect.java:275-302): a
// 1/1 given 3 takes 1, its controller the other 2.
func TestDealDamageDividedShareMeetsExcessDamage(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	small := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	big := g.NewCard(creatureDefPT(t, "5", "5"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(small), engine.CardEntity(big)})
	c.QueueDivision([]int{3, 1})
	err := castCounterSpell(t, g, p, c, "SP$ DealDamage | ValidTgts$ Creature | NumDmg$ 4 | TargetMin$ 1 | TargetMax$ 2 | DividedAsYouChoose$ 4 | ExcessDamage$ TargetedController")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(other).Life != 18 || marked(g, big) != 1 {
		t.Errorf("other's life = %d, big damage = %d, want 18 and 1", g.Player(other).Life, marked(g, big))
	}
}

// TestDealDamageDividedBadDivisionDealsNothing proves a division that does not
// add up fails the spell when it resolves, dealing nothing: DealDamage now
// reads Ability.targetsErr.
func TestDealDamageDividedBadDivisionDealsNothing(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "4", "4"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.PlayerEntity(other)})
	c.QueueDivision([]int{1, 1})
	err := castCounterSpell(t, g, p, c, arcLightning)
	if err == nil || !strings.Contains(err.Error(), "DealDamage") || !strings.Contains(err.Error(), "add up to 2, want 3") {
		t.Fatalf("err = %v, want DealDamage failing on the bad division", err)
	}
	if marked(g, a) != 0 || g.Player(other).Life != 20 {
		t.Errorf("damage = %d, life = %d, want none dealt", marked(g, a), g.Player(other).Life)
	}
}

// TestDealDamageDividedShapesItCannotReadFailLoudly proves the divided shapes
// this port does not resolve fail by name before any damage: DividedUpTo$
// (a further "how much" question at targeting), no division announced for a
// target, a division without targets of its own (Java deals NumDmg$ to each),
// and Radiance$ (Java's radiant cards take the last target's share).
func TestDealDamageDividedShapesItCannotReadFailLoudly(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	err := castCounterSpell(t, g, p, engine.NewScriptedController(), arcLightning+" | DividedUpTo$ True")
	if err == nil || !strings.Contains(err.Error(), "DividedUpTo$") {
		t.Errorf("up to: err = %v, want DividedUpTo$ named", err)
	}
	if g.Player(other).Life != 20 {
		t.Errorf("up to: other's life = %d, want 20", g.Player(other).Life)
	}

	cases := []struct {
		name, line, want string
		targets          bool
	}{
		{"no division announced", "DB$ DealDamage | ValidTgts$ Player | NumDmg$ 2 | DividedAsYouChoose$ 2", "no division was announced for player", true},
		{"untargeted", "DB$ DealDamage | Defined$ Opponent | NumDmg$ 2 | DividedAsYouChoose$ 2", "without its own targets", false},
		{"radiance", "DB$ DealDamage | ValidTgts$ Player | NumDmg$ 2 | DividedAsYouChoose$ 2 | Radiance$ True", "with Radiance$", true},
	}
	for _, tc := range cases {
		g, p, other := newTwoPlayerGame(t)
		var targets []engine.EntityID
		if tc.targets {
			targets = []engine.EntityID{engine.PlayerEntity(other)}
		}
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), targets, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want %q", tc.name, err, tc.want)
		}
		if g.Player(other).Life != 20 {
			t.Errorf("%s: other's life = %d, want 20", tc.name, g.Player(other).Life)
		}
	}

	g, p, other = newTwoPlayerGame(t)
	bear := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	_, err = resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(bear)},
		"DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 2 | DividedAsYouChoose$ 2")
	if err == nil || !strings.Contains(err.Error(), "no division was announced for card") {
		t.Errorf("card with no division: err = %v, want the card named", err)
	}
	if marked(g, bear) != 0 {
		t.Errorf("card with no division: damage = %d, want 0", marked(g, bear))
	}
}

// TestPreventDamageDividedGivesEachTargetItsShare proves
// DamagePreventEffect.java:105: Remedy's shields are each target's own
// share, so later damage is prevented by that much and no more.
func TestPreventDamageDividedGivesEachTargetItsShare(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	bear := g.NewCard(creatureDefPT(t, "5", "5"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bear), engine.PlayerEntity(p)})
	c.QueueDivision([]int{3, 2})
	if err := castCounterSpell(t, g, p, c, "SP$ PreventDamage | ValidTgts$ Any | Amount$ 5 | TargetMin$ 0 | TargetMax$ 5 | DividedAsYouChoose$ 5"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ DealDamage | Defined$ You | NumDmg$ 4")
	hit := engine.NewScriptedController()
	hit.QueueTargets([]engine.EntityID{engine.CardEntity(bear)})
	resolveLine(t, g, p, hit, "DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 4")
	if g.Player(p).Life != 18 || marked(g, bear) != 1 {
		t.Errorf("life = %d, creature damage = %d, want 18 and 1 (2 and 3 prevented)", g.Player(p).Life, marked(g, bear))
	}
}

// TestPreventDamageDividedShapesItCannotReadFailLoudly proves PreventDamage's
// divided refusals and its bad-division failure: nothing is shielded.
func TestPreventDamageDividedShapesItCannotReadFailLoudly(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.PlayerEntity(p)})
	c.QueueDivision([]int{4, 4})
	err := castCounterSpell(t, g, p, c, "SP$ PreventDamage | ValidTgts$ Any | Amount$ 5 | TargetMin$ 0 | TargetMax$ 5 | DividedAsYouChoose$ 5")
	if err == nil || !strings.Contains(err.Error(), "PreventDamage") || !strings.Contains(err.Error(), "add up to 8, want 5") {
		t.Errorf("bad division: err = %v, want PreventDamage failing on it", err)
	}

	g, p, _ = newTwoPlayerGame(t)
	_, err = resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.PlayerEntity(p)},
		"DB$ PreventDamage | ValidTgts$ Player | Amount$ 2 | DividedAsYouChoose$ 2")
	if err == nil || !strings.Contains(err.Error(), "no division was announced") {
		t.Errorf("no division: err = %v, want the missing division", err)
	}
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ DealDamage | Defined$ You | NumDmg$ 2")
	if g.Player(p).Life != 18 {
		t.Errorf("life = %d, want 18: no shield was made", g.Player(p).Life)
	}
}
