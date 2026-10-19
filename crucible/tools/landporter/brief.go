package main

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

const runbook = "docs/crucible/runbooks/subagent-worktrees.md"

// brief builds the prompt for an effect-porter-hard agent that works open-plan row id.
func brief(root, id, slug string) (string, error) {
	text, err := planRow(root, id)
	if err != nil {
		return "", err
	}
	b, err := os.ReadFile(filepath.Join(root, runbook))
	if err != nil {
		return "", err
	}
	_, after, ok := strings.Cut(string(b), "## Prompt block")
	if !ok {
		return "", fmt.Errorf("%s has no \"## Prompt block\"", runbook)
	}
	_, fence, ok := strings.Cut(after, "```text\n")
	if !ok {
		return "", fmt.Errorf("%s: no ```text block after \"## Prompt block\"", runbook)
	}
	block, _, ok := strings.Cut(fence, "\n```")
	if !ok {
		return "", fmt.Errorf("%s: unterminated prompt block", runbook)
	}
	return fmt.Sprintf(`Task: open-plan row %s (docs/crucible/00-master-implementation-plan-open.md):

%s

Java research: if an "Oracle notes" section follows, use its file:line cites and read Java only for gaps; otherwise research first. Cite file:line for every behavior you port. Deliverables: module tests (package x_test), at most one scenario (add-scenario skill) and only if no module test can express it; port-log note docs/crucible/porting/port-log/game-state/m6-%s.md plus one index row in game-state.md. Do NOT run card-smoke: the main session measures the corpus. Any Forge bug: report file:line in your final report (PORT-8), do not work around. Do NOT edit open-plan rows, smoke-probe.md measurements or the CLAUDE.md percentage.

%s
`, id, text, strings.ToLower(id)+"-"+slug, block), nil
}
