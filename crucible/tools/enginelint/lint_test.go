package main

import (
	"path/filepath"
	"strings"
	"testing"
)

// Internal test, deliberately. Check and LoadConfig are unexported because
// enginelint is a command, not a library, so there is no public API to test
// through (TEST-2). The fixtures under testdata are the real subject.

func TestCheck(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name           string
		dir            string
		wantViolations []string // substrings, one per expected violation
		wantUngrouped  []string
	}{
		{
			name: "a package obeying its groups is clean",
			dir:  "good",
		},
		{
			name: "a reference against the allowed direction is reported",
			dir:  "bad",
			wantViolations: []string{
				`state.go:6: group "state" may not reference "stack"`,
			},
			wantUngrouped: []string{"orphan.go"},
		},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			cfgPath := filepath.Join("testdata", tc.dir, "enginelint.json")
			cfg, err := LoadConfig(cfgPath)
			if err != nil {
				t.Fatalf("LoadConfig(%s): %v", cfgPath, err)
			}
			got, ungrouped, err := Check(cfgPath, cfg)
			if err != nil {
				t.Fatalf("Check: %v", err)
			}
			if len(got) != len(tc.wantViolations) {
				t.Fatalf("got %d violations, want %d:\n%s", len(got), len(tc.wantViolations), joinViolations(got))
			}
			for i, want := range tc.wantViolations {
				if !strings.Contains(got[i].String(), want) {
					t.Errorf("violation %d = %q, want it to contain %q", i, got[i], want)
				}
			}
			if strings.Join(ungrouped, ",") != strings.Join(tc.wantUngrouped, ",") {
				t.Errorf("ungrouped = %v, want %v", ungrouped, tc.wantUngrouped)
			}
		})
	}
}

// A group may always reference itself, or every single-group config would fail.
func TestAllowsIsReflexive(t *testing.T) {
	t.Parallel()
	c := &Config{Allow: map[string][]string{}}
	if !c.allows("state", "state") {
		t.Error(`allows("state","state") = false, want true`)
	}
	if c.allows("state", "stack") {
		t.Error(`allows("state","stack") with no rule = true, want false`)
	}
}

// Glob resolution must not depend on Go's map iteration order, or CI and a
// laptop can disagree about which group a file belongs to (GO-12).
func TestGroupOfIsDeterministic(t *testing.T) {
	t.Parallel()
	c := &Config{Groups: map[string][]string{
		"alpha": {"*.go"},
		"beta":  {"card*.go"},
		"gamma": {"card.go"},
	}}
	first, ok := c.groupOf("card.go")
	if !ok {
		t.Fatal("groupOf(card.go) found no group")
	}
	for i := range 200 {
		got, _ := c.groupOf("card.go")
		if got != first {
			t.Fatalf("iteration %d: groupOf(card.go) = %q, first call said %q", i, got, first)
		}
	}
}

func TestLoadConfigRejectsIncomplete(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct{ name, path string }{
		{"missing file", filepath.Join("testdata", "nope.json")},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			if _, err := LoadConfig(tc.path); err == nil {
				t.Errorf("LoadConfig(%s) = nil error, want one", tc.path)
			}
		})
	}
}

func joinViolations(vs []Violation) string {
	var b strings.Builder
	for _, v := range vs {
		b.WriteString("  " + v.String() + "\n")
	}
	return b.String()
}
