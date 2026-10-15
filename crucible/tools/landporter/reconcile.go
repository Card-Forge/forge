package main

import (
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
)

const (
	smokeDoc = "docs/crucible/porting/port-log/smoke-probe.md"
	claudeMD = "CLAUDE.md"
)

// proseDocs quote the whole-corpus percentage in prose.
var proseDocs = []string{
	claudeMD,
	"docs/crucible/00-master-implementation-plan.md",
	"docs/crucible/00-master-implementation-plan-in-progress.md",
	"docs/crucible/00-master-implementation-plan-open.md",
}

var (
	// "91.4% of the 33,513 judged", "91.4% of 33,513\njudged", "86.0% of the\ncorpus's judged"
	proseRE = regexp.MustCompile(`(\d+\.\d)%(\s+of\s+(?:the\s+)?)(?:([\d,]+)(\s+))?(judged|corpus's\s+judged)`)
	rowRE   = regexp.MustCompile(`(?m)^(\| (plays|refused|panicked|hung|placed|inconclusive)\s+\|\s*)([\d,]+)( \|)`)
	allRE   = regexp.MustCompile(`(\d+\.\d)%( of the )([\d,]+)( judged cards play\.)`)
	preCard = regexp.MustCompile(`(On the )([\d,]+)(\s+distinct\s+cards\s+of\s+the\s+)(\d+)(\s+quest)`)
	prePct  = regexp.MustCompile(`(it\s+is\s+)(\d+\.\d)%(\s+of\s+)([\d,]+)(\s+judged)`)
)

// Reconcile rewrites every quoted measurement from all (the whole corpus) and
// precon (the quest decks). A pattern that matches nowhere is an error: the doc
// drifted from what this tool expects, and a silent skip would leave a stale number.
// It returns the files it changed and the smoke-probe table's old counts.
func Reconcile(root string, all, precon Result) (changed []string, before map[string]int, err error) {
	before = map[string]int{}
	edit := func(rel string, f func(string) (string, error)) error {
		p := filepath.Join(root, rel)
		b, err := os.ReadFile(p)
		if err != nil {
			return err
		}
		out, err := f(string(b))
		if err != nil {
			return fmt.Errorf("%s: %w", rel, err)
		}
		if out == string(b) {
			return nil
		}
		changed = append(changed, rel)
		return os.WriteFile(p, []byte(out), 0o644)
	}

	counts := map[string]int{
		"plays": all.Plays, "refused": all.Refused, "panicked": all.Panicked,
		"hung": all.Hung, "placed": all.Placed, "inconclusive": all.Inconclusive,
	}
	err = edit(smokeDoc, func(s string) (string, error) {
		for name, re := range map[string]*regexp.Regexp{
			"results table": rowRE, "corpus percent sentence": allRE,
			"precon card sentence": preCard, "precon percent sentence": prePct,
		} {
			if !re.MatchString(s) {
				return "", fmt.Errorf("%s not found", name)
			}
		}
		s = rowRE.ReplaceAllStringFunc(s, func(m string) string {
			g := rowRE.FindStringSubmatch(m)
			before[g[2]], _ = strconv.Atoi(strings.ReplaceAll(g[3], ",", ""))
			return g[1] + commas(counts[g[2]]) + g[4]
		})
		s = allRE.ReplaceAllString(s, all.Pct+"%${2}"+commas(all.Judged)+"${4}")
		s = preCard.ReplaceAllString(s, "${1}"+commas(precon.Cards)+"${3}"+strconv.Itoa(precon.Decks)+"${5}")
		s = prePct.ReplaceAllString(s, "${1}"+precon.Pct+"%${3}"+commas(precon.Judged)+"${5}")
		return s, nil
	})
	if err != nil {
		return nil, nil, err
	}

	for _, rel := range proseDocs {
		err = edit(rel, func(s string) (string, error) {
			if !proseRE.MatchString(s) {
				return "", fmt.Errorf("no smoke percentage found")
			}
			return proseRE.ReplaceAllStringFunc(s, func(m string) string {
				g := proseRE.FindStringSubmatch(m)
				out := all.Pct + "%" + g[2]
				if g[3] != "" {
					out += commas(all.Judged) + g[4]
				}
				return out + g[5]
			}), nil
		})
		if err != nil {
			return nil, nil, err
		}
	}
	return changed, before, nil
}

// Table renders the before/after counts of the smoke-probe table.
func Table(before map[string]int, all, precon Result) string {
	var b strings.Builder
	b.WriteString("| | before | after |\n|---|---:|---:|\n")
	for _, row := range []struct {
		name string
		now  int
	}{{"plays", all.Plays}, {"refused", all.Refused}, {"placed", all.Placed}, {"inconclusive", all.Inconclusive}} {
		fmt.Fprintf(&b, "| %s | %s | %s |\n", row.name, commas(before[row.name]), commas(row.now))
	}
	fmt.Fprintf(&b, "judged play: %s%% of %s; precons %s%% of %s\n",
		all.Pct, commas(all.Judged), precon.Pct, commas(precon.Judged))
	return b.String()
}
