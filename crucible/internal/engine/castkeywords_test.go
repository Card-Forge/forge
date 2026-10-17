package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// CR 702.85a: casting a spell with cascade exiles cards from the top of the
// library until a nonland card of lesser mana value, which may be cast for free;
// the other exiled cards go to the bottom (CardFactoryUtil.java:711).
func TestCascadeCastsTheFirstCheaperNonlandCard(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	elf := g.NewCard(corpusCard(t, "Bloodbraid Elf"), p, engine.Hand)
	mountain := g.NewCard(corpusCard(t, "Mountain"), p, engine.Library)
	expensive := g.NewCard(corpusCard(t, "Serra Angel"), p, engine.Library) // mana value 5: not cheaper
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Library)
	deep := g.NewCard(corpusCard(t, "Forest"), p, engine.Library)
	g.Player(p).ManaPool.Add(mana.Red, 2)
	g.Player(p).ManaPool.Add(mana.Green, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardR)
	c.QueuePayGeneric(mana.ShardG)
	c.QueueConfirmEffect(true)
	if !g.CastSpell(p, elf, c) {
		t.Fatal("could not cast Bloodbraid Elf")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(bears).Zone; got != engine.Battlefield {
		t.Errorf("Grizzly Bears is in %v, want Battlefield: the first nonland card cheaper than 4", got)
	}
	if got := g.Card(elf).Zone; got != engine.Battlefield {
		t.Errorf("Bloodbraid Elf is in %v, want Battlefield", got)
	}
	for _, id := range []engine.CardID{mountain, expensive} {
		if got := g.Card(id).Zone; got != engine.Library {
			t.Errorf("%s is in %v, want the library's bottom", g.Card(id).Def.Faces[0].Name, got)
		}
	}
	if got := g.Card(deep).Zone; got != engine.Library {
		t.Errorf("an unrevealed card moved to %v", got)
	}
}

// A cascade the caster declines casts nothing: every exiled card goes to the
// bottom of the library.
func TestCascadeDeclinedPutsEveryExiledCardOnTheBottom(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	elf := g.NewCard(corpusCard(t, "Bloodbraid Elf"), p, engine.Hand)
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Library)
	g.Player(p).ManaPool.Add(mana.Red, 2)
	g.Player(p).ManaPool.Add(mana.Green, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardR)
	c.QueuePayGeneric(mana.ShardG)
	c.QueueConfirmEffect(false)
	if !g.CastSpell(p, elf, c) {
		t.Fatal("could not cast Bloodbraid Elf")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(bears).Zone; got != engine.Library {
		t.Errorf("Grizzly Bears is in %v, want Library: the cast was declined", got)
	}
}

// CR 702.40a: storm copies the spell once for each spell cast before it this
// turn (CardFactoryUtil.java:1814, TriggerCount$CurrentStormCount/Minus.1).
func TestStormCopiesTheSpellForEachEarlierSpell(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).SpellsCastThisTurn = 2
	g.Player(other).SpellsCastThisTurn = 1
	shot := g.NewCard(corpusCard(t, "Grapeshot"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Red, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardR)
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	for range 3 {
		c.QueueConfirmEffect(false) // keep each copy's target
	}
	if !g.CastSpell(p, shot, c) {
		t.Fatal("could not cast Grapeshot")
	}
	if got := g.StackLen(); got != 2 {
		t.Fatalf("stack = %d, want the spell and its storm trigger", got)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 16 {
		t.Errorf("opponent life = %d, want 16: Grapeshot and three copies, one damage each", got)
	}
}

// CR 702.110a: a creature with exploit may sacrifice a creature as it enters,
// and a card watching for it fires on that sacrifice (Mode$ Exploited,
// ValidSource$ Card.Self). The exploiter can sacrifice itself.
func TestExploitSacrificeFiresTheExploitedTrigger(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name    string
		accept  bool
		oppLife int
		myLife  int
	}{{"sacrificed", true, 18, 22}, {"declined", false, 20, 20}} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			g.SetTurnState(1, p, engine.Main1)
			sadist := g.NewCard(corpusCard(t, "Qarsi Sadist"), p, engine.Hand)
			bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
			g.Player(p).ManaPool.Add(mana.Black, 1)
			g.Player(p).ManaPool.Add(mana.Red, 1)
			c := engine.NewScriptedController()
			c.QueuePayGeneric(mana.ShardR)
			c.QueueConfirmEffect(tc.accept)
			// Exploit's Optional$ sacrifice is "up to one" once confirmed
			// (SacrificeEffect.java:138), a card choice.
			c.QueueCardChoice([]engine.CardID{bears})
			c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
			if !g.CastSpell(p, sadist, c) {
				t.Fatal("could not cast Qarsi Sadist")
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			wantBears := engine.Battlefield
			if tc.accept {
				wantBears = engine.Graveyard
			}
			if got := g.Card(bears).Zone; got != wantBears {
				t.Errorf("Grizzly Bears is in %v, want %v", got, wantBears)
			}
			if got, want := g.Player(other).Life, tc.oppLife; got != want {
				t.Errorf("opponent life = %d, want %d", got, want)
			}
			if got, want := g.Player(p).Life, tc.myLife; got != want {
				t.Errorf("life = %d, want %d", got, want)
			}
		})
	}
}
