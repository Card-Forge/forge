package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// radianceBoard is a two-player game with a green Elf on the opponent's side
// (the announced target) and a colorless Soldier beside it. The cast host of
// each test is itself a green 1/1 Elf, so it shares a color with the target
// without being targeted. Radiance$ (CardUtil.getRadiance) must reach the
// host and never the Soldier.
func radianceBoard(t *testing.T) (g *engine.Game, p engine.PlayerID, target, soldier engine.CardID) {
	t.Helper()
	g, p, other := newTwoPlayerGame(t)
	target = g.NewCard(etbChainDef(t, "Target Elf", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), other, engine.Battlefield)
	soldier = g.NewCard(tokenDefT(t, "Soldier Token", "Creature Soldier", "1", "1"), other, engine.Battlefield)
	return g, p, target, soldier
}

func TestRadiancePumpReachesEverySharedColorCreature(t *testing.T) {
	t.Parallel()

	g, p, target, soldier := radianceBoard(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
	host := resolveLine(t, g, p, c, "DB$ Pump | ValidTgts$ Creature | Radiance$ True | NumAtt$ +2 | NumDef$ +2")
	for name, id := range map[string]engine.CardID{"target": target, "host": host} {
		if pw, _ := g.Card(id).Power(); pw != 3 {
			t.Errorf("%s power = %d, want 3", name, pw)
		}
	}
	if pw, _ := g.Card(soldier).Power(); pw != 1 {
		t.Errorf("colorless soldier power = %d, want 1", pw)
	}
}

// TestRadianceSubAbilityInheritsTheTargetingParent is Rally the Righteous'
// shape: the Untap targets, its Pump child names Defined$ ParentTarget and no
// ValidTgts$, and getSATargetingCard walks up to the parent. A phased-out
// creature of the shared color is outside the battlefield and left alone.
func TestRadianceSubAbilityInheritsTheTargetingParent(t *testing.T) {
	t.Parallel()

	g, p, target, soldier := radianceBoard(t)
	phased := g.NewCard(etbChainDef(t, "Phased Elf", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	phaseOut(t, g, phased, p)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
	host := resolveLine(t, g, p, c,
		"DB$ Untap | ValidTgts$ Creature | Radiance$ True | SubAbility$ DBPump",
		"DBPump", "DB$ Pump | Defined$ ParentTarget | Radiance$ True | NumAtt$ +2")
	for name, want := range map[string]int{"target": 3, "host": 3} {
		id := map[string]engine.CardID{"target": target, "host": host}[name]
		if pw, _ := g.Card(id).Power(); pw != want {
			t.Errorf("%s power = %d, want %d", name, pw, want)
		}
	}
	if pw, _ := g.Card(soldier).Power(); pw != 1 {
		t.Errorf("colorless soldier power = %d, want 1", pw)
	}
	if pw, _ := g.Card(phased).Power(); pw != 1 {
		t.Errorf("phased-out elf power = %d, want 1", pw)
	}
}

func TestRadianceDealDamageKillsEverySharedColorCreature(t *testing.T) {
	t.Parallel()

	g, p, target, soldier := radianceBoard(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
	host := resolveLine(t, g, p, c, "DB$ DealDamage | ValidTgts$ Creature | Radiance$ True | NumDmg$ 1")
	if g.Card(target).Zone != engine.Graveyard || g.Card(host).Zone != engine.Graveyard {
		t.Errorf("target in %v, host in %v, want both in the graveyard", g.Card(target).Zone, g.Card(host).Zone)
	}
	if g.Card(soldier).Zone != engine.Battlefield {
		t.Errorf("colorless soldier in %v, want the battlefield", g.Card(soldier).Zone)
	}
}

func TestRadianceProtectionGrantsToUntargetedCreature(t *testing.T) {
	t.Parallel()

	g, p, target, soldier := radianceBoard(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
	c.QueueProtectionChoice(0)
	host := resolveLine(t, g, p, c,
		"DB$ Protection | ValidTgts$ Creature | Radiance$ True | Gains$ Choice | Choices$ AnyColor")
	for name, id := range map[string]engine.CardID{"target": target, "host": host} {
		if !strings.Contains(strings.Join(g.Card(id).KeywordLines(), "|"), "Protection from white") {
			t.Errorf("%s keywords = %v, want protection from white", name, g.Card(id).KeywordLines())
		}
	}
	if strings.Contains(strings.Join(g.Card(soldier).KeywordLines(), "|"), "Protection") {
		t.Errorf("colorless soldier keywords = %v, want none", g.Card(soldier).KeywordLines())
	}
}

// TestRadiancePreventDamageShieldsUntargetedCreature proves the shield lands
// on the host (never targeted): the self-inflicted damage that would kill the
// 1/1 host is prevented.
func TestRadiancePreventDamageShieldsUntargetedCreature(t *testing.T) {
	t.Parallel()

	g, p, target, _ := radianceBoard(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
	host := resolveLine(t, g, p, c,
		"DB$ PreventDamage | ValidTgts$ Creature | Radiance$ True | Amount$ 1 | SubAbility$ DBHit",
		"DBHit", "DB$ DealDamage | Defined$ Self | NumDmg$ 1")
	if z := g.Card(host).Zone; z != engine.Battlefield {
		t.Errorf("host in %v, want the battlefield (its shield prevents the damage)", z)
	}
}

// TestRadianceUntargetedWithoutCardTargets proves a Radiance$ line whose
// target is a player fans out to nothing (getRadiance reads card targets).
func TestRadianceUntargetedWithoutCardTargets(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	host := resolveLine(t, g, p, c, "DB$ DealDamage | ValidTgts$ Any | Radiance$ True | NumDmg$ 1")
	if got := g.Player(other).Life; got != 19 {
		t.Errorf("opponent life = %d, want 19", got)
	}
	if z := g.Card(host).Zone; z != engine.Battlefield {
		t.Errorf("host in %v, want the battlefield", z)
	}
}
