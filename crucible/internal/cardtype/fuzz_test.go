package cardtype_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// FuzzTypeLine checks the parser properties that still apply here: never panic,
// and parsing is deterministic. Parse-then-print is deliberately not a fixed
// point -- String() writes the printed form with a " - " separator, and Parse
// reads that dash as a subtype because Java does (PORT-7). The no-partial-result
// property does not apply either: Parse has no failure mode by design.
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

		if again := cardtype.Parse(reg, in); !again.Equal(line) {
			t.Fatalf("Parse(%q) is not deterministic: %q then %q", in, line, again)
		}

		// Nothing is dropped: a word the vocabulary has never seen is still a
		// subtype, which is what Forge's card database holds.
		for _, word := range cardtype.UnknownTypes(reg, in) {
			if !line.HasSubtype(word) {
				t.Fatalf("Parse(%q) dropped %q; Java keeps it", in, word)
			}
		}
	})
}
