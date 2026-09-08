package mana_test

import (
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/mana"
)

var update = flag.Bool("update", false, "rewrite golden files")

const goldenPath = "testdata/mana-costs.golden"

// TestCorpusManaCosts is one half of the P0 gate: every distinct ManaCost value
// in the card corpus parses, and round-trips through the printed form.
//
// It reads the card scripts in place, under forge-gui/res/cardsfolder, rather
// than a copy in testdata. The corpus is committed in this repository and is
// the same input the Java oracle reads, so a copy would be 33,682 files of
// duplicated data that could disagree with the original after an upstream sync.
// This is the L2 corpus layer of TEST-13, which is defined over the whole
// corpus by construction.
//
// Regenerate the golden with:
//
//	go test ./internal/mana -run TestCorpusManaCosts -update
func TestCorpusManaCosts(t *testing.T) {
	t.Parallel()

	costs := distinctScriptValues(t, "ManaCost:")
	if len(costs) < 500 {
		t.Fatalf("found %d distinct mana costs in the corpus, want at least 500 -- the scan is broken", len(costs))
	}

	lines := make([]string, 0, len(costs))
	for _, raw := range costs {
		cost, err := mana.Parse(raw)
		if err != nil {
			t.Errorf("corpus cost %q: %v", raw, err)
			continue
		}

		// Round trip, which is what the gate asks for: the printed form has to
		// parse back to the same cost, or the Java diff at P1 has nothing
		// stable to compare.
		again, err := mana.Parse(cost.String())
		if err != nil {
			t.Errorf("corpus cost %q printed as %q, which does not parse: %v", raw, cost, err)
			continue
		}
		if !again.Equal(cost) {
			t.Errorf("corpus cost %q: round trip through %q gave %q", raw, cost, again)
			continue
		}

		lines = append(lines, fmt.Sprintf("%s\t%s\t%d\t%s", raw, cost, cost.CMC(), cost.Colors()))
	}
	if t.Failed() {
		return
	}

	compareGolden(t, goldenPath, lines)
	t.Logf("round-tripped %d distinct mana costs from the corpus", len(costs))
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

	root := corpusDir(t)
	seen := make(map[string]bool, 1024)
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

// corpusDir locates the upstream card scripts relative to this source file, so
// the test does not depend on the working directory or on any environment
// variable (TEST-11).
func corpusDir(t *testing.T) string {
	t.Helper()

	_, file, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("runtime.Caller failed; cannot locate the card corpus")
	}
	// crucible/internal/mana/<file> -> repository root.
	root := filepath.Join(filepath.Dir(file), "..", "..", "..")
	dir := filepath.Join(root, "forge-gui", "res", "cardsfolder")
	if _, err := os.Stat(dir); err != nil {
		t.Fatalf("card corpus not found at %s: %v", dir, err)
	}
	return dir
}
