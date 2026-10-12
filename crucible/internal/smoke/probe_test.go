package smoke_test

import (
	"path/filepath"
	"runtime"
	"sync"
	"testing"
	"time"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/smoke"
)

var (
	dbOnce sync.Once
	dbVal  *compile.DB
	dbErr  error
)

// corpusDB loads the real corpus once: the probe's fixed board is built from
// corpus cards, so a synthetic database would not exercise it.
func corpusDB(t *testing.T) *compile.DB {
	t.Helper()
	dbOnce.Do(func() {
		_, file, _, _ := runtime.Caller(0)
		root := filepath.Join(filepath.Dir(file), "..", "..", "..")
		types := filepath.Join(root, "forge-gui", "res", "lists", "TypeLists.txt")
		dbVal, dbErr = compile.LoadDB(filepath.Join(root, "forge-gui", "res", "cardsfolder"), types)
		if dbErr != nil {
			return
		}
		var tokens map[string]*compile.Card
		if tokens, dbErr = compile.LoadTokenScripts(filepath.Join(root, "forge-gui", "res", "tokenscripts"), types); dbErr == nil {
			dbVal = dbVal.WithTokens(tokens)
		}
	})
	if dbErr != nil {
		t.Fatalf("load corpus: %v", dbErr)
	}
	return dbVal
}

func TestProbeClassifiesCards(t *testing.T) {
	t.Parallel()

	p := smoke.NewProber(corpusDB(t))
	for _, tc := range []struct {
		card string
		want smoke.Outcome
	}{
		{"Lightning Bolt", smoke.Plays},
		{"Grizzly Bears", smoke.Plays},
		{"Forest", smoke.Plays},
		{"Not A Real Card", smoke.Refused},
		// An activated ability the probe exercises after the card resolves.
		{"Prodigal Sorcerer", smoke.Plays},
		// Needs a spell on the stack to target: the probe retries with one.
		{"Counterspell", smoke.Plays},
		{"Essence Scatter", smoke.Plays},
		// Needs a tapped creature, an attacking creature, or the rich board.
		{"Assassinate", smoke.Plays},
		{"Armed Response", smoke.Plays},
		{"Abhorrent Oculus", smoke.Plays},
		// 12 generic exceeds the old pool of ten of one colour; Autochthon
		// Wurm needs the pool to hold enough for convoke-sized costs too.
		{"Blightsteel Colossus", smoke.Plays},
		{"Autochthon Wurm", smoke.Plays},
		// ValidTgts$ no generic board satisfies: the probe places a card that
		// the spec accepts, or casts one for a TargetType$ Spell target.
		{"Clear a Path", smoke.Plays},
		{"Celestial Purge", smoke.Plays},
		{"Annul", smoke.Plays},
		// Command-zone types are placed, not cast; a sticker sheet is refused.
		{"Akoum", smoke.Placed},
		{"Backup Plan", smoke.Placed},
		{"Ancestral Hot Dog Minotaur", smoke.Refused},
	} {
		t.Run(tc.card, func(t *testing.T) {
			t.Parallel()

			if got := p.Probe(tc.card); got.Outcome != tc.want {
				t.Errorf("Probe(%q) = %v (%s: %s), want %v", tc.card, got.Outcome, got.Stage, got.Detail, tc.want)
			}
		})
	}
}

// A probe that outlives its limit is reported Hung instead of blocking the run.
func TestProbeWithinReportsAHang(t *testing.T) {
	t.Parallel()

	p := smoke.NewProber(corpusDB(t))
	if got := p.ProbeWithin("Lightning Bolt", time.Nanosecond); got.Outcome != smoke.Hung {
		t.Errorf("ProbeWithin with a 1ns limit = %v, want hung", got.Outcome)
	}
	if got := p.ProbeWithin("Lightning Bolt", time.Minute); got.Outcome != smoke.Plays {
		t.Errorf("ProbeWithin with a generous limit = %v, want plays", got.Outcome)
	}
}

// Reason groups refusals that differ only in a card name, number or id.
func TestReasonBlanksWhatDiffersAcrossCards(t *testing.T) {
	t.Parallel()

	a := smoke.Reason(`engine: Pump: KW$ "Flying" not resolvable yet` + "\nstack frame")
	b := smoke.Reason(`engine: Pump: KW$ "Haste" not resolvable yet`)
	if a != b {
		t.Errorf("Reason differs across card-specific text: %q vs %q", a, b)
	}
	if got := smoke.Reason("engine: Draw: NumCards$ 12 is not resolvable"); got != "engine: Draw: NumCards$ N is not resolvable" {
		t.Errorf("Reason(number) = %q", got)
	}
}

func TestOutcomeStrings(t *testing.T) {
	t.Parallel()

	for o, want := range map[smoke.Outcome]string{
		smoke.Plays: "plays", smoke.Refused: "refused", smoke.Panicked: "panicked",
		smoke.Inconclusive: "inconclusive", smoke.Hung: "hung", smoke.Placed: "placed",
	} {
		if o.String() != want {
			t.Errorf("Outcome(%d).String() = %q, want %q", o, o.String(), want)
		}
	}
}

// A nil database is a refusal that says so, not a panic: the card lookup is
// nil-safe, which is what lets a bad corpus path end as a report line.
func TestProbeOnNoDatabaseRefuses(t *testing.T) {
	t.Parallel()

	if got := smoke.NewProber(nil).Probe("Lightning Bolt"); got.Outcome != smoke.Refused {
		t.Errorf("Probe on a nil database = %v, want refused", got.Outcome)
	}
}

// The tailored board places a target in the zone the spell names (exile, hand,
// graveyard), not only on the battlefield. The engine's own answer for these
// cards is not what is tested: whatever it is, the probe must reach it without
// a panic or a hang.
func TestProbeTailorsTargetsInEveryZone(t *testing.T) {
	t.Parallel()

	p := smoke.NewProber(corpusDB(t))
	for _, card := range []string{
		"Runic Repetition", "Pull from Eternity", "Ruthless Negotiation", "Never Happened", "Regenesis", "Release to Memory",
	} {
		t.Run(card, func(t *testing.T) {
			t.Parallel()

			got := p.Probe(card)
			if got.Outcome == smoke.Panicked || got.Outcome == smoke.Hung {
				t.Errorf("Probe(%q) = %v (%s: %s)", card, got.Outcome, got.Stage, got.Detail)
			}
		})
	}
}
