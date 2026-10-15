package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// This file covers ExcessSVar$: the damage a batch dealt a permanent past
// what was lethal to it, summed and bound as an SVar the rest of the chain
// reads (CardDamageTable.triggerExcessDamage, GameAction.dealDamage).

// gainExcess is the reader link every test here chains: the controller gains
// life equal to the Excess SVar.
var gainExcess = []string{"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ Excess"}

// TestExcessSVarBindsTheExcessForTheChain is Razor Rings' shape: 6 damage to
// a 2/2 is 4 excess, read by the GainLife link.
func TestExcessSVarBindsTheExcessForTheChain(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name          string
		power, tough  string
		marked, wantP int
	}{
		{"2/2", "2", "2", 0, 24},
		{"damaged 2/4", "2", "4", 1, 23},
		{"7/7", "7", "7", 0, 20},
	} {
		g, p, other := newTwoPlayerGame(t)
		victim := g.NewCard(creatureDefPT(t, tc.power, tc.tough), other, engine.Battlefield)
		g.Card(victim).Damage.Marked = tc.marked
		resolveLine(t, g, p, targeting(victim),
			"DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 6 | ExcessSVar$ Excess | SubAbility$ DBGain", gainExcess...)
		if got := g.Player(p).Life; got != tc.wantP {
			t.Errorf("%s: life = %d, want %d", tc.name, got, tc.wantP)
		}
	}
}

// TestExcessSVarConditionCheckGatesTheLink is Orbital Plunge's
// ConditionCheckSVar$ Excess: no excess, no token-shaped link.
func TestExcessSVarConditionCheckGatesTheLink(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		tough string
		want  int
	}{{"2", 21}, {"9", 20}} {
		g, p, other := newTwoPlayerGame(t)
		victim := g.NewCard(creatureDefPT(t, "2", tc.tough), other, engine.Battlefield)
		resolveLine(t, g, p, targeting(victim),
			"DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 6 | ExcessSVar$ Excess | SubAbility$ DBGain",
			"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 1 | ConditionCheckSVar$ Excess")
		if got := g.Player(p).Life; got != tc.want {
			t.Errorf("toughness %s: life = %d, want %d", tc.tough, got, tc.want)
		}
	}
}

// TestExcessSVarPlaneswalkerAndPlayer proves a planeswalker's excess is past
// its loyalty, and damage to a player adds none.
func TestExcessSVarPlaneswalkerAndPlayer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	pw := g.NewCard(planeswalkerDefLoyalty(t, "3"), other, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 3)
	resolveLine(t, g, p, targeting(pw),
		"DB$ DealDamage | ValidTgts$ Planeswalker | NumDmg$ 5 | ExcessSVar$ Excess | SubAbility$ DBGain", gainExcess...)
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("planeswalker: life = %d, want 22", got)
	}

	g, p, other = newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	resolveLine(t, g, p, c, "DB$ DealDamage | ValidTgts$ Any | NumDmg$ 5 | ExcessSVar$ Excess | SubAbility$ DBGain", gainExcess...)
	if g.Player(p).Life != 20 || g.Player(other).Life != 15 {
		t.Errorf("player: life %d/%d, want 20/15", g.Player(p).Life, g.Player(other).Life)
	}
}

// TestExcessSVarDeathtouchSourceCountsPastOne is Windswift Slice's bite with a
// deathtouch biter: 3 damage to a 5/5 is lethal past the first point, so 2
// excess (CardDamageTable.java:126).
func TestExcessSVarDeathtouchSourceCountsPastOne(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	biter := g.NewCard(creatureDefPTKeywords(t, "3", "3", "Deathtouch"), p, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "5", "5"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(biter)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	resolveLine(t, g, p, c, "DB$ Pump | ValidTgts$ Creature.YouCtrl | SubAbility$ Bite",
		"Bite", "DB$ DealDamage | ValidTgts$ Creature.YouDontCtrl | NumDmg$ X | DamageSource$ ParentTarget | ExcessSVar$ Excess | SubAbility$ DBGain",
		"X", "ParentTargeted$CardPower", gainExcess[0], gainExcess[1])
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22", got)
	}
}

// TestExcessSVarUnsetReadsZero proves an ExcessSVar$ ability that deals
// nothing -- its bite's biter is gone -- leaves the SVar reading 0, as Java's
// calculateAmount reads an unset SVar, rather than failing the chain.
func TestExcessSVarUnsetReadsZero(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets(nil)
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	resolveLine(t, g, p, c, "DB$ Pump | ValidTgts$ Creature.YouCtrl | TargetMin$ 0 | SubAbility$ Bite",
		"Bite", "DB$ DealDamage | ValidTgts$ Creature.YouDontCtrl | NumDmg$ 3 | DamageSource$ ParentTarget | ExcessSVar$ Excess | SubAbility$ DBGain",
		gainExcess[0], gainExcess[1])
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("life = %d, want 20", got)
	}
}

// TestExcessSVarCardDefinedValueSurvivesNoDamage proves a card that defines
// the SVar name itself keeps its value when the ability returns before
// dealing -- no damage source left -- as Java falls back to the card's SVar;
// an ability that reaches GameAction.dealDamage overrides it, even with
// nothing to deal (CardDamageTable.java:153-155).
func TestExcessSVarCardDefinedValueSurvivesNoDamage(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		line string
		want int
	}{
		{"DB$ DealDamage | Defined$ Opponent | DamageSource$ Remembered | NumDmg$ 3 | ExcessSVar$ Excess | SubAbility$ DBGain", 22},
		{"DB$ DealDamage | Defined$ Remembered | NumDmg$ 3 | ExcessSVar$ Excess | SubAbility$ DBGain", 20},
	} {
		g, p, _ := newTwoPlayerGame(t)
		resolveLine(t, g, p, engine.NewScriptedController(), tc.line, gainExcess[0], gainExcess[1], "Excess", "Number$2")
		if got := g.Player(p).Life; got != tc.want {
			t.Errorf("%q: life = %d, want %d", tc.line, got, tc.want)
		}
	}
}

// TestExcessSVarConditionTargetedBy is Violent Echoes' ExcessSVarCondition$
// Card.targetedBy: the targeted permanent's excess counts.
func TestExcessSVarConditionTargetedBy(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	resolveLine(t, g, p, targeting(victim),
		"DB$ DealDamage | ValidTgts$ Creature,Planeswalker | NumDmg$ 6 | ExcessSVar$ Excess | ExcessSVarCondition$ Card.targetedBy | SubAbility$ DBGain",
		gainExcess...)
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life = %d, want 24", got)
	}
}

// TestExcessSVarTargetedByReadsTheWholeChain proves targetedBy is the root
// ability's isTargeting over the whole chain (SpellAbility.java:2112-2118): a
// Radiance$ creature the DealDamage itself did not target counts once a later
// link of the chain targets it.
func TestExcessSVarTargetedByReadsTheWholeChain(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	targeted := g.NewCard(coloredCreatureDef(t, "R", "2", "2"), other, engine.Battlefield)
	radiant := g.NewCard(coloredCreatureDef(t, "R", "1", "1"), other, engine.Battlefield)
	g.NewCard(coloredCreatureDef(t, "R", "1", "1"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(targeted)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(radiant)})
	resolveLine(t, g, p, c,
		"DB$ DealDamage | ValidTgts$ Creature | Radiance$ True | NumDmg$ 6 | ExcessSVar$ Excess | ExcessSVarCondition$ Card.targetedBy | SubAbility$ DBMark",
		"DBMark", "DB$ Pump | ValidTgts$ Creature | SubAbility$ DBGain", gainExcess[0], gainExcess[1])
	// 4 from the target, 5 from the radiant creature the link targets; the
	// third creature's 5 is excess no ability of the chain targeted.
	if got := g.Player(p).Life; got != 29 {
		t.Errorf("life = %d, want 29", got)
	}
}

// TestExcessSVarConditionMatchesTheDamagedCard proves the condition reads the
// damaged permanent, not the source (CardDamageTable.java:137): a Planeswalker
// condition counts none of a creature's excess.
func TestExcessSVarConditionMatchesTheDamagedCard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	resolveLine(t, g, p, targeting(victim),
		"DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 6 | ExcessSVar$ Excess | ExcessSVarCondition$ Planeswalker | SubAbility$ DBGain",
		gainExcess...)
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("life = %d, want 20", got)
	}
}

// TestRhinosRampageCountsOnlyTheOpponentsCreature is the Fight shape:
// Creature.targetedBy+OppCtrl -- both fighters are targeted by the chain, the
// controller's own is excluded by OppCtrl. The root's +1/+0 is on the 5/1
// when the fight reads its power: the layers are rebuilt before each
// sub-ability (AbilityUtils.java:1366-1367), so it deals 6, not 5.
func TestRhinosRampageCountsOnlyTheOpponentsCreature(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(creatureDefPT(t, "5", "1"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "4", "2"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
	resolveLine(t, g, p, c, "DB$ Pump | ValidTgts$ Creature.YouCtrl | NumAtt$ +1 | SubAbility$ DBFight",
		"DBFight", "DB$ Fight | Defined$ ParentTarget | ValidTgts$ Creature.OppCtrl | ExcessSVar$ Excess | ExcessSVarCondition$ Creature.targetedBy+OppCtrl | SubAbility$ DBGain",
		gainExcess[0], gainExcess[1])
	// 6 damage to the 4/2 is 4 excess (unpumped, 5 would be 3); 4 to the 6/1
	// (3 excess) is not counted.
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life = %d, want 24", got)
	}
}

// TestExcessSVarFightWithoutTwoFightersReadsZero proves a fight that never
// happens leaves the SVar at 0.
func TestExcessSVarFightWithoutTwoFightersReadsZero(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets(nil)
	resolveLine(t, g, p, c,
		"DB$ Fight | Defined$ Self | ValidTgts$ Creature.OppCtrl | TargetMin$ 0 | TargetMax$ 1 | ExcessSVar$ Excess | SubAbility$ DBGain",
		gainExcess...)
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("life = %d, want 20", got)
	}
}

// TestExcessSVarRefusedShapes proves the ExcessSVar$ shapes this port does
// not evaluate fail by name.
func TestExcessSVarRefusedShapes(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)},
		"DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 6 | ExcessSVar$ Excess | ExcessSVarCondition$ Card.!targetedBy")
	if err == nil || !strings.Contains(err.Error(), "targetedBy") {
		t.Errorf("!targetedBy: err = %v, want one naming it", err)
	}
	if d := g.Card(victim).Damage.Marked; d != 0 {
		t.Errorf("!targetedBy: victim damage = %d, want 0 (refused before acting)", d)
	}

	g, p, other = newTwoPlayerGame(t)
	mine := g.NewCard(creatureDefPT(t, "5", "5"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
	_, err = castETBChain(t, g, p, etbChainDef(t, "Test Rampage", "DB$ Pump | ValidTgts$ Creature.YouCtrl | SubAbility$ DBFight",
		"DBFight", "DB$ Fight | Defined$ ParentTarget | ValidTgts$ Creature.OppCtrl | ExcessSVar$ Excess | ExcessSVarCondition$ Creature.!targetedBy"), c)
	if err == nil || !strings.Contains(err.Error(), "Fight") || !strings.Contains(err.Error(), "targetedBy") {
		t.Errorf("Fight !targetedBy: err = %v, want one naming Fight and targetedBy", err)
	}
	if z := g.Card(theirs).Zone; z != engine.Battlefield {
		t.Errorf("Fight !targetedBy: target zone = %v, want Battlefield (refused before acting)", z)
	}
}
