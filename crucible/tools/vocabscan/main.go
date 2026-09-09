// Command vocabscan reports the card-script vocabulary the corpus uses.
//
// M3 compiles scripts into a typed AST, and every token in the corpus needs a
// type. This is how the size and the contents of that job are known rather than
// estimated: it enumerates each vocabulary and says how often each token
// appears, so a token nothing supports is a listed name instead of a runtime
// surprise (P2, ADR-0007).
//
//	go run ./tools/vocabscan                       # one line per vocabulary
//	go run ./tools/vocabscan -kind paramKey        # every token, by frequency
//	go run ./tools/vocabscan -names                # every token of every kind, sorted
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
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

func main() {
	corpus := flag.String("corpus", "../forge-gui/res/cardsfolder", "card script directory")
	types := flag.String("types", "../forge-gui/res/lists/TypeLists.txt", "subtype vocabulary")
	kind := flag.String("kind", "", "print every token of this vocabulary, most used first")
	names := flag.Bool("names", false, "print every token of every vocabulary, sorted, as kind<TAB>name")
	flag.Parse()

	if err := run(*corpus, *types, *kind, *names); err != nil {
		fmt.Fprintf(os.Stderr, "vocabscan: %v\n", err)
		os.Exit(1)
	}
}

func run(corpus, types, kind string, names bool) error {
	v, cards, err := scan(corpus, types)
	if err != nil {
		return err
	}

	// bufio.Writer keeps the first write error and returns it from Flush, so
	// the per-line results are dropped and the flush is what reports.
	out := bufio.NewWriterSize(os.Stdout, 1<<20)

	switch {
	case names:
		for _, k := range vocab.Kinds() {
			for _, name := range v.Names(k) {
				_, _ = fmt.Fprintf(out, "%s\t%s\n", k, name)
			}
		}
	case kind != "":
		k, ok := kindByName(kind)
		if !ok {
			return fmt.Errorf("unknown vocabulary %q; have %s", kind, strings.Join(kindNames(), ", "))
		}
		for _, name := range byFrequency(v, k) {
			_, _ = fmt.Fprintf(out, "%8d\t%s\n", v.Count(k, name), name)
		}
	default:
		_, _ = fmt.Fprintf(out, "%d cards\n\n%-18s %8s %14s\n", cards, "vocabulary", "distinct", "occurrences")
		for _, k := range vocab.Kinds() {
			_, _ = fmt.Fprintf(out, "%-18s %8d %14d\n", k, v.Distinct(k), v.Occurrences(k))
		}
	}
	return out.Flush()
}

// scan parses the whole corpus and folds it into one vocabulary.
func scan(corpus, types string) (*vocab.Vocabulary, int, error) {
	reg, err := loadRegistry(types)
	if err != nil {
		return nil, 0, err
	}

	v := vocab.New()
	cards := 0
	err = filepath.WalkDir(corpus, func(path string, d fs.DirEntry, err error) error {
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
		return nil, 0, err
	}
	return v, cards, nil
}

// byFrequency sorts a vocabulary's tokens by use, ties broken by name so the
// output is stable.
func byFrequency(v *vocab.Vocabulary, k vocab.Kind) []string {
	out := v.Names(k)
	sort.SliceStable(out, func(i, j int) bool {
		ci, cj := v.Count(k, out[i]), v.Count(k, out[j])
		if ci != cj {
			return ci > cj
		}
		return out[i] < out[j]
	})
	return out
}

func kindByName(name string) (vocab.Kind, bool) {
	for _, k := range vocab.Kinds() {
		if k.String() == name {
			return k, true
		}
	}
	return 0, false
}

func kindNames() []string {
	out := make([]string, 0, len(vocab.Kinds()))
	for _, k := range vocab.Kinds() {
		out = append(out, k.String())
	}
	return out
}

func loadRegistry(path string) (*cardtype.Registry, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer func() { _ = f.Close() }()
	return cardtype.LoadRegistry(f)
}
