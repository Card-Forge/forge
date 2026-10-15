package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Defined$ TriggeredCardController/TriggeredCardOwner read getController()/
// getOwner() of the object the trigger mode stored as AbilityKey.Card
// (AbilityUtils.java:1020-1030, :1053-1059). For a card that left the
// battlefield that object is a frozen copy -- the CardLKI under Origin$
// Battlefield (TriggerChangesZone.java:192-194), the LKI copy for Sacrificed
// (Player.java:2276-2280) -- so a creature that left under a borrowed
// controller answers the borrower. Every test here borrows other's creature
// for p first: the card's live controller after it left is its owner, other.

// stealCreature has p gain control of other's victim (GainControl,
// permanent).
func stealCreature(t *testing.T, g *engine.Game, p, other engine.PlayerID, victim engine.CardID) {
	t.Helper()
	c := engine.NewScriptedController()
	c.QueueTargets(entities(victim))
	resolveLine(t, g, p, c, "DB$ GainControl | ValidTgts$ Creature.OppCtrl")
	if got := g.Card(victim).Controller(); got != p {
		t.Fatalf("victim controller = %v, want the borrower %v", got, p)
	}
	if got := g.Card(victim).Owner; got != other {
		t.Fatalf("victim owner = %v, want %v", got, other)
	}
}

// destroyCreature destroys victim with a targeted Destroy resolved by p.
func destroyCreature(t *testing.T, g *engine.Game, p engine.PlayerID, victim engine.CardID) {
	t.Helper()
	c := engine.NewScriptedController()
	c.QueueTargets(entities(victim))
	resolveLine(t, g, p, c, "DB$ Destroy | ValidTgts$ Creature.OppOwn")
	if z := g.Card(victim).Zone; z != engine.Graveyard {
		t.Fatalf("victim zone = %v, want Graveyard", z)
	}
}

// TestDiesTriggerNamesTheBorrowerAsTriggeredCardController proves the Origin$
// Battlefield branch: Dingus Egg's "that land's controller" for a stolen
// creature is the player who controlled it as it died (p), not its owner.
func TestDiesTriggerNamesTheBorrowerAsTriggeredCardController(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Dies Watcher",
		"Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Creature.YouOwn | TriggerZones$ Battlefield | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ TriggeredCardController | LifeAmount$ 2 | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ TriggeredCardOwner | LifeAmount$ 5"), other, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	stealCreature(t, g, p, other, victim)
	destroyCreature(t, g, p, victim)

	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{18, 25}; got != want {
		t.Errorf("life (p, other) = %v, want %v -- the borrower loses 2, the owner gains 5", got, want)
	}
}

// TestLeavesTriggerWithoutBattlefieldOriginReadsTheMovedCard proves the other
// branch of TriggerChangesZone.setTriggeringObjects: with no Origin$
// Battlefield the stored Card is the moved card itself (GameAction.java:602),
// whose controller in the graveyard is its owner.
func TestLeavesTriggerWithoutBattlefieldOriginReadsTheMovedCard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Graveyard Watcher",
		"Mode$ ChangesZone | Destination$ Graveyard | ValidCard$ Creature.YouOwn | TriggerZones$ Battlefield | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ TriggeredCardController | LifeAmount$ 2"), other, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	stealCreature(t, g, p, other, victim)
	destroyCreature(t, g, p, victim)

	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{20, 18}; got != want {
		t.Errorf("life (p, other) = %v, want %v -- the moved card answers its owner", got, want)
	}
}

// TestSacrificedTriggerNamesTheSacrificer proves Sacrificed's LKI copy: p
// sacrifices the creature it borrowed, and the watcher names p.
func TestSacrificedTriggerNamesTheSacrificer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Sacrifice Watcher",
		"Mode$ Sacrificed | ValidCard$ Creature.YouOwn | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ TriggeredCardController | LifeAmount$ 3"), other, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	stealCreature(t, g, p, other, victim)

	c := engine.NewScriptedController()
	c.QueueSacrificeChoice([]engine.CardID{victim})
	def := etbSacrificeTriggerDefParams(t, "Test Altar", "SacValid$ Creature.OppOwn | Defined$ You | Amount$ 1", nil)
	if _, err := castETBSacrifice(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(victim).Zone; z != engine.Graveyard {
		t.Fatalf("victim zone = %v, want Graveyard", z)
	}
	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{17, 20}; got != want {
		t.Errorf("life (p, other) = %v, want %v -- the sacrificer loses 3", got, want)
	}
}

// TestExiledTriggerNamesTheBorrower proves the exile half of the Origin$
// Battlefield branch (checkExiledTriggers).
func TestExiledTriggerNamesTheBorrower(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Exile Watcher",
		"Mode$ ChangesZone | Origin$ Battlefield | Destination$ Exile | ValidCard$ Creature.YouOwn | TriggerZones$ Battlefield | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ TriggeredCardController | LifeAmount$ 4"), other, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	stealCreature(t, g, p, other, victim)

	c := engine.NewScriptedController()
	c.QueueTargets(entities(victim))
	resolveLine(t, g, p, c, "DB$ ChangeZone | ValidTgts$ Creature.OppOwn | Origin$ Battlefield | Destination$ Exile")
	if z := g.Card(victim).Zone; z != engine.Exile {
		t.Fatalf("victim zone = %v, want Exile", z)
	}
	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{16, 20}; got != want {
		t.Errorf("life (p, other) = %v, want %v -- the borrower loses 4", got, want)
	}
}
