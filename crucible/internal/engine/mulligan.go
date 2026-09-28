// Mulligans: the pre-game procedure for replacing an opening hand.

package engine

// startingHandSize is CR 103.4's seven. Nothing that could change it --
// Font of Mythos, Vancouver's own scry step, a starting-hand-size static
// ability -- is built yet, so it is a constant rather than a Player field
// invented ahead of a caller that would set it.
const startingHandSize = 7

// startingLife is CR 103.3's twenty: RegisteredPlayer.startingLife's own
// default (RegisteredPlayer.java:25), what Game's constructor hands every
// seat when no StartingLife was forced (Game.java:347-351). The variants
// that raise it (Commander, Archenemy, Vanguard, RegisteredPlayer.java:137-171)
// have no game setup in this port, so it is a constant for the same reason
// startingHandSize is. Subgame's seats start at it (subgameeffect.go).
const startingLife = 20

// DealOpeningHands is CR 103.1-103.4's procedure up to the point
// PerformMulligans, below, can run: decide who plays first and deal each
// seated player an opening hand of startingHandSize, shuffled library first.
// Ported from GameAction.startGame with the Match-level parts trimmed --
// this port has no Match, so there is no previous game's loser to name and
// every game reaches CR 103.2's coin flip the same way
// (determineFirstTurnPlayer's own isFirstGame branch, Java's
// chooseStartingPlayer(isFirstGame) always called with true here).
//
// The coin flip is g.rand's own Int32n(len(players)) -- Aggregates.random's
// algorithm for a List source, which game.getPlayers() is, not a new
// derivation.
//
// The returned PlayerID is ChooseStartingPlayer's own answer, who actually
// goes first, not necessarily the player asked to decide. Calling
// PerformMulligans with it, and calling Game.StartTurn once mulligans are
// done, are both left to the caller: this port has no Match-level "play a
// whole game" loop for DealOpeningHands to be one step inside of yet, so
// there is no single flow to hand the return value to automatically.
func DealOpeningHands(g *Game, controller PlayerController) PlayerID {
	players := g.Players()
	decider := players[g.rand.Int32n(int32(len(players)))]
	first := controller.ChooseStartingPlayer(g, decider, true)

	for _, pid := range players {
		g.Shuffle(Library, pid)
		drawOpeningHand(g, pid)
	}
	return first
}

// drawOpeningHand moves the top startingHandSize cards of pid's library to
// their hand, fewer if the library runs out first: GameAction.startGame's
// p1.drawCards(p1.getStartingHandSize()) (GameAction.java:2341) without a
// shuffle, which each caller does or not as its own Java does.
func drawOpeningHand(g *Game, pid PlayerID) {
	lib := g.Zone(Library, pid)
	for i := 0; i < startingHandSize && lib.Len() > 0; i++ {
		g.Move(lib.Cards()[0], Hand, pid)
	}
}

// PerformMulligans runs the London mulligan procedure for every seated
// player, ported from forge-game/src/main/java/forge/game/mulligan/
// MulliganService.java and LondonMulligan.java.
//
// London is the only rule ported. It is the one modern paper Magic has used
// since 2019 and the only one relevant to the constructed corpus Crucible
// targets (ADR-0011); Original, Paris and Vancouver are the rules London
// replaced, and Houston is a Forge-specific casual variant. Porting a
// strategy hierarchy for four rules nothing in scope exercises would be
// exactly the speculative work CLAUDE.md rules out (PORT-6).
//
// Callers deal each player's opening hand before calling this (DealOpeningHands,
// above) -- Java's MulliganService assumes Game already has.
func PerformMulligans(g *Game, controller PlayerController, firstPlayer PlayerID) {
	order := turnOrderFrom(g, firstPlayer)
	// CR 103.4: every player gets one free mulligan in a game with more than
	// two players. Brawl grants the same and is not modeled.
	freeFirst := len(order) > 2

	kept := make(map[PlayerID]bool, len(order))
	timesMulliganed := make(map[PlayerID]int, len(order))

	for {
		allKept := true
		for _, pid := range order {
			if kept[pid] {
				continue
			}
			tuck := londonTuckCount(timesMulliganed[pid], freeFirst)
			keep := tuck > startingHandSize || controller.MulliganKeepHand(g, pid, firstPlayer, tuck)
			if g.Over() {
				return
			}
			if keep {
				kept[pid] = true
				continue
			}
			allKept = false
			mulligan(g, controller, pid, freeFirst, &timesMulliganed)
		}
		if allKept {
			break
		}
	}
}

// mulligan is one London mulligan: shuffle the hand back into the library,
// draw a fresh seven, then bottom the cards this mulligan costs. Ported from
// AbstractMulligan.mulligan and LondonMulligan.mulliganDraw.
func mulligan(g *Game, controller PlayerController, pid PlayerID, freeFirst bool, timesMulliganed *map[PlayerID]int) {
	for _, id := range append([]CardID(nil), g.Zone(Hand, pid).Cards()...) {
		g.Move(id, Library, pid)
	}
	g.Shuffle(Library, pid)
	(*timesMulliganed)[pid]++

	lib := g.Zone(Library, pid)
	for i := 0; i < startingHandSize && lib.Len() > 0; i++ {
		g.Move(lib.Cards()[0], Hand, pid)
	}

	tuck := londonTuckCount((*timesMulliganed)[pid], freeFirst)
	if tuck == 0 {
		return
	}
	hand := append([]CardID(nil), g.Zone(Hand, pid).Cards()...)
	// canMulligan's cutoff (tuck <= startingHandSize) is checked against the
	// PREVIOUS mulligan count, one step behind what this one actually costs
	// -- Java's own LondonMulligan.canMulligan reads the same field
	// tuckCardsDuringMulligan does, before this mulligan's increment. That
	// lets the last offered mulligan cost one more card than a hand can
	// give back; clamping here is a defensive floor against asking a
	// PlayerController for more cards than exist, not a rules change --
	// tucking every remaining card and tucking "every remaining card, and
	// then some" both leave an empty hand.
	if tuck > len(hand) {
		tuck = len(hand)
	}
	for _, id := range controller.TuckCardsViaMulligan(g, pid, hand, tuck) {
		g.Move(id, Library, pid)
	}
}

// londonTuckCount is LondonMulligan.tuckCardsDuringMulligan: no cost for the
// hand you kept without ever mulliganing, and one mulligan is free in a
// multiplayer game.
func londonTuckCount(timesMulliganed int, freeFirst bool) int {
	if timesMulliganed == 0 {
		return 0
	}
	if freeFirst {
		return timesMulliganed - 1
	}
	return timesMulliganed
}

// turnOrderFrom is seating order starting at first, the same rotation
// MulliganService.initializeMulligans builds before asking anyone anything.
func turnOrderFrom(g *Game, first PlayerID) []PlayerID {
	ids := g.Players()
	start := 0
	for i, id := range ids {
		if id == first {
			start = i
			break
		}
	}
	out := make([]PlayerID, len(ids))
	for i := range ids {
		out[i] = ids[(start+i)%len(ids)]
	}
	return out
}
