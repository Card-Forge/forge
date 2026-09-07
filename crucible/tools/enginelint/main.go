package main

import (
	"flag"
	"fmt"
	"os"
)

func main() {
	cfgPath := flag.String("config", "", "path to the enginelint JSON config")
	flag.Parse()
	if *cfgPath == "" {
		fmt.Fprintln(os.Stderr, "enginelint: -config is required")
		os.Exit(2)
	}

	cfg, err := LoadConfig(*cfgPath)
	if err != nil {
		fmt.Fprintf(os.Stderr, "enginelint: %v\n", err)
		os.Exit(2)
	}

	violations, ungrouped, err := Check(*cfgPath, cfg)
	if err != nil {
		fmt.Fprintf(os.Stderr, "enginelint: %v\n", err)
		os.Exit(2)
	}

	// An ungrouped file is a failure, not a warning. A file that quietly belongs
	// to no group is exempt from every rule, which is how this check decays into
	// decoration.
	for _, f := range ungrouped {
		fmt.Fprintf(os.Stderr, "enginelint: %s matches no group in the config\n", f)
	}
	for _, v := range violations {
		fmt.Fprintln(os.Stderr, "enginelint: "+v.String())
	}
	if len(violations) > 0 || len(ungrouped) > 0 {
		fmt.Fprintf(os.Stderr, "enginelint: %d violation(s), %d ungrouped file(s)\n", len(violations), len(ungrouped))
		os.Exit(1)
	}
	fmt.Printf("enginelint: %s clean\n", cfg.Package)
}
