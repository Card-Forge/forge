// Command javacycles counts direct two-package import cycles in a Java source
// tree.
//
// It exists so ADR-0003's central number is reproducible rather than asserted.
// That ADR concluded internal/engine must be one Go package because forge-game
// has 82 direct package cycles and Go forbids them — a claim worth being able
// to re-run, especially after an upstream sync changes the tree.
//
//	go run ./tools/javacycles -root ../forge-game/src/main/java -prefix forge.game
package main

import (
	"bufio"
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
)

func main() {
	root := flag.String("root", "", "Java source root, e.g. ../forge-game/src/main/java")
	prefix := flag.String("prefix", "", "only consider imports under this package prefix")
	expect := flag.Int("expect", -1, "if set, exit non-zero unless the count matches")
	verbose := flag.Bool("v", false, "list every cycle")
	flag.Parse()

	if *root == "" || *prefix == "" {
		fmt.Fprintln(os.Stderr, "javacycles: -root and -prefix are required")
		os.Exit(2)
	}

	edges, err := scan(*root, *prefix)
	if err != nil {
		fmt.Fprintf(os.Stderr, "javacycles: %v\n", err)
		os.Exit(2)
	}

	var cycles [][2]string
	for a, deps := range edges {
		for b := range deps {
			// Each unordered pair once: a < b keeps it deterministic.
			if a < b && edges[b][a] {
				cycles = append(cycles, [2]string{a, b})
			}
		}
	}
	sort.Slice(cycles, func(i, j int) bool {
		if cycles[i][0] != cycles[j][0] {
			return cycles[i][0] < cycles[j][0]
		}
		return cycles[i][1] < cycles[j][1]
	})

	if *verbose {
		for _, c := range cycles {
			fmt.Printf("  %s <-> %s\n", c[0], c[1])
		}
	}
	fmt.Printf("packages: %d\ndirect two-package cycles: %d\n", len(edges), len(cycles))

	if *expect >= 0 && len(cycles) != *expect {
		fmt.Fprintf(os.Stderr,
			"javacycles: expected %d cycles, found %d — ADR-0003's premise has changed and the ADR needs revisiting\n",
			*expect, len(cycles))
		os.Exit(1)
	}
}

var importRe = regexp.MustCompile(`^import\s+([\w.]+)\.[A-Z]\w*\s*;`)

// scan returns package -> set of packages it imports, restricted to prefix.
func scan(root, prefix string) (map[string]map[string]bool, error) {
	edges := map[string]map[string]bool{}
	err := filepath.WalkDir(root, func(path string, d os.DirEntry, err error) error {
		if err != nil || d.IsDir() || !strings.HasSuffix(path, ".java") {
			return err
		}
		rel, err := filepath.Rel(root, filepath.Dir(path))
		if err != nil {
			return err
		}
		pkg := strings.ReplaceAll(rel, string(filepath.Separator), ".")
		if !strings.HasPrefix(pkg, prefix) {
			return nil
		}
		f, err := os.Open(path)
		if err != nil {
			return err
		}
		defer f.Close()
		if edges[pkg] == nil {
			edges[pkg] = map[string]bool{}
		}
		sc := bufio.NewScanner(f)
		for sc.Scan() {
			line := strings.TrimSpace(sc.Text())
			// Imports precede the type declaration, so stop early rather than
			// scanning tens of thousands of body lines.
			if strings.HasPrefix(line, "public ") || strings.HasPrefix(line, "final ") {
				break
			}
			m := importRe.FindStringSubmatch(line)
			if m == nil {
				continue
			}
			if target := m[1]; strings.HasPrefix(target, prefix) && target != pkg {
				edges[pkg][target] = true
			}
		}
		return sc.Err()
	})
	return edges, err
}
