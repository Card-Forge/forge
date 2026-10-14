// Foretell (CR 702.143): a special action that exiles a card from your hand
// for {2} on your turn, and the later cast of it from exile for its foretell
// cost. Ported from the Foretell branch of CardFactoryUtil.java:2961 (the
// action, an AbilityStatic costing {2} with Hand as its zone) and
// GameActionUtil.java:205-220 (the cast: only from Exile, only the owner, only
// once the card has been there since an earlier turn, for the K:Foretell:<cost>
// cost). The card is not turned face down: the engine is omniscient, and
// nothing reads the face-down state of a foretold card.

package engine

import (
	"github.com/jczastkiewicz/crucible/internal/cost"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// foretellActionCost is the {2} the special action costs.
const foretellActionCost = "2"

// hasForetell reports whether c carries K:Foretell:<cost>: a cost-less
// Foretell (granted by an effect, castable for its mana cost less {2}) is not
// ported, so the action is not offered for it rather than stranding the card.
func hasForetell(c *Card) bool {
	for _, line := range c.KeywordLines() {
		if k := keyword.Parse(line); k.Name == "Foretell" && k.Details != "" {
			return true
		}
	}
	return false
}

// Foretell is the special action (CR 116.2h, 702.143a): pid, on their own
// turn, pays {2} and exiles card from their hand, foretold. False when the
// card is not in pid's hand, has no Foretell, it is not pid's turn, or the
// cost is not paid. The special action needs no timing beyond priority, which
// the caller holds.
func (g *Game) Foretell(pid PlayerID, card CardID, controller PlayerController) bool {
	c := g.Card(card)
	if c.Zone != Hand || c.Controller() != pid || c.Def == nil || !hasForetell(c) || pid != g.activePlayer {
		return false
	}
	if _, paid := g.payManaCostX(pid, mana.MustParse(foretellActionCost), controller); !paid {
		return false
	}
	g.moveByEffect(controller, card, Exile, 0, NoPlayer, false)
	c = g.Card(card)
	c.foretold, c.foretoldTurn = true, g.turn
	g.checkChangesZoneAllTriggers(controller, []CardID{card}, Hand, Exile)
	return true
}

// cardIsForetold is Card.isForetold (Card.java:6717-6727): in exile, whether
// the card lies there foretold; anywhere else, whether its cast was for its
// foretell cost (getCastSA().isForetold), which a spell keeps onto the
// battlefield (Lupine Harbingers' "if it was foretold") and loses with its
// cast record on any other move.
func (g *Game) cardIsForetold(id CardID) bool {
	c := g.Card(id)
	if c.Zone == Exile {
		return c.foretold
	}
	return c.wasCast && c.castOpt.foretold
}

// foretellCost is the mana cost c is cast for from exile, when pid may: they
// own it, it was foretold on an earlier turn and is still there, and its
// K:Foretell:<cost> is a plain mana cost (anything else is not offered, GO-7).
func (g *Game) foretellCost(pid PlayerID, c *Card) (mana.Cost, bool) {
	if c.Zone != Exile || c.Owner != pid || c.Def == nil || !c.foretold || c.foretoldTurn >= g.turn {
		return mana.Cost{}, false
	}
	for _, line := range c.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "Foretell" || k.Details == "" {
			continue
		}
		if parsed := cost.Parse(k.Details); !parsed.IsPureMana() {
			return mana.Cost{}, false
		}
		mc, err := mana.Parse(k.Details)
		if err != nil || mc.CountX() > 0 {
			return mana.Cost{}, false
		}
		return mc, true
	}
	return mana.Cost{}, false
}
