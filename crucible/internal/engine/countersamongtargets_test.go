package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// castCounterSpell casts a one-G sorcery carrying sp (and svars) for p with
// c answering, then resolves the stack, returning ResolveStack's error.
func castCounterSpell(t *testing.T, g *engine.Game, p engine.PlayerID, c *engine.ScriptedController, sp string, svars ...string) error {
	t.Helper()
	spell := g.NewCard(spellDefWith(t, "Counter Spell", "Sorcery", "G", sp, svars...), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	if !g.CastSpell(p, spell, c) {
		t.Fatal("CastSpell failed")
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

func p1p1(g *engine.Game, id engine.CardID) int { return g.Card(id).Counters.Count(engine.P1P1) }

// TestPutCounterEachOfUpToTwoTargetsGetsCounterNum proves Gird for Battle's
// shape: TargetMin$ 0 | TargetMax$ 2, each chosen target gets CounterNum$.
func TestPutCounterEachOfUpToTwoTargetsGetsCounterNum(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.CardEntity(b)})
	if err := castCounterSpell(t, g, p, c, "SP$ PutCounter | ValidTgts$ Creature | TargetMin$ 0 | TargetMax$ 2 | CounterType$ P1P1 | CounterNum$ 2"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if p1p1(g, a) != 2 || p1p1(g, b) != 2 {
		t.Errorf("counters = %d/%d, want 2/2", p1p1(g, a), p1p1(g, b))
	}
}

// TestPutCounterUpToOneChoosingNoneStillRunsItsChain proves an "up to one"
// choice of no target puts nothing and the SubAbility$ still runs (Basri
// Ket, Bagel and Schmear).
func TestPutCounterUpToOneChoosingNoneStillRunsItsChain(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets(nil)
	err := castCounterSpell(t, g, p, c, "SP$ PutCounter | ValidTgts$ Creature | TargetMin$ 0 | TargetMax$ 1 | CounterType$ P1P1 | CounterNum$ 1 | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 2")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if p1p1(g, a) != 0 || g.Player(p).Life != 22 {
		t.Errorf("counters = %d, life = %d, want 0 and 22", p1p1(g, a), g.Player(p).Life)
	}
}

// TestPutCounterDefinedWinsOverItsTargets proves getDefinedEntitiesOrTargeted's
// definedFirst (SpellAbilityEffect.java:379): Drillworks Mole's "Defined$
// Self & Targeted" puts a counter on the host and on the target.
func TestPutCounterDefinedWinsOverItsTargets(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	other := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	def := etbPutCounterTriggerDefParams(t, "Test Mole", "Defined$ Self & Targeted | TargetMin$ 0 | TargetMax$ 1 | ValidTgts$ Creature.Other | CounterType$ P1P1", nil)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	host := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(other)})
	if !g.CastSpell(p, host, c) {
		t.Fatal("CastSpell failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if p1p1(g, host) != 1 || p1p1(g, other) != 1 {
		t.Errorf("counters host/target = %d/%d, want 1/1", p1p1(g, host), p1p1(g, other))
	}
}

// TestPutCounterDefinedPartFailsLoudly proves an unreadable " & " part of
// Defined$ is an error, not a part skipped.
func TestPutCounterDefinedPartFailsLoudly(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	_, err := castETBPutCounter(t, g, p, etbPutCounterTriggerDefParams(t, "Test Bad Part", "Defined$ Self & Nonsense | CounterType$ P1P1", nil))
	if err == nil || !strings.Contains(err.Error(), "Nonsense") {
		t.Fatalf("err = %v, want one naming the unreadable part", err)
	}
}

// TestPutCounterOneTargetPerOpponentReadsANamedTargetMax proves Hapatra, the
// Desert Fang's shape: TargetMax$ OneEach is an SVar (PlayerCountOpponents),
// TargetsForEachPlayer$ on, and the target gets CounterNum$ X counters.
func TestPutCounterOneTargetPerOpponentReadsANamedTargetMax(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "4", "4"), other, engine.Battlefield)
	def := etbPutCounterTriggerDefParams(t, "Test Fang",
		"ValidTgts$ Creature.OppCtrl | TargetsForEachPlayer$ True | TargetMin$ 0 | TargetMax$ OneEach | CounterType$ M1M1 | CounterNum$ 2",
		map[string]string{"OneEach": "PlayerCountOpponents$Amount"})
	g.Player(p).ManaPool.Add(mana.Green, 1)
	host := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	if !g.CastSpell(p, host, c) {
		t.Fatal("CastSpell failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(victim).Counters.Count(engine.M1M1); n != 2 {
		t.Errorf("M1M1 on the opponent's creature = %d, want 2", n)
	}
}

// TestPutCounterDividedAsksForTheSplit proves CR 601.2d: two targets for
// three counters is a real choice, announced as the targets are chosen, and
// each target gets its own share, CounterNum$ unread (CountersPutEffect.java:
// 460-461).
func TestPutCounterDividedAsksForTheSplit(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.CardEntity(b)})
	c.QueueDivision([]int{2, 1})
	if err := castCounterSpell(t, g, p, c, "SP$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | CounterNum$ 3 | TargetMin$ 1 | TargetMax$ 3 | DividedAsYouChoose$ 3"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if p1p1(g, a) != 2 || p1p1(g, b) != 1 {
		t.Errorf("counters = %d/%d, want 2/1", p1p1(g, a), p1p1(g, b))
	}
}

// TestPutCounterDividedForcedSplitsAskNothing proves the two forced answers
// (PlayerControllerHuman.java:2495-2500): one target gets the whole amount,
// as many targets as the amount get one each. No division is queued, so
// asking would panic the scripted controller.
func TestPutCounterDividedForcedSplitsAskNothing(t *testing.T) {
	t.Parallel()
	const line = "SP$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | CounterNum$ 3 | TargetMin$ 1 | TargetMax$ 3 | DividedAsYouChoose$ 3"

	g, p, _ := newTwoPlayerGame(t)
	one := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(one)})
	if err := castCounterSpell(t, g, p, c, line); err != nil {
		t.Fatalf("one target: %v", err)
	}
	if p1p1(g, one) != 3 {
		t.Errorf("one target: counters = %d, want 3", p1p1(g, one))
	}

	g, p, _ = newTwoPlayerGame(t)
	var three []engine.EntityID
	for range 3 {
		three = append(three, engine.CardEntity(g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)))
	}
	c = engine.NewScriptedController()
	c.QueueTargets(three)
	if err := castCounterSpell(t, g, p, c, line); err != nil {
		t.Fatalf("three targets: %v", err)
	}
	for _, e := range three {
		id, _ := e.AsCard()
		if p1p1(g, id) != 1 {
			t.Errorf("three targets: counters on %d = %d, want 1", id, p1p1(g, id))
		}
	}
}

// TestPutCounterDividedBadAnswersFailLoudly proves a division that is not
// one share of at least one per target adding up to the amount, and more
// targets than the amount, fail the spell when it resolves and place nothing
// (GO-7).
func TestPutCounterDividedBadAnswersFailLoudly(t *testing.T) {
	t.Parallel()
	const line = "SP$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | CounterNum$ 3 | TargetMin$ 1 | TargetMax$ 3 | DividedAsYouChoose$ 3"
	cases := []struct {
		name    string
		targets int
		shares  []int
		want    string
	}{
		{"short sum", 2, []int{1, 1}, "add up to 2, want 3"},
		{"zero share", 2, []int{3, 0}, "below one"},
		{"share count", 2, []int{3}, "1 shares for 2 targets"},
		{"too many targets", 4, nil, "4 targets cannot each get one of 3"},
	}
	for _, tc := range cases {
		g, p, _ := newTwoPlayerGame(t)
		var chosen []engine.EntityID
		for range tc.targets {
			chosen = append(chosen, engine.CardEntity(g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)))
		}
		c := engine.NewScriptedController()
		c.QueueTargets(chosen)
		if tc.shares != nil {
			c.QueueDivision(tc.shares)
		}
		err := castCounterSpell(t, g, p, c, line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want %q", tc.name, err, tc.want)
		}
		for _, e := range chosen {
			id, _ := e.AsCard()
			if p1p1(g, id) != 0 {
				t.Errorf("%s: a counter was placed on %d", tc.name, id)
			}
		}
	}
}

// TestPutCounterDividedDroppedTargetLosesItsShare proves CR 608.2b over a
// division: a target gone before resolution takes its share with it, and the
// one left keeps its own share rather than the whole amount.
func TestPutCounterDividedDroppedTargetLosesItsShare(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	spell := g.NewCard(spellDefWith(t, "Elven Rite", "Sorcery", "G",
		"SP$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | CounterNum$ 3 | TargetMin$ 1 | TargetMax$ 3 | DividedAsYouChoose$ 3"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.CardEntity(b)})
	c.QueueDivision([]int{2, 1})
	g.Player(p).ManaPool.Add(mana.Green, 1)
	if !g.CastSpell(p, spell, c) {
		t.Fatal("CastSpell failed")
	}
	g.Move(a, engine.Graveyard, p)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if p1p1(g, b) != 1 {
		t.Errorf("counters on the target left = %d, want its own share 1", p1p1(g, b))
	}
}

// TestPutCounterDividedLinkCarriesItsOwnDivision proves a SubAbility$ link
// naming its own ValidTgts$ and DividedAsYouChoose$ announces its division
// with its targets (resolveChainTargets) and resolves with it.
func TestPutCounterDividedLinkCarriesItsOwnDivision(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.CardEntity(b)})
	c.QueueDivision([]int{1, 3})
	err := castCounterSpell(t, g, p, c, "SP$ GainLife | Defined$ You | LifeAmount$ 1 | SubAbility$ DBPut",
		"DBPut", "DB$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | TargetMin$ 1 | TargetMax$ 2 | DividedAsYouChoose$ 4")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if p1p1(g, a) != 1 || p1p1(g, b) != 3 || g.Player(p).Life != 21 {
		t.Errorf("counters = %d/%d, life = %d, want 1/3 and 21", p1p1(g, a), p1p1(g, b), g.Player(p).Life)
	}
}

// TestPutCounterDividedLinkBadAnswerFailsLoudly proves a link's bad division
// fails the spell rather than the link resolving over its parent's targets.
func TestPutCounterDividedLinkBadAnswerFailsLoudly(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.CardEntity(b)})
	c.QueueDivision([]int{1, 1})
	err := castCounterSpell(t, g, p, c, "SP$ PutCounter | Defined$ You | CounterType$ ENERGY | SubAbility$ DBPut",
		"DBPut", "DB$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | TargetMin$ 1 | TargetMax$ 2 | DividedAsYouChoose$ 4")
	if err == nil || !strings.Contains(err.Error(), "add up to 2, want 4") {
		t.Fatalf("err = %v, want the link's bad division", err)
	}
	if p1p1(g, a) != 0 || p1p1(g, b) != 0 {
		t.Errorf("counters = %d/%d, want none", p1p1(g, a), p1p1(g, b))
	}
}

// TestPutCounterDividedLinkFailsUnderARootThatIgnoresIt proves a link's bad
// division fails the link itself when its root (GainLife) never reads the
// recorded error: the link resolves over its own targets, none with a share.
func TestPutCounterDividedLinkFailsUnderARootThatIgnoresIt(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.CardEntity(b)})
	c.QueueDivision([]int{1, 1})
	err := castCounterSpell(t, g, p, c, "SP$ GainLife | Defined$ You | LifeAmount$ 1 | SubAbility$ DBPut",
		"DBPut", "DB$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | TargetMin$ 1 | TargetMax$ 2 | DividedAsYouChoose$ 4")
	if err == nil || !strings.Contains(err.Error(), "add up to 2, want 4") {
		t.Fatalf("err = %v, want the link's bad division", err)
	}
	if p1p1(g, a) != 0 || p1p1(g, b) != 0 {
		t.Errorf("counters = %d/%d, want none", p1p1(g, a), p1p1(g, b))
	}
}

// TestPutCounterDividedLinkItCannotAskFailsUnderARootThatIgnoresIt proves a
// link whose targets could not be asked for at all (DividedUpTo$, an
// unreadable total) fails on its own error rather than resolving over its
// parent's targets, none, silently.
func TestPutCounterDividedLinkItCannotAskFailsUnderARootThatIgnoresIt(t *testing.T) {
	t.Parallel()
	for _, extra := range []string{"DividedAsYouChoose$ Nope", "DividedAsYouChoose$ 2 | DividedUpTo$ True"} {
		g, p, _ := newTwoPlayerGame(t)
		a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
		err := castCounterSpell(t, g, p, engine.NewScriptedController(), "SP$ GainLife | Defined$ You | LifeAmount$ 1 | SubAbility$ DBPut",
			"DBPut", "DB$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | TargetMin$ 1 | TargetMax$ 2 | "+extra)
		if err == nil || !strings.Contains(err.Error(), "PutCounter") {
			t.Errorf("%s: err = %v, want the link's own error", extra, err)
		}
		if p1p1(g, a) != 0 {
			t.Errorf("%s: a counter was placed", extra)
		}
	}
}

// TestPutCounterDividedWithNoAnnouncedDivisionFailsLoudly proves an ability
// pushed with targets but no division (no targeting step ran) fails rather
// than putting CounterNum$ on each target.
func TestPutCounterDividedWithNoAnnouncedDivisionFailsLoudly(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(a)},
		"DB$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | CounterNum$ 2 | TargetMin$ 1 | TargetMax$ 2 | DividedAsYouChoose$ 2")
	if err == nil || !strings.Contains(err.Error(), "no division was announced") {
		t.Fatalf("err = %v, want the missing division", err)
	}
	if p1p1(g, a) != 0 {
		t.Errorf("counters = %d, want 0", p1p1(g, a))
	}
}

// TestPutCounterDividedTotalOfZeroAsksForNoTarget proves a DividedAsYouChoose$
// total of 0 asks for no target (TargetRestrictions.isMinTargetsChosen,
// TargetRestrictions.java:370-376) and puts nothing: no target is queued, so
// asking would panic the scripted controller.
func TestPutCounterDividedTotalOfZeroAsksForNoTarget(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	err := castCounterSpell(t, g, p, engine.NewScriptedController(),
		"SP$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | CounterNum$ X | TargetMin$ 1 | TargetMax$ X | DividedAsYouChoose$ X",
		"X", "Count$Valid Card.Artifact")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if p1p1(g, a) != 0 {
		t.Errorf("counters = %d, want 0", p1p1(g, a))
	}
}

// TestPutCounterDividedShapesItCannotReadFailLoudly proves the divided shapes
// this port does not resolve are errors: DividedUpTo$ (a further "how much"
// question), an unreadable total, and a division with no targets of its own
// (Defined$, or no ValidTgts$ at all).
func TestPutCounterDividedShapesItCannotReadFailLoudly(t *testing.T) {
	t.Parallel()
	cases := []struct {
		name, line, want string
		target           bool
	}{
		{"up to", "SP$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | TargetMin$ 1 | TargetMax$ 2 | DividedAsYouChoose$ 2 | DividedUpTo$ True", "DividedUpTo$", false},
		{"unreadable total", "SP$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | TargetMin$ 1 | TargetMax$ 2 | DividedAsYouChoose$ Nope", "Nope", false},
		{"defined", "SP$ PutCounter | ValidTgts$ Creature | Defined$ Targeted | CounterType$ P1P1 | TargetMin$ 1 | TargetMax$ 2 | DividedAsYouChoose$ 2", "without its own targets", true},
		{"untargeted", "SP$ PutCounter | Defined$ You | CounterType$ ENERGY | DividedAsYouChoose$ 2", "without its own targets", false},
	}
	for _, tc := range cases {
		g, p, _ := newTwoPlayerGame(t)
		a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
		c := engine.NewScriptedController()
		if tc.target {
			c.QueueTargets([]engine.EntityID{engine.CardEntity(a)})
		}
		err := castCounterSpell(t, g, p, c, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want %q", tc.name, err, tc.want)
		}
		if p1p1(g, a) != 0 {
			t.Errorf("%s: a counter was placed", tc.name)
		}
	}
}

// TestCopyOfADividedSpellCannotTakeNewTargets proves CR 707.10c's new
// targets for a divided copy, which keep the original's division values, fail
// loudly rather than reusing a division announced over other targets.
func TestCopyOfADividedSpellCannotTakeNewTargets(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	spell := g.NewCard(spellDefWith(t, "Elven Rite", "Sorcery", "W",
		"SP$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | TargetMin$ 1 | TargetMax$ 2 | DividedAsYouChoose$ 2"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a)})
	c.QueueConfirmEffect(true)
	err := castWithCopyWatcher(t, g, p, spell, c, "Sorcery", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | MayChooseTarget$ True")
	if err == nil || !strings.Contains(err.Error(), "DividedAsYouChoose$") {
		t.Fatalf("err = %v, want the divided copy refused", err)
	}
}

// TestCopyOfADividedSpellKeepingItsTargetsKeepsItsDivision proves CR 707.10:
// a copy that keeps the original's targets keeps their division too, so the
// copy and the original each put their own share.
func TestCopyOfADividedSpellKeepingItsTargetsKeepsItsDivision(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	spell := g.NewCard(spellDefWith(t, "Defend", "Sorcery", "W",
		"SP$ PutCounter | ValidTgts$ Creature | CounterType$ P1P1 | TargetMin$ 1 | TargetMax$ 3 | DividedAsYouChoose$ 3"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.CardEntity(b)})
	c.QueueDivision([]int{2, 1})
	c.QueueConfirmEffect(false)
	if err := castWithCopyWatcher(t, g, p, spell, c, "Sorcery", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | MayChooseTarget$ True"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if p1p1(g, a) != 4 || p1p1(g, b) != 2 {
		t.Errorf("counters = %d/%d, want 4/2 (the copy's 2/1 plus the original's)", p1p1(g, a), p1p1(g, b))
	}
}
