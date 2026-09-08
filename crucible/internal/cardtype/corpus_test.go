package cardtype_test

import (
	"flag"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

var update = flag.Bool("update", false, "rewrite golden files")

const goldenPath = "testdata/type-lines.golden"

// TestCorpusTypeLines is the other half of the P0 gate: every distinct Types
// value in the card corpus parses, and prints to the form the golden pins --
// which is the form Forge's own CardType.toString produces, checked card by
// card by the P1 dump diff.
//
// Like the mana corpus test, it reads the card scripts in place rather than a
// copy under testdata: the corpus is committed here and is the same input the
// Java oracle reads, so a copy would be 33,682 files that can drift. This is
// the L2 corpus layer of TEST-13.
//
// Regenerate the golden with:
//
//	go test ./internal/cardtype -run TestCorpusTypeLines -update
func TestCorpusTypeLines(t *testing.T) {
	t.Parallel()

	reg := corpusRegistry(t)
	lines := distinctScriptValues(t, "Types:")
	if len(lines) < 3000 {
		t.Fatalf("found %d distinct type lines in the corpus, want at least 3000 -- the scan is broken", len(lines))
	}

	out := make([]string, 0, len(lines))
	for _, raw := range lines {
		parsed := cardtype.Parse(reg, raw)

		// Parsing twice must agree. Parsing the *printed* form must not, and is
		// not checked: String() writes the " - " separator that Java's parser
		// reads back as a subtype, which is why gandalf_shadows_foe holds a
		// literal "-" (PORT-7).
		if again := cardtype.Parse(reg, raw); !again.Equal(parsed) {
			t.Errorf("corpus type line %q parsed differently twice: %q then %q", raw, parsed, again)
			continue
		}

		out = append(out, raw+"\t"+parsed.String())
	}
	if t.Failed() {
		return
	}

	compareGolden(t, goldenPath, out)
	t.Logf("round-tripped %d distinct type lines from the corpus", len(lines))
}

// TestCorpusUnknownTypes pins the words the vocabulary file does not contain.
//
// Forge drops every one of them, so this is a list of subtypes no Forge build
// can see -- joke-set types, and types printed after the vocabulary was last
// updated. Pinning it means an upstream sync that adds a type, or a card that
// introduces one, shows up as a golden diff instead of as a card that quietly
// lost a subtype.
func TestCorpusUnknownTypes(t *testing.T) {
	t.Parallel()

	reg := corpusRegistry(t)
	seen := make(map[string]bool)
	for _, raw := range distinctScriptValues(t, "Types:") {
		for _, word := range cardtype.UnknownTypes(reg, raw) {
			seen[word] = true
		}
	}

	words := make([]string, 0, len(seen))
	for w := range seen {
		words = append(words, w)
	}
	sort.Strings(words)

	compareGolden(t, "testdata/unknown-types.golden", words)
}

// corpusRegistry loads the upstream type vocabulary, the same file Forge loads
// at startup.
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

// compareGolden diffs generated lines against a golden file, or rewrites it
// under -update. A regenerated golden is reviewed, never committed blind
// (TEST-6).
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
	if len(want) == 1 && want[0] == "" {
		want = nil
	}

	for i := 0; i < len(lines) && i < len(want); i++ {
		if lines[i] != want[i] {
			t.Errorf("%s line %d:\n got %s\nwant %s", path, i+1, lines[i], want[i])
		}
	}
	if len(lines) != len(want) {
		t.Errorf("%s has %d lines, generated %d", path, len(want), len(lines))
	}
}

// distinctScriptValues returns the sorted distinct values of one card-script
// key across the whole corpus, with the key prefix stripped.
func distinctScriptValues(t *testing.T, key string) []string {
	t.Helper()

	root := filepath.Join(repoRoot(t), "forge-gui", "res", "cardsfolder")
	seen := make(map[string]bool, 4096)
	files := 0
	err := filepath.WalkDir(root, func(path string, d os.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if d.IsDir() || filepath.Ext(path) != ".txt" {
			return nil
		}
		files++
		raw, err := os.ReadFile(path)
		if err != nil {
			return err
		}
		for _, line := range strings.Split(string(raw), "\n") {
			line = strings.TrimRight(line, "\r")
			if value, ok := strings.CutPrefix(line, key); ok {
				seen[value] = true
			}
		}
		return nil
	})
	if err != nil {
		t.Fatalf("walk %s: %v", root, err)
	}
	if files < 30000 {
		t.Fatalf("read %d card scripts from %s, want at least 30000", files, root)
	}

	out := make([]string, 0, len(seen))
	for v := range seen {
		out = append(out, v)
	}
	sort.Strings(out)
	return out
}

// repoRoot locates the repository relative to this source file, so the test
// depends on neither the working directory nor the environment (TEST-11).
func repoRoot(t *testing.T) string {
	t.Helper()

	_, file, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("runtime.Caller failed; cannot locate the card corpus")
	}
	// crucible/internal/cardtype/<file> -> repository root.
	return filepath.Join(filepath.Dir(file), "..", "..", "..")
}
