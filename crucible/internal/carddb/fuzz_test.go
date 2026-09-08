package carddb_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// FuzzCardScript checks TEST-10's properties for this parser: it never panics,
// and a failed parse returns no card rather than a half-built one.
//
// There is no serialize half to round-trip against yet; the canonical dump
// arrives with the P1 gate in slice C, and its fixed point is checked there.
func FuzzCardScript(f *testing.F) {
	seeds := []string{
		"", "#comment\n", "Name:X\n",
		"Name:X\nManaCost:R\nTypes:Instant\n",
		"Name:X\nManaCost:R\nTypes:Instant\nALTERNATE\nName:Y\n",
		"Name:X\nManaCost:R\nTypes:Instant\nSPECIALIZE:WHITE\nName:W\n",
		"Name:X\nManaCost:R\nTypes:Instant\nVariant:V:K:Flying\n",
		"Name:X\nManaCost:R\nTypes:Instant\nSVar:A:1\nSVar:a:2\n",
		"Name:X\nManaCost:no cost\nTypes:Land\nCopyFaceFrom:Other\n",
		"ALTERNATE\n", "Variant:\n", "SVar:\n", "PT:/\n", ":::\n",
	}
	for _, s := range seeds {
		f.Add(s)
	}

	reg, err := cardtype.LoadRegistry(strings.NewReader(testTypeList))
	if err != nil {
		f.Fatalf("LoadRegistry failed: %v", err)
	}

	f.Fuzz(func(t *testing.T, script string) {
		card, err := carddb.ParseScript(reg, "fuzz", []byte(script))
		if err != nil {
			if card != nil {
				t.Fatalf("ParseScript(%q) failed with %v but still returned a card", script, err)
			}
			return
		}
		if card.Filename != "fuzz" {
			t.Fatalf("ParseScript(%q) lost the filename: %q", script, card.Filename)
		}
		// A face that holds anything must exist; an absent face with content is
		// the shape of a state-machine bug.
		for i := range card.Faces {
			face := &card.Faces[i]
			if !face.Present && (face.Name != "" || len(face.Abilities) > 0 || face.SVars.Len() > 0) {
				t.Fatalf("ParseScript(%q): face %d is absent but holds content", script, i)
			}
		}
	})
}
