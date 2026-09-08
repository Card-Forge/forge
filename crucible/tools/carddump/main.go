// Command carddump writes every card in the corpus as one line of canonical
// JSON, sorted by file name.
//
// It exists to be diffed against crucible/oracle-java's CardRulesDumper, which
// produces the same bytes from Forge's own reader. That diff is the P1 gate:
// 100% of the corpus, fully deterministic, and it catches a parser divergence
// before it can hide behind a rules bug (ADR-0010).
//
//	go run ./tools/carddump -corpus ../forge-gui/res/cardsfolder \
//	    -types ../forge-gui/res/lists/TypeLists.txt > go-cards.ndjson
package main

import (
	"bufio"
	"flag"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"sort"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

func main() {
	corpus := flag.String("corpus", "../forge-gui/res/cardsfolder", "card script directory")
	types := flag.String("types", "../forge-gui/res/lists/TypeLists.txt", "subtype vocabulary")
	flag.Parse()

	if err := run(*corpus, *types); err != nil {
		fmt.Fprintf(os.Stderr, "carddump: %v\n", err)
		os.Exit(1)
	}
}

func run(corpus, types string) error {
	reg, err := loadRegistry(types)
	if err != nil {
		return err
	}
	scripts, err := scriptsSortedByName(corpus)
	if err != nil {
		return err
	}

	cards := make([]*carddb.Card, 0, len(scripts))
	for _, path := range scripts {
		raw, err := os.ReadFile(path)
		if err != nil {
			return err
		}
		name := strings.TrimSuffix(filepath.Base(path), ".txt")
		card, err := carddb.ParseScript(reg, name, raw)
		if err != nil {
			return err
		}
		cards = append(cards, card)
	}

	// Placeholders are deliberately left unresolved: Forge's Reader leaves them
	// too, and filling them here would diff against a Java dump that has not.

	out := bufio.NewWriterSize(os.Stdout, 1<<20)
	for _, card := range cards {
		if _, err := out.Write(card.CanonicalJSON()); err != nil {
			return err
		}
		if err := out.WriteByte('\n'); err != nil {
			return err
		}
	}
	return out.Flush()
}

func loadRegistry(path string) (*cardtype.Registry, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer func() { _ = f.Close() }()
	return cardtype.LoadRegistry(f)
}

// scriptsSortedByName sorts by base name, not by path, so the order matches the
// Java dumper's on a tree where the same name cannot appear twice.
func scriptsSortedByName(root string) ([]string, error) {
	var out []string
	err := filepath.WalkDir(root, func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if !d.IsDir() && filepath.Ext(path) == ".txt" {
			out = append(out, path)
		}
		return nil
	})
	if err != nil {
		return nil, err
	}
	sort.Slice(out, func(i, j int) bool {
		return filepath.Base(out[i]) < filepath.Base(out[j])
	})
	return out, nil
}
