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

// etbDealDamageTriggerDefParams builds a *compile.Card whose own "when
// CARDNAME enters" trigger runs DB$ DealDamage with the given params string
// appended -- dealDamageEffect itself is unexported, so every case here is
// driven through the real cast+resolve pipeline rather than calling it
// directly (TEST-1), the identical reason etbDrawTriggerDefParams
// (draweffect_test.go) is.
func etbDealDamageTriggerDefParams(t *testing.T, name, extraParams string, svars map[string]string) *compile.Card {
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
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigDamage",
	}
	raw.Faces[0].SVars.Set("TrigDamage", "DB$ DealDamage | "+extraParams)
	for name, body := range svars {
		raw.Faces[0].SVars.Set(name, body)
	}

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBDealDamage casts def (built by etbDealDamageTriggerDefParams) for p
// and resolves the stack, returning ResolveStack's own error.
func castETBDealDamage(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card) error {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

// TestDealDamageEffectDefinedYouDamagesController proves Defined$ You damages
// the ability's own controller, not any opponent.
func TestDealDamageEffectDefinedYouDamagesController(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	if err := castETBDealDamage(t, g, p, etbDealDamageTriggerDefParams(t, "Test You", "Defined$ You | NumDmg$ 3", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 17 {
		t.Errorf("p's life = %d, want 17", g.Player(p).Life)
	}
	if g.Player(other).Life != 20 {
		t.Errorf("other's life = %d, want 20 -- Defined$ You must not hit an opponent", g.Player(other).Life)
	}
}

// TestDealDamageEffectDefinedOpponentDamagesEveryOpponent proves Defined$
// Opponent damages every opponent of the controller, not the controller
// itself -- a three-player game so "every" is observably more than one.
func TestDealDamageEffectDefinedOpponentDamagesEveryOpponent(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b", "c")
	p, o1, o2 := g.Players()[0], g.Players()[1], g.Players()[2]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(o1).Life, g.Player(o2).Life = 20, 20, 20

	if err := castETBDealDamage(t, g, p, etbDealDamageTriggerDefParams(t, "Test Opponent", "Defined$ Opponent | NumDmg$ 2", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(o1).Life != 18 {
		t.Errorf("o1's life = %d, want 18", g.Player(o1).Life)
	}
	if g.Player(o2).Life != 18 {
		t.Errorf("o2's life = %d, want 18", g.Player(o2).Life)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want 20 -- Defined$ Opponent must not hit the controller", g.Player(p).Life)
	}
}

// TestDealDamageEffectDefinedSelfDamagesTheHostCard proves Defined$ Self
// damages the ability's own host card rather than a player -- no Deathtouch
// here, since any nonzero deathtouch damage is lethal (CR 702.2b) and would
// destroy the creature before its own Damage could be inspected;
// TestDealDamageEffectDefinedSelfWithDeathtouchIsLethalToItself below proves
// that shape instead, through the state-based action it triggers rather
// than the marked-damage field a dead creature no longer meaningfully has.
func TestDealDamageEffectDefinedSelfDamagesTheHostCard(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "Test Self Damage"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Test Self Damage"
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "1", "3"
	raw.Faces[0].ManaCost = mana.MustParse("G")
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigDamage",
	}
	raw.Faces[0].SVars.Set("TrigDamage", "DB$ DealDamage | Defined$ Self | NumDmg$ 1")
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}

	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(creature).Zone != engine.Battlefield {
		t.Fatalf("creature zone = %v, want Battlefield -- 1 damage to a 3-toughness creature is not lethal", g.Card(creature).Zone)
	}
	if g.Card(creature).Damage.Marked != 1 {
		t.Errorf("Damage.Marked = %d, want 1", g.Card(creature).Damage.Marked)
	}
	if g.Player(other).Life != 20 {
		t.Errorf("other's life = %d, want 20 -- Defined$ Self must not touch a player", g.Player(other).Life)
	}
}

// TestDealDamageEffectDefinedSelfWithDeathtouchIsLethalToItself proves a
// Deathtouch source's own damage is marked as such (dealPermanentDamage's
// own deathtouch parameter, read off the source's HasKeyword("Deathtouch")),
// which CR 702.2b/704.5g's own state-based action reads as lethal
// regardless of amount -- the creature destroys itself, the identical rule
// that makes TestDealDamageEffectDefinedSelfDamagesTheHostCard's own
// Deathtouch-free shape the right one for inspecting marked damage instead.
func TestDealDamageEffectDefinedSelfWithDeathtouchIsLethalToItself(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "Test Self Deathtouch"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Test Self Deathtouch"
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "1", "3"
	raw.Faces[0].ManaCost = mana.MustParse("G")
	raw.Faces[0].Keywords = []string{"Deathtouch"}
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigDamage",
	}
	raw.Faces[0].SVars.Set("TrigDamage", "DB$ DealDamage | Defined$ Self | NumDmg$ 1")
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}

	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(creature).Zone != engine.Graveyard {
		t.Errorf("creature zone = %v, want Graveyard -- 1 deathtouch damage to a 3-toughness creature is still lethal", g.Card(creature).Zone)
	}
}

// TestDealDamageEffectResolvesNamedNumDmgSVar proves NumDmg$ resolves a named
// SVar reference through resolveNamedAmount (amount.go), not just a plain
// integer.
func TestDealDamageEffectResolvesNamedNumDmgSVar(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbDealDamageTriggerDefParams(t, "Test Named NumDmg", "Defined$ Player.Opponent | NumDmg$ X", map[string]string{"X": "5"})
	if err := castETBDealDamage(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(other).Life != 15 {
		t.Errorf("other's life = %d, want 15 -- NumDmg$ X must resolve through the named SVar X:5", g.Player(other).Life)
	}
}

// TestDealDamageEffectMissingNumDmgErrors proves a missing NumDmg$ is a real
// error rather than a silent zero-damage no-op (GO-7).
func TestDealDamageEffectMissingNumDmgErrors(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	err := castETBDealDamage(t, g, p, etbDealDamageTriggerDefParams(t, "Test No NumDmg", "Defined$ You", nil))
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming NumDmg$")
	}
	if !strings.Contains(err.Error(), "NumDmg") {
		t.Errorf("ResolveStack error = %q, want it to name NumDmg$", err.Error())
	}
}

// TestDealDamageEffectRejectsSubAbilityChain proves SubAbility$ (no chaining
// mechanism exists yet) is a real error rather than silently dropping the
// chained ability (PORT-8/GO-7).
// TestDealDamageEffectChainsIntoSubAbility proves SubAbility$ no longer
// blocks DealDamage's own resolution now that resolveSubAbility
// (subability.go) exists -- sword_of_fire_and_ice_and_war_and_peace.txt's
// own real shape (DealDamage chaining into GainLife) with a plain integer
// standing in for its own X/Y SVar amounts.
func TestDealDamageEffectChainsIntoSubAbility(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbDealDamageTriggerDefParams(t, "Test SubAbility",
		"Defined$ You | NumDmg$ 1 | SubAbility$ DBGainLife",
		map[string]string{"DBGainLife": "DB$ GainLife | Defined$ You | LifeAmount$ 3"})
	if err := castETBDealDamage(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 22 {
		t.Errorf("p's life = %d, want 22 -- DealDamage's own body (20-1=19) and the chained GainLife (19+3=22) must both run", g.Player(p).Life)
	}
}

// TestDealDamageEffectRejectsUnresolvableDamageSource proves a DamageSource$
// reference this port does not carry fails the whole line by name rather
// than guessing a source (PORT-8/GO-7).
func TestDealDamageEffectRejectsUnresolvableDamageSource(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbDealDamageTriggerDefParams(t, "Test DamageSource", "Defined$ You | NumDmg$ 1 | DamageSource$ OriginalHost", nil)
	err := castETBDealDamage(t, g, p, def)
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming DamageSource")
	}
	if !strings.Contains(err.Error(), "DamageSource$") || !strings.Contains(err.Error(), "OriginalHost") {
		t.Errorf("ResolveStack error = %q, want it to name DamageSource$ and its value", err.Error())
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want 20 -- a rejected line deals nothing", g.Player(p).Life)
	}
}

// TestDealDamageEffectFiresWhenConditionCheckSVarIsMet proves
// subAbilityConditionMet (condition.go) now gates DealDamage's own
// resolution the same way it gates a checkland's DB$ Tap: X GE1 holds (X is
// 1), so the damage happens as normal.
func TestDealDamageEffectFiresWhenConditionCheckSVarIsMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbDealDamageTriggerDefParams(t, "Test Condition Met",
		"Defined$ You | NumDmg$ 3 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", map[string]string{"X": "1"})
	if err := castETBDealDamage(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 17 {
		t.Errorf("p's life = %d, want 17 -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 holds (X is 1)", g.Player(p).Life)
	}
}

// TestDealDamageEffectNoOpsWhenConditionCheckSVarIsNotMet proves the negative
// control: X GE1 fails (X is 0), so the ability does nothing -- not an error,
// SpellAbilityCondition.areMet's own "declined by the rules" contract.
func TestDealDamageEffectNoOpsWhenConditionCheckSVarIsNotMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbDealDamageTriggerDefParams(t, "Test Condition Unmet",
		"Defined$ You | NumDmg$ 3 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", map[string]string{"X": "0"})
	if err := castETBDealDamage(t, g, p, def); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want 20 -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 fails (X is 0), no damage", g.Player(p).Life)
	}
}

// TestDealDamageEffectRejectsConditionItself proves Condition$ (the flag
// switch, distinct from the resolved ConditionCheckSVar$/ConditionPresent$
// pair) still fails loudly -- SpellAbilityCondition's own Threshold/
// Metalcraft/... family, no evaluator built for it.
func TestDealDamageEffectRejectsConditionItself(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbDealDamageTriggerDefParams(t, "Test Condition Flag", "Defined$ You | NumDmg$ 1 | Condition$ Teamwork", nil)
	err := castETBDealDamage(t, g, p, def)
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming Condition")
	}
	if !strings.Contains(err.Error(), "Condition") {
		t.Errorf("ResolveStack error = %q, want it to name Condition$", err.Error())
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want 20 -- a rejected line must not deal partial damage", g.Player(p).Life)
	}
}

// TestDealDamageEffectDamageIsPreventedByReplacement proves
// dealDamageEffect's own damage reaches damagePrevented/damagePreventedPlayer
// (replacement.go) the identical way combat damage already does -- CR 614's
// own "prevent all of this damage" applies regardless of the source.
func TestDealDamageEffectDamageIsPreventedByReplacement(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(replacementEnchantmentDef(t, "Test Damage Shield",
		"Event$ DamageDone | ValidTarget$ You | Prevent$ True | Description$ Prevent all damage that would be dealt to you."), other, engine.Battlefield)

	if err := castETBDealDamage(t, g, p, etbDealDamageTriggerDefParams(t, "Test Prevented", "Defined$ Player.Opponent | NumDmg$ 5", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(other).Life != 20 {
		t.Errorf("other's life = %d, want 20 -- Prevent$ True must stop dealDamageEffect's own damage too", g.Player(other).Life)
	}
}

// TestDealDamageEffectEventDoesNotCarryFlagCombat proves a script-driven
// DealDamage emits DamageDealt without FlagCombat -- FlagCombat's own doc
// comment (event.go) distinguishes combat damage from an effect's, and this
// is the first effect to ever produce the second case.
func TestDealDamageEffectEventDoesNotCarryFlagCombat(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	var sink recordingSink
	g.SetSink(&sink)

	if err := castETBDealDamage(t, g, p, etbDealDamageTriggerDefParams(t, "Test No Combat Flag", "Defined$ Player.Opponent | NumDmg$ 1", nil)); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	var saw bool
	for _, e := range sink.events {
		if e.Kind == engine.DamageDealt {
			saw = true
			if e.Flags&engine.FlagCombat != 0 {
				t.Errorf("DamageDealt Flags = %v, want FlagCombat unset for a script effect", e.Flags)
			}
		}
	}
	if !saw {
		t.Error("no DamageDealt event seen")
	}
}
