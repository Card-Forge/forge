package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// This file covers who decides a ChangeZone: Chooser$ on both paths
// (ChangeZoneEffect.java:530-546, 911-923), and the OriginAlternative$
// "search your library and/or graveyard" prompt (:939-970).

// deciderRecorder is a ScriptedController that records which player each
// card pick, confirm and ordering was asked of, and every pool offered.
type deciderRecorder struct {
	*engine.ScriptedController
	picks, confirms, orders []engine.PlayerID
	offers                  [][]engine.CardID
}

func newDeciderRecorder() *deciderRecorder {
	return &deciderRecorder{ScriptedController: engine.NewScriptedController()}
}

func (r *deciderRecorder) ChooseCardsForEffect(g *engine.Game, p engine.PlayerID, src engine.CardID, choices []engine.CardID, lo, hi int) []engine.CardID {
	r.picks = append(r.picks, p)
	r.offers = append(r.offers, append([]engine.CardID(nil), choices...))
	return r.ScriptedController.ChooseCardsForEffect(g, p, src, choices, lo, hi)
}

func (r *deciderRecorder) ConfirmEffect(g *engine.Game, p engine.PlayerID, src engine.CardID) bool {
	r.confirms = append(r.confirms, p)
	return r.ScriptedController.ConfirmEffect(g, p, src)
}

func (r *deciderRecorder) OrderCardsForZone(g *engine.Game, p engine.PlayerID, cards []engine.CardID, dest engine.ZoneType) []engine.CardID {
	r.orders = append(r.orders, p)
	return r.ScriptedController.OrderCardsForZone(g, p, cards, dest)
}

// TestSearchChooserPicksFromAnotherPlayersHand proves the dominant Chooser$
// shape (Thoughtseize-style "target opponent reveals their hand, you choose"):
// the opponent is the fetcher, the activator makes the pick. AlreadyRevealed$
// (a display-only flag) changes nothing.
func TestSearchChooserPicksFromAnotherPlayersHand(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	taken := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Hand)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := newDeciderRecorder()
	c.QueueCardChoice([]engine.CardID{taken})
	if err := pushLineFrom(t, g, p, host, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ ChangeZone | ValidTgts$ Opponent | Origin$ Hand | Destination$ Exile | DefinedPlayer$ Targeted | Chooser$ You | ChangeType$ Card | Mandatory$ True | AlreadyRevealed$ True"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(taken).Zone; z != engine.Exile {
		t.Errorf("taken zone = %v, want Exile", z)
	}
	if !slices.Equal(c.picks, []engine.PlayerID{p}) {
		t.Errorf("pick asked of %v, want the Chooser$ %v", c.picks, p)
	}
}

// TestSearchWithoutChooserIsDecidedByTheFetcher proves the default: with no
// Chooser$ the DefinedPlayer$ fetcher picks from their own hand.
func TestSearchWithoutChooserIsDecidedByTheFetcher(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	taken := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Hand)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := newDeciderRecorder()
	c.QueueCardChoice([]engine.CardID{taken})
	if err := pushLineFrom(t, g, p, host, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ ChangeZone | ValidTgts$ Opponent | Origin$ Hand | Destination$ Exile | DefinedPlayer$ Targeted | ChangeType$ Card | Mandatory$ True"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !slices.Equal(c.picks, []engine.PlayerID{other}) {
		t.Errorf("pick asked of %v, want the fetcher %v", c.picks, other)
	}
}

// TestSearchOfATargetedPlayerIsDecidedByTheActivator proves Java's order at
// ChangeZoneEffect.java:919-927: the decider is fixed before a ValidTgts$
// fetcher is swapped for the target, so with neither DefinedPlayer$ nor
// Chooser$ the activator picks from the targeted player's hand.
func TestSearchOfATargetedPlayerIsDecidedByTheActivator(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	taken := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Hand)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := newDeciderRecorder()
	c.QueueCardChoice([]engine.CardID{taken})
	if err := pushLineFrom(t, g, p, host, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ ChangeZone | ValidTgts$ Player | Origin$ Hand | Destination$ Exile | ChangeType$ Card | Mandatory$ True"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(taken).Zone; z != engine.Exile {
		t.Errorf("taken zone = %v, want Exile", z)
	}
	if !slices.Equal(c.picks, []engine.PlayerID{p}) {
		t.Errorf("pick asked of %v, want the activator %v", c.picks, p)
	}
}

// TestSearchChooserAmongSeveralIsTheActivatorsPick proves a Chooser$ naming
// several players is narrowed by the activator first (:913-916), and a pick
// outside them is an error.
func TestSearchChooserAmongSeveralIsTheActivatorsPick(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Library)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := newDeciderRecorder()
	c.QueuePlayerChoice(other)
	c.QueueCardChoice(nil)
	line := "DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeType$ Creature | Chooser$ Player"
	if err := pushLineFrom(t, g, p, host, c, nil, line); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !slices.Equal(c.picks, []engine.PlayerID{other}) {
		t.Errorf("pick asked of %v, want the chosen %v", c.picks, other)
	}

	bad := newDeciderRecorder()
	bad.QueuePlayerChoice(engine.PlayerID(99))
	if err := pushLineFrom(t, g, p, host, bad, nil, line); err == nil || !strings.Contains(err.Error(), "Chooser$ answer") {
		t.Errorf("err = %v, want the Chooser$ answer refused", err)
	}
}

// TestSearchChooserNamingNobodyLeavesTheFetcher proves an empty Chooser$
// set leaves each fetcher deciding (:914, chooser stays null).
func TestSearchChooserNamingNobodyLeavesTheFetcher(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	wanted := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Library)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := newDeciderRecorder()
	c.QueueCardChoice([]engine.CardID{wanted})
	if err := pushLineFrom(t, g, p, host, c, nil,
		"DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeType$ Creature | Chooser$ Remembered"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !slices.Equal(c.picks, []engine.PlayerID{p}) {
		t.Errorf("pick asked of %v, want the fetcher %v", c.picks, p)
	}
}

// TestKnownMoveChooserConfirmsAndOrders proves the known-origin Chooser$
// (:530-546, 566): the first player it names answers Optional$ and orders
// the cards going into a library; an empty set is an error.
func TestKnownMoveChooserConfirmsAndOrders(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := newDeciderRecorder()
	c.QueueCardOrder([]engine.CardID{b, a})
	c.QueueConfirmEffect(true)
	c.QueueConfirmEffect(true)
	if err := pushLineFrom(t, g, p, host, c, []engine.EntityID{engine.CardEntity(a), engine.CardEntity(b)},
		"DB$ ChangeZone | ValidTgts$ Creature | TargetMin$ 2 | TargetMax$ 2 | Origin$ Battlefield | Destination$ Library | Optional$ True | Chooser$ Opponent"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !slices.Equal(c.orders, []engine.PlayerID{other}) || !slices.Equal(c.confirms, []engine.PlayerID{other, other}) {
		t.Errorf("orders asked of %v, confirms of %v, want both of %v", c.orders, c.confirms, other)
	}
	// Moved b then a, each onto the top: a ends on top.
	if lib := g.Zone(engine.Library, p).Cards(); !slices.Equal(lib, []engine.CardID{a, b}) {
		t.Errorf("library = %v, want the Chooser$ order's result [%v %v]", lib, a, b)
	}

	if err := pushLineFrom(t, g, p, host, newDeciderRecorder(), nil,
		"DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Hand | Chooser$ Remembered"); err == nil || !strings.Contains(err.Error(), "names no player") {
		t.Errorf("err = %v, want an empty known-origin Chooser$ refused", err)
	}
	if err := pushLineFrom(t, g, p, host, newDeciderRecorder(), nil,
		"DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Hand | Chooser$ Bogus"); err == nil {
		t.Error("an unresolvable known-origin Chooser$ resolved, want an error")
	}
	if err := pushLineFrom(t, g, p, host, newDeciderRecorder(), nil,
		"DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeType$ Creature | Chooser$ Bogus"); err == nil {
		t.Error("an unresolvable search Chooser$ resolved, want an error")
	}
}

// originAltLine is the dominant OriginAlternative$ shape: a planeswalker
// deck's "search your library and/or graveyard for a card named ...".
const originAltLine = "DB$ ChangeZone | Origin$ Library | OriginAlternative$ Graveyard | Destination$ Hand | ChangeType$ Creature.YouOwn | Optional$ True"

// TestOriginAlternativeDeclinedLibrarySearchesTheGraveyard proves a "no" to
// the library leaves the graveyard as the only zone, searched without being
// asked, mandatory because it holds a match, and no shuffle.
func TestOriginAlternativeDeclinedLibrarySearchesTheGraveyard(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	lib := libraryCards(t, g, p, 6)
	dead := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Graveyard)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := newDeciderRecorder()
	c.QueueConfirmEffect(false) // search the library?
	c.QueueConfirmEffect(true)  // Optional$
	c.QueueCardChoice([]engine.CardID{dead})
	if err := pushLineFrom(t, g, p, host, c, nil, originAltLine); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(dead).Zone; z != engine.Hand {
		t.Errorf("graveyard card zone = %v, want Hand", z)
	}
	if len(c.offers) != 1 || !slices.Equal(c.offers[0], []engine.CardID{dead}) {
		t.Errorf("offered %v, want only the graveyard card", c.offers)
	}
	if got := g.Zone(engine.Library, p).Cards(); !slices.Equal(got, lib) {
		t.Errorf("library = %v, want it unshuffled %v", got, lib)
	}
}

// TestOriginAlternativeBothZonesOffersBoth proves a "yes" to the library and
// a "yes" to the graveyard offers matches from both.
func TestOriginAlternativeBothZonesOffersBoth(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	inLib := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Library)
	dead := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Graveyard)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := newDeciderRecorder()
	c.QueueConfirmEffect(true) // search the library?
	c.QueueConfirmEffect(true) // and the graveyard?
	c.QueueConfirmEffect(true) // Optional$
	c.QueueCardChoice([]engine.CardID{inLib})
	if err := pushLineFrom(t, g, p, host, c, nil, originAltLine); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if len(c.offers) != 1 || !slices.Contains(c.offers[0], inLib) || !slices.Contains(c.offers[0], dead) {
		t.Errorf("offered %v, want both %v and %v", c.offers, inLib, dead)
	}
	if z := g.Card(inLib).Zone; z != engine.Hand {
		t.Errorf("library card zone = %v, want Hand", z)
	}
}

// TestOriginAlternativeFallsBackToTheLastZone proves the loop's end: with
// two alternatives, no to the library and no to the graveyard leaves the
// hand, searched without a further question.
func TestOriginAlternativeFallsBackToTheLastZone(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	inHand := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Hand)
	g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Graveyard)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := newDeciderRecorder()
	c.QueueConfirmEffect(false)
	c.QueueConfirmEffect(false)
	c.QueueCardChoice([]engine.CardID{inHand})
	if err := pushLineFrom(t, g, p, host, c, nil,
		"DB$ ChangeZone | Hidden$ True | Origin$ Library | OriginAlternative$ Graveyard,Hand | Destination$ Battlefield | ChangeType$ Creature.YouOwn"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if len(c.offers) != 1 || !slices.Equal(c.offers[0], []engine.CardID{inHand}) || len(c.confirms) != 2 {
		t.Errorf("offered %v after %d confirms, want only the hand card after 2", c.offers, len(c.confirms))
	}
	if z := g.Card(inHand).Zone; z != engine.Battlefield {
		t.Errorf("hand card zone = %v, want Battlefield", z)
	}
}

// TestOriginAlternativeRefusals proves a malformed OriginAlternative$ and
// one on a known-origin move are errors, not ignored (GO-7).
func TestOriginAlternativeRefusals(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	if err := pushLineFrom(t, g, p, host, newDeciderRecorder(), nil,
		"DB$ ChangeZone | Origin$ Library | OriginAlternative$ Bogus | Destination$ Hand | ChangeType$ Card"); err == nil {
		t.Error("an unknown OriginAlternative$ zone resolved, want an error")
	}
	if err := pushLineFrom(t, g, p, host, newDeciderRecorder(), nil,
		"DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | OriginAlternative$ Graveyard | Destination$ Hand"); err == nil || !strings.Contains(err.Error(), "known-origin") {
		t.Errorf("err = %v, want OriginAlternative$ on a known-origin move refused", err)
	}
}
