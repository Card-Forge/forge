package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// threeWayAttack is a combat where a attacks with three creatures: toPlayer
// at b, toWalker at b's planeswalker, toBattle at a's own Siege b protects.
// b controls bCreature, a controls aCreature (neither attacks).
type threeWayAttack struct {
	g                            *engine.Game
	a, b                         engine.PlayerID
	toPlayer, toWalker, toBattle engine.CardID
	walker, battle               engine.CardID
	aCreature, bCreature         engine.CardID
}

func newThreeWayAttack(t *testing.T) threeWayAttack {
	t.Helper()
	g, a, b := newTwoPlayerGame(t)
	g.SetTurnState(1, a, engine.DeclareAttackers)
	w := threeWayAttack{g: g, a: a, b: b}
	w.toPlayer = g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	w.toWalker = g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	w.toBattle = g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	w.walker = g.NewCard(planeswalkerDefLoyalty(t, "3"), b, engine.Battlefield)
	g.Card(w.walker).Counters.Add(engine.Loyalty, 3)
	w.battle = g.NewCard(battleDefDefense(t, "5"), a, engine.Battlefield)
	g.Card(w.battle).Counters.Add(engine.Defense, 5)
	g.Card(w.battle).ProtectingPlayer = b
	w.bCreature = g.NewCard(creatureDefPT(t, "1", "1"), b, engine.Battlefield)
	g.Card(w.bCreature).Tapped = true
	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{w.toPlayer, w.toWalker, w.toBattle})
	c.QueueAttackTarget(engine.PlayerEntity(b))
	c.QueueAttackTarget(engine.CardEntity(w.walker))
	c.QueueAttackTarget(engine.CardEntity(w.battle))
	declareAttackers(t, g, c)
	w.aCreature = g.NewCard(creatureDefPT(t, "1", "1"), a, engine.Battlefield)
	return w
}

func propMatches(g *engine.Game, id engine.CardID, spec string, controller engine.PlayerID, source engine.CardID) bool {
	return engine.Matches(g, g.Card(id), valid.Parse(spec), controller, source)
}

// CardProperty.java:1506-1549: attackingSame reads the source's defender,
// attackingYouOrYourPW (and its LKI spelling) the card's own -- a player or a
// planeswalker they control, never a battle -- and any other suffix reads as
// plain "attacking".
func TestAttackingSuffixProperties(t *testing.T) {
	t.Parallel()

	w := newThreeWayAttack(t)
	g := w.g
	for _, tc := range []struct {
		name   string
		id     engine.CardID
		spec   string
		source engine.CardID
		ctrl   engine.PlayerID
		want   bool
	}{
		{"same defender as the source", w.toPlayer, "Creature.attackingSame", w.toPlayer, w.a, true},
		{"another defender than the source", w.toWalker, "Creature.attackingSame", w.toPlayer, w.a, false},
		{"source not attacking", w.toPlayer, "Creature.attackingSame", w.aCreature, w.a, false},
		{"attacking you", w.toPlayer, "Creature.attackingYouOrYourPW", engine.NoCard, w.b, true},
		{"attacking your planeswalker", w.toWalker, "Creature.attackingYouOrYourPW", engine.NoCard, w.b, true},
		{"attacking a battle you protect", w.toBattle, "Creature.attackingYouOrYourPW", engine.NoCard, w.b, false},
		{"LKI spelling", w.toWalker, "Creature.attackingYouOrYourPWLKI", engine.NoCard, w.b, true},
		{"not your attacker", w.toPlayer, "Creature.attackingYouOrYourPW", engine.NoCard, w.a, false},
		{"other suffix reads as attacking", w.toBattle, "Creature.attackingSomething", engine.NoCard, w.a, true},
		{"other suffix, not attacking", w.aCreature, "Creature.attackingSomething", engine.NoCard, w.a, false},
		{"not alone among three", w.toPlayer, "Creature.attackingAlone", engine.NoCard, w.a, false},
	} {
		if got := propMatches(g, tc.id, tc.spec, tc.ctrl, tc.source); got != tc.want {
			t.Errorf("%s: %s = %v, want %v", tc.name, tc.spec, got, tc.want)
		}
	}
}

// attackingAlone (CardProperty.java:1511-1513): the only attacker, still so
// once it has died attacking (Thijarian Witness reads it on the dead card).
func TestAttackingAloneCountsTheLoneAttacker(t *testing.T) {
	t.Parallel()

	g, _, _, attacker := attackingCombat(t)
	if !propMatches(g, attacker, "Creature.attackingAlone", g.Card(attacker).Controller(), engine.NoCard) {
		t.Errorf("a lone attacker is not attackingAlone")
	}
	g.Move(attacker, engine.Graveyard, g.Card(attacker).Owner)
	if !propMatches(g, attacker, "Card.attackingAlone", g.Card(attacker).Owner, engine.NoCard) {
		t.Errorf("a lone attacker that died is not attackingAlone")
	}
}

// attackingBattle reads the source's defender (CardProperty.java:1522), a
// Forge bug for Rampaging Geoderm: refused with a pending error, never read
// off the card.
func TestAttackingBattleIsRefused(t *testing.T) {
	t.Parallel()

	w := newThreeWayAttack(t)
	if propMatches(w.g, w.toBattle, "Card.attackingBattle", w.a, w.toPlayer) {
		t.Errorf("attackingBattle matched, want it refused")
	}
	err := w.g.TakePendingError()
	if err == nil || !strings.Contains(err.Error(), "attackingBattle") {
		t.Errorf("pending error = %v, want one naming attackingBattle", err)
	}
}

// DefenderCtrl and DefenderCtrlForRemembered (CardProperty.java:198-213): the
// player defending against the source -- or its first remembered card -- is
// a battle's protector, not its controller.
func TestDefenderCtrlReadsTheBattleProtector(t *testing.T) {
	t.Parallel()

	w := newThreeWayAttack(t)
	g := w.g
	host := g.NewCard(creatureDefPT(t, "1", "1"), w.a, engine.Battlefield)
	if !propMatches(g, w.bCreature, "Creature.DefenderCtrl", w.a, w.toBattle) {
		t.Errorf("b's creature is not DefenderCtrl for the attack on the battle b protects")
	}
	if propMatches(g, w.aCreature, "Creature.DefenderCtrl", w.a, w.toBattle) {
		t.Errorf("a's creature is DefenderCtrl for the attack on a's own battle, want the protector's only")
	}
	if propMatches(g, w.bCreature, "Creature.DefenderCtrlForRemembered", w.a, host) {
		t.Errorf("DefenderCtrlForRemembered matched with nothing remembered")
	}
	g.Card(host).Memory.Remember(engine.PlayerEntity(w.b))
	if propMatches(g, w.bCreature, "Creature.DefenderCtrlForRemembered", w.a, host) {
		t.Errorf("DefenderCtrlForRemembered matched with a player remembered first")
	}
	g.Card(host).Memory.ClearRemembered()
	g.Card(host).Memory.Remember(engine.CardEntity(w.toBattle))
	if !propMatches(g, w.bCreature, "Creature.DefenderCtrlForRemembered", w.a, host) {
		t.Errorf("b's creature is not DefenderCtrlForRemembered for the remembered attacker")
	}
	if propMatches(g, w.aCreature, "Creature.DefenderCtrlForRemembered", w.a, host) {
		t.Errorf("a's creature is DefenderCtrlForRemembered, want the protector's only")
	}
}
