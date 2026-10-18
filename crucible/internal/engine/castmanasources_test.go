package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// manaSourceDef is a permanent of typeLine whose one ability is a mana
// ability.
func manaSourceDef(t *testing.T, name, typeLine, ability string) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), typeLine)
	raw.Faces[0].Abilities = []string{ability}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// spentFromCount casts an instant costing cost that gains X life, X =
// Count$CastTotalManaSpent <restriction>, after fill floats the mana, and
// returns the life gained.
func spentFromCount(t *testing.T, cost, restriction string, fill func(g *engine.Game, p engine.PlayerID, c *engine.ScriptedController)) int {
	t.Helper()
	g, p, _ := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Main1)
	spell := g.NewCard(spellDefWith(t, "Test Sourced Spend", "Instant", cost,
		"SP$ GainLife | Defined$ You | LifeAmount$ X", "X", "Count$CastTotalManaSpent "+restriction), p, engine.Hand)
	c := engine.NewScriptedController()
	fill(g, p, c)
	before := g.Player(p).Life
	castThenResolve(t, g, p, spell, c)
	return g.Player(p).Life - before
}

// Count$CastTotalManaSpent <valid> counts the mana whose source card matches
// (AbilityUtils.java:1899-1907): a Cave's mana and a Mountain's paying {1}{R},
// one of them from a Cave.
func TestCastTotalManaSpentCountsManaFromAMatchingSource(t *testing.T) {
	t.Parallel()

	got := spentFromCount(t, "1 R", "Cave", func(g *engine.Game, p engine.PlayerID, c *engine.ScriptedController) {
		cave := g.NewCard(manaSourceDef(t, "Test Cave", "Land Cave", "AB$ Mana | Cost$ T | Produced$ R"), p, engine.Battlefield)
		mountain := g.NewCard(landDef(t, "Mountain", "Basic Land Mountain"), p, engine.Battlefield)
		if !g.ActivateManaAbility(p, cave, 0, c) || !g.TapLandForMana(p, mountain, mana.Red, c) {
			t.Fatal("could not float the mana")
		}
		c.QueuePayGeneric(mana.ShardR)
	})
	if got != 1 {
		t.Errorf("gained %d, want 1: one of the two mana came from a Cave", got)
	}
}

// Within one color the oldest floating unit pays first
// (ManaPool.tryPayCostWithColor, ManaPool.java:228-250): with a Mountain's red
// floated before a Cave's, a {R} spell spends the Mountain's.
func TestCastTotalManaSpentSpendsTheOldestUnitFirst(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name      string
		caveFirst bool
		want      int
	}{
		{"cave first", true, 1},
		{"mountain first", false, 0},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			got := spentFromCount(t, "R", "Cave", func(g *engine.Game, p engine.PlayerID, c *engine.ScriptedController) {
				cave := g.NewCard(manaSourceDef(t, "Test Cave", "Land Cave", "AB$ Mana | Cost$ T | Produced$ R"), p, engine.Battlefield)
				mountain := g.NewCard(landDef(t, "Mountain", "Basic Land Mountain"), p, engine.Battlefield)
				first := func() bool { return g.ActivateManaAbility(p, cave, 0, c) }
				second := func() bool { return g.TapLandForMana(p, mountain, mana.Red, c) }
				if !tc.caveFirst {
					first, second = second, first
				}
				if !first() || !second() {
					t.Fatal("could not float the mana")
				}
			})
			if got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
}

// A Treasure sacrificed for its mana is read as it was on the battlefield
// (Java's Mana keeps the battlefield Card object): tapped, though the card in
// the graveyard is not.
func TestCastTotalManaSpentReadsASacrificedSourceAsItLastWas(t *testing.T) {
	t.Parallel()

	got := spentFromCount(t, "R", "Treasure.tapped", func(g *engine.Game, p engine.PlayerID, c *engine.ScriptedController) {
		treasure := g.NewCard(manaSourceDef(t, "Test Treasure", "Artifact Treasure",
			"AB$ Mana | Cost$ T Sac<1/CARDNAME> | Produced$ Any"), p, engine.Battlefield)
		c.QueueManaColor(mana.Red)
		if !g.ActivateManaAbility(p, treasure, 0, c) {
			t.Fatal("could not float the Treasure's mana")
		}
		if g.Card(treasure).Zone != engine.Graveyard {
			t.Fatal("the Treasure was not sacrificed")
		}
	})
	if got != 1 {
		t.Errorf("gained %d, want 1: the Treasure was tapped when it made the mana", got)
	}
}

// Mana with no recorded source (a fixture's, the smoke probe's) matches no
// restriction; a free cast spends none.
func TestCastTotalManaSpentWithoutSourcesCountsNothing(t *testing.T) {
	t.Parallel()

	got := spentFromCount(t, "R", "Card", func(g *engine.Game, p engine.PlayerID, _ *engine.ScriptedController) {
		g.Player(p).ManaPool.Add(mana.Red, 1)
	})
	if got != 0 {
		t.Errorf("gained %d, want 0", got)
	}
}

// Drained mana keeps its sources (DrainMana$ True merges the opponent's pool
// into the activator's): a Cave's red drained and spent still counts as a
// Cave's.
func TestDrainedManaKeepsItsSource(t *testing.T) {
	t.Parallel()

	got := spentFromCount(t, "R", "Cave", func(g *engine.Game, p engine.PlayerID, c *engine.ScriptedController) {
		other := g.Players()[1]
		cave := g.NewCard(manaSourceDef(t, "Test Cave", "Land Cave", "AB$ Mana | Cost$ T | Produced$ R"), other, engine.Battlefield)
		if !g.ActivateManaAbility(other, cave, 0, c) {
			t.Fatal("could not float the opponent's mana")
		}
		if _, err := resolveNow(t, g, p, c, nil, "DB$ DrainMana | Defined$ Opponent | DrainMana$ True"); err != nil {
			t.Fatalf("DrainMana: %v", err)
		}
	})
	if got != 1 {
		t.Errorf("gained %d, want 1: the drained red came from a Cave", got)
	}
}

// Mana Kruphix keeps as colorless at the end of a step keeps its source
// (ManaPool.convertManaColor's Mana.convertColor keeps sourceCard).
func TestConvertedManaKeepsItsSource(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.NewCard(corpusCard(t, "Kruphix, God of Horizons"), p, engine.Battlefield)
	cave := g.NewCard(manaSourceDef(t, "Test Cave", "Land Cave", "AB$ Mana | Cost$ T | Produced$ R"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	if !g.ActivateManaAbility(p, cave, 0, c) {
		t.Fatal("could not float the Cave's mana")
	}
	g.AdvancePhase(c)
	if got := g.Player(p).ManaPool.Breakdown(); got != ([6]int{0, 0, 0, 0, 0, 1}) {
		t.Fatalf("pool after the step = %v, want one colorless", got)
	}
	g.SetTurnState(1, p, engine.Main2)
	spell := g.NewCard(spellDefWith(t, "Test Sourced Spend", "Instant", "1",
		"SP$ GainLife | Defined$ You | LifeAmount$ X", "X", "Count$CastTotalManaSpent Cave"), p, engine.Hand)
	c.QueuePayGeneric(mana.ShardC)
	before := g.Player(p).Life
	castThenResolve(t, g, p, spell, c)
	if got := g.Player(p).Life - before; got != 1 {
		t.Errorf("gained %d, want 1: the converted unit still came from a Cave", got)
	}
}

// A ritual's mana comes from the ritual (ManaEffect's AbilityManaPart source),
// and a clone's pool keeps its own sources: spending the clone's mana leaves
// the original's untouched.
func TestManaSourcesSurviveACloneAndARitual(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Main1)
	ritual := g.NewCard(spellDefWith(t, "Test Ritual", "Instant", "0", "SP$ Mana | Produced$ R | Amount$ 2"), p, engine.Hand)
	c := engine.NewScriptedController()
	castThenResolve(t, g, p, ritual, c)
	spell := g.NewCard(spellDefWith(t, "Test Sourced Spend", "Instant", "R",
		"SP$ GainLife | Defined$ You | LifeAmount$ X", "X", "Count$CastTotalManaSpent Instant"), p, engine.Hand)

	clone := g.Clone()
	before := clone.Player(p).Life
	castThenResolve(t, clone, p, spell, c)
	if got := clone.Player(p).Life - before; got != 1 {
		t.Errorf("clone gained %d, want 1: the ritual made the mana", got)
	}
	before = g.Player(p).Life
	castThenResolve(t, g, p, spell, c)
	if got := g.Player(p).Life - before; got != 1 {
		t.Errorf("original gained %d, want 1 after the clone spent its own copy", got)
	}
	if got := g.Player(p).ManaPool.Total(); got != 1 {
		t.Errorf("original pool holds %d, want the ritual's other red", got)
	}
}
