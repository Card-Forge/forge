package valid_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// "," is OR, "+" is AND, and the properties do not distribute across
// alternatives -- the rule that decides thousands of targeting restrictions.
func TestParseStructure(t *testing.T) {
	t.Parallel()

	spec := valid.Parse("Instant.YouCtrl+nonToken,Sorcery,Card.!token")
	if got := len(spec.Alternatives); got != 3 {
		t.Fatalf("parsed %d alternatives, want 3: %+v", got, spec)
	}

	first := spec.Alternatives[0]
	if first.Base.Name != "Instant" || first.Base.Negated {
		t.Errorf("base = %+v, want Instant, not negated", first.Base)
	}
	if len(first.Properties) != 2 ||
		first.Properties[0].Name != "YouCtrl" || first.Properties[1].Name != "nonToken" {
		t.Errorf("properties = %+v, want YouCtrl and nonToken", first.Properties)
	}
	if got := len(spec.Alternatives[1].Properties); got != 0 {
		t.Errorf("second alternative has %d properties, want 0 -- they do not distribute", got)
	}
	if p := spec.Alternatives[2].Properties[0]; !p.Negated || p.Name != "token" {
		t.Errorf("property = %+v, want token, negated", p)
	}
}

// A base carries its own negation, and the sign is not part of the name: Java
// consumes it before the type lookup.
func TestNegatedBase(t *testing.T) {
	t.Parallel()

	spec := valid.Parse("!Ongoing")
	base := spec.Alternatives[0].Base
	if !base.Negated || base.Name != "Ongoing" {
		t.Errorf("base = %+v, want Ongoing, negated", base)
	}
}

// Only the first dot separates the base from the properties, which is Java's
// split("\\.", 2). A property may hold further dots.
func TestOnlyTheFirstDotSeparates(t *testing.T) {
	t.Parallel()

	spec := valid.Parse("Card.EnchantedBy$CardCounters.P1P1")
	alt := spec.Alternatives[0]
	if alt.Base.Name != "Card" {
		t.Errorf("base = %q, want Card", alt.Base.Name)
	}
	if len(alt.Properties) != 1 || alt.Properties[0].Name != "EnchantedBy$CardCounters.P1P1" {
		t.Errorf("properties = %+v, want the whole remainder as one property", alt.Properties)
	}
}

// Nothing is trimmed. CardTraitBase splits the param with a plain split(","),
// so a script writing "Player, Planeswalker" produces an alternative whose base
// is " Planeswalker" -- and no type, subtype or supertype is spelled with a
// leading space, so it matches nothing. Trimming here would hide that.
func TestNothingIsTrimmed(t *testing.T) {
	t.Parallel()

	spec := valid.Parse("Player, Planeswalker")
	if got := spec.Alternatives[1].Base.Name; got != " Planeswalker" {
		t.Errorf("base = %q, want %q -- the space is the script's, not ours", got, " Planeswalker")
	}
}

// The parsed form writes back exactly what it read, which is what makes a
// corpus-wide round trip a real check rather than a tautology.
func TestRoundTrip(t *testing.T) {
	t.Parallel()

	for _, value := range []string{
		"Creature",
		"Creature.YouCtrl",
		"Instant.YouCtrl+nonToken,Sorcery.YouCtrl",
		"!Ongoing",
		"Card.!token",
		"Player, Planeswalker",
		"Creature.powerGE1",
		"Card.EnchantedBy$CardCounters.P1P1",
		"Permanent.SharesColorWith Valid Creature.YouCtrl",
	} {
		if got := valid.Parse(value).String(); got != value {
			t.Errorf("Parse(%q).String() = %q", value, got)
		}
	}
}

// The operand offset is Java's, hardcoded per field, and it is not the field's
// length: totalPT is written with an underscore.
func TestNumericComparisons(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		property string
		field    string
		operator string
		operand  string
	}{
		{"powerGE1", "power", "GE", "1"},
		{"powerEQ0", "power", "EQ", "0"},
		{"basePowerEQ2", "basePower", "EQ", "2"},
		{"toughnessLE3", "toughness", "LE", "3"},
		{"baseToughnessEQ1", "baseToughness", "EQ", "1"},
		{"cmcGE7", "cmc", "GE", "7"},
		{"cmcEQX", "cmc", "EQ", "X"},
		{"cmcEQChosen", "cmc", "EQ", "Chosen"},
		{"numColorsEQ2", "numColors", "EQ", "2"},
		{"numTypesGE3", "numTypes", "GE", "3"},
		{"totalPT_GE5", "totalPT", "GE", "5"},
	} {
		got := valid.Parse("Creature." + tt.property).Alternatives[0].Properties[0]
		if got.Compare == nil {
			t.Errorf("%q was not read as a comparison", tt.property)
			continue
		}
		if got.Compare.Field != tt.field || got.Compare.Operator != tt.operator || got.Compare.Operand != tt.operand {
			t.Errorf("%q = %+v, want %s %s %s", tt.property, *got.Compare, tt.field, tt.operator, tt.operand)
		}
	}
}

// A property that measures nothing is not a comparison, however much it looks
// like one.
func TestNotComparisons(t *testing.T) {
	t.Parallel()

	for _, property := range []string{"YouCtrl", "attacking", "nonLand", "Self", "EnchantedBy"} {
		if got := valid.Parse("Card." + property).Alternatives[0].Properties[0]; got.Compare != nil {
			t.Errorf("%q was read as a comparison: %+v", property, *got.Compare)
		}
	}
}

func TestEmpty(t *testing.T) {
	t.Parallel()

	if got := len(valid.Parse("").Alternatives); got != 0 {
		t.Errorf("Parse(\"\") produced %d alternatives, want none", got)
	}
}
