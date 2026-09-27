package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These tests cover MayPlay$ (StaticAbilityContinuous.java's RULES-layer
// play permission, Card.setMayPlay): CastSpell and PlayLand from a zone
// other than the hand, each through a real corpus static line.

// sba runs one state-based-action check, which is where Layer 8 rebuilds
// every MayPlay$ grant.
func sba(g *engine.Game) {
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
}

// Crucible of Worlds: "You may play lands from your graveyard." The grant
// names the land as it sits there; once it changes zones it is a new
// object, and only the next check grants it again.
func TestMayPlayLandFromGraveyard(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	forest := g.NewCard(corpusCard(t, "Forest"), p, engine.Graveyard)
	c := engine.NewScriptedController()
	if g.PlayLand(p, forest, c) {
		t.Fatal("played a land from the graveyard with no grant")
	}

	g.NewCard(corpusCard(t, "Crucible of Worlds"), p, engine.Battlefield)
	sba(g)
	g.Move(forest, engine.Exile, p)
	g.Move(forest, engine.Graveyard, p)
	if g.PlayLand(p, forest, c) {
		t.Error("a grant survived the land's zone change")
	}
	sba(g)
	if !g.PlayLand(p, forest, c) {
		t.Fatal("PlayLand from the graveyard under Crucible of Worlds = false")
	}
	if g.Card(forest).Zone != engine.Battlefield {
		t.Errorf("forest in %v, want the battlefield", g.Card(forest).Zone)
	}
}

// Future Sight: "You may play lands and cast spells from the top of your
// library." Only the top card (Card.TopLibrary) is granted.
func TestMayPlayTopOfLibrary(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Future Sight"), p, engine.Battlefield)
	top := g.NewCard(creatureDefCost(t, "Top Bear", "G"), p, engine.Library)
	second := g.NewCard(creatureDefCost(t, "Second Bear", "G"), p, engine.Library)
	if lib := g.Zone(engine.Library, p).Cards(); lib[0] != top {
		t.Fatalf("setup: library top is %d, want %d", lib[0], top)
	}
	sba(g)

	c := engine.NewScriptedController()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	if g.CastSpell(p, second, c) {
		t.Fatal("cast the second card of the library")
	}
	if !g.CastSpell(p, top, c) {
		t.Fatal("CastSpell(top of library) under Future Sight = false")
	}
	if g.Card(top).Zone != engine.Stack {
		t.Errorf("top card in %v, want the stack", g.Card(top).Zone)
	}
}

// Light Up the Stage's impulse draw: an Effect card's MayPlay$ over
// Card.IsRemembered in exile -- the corpus's dominant MayPlay$ shape (314 of
// 477 Effect-SVar lines). The exiled cards are castable; another exiled
// card is not.
func TestMayPlayImpulseDrawFromEffectCard(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	stranger := g.NewCard(creatureDefCost(t, "Stranger", "G"), p, engine.Exile)
	first := g.NewCard(creatureDefCost(t, "Exiled Bear", "G"), p, engine.Library)
	g.NewCard(creatureDefCost(t, "Exiled Elk", "G"), p, engine.Library)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c,
		"DB$ Dig | Defined$ You | DigNum$ 2 | ChangeNum$ All | DestinationZone$ Exile | RememberChanged$ True | SubAbility$ DBEffect",
		"DBEffect", "DB$ Effect | StaticAbilities$ StaticMayPlay | Duration$ UntilTheEndOfYourNextTurn | RememberObjects$ Remembered | ForgetOnMoved$ Exile | SubAbility$ DBCleanup",
		"DBCleanup", "DB$ Cleanup | ClearRemembered$ True",
		"StaticMayPlay", "Mode$ Continuous | Affected$ Card.IsRemembered | AffectedZone$ Exile | MayPlay$ True")
	if g.Card(first).Zone != engine.Exile {
		t.Fatalf("setup: dug card in %v, want exile", g.Card(first).Zone)
	}
	sba(g)

	g.Player(p).ManaPool.Add(mana.Green, 2)
	if g.CastSpell(p, stranger, c) {
		t.Error("cast an exiled card the effect does not remember")
	}
	if !g.CastSpell(p, first, c) {
		t.Fatal("CastSpell(impulse-drawn card) = false")
	}
}

// The same impulse draw through the real priority loop, with no manual
// check: priorityRound's own state-based-action check (CR 117.5) builds the
// grant before the caster is asked for an action, so the exiled card is
// castable in the same main phase.
func TestMayPlayGrantReachesThePriorityLoop(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	first := g.NewCard(creatureDefCost(t, "Exiled Bear", "G"), p, engine.Library)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c,
		"DB$ Dig | Defined$ You | DigNum$ 1 | ChangeNum$ All | DestinationZone$ Exile | RememberChanged$ True | SubAbility$ DBEffect",
		"DBEffect", "DB$ Effect | StaticAbilities$ StaticMayPlay | Duration$ UntilTheEndOfYourNextTurn | RememberObjects$ Remembered | ForgetOnMoved$ Exile | SubAbility$ DBCleanup",
		"DBCleanup", "DB$ Cleanup | ClearRemembered$ True",
		"StaticMayPlay", "Mode$ Continuous | Affected$ Card.IsRemembered | AffectedZone$ Exile | MayPlay$ True")

	g.Player(p).ManaPool.Add(mana.Green, 1)
	c.QueueAction(p, engine.Action{Kind: engine.ActionCast, Card: first})
	c.QueueAction(p, engine.Action{})
	c.QueueAction(other, engine.Action{})
	if err := g.PassPriority(engine.NewRegistry(), c); err != nil {
		t.Fatalf("PassPriority: %v", err)
	}
	if g.Card(first).Zone != engine.Battlefield {
		t.Errorf("impulse-drawn card in %v, want cast and resolved onto the battlefield", g.Card(first).Zone)
	}
}

// MayPlayWithoutManaCost$ from exile: cast with an empty pool.
func TestMayPlayWithoutManaCostFromExile(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(continuousDef(t, "Free Exile",
		"Mode$ Continuous | Affected$ Card.YouOwn | AffectedZone$ Exile | MayPlay$ True | MayPlayWithoutManaCost$ True"),
		p, engine.Battlefield)
	exiled := g.NewCard(creatureDefCost(t, "Pricey", "7 G"), p, engine.Exile)
	sba(g)

	if !g.CastSpell(p, exiled, engine.NewScriptedController()) {
		t.Fatal("CastSpell without mana under MayPlayWithoutManaCost$ = false")
	}
}

// MayPlayWithFlash$ lifts sorcery timing: a creature from the graveyard
// cast on the opponent's turn.
func TestMayPlayWithFlashIgnoresSorceryTiming(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	g.NewCard(continuousDef(t, "Flash Yard",
		"Mode$ Continuous | Affected$ Creature.YouOwn | AffectedZone$ Graveyard | MayPlay$ True | MayPlayWithFlash$ True"),
		p, engine.Battlefield)
	dead := g.NewCard(creatureDefCost(t, "Returning", "G"), p, engine.Graveyard)
	g.SetTurnState(1, other, engine.Main1)
	sba(g)

	g.Player(p).ManaPool.Add(mana.Green, 1)
	if !g.CastSpell(p, dead, engine.NewScriptedController()) {
		t.Fatal("CastSpell with MayPlayWithFlash$ on the opponent's turn = false")
	}
}

// MayPlayDontGrantZonePermissions$ changes how, not whether: alone it lets
// nothing be cast from exile.
func TestMayPlayWithoutZonePermissionGrantsNothingAlone(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(continuousDef(t, "Cost Only",
		"Mode$ Continuous | Affected$ Card.YouOwn | AffectedZone$ Exile | MayPlay$ True | MayPlayWithoutManaCost$ True | MayPlayDontGrantZonePermissions$ True"),
		p, engine.Battlefield)
	exiled := g.NewCard(creatureDefCost(t, "Pricey", "G"), p, engine.Exile)
	sba(g)

	g.Player(p).ManaPool.Add(mana.Green, 1)
	if g.CastSpell(p, exiled, engine.NewScriptedController()) {
		t.Error("cast from exile on a grant without zone permission")
	}
}

// Omniscience: casting from hand stays an option beside the free MayPlay$
// one, and this port has no decision to pick between them, so the cast is
// refused with a pending error rather than a guess (GO-7).
func TestMayPlayHandChoiceFailsClosed(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Omniscience"), p, engine.Battlefield)
	bear := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Hand)
	sba(g)

	g.Player(p).ManaPool.Add(mana.Green, 2)
	if g.CastSpell(p, bear, engine.NewScriptedController()) {
		t.Error("CastSpell picked between hand and Omniscience on its own")
	}
	if err := g.TakePendingError(); err == nil {
		t.Error("no pending error for the unresolvable choice")
	}
}
