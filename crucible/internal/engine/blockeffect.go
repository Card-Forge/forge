package engine

//enginelint:allow control ability game effecthelpers card condition id defined zone combat

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// blockEffect is BlockEffect.java: each DefinedBlocker$ creature on the
// battlefield blocks each DefinedAttacker$ that is attacking, skipping a
// pair already blocking. Every new pair fires "blocks" and "becomes blocked
// by a creature"; an attacker that was not blocked before fires "becomes
// blocked" with every blocker it now has.
type blockEffect struct{}

func (blockEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "Block", "Condition"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	var attackers, blockers []CardID
	if def, ok := a.Params.Param("DefinedAttacker"); ok {
		ids, err := definedCards(source, def, a.refs())
		if err != nil {
			return fmt.Errorf("engine: Block: %w", err)
		}
		for _, id := range ids {
			if g.combat.isAttacking(id) {
				attackers = append(attackers, id)
			}
		}
	}
	if def, ok := a.Params.Param("DefinedBlocker"); ok {
		ids, err := definedCards(source, def, a.refs())
		if err != nil {
			return fmt.Errorf("engine: Block: %w", err)
		}
		for _, id := range ids {
			c := g.Card(id)
			if c.Zone == Battlefield && c.Type().Has(cardtype.Creature) {
				blockers = append(blockers, id)
			}
		}
	}
	if len(attackers) == 0 || len(blockers) == 0 {
		return nil
	}
	for _, atk := range attackers {
		wasBlocked := g.combat.isBlocked(atk)
		for _, blk := range blockers {
			pair := Block{Blocker: blk, Attacker: atk}
			if containsBlock(g.combat.Blocks, pair) {
				continue
			}
			g.combat.Blocks = append(g.combat.Blocks, pair)
			g.recordBlockedBy(pair)
			g.checkAttackerBlockedByCreatureTriggers(controller, pair)
			g.checkBlocksTriggers(controller, pair)
		}
		if !wasBlocked {
			g.checkAttackerBlockedTriggers(controller, atk, g.blockersOf(atk))
		}
	}
	return nil
}

// containsBlock reports whether pair is already one of blocks.
func containsBlock(blocks []Block, pair Block) bool {
	for _, b := range blocks {
		if b == pair {
			return true
		}
	}
	return false
}

// blockersOf lists the creatures blocking attacker, in declaration order.
func (g *Game) blockersOf(attacker CardID) []CardID {
	var out []CardID
	for _, b := range g.combat.Blocks {
		if b.Attacker == attacker {
			out = append(out, b.Blocker)
		}
	}
	return out
}
