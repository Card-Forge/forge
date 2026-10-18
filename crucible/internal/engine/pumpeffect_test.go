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

// etbPumpTriggerDefParams builds a *compile.Card whose own "when CARDNAME
// enters" trigger runs DB$ Pump with the given params string appended --
// pumpEffect itself is unexported, so every case here is driven through the
// real cast+resolve pipeline rather than calling it directly (TEST-1), the
// identical reason etbGainLifeTriggerDefParams (gainlifeeffect_test.go) is.
func etbPumpTriggerDefParams(t *testing.T, name, extraParams string, svars map[string]string) *compile.Card {
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
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigPump",
	}
	raw.Faces[0].SVars.Set("TrigPump", "DB$ Pump | "+extraParams)
	for name, body := range svars {
		raw.Faces[0].SVars.Set(name, body)
	}

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBPump casts def (built by etbPumpTriggerDefParams) for p and
// resolves the stack, returning the creature's own CardID and
// ResolveStack's own error.
func castETBPump(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card) (engine.CardID, error) {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return creature, g.ResolveStack(engine.NewRegistry(), c)
}

// TestPumpEffectDefinedSelfGrantsPT proves the corpus's single largest real
// Defined$ shape: Defined$ Self grants NumAtt$/NumDef$ to the ability's own
// host, applied through applyPumpEffects (continuous.go) the moment
// ResolveStack's own CheckStateBasedActions call runs.
func TestPumpEffectDefinedSelfGrantsPT(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	creature, err := castETBPump(t, g, p, etbPumpTriggerDefParams(t, "Test Self PT", "Defined$ Self | NumAtt$ 2 | NumDef$ 2", nil))
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	pw, _ := g.Card(creature).Power()
	tg, _ := g.Card(creature).Toughness()
	if pw != 4 || tg != 4 {
		t.Errorf("power/toughness = %d/%d, want 4/4 (2/2 base + 2/2 pump)", pw, tg)
	}
}

// TestPumpEffectDefinedSelfGrantsKeyword proves KW$ grants a keyword through
// KeywordMod the same way NumAtt$/NumDef$ grants a PTEffect.
func TestPumpEffectDefinedSelfGrantsKeyword(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	creature, err := castETBPump(t, g, p, etbPumpTriggerDefParams(t, "Test Self Keyword", "Defined$ Self | KW$ Flying", nil))
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !g.Card(creature).HasKeyword("Flying") {
		t.Error("creature does not have Flying, want it granted by Pump")
	}
}

// TestPumpEffectExpiresAtCleanup proves the default duration -- "until end
// of turn" -- actually wears off: cleanupStep (turn.go) drops every
// non-Permanent pumpRecord, so the same creature's power/toughness reverts
// to base once AdvancePhase reaches Cleanup.
func TestPumpEffectExpiresAtCleanup(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	creature, err := castETBPump(t, g, p, etbPumpTriggerDefParams(t, "Test EOT Expiry", "Defined$ Self | NumAtt$ 3 | NumDef$ 3", nil))
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(creature).Power(); pw != 5 {
		t.Fatalf("power before cleanup = %d, want 5", pw)
	}

	g.SetTurnState(1, p, engine.EndOfTurn)
	g.AdvancePhase(engine.NewScriptedController()) // -> Cleanup

	pw, _ := g.Card(creature).Power()
	tg, _ := g.Card(creature).Toughness()
	if pw != 2 || tg != 2 {
		t.Errorf("power/toughness after cleanup = %d/%d, want 2/2 (pump worn off)", pw, tg)
	}
}

// TestPumpEffectPermanentDurationSurvivesCleanup proves the negative control:
// Duration$ Permanent is never dropped by cleanupStep.
func TestPumpEffectPermanentDurationSurvivesCleanup(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	creature, err := castETBPump(t, g, p,
		etbPumpTriggerDefParams(t, "Test Permanent", "Defined$ Self | NumAtt$ 1 | NumDef$ 1 | Duration$ Permanent", nil))
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	g.SetTurnState(1, p, engine.EndOfTurn)
	g.AdvancePhase(engine.NewScriptedController()) // -> Cleanup

	pw, _ := g.Card(creature).Power()
	tg, _ := g.Card(creature).Toughness()
	if pw != 3 || tg != 3 {
		t.Errorf("power/toughness after cleanup = %d/%d, want 3/3 -- Duration$ Permanent must survive cleanup", pw, tg)
	}
}

// TestPumpEffectRejectsSubAbilityChain proves SubAbility$ (no chaining
// mechanism exists yet) is a real error rather than silently dropping the
// chained ability (PORT-8/GO-7).
// TestPumpEffectChainsIntoSubAbility proves SubAbility$ no longer blocks
// Pump's own resolution now that resolveSubAbility (subability.go) exists
// -- rabaroo_troop.txt's own real shape (Pump | Defined$ Self | KW$ Flying,
// chaining into DB$ GainLife | Defined$ You | LifeAmount$ 1).
func TestPumpEffectChainsIntoSubAbility(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	def := etbPumpTriggerDefParams(t, "Test SubAbility",
		"Defined$ Self | NumAtt$ 2 | NumDef$ 2 | SubAbility$ DBGainLife",
		map[string]string{"DBGainLife": "DB$ GainLife | Defined$ You | LifeAmount$ 1"})
	creature, err := castETBPump(t, g, p, def)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(creature).Power(); pw != 4 {
		t.Errorf("power = %d, want 4 -- Pump's own body must still run", pw)
	}
	if g.Player(p).Life != 21 {
		t.Errorf("p's life = %d, want 21 -- the chained GainLife must run too", g.Player(p).Life)
	}
}

// TestPumpEffectRejectsUnreadHiddenKeyword proves a KW$ "HIDDEN" line nothing
// in this port reads (Whippoorwill's no-redirection line, ReplaceDamage.java:100)
// fails loudly, naming the line, rather than being granted and ignored.
func TestPumpEffectRejectsUnreadHiddenKeyword(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	def := etbPumpTriggerDefParams(t, "Test Hidden Keyword",
		`Defined$ Self | KW$ Flying & HIDDEN Damage that would be dealt to CARDNAME can't be redirected.`, nil)
	creature, err := castETBPump(t, g, p, def)
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming KW")
	}
	if !strings.Contains(err.Error(), "KW$ hidden keyword") || !strings.Contains(err.Error(), "can't be redirected") {
		t.Errorf("ResolveStack error = %q, want it to name the hidden KW$ line", err.Error())
	}
	if g.Card(creature).HasKeyword("Flying") {
		t.Error("creature has Flying: a rejected line must grant nothing")
	}
}

// TestPumpEffectFiresWhenConditionCheckSVarIsMet proves subAbilityConditionMet
// (condition.go) gates Pump's own resolution the identical way it gates
// DealDamage's/GainLife's and a checkland's DB$ Tap.
func TestPumpEffectFiresWhenConditionCheckSVarIsMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	def := etbPumpTriggerDefParams(t, "Test Condition Met",
		"Defined$ Self | NumAtt$ 1 | NumDef$ 1 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", map[string]string{"X": "1"})
	creature, err := castETBPump(t, g, p, def)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(creature).Power(); pw != 3 {
		t.Errorf("power = %d, want 3 -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 holds (X is 1)", pw)
	}
}

// TestPumpEffectNoOpsWhenConditionCheckSVarIsNotMet proves the negative
// control: X GE1 fails (X is 0), so the ability does nothing -- not an
// error, SpellAbilityCondition.areMet's own "declined by the rules" contract.
func TestPumpEffectNoOpsWhenConditionCheckSVarIsNotMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	def := etbPumpTriggerDefParams(t, "Test Condition Unmet",
		"Defined$ Self | NumAtt$ 1 | NumDef$ 1 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", map[string]string{"X": "0"})
	creature, err := castETBPump(t, g, p, def)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(creature).Power(); pw != 2 {
		t.Errorf("power = %d, want 2 -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 fails (X is 0), no pump", pw)
	}
}

// TestPumpEffectPumpZoneRestrictsTarget proves PumpZone$ (default
// Battlefield alone): naming a zone the target is not actually in skips the
// pump entirely, rather than applying it regardless of PumpZone$'s own
// restriction -- here Defined$ Self's own card is on the Battlefield (that
// is what "enters" means), so PumpZone$ Graveyard must never match it.
func TestPumpEffectPumpZoneRestrictsTarget(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	def := etbPumpTriggerDefParams(t, "Test PumpZone", "Defined$ Self | NumAtt$ 2 | NumDef$ 2 | PumpZone$ Graveyard", nil)
	creature, err := castETBPump(t, g, p, def)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if pw, _ := g.Card(creature).Power(); pw != 2 {
		t.Errorf("power = %d, want 2 -- PumpZone$ Graveyard must not match a card on the Battlefield", pw)
	}
}

// auraDefWithEnchantAndPumpTrigger builds an Aura -- K:Enchant$Creature, no
// mana cost -- whose own ETB trigger runs DB$ Pump with the given params,
// the shape a "enchanted creature gets +1/+1" Aura's own real card script
// carries: the boost lives on the Aura's own SVar, granted the instant it
// enters attached, through Defined$ Enchanted resolving against the Aura's
// own AttachedTo() (definedCards, defined.go).
func auraDefWithEnchantAndPumpTrigger(t *testing.T, name, extraParams string) *compile.Card {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, "Enchantment Aura")
	raw.Faces[0].Keywords = []string{"Enchant:Creature"}
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigPump",
	}
	raw.Faces[0].SVars.Set("TrigPump", "DB$ Pump | "+extraParams)

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// TestPumpEffectDefinedEnchantedGrantsHostPT proves the Aura shape:
// Defined$ Enchanted resolves against the Aura's own AttachedTo() rather
// than the Aura itself, so the +X/+X lands on the enchanted creature.
func TestPumpEffectDefinedEnchantedGrantsHostPT(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	target := g.NewCard(creatureDef(t), p, engine.Battlefield)
	aura := g.NewCard(auraDefWithEnchantAndPumpTrigger(t, "Test Enchanted Pump", "Defined$ Enchanted | NumAtt$ 1 | NumDef$ 1"), p, engine.Hand)
	c := engine.NewScriptedController()

	if !g.CastSpell(p, aura, c) {
		t.Fatal("CastSpell failed casting an Aura with exactly one legal target")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if host, ok := g.Card(aura).AttachedTo(); !ok || host != target {
		t.Fatalf("aura attached to %v, %v, want %v, true", host, ok, target)
	}

	pw, _ := g.Card(target).Power()
	tg, _ := g.Card(target).Toughness()
	if pw != 3 || tg != 3 {
		t.Errorf("enchanted creature's power/toughness = %d/%d, want 3/3 (2/2 base + 1/1 pump)", pw, tg)
	}
	if apw, _ := g.Card(aura).Power(); apw != 0 {
		t.Errorf("aura's own power = %d, want 0 (Defined$ Enchanted must not pump the Aura itself)", apw)
	}
}
