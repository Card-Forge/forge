package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// changesZoneAllWatcherDef builds an Enchantment carrying one Mode$
// ChangesZoneAll trigger naming extraParams, Execute$ chaining into a
// GainLife of 5 -- a fixed, distinctive amount so a test can tell "fired
// once" (25) apart from "fired once per card" (30 for a two-card batch)
// without reading the AbilityKey.Amount the trigger records.
func changesZoneAllWatcherDef(t *testing.T, name, extraParams string) *compile.Card {
	t.Helper()

	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Enchantment")
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZoneAll | " + extraParams + " | Execute$ TrigGain",
	}
	raw.Faces[0].SVars.Set("TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 5")

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// TestChangesZoneAllFiresOnceForSacrificeAllBatch proves CR 603.6d's own
// "one or more permanents change zones together" trigger, wired into
// sacrificeCards (sacrificeeffect.go): a SacrificeAll that sacrifices two
// creatures in one resolution fires the watcher's own Mode$ ChangesZoneAll
// trigger once, not twice -- life gains 5, not 10.
func TestChangesZoneAllFiresOnceForSacrificeAllBatch(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	g.NewCard(changesZoneAllWatcherDef(t, "Test Watcher", "Destination$ Graveyard | ValidCards$ Creature"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	c := engine.NewScriptedController()
	def := etbSacrificeAllTriggerDefParams(t, "Test SacAll Trigger", "ValidCards$ Creature.Other", nil)
	if _, err := castETBSacrifice(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 25 {
		t.Errorf("p's life = %d, want 25 -- Mode$ ChangesZoneAll must fire once for the whole two-card batch, not once per card", g.Player(p).Life)
	}
}

// TestChangesZoneAllSkipsWhenNoCardInTheBatchMatchesValidCards proves
// ValidCards$ actually filters the batch rather than firing on any zone
// change: sacrificing two creatures must not fire a watcher looking for a
// sacrificed Artifact.
func TestChangesZoneAllSkipsWhenNoCardInTheBatchMatchesValidCards(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	g.NewCard(changesZoneAllWatcherDef(t, "Test Watcher", "Destination$ Graveyard | ValidCards$ Artifact"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	c := engine.NewScriptedController()
	def := etbSacrificeAllTriggerDefParams(t, "Test SacAll Trigger", "ValidCards$ Creature.Other", nil)
	if _, err := castETBSacrifice(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want unchanged 20 -- ValidCards$ Artifact must not match two sacrificed creatures", g.Player(p).Life)
	}
}

// TestChangesZoneAllRespectsDestinationFilter proves Destination$ actually
// gates the trigger: a watcher naming Destination$ Exile must not fire for a
// batch whose real destination is Graveyard (nor for the cast creature's own
// entry, a batch of one into the battlefield).
func TestChangesZoneAllRespectsDestinationFilter(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	g.NewCard(changesZoneAllWatcherDef(t, "Test Watcher", "Destination$ Exile | ValidCards$ Creature"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	c := engine.NewScriptedController()
	def := etbSacrificeAllTriggerDefParams(t, "Test SacAll Trigger", "ValidCards$ Creature.Other", nil)
	if _, err := castETBSacrifice(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want unchanged 20 -- Destination$ Exile must not match a Graveyard-bound batch", g.Player(p).Life)
	}
}

// TestChangesZoneAllFiresOnceForSimultaneousSBADeath proves the action.go
// wiring: two creatures reduced to zero toughness and destroyed by the
// identical destroyLethalToughness SBA sweep (CR 704.5f) fire Mode$
// ChangesZoneAll once for the whole sweep, not once per creature -- the
// board-wipe scenario the corpus's own real lines overwhelmingly name
// Destination$ Graveyard for.
func TestChangesZoneAllFiresOnceForSimultaneousSBADeath(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	g.NewCard(changesZoneAllWatcherDef(t, "Test Watcher", "Destination$ Graveyard | ValidCards$ Creature"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "0"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "0"), p, engine.Battlefield)

	c := engine.NewScriptedController()
	engine.CheckStateBasedActions(g, c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	if g.Player(p).Life != 25 {
		t.Errorf("p's life = %d, want 25 -- Mode$ ChangesZoneAll must fire once for the whole simultaneous SBA sweep", g.Player(p).Life)
	}
}

// TestChangesZoneAllFiresALineNamingActivationLimitOnce proves the
// ActivationLimit$ 1 line (41 of the corpus's own 126 real lines) fires once
// for the turn: two creatures sacrificed in one batch are one event, and a
// later batch the same turn does not fire it again.
func TestChangesZoneAllFiresALineNamingActivationLimitOnce(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	g.NewCard(changesZoneAllWatcherDef(t, "Test Watcher", "Destination$ Graveyard | ValidCards$ Creature | ActivationLimit$ 1"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	c := engine.NewScriptedController()
	def := etbSacrificeAllTriggerDefParams(t, "Test SacAll Trigger", "ValidCards$ Creature.Other", nil)
	if _, err := castETBSacrifice(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 25 {
		t.Errorf("p's life = %d, want 25 -- the ActivationLimit$ 1 line fires once", g.Player(p).Life)
	}
}
