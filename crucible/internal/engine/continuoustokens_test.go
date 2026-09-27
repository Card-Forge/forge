package engine_test

import (
	"slices"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// TestLayersAddTypeChosenTypeUsesHostChoice proves AddType$ ChosenType
// becomes the host's own ChooseType pick.
func TestLayersAddTypeChosenTypeUsesHostChoice(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	host := g.NewCard(layer456Def(t, "Test Conspiracy", "Enchantment", "", nil,
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddType$ ChosenType"), p, engine.Battlefield)
	g.Card(host).Memory.SetChosenType("Goblin", false)
	creature := g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if typ := g.Card(creature).Type(); !typ.HasSubtype("Goblin") || typ.HasSubtype("ChosenType") {
		t.Errorf("Type() = %q, want Goblin added", typ)
	}
}

// TestLayersAddTypeAllBasicLandTypeReadsVocabulary proves AllBasicLandType
// expands to the registry's [BasicTypes] section.
func TestLayersAddTypeAllBasicLandTypeReadsVocabulary(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	g.NewCard(layer456Def(t, "Test Prismatic Omen", "Enchantment", "", nil,
		"Mode$ Continuous | Affected$ Land.YouCtrl | AddType$ AllBasicLandType"), p, engine.Battlefield)
	land := g.NewCard(layer456Def(t, "Test Desert", "Land Desert", "", nil, ""), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	typ := g.Card(land).Type()
	for _, basic := range []string{"Plains", "Island", "Swamp", "Mountain", "Forest"} {
		if !typ.HasSubtype(basic) {
			t.Errorf("Type() = %q, want %s", typ, basic)
		}
	}
}

// TestLayersRemoveCreatureTypesThenAddType proves the "is a Turtle" shape:
// RemoveCreatureTypes$ strips the printed Elf before AddType$ adds Turtle,
// and the card stays a creature.
func TestLayersRemoveCreatureTypesThenAddType(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	aura := g.NewCard(layer456Def(t, "Test Turtle Aura", "Enchantment Aura", "", nil,
		"Mode$ Continuous | AffectedDefined$ Enchanted | AddType$ Turtle | RemoveCreatureTypes$ True"),
		p, engine.Battlefield)
	creature := g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)
	g.Attach(aura, creature)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	typ := g.Card(creature).Type()
	if !typ.HasSubtype("Turtle") || typ.HasSubtype("Elf") || !typ.Has(cardtype.Creature) {
		t.Errorf("Type() = %q, want Creature Turtle, Elf removed", typ)
	}
}

// TestLayersSetColorChosenColorOverwrites proves SetColor$ ChosenColor is
// the host's own chosen color, replacing the printed one.
func TestLayersSetColorChosenColorOverwrites(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	host := g.NewCard(layer456Def(t, "Test Painter", "Enchantment", "", nil,
		"Mode$ Continuous | Affected$ Creature | SetColor$ ChosenColor"), p, engine.Battlefield)
	g.Card(host).Memory.SetChosenColors(mana.Blue)
	creature := g.NewCard(layer456Creature(t, "R"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if colors := g.Card(creature).Colors(); colors != mana.Blue {
		t.Errorf("Colors() = %v, want Blue", colors)
	}
}

// TestLayersKeywordChosenColorSubstituted proves a keyword token naming
// ChosenColor/chosenColor is rewritten with the host's chosen color, both
// spellings.
func TestLayersKeywordChosenColorSubstituted(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	host := g.NewCard(layer456Def(t, "Test Ward", "Enchantment", "", nil,
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddKeyword$ Protection:Card.ChosenColor:chosenColor"),
		p, engine.Battlefield)
	g.Card(host).Memory.SetChosenColors(mana.Red)
	creature := g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if lines := g.Card(creature).KeywordLines(); !slices.Contains(lines, "Protection:Card.Red:red") {
		t.Errorf("KeywordLines() = %v, want Protection:Card.Red:red", lines)
	}
}

// TestLayersKeywordAllColorsExpands proves AllColors expands to one keyword
// per color, WUBRG order.
func TestLayersKeywordAllColorsExpands(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	g.NewCard(layer456Def(t, "Test Prism", "Enchantment", "", nil,
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddKeyword$ Protection from allColors"),
		p, engine.Battlefield)
	creature := g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	want := []string{
		"Protection from white", "Protection from blue", "Protection from black",
		"Protection from red", "Protection from green",
	}
	if got := g.Card(creature).KeywordLines(); !slices.Equal(got, want) {
		t.Errorf("KeywordLines() = %v, want %v", got, want)
	}
}

// TestLayersKeywordCardColorsIsPerAffectedCard proves CardColors is
// resolved against each affected card's own colors, not the host's.
func TestLayersKeywordCardColorsIsPerAffectedCard(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	g.NewCard(layer456Def(t, "Test Veil", "Enchantment", "", nil,
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddKeyword$ Hexproof:CardColors"),
		p, engine.Battlefield)
	red := g.NewCard(layer456Creature(t, "R"), p, engine.Battlefield)
	gold := g.NewCard(layer456Creature(t, "W U"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := g.Card(red).KeywordLines(); !slices.Equal(got, []string{"Hexproof:Red"}) {
		t.Errorf("red KeywordLines() = %v, want [Hexproof:Red]", got)
	}
	if got := g.Card(gold).KeywordLines(); !slices.Equal(got, []string{"Hexproof:White", "Hexproof:Blue"}) {
		t.Errorf("gold KeywordLines() = %v, want [Hexproof:White Hexproof:Blue]", got)
	}
}

// TestLayersKeywordYourBasicExpands proves YourBasic becomes one keyword per
// basic land type among the controller's lands.
func TestLayersKeywordYourBasicExpands(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	g.NewCard(layer456Def(t, "Test Walker", "Enchantment", "", nil,
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddKeyword$ Landwalk:YourBasic"),
		p, engine.Battlefield)
	g.NewCard(layer456Def(t, "Test Island", "Basic Land Island", "", nil, ""), p, engine.Battlefield)
	creature := g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := g.Card(creature).KeywordLines(); !slices.Equal(got, []string{"Landwalk:Island"}) {
		t.Errorf("KeywordLines() = %v, want [Landwalk:Island]", got)
	}
}

// TestLayersRemoveAllAbilitiesRemovesKeywords proves the keyword half of
// "loses all abilities": a printed Flying is gone, and a keyword the same
// line adds survives, since removal runs before addition.
func TestLayersRemoveAllAbilitiesRemovesKeywords(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	aura := g.NewCard(layer456Def(t, "Test Frog Curse", "Enchantment Aura", "", nil,
		"Mode$ Continuous | AffectedDefined$ Enchanted | RemoveAllAbilities$ True | AddKeyword$ Defender"),
		p, engine.Battlefield)
	creature := g.NewCard(layer456Creature(t, "G", "Flying"), p, engine.Battlefield)
	g.Attach(aura, creature)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := g.Card(creature).KeywordLines(); !slices.Equal(got, []string{"Defender"}) {
		t.Errorf("KeywordLines() = %v, want [Defender]", got)
	}
}

// TestLayersKeywordCardManaCostDoesNothing proves a token this port cannot
// resolve (CardManaCost needs ManaCost.getShortString) leaves the whole line
// unapplied rather than granting the literal marker.
func TestLayersKeywordCardManaCostDoesNothing(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	g.NewCard(layer456Def(t, "Test Scavenger", "Enchantment", "", nil,
		"Mode$ Continuous | AffectedZone$ Graveyard | Affected$ Creature.YouOwn | AddKeyword$ Scavenge:CardManaCost & Haste"),
		p, engine.Battlefield)
	dead := g.NewCard(layer456Creature(t, "G"), p, engine.Graveyard)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := g.Card(dead).KeywordLines(); len(got) != 0 {
		t.Errorf("KeywordLines() = %v, want the whole line unapplied", got)
	}
}

// TestLayersAddAllCreatureTypesDoesNothing proves AddAllCreatureTypes$,
// which needs a cardtype.Line flag this port lacks, applies no part of its
// line's type change.
func TestLayersAddAllCreatureTypesDoesNothing(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	g.NewCard(layer456Def(t, "Test Amoeboid", "Enchantment", "", nil,
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddAllCreatureTypes$ True | AddType$ Angel"),
		p, engine.Battlefield)
	creature := g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if typ := g.Card(creature).Type(); typ.HasSubtype("Angel") {
		t.Errorf("Type() = %q, want the AddAllCreatureTypes$ line unapplied", typ)
	}
}
