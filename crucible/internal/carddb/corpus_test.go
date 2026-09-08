package carddb_test

import (
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

var update = flag.Bool("update", false, "rewrite golden files")

// TestCorpusParses is M2's first gate: every card script in the corpus parses,
// and every CopyFaceFrom: placeholder resolves.
//
// It reads the scripts in place, under forge-gui/res/cardsfolder, for the same
// reason internal/mana and internal/cardtype do: the corpus is committed here
// and is the input the Java oracle reads, so a copy would be 33,686 files that
// can drift. This is TEST-13's L2 layer.
//
// Regenerate the summary with:
//
//	go test ./internal/carddb -run TestCorpusParses -update
func TestCorpusParses(t *testing.T) {
	t.Parallel()

	reg := corpusRegistry(t)
	cards, failures := parseCorpus(t, reg)

	for i, f := range failures {
		if i == 20 {
			t.Errorf("... and %d more", len(failures)-20)
			break
		}
		t.Error(f)
	}
	if len(failures) > 0 {
		return
	}

	if err := carddb.ResolvePlaceholders(cards, carddb.IndexByFaceName(cards)); err != nil {
		t.Fatalf("resolve placeholders: %v", err)
	}

	compareGolden(t, "testdata/corpus-summary.golden", summarize(cards))
	t.Logf("parsed %d card scripts", len(cards))
}

// summarize reduces the corpus to counts that change only when the corpus or
// the parser does. A per-card golden would be 33,686 lines nobody reads; this
// is the shape of the whole corpus in twenty.
func summarize(cards []*carddb.Card) []string {
	var (
		splitTypes                                                  = map[string]int{}
		faceCounts                                                  = map[int]int{}
		withVariants, withPlaceholders, withTokens, withPartner     int
		abilities, keywords, triggers, statics, replacements, svars int
	)
	for _, c := range cards {
		splitTypes[c.SplitType.String()]++
		faceCounts[len(c.PresentFaces())]++
		if len(c.SupportedVariants) > 0 {
			withVariants++
		}
		if len(c.PlaceholderFaces) > 0 {
			withPlaceholders++
		}
		if len(c.Tokens) > 0 {
			withTokens++
		}
		if c.PartnerWith != "" || c.PartnerType != "" {
			withPartner++
		}
		for _, i := range c.PresentFaces() {
			f := &c.Faces[i]
			abilities += len(f.Abilities)
			keywords += len(f.Keywords)
			triggers += len(f.Triggers)
			statics += len(f.Statics)
			replacements += len(f.Replacements)
			svars += f.SVars.Len()
		}
	}

	out := []string{fmt.Sprintf("cards\t%d", len(cards))}
	for _, name := range sortedKeys(splitTypes) {
		out = append(out, fmt.Sprintf("split.%s\t%d", name, splitTypes[name]))
	}
	for n := 1; n <= carddb.NumFaces; n++ {
		if faceCounts[n] > 0 {
			out = append(out, fmt.Sprintf("faces.%d\t%d", n, faceCounts[n]))
		}
	}
	out = append(out,
		fmt.Sprintf("cards.withVariants\t%d", withVariants),
		fmt.Sprintf("cards.withPlaceholders\t%d", withPlaceholders),
		fmt.Sprintf("cards.withTokens\t%d", withTokens),
		fmt.Sprintf("cards.withPartner\t%d", withPartner),
		fmt.Sprintf("lines.A\t%d", abilities),
		fmt.Sprintf("lines.K\t%d", keywords),
		fmt.Sprintf("lines.T\t%d", triggers),
		fmt.Sprintf("lines.S\t%d", statics),
		fmt.Sprintf("lines.R\t%d", replacements),
		fmt.Sprintf("lines.SVar\t%d", svars),
	)
	return out
}

func sortedKeys(m map[string]int) []string {
	out := make([]string, 0, len(m))
	for k := range m {
		out = append(out, k)
	}
	sort.Strings(out)
	return out
}

// parseCorpus reads every script, returning the cards and one message per
// failure rather than stopping at the first: a parser gap usually shows up on
// hundreds of cards, and the shape of the list is the diagnosis.
func parseCorpus(t *testing.T, reg *cardtype.Registry) ([]*carddb.Card, []string) {
	t.Helper()

	root := filepath.Join(repoRoot(t), "forge-gui", "res", "cardsfolder")
	var (
		cards    []*carddb.Card
		failures []string
	)
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
		name := strings.TrimSuffix(filepath.Base(path), ".txt")
		card, err := carddb.ParseScript(reg, name, raw)
		if err != nil {
			failures = append(failures, err.Error())
			return nil
		}
		cards = append(cards, card)
		return nil
	})
	if err != nil {
		t.Fatalf("walk %s: %v", root, err)
	}
	if len(cards)+len(failures) < 30000 {
		t.Fatalf("read %d scripts from %s, want at least 30000", len(cards)+len(failures), root)
	}
	sort.Slice(cards, func(i, j int) bool { return cards[i].Filename < cards[j].Filename })
	sort.Strings(failures)
	return cards, failures
}

func corpusRegistry(t *testing.T) *cardtype.Registry {
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

func compareGolden(t *testing.T, path string, lines []string) {
	t.Helper()

	got := strings.Join(lines, "\n") + "\n"
	if *update {
		if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
			t.Fatalf("create %s: %v", filepath.Dir(path), err)
		}
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
	for i := 0; i < len(lines) && i < len(want); i++ {
		if lines[i] != want[i] {
			t.Errorf("%s line %d:\n got %s\nwant %s", path, i+1, lines[i], want[i])
		}
	}
	if len(lines) != len(want) {
		t.Errorf("%s has %d lines, generated %d", path, len(want), len(lines))
	}
}

func repoRoot(t *testing.T) string {
	t.Helper()

	_, file, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("runtime.Caller failed; cannot locate the card corpus")
	}
	// crucible/internal/carddb/<file> -> repository root.
	return filepath.Join(filepath.Dir(file), "..", "..", "..")
}
