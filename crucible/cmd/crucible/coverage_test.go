package main

import (
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/deck"
)

func parseTestDeck(t *testing.T, content string) *deck.Deck {
	t.Helper()
	return deck.Parse("fixture", []byte(content))
}

// Internal test: corpus-coverage is a command, so its helpers are unexported
// and there is no public API to test through (TEST-2).

func repoRoot(t *testing.T) string {
	t.Helper()

	_, file, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("runtime.Caller failed")
	}
	// crucible/cmd/crucible/<file> -> repository root.
	return filepath.Join(filepath.Dir(file), "..", "..", "..")
}

// The command's job is a lookup, and the lookups that matter are the ones a
// decklist gets wrong: case, accents, and which half of a split card it names.
func TestCardLookupMatchesForgesIndexing(t *testing.T) {
	t.Parallel()

	root := repoRoot(t)
	db, err := loadCards(
		filepath.Join(root, "forge-gui", "res", "cardsfolder"),
		filepath.Join(root, "forge-gui", "res", "lists", "TypeLists.txt"),
	)
	if err != nil {
		t.Fatalf("loadCards: %v", err)
	}

	for _, tt := range []struct {
		name string
		want string
		why  string
	}{
		{"Lightning Bolt", "Lightning Bolt", "an ordinary name"},
		{"lightning bolt", "Lightning Bolt", "CardDb's maps use CASE_INSENSITIVE_ORDER"},
		{"LIGHTNING BOLT", "Lightning Bolt", "same"},
		{"Lim-Dul's Vault", "Lim-Dûl's Vault", "CardDb also indexes the accent-free form"},
		{"Lim-Dûl's Vault", "Lim-Dûl's Vault", "and the real one"},
	} {
		card := lookup(db, tt.name)
		if card == nil {
			t.Errorf("lookup(%q) found nothing (%s)", tt.name, tt.why)
			continue
		}
		if got := card.Name(); got != tt.want {
			t.Errorf("lookup(%q) = %q, want %q (%s)", tt.name, got, tt.want, tt.why)
		}
	}

	// A split card is named by both halves joined, and by each half alone.
	joined := lookup(db, "Start // Fire")
	if joined == nil {
		t.Fatal(`lookup("Start // Fire") found nothing`)
	}
	for _, half := range []string{"Start", "Fire"} {
		if got := lookup(db, half); got == nil {
			t.Errorf("lookup(%q) found nothing; a decklist may name either half", half)
		}
	}
	if got := joined.Name(); got != "Start // Fire" {
		t.Errorf("split card Name() = %q, want %q", got, "Start // Fire")
	}
}

func TestMissingCardsReportsWhatIsAbsent(t *testing.T) {
	t.Parallel()

	root := repoRoot(t)
	db, err := loadCards(
		filepath.Join(root, "forge-gui", "res", "cardsfolder"),
		filepath.Join(root, "forge-gui", "res", "lists", "TypeLists.txt"),
	)
	if err != nil {
		t.Fatalf("loadCards: %v", err)
	}

	d := parseTestDeck(t, "[Main]\n4 Lightning Bolt\n1 Not A Real Card At All\n")
	missing := missingCards(db, d)
	if len(missing) != 1 || missing[0] != "Not A Real Card At All" {
		t.Errorf("missingCards() = %v, want exactly the invented name", missing)
	}
}

func TestDecklistPathsAcceptsAFileOrADirectory(t *testing.T) {
	t.Parallel()

	root := repoRoot(t)
	dir := filepath.Join(root, "forge-gui", "res", "cube")
	paths, err := decklistPaths(dir)
	if err != nil {
		t.Fatalf("decklistPaths(dir): %v", err)
	}
	if len(paths) < 10 {
		t.Fatalf("decklistPaths(%s) = %d files, want at least 10", dir, len(paths))
	}
	if !strings.HasSuffix(strings.ToLower(paths[0]), ".dck") {
		t.Errorf("decklistPaths returned %q, which is not a decklist", paths[0])
	}

	single, err := decklistPaths(paths[0])
	if err != nil {
		t.Fatalf("decklistPaths(file): %v", err)
	}
	if len(single) != 1 || single[0] != paths[0] {
		t.Errorf("decklistPaths(file) = %v, want just that file", single)
	}

	if _, err := decklistPaths(filepath.Join(root, "no-such-directory")); err == nil {
		t.Error("decklistPaths accepted a path that does not exist")
	}
}

// The command itself, end to end, because the reporting is most of what it
// does and a coverage report that fails silently is worse than none.
func TestCorpusCoverageCommand(t *testing.T) {
	t.Parallel()

	root := repoRoot(t)
	corpus := filepath.Join(root, "forge-gui", "res", "cardsfolder")
	types := filepath.Join(root, "forge-gui", "res", "lists", "TypeLists.txt")

	dir := t.TempDir()
	writeDeck(t, dir, "covered.dck", "[metadata]\nName=Covered\n[Main]\n4 Lightning Bolt\n20 Mountain\n")
	if err := corpusCoverage([]string{"-corpus", corpus, "-types", types, "-decks", dir, "-quiet"}); err != nil {
		t.Errorf("a deck of real cards reported %v, want no error", err)
	}

	writeDeck(t, dir, "uncovered.dck", "[metadata]\nName=Uncovered\n[Main]\n1 Not A Real Card At All\n")
	err := corpusCoverage([]string{"-corpus", corpus, "-types", types, "-decks", dir, "-quiet"})
	if err == nil {
		t.Fatal("a deck naming a card that does not exist reported no error")
	}
	if !strings.Contains(err.Error(), "1 unsupported") {
		t.Errorf("error = %v, want it to count the unsupported names", err)
	}

	if err := corpusCoverage([]string{"-corpus", corpus, "-types", types}); err == nil {
		t.Error("corpus-coverage without -decks reported no error")
	}
	if err := corpusCoverage([]string{"-corpus", corpus, "-types", types, "-decks", t.TempDir()}); err == nil {
		t.Error("corpus-coverage over a directory with no decklists reported no error")
	}
}

func writeDeck(t *testing.T, dir, name, content string) {
	t.Helper()
	if err := os.WriteFile(filepath.Join(dir, name), []byte(content), 0o644); err != nil {
		t.Fatalf("write %s: %v", name, err)
	}
}
