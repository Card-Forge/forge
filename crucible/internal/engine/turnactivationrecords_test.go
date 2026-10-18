package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestCountersRemovedThisTurnCountsCardsAndPlayers proves
// Count$CountersRemovedThisTurn <type> <valid> (AbilityUtils.java:2839-2844,
// Game.java:1306-1321): removals from cards and players are recorded as they
// happen -- only what was actually there counts -- read against the card as
// it was or the player, kept by Clone and cleared at cleanup.
func TestCountersRemovedThisTurnCountsCardsAndPlayers(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	libraryCards(t, g, p, 2)
	libraryCards(t, g, other, 2)
	mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	g.Card(mine).Counters.Add("OIL", 3)
	g.Card(theirs).Counters.Add("OIL", 2)
	g.Player(p).Counters.Add(engine.Energy, 5)
	c := engine.NewScriptedController()
	for _, step := range []struct {
		target engine.EntityID
		line   string
	}{
		{engine.CardEntity(mine), "DB$ RemoveCounter | Defined$ Targeted | CounterType$ OIL | CounterNum$ 5"},
		{engine.CardEntity(theirs), "DB$ RemoveCounter | Defined$ Targeted | CounterType$ OIL | CounterNum$ 1"},
		{engine.NoEntity, "DB$ RemoveCounter | Defined$ You | CounterType$ ENERGY | CounterNum$ 4"},
	} {
		var targets []engine.EntityID
		if step.target != engine.NoEntity {
			targets = []engine.EntityID{step.target}
		}
		if _, err := resolveNow(t, g, p, c, targets, step.line); err != nil {
			t.Fatalf("%s: %v", step.line, err)
		}
	}
	for _, tc := range []struct {
		x    string
		want int
	}{
		{"Count$CountersRemovedThisTurn OIL Card.YouCtrl", 3},
		{"Count$CountersRemovedThisTurn OIL Card", 4},
		{"Count$CountersRemovedThisTurn OIL Card.OppCtrl", 1},
		{"Count$CountersRemovedThisTurn ENERGY You", 4},
		{"Count$CountersRemovedThisTurn ENERGY Opponent", 0},
		{"Count$CountersRemovedThisTurn OIL You", 0},
		{"Count$CountersRemovedThisTurn ENERGY Card", 0},
	} {
		if got := measure(t, g, p, tc.x); got != tc.want {
			t.Errorf("%s = %d, want %d", tc.x, got, tc.want)
		}
	}
	for _, x := range []string{"Count$CountersRemovedThisTurn OIL", "Count$CountersRemovedThisTurn ENERGY Player.Bogus"} {
		if err := measureErr(t, g, p, x); err == nil {
			t.Errorf("%s: no error, want unresolved", x)
		}
	}
	clone := g.Clone()
	g.SetTurnState(1, p, engine.EndOfTurn)
	g.AdvancePhase(c) // Cleanup
	if got := measure(t, g, p, "Count$CountersRemovedThisTurn OIL Card"); got != 0 {
		t.Errorf("after cleanup = %d, want 0", got)
	}
	if got := measure(t, clone, p, "Count$CountersRemovedThisTurn OIL Card"); got != 4 {
		t.Errorf("clone = %d, want 4", got)
	}
}

// TestCountersRemovedThisTurnCountsDamagedLoyaltyOnce proves the planeswalker
// half of damage (Card.subtractCounter's delta, Card.java:1910-1911): five
// damage to a two-loyalty planeswalker removes two counters, not five.
func TestCountersRemovedThisTurnCountsDamagedLoyaltyOnce(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	walker := g.NewCard(planeswalkerDefLoyalty(t, "2"), other, engine.Battlefield)
	g.Card(walker).Counters.Add(engine.Loyalty, 2)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(walker)},
		"DB$ DealDamage | Defined$ Targeted | NumDmg$ 5"); err != nil {
		t.Fatal(err)
	}
	if got := measure(t, g, p, "Count$CountersRemovedThisTurn LOYALTY Card"); got != 2 {
		t.Errorf("loyalty removed = %d, want 2", got)
	}
}

// TestThisTurnActivatedCountsActivationsWithTheirHost proves
// Count$ThisTurnActivated_<valid> (AbilityUtils.java:2805-2816): each
// activation is recorded with its host as last known (MagicStack.java:986),
// so a card that exiled itself from the graveyard to pay still reads as in
// the graveyard; the record clears as the next turn begins (:968).
func TestThisTurnActivatedCountsActivationsWithTheirHost(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	libraryCards(t, g, p, 3)
	libraryCards(t, g, other, 3)
	c := engine.NewScriptedController()
	tapper := g.NewCard(copyTestDef(t, "Tapper", "Artifact", "", "",
		"A:AB$ GainLife | Cost$ T | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	grave := g.NewCard(copyTestDef(t, "Grave Ability", "Creature Zombie", "1", "1",
		"A:AB$ GainLife | Cost$ ExileFromGrave<1/CARDNAME> | ActivationZone$ Graveyard | Defined$ You | LifeAmount$ 1"), p, engine.Graveyard)
	for _, id := range []engine.CardID{tapper, grave} {
		if !g.ActivateAbility(p, id, 0, c) {
			t.Fatalf("ActivateAbility(%d) failed", id)
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatal(err)
		}
	}
	if z := g.Card(grave).Zone; z != engine.Exile {
		t.Fatalf("graveyard card in %v, want exiled by its cost", z)
	}
	for _, tc := range []struct {
		x    string
		want int
	}{
		{"Count$ThisTurnActivated_Activated.YouCtrl", 2},
		{"Count$ThisTurnActivated_Activated.Artifact+YouCtrl+inZoneBattlefield", 1},
		{"Count$ThisTurnActivated_Activated.YouCtrl+inZoneGraveyard", 1},
		{"Count$ThisTurnActivated_Activated.OppCtrl", 0},
		{"Count$ThisTurnActivated_Activated.Loyalty+YouCtrl", 0},
	} {
		if got := measure(t, g, p, tc.x); got != tc.want {
			t.Errorf("%s = %d, want %d", tc.x, got, tc.want)
		}
	}
	for _, x := range []string{"Count$ThisTurnActivated_", "Count$ThisTurnActivated_Activated.Bargain"} {
		if err := measureErr(t, g, p, x); err == nil {
			t.Errorf("%s: no error, want unresolved", x)
		}
	}
	clone := g.Clone()
	for g.ActivePhase() != engine.Untap {
		g.AdvancePhase(c)
	}
	if got := measure(t, g, p, "Count$ThisTurnActivated_Activated.YouCtrl"); got != 0 {
		t.Errorf("next turn = %d, want 0", got)
	}
	if got := measure(t, clone, p, "Count$ThisTurnActivated_Activated.YouCtrl"); got != 2 {
		t.Errorf("clone = %d, want 2", got)
	}
}

// TestActivatedThisGameReadsTheRootsRuns proves Count$ActivatedThisGame
// (AbilityUtils.java:1848-1850): an activated ability counts itself (noted as
// it goes on the stack), a static trigger only its earlier runs (noted after
// it resolves, TriggerHandler.java:539), and an ability this port cannot tie
// to its root is unresolved.
func TestActivatedThisGameReadsTheRootsRuns(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	libraryCards(t, g, p, 3)
	libraryCards(t, g, other, 3)
	c := engine.NewScriptedController()
	counter := g.NewCard(copyTestDef(t, "Counter", "Artifact", "", "",
		"A:AB$ GainLife | Cost$ 0 | Defined$ You | LifeAmount$ X", "SVar:X:Count$ActivatedThisGame"), p, engine.Battlefield)
	for want := 1; want <= 2; want++ {
		before := g.Player(p).Life
		if !g.ActivateAbility(p, counter, 0, c) {
			t.Fatal("ActivateAbility failed")
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatal(err)
		}
		if got := g.Player(p).Life - before; got != want {
			t.Errorf("activation %d gained %d, want %d", want, got, want)
		}
	}

	g.NewCard(copyTestDef(t, "Static Counter", "Artifact", "", "",
		"T:Mode$ Phase | Phase$ Upkeep | TriggerZones$ Battlefield | Static$ True | Execute$ Trig",
		"SVar:Trig:DB$ GainLife | Defined$ You | LifeAmount$ X", "SVar:X:Count$ActivatedThisGame"), p, engine.Battlefield)
	g.SetTurnState(2, p, engine.Untap)
	var gains []int
	for range 2 {
		before := g.Player(p).Life
		g.AdvancePhase(c) // Upkeep
		if err := g.TakePendingError(); err != nil {
			t.Fatal(err)
		}
		gains = append(gains, g.Player(p).Life-before)
		for g.ActivePhase() != engine.Untap {
			g.AdvancePhase(c)
		}
	}
	if gains[0] != 0 || gains[1] != 1 {
		t.Errorf("static trigger gains = %v, want [0 1]", gains)
	}

	if err := measureErr(t, g, p, "Count$ActivatedThisGame"); err == nil || !strings.Contains(err.Error(), "not resolvable") {
		t.Errorf("pushed without a root: err = %v, want unresolved", err)
	}
}

// TestDamageToOppsThisTurnSumsTheOpponentsDamage proves the player property
// DamageToOppsThisTurn (AbilityUtils.java:3657-3659, Player.java:815-817,
// Antagonism): each player's value is the damage their opponents were dealt.
func TestDamageToOppsThisTurnSumsTheOpponentsDamage(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	g.Player(p).DamageReceivedThisTurn = 2
	g.Player(other).DamageReceivedThisTurn = 3
	for _, tc := range []struct {
		x    string
		want int
	}{
		{"PlayerCountPlayers$DamageToOppsThisTurn", 5},
		{"PlayerCountOpponents$DamageToOppsThisTurn", 2},
	} {
		if got := measure(t, g, p, tc.x); got != tc.want {
			t.Errorf("%s = %d, want %d", tc.x, got, tc.want)
		}
	}
}

// TestRegeneratedThisTurnCountsTheHostsRegenerations proves
// Count$RegeneratedThisTurn (AbilityUtils.java:2130-2131, Spiny Starfish):
// each regeneration of the host counts (RegenerationEffect.java:41) until
// cleanup (Card.java:7275).
func TestRegeneratedThisTurnCountsTheHostsRegenerations(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	libraryCards(t, g, p, 2)
	libraryCards(t, g, other, 2)
	c := engine.NewScriptedController()
	star := g.NewCard(copyTestDef(t, "Starfish", "Creature Elf", "0", "1",
		"A:AB$ GainLife | Cost$ 0 | Defined$ You | LifeAmount$ X", "SVar:X:Count$RegeneratedThisTurn"), p, engine.Battlefield)
	gained := func() int {
		t.Helper()
		before := g.Player(p).Life
		mustActivate(t, g, p, c, star)
		return g.Player(p).Life - before
	}
	for range 2 {
		g.Card(star).RegenShields = 1
		if _, err := resolveNow(t, g, other, c, []engine.EntityID{engine.CardEntity(star)}, "DB$ Destroy | Defined$ Targeted"); err != nil {
			t.Fatal(err)
		}
	}
	if z := g.Card(star).Zone; z != engine.Battlefield {
		t.Fatalf("starfish in %v, want regenerated on the battlefield", z)
	}
	if n := gained(); n != 2 {
		t.Errorf("after two regenerations: %d, want 2", n)
	}
	g.SetTurnState(1, p, engine.EndOfTurn)
	g.AdvancePhase(c) // Cleanup
	if n := gained(); n != 0 {
		t.Errorf("after cleanup: %d, want 0", n)
	}
}

// TestFinishedUpkeepsResetAsCleanupBegins proves PhaseHandler.java:415: the
// upkeep count is zeroed as cleanup begins, not as the next turn does.
func TestFinishedUpkeepsResetAsCleanupBegins(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	libraryCards(t, g, p, 2)
	libraryCards(t, g, other, 2)
	c := engine.NewScriptedController()
	g.SetTurnState(1, p, engine.Untap)
	for g.ActivePhase() != engine.EndOfTurn {
		g.AdvancePhase(c)
	}
	if n := measure(t, g, p, "Count$FinishedUpkeepsThisTurn"); n != 1 {
		t.Errorf("end step: %d, want 1", n)
	}
	g.AdvancePhase(c) // Cleanup
	if n := measure(t, g, p, "Count$FinishedUpkeepsThisTurn"); n != 0 {
		t.Errorf("cleanup: %d, want 0", n)
	}
}
