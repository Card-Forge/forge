package main

import (
	"flag"
	"fmt"
	"os"
)

func main() {
	profile := flag.String("profile", "", "coverage profile written by go test -coverprofile")
	guideline := flag.String("guideline", "../docs/crucible/guidelines/03-testing-standards.md",
		"guideline holding the TEST-12 floor table")
	moduleRoot := flag.String("module", ".", "path to the Go module root")
	flag.Parse()

	if *profile == "" {
		fmt.Fprintln(os.Stderr, "covergate: -profile is required")
		os.Exit(2)
	}

	results, err := Check(*profile, *guideline, *moduleRoot)
	if err != nil {
		fmt.Fprintf(os.Stderr, "covergate: %v\n", err)
		os.Exit(2)
	}

	failed := 0
	for _, r := range results {
		if r.Failed() {
			failed++
			fmt.Fprintln(os.Stderr, "covergate: FAIL "+r.String())
			continue
		}
		fmt.Println("covergate: ok   " + r.String())
	}
	if failed > 0 {
		fmt.Fprintf(os.Stderr, "covergate: %d package(s) below their TEST-12 floor\n", failed)
		os.Exit(1)
	}
	fmt.Printf("covergate: %d package(s) at or above their TEST-12 floor\n", len(results))
}
