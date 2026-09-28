package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// jarkeldLine and sorrowsPathLine are the corpus's two SwitchBlock lines,
// verbatim from general_jarkeld.txt and sorrows_path.txt (descriptions
// trimmed).
const (
	jarkeldLine = "AB$ SwitchBlock | Cost$ T | ActivationPhases$ Declare Blockers | TargetMin$ 2 | TargetMax$ 2 | " +
		"ValidTgts$ Creature.attacking+blocked | DefinedAttacker$ Targeted | " +
		"DefinedBlocker$ Valid Creature.blockingTargeted | TgtPrompt$ Choose two target blocked attacking creatures"
	sorrowsPathLine = "AB$ SwitchBlock | Cost$ T | TargetMin$ 2 | TargetMax$ 2 | ValidTgts$ Creature.blocking+OppCtrl | " +
		"TargetsWithSameController$ True | DefinedAttacker$ Valid Creature.blockedByValidThisTurn Targeted | " +
		"DefinedBlocker$ Targeted | RemoveFromCombat$ True | TgtPrompt$ Choose two target blocking creatures an opponent controls"
)

// combatFixture seats p (attacking) and other (blocking) in p's Main1.
type combatFixture struct {
	g        *engine.Game
	p, other engine.PlayerID
}

func newCombatFixture(t *testing.T) combatFixture {
	t.Helper()
	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	return combatFixture{g: g, p: p, other: other}
}

// creature puts a creature built from def onto pid's battlefield.
func (f combatFixture) creature(pid engine.PlayerID, def *compile.Card) engine.CardID {
	return f.g.NewCard(def, pid, engine.Battlefield)
}

// fight declares attackers and blocks, then moves to the declare blockers
// step, where both real lines activate.
func (f combatFixture) fight(t *testing.T, attackers []engine.CardID, blocks []engine.Block) {
	t.Helper()
	ac := engine.NewScriptedController()
	ac.QueueAttackers(attackers)
	declareAttackers(t, f.g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks(blocks)
	declareBlockers(t, f.g, bc)
	f.g.SetTurnState(1, f.p, engine.DeclareBlockers)
}

// activate activates switcher's ability for pid with targets and resolves
// the stack, returning ResolveStack's error.
func (f combatFixture) activate(t *testing.T, pid engine.PlayerID, switcher engine.CardID, targets ...engine.CardID) error {
	t.Helper()
	c := engine.NewScriptedController()
	var tgts []engine.EntityID
	for _, id := range targets {
		tgts = append(tgts, engine.CardEntity(id))
	}
	c.QueueTargets(tgts)
	if !f.g.ActivateAbility(pid, switcher, 0, c) {
		t.Fatal("ActivateAbility returned false")
	}
	return f.g.ResolveStack(engine.NewRegistry(), c)
}

// wantBlocks compares the current blocks, in order.
func (f combatFixture) wantBlocks(t *testing.T, want ...engine.Block) {
	t.Helper()
	if got := f.g.Blocks(); !equalBlocks(got, want) {
		t.Errorf("Blocks() = %v, want %v", got, want)
	}
}

// TestSwitchBlockJarkeldTradesBlockers proves General Jarkeld's branch
// (SwitchBlockEffect.java:63-115): two target blocked attackers trade
// blockers, each blocker rejoining the other attacker, in battlefield scan
// order.
func TestSwitchBlockJarkeldTradesBlockers(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	jarkeld := f.creature(f.p, creatureDefWithAbility(t, "General Jarkeld", jarkeldLine))
	b1 := f.creature(f.other, creatureDefPT(t, "1", "1"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})

	if err := f.activate(t, f.p, jarkeld, a1, a2); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	f.wantBlocks(t, engine.Block{Blocker: b1, Attacker: a2}, engine.Block{Blocker: b2, Attacker: a1})
}

// TestSwitchBlockJarkeldMovesGangBlock proves every blocker of one attacker
// moves together: a1's two blockers end on a2, a2's one on a1.
func TestSwitchBlockJarkeldMovesGangBlock(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	jarkeld := f.creature(f.p, creatureDefWithAbility(t, "General Jarkeld", jarkeldLine))
	b1 := f.creature(f.other, creatureDefPT(t, "1", "1"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	b3 := f.creature(f.other, creatureDefPT(t, "1", "1"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{
		{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}, {Blocker: b3, Attacker: a1},
	})

	if err := f.activate(t, f.p, jarkeld, a1, a2); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	f.wantBlocks(t,
		engine.Block{Blocker: b1, Attacker: a2}, engine.Block{Blocker: b2, Attacker: a1}, engine.Block{Blocker: b3, Attacker: a2})
}

// TestSwitchBlockJarkeldIllegalBlockChangesNothing proves the "could be
// blocked by all creatures that the other is blocked by" condition: a2
// has flying and a1's blocker has neither flying nor reach
// (CombatUtil.canBlock -- CanBlock), so nothing switches.
func TestSwitchBlockJarkeldIllegalBlockChangesNothing(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPTKeywords(t, "2", "2", "Flying"))
	jarkeld := f.creature(f.p, creatureDefWithAbility(t, "General Jarkeld", jarkeldLine))
	b1 := f.creature(f.other, creatureDefPT(t, "1", "1"))
	b2 := f.creature(f.other, creatureDefPTKeywords(t, "1", "1", "Reach"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})

	if err := f.activate(t, f.p, jarkeld, a1, a2); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	f.wantBlocks(t, engine.Block{Blocker: b1, Attacker: a1}, engine.Block{Blocker: b2, Attacker: a2})
}

// TestSwitchBlockJarkeldOneTargetLeftChangesNothing proves "if targeting
// attackers but only one remains, this fizzles" (SwitchBlockEffect.java:
// 64-65): a2 leaves the battlefield before resolution, its target is
// dropped, and a1 keeps its blocker.
func TestSwitchBlockJarkeldOneTargetLeftChangesNothing(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	jarkeld := f.creature(f.p, creatureDefWithAbility(t, "General Jarkeld", jarkeldLine))
	b1 := f.creature(f.other, creatureDefPT(t, "1", "1"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a1), engine.CardEntity(a2)})
	if !f.g.ActivateAbility(f.p, jarkeld, 0, c) {
		t.Fatal("ActivateAbility returned false")
	}
	f.g.Move(a2, engine.Graveyard, f.p)
	if err := f.g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !containsBlock(f.g.Blocks(), engine.Block{Blocker: b1, Attacker: a1}) {
		t.Errorf("Blocks() = %v, want b1 still blocking a1", f.g.Blocks())
	}
	if containsBlock(f.g.Blocks(), engine.Block{Blocker: b2, Attacker: a1}) {
		t.Errorf("Blocks() = %v, b2 moved onto a1 with one target left", f.g.Blocks())
	}
}

// TestSwitchBlockJarkeldOutsideDeclareBlockersDeclines proves
// ActivationPhases$ Declare Blockers holds: in the combat damage step the
// ability cannot be activated.
func TestSwitchBlockJarkeldOutsideDeclareBlockersDeclines(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	jarkeld := f.creature(f.p, creatureDefWithAbility(t, "General Jarkeld", jarkeldLine))
	b1 := f.creature(f.other, creatureDefPT(t, "1", "1"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})
	f.g.SetTurnState(1, f.p, engine.CombatDamage)

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a1), engine.CardEntity(a2)})
	if f.g.ActivateAbility(f.p, jarkeld, 0, c) {
		t.Error("ActivateAbility succeeded in the combat damage step")
	}
}

// TestSwitchBlockJarkeldStrayBlockFailsClosed proves the PORT-8 guard: a
// blocker also blocking a third attacker would lose that block to Java's
// Combat.removeFromCombat (SwitchBlockEffect.java:88), which the card text
// does not do, so the line is an error and nothing moves.
func TestSwitchBlockJarkeldStrayBlockFailsClosed(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	x := f.creature(f.p, creatureDefPT(t, "1", "1"))
	jarkeld := f.creature(f.p, creatureDefWithAbility(t, "General Jarkeld", jarkeldLine))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	f.fight(t, []engine.CardID{a1, a2, x}, []engine.Block{{Blocker: b2, Attacker: a2}})
	// A Block effect makes a fresh creature of other's block both a1 and x.
	multi, err := resolveNow(t, f.g, f.other, engine.NewScriptedController(),
		[]engine.EntityID{engine.CardEntity(a1), engine.CardEntity(x)},
		"DB$ Block | DefinedAttacker$ Targeted | DefinedBlocker$ Self")
	if err != nil {
		t.Fatalf("Block: %v", err)
	}
	before := append([]engine.Block(nil), f.g.Blocks()...)

	err = f.activate(t, f.p, jarkeld, a1, a2)
	if err == nil || !strings.Contains(err.Error(), "SwitchBlockEffect.java:88") {
		t.Fatalf("ResolveStack error = %v, want the stray-block error", err)
	}
	if !equalBlocks(f.g.Blocks(), before) {
		t.Errorf("Blocks() = %v, want unchanged %v (multi-blocker %d)", f.g.Blocks(), before, multi)
	}
}

// TestSwitchBlockSorrowsPathTradesAttackers proves Sorrow's Path's branch
// (SwitchBlockEffect.java:116-164): two target blockers trade the attackers
// they block, found through the blocked-by history.
func TestSwitchBlockSorrowsPathTradesAttackers(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	path := f.creature(f.p, creatureDefWithAbility(t, "Sorrow's Path", sorrowsPathLine))
	b1 := f.creature(f.other, creatureDefPT(t, "1", "1"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})

	if err := f.activate(t, f.p, path, b1, b2); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	f.wantBlocks(t, engine.Block{Blocker: b2, Attacker: a1}, engine.Block{Blocker: b1, Attacker: a2})
}

// sorrowsPathDef is sorrows_path.txt as printed: a Land with the switch
// ability and its "whenever this becomes tapped" damage trigger.
func sorrowsPathDef(t *testing.T) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: "sorrows_path.txt"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Sorrow's Path"
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Land")
	raw.Faces[0].Abilities = []string{sorrowsPathLine}
	raw.Faces[0].Triggers = []string{"Mode$ Taps | ValidCard$ Card.Self | TriggerZones$ Battlefield | Execute$ TrigDamage"}
	raw.Faces[0].SVars.Set("TrigDamage", "DB$ DamageAll | ValidCards$ Creature.YouCtrl | ValidPlayers$ You | NumDmg$ 2")
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile Sorrow's Path: %v", err)
	}
	return c
}

// TestSwitchBlockSorrowsPathRealCard proves the printed card: a Land that
// entered this turn still pays its {T} (Card.isSick needs a creature,
// Card.java:3651), the switch happens, and its Taps trigger deals 2 to its
// controller and each creature they control. It also pins this port's
// stack order: checkTapsTriggers pushes the Taps trigger while the cost is
// paid, so it sits under the switch and resolves after it. Java (CR 603.3)
// puts it on top, so it resolves first -- ActivateAbility's general gap,
// effects-switchblock.md.
func TestSwitchBlockSorrowsPathRealCard(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	a1 := f.creature(f.p, creatureDefPT(t, "3", "3"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	path := f.creature(f.p, sorrowsPathDef(t))
	f.g.Card(path).SummonSick = true
	b1 := f.creature(f.other, creatureDefPT(t, "1", "1"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(b1), engine.CardEntity(b2)})
	if !f.g.ActivateAbility(f.p, path, 0, c) {
		t.Fatal("ActivateAbility returned false for a Land that entered this turn")
	}
	if top, ok := f.g.StackTop(); !ok || top.API != engine.APISwitchBlock || f.g.StackLen() != 2 {
		t.Fatalf("stack len %d, top %v, want the switch above the Taps trigger", f.g.StackLen(), top.API)
	}
	if err := f.g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	f.wantBlocks(t, engine.Block{Blocker: b2, Attacker: a1}, engine.Block{Blocker: b1, Attacker: a2})
	if life := f.g.Player(f.p).Life; life != 18 {
		t.Errorf("controller life = %d, want 18 (Taps trigger)", life)
	}
}

// TestSwitchBlockSorrowsPathReblockFiresBlocks proves RemoveFromCombat$'s
// re-block fires Mode$ Blocks again (SwitchBlockEffect.runTriggers): the
// watching blocker's controller draws once at declaration and once more on
// the switch.
func TestSwitchBlockSorrowsPathReblockFiresBlocks(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	libraryCards(t, f.g, f.other, 3)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	path := f.creature(f.p, creatureDefWithAbility(t, "Sorrow's Path", sorrowsPathLine))
	b1 := f.creature(f.other, creatureDefWithAbilityAndTrigger(t, "Test Blocks Watcher",
		"AB$ Pump | Cost$ T | Defined$ Self | NumAtt$ 1", "Mode$ Blocks | ValidCard$ Card.Self | Execute$ TrigDraw"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})
	if err := f.g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack (declaration triggers): %v", err)
	}
	if n := len(f.g.Zone(engine.Hand, f.other).Cards()); n != 1 {
		t.Fatalf("setup: hand = %d, want 1 after the declared block", n)
	}

	if err := f.activate(t, f.p, path, b1, b2); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := len(f.g.Zone(engine.Hand, f.other).Cards()); n != 2 {
		t.Errorf("hand = %d, want 2 (the re-block fired Mode$ Blocks)", n)
	}
}

// TestSwitchBlockSorrowsPathValidBlockedTriggerFailsClosed proves the PORT-8
// guard on SwitchBlockEffect.java:22-25: Java's re-block passes the
// attacker under a key TriggerBlocks never reads, so a Blocks trigger naming
// ValidBlocked$ would diverge either way; the line is an error and nothing
// moves.
func TestSwitchBlockSorrowsPathValidBlockedTriggerFailsClosed(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	libraryCards(t, f.g, f.other, 3)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	path := f.creature(f.p, creatureDefWithAbility(t, "Sorrow's Path", sorrowsPathLine))
	b1 := f.creature(f.other, creatureDefWithAbilityAndTrigger(t, "Test Blocks Watcher",
		"AB$ Pump | Cost$ T | Defined$ Self | NumAtt$ 1",
		"Mode$ Blocks | ValidCard$ Card.Self | ValidBlocked$ Creature | Execute$ TrigDraw"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})
	if err := f.g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack (declaration triggers): %v", err)
	}

	err := f.activate(t, f.p, path, b1, b2)
	if err == nil || !strings.Contains(err.Error(), "SwitchBlockEffect.java:24") {
		t.Fatalf("ResolveStack error = %v, want the ValidBlocked$ error", err)
	}
	f.wantBlocks(t, engine.Block{Blocker: b1, Attacker: a1}, engine.Block{Blocker: b2, Attacker: a2})
}

// TestSwitchBlockValidBlockedGuardTestsTheNewAttackerNotValidCard proves
// checkReblockTriggerKeys evaluates ValidBlocked$ against the switch's new
// attacker (the key SwitchBlockEffect.java:24 actually nulls), not
// ValidCard$ against the blocker: ValidCard$ Card.Self trivially matches the
// trigger's own host regardless of the switch, so a naive guard keyed on it
// would wrongly error here even though ValidBlocked$'s own property never
// matches either attacker.
func TestSwitchBlockValidBlockedGuardTestsTheNewAttackerNotValidCard(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	libraryCards(t, f.g, f.other, 3)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	path := f.creature(f.p, creatureDefWithAbility(t, "Sorrow's Path", sorrowsPathLine))
	b1 := f.creature(f.other, creatureDefWithAbilityAndTrigger(t, "Test Blocks Watcher",
		"AB$ Pump | Cost$ T | Defined$ Self | NumAtt$ 1",
		"Mode$ Blocks | ValidCard$ Card.Self | ValidBlocked$ Creature.powerGE5 | Execute$ TrigDraw"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})
	if err := f.g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack (declaration triggers): %v", err)
	}

	if err := f.activate(t, f.p, path, b1, b2); err != nil {
		t.Fatalf("ResolveStack: %v, want no error: a1 and a2 both have power < 5, so ValidBlocked$ never matches", err)
	}
	f.wantBlocks(t, engine.Block{Blocker: b2, Attacker: a1}, engine.Block{Blocker: b1, Attacker: a2})
}

// TestSwitchBlockValidBlockedGuardAppliesWithoutValidCard proves the guard
// checks ValidBlocked$ on its own: a Blocks trigger with no ValidCard$ at
// all still reads AbilityKey.Attackers in Java (TriggerBlocks.java:58 only
// tests ValidCard$ when the param is present), so it is just as subject to
// SwitchBlockEffect.java:24's bug as one that also carries ValidCard$.
func TestSwitchBlockValidBlockedGuardAppliesWithoutValidCard(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	libraryCards(t, f.g, f.other, 3)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	path := f.creature(f.p, creatureDefWithAbility(t, "Sorrow's Path", sorrowsPathLine))
	b1 := f.creature(f.other, creatureDefWithAbilityAndTrigger(t, "Test Blocks Watcher",
		"AB$ Pump | Cost$ T | Defined$ Self | NumAtt$ 1",
		"Mode$ Blocks | ValidBlocked$ Creature | Execute$ TrigDraw"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})
	if err := f.g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack (declaration triggers): %v", err)
	}

	err := f.activate(t, f.p, path, b1, b2)
	if err == nil || !strings.Contains(err.Error(), "SwitchBlockEffect.java:24") {
		t.Fatalf("ResolveStack error = %v, want the ValidBlocked$ error even without ValidCard$", err)
	}
	f.wantBlocks(t, engine.Block{Blocker: b1, Attacker: a1}, engine.Block{Blocker: b2, Attacker: a2})
}

// TestSwitchBlockSorrowsPathIllegalBlockChangesNothing proves the "could
// block all creatures that the other is blocking" condition: a1 has flying
// and b2 cannot block it.
func TestSwitchBlockSorrowsPathIllegalBlockChangesNothing(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	a1 := f.creature(f.p, creatureDefPTKeywords(t, "2", "2", "Flying"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	path := f.creature(f.p, creatureDefWithAbility(t, "Sorrow's Path", sorrowsPathLine))
	b1 := f.creature(f.other, creatureDefPTKeywords(t, "1", "1", "Reach"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})

	if err := f.activate(t, f.p, path, b1, b2); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	f.wantBlocks(t, engine.Block{Blocker: b1, Attacker: a1}, engine.Block{Blocker: b2, Attacker: a2})
}

// TestSwitchBlockSorrowsPathOneBlockerLeftChangesNothing proves "if
// targeting blockers but only one remains, this fizzles"
// (SwitchBlockEffect.java:117-118).
func TestSwitchBlockSorrowsPathOneBlockerLeftChangesNothing(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	path := f.creature(f.p, creatureDefWithAbility(t, "Sorrow's Path", sorrowsPathLine))
	b1 := f.creature(f.other, creatureDefPT(t, "1", "1"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	f.fight(t, []engine.CardID{a1, a2}, []engine.Block{{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}})

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(b1), engine.CardEntity(b2)})
	if !f.g.ActivateAbility(f.p, path, 0, c) {
		t.Fatal("ActivateAbility returned false")
	}
	f.g.Move(b2, engine.Graveyard, f.other)
	if err := f.g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !containsBlock(f.g.Blocks(), engine.Block{Blocker: b1, Attacker: a1}) {
		t.Errorf("Blocks() = %v, want b1 still blocking a1", f.g.Blocks())
	}
}

// TestSwitchBlockSorrowsPathSecondSwitchFailsClosed proves the PORT-8 guard
// on the blocked-by history: after a first switch, b1 blocks a2, which no
// blocked-by record names (SwitchBlockEffect.java never records one). A
// second activation pairing b1 with b3 would, in Java, strip b1 from a2
// and leave a2 without b3; the line is an error and nothing moves.
func TestSwitchBlockSorrowsPathSecondSwitchFailsClosed(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	a2 := f.creature(f.p, creatureDefPT(t, "4", "4"))
	a3 := f.creature(f.p, creatureDefPT(t, "3", "3"))
	path := f.creature(f.p, creatureDefWithAbility(t, "Sorrow's Path", sorrowsPathLine))
	path2 := f.creature(f.p, creatureDefWithAbility(t, "Sorrow's Path", sorrowsPathLine))
	b1 := f.creature(f.other, creatureDefPT(t, "1", "1"))
	b2 := f.creature(f.other, creatureDefPT(t, "3", "3"))
	b3 := f.creature(f.other, creatureDefPT(t, "2", "2"))
	f.fight(t, []engine.CardID{a1, a2, a3}, []engine.Block{
		{Blocker: b1, Attacker: a1}, {Blocker: b2, Attacker: a2}, {Blocker: b3, Attacker: a3},
	})
	if err := f.activate(t, f.p, path, b1, b2); err != nil {
		t.Fatalf("first switch: %v", err)
	}
	before := append([]engine.Block(nil), f.g.Blocks()...)

	err := f.activate(t, f.p, path2, b1, b3)
	if err == nil || !strings.Contains(err.Error(), "blocked-by record") {
		t.Fatalf("second switch error = %v, want the blocked-by history error", err)
	}
	if !equalBlocks(f.g.Blocks(), before) {
		t.Errorf("Blocks() = %v, want unchanged %v", f.g.Blocks(), before)
	}
}

// TestSwitchBlockRejectsUnknownDefined proves a Defined spelling outside the
// two corpus lines is an error, not a guess (PORT-8, GO-7).
func TestSwitchBlockRejectsUnknownDefined(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	a1 := f.creature(f.p, creatureDefPT(t, "2", "2"))
	b1 := f.creature(f.other, creatureDefPT(t, "1", "1"))
	f.fight(t, []engine.CardID{a1}, []engine.Block{{Blocker: b1, Attacker: a1}})

	_, err := resolveNow(t, f.g, f.p, engine.NewScriptedController(), nil,
		"DB$ SwitchBlock | DefinedAttacker$ Valid Creature.attacking | DefinedBlocker$ Targeted")
	if err == nil || !strings.Contains(err.Error(), "not resolvable yet") {
		t.Fatalf("error = %v, want not resolvable yet", err)
	}
	_, err = resolveNow(t, f.g, f.p, engine.NewScriptedController(), nil,
		"DB$ SwitchBlock | DefinedAttacker$ Targeted | DefinedBlocker$ Remembered")
	if err == nil || !strings.Contains(err.Error(), "not resolvable yet") {
		t.Fatalf("error = %v, want not resolvable yet", err)
	}
}

// TestSwitchBlockRejectsCondition proves Condition$ is rejected loudly.
func TestSwitchBlockRejectsCondition(t *testing.T) {
	t.Parallel()

	f := newCombatFixture(t)
	_, err := resolveNow(t, f.g, f.p, engine.NewScriptedController(), nil,
		"DB$ SwitchBlock | DefinedAttacker$ Targeted | DefinedBlocker$ Targeted | Condition$ Kicked")
	if err == nil || !strings.Contains(err.Error(), "Condition$") {
		t.Fatalf("error = %v, want Condition$ rejected", err)
	}
}

// equalBlocks reports whether x and y hold the same blocks in the same
// order.
func equalBlocks(x, y []engine.Block) bool {
	if len(x) != len(y) {
		return false
	}
	for i := range x {
		if x[i] != y[i] {
			return false
		}
	}
	return true
}

// containsBlock reports whether blocks holds pair.
func containsBlock(blocks []engine.Block, pair engine.Block) bool {
	for _, b := range blocks {
		if b == pair {
			return true
		}
	}
	return false
}
