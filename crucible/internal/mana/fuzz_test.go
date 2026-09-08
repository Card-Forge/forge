package mana_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/mana"
)

// FuzzManaCost checks TEST-10's three parser properties: never panic, parse and
// serialize is a fixed point, and a failed parse returns nothing rather than a
// half-built value.
func FuzzManaCost(f *testing.F) {
	seeds := []string{
		"", "0", "no cost", "R", "2 W W", "X R", "1 X R", "1 BG G",
		"2/B 2/R 2/G", "2B", "2 GWP", "2 R PRG G", "1 C", "S", "16",
		"{2}{W}{W}", "{X}{1}{R}", "{no cost}", "Q", "{W", "W}", "{}", "///",
	}
	for _, s := range seeds {
		f.Add(s)
	}

	f.Fuzz(func(t *testing.T, in string) {
		cost, err := mana.Parse(in)
		if err != nil {
			if !cost.Equal(mana.Cost{}) {
				t.Fatalf("Parse(%q) failed with %v but still returned %q", in, err, cost)
			}
			return
		}

		printed := cost.String()
		again, err := mana.Parse(printed)
		if err != nil {
			t.Fatalf("Parse(%q) printed %q, which does not parse: %v", in, printed, err)
		}
		if !again.Equal(cost) {
			t.Fatalf("Parse(%q) = %q, reparsed as %q", in, cost, again)
		}
		if again.String() != printed {
			t.Fatalf("Parse(%q) printed %q, reparse printed %q", in, printed, again.String())
		}
	})
}
