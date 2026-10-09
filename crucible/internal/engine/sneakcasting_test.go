package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// sneakCreatureDef is a 3/3 creature costing {3}{W} with the given Sneak
// keyword line.
func sneakCreatureDef(t *testing.T, keyword string) *compile.Card {
	t.Helper()
	def := creatureDefPTKeywords(t, "3", "3", keyword)
	def.Faces[0].ManaCost = mana.MustParse("3 W")
	return def
}

// sneakCombat seats an attacker for p, declares it attacking other, and
// moves to the declare blockers step with blockers declared (blocked by a
// fresh 2/2 of other's when blocked). It returns the attacker.
func sneakCombat(t *testing.T, g *engine.Game, p, other engine.PlayerID, blocked bool) engine.CardID {
	t.Helper()
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, c)
	g.SetTurnState(1, p, engine.DeclareBlockers)
	if blocked {
		blocker := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		c.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attacker}})
	}
	declareBlockers(t, g, c)
	return attacker
}

// matches reports whether card matches spec for p.
func matches(g *engine.Game, card engine.CardID, spec string, p engine.PlayerID) bool {
	return engine.Matches(g, g.Card(card), valid.Parse(spec), p, card)
}

// TestSneakCastsACreatureTappedAndAttacking proves the dominant shape: in
// the declare blockers step a creature is cast for its Sneak mana by
// returning an unblocked attacker; it leaves combat and goes to hand, and
// the sneaked creature enters tapped, attacking the same player, unblocked
// (PermanentEffect.java:33-35, 55-63), with Card.sneaked set.
func TestSneakCastsACreatureTappedAndAttacking(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	attacker := sneakCombat(t, g, p, other, false)
	if !matches(g, attacker, "Creature.attacking+unblocked", p) {
		t.Fatal("the attacker is not unblocked once blockers are declared")
	}
	ninja := g.NewCard(sneakCreatureDef(t, "Sneak:W"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueReturnChoice([]engine.CardID{attacker})
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, ninja, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	if g.Card(attacker).Zone != engine.Hand {
		t.Errorf("returned attacker is in %v, want the hand", g.Card(attacker).Zone)
	}
	for _, id := range g.Attackers() {
		if id == attacker {
			t.Error("the returned attacker is still attacking")
		}
	}
	if !matches(g, ninja, "Card.sneaked", p) {
		t.Error("the spell on the stack is not sneaked")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	n := g.Card(ninja)
	if n.Zone != engine.Battlefield || !n.Tapped {
		t.Fatalf("sneaked creature zone %v tapped %v, want the battlefield, tapped", n.Zone, n.Tapped)
	}
	if got := g.AttackTarget(ninja); got != engine.PlayerEntity(other) {
		t.Errorf("sneaked creature attacks %v, want the returned attacker's defender", got)
	}
	if !matches(g, ninja, "Creature.attacking+unblocked+sneaked", p) {
		t.Error("the sneaked creature is not an unblocked, sneaked attacker")
	}
}

// TestSneakIsOfferedOnlyInTheDeclareBlockersStep proves the timing
// (SpellAbilityRestriction.java:312-316) and Combat.isUnblocked's null
// state: before blockers are declared no attacker is unblocked, and outside
// the declare blockers step a creature with Sneak is cast only normally.
func TestSneakIsOfferedOnlyInTheDeclareBlockersStep(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, c)
	g.SetTurnState(1, p, engine.DeclareAttackers)
	if matches(g, attacker, "Creature.unblocked", p) {
		t.Error("an attacker is unblocked before blockers are declared")
	}
	ninja := g.NewCard(sneakCreatureDef(t, "Sneak:W"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 1)
	if g.CastSpell(p, ninja, c) {
		t.Error("a Sneak creature was cast in the declare attackers step")
	}
}

// TestSneakIsNotOfferedPastTheDeclareBlockersStep isolates the step check:
// an attacker still unblocked in the combat damage step does not make Sneak
// castable there.
func TestSneakIsNotOfferedPastTheDeclareBlockersStep(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	attacker := sneakCombat(t, g, p, other, false)
	g.SetTurnState(1, p, engine.CombatDamage)
	if !matches(g, attacker, "Creature.unblocked", p) {
		t.Fatal("the attacker is not unblocked")
	}
	ninja := g.NewCard(sneakCreatureDef(t, "Sneak:W"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 1)
	if g.CastSpell(p, ninja, engine.NewScriptedController()) {
		t.Error("a Sneak creature was cast in the combat damage step")
	}
}

// TestSneakedCreatureAttacksTheReturnedAttackersPlaneswalker proves the
// defender is what the returned attacker attacked, a planeswalker included,
// and that the planeswalker gone by resolution is the refused shape.
func TestSneakedCreatureAttacksTheReturnedAttackersPlaneswalker(t *testing.T) {
	t.Parallel()
	for _, gone := range []bool{false, true} {
		g, p, other := newTwoPlayerGame(t)
		attacker := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		pw := g.NewCard(planeswalkerDefLoyalty(t, "3"), other, engine.Battlefield)
		c := engine.NewScriptedController()
		c.QueueAttackers([]engine.CardID{attacker})
		c.QueueAttackTarget(engine.CardEntity(pw))
		declareAttackers(t, g, c)
		g.SetTurnState(1, p, engine.DeclareBlockers)
		declareBlockers(t, g, c)
		ninja := g.NewCard(sneakCreatureDef(t, "Sneak:W"), p, engine.Hand)
		c.QueueReturnChoice([]engine.CardID{attacker})
		g.Player(p).ManaPool.Add(mana.White, 1)
		if !g.CastSpell(p, ninja, c) {
			t.Fatalf("CastSpell failed: %v", g.TakePendingError())
		}
		if gone {
			g.Move(pw, engine.Graveyard, other)
		}
		err := g.ResolveStack(engine.NewRegistry(), c)
		if gone {
			if err == nil {
				t.Error("a sneaked creature resolved with its planeswalker defender gone")
			}
			continue
		}
		if err != nil {
			t.Fatal(err)
		}
		if got := g.AttackTarget(ninja); got != engine.CardEntity(pw) {
			t.Errorf("sneaked creature attacks %v, want the planeswalker", got)
		}
	}
}

// TestSneakNeedsAnUnblockedAttacker proves the Return part's payability: a
// blocked attacker cannot be returned, so Sneak is not offered.
func TestSneakNeedsAnUnblockedAttacker(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	attacker := sneakCombat(t, g, p, other, true)
	if matches(g, attacker, "Creature.unblocked", p) {
		t.Error("a blocked attacker is unblocked")
	}
	ninja := g.NewCard(sneakCreatureDef(t, "Sneak:W"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 1)
	if g.CastSpell(p, ninja, engine.NewScriptedController()) {
		t.Error("Sneak was cast with only a blocked attacker")
	}
}

// TestSneakIsNotOfferedForAShapeThisPortDoesNotRead proves a Sneak keyword
// with a third field (Java's ValidAfterStack$) or an X cost is never cast
// for a guessed cost (GO-7).
func TestSneakIsNotOfferedForAShapeThisPortDoesNotRead(t *testing.T) {
	t.Parallel()
	for _, kw := range []string{"Sneak:W:Card.Self", "Sneak:X W", "Sneak:"} {
		g, p, other := newTwoPlayerGame(t)
		sneakCombat(t, g, p, other, false)
		ninja := g.NewCard(sneakCreatureDef(t, kw), p, engine.Hand)
		g.Player(p).ManaPool.Add(mana.White, 4)
		if g.CastSpell(p, ninja, engine.NewScriptedController()) {
			t.Errorf("%q: a Sneak creature was cast", kw)
		}
	}
}

// TestSneakOnAnInstantIsAChoiceBesideItsNormalCast proves Sneak is its own
// spell beside the normal one (inst.addSpellAbility): an instant, castable
// either way in the declare blockers step, asks ChooseOption; the normal
// cast returns nothing and is not sneaked.
func TestSneakOnAnInstantIsAChoiceBesideItsNormalCast(t *testing.T) {
	t.Parallel()
	for _, pick := range []int{0, 1} {
		g, p, other := newTwoPlayerGame(t)
		attacker := sneakCombat(t, g, p, other, false)
		def := spellDefWith(t, "Sneaky Gain", "Instant", "1 W", "SP$ GainLife | Defined$ You | LifeAmount$ 3")
		def.Faces[0].Keywords = []string{"Sneak:W"}
		spell := g.NewCard(def, p, engine.Hand)
		c := engine.NewScriptedController()
		c.QueueOption(pick)
		if pick == 0 {
			c.QueuePayGeneric(mana.ShardW)
			g.Player(p).ManaPool.Add(mana.White, 2)
		} else {
			c.QueueReturnChoice([]engine.CardID{attacker})
			g.Player(p).ManaPool.Add(mana.White, 1)
		}
		if !g.CastSpell(p, spell, c) {
			t.Fatalf("pick %d: CastSpell failed: %v", pick, g.TakePendingError())
		}
		if returned := g.Card(attacker).Zone == engine.Hand; returned != (pick == 1) {
			t.Errorf("pick %d: attacker returned = %v", pick, returned)
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatal(err)
		}
		if got := g.Player(p).Life; got != 23 {
			t.Errorf("pick %d: life = %d, want 23", pick, got)
		}
	}
}

// TestSneakReturnChoiceMustBeAnUnblockedAttacker proves a controller answer
// naming no candidate fails the cast with a pending error (GO-7).
func TestSneakReturnChoiceMustBeAnUnblockedAttacker(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	sneakCombat(t, g, p, other, false)
	bystander := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	ninja := g.NewCard(sneakCreatureDef(t, "Sneak:W"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueReturnChoice([]engine.CardID{bystander})
	g.Player(p).ManaPool.Add(mana.White, 1)
	if g.CastSpell(p, ninja, c) {
		t.Fatal("CastSpell succeeded returning a creature that is not attacking")
	}
	if err := g.TakePendingError(); err == nil || !strings.Contains(err.Error(), "Sneak") {
		t.Errorf("pending error = %v, want the Sneak return error", err)
	}
	if matches(g, ninja, "Card.sneaked", p) {
		t.Error("a failed Sneak cast left the card sneaked")
	}
}

// TestSneakedCreatureWhoseDefenderLeftIsAnError proves the shape this port
// does not model (Java's CR 506.4c "<Nothing>" defender) is refused before
// the creature moves.
func TestSneakedCreatureWhoseDefenderLeftIsAnError(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	attacker := sneakCombat(t, g, p, other, false)
	ninja := g.NewCard(sneakCreatureDef(t, "Sneak:W"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueReturnChoice([]engine.CardID{attacker})
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, ninja, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	g.Player(other).Lost = true
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "no longer attackable") {
		t.Fatalf("ResolveStack = %v, want the defender error", err)
	}
	if g.Card(ninja).Zone == engine.Battlefield {
		t.Error("the sneaked creature entered anyway")
	}
}

// TestSneakedNoncreaturePermanentOnlyEntersTapped proves PermanentEffect's
// two halves split: any sneaked permanent enters tapped, only a creature
// joins combat.
func TestSneakedNoncreaturePermanentOnlyEntersTapped(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	attacker := sneakCombat(t, g, p, other, false)
	def := creatureDefPTKeywords(t, "1", "1", "Sneak:W")
	def.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Artifact")
	def.Faces[0].Power, def.Faces[0].Toughness = "", ""
	relic := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueReturnChoice([]engine.CardID{attacker})
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, relic, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if r := g.Card(relic); r.Zone != engine.Battlefield || !r.Tapped {
		t.Errorf("artifact zone %v tapped %v, want the battlefield, tapped", r.Zone, r.Tapped)
	}
	if len(g.Attackers()) != 0 {
		t.Errorf("attackers = %v, want none", g.Attackers())
	}
}

// TestSneakCombatStateSurvivesClone proves the blocked-status record is
// copied, not shared: a clone sees the attacker unblocked, and a Sneak cast
// on the clone leaves the original's combat as it was.
func TestSneakCombatStateSurvivesClone(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	attacker := sneakCombat(t, g, p, other, false)
	ninja := g.NewCard(sneakCreatureDef(t, "Sneak:W"), p, engine.Hand)
	clone := g.Clone()
	if !matches(clone, attacker, "Creature.unblocked", p) {
		t.Fatal("the clone's attacker is not unblocked")
	}
	c := engine.NewScriptedController()
	c.QueueReturnChoice([]engine.CardID{attacker})
	clone.Player(p).ManaPool.Add(mana.White, 1)
	if !clone.CastSpell(p, ninja, c) {
		t.Fatalf("CastSpell on the clone failed: %v", clone.TakePendingError())
	}
	if err := clone.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if got := g.Attackers(); len(got) != 1 || got[0] != attacker {
		t.Errorf("original attackers = %v, want only %v", got, attacker)
	}
	if matches(g, ninja, "Card.sneaked", p) {
		t.Error("the original's card is sneaked")
	}
}
