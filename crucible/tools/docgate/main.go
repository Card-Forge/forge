package main

import (
	"flag"
	"fmt"
	"os"
)

func main() {
	moduleRoot := flag.String("module", ".", "path to the Go module root")
	docsRoot := flag.String("docs", "../docs/crucible", "path to the Crucible documentation tree")
	flag.Parse()

	findings, err := Check(*moduleRoot, *docsRoot)
	if err != nil {
		fmt.Fprintf(os.Stderr, "docgate: %v\n", err)
		os.Exit(2)
	}

	for _, f := range findings {
		fmt.Fprintln(os.Stderr, "docgate: "+f.String())
	}
	if len(findings) > 0 {
		fmt.Fprintf(os.Stderr, "docgate: %d undocumented change(s)\n", len(findings))
		os.Exit(1)
	}
	fmt.Println("docgate: every package documented, every ADR reference resolves")
}
