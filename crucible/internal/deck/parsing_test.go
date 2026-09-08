package deck_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/deck"
)

func TestParseDeck(t *testing.T) {
	t.Parallel()

	d := deck.Parse("file-name", []byte(`[metadata]
Name=Burn
Description=Fast
[Main]
4 Lightning Bolt|2ED|1
20 Mountain
Sol Ring
[Sideboard]
2 Pyroblast|ICE
; a comment
# another
[quest]
whatever this is
`))

	if got, want := d.Name, "Burn"; got != want {
		t.Errorf("Name = %q, want %q -- metadata wins over the file name", got, want)
	}
	if got, want := d.Metadata["Description"], "Fast"; got != want {
		t.Errorf("Metadata[Description] = %q, want %q", got, want)
	}
	if got, want := d.Count(deck.Main), 25; got != want {
		t.Errorf("Count(Main) = %d, want %d", got, want)
	}
	if got, want := d.Count(deck.Sideboard), 2; got != want {
		t.Errorf("Count(Sideboard) = %d, want %d", got, want)
	}

	main := d.Cards(deck.Main)
	if len(main) != 3 {
		t.Fatalf("Main has %d entries, want 3: %+v", len(main), main)
	}
	if got := main[0]; got.Count != 4 || got.Name != "Lightning Bolt" || got.Edition != "2ED" || got.Extra != "1" {
		t.Errorf("Main[0] = %+v, want 4 Lightning Bolt from 2ED with extra 1", got)
	}
	if got := main[2]; got.Count != 1 || got.Name != "Sol Ring" {
		t.Errorf("Main[2] = %+v, want a single Sol Ring -- the count is optional", got)
	}

	// A section Forge does not know is skipped, not guessed at, and naming it
	// answers "why is that card missing" without opening the file.
	if diff := diffStrings(d.UnknownSections, []string{"quest"}); diff != "" {
		t.Errorf("UnknownSections: %s", diff)
	}
	if diff := diffStrings(d.Names(), []string{"Lightning Bolt", "Mountain", "Pyroblast", "Sol Ring"}); diff != "" {
		t.Errorf("Names(): %s -- distinct, sorted, across every section", diff)
	}
}

func TestSectionHeaders(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		header string
		want   deck.Section
		known  bool
	}{
		{"Main", deck.Main, true},
		{"main", deck.Main, true},
		{" Sideboard ", deck.Sideboard, true},
		{"COMMANDER", deck.Commander, true},
		{"Contraptions", deck.Contraptions, true},
		{"quest", 0, false},
		{"shop", 0, false},
	} {
		got, ok := deck.SectionFromHeader(tt.header)
		if ok != tt.known || (ok && got != tt.want) {
			t.Errorf("SectionFromHeader(%q) = %v, %v; want %v, %v", tt.header, got, ok, tt.want, tt.known)
		}
	}
	if got, want := deck.Commander.String(), "Commander"; got != want {
		t.Errorf("Commander.String() = %q, want %q", got, want)
	}
}

func TestParseSkipsWhatForgeSkips(t *testing.T) {
	t.Parallel()

	// Java matches a card line with ((\d+)\s+)?(.*?) and keeps whatever group 3
	// captures, so a bare "4" is a card *named* "4" rather than a count with no
	// card. Only a blank line disappears. Reproduced rather than tidied: a
	// decklist Forge opens must not be one Crucible refuses.
	d := deck.Parse("odd", []byte("[Main]\n\n4\n   \n2 Real Card\n"))
	if diff := diffStrings(d.Names(), []string{"4", "Real Card"}); diff != "" {
		t.Errorf("Names(): %s", diff)
	}
	if got, want := d.Count(deck.Main), 3; got != want {
		t.Errorf("Count(Main) = %d, want %d -- one card named \"4\", two Real Card", got, want)
	}
}

// A metadata line with no '=' is a key with an empty value, not an error:
// FileSection.parse does the same, and 1,945 decklists have a Description
// spanning several lines.
func TestMetadataLineWithoutSeparator(t *testing.T) {
	t.Parallel()

	d := deck.Parse("wrapped", []byte("[metadata]\nName=Lore\nDescription=First line\nand a second one\n"))
	if got, want := d.Name, "Lore"; got != want {
		t.Errorf("Name = %q, want %q", got, want)
	}
	if got, ok := d.Metadata["and a second one"]; !ok || got != "" {
		t.Errorf("continuation line = %q, %v; want an empty value", got, ok)
	}
	if got, ok := d.Meta("DESCRIPTION"); !ok || got != "First line" {
		t.Errorf("Meta(DESCRIPTION) = %q, %v; want the value, matched without case", got, ok)
	}
}

func TestEmptyDeck(t *testing.T) {
	t.Parallel()

	d := deck.Parse("empty", []byte(""))
	if got := len(d.Names()); got != 0 {
		t.Errorf("Names() = %d entries, want none", got)
	}
	if got, want := d.Name, "empty"; got != want {
		t.Errorf("Name = %q, want the file name %q when metadata gives none", got, want)
	}
	if got := d.Count(deck.Main); got != 0 {
		t.Errorf("Count(Main) = %d, want 0", got)
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
	return "got " + join(got) + ", want " + join(want)
}

func join(xs []string) string {
	if len(xs) == 0 {
		return "[]"
	}
	out := "["
	for i, x := range xs {
		if i > 0 {
			out += " "
		}
		out += x
	}
	return out + "]"
}
