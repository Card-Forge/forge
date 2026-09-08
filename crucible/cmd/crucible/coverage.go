package main

import (
	"flag"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"sort"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/deck"
)

// corpusCoverage reports, per decklist, which card names the database does not
// have. At M2 that is the whole of "supported"; from M6 the same command also
// answers whether every ability in those cards is implemented (ADR-0011).
func corpusCoverage(args []string) error {
	fs := flag.NewFlagSet("corpus-coverage", flag.ExitOnError)
	corpus := fs.String("corpus", "../forge-gui/res/cardsfolder", "card script directory")
	types := fs.String("types", "../forge-gui/res/lists/TypeLists.txt", "subtype vocabulary")
	decks := fs.String("decks", "", "decklist file, or a directory searched for .dck files")
	quiet := fs.Bool("quiet", false, "print only the summary and any unsupported cards")
	if err := fs.Parse(args); err != nil {
		return err
	}
	if *decks == "" {
		return fmt.Errorf("-decks is required")
	}

	db, err := loadCards(*corpus, *types)
	if err != nil {
		return err
	}
	paths, err := decklistPaths(*decks)
	if err != nil {
		return err
	}
	if len(paths) == 0 {
		return fmt.Errorf("no decklists under %s", *decks)
	}

	var (
		unsupported = map[string][]string{} // card name -> decks needing it
		decksClean  int
	)
	for _, path := range paths {
		content, err := os.ReadFile(path)
		if err != nil {
			return err
		}
		d := deck.Parse(strings.TrimSuffix(filepath.Base(path), ".dck"), content)

		missing := missingCards(db, d)
		if len(missing) == 0 {
			decksClean++
			if !*quiet {
				fmt.Printf("ok   %-60s %d cards\n", d.Name, len(d.Names()))
			}
			continue
		}
		for _, name := range missing {
			unsupported[name] = append(unsupported[name], d.Name)
		}
		fmt.Printf("MISS %-60s %d of %d cards missing\n", d.Name, len(missing), len(d.Names()))
	}

	fmt.Printf("\n%d of %d decklists fully covered\n", decksClean, len(paths))
	if len(unsupported) == 0 {
		return nil
	}

	names := make([]string, 0, len(unsupported))
	for name := range unsupported {
		names = append(names, name)
	}
	sort.Slice(names, func(i, j int) bool {
		if len(unsupported[names[i]]) != len(unsupported[names[j]]) {
			return len(unsupported[names[i]]) > len(unsupported[names[j]])
		}
		return names[i] < names[j]
	})

	fmt.Printf("\n%d card names are not in the database:\n", len(names))
	for i, name := range names {
		if i == 40 {
			fmt.Printf("  ... and %d more\n", len(names)-40)
			break
		}
		fmt.Printf("  %-50s wanted by %d deck(s)\n", name, len(unsupported[name]))
	}
	return fmt.Errorf("%d unsupported card name(s)", len(names))
}

// missingCards returns the deck's card names the database does not have.
//
// Lookup ignores case, because every name map in Forge's CardDb is built with
// String.CASE_INSENSITIVE_ORDER: a decklist writing "Knight Of The Reliquary"
// finds the card, and Crucible refusing it would be stricter than the engine
// it is scoped against.
func missingCards(db map[string]*carddb.Card, d *deck.Deck) []string {
	var missing []string
	for _, name := range d.Names() {
		if lookup(db, name) == nil {
			missing = append(missing, name)
		}
	}
	return missing
}

// lookup finds a card by the name a decklist wrote, ignoring case and accents.
func lookup(db map[string]*carddb.Card, name string) *carddb.Card {
	if card, ok := db[strings.ToLower(name)]; ok {
		return card
	}
	return db[strings.ToLower(carddb.NormalizeName(name))]
}

// loadCards parses the corpus and indexes it by card name, which is what a
// decklist refers to.
func loadCards(corpus, types string) (map[string]*carddb.Card, error) {
	f, err := os.Open(types)
	if err != nil {
		return nil, err
	}
	defer func() { _ = f.Close() }()
	reg, err := cardtype.LoadRegistry(f)
	if err != nil {
		return nil, err
	}

	db := map[string]*carddb.Card{}
	var cards []*carddb.Card
	err = filepath.WalkDir(corpus, func(path string, d fs.DirEntry, err error) error {
		if err != nil || d.IsDir() || filepath.Ext(path) != ".txt" {
			return err
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
		return nil, err
	}

	if err := carddb.ResolvePlaceholders(cards, carddb.IndexByFaceName(cards)); err != nil {
		return nil, err
	}

	// A decklist names the card: "Start // Fire" for a split card, the front
	// face for a transforming one.
	index := func(name string, card *carddb.Card, overwrite bool) {
		for _, key := range []string{strings.ToLower(name), strings.ToLower(carddb.NormalizeName(name))} {
			if overwrite || db[key] == nil {
				db[key] = card
			}
		}
	}
	for _, card := range cards {
		if name := card.Name(); name != "" {
			index(name, card, true)
		}
	}
	// Each face is addressable too, because decklists disagree about which
	// half of a split card to write.
	for name, card := range carddb.IndexByFaceName(cards) {
		index(name, card, false)
	}
	return db, nil
}

// decklistPaths accepts a single file or a directory to search.
func decklistPaths(root string) ([]string, error) {
	info, err := os.Stat(root)
	if err != nil {
		return nil, err
	}
	if !info.IsDir() {
		return []string{root}, nil
	}

	var out []string
	err = filepath.WalkDir(root, func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if !d.IsDir() && strings.EqualFold(filepath.Ext(path), ".dck") {
			out = append(out, path)
		}
		return nil
	})
	sort.Strings(out)
	return out, err
}
