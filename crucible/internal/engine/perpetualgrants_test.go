package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// perpetualGranterDef is Racketeer Boss's shape (racketeer_boss.txt) over a
// Clue instead of a Treasure: when it enters, choose a creature card in your
// hand; it perpetually gains castTrig, whose Execute$ is TrigToken. extra
// SVars follow as name/body pairs.
func perpetualGranterDef(t *testing.T, castTrig, trigToken, loseLine string, extra ...string) *compile.Card {
	t.Helper()
	svars := append([]string{
		"DBAnimate", "DB$ Animate | Defined$ ChosenCard | Triggers$ CastTrig | Duration$ Perpetual | SubAbility$ DBCleanup",
		"DBCleanup", "DB$ Cleanup | ClearChosenCard$ True",
		"CastTrig", castTrig,
		"TrigToken", trigToken,
		"DBLose", loseLine,
	}, extra...)
	return etbChainDef(t, "Test Racketeer",
		"DB$ ChooseCard | ChoiceZone$ Hand | Choices$ Creature.YouOwn | Amount$ 1 | SubAbility$ DBAnimate", svars...)
}

// racketeerDef is the plain Racketeer Boss shape: "When you cast this
// spell, create a Clue token and this spell perpetually loses this ability."
func racketeerDef(t *testing.T) *compile.Card {
	t.Helper()
	return perpetualGranterDef(t,
		"Mode$ SpellCast | ValidCard$ Card.Self | Execute$ TrigToken",
		"DB$ Token | TokenScript$ c_a_clue_draw | SubAbility$ DBLose",
		"DB$ LosePerpetual")
}

// grantTo resolves granter's ETB for p, choosing target from p's hand.
func grantTo(t *testing.T, g *engine.Game, p engine.PlayerID, granter *compile.Card, target engine.CardID) {
	t.Helper()
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{target})
	if _, err := castETBChain(t, g, p, granter, c); err != nil {
		t.Fatalf("granter ETB: %v", err)
	}
}

// recast returns id to p's hand if it is elsewhere, then casts it with one
// green mana and resolves the stack.
func recast(t *testing.T, g *engine.Game, p engine.PlayerID, id engine.CardID, c *engine.ScriptedController) {
	t.Helper()
	if g.Card(id).Zone != engine.Hand {
		g.Move(id, engine.Hand, p)
	}
	castGreenAndResolve(t, g, p, id, c)
}

// A granted "when you cast this spell" trigger survives the hand -> stack
// move, fires from the stack, and LosePerpetual strips it: the next cast
// fires nothing (LosePerpetualEffect.java:20-31).
func TestPerpetualCastTriggerFiresOnceThenIsLost(t *testing.T) {
	t.Parallel()

	g, p, _ := newTokenGame(t)
	target := g.NewCard(creatureDefManaCost(t, "G"), p, engine.Hand)
	grantTo(t, g, p, racketeerDef(t), target)

	recast(t, g, p, target, engine.NewScriptedController())
	if got := len(tokensOn(g, p, "Clue Token")); got != 1 {
		t.Fatalf("clues after first cast = %d, want 1", got)
	}
	recast(t, g, p, target, engine.NewScriptedController())
	if got := len(tokensOn(g, p, "Clue Token")); got != 1 {
		t.Errorf("clues after second cast = %d, want still 1 -- the trigger should have lost itself", got)
	}
}

// A perpetual grant survives every zone change (GameAction.java:265-266) and
// Game.Clone; losing it in a clone leaves the original's grant alone.
func TestPerpetualGrantSurvivesZoneChangesAndClone(t *testing.T) {
	t.Parallel()

	g, p, _ := newTokenGame(t)
	target := g.NewCard(creatureDefManaCost(t, "G"), p, engine.Hand)
	grantTo(t, g, p, racketeerDef(t), target)
	g.Move(target, engine.Graveyard, p)
	g.Move(target, engine.Library, p)

	clone := g.Clone()
	recast(t, clone, p, target, engine.NewScriptedController())
	recast(t, clone, p, target, engine.NewScriptedController())
	if got := len(tokensOn(clone, p, "Clue Token")); got != 1 {
		t.Errorf("clone: clues = %d, want 1", got)
	}
	recast(t, g, p, target, engine.NewScriptedController())
	if got := len(tokensOn(g, p, "Clue Token")); got != 1 {
		t.Errorf("original: clues = %d, want 1 -- the clone's LosePerpetual must not reach it", got)
	}
}

// Two grants of the same compiled SVar on one card are two rows: the fired
// trigger removes its own row only, so a declined twin keeps firing later.
func TestLosePerpetualRemovesOnlyTheFiredGrant(t *testing.T) {
	t.Parallel()

	g, p, _ := newTokenGame(t)
	target := g.NewCard(creatureDefManaCost(t, "G"), p, engine.Hand)
	granter := perpetualGranterDef(t,
		"Mode$ SpellCast | ValidCard$ Card.Self | OptionalDecider$ You | Execute$ TrigToken",
		"DB$ Token | TokenScript$ c_a_clue_draw | SubAbility$ DBLose",
		"DB$ LosePerpetual")
	grantTo(t, g, p, granter, target)
	grantTo(t, g, p, granter, target)

	// Both rows fire; the top one is declined, so only the other is lost.
	c := engine.NewScriptedController()
	c.QueueConfirmOptionalTrigger(false)
	c.QueueConfirmOptionalTrigger(true)
	recast(t, g, p, target, c)
	if got := len(tokensOn(g, p, "Clue Token")); got != 1 {
		t.Fatalf("clues after first cast = %d, want 1", got)
	}

	c = engine.NewScriptedController()
	c.QueueConfirmOptionalTrigger(true)
	recast(t, g, p, target, c)
	if got := len(tokensOn(g, p, "Clue Token")); got != 2 {
		t.Fatalf("clues after second cast = %d, want 2 -- the declined grant should still fire", got)
	}

	recast(t, g, p, target, engine.NewScriptedController())
	if got := len(tokensOn(g, p, "Clue Token")); got != 2 {
		t.Errorf("clues after third cast = %d, want 2 -- both grants lost", got)
	}
}

// Pass the Torch's "if you do": ConditionDefined$ Remembered counts what the
// trigger's host remembers (SpellAbilityCondition.java:350-351), so the
// grant is lost only when a remembered object matches.
func TestLosePerpetualConditionDefinedRemembered(t *testing.T) {
	t.Parallel()

	const rememberToken = "DB$ Token | TokenScript$ c_a_clue_draw | RememberTokens$ True | SubAbility$ DBLose"
	// A remembered player counts too when the spec names players
	// (GameObjectPredicates.restriction, SpellAbilityCondition.java:365).
	const rememberPlayer = "DB$ ChoosePlayer | Defined$ You | Choices$ You | RememberChosen$ True | SubAbility$ DBClue"
	for _, tt := range []struct {
		trigToken, present string
		want               int // clues after two casts
	}{
		{rememberToken, "Card", 1},
		{rememberToken, "Creature", 2},
		{rememberPlayer, "Player", 1},
		{rememberPlayer, "Opponent", 2},
	} {
		g, p, _ := newTokenGame(t)
		target := g.NewCard(creatureDefManaCost(t, "G"), p, engine.Hand)
		grantTo(t, g, p, perpetualGranterDef(t,
			"Mode$ SpellCast | ValidCard$ Card.Self | Execute$ TrigToken", tt.trigToken,
			"DB$ LosePerpetual | ConditionDefined$ Remembered | ConditionPresent$ "+tt.present,
			"DBClue", "DB$ Token | TokenScript$ c_a_clue_draw | SubAbility$ DBLose"), target)
		for range 2 {
			c := engine.NewScriptedController()
			c.QueuePlayerChoice(p)
			recast(t, g, p, target, c)
		}
		if got := len(tokensOn(g, p, "Clue Token")); got != tt.want {
			t.Errorf("ConditionPresent$ %s: clues = %d, want %d", tt.present, got, tt.want)
		}
	}
}

// Outside a granted trigger LosePerpetual does nothing and does not fail:
// Java's toRemove stays 0 (LosePerpetualEffect.java:18-28).
func TestLosePerpetualOutsideAGrantedTriggerIsANoOp(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ LosePerpetual")
}

// Params LosePerpetual cannot evaluate fail loudly instead of reading as
// never met (GO-7).
func TestLosePerpetualRejectsUnresolvedConditions(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"DB$ LosePerpetual | ConditionDefined$ Targeted | ConditionPresent$ Card",
		"DB$ LosePerpetual | Condition$ Threshold",
	} {
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, line)
		if err == nil || !strings.Contains(err.Error(), "not resolvable yet") {
			t.Errorf("%q: err = %v, want a not-resolvable-yet error", line, err)
		}
	}
}

// Animate's Triggers$ resolves only as a perpetual grant this port can
// apply; every other shape fails when applied (ADR-0023 decision 4).
func TestAnimateTriggerGrantShapesThatFail(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		name  string
		line  string
		svars []string
		want  string
	}{
		{
			name:  "a grant that ends",
			line:  "DB$ Animate | Defined$ Self | Triggers$ Grant",
			svars: []string{"Grant", "Mode$ Attacks | ValidCard$ Card.Self | Execute$ Gain", "Gain", "DB$ GainLife | LifeAmount$ 1"},
			want:  "Triggers$ not resolvable yet",
		},
		{
			name: "a perpetual characteristic change",
			line: "DB$ Animate | Defined$ Self | Power$ 3 | Triggers$ Grant | Duration$ Perpetual",
			svars: []string{"Grant", "Mode$ Attacks | ValidCard$ Card.Self | Execute$ Gain",
				"Gain", "DB$ GainLife | LifeAmount$ 1"},
			want: "Duration$ Perpetual on a characteristic change",
		},
		{
			name: "perpetual with nothing granted",
			line: "DB$ Animate | Defined$ Self | Power$ 3 | Duration$ Perpetual",
			want: `Duration$ "Perpetual" not resolvable yet`,
		},
		{
			name:  "a trigger that regrants itself (Snarlfang Vermin)",
			line:  "DB$ Animate | Defined$ Self | Triggers$ Grant | Duration$ Perpetual",
			svars: []string{"Grant", "Mode$ Attacks | ValidCard$ Card.Self | Execute$ Trig"},
			want:  "regrants itself",
		},
		{
			name: "a granted trigger targeting another zone (Pass the Torch)",
			line: "DB$ Animate | Defined$ Self | Triggers$ Grant | Duration$ Perpetual",
			svars: []string{"Grant", "Mode$ Attacks | ValidCard$ Card.Self | Execute$ TrigPlay",
				"TrigPlay", "DB$ Play | TgtZone$ Graveyard | ValidTgts$ Card.YouOwn | Optional$ True"},
			want: "TgtZone$",
		},
	} {
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tt.line, tt.svars...)
		if err == nil || !strings.Contains(err.Error(), tt.want) {
			t.Errorf("%s: err = %v, want one containing %q", tt.name, err, tt.want)
		}
	}
}

// AnimateAll grants the same way, each matching battlefield card getting
// the resolution's one row.
func TestAnimateAllGrantsPerpetualTriggers(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	bear := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ AnimateAll | ValidCards$ Creature.YouCtrl+Other | Triggers$ Grant | Duration$ Perpetual",
		"Grant", "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Card.Self | Execute$ Gain",
		"Gain", "DB$ GainLife | Defined$ You | LifeAmount$ 4")

	g.Card(bear).Damage.Mark(2, false)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life = %d, want 24 -- the granted dies trigger should have fired", got)
	}
}
