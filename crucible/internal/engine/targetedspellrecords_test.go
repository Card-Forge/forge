package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These tests cover the amounts that read a targeted spell after a Counter
// removed it: Java's target is the spell's SpellAbility, whose host stays the
// Card object that was on the stack -- its zone still Stack (Zone.java:
// 162-167), so getCMC adds the X paid (Card.java:7350-7353) and getCastSA
// still answers (Card.java:1630-1636) -- while Game.Move clears the live
// card's cast record.

// castOthersSpell casts def from other's hand at other's main phase, paying
// with the given pool and answers queued on c.
func castOthersSpell(t *testing.T, g *engine.Game, other engine.PlayerID, def *compile.Card, c *engine.ScriptedController, pool ...mana.Colors) engine.CardID {
	t.Helper()
	id := g.NewCard(def, other, engine.Hand)
	g.SetTurnState(1, other, engine.Main1)
	for _, col := range pool {
		g.Player(other).ManaPool.Add(col, 1)
	}
	if !g.CastSpell(other, id, c) {
		t.Fatalf("could not cast %s", def.Name)
	}
	return id
}

// counterWith casts the counterspell def as p at target and resolves the stack.
func counterWith(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, target engine.CardID, c *engine.ScriptedController, pool ...mana.Colors) error {
	t.Helper()
	id := g.NewCard(def, p, engine.Hand)
	for _, col := range pool {
		g.Player(p).ManaPool.Add(col, 1)
	}
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
	if !g.CastSpell(p, id, c) {
		t.Fatalf("could not cast %s at the spell", def.Name)
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

// Sound the Trumpets reads SpellTargeted$CardManaCostLKI after the counter:
// an {X} creature cast with X=3 had mana value 3 on the stack, more than 2,
// so no recruit -- its printed mana value 0 would have recruited.
func TestSpellTargetedManaValueCountsTheXPaid(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	libraryCards(t, g, p, 3)
	oc := engine.NewScriptedController()
	oc.QueuePayX(3)
	queueXPayGeneric(oc, mana.ShardG, 3)
	endless := castOthersSpell(t, g, other, corpusCard(t, "Endless One"), oc, mana.Green, mana.Green, mana.Green)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardU)
	if err := counterWith(t, g, p, corpusCard(t, "Sound the Trumpets"), endless, c, mana.Blue, mana.Blue, mana.Blue); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(endless).Zone; got != engine.Graveyard {
		t.Fatalf("Endless One is in %v, want the graveyard", got)
	}
	if got := g.Zone(engine.Library, p).Len(); got != 3 {
		t.Errorf("library holds %d cards, want 3: a mana value 3 spell recruits nothing", got)
	}
}

// A mana value 2 spell is 2 or less: Sound the Trumpets recruits.
func TestSpellTargetedManaValueAllowsTheRecruit(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	libraryCards(t, g, p, 3)
	oc := engine.NewScriptedController()
	oc.QueuePayGeneric(mana.ShardG)
	bears := castOthersSpell(t, g, other, corpusCard(t, "Grizzly Bears"), oc, mana.Green, mana.Green)
	held := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardU)
	c.QueueDiscardChoice([]engine.CardID{held})
	if err := counterWith(t, g, p, corpusCard(t, "Sound the Trumpets"), bears, c, mana.Blue, mana.Blue, mana.Blue); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Zone(engine.Library, p).Len(); got != 2 {
		t.Errorf("library holds %d cards, want 2: the recruit drew one", got)
	}
	if got := g.Card(held).Zone; got != engine.Graveyard {
		t.Errorf("the discarded card is in %v, want the graveyard", got)
	}
}

// Unravel compares Targeted$CastTotalManaSpent with Targeted$CardManaCost on
// the countered spell as it was cast: Grizzly Bears cast for {G} under a
// reduction spent 1, less than 2, so Unravel draws; cast for {1}{G} it does
// not. Reading the moved card's cleared record would draw both times.
func TestTargetedCastTotalManaSpentReadsTheCounteredSpell(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name    string
		reduced bool
		want    int
	}{
		{"full price", false, 3},
		{"reduced", true, 2},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			libraryCards(t, g, p, 3)
			oc := engine.NewScriptedController()
			pool := []mana.Colors{mana.Green}
			if tc.reduced {
				g.NewCard(copyTestDef(t, "Bear Discount", "Artifact", "", "",
					"S:Mode$ ReduceCost | ValidCard$ Creature | Type$ Spell | Activator$ You | Amount$ 1"), other, engine.Battlefield)
			} else {
				pool = append(pool, mana.Green)
				oc.QueuePayGeneric(mana.ShardG)
			}
			bears := castOthersSpell(t, g, other, corpusCard(t, "Grizzly Bears"), oc, pool...)
			c := engine.NewScriptedController()
			c.QueuePayGeneric(mana.ShardU)
			if err := counterWith(t, g, p, corpusCard(t, "Unravel"), bears, c, mana.Blue, mana.Blue, mana.Blue); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Zone(engine.Library, p).Len(); got != tc.want {
				t.Errorf("library holds %d cards, want %d", got, tc.want)
			}
		})
	}
}

// TargetedByTarget$ measures what the countered spell targeted
// (AbilityUtils.java:615-623): Shock's creature target counts once; no card
// is a commander (CardProperty.java:1983-1986), so Bane's Contingency's
// IsCommander filter reads 0 and it only counters.
func TestTargetedByTargetReadsTheCounteredSpellsTargets(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		measure  string
		wantLife int
	}{
		{"TargetedByTarget$Amount", 21},
		{"TargetedByTarget$Valid Creature.YouCtrl", 21},
		{"TargetedByTarget$Valid Card.IsCommander+YouCtrl+inZoneBattlefield", 20},
	} {
		g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
		bear := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
		oc := engine.NewScriptedController()
		oc.QueueTargets([]engine.EntityID{engine.CardEntity(bear)})
		shock := castOthersSpell(t, g, other, corpusCard(t, "Shock"), oc, mana.Red)
		counter := copyTestDef(t, "Test Counter", "Instant", "", "", "Cost:U",
			"A:SP$ Counter | TargetType$ Spell | ValidTgts$ Card | SubAbility$ DBGain",
			"SVar:DBGain:DB$ GainLife | Defined$ You | LifeAmount$ X", "SVar:X:"+tc.measure)
		if err := counterWith(t, g, p, counter, shock, engine.NewScriptedController(), mana.Blue); err != nil {
			t.Fatalf("%s: ResolveStack: %v", tc.measure, err)
		}
		if got := g.Player(p).Life; got != tc.wantLife {
			t.Errorf("%s: life = %d, want %d", tc.measure, got, tc.wantLife)
		}
		if got := g.Card(bear).Zone; got != engine.Battlefield {
			t.Errorf("%s: the Bears are in %v, want the battlefield (Shock was countered)", tc.measure, got)
		}
	}

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	libraryCards(t, g, p, 3)
	bear := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	oc := engine.NewScriptedController()
	oc.QueueTargets([]engine.EntityID{engine.CardEntity(bear)})
	shock := castOthersSpell(t, g, other, corpusCard(t, "Shock"), oc, mana.Red)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardU)
	if err := counterWith(t, g, p, corpusCard(t, "Bane's Contingency"), shock, c, mana.Blue, mana.Blue, mana.Blue); err != nil {
		t.Fatalf("Bane's Contingency: %v", err)
	}
	if got := g.Zone(engine.Library, p).Len(); got != 3 {
		t.Errorf("library holds %d cards, want 3: no commander, no scry or draw", got)
	}
}

// Targeted$ over an ability on the stack is its host card
// (SpellAbility.findTargetedCards' target-spells branch, SpellAbility.java:
// 1967-1974), and that ability's own targets are what TargetedByTarget$ reads.
func TestTargetedReadsATargetedAbilitysHost(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		measure  string
		wantLife int
	}{
		{"Targeted$Amount", 21},
		{"TargetedByTarget$Amount", 20},
		{"SpellTargeted$CardManaCost", 20},
	} {
		g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
		engineCard := g.NewCard(lifeEngine(t), other, engine.Battlefield)
		g.SetTurnState(1, other, engine.Main1)
		item := activateFor(t, g, other, engineCard, engine.NewScriptedController())
		counter := g.NewCard(copyTestDef(t, "Test Stifle", "Instant", "", "", "Cost:U",
			"A:SP$ Counter | TargetType$ Activated,Triggered | ValidTgts$ Card | SubAbility$ DBGain",
			"SVar:DBGain:DB$ GainLife | Defined$ You | LifeAmount$ X", "SVar:X:"+tc.measure), p, engine.Hand)
		g.Player(p).ManaPool.Add(mana.Blue, 1)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{item})
		if !g.CastSpell(p, counter, c) {
			t.Fatalf("%s: could not cast the counter at the ability", tc.measure)
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatalf("%s: ResolveStack: %v", tc.measure, err)
		}
		if got := g.Player(p).Life; got != tc.wantLife {
			t.Errorf("%s: life = %d, want %d", tc.measure, got, tc.wantLife)
		}
		if got := g.Player(other).Life; got != 20 {
			t.Errorf("%s: the countered ability's controller has %d life, want 20", tc.measure, got)
		}
	}
}

// A Branch result under a counterspell reads the same root targets
// (getAllTargetChoices starts at getRootAbility): the countered Bears' mana
// value 2.
func TestSpellTargetedFromABranchResult(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	oc := engine.NewScriptedController()
	oc.QueuePayGeneric(mana.ShardG)
	bears := castOthersSpell(t, g, other, corpusCard(t, "Grizzly Bears"), oc, mana.Green, mana.Green)
	counter := copyTestDef(t, "Test Counter", "Instant", "", "", "Cost:U",
		"A:SP$ Counter | TargetType$ Spell | ValidTgts$ Card | SubAbility$ DBBranch",
		"SVar:DBBranch:DB$ Branch | BranchConditionSVar$ X | BranchConditionSVarCompare$ GE2 | TrueSubAbility$ DBGain",
		"SVar:DBGain:DB$ GainLife | Defined$ You | LifeAmount$ X", "SVar:X:SpellTargeted$CardManaCost")
	if err := counterWith(t, g, p, counter, bears, engine.NewScriptedController(), mana.Blue); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22: the branch result read mana value 2", got)
	}
}

// SpellTargeted$ is 0 when the chain targets no spell, and a measure it
// cannot take on a card is an error, not 0 (GO-7).
func TestSpellTargetedWithoutASpellAndUnknownMeasures(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	if got := measure(t, g, p, "SpellTargeted$CardManaCostLKI"); got != 0 {
		t.Errorf("SpellTargeted$ with no spell targeted = %d, want 0", got)
	}
	if err := measureErr(t, g, p, "SpellTargeted$Bogus"); err == nil {
		t.Error("SpellTargeted$Bogus: no error, want unresolved")
	}

	oc := engine.NewScriptedController()
	oc.QueuePayGeneric(mana.ShardG)
	bears := castOthersSpell(t, g, other, corpusCard(t, "Grizzly Bears"), oc, mana.Green, mana.Green)
	counter := copyTestDef(t, "Test Counter", "Instant", "", "", "Cost:U",
		"A:SP$ Counter | TargetType$ Spell | ValidTgts$ Card | SubAbility$ DBGain",
		"SVar:DBGain:DB$ GainLife | Defined$ You | LifeAmount$ X", "SVar:X:SpellTargeted$CardManaCost/Plus.1")
	if err := counterWith(t, g, p, counter, bears, engine.NewScriptedController(), mana.Blue); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23: the Bears' mana value 2, plus 1", got)
	}

	g2, p2, other2 := newTwoPlayerGameOn(t, scenarioDB(t))
	oc2 := engine.NewScriptedController()
	oc2.QueuePayGeneric(mana.ShardG)
	bears2 := castOthersSpell(t, g2, other2, corpusCard(t, "Grizzly Bears"), oc2, mana.Green, mana.Green)
	bad := copyTestDef(t, "Test Counter", "Instant", "", "", "Cost:U",
		"A:SP$ Counter | TargetType$ Spell | ValidTgts$ Card | SubAbility$ DBGain",
		"SVar:DBGain:DB$ GainLife | Defined$ You | LifeAmount$ X", "SVar:X:SpellTargeted$Bogus")
	err := counterWith(t, g2, p2, bad, bears2, engine.NewScriptedController(), mana.Blue)
	if err == nil || !strings.Contains(err.Error(), "LifeAmount") {
		t.Errorf("SpellTargeted$Bogus on a countered spell: err = %v, want the LifeAmount$ refusal", err)
	}
}
