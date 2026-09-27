package engine

//enginelint:allow id card player game ability condition control zone trigger replacement

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
)

// ActivePlane is the face-up plane or phenomenon (Game.getActivePlanes,
// narrowed to one card by ADR-0029), NoCard while there is none. Its planar
// controller is the player whose Command zone holds it,
// Card(ActivePlane()).ZoneOwner.
func (g *Game) ActivePlane() CardID { return g.activePlane }

// SetActivePlane writes ActivePlane directly -- a state injection for setup
// and fixtures, the same relationship SetMonarch has to BecomeMonarch.
// Planeswalk is what real play calls. It does not move id anywhere.
func (g *Game) SetActivePlane(id CardID) { g.activePlane = id }

// PlanechaseActive reports whether this is a Planechase game: true once
// setup has put a card into any player's PlanarDeck (NewCard, ADR-0029),
// Java's getActivePlanes() != null gate. Every Planechase effect is a no-op
// while it is false.
func (g *Game) PlanechaseActive() bool { return g.planechaseActive }

// planeswalkEffect is PlaneswalkEffect.java: CR 901.8's planeswalk. Every
// player's current plane goes to the bottom of its owner's planar deck,
// then the top card of the activator's planar deck is turned face up in
// their Command zone and becomes the active plane.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/PlaneswalkEffect.java's
// resolve, with Player.planeswalk/planeswalkTo/leaveCurrentPlane
// (Player.java:2641-2679) inlined. Resolution order is Java's: the
// Planechase gate, Optional$, the Planeswalk replacement check, leaving,
// walking.
//
// Cause$ is read only into the Planeswalk replacement's run params
// (AbilityKey.Cause, for a replacement's ValidCause$); the replacement check
// below fails closed on any live one, so Cause$ has nothing left to change
// and is accepted -- it is the planar die's own synthetic line
// (Player.java:3272, "DB$ Planeswalk | Cause$ PlanarDie").
type planeswalkEffect struct{}

// planeswalkUnresolved names the params this port rejects: Defined$ (2 real
// lines, both Defined$ Remembered) planeswalks to a chosen set of planes,
// Spatial Merging's two at once, and DontPlaneswalkAway$ (1, with Defined$)
// keeps the old plane beside the new one. Both need more than one
// concurrent active plane, which ADR-0029's single activePlane cannot hold.
var planeswalkUnresolved = [...]string{"Defined", "DontPlaneswalkAway"}

func (planeswalkEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	for _, key := range planeswalkUnresolved {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Planeswalk: %s$ not resolvable yet (needs concurrent active planes, ADR-0029)", key)
		}
	}
	if !subAbilityConditionMet(g, g.Card(a.Source), a.Amounts, a.Params) {
		return nil
	}
	if !g.planechaseActive {
		// Not a Planechase game: nothing happens (PlaneswalkEffect.java:23).
		return nil
	}
	activator := a.Controller
	if _, ok := a.Params.Param("Optional"); ok && !c.ConfirmEffect(g, activator, a.Source) {
		return nil
	}
	if name, ok := g.planeswalkReplacement(); ok {
		return fmt.Errorf("engine: Planeswalk: %q's Event$ Planeswalk replacement not resolvable yet", name)
	}
	if g.planarDeckEmptyAfterLeaving(activator) {
		// Player.planeswalk's getZone(PlanarDeck).get(0) would throw
		// (Player.java:2642); refuse before anything moves (GO-7).
		return fmt.Errorf("engine: Planeswalk: activator's planar deck is empty")
	}
	if err := g.leaveCurrentPlanes(c); err != nil {
		return err
	}
	top := g.Zone(PlanarDeck, activator).Cards()[0]
	g.Move(top, Command, activator)
	g.activePlane = top
	g.checkPlaneswalkedToTriggers(c, []CardID{top})
	return g.TakePendingError()
}

// planeswalkReplacement names the host of a live Event$ Planeswalk
// replacement (Susan Foreman, P No Way Out, Fixed Point in Time's effect),
// if any. ReplacementType.Planeswalk is not ported (ADR-0029), so its mere
// presence refuses the planeswalk, whether or not its own conditions would
// hold, rather than planeswalking as if it were not there.
func (g *Game) planeswalkReplacement() (string, bool) {
	name, found := "", false
	g.eachReplacement("Planeswalk", func(h *Card, _ map[string]expr.Amount, _ *compile.Ability) bool {
		name, found = h.Def.Name, true
		return true
	})
	return name, found
}

// planarDeckEmptyAfterLeaving reports whether activator will have no card
// to planeswalk to once the leave step has returned the active plane to its
// owner's planar deck.
func (g *Game) planarDeckEmptyAfterLeaving(activator PlayerID) bool {
	if g.Zone(PlanarDeck, activator).Len() > 0 {
		return false
	}
	return g.activePlane == NoCard || g.Card(g.activePlane).Owner != activator
}

// leaveCurrentPlanes is PlaneswalkEffect.java:39-43's loop over every
// player, each running Player.leaveCurrentPlane (Player.java:2669-2679):
// Mode$ PlaneswalkedFrom fires with that player's current plane -- the
// active plane for its planar controller, none for everyone else -- and
// only then does the plane go to the bottom of its owner's planar deck
// (moveTo(ZoneType.PlanarDeck, plane, -1): the owner's zone, bottom).
// Players are walked in seat order, game.getPlayers(), skipping a player
// who has lost the way that list does.
func (g *Game) leaveCurrentPlanes(c PlayerController) error {
	for _, pid := range g.Players() {
		if g.Player(pid).Lost {
			continue
		}
		var current []CardID
		if g.activePlane != NoCard && g.Card(g.activePlane).ZoneOwner == pid {
			current = []CardID{g.activePlane}
		}
		g.checkPlaneswalkedFromTriggers(c, current)
		if err := g.TakePendingError(); err != nil {
			return err
		}
		for _, plane := range current {
			g.Move(plane, PlanarDeck, g.Card(plane).Owner)
			g.activePlane = NoCard
		}
	}
	return nil
}
