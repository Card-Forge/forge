package engine

//enginelint:allow card game ability condition control combat block blockeffect trigger valid effecthelpers id zone

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// switchBlockEffect is SwitchBlockEffect.java: two blocked attackers trade
// their blockers (General Jarkeld), or two blockers trade the attackers
// they block (Sorrow's Path), when every creature involved could legally
// block its new partner.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/SwitchBlockEffect.java's
// resolve. Scoped to the corpus's two real lines: DefinedAttacker$ and
// DefinedBlocker$ resolve only the spellings those lines write
// (switchBlockDefined); any other value is an error.
//
// Two pieces of Java have no counterpart, by design:
//
//   - combat.orderBlockersForDamageAssignment/orderAttackersForDamageAssignment
//     and unregisterAttacker re-cache Java's stored damage-assignment order.
//     This port stores none: AssignCombatDamage (control.go) is asked at
//     damage time from whatever Combat.Blocks then holds.
//   - combat.getBandOfAttacker(attacker1) == getBandOfAttacker(attacker2):
//     without banding every attacker is its own band, so the check is
//     attacker1 == attacker2, which two distinct targets never are.
//
// GameEventCombatChanged is a view event with no ADR-0013 counterpart.
type switchBlockEffect struct{}

func (switchBlockEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "SwitchBlock", "Condition"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	targetingAttacker, historyScan := false, false
	var attackers, blockers []CardID
	if def, ok := a.Params.Param("DefinedAttacker"); ok {
		targetingAttacker = def == "Targeted"
		historyScan = def == blockedByHistoryDefined
		ids, err := switchBlockDefined(g, a, def)
		if err != nil {
			return err
		}
		for _, id := range ids {
			if g.combat.isAttacking(id) {
				attackers = append(attackers, id)
			}
		}
	}
	if def, ok := a.Params.Param("DefinedBlocker"); ok {
		if def == "Targeted" {
			targetingAttacker = false
		}
		ids, err := switchBlockDefined(g, a, def)
		if err != nil {
			return err
		}
		for _, id := range ids {
			if isBlocking(g.combat.Blocks, id) {
				blockers = append(blockers, id)
			}
		}
	}
	if len(attackers) == 0 || len(blockers) == 0 {
		return nil
	}
	var plan switchPlan
	var ok bool
	var stray error
	if targetingAttacker {
		plan, ok = g.planAttackersSwitch(attackers, blockers)
		if ok {
			stray = g.checkStrayBlocks(blockers, attackers[:2], jarkeldStrayBlock)
		}
	} else {
		plan, ok = g.planBlockersSwitch(attackers, blockers)
		if ok && historyScan {
			stray = g.checkStrayBlocks(plan.leave, attackers, sorrowStrayBlock)
		}
	}
	if !ok {
		return nil
	}
	if stray != nil {
		return stray
	}
	_, reblock := a.Params.Param("RemoveFromCombat")
	if reblock {
		if err := g.checkReblockTriggerKeys(plan.join); err != nil {
			return err
		}
	}
	for _, id := range plan.leave {
		g.removeFromCombat(id)
	}
	for _, blk := range plan.join {
		g.combat.Blocks = append(g.combat.Blocks, blk)
		if reblock {
			// SwitchBlockEffect.runTriggers: AttackerBlockedByCreature,
			// then Blocks, right after each addBlocker.
			g.checkAttackerBlockedByCreatureTriggers(controller, blk)
			g.checkBlocksTriggers(controller, blk)
		}
	}
	return nil
}

// switchPlan is one switch worked out before anything changes: the
// creatures that leave combat (Combat.removeFromCombat drops every block
// each has, not only the switched ones), then the blocks re-added, in
// Java's order. Planning first lets checkReblockTriggerKeys fail the line
// before any block moves.
type switchPlan struct {
	leave []CardID
	join  []Block
}

// switchBlockDefined is AbilityUtils.getDefinedCards for the spellings the
// corpus's two DefinedAttacker$/DefinedBlocker$ lines write. The two
// "Valid ..." forms name ability-relative properties Matches cannot see (it
// takes no ability or targets -- phasesValidCards' own reason,
// phaseseffect.go), so each is answered here over the battlefield in
// Game.getCardsIn order (seat, then zone order):
//
//   - "Creature.blockingTargeted" (CardProperty.java:1558-1575): a creature
//     blocking at least one of the ability's targeted cards.
//   - "Creature.blockedByValidThisTurn Targeted" (CardProperty.java:1618-1633):
//     a creature blocked this turn by at least one targeted card
//     (Card.blockedByThisTurn). Java first tests each recorded blocker
//     against "Targeted" as a valid string; "Targeted" is no type
//     (Card.isValid's hasStringType fallthrough), so only its
//     getDefinedCards half can match.
func switchBlockDefined(g *Game, a *Ability, def string) ([]CardID, error) {
	var targets []CardID
	for _, e := range a.Targets {
		if id, ok := e.AsCard(); ok {
			targets = append(targets, id)
		}
	}
	var match func(c *Card) bool
	switch def {
	case "Targeted":
		return targets, nil
	case "Valid Creature.blockingTargeted":
		match = func(c *Card) bool {
			for _, t := range targets {
				if g.blocks(c.ID, t) {
					return true
				}
			}
			return false
		}
	case blockedByHistoryDefined:
		match = func(c *Card) bool {
			for _, t := range targets {
				if containsCard(c.blockedByThisTurn, t) {
					return true
				}
			}
			return false
		}
	default:
		return nil, fmt.Errorf("engine: SwitchBlock: Defined %q not resolvable yet", def)
	}
	var out []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if c := g.Card(id); c.Type().Has(cardtype.Creature) && match(c) {
				out = append(out, id)
			}
		}
	}
	return out, nil
}

// planAttackersSwitch is SwitchBlockEffect.java:61-112, General Jarkeld's
// branch: the first two attackers trade every blocker. ok is false, with
// nothing to do, when only one attacker is left or some blocker could not
// block the other attacker (CombatUtil.canBlock(attacker, blocker) --
// CanBlock). Every blocker leaves combat, then rejoins blocking the other
// attacker, or both when it blocked both.
func (g *Game) planAttackersSwitch(attackers, blockers []CardID) (switchPlan, bool) {
	if len(attackers) == 1 {
		return switchPlan{}, false
	}
	a1, a2 := attackers[0], attackers[1]
	for _, b := range blockers {
		if g.blocks(b, a1) && !g.CanBlock(a2, b) || g.blocks(b, a2) && !g.CanBlock(a1, b) {
			return switchPlan{}, false
		}
	}
	plan := switchPlan{leave: blockers}
	for _, b := range blockers {
		if g.blocks(b, a1) {
			plan.join = append(plan.join, Block{Blocker: b, Attacker: a2})
		}
		if g.blocks(b, a2) {
			plan.join = append(plan.join, Block{Blocker: b, Attacker: a1})
		}
	}
	return plan, true
}

// planBlockersSwitch is SwitchBlockEffect.java:113-163, Sorrow's Path's
// branch: the first two blockers trade every attacker. ok is false when
// only one blocker is left, when either blocks more attackers than the
// other could (canBlockAdditional()+1; nothing in this port grants an
// additional block, so the limit is 1), or when some attacker could not be
// blocked by the other blocker. Both blockers leave combat, then each
// rejoins blocking what the other blocked.
func (g *Game) planBlockersSwitch(attackers, blockers []CardID) (switchPlan, bool) {
	if len(blockers) == 1 {
		return switchPlan{}, false
	}
	b1, b2 := blockers[0], blockers[1]
	if len(g.attackersBlockedBy(b1)) > 1 || len(g.attackersBlockedBy(b2)) > 1 {
		return switchPlan{}, false
	}
	for _, at := range attackers {
		if g.blocks(b1, at) && !g.CanBlock(at, b2) || g.blocks(b2, at) && !g.CanBlock(at, b1) {
			return switchPlan{}, false
		}
	}
	plan := switchPlan{leave: []CardID{b1, b2}}
	for _, at := range attackers {
		if g.blocks(b1, at) {
			plan.join = append(plan.join, Block{Blocker: b2, Attacker: at})
		}
		if g.blocks(b2, at) {
			plan.join = append(plan.join, Block{Blocker: b1, Attacker: at})
		}
	}
	return plan, true
}

// checkReblockTriggerKeys fails closed on a Forge bug (PORT-8,
// forge-java-defects.md): SwitchBlockEffect.java:24 runs TriggerType.Blocks
// with AbilityKey.Attacker, but TriggerBlocks.java:61 reads
// AbilityKey.Attackers, so under RemoveFromCombat$ a Mode$ Blocks trigger
// naming ValidBlocked$ sees no attacker and never fires in Java. Firing it
// here would silently fix the bug; not firing it would copy it. So a switch
// that would test ValidBlocked$ against the new attacker (the key Java's bug
// nulls) is an error before anything moves, regardless of ValidCard$ (that
// param is TriggerBlocks.java:58's own, unrelated, unaffected check against
// the blocker). A Blocks trigger without ValidBlocked$ does not read the
// missing key and fires normally.
func (g *Game) checkReblockTriggerKeys(join []Block) error {
	for _, blk := range join {
		for _, pid := range g.Players() {
			for _, host := range g.traitHosts(pid) {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, face := range h.Def.Faces {
					for _, t := range face.Triggers {
						if !isBlocksTrigger(t) {
							continue
						}
						validBlocked, ok := t.Param("ValidBlocked")
						if ok && Matches(g, g.Card(blk.Attacker), valid.Parse(validBlocked), h.Controller(), host) {
							return fmt.Errorf("engine: SwitchBlock: RemoveFromCombat$ re-block would test a Mode$ Blocks "+
								"ValidBlocked$ trigger on card %d that Java never passes an attacker (SwitchBlockEffect.java:24)", host)
						}
					}
				}
			}
		}
	}
	return nil
}

// checkStrayBlocks fails closed (PORT-8, forge-java-defects.md) when a
// switching blocker also blocks a live attacker outside switched -- the
// two cases where SwitchBlockEffect.java does something its own card text
// does not, so copying Java would copy the bug and following the text
// would fix it silently. The error comes before anything moves; bug says
// which case:
//
//   - jarkeldStrayBlock: General Jarkeld moves only the block on "those
//     attacking creatures", but Combat.removeFromCombat
//     (SwitchBlockEffect.java:88) drops every block the creature has, and
//     only the switched one comes back.
//   - sorrowStrayBlock: Sorrow's Path finds its attackers through the
//     blocked-by history (`blockedByValidThisTurn Targeted`), but
//     SwitchBlockEffect.java's own addBlocker calls (:97, :103, :151, :157)
//     never record it (BlockEffect.java:60-61 does). After an earlier
//     switch, a targeted blocker can block an attacker no record names;
//     Java leaves that attacker out, strips the blocker from it and never
//     gives it the other blocker.
//
// Only a live attacker counts: a stale Block naming a creature that already
// left the battlefield (Game.Move does not leave combat) is one Java's own
// combat no longer holds.
func (g *Game) checkStrayBlocks(blockers, switched []CardID, bug string) error {
	for _, b := range blockers {
		for _, at := range g.attackersBlockedBy(b) {
			if g.Card(at).Zone == Battlefield && !containsCard(switched, at) {
				return fmt.Errorf("engine: SwitchBlock: blocker %d also blocks attacker %d: %s", b, at, bug)
			}
		}
	}
	return nil
}

// blockedByHistoryDefined is Sorrow's Path's DefinedAttacker$, the one
// spelling that reads Card.blockedByThisTurn.
const blockedByHistoryDefined = "Valid Creature.blockedByValidThisTurn Targeted"

// checkStrayBlocks' two bug descriptions.
const (
	jarkeldStrayBlock = "Java's removeFromCombat would drop that block too (SwitchBlockEffect.java:88)"
	sorrowStrayBlock  = "no blocked-by record names it, so Java's DefinedAttacker$ scan would miss it " +
		"(SwitchBlockEffect.java:151,157 never record one)"
)

// blocks is Combat.isBlocking(blocker, attacker).
func (g *Game) blocks(blocker, attacker CardID) bool {
	return containsBlock(g.combat.Blocks, Block{Blocker: blocker, Attacker: attacker})
}

// attackersBlockedBy is Combat.getAttackersBlockedBy: every attacker
// blocker blocks, in block order.
func (g *Game) attackersBlockedBy(blocker CardID) []CardID {
	var out []CardID
	for _, b := range g.combat.Blocks {
		if b.Blocker == blocker {
			out = append(out, b.Attacker)
		}
	}
	return out
}
