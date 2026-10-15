// landporter automates the main session's repeated steps after a worktree porter
// hands back: commit and merge its branch, remeasure the smoke probe, rewrite every
// doc that quotes the measurement, and finish with the docs commit and cleanup.
//
//	landporter land BRANCH MSG      merge-porters commit + merge, then reconcile
//	landporter reconcile            run both smoke probes, rewrite the quoted numbers
//	landporter finish BRANCH MSG    prettier, markdownlint, commit docs, cleanup
//	landporter brief ROW SLUG       agent prompt for an open-plan row
//	landporter plan rm ROW...       delete open-plan rows
//	landporter plan add -after ROW -id ID [-phase M6 -size S -dep D] TEXT   insert a row
package main

import (
	"errors"
	"flag"
	"fmt"
	"io"
	"os"
)

func main() {
	os.Exit(run(os.Args[1:], os.Stdout, os.Stderr, execRunner))
}

func run(args []string, out, errw io.Writer, r runner) int {
	if len(args) == 0 {
		_, _ = fmt.Fprintln(errw, "usage: landporter land BRANCH MSG | reconcile | finish BRANCH MSG | brief ROW SLUG | plan rm|add")
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
	case "plan":
		cmdErr = planCmd(root, args[1:])
	case "brief":
		if len(args) != 3 {
			_, _ = fmt.Fprintln(errw, "usage: landporter brief ROW SLUG")
			return 2
		}
		var p string
		if p, cmdErr = brief(root, args[1], args[2]); cmdErr == nil {
			_, _ = fmt.Fprint(out, p)
		}
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

func planCmd(root string, args []string) error {
	if len(args) < 2 {
		return errors.New("usage: landporter plan rm ROW... | plan add -after ROW -id ID [-phase P -size S -dep D] TEXT")
	}
	switch args[0] {
	case "rm":
		return planRm(root, args[1:])
	case "add":
		fs := flag.NewFlagSet("plan add", flag.ContinueOnError)
		after := fs.String("after", "", "row to insert after")
		id := fs.String("id", "", "new row id")
		phase := fs.String("phase", "M6", "phase cell")
		size := fs.String("size", "S", "size cell")
		dep := fs.String("dep", "`card-smoke` re-rank", "needs cell")
		if err := fs.Parse(args[1:]); err != nil {
			return err
		}
		if *after == "" || *id == "" || fs.NArg() != 1 {
			return errors.New("plan add needs -after, -id and one TEXT argument")
		}
		return planAdd(root, *after, *id, fs.Arg(0), *phase, *size, *dep)
	}
	return fmt.Errorf("unknown plan command %q", args[0])
}
