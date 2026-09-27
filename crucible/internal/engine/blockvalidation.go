// Block legality: CR 509.1b-c on a whole declaration of blockers (ADR-0024
// Decision 1), Java's own local approximation of it reproduced as it is
// (PORT-7).
//
// Ported from forge-game/src/main/java/forge/game/combat/CombatUtil.java:
// validateBlocks (:637), findFreeBlockers (:604), mustBlockAnAttacker
// (:745), attackerLureSatisfied, canBlock(attacker, blocker, combat),
// canBlock(blocker, combat), canBeBlocked (both overloads),
// canBlockMoreCreatures, canAttackerBeBlockedWithAmount; and from
// StaticAbilityCantAttackBlock.getMinMaxBlocker and
// StaticAbilityMustBlock.blocksEachCombatIfAble.
//
// Not CR 509.1c's maximum: Forge's own findFreeBlockers carries
// "TODO according to 509.1c, this should really check if the maximum
// possible is already fulfilled" (CombatUtil.java:602-603). A declaration
// is judged creature by creature -- a blocker with an unmet requirement it
// could have met by switching fails it -- not against the best achievable
// declaration. Parity with the oracle is what is measured, so the
// approximation is the rule here (ADR-0024 Considered Options 2).
//
// Java sources this port has none of, each read as its absent default: block
// costs (getBlockCost, Mode$ CantBlockUnless -- nil, so no requirement is
// excused by one), StaticAbilityBlockRestrict's per-player blocker limit,
// "can block an additional creature" (canBlockAny/canBlockAdditional -- so
// canBlockMoreCreatures is true only for a creature blocking nothing yet),
// and Mode$ CanBlockTapped.

package engine

import (
	"fmt"
	"math"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// blockCheck is the proposed combat validateBlocks reads -- Java's Combat
// with the declaration applied -- plus the static-ability answers its
// predicates need, computed up front so a static this port cannot evaluate
// is an error before any predicate runs.
type blockCheck struct {
	g      *Game
	blocks []Block
	// minMax is getMinMaxBlocker for every attacker and every creature a
	// MustBlock requirement names.
	minMax map[CardID][2]int
	// eachCombat is blocksEachCombatIfAble for the defender's creatures.
	eachCombat map[CardID]bool
}

// validateBlocks is CombatUtil.validateBlocks (CombatUtil.java:637) for
// defending, over blocks -- every defender's declaration so far, this one
// included. The first failure, in Java's order, is the error.
func (g *Game) validateBlocks(defending PlayerID, blocks []Block) error {
	army := g.creaturesInPlay(defending)
	v, err := g.newBlockCheck(blocks, army)
	if err != nil {
		return err
	}
	attackers := g.combat.Attackers
	var blockers []CardID
	for _, b := range v.allBlockers() {
		if g.Card(b).Controller() == defending {
			blockers = append(blockers, b)
		}
	}
	free := v.findFreeBlockers(army)

	for _, blocker := range army {
		must := g.Card(blocker).MustBlockAttackers()
		if len(must) > 0 {
			blockedSoFar := v.attackersBlockedBy(blocker)
			for _, card := range must {
				additional := v.minBlockers(card) - 1
				potential := 0
				// Java's own loop: the first pass already takes every free
				// blocker able to help, whatever additional is.
				for i := 0; i < additional; i++ {
					for _, fb := range append([]CardID(nil), free...) {
						if fb != blocker && g.CanBlock(card, fb) {
							free = withoutCards(free, []CardID{fb})
							potential++
						}
					}
				}
				if potential >= additional && !containsCard(blockedSoFar, card) &&
					(canBlockMoreCreatures(blockedSoFar) || containsCard(free, blocker)) &&
					g.combat.isAttacking(card) && g.CanBlock(card, blocker) {
					return illegal("CR 509.1c", "must still block", blocker, card)
				}
			}
		}
		if v.mustBlockAnAttacker(blocker, free, true) {
			reason := "must block an attacker, but has not been assigned to block any"
			if containsCard(blockers, blocker) {
				reason = "must block an attacker, but has not been assigned to block the right ones"
			}
			return illegal("CR 509.1c", reason, blocker)
		}
		if !containsCard(blockers, blocker) && v.eachCombat[blocker] {
			for _, attacker := range attackers {
				if !v.canBlockCombat(attacker, blocker) {
					continue
				}
				if v.minBlockers(attacker) > 1 {
					possible := append(append([]CardID(nil), free...), v.blockersOf(attacker)...)
					if !v.canBeBlockedBy(attacker, possible) {
						continue
					}
				}
				return illegal("CR 509.1c", "must block each combat but was not assigned to block any attacker", blocker)
			}
		}
	}

	for _, blocker := range blockers {
		b := g.Card(blocker)
		alone := b.hasKeywordText("CARDNAME can't attack or block alone.") || b.hasKeywordText("CARDNAME can't block alone.")
		switch {
		case len(blockers) < 2 && alone:
			return illegal("CR 509.1b", "can't block alone", blocker)
		case len(blockers) < 3 && b.hasKeywordText("CARDNAME can't block unless at least two other creatures block."):
			return illegal("CR 509.1b", "can't block unless at least two other creatures block", blocker)
		case b.hasKeywordText("CARDNAME can't block unless a creature with greater power also blocks."):
			ok, err := greaterPowerAlsoBlocks(g, blocker, blockers)
			if err != nil {
				return err
			}
			if !ok {
				return illegal("CR 509.1b", "can't block unless a creature with greater power also blocks", blocker)
			}
		}
	}

	for _, attacker := range attackers {
		if n := len(v.blockersOf(attacker)); n > 0 && !v.canBeBlockedWithAmount(attacker, n) {
			return illegal("CR 509.1b", fmt.Sprintf("cannot be blocked with %d creatures", n), attacker)
		}
	}
	return nil
}

// greaterPowerAlsoBlocks is validateBlocks' "a creature with greater power
// also blocks" test. An unresolvable power is an error, not a guess (GO-7).
func greaterPowerAlsoBlocks(g *Game, blocker CardID, blockers []CardID) (bool, error) {
	power, ok := g.Card(blocker).Power()
	if !ok {
		return false, fmt.Errorf("engine: blocker %d: power not resolvable", blocker)
	}
	for _, other := range blockers {
		p, ok := g.Card(other).Power()
		if !ok {
			return false, fmt.Errorf("engine: blocker %d: power not resolvable", other)
		}
		if p > power {
			return true, nil
		}
	}
	return false, nil
}

// newBlockCheck computes blockCheck's static-ability tables for blocks and
// the defending player's creatures (army).
func (g *Game) newBlockCheck(blocks []Block, army []CardID) (blockCheck, error) {
	v := blockCheck{g: g, blocks: blocks, minMax: map[CardID][2]int{}, eachCombat: map[CardID]bool{}}
	need := append([]CardID(nil), g.combat.Attackers...)
	for _, id := range army {
		need = append(need, g.Card(id).MustBlockAttackers()...)
		each, err := g.blocksEachCombatIfAble(id)
		if err != nil {
			return v, err
		}
		v.eachCombat[id] = each
	}
	for _, id := range need {
		if _, done := v.minMax[id]; done {
			continue
		}
		lo, hi, err := g.minMaxBlockers(id)
		if err != nil {
			return v, err
		}
		v.minMax[id] = [2]int{lo, hi}
	}
	return v, nil
}

// minMaxBlockers is StaticAbilityCantAttackBlock.getMinMaxBlocker against
// attacker's own defender: at least one blocker and no maximum, at least
// two for Menace (CR 702.111b, hardcoded in Java too), then every Mode$
// MinMaxBlocker static whose ValidCard$ matches overriding Min$ (a number,
// or All: every creature the defender controls) and Max$.
func (g *Game) minMaxBlockers(attacker CardID) (lo, hi int, err error) {
	lo, hi = 1, math.MaxInt
	if g.Card(attacker).HasKeyword("Menace") {
		lo = 2
	}
	err = g.eachCombatStatic("MinMaxBlocker", func(h *Card, face *compile.Face, s *compile.Ability) error {
		if !staticValidMatches(g, h, s, "ValidCard", attacker) {
			return nil
		}
		if raw, ok := s.Param("Min"); ok {
			if raw == "All" {
				// attacker reaches here via MustBlockAttackers() too, which
				// does not require attacking (a MustBlock host or
				// DefinedAttacker$ that never attacked): defenderOf panics
				// on a non-attacker, and "every creature the defender
				// controls" has no defender to count for one, so Min$ All
				// is a no-op (lo stays 1) rather than a crash.
				if !g.combat.isAttacking(attacker) {
					return nil
				}
				lo = len(g.creaturesInPlay(g.defenderOf(attacker)))
			} else if lo, ok = resolveNamedAmount(g, face.Amounts, h, raw); !ok {
				return fmt.Errorf("engine: Mode$ MinMaxBlocker: Min$ %q not resolvable", raw)
			}
		}
		if raw, ok := s.Param("Max"); ok {
			if hi, ok = resolveNamedAmount(g, face.Amounts, h, raw); !ok {
				return fmt.Errorf("engine: Mode$ MinMaxBlocker: Max$ %q not resolvable", raw)
			}
		}
		return nil
	})
	return lo, hi, err
}

// blocksEachCombatIfAble is StaticAbilityMustBlock.blocksEachCombatIfAble:
// some Mode$ MustBlock static in play names creature in ValidCreature$.
func (g *Game) blocksEachCombatIfAble(creature CardID) (bool, error) {
	found := false
	err := g.eachCombatStatic("MustBlock", func(h *Card, _ *compile.Face, s *compile.Ability) error {
		if staticValidMatches(g, h, s, "ValidCreature", creature) {
			found = true
		}
		return nil
	})
	return found, err
}

// blockersOf is Combat.getBlockers(attacker): a fresh list, in declaration
// order.
func (v blockCheck) blockersOf(attacker CardID) []CardID {
	var out []CardID
	for _, b := range v.blocks {
		if b.Attacker == attacker {
			out = append(out, b.Blocker)
		}
	}
	return out
}

// attackersBlockedBy is Combat.getAttackersBlockedBy(blocker).
func (v blockCheck) attackersBlockedBy(blocker CardID) []CardID {
	var out []CardID
	for _, b := range v.blocks {
		if b.Blocker == blocker {
			out = append(out, b.Attacker)
		}
	}
	return out
}

// allBlockers is Combat.getAllBlockers: each blocker once.
func (v blockCheck) allBlockers() []CardID {
	var out []CardID
	for _, b := range v.blocks {
		if !containsCard(out, b.Blocker) {
			out = append(out, b.Blocker)
		}
	}
	return out
}

func (v blockCheck) minBlockers(attacker CardID) int { return v.minMax[attacker][0] }

// canBlockMoreCreatures is CombatUtil.canBlockMoreCreatures with no
// "can block an additional creature" source: only a creature blocking
// nothing yet can block more.
func canBlockMoreCreatures(blockedBy []CardID) bool { return len(blockedBy) == 0 }

// canBlockInCombat is CombatUtil.canBlock(blocker, combat).
func (v blockCheck) canBlockInCombat(blocker CardID) bool {
	return canBlockMoreCreatures(v.attackersBlockedBy(blocker)) && v.g.canBlockAtAll(blocker)
}

// canBeBlockedInCombat is CombatUtil.canBeBlocked(attacker, combat,
// defendingPlayer): not already at its maximum blockers, and attacking
// defendingPlayer or something defendingPlayer controls (CR 802.4a). Java's
// third test, StaticAbilityCantAttackBlock.cantBlockBy(attacker, null), is
// false for every static (applyCantBlockByAbility returns false for a null
// blocker, StaticAbilityCantAttackBlock.java:269) and is not ported.
func (v blockCheck) canBeBlockedInCombat(attacker CardID, defendingPlayer PlayerID) bool {
	if v.minMax[attacker][1] == len(v.blockersOf(attacker)) {
		return false
	}
	// A card this port reaches through MustBlockAttackers() need not be
	// attacking (a MustBlock host or DefinedAttacker$ that never attacked
	// itself): CombatUtil.canBeBlocked returns true when Combat.getDefenderByAttacker
	// answers null (CombatUtil.java:526-527, "attacked == null"), skipping
	// the CR 802.4a check entirely rather than asking defenderOf, which
	// panics on a non-attacker.
	if !v.g.combat.isAttacking(attacker) {
		return true
	}
	return v.g.defenderOf(attacker) == defendingPlayer
}

// canBeBlockedBy is CombatUtil.canBeBlocked(attacker, blockers, combat):
// how many of blockers CanBlock accepts, checked against attacker's
// blocker-count limits.
func (v blockCheck) canBeBlockedBy(attacker CardID, blockers []CardID) bool {
	n := 0
	for _, b := range blockers {
		if v.g.CanBlock(attacker, b) {
			n++
		}
	}
	return v.canBeBlockedWithAmount(attacker, n)
}

// canBeBlockedWithAmount is CombatUtil.canAttackerBeBlockedWithAmount.
func (v blockCheck) canBeBlockedWithAmount(attacker CardID, n int) bool {
	mm := v.minMax[attacker]
	return n != 0 && mm[0] <= n && n <= mm[1]
}

// canBlockCombat is CombatUtil.canBlock(attacker, blocker, combat): the
// combat-aware pairing test, lure included -- a creature that must block a
// lure attacker can't block one without a lure.
func (v blockCheck) canBlockCombat(attacker, blocker CardID) bool {
	g := v.g
	if !v.canBlockInCombat(blocker) || !v.canBeBlockedInCombat(attacker, g.Card(blocker).Controller()) {
		return false
	}
	blockers := v.blockersOf(attacker)
	if containsCard(blockers, blocker) {
		return false
	}
	a := g.Card(attacker)
	mustBeBlockedBy := false
	for _, line := range a.KeywordLines() {
		if spec, ok := strings.CutPrefix(line, "MustBeBlockedBy "); ok {
			if validCardMatches(g, blocker, spec) && validCardCount(g, blockers, spec) == 0 {
				mustBeBlockedBy = true
				break
			}
		}
		if rest, ok := strings.CutPrefix(line, "MustBeBlockedByAll"); ok {
			// Corpus shape is "MustBeBlockedByAll:<spec>:<description>"
			// (marble_priest.txt, talruum_piper.txt) -- Java reads only the
			// second colon-separated segment (keyword.split(":")[1]); a
			// plain Cut on the first ":" would fold the description text
			// into spec too.
			if _, rest, ok := strings.Cut(rest, ":"); ok {
				spec, _, _ := strings.Cut(rest, ":")
				if validCardMatches(g, blocker, spec) {
					mustBeBlockedBy = true
					break
				}
			}
		}
	}
	lure := a.hasKeywordText("All creatures able to block CARDNAME do so.") ||
		(a.hasKeywordText("CARDNAME must be blocked if able.") && len(blockers) == 0) ||
		(a.hasKeywordText("CARDNAME must be blocked by exactly one creature if able.") && len(blockers) != 1) ||
		(a.hasKeywordText("CARDNAME must be blocked by two or more creatures if able.") && len(blockers) < 2)
	if !lure && !containsCard(g.Card(blocker).MustBlockAttackers(), attacker) && !mustBeBlockedBy &&
		v.mustBlockAnAttacker(blocker, nil, false) {
		return false
	}
	return g.CanBlock(attacker, blocker)
}

// findFreeBlockers is CombatUtil.findFreeBlockers (CombatUtil.java:604):
// the defender's creatures that could help meet a requirement without
// breaking another -- able to block, under no lure or MustBlock
// requirement of their own, and either blocking nothing or blocking an
// attacker that stays legally blocked without them.
func (v blockCheck) findFreeBlockers(army []CardID) []CardID {
	var free []CardID
	for _, blocker := range army {
		if !v.g.canBlockAtAll(blocker) || v.mustBlockAnAttacker(blocker, nil, false) {
			continue
		}
		blocked := v.attackersBlockedBy(blocker)
		change := len(blocked) == 0
		for _, attacker := range blocked {
			reduced := withoutCards(v.blockersOf(attacker), []CardID{blocker})
			if canBlockMoreCreatures(blocked) || v.canBeBlockedBy(attacker, reduced) {
				change = true
				break
			}
		}
		if change {
			free = append(free, blocker)
		}
	}
	return free
}

// mustBlockAnAttacker is CombatUtil.mustBlockAnAttacker
// (CombatUtil.java:745): blocker is under a lure or MustBlock requirement it
// could meet and does not. free is findFreeBlockers' list when hasFree --
// Java's freeBlockers argument, null otherwise.
func (v blockCheck) mustBlockAnAttacker(blocker CardID, free []CardID, hasFree bool) bool {
	g := v.g
	defender := g.Card(blocker).Controller()
	var required []CardID
	for _, attacker := range g.combat.Attackers {
		if v.attackerLureSatisfied(attacker, blocker, v.blockersOf(attacker)) {
			continue
		}
		if v.canBeBlockedInCombat(attacker, defender) && g.CanBlock(attacker, blocker) &&
			v.canStillBeBlocked(attacker, blocker, g.creaturesInPlay(g.defenderOf(attacker))) {
			required = append(required, attacker)
		}
	}
	for _, attacker := range g.Card(blocker).MustBlockAttackers() {
		if !v.canBeBlockedInCombat(attacker, defender) || !g.CanBlock(attacker, blocker) || !g.combat.isAttacking(attacker) {
			continue
		}
		pool := g.creaturesInPlay(g.defenderOf(attacker))
		if hasFree {
			pool = free
		}
		if v.canStillBeBlocked(attacker, blocker, pool) {
			required = append(required, attacker)
		}
	}
	if len(required) == 0 {
		return false
	}
	blockedBy := v.attackersBlockedBy(blocker)
	if containsAllCards(blockedBy, required) {
		return false
	}
	if !v.canBlockInCombat(blocker) {
		// It can't block more, but is it part of another requirement?
		for _, attacker := range g.combat.Attackers {
			blockers := v.blockersOf(attacker)
			if v.attackerLureSatisfied(attacker, blocker, blockers) && containsCard(blockers, blocker) &&
				!v.attackerLureSatisfied(attacker, blocker, withoutCards(blockers, []CardID{blocker})) {
				return false
			}
		}
	}
	for _, a := range blockedBy {
		if containsCard(required, a) {
			return false
		}
	}
	return true
}

// canStillBeBlocked is mustBlockAnAttacker's "if the attacker can only be
// blocked with multiple creatures, check that's possible" step: pool less
// blocker must still be able to block attacker legally.
func (v blockCheck) canStillBeBlocked(attacker, blocker CardID, pool []CardID) bool {
	if v.minBlockers(attacker) <= 1 {
		return true
	}
	return v.canBeBlockedBy(attacker, withoutCards(pool, []CardID{blocker}))
}

// attackerLureSatisfied is CombatUtil.attackerLureSatisfied: false while
// attacker's lure keyword still wants blocker -- "All creatures able to
// block CARDNAME do so.", a must-be-blocked count not yet met, or a
// MustBeBlockedBy/MustBeBlockedByAll keyword naming blocker.
func (v blockCheck) attackerLureSatisfied(attacker, blocker CardID, blockers []CardID) bool {
	g := v.g
	a := g.Card(attacker)
	if a.hasKeywordTextPrefix("All creatures able to block CARDNAME do so.") ||
		(a.hasKeywordTextPrefix("CARDNAME must be blocked if able.") && len(blockers) == 0) ||
		(a.hasKeywordTextPrefix("CARDNAME must be blocked by exactly one creature if able.") && len(blockers) != 1) ||
		(a.hasKeywordTextPrefix("CARDNAME must be blocked by two or more creatures if able.") && len(blockers) < 2) {
		return false
	}
	for _, line := range a.KeywordLines() {
		if spec, ok := strings.CutPrefix(line, "MustBeBlockedBy "); ok {
			if validCardMatches(g, blocker, spec) && validCardCount(g, blockers, spec) == 0 {
				return false
			}
		}
		if rest, ok := strings.CutPrefix(line, "MustBeBlockedByAll"); ok {
			// Same "second colon-separated segment" shape as canBlockCombat.
			if _, rest, ok := strings.Cut(rest, ":"); ok {
				spec, _, _ := strings.Cut(rest, ":")
				if validCardMatches(g, blocker, spec) {
					return false
				}
			}
		}
	}
	return true
}

// validCardMatches is Card.isValid(spec, null, null, null): no source card
// or controller.
func validCardMatches(g *Game, id CardID, spec string) bool {
	return Matches(g, g.Card(id), valid.Parse(spec), NoPlayer, NoCard)
}

// validCardCount is CardLists.getValidCardCount(list, spec, null, ...).
func validCardCount(g *Game, list []CardID, spec string) int {
	n := 0
	for _, id := range list {
		if validCardMatches(g, id, spec) {
			n++
		}
	}
	return n
}

// containsAllCards reports whether have holds every card in want.
func containsAllCards(have, want []CardID) bool {
	for _, id := range want {
		if !containsCard(have, id) {
			return false
		}
	}
	return true
}

// hasKeywordText is Card.hasKeyword for a keyword written as a whole line
// ("CARDNAME can't block."): some hidden keyword (Card.java:4981's
// shortcut) or current keyword line is exactly line.
func (c *Card) hasKeywordText(line string) bool {
	for _, l := range c.hiddenKeywords {
		if l == line {
			return true
		}
	}
	for _, l := range c.KeywordLines() {
		if l == line {
			return true
		}
	}
	return false
}

// hasKeywordTextPrefix is Card.hasStartOfKeyword: some hidden keyword or
// current keyword line starts with prefix.
func (c *Card) hasKeywordTextPrefix(prefix string) bool {
	for _, l := range c.hiddenKeywords {
		if strings.HasPrefix(l, prefix) {
			return true
		}
	}
	for _, l := range c.KeywordLines() {
		if strings.HasPrefix(l, prefix) {
			return true
		}
	}
	return false
}
