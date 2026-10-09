package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// charmWithLinkedMode is an instant Charm whose first mode pumps a creature
// its caster controls and chains a sub-ability that destroys a creature an
// opponent controls -- Inscription of Abundance's fight mode in shape (the
// link names its own ValidTgts$) -- and whose second mode gains 2 life.
func charmWithLinkedMode(t *testing.T) *compile.Card {
	t.Helper()
	return spellDefWith(t, "Linked Charm", "Instant", "W", "SP$ Charm | Choices$ DBPump,DBGain",
		"DBPump", "DB$ Pump | ValidTgts$ Creature.YouCtrl | NumAtt$ +1 | SubAbility$ DBKill",
		"DBKill", "DB$ Destroy | ValidTgts$ Creature.OppCtrl",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 2")
}

// TestCharmModeLinkChoosesItsOwnTarget proves CharmEffect.chainAbilities plus
// setupTargets: a chosen mode's SubAbility$ naming its own ValidTgts$ asks
// for its own target, after the mode's, and resolves against it rather than
// the mode's (the pumped creature survives, the opponent's is destroyed).
func TestCharmModeLinkChoosesItsOwnTarget(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	charm := g.NewCard(charmWithLinkedMode(t), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueModeChoice([]int{0})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, charm, c) {
		t.Fatal("CastSpell failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if g.Card(mine).Zone != engine.Battlefield {
		t.Errorf("the pumped creature is in %v, want the battlefield: the link destroyed the mode's target", g.Card(mine).Zone)
	}
	if pw, _ := g.Card(mine).Power(); pw != 3 {
		t.Errorf("pumped power = %d, want 3", pw)
	}
	if g.Card(theirs).Zone != engine.Graveyard {
		t.Errorf("the link's target is in %v, want the graveyard", g.Card(theirs).Zone)
	}
}

// TestCharmModeWithNoLegalLinkTargetIsNotCast proves CR 601.2c over a
// mode's link: with no creature an opponent controls, the chosen mode cannot
// be put on the stack, and nothing is paid.
func TestCharmModeWithNoLegalLinkTargetIsNotCast(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	charm := g.NewCard(charmWithLinkedMode(t), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueModeChoice([]int{0})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	g.Player(p).ManaPool.Add(mana.White, 1)
	if g.CastSpell(p, charm, c) {
		t.Fatal("CastSpell succeeded with no legal target for the mode's link")
	}
	if g.Card(charm).Zone != engine.Hand || g.Player(p).ManaPool.Total() != 1 {
		t.Errorf("charm in %v, pool %d: want it in hand, nothing paid", g.Card(charm).Zone, g.Player(p).ManaPool.Total())
	}
}

// TestCharmModeLinkTargetIsRecheckedAtResolution proves CR 608.2b reaches a
// mode's link (MagicStack.hasFizzled recurses into the chained modes and
// their subs): a link target that left is dropped while the mode's own legal
// target keeps the spell resolving, and a spell whose only targets all left
// fizzles.
func TestCharmModeLinkTargetIsRecheckedAtResolution(t *testing.T) {
	t.Parallel()
	for _, bothGone := range []bool{false, true} {
		g, p, other := newTwoPlayerGame(t)
		mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		charm := g.NewCard(charmWithLinkedMode(t), p, engine.Hand)
		c := engine.NewScriptedController()
		c.QueueModeChoice([]int{0})
		c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
		c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
		g.Player(p).ManaPool.Add(mana.White, 1)
		if !g.CastSpell(p, charm, c) {
			t.Fatal("CastSpell failed")
		}
		g.Move(theirs, engine.Exile, other)
		if bothGone {
			g.Move(mine, engine.Exile, p)
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatal(err)
		}
		if g.Card(theirs).Zone != engine.Exile {
			t.Errorf("bothGone=%v: the gone link target moved to %v", bothGone, g.Card(theirs).Zone)
		}
		if !bothGone {
			if pw, _ := g.Card(mine).Power(); pw != 3 {
				t.Errorf("pumped power = %d, want 3: the mode still resolves", pw)
			}
		}
		if g.Card(charm).Zone != engine.Graveyard {
			t.Errorf("bothGone=%v: charm in %v, want the graveyard", bothGone, g.Card(charm).Zone)
		}
	}
}

// TestCharmModeLinkTargetBecomesTarget proves getAllTargetChoices
// (MagicStack.java:410) covers a mode's link: the link's target becomes the
// target of the spell, firing its BecomesTarget trigger.
func TestCharmModeLinkTargetBecomesTarget(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	watcher := g.NewCard(becomesTargetCreatureDef(t, "Target Watcher", "Card.Self", ""), other, engine.Battlefield)
	charm := g.NewCard(charmWithLinkedMode(t), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueModeChoice([]int{0})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(watcher)})
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, charm, c) {
		t.Fatal("CastSpell failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(other).Life; got != 25 {
		t.Errorf("watcher's controller life = %d, want 25: the link's target became a target", got)
	}
}

// TestCopiedCharmKeepsItsModeLinkTargets proves CR 707.10 over a mode's link:
// a copy of the Charm keeps the original's link target (MayChooseTarget$
// declined), or the copier picks a new one for the link as well as the mode
// (setupNewTargets walks the whole chain, SpellAbility.java:2120-2139).
func TestCopiedCharmKeepsItsModeLinkTargets(t *testing.T) {
	t.Parallel()
	for _, retarget := range []bool{false, true} {
		g, p, other := newTwoPlayerGame(t)
		mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		first := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		second := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		charm := g.NewCard(charmWithLinkedMode(t), p, engine.Hand)
		c := engine.NewScriptedController()
		c.QueueModeChoice([]int{0})
		c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
		c.QueueTargets([]engine.EntityID{engine.CardEntity(first)})
		// The mode's own target: kept either way.
		c.QueueConfirmEffect(false)
		// The link's target.
		c.QueueConfirmEffect(retarget)
		if retarget {
			c.QueueTargets([]engine.EntityID{engine.CardEntity(second)})
		}
		if err := castWithCopyWatcher(t, g, p, charm, c, "Instant", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | MayChooseTarget$ True"); err != nil {
			t.Fatal(err)
		}
		if g.Card(first).Zone != engine.Graveyard {
			t.Errorf("retarget=%v: the original's link target is in %v, want the graveyard", retarget, g.Card(first).Zone)
		}
		if gone := g.Card(second).Zone == engine.Graveyard; gone != retarget {
			t.Errorf("retarget=%v: the other creature destroyed = %v", retarget, gone)
		}
		if pw, _ := g.Card(mine).Power(); pw != 4 {
			t.Errorf("retarget=%v: power = %d, want 4: both the copy and the original pump it", retarget, pw)
		}
	}
}
