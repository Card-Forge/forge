package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These tests cover Haunt (CR 702.55) on the real corpus cards: an instant or
// sorcery whose only spell is its haunt SVar, exiled haunting a creature after
// it resolves, and a creature exiled haunting one when it dies; either way the
// haunt SVar runs again when the haunted creature dies.

// hauntGame is a two-player game on the corpus with a Grizzly Bears on
// other's battlefield and name in p's hand.
func hauntGame(t *testing.T, name string) (g *engine.Game, p, other engine.PlayerID, haunter, bears engine.CardID) {
	t.Helper()
	g, p, other = newTwoPlayerGameOn(t, scenarioDB(t))
	bears = g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Battlefield)
	haunter = g.NewCard(corpusCard(t, name), p, engine.Hand)
	return g, p, other, haunter, bears
}

// destroyNow destroys target as an ability of p's and resolves the stack.
func destroyNow(t *testing.T, g *engine.Game, p engine.PlayerID, c *engine.ScriptedController, target engine.CardID) {
	t.Helper()
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(target)}, "DB$ Destroy | ValidTgts$ Creature"); err != nil {
		t.Fatal(err)
	}
}

// castBenediction casts Benediction of Moons ({W}: you gain 1 life for each
// player) and resolves it, choosing haunt for its haunt trigger's target when
// there is one to choose.
func castBenediction(t *testing.T, g *engine.Game, p engine.PlayerID, c *engine.ScriptedController, card engine.CardID, haunt []engine.EntityID) {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, card, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	if haunt != nil {
		c.QueueTargets(haunt)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
}

// A Haunt sorcery casts from its haunt SVar, resolves, and is then exiled
// haunting the creature its trigger targets; when that creature dies the SVar
// runs again from exile. Before, the card had no spell to cast at all.
func TestHauntSorceryCastsThenHauntsACreature(t *testing.T) {
	t.Parallel()

	g, p, other, bene, bears := hauntGame(t, "Benediction of Moons")
	c := engine.NewScriptedController()
	castBenediction(t, g, p, c, bene, []engine.EntityID{engine.CardEntity(bears)})
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life after the spell = %d, want 22", got)
	}
	if z := g.Card(bene).Zone; z != engine.Exile {
		t.Fatalf("Benediction is in %v, want Exile", z)
	}
	if got := g.Haunting(bene); got != bears {
		t.Fatalf("Benediction haunts %d, want the Bears %d", got, bears)
	}

	destroyNow(t, g, other, c, bears)
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life after the haunted creature died = %d, want 24", got)
	}
	if z := g.Card(bene).Zone; z != engine.Exile {
		t.Errorf("Benediction is in %v, want it still in Exile", z)
	}
	if got := g.Haunting(bene); got != engine.NoCard {
		t.Errorf("Benediction still haunts %d after the creature died", got)
	}
}

// payForSeize gives p the {2}{B}{B} Seize the Soul costs, all black, and a
// controller that pays its generic part with it.
func payForSeize(g *engine.Game, p engine.PlayerID) *engine.ScriptedController {
	g.Player(p).ManaPool.Add(mana.Black, 4)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardB)
	c.QueuePayGeneric(mana.ShardB)
	return c
}

// A spell that fizzles goes to the graveyard without its haunt trigger
// (Fizzle$ False): Seize the Soul's only target left before it resolved, so
// nothing is destroyed, no Spirit is made, and no haunt target is asked for
// (the scripted controller would panic on an unqueued target).
func TestHauntSpellThatFizzlesStaysInTheGraveyard(t *testing.T) {
	t.Parallel()

	g, p, other, seize, bears := hauntGame(t, "Seize the Soul")
	c := payForSeize(g, p)
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bears)})
	if !g.CastSpell(p, seize, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	g.Move(bears, engine.Hand, other)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if z := g.Card(seize).Zone; z != engine.Graveyard {
		t.Errorf("Seize the Soul is in %v, want Graveyard", z)
	}
	if n := len(g.Zone(engine.Battlefield, p).Cards()); n != 0 {
		t.Errorf("%d permanents on p's battlefield, want no Spirit", n)
	}
}

// Seize the Soul's whole chain resolves (destroy, then a Spirit token) before
// the card is exiled haunting a creature.
func TestHauntSpellChainResolvesThenHaunts(t *testing.T) {
	t.Parallel()

	g, p, _, seize, bears := hauntGame(t, "Seize the Soul")
	mine := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	c := payForSeize(g, p)
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bears)})
	if !g.CastSpell(p, seize, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if z := g.Card(bears).Zone; z != engine.Graveyard {
		t.Errorf("Bears are in %v, want Graveyard", z)
	}
	tokens := 0
	for _, id := range g.Zone(engine.Battlefield, p).Cards() {
		if g.Card(id).IsToken {
			tokens++
		}
	}
	if tokens != 1 {
		t.Errorf("%d tokens, want the one Spirit", tokens)
	}
	if got := g.Haunting(seize); got != mine {
		t.Errorf("Seize the Soul haunts %d, want %d", got, mine)
	}
}

// With no creature on the battlefield the haunt trigger has no legal target
// and never goes on the stack (CR 603.3d): the spell stays in the graveyard.
func TestHauntSpellWithNoCreatureStaysInTheGraveyard(t *testing.T) {
	t.Parallel()

	g, p, other, bene, bears := hauntGame(t, "Benediction of Moons")
	g.Move(bears, engine.Hand, other)
	c := engine.NewScriptedController()
	castBenediction(t, g, p, c, bene, nil)
	if z := g.Card(bene).Zone; z != engine.Graveyard {
		t.Errorf("Benediction is in %v, want Graveyard", z)
	}
	if got := g.Haunting(bene); got != engine.NoCard {
		t.Errorf("Benediction haunts %d, want nothing", got)
	}
}

// A haunted creature dealt lethal damage dies through the state-based action
// check (CR 704.5g), the same dies trigger path as a destroy effect.
func TestHauntTriggersWhenTheCreatureDiesToDamage(t *testing.T) {
	t.Parallel()

	g, p, other, bene, bears := hauntGame(t, "Benediction of Moons")
	c := engine.NewScriptedController()
	castBenediction(t, g, p, c, bene, []engine.EntityID{engine.CardEntity(bears)})
	if _, err := resolveNow(t, g, other, c, []engine.EntityID{engine.CardEntity(bears)},
		"DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 2"); err != nil {
		t.Fatal(err)
	}
	if z := g.Card(bears).Zone; z != engine.Graveyard {
		t.Fatalf("Bears are in %v, want Graveyard", z)
	}
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life = %d, want 24", got)
	}
}

// A haunted creature that leaves the battlefield any way but dying is a new
// object (CR 400.7): the haunt ends and its dies trigger never fires.
func TestHauntEndsWhenTheCreatureIsExiled(t *testing.T) {
	t.Parallel()

	g, p, other, bene, bears := hauntGame(t, "Benediction of Moons")
	c := engine.NewScriptedController()
	castBenediction(t, g, p, c, bene, []engine.EntityID{engine.CardEntity(bears)})
	if _, err := resolveNow(t, g, other, c, []engine.EntityID{engine.CardEntity(bears)},
		"DB$ ChangeZone | ValidTgts$ Creature | Origin$ Battlefield | Destination$ Exile"); err != nil {
		t.Fatal(err)
	}
	if got := g.Haunting(bene); got != engine.NoCard {
		t.Errorf("Benediction haunts %d after the creature was exiled", got)
	}
	g.Move(bears, engine.Battlefield, other)
	destroyNow(t, g, other, c, bears)
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22: the returned creature is not the haunted one", got)
	}
}

// A haunter that leaves exile haunts nothing any more (Java's haunterUnExiled
// trigger, CardFactoryUtil.java:1213-1226), even back in exile.
func TestHauntEndsWhenTheHaunterLeavesExile(t *testing.T) {
	t.Parallel()

	g, p, other, bene, bears := hauntGame(t, "Benediction of Moons")
	c := engine.NewScriptedController()
	castBenediction(t, g, p, c, bene, []engine.EntityID{engine.CardEntity(bears)})
	g.Move(bene, engine.Hand, p)
	g.Move(bene, engine.Exile, p)
	if got := g.Haunting(bene); got != engine.NoCard {
		t.Errorf("Benediction haunts %d after leaving exile", got)
	}
	destroyNow(t, g, other, c, bears)
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22: no haunt trigger", got)
	}
}

// A Haunt creature runs its SVar as it enters, is exiled haunting a creature
// when it dies, and runs the SVar again when that creature dies.
func TestHauntCreatureEntersDiesAndHaunts(t *testing.T) {
	t.Parallel()

	g, p, other, hunter, bears := hauntGame(t, "Blind Hunter")
	g.Player(p).ManaPool.Add(mana.White, 2)
	g.Player(p).ManaPool.Add(mana.Black, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardW)
	c.QueuePayGeneric(mana.ShardB)
	if !g.CastSpell(p, hunter, c) {
		t.Fatalf("CastSpell failed: %v", g.TakePendingError())
	}
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if lp, lo := g.Player(p).Life, g.Player(other).Life; lp != 22 || lo != 18 {
		t.Fatalf("life after entering = %d/%d, want 22/18", lp, lo)
	}

	c.QueueTargets([]engine.EntityID{engine.CardEntity(bears)})
	destroyNow(t, g, other, c, hunter)
	if z := g.Card(hunter).Zone; z != engine.Exile {
		t.Fatalf("Blind Hunter is in %v, want Exile", z)
	}
	if got := g.Haunting(hunter); got != bears {
		t.Fatalf("Blind Hunter haunts %d, want the Bears %d", got, bears)
	}

	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	destroyNow(t, g, other, c, bears)
	if lp, lo := g.Player(p).Life, g.Player(other).Life; lp != 24 || lo != 16 {
		t.Errorf("life after the haunted creature died = %d/%d, want 24/16", lp, lo)
	}
}

// hauntAbility is the compiled DB$ Haunt the corpus card name's haunter
// trigger executes.
func hauntAbility(t *testing.T, name string) *compile.Ability {
	t.Helper()
	for _, tr := range corpusCard(t, name).Faces[0].Triggers {
		for _, sub := range tr.Subs {
			if strings.EqualFold(sub.Key, "Execute") && sub.Ability.Name == "Haunt" {
				return sub.Ability
			}
		}
	}
	t.Fatalf("%s has no Haunt trigger", name)
	return nil
}

// Haunt does nothing to a card that is no longer in its graveyard
// (HauntEffect.java:25): the card stays where it is.
func TestHauntIgnoresACardOutsideTheGraveyard(t *testing.T) {
	t.Parallel()

	g, p, _, hunter, bears := hauntGame(t, "Blind Hunter")
	g.PushAbility(engine.Ability{API: engine.APIHaunt, Source: hunter, Controller: p, Params: hauntAbility(t, "Blind Hunter"),
		Targets: []engine.EntityID{engine.CardEntity(bears)}})
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatal(err)
	}
	if z := g.Card(hunter).Zone; z != engine.Hand {
		t.Errorf("Blind Hunter is in %v, want Hand", z)
	}
}

// Haunt refuses a line it cannot run (GO-7): Java's untargeted unhaunt, a
// param it does not read, and a haunter with no creature chosen.
func TestHauntRejectsWhatItCannotRun(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, line, want string
	}{
		{"untargeted", "DB$ Haunt", "untargeted Haunt"},
		{"unread param", "DB$ Haunt | ValidTgts$ Creature | Defined$ Self", "Defined$ not resolvable yet"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Errorf("err = %v, want %q", err, tc.want)
			}
		})
	}

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	hunter := g.NewCard(corpusCard(t, "Blind Hunter"), p, engine.Graveyard)
	g.PushAbility(engine.Ability{API: engine.APIHaunt, Source: hunter, Controller: p, Params: hauntAbility(t, "Blind Hunter")})
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err == nil || !strings.Contains(err.Error(), "no creature target") {
		t.Errorf("err = %v, want a no-creature-target error", err)
	}
}
