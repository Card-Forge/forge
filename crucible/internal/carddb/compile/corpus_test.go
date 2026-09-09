package compile_test

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// TestCorpusCompiles is M3's structural gate: every ability line in the corpus
// names a record type, and every sub-ability reference resolves to an SVar the
// same face defines.
//
// A failure here is a card script that says less than its text does, not a
// parser gap -- Java drops an unresolved reference with a message on stdout,
// so nothing downstream ever notices (PORT-8). The corpus currently has none:
// the five it had are fixed, and the fixes are carried until upstream merges
// them (docs/crucible/porting/card-script-defects.md).
func TestCorpusCompiles(t *testing.T) {
	t.Parallel()

	cards := parseCorpus(t)
	var (
		failures []string
		byKind   = map[string]int{}
		compiled int
		lines    int
		subs     int
	)
	for _, card := range cards {
		out, err := compile.Compile(card)
		if err != nil {
			failures = append(failures, err.Error())
			byKind[kindOf(err)]++
			continue
		}
		compiled++
		for i := range out.Faces {
			for _, group := range [][]*compile.Ability{
				out.Faces[i].Abilities, out.Faces[i].Triggers,
				out.Faces[i].Statics, out.Faces[i].Replacements,
			} {
				lines += len(group)
				for _, a := range group {
					subs += countSubs(a)
				}
			}
		}
	}

	t.Logf("compiled %d of %d cards: %d top-level lines, %d resolved sub-abilities",
		compiled, len(cards), lines, subs)
	for _, kind := range sortedKeys(byKind) {
		t.Logf("%-24s %d", kind, byKind[kind])
	}
	sort.Strings(failures)
	for i, f := range failures {
		if i == 20 {
			t.Errorf("... and %d more", len(failures)-20)
			break
		}
		t.Error(f)
	}
}

func countSubs(a *compile.Ability) int {
	n := len(a.Subs)
	for _, s := range a.Subs {
		n += countSubs(s.Ability)
	}
	return n
}

func kindOf(err error) string {
	switch {
	case errors.Is(err, compile.ErrMissingSVar):
		return "missing SVar"
	case errors.Is(err, compile.ErrCycle):
		return "cyclic chain"
	case errors.Is(err, compile.ErrNoRecord):
		return "no record key"
	}
	return "other"
}

func sortedKeys(m map[string]int) []string {
	out := make([]string, 0, len(m))
	for k := range m {
		out = append(out, k)
	}
	sort.Strings(out)
	return out
}

func parseCorpus(t *testing.T) []*carddb.Card {
	t.Helper()

	reg := testRegistry(t)
	var cards []*carddb.Card
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
		cards = append(cards, card)
		return nil
	})
	if err != nil {
		t.Fatalf("walk %s: %v", root, err)
	}
	if len(cards) < 30000 {
		t.Fatalf("parsed %d cards, want at least 30000", len(cards))
	}
	sort.Slice(cards, func(i, j int) bool { return cards[i].Filename < cards[j].Filename })
	return cards
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
	// crucible/internal/carddb/compile/<file> -> repository root.
	return filepath.Join(filepath.Dir(file), "..", "..", "..", "..")
}

var _ = fmt.Sprintf
