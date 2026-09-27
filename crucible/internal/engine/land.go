// Playing a land: CR 305. Not casting a spell -- no cost, no stack.

package engine

import "github.com/jczastkiewicz/crucible/internal/cardtype"

// maxLandPlays is CR 305.2's default one land per turn, the base
// LandPlayLimit (player.go) folds Layer 8's own AdjustLandPlays$ continuous
// effects on top of.
const maxLandPlays = 1

// PlayLand is CR 305: playing a land is not casting a spell, so it has no
// cost to pay and never touches the stack (CR 305.1) -- the card moves
// straight from hand to the battlefield.
//
// Timing is CR 305.3's own gate, collapsed to what this port can check
// without an interactive priority system: pid's own turn, one of pid's main
// phases, and nothing already on the stack. CR 305.3 itself phrases the last
// two as "any time they could cast a sorcery" (CR 307.5's own definition);
// this port has no PlayerController method that lets anyone respond to
// anything on the stack yet (stack.go's own doc comment), so "stack empty"
// is never false today -- checked anyway, since land.go should not have to
// change again once casting exists to make it meaningful.
//
// Reports whether the land was played. false covers every legal-but-declined
// case: not pid's turn, not a main phase, something on the stack, the card
// is neither in pid's hand nor MayPlay$-granted, the card is not a land, or
// the per-turn limit (LandPlayLimit, player.go) is already spent -- the
// same "declined by the rules, not a bug" contract PayManaCost and
// TapLandForMana already carry.
//
// checkLandPlayedTriggers (trigger.go, CR 603.5) runs after checkETBTriggers,
// Player.playLand's own real ordering (Java's moveTo fires ETB triggers
// internally before the explicit LandPlayed runTrigger call), and
// LandsPlayed only increments after both: NotFirstLand$'s own "if it wasn't
// the first land you played this turn" reads the count of lands played
// strictly before this one, the same value Java's own performTest sees
// since it runs before Player.addLandPlayedThisTurn() there too.
func (g *Game) PlayLand(pid PlayerID, card CardID, controller PlayerController) bool {
	if pid != g.activePlayer {
		return false
	}
	if g.activePhase != Main1 && g.activePhase != Main2 {
		return false
	}
	if len(g.stack) != 0 {
		return false
	}
	c := g.Card(card)
	// Outside pid's hand, a Layer 8 MayPlay$ grant (mayPlayOption, game.go)
	// is the only permission: Crucible of Worlds' "play lands from your
	// graveyard". A land has no mana cost or flash timing to vary, so any
	// live grant carrying the zone permission will do.
	if (c.Controller() != pid || c.Zone != Hand) && !g.mayPlayLand(pid, card) {
		return false
	}
	if !c.Type().Has(cardtype.Land) {
		return false
	}
	if !g.hasLandDrop(pid) {
		return false
	}
	g.playLandNow(controller, pid, card)
	return true
}

// hasLandDrop reports whether pid has a land play left this turn
// (LandPlayLimit, player.go) -- Player.canPlayLand's own count check, the
// half of it an effect's "you may play that card" still applies (CR 305.3,
// AbilityUtils.getSpellsFromPlayEffect).
func (g *Game) hasLandDrop(pid PlayerID) bool {
	limit, unlimited := g.Player(pid).LandPlayLimit(maxLandPlays)
	return unlimited || g.Player(pid).LandsPlayed < limit
}

// playLandNow is Player.playLandNoCheck: card goes onto the battlefield
// under pid from wherever it is, its ETB replacement and triggers run, then
// its LandPlayed triggers, and the play counts against pid's land drops.
// PlayLand calls it once its own timing and hand gates pass; Play
// (playeffect.go) calls it for a land from any zone.
func (g *Game) playLandNow(controller PlayerController, pid PlayerID, card CardID) {
	c := g.Card(card)
	origin := c.Zone
	g.Move(card, Battlefield, pid)
	c.controller = pid
	g.enterBattlefieldReplacements(controller, card, origin)
	g.checkETBTriggers(controller, card, origin)
	g.checkLandPlayedTriggers(controller, card, pid, origin)
	g.Player(pid).LandsPlayed++
}
