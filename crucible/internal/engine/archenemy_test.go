package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// My Laughter Echoes' lines, verbatim from
// forge-gui/res/cardsfolder/m/my_laughter_echoes.txt: the only real card
// whose DB$ SetInMotion resolves today.
const (
	echoesTrigger = "Mode$ SetInMotion | ValidCard$ !Ongoing | Execute$ Abandon | TriggerZones$ Command | " +
		"TriggerDescription$ Whenever you set a non-ongoing scheme in motion, you may abandon this scheme. " +
		"If you do, set that scheme in motion again."
	echoesAbandon = "DB$ Abandon | Optional$ True | RememberAbandoned$ True | SubAbility$ DBSetInMotionAgain"
	echoesAgain   = "DB$ SetInMotion | Again$ True | ConditionDefined$ Remembered | ConditionPresent$ Card | " +
		"SubAbility$ DBCleanup"
	echoesCleanup = "DB$ Cleanup | ClearRemembered$ True"

	// selfSchemeTrigger is the dominant real Mode$ SetInMotion shape (77 of
	// 85 lines): a scheme's own "When you set this scheme in motion, ...".
	selfSchemeTrigger = "Mode$ SetInMotion | ValidCard$ Card.Self | Execute$ TrigGain | TriggerZones$ Command | " +
		"TriggerDescription$ When you set this scheme in motion, you gain 3 life."
)

// schemeDeck seats n gain-3 schemes as p's scheme deck, first on top.
func schemeDeck(t *testing.T, g *engine.Game, p engine.PlayerID, n int) []engine.CardID {
	t.Helper()
	ids := make([]engine.CardID, n)
	for i := range ids {
		def := planarDef(t, "Test Scheme", "Scheme", []string{selfSchemeTrigger}, nil,
			"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 3")
		ids[i] = g.NewCard(def, p, engine.SchemeDeck)
	}
	return ids
}

// TestSetInMotionMovesTopSchemeAndFiresItsOwnTrigger proves the effect's
// bare shape end to end: the top scheme goes face up to Command, its own
// Mode$ SetInMotion trigger (ValidCard$ Card.Self, TriggerZones$ Command)
// resolves, and CR 704.6f's scheme state-based action then returns it to
// the bottom of the deck (GameAction.java:1745-1752).
func TestSetInMotionMovesTopSchemeAndFiresItsOwnTrigger(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	deck := schemeDeck(t, g, p, 2)
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ SetInMotion")
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23: the scheme's own set-in-motion trigger must resolve", got)
	}
	if got := g.Zone(engine.SchemeDeck, p).Cards(); len(got) != 2 || got[0] != deck[1] || got[1] != deck[0] {
		t.Errorf("scheme deck = %v, want [%v %v]: the finished scheme goes to the bottom", got, deck[1], deck[0])
	}
}

// TestSetInMotionRepeatNum proves RepeatNum$ sets that many schemes in
// motion, each firing its own trigger.
func TestSetInMotionRepeatNum(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	schemeDeck(t, g, p, 3)
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ SetInMotion | RepeatNum$ 2")
	if got := g.Player(p).Life; got != 26 {
		t.Errorf("life = %d, want 26 (two schemes set in motion)", got)
	}
}

// TestSetInMotionErrors proves each refused shape fails loudly, before
// anything moves (GO-7).
func TestSetInMotionErrors(t *testing.T) {
	t.Parallel()

	cases := []struct {
		name, line, want string
		deck             int
		svars            []string
	}{
		{"empty scheme deck", "DB$ SetInMotion", "scheme deck is empty", 0, nil},
		{"Again$ without a trigger", "DB$ SetInMotion | Again$ True", "no triggering scheme", 1, nil},
		{"RepeatNum$ unresolvable", "DB$ SetInMotion | RepeatNum$ Bogus", "RepeatNum", 1, nil},
		{"ConditionDefined$ beside another condition",
			"DB$ SetInMotion | ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCheckSVar$ X",
			"ConditionCheckSVar$ beside ConditionDefined$", 1, nil},
		{"ConditionDefined$ without ConditionPresent$",
			"DB$ SetInMotion | ConditionDefined$ Remembered", "without ConditionPresent$", 1, nil},
		{"ConditionDefined$ unknown", "DB$ SetInMotion | ConditionDefined$ Bogus | ConditionPresent$ Card",
			"ConditionDefined$", 1, nil},
		{"ConditionCompare$ too short",
			"DB$ SetInMotion | ConditionDefined$ Self | ConditionPresent$ Card | ConditionCompare$ GE",
			"ConditionCompare$", 1, nil},
		{"ConditionCompare$ unresolvable",
			"DB$ SetInMotion | ConditionDefined$ Self | ConditionPresent$ Card | ConditionCompare$ GEBogus",
			"ConditionCompare$", 1, nil},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			deck := schemeDeck(t, g, p, tc.deck)
			_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line, tc.svars...)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Fatalf("err = %v, want one naming %q", err, tc.want)
			}
			for _, id := range deck {
				if got := g.Card(id).Zone; got != engine.SchemeDeck {
					t.Errorf("scheme zone = %v, want SchemeDeck: nothing moves on an error", got)
				}
			}
		})
	}
}

// TestSetInMotionConditionDefinedGates proves ConditionDefined$ counts the
// ConditionPresent$ matches among the defined cards
// (SpellAbilityCondition.java:348-373): Self is present, so GE1 sets a
// scheme in motion and EQ0 does not.
func TestSetInMotionConditionDefinedGates(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		compare string
		moved   bool
	}{{"GE1", true}, {"EQ0", false}} {
		g, p, _ := newTwoPlayerGame(t)
		deck := schemeDeck(t, g, p, 1)
		resolveLine(t, g, p, engine.NewScriptedController(),
			"DB$ SetInMotion | ConditionDefined$ Self | ConditionPresent$ Card | ConditionCompare$ "+tc.compare)
		if got := g.Player(p).Life == 23; got != tc.moved {
			t.Errorf("ConditionCompare$ %s: set in motion = %v, want %v", tc.compare, got, tc.moved)
		}
		if g.Card(deck[0]).Zone != engine.SchemeDeck {
			t.Errorf("ConditionCompare$ %s: scheme must end in the SchemeDeck", tc.compare)
		}
	}
}

// TestSetInMotionRefusesALiveReplacement proves a live Event$ SetInMotion
// replacement (ReplacementType.SetInMotion, not ported) refuses the action
// rather than being ignored -- Plots That Span Centuries' effect shape.
func TestSetInMotionRefusesALiveReplacement(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	deck := schemeDeck(t, g, p, 1)
	g.NewCard(planarDef(t, "Plots Test", "Creature Elf", nil,
		[]string{"Event$ SetInMotion | ActiveZones$ Battlefield | ReplaceWith$ PlotRep"},
		"PlotRep", "DB$ SetInMotion | RepeatNum$ 3"), p, engine.Battlefield)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ SetInMotion")
	if err == nil || !strings.Contains(err.Error(), "replacement not resolvable yet") {
		t.Fatalf("err = %v, want the replacement refusal", err)
	}
	if g.Card(deck[0]).Zone != engine.SchemeDeck {
		t.Error("a refused set-in-motion must move nothing")
	}
}

// echoesGame seats My Laughter Echoes face up in p's Command zone and a
// one-scheme deck under it.
func echoesGame(t *testing.T) (*engine.Game, engine.PlayerID, engine.CardID, engine.CardID) {
	t.Helper()
	g, p, _ := newTwoPlayerGame(t)
	echoes := g.NewCard(planarDef(t, "My Laughter Echoes", "Ongoing Scheme", []string{echoesTrigger}, nil,
		"Abandon", echoesAbandon, "DBSetInMotionAgain", echoesAgain, "DBCleanup", echoesCleanup), p, engine.Command)
	return g, p, echoes, schemeDeck(t, g, p, 1)[0]
}

// TestMyLaughterEchoesSetsTheSchemeInMotionAgain proves the one reachable
// real DB$ SetInMotion line: Echoes watches a non-ongoing scheme being set
// in motion (ValidCard$ !Ongoing), is abandoned (Optional$ accepted,
// RememberAbandoned$), and Again$ sets the triggering scheme
// (AbilityKey.Scheme, carried down the SubAbility$ chain) in motion once
// more -- its own trigger fires twice.
func TestMyLaughterEchoesSetsTheSchemeInMotionAgain(t *testing.T) {
	t.Parallel()

	g, p, echoes, scheme := echoesGame(t)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)
	resolveLine(t, g, p, c, "DB$ SetInMotion")
	if got := g.Player(p).Life; got != 26 {
		t.Errorf("life = %d, want 26: the scheme is set in motion twice", got)
	}
	if got := g.Zone(engine.SchemeDeck, p).Cards(); len(got) != 2 || got[0] != echoes || got[1] != scheme {
		t.Errorf("scheme deck = %v, want [%v %v]", got, echoes, scheme)
	}
	if got := g.Card(echoes).Memory.Remembered(); len(got) != 0 {
		t.Errorf("Echoes remembers %v after DBCleanup, want nothing", got)
	}
}

// TestMyLaughterEchoesDeclinedDoesNotRepeat proves the ConditionDefined$
// Remembered gate: a declined abandon remembers nothing, so Again$ does not
// run.
func TestMyLaughterEchoesDeclinedDoesNotRepeat(t *testing.T) {
	t.Parallel()

	g, p, echoes, _ := echoesGame(t)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	resolveLine(t, g, p, c, "DB$ SetInMotion")
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23: declined, the scheme is set in motion once", got)
	}
	if g.Card(echoes).Zone != engine.Command {
		t.Error("a declined Echoes stays in Command")
	}
}

// TestMyLaughterEchoesIgnoresOngoingSchemes proves ValidCard$ !Ongoing: an
// Ongoing scheme set in motion does not trigger Echoes, and an Ongoing
// scheme is not returned by the scheme state-based action.
func TestMyLaughterEchoesIgnoresOngoingSchemes(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(planarDef(t, "My Laughter Echoes", "Ongoing Scheme", []string{echoesTrigger}, nil,
		"Abandon", echoesAbandon, "DBSetInMotionAgain", echoesAgain, "DBCleanup", echoesCleanup), p, engine.Command)
	ongoing := g.NewCard(planarDef(t, "Ongoing Test", "Ongoing Scheme", nil, nil), p, engine.SchemeDeck)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ SetInMotion")
	if g.StackLen() != 0 {
		t.Errorf("stack = %d, want empty: Echoes must not trigger on an Ongoing scheme", g.StackLen())
	}
	if g.Card(ongoing).Zone != engine.Command {
		t.Error("an Ongoing scheme stays face up in Command")
	}
}

// TestArchenemySetsASchemeInMotionAtMain1 proves the turn-based action
// (PhaseHandler.java:278-280, CR 904.4): as the archenemy's precombat main
// phase begins, the top scheme is set in motion; its trigger waits on the
// stack and the scheme stays in Command while it does (hasSourceOnStack).
// The opponent's scheme deck is untouched on the archenemy's turn.
func TestArchenemySetsASchemeInMotionAtMain1(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Draw)
	mine := schemeDeck(t, g, p, 2)
	theirs := schemeDeck(t, g, other, 1)
	c := engine.NewScriptedController()
	g.AdvancePhase(c)
	if err := g.TakePendingError(); err != nil {
		t.Fatalf("Main1: %v", err)
	}
	if g.ActivePhase() != engine.Main1 {
		t.Fatalf("phase = %v, want Main1", g.ActivePhase())
	}
	if g.Card(mine[0]).Zone != engine.Command || g.StackLen() != 1 {
		t.Fatalf("top scheme zone = %v, stack = %d; want Command with its trigger waiting", g.Card(mine[0]).Zone, g.StackLen())
	}
	if g.Card(theirs[0]).Zone != engine.SchemeDeck {
		t.Error("only the active archenemy sets a scheme in motion")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23", got)
	}
	if g.Card(mine[0]).Zone != engine.SchemeDeck {
		t.Error("the finished scheme must return to the scheme deck once its trigger has left the stack")
	}
}

// TestNoSchemeDeckNoMain1Action proves Player.isArchenemy's gate: a player
// with an empty scheme deck begins Main1 with nothing set in motion.
func TestNoSchemeDeckNoMain1Action(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Draw)
	g.AdvancePhase(engine.NewScriptedController())
	if err := g.TakePendingError(); err != nil {
		t.Fatalf("Main1: %v", err)
	}
	if g.StackLen() != 0 || len(g.Zone(engine.Command, p).Cards()) != 0 {
		t.Error("a player with no scheme deck sets nothing in motion")
	}
}

// TestMain1SetInMotionErrorIsPending proves a Main1 failure (here the
// refused replacement) reaches TakePendingError instead of vanishing in
// bookkeeping mode.
func TestMain1SetInMotionErrorIsPending(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Draw)
	deck := schemeDeck(t, g, p, 1)
	g.NewCard(planarDef(t, "Plots Test", "Creature Elf", nil,
		[]string{"Event$ SetInMotion | ActiveZones$ Battlefield | ReplaceWith$ PlotRep"},
		"PlotRep", "DB$ SetInMotion | RepeatNum$ 3"), p, engine.Battlefield)
	g.AdvancePhase(engine.NewScriptedController())
	if err := g.TakePendingError(); err == nil || !strings.Contains(err.Error(), "SetInMotion") {
		t.Fatalf("pending = %v, want the SetInMotion refusal", err)
	}
	if g.Card(deck[0]).Zone != engine.SchemeDeck {
		t.Error("a refused Main1 set-in-motion moves nothing")
	}
}

// TestSchemeStateBasedAction proves CR 704.6f's own conditions: a
// non-ongoing scheme in Command stays while it is the source of an ability
// on the stack and returns to the bottom of its owner's scheme deck once it
// is not; an Ongoing scheme never does.
func TestSchemeStateBasedAction(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	under := schemeDeck(t, g, p, 1)[0]
	def := planarDef(t, "Test Scheme", "Scheme", nil, nil)
	busy := g.NewCard(def, p, engine.Command)
	idle := g.NewCard(def, p, engine.Command)
	ongoing := g.NewCard(planarDef(t, "Ongoing Test", "Ongoing Scheme", nil, nil), p, engine.Command)
	gain := etbChainDef(t, "Gain", "DB$ GainLife | Defined$ You | LifeAmount$ 1").Faces[0].Triggers[0].Subs[0].Ability
	g.PushAbility(engine.Ability{API: engine.APIGainLife, Source: busy, Controller: p, Params: gain})
	c := engine.NewScriptedController()
	engine.CheckStateBasedActions(g, c)
	if g.Card(busy).Zone != engine.Command || g.Card(ongoing).Zone != engine.Command {
		t.Error("a scheme with an ability on the stack, and an Ongoing scheme, stay in Command")
	}
	if got := g.Zone(engine.SchemeDeck, p).Cards(); len(got) != 2 || got[0] != under || got[1] != idle {
		t.Errorf("scheme deck = %v, want [%v %v]: the idle scheme goes to the bottom", got, under, idle)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(busy).Zone != engine.SchemeDeck {
		t.Error("once its ability has left the stack, the scheme returns to the deck")
	}
}

// TestOngoingSchemePhaseTriggerFiresFromCommand proves a face-up Ongoing
// scheme's own Mode$ Phase line (17 real lines on Ongoing schemes) fires
// from the Command zone: checkPhaseTriggers walks phaseTriggerZones, which
// include Command, not traitHosts.
func TestOngoingSchemePhaseTriggerFiresFromCommand(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Untap)
	g.NewCard(planarDef(t, "Ongoing Upkeep", "Ongoing Scheme",
		[]string{"Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Command | Execute$ TrigGain"}, nil,
		"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 2"), p, engine.Command)
	c := engine.NewScriptedController()
	g.AdvancePhase(c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22: the Ongoing scheme's upkeep trigger must fire from Command", got)
	}
}
