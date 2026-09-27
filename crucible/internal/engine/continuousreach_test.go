package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// layer456TypeList is a small TypeLists.txt: enough basic, nonbasic-land and
// creature types for the Layer 4 expansions and category removals to have
// something to read.
const layer456TypeList = "[BasicTypes]\nPlains\nIsland\nSwamp\nMountain\nForest\n" +
	"[LandTypes]\nDesert\nGate\n[CreatureTypes]\nElf\nGoblin\nTurtle\nAngel\n"

func layer456Registry(t *testing.T) *cardtype.Registry {
	t.Helper()
	reg, err := cardtype.LoadRegistry(strings.NewReader(layer456TypeList))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	return reg
}

// layer456Game is a two-player game whose DB carries the subtype
// vocabulary, the way a corpus-loaded game's does (compile.LoadDB).
func layer456Game(t *testing.T) (*engine.Game, engine.PlayerID, engine.PlayerID) {
	t.Helper()
	db := compile.NewDB(nil).WithTypes(layer456Registry(t))
	g := engine.NewGame(db, javarand.New(1), []string{"a", "b"})
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	return g, p, other
}

// layer456Def compiles a card through the real pipeline: a type line, a
// mana cost ("" for none), printed keywords, one S: line and name=body SVar
// pairs.
func layer456Def(t *testing.T, name, typeLine, cost string, keywords []string, static string, svars ...string) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(layer456Registry(t), typeLine)
	if strings.Contains(typeLine, "Creature") {
		raw.Faces[0].Power, raw.Faces[0].Toughness = "2", "2"
	}
	if cost != "" {
		raw.Faces[0].ManaCost = mana.MustParse(cost)
	}
	raw.Faces[0].Keywords = keywords
	if static != "" {
		raw.Faces[0].Statics = []string{static}
	}
	for i := 0; i+1 < len(svars); i += 2 {
		raw.Faces[0].SVars.Set(svars[i], svars[i+1])
	}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

func layer456Creature(t *testing.T, cost string, keywords ...string) *compile.Card {
	t.Helper()
	return layer456Def(t, "Test Elf", "Creature Elf", cost, keywords, "")
}

// TestLayersReachEnchantedCreatureThroughAffectedDefined proves an Aura's
// AffectedDefined$ Enchanted line (Angelic Destiny's shape) changes the
// enchanted creature's type, color and keywords, and nothing else.
func TestLayersReachEnchantedCreatureThroughAffectedDefined(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	aura := g.NewCard(layer456Def(t, "Test Destiny", "Enchantment Aura", "", nil,
		"Mode$ Continuous | AffectedDefined$ Enchanted | Affected$ Creature | AddKeyword$ Flying & First Strike | AddType$ Angel | SetColor$ White"),
		p, engine.Battlefield)
	enchanted := g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)
	bystander := g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)
	g.Attach(aura, enchanted)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	c := g.Card(enchanted)
	if !c.HasKeyword("Flying") || !c.HasKeyword("First Strike") {
		t.Errorf("enchanted KeywordLines() = %v, want Flying and First Strike", c.KeywordLines())
	}
	if typ := c.Type(); !typ.HasSubtype("Angel") || !typ.HasSubtype("Elf") {
		t.Errorf("enchanted Type() = %q, want Angel added beside Elf", typ)
	}
	if colors := c.Colors(); colors != mana.White {
		t.Errorf("enchanted Colors() = %v, want White (SetColor$ overwrites Green)", colors)
	}
	b := g.Card(bystander)
	if b.HasKeyword("Flying") || b.Type().HasSubtype("Angel") || b.Colors() != mana.Green {
		t.Errorf("bystander changed: keywords %v, type %q, colors %v", b.KeywordLines(), b.Type(), b.Colors())
	}
}

// TestLayersUnattachedEquipmentReachesNothing proves AffectedDefined$
// Equipped on an Equipment attached to nothing affects no card -- not the
// Equipment itself, not every creature.
func TestLayersUnattachedEquipmentReachesNothing(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	g.NewCard(layer456Def(t, "Test Blade", "Artifact Equipment", "", nil,
		"Mode$ Continuous | AffectedDefined$ Equipped | AddKeyword$ Trample"), p, engine.Battlefield)
	creature := g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if g.Card(creature).HasKeyword("Trample") {
		t.Error("HasKeyword(\"Trample\") = true, want nothing reached by an unattached Equipment")
	}
}

// TestLayersSelfLineFollowsIsPresent proves StaticAbility.checkConditions'
// IsPresent$ gates an AffectedDefined$ Self line: "has first strike as long
// as you control an artifact" is off without one and on with one.
func TestLayersSelfLineFollowsIsPresent(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	self := g.NewCard(layer456Def(t, "Test Knight", "Creature Elf", "W", nil,
		"Mode$ Continuous | AffectedDefined$ Self | AddKeyword$ First Strike | IsPresent$ Artifact.YouCtrl"),
		p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if g.Card(self).HasKeyword("First Strike") {
		t.Error("with no artifact: HasKeyword(\"First Strike\") = true, want the IsPresent$ gate closed")
	}

	g.NewCard(artifactDefT(t), p, engine.Battlefield)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if !g.Card(self).HasKeyword("First Strike") {
		t.Error("with an artifact: HasKeyword(\"First Strike\") = false, want the IsPresent$ gate open")
	}
}

// TestLayersSelfLineFollowsCheckSVar proves the CheckSVar$ chain gates a
// line, its SVar resolved through Count$Valid: a type change that holds
// only while the controller has two or more creatures.
func TestLayersSelfLineFollowsCheckSVar(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	self := g.NewCard(layer456Def(t, "Test Shifter", "Creature Elf", "G", nil,
		"Mode$ Continuous | AffectedDefined$ Self | AddType$ Goblin | CheckSVar$ X | SVarCompare$ GE2",
		"X", "Count$Valid Creature.YouCtrl"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if g.Card(self).Type().HasSubtype("Goblin") {
		t.Error("one creature: Type() has Goblin, want the CheckSVar$ gate closed")
	}

	g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if !g.Card(self).Type().HasSubtype("Goblin") {
		t.Error("two creatures: Type() lacks Goblin, want the CheckSVar$ gate open")
	}
}

// TestLayersEffectZoneGraveyardIsOffOnTheBattlefield proves zonesCheck: a
// line that functions from the graveyard (EffectZone$ Graveyard) does
// nothing while its host is in play.
func TestLayersEffectZoneGraveyardIsOffOnTheBattlefield(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	g.NewCard(layer456Def(t, "Test Graveyard Anthem", "Creature Elf", "G", nil,
		"Mode$ Continuous | EffectZone$ Graveyard | Affected$ Creature.YouCtrl | AddKeyword$ Haste"),
		p, engine.Battlefield)
	creature := g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if g.Card(creature).HasKeyword("Haste") {
		t.Error("HasKeyword(\"Haste\") = true, want an EffectZone$ Graveyard line off while its host is in play")
	}
}

// TestLayersAffectedZoneReachesGraveyardAndClears proves AffectedZone$: a
// battlefield host grants a keyword to creature cards in its controller's
// graveyard, and the grant is gone once the host leaves -- the
// off-battlefield clear, since Move never touches a graveyard card's
// KeywordMod.
func TestLayersAffectedZoneReachesGraveyardAndClears(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	host := g.NewCard(layer456Def(t, "Test Necromancy", "Enchantment", "", nil,
		"Mode$ Continuous | AffectedZone$ Graveyard | Affected$ Creature.YouOwn | AddKeyword$ Haste"),
		p, engine.Battlefield)
	dead := g.NewCard(layer456Creature(t, "G"), p, engine.Graveyard)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if !g.Card(dead).HasKeyword("Haste") {
		t.Fatal("graveyard creature: HasKeyword(\"Haste\") = false, want the AffectedZone$ grant")
	}

	g.Move(host, engine.Exile, p)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if g.Card(dead).HasKeyword("Haste") {
		t.Error("after the host left: HasKeyword(\"Haste\") = true, want the grant cleared")
	}
}

// TestLayersCharacteristicDefiningAffectsHostOnly proves a CDA line reaches
// its host alone, whatever its Affected$ says.
func TestLayersCharacteristicDefiningAffectsHostOnly(t *testing.T) {
	t.Parallel()

	g, p, _ := layer456Game(t)
	host := g.NewCard(layer456Def(t, "Test Courier", "Creature Elf", "1", nil,
		"Mode$ Continuous | Affected$ Card.Self | CharacteristicDefining$ True | SetColor$ All"),
		p, engine.Battlefield)
	other := g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if colors := g.Card(host).Colors(); colors != mana.AllColors {
		t.Errorf("host Colors() = %v, want every color", colors)
	}
	if colors := g.Card(other).Colors(); colors != mana.Green {
		t.Errorf("other Colors() = %v, want Green untouched", colors)
	}
}

// TestLayersLineWithoutAffectedReachesEveryPermanent proves Java's default
// affected set: a line naming neither Affected$ nor AffectedDefined$ reaches
// every permanent on the battlefield, both players'.
func TestLayersLineWithoutAffectedReachesEveryPermanent(t *testing.T) {
	t.Parallel()

	g, p, other := layer456Game(t)
	g.NewCard(layer456Def(t, "Test Blanket", "Enchantment", "", nil,
		"Mode$ Continuous | AddKeyword$ Shroud"), p, engine.Battlefield)
	mine := g.NewCard(layer456Creature(t, "G"), p, engine.Battlefield)
	theirs := g.NewCard(layer456Creature(t, "G"), other, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	for _, id := range []engine.CardID{mine, theirs} {
		if !slices.Contains(g.Card(id).KeywordLines(), "Shroud") {
			t.Errorf("card %d KeywordLines() = %v, want Shroud", id, g.Card(id).KeywordLines())
		}
	}
}
