package smoke_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/cost"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/smoke"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// Every decision has a legal default answer: a table over the whole
// PlayerController surface, because a controller that panics on one decision
// makes the cards that ask it unmeasurable.
func TestControllerAnswersEveryDecisionLegally(t *testing.T) {
	t.Parallel()

	db := corpusDB(t)
	g := engine.NewGame(db, javarand.New(1), []string{"a", "b"})
	p, o := g.Players()[0], g.Players()[1]
	bears, _ := db.Card("Grizzly Bears")
	a, b := g.NewCard(bears, p, engine.Hand), g.NewCard(bears, p, engine.Hand)
	hand := []engine.CardID{a, b}
	g.Player(p).ManaPool.Add(mana.Red, 3)
	g.Player(p).ManaPool.AddColorless(1)
	c := &smoke.Controller{}

	if c.ChooseStartingPlayer(g, p, true) != p || c.ChooseStartingHand(g, p, [][]engine.CardID{hand}) != 0 {
		t.Error("starting player or hand")
	}
	if !c.MulliganKeepHand(g, p, p, 0) || len(c.TuckCardsViaMulligan(g, p, hand, 1)) != 1 || c.ChooseLegendaryToKeep(g, p, hand) != a {
		t.Error("mulligan answers")
	}
	if got := c.DeclareCombatAttackers(g, p, hand); len(got) != 2 {
		t.Errorf("attackers = %v, want all", got)
	}
	if c.ExertAttackers(g, p, hand) != nil || c.DeclareCombatBlockers(g, o, hand, hand) != nil {
		t.Error("exert or block")
	}
	if got := c.ChooseAttackTarget(g, p, a, []engine.EntityID{engine.PlayerEntity(o)}); got != engine.PlayerEntity(o) {
		t.Errorf("attack target = %v", got)
	}
	if got := c.DivideAmongTargets(g, p, a, []engine.EntityID{engine.CardEntity(b), engine.PlayerEntity(o)}, 5); len(got) != 2 || got[0] != 4 || got[1] != 1 {
		t.Errorf("division = %v, want [4 1]: one each, the rest to the first", got)
	}
	if got := c.DivideAmongTargets(g, p, a, nil, 3); len(got) != 0 {
		t.Errorf("division over no targets = %v, want none", got)
	}
	if got := c.AssignCombatDamage(g, p, a, hand); len(got) != 2 || got[0].Amount != 1 {
		t.Errorf("damage assignment = %v", got)
	}
	if len(c.DiscardToHandSize(g, p, hand, 1)) != 1 || len(c.ChooseCardsToDiscard(g, p, hand, 5)) != 2 {
		t.Error("discards are clipped to the hand")
	}
	if top, bottom := c.ArrangeForScry(g, p, hand); len(top) != 2 || bottom != nil {
		t.Error("scry keeps cards on top")
	}
	if top, gy := c.ArrangeForSurveil(g, p, hand); len(top) != 2 || gy != nil {
		t.Error("surveil keeps cards on top")
	}
	for _, f := range []func(*engine.Game, engine.PlayerID, []engine.CardID, int) []engine.CardID{
		c.ChoosePermanentsToSacrifice, c.ChoosePermanentsToTap, c.ChoosePermanentsToReturn,
	} {
		if got := f(g, p, hand, 1); len(got) != 1 {
			t.Errorf("permanent choice = %v, want one", got)
		}
	}
	targets := []engine.EntityID{engine.PlayerEntity(p), engine.PlayerEntity(o)}
	if got := c.ChooseTargets(g, p, targets, 0, 2); len(got) != 1 {
		t.Errorf("optional targets = %v, want one", got)
	}
	if got := c.ChooseTargets(g, p, targets, 2, 2); len(got) != 2 {
		t.Errorf("required targets = %v, want both", got)
	}
	if got := c.ChooseTargets(g, p, nil, 0, 1); len(got) != 0 {
		t.Errorf("no targets available = %v", got)
	}
	if c.ChooseBattleProtector(g, p, a, []engine.PlayerID{o}) != o {
		t.Error("battle protector")
	}

	both := mana.Red | mana.Green
	if c.ChooseHybridManaColor(g, p, both) != mana.Red || c.ChooseManaColor(g, p, a, both) != mana.Red {
		t.Error("colour choices take the first bit")
	}
	if !c.ChoosePayMonocoloredHybrid(g, p, mana.Red, 2) || !c.ChoosePayColorlessHybrid(g, p, mana.Red) || c.ChoosePayPhyrexian(g, p, mana.Red) {
		t.Error("hybrid and Phyrexian payment")
	}
	if c.ChoosePayHybridPhyrexian(g, p, both) != mana.Red {
		t.Error("hybrid Phyrexian colour")
	}
	if c.ChoosePayGeneric(g, p) != mana.ShardR || c.ChoosePaySnow(g, p) != mana.ShardR || c.ChoosePayX(g, p, mana.Cost{}) != 2 {
		t.Error("generic payment uses the colour with most mana")
	}
	if c.ChooseEnchantTarget(g, p, a, hand) != a {
		t.Error("enchant target")
	}
	if got := c.ChooseCardsForEffect(g, p, a, hand, 0, 2); len(got) != 1 {
		t.Errorf("cards for effect = %v", got)
	}
	if c.ChoosePlayerForEffect(g, p, a, []engine.PlayerID{o}) != o {
		t.Error("player for effect")
	}
	if got := c.ChooseColors(g, p, a, mana.AllColors, 2, 3); got.Count() != 2 {
		t.Errorf("colours = %v, want two", got)
	}
	if c.ChooseNumber(g, p, a, 0, 9) != 2 || c.ChooseNumber(g, p, a, 5, 9) != 5 || c.ChooseNumber(g, p, a, 0, 1) != 1 {
		t.Error("numbers are 2 clamped to the range")
	}
	if got := c.ChooseEntitiesForEffect(g, p, a, targets, 1, 2); len(got) != 1 {
		t.Errorf("entities = %v", got)
	}
	if got := c.OrderCardsForZone(g, p, hand, engine.Library); len(got) != 2 {
		t.Errorf("order = %v", got)
	}
	if got := c.ChooseAbilitiesForEffect(g, p, a, []string{"x", "y"}, 5); len(got) != 2 {
		t.Errorf("abilities = %v", got)
	}
	if one, two := c.ChooseAbilitiesForEffect(g, 0, a, []string{"x", "y", "z"}, 1), c.ChooseAbilitiesForEffect(g, 1, a, []string{"x", "y", "z"}, 1); one[0] == two[0] {
		t.Errorf("two players picked alike: %v %v", one, two)
	}
	if got := c.ChooseModesForAbility(g, p, a, []string{"x", "y", "z"}, 2, 2); len(got) != 2 {
		t.Errorf("modes = %v", got)
	}
	if c.ChooseProtectionType(g, p, a, []string{"x"}) != 0 || c.ChooseOption(g, p, a, []string{"x"}) != 0 ||
		c.ChooseSector(g, p, a, []string{"x"}) != 0 || c.ChooseReplacementEffect(g, p, nil) != 0 {
		t.Error("index choices take the first option")
	}
	if c.ChooseRoomDoor(g, p, a, []engine.Door{engine.DoorRight, engine.DoorLeft}) != engine.DoorRight {
		t.Error("room door")
	}
	if (c.TakeAction(g, p) != engine.Action{}) {
		t.Error("TakeAction should pass")
	}
}

// The yes answers are budgeted: "you may repeat" answered yes forever loops the
// engine, so after the budget every optional question is answered no.
func TestControllerYesBudgetRunsOut(t *testing.T) {
	t.Parallel()

	g := engine.NewGame(corpusDB(t), javarand.New(1), []string{"a", "b"})
	p := g.Players()[0]
	c := &smoke.Controller{}
	answers := []func() bool{
		func() bool { return c.ConfirmOptionalTrigger(g, p, engine.NoCard) },
		func() bool { return c.ConfirmPayCost(g, p, cost.Cost{}, engine.NoCard) },
		func() bool { return c.ConfirmReveal(g, p, engine.NoCard) },
		func() bool { return c.ConfirmEffect(g, p, engine.NoCard) },
		func() bool { return c.ConfirmReplacementEffect(g, p, engine.NoCard, "") },
		func() bool { return c.WillPutCardOnTop(g, p, engine.NoCard) },
		func() bool { return c.ChooseBinary(g, p, engine.NoCard, engine.TapOrUntap) },
		func() bool { return c.CallCoinFlip(g, p, engine.NoCard) },
		func() bool { return c.ChooseTapOrUntap(g, p, engine.NoCard) },
	}
	for i, ask := range answers {
		if !ask() {
			t.Errorf("question %d: the first answer should be yes", i)
		}
	}
	yes := len(answers)
	for yes < 1000 && answers[yes%len(answers)]() {
		yes++
	}
	if yes < 20 || yes > 100 {
		t.Errorf("answered yes %d times before saying no, want a budget of a few dozen", yes)
	}
}
