package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// Prepare (AlterAttributeEffect.java:65-89): a prepared creature's controller
// may cast a copy of its prepare spell from exile; casting it, or the creature
// leaving the battlefield, unprepares it.

// enterPreparedEater casts the real Adventurous Eater ("This creature enters
// prepared"; its spell is Have a Bite) for p and returns it with its exiled
// spell copy.
func enterPreparedEater(t *testing.T, g *engine.Game, p engine.PlayerID) (eater, spell engine.CardID) {
	t.Helper()
	eater = g.NewCard(corpusCard(t, "Adventurous Eater"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Black, 3)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardB)
	c.QueuePayGeneric(mana.ShardB)
	if err := castEntrant(t, g, p, eater, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	exiled := g.Zone(engine.Exile, p).Cards()
	if len(exiled) != 1 {
		t.Fatalf("exile holds %d cards, want the prepared copy", len(exiled))
	}
	return eater, exiled[0]
}

// The creature enters prepared: its spell's copy waits in exile, a token, and
// casting it puts a +1/+1 counter on the target, gains a life, unprepares the
// creature and leaves no card behind.
func TestPreparedCreatureCastsACopyOfItsSpell(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	eater, spell := enterPreparedEater(t, g, p)
	if got := g.Card(eater).Zone; got != engine.Battlefield {
		t.Fatalf("Adventurous Eater is in %v, want the battlefield", got)
	}
	copyCard := g.Card(spell)
	if copyCard.Def.Name != "Have a Bite" || !copyCard.IsToken {
		t.Fatalf("exiled %q (token %v), want a token Have a Bite", copyCard.Def.Name, copyCard.IsToken)
	}
	sba(g)
	if got := g.Card(spell).Zone; got != engine.Exile {
		t.Fatalf("the copy is in %v after state-based actions, want exile (GameAction.java:1922)", got)
	}

	g.Player(p).ManaPool.Add(mana.Black, 1)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(eater)})
	before := g.Player(p).Life
	if !g.CastSpell(p, spell, c) {
		t.Fatal("could not cast the prepared copy")
	}
	if g.CastSpell(p, spell, c) {
		t.Fatal("cast the copy twice")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	sba(g)
	if got := g.Card(eater).Counters.Count(engine.P1P1); got != 1 {
		t.Errorf("+1/+1 counters = %d, want 1", got)
	}
	if got := g.Player(p).Life - before; got != 1 {
		t.Errorf("gained %d, want 1", got)
	}
	if got := g.Card(spell).Zone; got != engine.None {
		t.Errorf("the resolved copy is in %v, want gone", got)
	}
	if got := len(g.Zone(engine.Exile, p).Cards()); got != 0 {
		t.Errorf("exile holds %d cards, want none: casting the copy unprepared it", got)
	}
}

// Only the prepared creature's controller may cast the copy, and a creature
// leaving the battlefield takes its copy with it.
func TestPreparedCopyBelongsToTheCreaturesController(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	eater, spell := enterPreparedEater(t, g, p)
	g.Player(other).ManaPool.Add(mana.Black, 1)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(eater)})
	if g.CastSpell(other, spell, c) {
		t.Fatal("the opponent cast p's prepared copy")
	}
	g.Move(eater, engine.Graveyard, p)
	if got := g.Card(spell).Zone; got != engine.None {
		t.Errorf("the copy is in %v after the creature left, want gone", got)
	}
}

// The copy waiting in exile matches no valid string (Card.java:5845-5848), a
// negated one aside; IsPrepared reads the creature, prepared the copy.
func TestPreparedCopyIsHiddenFromValidStrings(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	eater, spell := enterPreparedEater(t, g, p)
	for _, tc := range []struct {
		name string
		id   engine.CardID
		spec string
		want bool
	}{
		{"copy is no Card", spell, "Card", false},
		{"copy is no Sorcery", spell, "Sorcery", false},
		{"copy passes a negated restriction", spell, "!Creature", true},
		{"creature is prepared", eater, "Creature.IsPrepared", true},
		{"creature is no prepared spell", eater, "Card.prepared", false},
	} {
		if got := engine.Matches(g, g.Card(tc.id), valid.Parse(tc.spec), p, eater); got != tc.want {
			t.Errorf("%s: Matches(%q) = %v, want %v", tc.name, tc.spec, got, tc.want)
		}
	}
	g.Player(p).ManaPool.Add(mana.Black, 1)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(eater)})
	if !g.CastSpell(p, spell, c) {
		t.Fatal("could not cast the prepared copy")
	}
	if !engine.Matches(g, g.Card(spell), valid.Parse("Card.prepared"), p, eater) {
		t.Error("the cast copy is not a prepared spell")
	}
	if engine.Matches(g, g.Card(eater), valid.Parse("Creature.IsPrepared"), p, eater) {
		t.Error("the creature is still prepared after its copy was cast")
	}
}

// AlterAttribute Activate$ False unprepares (Biblioplex Tomekeeper's second
// mode): the copy ceases to exist. A creature with no prepare spell never
// becomes prepared, and one already prepared gets no second copy.
func TestAlterAttributePreparedOnAndOff(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	eater, spell := enterPreparedEater(t, g, p)
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	both := []engine.EntityID{engine.CardEntity(eater), engine.CardEntity(bears)}
	alter := func(line string) {
		t.Helper()
		if _, err := resolveNow(t, g, p, engine.NewScriptedController(), both, line); err != nil {
			t.Fatalf("%q: %v", line, err)
		}
	}
	alter("DB$ AlterAttribute | ValidTgts$ Creature | TargetMin$ 2 | TargetMax$ 2 | Attributes$ Prepared | RememberAltered$ True")
	if got := len(g.Zone(engine.Exile, p).Cards()); got != 1 {
		t.Fatalf("exile holds %d cards, want only the first copy", got)
	}
	if engine.Matches(g, g.Card(bears), valid.Parse("Creature.IsPrepared"), p, bears) {
		t.Error("Grizzly Bears became prepared without a prepare spell")
	}
	alter("DB$ AlterAttribute | ValidTgts$ Creature | TargetMin$ 2 | TargetMax$ 2 | Attributes$ Prepared | Activate$ False")
	if got := g.Card(spell).Zone; got != engine.None {
		t.Errorf("the copy is in %v after unpreparing, want gone", got)
	}
	alter("DB$ AlterAttribute | ValidTgts$ Creature | TargetMin$ 2 | TargetMax$ 2 | Attributes$ Prepared")
	if !engine.Matches(g, g.Card(eater), valid.Parse("Creature.IsPrepared"), p, eater) {
		t.Error("Adventurous Eater did not become prepared again")
	}
}
