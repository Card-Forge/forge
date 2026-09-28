package engine_test

import (
	"errors"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// declareAttackers runs g.DeclareCombatAttackers and fails the test on an
// error: every caller expects a legal declaration.
func declareAttackers(t testing.TB, g *engine.Game, c engine.PlayerController) []engine.CardID {
	t.Helper()
	got, err := g.DeclareCombatAttackers(c)
	if err != nil {
		t.Fatalf("DeclareCombatAttackers: %v", err)
	}
	return got
}

// declareBlockers is declareAttackers for g.DeclareCombatBlockers.
func declareBlockers(t testing.TB, g *engine.Game, c engine.PlayerController) []engine.Block {
	t.Helper()
	got, err := g.DeclareCombatBlockers(c)
	if err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
	return got
}

// wantIllegal fails the test unless err is an *engine.IllegalDeclarationError
// citing rule.
func wantIllegal(t testing.TB, err error, rule string) engine.IllegalDeclarationError {
	t.Helper()
	var ill *engine.IllegalDeclarationError
	if !errors.As(err, &ill) {
		t.Fatalf("err = %v, want an *IllegalDeclarationError (%s)", err, rule)
	}
	if ill.Rule != rule || !strings.Contains(ill.Error(), rule) {
		t.Fatalf("rule = %q (%v), want %q", ill.Rule, ill, rule)
	}
	return *ill
}

// wantErrorContaining fails the test unless err is a plain error (not an
// illegal declaration) mentioning want.
func wantErrorContaining(t testing.TB, err error, want string) {
	t.Helper()
	var ill *engine.IllegalDeclarationError
	if err == nil || errors.As(err, &ill) || !strings.Contains(err.Error(), want) {
		t.Fatalf("err = %v, want an error mentioning %q", err, want)
	}
}

// combatCreatureDef is a 2/2 creature compiled from script text: its S:
// lines, T: lines and SVars (name, body pairs).
func combatCreatureDef(t *testing.T, name string, statics, triggers []string, svars ...string) *compile.Card {
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
	raw.Faces[0].Statics = statics
	raw.Faces[0].Triggers = triggers
	for i := 0; i+1 < len(svars); i += 2 {
		raw.Faces[0].SVars.Set(svars[i], svars[i+1])
	}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// combatGame is a two-player game on a's first-turn Main1.
func combatGame(t *testing.T) (*engine.Game, engine.PlayerID, engine.PlayerID) {
	t.Helper()
	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	g.Player(a).Life, g.Player(b).Life = 20, 20
	return g, a, b
}

// declareAttacking declares attackers as a legal declaration.
func declareAttacking(t *testing.T, g *engine.Game, attackers ...engine.CardID) {
	t.Helper()
	ac := engine.NewScriptedController()
	ac.QueueAttackers(attackers)
	declareAttackers(t, g, ac)
}

// tryBlocks declares blocks and returns the error.
func tryBlocks(g *engine.Game, blocks ...engine.Block) error {
	bc := engine.NewScriptedController()
	bc.QueueBlocks(blocks)
	_, err := g.DeclareCombatBlockers(bc)
	return err
}

func TestDeclareAttackersRejectsIneligibleAndRepeatedAttackers(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	ready := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	tapped := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	g.Card(tapped).Tapped = true
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	for _, answer := range [][]engine.CardID{{tapped}, {theirs}, {ready, ready}} {
		ac := engine.NewScriptedController()
		ac.QueueAttackers(answer)
		_, err := g.DeclareCombatAttackers(ac)
		wantIllegal(t, err, "CR 508.1a")
		if g.Card(ready).Tapped || len(g.Attackers()) != 0 {
			t.Fatalf("answer %v: the illegal declaration was applied", answer)
		}
	}
}

func TestDeclareAttackersRejectsAnUnofferedDefender(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	g.NewCard(planeswalkerDefLoyalty(t, "3"), b, engine.Battlefield)
	own := g.NewCard(planeswalkerDefLoyalty(t, "3"), a, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	ac.QueueAttackTarget(engine.CardEntity(own))
	_, err := g.DeclareCombatAttackers(ac)
	wantIllegal(t, err, "CR 508.1b")
	if len(g.Attackers()) != 0 {
		t.Errorf("attackers = %v, want none", g.Attackers())
	}
}

// "CARDNAME attacks each combat if able." (S:Mode$ MustAttack | ValidCreature$
// Card.Self, 67 corpus lines): left home it breaks CR 508.1d; attacking, or
// being unable to attack, is legal.
func TestMustAttackStaticRequiresTheCreatureToAttack(t *testing.T) {
	t.Parallel()
	g, a, _ := combatGame(t)
	def := combatCreatureDef(t, "Berserker", []string{"Mode$ MustAttack | ValidCreature$ Card.Self | Description$ CARDNAME attacks each combat if able."}, nil)
	berserker := g.NewCard(def, a, engine.Battlefield)
	other := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{other})
	_, err := g.DeclareCombatAttackers(ac)
	ill := wantIllegal(t, err, "CR 508.1d")
	if len(ill.Cards) != 1 || ill.Cards[0] != berserker {
		t.Errorf("cards = %v, want the berserker", ill.Cards)
	}
	if g.Card(other).Tapped {
		t.Error("the illegal declaration tapped an attacker")
	}

	declareAttacking(t, g, berserker)
	if !g.Card(berserker).Tapped {
		t.Error("the declared berserker did not tap")
	}
}

func TestMustAttackStaticExcusesACreatureThatCannotAttack(t *testing.T) {
	t.Parallel()
	g, a, _ := combatGame(t)
	def := combatCreatureDef(t, "Berserker", []string{"Mode$ MustAttack | ValidCreature$ Card.Self"}, nil)
	berserker := g.NewCard(def, a, engine.Battlefield)
	g.Card(berserker).Tapped = true
	g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	declareAttacking(t, g)
}

// Condition$ gates the requirement (Threshold here); a static whose
// Condition$ fails asks nothing.
func TestMustAttackStaticHonoursItsCondition(t *testing.T) {
	t.Parallel()
	g, a, _ := combatGame(t)
	def := combatCreatureDef(t, "Threshold Beast", []string{"Mode$ MustAttack | ValidCreature$ Card.Self | Condition$ Threshold"}, nil)
	beast := g.NewCard(def, a, engine.Battlefield)
	declareAttacking(t, g)

	for i := 0; i < 7; i++ {
		g.NewCard(creatureDefPT(t, "1", "1"), a, engine.Graveyard)
	}
	ac := engine.NewScriptedController()
	ac.QueueAttackers(nil)
	_, err := g.DeclareCombatAttackers(ac)
	ill := wantIllegal(t, err, "CR 508.1d")
	if ill.Cards[0] != beast {
		t.Errorf("cards = %v, want the beast", ill.Cards)
	}
}

// MustAttack$ names the player the creature has to attack: in a
// three-player game attacking the other opponent leaves the requirement
// unmet, while a player the static names who is the active player itself is
// dropped (CR 506.2).
func TestMustAttackStaticNamesTheDefender(t *testing.T) {
	t.Parallel()
	g := newGame(t, "a", "b", "c")
	ps := g.Players()
	for _, p := range ps {
		g.Player(p).Life = 20
	}
	g.SetTurnState(1, ps[1], engine.Main1)
	// b's creature must attack "You" -- the static's controller, c.
	host := g.NewCard(combatCreatureDef(t, "Taunter", []string{"Mode$ MustAttack | ValidCreature$ Creature.OppCtrl | MustAttack$ You"}, nil), ps[2], engine.Battlefield)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), ps[1], engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	ac.QueueAttackTarget(engine.PlayerEntity(ps[0]))
	_, err := g.DeclareCombatAttackers(ac)
	wantIllegal(t, err, "CR 508.1d")

	ac.QueueAttackers([]engine.CardID{attacker})
	ac.QueueAttackTarget(engine.PlayerEntity(ps[2]))
	declareAttackers(t, g, ac)

	// On c's own turn the static names c, the active player: no requirement.
	g2 := newGame(t, "a", "b")
	x, y := g2.Players()[0], g2.Players()[1]
	g2.SetTurnState(1, x, engine.Main1)
	g2.NewCard(combatCreatureDef(t, "Taunter", []string{"Mode$ MustAttack | ValidCreature$ Creature | MustAttack$ You"}, nil), x, engine.Battlefield)
	g2.NewCard(creatureDefPT(t, "2", "2"), y, engine.Battlefield)
	declareAttacking(t, g2)
	_ = host
}

// MustAttack$ Player.Other names every player but the static's own
// controller (matchesPlayerSpec's "Other", host = controller) --
// definedPlayers' generic Player.<property> fallthrough, added porting
// Subgame (effects-subgame.md), now reaches definedEntities the same way
// every other MustAttack$ spelling does. 2 real corpus lines.
func TestMustAttackStaticPlayerOtherNamesEveryOpponent(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	host := g.NewCard(combatCreatureDef(t, "Taunter", []string{"Mode$ MustAttack | ValidCreature$ Card.Self | MustAttack$ Player.Other"}, nil), a, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers(nil)
	_, err := g.DeclareCombatAttackers(ac)
	wantIllegal(t, err, "CR 508.1d")

	ac.QueueAttackers([]engine.CardID{host})
	ac.QueueAttackTarget(engine.PlayerEntity(b))
	declareAttackers(t, g, ac)
}

// "Target creature attacks this turn if able" is an effect card in the
// Command zone carrying Mode$ MustAttack | ValidCreature$ Card.IsRemembered
// (18 corpus lines).
func TestMustAttackFromAnEffectCard(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	if _, err := resolveNow(t, g, a, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)},
		"DB$ Effect | ValidTgts$ Creature | RememberObjects$ Targeted | StaticAbilities$ MustAttack",
		"MustAttack", "Mode$ MustAttack | ValidCreature$ Card.IsRemembered"); err != nil {
		t.Fatalf("resolve: %v", err)
	}
	g.SetTurnState(2, b, engine.Main1)
	ac := engine.NewScriptedController()
	ac.QueueAttackers(nil)
	_, err := g.DeclareCombatAttackers(ac)
	wantIllegal(t, err, "CR 508.1d")
	declareAttacking(t, g, victim)
}

func TestMustAttackStaticRejectsUnportedShapes(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct{ static, want string }{
		{"Mode$ MustAttack | ValidCreature$ Card.Self | ValidPlayer$ Opponent", "ValidPlayer$ not resolvable yet"},
		{"Mode$ MustAttack | ValidCreature$ Card.Self | Condition$ Monarch", `Condition$ "Monarch" not resolvable yet`},
		{"Mode$ MustAttack | ValidCreature$ Card.Self | AffectedZone$ Graveyard", `AffectedZone$ "Graveyard" not resolvable yet`},
		{"Mode$ MustAttack | ValidCreature$ Card.Self | MustAttack$ CardOwner", "MustAttack$"},
		{"Mode$ AttackRequirement | ValidCard$ Creature | ValidAttacker$ Creature", "Mode$ AttackRequirement static not resolvable yet"},
		{"Mode$ PlayerMustAttack | ValidPlayer$ Player | MustAttack$ Player", "Mode$ PlayerMustAttack static not resolvable yet"},
	} {
		g, a, _ := combatGame(t)
		g.NewCard(combatCreatureDef(t, "Odd", []string{tc.static}, nil), a, engine.Battlefield)
		ac := engine.NewScriptedController()
		ac.QueueAttackers(nil)
		_, err := g.DeclareCombatAttackers(ac)
		wantErrorContaining(t, err, tc.want)
	}
}

// IsPresent$ gates a MustAttack static like a Condition$.
func TestMustAttackStaticHonoursIsPresent(t *testing.T) {
	t.Parallel()
	g, a, _ := combatGame(t)
	def := combatCreatureDef(t, "Loner", []string{"Mode$ MustAttack | ValidCreature$ Card.Self | IsPresent$ Creature.Other+YouCtrl | PresentCompare$ EQ0"}, nil)
	g.NewCard(def, a, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	declareAttacking(t, g)
}

// Two goads by the same player are one requirement (Card.getGoaded is a
// set); attacking meets it.
func TestGoadedTwiceBySamePlayerAttacksLegally(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	for i := 0; i < 2; i++ {
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
		resolveLine(t, g, a, c, "DB$ Goad | ValidTgts$ Creature")
	}
	g.SetTurnState(2, b, engine.Main1)
	declareAttacking(t, g, victim)
	if got := g.AttackTarget(victim); got != engine.PlayerEntity(a) {
		t.Errorf("target = %v, want the goader, the only opponent", got)
	}
}

// --- Blockers ---

func TestDeclareBlockersRejectsIllegalPairings(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	second := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	tapped := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	g.Card(tapped).Tapped = true
	cantBlock := g.NewCard(creatureDefPTKeywords(t, "2", "2", "CARDNAME can't block."), b, engine.Battlefield)
	homebody := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	declareAttacking(t, g, attacker, second)

	for _, tc := range []struct {
		blocks []engine.Block
		rule   string
	}{
		{[]engine.Block{{Blocker: blocker, Attacker: homebody}}, "CR 802.4a"},
		{[]engine.Block{{Blocker: tapped, Attacker: attacker}}, "CR 509.1a"},
		{[]engine.Block{{Blocker: blocker, Attacker: attacker}, {Blocker: blocker, Attacker: attacker}}, "CR 509.1a"},
		{[]engine.Block{{Blocker: blocker, Attacker: attacker}, {Blocker: blocker, Attacker: second}}, "CR 509.1a"},
		{[]engine.Block{{Blocker: cantBlock, Attacker: attacker}}, "CR 509.1b"},
	} {
		wantIllegal(t, tryBlocks(g, tc.blocks...), tc.rule)
		if len(g.Blocks()) != 0 {
			t.Fatalf("blocks %v: the illegal declaration was applied", tc.blocks)
		}
	}
}

// MustBlock (AB$ MustBlock | ValidTgts$ Creature, the dominant corpus
// shape): the targeted creature must block the host if able.
func TestMustBlockEffectRequiresTheBlock(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	bystander := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	host, err := resolveNow(t, g, a, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(blocker)},
		"DB$ MustBlock | ValidTgts$ Creature")
	if err != nil {
		t.Fatalf("resolve: %v", err)
	}
	if got := g.Card(blocker).MustBlockAttackers(); len(got) != 1 || got[0] != host {
		t.Fatalf("must block = %v, want the host", got)
	}
	declareAttacking(t, g, host)

	ill := wantIllegal(t, tryBlocks(g), "CR 509.1c")
	if ill.Cards[0] != blocker || ill.Cards[1] != host {
		t.Errorf("cards = %v, want the blocker and the host", ill.Cards)
	}
	wantIllegal(t, tryBlocks(g, engine.Block{Blocker: bystander, Attacker: host}), "CR 509.1c")
	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: host}})
	declareBlockers(t, g, bc)

	// It lasts the turn: gone at cleanup.
	g.SetTurnState(1, a, engine.EndOfTurn)
	g.AdvancePhase(engine.NewScriptedController())
	if got := g.Card(blocker).MustBlockAttackers(); got != nil {
		t.Errorf("must block after cleanup = %v, want none", got)
	}
}

// A MustBlock host that never attacks itself (e.g. a DefinedAttacker$ chosen
// off the battlefield, or the host activated before combat and left home)
// must not crash declaration once someone else attacks: CombatUtil.canBeBlocked
// returns true when the named attacker isn't attacking (CombatUtil.java:526-527),
// skipping the CR 802.4a defender check rather than asking defenderOf, which
// panics on a non-attacker.
func TestMustBlockHostThatNeverAttacksDoesNotPanic(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	other := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	// resolveNow's own etbChainDef host is the MustBlock ability's source
	// and Defined$ Self attacker -- it stays home; only other attacks.
	if _, err := resolveNow(t, g, a, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(blocker)},
		"DB$ MustBlock | ValidTgts$ Creature"); err != nil {
		t.Fatalf("resolve: %v", err)
	}
	declareAttacking(t, g, other)

	if err := tryBlocks(g); err != nil {
		t.Fatalf("DeclareCombatBlockers with no blocks: %v", err)
	}
}

// A blocker that can't block (tapped) is excused from its requirement.
func TestMustBlockExcusesATappedBlocker(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	host, err := resolveNow(t, g, a, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(blocker)},
		"DB$ MustBlock | ValidTgts$ Creature")
	if err != nil {
		t.Fatalf("resolve: %v", err)
	}
	g.Card(blocker).Tapped = true
	declareAttacking(t, g, host)
	if err := tryBlocks(g); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
}

// Whenever it attacks, up to one target creature blocks it this combat if
// able (Mode$ Attacks | Execute$ DB$ MustBlock | DefinedAttacker$
// TriggeredAttackerLKICopy | BlockAllDefined$ True | Duration$
// UntilEndOfCombat): the requirement is made by the attack trigger and ends
// with the combat.
func TestMustBlockFromAnAttackTriggerEndsWithCombat(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	def := combatCreatureDef(t, "Provoker", nil,
		[]string{"Mode$ Attacks | ValidCard$ Card.Self | Execute$ TrigMustBlock"},
		"TrigMustBlock", "DB$ MustBlock | ValidTgts$ Creature.DefenderCtrl | TargetMin$ 0 | TargetMax$ 1 | DefinedAttacker$ TriggeredAttackerLKICopy | BlockAllDefined$ True | Duration$ UntilEndOfCombat")
	attacker := g.NewCard(def, a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	ac.QueueTargets([]engine.EntityID{engine.CardEntity(blocker)})
	declareAttackers(t, g, ac)
	if err := g.ResolveStack(engine.NewRegistry(), ac); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(blocker).MustBlockAttackers(); len(got) != 1 || got[0] != attacker {
		t.Fatalf("must block = %v, want the attacker", got)
	}
	wantIllegal(t, tryBlocks(g), "CR 509.1c")
	if err := tryBlocks(g, engine.Block{Blocker: blocker, Attacker: attacker}); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}

	g.SetTurnState(1, a, engine.CombatDamage)
	g.AdvancePhase(ac)
	if got := g.Card(blocker).MustBlockAttackers(); got != nil {
		t.Errorf("must block after combat = %v, want none", got)
	}
}

func TestMustBlockEffectRejectsUnportedShapes(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct{ line, want string }{
		{"DB$ MustBlock | Choices$ Creature | Chooser$ TriggeredDefendingPlayer", "Choices$ not resolvable yet"},
		{"DB$ MustBlock | ValidTgts$ Creature | Duration$ UntilYourNextTurn", `Duration$ "UntilYourNextTurn" not resolvable yet`},
		{"DB$ MustBlock | ValidTgts$ Creature | DefinedAttacker$ ParentTarget", "DefinedAttacker$"},
	} {
		g, a, b := combatGame(t)
		blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
		_, err := resolveNow(t, g, a, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(blocker)}, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: err = %v, want %q", tc.line, err, tc.want)
		}
	}
}

// A DefinedAttacker$ naming nothing makes no requirement; a Defined$ blocker
// that left the battlefield is skipped.
func TestMustBlockEffectSkipsMissingCards(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	if _, err := resolveNow(t, g, a, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(blocker)},
		"DB$ MustBlock | ValidTgts$ Creature | DefinedAttacker$ Remembered"); err != nil {
		t.Fatalf("resolve: %v", err)
	}
	gone := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Graveyard)
	if _, err := resolveNow(t, g, a, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(gone)},
		"DB$ MustBlock | ValidTgts$ Creature"); err != nil {
		t.Fatalf("resolve: %v", err)
	}
	if g.Card(blocker).MustBlockAttackers() != nil || g.Card(gone).MustBlockAttackers() != nil {
		t.Error("a requirement was recorded")
	}
}

// "Target creature blocks this turn if able" as an effect card: Mode$
// MustBlock | ValidCreature$ Card.IsRemembered (14 corpus lines) -- the
// creature must block some attacker it can.
func TestMustBlockStaticFromAnEffectCard(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	host, err := resolveNow(t, g, a, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(blocker)},
		"DB$ Effect | ValidTgts$ Creature | RememberObjects$ Targeted | StaticAbilities$ MustBlock",
		"MustBlock", "Mode$ MustBlock | ValidCreature$ Card.IsRemembered")
	if err != nil {
		t.Fatalf("resolve: %v", err)
	}
	declareAttacking(t, g, host)
	ill := wantIllegal(t, tryBlocks(g), "CR 509.1c")
	if ill.Cards[0] != blocker {
		t.Errorf("cards = %v, want the blocker", ill.Cards)
	}
	if err := tryBlocks(g, engine.Block{Blocker: blocker, Attacker: host}); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
}

// "CARDNAME blocks each combat if able." is excused when the only attacker
// has Menace and no second creature could join the block.
func TestBlocksEachCombatExcusedByMenace(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Menace"), a, engine.Battlefield)
	g.NewCard(combatCreatureDef(t, "Guard", []string{"Mode$ MustBlock | ValidCreature$ Card.Self"}, nil), b, engine.Battlefield)
	declareAttacking(t, g, attacker)
	if err := tryBlocks(g); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
}

// Lure keywords (K:All creatures able to block CARDNAME do so., 11 printed
// corpus lines; K:CARDNAME must be blocked if able., 16): a creature able to
// block the lure attacker must, and may not block another instead.
func TestLureAttackerMustBeBlocked(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	lure := g.NewCard(creatureDefPTKeywords(t, "2", "2", "All creatures able to block CARDNAME do so."), a, engine.Battlefield)
	plain := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	declareAttacking(t, g, lure, plain)

	ill := wantIllegal(t, tryBlocks(g), "CR 509.1c")
	if !strings.Contains(ill.Reason, "any") {
		t.Errorf("reason = %q, want the not-blocking-any message", ill.Reason)
	}
	ill = wantIllegal(t, tryBlocks(g, engine.Block{Blocker: blocker, Attacker: plain}), "CR 509.1c")
	if !strings.Contains(ill.Reason, "right ones") {
		t.Errorf("reason = %q, want the wrong-attacker message", ill.Reason)
	}
	if err := tryBlocks(g, engine.Block{Blocker: blocker, Attacker: lure}); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
}

// "CARDNAME must be blocked if able." wants one blocker, not every one.
func TestMustBeBlockedIfAbleIsMetByOneBlocker(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "CARDNAME must be blocked if able."), a, engine.Battlefield)
	first := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	declareAttacking(t, g, attacker)
	wantIllegal(t, tryBlocks(g), "CR 509.1c")
	if err := tryBlocks(g, engine.Block{Blocker: first, Attacker: attacker}); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
}

// MustBeBlockedBy <valid>: a creature matching valid must block it while no
// matching creature does.
func TestMustBeBlockedByValidBlocker(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "MustBeBlockedBy Creature.powerGE3"), a, engine.Battlefield)
	big := g.NewCard(creatureDefPT(t, "3", "3"), b, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), b, engine.Battlefield)
	declareAttacking(t, g, attacker)
	ill := wantIllegal(t, tryBlocks(g), "CR 509.1c")
	if ill.Cards[0] != big {
		t.Errorf("cards = %v, want the power-3 creature", ill.Cards)
	}
	if err := tryBlocks(g, engine.Block{Blocker: big, Attacker: attacker}); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
}

// MustBeBlockedByAll:<valid> wants every matching creature blocking it.
func TestMustBeBlockedByAllValidBlockers(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "MustBeBlockedByAll:Creature"), a, engine.Battlefield)
	first := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	second := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	declareAttacking(t, g, attacker)
	wantIllegal(t, tryBlocks(g, engine.Block{Blocker: first, Attacker: attacker}), "CR 509.1c")
	if err := tryBlocks(g, engine.Block{Blocker: first, Attacker: attacker}, engine.Block{Blocker: second, Attacker: attacker}); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
}

func TestBlockerCountKeywordsAndStatics(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	single := g.NewCard(combatCreatureDef(t, "Loner", []string{"Mode$ MinMaxBlocker | ValidCard$ Card.Self | Max$ 1"}, nil), a, engine.Battlefield)
	crowd := g.NewCard(combatCreatureDef(t, "Crowd", []string{"Mode$ MinMaxBlocker | ValidCard$ Card.Self | Min$ All"}, nil), a, engine.Battlefield)
	b1 := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	b2 := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	declareAttacking(t, g, single, crowd)

	wantIllegal(t, tryBlocks(g, engine.Block{Blocker: b1, Attacker: single}, engine.Block{Blocker: b2, Attacker: single}), "CR 509.1b")
	wantIllegal(t, tryBlocks(g, engine.Block{Blocker: b1, Attacker: crowd}), "CR 509.1b")
	if err := tryBlocks(g, engine.Block{Blocker: b1, Attacker: crowd}, engine.Block{Blocker: b2, Attacker: crowd}); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
}

func TestMinMaxBlockerRejectsUnresolvableAmounts(t *testing.T) {
	t.Parallel()
	for _, static := range []string{
		"Mode$ MinMaxBlocker | ValidCard$ Card.Self | Min$ Bogus",
		"Mode$ MinMaxBlocker | ValidCard$ Card.Self | Max$ Bogus",
	} {
		g, a, b := combatGame(t)
		attacker := g.NewCard(combatCreatureDef(t, "Odd", []string{static}, nil), a, engine.Battlefield)
		g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
		declareAttacking(t, g, attacker)
		wantErrorContaining(t, tryBlocks(g), "not resolvable")
	}
}

func TestCantBlockAloneKeywords(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	shy := g.NewCard(creatureDefPTKeywords(t, "2", "2", "CARDNAME can't block alone."), b, engine.Battlefield)
	pack := g.NewCard(creatureDefPTKeywords(t, "2", "2", "CARDNAME can't block unless at least two other creatures block."), b, engine.Battlefield)
	weak := g.NewCard(creatureDefPTKeywords(t, "1", "1", "CARDNAME can't block unless a creature with greater power also blocks."), b, engine.Battlefield)
	big := g.NewCard(creatureDefPT(t, "3", "3"), b, engine.Battlefield)
	tiny := g.NewCard(creatureDefPT(t, "1", "1"), b, engine.Battlefield)
	declareAttacking(t, g, attacker)

	wantIllegal(t, tryBlocks(g, engine.Block{Blocker: shy, Attacker: attacker}), "CR 509.1b")
	wantIllegal(t, tryBlocks(g, engine.Block{Blocker: pack, Attacker: attacker}, engine.Block{Blocker: shy, Attacker: attacker}), "CR 509.1b")
	wantIllegal(t, tryBlocks(g, engine.Block{Blocker: weak, Attacker: attacker}, engine.Block{Blocker: tiny, Attacker: attacker}), "CR 509.1b")
	if err := tryBlocks(g,
		engine.Block{Blocker: shy, Attacker: attacker},
		engine.Block{Blocker: pack, Attacker: attacker},
		engine.Block{Blocker: weak, Attacker: attacker},
		engine.Block{Blocker: big, Attacker: attacker}); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
}

// "CARDNAME can't attack or block alone." with no other creature can't
// block at all: CanBlock itself refuses it.
func TestCantBlockAloneWithoutCompanyCannotBlock(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	shy := g.NewCard(creatureDefPTKeywords(t, "2", "2", "CARDNAME can't attack or block alone."), b, engine.Battlefield)
	if g.CanBlock(attacker, shy) {
		t.Error("CanBlock = true for a lone can't-block-alone creature")
	}
}

// A MustBlock requirement against a Menace attacker holds only if another
// free creature can join the block; blocking it together is legal.
func TestMustBlockAgainstMenaceNeedsAPartner(t *testing.T) {
	t.Parallel()
	def := combatCreatureDef(t, "Menacer", nil,
		[]string{"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ Trig"},
		"Trig", "DB$ MustBlock | ValidTgts$ Creature")
	def.Faces[0].Keywords = []string{"Menace"}

	g, a, b := combatGame(t)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	partner := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	host := g.NewCard(def, a, engine.Battlefield)
	resolveExecute(t, g, a, host, blocker)
	declareAttacking(t, g, host)
	wantIllegal(t, tryBlocks(g), "CR 509.1c")
	if err := tryBlocks(g, engine.Block{Blocker: blocker, Attacker: host}, engine.Block{Blocker: partner, Attacker: host}); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}

	// Without a partner the requirement cannot be met, so blocking nothing
	// is legal.
	g2, a2, b2 := combatGame(t)
	lone := g2.NewCard(creatureDefPT(t, "2", "2"), b2, engine.Battlefield)
	host2 := g2.NewCard(def, a2, engine.Battlefield)
	resolveExecute(t, g2, a2, host2, lone)
	declareAttacking(t, g2, host2)
	if err := tryBlocks(g2); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
}

// resolveExecute resolves host's first trigger's Execute$ ability as p's,
// targeting target.
func resolveExecute(t *testing.T, g *engine.Game, p engine.PlayerID, host, target engine.CardID) {
	t.Helper()
	face := g.Card(host).Def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		api, _ := engine.APIByName(sub.Ability.Name)
		g.PushAbility(engine.Ability{API: api, Source: host, Controller: p, Params: sub.Ability, Amounts: face.Amounts,
			Targets: []engine.EntityID{engine.CardEntity(target)}})
		if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		return
	}
	t.Fatal("no Execute$")
}

// DefenderCtrl (CardProperty.java:197-212) is relative to the attacking
// source -- or, for an Aura or Equipment source, to the creature it is
// attached to (Combat.getDefendingPlayerRelatedTo).
func TestDefenderCtrlFollowsTheAttackOrItsAttachment(t *testing.T) {
	t.Parallel()
	g, a, b := combatGame(t)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	homebody := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	aura := g.NewCard(auraDef(t), a, engine.Battlefield)
	g.Attach(aura, attacker)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	spec := valid.Parse("Creature.DefenderCtrl")
	if engine.Matches(g, g.Card(theirs), spec, a, attacker) {
		t.Error("DefenderCtrl matched before any attack")
	}
	declareAttacking(t, g, attacker)
	for _, src := range []engine.CardID{attacker, aura} {
		if !engine.Matches(g, g.Card(theirs), spec, a, src) {
			t.Errorf("source %d: the defender's creature did not match", src)
		}
		if engine.Matches(g, g.Card(homebody), spec, a, src) {
			t.Errorf("source %d: the attacker's own creature matched", src)
		}
	}
	if engine.Matches(g, g.Card(theirs), spec, a, homebody) {
		t.Error("a non-attacking source matched")
	}
}

// A lure keyword printed on a card (a K: line, compiled from script text the
// way the corpus is) is what the block validator reads.
func TestCompiledLureKeywordIsEnforced(t *testing.T) {
	t.Parallel()
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "Lure Beast"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Lure Beast"
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "2", "2"
	raw.Faces[0].Keywords = []string{"All creatures able to block CARDNAME do so."}
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	g, a, b := combatGame(t)
	lure := g.NewCard(def, a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	declareAttacking(t, g, lure)
	wantIllegal(t, tryBlocks(g), "CR 509.1c")
	if err := tryBlocks(g, engine.Block{Blocker: blocker, Attacker: lure}); err != nil {
		t.Fatalf("DeclareCombatBlockers: %v", err)
	}
}
