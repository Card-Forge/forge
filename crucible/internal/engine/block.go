// Declaring blockers: CR 509.1, the step after declaring attackers.

package engine

import "github.com/jczastkiewicz/crucible/internal/cardtype"

// Blocks returns the current combat's blocking assignments, if any.
func (g *Game) Blocks() []Block { return g.combat.Blocks }

// DeclareCombatBlockers is CR 509.1: the defending player chooses which of their
// untapped creatures block which attacker. Unlike declaring attackers,
// blocking does not tap the blocker -- CR 508.1f only taps attackers, CR
// 509 has no equivalent.
//
// The offered eligible list is still only "untapped creature the defending
// player controls" -- CR 509.1a's own "eligible" is a property of the
// creature (untapped, yours), not of a specific attacker/blocker pairing.
// The answer is then checked in two passes, both Java's (ADR-0024):
//
//   - Each pairing, as InputBlock.onCardSelected checks every click
//     (InputBlock.java:151, CombatUtil.canBlock(attacker, blocker, combat)):
//     the attacker is one this defender is being asked about (CR 802.4a),
//     the blocker is one it was offered, the pair is not repeated, CanBlock
//     accepts it (CR 509.1b: CantBlockBy statics, flying/reach, Fear,
//     Horsemanship, Landwalk, Protection, Skulk -- staticability.go), and
//     the blocker is not already blocking another attacker
//     (CombatUtil.canBlockMoreCreatures -- no "can block an additional
//     creature" source exists in this port, so every blocker blocks one).
//   - The whole declaration, with every earlier defender's blocks in it the
//     way Java's Combat holds them: validateBlocks (blockvalidation.go),
//     Java's CombatUtil.validateBlocks -- block requirements (MustBlock,
//     Mode$ MustBlock, lure keywords), can't-block-alone keywords and each
//     attacker's blocker-count limits (Menace, Mode$ MinMaxBlocker).
//
// A failure is an *IllegalDeclarationError and nothing is applied: no
// Block is dropped or added on the controller's behalf (ADR-0024 Decision
// 2), since a fixture could not tell a legal declaration from a repaired
// one.
//
// Two exceptions, both ADR-0035's, both only for a defender some Event$
// DeclareBlocker replacement applies to (Camouflage's effect card;
// declareBlockersReplaced, replacement.go): the controller is not asked --
// the replacement's ReplaceWith$ declares that defender's blocks in its
// place -- and those blocks are not checked by either pass above, but
// repaired by Java's own steady-state loop (repairReplacedBlocks, below),
// PhaseHandler.java's replaced path, which never validates.
//
// "Who is defending" is each attacker's own defender (defenderOf,
// attack.go) -- the controller of whatever it's attacking, a player,
// planeswalker or battle. A two-player game, or a multiplayer game where
// the active player sent every attacker at one opponent, has exactly one:
// that defender is asked once, for every attacker. When attackers are split
// across more than one defending player at once, each defender is asked in
// turn, only about the attackers actually attacking them, offering only
// their own eligible creatures -- CR 509.1's "the defending player" read per
// defender rather than assumed singular. Defenders are asked in the order
// their first attacker appears in attackers, so the sequence is
// deterministic across a run (GO-12).
//
// A defender with no eligible creature is skipped, not asked with an
// empty list -- the same reasoning DeclareCombatAttackers uses for an
// active player with nothing to attack with, and Java's own
// CombatUtil.canBlock(p, combat) guard (PhaseHandler.java:664). The combined
// result is nil, not an empty non-nil slice, when every defender is skipped
// this way.
//
// checkBlocksTriggers/checkAttackerBlockedByCreatureTriggers (trigger.go)
// each run once per Block, after every defender's declaration passed --
// CR 509.2's own "whenever ~ blocks"/"whenever ~ becomes blocked by a
// creature" fire only for a legally declared block.
// checkAttackerBlockedTriggers (CR 509.2's own "becomes blocked," the whole
// blocker group rather than one at a time) runs once per distinct attacker
// afterward, once every Block naming it is known.
func (g *Game) DeclareCombatBlockers(controller PlayerController) ([]Block, error) {
	attackers := g.combat.Attackers
	if len(attackers) == 0 {
		return nil, nil
	}

	byDefender := map[PlayerID][]CardID{}
	var defenders []PlayerID
	for _, id := range attackers {
		d := g.defenderOf(id)
		if _, ok := byDefender[d]; !ok {
			defenders = append(defenders, d)
		}
		byDefender[d] = append(byDefender[d], id)
	}

	var blocks []Block
	for _, defender := range defenders {
		var eligible []CardID
		for _, id := range g.Zone(Battlefield, defender).Cards() {
			c := g.Card(id)
			if c.Type().Has(cardtype.Creature) && !c.Tapped {
				eligible = append(eligible, id)
			}
		}
		if len(eligible) == 0 {
			continue
		}
		replaced, handled, err := g.declareBlockersReplaced(controller, defender, blocks)
		if err != nil {
			return nil, err
		}
		if handled {
			if blocks, err = g.repairReplacedBlocks(defender, replaced); err != nil {
				return nil, err
			}
			continue
		}
		answer := controller.DeclareCombatBlockers(g, defender, byDefender[defender], eligible)
		for i, blk := range answer {
			if err := g.checkBlockPairing(blk, answer[:i], byDefender[defender], eligible); err != nil {
				return nil, err
			}
		}
		blocks = append(blocks, answer...)
		if err := g.validateBlocks(defender, blocks); err != nil {
			return nil, err
		}
	}
	g.combat.Blocks = blocks
	blockersByAttacker := map[CardID][]CardID{}
	var blockedAttackers []CardID
	for _, blk := range blocks {
		g.recordBlockedBy(blk)
		g.checkBlocksTriggers(controller, blk)
		g.checkAttackerBlockedByCreatureTriggers(controller, blk)
		if _, ok := blockersByAttacker[blk.Attacker]; !ok {
			blockedAttackers = append(blockedAttackers, blk.Attacker)
		}
		blockersByAttacker[blk.Attacker] = append(blockersByAttacker[blk.Attacker], blk.Blocker)
	}
	for _, attacker := range blockedAttackers {
		g.checkAttackerBlockedTriggers(controller, attacker, blockersByAttacker[attacker])
	}
	return blocks, nil
}

// repairReplacedBlocks is PhaseHandler.declareBlockersTurnBasedAction's
// steady-state loop (PhaseHandler.java:693-723), run on defender's blockers
// after a DeclareBlocker replacement declared them: each pass drops every
// blocker breaking a can't-block-alone rule against that pass's snapshot of
// defender's blockers, until a pass drops none. The if/else-if order is
// Java's -- a creature that can't block alone but has company is still
// checked against the greater-power rule. Dropping a blocker drops all its
// blocks (Combat.undoBlockingAssignment).
//
// This is ADR-0035's scoped exception to ADR-0024 Decision 2: on the
// replaced path Java never calls CombatUtil.validateBlocks, so a MustBlock
// or lure requirement goes unchecked and a can't-block-alone violation is
// repaired, not rejected. Every other declaration still goes through
// validateBlocks. Java runs this loop on the normal path too; there,
// validateBlocks has already rejected every violation it would repair.
//
// PhaseHandler.java:681-691's block-cost pass before it is not ported: this
// port has no block costs (blockvalidation.go's absent-default list).
func (g *Game) repairReplacedBlocks(defender PlayerID, blocks []Block) ([]Block, error) {
	for {
		var remaining []CardID
		for _, blk := range blocks {
			if g.Card(blk.Blocker).Controller() == defender && !containsCard(remaining, blk.Blocker) {
				remaining = append(remaining, blk.Blocker)
			}
		}
		var drop []CardID
		for _, id := range remaining {
			c := g.Card(id)
			remove := false
			switch {
			case len(remaining) < 2 && (c.hasKeywordText("CARDNAME can't attack or block alone.") || c.hasKeywordText("CARDNAME can't block alone.")):
				remove = true
			case len(remaining) < 3 && c.hasKeywordText("CARDNAME can't block unless at least two other creatures block."):
				remove = true
			case c.hasKeywordText("CARDNAME can't block unless a creature with greater power also blocks."):
				ok, err := greaterPowerAlsoBlocks(g, id, remaining)
				if err != nil {
					return nil, err
				}
				remove = !ok
			}
			if remove {
				drop = append(drop, id)
			}
		}
		if len(drop) == 0 {
			return blocks, nil
		}
		kept := blocks[:0:0]
		for _, blk := range blocks {
			if !containsCard(drop, blk.Blocker) {
				kept = append(kept, blk)
			}
		}
		blocks = kept
	}
}

// recordBlockedBy is Card.addBlockedByThisTurn for blk: its attacker was
// blocked by its blocker this turn (PhaseHandler.java:805,
// BlockEffect.java:61). Java stores an LKI copy and compares by id; the
// CardID is that id.
func (g *Game) recordBlockedBy(blk Block) {
	a := g.Card(blk.Attacker)
	a.blockedByThisTurn = append(a.blockedByThisTurn, blk.Blocker)
}

// checkBlockPairing is DeclareCombatBlockers' per-pair pass (its doc
// comment): blk against the pairs declared before it (earlier), the
// attackers this defender was asked about and the creatures it was offered.
func (g *Game) checkBlockPairing(blk Block, earlier []Block, attackers, eligible []CardID) error {
	if !containsCard(attackers, blk.Attacker) {
		return illegal("CR 802.4a", "can only block a creature attacking its controller", blk.Blocker, blk.Attacker)
	}
	if !containsCard(eligible, blk.Blocker) {
		return illegal("CR 509.1a", "not an eligible blocker", blk.Blocker)
	}
	for _, e := range earlier {
		if e == blk {
			return illegal("CR 509.1a", "declared blocking the same attacker twice", blk.Blocker, blk.Attacker)
		}
		if e.Blocker == blk.Blocker {
			return illegal("CR 509.1a", "cannot block more than one attacker", blk.Blocker)
		}
	}
	if !g.CanBlock(blk.Attacker, blk.Blocker) {
		return illegal("CR 509.1b", "cannot block this attacker", blk.Blocker, blk.Attacker)
	}
	return nil
}

// CanBlock reports whether blocker may legally block attacker (CR 509.1),
// Java's CombatUtil.canBlock(attacker, blocker): blocker can block at all
// (canBlockAtAll) and no Mode$ CantBlockBy static ability in play excludes
// the pair (cantBlockBy, staticability.go).
func (g *Game) CanBlock(attacker, blocker CardID) bool {
	return g.canBlockAtAll(blocker) && !cantBlockBy(g, attacker, blocker)
}

// canBlockAtAll is CombatUtil.canBlock(blocker) (CombatUtil.java:459-486)
// for the sources this port has: a creature, untapped, not detained (CR
// 701.35) or suspected (CR 701.60c -- Java's CantBlock statics for both),
// without "CARDNAME can't block." or "CARDNAME can't attack or block.", and
// -- for "CARDNAME can't [attack or] block alone." -- its controller has
// another creature.
func (g *Game) canBlockAtAll(blocker CardID) bool {
	b := g.Card(blocker)
	if !b.Type().Has(cardtype.Creature) || b.Tapped || b.isDetained() || b.Suspected {
		return false
	}
	if b.hasKeywordText("CARDNAME can't block.") || b.hasKeywordText("CARDNAME can't attack or block.") {
		return false
	}
	if b.hasKeywordText("CARDNAME can't attack or block alone.") || b.hasKeywordText("CARDNAME can't block alone.") {
		return len(g.creaturesInPlay(b.Controller())) >= 2
	}
	return true
}

// creaturesInPlay is Player.getCreaturesInPlay: every creature pid
// controls on the battlefield, tapped or not, in battlefield order.
func (g *Game) creaturesInPlay(pid PlayerID) []CardID {
	var out []CardID
	for _, id := range g.Zone(Battlefield, pid).Cards() {
		if g.Card(id).Type().Has(cardtype.Creature) {
			out = append(out, id)
		}
	}
	return out
}
