// Package main implements docgate, the doc-before-code check.
//
// DOC-12 and ADRP-4 say documentation lands in the same commit as the code it
// describes: a new package gets a row in architecture/module-map.md, a ported
// unit gets a port-log note, and an ADR merges before the code that cites it.
// Until this tool existed, all three were enforced by review, which is the
// enforcement that quietly stops happening under deadline.
//
// It reads the module and the documents, and reports four things:
//
//	a Go package with no row in the module map            (DOC-12)
//	a row or link in that map pointing at nothing         (DOC-11)
//	a ported package with no port-log note in its row     (PORT-4)
//	an "ADR-nnnn" reference in Go code with no ADR file   (ADRP-4)
package main

import (
	"fmt"
	"go/parser"
	"go/token"
	"io/fs"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
)

// Finding is one documentation gap. Every field is filled in from the thing
// that is wrong, so the message names a file a reader can open.
type Finding struct {
	File   string // path as given on the command line, so it is clickable
	Line   int    // 0 when the finding is about a directory
	Reason string
}

func (f Finding) String() string {
	if f.Line == 0 {
		return fmt.Sprintf("%s: %s", f.File, f.Reason)
	}
	return fmt.Sprintf("%s:%d: %s", f.File, f.Line, f.Reason)
}

// moduleMapPath is where the package rows live, relative to the docs root.
const moduleMapPath = "architecture/module-map.md"

// packagesHeading starts the section docgate reads. Rows outside it -- the
// planned-but-not-built table, for instance -- deliberately do not count, or a
// package could satisfy the check by being listed as future work.
const packagesHeading = "## Packages"

var (
	// A markdown link: [text](target). Text may carry backticks.
	linkRE = regexp.MustCompile(`\[([^\]]*)\]\(([^)]+)\)`)
	// A backticked package path in the first cell of a row.
	packageRE = regexp.MustCompile("`((?:pkg|internal|tools|cmd)/[A-Za-z0-9_/-]+)`")
	// An ADR citation in a Go comment: "// ADR-0009: arena handles" (GO-15).
	adrRefRE = regexp.MustCompile(`ADR-(\d{4})`)
	// The provenance comment every ported unit carries (GO-15).
	portedRE = regexp.MustCompile(`(?m)^//\s*Ported from `)
)

// row is one documented package.
type row struct {
	pkg     string // import path relative to the module root
	line    int
	portLog string // the last cell, verbatim
}

// Check runs every gate. moduleRoot is the Go module directory, docsRoot the
// documentation tree; both are used verbatim in findings.
func Check(moduleRoot, docsRoot string) ([]Finding, error) {
	mapPath := filepath.Join(docsRoot, moduleMapPath)
	rows, links, err := readModuleMap(mapPath)
	if err != nil {
		return nil, err
	}

	pkgs, err := findPackages(moduleRoot)
	if err != nil {
		return nil, err
	}

	var findings []Finding

	documented := make(map[string]row, len(rows))
	for _, r := range rows {
		documented[r.pkg] = r
		if _, err := os.Stat(filepath.Join(moduleRoot, filepath.FromSlash(r.pkg))); err != nil {
			findings = append(findings, Finding{mapPath, r.line,
				fmt.Sprintf("row for %q, which does not exist under %s", r.pkg, moduleRoot)})
		}
	}

	for _, target := range links {
		if _, err := os.Stat(filepath.Join(filepath.Dir(mapPath), filepath.FromSlash(target.path))); err != nil {
			findings = append(findings, Finding{mapPath, target.line,
				fmt.Sprintf("link to %q, which does not exist", target.path)})
		}
	}

	for _, pkg := range pkgs {
		r, ok := documented[pkg.path]
		if !ok {
			findings = append(findings, Finding{filepath.Join(moduleRoot, filepath.FromSlash(pkg.path)), 0,
				fmt.Sprintf("no row in %s (DOC-12): every package is documented in the commit that creates it", moduleMapPath)})
			continue
		}
		// A package whose files say where they came from is a port, and PORT-4
		// requires the note that says how it diverged.
		if pkg.ported && !linkRE.MatchString(r.portLog) {
			findings = append(findings, Finding{mapPath, r.line,
				fmt.Sprintf("%q is a port -- its files carry a \"Ported from\" comment -- but its row links to no port-log note (PORT-4)", pkg.path)})
		}
	}

	refs, err := findADRRefs(moduleRoot)
	if err != nil {
		return nil, err
	}
	for _, ref := range refs {
		matches, err := filepath.Glob(filepath.Join(docsRoot, "adr", ref.number+"-*.md"))
		if err != nil {
			return nil, fmt.Errorf("search for ADR %s: %w", ref.number, err)
		}
		if len(matches) == 0 {
			findings = append(findings, Finding{ref.file, ref.line,
				fmt.Sprintf("cites ADR-%s, which does not exist (ADRP-4): the ADR merges before the code", ref.number)})
		}
	}

	sort.Slice(findings, func(i, j int) bool {
		if findings[i].File != findings[j].File {
			return findings[i].File < findings[j].File
		}
		return findings[i].Line < findings[j].Line
	})
	return findings, nil
}

type link struct {
	path string
	line int
}

// readModuleMap returns the package rows and every link inside the Packages
// section.
func readModuleMap(path string) ([]row, []link, error) {
	raw, err := os.ReadFile(path)
	if err != nil {
		return nil, nil, fmt.Errorf("read module map: %w", err)
	}

	var (
		rows    []row
		links   []link
		inScope bool
	)
	for i, text := range strings.Split(string(raw), "\n") {
		lineNo := i + 1
		if strings.HasPrefix(text, "## ") {
			inScope = strings.TrimSpace(text) == packagesHeading
			continue
		}
		if !inScope || !strings.HasPrefix(strings.TrimSpace(text), "|") {
			continue
		}

		cells := splitRow(text)
		if len(cells) == 0 || strings.HasPrefix(strings.TrimSpace(cells[0]), "-") {
			continue // separator row
		}
		pkg := packageRE.FindStringSubmatch(cells[0])
		if pkg == nil {
			continue // header row, or a row about something that is not a package
		}
		rows = append(rows, row{pkg: pkg[1], line: lineNo, portLog: cells[len(cells)-1]})

		for _, m := range linkRE.FindAllStringSubmatch(text, -1) {
			target := m[2]
			if strings.HasPrefix(target, "http") || strings.HasPrefix(target, "#") {
				continue
			}
			links = append(links, link{path: target, line: lineNo})
		}
	}
	if len(rows) == 0 {
		return nil, nil, fmt.Errorf("%s: no package rows under %q", path, packagesHeading)
	}
	return rows, links, nil
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

type pkgInfo struct {
	path   string // relative to the module root, slash-separated
	ported bool   // at least one file carries a provenance comment
}

// findPackages returns every directory under root holding Go source, except
// testdata trees, which hold fixtures rather than packages.
func findPackages(root string) ([]pkgInfo, error) {
	found := make(map[string]bool)
	ported := make(map[string]bool)

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
		if filepath.Ext(path) != ".go" {
			return nil
		}

		rel, err := filepath.Rel(root, filepath.Dir(path))
		if err != nil {
			return err
		}
		dir := filepath.ToSlash(rel)
		if dir == "." {
			return nil // the module root itself holds no package here
		}
		found[dir] = true

		if !strings.HasSuffix(path, "_test.go") && !ported[dir] {
			raw, err := os.ReadFile(path)
			if err != nil {
				return err
			}
			if portedRE.Match(raw) {
				ported[dir] = true
			}
		}
		return nil
	})
	if err != nil {
		return nil, fmt.Errorf("walk %s: %w", root, err)
	}

	out := make([]pkgInfo, 0, len(found))
	for dir := range found {
		out = append(out, pkgInfo{path: dir, ported: ported[dir]})
	}
	sort.Slice(out, func(i, j int) bool { return out[i].path < out[j].path })
	return out, nil
}

type adrRef struct {
	file   string
	line   int
	number string
}

// findADRRefs collects every ADR citation in a Go comment. Test files count: a
// test that pins an ADR's decision is exactly the code that must not outlive
// it.
//
// Comments only, and through the parser rather than a line scan, because a
// citation is a claim the code makes about a decision. A string literal holding
// an ADR number -- a test asserting on this tool's own output, for one --
// claims nothing.
func findADRRefs(root string) ([]adrRef, error) {
	var refs []adrRef
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
		if filepath.Ext(path) != ".go" {
			return nil
		}

		fset := token.NewFileSet()
		file, err := parser.ParseFile(fset, path, nil, parser.ParseComments|parser.SkipObjectResolution)
		if err != nil {
			return fmt.Errorf("parse %s: %w", path, err)
		}
		for _, group := range file.Comments {
			for _, c := range group.List {
				for _, m := range adrRefRE.FindAllStringSubmatch(c.Text, -1) {
					refs = append(refs, adrRef{file: path, line: fset.Position(c.Pos()).Line, number: m[1]})
				}
			}
		}
		return nil
	})
	if err != nil {
		return nil, fmt.Errorf("walk %s: %w", root, err)
	}
	return refs, nil
}
