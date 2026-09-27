package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// runChaos resolves line (a DB$ RunChaos record, plus svars) from a fresh
// host p controls, its Remembered list seeded with remembered first
// (Defined$ Remembered, Pools of Becoming's shape), and returns the stack's
// error.
func runChaos(t *testing.T, g *engine.Game, p engine.PlayerID, remembered []engine.CardID, line string, svars ...string) error {
	t.Helper()
	def := etbChainDef(t, "Chaos Runner", line, svars...)
	host := g.NewCard(def, p, engine.Battlefield)
	for _, id := range remembered {
		g.Card(host).Memory.Remember(engine.CardEntity(id))
	}
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if strings.EqualFold(sub.Key, "Execute") {
			g.PushAbility(engine.Ability{API: engine.APIRunChaos, Source: host, Controller: p, Params: sub.Ability, Amounts: face.Amounts})
			return g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController())
		}
	}
	t.Fatal("no Execute$")
	return nil
}

// TestRunChaosTriggersEachRevealedPlanesChaosAbility proves Pools of
// Becoming's "each of the revealed cards' {CHAOS} abilities triggers"
// (RunChaosEffect.java:20-38): every remembered plane's chaos ability fires
// from inside the planar deck, once per plane however often it is
// remembered (getTargetCards is a CardCollection); a plane with none adds
// nothing; chaos does not ensue, so the active plane's own chaos ability
// stays silent; and the copies go on the stack, resolving only after the
// rest of the RunChaos chain (the SetLife below).
func TestRunChaosTriggersEachRevealedPlanesChaosAbility(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	faceUpPlane(g, p, gainOnChaos(t, "Active", "1"))
	deck := planarDeck(g, p, gainOnChaos(t, "First", "3"), plainPlane(t, "Blank"), gainOnChaos(t, "Third", "10"))

	remembered := []engine.CardID{deck[0], deck[1], deck[2], deck[0]}
	if err := runChaos(t, g, p, remembered, "DB$ RunChaos | Defined$ Remembered | SubAbility$ After",
		"After", "DB$ SetLife | Defined$ You | LifeAmount$ 5"); err != nil {
		t.Fatalf("RunChaos: %v", err)
	}
	if got := g.Player(p).Life; got != 18 {
		t.Errorf("life = %d, want 18 (set to 5, then 3 + 10 from the two revealed planes)", got)
	}
	for _, id := range deck {
		if c := g.Card(id); c.Zone != engine.PlanarDeck {
			t.Errorf("%s moved to %v", c.Def.Name, c.Zone)
		}
	}
}

// TestRunChaosStacksInTheOrderFound proves the copies keep getTargetCards'
// order on the stack (pushTriggeredAbilities has no ordering decision; the
// AI's orderAndPlaySimultaneousSa plays them in list order,
// PlayerControllerAi.java:1296-1321): the last card's ability is on top and
// resolves first.
func TestRunChaosStacksInTheOrderFound(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	deck := planarDeck(g, p,
		planarDef(t, "Setter", "Plane", []string{chaosTrigger}, nil, "Chaos", "DB$ SetLife | Defined$ You | LifeAmount$ 10"),
		gainOnChaos(t, "Gainer", "3"))

	if err := runChaos(t, g, p, deck, "DB$ RunChaos | Defined$ Remembered"); err != nil {
		t.Fatalf("RunChaos: %v", err)
	}
	if got := g.Player(p).Life; got != 10 {
		t.Errorf("life = %d, want 10 (Gainer's ability resolves first, Setter's last)", got)
	}
}

// TestRunChaosCopiesBelongToTheActivator proves RunChaosEffect.java:23's
// copy(sa.getActivatingPlayer()): a plane from the other player's planar
// deck triggers under the activator, so its "you" is the activator. No
// triggering player is recorded: Java never calls setTriggeringObjects on
// the copies, so a body reading Defined$ TriggeredPlayer (0 of 158 real chaos
// abilities) gets the defined.go error.
func TestRunChaosCopiesBelongToTheActivator(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	deck := planarDeck(g, other, planarDef(t, "Theirs", "Plane", []string{chaosTrigger}, nil,
		"Chaos", "DB$ GainLife | Defined$ You | LifeAmount$ 4"))

	if err := runChaos(t, g, p, deck, "DB$ RunChaos | Defined$ Remembered"); err != nil {
		t.Fatalf("RunChaos: %v", err)
	}
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("activator's life = %d, want 24", got)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("plane owner's life = %d, want 20", got)
	}

	reader := planarDeck(g, other, planarDef(t, "Reader", "Plane", []string{chaosTrigger}, nil,
		"Chaos", "DB$ LoseLife | Defined$ TriggeredPlayer | LifeAmount$ 2"))
	err := runChaos(t, g, p, reader, "DB$ RunChaos | Defined$ Remembered")
	if err == nil || !strings.Contains(err.Error(), "recorded no player") {
		t.Errorf("TriggeredPlayer: err = %v, want the no-player error", err)
	}
}

// TestRunChaosNeedsNoPlanechaseGame proves RunChaosEffect.java has no
// getActivePlanes() gate (unlike ChaosEnsuesEffect.java:33): a chaos
// ability on a card outside any planar deck still triggers in a normal game.
// With no Defined$ the host itself is the card (Self), and it has none.
func TestRunChaosNeedsNoPlanechaseGame(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	exiled := g.NewCard(gainOnChaos(t, "Exiled", "2"), p, engine.Exile)
	if g.PlanechaseActive() {
		t.Fatal("PlanechaseActive with no planar deck")
	}
	if err := runChaos(t, g, p, []engine.CardID{exiled}, "DB$ RunChaos | Defined$ Remembered"); err != nil {
		t.Fatalf("RunChaos: %v", err)
	}
	if err := runChaos(t, g, p, nil, "DB$ RunChaos"); err != nil {
		t.Fatalf("RunChaos (Self): %v", err)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22", got)
	}
}

// TestRunChaosConditionFalseDoesNothing proves the Condition* gate every
// sub-ability shares runs first.
func TestRunChaosConditionFalseDoesNothing(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	deck := planarDeck(g, p, gainOnChaos(t, "Revealed", "2"))
	if err := runChaos(t, g, p, deck, "DB$ RunChaos | Defined$ Remembered | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", "X", "0"); err != nil {
		t.Fatalf("RunChaos: %v", err)
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("life = %d, want 20", got)
	}
}

// TestRunChaosRejectsUnportedChaosTriggers proves every chaos-trigger shape
// this port does not resolve fails the whole RunChaos before any copy
// reaches the stack -- a good plane remembered first stays silent.
// OptionalDecider$ (6 real lines) and Cost$ never reach the copy in Java
// (RunChaosEffect.java:27,30 set the flag on the RunChaos ability,
// forge-java-defects.md); the rest have no real line.
func TestRunChaosRejectsUnportedChaosTriggers(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ trigger, want string }{
		{chaosTrigger + " | OptionalDecider$ You", "OptionalDecider$ not resolvable yet"},
		{chaosTrigger + " | Cost$ 1", "Cost$ not resolvable yet"},
		{chaosTrigger + " | Static$ True", "Static$ not resolvable yet"},
		{chaosTrigger + " | TriggerController$ Opponent", "TriggerController$ not resolvable yet"},
		{"Mode$ ChaosEnsues | TriggerZones$ Command", "without Execute$ not resolvable yet"},
		{"Mode$ ChaosEnsues | TriggerZones$ Command | Execute$ Odd", `Execute$ API "Bogus" not resolvable yet`},
	} {
		g, p, _ := newTwoPlayerGame(t)
		deck := planarDeck(g, p, gainOnChaos(t, "Good", "3"),
			planarDef(t, "Bad", "Plane", []string{tc.trigger}, nil, "Chaos", "DB$ GainLife | Defined$ You | LifeAmount$ 7", "Odd", "DB$ Bogus"))
		err := runChaos(t, g, p, deck, "DB$ RunChaos | Defined$ Remembered")
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want %q", tc.trigger, err, tc.want)
		}
		if got := g.Player(p).Life; got != 20 {
			t.Errorf("%s: life = %d, want 20 (nothing fires)", tc.trigger, got)
		}
	}
}

// TestRunChaosRejectsAnUnresolvableDefined proves a Defined$ spelling
// definedCards does not know is an error, not an empty card list.
func TestRunChaosRejectsAnUnresolvableDefined(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	err := runChaos(t, g, p, nil, "DB$ RunChaos | Defined$ Bogus")
	if err == nil || !strings.Contains(err.Error(), "not resolvable yet") {
		t.Errorf("err = %v, want a not resolvable yet error", err)
	}
}
