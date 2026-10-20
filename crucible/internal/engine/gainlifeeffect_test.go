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

// etbGainLifeTriggerDefParams builds a *compile.Card whose own "when
// CARDNAME enters" trigger runs DB$ GainLife with the given params string
// appended -- gainLifeEffect itself is unexported, so every case here is
// driven through the real cast+resolve pipeline rather than calling it
// directly (TEST-1), the identical reason etbDealDamageTriggerDefParams
// (dealdamageeffect_test.go) is.
func etbGainLifeTriggerDefParams(t *testing.T, name, extraParams string, svars map[string]string) *compile.Card {
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
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigLife",
	}
	raw.Faces[0].SVars.Set("TrigLife", "DB$ GainLife | "+extraParams)
	for name, body := range svars {
		raw.Faces[0].SVars.Set(name, body)
	}

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBGainLife casts def (built by etbGainLifeTriggerDefParams) for p and
// resolves the stack, returning ResolveStack's own error.
func castETBGainLife(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card) error {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

// TestGainLifeEffectDefinedYouGainsController proves the corpus's single
// largest real Defined$ shape: Defined$ You gains life for the ability's own
// controller, not any opponent.
func TestGainLifeEffectDefinedYouGainsController(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	if err := castETBGainLife(t, g, p, etbGainLifeTriggerDefParams(t, "Test You", "Defined$ You | LifeAmount$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 23 {
		t.Errorf("p's life = %d, want 23", g.Player(p).Life)
	}
	if g.Player(other).Life != 20 {
		t.Errorf("other's life = %d, want 20 -- Defined$ You must not touch an opponent", g.Player(other).Life)
	}
}

// TestGainLifeEffectDefinedOpponentGainsEveryOpponent proves Defined$
// Opponent gains life for every opponent of the controller, not the
// controller itself -- a three-player game so "every" is observably more
// than one.
func TestGainLifeEffectDefinedOpponentGainsEveryOpponent(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b", "c")
	p, o1, o2 := g.Players()[0], g.Players()[1], g.Players()[2]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(o1).Life, g.Player(o2).Life = 20, 20, 20

	if err := castETBGainLife(t, g, p, etbGainLifeTriggerDefParams(t, "Test Opponent", "Defined$ Player.Opponent | LifeAmount$ 2", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(o1).Life != 22 {
		t.Errorf("o1's life = %d, want 22", g.Player(o1).Life)
	}
	if g.Player(o2).Life != 22 {
		t.Errorf("o2's life = %d, want 22", g.Player(o2).Life)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want 20 -- Defined$ Player.Opponent must not touch the controller", g.Player(p).Life)
	}
}

// TestGainLifeEffectResolvesNamedLifeAmountSVar proves LifeAmount$ resolves a
// named SVar reference through resolveNamedAmount (amount.go), not just a
// plain integer.
func TestGainLifeEffectResolvesNamedLifeAmountSVar(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbGainLifeTriggerDefParams(t, "Test Named LifeAmount", "Defined$ You | LifeAmount$ X", map[string]string{"X": "5"})
	if err := castETBGainLife(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 25 {
		t.Errorf("p's life = %d, want 25 -- LifeAmount$ X must resolve through the named SVar X:5", g.Player(p).Life)
	}
}

// TestGainLifeEffectMissingLifeAmountErrors proves a missing LifeAmount$ is
// a real error rather than a silent zero-gain no-op (GO-7).
func TestGainLifeEffectMissingLifeAmountErrors(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	err := castETBGainLife(t, g, p, etbGainLifeTriggerDefParams(t, "Test No LifeAmount", "Defined$ You", nil))
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming LifeAmount$")
	}
	if !strings.Contains(err.Error(), "LifeAmount") {
		t.Errorf("ResolveStack error = %q, want it to name LifeAmount$", err.Error())
	}
}

// TestGainLifeEffectChainsIntoSubAbility proves SubAbility$ no longer
// blocks GainLife's own resolution now that resolveSubAbility
// (subability.go) exists: the chained DB$ LoseLife runs too, not just
// GainLife's own body -- geyadrone_dihada.txt's own real shape (LoseLife
// chaining into GainLife) with the two effects swapped, since GainLife is
// this file's own subject.
func TestGainLifeEffectChainsIntoSubAbility(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbGainLifeTriggerDefParams(t, "Test SubAbility",
		"Defined$ You | LifeAmount$ 1 | SubAbility$ DBLoseLifeOpp",
		map[string]string{"DBLoseLifeOpp": "DB$ LoseLife | Defined$ Opponent | LifeAmount$ 2"})
	if err := castETBGainLife(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 21 {
		t.Errorf("p's life = %d, want 21 -- GainLife's own body must still run", g.Player(p).Life)
	}
	if g.Player(other).Life != 18 {
		t.Errorf("other's life = %d, want 18 -- the chained LoseLife must run too", g.Player(other).Life)
	}
}

// TestGainLifeEffectFiresWhenConditionCheckSVarIsMet proves
// subAbilityConditionMet (condition.go) gates GainLife's own resolution the
// identical way it gates DealDamage's and a checkland's DB$ Tap: X GE1
// holds (X is 1), so the life gain happens as normal.
func TestGainLifeEffectFiresWhenConditionCheckSVarIsMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbGainLifeTriggerDefParams(t, "Test Condition Met",
		"Defined$ You | LifeAmount$ 3 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", map[string]string{"X": "1"})
	if err := castETBGainLife(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 23 {
		t.Errorf("p's life = %d, want 23 -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 holds (X is 1)", g.Player(p).Life)
	}
}

// TestGainLifeEffectNoOpsWhenConditionCheckSVarIsNotMet proves the negative
// control: X GE1 fails (X is 0), so the ability does nothing -- not an
// error, SpellAbilityCondition.areMet's own "declined by the rules" contract.
func TestGainLifeEffectNoOpsWhenConditionCheckSVarIsNotMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbGainLifeTriggerDefParams(t, "Test Condition Unmet",
		"Defined$ You | LifeAmount$ 3 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", map[string]string{"X": "0"})
	if err := castETBGainLife(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want 20 -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 fails (X is 0), no life gained", g.Player(p).Life)
	}
}

// TestGainLifeEffectRejectsConditionItself proves Condition$ (the flag
// switch, distinct from the resolved ConditionCheckSVar$/ConditionPresent$
// pair) still fails loudly -- SpellAbilityCondition's own Threshold/
// Metalcraft/... family, no evaluator built for it.
func TestGainLifeEffectRejectsConditionItself(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbGainLifeTriggerDefParams(t, "Test Condition Flag", "Defined$ You | LifeAmount$ 1 | Condition$ Teamwork", nil)
	err := castETBGainLife(t, g, p, def)
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming Condition")
	}
	if !strings.Contains(err.Error(), "Condition") {
		t.Errorf("ResolveStack error = %q, want it to name Condition$", err.Error())
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want 20 -- a rejected line must not grant partial life", g.Player(p).Life)
	}
}

// TestGainLifeEffectEmitsLifeChangedEvent proves gainLifeEffect emits the
// identical LifeChanged event dealPlayerDamage already emits for a life
// LOSS (combatdamage.go), here with a positive amount.
func TestGainLifeEffectEmitsLifeChangedEvent(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	var sink recordingSink
	g.SetSink(&sink)

	if err := castETBGainLife(t, g, p, etbGainLifeTriggerDefParams(t, "Test Event", "Defined$ You | LifeAmount$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	var saw bool
	for _, e := range sink.events {
		if e.Kind == engine.LifeChanged {
			saw = true
			if e.Amount != 3 {
				t.Errorf("LifeChanged Amount = %d, want 3", e.Amount)
			}
		}
	}
	if !saw {
		t.Error("no LifeChanged event seen")
	}
}

// lifeGainedTriggerCreatureDefPT builds a creature whose own Mode$
// LifeGained trigger watches for its own controller gaining life --
// checkLifeGainedTriggers' own real caller, trigger.go.
func lifeGainedTriggerCreatureDefPT(t *testing.T, name string) *compile.Card {
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
	raw.Faces[0].Triggers = []string{
		"Mode$ LifeGained | ValidPlayer$ You | TriggerZones$ Battlefield | Execute$ TrigDraw",
	}
	raw.Faces[0].SVars.Set("TrigDraw", "DB$ Draw | Defined$ You | NumCards$ 1")

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// lifeGainedTriggerCreatureDefPTWithExtra is lifeGainedTriggerCreatureDefPT's
// own sibling, carrying extra trigger params past the bare ValidPlayer$ You
// shape -- FirstTime$/ActivationLimit$'s own tests need one each.
func lifeGainedTriggerCreatureDefPTWithExtra(t *testing.T, name, extra string) *compile.Card {
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
	raw.Faces[0].Triggers = []string{
		"Mode$ LifeGained | ValidPlayer$ You | TriggerZones$ Battlefield | " + extra + " | Execute$ TrigDraw",
	}
	raw.Faces[0].SVars.Set("TrigDraw", "DB$ Draw | Defined$ You | NumCards$ 1")

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// TestGainLifeEffectFiresLifeGainedTriggerFirstTimeOnFirstGain proves
// FirstTime$ True fires on the first life gain of the turn -- a new
// pre-increment read of Player.LifeGainedTimesThisTurn.
func TestGainLifeEffectFiresLifeGainedTriggerFirstTimeOnFirstGain(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(lifeGainedTriggerCreatureDefPTWithExtra(t, "Test First Time Watcher", "FirstTime$ True"), p, engine.Battlefield)
	top := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)

	if err := castETBGainLife(t, g, p, etbGainLifeTriggerDefParams(t, "Test Gainer", "Defined$ You | LifeAmount$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	if g.Card(top).Zone != engine.Hand {
		t.Errorf("library card zone = %v, want Hand -- FirstTime$ True must fire on the first life gain this turn", g.Card(top).Zone)
	}
}

// TestGainLifeEffectSkipsLifeGainedTriggerFirstTimeOnSecondGain proves the
// other direction: a second life gain the same turn does not fire
// FirstTime$ True again.
func TestGainLifeEffectSkipsLifeGainedTriggerFirstTimeOnSecondGain(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(lifeGainedTriggerCreatureDefPTWithExtra(t, "Test First Time Watcher", "FirstTime$ True"), p, engine.Battlefield)
	top := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)

	if err := castETBGainLife(t, g, p, etbGainLifeTriggerDefParams(t, "Test First Gainer", "Defined$ You | LifeAmount$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack (first): %v", err)
	}
	if g.Card(top).Zone != engine.Hand {
		t.Fatalf("library card zone after first gain = %v, want Hand", g.Card(top).Zone)
	}

	second := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)
	if err := castETBGainLife(t, g, p, etbGainLifeTriggerDefParams(t, "Test Second Gainer", "Defined$ You | LifeAmount$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack (second): %v", err)
	}

	if g.Card(second).Zone != engine.Library {
		t.Errorf("library card zone after second gain = %v, want unchanged Library -- FirstTime$ True must not fire a second time this turn", g.Card(second).Zone)
	}
}

// TestGainLifeEffectHonorsLifeGainedActivationLimit proves Trigger.
// checkActivationLimit: a watcher naming ActivationLimit$ 1 fires for the
// first life gain of the turn and not for the second.
func TestGainLifeEffectHonorsLifeGainedActivationLimit(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(lifeGainedTriggerCreatureDefPTWithExtra(t, "Test Activation Limit Watcher", "ActivationLimit$ 1"), p, engine.Battlefield)
	top := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)
	second := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)

	if err := castETBGainLife(t, g, p, etbGainLifeTriggerDefParams(t, "Test Gainer", "Defined$ You | LifeAmount$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(top).Zone != engine.Hand && g.Card(second).Zone != engine.Hand {
		t.Fatal("the first life gain did not fire the ActivationLimit$ 1 watcher")
	}
	if err := castETBGainLife(t, g, p, etbGainLifeTriggerDefParams(t, "Test Second Gainer", "Defined$ You | LifeAmount$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack (second): %v", err)
	}
	inHand := 0
	for _, id := range []engine.CardID{top, second} {
		if g.Card(id).Zone == engine.Hand {
			inHand++
		}
	}
	if inHand != 1 {
		t.Errorf("%d library cards drawn after two life gains, want 1 -- ActivationLimit$ 1 fires once a turn", inHand)
	}
}

// TestGainLifeEffectFiresLifeGainedTrigger proves gainLifeEffect's own
// checkLifeGainedTriggers call (trigger.go) fires a real "whenever you gain
// life" watcher end to end: p gains life from GainLife, a separate
// permanent's own Mode$ LifeGained trigger detects it and its own Execute$
// Draw resolves.
func TestGainLifeEffectFiresLifeGainedTrigger(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(lifeGainedTriggerCreatureDefPT(t, "Test Life Watcher"), p, engine.Battlefield)
	top := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)

	if err := castETBGainLife(t, g, p, etbGainLifeTriggerDefParams(t, "Test Gainer", "Defined$ You | LifeAmount$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 23 {
		t.Fatalf("p's life = %d, want 23", g.Player(p).Life)
	}
	if g.Card(top).Zone != engine.Hand {
		t.Errorf("library card zone = %v, want Hand -- the LifeGained watcher's own Draw should have resolved", g.Card(top).Zone)
	}
}

// TestGainLifeEffectSkipsLifeGainedTriggerForOpponent proves the negative
// control: ValidPlayer$ You never matches the opponent's own life gain, so
// the watcher does not fire.
func TestGainLifeEffectSkipsLifeGainedTriggerForOpponent(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(lifeGainedTriggerCreatureDefPT(t, "Test Life Watcher"), p, engine.Battlefield)

	if err := castETBGainLife(t, g, p, etbGainLifeTriggerDefParams(t, "Test Gainer", "Defined$ Player.Opponent | LifeAmount$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(other).Life != 23 {
		t.Fatalf("other's life = %d, want 23", g.Player(other).Life)
	}
	if got := g.StackLen(); got != 0 {
		t.Errorf("StackLen() = %d, want 0 -- ValidPlayer$ You never matches other's own life gain", got)
	}
}
