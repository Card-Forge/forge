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

// etbPumpAllTriggerDefParams builds a *compile.Card whose own "when
// CARDNAME enters" trigger runs DB$ PumpAll with the given params string
// appended -- pumpAllEffect itself is unexported, so every case here is
// driven through the real cast+resolve pipeline rather than calling it
// directly (TEST-1), the identical reason etbPumpTriggerDefParams
// (pumpeffect_test.go) is.
func etbPumpAllTriggerDefParams(t *testing.T, name, extraParams string, svars map[string]string) *compile.Card {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "2", "2"
	raw.Faces[0].ManaCost = mana.MustParse("G")
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigPumpAll",
	}
	raw.Faces[0].SVars.Set("TrigPumpAll", "DB$ PumpAll | "+extraParams)
	for name, body := range svars {
		raw.Faces[0].SVars.Set(name, body)
	}

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBPumpAll casts def (built by etbPumpAllTriggerDefParams) for p and
// resolves the stack, returning the creature's own CardID and
// ResolveStack's own error.
func castETBPumpAll(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card) (engine.CardID, error) {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return creature, g.ResolveStack(engine.NewRegistry(), c)
}

// TestPumpAllEffectPumpsEveryMatchingCreature proves the corpus's dominant
// real shape: a blanket ValidCards$ match, no Defined$ and no target,
// applies to every matching card across every player's own battlefield --
// including the caster's own pre-existing creature and the entering
// creature itself, both Creature.YouCtrl.
func TestPumpAllEffectPumpsEveryMatchingCreature(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	existing := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	trigger, err := castETBPumpAll(t, g, p, etbPumpAllTriggerDefParams(t, "Test PumpAll", "ValidCards$ Creature.YouCtrl | NumAtt$ 1 | NumDef$ 1", nil))
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	if pw, _ := g.Card(existing).Power(); pw != 2 {
		t.Errorf("existing creature power = %d, want 2 (1 base + 1 pump)", pw)
	}
	if pw, _ := g.Card(trigger).Power(); pw != 3 {
		t.Errorf("triggering creature power = %d, want 3 (2 base + 1 pump) -- PumpAll must include itself", pw)
	}
}

// TestPumpAllEffectSkipsNonMatchingCards proves ValidCards$ actually
// restricts the set: Creature.YouCtrl must never touch an opponent's
// creature.
func TestPumpAllEffectSkipsNonMatchingCards(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)

	if _, err := castETBPumpAll(t, g, p, etbPumpAllTriggerDefParams(t, "Test PumpAll Restrict", "ValidCards$ Creature.YouCtrl | NumAtt$ 3 | NumDef$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(theirs).Power(); pw != 1 {
		t.Errorf("opponent's creature power = %d, want 1 -- Creature.YouCtrl must not match it", pw)
	}
}

// TestPumpAllEffectGrantsKeyword proves KW$ grants a keyword through
// KeywordMod the same way it does for Pump.
func TestPumpAllEffectGrantsKeyword(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	existing := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	if _, err := castETBPumpAll(t, g, p, etbPumpAllTriggerDefParams(t, "Test PumpAll Keyword", "ValidCards$ Creature.YouCtrl | KW$ Flying", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !g.Card(existing).HasKeyword("Flying") {
		t.Error("existing creature does not have Flying, want it granted by PumpAll")
	}
}

// TestPumpAllEffectExpiresAtCleanup proves the default duration -- "until
// end of turn" -- wears off through the identical cleanupStep mechanism
// Pump's own record uses.
func TestPumpAllEffectExpiresAtCleanup(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	existing := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	if _, err := castETBPumpAll(t, g, p, etbPumpAllTriggerDefParams(t, "Test PumpAll EOT", "ValidCards$ Creature.YouCtrl | NumAtt$ 2 | NumDef$ 2", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(existing).Power(); pw != 3 {
		t.Fatalf("power before cleanup = %d, want 3", pw)
	}

	g.SetTurnState(1, p, engine.EndOfTurn)
	g.AdvancePhase(engine.NewScriptedController()) // -> Cleanup

	if pw, _ := g.Card(existing).Power(); pw != 1 {
		t.Errorf("power after cleanup = %d, want 1 (pump worn off)", pw)
	}
}

// TestPumpAllEffectPermanentDurationSurvivesCleanup proves the negative
// control: Duration$ Permanent is never dropped by cleanupStep.
func TestPumpAllEffectPermanentDurationSurvivesCleanup(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	existing := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	if _, err := castETBPumpAll(t, g, p,
		etbPumpAllTriggerDefParams(t, "Test PumpAll Permanent", "ValidCards$ Creature.YouCtrl | NumAtt$ 1 | NumDef$ 1 | Duration$ Permanent", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	g.SetTurnState(1, p, engine.EndOfTurn)
	g.AdvancePhase(engine.NewScriptedController()) // -> Cleanup

	if pw, _ := g.Card(existing).Power(); pw != 2 {
		t.Errorf("power after cleanup = %d, want 2 -- Duration$ Permanent must survive cleanup", pw)
	}
}

// TestPumpAllEffectRejectsSubAbilityChain proves SubAbility$ (no chaining
// mechanism exists yet) is a real error rather than silently dropping the
// chained ability (PORT-8/GO-7).
// TestPumpAllEffectChainsIntoSubAbility proves SubAbility$ no longer
// blocks PumpAll's own resolution now that resolveSubAbility (subability.go)
// exists: the chained GainLife runs too, not just PumpAll's own body.
func TestPumpAllEffectChainsIntoSubAbility(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	existing := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	def := etbPumpAllTriggerDefParams(t, "Test PumpAll SubAbility",
		"ValidCards$ Creature.YouCtrl | NumAtt$ 2 | NumDef$ 2 | SubAbility$ DBGainLife",
		map[string]string{"DBGainLife": "DB$ GainLife | Defined$ You | LifeAmount$ 3"})
	if _, err := castETBPumpAll(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(existing).Power(); pw != 3 {
		t.Errorf("power = %d, want 3 -- PumpAll's own body must still run", pw)
	}
	if g.Player(p).Life != 23 {
		t.Errorf("p's life = %d, want 23 -- the chained GainLife must run too", g.Player(p).Life)
	}
}

// TestPumpAllEffectValidTgtsPumpsOnlyTheTargetedPlayersCards proves a PumpAll
// naming ValidTgts$ Player reads the chosen players' cards, not everyone's
// (PumpAllEffect.java:133-137), and RememberPumped$ records each pumped card.
func TestPumpAllEffectValidTgtsPumpsOnlyTheTargetedPlayersCards(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)

	def := etbPumpAllTriggerDefParams(t, "Test PumpAll ValidTgts", "ValidCards$ Creature | NumAtt$ 1 | NumDef$ 1 | ValidTgts$ Player | RememberPumped$ True", nil)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got, _ := g.Card(theirs).Power(); got != 2 {
		t.Errorf("the targeted player's creature power = %d, want 2", got)
	}
	if got, _ := g.Card(mine).Power(); got != 1 {
		t.Errorf("the other player's creature power = %d, want 1", got)
	}
	remembered := g.Card(creature).Memory.Remembered()
	if len(remembered) != 1 || remembered[0] != engine.CardEntity(theirs) {
		t.Errorf("remembered = %v, want only the pumped creature", remembered)
	}
}

// TestPumpAllEffectDefinedNarrowsToPlayer proves Defined$ narrows the
// blanket match to specific players' own cards: ValidCards$ Creature alone
// (no YouCtrl) would otherwise match both players' creatures, but
// Defined$ You must restrict the scan to the caster's own battlefield.
func TestPumpAllEffectDefinedNarrowsToPlayer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)

	if _, err := castETBPumpAll(t, g, p, etbPumpAllTriggerDefParams(t, "Test PumpAll Defined", "ValidCards$ Creature | NumAtt$ 2 | NumDef$ 2 | Defined$ You", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(mine).Power(); pw != 3 {
		t.Errorf("my creature power = %d, want 3 (1 base + 2 pump)", pw)
	}
	if pw, _ := g.Card(theirs).Power(); pw != 1 {
		t.Errorf("opponent's creature power = %d, want 1 -- Defined$ You must restrict the scan to my own battlefield", pw)
	}
}

// TestPumpAllEffectFiresWhenConditionCheckSVarIsMet proves
// subAbilityConditionMet (condition.go) gates PumpAll's own resolution the
// identical way it gates Pump's/DealDamage's/GainLife's.
func TestPumpAllEffectFiresWhenConditionCheckSVarIsMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	existing := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	def := etbPumpAllTriggerDefParams(t, "Test PumpAll Condition Met",
		"ValidCards$ Creature.YouCtrl | NumAtt$ 1 | NumDef$ 1 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", map[string]string{"X": "1"})
	if _, err := castETBPumpAll(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(existing).Power(); pw != 2 {
		t.Errorf("power = %d, want 2 -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 holds (X is 1)", pw)
	}
}

// TestPumpAllEffectNoOpsWhenConditionCheckSVarIsNotMet proves the negative
// control: X GE1 fails (X is 0), so the ability does nothing.
func TestPumpAllEffectNoOpsWhenConditionCheckSVarIsNotMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	existing := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	def := etbPumpAllTriggerDefParams(t, "Test PumpAll Condition Unmet",
		"ValidCards$ Creature.YouCtrl | NumAtt$ 1 | NumDef$ 1 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", map[string]string{"X": "0"})
	if _, err := castETBPumpAll(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(existing).Power(); pw != 1 {
		t.Errorf("power = %d, want 1 -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 fails (X is 0), no pump", pw)
	}
}

// TestPumpAllEffectDefaultZoneExcludesHand proves PumpZone$'s own default,
// Battlefield alone, actually restricts the scan: a card in Hand matching
// ValidCards$ must never be reached by the blanket zone walk.
func TestPumpAllEffectDefaultZoneExcludesHand(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	inHand := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand)

	if _, err := castETBPumpAll(t, g, p, etbPumpAllTriggerDefParams(t, "Test PumpAll Zone", "ValidCards$ Creature.YouCtrl | NumAtt$ 3 | NumDef$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(inHand).Power(); pw != 1 {
		t.Errorf("hand creature power = %d, want 1 -- the default zone scan (Battlefield alone) must not reach it", pw)
	}
}
