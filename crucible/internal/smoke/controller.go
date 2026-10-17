// Package smoke measures how much of the card corpus the engine can play.
//
// A probe puts one card into a fixed two-player board, casts or plays it,
// resolves the stack, activates each of its abilities once and lets two turns
// run, with a Controller answering every decision. What the engine refuses
// (an unimplemented effect, a pending error, a recovered panic) is the
// finding; a card that goes through clean is "plays" (ADR-0011).
//
// "Plays" means no error surfaced on that path. The probe cannot see what
// the engine does silently wrong, so the number is an upper bound on what is
// supported, and a loud refusal (GO-7) is the only thing it can count.
package smoke

import (
	"math/bits"

	"github.com/jczastkiewicz/crucible/internal/cost"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Controller answers every engine decision deterministically and without a
// script: the first legal option, the smallest sensible count, "yes" to every
// optional question, attack with everything, never block. It passes priority
// when asked (the probe drives the casts itself).
//
// It is a measuring tool, not an AI: it only has to keep the engine moving so
// that a card's own abilities are exercised.
//
// "Yes" is bounded: a "you may repeat this" effect (Ad Nauseam, Pain's
// Reward) answered yes forever would loop in the engine, which is the
// controller's fault and not the engine's. After maxYes affirmative answers
// every optional question is answered no.
type Controller struct {
	yes int
	// blockAll makes the controller block each attacker with a creature of its
	// own, which a combat variant of the probe needs (a blocking creature to target).
	blockAll bool
	// blockEvery makes every creature the controller can block with block,
	// the attackers taken in turn, so that a Wall, a Zombie or a legendary
	// creature a staged variant placed blocks too (staged.go).
	blockEvery bool
}

// maxYes is how many optional questions a Controller answers yes to.
const maxYes = 40

var _ engine.PlayerController = (*Controller)(nil)

// affirm is the bounded "yes".
func (c *Controller) affirm() bool {
	c.yes++
	return c.yes <= maxYes
}

// clip bounds n to [0, limit].
func clip(n, limit int) int {
	if n < 0 {
		return 0
	}
	if n > limit {
		return limit
	}
	return n
}

// pick is how many of avail items a "lo..hi" choice takes: at least lo, and
// one when the choice is optional, so that the effect is exercised.
func pick(lo, hi, avail int) int {
	n := lo
	if n < 1 {
		n = 1
	}
	if n > hi {
		n = hi
	}
	return clip(n, avail)
}

func firstBit(c mana.Colors) mana.Colors { return c & -c }

// ChooseStartingPlayer takes the first turn itself.
func (*Controller) ChooseStartingPlayer(_ *engine.Game, decider engine.PlayerID, _ bool) engine.PlayerID {
	return decider
}

// ChooseStartingHand takes the first candidate hand.
func (*Controller) ChooseStartingHand(*engine.Game, engine.PlayerID, [][]engine.CardID) int { return 0 }

// MulliganKeepHand keeps every opening hand.
func (*Controller) MulliganKeepHand(*engine.Game, engine.PlayerID, engine.PlayerID, int) bool {
	return true
}

// TuckCardsViaMulligan tucks the first cards of the hand.
func (*Controller) TuckCardsViaMulligan(_ *engine.Game, _ engine.PlayerID, hand []engine.CardID, n int) []engine.CardID {
	return hand[:clip(n, len(hand))]
}

// ChooseLegendaryToKeep keeps the first legendary permanent.
func (*Controller) ChooseLegendaryToKeep(_ *engine.Game, _ engine.PlayerID, dups []engine.CardID) engine.CardID {
	return dups[0]
}

// DeclareCombatAttackers attacks with every eligible creature.
func (*Controller) DeclareCombatAttackers(_ *engine.Game, _ engine.PlayerID, eligible []engine.CardID) []engine.CardID {
	return eligible
}

// ExertAttackers exerts nothing.
func (*Controller) ExertAttackers(*engine.Game, engine.PlayerID, []engine.CardID) []engine.CardID {
	return nil
}

// ChooseAttackTarget attacks the first eligible defender.
func (*Controller) ChooseAttackTarget(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, eligible []engine.EntityID) engine.EntityID {
	return eligible[0]
}

// DeclareCombatBlockers never blocks, unless the probe set blockAll: then each
// attacker is blocked by the next eligible creature; or blockEvery: then each
// eligible creature blocks, the first the first attacker, the second the
// second, wrapping round.
func (c *Controller) DeclareCombatBlockers(_ *engine.Game, _ engine.PlayerID, attackers []engine.CardID, eligible []engine.CardID) []engine.Block {
	var out []engine.Block
	if c.blockEvery && len(attackers) > 0 {
		for i, b := range eligible {
			out = append(out, engine.Block{Blocker: b, Attacker: attackers[i%len(attackers)]})
		}
		return out
	}
	if !c.blockAll {
		return nil
	}
	for i, a := range attackers {
		if i < len(eligible) {
			out = append(out, engine.Block{Blocker: eligible[i], Attacker: a})
		}
	}
	return out
}

// AssignCombatDamage assigns one damage to each blocker.
func (*Controller) AssignCombatDamage(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, blockers []engine.CardID) []engine.DamageAssignment {
	out := make([]engine.DamageAssignment, 0, len(blockers))
	for _, b := range blockers {
		out = append(out, engine.DamageAssignment{Blocker: b, Amount: 1})
	}
	return out
}

// DiscardToHandSize discards the first cards of the hand.
func (*Controller) DiscardToHandSize(_ *engine.Game, _ engine.PlayerID, hand []engine.CardID, n int) []engine.CardID {
	return hand[:clip(n, len(hand))]
}

// ChooseCardsToDiscard discards the first cards of the hand.
func (*Controller) ChooseCardsToDiscard(_ *engine.Game, _ engine.PlayerID, hand []engine.CardID, n int) []engine.CardID {
	return hand[:clip(n, len(hand))]
}

// ArrangeForScry keeps every card on top.
func (*Controller) ArrangeForScry(_ *engine.Game, _ engine.PlayerID, topN []engine.CardID) (toTop, toBottom []engine.CardID) {
	return topN, nil
}

// ArrangeForSurveil keeps every card on top.
func (*Controller) ArrangeForSurveil(_ *engine.Game, _ engine.PlayerID, topN []engine.CardID) (toTop, toGraveyard []engine.CardID) {
	return topN, nil
}

// ChoosePermanentsToSacrifice takes the first candidates.
func (*Controller) ChoosePermanentsToSacrifice(_ *engine.Game, _ engine.PlayerID, c []engine.CardID, n int) []engine.CardID {
	return c[:clip(n, len(c))]
}

// ChoosePermanentsToTap takes the first candidates.
func (*Controller) ChoosePermanentsToTap(_ *engine.Game, _ engine.PlayerID, c []engine.CardID, n int) []engine.CardID {
	return c[:clip(n, len(c))]
}

// ChoosePermanentsToReturn takes the first candidates.
func (*Controller) ChoosePermanentsToReturn(_ *engine.Game, _ engine.PlayerID, c []engine.CardID, n int) []engine.CardID {
	return c[:clip(n, len(c))]
}

// ChooseTargets takes the first valid targets, at least one when the choice allows it.
func (*Controller) ChooseTargets(_ *engine.Game, _ engine.PlayerID, valid []engine.EntityID, lo, hi int) []engine.EntityID {
	return valid[:pick(lo, hi, len(valid))]
}

// DivideAmongTargets gives every target one and the rest to the first.
func (*Controller) DivideAmongTargets(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, targets []engine.EntityID, amount int) []int {
	shares := make([]int, len(targets))
	for i := range shares {
		shares[i] = 1
	}
	if len(shares) > 0 {
		shares[0] += amount - len(shares)
	}
	return shares
}

// ChooseBattleProtector picks the first eligible player.
func (*Controller) ChooseBattleProtector(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, eligible []engine.PlayerID) engine.PlayerID {
	return eligible[0]
}

// ChooseHybridManaColor picks the first colour offered.
func (*Controller) ChooseHybridManaColor(_ *engine.Game, _ engine.PlayerID, options mana.Colors) mana.Colors {
	return firstBit(options)
}

// ChoosePayMonocoloredHybrid pays the coloured half.
func (*Controller) ChoosePayMonocoloredHybrid(*engine.Game, engine.PlayerID, mana.Colors, int) bool {
	return true
}

// ChoosePayColorlessHybrid pays the coloured half.
func (*Controller) ChoosePayColorlessHybrid(*engine.Game, engine.PlayerID, mana.Colors) bool {
	return true
}

// ChoosePayPhyrexian pays mana, never life.
func (*Controller) ChoosePayPhyrexian(*engine.Game, engine.PlayerID, mana.Colors) bool { return false }

// ChoosePayHybridPhyrexian pays the first colour offered.
func (*Controller) ChoosePayHybridPhyrexian(_ *engine.Game, _ engine.PlayerID, colors mana.Colors) mana.Colors {
	return firstBit(colors)
}

// richestShard is the shard of the colour the player has most of floating,
// so a generic payment never runs out of one colour before the others.
func richestShard(g *engine.Game, pid engine.PlayerID) mana.Shard {
	pool := g.Player(pid).ManaPool
	shards := [...]mana.Shard{mana.ShardW, mana.ShardU, mana.ShardB, mana.ShardR, mana.ShardG, mana.ShardC}
	best, bestN := mana.ShardC, -1
	for i, n := range pool.Breakdown() {
		if n > bestN {
			best, bestN = shards[i], n
		}
	}
	return best
}

// ChoosePayGeneric pays generic mana with the colour floating most.
func (*Controller) ChoosePayGeneric(g *engine.Game, decider engine.PlayerID) mana.Shard {
	return richestShard(g, decider)
}

// ChoosePayX chooses X = 2.
func (*Controller) ChoosePayX(*engine.Game, engine.PlayerID, mana.Cost) int { return 2 }

// ChoosePaySnow pays snow mana with the colour floating most.
func (*Controller) ChoosePaySnow(g *engine.Game, decider engine.PlayerID) mana.Shard {
	return richestShard(g, decider)
}

// ChooseEnchantTarget picks the first eligible permanent.
func (*Controller) ChooseEnchantTarget(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, eligible []engine.CardID) engine.CardID {
	return eligible[0]
}

// ConfirmOptionalTrigger accepts, within the yes budget.
func (c *Controller) ConfirmOptionalTrigger(*engine.Game, engine.PlayerID, engine.CardID) bool {
	return c.affirm()
}

// ConfirmPayCost agrees to pay, within the yes budget.
func (c *Controller) ConfirmPayCost(*engine.Game, engine.PlayerID, cost.Cost, engine.CardID) bool {
	return c.affirm()
}

// ChooseManaColor picks the first colour offered.
func (*Controller) ChooseManaColor(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, options mana.Colors) mana.Colors {
	return firstBit(options)
}

// ChooseCardsForEffect takes the first options, at least one when the choice allows it.
func (*Controller) ChooseCardsForEffect(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, options []engine.CardID, lo, hi int) []engine.CardID {
	return options[:pick(lo, hi, len(options))]
}

// ChoosePlayerForEffect picks the first player.
func (*Controller) ChoosePlayerForEffect(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, options []engine.PlayerID) engine.PlayerID {
	return options[0]
}

// ChooseColors picks the first colours offered, at least one when the choice allows it.
func (*Controller) ChooseColors(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, options mana.Colors, lo, hi int) mana.Colors {
	var out mana.Colors
	for n := pick(lo, hi, bits.OnesCount8(uint8(options))); n > 0; n-- {
		bit := firstBit(options &^ out)
		out |= bit
	}
	return out
}

// ChooseNumber chooses 2, clamped to the range.
func (*Controller) ChooseNumber(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, lo, hi int) int {
	n := 2
	if n < lo {
		n = lo
	}
	if n > hi {
		n = hi
	}
	return n
}

// ChooseTapOrUntap taps, within the yes budget.
func (c *Controller) ChooseTapOrUntap(*engine.Game, engine.PlayerID, engine.CardID) bool {
	return c.affirm()
}

// ChooseEntitiesForEffect takes the first options, at least one when the choice allows it.
func (*Controller) ChooseEntitiesForEffect(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, options []engine.EntityID, lo, hi int) []engine.EntityID {
	return options[:pick(lo, hi, len(options))]
}

// ConfirmReveal reveals, within the yes budget.
func (c *Controller) ConfirmReveal(*engine.Game, engine.PlayerID, engine.CardID) bool {
	return c.affirm()
}

// ConfirmEffect accepts the optional effect, within the yes budget.
func (c *Controller) ConfirmEffect(*engine.Game, engine.PlayerID, engine.CardID) bool {
	return c.affirm()
}

// OrderCardsForZone keeps the order given.
func (*Controller) OrderCardsForZone(_ *engine.Game, _ engine.PlayerID, cards []engine.CardID, _ engine.ZoneType) []engine.CardID {
	return cards
}

func indices(n int) []int {
	out := make([]int, n)
	for i := range out {
		out[i] = i
	}
	return out
}

// ChooseAbilitiesForEffect takes the first abilities.
func (*Controller) ChooseAbilitiesForEffect(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, options []string, amount int) []int {
	return indices(clip(amount, len(options)))
}

// ChooseModesForAbility takes the first modes, at least one when the choice allows it.
func (*Controller) ChooseModesForAbility(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, options []string, lo, hi int) []int {
	return indices(pick(lo, hi, len(options)))
}

// ChooseProtectionType picks the first option.
func (*Controller) ChooseProtectionType(*engine.Game, engine.PlayerID, engine.CardID, []string) int {
	return 0
}

// CallCoinFlip calls heads, within the yes budget.
func (c *Controller) CallCoinFlip(*engine.Game, engine.PlayerID, engine.CardID) bool {
	return c.affirm()
}

// WillPutCardOnTop puts the card on top, within the yes budget.
func (c *Controller) WillPutCardOnTop(*engine.Game, engine.PlayerID, engine.CardID) bool {
	return c.affirm()
}

// ChooseBinary answers yes, within the yes budget.
func (c *Controller) ChooseBinary(*engine.Game, engine.PlayerID, engine.CardID, engine.BinaryChoice) bool {
	return c.affirm()
}

// ChooseOption picks the first option.
func (*Controller) ChooseOption(*engine.Game, engine.PlayerID, engine.CardID, []string) int { return 0 }

// ChooseSector picks the first sector.
func (*Controller) ChooseSector(*engine.Game, engine.PlayerID, engine.CardID, []string) int { return 0 }

// ChooseRoomDoor picks the first door.
func (*Controller) ChooseRoomDoor(_ *engine.Game, _ engine.PlayerID, _ engine.CardID, doors []engine.Door) engine.Door {
	return doors[0]
}

// ChooseReplacementEffect applies the first replacement effect.
func (*Controller) ChooseReplacementEffect(*engine.Game, engine.PlayerID, []engine.ReplacementOption) int {
	return 0
}

// ConfirmReplacementEffect accepts the replacement effect, within the yes budget.
func (c *Controller) ConfirmReplacementEffect(*engine.Game, engine.PlayerID, engine.CardID, string) bool {
	return c.affirm()
}

// TakeAction passes: the probe casts and activates itself, and the turns it
// lets run need no plays.
func (*Controller) TakeAction(*engine.Game, engine.PlayerID) engine.Action { return engine.Action{} }
