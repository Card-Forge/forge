package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// wantX fails t unless the stack's top item announced X = want (CR
// 601.2b), or announced none when announced is false.
func wantX(t *testing.T, g *engine.Game, want int, announced bool) {
	t.Helper()
	top, ok := g.StackTop()
	if !ok {
		t.Fatal("stack empty, want the paid ability on top")
	}
	got, gotAnnounced := top.XManaCostPaid()
	if got != want || gotAnnounced != announced {
		t.Errorf("XManaCostPaid() = (%d, %v), want (%d, %v)", got, gotAnnounced, want, announced)
	}
}

// queueGeneric queues n answers of shard for ChoosePayGeneric.
func queueGeneric(c *engine.ScriptedController, shard mana.Shard, n int) {
	for range n {
		c.QueuePayGeneric(shard)
	}
}

// A permanent spell with {X} in its cost keeps the X its caster announced
// (PlaySpellAbility.java:462-465, ability.setXManaCostPaid) on its stack
// item, where ChangeX and a Count$xPaid reader find it.
func TestCastPermanentSpellRecordsAnnouncedX(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.Player(p).ManaPool.Add(mana.Green, 4)
	creature := g.NewCard(creatureDefManaCost(t, "X G"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueuePayX(3)
	queueGeneric(c, mana.ShardG, 3)

	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting {X}{G} with X=3 and four green")
	}
	wantX(t, g, 3, true)
}

// A cost with no {X} announces none: Java's xManaCostPaid stays null, the
// value ChangeXEffect.java refuses to overwrite.
func TestCastSpellWithoutXAnnouncesNone(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.Player(p).ManaPool.Add(mana.Red, 1)
	creature := g.NewCard(creatureDefManaCost(t, "R"), p, engine.Hand)
	c := engine.NewScriptedController()

	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting {R} with one red")
	}
	wantX(t, g, 0, false)
}

// X = 0 is announced, not absent: the cost carried an X symbol.
func TestCastSpellWithXZeroAnnouncesZero(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(creatureDefManaCost(t, "X G"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueuePayX(0)

	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting {X}{G} with X=0")
	}
	wantX(t, g, 0, true)
}

// An Instant's cast path (castInstantOrSorcery) records X too.
func TestCastInstantRecordsAnnouncedX(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.Player(p).ManaPool.Add(mana.White, 3)
	spell := g.NewCard(gainInstant(t, "Gain X", "X W", "1"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueuePayX(2)
	queueGeneric(c, mana.ShardW, 2)

	if !g.CastSpell(p, spell, c) {
		t.Fatal("CastSpell failed casting {X}{W} with X=2 and three white")
	}
	wantX(t, g, 2, true)
}

// An Aura's cast path (castAura) records X too.
func TestCastAuraRecordsAnnouncedX(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.White, 2)
	def := auraDefWithEnchant(t, "Creature")
	def.Faces[0].ManaCost = mana.MustParse("X W")
	aura := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueuePayX(1)
	queueGeneric(c, mana.ShardW, 1)

	if !g.CastSpell(p, aura, c) {
		t.Fatal("CastSpell failed casting an {X}{W} Aura with X=1 and one legal host")
	}
	wantX(t, g, 1, true)
}

// An activated ability's {X} (CR 602.2b) is recorded on its stack item the
// same way a spell's is.
func TestActivateAbilityRecordsAnnouncedX(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.Player(p).ManaPool.Add(mana.Green, 2)
	def := creatureDefWithAbility(t, "Test X Gain", "AB$ GainLife | Cost$ X | Defined$ You | LifeAmount$ 1")
	creature := g.NewCard(def, p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueuePayX(2)
	queueGeneric(c, mana.ShardG, 2)

	if !g.ActivateAbility(p, creature, 0, c) {
		t.Fatal("ActivateAbility failed paying Cost$ X with X=2 and two green")
	}
	wantX(t, g, 2, true)
}

// A spell cast without paying its mana cost announces no X
// (PlaySpellAbility.announceValuesLikeX's setXManaCostPaid(null): no cost
// part carries an X any more), so ChangeX cannot give it one.
func TestCastWithoutPayingAnnouncesNoX(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	spell := g.NewCard(gainInstant(t, "Gain X", "X W", "1"), p, engine.Exile)
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | WithoutManaCost$ True")
	g.Card(host).Memory.Remember(engine.CardEntity(spell))
	top, _ := g.StackTop()
	if err := engine.NewRegistry().Resolve(g, &top, engine.NewScriptedController()); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(spell).Zone; got != engine.Stack {
		t.Fatalf("spell zone = %v, want Stack", got)
	}
	wantX(t, g, 0, false)
}

// HasXManaCost$ (TriggerSpellAbilityCastOrCopy.java:171-181) fires a
// SpellCast trigger for a spell whose mana cost carries {X}.
func TestSpellCastTriggerHasXManaCostFiresForXSpell(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(spellCastWatcherDefExtra(t, "X Watcher", "ValidActivatingPlayer$ You | HasXManaCost$ True"), p, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.Green, 2)
	creature := g.NewCard(creatureDefManaCost(t, "X G"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueuePayX(1)
	queueGeneric(c, mana.ShardG, 1)

	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting {X}{G} with X=1 and two green")
	}
	if got := g.StackLen(); got != 2 {
		t.Fatalf("StackLen = %d, want 2: the spell and the HasXManaCost trigger", got)
	}
	if top, _ := g.StackTop(); top.API != engine.APIGainLife {
		t.Errorf("top = %v, want the watcher's GainLife trigger", top.API)
	}
}

// HasXManaCost$ skips a spell whose mana cost has no {X}.
func TestSpellCastTriggerHasXManaCostSkipsNonXSpell(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(spellCastWatcherDefExtra(t, "X Watcher", "ValidActivatingPlayer$ You | HasXManaCost$ True"), p, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(creatureDefManaCost(t, "G"), p, engine.Hand)

	if !g.CastSpell(p, creature, engine.NewScriptedController()) {
		t.Fatal("CastSpell failed casting {G} with one green")
	}
	if got := g.StackLen(); got != 1 {
		t.Errorf("StackLen = %d, want 1: no trigger for a spell without {X}", got)
	}
}

// HasXManaCost$ reads the printed mana cost (cast.getManaCost().countX()),
// not whether X was announced: a spell cast without paying still fires it.
func TestSpellCastTriggerHasXManaCostReadsPrintedCost(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(spellCastWatcherDefExtra(t, "X Watcher", "ValidActivatingPlayer$ You | HasXManaCost$ True"), p, engine.Battlefield)
	spell := g.NewCard(gainInstant(t, "Gain X", "X W", "1"), p, engine.Exile)
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | WithoutManaCost$ True")
	g.Card(host).Memory.Remember(engine.CardEntity(spell))
	top, _ := g.StackTop()
	if err := engine.NewRegistry().Resolve(g, &top, engine.NewScriptedController()); err != nil {
		t.Fatal(err)
	}
	if got := g.StackLen(); got != 3 {
		t.Fatalf("StackLen = %d, want 3: the Play, the free X spell, the HasXManaCost trigger", got)
	}
	if top, _ := g.StackTop(); top.API != engine.APIGainLife {
		t.Errorf("top = %v, want the watcher's GainLife trigger", top.API)
	}
}
