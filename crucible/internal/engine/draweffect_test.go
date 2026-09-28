package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// etbDrawTriggerDefParams builds a *compile.Card whose own "when CARDNAME
// enters" trigger runs DB$ Draw with the given Defined$/NumCards$ values (an
// empty string omits the key, exercising Java's own default) -- drawEffect
// itself is unexported, so every case here is driven through the real
// cast+resolve pipeline rather than calling it directly (TEST-1).
func etbDrawTriggerDefParams(t *testing.T, name, defined, numCards string) *compile.Card {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "1", "1"
	raw.Faces[0].ManaCost = mana.MustParse("G")
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigDraw",
	}
	params := "DB$ Draw"
	if defined != "" {
		params += " | Defined$ " + defined
	}
	if numCards != "" {
		params += " | NumCards$ " + numCards
	}
	raw.Faces[0].SVars.Set("TrigDraw", params)

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBDraw casts def (built by etbDrawTriggerDefParams) for p and resolves
// the stack, returning ResolveStack's own error.
func castETBDraw(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card) error {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

// TestDrawEffectDefaultsToOneCard proves NumCards$'s absence resolves to 1,
// Java's own `sa.hasParam("NumCards") ? ... : 1`.
func TestDrawEffectDefaultsToOneCard(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	top := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)

	if err := castETBDraw(t, g, p, etbDrawTriggerDefParams(t, "Test No NumCards", "You", "")); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(top).Zone != engine.Hand {
		t.Errorf("library card zone = %v, want Hand", g.Card(top).Zone)
	}
}

// TestDrawEffectResolvesNumCardsTwo proves a plain base-10 NumCards$ draws
// that many.
func TestDrawEffectResolvesNumCardsTwo(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	first := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)
	second := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)

	if err := castETBDraw(t, g, p, etbDrawTriggerDefParams(t, "Test NumCards 2", "You", "2")); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(first).Zone != engine.Hand {
		t.Errorf("first library card zone = %v, want Hand", g.Card(first).Zone)
	}
	if g.Card(second).Zone != engine.Hand {
		t.Errorf("second library card zone = %v, want Hand", g.Card(second).Zone)
	}
}

// TestDrawEffectResolvesDefinedOpponent proves Defined$ Opponent draws for
// every opponent of the ability's own controller, not the controller itself.
func TestDrawEffectResolvesDefinedOpponent(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	ownTop := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)
	opponentTop := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Library)

	if err := castETBDraw(t, g, p, etbDrawTriggerDefParams(t, "Test Defined Opponent", "Opponent", "1")); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(opponentTop).Zone != engine.Hand {
		t.Errorf("opponent's library card zone = %v, want Hand", g.Card(opponentTop).Zone)
	}
	if g.Card(ownTop).Zone != engine.Library {
		t.Errorf("caster's own library card zone = %v, want Library -- Defined$ Opponent draws for the opponent, not the caster", g.Card(ownTop).Zone)
	}
}

// TestDrawEffectEmptyLibraryLosesTheGame proves an empty library records the
// attempt rather than erroring or panicking -- the same DrawCards mechanism
// drawStep already uses (turn.go) -- and that ResolveStack's own
// CheckStateBasedActions call right after resolving (stack.go) reads and
// consumes the flag into an actual loss (CR 704.5b) within the same
// ResolveStack call, not on some later check.
func TestDrawEffectEmptyLibraryLosesTheGame(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	if err := castETBDraw(t, g, p, etbDrawTriggerDefParams(t, "Test Empty Library", "You", "1")); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !g.Player(p).Lost {
		t.Error("Player.Lost = false, want true -- an attempted draw from an empty library loses (CR 704.5b)")
	}
}

// TestDrawEffectUnsupportedDefinedErrors proves a Defined$ shape this port
// cannot resolve yet (TriggeredPlayer, ...) is a real error naming the
// param, not a silent no-op or a wrong guess (GO-7). "Remembered" and
// "ChosenPlayer" read the host's Memory (defined.go), and
// "Targeted"/"TargetedPlayer" moved out of this group once targeting.go
// landed: definedPlayers now resolves them by reading a.Targets, which
// resolveTargets fills from ValidTgts$ -- an ability naming Defined$
// Targeted with no ValidTgts$ of its own (this test's old shape) is not a
// real corpus line (nothing to target without ValidTgts$ naming
// candidates), so it is not a representative "unsupported" case anymore.
func TestDrawEffectUnsupportedDefinedErrors(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	err := castETBDraw(t, g, p, etbDrawTriggerDefParams(t, "Test Defined TriggeredPlayer", "TriggeredPlayer", "1"))
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming Defined$")
	}
	if !strings.Contains(err.Error(), "Defined") {
		t.Errorf("ResolveStack error = %q, want it to name Defined$", err.Error())
	}
}

// TestDrawEffectNonNumericNumCardsErrors proves an SVar-driven NumCards$ (X,
// Y, a named SVar) errors rather than resolving to zero or panicking --
// compareMatches (valid.go) already documents the identical gap for a
// valid-string's own numeric compare.
func TestDrawEffectNonNumericNumCardsErrors(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	err := castETBDraw(t, g, p, etbDrawTriggerDefParams(t, "Test NumCards X", "You", "X"))
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming NumCards$")
	}
	if !strings.Contains(err.Error(), "NumCards") {
		t.Errorf("ResolveStack error = %q, want it to name NumCards$", err.Error())
	}
}

// TestDrawEffectRespectsConditionCheckSVar proves Draw calls
// subAbilityConditionMet the same way every other M6 effect does: this file
// had no call to it at all until found missing (rules review on the
// LosePerpetual/ADR-0023 landing), so a Condition*$ param on DB$ Draw
// silently drew anyway regardless of whether the condition held.
func TestDrawEffectRespectsConditionCheckSVar(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name    string
		compare string
		want    bool
	}{
		{"met", "GE1", true},
		{"unmet", "GE2", false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGame(t)
			top := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)
			resolveLine(t, g, p, engine.NewScriptedController(),
				"DB$ StoreSVar | SVar$ X | Type$ Number | Expression$ 1 | SubAbility$ DBDraw",
				"DBDraw", "DB$ Draw | Defined$ You | ConditionCheckSVar$ X | ConditionSVarCompare$ "+tc.compare)
			got := g.Card(top).Zone == engine.Hand
			if got != tc.want {
				t.Errorf("drew = %v, want %v (ConditionSVarCompare$ %s)", got, tc.want, tc.compare)
			}
		})
	}
}
