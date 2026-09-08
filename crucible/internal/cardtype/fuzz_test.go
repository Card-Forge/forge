package cardtype_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// FuzzTypeLine checks TEST-10's parser properties that apply here: never panic,
// and parse-then-print is a fixed point. The third property, no partial result
// on failure, does not apply -- Parse has no failure mode by design.
func FuzzTypeLine(f *testing.F) {
	seeds := []string{
		"", " ", "-", " - ", "Creature", "Legendary Creature Elf Warrior",
		"Legendary Creature - Elf Warrior", "Basic Land Island",
		"Kindred Enchantment Elf Aura", "Artifact Creature Killbot",
		"Legendary Creature Time Lord Warrior", "Time Lord", "Time",
		"Enchantment Land Urza's Saga", "Plane Bolas's Meditation Realm",
		"Creature Elf Equipment", "Creature -", "CreatureElf", "creature elf",
	}
	for _, s := range seeds {
		f.Add(s)
	}

	reg, err := cardtype.LoadRegistry(strings.NewReader(testTypeList))
	if err != nil {
		f.Fatalf("LoadRegistry failed: %v", err)
	}

	f.Fuzz(func(t *testing.T, in string) {
		line := cardtype.Parse(reg, in)

		printed := line.String()
		again := cardtype.Parse(reg, printed)
		if !again.Equal(line) {
			t.Fatalf("Parse(%q) = %q, reparsed as %q", in, printed, again)
		}
		if again.String() != printed {
			t.Fatalf("Parse(%q) printed %q, reparse printed %q", in, printed, again.String())
		}

		// Every word Parse threw away is either an unknown type or a subtype
		// the card's types do not allow. Nothing else may vanish.
		for _, word := range cardtype.UnknownTypes(reg, in) {
			if line.HasSubtype(word) {
				t.Fatalf("Parse(%q) kept %q, which the vocabulary does not contain", in, word)
			}
		}
	})
}
