package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Emerge (CR 702.119, CardFactoryUtil.java:2808-2821): cast the spell by
// sacrificing a creature and paying the emerge cost reduced by that creature's
// mana value; Count$Emerged and Emerged$ read the cast.

// aliens is how many Alien tokens p controls.
func aliens(g *engine.Game, p engine.PlayerID) int {
	n := 0
	for _, id := range g.Zone(engine.Battlefield, p).Cards() {
		if strings.Contains(g.Card(id).Type().String(), "Alien") && g.Card(id).IsToken {
			n++
		}
	}
	return n
}

// Adipose Offspring emerged off a three-mana 1/4 costs {2}{W} and makes four
// Aliens, the sacrificed creature's toughness; cast for its mana cost it makes
// one.
func TestEmergeSacrificesAndReducesTheCost(t *testing.T) {
	t.Parallel()

	for _, emerge := range []bool{true, false} {
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		g.SetTurnState(1, p, engine.Main1)
		turtle := g.NewCard(amountDef(t, "Test Turtle", "Creature Turtle", "2 U", "1", "4", nil), p, engine.Battlefield)
		offspring := g.NewCard(corpusCard(t, "Adipose Offspring"), p, engine.Hand)
		c := engine.NewScriptedController()
		if emerge {
			g.Player(p).ManaPool.Add(mana.White, 3)
			c.QueueOption(1)
			c.QueueSacrificeChoice([]engine.CardID{turtle})
			c.QueuePayGeneric(mana.ShardW)
			c.QueuePayGeneric(mana.ShardW)
		} else {
			g.Player(p).ManaPool.Add(mana.White, 4)
			c.QueueOption(0)
			for range 3 {
				c.QueuePayGeneric(mana.ShardW)
			}
		}
		castThenResolve(t, g, p, offspring, c)
		if err := g.TakePendingError(); err != nil {
			t.Fatalf("emerge=%v: pending error: %v", emerge, err)
		}
		want, wantZone := 1, engine.Battlefield
		if emerge {
			want, wantZone = 4, engine.Graveyard
		}
		if got := aliens(g, p); got != want {
			t.Errorf("emerge=%v: %d Aliens, want %d", emerge, got, want)
		}
		if got := g.Card(turtle).Zone; got != wantZone {
			t.Errorf("emerge=%v: the creature in %v, want %v", emerge, got, wantZone)
		}
		if got := g.Player(p).ManaPool.Total(); got != 0 {
			t.Errorf("emerge=%v: %d mana left over, want 0", emerge, got)
		}
	}
}

// With nothing to sacrifice Emerge is not offered (its IsPresent$
// restriction), so the cast asks no option; the generic reduction never goes
// below zero.
func TestEmergeNeedsSomethingToSacrifice(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	offspring := g.NewCard(corpusCard(t, "Adipose Offspring"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 4)
	c := engine.NewScriptedController()
	for range 3 {
		c.QueuePayGeneric(mana.ShardW)
	}
	castThenResolve(t, g, p, offspring, c)
	if got := aliens(g, p); got != 1 {
		t.Errorf("%d Aliens, want 1", got)
	}

	g2, p2, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g2.SetTurnState(1, p2, engine.Main1)
	big := g2.NewCard(amountDef(t, "Test Titan", "Creature Giant", "7 G", "2", "2", nil), p2, engine.Battlefield)
	offspring2 := g2.NewCard(corpusCard(t, "Adipose Offspring"), p2, engine.Hand)
	g2.Player(p2).ManaPool.Add(mana.White, 1)
	c2 := engine.NewScriptedController()
	c2.QueueOption(1)
	c2.QueueSacrificeChoice([]engine.CardID{big})
	castThenResolve(t, g2, p2, offspring2, c2)
	if got := aliens(g2, p2); got != 2 {
		t.Errorf("emerged off an eight-drop: %d Aliens, want 2", got)
	}
}

// A sacrifice answer outside the candidates cancels the cast: nothing is
// sacrificed and no mana is spent.
func TestEmergeRefusesABadSacrifice(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.NewCard(amountDef(t, "Test Turtle", "Creature Turtle", "2 U", "1", "4", nil), p, engine.Battlefield)
	theirs := g.NewCard(amountDef(t, "Test Turtle", "Creature Turtle", "2 U", "1", "4", nil), other, engine.Battlefield)
	offspring := g.NewCard(corpusCard(t, "Adipose Offspring"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 3)
	c := engine.NewScriptedController()
	c.QueueOption(1)
	c.QueueSacrificeChoice([]engine.CardID{theirs})
	if g.CastSpell(p, offspring, c) {
		t.Fatal("cast with an opponent's creature as the emerge sacrifice")
	}
	if got := g.Card(offspring).Zone; got != engine.Hand {
		t.Errorf("Adipose Offspring in %v, want Hand", got)
	}
	if got := g.Player(p).ManaPool.Total(); got != 3 {
		t.Errorf("%d mana left, want 3", got)
	}
}
