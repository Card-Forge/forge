package smoke

import (
	"fmt"
	"regexp"
	"runtime/debug"
	"strings"
	"sync/atomic"
	"time"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// Outcome is how a probe ended.
type Outcome uint8

const (
	// Plays means the card was cast or played, resolved, its abilities activated
	// and two turns ran, with no error.
	Plays Outcome = iota
	// Refused means the engine returned an error or left a pending one. The
	// loud half of GO-7, and the M6 backlog.
	Refused
	// Panicked means an engine invariant breach, recovered at the probe boundary.
	// Always a bug.
	Panicked
	// Inconclusive means the card could not be cast or played in the fixed
	// board (a cost the board cannot pay, a restriction). Not counted for
	// or against support.
	Inconclusive
	// Hung means the probe did not return within the time limit, so the engine
	// loops on this card. Always a bug.
	Hung
)

func (o Outcome) String() string {
	return [...]string{"plays", "refused", "panicked", "inconclusive", "hung"}[o]
}

// Result is one card's probe.
type Result struct {
	Card    string
	Outcome Outcome
	// Detail is the first error or panic message, as the engine wrote it.
	Detail string
	// Stage names where it happened: "cast", "resolve", "activate N", "turns".
	Stage string
}

// Prober probes cards against one shared, immutable card database.
type Prober struct {
	db       *compile.DB
	registry *engine.Registry
}

// NewProber returns a Prober over db, which must also hold the token
// scripts (compile.DB.WithTokens) for Token effects to resolve.
func NewProber(db *compile.DB) *Prober {
	return &Prober{db: db, registry: engine.NewRegistry()}
}

// filler is a vanilla card the board is built from. Every one is a basic land
// or a bear the corpus has had since the beginning.
const (
	bears = "Grizzly Bears"
	land  = "Forest"
)

// ProbeWithin is Probe with a time limit. A probe that outlives it is reported
// Hung and its goroutine is left running: the engine has no cancellation (a
// loop in it is the bug being reported), so the caller should expect the
// process to carry the leaked goroutines until it exits.
func (p *Prober) ProbeWithin(name string, limit time.Duration) Result {
	var where atomic.Value
	where.Store("setup")
	done := make(chan Result, 1)
	go func() { done <- p.probe(name, &where) }()
	select {
	case r := <-done:
		return r
	case <-time.After(limit):
		return Result{Card: name, Outcome: Hung, Stage: where.Load().(string), Detail: fmt.Sprintf("no result after %s", limit)}
	}
}

// Probe runs one card through the fixed board. It never panics: an engine
// invariant breach is the Panicked outcome (GO-7's game boundary, in
// miniature).
func (p *Prober) Probe(name string) Result {
	var where atomic.Value
	where.Store("setup")
	return p.probe(name, &where)
}

// probe is Probe, publishing the stage it is in to where, so a caller that
// gives up on it can say where it was stuck.
func (p *Prober) probe(name string, where *atomic.Value) (res Result) {
	res = Result{Card: name}
	stage := "setup"
	defer func() {
		if r := recover(); r != nil {
			res.Outcome, res.Stage = Panicked, stage
			res.Detail = fmt.Sprintf("%v\n%s", r, firstFrames(debug.Stack(), 6))
		}
	}()

	def, ok := p.db.Card(name)
	if !ok {
		res.Outcome, res.Detail = Refused, "no such card in the database"
		return res
	}
	bearsDef, ok1 := p.db.Card(bears)
	landDef, ok2 := p.db.Card(land)
	if !ok1 || !ok2 {
		res.Outcome, res.Detail = Refused, "the filler cards are missing from the database"
		return res
	}

	g := engine.NewGame(p.db, javarand.New(1), []string{"a", "b"})
	pid, opp := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, pid, engine.Main1)
	for _, who := range []engine.PlayerID{pid, opp} {
		g.Player(who).Life = 20
		for range 10 {
			g.NewCard(landDef, who, engine.Library)
		}
		g.NewCard(bearsDef, who, engine.Battlefield)
		g.NewCard(landDef, who, engine.Battlefield)
		g.NewCard(bearsDef, who, engine.Graveyard)
	}
	fill := func() {
		pool := &g.Player(pid).ManaPool
		for _, c := range [...]mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red, mana.Green} {
			pool.Add(c, 10)
		}
		pool.AddColorless(10)
	}
	fill()

	ctl := &Controller{}
	card := g.NewCard(def, pid, engine.Hand)
	fail := func(o Outcome, st string, err error) Result {
		res.Outcome, res.Stage, res.Detail = o, st, err.Error()
		return res
	}
	// settle returns the first error the engine left: a returned one, or a
	// pending one a refused path recorded instead.
	settle := func(err error) error {
		if err != nil {
			return err
		}
		return g.TakePendingError()
	}

	stage = "cast"
	where.Store(stage)
	var started bool
	if g.Card(card).Type().Has(cardtype.Land) {
		started = g.PlayLand(pid, card, ctl)
	} else {
		started = g.CastSpell(pid, card, ctl)
	}
	if !started {
		if err := g.TakePendingError(); err != nil {
			return fail(Refused, stage, err)
		}
		res.Outcome, res.Stage = Inconclusive, stage
		res.Detail = "could not be cast or played on the fixed board"
		return res
	}

	stage = "resolve"
	where.Store(stage)
	if err := settle(g.ResolveStack(p.registry, ctl)); err != nil {
		return fail(Refused, stage, err)
	}

	for i, ab := range def.Faces[0].Abilities {
		if ab.Record != compile.Activated {
			continue
		}
		stage = fmt.Sprintf("activate %d (%s)", i, ab.Name)
		where.Store(stage)
		fill()
		if !g.ActivateAbility(pid, card, i, ctl) {
			if err := g.TakePendingError(); err != nil {
				return fail(Refused, stage, err)
			}
			continue
		}
		if err := settle(g.ResolveStack(p.registry, ctl)); err != nil {
			return fail(Refused, stage, err)
		}
	}

	stage = "turns"
	where.Store(stage)
	if err := settle(g.Run(p.registry, ctl, 3)); err != nil {
		return fail(Refused, stage, err)
	}
	res.Outcome = Plays
	return res
}

// firstFrames keeps the top n frames of a stack, enough to name the engine
// function that broke an invariant without a page of runtime frames.
func firstFrames(stack []byte, n int) string {
	lines := strings.Split(string(stack), "\n")
	if len(lines) > 1+2*n {
		lines = lines[:1+2*n]
	}
	return strings.Join(lines, "\n")
}

var (
	cardName = regexp.MustCompile(`"[^"]*"`)
	digits   = regexp.MustCompile(`\b\d+\b`)
	ids      = regexp.MustCompile(`\b[A-Za-z]+#\d+\b`)
)

// Reason reduces a refusal to the cause it shares across cards: quoted names,
// numbers and ids are blanked, so "Defined$ \"X\" not resolvable yet" for two
// cards counts once.
func Reason(detail string) string {
	first, _, _ := strings.Cut(detail, "\n")
	first = cardName.ReplaceAllString(first, `"_"`)
	first = ids.ReplaceAllString(first, "_")
	first = digits.ReplaceAllString(first, "N")
	return strings.TrimSpace(first)
}
