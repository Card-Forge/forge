// Sagas (CR 714): a Saga enters with a lore counter, gets another at the
// beginning of its controller's precombat main phase (CR 714.3b, turn-based),
// triggers the chapter ability its lore counters reach (the `K:Chapter`
// expansion, keyword.Expand), and is sacrificed once its last chapter ability
// has left the stack (CR 714.4, a state-based action).

//enginelint:allow id zone card game player ability control keyword parts sacrificeeffect

package engine

import (
	"strconv"

	"github.com/jczastkiewicz/crucible/internal/keyword"
)

// Lore is the counter a Saga's chapters count.
const Lore CounterType = "LORE"

// finalChapter is Card.getFinalChapterNr: the highest chapter of c's `Chapter`
// keyword, 0 when it has none.
func (c *Card) finalChapter() int {
	for _, line := range c.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "Chapter" {
			continue
		}
		if n, err := strconv.Atoi(k.Args()[0]); err == nil {
			return n
		}
	}
	return 0
}

// isSaga is Card.isSaga: the type line says Saga.
func (c *Card) isSaga() bool { return c.Type().HasSubtype("Saga") }

// applySagaCounter is CardState.getSagaRep's "etbCounter:LORE:1" for a Saga
// that does not read ahead: it enters with one lore counter, before its enter
// triggers look at it, so chapter I triggers from the counter's placement.
func (g *Game) applySagaCounter(controller PlayerController, moved CardID) {
	c := g.Card(moved)
	if !c.isSaga() || c.HasKeyword("Read ahead") {
		return
	}
	// The etbCounter's PutCounter is placed by its activator, the Saga's
	// controller (CardFactoryUtil.makeEtbCounter, CardFactoryUtil.java:564).
	if n := g.countersReplaced(controller, c.Controller(), CardEntity(moved), Lore, 1); n > 0 {
		g.addCardCounters(controller, c.Controller(), NoCard, moved, Lore, n)
	}
}

// sagaLoreCounters is PhaseHandler.java:283-289, CR 714.3b: at the beginning of
// the active player's precombat main phase each Saga they control with a
// chapter ability gets a lore counter.
func (g *Game) sagaLoreCounters(controller PlayerController) {
	for _, id := range append([]CardID(nil), g.Zone(Battlefield, g.activePlayer).Cards()...) {
		c := g.Card(id)
		if !c.isSaga() || c.finalChapter() == 0 {
			continue
		}
		// Placed by the turn's player (PhaseHandler.java:286, playerTurn).
		if n := g.countersReplaced(controller, g.activePlayer, CardEntity(id), Lore, 1); n > 0 {
			g.addCardCounters(controller, g.activePlayer, NoCard, id, Lore, n)
		}
	}
}

// sacrificeCompletedSagas is GameAction.stateBasedAction_Saga (CR 714.4): a Saga
// with as many lore counters as its final chapter, and none of its chapter
// abilities on the stack, is sacrificed. Phased-out permanents are skipped as
// Card.canBeSacrificedBy refuses them.
func sacrificeCompletedSagas(g *Game, controller PlayerController) bool {
	var done []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(id)
			final := c.finalChapter()
			if final == 0 || !c.isSaga() || c.IsPhasedOut() || c.Counters.Count(Lore) < final {
				continue
			}
			if !g.hasChapterOnStack(id) {
				done = append(done, id)
			}
		}
	}
	if len(done) == 0 {
		return false
	}
	sacrificeCards(g, controller, &Ability{Source: done[0], Controller: g.Card(done[0]).Controller()}, done)
	return true
}

// hasChapterOnStack is MagicStack.hasSourceOnStack(c, SpellAbility::isChapter):
// an ability synthesized from the card's `Chapter` keyword is on the stack.
func (g *Game) hasChapterOnStack(id CardID) bool {
	for i := range g.stack {
		item := &g.stack[i]
		if item.Source != id || item.Params == nil {
			continue
		}
		if keyword.Parse(item.Params.Keyword).Name == "Chapter" {
			return true
		}
	}
	return false
}
