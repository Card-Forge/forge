// Basic land mana abilities: CR 305.6's intrinsic "T: Add [color]" ability
// every land with a basic land type carries, whether or not the card's own
// text prints it.

package engine

import (
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// basicLandType maps CR 305.6's five basic land colors to the subtype that
// grants each one its intrinsic mana ability. Forge synthesizes this ability
// from the type line rather than card script text -- CardState.java's
// getLandTraitChanges/getLandManaForColor builds it on the fly from
// hasSubtype(color.getBasicLandType()), and forge-gui/res/cardsfolder's own
// plains.txt has no A: line at all, only the Oracle: reminder ({T}: Add
// {W}.) -- which is why this port keys off cardtype.Line's subtypes instead
// of anything compiled from a card script.
var basicLandType = map[mana.Colors]string{
	mana.White: "Plains",
	mana.Blue:  "Island",
	mana.Black: "Swamp",
	mana.Red:   "Mountain",
	mana.Green: "Forest",
}

// TapLandForMana is CR 305.6 plus CR 605.3's mana ability rule: a mana
// ability resolves immediately with no stack, so tapping the land and
// adding to the pool happen in the same call. color picks which of the
// land's basic land types to tap for when it has more than one -- a
// Snow-Covered Plains Island keeps two separate intrinsic abilities, and
// tapping activates only one of them.
//
// The mana produced is snow (CR 106.3a: any mana a snow permanent produces
// is snow mana of that type) exactly when the land itself carries the Snow
// supertype (forge-gui/res/cardsfolder's own snow_covered_plains.txt:
// "Types:Basic Snow Land Plains", the identical no-A:-line shape plains.txt
// has, differing only in that one supertype) -- nothing about the ability
// itself changes, so this is the only place that distinction is read.
//
// Reports whether the tap succeeded. false covers every legal-but-failed
// case a bad decision could reach: land not controlled by pid, not on the
// battlefield, already tapped, or lacking the basic land type color asks
// for -- the same "declined by the rules, not a bug" contract
// [Game.PayManaCost]'s own bool return already carries.
//
// color itself must be exactly one of mana.White/Blue/Black/Red/Green, the
// same invariant [Pool.Add] enforces -- anything else (the empty set, more
// than one bit) is not a key basicLandType holds, so it falls into the
// panic below rather than silently reporting false. That is an engine
// invariant breach, not something a fixture or a card can cause (GO-7):
// nothing here ever computes a Colors value with more than one bit, so
// reaching that panic means a caller passed one in by hand.
//
// A successful tap checks CR 603's own "becomes tapped" and "taps for mana"
// triggers (checkTapsTriggers/checkTapsForManaTriggers, trigger.go) --
// this port's only other tap site (DeclareCombatAttackers, attack.go) checks
// the first but not the second, since attacking is not a mana ability.
func (g *Game) TapLandForMana(pid PlayerID, land CardID, color mana.Colors, controller PlayerController) bool {
	basic, ok := basicLandType[color]
	if !ok {
		panic("engine: TapLandForMana wants exactly one basic land color")
	}
	c := g.Card(land)
	if c.Controller() != pid || c.Zone != Battlefield || c.Tapped || c.isDetained() {
		return false
	}
	if !c.Type().HasSubtype(basic) {
		return false
	}
	c.Tapped = true
	produced := g.manaReplaced(controller, pid, land,
		producedMana{color: color, snow: c.Type().HasSupertype(cardtype.Snow), amount: 1})
	g.addProducedMana(pid, produced)
	g.checkTapsTriggers(controller, land, pid, false)
	g.noteTapsForMana(land, pid)
	g.checkTapsForManaTriggers(controller, land, pid, produced)
	return true
}

// addProducedMana puts one production of mana in pid's pool, after
// ProduceMana replacements (manaReplaced, replacement.go) have had their say.
func (g *Game) addProducedMana(pid PlayerID, m producedMana) {
	if m.amount <= 0 {
		return
	}
	pool := &g.Player(pid).ManaPool
	switch {
	case m.colorless && m.snow:
		pool.AddSnowColorless(m.amount)
	case m.colorless:
		pool.AddColorless(m.amount)
	case m.snow:
		pool.AddSnow(m.color, m.amount)
	default:
		pool.Add(m.color, m.amount)
	}
}
