// Internal test: the commands live in package main, which an external test package cannot import.
package main

import (
	"bytes"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

const allOut = `
  plays          30618
  refused         2895
  panicked           0
  hung               0
  placed           372  (Command-zone cards)
  inconclusive     193
  91.4% of the 33513 judged cards play (an upper bound)
`

const preOut = `smoke probe of 505 decklists under x (285 card names unknown to the corpus, left out): 8668 cards
  plays          8093
  refused         547
  panicked          0
  hung              0
  placed            0
  inconclusive     28
  93.7% of the 8640 judged cards play (an upper bound)
`

func TestParseSmoke(t *testing.T) {
	t.Parallel()
	r, err := ParseSmoke(preOut)
	if err != nil {
		t.Fatal(err)
	}
	if r.Plays != 8093 || r.Refused != 547 || r.Inconclusive != 28 || r.Pct != "93.7" || r.Judged != 8640 ||
		r.Decks != 505 || r.Cards != 8668 {
		t.Fatalf("got %+v", r)
	}
	if _, err := ParseSmoke("plays 1"); err == nil {
		t.Fatal("truncated output must fail")
	}
	if _, err := ParseSmoke(strings.Replace(allOut, "91.4% of the 33513 judged", "", 1)); err == nil {
		t.Fatal("missing percent must fail")
	}
}

func tree(t *testing.T) string {
	t.Helper()
	root := t.TempDir()
	files := map[string]string{
		smokeDoc: "| plays        | 30,000 |\n| refused      |  2,000 |\n| panicked     |      0 |\n" +
			"| hung         |      0 |\n| placed       |    300 |\n| inconclusive |    100 |\n\n" +
			"90.0% of the 33,000 judged cards play. On the 8,000 distinct cards of the 500 quest\n" +
			"preconstructed decks it is 92.0% of 8,000\njudged.\n",
		claudeMD: "plays: 90.0% of 33,000\njudged, an upper bound.\n",
		"docs/crucible/00-master-implementation-plan.md":             "86.0% of the\ncorpus's judged cards play\n",
		"docs/crucible/00-master-implementation-plan-in-progress.md": "90.0%\nof the 33,512 judged corpus cards\n",
		"docs/crucible/00-master-implementation-plan-open.md":        "90.0% of the 33,000 judged corpus\n",
	}
	for rel, s := range files {
		p := filepath.Join(root, rel)
		if err := os.MkdirAll(filepath.Dir(p), 0o755); err != nil {
			t.Fatal(err)
		}
		if err := os.WriteFile(p, []byte(s), 0o644); err != nil {
			t.Fatal(err)
		}
	}
	return root
}

func TestReconcileRewritesEveryQuotedNumber(t *testing.T) {
	t.Parallel()
	root := tree(t)
	all, _ := ParseSmoke(allOut)
	pre, _ := ParseSmoke(preOut)
	changed, before, err := Reconcile(root, all, pre)
	if err != nil {
		t.Fatal(err)
	}
	if len(changed) != 5 || before["plays"] != 30000 {
		t.Fatalf("changed %v before %v", changed, before)
	}
	read := func(rel string) string {
		b, _ := os.ReadFile(filepath.Join(root, rel))
		return string(b)
	}
	for rel, want := range map[string]string{
		smokeDoc: "| plays        | 30,618 |",
		claudeMD: "91.4% of 33,513\njudged",
		"docs/crucible/00-master-implementation-plan.md":             "91.4% of the\ncorpus's judged",
		"docs/crucible/00-master-implementation-plan-in-progress.md": "91.4%\nof the 33,513 judged",
	} {
		if !strings.Contains(read(rel), want) {
			t.Errorf("%s lacks %q:\n%s", rel, want, read(rel))
		}
	}
	for _, want := range []string{"91.4% of the 33,513 judged cards play.", "On the 8,668 distinct cards of the 505 quest",
		"it is 93.7% of 8,640\njudged"} {
		if !strings.Contains(read(smokeDoc), want) {
			t.Errorf("smoke doc lacks %q:\n%s", want, read(smokeDoc))
		}
	}
	if tb := Table(before, all, pre); !strings.Contains(tb, "| plays | 30,000 | 30,618 |") {
		t.Errorf("table:\n%s", tb)
	}
}

func TestReconcileFailsWhenADocDrifts(t *testing.T) {
	t.Parallel()
	root := tree(t)
	if err := os.WriteFile(filepath.Join(root, claudeMD), []byte("no percentage here\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	all, _ := ParseSmoke(allOut)
	pre, _ := ParseSmoke(preOut)
	if _, _, err := Reconcile(root, all, pre); err == nil || !strings.Contains(err.Error(), claudeMD) {
		t.Fatalf("want an error naming CLAUDE.md, got %v", err)
	}
}

// fake records calls and answers by command prefix.
type fake struct {
	calls []string
	reply map[string]string
	fail  map[string]bool
}

func (f *fake) run(_, name string, args ...string) (string, error) {
	line := name + " " + strings.Join(args, " ")
	f.calls = append(f.calls, line)
	for k, v := range f.reply {
		if strings.Contains(line, k) {
			if f.fail[k] {
				return v, os.ErrInvalid
			}
			return v, nil
		}
	}
	return "", nil
}

func TestLandMergesThenReconciles(t *testing.T) {
	t.Parallel()
	root := tree(t)
	f := &fake{reply: map[string]string{
		"rev-parse":              root + "\n",
		"-all":                   allOut,
		"-decks":                 preOut,
		"merge-porters.sh merge": "merged; 191 resolved\n",
	}}
	var out, errw bytes.Buffer
	if code := run([]string{"land", "b", "msg"}, &out, &errw, f.run); code != 0 {
		t.Fatalf("exit %d: %s", code, errw.String())
	}
	joined := strings.Join(f.calls, "\n")
	if !strings.Contains(joined, "merge-porters.sh commit b msg") ||
		strings.Index(joined, "commit b") > strings.Index(joined, "merge b") {
		t.Fatalf("order:\n%s", joined)
	}
	if !strings.Contains(out.String(), "| plays | 30,000 | 30,618 |") {
		t.Fatalf("out:\n%s", out.String())
	}
}

func TestFinishForcesOnlyAMergedCleanWorktree(t *testing.T) {
	t.Parallel()
	porcelain := "worktree /r\nbranch refs/heads/master\n\nworktree /r/.claude/worktrees/a\nbranch refs/heads/b\n"
	for name, tc := range map[string]struct {
		ahead     string
		wantForce bool
	}{"merged": {"0\n", true}, "unmerged commits": {"2\n", false}} {
		f := &fake{
			reply: map[string]string{
				"rev-parse --show-toplevel": "/r\n",
				"cleanup":                   "use 'remove -f -f' to override or unlock first\n",
				"worktree list":             porcelain,
				"rev-list":                  tc.ahead,
			},
			fail: map[string]bool{"cleanup": true},
		}
		var out, errw bytes.Buffer
		code := run([]string{"finish", "b", "docs: x"}, &out, &errw, f.run)
		forced := strings.Contains(strings.Join(f.calls, "\n"), "worktree remove -f -f /r/.claude/worktrees/a")
		if forced != tc.wantForce || (code == 0) != tc.wantForce {
			t.Errorf("%s: forced=%v code=%d err=%s", name, forced, code, errw.String())
		}
	}
}

func TestUsageErrors(t *testing.T) {
	t.Parallel()
	f := &fake{reply: map[string]string{"rev-parse": "/r\n"}}
	for _, args := range [][]string{{}, {"land"}, {"finish", "b"}, {"nope"}} {
		var out, errw bytes.Buffer
		if code := run(args, &out, &errw, f.run); code != 2 {
			t.Errorf("%v: exit %d", args, code)
		}
	}
}
