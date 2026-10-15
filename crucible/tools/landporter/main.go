// landporter automates the main session's repeated steps after a worktree porter
// hands back: commit and merge its branch, remeasure the smoke probe, rewrite every
// doc that quotes the measurement, and finish with the docs commit and cleanup.
//
//	landporter land BRANCH MSG      merge-porters commit + merge, then reconcile
//	landporter reconcile            run both smoke probes, rewrite the quoted numbers
//	landporter finish BRANCH MSG    prettier, markdownlint, commit docs, cleanup
package main

import (
	"fmt"
	"io"
	"os"
)

func main() {
	os.Exit(run(os.Args[1:], os.Stdout, os.Stderr, execRunner))
}

func run(args []string, out, errw io.Writer, r runner) int {
	if len(args) == 0 {
		_, _ = fmt.Fprintln(errw, "usage: landporter land BRANCH MSG | reconcile | finish BRANCH MSG")
		return 2
	}
	root, err := r(".", "git", "rev-parse", "--show-toplevel")
	if err != nil {
		_, _ = fmt.Fprintf(errw, "landporter: not in a git checkout: %v\n", err)
		return 2
	}
	root = trimNL(root)
	var cmdErr error
	switch args[0] {
	case "land":
		if len(args) != 3 {
			_, _ = fmt.Fprintln(errw, "usage: landporter land BRANCH MSG")
			return 2
		}
		cmdErr = land(root, args[1], args[2], out, r)
	case "reconcile":
		cmdErr = reconcileCmd(root, out, r)
	case "finish":
		if len(args) != 3 {
			_, _ = fmt.Fprintln(errw, "usage: landporter finish BRANCH MSG")
			return 2
		}
		cmdErr = finish(root, args[1], args[2], out, r)
	default:
		_, _ = fmt.Fprintf(errw, "landporter: unknown command %q\n", args[0])
		return 2
	}
	if cmdErr != nil {
		_, _ = fmt.Fprintf(errw, "landporter: %v\n", cmdErr)
		return 1
	}
	return 0
}
