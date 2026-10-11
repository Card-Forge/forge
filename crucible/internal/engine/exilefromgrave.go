// Exiling a card from the graveyard or the hand as an activation cost:
// ExileFromGrave<1/CARDNAME>/ExileFromHand<1/CARDNAME>, CostExile.java's own
// graveyard-origin and hand-origin constructors (the identical class
// exile.go's own battlefield ExileCost already reads, a different ZoneType
// argument each) -- SelfExile's own two siblings for an ability activated
// from somewhere other than the battlefield (ActivationZone$ Graveyard/Hand,
// activateability.go).

package engine

// exileFromGraveyard moves id from the graveyard to exile as a paid cost and
// fires the graveyard-origin ChangesZone triggers the move meets
// (checkLeftGraveyardTriggers; GameAction.changeZone runs ChangesZone for a
// cost's move as for any other, GameAction.java:601-613). Paying for a spell
// being cast, the triggers wait for its SpellCast batch (noteGraveyardExit),
// as Java holds them until the cast is done (TriggerHandler.java:264).
// Paying for an activated ability, they go on the stack as the cost is paid,
// under the ability: the order every cost-paid trigger of an activation
// already has here (a sacrificed creature's dies trigger, a discarded card's
// Discarded trigger). No last-known information is frozen: that snapshot
// exists for a battlefield departure.
func exileFromGraveyard(g *Game, controller PlayerController, id CardID) {
	c := g.Card(id)
	from := c.Zone
	g.Move(id, Exile, c.Owner)
	if !g.noteGraveyardExit(id, from) && from == Graveyard {
		g.checkLeftGraveyardTriggers(controller, id, g.Card(id).Zone)
	}
}

// exileFromHand is exileFromGraveyard's own sibling for ExileFromHand<1/
// CARDNAME|NICKNAME>: a card leaving the hand. This port has no walk for a
// hand-origin ChangesZone trigger to a zone other than the battlefield, so
// none fires (m5-t-triggers-animate.md's audit table).
func exileFromHand(g *Game, id CardID) {
	c := g.Card(id)
	g.Move(id, Exile, c.Owner)
}
