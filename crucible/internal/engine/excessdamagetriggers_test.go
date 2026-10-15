package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// This file covers the excess-damage record of a damage batch
// (CardDamageTable.triggerExcessDamage): Damage.ExcessThisTurn, the
// wasDealtExcessDamageThisTurn valid property, and the Mode$ ExcessDamage and
// ExcessDamageAll triggers.

// TestExcessDamageTriggerReadsTheExcess is Fall of Cair Andros' shape: a
// noncombat excess to an opponent's creature fires once with
// TriggerCount$DamageAmount the excess; no excess, or a CombatDamage$ True
// line, does not fire.
func TestExcessDamageTriggerReadsTheExcess(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, combat, dmg string
		want              int
	}{
		{"excess", "False", "5", 23},
		{"no excess", "False", "2", 20},
		{"combat only", "True", "5", 20},
	} {
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(scriptDef(t, "Watcher", "Enchantment",
			"T:Mode$ ExcessDamage | ValidTarget$ Creature.OppCtrl | CombatDamage$ "+tc.combat+" | TriggerZones$ Battlefield | Execute$ TrigGain",
			"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ X",
			"SVar:X:TriggerCount$DamageAmount"), p, engine.Battlefield)
		victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		resolveLine(t, g, p, targeting(victim), "DB$ DealDamage | ValidTgts$ Creature | NumDmg$ "+tc.dmg)
		if got := g.Player(p).Life; got != tc.want {
			t.Errorf("%s: life = %d, want %d", tc.name, got, tc.want)
		}
	}
}

// TestExcessDamageAllFiresOncePerBatch is Magmatic Galleon's shape: two of
// the opponent's creatures dealt excess by one sweep fire it once; excess to
// the controller's own creature alone does not.
func TestExcessDamageAllFiresOncePerBatch(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name    string
		theirs  bool
		wantGot int
	}{
		{"opponent's creatures", true, 21},
		{"own creature only", false, 20},
	} {
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(scriptDef(t, "Galleon", "Enchantment",
			"T:Mode$ ExcessDamageAll | ValidTarget$ Creature.OppCtrl | CombatDamage$ False | TriggerZones$ Battlefield | Execute$ TrigGain",
			"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
		if tc.theirs {
			g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
			g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
		} else {
			g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
		}
		resolveLine(t, g, p, engine.NewScriptedController(), "DB$ DamageAll | ValidCards$ Creature | NumDmg$ 3")
		if got := g.Player(p).Life; got != tc.wantGot {
			t.Errorf("%s: life = %d, want %d", tc.name, got, tc.wantGot)
		}
	}
}

// TestDamageDoneSeesTheSameBatchsExcess is Zangief's shape: a DamageDone
// trigger gated on ValidTarget$ Creature.wasDealtExcessDamageThisTurn fires for
// the damage that dealt the excess (Java tests it after the batch,
// GameAction.java:2766-2779), not for damage that was not excess.
func TestDamageDoneSeesTheSameBatchsExcess(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		dmg  string
		want int
	}{
		{"5", 21},
		{"2", 20},
	} {
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(scriptDef(t, "Piledriver", "Enchantment",
			"T:Mode$ DamageDone | ValidTarget$ Creature.wasDealtExcessDamageThisTurn | TriggerZones$ Battlefield | Execute$ TrigGain",
			"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
		victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		resolveLine(t, g, p, targeting(victim), "DB$ DealDamage | ValidTgts$ Creature | NumDmg$ "+tc.dmg)
		if got := g.Player(p).Life; got != tc.want {
			t.Errorf("%s damage: life = %d, want %d", tc.dmg, got, tc.want)
		}
	}
}

// TestExcessFlagSurvivesHealing proves a heal keeps the turn's excess record
// (Card.healDamage, Card.java:6181-6185) while the cleanup step's Clear drops
// it.
func TestExcessFlagSurvivesHealing(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	resolveBeforeSBA(t, g, p, []engine.EntityID{engine.CardEntity(victim)}, "DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 3")
	excess := valid.Parse("Creature.wasDealtExcessDamageThisTurn")
	if !engine.Matches(g, g.Card(victim), excess, p, engine.NoCard) {
		t.Fatal("3 damage to a 2/2: wasDealtExcessDamageThisTurn false, want true")
	}
	resolveBeforeSBA(t, g, p, []engine.EntityID{engine.CardEntity(victim)}, "DB$ HealDamage | ValidTgts$ Creature")
	if d := g.Card(victim).Damage; d.Marked != 0 || !d.ExcessThisTurn {
		t.Errorf("after the heal: marked %d, excess %v; want 0 and true", d.Marked, d.ExcessThisTurn)
	}
	g.Card(victim).Damage.Clear()
	if engine.Matches(g, g.Card(victim), excess, p, engine.NoCard) {
		t.Error("after Clear: wasDealtExcessDamageThisTurn true, want false")
	}
}

// TestTriggeredTargetValidProperty is Toralf's Creature.!TriggeredTarget
// read the other way: a DamageDone trigger's PumpAll reaches only the damaged
// creature, its AbilityKey.Target.
func TestTriggeredTargetValidProperty(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(scriptDef(t, "Watcher", "Enchantment",
		"T:Mode$ DamageDone | ValidTarget$ Creature | TriggerZones$ Battlefield | Execute$ TrigPump",
		"SVar:TrigPump:DB$ PumpAll | ValidCards$ Creature.TriggeredTarget | NumAtt$ +3"), p, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "2", "4"), other, engine.Battlefield)
	bystander := g.NewCard(creatureDefPT(t, "2", "4"), other, engine.Battlefield)
	resolveLine(t, g, p, targeting(victim), "DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 1")
	sba(g)
	if pw, _ := g.Card(victim).Power(); pw != 5 {
		t.Errorf("damaged creature power = %d, want 5", pw)
	}
	if pw, _ := g.Card(bystander).Power(); pw != 2 {
		t.Errorf("bystander power = %d, want 2", pw)
	}
	if engine.Matches(g, g.Card(victim), valid.Parse("Creature.TriggeredTarget"), p, engine.NoCard) {
		t.Error("TriggeredTarget with no ability in context: true, want false")
	}
}
