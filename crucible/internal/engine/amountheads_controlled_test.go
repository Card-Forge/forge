package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestDomainCountUsesControllerNotOwner is a regression test for a bug the
// Layer 7a review caught before merge: domainCount (amountheads.go) read
// g.Zone(Battlefield, pid).Cards(), which is keyed by owner (zone.go), not
// controller. A GainControl'd land sits in its owner's battlefield zone, so
// Count$Domain undercounted a controller's stolen basic lands and would
// have overcounted the owner's own domain after losing control of one.
func TestDomainCountUsesControllerNotOwner(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(continuousDefWithSVar(t, "Test Domain Anthem",
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ X",
		"X", "Count$Domain"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	plains := g.NewCard(landDef(t, "Plains", "Basic Land Plains"), other, engine.Battlefield)

	// Before the steal: the Plains is other's, so p's own Domain is 0.
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if pw, ok := g.Card(creature).Power(); !ok || pw != 2 {
		t.Fatalf("Power() before steal = (%d, %v), want (2, true) -- p controls no basic land yet", pw, ok)
	}

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(plains)})
	def := etbChainDef(t, "Test Steal Land", "DB$ GainControl | ValidTgts$ Land.OppCtrl")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(plains).Controller(); got != p {
		t.Fatalf("plains controller = %v, want %v", got, p)
	}

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if pw, ok := g.Card(creature).Power(); !ok || pw != 3 {
		t.Errorf("Power() after steal = (%d, %v), want (3, true) -- p now controls the stolen Plains, "+
			"still owned by other, and Count$Domain must count what p controls, not what p owns", pw, ok)
	}
}

// TestDevotionAndChromaCountUseControllerNotOwner is the same regression for
// devotionCount and Count$Chroma: both summed mana symbols over
// g.Zone(Battlefield, pid).Cards(), the owner's zone, instead of walking
// every player's battlefield and filtering on Controller.
func TestDevotionAndChromaCountUseControllerNotOwner(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(continuousDefWithSVar(t, "Test Devotion Anthem",
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ X",
		"X", "Count$Devotion.Black"), p, engine.Battlefield)
	g.NewCard(continuousDefWithSVar(t, "Test Chroma Anthem",
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddToughness$ Y",
		"Y", "Count$Chroma.Black"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	stolen := g.NewCard(creatureDefManaCost(t, "B B"), other, engine.Battlefield)

	// Before the steal: the BB creature is other's, so p reads no devotion
	// or chroma from it.
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	pw, pok := g.Card(creature).Power()
	tw, tok := g.Card(creature).Toughness()
	if !pok || pw != 2 || !tok || tw != 2 {
		t.Fatalf("P/T before steal = (%d,%v)/(%d,%v), want (2,true)/(2,true) -- p controls no black permanent yet",
			pw, pok, tw, tok)
	}

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(stolen)})
	def := etbChainDef(t, "Test Steal Creature", "DB$ GainControl | ValidTgts$ Creature.OppCtrl")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(stolen).Controller(); got != p {
		t.Fatalf("stolen creature controller = %v, want %v", got, p)
	}

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	pw, pok = g.Card(creature).Power()
	tw, tok = g.Card(creature).Toughness()
	if !pok || pw != 4 || !tok || tw != 4 {
		t.Errorf("P/T after steal = (%d,%v)/(%d,%v), want (4,true)/(4,true) -- p now controls the stolen BB "+
			"creature, still owned by other, and Count$Devotion/Count$Chroma must count what p controls, not "+
			"what p owns", pw, pok, tw, tok)
	}
}
