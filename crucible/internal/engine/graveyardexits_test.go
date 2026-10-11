package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These tests cover the zone changes that run ChangesZone outside
// moveByEffect (GameAction.java:601-613 runs it for every one): a card leaving
// a graveyard as a cost or a cast, and a permanent a Moved replacement sends
// to its owner's library instead of the graveyard.

// graveyardLeaveWatcher is an Enchantment gaining its controller 1 life
// whenever a card matching validCard leaves its owner's graveyard.
func graveyardLeaveWatcher(t *testing.T, validCard string) *compile.Card {
	t.Helper()
	return triggerWatcherDef(t, "Test Graveyard Watcher",
		"Mode$ ChangesZone | Origin$ Graveyard | Destination$ Any | ValidCard$ "+validCard+" | TriggerZones$ Battlefield | Execute$ Gain",
		"Gain", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
}

// delveSorceryDef is a {4}{U} Delve sorcery: "You gain 5 life."
func delveSorceryDef(t *testing.T) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: "Test Delve"}
	f := &raw.Faces[0]
	f.Present = true
	f.Name = "Test Delve"
	f.Type = cardtype.Parse(attachmentTypeRegistry(t), "Sorcery")
	f.ManaCost = mana.MustParse("4 U")
	f.Keywords = []string{"Delve"}
	f.Abilities = []string{"SP$ GainLife | Defined$ You | LifeAmount$ 5"}
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	return def
}

// An ExileFromGrave<1/CARDNAME> cost takes its card out of the graveyard: a
// watcher's graveyard-origin trigger fires. Paying for an activated ability,
// it goes on the stack as the cost is paid, under the ability.
func TestExileFromGraveCostFiresGraveyardLeaveTriggers(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	watcher := g.NewCard(graveyardLeaveWatcher(t, "Creature.YouOwn"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefWithAbility(t, "Test Exile From Grave",
		"AB$ GainLife | Cost$ ExileFromGrave<1/CARDNAME> | ActivationZone$ Graveyard | Defined$ You | LifeAmount$ 3"), p, engine.Graveyard)

	c := engine.NewScriptedController()
	if !g.ActivateAbility(p, creature, 0, c) {
		t.Fatal("ActivateAbility returned false")
	}
	if g.StackLen() != 2 {
		t.Fatalf("StackLen() = %d, want 2 (the ability over the watcher's trigger)", g.StackLen())
	}
	if top, _ := g.StackTop(); top.Source != creature {
		t.Errorf("stack top source = %v, want the activated ability's %v", top.Source, creature)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life = %d, want 24 (3 from the ability, 1 from %v's trigger)", got, watcher)
	}
}

// Delve exiles cards from the graveyard while the spell is cast; their
// triggers wait for the cast and go on the stack above the spell
// (TriggerHandler.java:264).
func TestDelveExileTriggersGoAboveTheSpell(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	watcher := g.NewCard(graveyardLeaveWatcher(t, "Card.YouOwn"), p, engine.Battlefield)
	spell := g.NewCard(delveSorceryDef(t), p, engine.Hand)
	a := g.NewCard(creatureDef(t), p, engine.Graveyard)
	b := g.NewCard(creatureDef(t), p, engine.Graveyard)
	g.Player(p).ManaPool.Add(mana.Blue, 3)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{a, b})
	c.QueuePayGeneric(mana.ShardU)
	c.QueuePayGeneric(mana.ShardU)
	if !g.CastSpell(p, spell, c) {
		t.Fatal("CastSpell failed")
	}
	if g.Card(a).Zone != engine.Exile || g.Card(b).Zone != engine.Exile {
		t.Fatalf("delved cards in %v and %v, want Exile", g.Card(a).Zone, g.Card(b).Zone)
	}
	if g.StackLen() != 3 {
		t.Fatalf("StackLen() = %d, want 3 (the spell under two triggers)", g.StackLen())
	}
	if top, _ := g.StackTop(); top.Source != watcher {
		t.Errorf("stack top source = %v, want the watcher %v", top.Source, watcher)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 27 {
		t.Errorf("life = %d, want 27", got)
	}
}

// A cast from the graveyard (a flashback-style alternative here: a MayPlay
// grant is not needed, the card's own Flashback keyword is) moves the card to
// the stack: the graveyard-origin trigger fires with the cast's batch, above
// the spell.
func TestCastFromGraveyardFiresGraveyardLeaveTriggers(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	watcher := g.NewCard(graveyardLeaveWatcher(t, "Instant.YouOwn"), p, engine.Battlefield)
	geistflame := g.NewCard(corpusCard(t, "Geistflame"), p, engine.Graveyard)
	g.Player(p).ManaPool.Add(mana.Red, 4)

	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardR)
	c.QueuePayGeneric(mana.ShardR)
	c.QueuePayGeneric(mana.ShardR)
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if !g.CastSpell(p, geistflame, c) {
		t.Fatal("CastSpell from the graveyard failed")
	}
	if top, _ := g.StackTop(); top.Source != watcher {
		t.Errorf("stack top source = %v, want the watcher %v", top.Source, watcher)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("life = %d, want 21", got)
	}
}

// Erebos's Titan watches from its owner's graveyard (TriggerZones$
// Graveyard): an opponent's creature card leaving their graveyard lets its
// owner discard a card to return it to hand. Raise Dead's return of Golgari
// Brownscale fires both the Titan and Brownscale's own trigger; APNAP puts
// the non-active player's Titan trigger on top.
func TestGraveyardWatcherSeesAnOpponentsCreatureLeave(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	raise := g.NewCard(corpusCard(t, "Raise Dead"), p, engine.Hand)
	brownscale := g.NewCard(corpusCard(t, "Golgari Brownscale"), p, engine.Graveyard)
	titan := g.NewCard(corpusCard(t, "Erebos's Titan"), other, engine.Graveyard)
	island := g.NewCard(corpusCard(t, "Island"), other, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Black, 1)

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(brownscale)})
	c.QueueConfirmOptionalTrigger(true)
	c.QueueDiscardChoice([]engine.CardID{island})
	if !g.CastSpell(p, raise, c) {
		t.Fatal("CastSpell failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if z := g.Card(titan).Zone; z != engine.Hand {
		t.Errorf("Erebos's Titan in %v, want Hand", z)
	}
	if z := g.Card(island).Zone; z != engine.Graveyard {
		t.Errorf("discarded Island in %v, want Graveyard", z)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22 from Brownscale's own trigger", got)
	}
}

// A Moved replacement that sends a dying permanent to its owner's library
// (Progenitus' shape) makes it leave the battlefield for the library: its own
// "leaves the battlefield" trigger fires there (CR 603.6d), where before only
// an exile redirect did.
func TestMovedToLibraryInsteadOfDyingFiresLeaveTriggers(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := scriptDef(t, "Test Tuck", "Creature",
		"R:Event$ Moved | ActiveZones$ Battlefield | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Card.Self | ReplaceWith$ ToLib",
		"SVar:ToLib:DB$ ChangeZone | Hidden$ True | Origin$ All | Destination$ Library | Defined$ ReplacedCard | LibraryPosition$ 0",
		"T:Mode$ ChangesZone | Origin$ Battlefield | Destination$ Any | ValidCard$ Card.Self | Execute$ Gain",
		"SVar:Gain:DB$ GainLife | Defined$ You | LifeAmount$ 2")
	creature := g.NewCard(def, p, engine.Battlefield)
	g.Card(creature).Damage.Mark(1, false)

	c := engine.NewScriptedController()
	engine.CheckStateBasedActions(g, c)
	if z := g.Card(creature).Zone; z != engine.Library {
		t.Fatalf("zone = %v, want Library", z)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22: the leaves trigger did not fire", got)
	}
}

// MoveToLibraryTop freezes last-known information as Move does: a watcher's
// ValidCard$ reads the counters the permanent left with.
func TestTuckToLibraryTopReadsLastKnownCounters(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	watcher := triggerWatcherDef(t, "Test Tuck Watcher",
		"Mode$ ChangesZone | Origin$ Battlefield | Destination$ Library | ValidCard$ Creature.counters_GE1_P1P1 | TriggerZones$ Battlefield | Execute$ Gain",
		"Gain", "DB$ GainLife | Defined$ You | LifeAmount$ 4")
	g.NewCard(watcher, p, engine.Battlefield)
	creature := g.NewCard(creatureDef(t), p, engine.Battlefield)
	g.Card(creature).Counters.Add(engine.P1P1, 1)

	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(creature)},
		"DB$ ChangeZone | ValidTgts$ Creature | Origin$ Battlefield | Destination$ Library | LibraryPosition$ 0"); err != nil {
		t.Fatal(err)
	}
	if z := g.Card(creature).Zone; z != engine.Library {
		t.Fatalf("zone = %v, want Library", z)
	}
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life = %d, want 24: the watcher did not see the counter the creature left with", got)
	}
}
