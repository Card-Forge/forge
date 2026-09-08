package main

import (
	"path/filepath"
	"strings"
	"testing"
)

// Internal test, deliberately. covergate is a command, not a library, so Check
// and its helpers are unexported and there is no public API to test through
// (TEST-2). The fixture module, guideline and profiles under testdata are the
// real subject.

const (
	guideline = "testdata/guideline.md"
	module    = "testdata/module"
)

func TestCheck(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name     string
		profile  string
		module   string
		wantFail []string // substrings, one per expected failure
		wantOK   []string // substrings that must appear among the passes
	}{
		{
			name:    "every package at or above its floor",
			profile: "green.out",
			module:  module,
			wantOK: []string{
				"internal/mana: 100.0%, floor 90%",
				"internal/carddb/compile: 96.0%, floor 95%",
				"internal/engine/effect: 10.0%, no floor",
				"tools/thing: 0.0%, no floor",
			},
		},
		{
			name:     "a package under its floor fails",
			profile:  "below.out",
			module:   module,
			wantFail: []string{"internal/mana: 80.0%, floor 90%"},
			wantOK:   []string{"internal/carddb/compile: 95.0%, floor 95%"},
		},
		{
			name:     "a profile missing a package fails rather than passing it",
			profile:  "stale.out",
			module:   module,
			wantFail: []string{"internal/mana: no coverage data"},
		},
		{
			name:     "a package matching no row fails",
			profile:  "orphan.out",
			module:   "testdata/module-orphan",
			wantFail: []string{"pkg/orphan: no coverage floor declared (TEST-12)"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			results, err := Check(filepath.Join("testdata", tt.profile), guideline, tt.module)
			if err != nil {
				t.Fatalf("Check(%s) failed: %v", tt.profile, err)
			}

			var failed, passed []string
			for _, r := range results {
				if r.Failed() {
					failed = append(failed, r.String())
					continue
				}
				passed = append(passed, r.String())
			}

			if len(failed) != len(tt.wantFail) {
				t.Fatalf("Check(%s) reported %d failures, want %d:\n%s",
					tt.profile, len(failed), len(tt.wantFail), strings.Join(failed, "\n"))
			}
			for _, want := range tt.wantFail {
				if !containsSubstring(failed, want) {
					t.Errorf("Check(%s): no failure contains %q. Got:\n%s", tt.profile, want, strings.Join(failed, "\n"))
				}
			}
			for _, want := range tt.wantOK {
				if !containsSubstring(passed, want) {
					t.Errorf("Check(%s): no pass contains %q. Got:\n%s", tt.profile, want, strings.Join(passed, "\n"))
				}
			}
		})
	}
}

// The floor a package gets is the most specific row naming it, so a package
// listed by name is not silently governed by a `/**` row above it.
func TestMostSpecificRowWins(t *testing.T) {
	t.Parallel()

	floors, err := readFloors(guideline)
	if err != nil {
		t.Fatalf("readFloors failed: %v", err)
	}
	for _, tt := range []struct {
		pkg   string
		floor float64
	}{
		{"internal/carddb/compile", 95}, // named row, not the carddb/** row
		{"internal/carddb", 90},         // the /** row covers the package itself
		{"internal/carddb/reader", 90},
		{"internal/mana", 90},
	} {
		got, ok := matchFloor(floors, tt.pkg)
		if !ok {
			t.Errorf("matchFloor(%q): no row matched", tt.pkg)
			continue
		}
		if got.min != tt.floor {
			t.Errorf("matchFloor(%q) floor = %v, want %v", tt.pkg, got.min, tt.floor)
		}
	}
}

// Only the TEST-12 table is read. Another table in the same guideline -- and
// there are several -- must not become a floor.
func TestOnlyTheFloorTableIsRead(t *testing.T) {
	t.Parallel()

	floors, err := readFloors(guideline)
	if err != nil {
		t.Fatalf("readFloors failed: %v", err)
	}
	if len(floors) != 4 {
		t.Fatalf("readFloors returned %d rows, want 4 -- a table outside TEST-12 was read", len(floors))
	}
	if _, ok := matchFloor(floors, "time.Sleep"); ok {
		t.Error("a row from the TEST-11 table became a coverage floor")
	}
}

func TestReadFloorsRejectsABadFloor(t *testing.T) {
	t.Parallel()

	if _, err := readFloors("testdata/guideline-bad-floor.md"); err == nil {
		t.Error("readFloors accepted a floor that is neither a percentage nor \"none\"")
	}
}

func TestCheckRejectsAnEmptyProfile(t *testing.T) {
	t.Parallel()

	// Reading an empty profile as "nothing to check" would turn the gate off
	// exactly when the test run failed to produce data.
	if _, err := Check("testdata/guideline.md", guideline, module); err == nil {
		t.Error("Check accepted a file with no coverage records")
	}
}

func containsSubstring(haystack []string, want string) bool {
	for _, s := range haystack {
		if strings.Contains(s, want) {
			return true
		}
	}
	return false
}
