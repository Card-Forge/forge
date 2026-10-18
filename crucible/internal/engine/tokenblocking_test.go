package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TokenBlocking$ is SpellAbilityEffect.addToCombat's blocking half
// (SpellAbilityEffect.java:795-829), run per token by Token and CopyPermanent
// (TokenEffectBase.java:188): the token blocks the first card the param names
// when its controller defends against that attacker, with no legality check,
// and runs AttackerBlockedByCreature, AttackerBlockedOnce (that attacker only)
// and -- for an attacker not blocked before -- AttackerBlocked.

// blockCounters puts three watchers on p's battlefield, one per mode
// addToCombatBlocking runs, each gaining p a different amount: 1 per
// AttackerBlockedByCreature, 10 per AttackerBlockedOnce, 100 per AttackerBlocked.
func blockCounters(t *testing.T, g *engine.Game, p engine.PlayerID) {
	t.Helper()
	for _, w := range []struct{ mode, gain string }{
		{"AttackerBlockedByCreature | ValidCard$ Creature", "1"},
		{"AttackerBlockedOnce", "10"},
		{"AttackerBlocked | ValidCard$ Creature", "100"},
	} {
		g.NewCard(watcherDef(t, "Watcher "+w.gain, "Mode$ "+w.mode+" | TriggerZones$ Battlefield",
			"DB$ GainLife | Defined$ You | LifeAmount$ "+w.gain), p, engine.Battlefield)
	}
}

// attackingCombat is a two-player game on the scenario DB where other, the
// active player, has declared its one 2/2 attacking p.
func attackingCombat(t *testing.T) (g *engine.Game, p, other engine.PlayerID, attacker engine.CardID) {
	t.Helper()
	g, p, other = newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, other, engine.DeclareAttackers)
	attacker = g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)
	return g, p, other, attacker
}

// Brimaz blocks: its AttackerBlocked trigger makes a Cat Soldier blocking the
// same attacker. The declaration ran every mode once; the token's block runs
// AttackerBlockedByCreature and AttackerBlockedOnce again but not
// AttackerBlocked, since the attacker was already blocked.
func TestTokenBlockingBrimazTokenJoinsTheBlock(t *testing.T) {
	t.Parallel()

	g, p, _, attacker := attackingCombat(t)
	brimaz := g.NewCard(corpusCard(t, "Brimaz, King of Oreskos"), p, engine.Battlefield)
	blockCounters(t, g, p)
	g.SetTurnState(1, g.ActivePlayer(), engine.DeclareBlockers)
	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: brimaz, Attacker: attacker}})
	declareBlockers(t, g, bc)
	if err := g.ResolveStack(engine.NewRegistry(), bc); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	var blockers []engine.CardID
	for _, b := range g.Blocks() {
		if b.Attacker == attacker {
			blockers = append(blockers, b.Blocker)
		}
	}
	if len(blockers) != 2 || blockers[0] != brimaz {
		t.Fatalf("blockers of the attacker = %v, want Brimaz then its token", blockers)
	}
	if name := g.Card(blockers[1]).Name(); name != "Cat Soldier Token" && !strings.Contains(name, "Cat Soldier") {
		t.Errorf("second blocker is %q, want the Cat Soldier token", name)
	}
	if got := g.Player(p).Life; got != 20+2*1+2*10+100 {
		t.Errorf("life = %d, want %d: two ByCreature, two Once, one AttackerBlocked", got, 20+2*1+2*10+100)
	}
}

// An unblocked attacker (Nature Shields Its Own's shape, an AttackerUnblocked
// trigger) becomes blocked by the token: all three modes run, and the attacker
// deals no combat damage to the defending player.
func TestTokenBlockingUnblockedAttackerBecomesBlocked(t *testing.T) {
	t.Parallel()

	g, p, _, attacker := attackingCombat(t)
	g.NewCard(watcherDef(t, "Shield", "Mode$ AttackerUnblocked | ValidCard$ Creature | TriggerZones$ Battlefield",
		"DB$ Token | TokenOwner$ You | TokenScript$ g_0_1_plant | TokenBlocking$ TriggeredAttacker"), p, engine.Battlefield)
	blockCounters(t, g, p)
	g.SetTurnState(1, g.ActivePlayer(), engine.DeclareBlockers)
	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)
	if err := g.ResolveStack(engine.NewRegistry(), bc); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !slices.ContainsFunc(g.Blocks(), func(b engine.Block) bool { return b.Attacker == attacker }) {
		t.Fatalf("Blocks() = %v, want the plant blocking the attacker", g.Blocks())
	}
	if got := g.Player(p).Life; got != 20+1+10+100 {
		t.Errorf("life = %d, want %d: ByCreature, Once and AttackerBlocked once each", got, 20+1+10+100)
	}
}

// Mirror Match's shape: CopyPermanent's TokenBlocking$ blocks with the copy.
func TestTokenBlockingCopyPermanentBlocksWithTheCopy(t *testing.T) {
	t.Parallel()

	g, p, _, attacker := attackingCombat(t)
	g.NewCard(watcherDef(t, "Mirror", "Mode$ AttackerUnblocked | ValidCard$ Creature | TriggerZones$ Battlefield",
		"DB$ CopyPermanent | Defined$ TriggeredAttacker | TokenBlocking$ TriggeredAttacker"), p, engine.Battlefield)
	g.SetTurnState(1, g.ActivePlayer(), engine.DeclareBlockers)
	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)
	if err := g.ResolveStack(engine.NewRegistry(), bc); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	blocks := g.Blocks()
	if len(blocks) != 1 || blocks[0].Attacker != attacker || g.Card(blocks[0].Blocker).Controller() != p {
		t.Fatalf("Blocks() = %v, want p's copy blocking the attacker", blocks)
	}
}

// Shapes where the token only enters: outside combat, an attacker that is not
// attacking, and a token whose controller is not the attacker's defender.
func TestTokenBlockingSkipsWithoutABlock(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name        string
		phase       engine.PhaseType
		attacking   bool
		asAttacker  bool
		wantBlocked bool
	}{
		{"in combat", engine.DeclareBlockers, true, false, true},
		{"outside combat", engine.Main2, true, false, false},
		{"not an attacker", engine.DeclareBlockers, false, false, false},
		{"not the defender", engine.DeclareBlockers, true, true, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other, attacker := attackingCombat(t)
			target := attacker
			if !tc.attacking {
				target = g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
			}
			g.SetTurnState(1, other, tc.phase)
			who := p
			if tc.asAttacker {
				who = other
			}
			host, err := resolveNow(t, g, who, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(target)},
				"DB$ Token | TokenScript$ g_0_1_plant | RememberTokens$ True | TokenBlocking$ Targeted")
			if err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if len(g.Card(host).Memory.Remembered()) != 1 {
				t.Fatalf("remembered %v, want the one token", g.Card(host).Memory.Remembered())
			}
			if blocked := len(g.Blocks()) > 0; blocked != tc.wantBlocked {
				t.Errorf("Blocks() = %v, want blocked %v", g.Blocks(), tc.wantBlocked)
			}
		})
	}
}

// An attacker attacking a planeswalker is defended by the planeswalker's
// controller (Combat.getDefenderPlayerByAttacker, Combat.java:297-302): that
// player's token blocks it.
func TestTokenBlockingAttackOnAPlaneswalker(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, other, engine.DeclareAttackers)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	pw := g.NewCard(corpusCard(t, "Jace Beleren"), p, engine.Battlefield)
	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	ac.QueueAttackTarget(engine.CardEntity(pw))
	declareAttackers(t, g, ac)
	if got := g.AttackTarget(attacker); got != engine.CardEntity(pw) {
		t.Fatalf("attack target = %v, want the planeswalker", got)
	}
	g.SetTurnState(1, other, engine.DeclareBlockers)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(attacker)},
		"DB$ Token | TokenScript$ g_0_1_plant | TokenBlocking$ Targeted"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if len(g.Blocks()) != 1 || g.Blocks()[0].Attacker != attacker {
		t.Errorf("Blocks() = %v, want the plant blocking the planeswalker's attacker", g.Blocks())
	}
}

// Refused: a block before blockers are declared (DeclareCombatBlockers would
// replace it; no corpus line gets there), and a Defined$ the port cannot read.
func TestTokenBlockingUnresolvableShapesFail(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, line, want string
		phase            engine.PhaseType
	}{
		{"before blockers", "DB$ Token | TokenScript$ g_0_1_plant | TokenBlocking$ Targeted", "before blockers are declared", engine.DeclareAttackers},
		{"unreadable defined", "DB$ Token | TokenScript$ g_0_1_plant | TokenBlocking$ Bogus", "TokenBlocking$", engine.DeclareBlockers},
		{"copy, before blockers", "DB$ CopyPermanent | Defined$ Targeted | TokenBlocking$ Targeted", "CopyPermanent", engine.DeclareAttackers},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other, attacker := attackingCombat(t)
			g.SetTurnState(1, other, tc.phase)
			_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(attacker)}, tc.line)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Errorf("ResolveStack = %v, want an error naming %q", err, tc.want)
			}
		})
	}
}
