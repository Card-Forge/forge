package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These tests cover Layer 3's GainTextOf$ (CR 613.1c,
// StaticAbilityContinuous.java's TEXT branch) through its one real corpus
// line, Volrath's Shapeshifter, compiled from the real card script: "As
// long as the top card of your graveyard is a creature card, CARDNAME has
// the full text of that card and has the text '{2}: Discard a card.'"

const volrath = "Volrath's Shapeshifter"

// corpusCard is name's compiled definition from the real corpus.
func corpusCard(t *testing.T, name string) *compile.Card {
	t.Helper()
	def, ok := scenarioDB(t).Card(name)
	if !ok {
		t.Fatalf("no corpus card %q", name)
	}
	return def
}

// volrathGame is a two-player game with the real Volrath's Shapeshifter on
// p's battlefield and each named corpus card put into p's graveyard in
// order, the last one on top.
func volrathGame(t *testing.T, graveyard ...string) (*engine.Game, engine.PlayerID, engine.CardID) {
	t.Helper()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	shifter := g.NewCard(corpusCard(t, volrath), p, engine.Battlefield)
	for _, name := range graveyard {
		g.NewCard(corpusCard(t, name), p, engine.Graveyard)
	}
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	return g, p, shifter
}

// TestGainTextOfTakesTopCreaturesFullText proves the gained characteristics
// the reminder text lists: name, mana cost, color, types, abilities, power
// and toughness -- all of Serra Angel's, none of the Shapeshifter's own --
// plus the GainTextAbilities$ ability appended after the gained ones.
func TestGainTextOfTakesTopCreaturesFullText(t *testing.T) {
	t.Parallel()

	g, _, shifter := volrathGame(t, "Serra Angel")
	c := g.Card(shifter)

	if c.Def.Name != "Serra Angel" {
		t.Errorf("name = %q, want Serra Angel", c.Def.Name)
	}
	if c.CMC() != 5 || c.Colors() != mana.White {
		t.Errorf("CMC %d colors %v, want 5 and white", c.CMC(), c.Colors())
	}
	if !c.Type().HasSubtype("Angel") || c.Type().HasSubtype("Shapeshifter") {
		t.Errorf("type = %v, want an Angel and no Shapeshifter", c.Type())
	}
	if !c.HasKeyword("Flying") || !c.HasKeyword("Vigilance") {
		t.Error("gained keywords missing: want Flying and Vigilance")
	}
	wantPT(t, g, shifter, 4, 4)
	abilities := c.Def.Faces[0].Abilities
	if len(abilities) != 1 || abilities[0].Name != "Discard" || abilities[0].SVar != "VolrathDiscard" {
		t.Errorf("abilities = %d, want only the gained VolrathDiscard", len(abilities))
	}
	if c.UncopiedDef().Name != volrath {
		t.Errorf("UncopiedDef = %q, want the printed %s (the fixture dump's name)", c.UncopiedDef().Name, volrath)
	}
}

// TestGainTextOfNeedsACreatureOnTop proves both halves of "as long as the
// top card of your graveyard is a creature card": a non-creature on top,
// even over a creature, and an empty graveyard both leave the printed 0/1.
func TestGainTextOfNeedsACreatureOnTop(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name      string
		graveyard []string
	}{
		{"non-creature on top", []string{"Grizzly Bears", "Lightning Bolt"}},
		{"empty graveyard", nil},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, _, shifter := volrathGame(t, tc.graveyard...)
			if got := g.Card(shifter).Def.Name; got != volrath {
				t.Errorf("name = %q, want the printed %s", got, volrath)
			}
			wantPT(t, g, shifter, 0, 1)
		})
	}
}

// TestGainTextOfFollowsTheGraveyardTop proves the text is recomputed every
// pass, not latched: activating the gained "{2}: Discard a card." (by its
// index in the text-changed ability list) to discard a non-creature puts
// that card on top, and the Shapeshifter is printed again on the next
// check; discarding a creature later makes it that creature.
func TestGainTextOfFollowsTheGraveyardTop(t *testing.T) {
	t.Parallel()

	g, p, shifter := volrathGame(t, "Grizzly Bears")
	bolt := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	angel := g.NewCard(corpusCard(t, "Serra Angel"), p, engine.Hand)
	if got := g.Card(shifter).Def.Name; got != "Grizzly Bears" {
		t.Fatalf("setup: name = %q, want Grizzly Bears", got)
	}

	c := engine.NewScriptedController()
	discard := func(card engine.CardID) {
		t.Helper()
		g.Player(p).ManaPool.Add(mana.Blue, 2)
		c.QueuePayGeneric(mana.ShardU)
		c.QueuePayGeneric(mana.ShardU)
		abilities := g.Card(shifter).Def.Faces[0].Abilities
		c.QueueDiscardChoice([]engine.CardID{card})
		if !g.ActivateAbility(p, shifter, len(abilities)-1, c) {
			t.Fatal("ActivateAbility(gained discard) = false")
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatal(err)
		}
		engine.CheckStateBasedActions(g, c)
	}

	discard(bolt)
	if got := g.Card(shifter).Def.Name; got != volrath {
		t.Errorf("after discarding Lightning Bolt: name = %q, want the printed %s", got, volrath)
	}
	wantPT(t, g, shifter, 0, 1)

	discard(angel)
	if got := g.Card(shifter).Def.Name; got != "Serra Angel" {
		t.Errorf("after discarding Serra Angel: name = %q, want Serra Angel", got)
	}
}

// TestGainTextOfEndsWhenThePermanentLeaves proves the change dies with the
// object (CR 400.7): the Shapeshifter in the graveyard is its printed self.
func TestGainTextOfEndsWhenThePermanentLeaves(t *testing.T) {
	t.Parallel()

	g, p, shifter := volrathGame(t, "Serra Angel")
	g.Move(shifter, engine.Graveyard, p)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := g.Card(shifter).Def.Name; got != volrath {
		t.Errorf("in the graveyard: name = %q, want the printed %s", got, volrath)
	}
}

// TestGainTextOfIsNotCopiable proves CR 707.2's exclusion of text-changing
// effects from copiable values: a copy of a text-changed Shapeshifter is a
// printed Shapeshifter. Its own copied static gives it the same text while a
// creature is on top; once a non-creature is, it is the printed
// Shapeshifter, not a leftover Serra Angel.
func TestGainTextOfIsNotCopiable(t *testing.T) {
	t.Parallel()

	g, p, shifter := volrathGame(t, "Serra Angel")
	c := engine.NewScriptedController()
	host := g.NewCard(copyTestDef(t, "Copier", "Creature Shapeshifter", "1", "1",
		"A:AB$ Clone | ValidTgts$ Creature"), p, engine.Battlefield)
	mustActivate(t, g, p, c, host, shifter)
	if got := g.Card(host).UncopiedDef().Name; got != "Copier" {
		t.Errorf("copy's uncopied name = %q, want Copier", got)
	}

	g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Graveyard)
	engine.CheckStateBasedActions(g, c)
	if got := g.Card(host).Def.Name; got != volrath {
		t.Errorf("copy's name = %q, want the printed %s", got, volrath)
	}
	wantPT(t, g, host, 0, 1)
}

// TestGainTextOfGainedStaticApplies proves a static ability in the gained
// text takes part in every later layer: a Shapeshifter that is Lord of
// Atlantis gives another Merfolk +1/+1 (Layer 7c) and islandwalk (Layer 6).
func TestGainTextOfGainedStaticApplies(t *testing.T) {
	t.Parallel()

	g, p, _ := volrathGame(t, "Lord of Atlantis")
	merfolk := g.NewCard(corpusCard(t, "Merfolk of the Pearl Trident"), p, engine.Battlefield)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	wantPT(t, g, merfolk, 2, 2)
	if !g.Card(merfolk).HasKeyword("Landwalk") {
		t.Error("gained lord did not grant islandwalk")
	}
}

// TestGainTextOfCharacteristicDefiningPT proves a gained "*" power and
// toughness resolves through the gained characteristic-defining ability
// (Layer 7a): Nightmare is as big as its controller's Swamp count.
func TestGainTextOfCharacteristicDefiningPT(t *testing.T) {
	t.Parallel()

	// Swamps first: with none, the gained Nightmare is 0/0 and dies on the
	// very check that makes it one.
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	for range 3 {
		g.NewCard(corpusCard(t, "Swamp"), p, engine.Battlefield)
	}
	shifter := g.NewCard(corpusCard(t, volrath), p, engine.Battlefield)
	g.NewCard(corpusCard(t, "Nightmare"), p, engine.Graveyard)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	wantPT(t, g, shifter, 3, 3)
	if !g.Card(shifter).HasKeyword("Flying") {
		t.Error("gained Nightmare lacks flying")
	}
}

// TestGainTextOfSurvivesClone proves Game.Clone carries the change and that
// the two games then evolve independently: a creature milled in the clone
// changes only the clone's Shapeshifter.
func TestGainTextOfSurvivesClone(t *testing.T) {
	t.Parallel()

	g, p, shifter := volrathGame(t, "Serra Angel")
	clone := g.Clone()
	if got := clone.Card(shifter).Def.Name; got != "Serra Angel" {
		t.Fatalf("clone's name = %q, want Serra Angel", got)
	}
	clone.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Graveyard)
	engine.CheckStateBasedActions(clone, engine.NewScriptedController())
	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := clone.Card(shifter).Def.Name; got != "Grizzly Bears" {
		t.Errorf("clone's name = %q, want Grizzly Bears", got)
	}
	if got := g.Card(shifter).Def.Name; got != "Serra Angel" {
		t.Errorf("original's name = %q, want Serra Angel -- the clone leaked", got)
	}
	if !g.Card(shifter).Type().Has(cardtype.Creature) {
		t.Error("original stopped being a creature")
	}
}
