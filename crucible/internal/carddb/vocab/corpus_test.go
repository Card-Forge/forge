package vocab_test

import (
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

var update = flag.Bool("update", false, "rewrite golden files")

// TestCorpusVocabulary pins every token the corpus uses, by kind.
//
// This is the substrate of M3's P2 gate: the compiler needs a type for each of
// these, and the golden is what makes a token appearing or disappearing an
// explicit review rather than a silent widening of the job. Names only, no
// counts -- a count changes whenever a card is added, and that is not the
// signal.
//
// Regenerate with:
//
//	go test ./internal/carddb/vocab -run TestCorpusVocabulary -update
func TestCorpusVocabulary(t *testing.T) {
	t.Parallel()

	v, cards := scanCorpus(t)
	if cards < 30000 {
		t.Fatalf("scanned %d cards, want at least 30000 -- the walk is broken", cards)
	}

	var lines []string
	for _, kind := range vocab.Kinds() {
		for _, name := range v.Names(kind) {
			lines = append(lines, fmt.Sprintf("%s\t%s", kind, name))
		}
		t.Logf("%-16s %5d distinct %8d occurrences", kind, v.Distinct(kind), v.Occurrences(kind))
	}
	compareGolden(t, "testdata/vocabulary.golden", lines)
}

// A token is counted once per use, not once per card: what a missing token
// would cost is measured in abilities, not in scripts.
func TestOccurrencesCountUses(t *testing.T) {
	t.Parallel()

	v := vocab.New()
	v.AddCard(parseCard(t, "Name:X\nManaCost:R\nTypes:Instant\n"+
		"A:SP$ Draw | Defined$ You | NumCards$ 1\n"+
		"SVar:Other:DB$ Draw | Defined$ You\n"))

	if got, want := v.Count(vocab.API, "Draw"), 2; got != want {
		t.Errorf("Count(API, Draw) = %d, want %d", got, want)
	}
	if got, want := v.Count(vocab.ParamKey, "Defined"), 2; got != want {
		t.Errorf("Count(ParamKey, Defined) = %d, want %d", got, want)
	}
	if got, want := v.Distinct(vocab.API), 1; got != want {
		t.Errorf("Distinct(API) = %d, want %d", got, want)
	}
}

// An SVar body is a param map, an amount expression, or a literal, and the
// three land in different vocabularies. Counting `Enchanted$CardPower` as a
// param named `Enchanted` would invent a key no ability accepts.
func TestSVarBodyForms(t *testing.T) {
	t.Parallel()

	v := vocab.New()
	v.AddCard(parseCard(t, "Name:X\nManaCost:R\nTypes:Instant\n"+
		"SVar:Sub:DB$ Draw | Defined$ You\n"+
		"SVar:X:Count$CardsInYourHand\n"+
		"SVar:Y:Enchanted$CardToughness/Minus.1\n"+
		"SVar:Z:5\n"+
		"SVar:AIPreference:DiscardCost$Card.cmcLE1\n"))

	for _, tt := range []struct {
		kind vocab.Kind
		name string
		want int
	}{
		{vocab.API, "Draw", 1},
		{vocab.SVarHead, "Count", 1},
		{vocab.CountHead, "CardsInYourHand", 1},
		{vocab.SVarHead, "Enchanted", 1},
		{vocab.SVarProperty, "CardToughness", 1},
		{vocab.CountOperator, "Minus", 1},
		{vocab.AIHintKey, "DiscardCost", 1},
		{vocab.ParamKey, "Enchanted", 0},
		{vocab.ParamKey, "DiscardCost", 0},
		{vocab.ParamKey, "Count", 0},
	} {
		if got := v.Count(tt.kind, tt.name); got != tt.want {
			t.Errorf("Count(%s, %q) = %d, want %d", tt.kind, tt.name, got, tt.want)
		}
	}
}

func parseCard(t *testing.T, script string) *carddb.Card {
	t.Helper()

	card, err := carddb.ParseScript(testRegistry(t), "fixture", []byte(script))
	if err != nil {
		t.Fatalf("ParseScript failed: %v", err)
	}
	return card
}

func scanCorpus(t *testing.T) (*vocab.Vocabulary, int) {
	t.Helper()

	reg := testRegistry(t)
	v := vocab.New()
	cards := 0
	root := filepath.Join(repoRoot(t), "forge-gui", "res", "cardsfolder")
	err := filepath.WalkDir(root, func(path string, d os.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if d.IsDir() || filepath.Ext(path) != ".txt" {
			return nil
		}
		raw, err := os.ReadFile(path)
		if err != nil {
			return err
		}
		card, err := carddb.ParseScript(reg, strings.TrimSuffix(filepath.Base(path), ".txt"), raw)
		if err != nil {
			return err
		}
		v.AddCard(card)
		cards++
		return nil
	})
	if err != nil {
		t.Fatalf("walk %s: %v", root, err)
	}
	return v, cards
}

func testRegistry(t *testing.T) *cardtype.Registry {
	t.Helper()

	path := filepath.Join(repoRoot(t), "forge-gui", "res", "lists", "TypeLists.txt")
	f, err := os.Open(path)
	if err != nil {
		t.Fatalf("open %s: %v", path, err)
	}
	defer func() { _ = f.Close() }()
	reg, err := cardtype.LoadRegistry(f)
	if err != nil {
		t.Fatalf("load %s: %v", path, err)
	}
	return reg
}

func repoRoot(t *testing.T) string {
	t.Helper()

	_, file, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("runtime.Caller failed; cannot locate the corpus")
	}
	// crucible/internal/carddb/vocab/<file> -> repository root.
	return filepath.Join(filepath.Dir(file), "..", "..", "..", "..")
}

func compareGolden(t *testing.T, path string, lines []string) {
	t.Helper()

	got := strings.Join(lines, "\n") + "\n"
	if *update {
		if err := os.WriteFile(path, []byte(got), 0o644); err != nil {
			t.Fatalf("write %s: %v", path, err)
		}
		t.Logf("wrote %s (%d lines)", path, len(lines))
		return
	}

	raw, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("read %s: %v (regenerate with -update)", path, err)
	}
	want := strings.Split(strings.TrimRight(string(raw), "\n"), "\n")
	shown := 0
	for i := 0; i < len(lines) && i < len(want); i++ {
		if lines[i] != want[i] && shown < 20 {
			t.Errorf("%s line %d:\n got %s\nwant %s", path, i+1, lines[i], want[i])
			shown++
		}
	}
	if len(lines) != len(want) {
		t.Errorf("%s has %d lines, generated %d", path, len(want), len(lines))
	}
}
