package main

import (
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/smoke"
)

// Internal tests (TEST-2): card-smoke is a command, so its helpers are
// unexported and there is no public API to test through.

var (
	smokeDBOnce sync.Once
	smokeDBVal  *compile.DB
	smokeDBErr  error
)

func smokeDB(t *testing.T) *compile.DB {
	t.Helper()
	smokeDBOnce.Do(func() {
		root := repoRoot(t)
		types := filepath.Join(root, "forge-gui", "res", "lists", "TypeLists.txt")
		smokeDBVal, smokeDBErr = compile.LoadDB(filepath.Join(root, "forge-gui", "res", "cardsfolder"), types)
	})
	if smokeDBErr != nil {
		t.Fatalf("load corpus: %v", smokeDBErr)
	}
	return smokeDBVal
}

// The card set comes from a decklist the way a deck writes the names: any case,
// duplicates collapsed, names the corpus lacks counted and left out.
func TestSmokeCardsFromDecklists(t *testing.T) {
	t.Parallel()

	db := smokeDB(t)
	dir := t.TempDir()
	deckText := "[Main]\n4 lightning bolt\n2 Lightning Bolt\n20 Mountain\n1 Not A Real Card\n"
	if err := os.WriteFile(filepath.Join(dir, "burn.dck"), []byte(deckText), 0o644); err != nil {
		t.Fatal(err)
	}

	names, source, err := smokeCards(db, dir, "", false)
	if err != nil {
		t.Fatalf("smokeCards: %v", err)
	}
	if strings.Join(names, ",") != "Lightning Bolt,Mountain" {
		t.Errorf("names = %v, want Lightning Bolt and Mountain once each", names)
	}
	if !strings.Contains(source, "1 decklists") || !strings.Contains(source, "1 card names unknown") {
		t.Errorf("source = %q, want the deck count and the unknown-name count", source)
	}
}

func TestSmokeCardsFromNamesAndFlags(t *testing.T) {
	t.Parallel()

	db := smokeDB(t)
	names, _, err := smokeCards(db, "", "lightning bolt, Forest", false)
	if err != nil || len(names) != 2 || names[0] != "Lightning Bolt" {
		t.Errorf("smokeCards(-cards) = %v, %v", names, err)
	}
	if _, _, err := smokeCards(db, "", "Not A Real Card", false); err == nil {
		t.Error("an unknown -cards name should be an error")
	}
	if all, _, err := smokeCards(db, "", "", true); err != nil || len(all) != len(db.Names()) {
		t.Errorf("-all = %d names, %v, want the whole corpus (%d)", len(all), err, len(db.Names()))
	}
	if _, _, err := smokeCards(db, "", "", false); err == nil {
		t.Error("no card source should be an error")
	}
	if _, _, err := smokeCards(db, filepath.Join(t.TempDir(), "missing"), "", false); err == nil {
		t.Error("a missing decklist path should be an error")
	}
}

// The report counts the outcomes, groups refusals by cause with an example,
// and prints every panic and hang in full.
func TestReportSummarisesTheProbe(t *testing.T) {
	t.Parallel()

	results := []smoke.Result{
		{Card: "A", Outcome: smoke.Plays},
		{Card: "B", Outcome: smoke.Refused, Stage: "resolve", Detail: `engine: Pump: KW$ "Flying" not resolvable yet`},
		{Card: "C", Outcome: smoke.Refused, Stage: "resolve", Detail: `engine: Pump: KW$ "Haste" not resolvable yet`},
		{Card: "D", Outcome: smoke.Refused, Stage: "activate 0 (Draw)", Detail: "engine: Draw: NumCards$ 4 is not resolvable"},
		{Card: "E", Outcome: smoke.Panicked, Stage: "cast", Detail: "boom"},
		{Card: "F", Outcome: smoke.Hung, Stage: "turns", Detail: "no result after 1s"},
		{Card: "G", Outcome: smoke.Inconclusive, Stage: "cast"},
	}
	out := report("a test set", results, 1, true, false)
	for _, want := range []string{
		"a test set: 7 cards", "plays             1", "refused           3", "hung              1",
		"16.7% of the 6 judged", "    2  resolve: engine: Pump", "e.g. B, C",
		"and 1 more causes", "PANICKED E at cast", "HUNG F at turns", "inconclusive G",
	} {
		if !strings.Contains(out, want) {
			t.Errorf("report lacks %q:\n%s", want, out)
		}
	}
}

func TestProbeAllKeepsNameOrder(t *testing.T) {
	t.Parallel()

	names := []string{"Forest", "Lightning Bolt", "Grizzly Bears"}
	got := probeAll(smoke.NewProber(smokeDB(t)), names, 0, time.Minute)
	for i, r := range got {
		if r.Card != names[i] || r.Outcome != smoke.Plays {
			t.Errorf("result %d = %+v, want %s to play", i, r, names[i])
		}
	}
}

func TestCardSmokeCommandEndToEnd(t *testing.T) {
	t.Parallel()

	root := repoRoot(t)
	args := []string{
		"-corpus", filepath.Join(root, "forge-gui", "res", "cardsfolder"),
		"-tokens", filepath.Join(root, "forge-gui", "res", "tokenscripts"),
		"-types", filepath.Join(root, "forge-gui", "res", "lists", "TypeLists.txt"),
		"-cards", "Lightning Bolt", "-workers", "1",
	}
	if err := cardSmoke(args); err != nil {
		t.Fatalf("cardSmoke: %v", err)
	}
	if err := cardSmoke(append(args[:6:6], "-cards", "Not A Real Card")); err == nil {
		t.Error("an unknown card should fail the command")
	}
}
