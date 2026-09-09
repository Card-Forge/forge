package carddb_test

import (
	"errors"
	"fmt"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// A cut-down vocabulary, inline so a case can be read without opening another
// file. The real one is exercised by the corpus test.
const testTypeList = `[BasicTypes]
Island:Islands
[LandTypes]
Cave:Caves
[CreatureTypes]
Elf:Elves
Warrior:Warriors
Human:Humans
[SpellTypes]
Arcane
[EnchantmentTypes]
Aura:Auras
[ArtifactTypes]
Equipment
[WalkerTypes]
Jace
[DungeonTypes]
Undercity
[BattleTypes]
Siege
[PlanarTypes]
Bolas's Meditation Realm
`

func testRegistry(t *testing.T) *cardtype.Registry {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader(testTypeList))
	if err != nil {
		t.Fatalf("LoadRegistry failed: %v", err)
	}
	return reg
}

func parse(t *testing.T, script string) *carddb.Card {
	t.Helper()

	card, err := carddb.ParseScript(testRegistry(t), "fixture", []byte(script))
	if err != nil {
		t.Fatalf("ParseScript failed: %v", err)
	}
	return card
}

func TestParseSingleFaceCard(t *testing.T) {
	t.Parallel()

	card := parse(t, `#comment
Name:Grizzly Bears
ManaCost:1 G
Types:Creature Bear
PT:2/2
K:Trample
A:AB$ Pump | Cost$ G
T:Mode$ ChangesZone | Origin$ Any
S:Mode$ Continuous | Affected$ Creature.YouCtrl
R:Event$ Moved | ValidCard$ Card.Self
SVar:X:Count$CardsInYourHand
Oracle:Trample
`)

	face := card.Primary()
	if !face.Present || face.Name != "Grizzly Bears" {
		t.Fatalf("primary face = %+v, want a present face named Grizzly Bears", face.Present)
	}
	if got, want := face.ManaCost.String(), "{1}{G}"; got != want {
		t.Errorf("ManaCost = %q, want %q", got, want)
	}
	if !face.Type.Has(cardtype.Creature) {
		t.Errorf("Type = %q, want a creature", face.Type)
	}
	if face.Power != "2" || face.Toughness != "2" {
		t.Errorf("PT = %q/%q, want 2/2", face.Power, face.Toughness)
	}
	if got, want := card.SplitType, carddb.SplitNone; got != want {
		t.Errorf("SplitType = %v, want %v", got, want)
	}
	for _, tt := range []struct {
		name string
		got  []string
	}{
		{"Abilities", face.Abilities},
		{"Keywords", face.Keywords},
		{"Triggers", face.Triggers},
		{"Statics", face.Statics},
		{"Replacements", face.Replacements},
	} {
		if len(tt.got) != 1 {
			t.Errorf("%s = %v, want exactly one line", tt.name, tt.got)
		}
	}
	if got, ok := face.SVars.Get("X"); !ok || got != "Count$CardsInYourHand" {
		t.Errorf("SVars.Get(X) = %q, %v; want the count expression", got, ok)
	}
	// Ability lines are kept verbatim: nothing here interprets them (PORT-2).
	if got, want := face.Abilities[0], "AB$ Pump | Cost$ G"; got != want {
		t.Errorf("Abilities[0] = %q, want %q -- the line must survive unparsed", got, want)
	}
}

func TestFaceSwitching(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name   string
		script string
		faces  []int
		names  map[int]string
		split  carddb.SplitType
	}{
		{
			name:   "ALTERNATE moves to face 1 and takes no value",
			script: "Name:Front\nManaCost:R\nTypes:Instant\nAlternateMode:DoubleFaced\nALTERNATE\nName:Back\nManaCost:no cost\nTypes:Land\n",
			faces:  []int{0, 1},
			names:  map[int]string{0: "Front", 1: "Back"},
			split:  carddb.SplitTransform,
		},
		{
			name:   "SPECIALIZE moves to the colour's face",
			script: "Name:Base\nManaCost:2\nTypes:Creature Human\nAlternateMode:Specialize\nSPECIALIZE:WHITE\nName:White One\nSPECIALIZE:GREEN\nName:Green One\n",
			faces:  []int{0, 2, 6},
			names:  map[int]string{0: "Base", 2: "White One", 6: "Green One"},
			split:  carddb.SplitSpecialize,
		},
		{
			name:   "AlternateMode alone does not switch faces",
			script: "Name:Only\nManaCost:R\nTypes:Instant\nAlternateMode:Split\nK:Fuse\n",
			faces:  []int{0},
			names:  map[int]string{0: "Only"},
			split:  carddb.SplitSplit,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			card := parse(t, tt.script)
			if diff := diffInts(card.PresentFaces(), tt.faces); diff != "" {
				t.Errorf("PresentFaces(): %s", diff)
			}
			for index, want := range tt.names {
				if got := card.Faces[index].Name; got != want {
					t.Errorf("face %d name = %q, want %q", index, got, want)
				}
			}
			if card.SplitType != tt.split {
				t.Errorf("SplitType = %v, want %v", card.SplitType, tt.split)
			}
		})
	}
}

// The last AlternateMode wins, which is what a corpus card relies on:
// paradox_shaper_omit_variables declares Prepare twice.
func TestRepeatedAlternateModeKeepsTheLast(t *testing.T) {
	t.Parallel()

	card := parse(t, "Name:X\nManaCost:R\nTypes:Instant\nAlternateMode:Prepare\nAlternateMode:Split\n")
	if got, want := card.SplitType, carddb.SplitSplit; got != want {
		t.Errorf("SplitType = %v, want %v", got, want)
	}
}

func TestVariantRecursesOntoItsOwnFace(t *testing.T) {
	t.Parallel()

	card := parse(t, "Name:Attraction\nManaCost:2\nTypes:Artifact\nK:Base\nVariant:L1:K:Lights One\nVariant:L1:A:AB$ Draw\nVariant:L2:K:Lights Two\n")

	face := card.Primary()
	if diff := diffStrings(face.Keywords, []string{"Base"}); diff != "" {
		t.Errorf("primary keywords: %s -- a variant line must not land on the base face", diff)
	}
	if diff := diffStrings(card.SupportedVariants, []string{"L1", "L2"}); diff != "" {
		t.Errorf("SupportedVariants: %s", diff)
	}
	l1, ok := face.Variants["L1"]
	if !ok {
		t.Fatalf("variant L1 missing; got %v", face.Variants)
	}
	if diff := diffStrings(l1.Keywords, []string{"Lights One"}); diff != "" {
		t.Errorf("L1 keywords: %s", diff)
	}
	if diff := diffStrings(l1.Abilities, []string{"AB$ Draw"}); diff != "" {
		t.Errorf("L1 abilities: %s -- a second line for the same variant reuses its face", diff)
	}
}

func TestCopyFaceFromResolvesAfterTheCorpusLoads(t *testing.T) {
	t.Parallel()

	reg := testRegistry(t)
	source, err := carddb.ParseScript(reg, "source", []byte("Name:Source\nManaCost:G\nTypes:Creature Elf\nK:Reach\n"))
	if err != nil {
		t.Fatalf("parse source: %v", err)
	}
	copier, err := carddb.ParseScript(reg, "copier", []byte("Name:Copier\nManaCost:1 G\nTypes:Creature Elf\nALTERNATE\nCopyFaceFrom:Source\n"))
	if err != nil {
		t.Fatalf("parse copier: %v", err)
	}

	// Before resolution the face is deliberately absent: it belongs to another
	// card that may not have been read yet.
	if copier.Faces[carddb.FaceAlternate].Present {
		t.Error("alternate face present before ResolvePlaceholders")
	}
	if got, want := copier.PlaceholderFaces[carddb.FaceAlternate], "Source"; got != want {
		t.Errorf("PlaceholderFaces[1] = %q, want %q", got, want)
	}

	cards := []*carddb.Card{source, copier}
	byName := map[string]*carddb.Card{"Source": source, "Copier": copier}
	if err := carddb.ResolvePlaceholders(cards, byName); err != nil {
		t.Fatalf("ResolvePlaceholders: %v", err)
	}
	if got, want := copier.Faces[carddb.FaceAlternate].Name, "Source"; got != want {
		t.Errorf("resolved face name = %q, want %q", got, want)
	}

	byName = map[string]*carddb.Card{"Copier": copier}
	if err := carddb.ResolvePlaceholders([]*carddb.Card{copier}, byName); err == nil {
		t.Error("ResolvePlaceholders accepted a CopyFaceFrom naming no card")
	}
}

// Java keeps SVars in a TreeMap with CASE_INSENSITIVE_ORDER, so the name is
// matched without case, the first spelling is the one kept, and iteration is
// sorted rather than in script order. The canonical dump depends on all three.
func TestSVarsFollowJavasTreeMap(t *testing.T) {
	t.Parallel()

	card := parse(t, "Name:X\nManaCost:R\nTypes:Instant\nSVar:Zed:1\nSVar:apple:2\nSVar:ZED:3\nSVar:Beta:4\n")
	svars := card.Primary().SVars

	if got, want := svars.Len(), 3; got != want {
		t.Errorf("Len() = %d, want %d -- Zed and ZED are one variable", got, want)
	}
	if got, _ := svars.Get("zEd"); got != "3" {
		t.Errorf("Get(zEd) = %q, want %q -- the later value wins", got, "3")
	}
	if diff := diffStrings(svars.Names(), []string{"apple", "Beta", "Zed"}); diff != "" {
		t.Errorf("Names(): %s -- sorted, ignoring case, first spelling kept", diff)
	}
}

func TestCardLevelKeys(t *testing.T) {
	t.Parallel()

	card := parse(t, `Name:Legend
ManaCost:2 W
Types:Legendary Creature Human
K:Partner with:Other Legend
AI:RemoveDeck:All,Random
DeckHints:Ability$Graveyard
DeckNeeds:Type$Elf
DeckHas:Keyword$Trample
MeldPair:Other Legend
HandLifeModifier:+1/+2
SETCOLORID:2
A:AB$ Token | TokenScript$ w_1_1_soldier,w_2_2_knight | Cost$ W
`)

	if got, want := card.PartnerWith, "Other Legend"; got != want {
		t.Errorf("PartnerWith = %q, want %q", got, want)
	}
	if !card.RemovedFromAIDecks || !card.RemovedFromRandomDecks || card.RemovedFromNonCommanderDecks {
		t.Errorf("AI hints = %v/%v/%v, want true/true/false",
			card.RemovedFromAIDecks, card.RemovedFromRandomDecks, card.RemovedFromNonCommanderDecks)
	}
	if card.DeckHints == "" || card.DeckNeeds == "" || card.DeckHas == "" {
		t.Errorf("deck metadata = %q/%q/%q, want all three kept", card.DeckHints, card.DeckNeeds, card.DeckHas)
	}
	if got, want := card.MeldWith, "Other Legend"; got != want {
		t.Errorf("MeldWith = %q, want %q", got, want)
	}
	if got, want := card.HandLifeModifier, "+1/+2"; got != want {
		t.Errorf("HandLifeModifier = %q, want %q", got, want)
	}
	if got, want := card.SetColorID, 2; got != want {
		t.Errorf("SetColorID = %d, want %d", got, want)
	}
	if diff := diffStrings(card.Tokens, []string{"w_1_1_soldier", "w_2_2_knight"}); diff != "" {
		t.Errorf("Tokens: %s", diff)
	}
}

// Java's ColorSet.fromNames trims nothing and ignores what it does not
// recognise, so a stray space silently drops a colour. Pinned rather than
// fixed: the static database has to match the oracle first (PORT-7).
func TestColorsFollowJavasQuirks(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name  string
		value string
		want  string
	}{
		{"a space after the comma loses the colour", "black, red", "B"},
		{"no space keeps both", "black,red", "BR"},
		{"an unrecognised word contributes nothing", "puce", "C"},
	}
	for _, tt := range tests {
		card := parse(t, "Name:X\nManaCost:2\nTypes:Artifact\nColors:"+tt.value+"\n")
		face := card.Primary()
		if !face.HasColors {
			t.Errorf("Colors:%s left HasColors false", tt.value)
		}
		if got := face.Colors.String(); got != tt.want {
			t.Errorf("Colors:%s = %s, want %s -- %s", tt.value, got, tt.want, tt.name)
		}
	}
}

func TestColorsOverride(t *testing.T) {
	t.Parallel()

	card := parse(t, "Name:X\nManaCost:2\nTypes:Artifact\nColors:red,green\n")
	face := card.Primary()
	if !face.HasColors {
		t.Fatal("HasColors = false, want true -- a declared override is not the same as none")
	}
	if got, want := face.Colors.String(), "RG"; got != want {
		t.Errorf("Colors = %s, want %s", got, want)
	}

	// Absent Colors: leaves the override undeclared, so the colour is derived
	// from the mana cost later rather than being wrongly recorded as colorless.
	if parse(t, "Name:Y\nManaCost:R\nTypes:Instant\n").Primary().HasColors {
		t.Error("HasColors = true for a card with no Colors: line")
	}
}

func TestParseRejectsBrokenScripts(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name   string
		script string
		want   string
	}{
		{"unknown key", "Name:X\nManaCost:R\nTypes:Instant\nWibble:1\n", `unknown key "Wibble"`},
		{"SVar without a value", "Name:X\nManaCost:R\nTypes:Instant\nSVar:Lonely\n", "a name but no value"},
		{"PT with no slash", "Name:X\nManaCost:R\nTypes:Creature Elf\nPT:22\n", "is not power/toughness"},
		{"PT with two slashes", "Name:X\nManaCost:R\nTypes:Creature Elf\nPT:2/2/2\n", "is not power/toughness"},
		{"PT that is not a number", "Name:X\nManaCost:R\nTypes:Creature Elf\nPT:X/2\n", `PT: power "X"`},
		{"unknown split mode", "Name:X\nManaCost:R\nTypes:Instant\nAlternateMode:Wobble\n", `unknown mode "Wobble"`},
		{"unknown specialize colour", "Name:X\nManaCost:R\nTypes:Instant\nSPECIALIZE:PUCE\n", `unknown colour "PUCE"`},
		{"variant with no name", "Name:X\nManaCost:R\nTypes:Instant\nVariant:Flying\n", "no variant name"},
		{"variant with an empty name", "Name:X\nManaCost:R\nTypes:Instant\nVariant::K:Flying\n", "no variant name"},
		{"entry before Name", "ManaCost:R\nTypes:Instant\n", `"ManaCost" before any Name: line`},
		{"bad mana cost", "Name:X\nManaCost:Q\nTypes:Instant\n", "unknown mana symbol"},
		{"bad SETCOLORID", "Name:X\nManaCost:R\nTypes:Instant\nSETCOLORID:blue\n", "SETCOLORID"},
	}

	reg := testRegistry(t)
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			card, err := carddb.ParseScript(reg, "fixture", []byte(tt.script))
			if err == nil {
				t.Fatalf("ParseScript accepted %q, got card %+v", tt.script, card)
			}
			if !errors.Is(err, carddb.ErrBadScript) {
				t.Errorf("error = %v, want one wrapping ErrBadScript", err)
			}
			if !strings.Contains(err.Error(), tt.want) {
				t.Errorf("error = %v, want it to contain %q", err, tt.want)
			}
			if !strings.Contains(err.Error(), "fixture:") {
				t.Errorf("error = %v, want the file and line named", err)
			}
			if card != nil {
				t.Errorf("ParseScript returned a card alongside an error")
			}
		})
	}
}

// Two corpus lines match no key in Forge's switch and are dropped silently.
// Crucible drops the same two knowingly, and errors on anything else.
func TestKnownCorpusDefectsAreIgnored(t *testing.T) {
	t.Parallel()

	card := parse(t, "Name:X\nManaCost:R\nTypes:Instant\nODeckHints:Ability$Graveyard\nDBCleanup:DB$ Cleanup\n")
	if card.DeckHints != "" {
		t.Errorf("DeckHints = %q, want empty -- ODeckHints is a typo, not an alias", card.DeckHints)
	}
}

func TestParsePanicsOnNilRegistry(t *testing.T) {
	t.Parallel()

	defer func() {
		if recover() == nil {
			t.Error("ParseScript(nil, ...) did not panic")
		}
	}()
	_, _ = carddb.ParseScript(nil, "fixture", []byte("Name:X\n"))
}

func TestSplitTypeNames(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		script string
		want   carddb.SplitType
	}{
		{"DoubleFaced", carddb.SplitTransform},
		{"Transform", carddb.SplitTransform},
		{"Adventure", carddb.SplitAdventure},
		{"None", carddb.SplitNone},
	} {
		got, ok := carddb.SplitTypeFromScript(tt.script)
		if !ok || got != tt.want {
			t.Errorf("SplitTypeFromScript(%q) = %v, %v; want %v, true", tt.script, got, ok, tt.want)
		}
	}
	if _, ok := carddb.SplitTypeFromScript("Nonsense"); ok {
		t.Error(`SplitTypeFromScript("Nonsense") reported a mode`)
	}
	if got, want := carddb.SplitTransform.String(), "Transform"; got != want {
		t.Errorf("SplitTransform.String() = %q, want %q", got, want)
	}
}

func diffStrings(got, want []string) string {
	if len(got) == len(want) {
		same := true
		for i := range got {
			if got[i] != want[i] {
				same = false
				break
			}
		}
		if same {
			return ""
		}
	}
	return "got " + strings.Join(got, ",") + ", want " + strings.Join(want, ",")
}

func diffInts(got, want []int) string {
	if len(got) == len(want) {
		same := true
		for i := range got {
			if got[i] != want[i] {
				same = false
				break
			}
		}
		if same {
			return ""
		}
	}
	return fmt.Sprintf("got %v, want %v", got, want)
}

// A characteristic-defining power reduces to a number, and the sign that binds
// the star goes with it. The numbers are what the oracle dump diffs, so they
// are pinned here per form rather than only in aggregate.
func TestPowerToughnessNumbers(t *testing.T) {
	t.Parallel()

	tests := []struct {
		pt               string
		power, toughness int
	}{
		{"2/3", 2, 3},
		{"0/0", 0, 0},
		{"*/*", 0, 0},
		{"1+*/1+*", 1, 1},
		{"*+1/*", 1, 0},
		{"7-*/2", 7, 2},
		{"-1/2", -1, 2},
	}

	reg := testRegistry(t)
	for _, tt := range tests {
		t.Run(tt.pt, func(t *testing.T) {
			t.Parallel()

			card, err := carddb.ParseScript(reg, "fixture", []byte("Name:X\nManaCost:R\nTypes:Creature Elf\nPT:"+tt.pt+"\n"))
			if err != nil {
				t.Fatalf("ParseScript failed: %v", err)
			}
			face := card.Primary()
			if got := face.IntPower(); got != tt.power {
				t.Errorf("IntPower() = %d, want %d", got, tt.power)
			}
			if got := face.IntToughness(); got != tt.toughness {
				t.Errorf("IntToughness() = %d, want %d", got, tt.toughness)
			}
		})
	}
}

// A face with no PT: line is not a 0/0. Java leaves both fields at
// Integer.MAX_VALUE, and callers rely on telling the two apart.
func TestPowerToughnessUnset(t *testing.T) {
	t.Parallel()

	face := parse(t, "Name:X\nManaCost:R\nTypes:Instant\n").Primary()
	if got := face.IntPower(); got != carddb.PTUnset {
		t.Errorf("IntPower() = %d, want PTUnset (%d)", got, carddb.PTUnset)
	}
	if got := face.IntToughness(); got != carddb.PTUnset {
		t.Errorf("IntToughness() = %d, want PTUnset (%d)", got, carddb.PTUnset)
	}
}
