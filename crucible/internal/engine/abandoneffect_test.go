package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// resolveAbandonInCommand puts a host for trig (etbChainDef) directly into
// the Command zone -- resolveNow's own shape, minus the Battlefield zone,
// since every real DB$/AB$ Abandon line's host is a Command-zone Ongoing
// Scheme (AbandonEffect.java's own sa.getHostCard()).
func resolveAbandonInCommand(t *testing.T, g *engine.Game, p engine.PlayerID, c *engine.ScriptedController, trig string, svars ...string) (engine.CardID, error) {
	t.Helper()
	def := etbChainDef(t, "Test Scheme", trig, svars...)
	host := g.NewCard(def, p, engine.Command)
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		api, ok := engine.APIByName(sub.Ability.Name)
		if !ok {
			t.Fatalf("unknown API %q", sub.Ability.Name)
		}
		g.PushAbility(engine.Ability{API: api, Source: host, Controller: p, Params: sub.Ability, Amounts: face.Amounts})
		return host, g.ResolveStack(engine.NewRegistry(), c)
	}
	t.Fatal("no Execute$")
	return 0, nil
}

// TestAbandonEffectMovesCommandCardToSchemeDeckAndRemembers proves the
// dominant bare `DB$ Abandon` shape (14 of the corpus's 21 real lines):
// AbandonEffect.java's own controller.getZone(Command).remove(source);
// controller.getZone(SchemeDeck).add(source), plus RememberAbandoned$
// (source.addRemembered(source)).
func TestAbandonEffectMovesCommandCardToSchemeDeckAndRemembers(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	scheme, err := resolveAbandonInCommand(t, g, p, engine.NewScriptedController(),
		"DB$ Abandon | RememberAbandoned$ True")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(scheme).Zone; got != engine.SchemeDeck {
		t.Errorf("scheme zone = %v, want SchemeDeck", got)
	}
	if !containsEntity(g.Card(scheme).Memory.Remembered(), engine.CardEntity(scheme)) {
		t.Error("RememberAbandoned$ True must remember the abandoned scheme on itself")
	}
}

// TestAbandonEffectOptionalDeclinedLeavesSchemeInCommand proves Optional$
// asks controller.ConfirmEffect (Java's confirmAction) first, and a
// declined answer abandons nothing.
func TestAbandonEffectOptionalDeclinedLeavesSchemeInCommand(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	scheme, err := resolveAbandonInCommand(t, g, p, c, "DB$ Abandon | Optional$ True")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(scheme).Zone; got != engine.Command {
		t.Errorf("scheme zone = %v, want Command -- a declined Optional$ must abandon nothing", got)
	}
}

// TestAbandonEffectOptionalAcceptedAbandons proves the accepted half of the
// same Optional$ branch.
func TestAbandonEffectOptionalAcceptedAbandons(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)
	scheme, err := resolveAbandonInCommand(t, g, p, c, "DB$ Abandon | Optional$ True")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(scheme).Zone; got != engine.SchemeDeck {
		t.Errorf("scheme zone = %v, want SchemeDeck", got)
	}
}

// TestAbandonEffectConditionUnmetLeavesSchemeInCommand proves
// subAbilityConditionMet (condition.go) gates the whole ability before
// this file's own body runs -- ConditionCheckSVar$/ConditionSVarCompare$
// never reach a special case here.
func TestAbandonEffectConditionUnmetLeavesSchemeInCommand(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	scheme, err := resolveAbandonInCommand(t, g, p, engine.NewScriptedController(),
		"DB$ Abandon | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", "X", "0")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(scheme).Zone; got != engine.Command {
		t.Errorf("scheme zone = %v, want Command -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 fails (X is 0)", got)
	}
}

// TestAbandonEffectHostNotInCommandIsNoOp proves the host-not-in-Command
// guard: a DB$ Abandon chained after some other effect moved its own host
// out of the Command zone first must not move it again or fire the
// Abandoned trigger a second time.
func TestAbandonEffectHostNotInCommandIsNoOp(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := etbChainDef(t, "Test Scheme", "DB$ Abandon")
	scheme := g.NewCard(def, p, engine.Battlefield)
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		api, ok := engine.APIByName(sub.Ability.Name)
		if !ok {
			t.Fatalf("unknown API %q", sub.Ability.Name)
		}
		g.PushAbility(engine.Ability{API: api, Source: scheme, Controller: p, Params: sub.Ability, Amounts: face.Amounts})
		if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
	}
	if got := g.Card(scheme).Zone; got != engine.Battlefield {
		t.Errorf("scheme zone = %v, want Battlefield -- Abandon on a non-Command host must be a no-op", got)
	}
}

// TestAbandonEffectFiresWatchingAbandonedTrigger proves Mode$ Abandoned
// (checkAbandonedTriggers, trigger.go) fires for a watching permanent that
// remembered the scheme -- the shape a Command-zone Effect card built by
// DB$ Effect (RememberObjects$ Self, effecteffect.go) would use, minus its
// own Static$ True: bow_to_my_command.txt's own real TrigAbandoned line
// carries Static$ True and runs inline instead (TestAbandonEffectRunsStaticTriggerInline),
// so this synthetic non-static watcher is what exercises the walk itself.
func TestAbandonEffectFiresWatchingAbandonedTrigger(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "Test Watcher"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Test Watcher"
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "1", "1"
	raw.Faces[0].ManaCost = mana.MustParse("G")
	raw.Faces[0].Triggers = []string{
		"Mode$ Abandoned | ValidCard$ Card.IsRemembered | Execute$ TrigGain",
	}
	raw.Faces[0].SVars.Set("TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
	watcherDef, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	watcher := g.NewCard(watcherDef, p, engine.Battlefield)

	// A second watcher, identically triggered, but never told to remember
	// anything -- ValidCard$ Card.IsRemembered must fail its own check and
	// skip this one (checkAbandonedTriggers' own ValidCard$ branch).
	nonWatcherDef, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	g.NewCard(nonWatcherDef, p, engine.Battlefield)

	scheme, err := resolveAbandonInCommandRemembered(t, g, p, watcher, engine.NewScriptedController())
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(scheme).Zone; got != engine.SchemeDeck {
		t.Errorf("scheme zone = %v, want SchemeDeck", got)
	}
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("p's life = %d, want 21 -- the watcher's own Mode$ Abandoned trigger must fire", got)
	}
}

// TestAbandonEffectRunsStaticTriggerInline proves Static$ True resolves at
// once, off the stack -- bow_to_my_command.txt's own TrigAbandoned shape
// (TriggerHandler.java:301-307).
func TestAbandonEffectRunsStaticTriggerInline(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "Test Static Watcher"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Test Static Watcher"
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "1", "1"
	raw.Faces[0].ManaCost = mana.MustParse("G")
	raw.Faces[0].Triggers = []string{
		"Mode$ Abandoned | ValidCard$ Card.IsRemembered | Static$ True | Execute$ TrigGain",
	}
	raw.Faces[0].SVars.Set("TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
	watcherDef, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	watcher := g.NewCard(watcherDef, p, engine.Battlefield)

	scheme, err := resolveAbandonInCommandRemembered(t, g, p, watcher, engine.NewScriptedController())
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(scheme).Zone; got != engine.SchemeDeck {
		t.Errorf("scheme zone = %v, want SchemeDeck", got)
	}
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("p's life = %d, want 21 -- a Static$ True trigger resolves inline", got)
	}
	if got := g.StackLen(); got != 0 {
		t.Errorf("stack holds %d items, want none: the trigger never used the stack", got)
	}
}

// resolveAbandonInCommandRemembered is
// TestAbandonEffectFiresWatchingAbandonedTrigger's own setup: builds the
// scheme host, has watcher remember it (Card.IsRemembered's own precondition,
// valid.go), then resolves a bare DB$ Abandon against the scheme.
func resolveAbandonInCommandRemembered(t *testing.T, g *engine.Game, p engine.PlayerID, watcher engine.CardID, c *engine.ScriptedController) (engine.CardID, error) {
	t.Helper()
	def := etbChainDef(t, "Test Scheme", "DB$ Abandon")
	scheme := g.NewCard(def, p, engine.Command)
	g.Card(watcher).Memory.Remember(engine.CardEntity(scheme))
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		api, ok := engine.APIByName(sub.Ability.Name)
		if !ok {
			t.Fatalf("unknown API %q", sub.Ability.Name)
		}
		g.PushAbility(engine.Ability{API: api, Source: scheme, Controller: p, Params: sub.Ability, Amounts: face.Amounts})
		return scheme, g.ResolveStack(engine.NewRegistry(), c)
	}
	t.Fatal("no Execute$")
	return 0, nil
}

// containsEntity reports whether id is among es -- Memory.Remembered's own
// []EntityID has no membership test exported, so tests build their own tiny
// one rather than reaching into the package's internal containsEntity.
func containsEntity(es []engine.EntityID, id engine.EntityID) bool {
	for _, e := range es {
		if e == id {
			return true
		}
	}
	return false
}
