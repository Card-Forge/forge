package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// With a lone opponent controlling no planeswalker or battle, an attacker's
// target is assigned automatically -- no QueueAttackTarget call at all, so
// asking anyway would panic on the empty queue, which is exactly what this
// test's absence of one proves did not happen.
func TestDeclareCombatAttackersAutoAssignsWhenOpponentHasNoPlaneswalkerOrBattle(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, c)

	if got, want := g.AttackTarget(attacker), engine.PlayerEntity(b); got != want {
		t.Errorf("AttackTarget() = %v, want %v", got, want)
	}
}

// A planeswalker the opponent controls is a second eligible target, so the
// attacking player has to be asked which one -- an empty queue must panic.
func TestDeclareCombatAttackersAsksWhenOpponentControlsAPlaneswalker(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	g.NewCard(planeswalkerDefLoyalty(t, "3"), b, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{attacker})

	defer func() {
		if recover() == nil {
			t.Error("DeclareCombatAttackers did not ask for an attack target with two eligible ones")
		}
	}()
	declareAttackers(t, g, c)
}

// The queued answer for a two-target choice is honored and stored.
func TestDeclareCombatAttackersHonorsAQueuedPlaneswalkerTarget(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	pw := g.NewCard(planeswalkerDefLoyalty(t, "3"), b, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{attacker})
	c.QueueAttackTarget(engine.CardEntity(pw))
	declareAttackers(t, g, c)

	if got, want := g.AttackTarget(attacker), engine.CardEntity(pw); got != want {
		t.Errorf("AttackTarget() = %v, want %v", got, want)
	}
}

// A battle the opponent protects is eligible the same way a planeswalker is,
// and the same two-target choice applies: CombatUtil.getAllPossibleDefenders
// (CombatUtil.java:66-73) offers every battle whose protector is an opponent
// of the attacking player, here a Siege a itself controls.
func TestDeclareCombatAttackersHonorsAQueuedBattleTarget(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	battle := g.NewCard(battleDefDefense(t, "4"), a, engine.Battlefield)
	g.Card(battle).ProtectingPlayer = b

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{attacker})
	c.QueueAttackTarget(engine.CardEntity(battle))
	declareAttackers(t, g, c)

	if got, want := g.AttackTarget(attacker), engine.CardEntity(battle); got != want {
		t.Errorf("AttackTarget() = %v, want %v", got, want)
	}
}

// A battle the opponent controls but the attacking player protects is not
// attackable (CombatUtil.java:70: its protector is no opponent of theirs),
// and neither is one with no protector: the opponent is the only target left,
// so the attacker is assigned to them without a question.
func TestDeclareCombatAttackersSkipsABattleTheAttackerProtects(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	protected := g.NewCard(battleDefDefense(t, "4"), b, engine.Battlefield)
	g.Card(protected).ProtectingPlayer = a
	g.NewCard(battleDefDefense(t, "4"), a, engine.Battlefield) // no protector

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, c)

	if got, want := g.AttackTarget(attacker), engine.PlayerEntity(b); got != want {
		t.Errorf("AttackTarget() = %v, want %v (the opponent, the only eligible target)", got, want)
	}
}

// Multiplayer's own case: two living opponents means two eligible targets,
// so the attacking player has to choose which one to send an attacker at --
// CR 508.1d's "which opponent" freed from the old single-nextPlayerAfter
// assumption.
func TestDeclareCombatAttackersMultiplayerChoosesWhichOpponent(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b", "c")
	a, _, c2 := g.Players()[0], g.Players()[1], g.Players()[2]
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{attacker})
	c.QueueAttackTarget(engine.PlayerEntity(c2))
	declareAttackers(t, g, c)

	if got, want := g.AttackTarget(attacker), engine.PlayerEntity(c2); got != want {
		t.Errorf("AttackTarget() = %v, want %v", got, want)
	}
}

// A player who has already lost is not an eligible target -- the same
// nextPlayerAfter-style skip turn order itself uses.
func TestDeclareCombatAttackersSkipsALostPlayer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b", "c")
	a, b, c2 := g.Players()[0], g.Players()[1], g.Players()[2]
	g.Player(b).Lost = true
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, c)

	if got, want := g.AttackTarget(attacker), engine.PlayerEntity(c2); got != want {
		t.Errorf("AttackTarget() = %v, want %v (b has lost, only c is eligible)", got, want)
	}
}

// Attacking a planeswalker routes combat damage into its loyalty counters,
// not the defending player's life -- CR 120.3c.
func TestDealCombatDamageToAPlaneswalkerRemovesLoyalty(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.Player(a).Life, g.Player(b).Life = 20, 20
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "4", "4"), a, engine.Battlefield)
	pw := g.NewCard(planeswalkerDefLoyalty(t, "6"), b, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 6)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	ac.QueueAttackTarget(engine.CardEntity(pw))
	declareAttackers(t, g, ac)

	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)

	g.DealCombatDamage(engine.NewScriptedController())

	if got := g.Card(pw).Counters.Count(engine.Loyalty); got != 2 {
		t.Errorf("planeswalker loyalty = %d, want 2 (6 - 4)", got)
	}
	if g.Player(b).Life != 20 {
		t.Errorf("defender life = %d, want unchanged at 20 (the attacker hit the planeswalker, not the player)", g.Player(b).Life)
	}
}

// Combat damage removing loyalty emits CounterChanged with a negative amount
// -- the same event a gain would emit, sign flipped, not a separate kind for
// loss.
func TestDealCombatDamageToAPlaneswalkerEmitsCounterChanged(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.Player(a).Life, g.Player(b).Life = 20, 20
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "4", "4"), a, engine.Battlefield)
	pw := g.NewCard(planeswalkerDefLoyalty(t, "6"), b, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 6)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	ac.QueueAttackTarget(engine.CardEntity(pw))
	declareAttackers(t, g, ac)

	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)

	var sink recordingSink
	g.SetSink(&sink)

	g.DealCombatDamage(engine.NewScriptedController())

	var found *engine.Event
	for i := range sink.events {
		if sink.events[i].Kind == engine.CounterChanged {
			found = &sink.events[i]
		}
	}
	if found == nil {
		t.Fatalf("no CounterChanged event among %d emitted", len(sink.events))
	}
	if found.Source != attacker {
		t.Errorf("source %v, want the attacker %v", found.Source, attacker)
	}
	if found.Target != engine.CardEntity(pw) {
		t.Errorf("target %v, want the planeswalker %v", found.Target, engine.CardEntity(pw))
	}
	if found.Amount != -4 {
		t.Errorf("amount %d, want -4", found.Amount)
	}
	if found.Detail != uint32(engine.CounterDetailLoyalty) {
		t.Errorf("detail %d, want CounterDetailLoyalty", found.Detail)
	}
}

// Attacking a battle routes combat damage into its defense counters -- CR
// 121.5's combat-damage analogue to a planeswalker's loyalty.
func TestDealCombatDamageToABattleRemovesDefense(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.Player(a).Life, g.Player(b).Life = 20, 20
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "3", "3"), a, engine.Battlefield)
	battle := g.NewCard(battleDefDefense(t, "5"), a, engine.Battlefield)
	g.Card(battle).Counters.Add(engine.Defense, 5)
	g.Card(battle).ProtectingPlayer = b

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	ac.QueueAttackTarget(engine.CardEntity(battle))
	declareAttackers(t, g, ac)

	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)

	g.DealCombatDamage(engine.NewScriptedController())

	if got := g.Card(battle).Counters.Count(engine.Defense); got != 2 {
		t.Errorf("battle defense = %d, want 2 (5 - 3)", got)
	}
	if g.Player(b).Life != 20 {
		t.Errorf("defender life = %d, want unchanged at 20 (the attacker hit the battle, not the player)", g.Player(b).Life)
	}
}

// Trample excess against a planeswalker target lands on its loyalty, the
// same dispatch a trample excess against a player goes through -- CR
// 702.19b composed with CR 120.3c.
func TestDealCombatDamageTrampleExcessAgainstAPlaneswalkerRemovesLoyalty(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.Player(a).Life, g.Player(b).Life = 20, 20
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPTKeywords(t, "6", "6", "Trample"), a, engine.Battlefield)
	pw := g.NewCard(planeswalkerDefLoyalty(t, "6"), b, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 6)
	blocker := g.NewCard(creatureDefPT(t, "1", "2"), b, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	ac.QueueAttackTarget(engine.CardEntity(pw))
	declareAttackers(t, g, ac)

	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attacker}})
	declareBlockers(t, g, bc)

	g.DealCombatDamage(engine.NewScriptedController())

	if got := g.Card(blocker).Damage.Marked; got != 2 {
		t.Errorf("blocker damage = %d, want 2 (exactly lethal)", got)
	}
	if got := g.Card(pw).Counters.Count(engine.Loyalty); got != 2 {
		t.Errorf("planeswalker loyalty = %d, want 2 (6 - 4 trample excess)", got)
	}
}

// Blocker eligibility still resolves to the planeswalker's controller, not
// the planeswalker itself -- defenderOf's permanent branch (CR 802.4a).
func TestDeclareCombatBlockersOffersThePlaneswalkersControllersCreatures(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	pw := g.NewCard(planeswalkerDefLoyalty(t, "3"), b, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "1", "1"), b, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	ac.QueueAttackTarget(engine.CardEntity(pw))
	declareAttackers(t, g, ac)

	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attacker}})
	got := declareBlockers(t, g, bc)

	if len(got) != 1 || got[0].Blocker != blocker {
		t.Errorf("DeclareCombatBlockers() = %v, want a block naming %v", got, blocker)
	}
}
