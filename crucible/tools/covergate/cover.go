// Package main implements covergate, the per-package coverage floor check.
//
// TEST-12 declares a floor per package group and REV-4 lists it as blocking,
// but nothing measured it: a package could fall from 96% to 40% and merge. This
// tool closes that.
//
// The floors are not configured here. They are read from the TEST-12 table in
// docs/crucible/guidelines/03-testing-standards.md, so the guideline is the
// single place the numbers exist (DOC-11) and a change to CI's behaviour is a
// visible change to the rule.
package main

import (
	"bufio"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"
)

// floorsHeading starts the section holding the table. Rows anywhere else in the
// guideline are prose about coverage, not the contract.
const floorsHeading = "## TEST-12"

var (
	// A backticked package pattern in the first cell: `internal/mana`,
	// `pkg/**`, `internal/carddb/**`.
	patternRE = regexp.MustCompile("`([A-Za-z0-9_./*-]+)`")
	// A floor: "90%".
	floorRE = regexp.MustCompile(`^(\d{1,3})%$`)
)

// floor is one row of the TEST-12 table.
type floor struct {
	patterns []string
	min      float64 // percent; ignored when exempt
	exempt   bool
	line     int
}

// coverage is one package's statement count.
type coverage struct {
	covered, total int
}

func (c coverage) percent() float64 {
	if c.total == 0 {
		return 100 // nothing to cover; not a failure
	}
	return 100 * float64(c.covered) / float64(c.total)
}

// Result is one package's verdict.
type Result struct {
	Pkg      string
	Percent  float64
	Floor    float64
	Exempt   bool
	NoFloor  bool // matched no row in TEST-12
	NoData   bool // exists in the module but not in the profile
	FloorSrc int  // guideline line the floor came from
}

// Failed reports whether this result blocks the build.
func (r Result) Failed() bool {
	return r.NoFloor || r.NoData || (!r.Exempt && r.Percent < r.Floor)
}

func (r Result) String() string {
	switch {
	case r.NoFloor:
		return fmt.Sprintf("%s: no coverage floor declared (TEST-12): add a row, or the package has no gate", r.Pkg)
	case r.NoData:
		return fmt.Sprintf("%s: no coverage data; was the profile written with ./... ?", r.Pkg)
	case r.Exempt:
		return fmt.Sprintf("%s: %.1f%%, no floor (TEST-12 line %d)", r.Pkg, r.Percent, r.FloorSrc)
	default:
		return fmt.Sprintf("%s: %.1f%%, floor %.0f%% (TEST-12 line %d)", r.Pkg, r.Percent, r.Floor, r.FloorSrc)
	}
}

// Check measures every package in the module against the TEST-12 table.
func Check(profilePath, guidelinePath, moduleRoot string) ([]Result, error) {
	floors, err := readFloors(guidelinePath)
	if err != nil {
		return nil, err
	}
	modulePath, err := readModulePath(moduleRoot)
	if err != nil {
		return nil, err
	}
	measured, err := readProfile(profilePath, modulePath)
	if err != nil {
		return nil, err
	}
	pkgs, err := findPackages(moduleRoot)
	if err != nil {
		return nil, err
	}

	// A package in the profile but not on disk cannot happen from one run, but
	// a stale profile makes it possible, and silently ignoring it would let a
	// deleted package's old numbers stand in for a new one.
	for pkg := range measured {
		if !contains(pkgs, pkg) {
			pkgs = append(pkgs, pkg)
		}
	}
	sort.Strings(pkgs)

	results := make([]Result, 0, len(pkgs))
	for _, pkg := range pkgs {
		r := Result{Pkg: pkg}
		cov, ok := measured[pkg]
		if !ok {
			r.NoData = true
			results = append(results, r)
			continue
		}
		r.Percent = cov.percent()

		f, ok := matchFloor(floors, pkg)
		if !ok {
			r.NoFloor = true
			results = append(results, r)
			continue
		}
		r.Floor, r.Exempt, r.FloorSrc = f.min, f.exempt, f.line
		results = append(results, r)
	}
	return results, nil
}

// matchFloor returns the most specific row covering pkg. Longest pattern wins,
// so a package listed by name beats one caught by a `/**` prefix.
func matchFloor(floors []floor, pkg string) (floor, bool) {
	var (
		best    floor
		bestLen = -1
	)
	for _, f := range floors {
		for _, pattern := range f.patterns {
			if !matchPattern(pattern, pkg) {
				continue
			}
			if len(pattern) > bestLen {
				best, bestLen = f, len(pattern)
			}
		}
	}
	return best, bestLen >= 0
}

func matchPattern(pattern, pkg string) bool {
	if prefix, ok := strings.CutSuffix(pattern, "/**"); ok {
		return pkg == prefix || strings.HasPrefix(pkg, prefix+"/")
	}
	return pattern == pkg
}

// readFloors parses the TEST-12 table.
func readFloors(path string) ([]floor, error) {
	raw, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read guideline: %w", err)
	}

	var (
		floors  []floor
		inScope bool
	)
	for i, text := range strings.Split(string(raw), "\n") {
		if strings.HasPrefix(text, "## ") {
			inScope = strings.HasPrefix(text, floorsHeading)
			continue
		}
		if !inScope || !strings.HasPrefix(strings.TrimSpace(text), "|") {
			continue
		}

		cells := splitRow(text)
		if len(cells) < 2 || strings.HasPrefix(cells[0], "-") {
			continue // separator row
		}
		patterns := patternRE.FindAllStringSubmatch(cells[0], -1)
		if patterns == nil {
			continue // header row
		}

		f := floor{line: i + 1}
		for _, m := range patterns {
			f.patterns = append(f.patterns, m[1])
		}
		switch value := cells[1]; {
		case value == "none":
			f.exempt = true
		case floorRE.MatchString(value):
			n, err := strconv.Atoi(strings.TrimSuffix(value, "%"))
			if err != nil {
				return nil, fmt.Errorf("%s:%d: floor %q: %w", path, i+1, value, err)
			}
			f.min = float64(n)
		default:
			return nil, fmt.Errorf("%s:%d: floor %q is neither a percentage nor \"none\"", path, i+1, value)
		}
		floors = append(floors, f)
	}
	if len(floors) == 0 {
		return nil, fmt.Errorf("%s: no floors found under %q", path, floorsHeading)
	}
	return floors, nil
}

func splitRow(text string) []string {
	trimmed := strings.Trim(strings.TrimSpace(text), "|")
	if trimmed == "" {
		return nil
	}
	cells := strings.Split(trimmed, "|")
	for i := range cells {
		cells[i] = strings.TrimSpace(cells[i])
	}
	return cells
}

// readProfile sums the statements of a Go coverage profile per package.
func readProfile(path, modulePath string) (map[string]coverage, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, fmt.Errorf("read profile: %w", err)
	}
	defer func() { _ = f.Close() }()

	out := make(map[string]coverage)
	sc := bufio.NewScanner(f)
	sc.Buffer(make([]byte, 0, 1<<20), 1<<20)
	for line := 1; sc.Scan(); line++ {
		text := strings.TrimSpace(sc.Text())
		if text == "" || strings.HasPrefix(text, "mode:") {
			continue
		}

		// path/to/file.go:12.34,15.2 3 1
		fields := strings.Fields(text)
		if len(fields) != 3 {
			return nil, fmt.Errorf("%s:%d: want 3 fields, got %d", path, line, len(fields))
		}
		file, _, ok := strings.Cut(fields[0], ":")
		if !ok {
			return nil, fmt.Errorf("%s:%d: no position in %q", path, line, fields[0])
		}
		stmts, err := strconv.Atoi(fields[1])
		if err != nil {
			return nil, fmt.Errorf("%s:%d: statement count %q: %w", path, line, fields[1], err)
		}
		count, err := strconv.Atoi(fields[2])
		if err != nil {
			return nil, fmt.Errorf("%s:%d: hit count %q: %w", path, line, fields[2], err)
		}

		pkg := strings.TrimPrefix(filepath.ToSlash(filepath.Dir(file)), modulePath+"/")
		cov := out[pkg]
		cov.total += stmts
		if count > 0 {
			cov.covered += stmts
		}
		out[pkg] = cov
	}
	if err := sc.Err(); err != nil {
		return nil, fmt.Errorf("scan %s: %w", path, err)
	}
	if len(out) == 0 {
		return nil, fmt.Errorf("%s: no coverage records", path)
	}
	return out, nil
}

// readModulePath returns the module path from go.mod, so profile paths can be
// cut down to package paths.
func readModulePath(moduleRoot string) (string, error) {
	raw, err := os.ReadFile(filepath.Join(moduleRoot, "go.mod"))
	if err != nil {
		return "", fmt.Errorf("read go.mod: %w", err)
	}
	for _, line := range strings.Split(string(raw), "\n") {
		if path, ok := strings.CutPrefix(strings.TrimSpace(line), "module "); ok {
			return strings.TrimSpace(path), nil
		}
	}
	return "", fmt.Errorf("%s/go.mod: no module line", moduleRoot)
}

// findPackages returns every directory under root holding non-test Go source,
// except testdata trees. A package with only test files has nothing to cover.
func findPackages(root string) ([]string, error) {
	found := make(map[string]bool)
	err := filepath.WalkDir(root, func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if d.IsDir() {
			name := d.Name()
			if path != root && (name == "testdata" || strings.HasPrefix(name, ".") || strings.HasPrefix(name, "_")) {
				return fs.SkipDir
			}
			return nil
		}
		if filepath.Ext(path) != ".go" || strings.HasSuffix(path, "_test.go") {
			return nil
		}
		rel, err := filepath.Rel(root, filepath.Dir(path))
		if err != nil {
			return err
		}
		if dir := filepath.ToSlash(rel); dir != "." {
			found[dir] = true
		}
		return nil
	})
	if err != nil {
		return nil, fmt.Errorf("walk %s: %w", root, err)
	}

	out := make([]string, 0, len(found))
	for dir := range found {
		out = append(out, dir)
	}
	sort.Strings(out)
	return out, nil
}

func contains(xs []string, want string) bool {
	for _, x := range xs {
		if x == want {
			return true
		}
	}
	return false
}
