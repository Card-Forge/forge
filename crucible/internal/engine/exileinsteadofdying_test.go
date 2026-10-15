package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// This file covers ReplaceDyingDefined$: "if that creature would die this
// turn, exile it instead" -- an effect card remembering the creature, whose
// Moved replacement sends it to exile when it would go from the battlefield
// to a graveyard, until end of turn (SpellAbilityEffect.replaceDying).

// resolveBeforeSBA resolves line as p's ability straight through the
// Registry, off the stack, so no state-based action runs after it: what the
// effect left behind can be looked at before anything dies.
func resolveBeforeSBA(t *testing.T, g *engine.Game, p engine.PlayerID, targets []engine.EntityID, line string, svars ...string) engine.CardID {
	t.Helper()
	def := etbChainDef(t, "Test Now", line, svars...)
	host := g.NewCard(def, p, engine.Battlefield)
	for _, sub := range def.Faces[0].Triggers[0].Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		api, ok := engine.APIByName(sub.Ability.Name)
		if !ok {
			t.Fatalf("unknown API %q", sub.Ability.Name)
		}
		a := engine.Ability{API: api, Source: host, Controller: p, Params: sub.Ability, Amounts: def.Faces[0].Amounts, Targets: targets}
		if err := engine.NewRegistry().Resolve(g, &a, engine.NewScriptedController()); err != nil {
			t.Fatalf("%q: %v", line, err)
		}
	}
	return host
}

// targeting is a controller whose first target answer is ids.
func targeting(ids ...engine.CardID) *engine.ScriptedController {
	c := engine.NewScriptedController()
	var es []engine.EntityID
	for _, id := range ids {
		es = append(es, engine.CardEntity(id))
	}
	c.QueueTargets(es)
	return c
}

// TestLavaCoilExilesTheCreatureItKills is the dominant shape: 4 damage to a
// 3/3 with ReplaceDyingDefined$ Targeted. The effect remembers the 3/3; the
// lethal-damage SBA exiles it instead, and the effect, left remembering
// nothing, leaves the game.
func TestLavaCoilExilesTheCreatureItKills(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	resolveBeforeSBA(t, g, p, []engine.EntityID{engine.CardEntity(victim)},
		"DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 4 | ReplaceDyingDefined$ Targeted")

	effects := commandEffects(g, p)
	if len(effects) != 1 {
		t.Fatalf("command zone holds %d cards, want 1 effect", len(effects))
	}
	eff := effects[0]
	if c := g.Card(eff); !c.IsEffect || c.Def.Name != "Test Now's Effect" {
		t.Errorf("effect = %q (effect %v), want Test Now's Effect", c.Def.Name, c.IsEffect)
	}
	if got := g.Card(eff).Memory.Remembered(); len(got) != 1 || got[0] != engine.CardEntity(victim) {
		t.Errorf("effect remembers %v, want the victim", got)
	}
	sba(g)
	if z := g.Card(victim).Zone; z != engine.Exile {
		t.Errorf("victim zone = %v, want Exile", z)
	}
	if z := g.Card(eff).Zone; z != engine.None {
		t.Errorf("effect zone = %v, want None once its creature left", z)
	}
}

// TestReplaceDyingLastsTheTurnOnly proves a creature that survives the damage
// is still exiled if it dies later this turn (a Destroy), and that after
// cleanup the effect is gone and a later death goes to the graveyard.
func TestReplaceDyingLastsTheTurnOnly(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	first := g.NewCard(creatureDefPT(t, "3", "5"), other, engine.Battlefield)
	second := g.NewCard(creatureDefPT(t, "3", "5"), other, engine.Battlefield)
	resolveLine(t, g, p, targeting(first), "DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 2 | ReplaceDyingDefined$ Targeted")
	resolveLine(t, g, p, targeting(second), "DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 2 | ReplaceDyingDefined$ Targeted")
	sba(g)
	if z := g.Card(first).Zone; z != engine.Battlefield {
		t.Fatalf("2 damage to a 3/5: zone = %v, want Battlefield", z)
	}
	if n := len(commandEffects(g, p)); n != 2 {
		t.Fatalf("command zone holds %d cards, want the two effects", n)
	}
	resolveLine(t, g, p, targeting(first), "DB$ Destroy | ValidTgts$ Creature")
	if z := g.Card(first).Zone; z != engine.Exile {
		t.Errorf("destroyed this turn: zone = %v, want Exile", z)
	}

	endTurn(g, 1, p)
	if n := len(commandEffects(g, p)); n != 0 {
		t.Errorf("command zone holds %d cards after cleanup, want none", n)
	}
	g.SetTurnState(2, p, engine.Main1)
	resolveLine(t, g, p, targeting(second), "DB$ Destroy | ValidTgts$ Creature")
	if z := g.Card(second).Zone; z != engine.Graveyard {
		t.Errorf("destroyed next turn: zone = %v, want Graveyard", z)
	}
}

// TestReplaceDyingFilterAndPlayerTarget is Disintegrate's
// ThisTargetedCard.Creature: a player target makes no effect at all
// (SpellAbilityEffect.java:712-715), and the ".Creature" filter
// (AbilityUtils.java:319-326) keeps a creature target.
func TestReplaceDyingFilterAndPlayerTarget(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	resolveLine(t, g, p, c, "DB$ DealDamage | ValidTgts$ Any | NumDmg$ 3 | ReplaceDyingDefined$ ThisTargetedCard.Creature")
	if n := len(commandEffects(g, p)); n != 0 {
		t.Errorf("player target: command zone holds %d cards, want none", n)
	}
	if got := g.Player(other).Life; got != 17 {
		t.Errorf("life = %d, want 17", got)
	}

	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	resolveLine(t, g, p, targeting(victim), "DB$ DealDamage | ValidTgts$ Any | NumDmg$ 3 | ReplaceDyingDefined$ ThisTargetedCard.Creature")
	sba(g)
	if z := g.Card(victim).Zone; z != engine.Exile {
		t.Errorf("creature target: zone = %v, want Exile", z)
	}
}

// TestReplaceDyingRememberedDamaged is Incendiary Flow's shape:
// RememberDamaged$ fills the host's list, ReplaceDyingDefined$
// Remembered.Creature reads it, and the chain's Cleanup clears the host's
// list without touching the effect's own.
func TestReplaceDyingRememberedDamaged(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	host := resolveLine(t, g, p, targeting(victim),
		"DB$ DealDamage | ValidTgts$ Any | NumDmg$ 3 | RememberDamaged$ True | ReplaceDyingDefined$ Remembered.Creature | SubAbility$ DBCleanup",
		"DBCleanup", "DB$ Cleanup | ClearRemembered$ True")
	if got := g.Card(host).Memory.Remembered(); len(got) != 0 {
		t.Errorf("host remembers %v after Cleanup, want nothing", got)
	}
	sba(g)
	if z := g.Card(victim).Zone; z != engine.Exile {
		t.Errorf("zone = %v, want Exile", z)
	}
}

// TestReplaceDyingPreventedDamageRemembersNothing proves a creature whose
// damage was prevented in full is not remembered by RememberDamaged$, so
// Remembered names no card and no effect is made.
func TestReplaceDyingPreventedDamageRemembersNothing(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(replacementEnchantmentDef(t, "Test Fog",
		"Event$ DamageDone | Prevent$ True | Description$ Prevent all damage."), other, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	resolveLine(t, g, p, targeting(victim),
		"DB$ DealDamage | ValidTgts$ Any | NumDmg$ 3 | RememberDamaged$ True | ReplaceDyingDefined$ Remembered")
	if n := len(commandEffects(g, p)); n != 0 {
		t.Errorf("command zone holds %d cards, want none", n)
	}
}

// TestAngerOfTheGodsExilesWhatDies is DamageAll's shape: every creature is
// dealt damage and remembered; the one that dies is exiled, the
// indestructible one stays.
func TestAngerOfTheGodsExilesWhatDies(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	small := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	tough := g.NewCard(indestructibleCreatureDefPT(t, "2", "2"), other, engine.Battlefield)
	host := resolveBeforeSBA(t, g, p, nil,
		"DB$ DamageAll | ValidCards$ Creature.OppCtrl | NumDmg$ 3 | RememberDamaged$ True | ReplaceDyingDefined$ Remembered | SubAbility$ DBCleanup",
		"DBCleanup", "DB$ Cleanup | ClearRemembered$ True")
	eff := commandEffects(g, p)
	if len(eff) != 1 {
		t.Fatalf("command zone holds %d cards, want 1 effect", len(eff))
	}
	if got := g.Card(eff[0]).Memory.Remembered(); len(got) != 2 {
		t.Errorf("effect remembers %v, want both creatures", got)
	}
	if got := g.Card(host).Memory.Remembered(); len(got) != 0 {
		t.Errorf("host remembers %v after Cleanup, want nothing", got)
	}
	sba(g)
	if z := g.Card(small).Zone; z != engine.Exile {
		t.Errorf("2/2 zone = %v, want Exile", z)
	}
	if z := g.Card(tough).Zone; z != engine.Battlefield {
		t.Errorf("indestructible zone = %v, want Battlefield", z)
	}
}

// TestDamageAllZeroDamageStillRunsReplaceDying proves DamageAll reaches
// replaceDying whatever the amount: with NumDmg$ 0 nothing is dealt or
// remembered, so a Remembered spec makes no effect, but a Self one does.
func TestDamageAllZeroDamageStillRunsReplaceDying(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ DamageAll | ValidCards$ Creature | NumDmg$ 0 | ReplaceDyingDefined$ Self")
	if n := len(commandEffects(g, p)); n != 1 {
		t.Errorf("command zone holds %d cards, want 1 effect", n)
	}
}

// TestBleedDryExilesThePumpedCreature is Pump's shape: -13/-13 kills by the
// toughness SBA, and the creature is exiled instead.
func TestBleedDryExilesThePumpedCreature(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	resolveLine(t, g, p, targeting(victim), "DB$ Pump | ValidTgts$ Creature | NumAtt$ -13 | NumDef$ -13 | IsCurse$ True | ReplaceDyingDefined$ Targeted")
	sba(g)
	if z := g.Card(victim).Zone; z != engine.Exile {
		t.Errorf("zone = %v, want Exile", z)
	}
}

// TestPumpOfNothingStillMakesTheEffect proves Necrotic Wound with X=0 makes
// its effect: Java's PumpEffect.resolve reaches replaceDying with nothing to
// pump (:506).
func TestPumpOfNothingStillMakesTheEffect(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	resolveLine(t, g, p, targeting(victim), "DB$ Pump | ValidTgts$ Creature | NumAtt$ -0 | NumDef$ -0 | ReplaceDyingDefined$ Targeted")
	if n := len(commandEffects(g, p)); n != 1 {
		t.Fatalf("command zone holds %d cards, want 1 effect", n)
	}
	resolveLine(t, g, p, targeting(victim), "DB$ Destroy | ValidTgts$ Creature")
	if z := g.Card(victim).Zone; z != engine.Exile {
		t.Errorf("zone = %v, want Exile", z)
	}
}

// TestFightReplaceDyingExilesTheTarget is Unnatural Aggression's fight: both
// 1/1s die; the target is exiled, the host goes to its graveyard.
func TestFightReplaceDyingExilesTheTarget(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	host := resolveLine(t, g, p, targeting(victim), "DB$ Fight | Defined$ Self | ValidTgts$ Creature.OppCtrl | ReplaceDyingDefined$ Targeted")
	sba(g)
	if z := g.Card(victim).Zone; z != engine.Exile {
		t.Errorf("target zone = %v, want Exile", z)
	}
	if z := g.Card(host).Zone; z != engine.Graveyard {
		t.Errorf("host zone = %v, want Graveyard", z)
	}
}

// TestFightWithoutTwoFightersMakesNoEffect is Mawloc's TargetMin$ 0 with no
// target: no fight, no damage, no effect (FightEffect.java:60-62).
func TestFightWithoutTwoFightersMakesNoEffect(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets(nil)
	resolveLine(t, g, p, c, "DB$ Fight | Defined$ Self | ValidTgts$ Creature.OppCtrl | TargetMin$ 0 | TargetMax$ 1 | ReplaceDyingDefined$ Targeted")
	if n := len(commandEffects(g, p)); n != 0 {
		t.Errorf("command zone holds %d cards, want none", n)
	}
}

// TestSerpentineSpikeDamageResolveRemembersAndExiles is the DamageMap$
// chain ending in DamageResolve | RememberDamaged$ | ReplaceDyingDefined$
// Remembered: the batch is dealt by the DamageResolve, its host remembers
// the creatures, and the one that dies is exiled.
func TestSerpentineSpikeDamageResolveRemembersAndExiles(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "2", "5"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(b)})
	resolveLine(t, g, p, c,
		"DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 2 | DamageMap$ True | SubAbility$ DBTwo",
		"DBTwo", "DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 3 | TargetUnique$ True | SubAbility$ DBResolve",
		"DBResolve", "DB$ DamageResolve | RememberDamaged$ True | ReplaceDyingDefined$ Remembered")
	// The SBA after resolution already exiled the 2/2, which the effect
	// forgot; the 2/5 it still remembers.
	eff := commandEffects(g, p)
	if len(eff) != 1 {
		t.Fatalf("command zone holds %d cards, want 1 effect", len(eff))
	}
	if got := g.Card(eff[0]).Memory.Remembered(); len(got) != 1 || got[0] != engine.CardEntity(b) {
		t.Errorf("effect remembers %v, want the 2/5", got)
	}
	if z := g.Card(a).Zone; z != engine.Exile {
		t.Errorf("2/2 zone = %v, want Exile", z)
	}
	if z := g.Card(b).Zone; z != engine.Battlefield {
		t.Errorf("2/5 zone = %v, want Battlefield", z)
	}
}

// TestBurnTheAccursedMakesTheEffectBeforeTheBatch is DealDamage under
// DamageMap$ naming ReplaceDyingDefined$: the effect is made as that link
// resolves (DamageDealEffect.java:269-272), before DamageResolve deals.
func TestBurnTheAccursedMakesTheEffectBeforeTheBatch(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	resolveLine(t, g, p, targeting(victim),
		"DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 5 | ReplaceDyingDefined$ Targeted | DamageMap$ True | SubAbility$ DBPlayer",
		"DBPlayer", "DB$ DealDamage | Defined$ TargetedController | NumDmg$ 2 | SubAbility$ DBResolve",
		"DBResolve", "DB$ DamageResolve")
	if got := g.Player(other).Life; got != 18 {
		t.Errorf("life = %d, want 18", got)
	}
	sba(g)
	if z := g.Card(victim).Zone; z != engine.Exile {
		t.Errorf("zone = %v, want Exile", z)
	}
}

// TestReplaceDyingKickedConditionUnkicked proves Scorching Lava's
// ReplaceDyingCondition$ Kicked makes no effect for an unkicked spell.
func TestReplaceDyingKickedConditionUnkicked(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	resolveLine(t, g, p, targeting(victim),
		"DB$ DealDamage | ValidTgts$ Any | NumDmg$ 2 | ReplaceDyingDefined$ ThisTargetedCard.Creature | ReplaceDyingCondition$ Kicked")
	if n := len(commandEffects(g, p)); n != 0 {
		t.Errorf("command zone holds %d cards, want none", n)
	}
	sba(g)
	if z := g.Card(victim).Zone; z != engine.Graveyard {
		t.Errorf("zone = %v, want Graveyard", z)
	}
}

// TestReplaceDyingRefusedShapes proves each ReplaceDying shape this port does
// not build fails by name before the effect acts.
func TestReplaceDyingRefusedShapes(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ line, want string }{
		{"DB$ Fight | Defined$ Self | ValidTgts$ Creature | ReplaceDyingDefined$ ThisTargetedCard | ReplaceDyingZone$ Hand", "ReplaceDyingZone$"},
		{"DB$ Pump | ValidTgts$ Creature | NumAtt$ -1 | NumDef$ -1 | ReplaceDyingDefined$ Bogus", "ReplaceDyingDefined$"},
		{"DB$ DamageAll | ValidCards$ Creature | NumDmg$ 1 | ReplaceDyingDefined$ Bogus", "ReplaceDyingDefined$"},
		{"DB$ Fight | Defined$ Self | ValidTgts$ Creature | ReplaceDyingDefined$ Targeted | ReplaceDyingCondition$ Bargain", "ReplaceDyingCondition$"},
	} {
		g, p, other := newTwoPlayerGame(t)
		victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)}, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: err = %v, want one naming %s", tc.line, err, tc.want)
		}
		if d := g.Card(victim).Damage.Marked; d != 0 {
			t.Errorf("%q: victim damage = %d, want 0 (refused before acting)", tc.line, d)
		}
	}
}
