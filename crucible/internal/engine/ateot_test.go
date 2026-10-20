package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestAtEOTSacrificesTokensAtTheNextEndStep proves SpellAbilityEffect.
// registerDelayedTrigger for Token: the tokens a line makes with AtEOT$
// Sacrifice stay until the end step and are then sacrificed.
func TestAtEOTSacrificesTokensAtTheNextEndStep(t *testing.T) {
	t.Parallel()

	g, p, other := newTokenGame(t)
	libraryCards(t, g, p, 5)
	libraryCards(t, g, other, 5)
	c := engine.NewScriptedController()
	def := etbChainDef(t, "Test Eot Maker",
		"DB$ Token | TokenAmount$ 2 | TokenScript$ w_1_1_soldier | TokenOwner$ You | AtEOT$ Sacrifice")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := len(tokensOn(g, p, "Soldier Token")); n != 2 {
		t.Fatalf("tokens before the end step = %d, want 2", n)
	}
	advanceUntil(t, g, c, func() bool { return g.ActivePhase() == engine.EndOfTurn })
	if n := len(tokensOn(g, p, "Soldier Token")); n != 0 {
		t.Errorf("tokens after the end step = %d, want 0", n)
	}
}

// TestAtEOTHandReturnsThePumpedCreature proves AtEOT$ Hand on Pump: the
// target is pumped now and returned to its owner's hand at the end step.
func TestAtEOTHandReturnsThePumpedCreature(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	libraryCards(t, g, p, 5)
	libraryCards(t, g, other, 5)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	def := etbChainDef(t, "Test Eot Pump",
		"DB$ Pump | ValidTgts$ Creature.YouCtrl | NumAtt$ +2 | AtEOT$ Hand")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(mine).Zone; z != engine.Battlefield {
		t.Fatalf("creature zone before the end step = %v, want Battlefield", z)
	}
	advanceUntil(t, g, c, func() bool { return g.ActivePhase() == engine.EndOfTurn })
	if z := g.Card(mine).Zone; z != engine.Hand {
		t.Errorf("creature zone after the end step = %v, want Hand", z)
	}
}

// TestExileFaceDownTurnsTheExiledCardFaceDown proves ExileFaceDown$ on Dig
// and ChangeZoneAll: the card lands in exile with no characteristics, and a
// line without the param leaves it face up.
func TestExileFaceDownTurnsTheExiledCardFaceDown(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		line string
		down bool
	}{
		{"DB$ Dig | DigNum$ 1 | DestinationZone$ Exile | SkipReorder$ True | ExileFaceDown$ True", true},
		{"DB$ Dig | DigNum$ 1 | DestinationZone$ Exile | SkipReorder$ True", false},
		{"DB$ ChangeZoneAll | ChangeType$ Card.YouOwn | Origin$ Library | Destination$ Exile | ExileFaceDown$ True", true},
	} {
		g, p, other := newTwoPlayerGame(t)
		libraryCards(t, g, p, 1)
		libraryCards(t, g, other, 1)
		c := engine.NewScriptedController()
		if strings.HasPrefix(tc.line, "DB$ Dig") {
			c.QueueCardChoice(g.Zone(engine.Library, p).Cards()[:1])
		}
		def := etbChainDef(t, "Test Face Down", tc.line)
		if _, err := castETBChain(t, g, p, def, c); err != nil {
			t.Fatalf("%q: ResolveStack: %v", tc.line, err)
		}
		exiled := g.Zone(engine.Exile, p).Cards()
		if len(exiled) == 0 {
			t.Fatalf("%q: nothing exiled", tc.line)
		}
		for _, id := range exiled {
			if got := g.Card(id).IsFaceDown(); got != tc.down {
				t.Errorf("%q: exiled card face down = %v, want %v", tc.line, got, tc.down)
			}
		}
	}
}

// TestAtEOTExilesWhatChangeZoneReturned proves AtEOT$ on ChangeZone
// (ChangeZoneEffect.java:861): the creature returned from the graveyard is
// exiled at the end step.
func TestAtEOTExilesWhatChangeZoneReturned(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	libraryCards(t, g, p, 5)
	libraryCards(t, g, other, 5)
	dead := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Graveyard)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(dead)})
	def := etbChainDef(t, "Test Eot Reanimate",
		"DB$ ChangeZone | Origin$ Graveyard | Destination$ Battlefield | ValidTgts$ Creature.YouOwn | AtEOT$ Exile")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(dead).Zone; z != engine.Battlefield {
		t.Fatalf("creature zone before the end step = %v, want Battlefield", z)
	}
	advanceUntil(t, g, c, func() bool { return g.ActivePhase() == engine.EndOfTurn })
	if z := g.Card(dead).Zone; z != engine.Exile {
		t.Errorf("creature zone after the end step = %v, want Exile", z)
	}
}

// TestAtEOTReturnsWhatChangeZoneAllMoved proves AtEOT$ on ChangeZoneAll
// (ChangeZoneAllEffect.java:202): every creature it put onto the battlefield
// goes back to hand at the end step.
func TestAtEOTReturnsWhatChangeZoneAllMoved(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	libraryCards(t, g, p, 5)
	libraryCards(t, g, other, 5)
	a := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Graveyard)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
	c := engine.NewScriptedController()
	c.QueueCardOrder([]engine.CardID{a, b})
	def := etbChainDef(t, "Test Eot Mass Reanimate",
		"DB$ ChangeZoneAll | ChangeType$ Creature.YouOwn | Origin$ Graveyard | Destination$ Battlefield | AtEOT$ Hand")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	for _, id := range []engine.CardID{a, b} {
		if z := g.Card(id).Zone; z != engine.Battlefield {
			t.Fatalf("creature zone before the end step = %v, want Battlefield", z)
		}
	}
	advanceUntil(t, g, c, func() bool { return g.ActivePhase() == engine.EndOfTurn })
	for _, id := range []engine.CardID{a, b} {
		if z := g.Card(id).Zone; z != engine.Hand {
			t.Errorf("creature zone after the end step = %v, want Hand", z)
		}
	}
}
