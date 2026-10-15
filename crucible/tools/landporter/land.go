package main

import (
	"fmt"
	"io"
	"os/exec"
	"path/filepath"
	"strings"
)

const trailer = "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"

// runner executes name in dir and returns combined output.
type runner func(dir, name string, args ...string) (string, error)

func execRunner(dir, name string, args ...string) (string, error) {
	c := exec.Command(name, args...)
	c.Dir = dir
	out, err := c.CombinedOutput()
	return string(out), err
}

func trimNL(s string) string { return strings.TrimRight(s, "\r\n") }

func step(out io.Writer, r runner, dir, name string, args ...string) (string, error) {
	_, _ = fmt.Fprintf(out, "$ %s %s\n", name, strings.Join(args, " "))
	o, err := r(dir, name, args...)
	if err != nil {
		return o, fmt.Errorf("%s %s: %w\n%s", name, strings.Join(args, " "), err, tail(o, 20))
	}
	return o, nil
}

func tail(s string, n int) string {
	l := strings.Split(trimNL(s), "\n")
	if len(l) > n {
		l = l[len(l)-n:]
	}
	return strings.Join(l, "\n")
}

// land commits and merges the porter's branch, then reconciles the docs.
func land(root, branch, msg string, out io.Writer, r runner) error {
	script := filepath.Join(root, "crucible/scripts/merge-porters.sh")
	if _, err := step(out, r, root, script, "commit", branch, msg); err != nil {
		return err
	}
	o, err := step(out, r, root, script, "merge", branch)
	if err != nil {
		return err
	}
	_, _ = fmt.Fprintln(out, tail(o, 3))
	return reconcileCmd(root, out, r)
}

// reconcileCmd runs both smoke probes and rewrites the quoted numbers.
func reconcileCmd(root string, out io.Writer, r runner) error {
	mod := filepath.Join(root, "crucible")
	ao, err := step(out, r, mod, "go", "run", "./cmd/crucible", "card-smoke", "-all", "-workers", "8")
	if err != nil {
		return err
	}
	all, err := ParseSmoke(ao)
	if err != nil {
		return err
	}
	po, err := step(out, r, mod, "go", "run", "./cmd/crucible", "card-smoke",
		"-decks", "../forge-gui/res/quest/precons", "-workers", "8")
	if err != nil {
		return err
	}
	precon, err := ParseSmoke(po)
	if err != nil {
		return err
	}
	if all.Panicked > 0 || all.Hung > 0 {
		_, _ = fmt.Fprintf(out, "WARNING: %d panicked, %d hung: an engine bug\n", all.Panicked, all.Hung)
	}
	changed, before, err := Reconcile(root, all, precon)
	if err != nil {
		return err
	}
	_, _ = fmt.Fprint(out, Table(before, all, precon))
	_, _ = fmt.Fprintf(out, "rewrote: %s\n", strings.Join(changed, ", "))
	_, _ = fmt.Fprintln(out, "now: edit open-plan rows, smoke-probe refusal table, stale notes; then landporter finish")
	return nil
}

// finish formats and lints the docs, commits everything pending, removes the porter's worktree.
func finish(root, branch, msg string, out io.Writer, r runner) error {
	if _, err := step(out, r, root, "prettier", "--write", "CLAUDE.md", "docs/crucible"); err != nil {
		return err
	}
	if _, err := step(out, r, root, "npx", "markdownlint-cli2", "CLAUDE.md", "docs/crucible/**/*.md"); err != nil {
		return err
	}
	if _, err := step(out, r, root, "git", "add", "-A"); err != nil {
		return err
	}
	if _, err := step(out, r, root, "git", "commit", "-q", "-m", msg+"\n\n"+trailer); err != nil {
		return err
	}
	script := filepath.Join(root, "crucible/scripts/merge-porters.sh")
	o, err := r(root, script, "cleanup", branch)
	if err == nil && !strings.Contains(o, "remove -f -f") {
		_, _ = fmt.Fprintln(out, tail(o, 1))
		return nil
	}
	// The porter's pid still holds the worktree lock; its work is merged and committed above.
	wt, werr := r(root, "git", "worktree", "list", "--porcelain")
	if werr != nil {
		return werr
	}
	path := worktreeOf(wt, branch)
	if path == "" {
		return fmt.Errorf("cleanup failed and no worktree is on %s:\n%s", branch, tail(o, 5))
	}
	// Force only when nothing would be lost: no commit HEAD lacks, no uncommitted file.
	if n, err := r(root, "git", "rev-list", "--count", "HEAD.."+branch); err != nil || trimNL(n) != "0" {
		return fmt.Errorf("cleanup refused and %s has commits HEAD lacks; not forcing", branch)
	}
	if dirty, err := r(path, "git", "status", "--porcelain"); err != nil || trimNL(dirty) != "" {
		return fmt.Errorf("cleanup refused and %s has uncommitted changes; not forcing", path)
	}
	if _, err := step(out, r, root, "git", "worktree", "remove", "-f", "-f", path); err != nil {
		return err
	}
	_, err = step(out, r, root, "git", "branch", "-D", branch)
	return err
}

// worktreeOf finds the worktree path checked out on branch in `git worktree list --porcelain`.
func worktreeOf(porcelain, branch string) string {
	var cur string
	for _, l := range strings.Split(porcelain, "\n") {
		switch {
		case strings.HasPrefix(l, "worktree "):
			cur = strings.TrimPrefix(l, "worktree ")
		case l == "branch refs/heads/"+branch:
			return cur
		}
	}
	return ""
}
