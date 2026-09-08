package main

import (
	"path/filepath"
	"strings"
	"testing"
)

// Internal test, deliberately. docgate is a command, not a library, so Check
// and its helpers are unexported and there is no public API to test through
// (TEST-2). The fixture trees under testdata are the real subject.

func TestCheck(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name string
		dir  string
		want []string // substrings, one per expected finding
	}{
		{
			name: "a module whose packages are all documented is clean",
			dir:  "good",
		},
		{
			name: "every kind of gap is reported",
			dir:  "bad",
			want: []string{
				`pkg/undocumented: no row in architecture/module-map.md (DOC-12)`,
				`module-map.md:8: "internal/ported" is a port`,
				`module-map.md:9: row for "pkg/deleted", which does not exist`,
				`module-map.md:9: link to "../../module/pkg/deleted", which does not exist`,
				`module-map.md:9: link to "../porting/port-log/gone.md", which does not exist`,
				`documented.go:4: cites ADR-9999, which does not exist (ADRP-4)`,
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			moduleRoot := filepath.Join("testdata", tt.dir, "module")
			docsRoot := filepath.Join("testdata", tt.dir, "docs")

			findings, err := Check(moduleRoot, docsRoot)
			if err != nil {
				t.Fatalf("Check(%s) failed: %v", tt.dir, err)
			}
			if len(findings) != len(tt.want) {
				t.Fatalf("Check(%s) returned %d findings, want %d:\n%s",
					tt.dir, len(findings), len(tt.want), format(findings))
			}
			for _, want := range tt.want {
				if !containsFinding(findings, want) {
					t.Errorf("Check(%s): no finding contains %q. Got:\n%s", tt.dir, want, format(findings))
				}
			}
		})
	}
}

// A row under "Planned, not built" must not satisfy the gate, or a package
// could be documented as future work while it already exists.
func TestPlannedRowsDoNotCount(t *testing.T) {
	t.Parallel()

	findings, err := Check(filepath.Join("testdata", "bad", "module"), filepath.Join("testdata", "bad", "docs"))
	if err != nil {
		t.Fatalf("Check failed: %v", err)
	}
	if !containsFinding(findings, "pkg/undocumented: no row") {
		t.Errorf("pkg/undocumented is listed only under \"Planned, not built\" and was accepted anyway. Got:\n%s",
			format(findings))
	}
}

func TestCheckReportsAMissingModuleMap(t *testing.T) {
	t.Parallel()

	if _, err := Check(filepath.Join("testdata", "good", "module"), "testdata"); err == nil {
		t.Error("Check with no module map succeeded, want an error")
	}
}

func containsFinding(findings []Finding, want string) bool {
	for _, f := range findings {
		if strings.Contains(f.String(), want) {
			return true
		}
	}
	return false
}

func format(findings []Finding) string {
	if len(findings) == 0 {
		return "  (none)"
	}
	lines := make([]string, len(findings))
	for i, f := range findings {
		lines[i] = "  " + f.String()
	}
	return strings.Join(lines, "\n")
}
