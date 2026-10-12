package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// etbDiscardTriggerDefParams builds a *compile.Card whose own "when
// CARDNAME enters" trigger runs DB$ Discard with the given params string
// appended -- discardEffect itself is unexported, so every case here is
// driven through the real cast+resolve pipeline rather than calling it
// directly (TEST-1), the identical reason etbPutCounterTriggerDefParams
// (putcountereffect_test.go) is.
func etbDiscardTriggerDefParams(t *testing.T, name, extraParams string, svars map[string]string) *compile.Card {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "1", "1"
	raw.Faces[0].ManaCost = mana.MustParse("G")
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigDiscard",
	}
	raw.Faces[0].SVars.Set("TrigDiscard", "DB$ Discard | "+extraParams)
	for name, body := range svars {
		raw.Faces[0].SVars.Set(name, body)
	}

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBDiscard casts def (built by etbDiscardTriggerDefParams) for p on
// controller c and resolves the stack, returning the creature's own CardID
// and ResolveStack's own error. c is passed in, rather than built fresh the
// way castETBPutCounter's own does, because a scripted discard choice has to
// be queued on it before this runs.
func castETBDiscard(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, c *engine.ScriptedController) (engine.CardID, error) {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return creature, g.ResolveStack(engine.NewRegistry(), c)
}

// TestDiscardEffectDefinedYouChoosesFromOwnHand proves the corpus's single
// largest real shape: Mode$ TgtChoose | Defined$ You asks the ability's own
// controller which of their own hand to discard through the new
// ChooseCardsToDiscard decision, then moves the chosen cards to their
// owner's graveyard.
func TestDiscardEffectDefinedYouChoosesFromOwnHand(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	keep := g.NewCard(nil, p, engine.Hand)
	toss := g.NewCard(nil, p, engine.Hand)

	c := engine.NewScriptedController()
	c.QueueDiscardChoice([]engine.CardID{toss})

	if _, err := castETBDiscard(t, g, p, etbDiscardTriggerDefParams(t, "Test Defined You", "Mode$ TgtChoose | Defined$ You | NumCards$ 1", nil), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(toss).Zone != engine.Graveyard {
		t.Errorf("discarded card zone = %v, want Graveyard", g.Card(toss).Zone)
	}
	if g.Card(keep).Zone != engine.Hand {
		t.Errorf("kept card zone = %v, want Hand", g.Card(keep).Zone)
	}
}

// TestDiscardEffectDefaultNumCardsIsOne proves NumCards$'s own Java default
// -- calculateAmount is never reached without the param, but the resolve
// loop's own default of "1" mirrors AbilityUtils' "no NumCards$ means one
// card" reading -- rather than treating an absent NumCards$ as zero.
func TestDiscardEffectDefaultNumCardsIsOne(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	toss := g.NewCard(nil, p, engine.Hand)

	c := engine.NewScriptedController()
	c.QueueDiscardChoice([]engine.CardID{toss})

	if _, err := castETBDiscard(t, g, p, etbDiscardTriggerDefParams(t, "Test Default NumCards", "Mode$ TgtChoose | Defined$ You", nil), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(toss).Zone != engine.Graveyard {
		t.Errorf("discarded card zone = %v, want Graveyard -- NumCards$'s own default is 1, not 0", g.Card(toss).Zone)
	}
}

// TestDiscardEffectDefinedOpponentDiscardsFromTheirHand proves the
// player-shaped Defined$ dispatch reaches the opponent's own hand, not the
// caster's.
func TestDiscardEffectDefinedOpponentDiscardsFromTheirHand(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(opp).Life = 20, 20
	oppCard := g.NewCard(nil, opp, engine.Hand)
	myCard := g.NewCard(nil, p, engine.Hand)

	c := engine.NewScriptedController()
	c.QueueDiscardChoice([]engine.CardID{oppCard})

	if _, err := castETBDiscard(t, g, p, etbDiscardTriggerDefParams(t, "Test Defined Opponent", "Mode$ TgtChoose | Defined$ Opponent | NumCards$ 1", nil), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(oppCard).Zone != engine.Graveyard {
		t.Errorf("opponent's card zone = %v, want Graveyard", g.Card(oppCard).Zone)
	}
	if g.Card(myCard).Zone != engine.Hand {
		t.Errorf("caster's own card zone = %v, want Hand -- Defined$ Opponent must not touch it", g.Card(myCard).Zone)
	}
}

// TestDiscardEffectDefinedPlayerAsksEveryPlayer proves the new definedPlayers
// "Player" case (defined.go): "each player discards a card" (rotting_rats.txt's
// own real shape) asks every player in turn order, not just the caster, and
// each answers its own queued choice independently.
func TestDiscardEffectDefinedPlayerAsksEveryPlayer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(opp).Life = 20, 20
	myToss := g.NewCard(nil, p, engine.Hand)
	oppToss := g.NewCard(nil, opp, engine.Hand)

	c := engine.NewScriptedController()
	c.QueueDiscardChoice([]engine.CardID{myToss})
	c.QueueDiscardChoice([]engine.CardID{oppToss})

	if _, err := castETBDiscard(t, g, p, etbDiscardTriggerDefParams(t, "Test Defined Player", "Mode$ TgtChoose | Defined$ Player | NumCards$ 1", nil), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(myToss).Zone != engine.Graveyard {
		t.Errorf("caster's own card zone = %v, want Graveyard", g.Card(myToss).Zone)
	}
	if g.Card(oppToss).Zone != engine.Graveyard {
		t.Errorf("opponent's card zone = %v, want Graveyard -- Defined$ Player reaches every player", g.Card(oppToss).Zone)
	}
}

// TestDiscardEffectClampsCountToHandSize proves NumCards$ is clamped to the
// discarding player's actual hand size, matching Java's own
// `Math.min(numCards, numCardsInHand)`, rather than asking the controller for
// more cards than the hand holds.
func TestDiscardEffectClampsCountToHandSize(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	toss := g.NewCard(nil, p, engine.Hand)

	c := engine.NewScriptedController()
	c.QueueDiscardChoice([]engine.CardID{toss})

	if _, err := castETBDiscard(t, g, p, etbDiscardTriggerDefParams(t, "Test Clamp", "Mode$ TgtChoose | Defined$ You | NumCards$ 5", nil), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(toss).Zone != engine.Graveyard {
		t.Errorf("discarded card zone = %v, want Graveyard", g.Card(toss).Zone)
	}
}

// TestDiscardEffectSkipsControllerWhenHandEmpty proves an empty hand skips
// the ChooseCardsToDiscard call entirely (Java's own `if (dPHand.isEmpty())
// continue`), rather than asking for zero cards and consuming a queued
// answer that was never provided.
func TestDiscardEffectSkipsControllerWhenHandEmpty(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	c := engine.NewScriptedController()
	// No QueueDiscardChoice call: the empty hand (the creature just cast is
	// on the stack, not in hand, by the time the ETB trigger resolves) must
	// never reach the controller, or this panics on an exhausted queue.
	if _, err := castETBDiscard(t, g, p, etbDiscardTriggerDefParams(t, "Test Empty Hand", "Mode$ TgtChoose | Defined$ You | NumCards$ 1", nil), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
}

// TestDiscardEffectResolvesNamedNumCardsSVar proves NumCards$ resolves a
// named SVar reference through resolveNamedAmount (amount.go), not just a
// plain integer.
func TestDiscardEffectResolvesNamedNumCardsSVar(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	toss1 := g.NewCard(nil, p, engine.Hand)
	toss2 := g.NewCard(nil, p, engine.Hand)

	c := engine.NewScriptedController()
	c.QueueDiscardChoice([]engine.CardID{toss1, toss2})
	c.QueueCardOrder([]engine.CardID{toss1, toss2})

	def := etbDiscardTriggerDefParams(t, "Test Named NumCards", "Mode$ TgtChoose | Defined$ You | NumCards$ X", map[string]string{"X": "2"})
	if _, err := castETBDiscard(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(toss1).Zone != engine.Graveyard || g.Card(toss2).Zone != engine.Graveyard {
		t.Errorf("discarded card zones = %v, %v, want both Graveyard -- NumCards$ X must resolve through the named SVar X:2", g.Card(toss1).Zone, g.Card(toss2).Zone)
	}
}

// TestDiscardEffectRejectsMissingMode proves an absent Mode$ (required on
// every real corpus line) is a real error rather than a silent guess at
// which mode was meant (GO-7).
func TestDiscardEffectRejectsMissingMode(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	c := engine.NewScriptedController()
	_, err := castETBDiscard(t, g, p, etbDiscardTriggerDefParams(t, "Test No Mode", "Defined$ You | NumCards$ 1", nil), c)
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming Mode")
	}
	if !strings.Contains(err.Error(), "Mode") {
		t.Errorf("ResolveStack error = %q, want it to name Mode$", err.Error())
	}
}

// TestDiscardEffectRejectsUnsupportedMode proves a Mode$ DiscardEffect.java
// does not know fails loudly by name rather than falling through to
// TgtChoose's own behavior.
func TestDiscardEffectRejectsUnsupportedMode(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20

	c := engine.NewScriptedController()
	_, err := castETBDiscard(t, g, p, etbDiscardTriggerDefParams(t, "Test Bogus Mode", "Mode$ Bogus | Defined$ You | NumCards$ 1", nil), c)
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming Mode")
	}
	if !strings.Contains(err.Error(), "Mode") {
		t.Errorf("ResolveStack error = %q, want it to name Mode$", err.Error())
	}
}

// TestDiscardEffectChainsIntoSubAbility proves SubAbility$ no longer
// blocks Discard's own resolution now that resolveSubAbility (subability.go)
// exists: the chained GainLife runs too, not just Discard's own body.
func TestDiscardEffectChainsIntoSubAbility(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	toss := g.NewCard(nil, p, engine.Hand)

	def := etbDiscardTriggerDefParams(t, "Test SubAbility",
		"Mode$ TgtChoose | Defined$ You | NumCards$ 1 | SubAbility$ DBGainLife",
		map[string]string{"DBGainLife": "DB$ GainLife | Defined$ You | LifeAmount$ 2"})
	c := engine.NewScriptedController()
	c.QueueDiscardChoice([]engine.CardID{toss})
	if _, err := castETBDiscard(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(toss).Zone != engine.Graveyard {
		t.Errorf("toss zone = %v, want Graveyard -- Discard's own body must still run", g.Card(toss).Zone)
	}
	if g.Player(p).Life != 22 {
		t.Errorf("p's life = %d, want 22 -- the chained GainLife must run too", g.Player(p).Life)
	}
}

// TestDiscardEffectTargetedPlayerDiscards proves a ValidTgts$ trigger, its
// target chosen by resolveTargets (targeting.go) when it goes on the stack,
// makes the target player discard -- getTargetPlayers, not the controller.
func TestDiscardEffectTargetedPlayerDiscards(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(opp).Life = 20, 20
	oppCard := g.NewCard(nil, opp, engine.Hand)

	def := etbDiscardTriggerDefParams(t, "Test ValidTgts", "Mode$ TgtChoose | NumCards$ 1 | ValidTgts$ Opponent", nil)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(opp)})
	c.QueueDiscardChoice([]engine.CardID{oppCard})
	if _, err := castETBDiscard(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(oppCard).Zone != engine.Graveyard {
		t.Errorf("target's card zone = %v, want Graveyard", g.Card(oppCard).Zone)
	}
}

// TestDiscardEffectFiresWhenConditionCheckSVarIsMet proves
// subAbilityConditionMet (condition.go) gates Discard's own resolution the
// identical way it gates PutCounter's/Pump's/DealDamage's/GainLife's.
func TestDiscardEffectFiresWhenConditionCheckSVarIsMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	toss := g.NewCard(nil, p, engine.Hand)

	c := engine.NewScriptedController()
	c.QueueDiscardChoice([]engine.CardID{toss})

	def := etbDiscardTriggerDefParams(t, "Test Condition Met",
		"Mode$ TgtChoose | Defined$ You | NumCards$ 1 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", map[string]string{"X": "1"})
	if _, err := castETBDiscard(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(toss).Zone != engine.Graveyard {
		t.Errorf("discarded card zone = %v, want Graveyard -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 holds (X is 1)", g.Card(toss).Zone)
	}
}

// TestDiscardEffectNoOpsWhenConditionCheckSVarIsNotMet proves the negative
// control: X GE1 fails (X is 0), so the ability does nothing -- and, in
// particular, never reaches the controller at all.
func TestDiscardEffectNoOpsWhenConditionCheckSVarIsNotMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	kept := g.NewCard(nil, p, engine.Hand)

	c := engine.NewScriptedController()
	// No QueueDiscardChoice call: the unmet condition must never reach the
	// controller, or this panics on an exhausted queue.
	def := etbDiscardTriggerDefParams(t, "Test Condition Unmet",
		"Mode$ TgtChoose | Defined$ You | NumCards$ 1 | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", map[string]string{"X": "0"})
	if _, err := castETBDiscard(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(kept).Zone != engine.Hand {
		t.Errorf("kept card zone = %v, want Hand -- ConditionCheckSVar$ X | ConditionSVarCompare$ GE1 fails (X is 0)", g.Card(kept).Zone)
	}
}
