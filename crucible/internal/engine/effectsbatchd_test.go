package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// contraptionDef is a Contraption carrying one Mode$ CrankContraption trigger
// -- clock_of_doooooooooooom.txt's own real corpus shape (widget_contraption.txt
// et al.'s own "T:Mode$ CrankContraption | ValidCard$ Card.Self").
func contraptionDef(t *testing.T, name, trigger string, svars ...string) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Artifact Contraption")
	raw.Faces[0].Triggers = []string{trigger}
	for i := 0; i+1 < len(svars); i += 2 {
		raw.Faces[0].SVars.Set(svars[i], svars[i+1])
	}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// TestAdvanceCrankEffectAdvancesCounterAndCranksChosenContraption proves
// AdvanceCrank's own one real corpus shape (clock_of_doooooooooooom.txt's
// bare "AB$ AdvanceCrank"): the activator's own CRANK! counter moves from
// its default (3) to the next sprocket (1, Player.java:4015's own
// "crankCounter % 3 + 1"), and a Contraption dialed to that sprocket the
// controller chooses to crank fires its own Mode$ CrankContraption trigger.
func TestAdvanceCrankEffectAdvancesCounterAndCranksChosenContraption(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	onSprocket1 := g.NewCard(contraptionDef(t, "Test Contraption 1",
		"Mode$ CrankContraption | ValidCard$ Card.Self | Execute$ TrigCrank",
		"TrigCrank", "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1"), p, engine.Battlefield)
	g.Card(onSprocket1).Sprocket = 1
	// Dialed to a sprocket the counter never lands on this turn -- proves the
	// sprocket filter, not just "every Contraption cranks."
	onSprocket2 := g.NewCard(contraptionDef(t, "Test Contraption 2",
		"Mode$ CrankContraption | ValidCard$ Card.Self | Execute$ TrigCrank",
		"TrigCrank", "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1"), p, engine.Battlefield)
	g.Card(onSprocket2).Sprocket = 2

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{onSprocket1})
	resolveLine(t, g, p, c, "DB$ AdvanceCrank")

	if got := g.Player(p).CrankCounter; got != 1 {
		t.Errorf("CrankCounter = %d, want 1", got)
	}
	if got := g.Card(onSprocket1).Counters.Count(engine.CounterType("P1P1")); got != 1 {
		t.Errorf("onSprocket1 P1P1 counters = %d, want 1 (chosen and cranked)", got)
	}
	if got := g.Card(onSprocket2).Counters.Count(engine.CounterType("P1P1")); got != 0 {
		t.Errorf("onSprocket2 P1P1 counters = %d, want 0 (wrong sprocket this turn)", got)
	}
}

// TestAdvanceCrankEffectNoContraptionsIsANoOp proves the counter still
// advances with nothing on the battlefield to crank, and no
// ChooseCardsForEffect call happens at all (a queued choice with nothing
// queued would panic the ScriptedController).
func TestAdvanceCrankEffectNoContraptionsIsANoOp(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ AdvanceCrank")

	if got := g.Player(p).CrankCounter; got != 1 {
		t.Errorf("CrankCounter = %d, want 1", got)
	}
}

// TestAdvanceCrankEffectChoosingNoneSkipsEveryTrigger proves "any number,"
// including zero: a contraption on the landed sprocket that the controller
// declines to crank does not fire.
func TestAdvanceCrankEffectChoosingNoneSkipsEveryTrigger(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	onSprocket1 := g.NewCard(contraptionDef(t, "Test Contraption",
		"Mode$ CrankContraption | ValidCard$ Card.Self | Execute$ TrigCrank",
		"TrigCrank", "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1"), p, engine.Battlefield)
	g.Card(onSprocket1).Sprocket = 1

	c := engine.NewScriptedController()
	c.QueueCardChoice(nil)
	resolveLine(t, g, p, c, "DB$ AdvanceCrank")

	if got := g.Card(onSprocket1).Counters.Count(engine.CounterType("P1P1")); got != 0 {
		t.Errorf("P1P1 counters = %d, want 0 (declined)", got)
	}
}

// claimPrizeDef is an Attraction carrying one Mode$ ClaimPrize trigger --
// pick_a_beeble.txt's own K:Prize shape, hand-written here since this port
// does not compile the Prize keyword itself yet (only the literal T: line
// the_most_dangerous_gamer.txt also carries).
func claimPrizeDef(t *testing.T, name, validCard string) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Artifact Attraction")
	raw.Faces[0].Triggers = []string{
		"Mode$ ClaimPrize | ValidCard$ " + validCard + " | Execute$ TrigProof",
	}
	raw.Faces[0].SVars.Set("TrigProof", "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1")
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// TestClaimThePrizeEffectFiresOwnClaimPrizeTrigger proves ClaimThePrize's own
// dominant real corpus shape (pick_a_beeble.txt's bare "DB$ ClaimThePrize",
// Defined$ defaulting to Self): claiming the prize of an Attraction fires
// that Attraction's own Mode$ ClaimPrize trigger.
func TestClaimThePrizeEffectFiresOwnClaimPrizeTrigger(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	attraction := g.NewCard(claimPrizeDef(t, "Test Attraction", "Card.Self"), p, engine.Battlefield)

	g.PushAbility(engine.Ability{
		API:        engine.APIClaimThePrize,
		Source:     attraction,
		Controller: p,
		Params:     &compile.Ability{Name: "ClaimThePrize"},
	})
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	if got := g.Card(attraction).Counters.Count(engine.CounterType("P1P1")); got != 1 {
		t.Errorf("attraction P1P1 counters = %d, want 1", got)
	}
}

// TestClaimThePrizeEffectDefinedAndWatcherTrigger proves Defined$ (a
// different card than the ability's own host) and the_most_dangerous_gamer.
// txt's own watcher shape together: a permanent that never itself claims a
// prize can still watch another player-controlled Attraction claim one.
func TestClaimThePrizeEffectDefinedAndWatcherTrigger(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	attraction := g.NewCard(claimPrizeDef(t, "Test Attraction", "Attraction.YouCtrl"), p, engine.Battlefield)

	claimerDef := etbChainDef(t, "Test Claimer", "DB$ ClaimThePrize | Defined$ Remembered")
	host := g.NewCard(claimerDef, p, engine.Battlefield)
	g.Card(host).Memory.Remember(engine.CardEntity(attraction))

	face := claimerDef.Faces[0]
	pushed := false
	for _, sub := range face.Triggers[0].Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		api, ok := engine.APIByName(sub.Ability.Name)
		if !ok {
			t.Fatalf("unknown API %q", sub.Ability.Name)
		}
		g.PushAbility(engine.Ability{API: api, Source: host, Controller: p, Params: sub.Ability, Amounts: face.Amounts})
		pushed = true
	}
	if !pushed {
		t.Fatal("no Execute$")
	}
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	if got := g.Card(attraction).Counters.Count(engine.CounterType("P1P1")); got != 1 {
		t.Errorf("attraction P1P1 counters = %d, want 1", got)
	}
}

// TestAdvanceCrankEffectRejectsUnresolvableDefined proves the shared
// targetedOrDefinedPlayers error path surfaces rather than being silently
// swallowed (GO-7): a Defined$ spelling this port's definedPlayers does not
// know errors instead of resolving to nothing.
func TestAdvanceCrankEffectRejectsUnresolvableDefined(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ AdvanceCrank | Defined$ Bogus"); err == nil {
		t.Fatal("want an error for Defined$ Bogus, got nil")
	}
}

// TestClaimThePrizeEffectRejectsUnresolvableDefined is
// TestAdvanceCrankEffectRejectsUnresolvableDefined's own sibling for
// targetedOrDefinedCards.
func TestClaimThePrizeEffectRejectsUnresolvableDefined(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ ClaimThePrize | Defined$ Bogus"); err == nil {
		t.Fatal("want an error for Defined$ Bogus, got nil")
	}
}

// TestClaimThePrizeEffectRejectsConditionDefined pins pick_a_beeble.txt's own
// real corpus line ("ConditionDefined$ Self | ConditionPresent$
// Card.Self+counters_GE6_LUCK"): isPresentMatches (trigger.go) treats any
// ConditionDefined$ as never met and returns false silently, which would
// make this resolve to an invisible no-op on the corpus's only real use.
// Rejected loudly instead (GO-7), the same as every other effect naming
// ConditionDefined$ -- found and fixed porting SetInMotion.
func TestClaimThePrizeEffectRejectsConditionDefined(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
		"DB$ ClaimThePrize | ConditionDefined$ Self | ConditionPresent$ Card.Self+counters_GE6_LUCK")
	if err == nil || !strings.Contains(err.Error(), "ConditionDefined$ not resolvable yet") {
		t.Errorf("err = %v, want ConditionDefined$ rejected", err)
	}
}

// TestClaimThePrizeEffectConditionPresentFalseIsANoOp proves
// subAbilityConditionMet's gate applies here the same as every other effect
// (pick_a_beeble.txt's own real "ConditionPresent$ Card.Self+counters_GE6_
// LUCK" shape): an unmet condition skips ClaimThePrize's own trigger fire
// entirely, rather than claiming anyway.
func TestClaimThePrizeEffectConditionPresentFalseIsANoOp(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	attraction := g.NewCard(claimPrizeDef(t, "Test Attraction", "Card.Self"), p, engine.Battlefield)

	g.PushAbility(engine.Ability{
		API:        engine.APIClaimThePrize,
		Source:     attraction,
		Controller: p,
		Params: &compile.Ability{Name: "ClaimThePrize", Params: []vocab.Param{
			{Key: "ConditionPresent", Value: "Card.Self+counters_GE6_LUCK"},
		}},
	})
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	if got := g.Card(attraction).Counters.Count(engine.CounterType("P1P1")); got != 0 {
		t.Errorf("attraction P1P1 counters = %d, want 0 (condition unmet)", got)
	}
}

// TestClaimThePrizeEffectValidCardMismatchDoesNotFire proves a watching
// Mode$ ClaimPrize trigger whose ValidCard$ does not match the claimed card
// stays silent -- the_most_dangerous_gamer.txt's own ValidCard$
// Attraction.YouCtrl would not fire for an opponent's Attraction, the
// mismatch this test stands in for with a plain ValidCard$ Creature.
func TestClaimThePrizeEffectValidCardMismatchDoesNotFire(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	watcher := g.NewCard(claimPrizeDef(t, "Test Watcher", "Creature"), p, engine.Battlefield)
	attraction := g.NewCard(claimPrizeDef(t, "Test Attraction", "Creature"), p, engine.Battlefield)
	// claimPrizeDef's own ValidCard$ "Creature" does not match an Artifact
	// Attraction either, keeping the claimed card's own trigger from also
	// firing so only the watcher's own mismatch is under test.

	g.PushAbility(engine.Ability{
		API:        engine.APIClaimThePrize,
		Source:     attraction,
		Controller: p,
		Params:     &compile.Ability{Name: "ClaimThePrize"},
	})
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	if got := g.Card(watcher).Counters.Count(engine.CounterType("P1P1")); got != 0 {
		t.Errorf("watcher P1P1 counters = %d, want 0 (ValidCard$ Creature does not match an Attraction)", got)
	}
}
