package valid_test

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
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

var update = flag.Bool("update", false, "rewrite golden files")

// TestCorpusValidStrings parses every valid string the corpus writes.
//
// Two things are checked. The parse writes back byte for byte, so the
// structure is a view of the script rather than a rewrite of it. And no base
// is padded with space: nothing in Java's type lookup tolerates one, so a
// padded base is a targeting restriction that silently matches nothing --
// which is how two cards shipped unable to target planeswalkers
// (docs/crucible/porting/card-script-defects.md).
func TestCorpusValidStrings(t *testing.T) {
	t.Parallel()

	var (
		parsed   int
		fields   = map[string]int{}
		ops      = map[string]int{}
		operands = map[string]int{}
		padded   []string
		roundTip []string
	)
	forEachValidString(t, func(card, key, value string) {
		parsed++
		spec := valid.Parse(value)
		if got := spec.String(); got != value {
			roundTip = append(roundTip, fmt.Sprintf("%s: %s$ %q became %q", card, key, value, got))
		}
		for _, alt := range spec.Alternatives {
			if name := alt.Base.Name; name != strings.TrimSpace(name) {
				padded = append(padded, fmt.Sprintf("%s: %s$ %q has the base %q", card, key, value, name))
			}
			for _, property := range alt.Properties {
				if property.Compare == nil {
					continue
				}
				fields[property.Compare.Field]++
				ops[property.Compare.Operator]++
				operands[property.Compare.Operand]++
			}
		}
	})

	report(t, "round trip", roundTip)
	report(t, "base padded with space", padded)

	if parsed < 40000 {
		t.Fatalf("read %d valid strings, want at least 40000 -- the walk is broken", parsed)
	}
	t.Logf("parsed %d valid strings", parsed)

	var lines []string
	for _, name := range sortedKeys(fields) {
		lines = append(lines, fmt.Sprintf("field\t%s\t%d", name, fields[name]))
	}
	for _, name := range sortedKeys(ops) {
		lines = append(lines, fmt.Sprintf("operator\t%s\t%d", name, ops[name]))
	}
	for _, name := range sortedKeys(operands) {
		lines = append(lines, fmt.Sprintf("operand\t%s", name))
	}
	compareGolden(t, "testdata/comparisons.golden", lines)
}

func report(t *testing.T, what string, failures []string) {
	t.Helper()

	for i, f := range failures {
		if i == 10 {
			t.Errorf("%s: ... and %d more", what, len(failures)-10)
			break
		}
		t.Errorf("%s: %s", what, f)
	}
}

// forEachValidString hands fn every value the corpus writes under a
// valid-typed param key, on every face and every functional variant.
func forEachValidString(t *testing.T, fn func(card, key, value string)) {
	t.Helper()

	reg := testRegistry(t)
	root := filepath.Join(repoRoot(t), "forge-gui", "res", "cardsfolder")
	cards := 0
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
			return err
		}
		cards++
		for _, i := range card.PresentFaces() {
			eachFace(&card.Faces[i], name, fn)
		}
		return nil
	})
	if err != nil {
		t.Fatalf("walk %s: %v", root, err)
	}
	if cards < 30000 {
		t.Fatalf("read %d cards, want at least 30000", cards)
	}
}

func eachFace(face *carddb.Face, card string, fn func(card, key, value string)) {
	lines := make([]string, 0, 16)
	lines = append(lines, face.Abilities...)
	lines = append(lines, face.Triggers...)
	lines = append(lines, face.Statics...)
	lines = append(lines, face.Replacements...)
	for _, name := range face.SVars.Names() {
		value, _ := face.SVars.Get(name)
		lines = append(lines, value)
	}
	for _, line := range lines {
		for _, p := range vocab.SplitParams(line) {
			if vocab.IsValidKey(p.Key) {
				fn(card, p.Key, p.Value)
			}
		}
	}
	for _, variant := range face.Variants {
		eachFace(variant, card, fn)
	}
}

func sortedKeys(m map[string]int) []string {
	out := make([]string, 0, len(m))
	for k := range m {
		out = append(out, k)
	}
	sort.Strings(out)
	return out
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
	// crucible/internal/valid/<file> -> repository root.
	return filepath.Join(filepath.Dir(file), "..", "..", "..")
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
