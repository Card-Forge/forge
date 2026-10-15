package main

import (
	"errors"
	"fmt"
	"regexp"
	"strconv"
	"strings"
)

// Result is one card-smoke summary.
type Result struct {
	Plays, Refused, Panicked, Hung, Placed, Inconclusive int
	Judged                                               int
	Pct                                                  string // one decimal, "91.4"
	Decks, Cards                                         int    // -decks runs only
	Causes                                               []Cause
}

var (
	countRE = regexp.MustCompile(`(?m)^\s*(plays|refused|panicked|hung|placed|inconclusive)\s+(\d+)`)
	pctRE   = regexp.MustCompile(`(\d+\.\d)% of the (\d+) judged`)
	decksRE = regexp.MustCompile(`smoke probe of (\d+) decklists.*: (\d+) cards`)
)

// Cause is one line of the refusal table: cards blocked and the error text.
type Cause struct {
	Cards int
	Text  string
}

var causeRE = regexp.MustCompile(`(?m)^\s+(\d+)  (\S.*)$`)

// ParseSmoke reads the summary block `crucible card-smoke` prints.
func ParseSmoke(out string) (Result, error) {
	var r Result
	slot := map[string]*int{
		"plays": &r.Plays, "refused": &r.Refused, "panicked": &r.Panicked,
		"hung": &r.Hung, "placed": &r.Placed, "inconclusive": &r.Inconclusive,
	}
	seen := 0
	for _, m := range countRE.FindAllStringSubmatch(out, -1) {
		n, _ := strconv.Atoi(m[2])
		*slot[m[1]] = n
		seen++
	}
	if seen != len(slot) {
		return Result{}, fmt.Errorf("smoke output has %d of %d outcome lines", seen, len(slot))
	}
	p := pctRE.FindStringSubmatch(out)
	if p == nil {
		return Result{}, errors.New("smoke output has no \"N% of the M judged\" line")
	}
	r.Pct = p[1]
	r.Judged, _ = strconv.Atoi(p[2])
	if d := decksRE.FindStringSubmatch(out); d != nil {
		r.Decks, _ = strconv.Atoi(d[1])
		r.Cards, _ = strconv.Atoi(d[2])
	}
	if _, tbl, ok := strings.Cut(out, "refusal causes"); ok {
		for _, m := range causeRE.FindAllStringSubmatch(tbl, -1) {
			n, _ := strconv.Atoi(m[1])
			r.Causes = append(r.Causes, Cause{n, m[2]})
		}
	}
	return r, nil
}

// commas formats 30618 as "30,618".
func commas(n int) string {
	s := strconv.Itoa(n)
	for i := len(s) - 3; i > 0; i -= 3 {
		s = s[:i] + "," + s[i:]
	}
	return s
}
