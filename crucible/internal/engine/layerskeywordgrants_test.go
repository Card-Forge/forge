package engine_test

import (
	"slices"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// TestCantHaveKeywordStripsTheKeywordWhateverGrantsIt proves CantHaveKeyword$
// (the Archetype cycle, Card.java:5198-5200): creatures the opponent controls
// lose flying, and a grant from an effect that came later does not give it
// back, while the controller's own creatures keep theirs. When the Archetype
// leaves, the keyword returns.
func TestCantHaveKeywordStripsTheKeywordWhateverGrantsIt(t *testing.T) {
	t.Parallel()

	g, p := textGame(t)
	other := g.Players()[1]
	archetype := g.NewCard(scriptDef(t, "Archetype", "Creature",
		"S:Mode$ Continuous | Affected$ Creature.OppCtrl | RemoveKeyword$ Flying | CantHaveKeyword$ Flying"), p, engine.Battlefield)
	mine := g.NewCard(creatureDefPTKeywords(t, "1", "1", "Flying"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPTKeywords(t, "1", "1", "Flying", "Trample"), other, engine.Battlefield)
	g.NewCard(scriptDef(t, "Granter", "Enchantment",
		"S:Mode$ Continuous | Affected$ Creature | AddKeyword$ Flying & Haste"), other, engine.Battlefield)
	sba(g)

	if !g.Card(mine).HasKeyword("Flying") {
		t.Error("the Archetype's controller lost flying")
	}
	if g.Card(theirs).HasKeyword("Flying") || !g.Card(theirs).HasKeyword("Trample") || !g.Card(theirs).HasKeyword("Haste") {
		t.Errorf("opponent's keywords = %v, want Trample and Haste but no Flying", g.Card(theirs).KeywordLines())
	}
	g.Move(archetype, engine.Graveyard, p)
	sba(g)
	if !g.Card(theirs).HasKeyword("Flying") {
		t.Errorf("opponent's keywords = %v after the Archetype left, want Flying back", g.Card(theirs).KeywordLines())
	}
}

// TestSharedKeywordsZoneGrantsOnlyWhatCardsInTheZoneCarry proves
// SharedKeywordsZone$ with SharedRestrictions$ (Cairn Wanderer, Escaped
// Shapeshifter): of the keywords the line lists, the host gets the ones a
// matching card in the zone has, and a bare Protection or Trample token stands
// for that card's own line.
func TestSharedKeywordsZoneGrantsOnlyWhatCardsInTheZoneCarry(t *testing.T) {
	t.Parallel()

	g, p := textGame(t)
	other := g.Players()[1]
	wanderer := g.NewCard(scriptDef(t, "Wanderer", "Creature",
		"S:Mode$ Continuous | EffectZone$ Battlefield | AffectedDefined$ Self | AddKeyword$ Flying & Fear & First Strike & Trample & Protection | "+
			"SharedKeywordsZone$ Graveyard | SharedRestrictions$ Creature"), p, engine.Battlefield)
	g.NewCard(creatureDefPTKeywords(t, "1", "1", "Flying", "Trample", "Protection:Card.Red:red:Protection from red"), other, engine.Graveyard)
	g.NewCard(scriptDef(t, "Not A Creature", "Artifact", "K:Fear"), p, engine.Graveyard)
	sba(g)

	lines := g.Card(wanderer).KeywordLines()
	for _, want := range []string{"Flying", "Trample", "Protection:Card.Red:red:Protection from red"} {
		if !slices.Contains(lines, want) {
			t.Errorf("keyword lines = %v, want %q", lines, want)
		}
	}
	if g.Card(wanderer).HasKeyword("Fear") || g.Card(wanderer).HasKeyword("First Strike") {
		t.Errorf("keyword lines = %v, want no Fear (not a creature card) and no First Strike (nobody has it)", lines)
	}
}

// TestSharedKeywordsZoneOnTheBattlefieldReadsTheControllersPointOfView proves
// Escaped Shapeshifter's restriction: only an opponent's creature not named
// like the host counts, and ProtectionColor stands for "Protection from
// <color>" lines.
func TestSharedKeywordsZoneOnTheBattlefieldReadsTheControllersPointOfView(t *testing.T) {
	t.Parallel()

	g, p := textGame(t)
	other := g.Players()[1]
	shifter := g.NewCard(scriptDef(t, "Escaped Shapeshifter", "Creature",
		"S:Mode$ Continuous | AffectedDefined$ Self | AddKeyword$ Flying & First Strike & Trample & ProtectionColor | "+
			"SharedKeywordsZone$ Battlefield | SharedRestrictions$ Creature.!namedEscaped Shapeshifter+OppCtrl"), p, engine.Battlefield)
	g.NewCard(creatureDefPTKeywords(t, "1", "1", "First Strike", "Protection from white"), p, engine.Battlefield)
	sba(g)
	if lines := g.Card(shifter).KeywordLines(); len(lines) != 0 {
		t.Fatalf("keyword lines = %v, want none: the only first striker is the controller's", lines)
	}
	g.NewCard(creatureDefPTKeywords(t, "1", "1", "First Strike", "Protection from white"), other, engine.Battlefield)
	sba(g)
	lines := g.Card(shifter).KeywordLines()
	for _, want := range []string{"First Strike", "Protection from white"} {
		if !slices.Contains(lines, want) {
			t.Errorf("keyword lines = %v, want %q", lines, want)
		}
	}
}

// TestSharedKeywordsZoneGrantsOnlyWhatTheHostsDelveExiled proves Soulflayer's
// "delved" restriction (CardProperty.java:1367-1369): of the exiled creature
// cards, only the ones its own delve exiled count, and the list survives the
// spell becoming a permanent.
func TestSharedKeywordsZoneGrantsOnlyWhatTheHostsDelveExiled(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	soul := g.NewCard(corpusCard(t, "Soulflayer"), p, engine.Hand)
	flyer := g.NewCard(creatureDefPTKeywords(t, "1", "1", "Flying"), p, engine.Graveyard)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
	grave := g.Zone(engine.Graveyard, p).Cards()
	// Trample sits in exile, but nothing delved it.
	g.NewCard(creatureDefPTKeywords(t, "1", "1", "Trample"), p, engine.Exile)
	g.Player(p).ManaPool.Add(mana.Black, 2)

	c := engine.NewScriptedController()
	c.QueueCardChoice(grave)
	c.QueuePayGeneric(mana.ShardB)
	c.QueuePayGeneric(mana.ShardB)
	if !g.CastSpell(p, soul, c) {
		t.Fatal("Soulflayer could not be cast delving four cards")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	sba(g)
	if g.Card(soul).Zone != engine.Battlefield {
		t.Fatalf("Soulflayer is in %v, want Battlefield", g.Card(soul).Zone)
	}
	if !g.Card(soul).HasKeyword("Flying") {
		t.Errorf("keyword lines = %v, want Flying from the delved flyer %v", g.Card(soul).KeywordLines(), flyer)
	}
	if g.Card(soul).HasKeyword("Trample") {
		t.Errorf("keyword lines = %v, want no Trample: that card was not delved", g.Card(soul).KeywordLines())
	}
}

// TestSharedKeywordsZoneRefusesAZoneThatIsNone proves a zone name that is none
// applies nothing (GO-7).
func TestSharedKeywordsZoneRefusesAZoneThatIsNone(t *testing.T) {
	t.Parallel()

	g, p := textGame(t)
	bad := g.NewCard(scriptDef(t, "Bad", "Creature",
		"S:Mode$ Continuous | AffectedDefined$ Self | AddKeyword$ Flying | SharedKeywordsZone$ Nowhere"), p, engine.Battlefield)
	sba(g)
	if g.Card(bad).HasKeyword("Flying") {
		t.Error("a zone that is none granted flying")
	}
}
