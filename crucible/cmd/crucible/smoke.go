package main

import (
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/deck"
	"github.com/jczastkiewicz/crucible/internal/smoke"
)

// cardSmoke is `crucible card-smoke`: it probes cards through the engine and
// reports how many play (internal/smoke). The card set is the distinct cards of
// the decklists under -decks, the names in -cards, or the whole corpus with -all.
//
// It prints the cards the engine refused and, above all, the refusal reasons
// grouped by cause: that table is the M6 backlog ordered by how many cards
// each cause blocks.
func cardSmoke(args []string) error {
	fs := flag.NewFlagSet("card-smoke", flag.ExitOnError)
	corpus := fs.String("corpus", "../forge-gui/res/cardsfolder", "card script directory")
	tokens := fs.String("tokens", "../forge-gui/res/tokenscripts", "token script directory")
	types := fs.String("types", "../forge-gui/res/lists/TypeLists.txt", "subtype vocabulary")
	decks := fs.String("decks", "", "decklist file, or a directory searched for .dck files")
	cards := fs.String("cards", "", "comma-separated card names")
	all := fs.Bool("all", false, "probe every card in the corpus")
	workers := fs.Int("workers", 4, "games probed in parallel")
	top := fs.Int("top", 25, "refusal causes to print")
	list := fs.Bool("list", false, "print one line per card")
	timeout := fs.Duration("timeout", 20*time.Second, "limit per card before it is reported hung")
	if err := fs.Parse(args); err != nil {
		return err
	}

	db, err := compile.LoadDB(*corpus, *types)
	if err != nil {
		return err
	}
	tok, err := compile.LoadTokenScripts(*tokens, *types)
	if err != nil {
		return err
	}
	db = db.WithTokens(tok)

	names, source, err := smokeCards(db, *decks, *cards, *all)
	if err != nil {
		return err
	}

	results := probeAll(smoke.NewProber(db), names, *workers, *timeout)
	fmt.Print(report(source, results, *top, *list))
	return nil
}

// smokeCards resolves the card set to names the database holds.
func smokeCards(db *compile.DB, decks, cards string, all bool) (names []string, source string, err error) {
	byLower := map[string]string{}
	for _, n := range db.Names() {
		byLower[strings.ToLower(n)] = n
	}
	resolve := func(name string) (string, bool) {
		for _, k := range []string{strings.ToLower(name), strings.ToLower(carddb.NormalizeName(name))} {
			if n, ok := byLower[k]; ok {
				return n, true
			}
		}
		return "", false
	}

	switch {
	case all:
		return db.Names(), "the whole corpus", nil
	case cards != "":
		for _, n := range strings.Split(cards, ",") {
			if r, ok := resolve(strings.TrimSpace(n)); ok {
				names = append(names, r)
			} else {
				return nil, "", fmt.Errorf("no card named %q", n)
			}
		}
		return names, "the named cards", nil
	case decks != "":
		paths, err := decklistPaths(decks)
		if err != nil {
			return nil, "", err
		}
		seen := map[string]bool{}
		unknown := 0
		for _, path := range paths {
			content, err := os.ReadFile(path)
			if err != nil {
				return nil, "", err
			}
			for _, n := range deck.Parse(strings.TrimSuffix(filepath.Base(path), ".dck"), content).Names() {
				r, ok := resolve(n)
				switch {
				case !ok:
					unknown++
				case !seen[r]:
					seen[r] = true
					names = append(names, r)
				}
			}
		}
		sort.Strings(names)
		src := fmt.Sprintf("%d decklists under %s", len(paths), decks)
		if unknown > 0 {
			src += fmt.Sprintf(" (%d card names unknown to the corpus, left out)", unknown)
		}
		return names, src, nil
	}
	return nil, "", fmt.Errorf("one of -decks, -cards or -all is required")
}

// probeAll probes names on workers goroutines, in name order in the result.
func probeAll(p *smoke.Prober, names []string, workers int, limit time.Duration) []smoke.Result {
	if workers < 1 {
		workers = 1
	}
	results := make([]smoke.Result, len(names))
	jobs := make(chan int)
	var wg sync.WaitGroup
	for range workers {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for i := range jobs {
				results[i] = p.ProbeWithin(names[i], limit)
			}
		}()
	}
	for i := range names {
		jobs <- i
	}
	close(jobs)
	wg.Wait()
	return results
}

// report renders the summary, the refusal causes by weight, and every panic.
func report(source string, results []smoke.Result, top int, list bool) string {
	var out strings.Builder
	var count [5]int
	causes := map[string][]string{}
	var panics []smoke.Result
	for _, r := range results {
		count[r.Outcome]++
		switch r.Outcome {
		case smoke.Refused:
			key := r.Stage[:strings.IndexAny(r.Stage+" ", " ")] + ": " + smoke.Reason(r.Detail)
			causes[key] = append(causes[key], r.Card)
		case smoke.Panicked, smoke.Hung:
			panics = append(panics, r)
		}
		if list {
			fmt.Fprintf(&out, "%-12s %-45s %-16s %s\n", r.Outcome, r.Card, r.Stage, smoke.Reason(r.Detail))
		}
	}
	judged := count[smoke.Plays] + count[smoke.Refused] + count[smoke.Panicked] + count[smoke.Hung]
	fmt.Fprintf(&out, "\nsmoke probe of %s: %d cards\n", source, len(results))
	fmt.Fprintf(&out, "  plays        %6d\n  refused      %6d\n  panicked     %6d\n  hung         %6d\n  inconclusive %6d\n",
		count[smoke.Plays], count[smoke.Refused], count[smoke.Panicked], count[smoke.Hung], count[smoke.Inconclusive])
	if judged > 0 {
		fmt.Fprintf(&out, "  %.1f%% of the %d judged cards play (an upper bound: silent misses are invisible)\n",
			100*float64(count[smoke.Plays])/float64(judged), judged)
	}

	keys := make([]string, 0, len(causes))
	for k := range causes {
		keys = append(keys, k)
	}
	sort.Slice(keys, func(i, j int) bool {
		if len(causes[keys[i]]) != len(causes[keys[j]]) {
			return len(causes[keys[i]]) > len(causes[keys[j]])
		}
		return keys[i] < keys[j]
	})
	if len(keys) > 0 {
		fmt.Fprintf(&out, "\nrefusal causes, most cards first:\n")
	}
	for i, k := range keys {
		if i == top {
			fmt.Fprintf(&out, "  ... and %d more causes\n", len(keys)-top)
			break
		}
		cs := causes[k]
		sort.Strings(cs)
		eg := strings.Join(cs[:min(3, len(cs))], ", ")
		fmt.Fprintf(&out, "  %5d  %s\n         e.g. %s\n", len(cs), k, eg)
	}
	for _, r := range panics {
		fmt.Fprintf(&out, "\n%s %s at %s:\n%s\n", strings.ToUpper(r.Outcome.String()), r.Card, r.Stage, r.Detail)
	}
	return out.String()
}
