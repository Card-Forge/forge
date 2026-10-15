package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// The attacked defenders a Mode$ AttackersDeclared/AttackersDeclaredOneTarget
// trigger records (AbilityKey.AttackedTarget, TriggerAttackersDeclared.java:
// 80-96) and the attackers it records, read back through Defined$
// TriggeredAttackedTarget, the TriggeredAttackers/NotDefinedTriggeredAttackers
// valid properties and "attacking TriggeredAttackedTarget".

// attackWithTargets declares attackers for p, each at the matching target, and
// resolves the triggers the declaration put on the stack.
func attackWithTargets(t *testing.T, g *engine.Game, attackers []engine.CardID, targets []engine.EntityID) {
	t.Helper()
	c := engine.NewScriptedController()
	c.QueueAttackers(attackers)
	for _, e := range targets {
		c.QueueAttackTarget(e)
	}
	declareAttackers(t, g, c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
}

// TestTriggeredAttackedTargetNamesTheAttackedPlayer proves the player half:
// Defined$ TriggeredAttackedTarget is the player attacked.
func TestTriggeredAttackedTargetNamesTheAttackedPlayer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.NewCard(triggerWatcherDef(t, "Test Attack Watcher",
		"Mode$ AttackersDeclared | AttackingPlayer$ You | TriggerZones$ Battlefield | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ TriggeredAttackedTarget | LifeAmount$ 2"), p, engine.Battlefield)

	attackAndResolve(t, g, a)
	if got := [2]int{g.Player(p).Life, g.Player(other).Life}; got != [2]int{20, 18} {
		t.Errorf("life (attacker, defender) = %v, want [20 18]", got)
	}
}

// TestTriggeredAttackedTargetFollowsTheValidAttackers proves the
// ValidAttackers$ rewrite (TriggerAttackersDeclared.java:83-90): the recorded
// defenders are those of the matching attackers alone, so a big attacker on a
// planeswalker records the planeswalker (the card half) and not the player a
// small attacker attacks (the player half reads none).
func TestTriggeredAttackedTargetFollowsTheValidAttackers(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	small := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	big := g.NewCard(creatureDefPT(t, "4", "4"), p, engine.Battlefield)
	pw := g.NewCard(planeswalkerDefLoyalty(t, "5"), other, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 5)
	g.NewCard(triggerWatcherDef(t, "Test Attack Watcher",
		"Mode$ AttackersDeclared | ValidAttackers$ Creature.powerGE3 | TriggerZones$ Battlefield | Execute$ TrigPut",
		"TrigPut", "DB$ PutCounter | Defined$ TriggeredAttackedTarget | CounterType$ P1P1 | CounterNum$ 1 | SubAbility$ DBLose",
		"DBLose", "DB$ LoseLife | Defined$ TriggeredAttackedTarget | LifeAmount$ 3"), p, engine.Battlefield)

	attackWithTargets(t, g, []engine.CardID{small, big}, []engine.EntityID{engine.PlayerEntity(other), engine.CardEntity(pw)})
	if got := p1p1(g, pw); got != 1 {
		t.Errorf("planeswalker +1/+1 counters = %d, want 1", got)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("defender life = %d, want 20 -- the player is not the big attacker's defender", got)
	}
}

// TestTriggeredAttackedTargetKeepsWhatAttackedTargetNames proves the
// AttackedTarget$ filter (TriggerAttackersDeclared.java:93-95): of the two
// attacked defenders, only the planeswalker the spec names is recorded.
func TestTriggeredAttackedTargetKeepsWhatAttackedTargetNames(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	toPlayer := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	toWalker := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	pw := g.NewCard(planeswalkerDefLoyalty(t, "5"), other, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 5)
	g.NewCard(triggerWatcherDef(t, "Test Attack Watcher",
		"Mode$ AttackersDeclared | AttackedTarget$ Planeswalker | TriggerZones$ Battlefield | Execute$ TrigPut",
		"TrigPut", "DB$ PutCounter | Defined$ TriggeredAttackedTarget | CounterType$ P1P1 | CounterNum$ 1 | SubAbility$ DBLose",
		"DBLose", "DB$ LoseLife | Defined$ TriggeredAttackedTarget | LifeAmount$ 3"), p, engine.Battlefield)

	attackWithTargets(t, g, []engine.CardID{toPlayer, toWalker}, []engine.EntityID{engine.PlayerEntity(other), engine.CardEntity(pw)})
	if got := p1p1(g, pw); got != 1 {
		t.Errorf("planeswalker +1/+1 counters = %d, want 1", got)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("defender life = %d, want 20 -- AttackedTarget$ Planeswalker drops the player", got)
	}
}

// TestUnrecordedAttackedTargetStaysUnresolved proves both Defined$ readers
// fail loudly under a mode that recorded no attacked target (GO-7).
func TestUnrecordedAttackedTargetStaysUnresolved(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ name, exec string }{
		{"players", "DB$ LoseLife | Defined$ TriggeredAttackedTarget | LifeAmount$ 1"},
		{"cards", "DB$ PutCounter | Defined$ TriggeredAttackedTarget | CounterType$ P1P1 | CounterNum$ 1"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			g.NewCard(triggerWatcherDef(t, "Test Upkeep Watcher",
				"Mode$ Phase | Phase$ Upkeep | TriggerZones$ Battlefield | Execute$ TrigDo",
				"TrigDo", tc.exec), p, engine.Battlefield)
			g.SetTurnState(2, other, engine.Untap)

			c := engine.NewScriptedController()
			g.AdvancePhase(c)
			err := g.ResolveStack(engine.NewRegistry(), c)
			if err == nil || !strings.Contains(err.Error(), "recorded no attacked target") {
				t.Fatalf("ResolveStack error = %v, want one naming the unrecorded attacked target", err)
			}
		})
	}
}

// TestTriggeredAttackersValidProperties proves the two valid properties Jin
// Sakai's and Squall's counts read (CardProperty.java:2045-2068): with
// ValidAttackers$ keeping the big attacker only, TriggeredAttackers counts it,
// and NotDefinedTriggeredAttackers+attacking TriggeredAttackedTarget counts the
// small one attacking the same player -- not the creature left home.
func TestTriggeredAttackersValidProperties(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		count string
		want  int
	}{
		{"Count$Valid Creature.TriggeredAttackers", 1},
		{"Count$Valid Creature.NotDefinedTriggeredAttackers+attacking TriggeredAttackedTarget", 1},
		{"Count$Valid Creature.NotDefinedTriggeredAttackers", 2},
		{"Count$Valid Creature.attacking TriggeredAttackedTarget", 2},
	} {
		t.Run(tc.count, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			small := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			big := g.NewCard(creatureDefPT(t, "4", "4"), p, engine.Battlefield)
			g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			g.NewCard(triggerWatcherDef(t, "Test Attack Watcher",
				"Mode$ AttackersDeclaredOneTarget | ValidAttackers$ Creature.powerGE3 | AttackedTarget$ Player | TriggerZones$ Battlefield | Execute$ TrigGain",
				"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ X",
				"X", tc.count), p, engine.Battlefield)

			attackAndResolve(t, g, small, big)
			if got := g.Player(p).Life - 20; got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
}

// TestTriggeredAttackersPropertyOutsideItsAbility proves the non-SpellAbility
// reading: a static's Affected$ is matched with no ability in context, so it
// finds no triggering objects -- TriggeredAttackers matches nothing and
// NotDefinedTriggeredAttackers every creature.
func TestTriggeredAttackersPropertyOutsideItsAbility(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		affected string
		want     int
	}{
		{"Creature.TriggeredAttackers", 2},
		{"Creature.NotDefinedTriggeredAttackers", 3},
	} {
		t.Run(tc.affected, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
			g.NewCard(continuousDef(t, "Test Anthem", "Mode$ Continuous | Affected$ "+tc.affected+" | AddPower$ 1"), p, engine.Battlefield)
			engine.CheckStateBasedActions(g, engine.NewScriptedController())
			if pw, ok := g.Card(creature).Power(); !ok || pw != tc.want {
				t.Errorf("Power() = %d, %v, want %d", pw, ok, tc.want)
			}
		})
	}
}

// TestUnrecordedTriggeredAttackersPropertyFailsLoudly proves the GO-7 side of
// the properties: inside its own ability, a trigger mode that recorded no
// attackers (Mode$ Attacks records the one attacker) or no attacked target
// leaves an error rather than reading Java's default empty list.
func TestUnrecordedTriggeredAttackersPropertyFailsLoudly(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ count, want string }{
		{"Count$Valid Creature.TriggeredAttackers", "recorded no attackers"},
		{"Count$Valid Creature.attacking TriggeredAttackedTarget", "recorded no attacked target"},
	} {
		t.Run(tc.count, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			a := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
			g.NewCard(triggerWatcherDef(t, "Test Attack Watcher",
				"Mode$ Attacks | ValidCard$ Creature.YouCtrl | TriggerZones$ Battlefield | Execute$ TrigGain",
				"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ X",
				"X", tc.count), p, engine.Battlefield)

			c := engine.NewScriptedController()
			c.QueueAttackers([]engine.CardID{a})
			declareAttackers(t, g, c)
			err := g.ResolveStack(engine.NewRegistry(), c)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Fatalf("ResolveStack error = %v, want one naming %q", err, tc.want)
			}
		})
	}
}

// TestSquallDealsDamageToTheAttackedOpponent proves Squall, Gunblade Duelist's
// real script end to end: ValidAttackers$ Creature.powerEQChosen reads the
// chosen number (CardProperty.java:1465-1469), the condition counts
// TriggeredAttackers, and Defined$ TriggeredAttackedTarget is the defending
// opponent, dealt damage equal to Squall's power. A chosen number no attacker
// has fires nothing.
func TestSquallDealsDamageToTheAttackedOpponent(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		chosen, want int
	}{
		{4, 17},
		{6, 20},
	} {
		g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
		squall := g.NewCard(corpusCard(t, "Squall, Gunblade Duelist"), p, engine.Battlefield)
		g.Card(squall).Memory.SetChosenNumber(tc.chosen)
		a := g.NewCard(creatureDefPT(t, "4", "1"), p, engine.Battlefield)

		attackAndResolve(t, g, a)
		if got := g.Player(other).Life; got != tc.want {
			t.Errorf("chosen %d: defender life = %d, want %d", tc.chosen, got, tc.want)
		}
	}
}
