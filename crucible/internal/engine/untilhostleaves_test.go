package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// This file covers ChangeZone's Duration$ UntilHostLeavesPlay: the
// Banishing Light / Banisher Priest "exile until this leaves the
// battlefield" shape (changeZoneUntilCommand, SpellAbilityEffect.java:833-915).

const oRingLine = "DB$ ChangeZone | ValidTgts$ Creature.OppCtrl | Origin$ Battlefield | Destination$ Exile | Duration$ UntilHostLeavesPlay"

// pushLineFrom pushes trig (and its SubAbility$ svars) as p's ability with
// source as its host, then resolves the stack -- for a host that is not the
// card the line was compiled on, or not on the battlefield at all.
func pushLineFrom(t *testing.T, g *engine.Game, p engine.PlayerID, source engine.CardID, c engine.PlayerController, targets []engine.EntityID, trig string, svars ...string) error {
	t.Helper()
	def := etbChainDef(t, "Test Pushed", trig, svars...)
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		api, ok := engine.APIByName(sub.Ability.Name)
		if !ok {
			t.Fatalf("unknown API %q", sub.Ability.Name)
		}
		g.PushAbility(engine.Ability{API: api, Source: source, Controller: p, Params: sub.Ability, Amounts: face.Amounts, Targets: targets})
		return g.ResolveStack(engine.NewRegistry(), c)
	}
	t.Fatal("no Execute$")
	return nil
}

// exileUntilHostLeaves casts an O-Ring creature for p whose enter trigger
// exiles victim, and checks the exile happened.
func exileUntilHostLeaves(t *testing.T, g *engine.Game, p engine.PlayerID, victim engine.CardID) engine.CardID {
	t.Helper()
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	host, err := castETBChain(t, g, p, etbChainDef(t, "Test Banisher", oRingLine), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(victim).Zone; z != engine.Exile {
		t.Fatalf("victim zone = %v, want Exile", z)
	}
	if g.Card(victim).ExiledWith() != host {
		t.Fatalf("victim exiled with %v, want the host %v", g.Card(victim).ExiledWith(), host)
	}
	return host
}

// TestExileUntilHostLeavesReturnsAtStateBasedCheck proves the dominant
// shape: the exiled creature comes back under its owner's control, a new
// summoning-sick object, once the host leaves -- here by a bare zone change
// with no controller at hand, carried out by the next state-based pass.
func TestExileUntilHostLeavesReturnsAtStateBasedCheck(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	g.Card(victim).Tapped = true
	host := exileUntilHostLeaves(t, g, p, victim)

	g.Move(host, engine.Graveyard, p)
	if z := g.Card(victim).Zone; z != engine.Exile {
		t.Fatalf("victim zone before the state-based check = %v, want Exile", z)
	}
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	v := g.Card(victim)
	if v.Zone != engine.Battlefield || v.Controller() != other || v.Tapped || !v.SummonSick {
		t.Errorf("returned: zone %v controller %v tapped %v sick %v, want Battlefield %v false true",
			v.Zone, v.Controller(), v.Tapped, v.SummonSick, other)
	}
}

// TestExileUntilHostLeavesReturnsInsideTheSameEffect proves a host moved by
// an effect brings the card back before that effect's next step: the
// SubAbility$ bounce after the host's own bounce finds it on the battlefield.
func TestExileUntilHostLeavesReturnsInsideTheSameEffect(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	host := exileUntilHostLeaves(t, g, p, victim)

	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(host)},
		"DB$ ChangeZone | ValidTgts$ Creature | Origin$ Battlefield | Destination$ Hand | SubAbility$ DBAll",
		"DBAll", "DB$ ChangeZoneAll | ChangeType$ Creature.OppCtrl | Origin$ Battlefield | Destination$ Hand"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(host).Zone; z != engine.Hand {
		t.Errorf("host zone = %v, want Hand", z)
	}
	if z, o := g.Card(victim).Zone, g.Card(victim).ZoneOwner; z != engine.Hand || o != other {
		t.Errorf("victim = %v of %v, want back on the battlefield in time to be bounced to %v's Hand", z, o, other)
	}
}

// TestExileUntilHostLeavesDoesNothingOnceTheHostIsGone proves
// checkValidDuration: a host already off the battlefield when the ability
// resolves exiles nothing at all.
func TestExileUntilHostLeavesDoesNothingOnceTheHostIsGone(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
	if err := pushLineFrom(t, g, p, host, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)}, oRingLine); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(victim).Zone; z != engine.Battlefield {
		t.Errorf("victim zone = %v, want Battlefield", z)
	}
}

// TestExileUntilHostLeavesSkipsACardThatMovedOn proves the
// equalsWithGameTimestamp check: a card that left exile some other way is
// a new object and stays where it went.
func TestExileUntilHostLeavesSkipsACardThatMovedOn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	host := exileUntilHostLeaves(t, g, p, victim)

	g.Move(victim, engine.Graveyard, other)
	g.Move(host, engine.Graveyard, p)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if z := g.Card(victim).Zone; z != engine.Graveyard {
		t.Errorf("victim zone = %v, want Graveyard", z)
	}
}

// TestExileUntilHostLeavesNeverReturnsAToken proves the NON_TOKEN filter
// (SpellAbilityEffect.java:841): an exiled token is not brought back.
func TestExileUntilHostLeavesNeverReturnsAToken(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	g.Card(victim).IsToken = true
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	host, err := castETBChain(t, g, p, etbChainDef(t, "Test Banisher", oRingLine), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	g.Move(host, engine.Graveyard, p)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if z := g.Card(victim).Zone; z == engine.Battlefield {
		t.Errorf("token zone = %v, want it gone", z)
	}
}

// TestExileUntilHostLeavesReturnsAtOnceWhenTheHostExiledItself proves the
// corner case of SpellAbilityEffect.java:909-911: the host is not in play
// after the moves, so the return runs right away.
func TestExileUntilHostLeavesReturnsAtOnceWhenTheHostExiledItself(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	if err := pushLineFrom(t, g, p, host, engine.NewScriptedController(),
		[]engine.EntityID{engine.CardEntity(host), engine.CardEntity(victim)},
		"DB$ ChangeZone | ValidTgts$ Creature | TargetMin$ 2 | TargetMax$ 2 | Origin$ Battlefield | Destination$ Exile | Duration$ UntilHostLeavesPlay"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	for _, id := range []engine.CardID{host, victim} {
		if z := g.Card(id).Zone; z != engine.Battlefield {
			t.Errorf("card %v zone = %v, want back on the Battlefield", id, z)
		}
	}
}

// TestExileUntilHostLeavesReturnsAHandCardToItsHand proves the hidden-origin
// path (Kitesail Freebooter): a card taken from a hand goes back to its
// owner's hand.
func TestExileUntilHostLeavesReturnsAHandCardToItsHand(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	taken := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueCardChoice([]engine.CardID{taken})
	host, err := castETBChain(t, g, p, etbChainDef(t, "Test Freebooter",
		"DB$ ChangeZone | ValidTgts$ Opponent | Origin$ Hand | Destination$ Exile | DefinedPlayer$ Targeted | Chooser$ You | ChangeType$ Card | ChangeNum$ 1 | IsCurse$ True | Duration$ UntilHostLeavesPlay"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(taken).Zone; z != engine.Exile {
		t.Fatalf("taken zone = %v, want Exile", z)
	}
	g.Move(host, engine.Graveyard, p)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if z, o := g.Card(taken).Zone, g.Card(taken).ZoneOwner; z != engine.Hand || o != other {
		t.Errorf("taken = %v of %v, want Hand of %v", z, o, other)
	}
}

// TestExileUntilHostLeavesReturnsEveryCardOfASearch proves the hidden
// battlefield shape (Glorious Protector, Lumbering Battlement): every card
// the search exiled comes back together.
func TestExileUntilHostLeavesReturnsEveryCardOfASearch(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{a, b})
	if err := pushLineFrom(t, g, p, host, c, nil,
		"DB$ ChangeZone | Origin$ Battlefield | Destination$ Exile | Hidden$ True | ChangeType$ Creature.Other+YouCtrl | ChangeNum$ 2 | Duration$ UntilHostLeavesPlay"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	for _, id := range []engine.CardID{a, b} {
		if z := g.Card(id).Zone; z != engine.Exile {
			t.Fatalf("card %v zone = %v, want Exile", id, z)
		}
	}
	g.Move(host, engine.Graveyard, p)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	for _, id := range []engine.CardID{a, b} {
		if z := g.Card(id).Zone; z != engine.Battlefield {
			t.Errorf("card %v zone = %v, want back on the Battlefield", id, z)
		}
	}
}

// TestExileUntilHostLeavesRejectsOtherDurations proves every Duration$ but
// UntilHostLeavesPlay, and the ReturnAbility$ delayed trigger, are refused
// before anything moves (GO-7).
func TestExileUntilHostLeavesRejectsOtherDurations(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ line, want string }{
		{"DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Exile | Duration$ UntilYourNextTurn", `Duration$ "UntilYourNextTurn" not resolvable yet`},
		{"DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Exile | Duration$ UntilHostLeavesPlay | ReturnAbility$ DBX", "ReturnAbility$ not resolvable yet"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		host, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line, "DBX", "DB$ BlankLine")
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: err = %v, want %q", tc.line, err, tc.want)
		}
		if z := g.Card(host).Zone; z != engine.Battlefield {
			t.Errorf("%q: host zone = %v, want it unmoved", tc.line, z)
		}
	}
}
