package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// commandEffects is every card in p's Command zone -- where Effect puts the
// effect card it creates.
func commandEffects(g *engine.Game, p engine.PlayerID) []engine.CardID {
	return g.Zone(engine.Command, p).Cards()
}

// endTurn advances a game sitting in active's End of Turn step into its
// cleanup step, where end-of-turn effects end.
func endTurn(g *engine.Game, turn int, active engine.PlayerID) {
	g.SetTurnState(turn, active, engine.EndOfTurn)
	g.AdvancePhase(engine.NewScriptedController())
}

// TestEffectCardMakesRememberedCreatureUnblockableThisTurn proves the
// dominant resolvable shape: an effect card remembering a creature carries a
// Mode$ CantBlockBy static aimed at Creature.IsRemembered, active from the
// Command zone, and it ends at cleanup (no Duration$ is end of turn).
func TestEffectCardMakesRememberedCreatureUnblockableThisTurn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	host := resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Effect | RememberObjects$ Self | StaticAbilities$ Unblockable | ExileOnMoved$ Battlefield",
		"Unblockable", "Mode$ CantBlockBy | ValidAttacker$ Creature.IsRemembered | Description$ It can't be blocked this turn.")
	blocker := g.NewCard(creatureDef(t), other, engine.Battlefield)

	effects := commandEffects(g, p)
	if len(effects) != 1 {
		t.Fatalf("command zone holds %d cards, want 1 effect", len(effects))
	}
	eff := effects[0]
	if c := g.Card(eff); !c.IsEffect || c.Def.Name != "Test Shape's Effect" {
		t.Errorf("effect card = %+v, want an effect named Test Shape's Effect", c.Def.Name)
	}
	if g.CanBlock(host, blocker) {
		t.Error("CanBlock = true, want the effect to make the remembered creature unblockable")
	}

	endTurn(g, 1, p)
	if z := g.Card(eff).Zone; z != engine.None {
		t.Errorf("effect zone after cleanup = %v, want None (removed from the game)", z)
	}
	if !g.CanBlock(host, blocker) {
		t.Error("CanBlock = false after the effect ended, want true")
	}
}

// TestEffectCardExileOnMovedEndsWithTheCreature proves ExileOnMoved$: the
// effect goes to exile the moment its remembered creature leaves the named
// zone, even with Duration$ Permanent.
func TestEffectCardExileOnMovedEndsWithTheCreature(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	host := resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Effect | RememberObjects$ Self | StaticAbilities$ Unblockable | ExileOnMoved$ Battlefield | Duration$ Permanent",
		"Unblockable", "Mode$ CantBlockBy | ValidAttacker$ Creature.IsRemembered | Description$ It can't be blocked.")
	eff := commandEffects(g, p)[0]

	endTurn(g, 1, p)
	if z := g.Card(eff).Zone; z != engine.Command {
		t.Fatalf("Permanent effect zone after cleanup = %v, want Command", z)
	}
	g.Move(host, engine.Graveyard, p)
	if z := g.Card(eff).Zone; z != engine.None {
		t.Errorf("effect zone after its creature died = %v, want None (removed from the game)", z)
	}
}

// TestEffectCardForgetOnMovedExilesWhenNothingIsLeft proves ForgetOnMoved$:
// a remembered card leaving the zone is forgotten, and an effect left
// remembering no card is exiled.
func TestEffectCardForgetOnMovedExilesWhenNothingIsLeft(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	host := resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Effect | RememberObjects$ Self | StaticAbilities$ Unblockable | ForgetOnMoved$ Battlefield | Duration$ Permanent",
		"Unblockable", "Mode$ CantBlockBy | ValidAttacker$ Creature.IsRemembered | Description$ It can't be blocked.")
	eff := commandEffects(g, p)[0]
	if got := g.Card(eff).Memory.Remembered(); len(got) != 1 || got[0] != engine.CardEntity(host) {
		t.Fatalf("effect remembers %v, want the host", got)
	}
	g.Move(host, engine.Hand, p)
	if got := g.Card(eff).Memory.Remembered(); len(got) != 0 {
		t.Errorf("effect remembers %v after its card moved, want nothing", got)
	}
	if z := g.Card(eff).Zone; z != engine.None {
		t.Errorf("effect zone = %v, want None (removed from the game) once it remembers no card", z)
	}
}

// TestEffectCardAnthemAppliesFromCommandZone proves a Mode$ Continuous
// static on an effect card reaches the battlefield (Layer 7c) and, with
// Duration$ Permanent, outlives the turn.
func TestEffectCardAnthemAppliesFromCommandZone(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	host := resolveLine(t, g, p, c,
		"DB$ Effect | Name$ Emblem - Test | StaticAbilities$ Anthem | Duration$ Permanent",
		"Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Description$ Creatures you control get +1/+1.")
	theirs := g.NewCard(creatureDef(t), other, engine.Battlefield)

	endTurn(g, 1, p)
	engine.CheckStateBasedActions(g, c)
	if pw, _ := g.Card(host).Power(); pw != 2 {
		t.Errorf("host power = %d, want 2", pw)
	}
	if pw, _ := g.Card(theirs).Power(); pw != 2 {
		t.Errorf("opponent's creature power = %d, want its printed 2", pw)
	}
	if name := g.Card(commandEffects(g, p)[0]).Def.Name; name != "Emblem - Test" {
		t.Errorf("effect name = %q, want Name$'s", name)
	}
}

// TestEffectCardReplacementIsActiveInCommandZone proves an effect card's
// replacement applies from the Command zone although its script names no
// ActiveZones$ (a card's default is the battlefield): EffectEffect.java sets
// every trait's active zone to Command. EffectOwner$ gives the effect to the
// opponent, so "You" is them.
func TestEffectCardReplacementIsActiveInCommandZone(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Effect | EffectOwner$ Opponent | ReplacementEffects$ Fog | SubAbility$ DBDamage",
		"Fog", "Event$ DamageDone | Prevent$ True | ValidTarget$ You | Description$ Prevent all damage dealt to you this turn.",
		"DBDamage", "DB$ DealDamage | Defined$ Opponent | NumDmg$ 3")
	if n := len(commandEffects(g, other)); n != 1 {
		t.Fatalf("opponent's command zone holds %d, want the effect", n)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("opponent life = %d, want 20 -- the effect prevents the damage", got)
	}
}

// TestEffectCardTriggerFiresFromCommandZone proves an effect card's
// Triggers$ fire from the Command zone -- even a trigger whose own
// TriggerZones$ names the battlefield.
func TestEffectCardTriggerFiresFromCommandZone(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c,
		"DB$ Effect | Triggers$ TrigCast",
		"TrigCast", "Mode$ SpellCast | ValidActivatingPlayer$ You | TriggerZones$ Battlefield | Execute$ TrigGain | TriggerDescription$ Whenever you cast a spell this turn, gain 2 life.",
		"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 2")
	if _, err := castETBChain(t, g, p, etbChainDef(t, "Second", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23 (2 from the effect's cast trigger, 1 from the ETB)", got)
	}
}

// TestEffectCardUntilTheEndOfYourNextTurn proves the duration outlives the
// cleanup of the turn it was made in and of the opponent's turn, and ends at
// the cleanup of its controller's next turn.
func TestEffectCardUntilTheEndOfYourNextTurn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Effect | StaticAbilities$ Anthem | Duration$ UntilTheEndOfYourNextTurn",
		"Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | Description$ +1/+0.")
	eff := commandEffects(g, p)[0]

	endTurn(g, 1, p)
	endTurn(g, 2, other)
	if z := g.Card(eff).Zone; z != engine.Command {
		t.Fatalf("effect zone after two cleanups = %v, want Command", z)
	}
	endTurn(g, 3, p)
	if z := g.Card(eff).Zone; z != engine.None {
		t.Errorf("effect zone after its controller's next turn = %v, want None (removed from the game)", z)
	}
}

// TestEffectCardUntilYourNextTurn proves the effect lasts through the
// opponent's turn and ends as its controller's next turn begins.
func TestEffectCardUntilYourNextTurn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c,
		"DB$ Effect | StaticAbilities$ Anthem | Duration$ UntilYourNextTurn",
		"Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | Description$ +1/+0.")
	eff := commandEffects(g, p)[0]

	g.SetTurnState(1, p, engine.Cleanup)
	g.AdvancePhase(c)
	if g.ActivePlayer() != other || g.Card(eff).Zone != engine.Command {
		t.Fatalf("active = %v zone = %v, want the opponent's turn with the effect still in force", g.ActivePlayer(), g.Card(eff).Zone)
	}
	g.SetTurnState(2, other, engine.Cleanup)
	g.AdvancePhase(c)
	if g.ActivePlayer() != p {
		t.Fatalf("active = %v, want p", g.ActivePlayer())
	}
	if z := g.Card(eff).Zone; z != engine.None {
		t.Errorf("effect zone as its controller's turn began = %v, want None (removed from the game)", z)
	}
}

// TestEffectCardUntilEndOfCombat proves the effect ends when combat ends.
func TestEffectCardUntilEndOfCombat(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c,
		"DB$ Effect | StaticAbilities$ Anthem | Duration$ UntilEndOfCombat",
		"Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | Description$ +1/+0.")
	eff := commandEffects(g, p)[0]
	g.SetTurnState(1, p, engine.CombatDamage)
	g.AdvancePhase(c)
	g.AdvancePhase(c) // combat ends as the end of combat step does
	if z := g.Card(eff).Zone; z != engine.None {
		t.Errorf("effect zone at end of combat = %v, want None (removed from the game)", z)
	}
}

// TestEffectCardHostLeavesPlay proves UntilHostLeavesPlayOrEOT ends with
// the host leaving the battlefield.
func TestEffectCardHostLeavesPlay(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	host := resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Effect | StaticAbilities$ Anthem | Duration$ UntilHostLeavesPlayOrEOT",
		"Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | Description$ +1/+0.")
	eff := commandEffects(g, p)[0]
	g.Move(host, engine.Exile, p)
	if z := g.Card(eff).Zone; z != engine.None {
		t.Errorf("effect zone after its host left = %v, want None (removed from the game)", z)
	}
}

// TestEffectCardUniqueMakesOnePerPlayer proves Unique$: a player who already
// has an effect of that name gets no second one.
func TestEffectCardUniqueMakesOnePerPlayer(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	line := "DB$ Effect | Name$ Test Unique | Unique$ True | Duration$ Permanent"
	resolveLine(t, g, p, engine.NewScriptedController(), line)
	resolveLine(t, g, p, engine.NewScriptedController(), line)
	if n := len(commandEffects(g, p)); n != 1 {
		t.Errorf("command zone holds %d, want 1", n)
	}
}

// TestEffectCardCloneIsIndependent proves an effect card's lifetime is
// copied by Game.Clone and ending it on the clone leaves the original.
func TestEffectCardCloneIsIndependent(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Effect | StaticAbilities$ Anthem",
		"Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | Description$ +1/+0.")
	eff := commandEffects(g, p)[0]
	clone := g.Clone()
	endTurn(clone, 1, p)
	if z := clone.Card(eff).Zone; z != engine.None {
		t.Errorf("clone's effect zone = %v, want None (removed from the game)", z)
	}
	if z := g.Card(eff).Zone; z != engine.Command {
		t.Errorf("original's effect zone = %v, want Command", z)
	}
}

// TestEffectCardForgottenSurvivesWhileAnotherRememberedCardRemains proves
// effectRemembersAnyCard's other branch: with two remembered cards, one
// leaving the watched zone is forgotten but the effect stays, since it
// still remembers the other.
func TestEffectCardForgottenSurvivesWhileAnotherRememberedCardRemains(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	other := g.NewCard(creatureDef(t), p, engine.Battlefield)
	host, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(other)},
		"DB$ Effect | RememberObjects$ Self & Targeted | StaticAbilities$ Unblockable | ForgetOnMoved$ Battlefield | Duration$ Permanent",
		"Unblockable", "Mode$ CantBlockBy | ValidAttacker$ Creature.IsRemembered | Description$ It can't be blocked.")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	eff := commandEffects(g, p)[0]
	if got := g.Card(eff).Memory.Remembered(); len(got) != 2 {
		t.Fatalf("effect remembers %v, want 2 cards", got)
	}
	g.Move(host, engine.Hand, p)
	if got := g.Card(eff).Memory.Remembered(); len(got) != 1 || got[0] != engine.CardEntity(other) {
		t.Errorf("effect remembers %v after host moved, want just the other card", got)
	}
	if z := g.Card(eff).Zone; z != engine.Command {
		t.Errorf("effect zone = %v, want Command -- it still remembers a card", z)
	}
}

// TestEffectCardOwnerAndImprintAndChosenNumber proves EffectOwner$ (the
// effect lands in a different player's Command zone), ImprintCards$
// (Memory.Imprint) and SetChosenNumber$ (Memory.SetChosenNumber) each take.
func TestEffectCardOwnerAndImprintAndChosenNumber(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	host := resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Effect | EffectOwner$ Opponent | ImprintCards$ Self | SetChosenNumber$ 7 | Duration$ Permanent")
	if n := len(commandEffects(g, p)); n != 0 {
		t.Errorf("caster's command zone holds %d, want 0 -- EffectOwner$ Opponent", n)
	}
	effs := commandEffects(g, other)
	if len(effs) != 1 {
		t.Fatalf("opponent's command zone holds %d, want 1", len(effs))
	}
	eff := g.Card(effs[0])
	if got := eff.Memory.Imprinted(); len(got) != 1 || got[0] != host {
		t.Errorf("imprinted = %v, want [%v]", got, host)
	}
	if n, ok := eff.Memory.ChosenNumber(); !ok || n != 7 {
		t.Errorf("chosen number = %d, %v, want 7, true", n, ok)
	}
}

// TestEffectCardRememberObjectsUnresolvableSpecErrors proves a
// RememberObjects$ spec definedEntities cannot resolve fails the line
// rather than silently creating an effect that remembers nothing.
func TestEffectCardRememberObjectsUnresolvableSpecErrors(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ Effect | RememberObjects$ TriggeredCard")
	if err == nil || !strings.Contains(err.Error(), "RememberObjects$") {
		t.Errorf("err = %v, want a RememberObjects$ error", err)
	}
	if n := len(commandEffects(g, p)); n != 0 {
		t.Errorf("command zone holds %d, want no effect", n)
	}
}

// TestEffectFailsClosedOnMalformedShapes proves several of Effect's own
// error branches leave no effect card behind: an unmet Condition gate, a
// ForgetOnMoved$/ExileOnMoved$ zone name effectMoveWatch cannot parse, a
// RememberObjects$ spec resolving to an empty list while a move watch is
// armed (Java creates no effect when there is nothing to watch), an
// ImprintCards$ spec definedCards cannot resolve, a SetChosenNumber$
// expression that is not resolvable, and an EffectOwner$ spec
// definedPlayers cannot resolve.
func TestEffectFailsClosedOnMalformedShapes(t *testing.T) {
	t.Parallel()

	t.Run("ConditionUnmet", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
			"DB$ Effect | ConditionCheckSVar$ Armed | ConditionSVarCompare$ GE1 | Duration$ Permanent",
			"Armed", "Number$0")
		if err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if n := len(commandEffects(g, p)); n != 0 {
			t.Errorf("command zone holds %d, want 0 -- unmet condition", n)
		}
	})

	t.Run("BadMoveWatchZone", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
			"DB$ Effect | RememberObjects$ Self | ExileOnMoved$ Bogus | Duration$ Permanent")
		if err == nil || !strings.Contains(err.Error(), "not resolvable") {
			t.Errorf("err = %v, want a zone-not-resolvable error", err)
		}
		if n := len(commandEffects(g, p)); n != 0 {
			t.Errorf("command zone holds %d, want no effect", n)
		}
	})

	t.Run("EmptyRememberWithWatchCreatesNoEffect", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
			"DB$ Effect | RememberObjects$ Remembered | ForgetOnMoved$ Battlefield | Duration$ Permanent")
		if err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if n := len(commandEffects(g, p)); n != 0 {
			t.Errorf("command zone holds %d, want 0 -- RememberObjects$ resolved to nothing while a watch is armed", n)
		}
	})

	t.Run("ImprintCardsUnresolvable", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
			"DB$ Effect | ImprintCards$ TriggeredCard | Duration$ Permanent")
		if err == nil || !strings.Contains(err.Error(), "ImprintCards$") {
			t.Errorf("err = %v, want an ImprintCards$ error", err)
		}
		if n := len(commandEffects(g, p)); n != 0 {
			t.Errorf("command zone holds %d, want no effect", n)
		}
	})

	t.Run("SetChosenNumberUnresolvable", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
			"DB$ Effect | SetChosenNumber$ Bogus | Duration$ Permanent")
		if err == nil {
			t.Error("err = nil, want a SetChosenNumber$ error")
		}
		if n := len(commandEffects(g, p)); n != 0 {
			t.Errorf("command zone holds %d, want no effect", n)
		}
	})

	t.Run("EffectOwnerUnresolvable", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
			"DB$ Effect | EffectOwner$ TriggeredCard | Duration$ Permanent")
		if err == nil || !strings.Contains(err.Error(), "EffectOwner$") {
			t.Errorf("err = %v, want an EffectOwner$ error", err)
		}
		if n := len(commandEffects(g, p)); n != 0 {
			t.Errorf("command zone holds %d, want no effect", n)
		}
	})
}

// TestEffectRejectsUnportedParams proves the fail-closed gate: a param this
// port does not model, or a duration it does not track, fails the line
// before any effect card exists.
func TestEffectRejectsUnportedParams(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"DB$ Effect | ExileOnCounter$ P1P1",
		"DB$ Effect | Adventure$ True",
		"DB$ Effect | Duration$ UntilFacedown",
		"DB$ Effect | RememberKeywords$ Flying",
	} {
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, line)
		if err == nil || !strings.Contains(err.Error(), "not resolvable yet") {
			t.Errorf("%q: err = %v, want a not-resolvable-yet error", line, err)
		}
		if n := len(commandEffects(g, p)); n != 0 {
			t.Errorf("%q: command zone holds %d, want no effect", line, n)
		}
	}
}
