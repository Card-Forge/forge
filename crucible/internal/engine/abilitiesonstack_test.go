package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Abilities on the stack as targets (ADR-0041): Stifle and its kin offer an
// activated or triggered ability as its stack item, counter it without moving
// any card, and fizzle once it has left the stack.

// lifeEngine is an artifact whose one activated ability gains its controller 2
// life for {1}.
func lifeEngine(t *testing.T) *compile.Card {
	t.Helper()
	return scriptDef(t, "Life Engine", "Artifact", "A:AB$ GainLife | Cost$ 1 | Defined$ You | LifeAmount$ 2")
}

// activateFor activates card's first ability for pid with one colorless mana
// and returns the stack item it became.
func activateFor(t *testing.T, g *engine.Game, pid engine.PlayerID, card engine.CardID, c *engine.ScriptedController) engine.EntityID {
	t.Helper()
	g.Player(pid).ManaPool.AddColorless(1)
	c.QueuePayGeneric(mana.ShardC)
	if !g.ActivateAbility(pid, card, 0, c) {
		t.Fatal("ActivateAbility failed")
	}
	top, ok := g.StackTop()
	if !ok || top.Source != card {
		t.Fatal("the activated ability is not on top of the stack")
	}
	return engine.StackItemEntity(top.ID)
}

// castWatcherTrigger puts other's "whenever a creature spell is cast, gain 3
// life" trigger on the stack above p's Grizzly Bears spell, and returns the
// trigger's stack item and the Bears.
func castWatcherTrigger(t *testing.T, g *engine.Game, p, other engine.PlayerID, c *engine.ScriptedController) (engine.EntityID, engine.CardID) {
	t.Helper()
	watcher := g.NewCard(scriptDef(t, "Cast Watcher", "Enchantment",
		"T:Mode$ SpellCast | ValidCard$ Creature | TriggerZones$ Battlefield | Execute$ TrigGain",
		"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 3"), other, engine.Battlefield)
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 2)
	c.QueuePayGeneric(mana.ShardG)
	if !g.CastSpell(p, bears, c) {
		t.Fatal("could not cast Grizzly Bears")
	}
	top, ok := g.StackTop()
	if !ok || top.Source != watcher {
		t.Fatal("the cast trigger is not on top of the stack")
	}
	return engine.StackItemEntity(top.ID), bears
}

// castCounter casts name for p with mana -- its colored pips plus at most one
// generic, paid with colorless -- queuing targets, and resolves the whole
// stack.
func castCounter(t *testing.T, g *engine.Game, p engine.PlayerID, name string, r *stackOfferRecorder, targets []engine.EntityID, pool ...mana.Colors) error {
	t.Helper()
	for _, col := range pool {
		g.Player(p).ManaPool.Add(col, 1)
	}
	g.Player(p).ManaPool.AddColorless(1)
	r.QueuePayGeneric(mana.ShardC)
	spell := g.NewCard(corpusCard(t, name), p, engine.Hand)
	if targets != nil {
		r.QueueTargets(targets)
	}
	if !g.CastSpell(p, spell, r) {
		t.Fatalf("could not cast %s", name)
	}
	return g.ResolveStack(engine.NewRegistry(), r)
}

// Stifle counters an activated ability: it is offered as its stack item, its
// effect never happens, and its host stays where it is.
func TestStifleCountersAnActivatedAbility(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	art := g.NewCard(lifeEngine(t), other, engine.Battlefield)
	r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
	item := activateFor(t, g, other, art, r.ScriptedController)
	if err := castCounter(t, g, p, "Stifle", r, []engine.EntityID{item}, mana.Blue); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if len(r.offers) != 1 || !slices.Equal(r.offers[0], []engine.EntityID{item}) {
		t.Errorf("Stifle offered %v, want just the activated ability %v", r.offers, item)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("the countered ability's controller has %d life, want 20", got)
	}
	if g.StackLen() != 0 || g.Card(art).Zone != engine.Battlefield {
		t.Errorf("stack has %d items, artifact in %v; want an empty stack and the artifact untouched", g.StackLen(), g.Card(art).Zone)
	}
}

// Stifle counters a real triggered ability (a SpellCast trigger above the
// creature spell that caused it); the spell itself still resolves.
func TestStifleCountersATriggeredAbility(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
	trig, bears := castWatcherTrigger(t, g, p, other, r.ScriptedController)
	if err := castCounter(t, g, p, "Stifle", r, []engine.EntityID{trig}, mana.Blue); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("the trigger's controller has %d life, want 20: the trigger was countered", got)
	}
	if got := g.Card(bears).Zone; got != engine.Battlefield {
		t.Errorf("Grizzly Bears is in %v, want it resolved onto the battlefield", got)
	}
}

// Bind names Activated only: a trigger is not a legal target, so with only a
// trigger on the stack Bind cannot be cast (CR 601.2c).
func TestBindDoesNotOfferATriggeredAbility(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	castWatcherTrigger(t, g, p, other, engine.NewScriptedController())
	g.Player(p).ManaPool.Add(mana.Green, 1)
	g.Player(p).ManaPool.AddColorless(1)
	bind := g.NewCard(corpusCard(t, "Bind"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardC)
	if g.CastSpell(p, bind, c) {
		t.Fatal("Bind was cast with only a triggered ability on the stack")
	}
}

// Two Binds on one activated ability: the first to resolve counters it and
// draws, the second finds its only target gone and fizzles without drawing
// (CR 608.2b, MagicStack.java:753-755).
func TestCounterOfAnAbilityThatLeftTheStackFizzles(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	for range 3 {
		g.NewCard(corpusCard(t, "Forest"), p, engine.Library)
	}
	art := g.NewCard(lifeEngine(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	item := activateFor(t, g, other, art, c)
	for range 2 {
		g.Player(p).ManaPool.Add(mana.Green, 1)
		g.Player(p).ManaPool.AddColorless(1)
		bind := g.NewCard(corpusCard(t, "Bind"), p, engine.Hand)
		c.QueueTargets([]engine.EntityID{item})
		c.QueuePayGeneric(mana.ShardC)
		if !g.CastSpell(p, bind, c) {
			t.Fatal("could not cast Bind")
		}
	}
	hand := g.Zone(engine.Hand, p).Len()
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Zone(engine.Hand, p).Len() - hand; got != 1 {
		t.Errorf("drew %d cards, want 1: the second Bind fizzles", got)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("other has %d life, want 20", got)
	}
}

// Deny the Witch's LoseLife reads the countered ability's controller after
// the ability left the stack: Java's target is the SpellAbility object, which
// keeps its activating player (AbilityUtils.java:956-962).
func TestDenyTheWitchDrainsTheCounteredAbilitysController(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	art := g.NewCard(lifeEngine(t), other, engine.Battlefield)
	r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
	item := activateFor(t, g, other, art, r.ScriptedController)
	if err := castCounter(t, g, p, "Deny the Witch", r, []engine.EntityID{item}, mana.White, mana.Blue, mana.Black); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 18 {
		t.Errorf("other has %d life, want 18: countered, then 2 life lost", got)
	}
}

// Trickbind remembers the countered ability's host (RememberCountered$,
// CounterEffect.java:109-110), and its Effect stops that permanent's activated
// abilities for the turn.
func TestTrickbindStopsTheHostsAbilities(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	art := g.NewCard(lifeEngine(t), other, engine.Battlefield)
	r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
	item := activateFor(t, g, other, art, r.ScriptedController)
	if err := castCounter(t, g, p, "Trickbind", r, []engine.EntityID{item}, mana.Blue); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	g.Player(other).ManaPool.AddColorless(1)
	if g.ActivateAbility(other, art, 0, r.ScriptedController) {
		t.Error("the artifact's ability was activated again after Trickbind")
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("other has %d life, want 20", got)
	}
}

// Teferi's Response: an opponent's ability that targets a land you control is
// a legal target (TargetValidTargeting$), countering it destroys its host
// (DestroyPermanent$, CounterEffect.java:105-107), and you draw two.
func TestTeferisResponseCountersAndDestroysTheSource(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	for range 3 {
		g.NewCard(corpusCard(t, "Forest"), p, engine.Library)
	}
	land := g.NewCard(corpusCard(t, "Forest"), p, engine.Battlefield)
	wrecker := g.NewCard(scriptDef(t, "Land Wrecker", "Artifact", "A:AB$ Destroy | Cost$ 1 | ValidTgts$ Land"), other, engine.Battlefield)
	r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
	r.QueueTargets([]engine.EntityID{engine.CardEntity(land)})
	item := activateFor(t, g, other, wrecker, r.ScriptedController)
	hand := g.Zone(engine.Hand, p).Len()
	if err := castCounter(t, g, p, "Teferi's Response", r, []engine.EntityID{item}, mana.Blue); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(land).Zone; got != engine.Battlefield {
		t.Errorf("the land is in %v, want it kept", got)
	}
	if got := g.Card(wrecker).Zone; got != engine.Graveyard {
		t.Errorf("the ability's source is in %v, want it destroyed", got)
	}
	if got := g.Zone(engine.Hand, p).Len() - hand; got != 2 {
		t.Errorf("drew %d, want 2", got)
	}
}

// An ability that targets nothing of yours is not a legal Teferi's Response
// target, so it cannot be cast.
func TestTeferisResponseNeedsAnAbilityTargetingYourLand(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	art := g.NewCard(lifeEngine(t), other, engine.Battlefield)
	activateFor(t, g, other, art, engine.NewScriptedController())
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	g.Player(p).ManaPool.AddColorless(1)
	resp := g.NewCard(corpusCard(t, "Teferi's Response"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardC)
	if g.CastSpell(p, resp, c) {
		t.Fatal("Teferi's Response was cast with no ability targeting a land")
	}
}

// Consign to Memory (Spell.Colorless,Triggered): the Colorless property is the
// spell's host card's (SpellAbilityProperty.java:319-320), so a green spell is
// not offered while the trigger it caused is.
func TestConsignToMemoryReadsTheHostCardsColor(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
	trig, bears := castWatcherTrigger(t, g, p, other, r.ScriptedController)
	if err := castCounter(t, g, p, "Consign to Memory", r, []engine.EntityID{trig}, mana.Blue); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if len(r.offers) == 0 || slices.Contains(r.offers[0], engine.CardEntity(bears)) || !slices.Contains(r.offers[0], trig) {
		t.Errorf("offered %v, want the trigger and not the green spell", r.offers)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("other has %d life, want 20", got)
	}
}

// Spider-Punk's "spells and abilities can't be countered" (ValidSA$
// Spell,Activated,Triggered) stops Stifle: the ability still resolves.
func TestSpiderPunkStopsCounteringAnAbility(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Spider-Punk"), other, engine.Battlefield)
	art := g.NewCard(lifeEngine(t), other, engine.Battlefield)
	r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
	item := activateFor(t, g, other, art, r.ScriptedController)
	if err := castCounter(t, g, p, "Stifle", r, []engine.EntityID{item}, mana.Blue); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 22 {
		t.Errorf("other has %d life, want 22: the ability could not be countered", got)
	}
}

// A Counter replacement whose ValidSA$ puts a property on an ability kind is
// not read: Stifle fails naming the line rather than countering past it.
func TestUnreadAbilityCounterReplacementIsAnError(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(scriptDef(t, "Odd Shield", "Enchantment",
		"R:Event$ Counter | ValidSA$ Activated.YouCtrl | Layer$ CantHappen | ActiveZones$ Battlefield"), other, engine.Battlefield)
	art := g.NewCard(lifeEngine(t), other, engine.Battlefield)
	r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
	item := activateFor(t, g, other, art, r.ScriptedController)
	err := castCounter(t, g, p, "Stifle", r, []engine.EntityID{item}, mana.Blue)
	if err == nil || !strings.Contains(err.Error(), "Odd Shield") {
		t.Fatalf("ResolveStack = %v, want the Odd Shield replacement refused", err)
	}
}

// Host-card properties on a spell alternative (Tale's End's Spell.Legendary,
// Louisoix's Sacrifice's Spell.nonCreature) rule out a nonlegendary creature
// spell while the activated ability stays a candidate.
func TestHostCardTypePropertiesFilterSpells(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	art := g.NewCard(lifeEngine(t), other, engine.Battlefield)
	r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 2)
	r.QueuePayGeneric(mana.ShardG)
	if !g.CastSpell(p, bears, r) {
		t.Fatal("could not cast Grizzly Bears")
	}
	item := activateFor(t, g, other, art, r.ScriptedController)
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	spell := g.NewCard(instantDefWithAbility(t, "Picky Counter", "U",
		"SP$ Counter | TargetType$ Spell.Legendary,Spell.nonCreature,Activated | ValidTgts$ Card"), p, engine.Hand)
	r.QueueTargets([]engine.EntityID{item})
	if !g.CastSpell(p, spell, r) {
		t.Fatal("could not cast the counter")
	}
	if err := g.ResolveStack(engine.NewRegistry(), r); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if len(r.offers) != 1 || !slices.Equal(r.offers[0], []engine.EntityID{item}) {
		t.Errorf("offered %v, want only the activated ability", r.offers)
	}
}

// Targeting an ability an opponent controls is a crime (MagicStack.java:
// 1041-1044): a Mode$ CommitCrime watcher of the caster triggers.
func TestTargetingAnOpponentsAbilityIsACrime(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(scriptDef(t, "Crime Watcher", "Enchantment",
		"T:Mode$ CommitCrime | ValidPlayer$ You | TriggerZones$ Battlefield | Execute$ TrigGain",
		"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 5"), p, engine.Battlefield)
	art := g.NewCard(lifeEngine(t), other, engine.Battlefield)
	r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
	item := activateFor(t, g, other, art, r.ScriptedController)
	if err := castCounter(t, g, p, "Stifle", r, []engine.EntityID{item}, mana.Blue); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 25 {
		t.Errorf("p has %d life, want 25: Stifle on an opponent's ability is a crime", got)
	}
}

// Reroute changes the single target of an activated ability (ChangeTargets
// over a stack item): the ping lands on the new target.
func TestRerouteChangesAnActivatedAbilitysTarget(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	for range 2 {
		g.NewCard(corpusCard(t, "Forest"), p, engine.Library)
	}
	mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	pinger := g.NewCard(scriptDef(t, "Pinger", "Artifact", "A:AB$ DealDamage | Cost$ 1 | ValidTgts$ Creature | NumDmg$ 1"), other, engine.Battlefield)
	r := &stackOfferRecorder{ScriptedController: engine.NewScriptedController()}
	r.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	item := activateFor(t, g, other, pinger, r.ScriptedController)
	r.QueueTargets([]engine.EntityID{item})
	r.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)}) // the new target
	if err := castCounter(t, g, p, "Reroute", r, nil, mana.Red); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(mine).Zone != engine.Battlefield || g.Card(theirs).Zone != engine.Graveyard {
		t.Errorf("mine in %v, theirs in %v; want the ping moved onto theirs", g.Card(mine).Zone, g.Card(theirs).Zone)
	}
}

// Copying an activated or triggered ability is not built: Strionic
// Resonator's copy fails loudly rather than copying it as a spell.
func TestCopyingATriggeredAbilityIsRefused(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	res := g.NewCard(corpusCard(t, "Strionic Resonator"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	trig, _ := castWatcherTrigger(t, g, p, other, c)
	g.Player(other).ManaPool.AddColorless(2)
	c.QueuePayGeneric(mana.ShardC)
	c.QueuePayGeneric(mana.ShardC)
	c.QueueTargets([]engine.EntityID{trig})
	if !g.ActivateAbility(other, res, 0, c) {
		t.Fatal("could not activate Strionic Resonator over a trigger")
	}
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "CopySpellAbility") {
		t.Fatalf("ResolveStack = %v, want the CopySpellAbility refusal", err)
	}
}

// A TargetType$ property neither the ability nor the host-card fallback reads
// is an error, not a false (GO-7).
func TestUnknownAbilityTargetPropertyIsAnError(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	art := g.NewCard(lifeEngine(t), other, engine.Battlefield)
	activateFor(t, g, other, art, engine.NewScriptedController())
	err := castInstantLine(t, g, p, "SP$ Counter | TargetType$ Activated.Bogus | ValidTgts$ Card")
	if err == nil || !strings.Contains(err.Error(), "Bogus") {
		t.Fatalf("ResolveStack = %v, want the Bogus property refused", err)
	}
}

// An ability whose host is an effect card is matched against a ValidTgts$
// past a bare Card only through its effect source, which is not recorded: an
// error, never a guess.
func TestAbilityOfAnEffectCardAgainstATypedSpecIsAnError(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	eff := g.NewCard(scriptDef(t, "Effect Host", "Artifact"), other, engine.Command)
	g.Card(eff).IsEffect = true
	g.PushAbility(engine.Ability{API: engine.APIGainLife, Source: eff, Controller: other,
		Params: etbChainDef(t, "Gain", "DB$ GainLife | Defined$ You | LifeAmount$ 1").Faces[0].Triggers[0].Subs[0].Ability})
	err := castInstantLine(t, g, p, "SP$ Counter | TargetType$ Activated | ValidTgts$ Artifact")
	if err == nil || !strings.Contains(err.Error(), "effect card") {
		t.Fatalf("ResolveStack = %v, want the effect-source refusal", err)
	}
}

// castInstantLine casts a {U} instant whose spell is line for p and resolves
// the stack. A target question the engine cannot ask fails at resolution, so
// nothing is queued.
func castInstantLine(t *testing.T, g *engine.Game, p engine.PlayerID, line string) error {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	spell := g.NewCard(instantDefWithAbility(t, "Test Counter", "U", line), p, engine.Hand)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, spell, c) {
		t.Fatal("could not cast the instant")
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}
