package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Animate's Abilities$ (AnimateEffectBase.java:159-168): the animated card
// gains the compiled activated abilities for as long as the record lasts.

// grantedGain is the SVar every test below grants: "{1}: You gain 1 life."
const grantedGain = "AB$ GainLife | Cost$ 1 | Defined$ You | LifeAmount$ 1"

// canActivateGrant reports whether p can activate creature's ability 0 (the
// granted one: creatureDefPT prints none), paying {1} from a fresh pool.
func canActivateGrant(t *testing.T, g *engine.Game, p engine.PlayerID, creature engine.CardID) bool {
	t.Helper()
	g.Player(p).ManaPool.AddColorless(1)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardC)
	if !g.ActivateAbility(p, creature, 0, c) {
		return false
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	return true
}

// Duration$ AsLongAsControl (Quicksmith Rebel's shape): the granted ability
// outlives cleanup and goes with the host's control.
func TestAnimateAbilitiesAsLongAsControlEndsWithTheHost(t *testing.T) {
	t.Parallel()

	for _, how := range []string{"cleanup", "dies", "stolen"} {
		g, p, other := newTwoPlayerGame(t)
		creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		host := stealUntil(t, g, p, creature,
			"DB$ Animate | ValidTgts$ Creature.YouCtrl | Abilities$ ABGain | Duration$ AsLongAsControl", "ABGain", grantedGain)
		sba(g)
		if !canActivateGrant(t, g, p, creature) {
			t.Fatalf("%s: the granted ability cannot be activated", how)
		}
		want := true
		switch how {
		case "cleanup":
			endTurn(g, 1, p)
			g.SetTurnState(2, p, engine.Main1)
		case "dies":
			g.Move(host, engine.Graveyard, p)
			want = false
		case "stolen":
			stealUntil(t, g, other, host, "DB$ GainControl | ValidTgts$ Creature.OppCtrl")
			want = false
		}
		sba(g)
		if got := canActivateGrant(t, g, p, creature); got != want {
			t.Errorf("%s: ability activatable = %v, want %v", how, got, want)
		}
	}
	// 20 + the activations that succeeded is checked through the bool above.
}

// No Duration$ is until end of turn (Retraction Helix's shape).
func TestAnimateAbilitiesEndAtCleanup(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	stealUntil(t, g, p, creature, "DB$ Animate | ValidTgts$ Creature | Abilities$ ABGain", "ABGain", grantedGain)
	if !canActivateGrant(t, g, p, creature) {
		t.Fatal("the granted ability cannot be activated")
	}
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("life = %d, want 21", got)
	}
	endTurn(g, 1, p)
	g.SetTurnState(2, p, engine.Main1)
	sba(g)
	if canActivateGrant(t, g, p, creature) {
		t.Error("the granted ability outlived the turn")
	}
}

// The shapes left unported are refused before anything changes (GO-7):
// TransferActivator$ (Martyrdom's "only you may activate"), and a Perpetual
// grant, whose zone-surviving store holds triggers only.
func TestAnimateAbilitiesRejectsUnportedShapes(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ line, want string }{
		{"DB$ Animate | ValidTgts$ Creature | Abilities$ ABGain | TransferActivator$ True", "TransferActivator$"},
		{"DB$ Animate | ValidTgts$ Creature | Abilities$ ABGain | Duration$ Perpetual", "Perpetual"},
		{"DB$ Animate | ValidTgts$ Creature | Abilities$ ABGain | Triggers$ TrigX | Duration$ Perpetual", "Perpetual"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(creature)}, tc.line,
			"ABGain", grantedGain,
			"TrigX", "Mode$ Attacks | ValidCard$ Card.Self | Execute$ ABGain")
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: err = %v, want one naming %s", tc.line, err, tc.want)
		}
	}
}

// A grant compile left unfollowed (an ability that regrants itself) is
// refused, as a Triggers$ one is.
func TestAnimateAbilitiesRejectsASelfRegrant(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	// etbChainDef names the line's own SVar Trig: granting Trig is the chain
	// coming back to itself, which compile leaves unfollowed.
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
		"DB$ Animate | Defined$ Self | Abilities$ Trig")
	if err == nil || !strings.Contains(err.Error(), "regrants itself") {
		t.Errorf("err = %v, want the regrant refusal", err)
	}
}

// AnimateAll shares the record: every matching creature gains the ability
// (Brawl's shape).
func TestAnimateAllGrantsTheAbilityToEachCard(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ AnimateAll | ValidCards$ Creature.powerEQ2 | Abilities$ ABGain", "ABGain", grantedGain)
	sba(g)
	for _, id := range []engine.CardID{a, b} {
		if !canActivateGrant(t, g, p, id) {
			t.Errorf("creature %v lacks the granted ability", id)
		}
	}
}

// Animate's Triggers$ without Duration$ Perpetual (AnimateEffect.java:127-130):
// the animated card carries the trigger until the record ends -- cleanup by
// default.
func TestAnimateTriggersEndAtCleanup(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	for range 3 {
		g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)
	}
	stealUntil(t, g, p, creature, "DB$ Animate | ValidTgts$ Creature | Triggers$ TrigGain",
		"TrigGain", "Mode$ Drawn | ValidCard$ Card.YouCtrl | TriggerZones$ Battlefield | Execute$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 2")
	c := engine.NewScriptedController()
	g.DrawCards(p, 1, c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Fatalf("life after a draw = %d, want 22: the granted trigger did not fire", got)
	}
	endTurn(g, 1, p)
	g.SetTurnState(2, p, engine.Main1)
	sba(g)
	g.DrawCards(p, 1, c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life after the next turn's draw = %d, want 22: the trigger outlived the turn", got)
	}
}
