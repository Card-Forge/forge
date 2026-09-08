// Package deck reads Forge's `.dck` decklists.
//
// A decklist is what scopes the whole project: ADR-0011 defines "done" for card
// support by the decks actually being simulated, not by the corpus, so this is
// the package that says which cards have to work.
//
// Ported from forge-core/src/main/java/forge/deck/DeckSection.java,
// Deck.java (loadDeckSections) and CardPool.processCardList, plus the section
// format in forge/util/FileSection.java.
// Deviations recorded in docs/crucible/porting/port-log/deck-serializer.md.
package deck

import (
	"sort"
	"strings"
)

// Section is a part of a decklist. Values and names are Java's DeckSection, in
// its declaration order.
type Section uint8

// The ten deck sections.
const (
	Main Section = iota
	Sideboard
	Commander
	Avatar
	Planes
	Schemes
	Conspiracy
	Dungeon
	Attractions
	Contraptions

	numSections = int(Contraptions) + 1
)

var sectionNames = [numSections]string{
	Main:         "Main",
	Sideboard:    "Sideboard",
	Commander:    "Commander",
	Avatar:       "Avatar",
	Planes:       "Planes",
	Schemes:      "Schemes",
	Conspiracy:   "Conspiracy",
	Dungeon:      "Dungeon",
	Attractions:  "Attractions",
	Contraptions: "Contraptions",
}

// String returns the section's name as a decklist writes it.
func (s Section) String() string { return sectionNames[s] }

// SectionFromHeader matches a `[...]` header to a section, ignoring case and
// surrounding space, which is what Java's smartValueOf does. A header naming
// something else -- `[quest]`, `[shop]`, `[duel]` -- is not a section and is
// reported as such rather than guessed at.
func SectionFromHeader(header string) (Section, bool) {
	want := strings.TrimSpace(header)
	for i, name := range sectionNames {
		if strings.EqualFold(name, want) {
			return Section(i), true
		}
	}
	return 0, false
}

// Entry is one line of a decklist: how many of which card.
type Entry struct {
	Count int
	// Name is the card name alone, which is what the card database is keyed by.
	Name string
	// Edition is the set code after the first `|`, empty when the line does not
	// pin one. A deck that pins editions still plays the same cards.
	Edition string
	// Extra is anything after a second `|` -- an art index, usually. Kept so a
	// line can be written back unchanged, and otherwise unused.
	Extra string
}

// Deck is a parsed decklist.
type Deck struct {
	// Name is the `Name=` metadata value, which is the deck's display name and
	// need not match the file name.
	Name string

	// Metadata is every `[metadata]` key, in the file's own spelling. Look a
	// key up with [Deck.Meta] rather than directly: Forge's own map ignores
	// case.
	Metadata map[string]string

	// Sections holds only the sections the file actually contains.
	Sections map[Section][]Entry

	// UnknownSections names headers that are not deck sections, in file order.
	// Forge skips them silently; keeping them makes a `[quest]` block visible
	// to anyone asking why a card is missing.
	UnknownSections []string
}

// Meta returns a metadata value, matched without case, which is how Forge's
// TreeMap holds them.
func (d *Deck) Meta(key string) (string, bool) {
	for k, v := range d.Metadata {
		if strings.EqualFold(k, key) {
			return v, true
		}
	}
	return "", false
}

// Cards returns every entry in a section.
func (d *Deck) Cards(s Section) []Entry { return d.Sections[s] }

// Count returns how many cards a section holds, counting duplicates.
func (d *Deck) Count(s Section) int {
	total := 0
	for _, e := range d.Sections[s] {
		total += e.Count
	}
	return total
}

// Names returns the distinct card names across every section, sorted. This is
// what a coverage check asks for: a deck needs a card supported once, however
// many copies it plays.
func (d *Deck) Names() []string {
	seen := make(map[string]bool, 64)
	for _, entries := range d.Sections {
		for _, e := range entries {
			seen[e.Name] = true
		}
	}
	out := make([]string, 0, len(seen))
	for name := range seen {
		out = append(out, name)
	}
	sort.Strings(out)
	return out
}
