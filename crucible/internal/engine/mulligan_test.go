package engine_test

import (
	"fmt"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// scriptedMulliganController answers MulliganKeepHand from a fixed, per-player
// sequence and TuckCardsViaMulligan by always tucking the first N cards of
// whatever hand it is actually handed. Unlike ScriptedController's global
// queues, this does not require predicting which CardIDs a shuffle produces --
// exactly the reason the mulligan tests use a purpose-built fake rather than
// the engine's own ScriptedController.
type scriptedMulliganController struct {
	keep map[engine.PlayerID][]bool
	next map[engine.PlayerID]int
	// onDecide, if set, runs after computing an answer and before returning
	// it -- the hook TestPerformMulligansStopsWhenTheGameIsAlreadyOver uses
	// to make the game end as a side effect of one player's decision, the
	// only way to exercise the mid-loop Over() check without a real SBA
	// interaction inside the mulligan procedure itself.
	onDecide func()
}

func newScriptedMulliganController() *scriptedMulliganController {
	return &scriptedMulliganController{
		keep: map[engine.PlayerID][]bool{},
		next: map[engine.PlayerID]int{},
	}
}

func (c *scriptedMulliganController) queue(p engine.PlayerID, answers ...bool) {
	c.keep[p] = append(c.keep[p], answers...)
}

func (c *scriptedMulliganController) ChooseStartingPlayer(*engine.Game, engine.PlayerID, bool) engine.PlayerID {
	panic("not used by mulligan tests")
}

func (c *scriptedMulliganController) ChooseStartingHand(*engine.Game, engine.PlayerID, [][]engine.CardID) int {
	panic("not used by mulligan tests")
}

func (c *scriptedMulliganController) MulliganKeepHand(_ *engine.Game, decider, _ engine.PlayerID, _ int) bool {
	i := c.next[decider]
	if i >= len(c.keep[decider]) {
		panic(fmt.Sprintf("scriptedMulliganController: ran out of answers for player %d", decider))
	}
	c.next[decider] = i + 1
	answer := c.keep[decider][i]
	if c.onDecide != nil {
		c.onDecide()
	}
	return answer
}

func (c *scriptedMulliganController) TuckCardsViaMulligan(_ *engine.Game, _ engine.PlayerID, hand []engine.CardID, cardsToReturn int) []engine.CardID {
	return hand[:cardsToReturn]
}

// ChooseLegendaryToKeep, DeclareCombatAttackers, ChooseAttackTarget,
// DeclareCombatBlockers, AssignCombatDamage, DiscardToHandSize,
// ChooseCardsToDiscard, ArrangeForScry, ArrangeForSurveil, ChooseTargets, ChooseBattleProtector,
// ChooseHybridManaColor, ChoosePayMonocoloredHybrid,
// ChoosePayColorlessHybrid, ChoosePayPhyrexian, ChoosePayHybridPhyrexian,
// ChoosePayGeneric, ChoosePayX and ChoosePaySnow are never exercised by this controller's own tests -- no scenario here
// creates a legend-rule conflict, reaches combat, reaches cleanup with a
// hand over size, has a Battle needing a protector, or pays a mana cost --
// but the interface still has to be satisfied.
func (c *scriptedMulliganController) ChooseLegendaryToKeep(_ *engine.Game, _ engine.PlayerID, _ []engine.CardID) engine.CardID {
	panic("scriptedMulliganController: ChooseLegendaryToKeep was not expected to be called")
}

func (c *scriptedMulliganController) DeclareCombatAttackers(_ *engine.Game, _ engine.PlayerID, _ []engine.CardID) []engine.CardID {
	panic("scriptedMulliganController: DeclareCombatAttackers was not expected to be called")
}

func (c *scriptedMulliganController) ExertAttackers(_ *engine.Game, _ engine.PlayerID, _ []engine.CardID) []engine.CardID {
	panic("scriptedMulliganController: ExertAttackers was not expected to be called")
}

func (c *scriptedMulliganController) ChooseAttackTarget(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []engine.EntityID) engine.EntityID {
	panic("scriptedMulliganController: ChooseAttackTarget was not expected to be called")
}

func (c *scriptedMulliganController) DeclareCombatBlockers(_ *engine.Game, _ engine.PlayerID, _ []engine.CardID, _ []engine.CardID) []engine.Block {
	panic("scriptedMulliganController: DeclareCombatBlockers was not expected to be called")
}

func (c *scriptedMulliganController) AssignCombatDamage(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []engine.CardID) []engine.DamageAssignment {
	panic("scriptedMulliganController: AssignCombatDamage was not expected to be called")
}

func (c *scriptedMulliganController) DiscardToHandSize(_ *engine.Game, _ engine.PlayerID, _ []engine.CardID, _ int) []engine.CardID {
	panic("scriptedMulliganController: DiscardToHandSize was not expected to be called")
}

func (c *scriptedMulliganController) ChooseCardsToDiscard(_ *engine.Game, _ engine.PlayerID, _ []engine.CardID, _ int) []engine.CardID {
	panic("scriptedMulliganController: ChooseCardsToDiscard was not expected to be called")
}

func (c *scriptedMulliganController) ArrangeForScry(_ *engine.Game, _ engine.PlayerID, _ []engine.CardID) ([]engine.CardID, []engine.CardID) {
	panic("scriptedMulliganController: ArrangeForScry was not expected to be called")
}

func (c *scriptedMulliganController) ArrangeForSurveil(_ *engine.Game, _ engine.PlayerID, _ []engine.CardID) ([]engine.CardID, []engine.CardID) {
	panic("scriptedMulliganController: ArrangeForSurveil was not expected to be called")
}

func (c *scriptedMulliganController) ChooseTargets(_ *engine.Game, _ engine.PlayerID, _ []engine.EntityID, _, _ int) []engine.EntityID {
	panic("scriptedMulliganController: ChooseTargets was not expected to be called")
}

func (c *scriptedMulliganController) ChooseBattleProtector(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []engine.PlayerID) engine.PlayerID {
	panic("scriptedMulliganController: ChooseBattleProtector was not expected to be called")
}

func (c *scriptedMulliganController) ChooseHybridManaColor(_ *engine.Game, _ engine.PlayerID, _ mana.Colors) mana.Colors {
	panic("scriptedMulliganController: ChooseHybridManaColor was not expected to be called")
}

func (c *scriptedMulliganController) ChoosePayMonocoloredHybrid(_ *engine.Game, _ engine.PlayerID, _ mana.Colors, _ int) bool {
	panic("scriptedMulliganController: ChoosePayMonocoloredHybrid was not expected to be called")
}

func (c *scriptedMulliganController) ChoosePayColorlessHybrid(_ *engine.Game, _ engine.PlayerID, _ mana.Colors) bool {
	panic("scriptedMulliganController: ChoosePayColorlessHybrid was not expected to be called")
}

func (c *scriptedMulliganController) ChoosePayPhyrexian(_ *engine.Game, _ engine.PlayerID, _ mana.Colors) bool {
	panic("scriptedMulliganController: ChoosePayPhyrexian was not expected to be called")
}

func (c *scriptedMulliganController) ChoosePayHybridPhyrexian(_ *engine.Game, _ engine.PlayerID, _ mana.Colors) mana.Colors {
	panic("scriptedMulliganController: ChoosePayHybridPhyrexian was not expected to be called")
}

func (c *scriptedMulliganController) ChoosePayGeneric(_ *engine.Game, _ engine.PlayerID) mana.Shard {
	panic("scriptedMulliganController: ChoosePayGeneric was not expected to be called")
}

func (c *scriptedMulliganController) ChoosePayX(_ *engine.Game, _ engine.PlayerID, _ mana.Cost) int {
	panic("scriptedMulliganController: ChoosePayX was not expected to be called")
}

func (c *scriptedMulliganController) ChoosePaySnow(_ *engine.Game, _ engine.PlayerID) mana.Shard {
	panic("scriptedMulliganController: ChoosePaySnow was not expected to be called")
}

func (c *scriptedMulliganController) ChooseEnchantTarget(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []engine.CardID) engine.CardID {
	panic("scriptedMulliganController: ChooseEnchantTarget was not expected to be called")
}

func (c *scriptedMulliganController) ConfirmOptionalTrigger(_ *engine.Game, _ engine.PlayerID, _ engine.CardID) bool {
	panic("scriptedMulliganController: ConfirmOptionalTrigger was not expected to be called")
}

func (c *scriptedMulliganController) ConfirmPayCost(_ *engine.Game, _ engine.PlayerID, _ mana.Cost, _ engine.CardID) bool {
	panic("scriptedMulliganController: ConfirmPayCost was not expected to be called")
}

func (c *scriptedMulliganController) ChooseManaColor(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ mana.Colors) mana.Colors {
	panic("scriptedMulliganController: ChooseManaColor was not expected to be called")
}

func (c *scriptedMulliganController) ChoosePermanentsToSacrifice(_ *engine.Game, _ engine.PlayerID, _ []engine.CardID, _ int) []engine.CardID {
	panic("scriptedMulliganController: ChoosePermanentsToSacrifice was not expected to be called")
}

func (c *scriptedMulliganController) ChoosePermanentsToTap(_ *engine.Game, _ engine.PlayerID, _ []engine.CardID, _ int) []engine.CardID {
	panic("scriptedMulliganController: ChoosePermanentsToTap was not expected to be called")
}

func (c *scriptedMulliganController) ChoosePermanentsToReturn(_ *engine.Game, _ engine.PlayerID, _ []engine.CardID, _ int) []engine.CardID {
	panic("scriptedMulliganController: ChoosePermanentsToReturn was not expected to be called")
}

var _ engine.PlayerController = (*scriptedMulliganController)(nil)

// deciderSpyController wraps ScriptedController to record the decider
// ChooseStartingPlayer was actually asked with, then delegates to it for
// the answer -- embedding rather than a fresh 11-method stub, since every
// other method's behavior is exactly ScriptedController's own.
type deciderSpyController struct {
	*engine.ScriptedController
	decider engine.PlayerID
}

func (c *deciderSpyController) ChooseStartingPlayer(g *engine.Game, decider engine.PlayerID, isFirstGame bool) engine.PlayerID {
	c.decider = decider
	return c.ScriptedController.ChooseStartingPlayer(g, decider, isFirstGame)
}

// DealOpeningHands shuffles every player's library and deals each a
// startingHandSize hand.
func TestDealOpeningHandsDealsToEveryPlayer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	for i := 0; i < 10; i++ {
		g.NewCard(nil, a, engine.Library)
		g.NewCard(nil, b, engine.Library)
	}

	c := engine.NewScriptedController()
	c.QueueStartingPlayer(a)
	engine.DealOpeningHands(g, c)

	if got := g.Zone(engine.Hand, a).Len(); got != 7 {
		t.Errorf("a's hand = %d cards, want 7", got)
	}
	if got := g.Zone(engine.Library, a).Len(); got != 3 {
		t.Errorf("a's library = %d cards, want 3", got)
	}
	if got := g.Zone(engine.Hand, b).Len(); got != 7 {
		t.Errorf("b's hand = %d cards, want 7", got)
	}
	if got := g.Zone(engine.Library, b).Len(); got != 3 {
		t.Errorf("b's library = %d cards, want 3", got)
	}
}

// A library with fewer than startingHandSize cards deals what it has,
// rather than asking for more cards than exist -- the same defensive floor
// mulligan's own fresh-seven draw already applies.
func TestDealOpeningHandsWithAShortLibraryDealsWhatThereIs(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	for i := 0; i < 3; i++ {
		g.NewCard(nil, p, engine.Library)
	}

	c := engine.NewScriptedController()
	c.QueueStartingPlayer(p)
	engine.DealOpeningHands(g, c)

	if got := g.Zone(engine.Hand, p).Len(); got != 3 {
		t.Errorf("hand = %d cards, want 3", got)
	}
	if got := g.Zone(engine.Library, p).Len(); got != 0 {
		t.Errorf("library = %d cards, want 0", got)
	}
}

// The returned player is ChooseStartingPlayer's own answer, not necessarily
// the player the coin flip named as decider.
func TestDealOpeningHandsReturnsChooseStartingPlayerAnswer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]

	c := engine.NewScriptedController()
	c.QueueStartingPlayer(b)
	if got := engine.DealOpeningHands(g, c); got != b {
		t.Errorf("DealOpeningHands() = %v, want %v (queued answer, not necessarily the decider %v)", got, b, a)
	}
}

// The coin flip names one of the actual seated players as decider, never a
// zero value or a handle outside the game.
func TestDealOpeningHandsDeciderIsASeatedPlayer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]

	spy := &deciderSpyController{ScriptedController: engine.NewScriptedController()}
	spy.QueueStartingPlayer(a)
	engine.DealOpeningHands(g, spy)

	if spy.decider != a && spy.decider != b {
		t.Errorf("decider = %v, want %v or %v", spy.decider, a, b)
	}
}

func TestPerformMulligansEveryoneKeepsImmediately(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	var handA, handB []engine.CardID
	for i := 0; i < 7; i++ {
		handA = append(handA, g.NewCard(nil, a, engine.Hand))
		handB = append(handB, g.NewCard(nil, b, engine.Hand))
	}

	c := newScriptedMulliganController()
	c.queue(a, true)
	c.queue(b, true)
	engine.PerformMulligans(g, c, a)

	if got := g.Zone(engine.Hand, a).Cards(); !equalCardIDs(got, handA) {
		t.Errorf("a's hand %v, want unchanged %v", got, handA)
	}
	if got := g.Zone(engine.Hand, b).Cards(); !equalCardIDs(got, handB) {
		t.Errorf("b's hand %v, want unchanged %v", got, handB)
	}
}

// Heads-up London gives no free mulligan: the very first one costs a card.
func TestPerformMulligansOneMulliganTwoPlayer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	for i := 0; i < 7; i++ {
		g.NewCard(nil, a, engine.Hand)
	}
	for i := 0; i < 7; i++ {
		g.NewCard(nil, b, engine.Hand)
	}
	poolA := g.Zone(engine.Hand, a).Len() + g.Zone(engine.Library, a).Len()

	c := newScriptedMulliganController()
	c.queue(a, false, true)
	c.queue(b, true)
	engine.PerformMulligans(g, c, a)

	if got := g.Zone(engine.Hand, a).Len(); got != 6 {
		t.Errorf("a's hand has %d cards, want 6 (redrew 7, tucked 1)", got)
	}
	if got := g.Zone(engine.Hand, b).Len(); got != 7 {
		t.Errorf("b's hand has %d cards, want 7 (kept, untouched)", got)
	}
	if got := g.Zone(engine.Hand, a).Len() + g.Zone(engine.Library, a).Len(); got != poolA {
		t.Errorf("a's hand+library pool is %d, want %d -- a mulligan must not create or destroy cards", got, poolA)
	}
}

// CR 103.4: a game with more than two players gives everyone one free
// mulligan -- the first one costs nothing.
func TestPerformMulligansFirstIsFreeWithThreePlayers(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b", "c")
	a, b, cc := g.Players()[0], g.Players()[1], g.Players()[2]
	for _, p := range []engine.PlayerID{a, b, cc} {
		for i := 0; i < 7; i++ {
			g.NewCard(nil, p, engine.Hand)
		}
	}

	ctl := newScriptedMulliganController()
	ctl.queue(a, false, true)
	ctl.queue(b, true)
	ctl.queue(cc, true)
	engine.PerformMulligans(g, ctl, a)

	if got := g.Zone(engine.Hand, a).Len(); got != 7 {
		t.Errorf("a's hand has %d cards, want 7 -- the first mulligan in a 3-player game is free", got)
	}
}

// A hand can shrink all the way to zero, and a player can still choose to
// keep it -- London does not stop a player from mulliganing away everything.
func TestPerformMulligansCanReachAnEmptyHand(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	for i := 0; i < 7; i++ {
		g.NewCard(nil, a, engine.Hand)
	}
	for i := 0; i < 7; i++ {
		g.NewCard(nil, b, engine.Hand)
	}

	c := newScriptedMulliganController()
	// 7 mulligans at 1, 2, 3, 4, 5, 6, 7 cards tucked leaves 6, 5, 4, 3, 2, 1, 0.
	c.queue(a, false, false, false, false, false, false, false, true)
	c.queue(b, true)
	engine.PerformMulligans(g, c, a)

	if got := g.Zone(engine.Hand, a).Len(); got != 0 {
		t.Errorf("a's hand has %d cards, want 0", got)
	}
}

// canMulligan's cutoff stops offering once taking another mulligan would
// cost more than a hand can pay back -- the last offer is answered, but the
// one after it never asks at all.
func TestPerformMulligansStopsOfferingPastTheCutoff(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	for i := 0; i < 7; i++ {
		g.NewCard(nil, a, engine.Hand)
	}
	for i := 0; i < 7; i++ {
		g.NewCard(nil, b, engine.Hand)
	}

	c := newScriptedMulliganController()
	// 8 mulligans in a row: canMulligan is still true for the 8th ask (tuck
	// computed from 7 prior mulligans is 7, right at the boundary), but the
	// 9th is never asked -- tuck computed from 8 prior mulligans is 8, past
	// it. Only 8 answers are queued; if a 9th ask happened, the controller
	// would panic on an empty queue and fail the test that way.
	c.queue(a, false, false, false, false, false, false, false, false)
	c.queue(b, true)
	engine.PerformMulligans(g, c, a)

	if got := g.Zone(engine.Hand, a).Len(); got != 0 {
		t.Errorf("a's hand has %d cards, want 0 (8th mulligan tucks every card of a fresh 7, clamped)", got)
	}
}

// If the game ends as a side effect of one player's decision -- nothing
// currently causes that inside the mulligan procedure itself, but a future
// caller answering a decision could concede, and Java's own loop checks for
// exactly this after every ask -- the procedure stops immediately rather
// than asking the next player to decide something in a game that is no
// longer being played.
func TestPerformMulligansStopsWhenTheGameEndsMidLoop(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.Player(a).Life, g.Player(b).Life = 20, 20
	for i := 0; i < 7; i++ {
		g.NewCard(nil, a, engine.Hand)
	}

	c := newScriptedMulliganController()
	c.queue(a, true)
	c.onDecide = func() {
		g.Player(a).Life, g.Player(b).Life = 0, 0
		engine.CheckStateBasedActions(g, c)
	}
	// b has no answers queued: if the loop reached b after a's decision
	// ended the game, this would panic.
	engine.PerformMulligans(g, c, a)

	if !g.Over() {
		t.Fatal("setup: the decision hook did not end the game")
	}
}

func equalCardIDs(a, b []engine.CardID) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

func (c *scriptedMulliganController) ChooseCardsForEffect(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []engine.CardID, _, _ int) []engine.CardID {
	panic("scriptedMulliganController: ChooseCardsForEffect was not expected to be called")
}

func (c *scriptedMulliganController) ChoosePlayerForEffect(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []engine.PlayerID) engine.PlayerID {
	panic("scriptedMulliganController: ChoosePlayerForEffect was not expected to be called")
}

func (c *scriptedMulliganController) ChooseColors(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ mana.Colors, _, _ int) mana.Colors {
	panic("scriptedMulliganController: ChooseColors was not expected to be called")
}

func (c *scriptedMulliganController) ChooseNumber(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _, _ int) int {
	panic("scriptedMulliganController: ChooseNumber was not expected to be called")
}

func (c *scriptedMulliganController) ChooseTapOrUntap(_ *engine.Game, _ engine.PlayerID, _ engine.CardID) bool {
	panic("scriptedMulliganController: ChooseTapOrUntap was not expected to be called")
}

func (c *scriptedMulliganController) ChooseEntitiesForEffect(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []engine.EntityID, _, _ int) []engine.EntityID {
	panic("scriptedMulliganController: ChooseEntitiesForEffect was not expected to be called")
}

func (c *scriptedMulliganController) ConfirmReveal(_ *engine.Game, _ engine.PlayerID, _ engine.CardID) bool {
	panic("scriptedMulliganController: ConfirmReveal was not expected to be called")
}

func (c *scriptedMulliganController) ConfirmEffect(_ *engine.Game, _ engine.PlayerID, _ engine.CardID) bool {
	panic("scriptedMulliganController: ConfirmEffect was not expected to be called")
}

func (c *scriptedMulliganController) OrderCardsForZone(_ *engine.Game, _ engine.PlayerID, _ []engine.CardID, _ engine.ZoneType) []engine.CardID {
	panic("scriptedMulliganController: OrderCardsForZone was not expected to be called")
}

func (c *scriptedMulliganController) ChooseModesForAbility(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []string, _, _ int) []int {
	panic("scriptedMulliganController: ChooseModesForAbility was not expected to be called")
}

func (c *scriptedMulliganController) ChooseProtectionType(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []string) int {
	panic("scriptedMulliganController: ChooseProtectionType was not expected to be called")
}

func (c *scriptedMulliganController) ChooseBinary(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ engine.BinaryChoice) bool {
	panic("scriptedMulliganController: ChooseBinary was not expected to be called")
}

func (c *scriptedMulliganController) ChooseOption(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []string) int {
	panic("scriptedMulliganController: ChooseOption was not expected to be called")
}

func (c *scriptedMulliganController) ChooseSector(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []string) int {
	panic("scriptedMulliganController: ChooseSector was not expected to be called")
}

func (c *scriptedMulliganController) ChooseRoomDoor(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []engine.Door) engine.Door {
	panic("scriptedMulliganController: ChooseRoomDoor was not expected to be called")
}

func (c *scriptedMulliganController) CallCoinFlip(_ *engine.Game, _ engine.PlayerID, _ engine.CardID) bool {
	panic("scriptedMulliganController: CallCoinFlip was not expected to be called")
}

func (c *scriptedMulliganController) WillPutCardOnTop(_ *engine.Game, _ engine.PlayerID, _ engine.CardID) bool {
	panic("scriptedMulliganController: WillPutCardOnTop was not expected to be called")
}

func (c *scriptedMulliganController) ChooseAbilitiesForEffect(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, _ []string, _ int) []int {
	panic("scriptedMulliganController: ChooseAbilitiesForEffect was not expected to be called")
}

func (c *scriptedMulliganController) TakeAction(_ *engine.Game, _ engine.PlayerID) engine.Action {
	panic("scriptedMulliganController: TakeAction was not expected to be called")
}
