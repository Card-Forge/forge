package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// These tests cover the two rules readers of ControlPlayer's redirect
// (ADR-0030): a player another player controls cannot reach outside the
// game -- Learn offers no sideboard Lesson (Player.java:3906) and a Wish
// does not search Sideboard (ChangeZoneEffect.java:987-991).

// resolveAs is resolveNow for any PlayerController: pid's ability from a
// host on pid's battlefield, resolved in the current phase.
func resolveAs(t *testing.T, g *engine.Game, pid engine.PlayerID, c engine.PlayerController, line string) {
	t.Helper()
	def := etbChainDef(t, "Test As", line)
	host := g.NewCard(def, pid, engine.Battlefield)
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		api, ok := engine.APIByName(sub.Ability.Name)
		if !ok {
			t.Fatalf("unknown API %q", sub.Ability.Name)
		}
		g.PushAbility(engine.Ability{API: api, Source: host, Controller: pid, Params: sub.Ability, Amounts: face.Amounts})
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatalf("%q: %v", line, err)
		}
		return
	}
	t.Fatal("no Execute$")
}

// controlledNextTurn is a two-player game in turn 2's Main1 with other
// controlled by p (Mindslaver resolved in p's turn 1).
func controlledNextTurn(t *testing.T) (*engine.Game, engine.PlayerID, engine.PlayerID) {
	t.Helper()
	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	resolveLine(t, g, p, c, "DB$ ControlPlayer | ValidTgts$ Player")
	advanceToPhase(t, g, c, 2, engine.Main1)
	wantControl(t, g, other, p)
	return g, p, other
}

// TestLearnSkipsSideboardLessonsForAControlledPlayer proves
// Player.learnLesson's isControlled check: a controlled player is offered
// their hand only; free again, the Lesson is back.
func TestLearnSkipsSideboardLessonsForAControlledPlayer(t *testing.T) {
	t.Parallel()

	g, _, other := controlledNextTurn(t)
	lesson := &compile.Card{Name: "Lesson"}
	lesson.Faces[0].Type = cardtype.Parse(lessonRegistry(t), "Sorcery Lesson")
	sideboard := g.NewCard(lesson, other, engine.Sideboard)
	inHand := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Hand)

	controlled := &offerRecorder{ScriptedController: engine.NewScriptedController()}
	controlled.QueueCardChoice(nil)
	resolveAs(t, g, other, controlled, "DB$ Learn")
	hand := g.Zone(engine.Hand, other).Cards()
	if len(controlled.offers) != 1 || !slices.Equal(controlled.offers[0], hand) || !slices.Contains(hand, inHand) {
		t.Errorf("controlled player offered %v, want their hand %v only, not the Lesson %v", controlled.offers, hand, sideboard)
	}

	advanceToPhase(t, g, engine.NewScriptedController(), 3, engine.Main1)
	wantControl(t, g, other, engine.NoPlayer)
	free := engine.NewScriptedController()
	free.QueueCardChoice([]engine.CardID{sideboard})
	resolveAs(t, g, other, free, "DB$ Learn")
	if z := g.Card(sideboard).Zone; z != engine.Hand {
		t.Errorf("free player's Lesson in %v, want Hand", z)
	}
}

// TestWishCannotReachOutsideTheGameForAControlledPlayer proves the Wish
// rule: Origin$ Sideboard is dropped for a controlled fetcher -- nothing is
// offered and the card stays put, while another Origin$ zone is still
// searched -- and the same Wish works once the player is free.
func TestWishCannotReachOutsideTheGameForAControlledPlayer(t *testing.T) {
	t.Parallel()

	const wish = "DB$ ChangeZone | Origin$ Sideboard | Destination$ Hand | ChangeType$ Card.YouOwn | ChangeNum$ 1 | Hidden$ True | Reveal$ True"
	g, _, other := controlledNextTurn(t)
	outside := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Sideboard)
	exiled := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Exile)

	controlled := &offerRecorder{ScriptedController: engine.NewScriptedController()}
	resolveAs(t, g, other, controlled, wish)
	if len(controlled.offers) != 0 || g.Card(outside).Zone != engine.Sideboard {
		t.Errorf("controlled Wish offered %v, card in %v; want no offer, card still in Sideboard",
			controlled.offers, g.Card(outside).Zone)
	}

	both := &offerRecorder{ScriptedController: engine.NewScriptedController()}
	both.QueueCardChoice(nil)
	resolveAs(t, g, other, both,
		"DB$ ChangeZone | Origin$ Sideboard,Exile | Destination$ Hand | ChangeType$ Card.YouOwn | ChangeNum$ 1 | Hidden$ True")
	if len(both.offers) != 1 || !slices.Equal(both.offers[0], []engine.CardID{exiled}) {
		t.Errorf("controlled Sideboard,Exile search offered %v, want only the exiled card %v", both.offers, exiled)
	}

	advanceToPhase(t, g, engine.NewScriptedController(), 3, engine.Main1)
	free := engine.NewScriptedController()
	free.QueueCardChoice([]engine.CardID{outside})
	resolveAs(t, g, other, free, wish)
	if z := g.Card(outside).Zone; z != engine.Hand {
		t.Errorf("free Wish left the card in %v, want Hand", z)
	}
}

// TestSideboardOriginIsHiddenWithoutHiddenParam proves SpellAbility.isHidden
// counts Sideboard as a hidden zone (ZoneType.java:23): a Sideboard Origin$
// without Hidden$ still searches and chooses rather than moving the host.
func TestSideboardOriginIsHiddenWithoutHiddenParam(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	outside := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Sideboard)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{outside})
	resolveAs(t, g, p, c, "DB$ ChangeZone | Origin$ Sideboard | Destination$ Hand | ChangeType$ Card.YouOwn | ChangeNum$ 1")
	if z := g.Card(outside).Zone; z != engine.Hand {
		t.Errorf("card in %v, want Hand", z)
	}
}
