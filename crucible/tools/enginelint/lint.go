// Package main implements enginelint, a file-group dependency check for a
// single Go package.
//
// It exists because of ADR-0003. forge-game has 82 direct package cycles, so
// internal/engine is one Go package and cannot be subdivided — and that ADR's
// own consequences admit the cost: "discipline replaces enforcement inside that
// boundary, which is exactly the weakest kind of guarantee."
//
// Go has no sub-package visibility, so the compiler cannot help. This tool
// restores enforcement by reading the package's own files, grouping them, and
// failing on a reference that crosses a group boundary the config does not
// allow.
package main

import (
	"encoding/json"
	"fmt"
	"go/ast"
	"go/build"
	"go/parser"
	"go/token"
	"os"
	"path/filepath"
	"sort"
	"strings"
)

// Config declares the groups within one package and which may reference which.
type Config struct {
	// Package is the directory to check, relative to the config file.
	Package string `json:"package"`
	// Groups maps a group name to the file globs that belong to it. A file
	// matching no glob is an error: silent membership is how a rule decays.
	Groups map[string][]string `json:"groups"`
	// Allow maps a group to the groups it may reference, itself implied.
	Allow map[string][]string `json:"allow"`
}

// Violation is one disallowed cross-group reference.
type Violation struct {
	File     string
	Line     int
	Ident    string
	FromGrp  string
	ToGrp    string
	DeclFile string
}

func (v Violation) String() string {
	return fmt.Sprintf("%s:%d: group %q may not reference %q (%s is declared in %s)",
		v.File, v.Line, v.FromGrp, v.ToGrp, v.Ident, v.DeclFile)
}

// LoadConfig reads a JSON config.
func LoadConfig(path string) (*Config, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read config: %w", err)
	}
	var c Config
	if err := json.Unmarshal(b, &c); err != nil {
		return nil, fmt.Errorf("parse config %s: %w", path, err)
	}
	if c.Package == "" {
		return nil, fmt.Errorf("config %s: package is required", path)
	}
	if len(c.Groups) == 0 {
		return nil, fmt.Errorf("config %s: at least one group is required", path)
	}
	return &c, nil
}

// groupOf returns the group a file belongs to, or "" with ok=false.
func (c *Config) groupOf(base string) (string, bool) {
	// Sorted for determinism: a file matching two globs must resolve the same
	// way on every machine, or CI and a laptop disagree.
	names := make([]string, 0, len(c.Groups))
	for g := range c.Groups {
		names = append(names, g)
	}
	sort.Strings(names)
	for _, g := range names {
		for _, pat := range c.Groups[g] {
			if ok, err := filepath.Match(pat, base); err == nil && ok {
				return g, true
			}
		}
	}
	return "", false
}

func (c *Config) allows(from, to string) bool {
	if from == to {
		return true
	}
	for _, g := range c.Allow[from] {
		if g == to {
			return true
		}
	}
	return false
}

// Check parses the package named by the config and returns every violation,
// plus any file that matched no group.
func Check(cfgPath string, c *Config) (violations []Violation, ungrouped []string, err error) {
	dir := filepath.Join(filepath.Dir(cfgPath), c.Package)
	fset := token.NewFileSet()
	files, err := parsePackage(fset, dir)
	if err != nil {
		return nil, nil, err
	}

	// declaredIn maps a top-level identifier to the file that declares it.
	// Within one package an unqualified reference resolves to a package-level
	// declaration, so go/ast is sufficient and go/types is not needed —
	// which keeps this stdlib-only (ADR-0002).
	declaredIn := map[string]string{}
	fileGroup := map[string]string{}

	for base, f := range files {
		g, ok := c.groupOf(base)
		if !ok {
			ungrouped = append(ungrouped, base)
			continue
		}
		fileGroup[base] = g
		for _, d := range f.Decls {
			for _, name := range topLevelNames(d) {
				declaredIn[name] = base
			}
		}
	}
	sort.Strings(ungrouped)

	for base, f := range files {
		from, ok := fileGroup[base]
		if !ok {
			continue
		}
		ast.Inspect(f, func(n ast.Node) bool {
			id, ok := n.(*ast.Ident)
			if !ok {
				return true
			}
			decl, ok := declaredIn[id.Name]
			if !ok || decl == base {
				return true
			}
			to := fileGroup[decl]
			if c.allows(from, to) {
				return true
			}
			violations = append(violations, Violation{
				File: base, Line: fset.Position(id.Pos()).Line,
				Ident: id.Name, FromGrp: from, ToGrp: to, DeclFile: decl,
			})
			return true
		})
	}
	sort.Slice(violations, func(i, j int) bool {
		if violations[i].File != violations[j].File {
			return violations[i].File < violations[j].File
		}
		return violations[i].Line < violations[j].Line
	})
	return violations, ungrouped, nil
}

// topLevelNames returns the package-level identifiers a declaration introduces.
func topLevelNames(d ast.Decl) []string {
	var out []string
	switch d := d.(type) {
	case *ast.FuncDecl:
		// Methods are reached through their receiver's type, which is itself a
		// top-level name, so only plain functions are tracked here.
		if d.Recv == nil {
			out = append(out, d.Name.Name)
		}
	case *ast.GenDecl:
		for _, spec := range d.Specs {
			switch s := spec.(type) {
			case *ast.TypeSpec:
				out = append(out, s.Name.Name)
			case *ast.ValueSpec:
				for _, n := range s.Names {
					out = append(out, n.Name)
				}
			}
		}
	}
	return out
}

// parsePackage returns the non-test Go files of one directory, keyed by base
// name, honouring build constraints.
//
// go/parser.ParseDir would be shorter and is wrong here: it is deprecated, and
// it does not consider build tags when associating files with a package. A tool
// whose entire job is knowing which files are in a package cannot be blind to
// the thing that decides it — a debug-only file behind a build tag would be
// grouped and checked as though it were always compiled.
//
// go/build is stdlib and applies the same constraints the compiler does, which
// keeps this dependency-free (ADR-0002).
func parsePackage(fset *token.FileSet, dir string) (map[string]*ast.File, error) {
	entries, err := os.ReadDir(dir)
	if err != nil {
		return nil, fmt.Errorf("read %s: %w", dir, err)
	}
	ctx := build.Default
	out := map[string]*ast.File{}
	for _, e := range entries {
		name := e.Name()
		if e.IsDir() || !strings.HasSuffix(name, ".go") {
			continue
		}
		// Test files are excluded: they legitimately reach across groups, and
		// forcing them to comply would push tests toward the structure rather
		// than the behaviour (TEST-1).
		if strings.HasSuffix(name, "_test.go") {
			continue
		}
		ok, err := ctx.MatchFile(dir, name)
		if err != nil {
			return nil, fmt.Errorf("build constraints for %s: %w", name, err)
		}
		if !ok {
			continue
		}
		f, err := parser.ParseFile(fset, filepath.Join(dir, name), nil, 0)
		if err != nil {
			return nil, fmt.Errorf("parse %s: %w", name, err)
		}
		out[name] = f
	}
	return out, nil
}
