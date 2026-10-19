// Command genregistry generates internal/engine's NewRegistry from the
// package's own effect types (ADR-0017).
//
// Every type named *Effect with a value-receiver
// Resolve(*Game, *Ability, PlayerController) error registers under the API its
// name gives: drawEffect -> APIDraw. A type that serves several APIs, another
// API, or needs field values says so with //crucible:register lines in its doc
// comment. Hand-written wiring made every effect port edit the same function,
// so two parallel ports always conflicted there.
//
//	go generate -run genregistry ./internal/engine   # from the module root
//	go run ./tools/genregistry -dir internal/engine -check \
//	    -docs ../CLAUDE.md
//
// -check writes nothing and fails when the committed file is stale, or when a
// doc's resolved-API count disagrees with the registry. ADR-0008 named that
// check as the condition for trusting generated wiring at all.
package main

import (
	"bytes"
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	dir := flag.String("dir", ".", "the engine package directory")
	out := flag.String("out", "registry_gen.go", "output file, inside -dir")
	check := flag.Bool("check", false, "fail if the output file is stale instead of writing it")
	docs := flag.String("docs", "", "comma-separated docs whose resolved-API count must match")
	flag.Parse()

	reg, err := Scan(*dir, *out)
	if err != nil {
		fail(2, "%v", err)
	}
	src, err := Render(reg)
	if err != nil {
		fail(2, "format: %v", err)
	}

	path := filepath.Join(*dir, *out)
	var problems []string
	if *check {
		have, err := os.ReadFile(path)
		if err != nil || !bytes.Equal(have, src) {
			problems = append(problems, fmt.Sprintf("%s is stale: run go generate -run genregistry ./internal/engine", path))
		}
	} else if err := os.WriteFile(path, src, 0o644); err != nil {
		fail(2, "%v", err)
	}
	if *docs != "" {
		problems = append(problems, CheckDocs(strings.Split(*docs, ","), reg.M6Count())...)
	}
	for _, p := range problems {
		fmt.Fprintln(os.Stderr, "genregistry: "+p)
	}
	if len(problems) > 0 {
		os.Exit(1)
	}
	fmt.Printf("genregistry: %d APIs registered, %d resolved script-driven\n", len(reg.Entries), reg.M6Count())
}

func fail(code int, format string, args ...any) {
	fmt.Fprintf(os.Stderr, "genregistry: "+format+"\n", args...)
	os.Exit(code)
}
