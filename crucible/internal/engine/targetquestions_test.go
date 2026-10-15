package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Target questions this port cannot ask fail the ability loudly when it
// resolves; the ones it can ask are asked the way Java asks them.

// An unresolvable TargetMin$/TargetMax$ is not "no legal target": the ability
// goes on the stack and fails naming the param.
func TestUnresolvableTargetBoundsFailTheAbility(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct{ line, want string }{
		{"DB$ Pump | ValidTgts$ Creature | TargetMin$ Bogus | NumAtt$ +1", "TargetMin$"},
		{"DB$ Pump | ValidTgts$ Creature | TargetMin$ 0 | TargetMax$ Bogus | NumAtt$ +1", "TargetMax$"},
	} {
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		_, err := castETBChain(t, g, p, etbChainDef(t, "Bounds", tc.line), engine.NewScriptedController())
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: ResolveStack = %v, want an error naming %s", tc.line, err, tc.want)
		}
	}
}

// A TargetType$ past the literal Spell asks over every stack item: with no
// activated or triggered ability there, an up-to-one Counter has nothing to
// choose and does nothing; with a spell there, it counters it like any Counter.
func TestCounterSpellOrAbilityCountersASpell(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	bears := castBearsSpell(t, g, other)
	def := etbChainDef(t, "Disallower", "DB$ Counter | TargetType$ Spell,Activated,Triggered | ValidTgts$ Card")
	sub := def.Faces[0].Triggers[0].Subs[0].Ability
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.PushAbility(engine.Ability{API: engine.APICounter, Source: host, Controller: p, Params: sub,
		Targets: []engine.EntityID{engine.CardEntity(bears)}})
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(bears).Zone; got != engine.Graveyard {
		t.Errorf("Grizzly Bears is in %v, want countered into the graveyard", got)
	}
}

// With an activated or triggered ability on the stack that would be a legal
// target, the question is one this port cannot ask: the ability fails.
func TestCounterTargetingATriggeredAbilityFails(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	// A trigger of other's on the stack, under the Stifle-like ETB.
	trigHost := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	tdef := etbChainDef(t, "Trigger", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
	g.PushAbility(engine.Ability{API: engine.APIGainLife, Source: trigHost, Controller: other, Params: tdef.Faces[0].Triggers[0].Subs[0].Ability})
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	stifle := g.NewCard(corpusCard(t, "Stifle"), p, engine.Hand)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, stifle, c) {
		t.Fatal("could not cast Stifle with a trigger on the stack")
	}
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "targeting an ability on the stack not resolvable yet") {
		t.Fatalf("ResolveStack = %v, want the ability-target refusal", err)
	}
}

// With nothing on the stack, Stifle has no legal target and cannot be cast
// (CR 601.2c), the answer Java's candidate scan gives.
func TestStifleWithAnEmptyStackCannotBeCast(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	stifle := g.NewCard(corpusCard(t, "Stifle"), p, engine.Hand)
	if g.CastSpell(p, stifle, engine.NewScriptedController()) {
		t.Fatal("Stifle was cast over an empty stack")
	}
	if got := g.Card(stifle).Zone; got != engine.Hand {
		t.Errorf("Stifle is in %v, want hand", got)
	}
}

// TargetsWithDefinedController$ reads the dying creature's controller off the
// trigger: TriggeredCardOpponent (AbilityUtils.java:1038-1050) and
// "Player.OpponentOf <Defined>" (PlayerProperty.java:38-49) both offer only
// creatures of that controller's opponents.
func TestTargetsWithDefinedControllerReadsTheTrigger(t *testing.T) {
	t.Parallel()
	for _, spec := range []string{"TriggeredCardOpponent", "Player.OpponentOf TriggeredCardController"} {
		t.Run(spec, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			g.NewCard(scriptDef(t, "Death Watcher", "Enchantment",
				"T:Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Creature.YouCtrl | TriggerZones$ Battlefield | Execute$ TrigPut",
				"SVar:TrigPut:DB$ PutCounter | ValidTgts$ Creature | TargetsWithDefinedController$ "+spec+" | CounterType$ P1P1 | CounterNum$ 1"),
				p, engine.Battlefield)
			mine := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
			theirs := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
			dying := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
			bolt := g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
			g.Player(p).ManaPool.Add(mana.Red, 1)
			r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
			r.QueueTargets([]engine.EntityID{engine.CardEntity(dying)})
			r.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
			if !g.CastSpell(p, bolt, r) {
				t.Fatal("could not cast Lightning Bolt")
			}
			if err := g.ResolveStack(engine.NewRegistry(), r); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if len(r.offers) != 2 {
				t.Fatalf("offered %d target questions, want 2", len(r.offers))
			}
			if slices.Contains(r.offers[1], engine.CardEntity(mine)) || !slices.Contains(r.offers[1], engine.CardEntity(theirs)) {
				t.Errorf("trigger candidates = %v, want the opponent's creature only", r.offers[1])
			}
			if got := g.Card(theirs).Counters.Count(engine.P1P1); got != 1 {
				t.Errorf("the opposing creature has %d counters, want 1", got)
			}
		})
	}
}

// "OpponentOf <Defined>" outside the ability reading it has no trigger to
// read: unresolved, never guessed.
func TestOpponentOfTriggeredOutsideItsAbilityIsUnresolved(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	_, err := castETBChain(t, g, p, etbChainDef(t, "Opp Pump",
		"DB$ Pump | ValidTgts$ Creature | TargetsWithDefinedController$ Player.OpponentOf TriggeredPlayer | NumAtt$ +1"),
		engine.NewScriptedController())
	if err == nil || !strings.Contains(err.Error(), "TargetsWithDefinedController$") {
		t.Fatalf("ResolveStack = %v, want the TargetsWithDefinedController$ error", err)
	}
}

// TargetingPlayerControls$ at fizzle time reads the targeting player recorded
// when the targets were chosen (SpellAbility.java:1474-1476). An ability that
// recorded none and whose TargetingPlayer$ no longer names a player fails
// loudly instead of reading the controller.
func TestUnrecordedTargetingPlayerAtFizzleTimeFails(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	target := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	def := etbChainDef(t, "Controls", "DB$ Pump | ValidTgts$ Creature | TargetingPlayer$ TriggeredPlayer | TargetingPlayerControls$ True | NumAtt$ +1")
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.PushAbility(engine.Ability{API: engine.APIPump, Source: host, Controller: p, Params: def.Faces[0].Triggers[0].Subs[0].Ability,
		Targets: []engine.EntityID{engine.CardEntity(target)}})
	err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController())
	if err == nil {
		// The fizzle check has no error return; it leaves the error on the Game.
		err = g.TakePendingError()
	}
	if err == nil || !strings.Contains(err.Error(), "TargetingPlayer$") {
		t.Fatalf("ResolveStack = %v, want the TargetingPlayer$ error", err)
	}
	if got, _ := g.Card(target).Power(); got != 2 {
		t.Errorf("target power = %d, want 2: the illegal target is not pumped", got)
	}
}
