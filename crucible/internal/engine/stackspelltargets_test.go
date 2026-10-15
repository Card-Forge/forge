package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These tests cover a ChangeZone whose Origin$ names the stack: the targeted
// spell is offered as its card (AbilityFactory.adjustChangeZoneTarget makes
// Origin$ the target zones) and, resolving, leaves the stack before its card
// moves (ChangeZoneEffect.java:577-579, MagicStack.remove(Card)).

// castBearsSpell puts the real Grizzly Bears on the stack as other's spell.
func castBearsSpell(t *testing.T, g *engine.Game, other engine.PlayerID) engine.CardID {
	t.Helper()
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Hand)
	g.SetTurnState(1, other, engine.Main1)
	g.Player(other).ManaPool.Add(mana.Green, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardG)
	if !g.CastSpell(other, bears, c) {
		t.Fatal("could not cast Grizzly Bears")
	}
	return bears
}

// stackOfferRecorder records every candidate list ChooseTargets is offered,
// then answers from the embedded queue.
type stackOfferRecorder struct {
	*engine.ScriptedController
	offers [][]engine.EntityID
}

func (r *stackOfferRecorder) ChooseTargets(g *engine.Game, decider engine.PlayerID, valid []engine.EntityID, lo, hi int) []engine.EntityID {
	r.offers = append(r.offers, append([]engine.EntityID(nil), valid...))
	return r.ScriptedController.ChooseTargets(g, decider, valid, lo, hi)
}

func TestChangeZoneReturnsATargetedSpellToItsOwnersHand(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	bears := castBearsSpell(t, g, other)
	unsub := g.NewCard(corpusCard(t, "Unsubstantiate"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Blue, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardU)
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bears)})
	if !g.CastSpell(p, unsub, c) {
		t.Fatal("could not cast Unsubstantiate at the Bears spell")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(bears); got.Zone != engine.Hand || got.ZoneOwner != other {
		t.Errorf("Grizzly Bears is in %v of %v, want its owner's hand", got.Zone, got.ZoneOwner)
	}
	if n := g.StackLen(); n != 0 {
		t.Errorf("stack holds %d items, want 0: the Bears spell left it", n)
	}
	if got := g.Card(unsub).Zone; got != engine.Graveyard {
		t.Errorf("Unsubstantiate is in %v, want the graveyard", got)
	}
}

// Origin$ Battlefield,Stack asks over the battlefield first, then the stack
// (Game.getCardsIn over the zone list); a spell is never its own candidate
// (CR 115.5).
func TestChangeZoneOriginStackOffersTheBattlefieldThenTheStack(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	wall := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	bears := castBearsSpell(t, g, other)
	unsub := g.NewCard(corpusCard(t, "Unsubstantiate"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Blue, 2)
	r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
	r.QueuePayGeneric(mana.ShardU)
	r.QueueTargets([]engine.EntityID{engine.CardEntity(wall)})
	if !g.CastSpell(p, unsub, r) {
		t.Fatal("could not cast Unsubstantiate")
	}
	if len(r.offers) != 1 {
		t.Fatalf("offered %d target questions, want 1", len(r.offers))
	}
	want := []engine.EntityID{engine.CardEntity(wall), engine.CardEntity(bears)}
	if !slices.Equal(r.offers[0], want) {
		t.Errorf("candidates = %v, want %v (battlefield, then the stack)", r.offers[0], want)
	}
	if err := g.ResolveStack(engine.NewRegistry(), r); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(wall).Zone; got != engine.Hand {
		t.Errorf("the battlefield target is in %v, want hand", got)
	}
	if got := g.Card(bears).Zone; got != engine.Battlefield {
		t.Errorf("the untargeted Bears spell is in %v, want the battlefield", got)
	}
}

// A spell that left the stack before ChangeZone resolves is a new object: the
// target is illegal (CR 608.2b) and the spell does nothing.
func TestChangeZoneSpellTargetThatResolvedFizzles(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	bears := castBearsSpell(t, g, other)
	unsub := g.NewCard(corpusCard(t, "Unsubstantiate"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Blue, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardU)
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bears)})
	if !g.CastSpell(p, unsub, c) {
		t.Fatal("could not cast Unsubstantiate")
	}
	// The Bears move off the stack under the Unsubstantiate.
	g.Move(bears, engine.Battlefield, other)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(bears).Zone; got != engine.Battlefield {
		t.Errorf("Grizzly Bears is in %v, want the battlefield (the target changed zones)", got)
	}
}

// A Defined$ spell other than the host with Origin$ Stack is not audited:
// it fails loudly instead of moving.
func TestChangeZoneDefinedSpellOnTheStackFails(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	bears := castBearsSpell(t, g, other)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.Card(host).Memory.Remember(engine.CardEntity(bears))
	def := etbChainDef(t, "Defined Bouncer", "DB$ ChangeZone | Defined$ Remembered | Origin$ Stack | Destination$ Hand")
	sub := def.Faces[0].Triggers[0].Subs[0].Ability
	g.PushAbility(engine.Ability{API: engine.APIChangeZone, Source: host, Controller: p, Params: sub})
	err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController())
	if err == nil || !strings.Contains(err.Error(), "Origin$ Stack for a spell other than its own host") {
		t.Fatalf("ResolveStack = %v, want the Origin$ Stack refusal", err)
	}
}

// ChangeZoneAll's Resolve does not take a spell off the stack, so an
// Origin$ Stack target is refused when targets are chosen, and the ability
// fails as it resolves.
func TestChangeZoneAllOriginStackTargetFails(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	def := etbChainDef(t, "Stack Sweeper", "DB$ ChangeZoneAll | ValidTgts$ Card | Origin$ Stack | Destination$ Hand | ChangeType$ Card")
	_, err := castETBChain(t, g, p, def, engine.NewScriptedController())
	if err == nil || !strings.Contains(err.Error(), "targeting a card in Stack not resolvable yet") {
		t.Fatalf("ResolveStack = %v, want the ChangeZoneAll Origin$ Stack refusal", err)
	}
}
