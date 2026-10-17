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
	// Placed means the card is a Command-zone type (a plane, a scheme, a
	// Vanguard avatar, a conspiracy, a phenomenon) with no spell to cast: it
	// was put where the game keeps it and three turns ran with its statics and
	// triggers live. The abilities that need the game mode (set in motion,
	// chaos, planeswalk) are not exercised, so it is counted apart from Plays.
	Placed
)

func (o Outcome) String() string {
	return [...]string{"plays", "refused", "panicked", "inconclusive", "hung", "placed"}[o]
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
	tailor   tailor
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
	bolt  = "Lightning Bolt"
)

// richBoard is what the richer variant adds for each player, by card name, so
// that a spell asking for an artifact, an enchantment, a flier, a planeswalker,
// a creature card in a graveyard or six cards to exile finds one. A name the
// database lacks is skipped.
var richBoard = struct{ battlefield, graveyard, exile, hand []string }{
	battlefield: []string{"Serra Angel", "Glorious Anthem", "Sol Ring", "Bonesplitter", "Jace Beleren", "Island", "Mountain", "Plains", "Swamp", "Grizzly Bears"},
	graveyard:   []string{"Lightning Bolt", "Divination", "Sol Ring", "Glorious Anthem", "Serra Angel", "Island", "Grizzly Bears"},
	exile:       []string{"Grizzly Bears"},
	hand:        []string{"Forest", "Forest", "Grizzly Bears"},
}

// variant is one way to set up the board for a probe.
type variant struct {
	// rich adds richBoard to both sides.
	rich bool
	// opposing is a spell left on the stack ("" for none).
	opposing string
	// combat sends the probe player's creatures into an attack that the
	// opponent blocks, and casts the card in the declare blockers step.
	combat bool
	// tailor adds, for each ValidTgts$ the card names, a corpus card that
	// accepts it (tailor.go).
	tailor bool
	// timed plays the game, through the turn's own steps, up to at before the
	// card is put in hand and cast (timing.go).
	timed bool
	at    moment
	// blocks makes the defender of a timed walk block with every creature it
	// can (Controller.blockEvery).
	blocks bool
	// stateTailor adds, for each target spec naming game state, a corpus
	// card the spec accepts with that state stripped (staged.go).
	stateTailor bool
	// stage is game state played into being before the card is cast
	// (staged.go).
	stage staging
}

// variants is tried in order for a card the previous ones could not cast: the
// plain board, a spell on the stack, the rich board, then combat. A card whose
// spell carries a timing restriction is then tried at each moment of a turn
// (timedVariants, timing.go), and any card still not cast on the staged
// boards, whose game state is played into being (stagedVariants, staged.go).
var variants = [...]variant{
	{},
	{opposing: bolt},
	{opposing: bears},
	{rich: true},
	{rich: true, opposing: bolt},
	{rich: true, opposing: bears},
	{rich: true, combat: true},
	{tailor: true},
	{rich: true, tailor: true},
}

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
// gives up on it can say where it was stuck. A card that cannot be cast on the
// plain board (a counterspell has nothing to counter, a Reanimate no creature
// card to return, an instant no attacker to target) is retried on each of the
// other variants until one casts it.
func (p *Prober) probe(name string, where *atomic.Value) Result {
	res := p.run(name, where, variants[0])
	rest := append([]variant(nil), variants[1:]...)
	if def, ok := p.db.Card(name); ok {
		rest = append(rest, timedVariants(def)...)
	}
	rest = append(rest, stagedVariants[:]...)
	for _, v := range rest {
		if res.Outcome != Inconclusive {
			break
		}
		if again := p.run(name, where, v); again.Outcome != Inconclusive {
			return again
		}
	}
	return res
}

// run is one probe on the board v describes.
func (p *Prober) run(name string, where *atomic.Value, v variant) (res Result) {
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
		if v.rich {
			p.addRich(g, who)
		}
	}
	if v.tailor {
		p.addTailored(g, pid, opp, def)
	}
	if v.stateTailor {
		p.addStateTailored(g, pid, opp, def)
	}
	fill := func() {
		pool := &g.Player(pid).ManaPool
		for _, c := range [...]mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red, mana.Green} {
			pool.Add(c, 20)
		}
		pool.AddColorless(20)
	}
	fill()

	ctl := &Controller{}
	if v.opposing != "" {
		spellDef, ok := p.db.Card(v.opposing)
		if !ok {
			res.Outcome, res.Detail = Inconclusive, "no spell to put on the stack"
			return res
		}
		// The opponent can only cast an instant on the probe's turn; a
		// creature spell is the probe player's own.
		caster := opp
		if v.opposing == bears {
			caster = pid
		}
		g.Player(caster).ManaPool.Add(mana.Red, 2)
		g.Player(caster).ManaPool.Add(mana.Green, 2)
		if !g.CastSpell(caster, g.NewCard(spellDef, caster, engine.Hand), ctl) {
			res.Outcome, res.Detail = Inconclusive, "the opposing spell could not be cast"
			return res
		}
	}
	if v.timed {
		ctl.blockEvery = v.blocks
		if err := p.walk(g, ctl, pid, opp, v.at); err != nil {
			res.Outcome, res.Detail = Inconclusive, err.Error()
			return res
		}
		fill()
	}
	if v.stage != stageNone {
		if err := p.playStage(g, ctl, v.stage); err != nil {
			res.Outcome, res.Detail = Inconclusive, err.Error()
			return res
		}
		fill()
	}
	if v.tailor {
		p.stackTailored(g, pid, opp, def, ctl)
	}
	if v.combat && !p.startCombat(g, ctl, pid) {
		res.Outcome, res.Detail = Inconclusive, "no combat could be set up"
		return res
	}
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

	face := def.Faces[0].Type
	if face.Has(cardtype.Stickers) {
		res.Outcome, res.Stage = Refused, "setup"
		res.Detail = "engine: sticker sheets are not ported (open plan R15)"
		return res
	}
	if isCommandType(face) {
		stage = "turns"
		where.Store(stage)
		id := g.NewCard(def, pid, engine.Command)
		if face.Has(cardtype.Plane) {
			g.SetActivePlane(id)
		}
		if err := settle(g.Run(p.registry, ctl, 3)); err != nil {
			return fail(Refused, stage, err)
		}
		res.Outcome = Placed
		return res
	}
	card := g.NewCard(def, pid, engine.Hand)

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

// isCommandType reports whether the card lives in the Command zone and has no
// spell to cast: a plane, phenomenon, scheme, Vanguard avatar or conspiracy.
func isCommandType(t cardtype.Line) bool {
	for _, c := range [...]cardtype.CoreType{cardtype.Plane, cardtype.Phenomenon, cardtype.Scheme, cardtype.Vanguard, cardtype.Conspiracy} {
		if t.Has(c) {
			return true
		}
	}
	return false
}

// addRich puts richBoard on who's side.
func (p *Prober) addRich(g *engine.Game, who engine.PlayerID) {
	add := func(names []string, zone engine.ZoneType) {
		for _, n := range names {
			if def, ok := p.db.Card(n); ok {
				g.NewCard(def, who, zone)
			}
		}
	}
	add(richBoard.battlefield, engine.Battlefield)
	// A tapped creature, for "target tapped creature".
	if def, ok := p.db.Card(bears); ok {
		g.Card(g.NewCard(def, who, engine.Battlefield)).Tapped = true
	}
	add(richBoard.graveyard, engine.Graveyard)
	add(richBoard.exile, engine.Exile)
	add(richBoard.hand, engine.Hand)
}

// startCombat declares pid's creatures as attackers and the opponent's as
// blockers (the Controller blocks when it is the defender), leaving the game in
// the declare blockers step with priority, so that "target attacking creature"
// and "target blocking creature" have a target. It reports whether any attacker
// was declared.
func (p *Prober) startCombat(g *engine.Game, ctl *Controller, pid engine.PlayerID) bool {
	ctl.blockAll = true
	g.SetTurnState(1, pid, engine.DeclareAttackers)
	attackers, err := g.DeclareCombatAttackers(ctl)
	if err != nil || len(attackers) == 0 {
		return false
	}
	g.SetTurnState(1, pid, engine.DeclareBlockers)
	_, err = g.DeclareCombatBlockers(ctl)
	return err == nil
}
