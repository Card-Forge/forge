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

// etbLoseLifeTriggerDefParams builds a *compile.Card whose own "when
// CARDNAME enters" trigger runs DB$ LoseLife with the given params string
// appended -- loseLifeEffect itself is unexported, so every case here is
// driven through the real cast+resolve pipeline rather than calling it
// directly (TEST-1), the identical reason etbGainLifeTriggerDefParams
// (gainlifeeffect_test.go) is.
func etbLoseLifeTriggerDefParams(t *testing.T, name, extraParams string, svars map[string]string) *compile.Card {
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
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigLoseLife",
	}
	raw.Faces[0].SVars.Set("TrigLoseLife", "DB$ LoseLife | "+extraParams)
	for name, body := range svars {
		raw.Faces[0].SVars.Set(name, body)
	}

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBLoseLife casts def (built by etbLoseLifeTriggerDefParams) for p and
// resolves the stack, returning ResolveStack's own error.
func castETBLoseLife(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card) error {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

// TestLoseLifeEffectDefinedYouDrainsController proves the corpus's single
// largest real Defined$ shape: Defined$ You drains life from the ability's
// own controller, not any opponent.
func TestLoseLifeEffectDefinedYouDrainsController(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	if err := castETBLoseLife(t, g, p, etbLoseLifeTriggerDefParams(t, "Test You", "Defined$ You | LifeAmount$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 17 {
		t.Errorf("p's life = %d, want 17", g.Player(p).Life)
	}
	if g.Player(other).Life != 20 {
		t.Errorf("other's life = %d, want 20 -- Defined$ You must not touch an opponent", g.Player(other).Life)
	}
}

// TestLoseLifeEffectDefinedOpponentDrainsEveryOpponent proves Defined$
// Opponent drains every opponent of the controller, not the controller
// itself -- a three-player game so "every" is observably more than one.
func TestLoseLifeEffectDefinedOpponentDrainsEveryOpponent(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b", "c")
	p, o1, o2 := g.Players()[0], g.Players()[1], g.Players()[2]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(o1).Life, g.Player(o2).Life = 20, 20, 20

	if err := castETBLoseLife(t, g, p, etbLoseLifeTriggerDefParams(t, "Test Opponent", "Defined$ Opponent | LifeAmount$ 2", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(o1).Life != 18 {
		t.Errorf("o1's life = %d, want 18", g.Player(o1).Life)
	}
	if g.Player(o2).Life != 18 {
		t.Errorf("o2's life = %d, want 18", g.Player(o2).Life)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want 20 -- Defined$ Opponent must not touch the controller", g.Player(p).Life)
	}
}

// TestLoseLifeEffectResolvesNamedLifeAmountSVar proves LifeAmount$ resolves a
// named SVar reference through resolveNamedAmount (amount.go), not just a
// plain integer.
func TestLoseLifeEffectResolvesNamedLifeAmountSVar(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbLoseLifeTriggerDefParams(t, "Test Named LifeAmount", "Defined$ You | LifeAmount$ X", map[string]string{"X": "5"})
	if err := castETBLoseLife(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 15 {
		t.Errorf("p's life = %d, want 15 -- LifeAmount$ X must resolve through the named SVar X:5", g.Player(p).Life)
	}
}

// TestLoseLifeEffectMissingLifeAmountErrors proves a missing LifeAmount$ is
// a real error rather than a silent zero-loss no-op (GO-7).
func TestLoseLifeEffectMissingLifeAmountErrors(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	err := castETBLoseLife(t, g, p, etbLoseLifeTriggerDefParams(t, "Test No LifeAmount", "Defined$ You", nil))
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming LifeAmount$")
	}
	if !strings.Contains(err.Error(), "LifeAmount") {
		t.Errorf("ResolveStack error = %q, want it to name LifeAmount$", err.Error())
	}
}

// TestLoseLifeEffectChainsIntoSubAbility proves SubAbility$ no longer
// blocks LoseLife's own resolution now that resolveSubAbility
// (subability.go) exists -- radiant_epicure.txt's own real shape
// (DB$ LoseLife | Defined$ Player.Opponent | LifeAmount$ X |
// SubAbility$ DBGainLife, chaining into DB$ GainLife | Defined$ You |
// LifeAmount$ X), a plain integer standing in for its own Converge-driven
// X (ManaSpent$, still its own separate unresolved mechanic).
func TestLoseLifeEffectChainsIntoSubAbility(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbLoseLifeTriggerDefParams(t, "Test SubAbility",
		"Defined$ Player.Opponent | LifeAmount$ 2 | SubAbility$ DBGainLife",
		map[string]string{"DBGainLife": "DB$ GainLife | Defined$ You | LifeAmount$ 2"})
	if err := castETBLoseLife(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(other).Life != 18 {
		t.Errorf("other's life = %d, want 18 -- LoseLife's own body must still run", g.Player(other).Life)
	}
	if g.Player(p).Life != 22 {
		t.Errorf("p's life = %d, want 22 -- the chained GainLife must run too", g.Player(p).Life)
	}
}

// TestLoseLifeEffectFiresWhenConditionCheckSVarIsMet proves
// subAbilityConditionMet (condition.go) gates LoseLife's own resolution the
// identical way it gates GainLife's/DealDamage's and a checkland's DB$ Tap.
func TestLoseLifeEffectFiresWhenConditionCheckSVarIsMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbLoseLifeTriggerDefParams(t, "Test Condition Met",
		"Defined$ You | LifeAmount$ 3 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", map[string]string{"X": "1"})
	if err := castETBLoseLife(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 17 {
		t.Errorf("p's life = %d, want 17 -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 holds (X is 1)", g.Player(p).Life)
	}
}

// TestLoseLifeEffectNoOpsWhenConditionCheckSVarIsNotMet proves the negative
// control: X GE1 fails (X is 0), so the ability does nothing -- not an
// error, SpellAbilityCondition.areMet's own "declined by the rules" contract.
func TestLoseLifeEffectNoOpsWhenConditionCheckSVarIsNotMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbLoseLifeTriggerDefParams(t, "Test Condition Unmet",
		"Defined$ You | LifeAmount$ 3 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", map[string]string{"X": "0"})
	if err := castETBLoseLife(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want 20 -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 fails (X is 0), no life lost", g.Player(p).Life)
	}
}

// TestLoseLifeEffectRejectsConditionItself proves Condition$ (the flag
// switch, distinct from the resolved ConditionCheckSVar$/ConditionPresent$
// pair) still fails loudly -- SpellAbilityCondition's own Threshold/
// Metalcraft/... family, no evaluator built for it.
func TestLoseLifeEffectRejectsConditionItself(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbLoseLifeTriggerDefParams(t, "Test Condition Flag", "Defined$ You | LifeAmount$ 1 | Condition$ Threshold", nil)
	err := castETBLoseLife(t, g, p, def)
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming Condition")
	}
	if !strings.Contains(err.Error(), "Condition") {
		t.Errorf("ResolveStack error = %q, want it to name Condition$", err.Error())
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want 20 -- a rejected line must not drain partial life", g.Player(p).Life)
	}
}

// TestLoseLifeEffectEmitsLifeChangedEvent proves loseLifeEffect emits the
// identical LifeChanged event dealPlayerDamage/gainLifeEffect already emit,
// here with a negative amount for a loss.
func TestLoseLifeEffectEmitsLifeChangedEvent(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	var sink recordingSink
	g.SetSink(&sink)

	if err := castETBLoseLife(t, g, p, etbLoseLifeTriggerDefParams(t, "Test Event", "Defined$ You | LifeAmount$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	var saw bool
	for _, e := range sink.events {
		if e.Kind == engine.LifeChanged {
			saw = true
			if e.Amount != -3 {
				t.Errorf("LifeChanged Amount = %d, want -3", e.Amount)
			}
		}
	}
	if !saw {
		t.Error("no LifeChanged event seen")
	}
}

// castETBLoseLifeOnController is castETBLoseLife's own variant taking an
// external controller, needed once a scenario has to queue an answer
// (QueueTargets) before casting.
func castETBLoseLifeOnController(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, c *engine.ScriptedController) error {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

// TestLoseLifeEffectValidTgtsOpponentDrainsChosenTarget proves the first
// real ValidTgts$ shape this port resolves for LoseLife: resolveTargets
// (targeting.go) picks a legal target from ValidTgts$ Opponent, and
// loseLifeEffect reads it from a.Targets directly -- bypassing Defined$
// entirely, the identical dispatch LifeLoseEffect.java's own
// getTargetPlayers(sa) makes.
func TestLoseLifeEffectValidTgtsOpponentDrainsChosenTarget(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(opp).Life = 20, 20

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(opp)})
	def := etbLoseLifeTriggerDefParams(t, "Test ValidTgts Opponent", "ValidTgts$ Opponent | LifeAmount$ 3", nil)
	if err := castETBLoseLifeOnController(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(opp).Life; got != 17 {
		t.Errorf("opponent life = %d, want 17", got)
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("caster life = %d, want unchanged 20 -- ValidTgts$ Opponent must not drain the caster", got)
	}
}

// TestLoseLifeEffectValidTgtsPlayerCanTargetCaster proves ValidTgts$ Player
// (the corpus's own other real base, alongside Opponent) can legally choose
// the caster themselves -- Player's own candidate set is every player, not
// only opponents.
func TestLoseLifeEffectValidTgtsPlayerCanTargetCaster(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(opp).Life = 20, 20

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
	def := etbLoseLifeTriggerDefParams(t, "Test ValidTgts Player", "ValidTgts$ Player | LifeAmount$ 5", nil)
	if err := castETBLoseLifeOnController(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 15 {
		t.Errorf("caster life = %d, want 15", got)
	}
	if got := g.Player(opp).Life; got != 20 {
		t.Errorf("opponent life = %d, want unchanged 20", got)
	}
}

// TestLoseLifeEffectValidTgtsIgnoresDefinedWhenBothPresent proves the two
// are mutually exclusive the way LifeLoseEffect.java's own getTargetPlayers
// makes them: a line naming both ValidTgts$ and Defined$ (0 real corpus
// lines do, but the dispatch itself should not silently prefer the wrong
// one if it ever happened) reads the targeted player, never Defined$'s own
// value.
func TestLoseLifeEffectValidTgtsIgnoresDefinedWhenBothPresent(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(opp).Life = 20, 20

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(opp)})
	def := etbLoseLifeTriggerDefParams(t, "Test Both Present", "ValidTgts$ Player | Defined$ You | LifeAmount$ 4", nil)
	if err := castETBLoseLifeOnController(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(opp).Life; got != 16 {
		t.Errorf("targeted opponent life = %d, want 16 -- ValidTgts$ must win over Defined$ You", got)
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("caster life = %d, want unchanged 20 -- Defined$ You must be ignored when ValidTgts$ is present", got)
	}
}

// TestLoseLifeEffectValidTgtsUnrecognizedPropertySkipsSilently proves a
// qualified ValidTgts$ this port's matchesPlayerProperty does not recognize
// (Player.wasDealtDamageThisTurnBySource, 1 real line) resolves to zero
// legal targets rather than a wrong one -- CR 603.3c's own "no legal
// targets" outcome, the ability doing nothing, no error and no controller
// call (an unconsumed queue slot proves it).
func TestLoseLifeEffectValidTgtsUnrecognizedPropertySkipsSilently(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(opp).Life = 20, 20

	c := engine.NewScriptedController()
	// No QueueTargets call: an unrecognized property must never reach the
	// controller, or this panics on an exhausted queue.
	def := etbLoseLifeTriggerDefParams(t, "Test Unrecognized Property",
		"ValidTgts$ Player.wasDealtDamageThisTurnBySource | LifeAmount$ 3", nil)
	if err := castETBLoseLifeOnController(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("caster life = %d, want unchanged 20", got)
	}
	if got := g.Player(opp).Life; got != 20 {
		t.Errorf("opponent life = %d, want unchanged 20", got)
	}
}

// TestLoseLifeEffectValidTgtsRadianceIgnoresPlayerTargets proves Radiance$
// (0 real LoseLife lines combine it, a constructed case) fans out from card
// targets only (CardUtil.getRadiance): a player target is chosen and loses
// life as for any ValidTgts$ line, with nothing added.
func TestLoseLifeEffectValidTgtsRadianceIgnoresPlayerTargets(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(opp).Life = 20, 20

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(opp)})
	def := etbLoseLifeTriggerDefParams(t, "Test Radiance", "ValidTgts$ Opponent | Radiance$ True | LifeAmount$ 3", nil)
	if err := castETBLoseLifeOnController(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(opp).Life; got != 17 {
		t.Errorf("opponent life = %d, want 17", got)
	}
}

// TestLoseLifeEffectValidTgtsMaxTwoAsksForBothOpponentsAtOnce proves
// TargetMax$ (2, past the 1/1 default) reaches ChooseTargets: with two
// opponents both matching ValidTgts$ Opponent, the controller is asked
// once for up to two targets, and both chosen ones lose life.
func TestLoseLifeEffectValidTgtsMaxTwoAsksForBothOpponentsAtOnce(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b", "c")
	p, opp1, opp2 := g.Players()[0], g.Players()[1], g.Players()[2]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(opp1).Life, g.Player(opp2).Life = 20, 20, 20

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(opp1), engine.PlayerEntity(opp2)})
	def := etbLoseLifeTriggerDefParams(t, "Test Max Two",
		"ValidTgts$ Opponent | TargetMin$ 1 | TargetMax$ 2 | LifeAmount$ 3", nil)
	if err := castETBLoseLifeOnController(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(opp1).Life; got != 17 {
		t.Errorf("opp1 life = %d, want 17", got)
	}
	if got := g.Player(opp2).Life; got != 17 {
		t.Errorf("opp2 life = %d, want 17", got)
	}
}
