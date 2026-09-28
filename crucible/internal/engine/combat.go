// Combat's own state: CR 506-510 -- attackers and their targets (attack.go),
// blocks (block.go), and the error an illegal declaration of either returns.

package engine

import "fmt"

// Combat is the game's current combat, if one is happening. Attackers is who
// was declared this combat (CR 508.1); AttackTargets is what each one is
// attacking -- a player, or a planeswalker/battle that player controls (CR
// 508.1d); Blocks is who blocks them (CR 509.1). The rest of CR 508-510
// lands once something needs it (M5-M6).
type Combat struct {
	Attackers     []CardID
	AttackTargets map[CardID]EntityID
	Blocks        []Block
	// ForcedBlocked is Combat.setBlocked(attacker, true) for an attacker
	// that became blocked without a blocker (BecomesBlocked, CR 509.1h): it
	// stays blocked and deals no combat damage unless it has trample.
	ForcedBlocked []CardID
}

// IllegalDeclarationError is a declaration of attackers or blockers that
// fails CR 508.1/509.1's legality check (ADR-0024): a restriction broken, or
// fewer requirements obeyed than Java's validator says were possible. The
// declaration is not applied; the caller gets this back instead (GO-7,
// ADR-0019 Decision 5). Rule names the CR rule broken, Cards the creatures
// the check names, and Reason is Java's own message for the failure
// (CombatUtil.validateBlocks' strings) or this port's equivalent.
type IllegalDeclarationError struct {
	Rule   string
	Cards  []CardID
	Reason string
}

func (e *IllegalDeclarationError) Error() string {
	return fmt.Sprintf("engine: illegal declaration (%s): %s %v", e.Rule, e.Reason, e.Cards)
}

// illegal builds an IllegalDeclarationError.
func illegal(rule, reason string, cards ...CardID) error {
	return &IllegalDeclarationError{Rule: rule, Cards: cards, Reason: reason}
}

// isBlocked is Combat.isBlocked: an attacker with a blocker, or one an
// effect made blocked.
func (c *Combat) isBlocked(attacker CardID) bool {
	for _, b := range c.Blocks {
		if b.Attacker == attacker {
			return true
		}
	}
	return containsCard(c.ForcedBlocked, attacker)
}

// isAttacking reports whether id is one of this combat's attackers.
func (c *Combat) isAttacking(id CardID) bool { return containsCard(c.Attackers, id) }

// Block is one blocking assignment: Blocker blocks Attacker (CR 509.1). A
// single Attacker can appear in more than one Block -- gang blocking is
// ordinary, CR 509.1c only limits how many attackers one blocker can
// choose, not the reverse.
type Block struct {
	Blocker, Attacker CardID
}

// DamageAssignment is one entry in the order an attacking player divides a
// gang-blocked attacker's combat damage (CR 510.1c, combatdamage.go):
// Blocker receives Amount, and entries are applied in the order returned.
// The rule requires each blocker in that order to receive at least its
// lethal amount before any is left for the next one, but that isn't checked
// here -- trust the controller's answer, the same as ChooseLegendaryToKeep.
type DamageAssignment struct {
	Blocker CardID
	Amount  int
}

// removeFromCombat is CR 506.4's own "if a permanent leaves combat" --
// removeFromCombatEffect.go's own real caller, CR 400.7's own Move (game.go)
// leaving combat untouched itself since that rule fires from far more places
// than a script-driven zone change alone (first strike/second strike damage,
// a Battle losing its own last defender, ...), none of which this port
// tracks well enough yet to fire this on its own (game-state.md's "Not
// ported yet"). id stops being an attacker and every block naming it either
// side (attacker or blocker) drops -- CR 509.1h's own "removed from combat"
// for a blocker gone mid-combat, and the identical rule for an attacker
// applied to Blocks the other direction, both a plain filter since neither
// side's own departure ever implies a different one should also leave.
func (g *Game) removeFromCombat(id CardID) {
	var attackers []CardID
	for _, a := range g.combat.Attackers {
		if a != id {
			attackers = append(attackers, a)
		}
	}
	g.combat.Attackers = attackers
	if g.combat.AttackTargets != nil {
		delete(g.combat.AttackTargets, id)
	}
	var blocks []Block
	var lostBlocker []CardID
	for _, b := range g.combat.Blocks {
		if b.Attacker != id && b.Blocker != id {
			blocks = append(blocks, b)
		} else if b.Blocker == id && !containsCard(lostBlocker, b.Attacker) {
			lostBlocker = append(lostBlocker, b.Attacker)
		}
	}
	g.combat.Blocks = blocks
	g.combat.ForcedBlocked = withoutCards(g.combat.ForcedBlocked, []CardID{id})
	// CR 509.1h/506.4: an attacker whose last blocker leaves combat remains
	// blocked. Java keeps its AttackingBand's blocked flag
	// (Combat.removeFromCombat, Combat.java:611-638, never clears it); here
	// that attacker joins ForcedBlocked, which isBlocked and combat damage
	// already read as "blocked, no blocker".
	for _, atk := range lostBlocker {
		if !g.combat.isBlocked(atk) {
			g.combat.ForcedBlocked = append(g.combat.ForcedBlocked, atk)
		}
	}
}

// clone is Combat's half of Game.Clone: a shared backing array would let a
// declaration on the clone alias the original, the same reasoning PT's own
// clone has.
func (c Combat) clone() Combat {
	var targets map[CardID]EntityID
	if c.AttackTargets != nil {
		targets = make(map[CardID]EntityID, len(c.AttackTargets))
		for k, v := range c.AttackTargets {
			targets[k] = v
		}
	}
	return Combat{
		Attackers:     append([]CardID(nil), c.Attackers...),
		AttackTargets: targets,
		Blocks:        append([]Block(nil), c.Blocks...),
		ForcedBlocked: append([]CardID(nil), c.ForcedBlocked...),
	}
}
