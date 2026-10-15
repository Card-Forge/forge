package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// This file covers ReplaceDyingExiledWith$: the creature the effect exiles
// instead of letting it die is exiled with the card whose ability made the
// effect (ExiledWithEffectSource$ on the replacing ChangeZone,
// SpellAbilityEffect.java:732-734, :1092-1094), Gut, Fanatical Priestess's
// fight.

// gutChain is Gut, Fanatical Priestess's ETB trigger: up to one creature you
// control fights a creature you don't control, which is exiled with Gut if it
// would die this turn.
var gutChain = []string{
	"DB$ Pump | ValidTgts$ Creature.YouCtrl | TargetMin$ 0 | TargetMax$ 1 | SubAbility$ DBFight",
	"DBFight", "DB$ Fight | Defined$ ParentTarget | ValidTgts$ Creature.YouDontCtrl | ReplaceDyingDefined$ ThisTargetedCard | ReplaceDyingExiledWith$ True",
}

// TestGutExilesTheFoughtCreatureWithItself is the dominant shape: the 2/2
// the 3/3 fights dies, goes to exile instead, and reads as exiled with the
// trigger's host (ExiledWithSource, CardProperty.java:397-411).
func TestGutExilesTheFoughtCreatureWithItself(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
	host := resolveLine(t, g, p, c, gutChain[0], gutChain[1:]...)
	sba(g)

	if z := g.Card(theirs).Zone; z != engine.Exile {
		t.Fatalf("fought creature zone = %v, want Exile", z)
	}
	if got := g.Card(theirs).ExiledWith(); got != host {
		t.Errorf("ExiledWith = %v, want the trigger's host %v", got, host)
	}
	if !engine.Matches(g, g.Card(theirs), valid.Parse("Card.ExiledWithSource"), p, host) {
		t.Error("Card.ExiledWithSource from the host: false, want true")
	}
	if z := g.Card(mine).Zone; z != engine.Battlefield {
		t.Errorf("my 3/3 zone = %v, want Battlefield", z)
	}
}

// TestReplaceDyingWithoutExiledWithMarksNothing proves the plain shape (Lava
// Coil) still exiles without naming a host: the mark is the param's.
func TestReplaceDyingWithoutExiledWithMarksNothing(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	resolveLine(t, g, p, targeting(victim), "DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 4 | ReplaceDyingDefined$ Targeted")
	sba(g)
	if z := g.Card(victim).Zone; z != engine.Exile {
		t.Fatalf("victim zone = %v, want Exile", z)
	}
	if got := g.Card(victim).ExiledWith(); got != engine.NoCard {
		t.Errorf("ExiledWith = %v, want none", got)
	}
}

// TestExiledWithEffectSourceOnAPermanentFails proves a Moved replacement
// naming ExiledWithEffectSource$ on a card that is not an effect -- Java's
// getEffectSource is null there -- is not applied and leaves a pending error.
func TestExiledWithEffectSourceOnAPermanentFails(t *testing.T) {
	t.Parallel()

	g, p, opp := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(scriptDef(t, "Exiler", "Creature Spirit",
		"R:Event$ Moved | ActiveZones$ Battlefield | Origin$ Battlefield | Destination$ Graveyard | ValidLKI$ Creature.OppCtrl | ReplaceWith$ Exile",
		"SVar:Exile:DB$ ChangeZone | Origin$ Battlefield | Destination$ Exile | Defined$ ReplacedCard | ExiledWithEffectSource$ True"), opp, engine.Battlefield)
	victim := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	g.Card(victim).Damage.Mark(2, false)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	err := g.TakePendingError()
	if err == nil || !strings.Contains(err.Error(), "ExiledWithEffectSource$") {
		t.Errorf("pending error = %v, want one naming ExiledWithEffectSource$", err)
	}
	if got := g.Card(victim).Zone; got != engine.Graveyard {
		t.Errorf("victim zone = %v, want Graveyard: the replacement was not applied", got)
	}
}
