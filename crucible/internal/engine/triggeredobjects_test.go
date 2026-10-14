package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Each trigger mode records the run-time objects its Java Trigger subclass's
// setTriggeringObjects keeps, so Defined$ Triggered<Key> and Triggered<Key>$
// amounts on the ability it puts on the stack name them. Every test here
// sets the event up so the recorded object differs from the trigger's own
// controller -- a test that recorded nothing would have errored, one that
// read the controller instead would act on the wrong player.

// TestPhaseTriggerRecordsTheTurnsPlayer proves TriggerPhase's AbilityKey.Player
// is the turn's player (PhaseHandler.java:438), not the trigger's
// controller: p's "at the beginning of each upkeep, that player loses 2 life"
// hits other on other's upkeep.
func TestPhaseTriggerRecordsTheTurnsPlayer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Upkeep Watcher",
		"Mode$ Phase | Phase$ Upkeep | TriggerZones$ Battlefield | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ TriggeredPlayer | LifeAmount$ 2"), p, engine.Battlefield)
	g.SetTurnState(2, other, engine.Untap)

	c := engine.NewScriptedController()
	g.AdvancePhase(c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{20, 18}; got != want {
		t.Errorf("life (p, other) = %v, want %v -- TriggeredPlayer is the player whose upkeep it is", got, want)
	}
}

// TestPhaseTriggerAmountReadsTheTriggeredPlayer proves the amount side of
// the same key: TriggeredPlayer$CardsInHand (Dreamborn Muse, Iron Maiden,
// Price of Knowledge) counts the turn player's hand.
func TestPhaseTriggerAmountReadsTheTriggeredPlayer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Upkeep Watcher",
		"Mode$ Phase | Phase$ Upkeep | ValidPlayer$ Opponent | TriggerZones$ Battlefield | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ TriggeredPlayer | LifeAmount$ X",
		"X", "TriggeredPlayer$CardsInHand"), p, engine.Battlefield)
	for range 3 {
		g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Hand)
	}
	g.SetTurnState(2, other, engine.Untap)

	c := engine.NewScriptedController()
	g.AdvancePhase(c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 17 {
		t.Errorf("other life = %d, want 17 -- X is the 3 cards in the triggered player's hand", got)
	}
}

// TestDelayedPhaseTriggerRecordsTheTurnsPlayer proves a delayed Mode$ Phase
// trigger (delayedPhaseTriggerMatches) records the same key: p's "at the
// beginning of the next upkeep" fires in other's upkeep and names other.
func TestDelayedPhaseTriggerRecordsTheTurnsPlayer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ DelayedTrigger | Mode$ Phase | Phase$ Upkeep | Execute$ DBLose",
		"DBLose", "DB$ LoseLife | Defined$ TriggeredPlayer | LifeAmount$ 3")
	g.SetTurnState(2, other, engine.Untap)

	g.AdvancePhase(c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{20, 17}; got != want {
		t.Errorf("life (p, other) = %v, want %v", got, want)
	}
}

// TestDrawnTriggerRecordsTheDrawerAndTheCard proves TriggerDrawn.java:113:
// AbilityKey.Player is who drew (Underworld Dreams' "that player") and
// AbilityKey.Card the drawn card, whose owner Defined$ TriggeredCardOwner
// names (Spiteful Visions).
func TestDrawnTriggerRecordsTheDrawerAndTheCard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Draw Watcher",
		"Mode$ Drawn | ValidCard$ Card.OppOwn | TriggerZones$ Battlefield | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ TriggeredPlayer | LifeAmount$ 1 | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ TriggeredCardOwner | LifeAmount$ 3"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Library)

	c := engine.NewScriptedController()
	g.DrawCards(other, 1, c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{20, 22}; got != want {
		t.Errorf("life (p, other) = %v, want %v -- the drawer loses 1, the drawn card's owner gains 3", got, want)
	}
}

// TestTapsAndUntapsTriggersRecordTheCard proves TriggerTaps.java:94 and
// TriggerUntaps.java:67: AbilityKey.Card is the permanent that tapped or
// untapped, whose controller Defined$ TriggeredCardController names.
func TestTapsAndUntapsTriggersRecordTheCard(t *testing.T) {
	t.Parallel()

	for _, mode := range []string{"Taps", "Untaps"} {
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(triggerWatcherDef(t, "Test "+mode+" Watcher",
			"Mode$ "+mode+" | ValidCard$ Land | Execute$ TrigLose",
			"TrigLose", "DB$ LoseLife | Defined$ TriggeredCardController | LifeAmount$ 1"), p, engine.Battlefield)
		plains := g.NewCard(landDef(t, "Plains", "Basic Land Plains"), other, engine.Battlefield)

		c := engine.NewScriptedController()
		if mode == "Taps" {
			g.SetTurnState(1, other, engine.Main1)
			if !g.TapLandForMana(other, plains, mana.White, c) {
				t.Fatal("TapLandForMana failed tapping a Plains for white")
			}
		} else {
			// p's Cleanup wraps to other's turn 2, whose untap step untaps
			// the Plains.
			g.Card(plains).Tapped = true
			g.SetTurnState(1, p, engine.Cleanup)
			g.AdvancePhase(c)
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatalf("%s: ResolveStack: %v", mode, err)
		}
		if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{20, 19}; got != want {
			t.Errorf("%s: life (p, other) = %v, want %v", mode, got, want)
		}
	}
}

// TestSacrificedTriggerRecordsTheCard proves TriggerSacrificed.java:76: p's
// watcher sees other's creature sacrificed and names its controller.
func TestSacrificedTriggerRecordsTheCard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Sacrifice Watcher",
		"Mode$ Sacrificed | ValidCard$ Creature | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ TriggeredCardController | LifeAmount$ 2"), p, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueSacrificeChoice([]engine.CardID{victim})
	def := etbSacrificeTriggerDefParams(t, "Test Edict", "SacValid$ Creature | Defined$ Opponent | Amount$ 1", nil)
	if _, err := castETBSacrifice(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{20, 18}; got != want {
		t.Errorf("life (p, other) = %v, want %v", got, want)
	}
}

// combatHit has attacker (p's 3/3) hit other unblocked and deals combat
// damage, then resolves what triggered.
func combatHit(t *testing.T, g *engine.Game, attacker engine.CardID) {
	t.Helper()
	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)
	if err := g.ResolveStack(engine.NewRegistry(), bc); err != nil {
		t.Fatalf("ResolveStack after blockers: %v", err)
	}
	dc := engine.NewScriptedController()
	g.DealCombatDamage(dc)
	if err := g.ResolveStack(engine.NewRegistry(), dc); err != nil {
		t.Fatalf("ResolveStack after damage: %v", err)
	}
}

// TestDamageDoneOnceRecordsTargetAndAttackingPlayer proves
// TriggerDamageDoneOnce.java:57-69 on other's watcher: AbilityKey.Target is
// the damaged player (The Raven's Warning's "that player") and
// AbilityKey.AttackingPlayer the sources' controller (Contested Game Ball).
func TestDamageDoneOnceRecordsTargetAndAttackingPlayer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	attacker := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Damage Watcher",
		"Mode$ DamageDoneOnce | CombatDamage$ True | ValidTarget$ You | Execute$ TrigGain",
		"TrigGain", "DB$ GainLife | Defined$ TriggeredAttackingPlayer | LifeAmount$ 5 | SubAbility$ DBLose",
		"DBLose", "DB$ LoseLife | Defined$ TriggeredTarget | LifeAmount$ 1"), other, engine.Battlefield)

	combatHit(t, g, attacker)
	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{25, 16}; got != want {
		t.Errorf("life (p, other) = %v, want %v", got, want)
	}
}

// TestDamageDealtOnceRecordsTheSource proves TriggerDamageDealtOnce.java:98:
// AbilityKey.Source is the creature that dealt the damage, whose controller
// Defined$ TriggeredSourceController names -- p, though other controls the
// watcher.
func TestDamageDealtOnceRecordsTheSource(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	attacker := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Source Watcher",
		"Mode$ DamageDealtOnce | ValidSource$ Creature | CombatDamage$ True | Execute$ TrigGain",
		"TrigGain", "DB$ GainLife | Defined$ TriggeredSourceController | LifeAmount$ 5"), other, engine.Battlefield)

	combatHit(t, g, attacker)
	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{25, 17}; got != want {
		t.Errorf("life (p, other) = %v, want %v", got, want)
	}
}

// TestAttackerUnblockedOnceRecordsTheAttackingPlayer proves
// TriggerAttackerUnblockedOnce.java:69 (Coveted Jewel): AbilityKey
// .AttackingPlayer is the combat's attacking player (Combat.java:699).
func TestAttackerUnblockedOnceRecordsTheAttackingPlayer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	attacker := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Unblocked Watcher",
		"Mode$ AttackerUnblockedOnce | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ TriggeredAttackingPlayer | LifeAmount$ 2"), other, engine.Battlefield)

	combatHit(t, g, attacker)
	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{18, 17}; got != want {
		t.Errorf("life (p, other) = %v, want %v", got, want)
	}
}

// TestBlocksTriggerRecordsTheBlocker proves TriggerBlocks.java:71:
// AbilityKey.Blocker is the creature that blocked, here destroyed by p's
// "whenever a creature blocks, destroy it".
func TestBlocksTriggerRecordsTheBlocker(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	attacker := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Block Watcher",
		"Mode$ Blocks | ValidCard$ Creature | Execute$ TrigDestroy",
		"TrigDestroy", "DB$ Destroy | Defined$ TriggeredBlocker"), p, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attacker}})
	declareBlockers(t, g, bc)
	if err := g.ResolveStack(engine.NewRegistry(), bc); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(blocker).Zone; z != engine.Graveyard {
		t.Errorf("blocker zone = %v, want Graveyard", z)
	}
}

// TestLandPlayedTriggerRecordsTheLand proves TriggerLandPlayed.java:58:
// AbilityKey.Card is the land played.
func TestLandPlayedTriggerRecordsTheLand(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Land Watcher",
		"Mode$ LandPlayed | ValidCard$ Land | TriggerZones$ Battlefield | Execute$ TrigDestroy",
		"TrigDestroy", "DB$ Destroy | Defined$ TriggeredCard"), p, engine.Battlefield)
	g.SetTurnState(1, other, engine.Main1)
	land := g.NewCard(landDef(t, "Plains", "Basic Land Plains"), other, engine.Hand)

	c := engine.NewScriptedController()
	if !g.PlayLand(other, land, c) {
		t.Fatal("PlayLand failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(land).Zone; z != engine.Graveyard {
		t.Errorf("land zone = %v, want Graveyard", z)
	}
}

// TestBecomesTargetTriggerRecordsTheTarget proves
// TriggerBecomesTarget.java:85's AbilityKey.Target: the watcher's "destroy
// it" names the targeted creature.
func TestBecomesTargetTriggerRecordsTheTarget(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Target Watcher",
		"Mode$ BecomesTarget | ValidTarget$ Creature | Execute$ TrigDestroy",
		"TrigDestroy", "DB$ Destroy | Defined$ TriggeredTarget"), p, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	def := etbLoseLifeTriggerDefParams(t, "Test Targeter", "ValidTgts$ Creature.YouCtrl+Other | LifeAmount$ 1", nil)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	host := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, host, c) {
		t.Fatal("CastSpell failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(victim).Zone; z != engine.Graveyard {
		t.Errorf("targeted creature zone = %v, want Graveyard", z)
	}
}

// TestChangesZoneAllRecordsTheBatch proves TriggerChangesZoneAll.java:57-63:
// AbilityKey.Amount is the number of cards the trigger's filter kept, read
// by TriggerCount$Amount.
func TestChangesZoneAllRecordsTheBatch(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Batch Watcher",
		"Mode$ ChangesZoneAll | ValidCards$ Creature | Origin$ Battlefield | Destination$ Graveyard | Execute$ TrigGain",
		"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ X",
		"X", "TriggerCount$Amount"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	c := engine.NewScriptedController()
	def := etbSacrificeAllTriggerDefParams(t, "Test SacAll", "ValidCards$ Creature.Other", nil)
	if _, err := castETBSacrifice(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("p life = %d, want 22 -- Amount is the two creatures of the batch", got)
	}
}

// TestExiledFromBattlefieldTriggerRecordsTheCard proves the Exile
// destination of a ChangesZone line (exile.go) records AbilityKey.Card like
// the graveyard one: other's creature exiled for its own cost names other.
func TestExiledFromBattlefieldTriggerRecordsTheCard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Exile Watcher",
		"Mode$ ChangesZone | Origin$ Battlefield | Destination$ Exile | ValidCard$ Creature | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ TriggeredCardController | LifeAmount$ 2"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefWithAbility(t, "Test Exiler", "AB$ GainLife | Cost$ Exile<1/CARDNAME> | Defined$ You | LifeAmount$ 1"), other, engine.Battlefield)
	g.SetTurnState(1, other, engine.Main1)

	c := engine.NewScriptedController()
	if !g.ActivateAbility(other, creature, 0, c) {
		t.Fatal("ActivateAbility failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got, want := [2]int{g.Player(p).Life, g.Player(other).Life}, [2]int{20, 19}; got != want {
		t.Errorf("life (p, other) = %v, want %v", got, want)
	}
}

// TestTriggeredCardOwnerNeedsARecordedCard proves the unset key stays an
// error (GO-7): a Phase trigger records no card, so TriggeredCardOwner
// fails loudly rather than naming nobody.
func TestTriggeredCardOwnerNeedsARecordedCard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Test Upkeep Watcher",
		"Mode$ Phase | Phase$ Upkeep | TriggerZones$ Battlefield | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ TriggeredCardOwner | LifeAmount$ 2"), p, engine.Battlefield)
	g.SetTurnState(2, other, engine.Untap)

	c := engine.NewScriptedController()
	g.AdvancePhase(c)
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), `"TriggeredCardOwner": the trigger recorded no card`) {
		t.Fatalf("ResolveStack error = %v, want the trigger recorded no card", err)
	}
}
