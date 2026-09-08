package deck_test

import (
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/deck"
)

var update = flag.Bool("update", false, "rewrite golden files")

// TestCorpusDecklists parses every `.dck` file the repository ships. Upstream
// writes these with its own serializer, so they are the only sample of the
// format that is guaranteed to be what Forge produces -- 14,044 of them,
// covering quest decks, cubes, adventure decks and precons.
//
// Regenerate the summary with:
//
//	go test ./internal/deck -run TestCorpusDecklists -update
func TestCorpusDecklists(t *testing.T) {
	t.Parallel()

	paths := decklistPaths(t)
	if len(paths) < 10000 {
		t.Fatalf("found %d decklists, want at least 10000 -- the scan is broken", len(paths))
	}

	var (
		failures  []string // decklists whose Main section came out empty and unexplained
		cards     int
		sections  = map[string]int{}
		unknown   = map[string]int{}
		withMeta  int
		emptyMain int
	)
	for _, path := range paths {
		raw, err := os.ReadFile(path)
		if err != nil {
			t.Fatalf("read %s: %v", path, err)
		}
		d := deck.Parse(strings.TrimSuffix(filepath.Base(path), ".dck"), raw)

		cards += len(d.Names())
		if len(d.Metadata) > 0 {
			withMeta++
		}
		if d.Count(deck.Main) == 0 {
			emptyMain++
			// An empty Main is normal for a deck that is all Commander,
			// Schemes or Planes, and suspicious otherwise.
			if len(d.Names()) == 0 && len(d.UnknownSections) == 0 {
				failures = append(failures, fmt.Sprintf("%s: no cards and no unknown sections", path))
			}
		}
		for s := deck.Main; int(s) <= int(deck.Contraptions); s++ {
			if len(d.Cards(s)) > 0 {
				sections[s.String()]++
			}
		}
		for _, name := range d.UnknownSections {
			unknown[strings.ToLower(name)]++
		}
	}

	for i, f := range failures {
		if i == 10 {
			t.Errorf("... and %d more", len(failures)-10)
			break
		}
		t.Error(f)
	}
	if len(failures) > 0 {
		return
	}

	summary := []string{
		fmt.Sprintf("decklists\t%d", len(paths)),
		fmt.Sprintf("withMetadata\t%d", withMeta),
		fmt.Sprintf("emptyMain\t%d", emptyMain),
		fmt.Sprintf("distinctCardEntries\t%d", cards),
	}
	for _, name := range sortedKeys(sections) {
		summary = append(summary, fmt.Sprintf("section.%s\t%d", name, sections[name]))
	}
	for _, name := range sortedKeys(unknown) {
		summary = append(summary, fmt.Sprintf("nonSection.%s\t%d", name, unknown[name]))
	}
	compareGolden(t, "testdata/decklists-summary.golden", summary)
	t.Logf("parsed %d decklists", len(paths))
}

func sortedKeys(m map[string]int) []string {
	out := make([]string, 0, len(m))
	for k := range m {
		out = append(out, k)
	}
	sort.Strings(out)
	return out
}

func decklistPaths(t *testing.T) []string {
	t.Helper()

	_, file, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("runtime.Caller failed; cannot locate the decklists")
	}
	// crucible/internal/deck/<file> -> repository root.
	root := filepath.Join(filepath.Dir(file), "..", "..", "..", "forge-gui", "res")

	var out []string
	err := filepath.WalkDir(root, func(path string, d os.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if !d.IsDir() && strings.EqualFold(filepath.Ext(path), ".dck") {
			out = append(out, path)
		}
		return nil
	})
	if err != nil {
		t.Fatalf("walk %s: %v", root, err)
	}
	sort.Strings(out)
	return out
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
