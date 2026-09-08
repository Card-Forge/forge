package cardtype_test

import (
	"errors"
	"fmt"
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// A cut-down vocabulary, inline so the cases below can be read without opening
// another file. The real 576-line file is exercised by the corpus test.
const testTypeList = `[BasicTypes]
Plains
Island:Islands
[LandTypes]
Urza's
Desert:Deserts
[CreatureTypes]
Elf:Elves
Warrior:Warriors
Human:Humans
Time Lord:Time Lords
[SpellTypes]
Arcane
[EnchantmentTypes]
Aura:Auras
Saga:Sagas
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
[SomeSectionThisPortIgnores]
Nonsense
`

func testRegistry(t *testing.T) *cardtype.Registry {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader(testTypeList))
	if err != nil {
		t.Fatalf("LoadRegistry failed: %v", err)
	}
	return reg
}

func TestParseTypeLine(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name      string
		in        string
		want      string
		core      []cardtype.CoreType
		super     []cardtype.Supertype
		subtypes  []string
		permanent bool
	}{
		{
			name:      "creature with two subtypes",
			in:        "Legendary Creature Elf Warrior",
			want:      "Legendary Creature - Elf Warrior",
			core:      []cardtype.CoreType{cardtype.Creature},
			super:     []cardtype.Supertype{cardtype.Legendary},
			subtypes:  []string{"Elf", "Warrior"},
			permanent: true,
		},
		{
			name:      "subtype order is kept as printed",
			in:        "Creature Warrior Elf",
			want:      "Creature - Warrior Elf",
			core:      []cardtype.CoreType{cardtype.Creature},
			subtypes:  []string{"Warrior", "Elf"},
			permanent: true,
		},
		{
			name:      "multiword subtype stays whole",
			in:        "Legendary Creature Time Lord Warrior",
			want:      "Legendary Creature - Time Lord Warrior",
			core:      []cardtype.CoreType{cardtype.Creature},
			super:     []cardtype.Supertype{cardtype.Legendary},
			subtypes:  []string{"Time Lord", "Warrior"},
			permanent: true,
		},
		{
			name:      "core types print in rules order, not script order",
			in:        "Creature Artifact Elf",
			want:      "Artifact Creature - Elf",
			core:      []cardtype.CoreType{cardtype.Artifact, cardtype.Creature},
			subtypes:  []string{"Elf"},
			permanent: true,
		},
		{
			name:     "instant is not a permanent",
			in:       "Instant Arcane",
			want:     "Instant - Arcane",
			core:     []cardtype.CoreType{cardtype.Instant},
			subtypes: []string{"Arcane"},
		},
		{
			name:      "basic land type counts as a land type",
			in:        "Basic Land Island",
			want:      "Basic Land - Island",
			core:      []cardtype.CoreType{cardtype.Land},
			super:     []cardtype.Supertype{cardtype.Basic},
			subtypes:  []string{"Island"},
			permanent: true,
		},
		{
			name:      "kindred admits creature types without being a creature",
			in:        "Kindred Enchantment Elf Aura",
			want:      "Kindred Enchantment - Elf Aura",
			core:      []cardtype.CoreType{cardtype.Kindred, cardtype.Enchantment},
			subtypes:  []string{"Elf", "Aura"},
			permanent: true,
		},
		{
			name:      "a subtype from another category is kept, as Forge keeps it",
			in:        "Creature Elf Equipment",
			want:      "Creature - Elf Equipment",
			core:      []cardtype.CoreType{cardtype.Creature},
			subtypes:  []string{"Elf", "Equipment"},
			permanent: true,
		},
		{
			name:      "a subtype the vocabulary never heard of is kept too",
			in:        "Artifact Creature Killbot",
			want:      "Artifact Creature - Killbot",
			core:      []cardtype.CoreType{cardtype.Artifact, cardtype.Creature},
			subtypes:  []string{"Killbot"},
			permanent: true,
		},
		{
			name:      "a dash in a script type line is a subtype, not a separator",
			in:        "Legendary Creature - Avatar Wizard",
			want:      "Legendary Creature - - Avatar Wizard",
			core:      []cardtype.CoreType{cardtype.Creature},
			super:     []cardtype.Supertype{cardtype.Legendary},
			subtypes:  []string{"-", "Avatar", "Wizard"},
			permanent: true,
		},
		{
			name: "empty line is empty",
			in:   "",
			want: "",
		},
	}

	reg := testRegistry(t)
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			got := cardtype.Parse(reg, tt.in)
			if got.String() != tt.want {
				t.Errorf("Parse(%q).String() = %q, want %q", tt.in, got, tt.want)
			}
			if diff := diffSlices(got.CoreTypes(), tt.core); diff != "" {
				t.Errorf("Parse(%q).CoreTypes(): %s", tt.in, diff)
			}
			if diff := diffSlices(got.Supertypes(), tt.super); diff != "" {
				t.Errorf("Parse(%q).Supertypes(): %s", tt.in, diff)
			}
			if diff := diffSlices(got.Subtypes(), tt.subtypes); diff != "" {
				t.Errorf("Parse(%q).Subtypes(): %s", tt.in, diff)
			}
			if got.IsPermanent() != tt.permanent {
				t.Errorf("Parse(%q).IsPermanent() = %v, want %v", tt.in, got.IsPermanent(), tt.permanent)
			}
			if got.IsEmpty() != (tt.in == "") {
				t.Errorf("Parse(%q).IsEmpty() = %v, want %v", tt.in, got.IsEmpty(), tt.in == "")
			}
		})
	}
}

func TestUnknownTypes(t *testing.T) {
	t.Parallel()

	reg := testRegistry(t)
	tests := []struct {
		in   string
		want []string
	}{
		{"Legendary Creature Elf Warrior", nil},
		{"Artifact Creature Killbot", []string{"Killbot"}},
		// Known to the vocabulary, if in another category: kept by Parse and
		// deliberately not reported as unknown.
		{"Creature Elf Equipment", nil},
		{"Legendary Planeswalker Sivitri", []string{"Sivitri"}},
	}
	for _, tt := range tests {
		if diff := diffSlices(cardtype.UnknownTypes(reg, tt.in), tt.want); diff != "" {
			t.Errorf("UnknownTypes(%q): %s", tt.in, diff)
		}
	}
}

func TestLineQueries(t *testing.T) {
	t.Parallel()

	reg := testRegistry(t)
	line := cardtype.Parse(reg, "Legendary Creature Time Lord Warrior")

	if !line.Has(cardtype.Creature) || line.Has(cardtype.Land) {
		t.Errorf("Has: Creature = %v, Land = %v, want true and false", line.Has(cardtype.Creature), line.Has(cardtype.Land))
	}
	if !line.HasSupertype(cardtype.Legendary) || line.HasSupertype(cardtype.Snow) {
		t.Errorf("HasSupertype: Legendary = %v, Snow = %v, want true and false",
			line.HasSupertype(cardtype.Legendary), line.HasSupertype(cardtype.Snow))
	}
	if !line.HasSubtype("Time Lord") || line.HasSubtype("Time") {
		t.Errorf("HasSubtype: %q = %v, %q = %v, want true and false -- a multiword subtype is one subtype",
			"Time Lord", line.HasSubtype("Time Lord"), "Time", line.HasSubtype("Time"))
	}
	if diff := diffSlices(line.CreatureTypes(reg), []string{"Time Lord", "Warrior"}); diff != "" {
		t.Errorf("CreatureTypes(): %s", diff)
	}
	if got := cardtype.Parse(reg, "Enchantment Aura").CreatureTypes(reg); got != nil {
		t.Errorf("CreatureTypes() on a non-creature = %v, want nil", got)
	}

	other := cardtype.Parse(reg, "Legendary Creature Warrior Time Lord")
	if line.Equal(other) {
		t.Errorf("%q equals %q, but subtype order is part of the printed type line", line, other)
	}
	if !line.Equal(cardtype.Parse(reg, "Legendary Creature Time Lord Warrior")) {
		t.Errorf("%q does not equal a second parse of the same text", line)
	}
}

func TestParsePanicsOnNilRegistry(t *testing.T) {
	t.Parallel()

	// A missing registry is an engine invariant breach, not bad card data
	// (GO-7): no card script can cause it.
	defer func() {
		if recover() == nil {
			t.Error("Parse(nil, ...) did not panic")
		}
	}()
	cardtype.Parse(nil, "Creature Elf")
}

func TestLoadRegistry(t *testing.T) {
	t.Parallel()

	reg := testRegistry(t)

	if got, want := reg.Count(cardtype.CategoryCreature), 4; got != want {
		t.Errorf("Count(CreatureTypes) = %d, want %d", got, want)
	}
	if !reg.IsCreatureType("Elf") || reg.IsCreatureType("Equipment") {
		t.Errorf("IsCreatureType: Elf = %v, Equipment = %v, want true and false",
			reg.IsCreatureType("Elf"), reg.IsCreatureType("Equipment"))
	}
	if !reg.IsLandType("Island") || !reg.IsLandType("Desert") {
		t.Errorf("IsLandType: Island = %v, Desert = %v, want both true -- basic types are land types",
			reg.IsLandType("Island"), reg.IsLandType("Desert"))
	}
	if reg.Is(cardtype.CategoryCreature, "Nonsense") {
		t.Error("a section this port does not know was loaded anyway")
	}

	for _, tt := range []struct{ singular, plural string }{
		{"Elf", "Elves"},
		{"Island", "Islands"},
		{"Creature", "creatures"},  // core types carry their own plurals
		{"Sorcery", "sorceries"},   //
		{"Equipment", "Equipment"}, // no plural in the file: unchanged
		{"Nonexistent", "Nonexistent"},
	} {
		if got := reg.Plural(tt.singular); got != tt.plural {
			t.Errorf("Plural(%q) = %q, want %q", tt.singular, got, tt.plural)
		}
		if got := reg.Singular(tt.plural); got != tt.singular {
			t.Errorf("Singular(%q) = %q, want %q", tt.plural, got, tt.singular)
		}
	}
}

func TestLoadRegistryRejectsBadInput(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name string
		in   string
	}{
		{"no creature types at all", "[BasicTypes]\nPlains\n"},
		{"empty singular", "[CreatureTypes]\nElf\n:Plural\n"},
		{"empty plural", "[CreatureTypes]\nElf\nWarrior:\n"},
	}
	for _, tt := range tests {
		if _, err := cardtype.LoadRegistry(strings.NewReader(tt.in)); err == nil {
			t.Errorf("LoadRegistry(%s) succeeded, want an error", tt.name)
		} else if !errors.Is(err, cardtype.ErrBadTypeList) {
			t.Errorf("LoadRegistry(%s) error = %v, want one wrapping ErrBadTypeList", tt.name, err)
		}
	}
}

func TestCoreTypeTable(t *testing.T) {
	t.Parallel()

	// Declaration order decides print order: Kindred prints before every other
	// core type, exactly as Java's enum orders them.
	order := []cardtype.CoreType{
		cardtype.Kindred, cardtype.Artifact, cardtype.Battle, cardtype.Conspiracy,
		cardtype.Enchantment, cardtype.Creature, cardtype.Dungeon, cardtype.Instant,
		cardtype.Land, cardtype.Phenomenon, cardtype.Plane, cardtype.Planeswalker,
		cardtype.Scheme, cardtype.Sorcery, cardtype.Vanguard,
	}
	for i, ct := range order {
		if int(ct) != i {
			t.Errorf("%v has index %d, want %d -- CoreType declaration order changed", ct, ct, i)
		}
		if got, ok := cardtype.CoreTypeFromName(ct.String()); !ok || got != ct {
			t.Errorf("CoreTypeFromName(%q) = %v, %v; want %v, true", ct, got, ok, ct)
		}
	}

	for _, tt := range []struct {
		ct        cardtype.CoreType
		plural    string
		permanent bool
	}{
		{cardtype.Creature, "creatures", true},
		{cardtype.Sorcery, "sorceries", false},
		{cardtype.Battle, "battles", true},
		{cardtype.Kindred, "kindreds", false},
	} {
		if got := tt.ct.Plural(); got != tt.plural {
			t.Errorf("%v.Plural() = %q, want %q", tt.ct, got, tt.plural)
		}
		if got := tt.ct.IsPermanent(); got != tt.permanent {
			t.Errorf("%v.IsPermanent() = %v, want %v", tt.ct, got, tt.permanent)
		}
	}

	if _, ok := cardtype.CoreTypeFromName("creature"); ok {
		t.Error(`CoreTypeFromName("creature") matched; card scripts are case-sensitive`)
	}
	if _, ok := cardtype.SupertypeFromName("Legendary"); !ok {
		t.Error(`SupertypeFromName("Legendary") did not match`)
	}
}

// diffSlices reports a readable difference, so a failure names input, want and
// got without pulling in a comparison library (TEST-8, TEST-9).
func diffSlices[T comparable](got, want []T) string {
	if slices.Equal(got, want) {
		return ""
	}
	return fmt.Sprintf("got %v, want %v", got, want)
}
