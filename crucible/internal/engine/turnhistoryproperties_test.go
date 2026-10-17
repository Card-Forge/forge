package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// historyMatches reads one valid string against id from you's point of view,
// with no source card.
func historyMatches(g *engine.Game, id engine.CardID, spec string, you engine.PlayerID) bool {
	return engine.Matches(g, g.Card(id), valid.Parse(spec), you, engine.NoCard)
}

// TestCombatHistoryPropertiesReadTheBlockAndTheDamage covers the properties
// Avenging Arrow, Fatal Blow, Sizzling Barrage, Glyph of Delusion and You
// Cannot Pass! target with (CardProperty.java:1144-1210, :1613-1646): after a
// blocked combat, the attacker was blocked, both dealt and were dealt damage,
// the blocker blocked; an unblocked attacker dealt damage to its defender.
func TestCombatHistoryPropertiesReadTheBlockAndTheDamage(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	attacker := g.NewCard(creatureDef(t), p, engine.Battlefield)
	free := g.NewCard(creatureDef(t), p, engine.Battlefield)
	wall := g.NewCard(copyTestDef(t, "Test Wall", "Creature Wall", "1", "4"), other, engine.Battlefield)
	idle := g.NewCard(creatureDef(t), other, engine.Battlefield)

	for _, spec := range []string{
		"Creature.dealtDamageThisTurn", "Creature.wasDealtDamageThisTurn", "Creature.blockedThisTurn",
		"Creature.blockedByThisTurn", "Creature.dealtDamageToYouThisTurn",
	} {
		if historyMatches(g, attacker, spec, other) || historyMatches(g, wall, spec, other) {
			t.Errorf("%s matched before any combat", spec)
		}
	}

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker, free})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: wall, Attacker: attacker}})
	declareBlockers(t, g, bc)
	g.DealCombatDamage(engine.NewScriptedController())

	for _, tc := range []struct {
		card engine.CardID
		spec string
		you  engine.PlayerID
		want bool
	}{
		{attacker, "Creature.dealtDamageThisTurn", p, true},
		{wall, "Creature.dealtDamageThisTurn", p, true},
		{idle, "Creature.dealtDamageThisTurn", p, false},
		{attacker, "Creature.wasDealtDamageThisTurn", p, true},
		{wall, "Creature.wasDealtDamageThisTurn", p, true},
		{free, "Creature.wasDealtDamageThisTurn", p, false},
		{wall, "Creature.blockedThisTurn", p, true},
		{attacker, "Creature.blockedThisTurn", p, false},
		{attacker, "Creature.blockedByThisTurn", p, true},
		{attacker, "Creature.gotBlockedThisTurn", p, true},
		{free, "Creature.gotBlockedThisTurn", p, false},
		{wall, "Creature.blockedValidThisTurn Creature.Elf", p, true},
		{wall, "Creature.blockedValidThisTurn Zombie", p, false},
		{attacker, "Creature.blockedByValidThisTurn Wall", p, true},
		{attacker, "Creature.blockedByValidThisTurn Creature.Legendary", p, false},
		// The Defined$ fallback (Targeted) needs an ability in context.
		{attacker, "Creature.blockedByValidThisTurn Targeted", p, false},
		{free, "Creature.dealtDamageToYouThisTurn", other, true},
		{free, "Creature.dealtDamageToYouThisTurn", p, false},
		{attacker, "Creature.dealtDamageToYouThisTurn", other, false},
	} {
		if got := historyMatches(g, tc.card, tc.spec, tc.you); got != tc.want {
			t.Errorf("%s on card %d (you %d) = %v, want %v", tc.spec, tc.card, tc.you, got, tc.want)
		}
	}

	// A regeneration heals the damage, not the record of it
	// (Card.healDamage, Card.java:6181-6185).
	g.Card(wall).Damage.Heal()
	if !historyMatches(g, wall, "Creature.wasDealtDamageThisTurn", p) {
		t.Error("a healed creature lost wasDealtDamageThisTurn")
	}

	clone := g.Clone()
	c := engine.NewScriptedController()
	for g.ActivePhase() != engine.Untap {
		g.AdvancePhase(c)
	}
	for _, spec := range []string{
		"Creature.dealtDamageThisTurn", "Creature.wasDealtDamageThisTurn", "Creature.blockedThisTurn",
		"Creature.blockedByThisTurn", "Creature.blockedValidThisTurn Creature",
	} {
		if historyMatches(g, attacker, spec, p) || historyMatches(g, wall, spec, p) {
			t.Errorf("%s survived the cleanup", spec)
		}
	}
	if !historyMatches(clone, wall, "Creature.blockedThisTurn", p) {
		t.Error("the clone lost the blocker's record when the original's turn ended")
	}
}

// TestBlockedThisTurnEndsWithTheObject: a blocker that leaves the battlefield
// is a new object (CR 400.7) with no record.
func TestBlockedThisTurnEndsWithTheObject(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	attacker := g.NewCard(creatureDef(t), p, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "0", "5"), other, engine.Battlefield)
	declareOneAttackerAndBlocker(t, g, attacker, blocker)
	if !historyMatches(g, blocker, "Creature.blockedThisTurn", other) {
		t.Fatal("the blocker did not block")
	}
	g.Move(blocker, engine.Hand, other)
	g.Move(blocker, engine.Battlefield, other)
	if historyMatches(g, blocker, "Creature.blockedThisTurn", other) {
		t.Error("blockedThisTurn survived a zone change")
	}
}

// TestThisTurnEnteredReadsTheZoneEntry covers Cradle to Grave's and Reenact the
// Crime's ThisTurnEntered (CardProperty.java:969-986): a card that moved into
// its zone this turn, not one only placed there, and for the zone form the
// source controller's zone.
func TestThisTurnEnteredReadsTheZoneEntry(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	placed := g.NewCard(creatureDef(t), p, engine.Battlefield)
	cast := g.NewCard(creatureDef(t), p, engine.Hand)
	mustCastAndResolve(t, g, p, c, cast)
	dead := g.NewCard(creatureDef(t), other, engine.Battlefield)
	g.Move(dead, engine.Graveyard, other)

	for _, tc := range []struct {
		card engine.CardID
		spec string
		you  engine.PlayerID
		want bool
	}{
		{placed, "Creature.ThisTurnEntered", p, false},
		{cast, "Creature.ThisTurnEntered", p, true},
		{dead, "Card.ThisTurnEntered", p, true},
		{dead, "Card.ThisTurnEnteredGraveyard", other, true},
		{dead, "Card.ThisTurnEnteredGraveyard", p, false},
		{dead, "Card.ThisTurnEnteredYourGraveyard", other, true},
		{dead, "Card.ThisTurnEnteredHand", other, false},
		{dead, "Card.ThisTurnEnteredNowhere", other, false},
		{cast, "Creature.!ThisTurnEntered", p, false},
	} {
		if got := historyMatches(g, tc.card, tc.spec, tc.you); got != tc.want {
			t.Errorf("%s on card %d (you %d) = %v, want %v", tc.spec, tc.card, tc.you, got, tc.want)
		}
	}

	for g.ActivePhase() != engine.Untap {
		g.AdvancePhase(c)
	}
	if historyMatches(g, cast, "Creature.ThisTurnEntered", p) {
		t.Error("ThisTurnEntered still holds on the next turn")
	}
}

// TestEnteredSinceYourLastTurnReadsTheLastTurnNumber is Premature Burial's
// target (CardProperty.java:951-954, Player.java:2514-2519): entered after the
// source controller's last turn ended.
func TestEnteredSinceYourLastTurnReadsTheLastTurnNumber(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	early := g.NewCard(creatureDef(t), other, engine.Hand)
	g.Move(early, engine.Battlefield, other)
	if !historyMatches(g, early, "Creature.EnteredSinceYourLastTurn", p) {
		t.Error("a creature that entered before p's first turn ended is not EnteredSinceYourLastTurn")
	}
	for g.ActivePhase() != engine.Untap {
		g.AdvancePhase(c)
	}
	// Turn 2, the other player's: p's last turn was turn 1.
	late := g.NewCard(creatureDef(t), other, engine.Hand)
	g.Move(late, engine.Battlefield, other)
	if historyMatches(g, early, "Creature.EnteredSinceYourLastTurn", p) {
		t.Error("a creature that entered during p's last turn is EnteredSinceYourLastTurn")
	}
	if !historyMatches(g, late, "Creature.EnteredSinceYourLastTurn", p) {
		t.Error("a creature that entered after p's last turn is not EnteredSinceYourLastTurn")
	}
	if !historyMatches(g, early, "Creature.EnteredSinceYourLastTurn", other) {
		t.Error("the other player has had no turn yet, so every entry is since it")
	}
}

// TestWasCastFromReadsTheCastZone is Laquatus's Disdain's target and the
// "if you cast it from your hand" conditions (CardProperty.java:1929-1949):
// the zone the card was cast from, whose zone it was and who cast it.
func TestWasCastFromReadsTheCastZone(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	flashback := copyTestDef(t, "Test Flashback", "Instant", "", "", "A:SP$ GainLife | LifeAmount$ 1", "K:Flashback:G", "Cost:G")
	fromGrave := g.NewCard(flashback, p, engine.Graveyard)
	fromHand := g.NewCard(copyTestDef(t, "Test Bear", "Creature Elf", "2", "2", "Cost:G"), p, engine.Hand)
	placed := g.NewCard(creatureDef(t), p, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	if !g.CastSpell(p, fromGrave, c) {
		t.Fatal("the flashback cast from the graveyard failed")
	}

	for _, tc := range []struct {
		card engine.CardID
		spec string
		you  engine.PlayerID
		want bool
	}{
		{fromGrave, "Card.wasCastFromGraveyard", other, true},
		{fromGrave, "Card.wasCastFromHand", other, false},
		{fromGrave, "Card.wasCastFromYourGraveyard", p, true},
		{fromGrave, "Card.wasCastFromYourGraveyard", other, false},
		{fromGrave, "Card.wasCastFromTheirGraveyard", other, true},
		{fromGrave, "Card.wasCastFromGraveyardByYou", p, true},
		{fromGrave, "Card.wasCastFromGraveyardByYou", other, false},
		{fromGrave, "Card.wasCastFromNowhere", p, false},
		{placed, "Card.wasCastFromHand", p, false},
	} {
		if got := historyMatches(g, tc.card, tc.spec, tc.you); got != tc.want {
			t.Errorf("%s on card %d (you %d) = %v, want %v", tc.spec, tc.card, tc.you, got, tc.want)
		}
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}

	// A permanent spell keeps its cast record onto the battlefield.
	g.Player(p).ManaPool.Add(mana.Green, 1)
	if !g.CastSpell(p, fromHand, c) {
		t.Fatal("the creature cast from hand failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if !historyMatches(g, fromHand, "Creature.wasCastFromYourHandByYou", p) {
		t.Error("a creature cast from p's hand is not wasCastFromYourHandByYou for p")
	}
	if historyMatches(g, fromHand, "Creature.!wasCastFromHand", p) {
		t.Error("!wasCastFromHand matched a creature cast from hand")
	}
}

// TestSecondSpellCastThisTurnIsTheSecondObjectCast is Second Guess's target
// (CardProperty.java:924-931): the second spell cast this turn, by anyone,
// while it is still that object on the stack.
func TestSecondSpellCastThisTurnIsTheSecondObjectCast(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	instant := copyTestDef(t, "Test Instant", "Instant", "", "", "A:SP$ GainLife | LifeAmount$ 1", "Cost:G")
	first := g.NewCard(instant, p, engine.Hand)
	second := g.NewCard(instant, p, engine.Hand)
	if historyMatches(g, first, "Card.SecondSpellCastThisTurn", other) {
		t.Error("SecondSpellCastThisTurn matched with no spell cast")
	}
	g.Player(p).ManaPool.Add(mana.Green, 2)
	if !g.CastSpell(p, first, c) || !g.CastSpell(p, second, c) {
		t.Fatal("CastSpell failed")
	}
	if historyMatches(g, first, "Card.SecondSpellCastThisTurn", other) {
		t.Error("the first spell is SecondSpellCastThisTurn")
	}
	if !historyMatches(g, second, "Card.SecondSpellCastThisTurn", other) {
		t.Error("the second spell on the stack is not SecondSpellCastThisTurn")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if historyMatches(g, second, "Card.SecondSpellCastThisTurn", other) {
		t.Error("the card in the graveyard is still that spell")
	}
}
