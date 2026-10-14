// Combat damage: CR 510.1-510.4 (the first strike sub-step included), CR
// 702.19 (trample).

package engine

import (
	"strconv"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/keyword"
)

// DealFirstStrikeDamage is CR 510.4's first sub-step: only creatures with
// first strike or double strike deal damage. A safe no-op when nothing in
// combat has either keyword -- a fixture need not check first before
// calling it, the same "the game decides what is legal" reasoning
// `Game.DeclareCombatAttackers` already applies to an empty eligible list.
//
// This and `DealCombatDamage` share `dealCombatDamageStep`; see its own doc
// comment for the shape both steps follow.
func (g *Game) DealFirstStrikeDamage(controller PlayerController) {
	g.dealCombatDamageStep(controller, true)
}

// DealCombatDamage is CR 510.4's regular step: every creature without first
// strike deals damage, and every creature with double strike deals damage
// again. When nothing in combat has first strike or double strike, this is
// also the entirety of CR 510.1-510.3 -- the ordinary case, and the only
// step most fixtures need to call.
//
// See `dealCombatDamageStep`'s doc comment for what each exchange computes,
// including trample (CR 702.19).
func (g *Game) DealCombatDamage(controller PlayerController) {
	g.dealCombatDamageStep(controller, false)
}

// dealCombatDamageStep is one damage sub-step, first-strike or regular
// (`firstStrike` selects which). A creature deals damage in a step per
// `dealsInStep`'s doc comment.
//
// All of a step's damage is simultaneous (CR 510.2). Applying one attacker's
// exchange at a time produces the same life totals as computing every amount
// first and applying them together, since no state-based action runs between
// exchanges; checkDamageDoneTriggersToCard/ToPlayer (trigger.go) fire per
// exchange for the identical reason. The two places simultaneity does matter
// run off the step's shared damageTable, consulted once after the whole step
// finishes (checkDamageTableTriggers, trigger.go): Mode$ DamageDoneOnce, where
// a target hit by more than one source in the same step (a gang-blocked
// attacker, several unblocked attackers hitting the same player) sees the
// combined total in one trigger firing rather than one per source, and
// lifelink (applyLifelink, lifelink.go), one gain per source for everything it
// dealt in the step.
//
// An unblocked attacker deals its power to whatever it's attacking (CR
// 508.1d) -- a player, planeswalker or battle, via dealAttackTargetDamage.
// A blocked attacker with exactly one live blocker and no trample exchanges full power for
// full power automatically, no decision needed, the same "nothing
// meaningful to decide" reasoning DeclareCombatAttackers/Blockers use for
// an empty eligible list. A gang-blocked attacker (more than one live
// blocker) asks the attacking player how to divide its power among them
// (AssignCombatDamage, CR 510.1c) -- trusted the same way
// ChooseLegendaryToKeep's answer is, including the "lethal before moving
// on" ordering constraint CR 510.1c itself imposes.
//
// Trample (CR 702.19) changes only how much of a blocked attacker's power
// reaches its blocker(s) versus what it's attacking, not who decides:
// against a single live blocker, `lethalDamage` computes the minimum this
// port can assign it (the game deciding, since no decision was being asked
// in that case anyway) and the rest tramples over, unless lethal can't be
// computed at all (`lethalDamage`'s own doc comment) -- against a
// gang-blocked attacker, whatever the controller's AssignCombatDamage
// answer leaves unassigned across all named blockers tramples over instead
// of being wasted. A non-trampler's unassigned remainder is wasted exactly
// as before this method existed.
//
// A creature that has left the battlefield since DeclareCombatBlockers --
// reachable now that first strike damage can kill a creature before the
// regular step runs, CR 510.1c's "attacker remains blocked but a removed
// blocker gets nothing" exception -- deals no damage and receives none: an
// attacker checks `g.alive` before doing anything at all, and dead blockers
// are filtered out of its list before either side of the exchange runs.
func (g *Game) dealCombatDamageStep(controller PlayerController, firstStrike bool) {
	blockersOf := make(map[CardID][]CardID, len(g.combat.Blocks))
	for _, b := range g.combat.Blocks {
		blockersOf[b.Attacker] = append(blockersOf[b.Attacker], b.Blocker)
	}

	var table damageTable
	for _, atkID := range g.combat.Attackers {
		if !g.alive(atkID) {
			continue
		}
		atk := g.Card(atkID)
		declaredBlockers := blockersOf[atkID]
		var blockers []CardID
		for _, id := range declaredBlockers {
			if g.alive(id) {
				blockers = append(blockers, id)
			}
		}

		if g.dealsInStep(atk, firstStrike) {
			g.noteFirstStrikeDealer(atkID, firstStrike)
			if power, ok := netCombatDamage(g, atk); ok && power > 0 {
				g.dealAttackerDamage(controller, atkID, power, blockers, len(declaredBlockers) == 0 && !containsCard(g.combat.ForcedBlocked, atkID), &table)
			}
		}

		for _, blkID := range blockers {
			blk := g.Card(blkID)
			if g.dealsInStep(blk, firstStrike) {
				g.noteFirstStrikeDealer(blkID, firstStrike)
				if bp, ok := netCombatDamage(g, blk); ok && bp > 0 {
					g.dealPermanentDamage(controller, blkID, atkID, bp, blk.HasKeyword("Deathtouch"), true, &table)
				}
			}
		}
	}
	g.checkDamageTableTriggers(controller, table, true)
	if !firstStrike {
		// Combat.assignCombatDamage(false) clears the list "since it doesn't
		// matter anymore" (Combat.java:921-923).
		g.combat.dealtFirstStrike = nil
	}
}

// noteFirstStrikeDealer records id as having taken part in the first-strike
// step (Combat.java:716, 774), once.
func (g *Game) noteFirstStrikeDealer(id CardID, firstStrike bool) {
	if firstStrike && !containsCard(g.combat.dealtFirstStrike, id) {
		g.combat.dealtFirstStrike = append(g.combat.dealtFirstStrike, id)
	}
}

// combatDamageAssigned is Combat.assignCombatDamage's return value
// (Combat.java:919-925), computed before any damage is dealt: some live
// attacker dealing damage in this step has power above zero, or some live
// blocker dealing damage in this step still blocks a live attacker.
// dealsInStep reads the per-combatant "dealt first-strike damage" set
// (Combat.java:906-917).
func (g *Game) combatDamageAssigned(firstStrike bool) bool {
	for _, id := range g.combat.Attackers {
		c := g.Card(id)
		if !g.alive(id) || !g.dealsInStep(c, firstStrike) {
			continue
		}
		if power, ok := netCombatDamage(g, c); ok && power > 0 {
			return true
		}
	}
	for _, b := range g.combat.Blocks {
		if g.alive(b.Blocker) && g.alive(b.Attacker) && g.dealsInStep(g.Card(b.Blocker), firstStrike) {
			return true
		}
	}
	return false
}

// dealsInStep reports whether c deals damage in the first-strike step
// (firstStrike true) or the regular step (false) -- CR 510.4, 702.4b, 702.7c:
// double strike acts in both, first strike (alone) only in the first, and
// everything else only in the regular one.
func (g *Game) dealsInStep(c *Card, firstStrike bool) bool {
	if c.HasKeyword("Double Strike") {
		return true
	}
	if firstStrike {
		return c.HasKeyword("First Strike")
	}
	// Combat.dealDamageThisPhase (Combat.java:906-917): anyone who did not
	// take part in the first-strike step, whatever keywords they hold now.
	return !containsCard(g.combat.dealtFirstStrike, c.ID)
}

// alive reports whether id is still the card it was declared into combat as
// -- on the battlefield, not replaced by a state-based action that ran
// since (most likely the previous damage sub-step's own kills).
func (g *Game) alive(id CardID) bool {
	return g.Card(id).Zone == Battlefield
}

// dealAttackerDamage is attacker's half of dealCombatDamageStep's exchange:
// where its power goes, given liveBlockers (already filtered to what's
// still on the battlefield) and wasUnblocked (true only when the attacker
// was never blocked at all, as opposed to blocked by creatures that have
// since died -- CR 510.1c treats the two differently). Damage that reaches
// past every blocker goes to whatever attacker is attacking (CR 508.1d) --
// a player, a planeswalker, or a battle -- not always the defending player,
// via dealAttackTargetDamage.
func (g *Game) dealAttackerDamage(controller PlayerController, attacker CardID, power int, liveBlockers []CardID, wasUnblocked bool, table *damageTable) {
	atk := g.Card(attacker)
	deathtouch := atk.HasKeyword("Deathtouch")
	trample := atk.HasKeyword("Trample")

	switch len(liveBlockers) {
	case 0:
		// Unblocked always hits its target (CR 510.1a). Blocked with every
		// blocker since dead hits it too, but only with trample (CR
		// 702.19e) -- without it, CR 510.1c leaves the attacker dealing
		// nothing at all, not even to what it's attacking.
		if wasUnblocked || trample {
			g.dealAttackTargetDamage(controller, attacker, power, deathtouch, table)
		}

	case 1:
		blocker := liveBlockers[0]
		toBlocker := power
		if trample {
			if lethal, ok := lethalDamage(g.Card(blocker), deathtouch); ok && lethal < power {
				toBlocker = lethal
			}
		}
		g.dealPermanentDamage(controller, attacker, blocker, toBlocker, deathtouch, true, table)
		if trample && power > toBlocker {
			g.dealAttackTargetDamage(controller, attacker, power-toBlocker, deathtouch, table)
		}

	default:
		assigned := 0
		for _, a := range controller.AssignCombatDamage(g, atk.Controller(), attacker, liveBlockers) {
			g.dealPermanentDamage(controller, attacker, a.Blocker, a.Amount, deathtouch, true, table)
			assigned += a.Amount
		}
		if trample && power > assigned {
			g.dealAttackTargetDamage(controller, attacker, power-assigned, deathtouch, table)
		}
	}
}

// dealAttackTargetDamage sends amount to whatever attacker is attacking (CR
// 508.1d, attack.go) -- a player (dealPlayerDamage) or a planeswalker/battle
// (dealPermanentDamage) -- rather than assuming it's always the defending
// player directly, the way this port's combat did before a target could be
// anything else.
func (g *Game) dealAttackTargetDamage(controller PlayerController, attacker CardID, amount int, deathtouch bool, table *damageTable) {
	target := g.combat.AttackTargets[attacker]
	if pid, ok := target.AsPlayer(); ok {
		g.dealPlayerDamage(controller, attacker, pid, amount, true, table)
		return
	}
	cid, _ := target.AsCard()
	g.dealPermanentDamage(controller, attacker, cid, amount, deathtouch, true, table)
}

// lethalDamage is CR 510.1c/702.19c's "lethal damage": 1 from a deathtouch
// source, otherwise however much toughness the target has left this turn.
// The second return value is false when that can't be computed at all (an
// unresolvable toughness -- `Toughness`'s own `*`/Count$ gap, game-state.md);
// its caller treats that the same as "not trampling this blocker" -- full
// power assigned to it, nothing guessed at -- rather than picking a lethal
// amount that might be wrong in either direction.
func lethalDamage(target *Card, deathtouch bool) (int, bool) {
	if deathtouch {
		return 1, true
	}
	t, ok := target.Toughness()
	if !ok {
		return 0, false
	}
	remaining := t - target.Damage.Marked
	if remaining < 0 {
		remaining = 0
	}
	return remaining, true
}

// dealPermanentDamage marks amount on target -- Damage if it's a creature,
// loyalty or defense counters removed if it's a planeswalker or battle (CR
// 120.3c, 121.5) -- sourced from source, emits the DamageDealt event, and
// checks CR 603's own "whenever ~ deals damage" trigger
// (checkDamageDoneTriggersToCard, trigger.go). A permanent can be more than
// one of these (a creature planeswalker); each check runs independently
// rather than picking one, the same as Forge's own
// Card.addDamageAfterPrevention does.
//
// CR 614's own "prevent all of this damage" replacement effects are checked
// first (damagePrevented, replacement.go): a prevented instance never
// happened, so nothing below it -- marking, the event, the trigger check --
// runs at all, the identical short-circuit Java's own
// ReplacementResult.Prevented gives it. damageReplaced (replacement.go, CR
// 616's own "Updated" outcome) runs next -- "prevent N of that damage"
// reduces amount rather than skipping the event outright, so the SAME
// DamageDealt/trigger check below still runs, just against the smaller
// number, unless the reduction brings it to zero or below, which folds into
// the identical "nothing happens" return damagePrevented already gives.
//
// isCombat is threaded through rather than assumed: every call site until
// dealDamageEffect (dealdamageeffect.go, M6's own DealDamage) was combat's
// own, so it carried a bare `true` inline until a script-driven caller
// needed `false` instead -- FlagCombat's own doc comment already drew the
// line ("marks damage dealt in combat rather than by an effect") before
// anything but combat could produce the second case.
//
// table, when non-nil, records the actual (post-prevention, post-
// replacement) amount dealt -- Mode$ DamageDoneOnce's own batch
// (checkDamageDoneOnceTriggers, trigger.go, damageTable's own doc comment,
// above), consumed once by the caller after every exchange in one
// damage-dealing action has run, not per exchange the way
// checkDamageDoneTriggersToCard already is. Every real call site this port
// has today does build one (dealCombatDamageStep, dealDamageEffect); the
// nil case exists only so a future caller that genuinely has no batch to
// report to can opt out rather than being forced to build an empty one.
func (g *Game) dealPermanentDamage(controller PlayerController, source, target CardID, amount int, deathtouch, isCombat bool, table *damageTable) {
	if amount <= 0 {
		return
	}
	preventable := g.canDamagePrevented(source, isCombat)
	if preventable && g.damagePrevented(source, target, isCombat, amount) {
		return
	}
	if preventable {
		if amount = g.applyPreventShields(CardEntity(target), amount); amount <= 0 {
			return
		}
	}
	amount, redirect, redirected := g.damageReplaced(controller, source, target, isCombat, amount)
	g.dealRedirectedDamage(controller, source, redirect, redirected, isCombat, table)
	if amount <= 0 {
		return
	}
	g.Card(source).damageDealtThisTurn += amount
	c := g.Card(target)
	// GameAction.java:2750-2752: what was lethal before this damage, for the
	// batch's excess (bindExcessSVar).
	lethal := excessDamageValue(c, false)
	t := c.Type()
	c.Damage.Sources = append(c.Damage.Sources, source)
	if t.Has(cardtype.Planeswalker) {
		c.Counters.Add(Loyalty, -amount)
		emitCounterChanged(g, source, CardEntity(target), Loyalty, -amount)
	}
	if t.Has(cardtype.Battle) {
		c.Counters.Add(Defense, -amount)
		emitCounterChanged(g, source, CardEntity(target), Defense, -amount)
	}
	if t.Has(cardtype.Creature) {
		if g.witherDamage(source) {
			// CR 120.3d: damage from a source with wither or infect is
			// dealt as -1/-1 counters, not marked damage; the deathtouch flag
			// is set either way (Card.addDamageAfterPrevention).
			if n := g.countersReplaced(controller, g.Card(source).Controller(), CardEntity(target), M1M1, amount); n > 0 {
				c.Counters.Add(M1M1, n)
				emitCounterChanged(g, source, CardEntity(target), M1M1, n)
			}
			if deathtouch {
				c.Damage.Deathtouch = true
			}
		} else {
			c.Damage.Mark(amount, deathtouch)
		}
	}
	var flags EventFlags
	if isCombat {
		flags |= FlagCombat
	}
	if deathtouch {
		flags |= FlagDeathtouch
	}
	g.sink.Emit(Event{Kind: DamageDealt, Source: source, Target: CardEntity(target), Amount: int32(amount), Flags: flags})
	g.checkDamageDoneTriggersToCard(controller, source, target, amount, isCombat)
	if table != nil {
		*table = append(*table, damageEntry{Source: source, Target: CardEntity(target), Amount: amount, Lethal: lethal})
	}
}

// dealPlayerDamage reduces target's life by amount, emits DamageDealt and
// LifeChanged -- both, because Java's own combat damage step fires the
// equivalent of each separately and nothing downstream should have to derive
// one from the other -- and checks CR 603's own "whenever ~ deals damage"
// trigger (checkDamageDoneTriggersToPlayer, trigger.go), ValidTarget matched
// against a Player rather than a Card this time. Every call site already
// guards amount > 0 itself (unlike dealCreatureDamage, whose amount can come
// straight from an untrusted AssignCombatDamage answer), so there's nothing
// to re-check here.
//
// CR 614's own "prevent all of this damage" replacement effects are checked
// first (damagePreventedPlayer, replacement.go), the identical short-circuit
// dealPermanentDamage's own doc comment gives its own card-target twin.
// damageReplacedPlayer (replacement.go) runs next, the identical "Updated"
// outcome dealPermanentDamage's own doc comment already gives -- amount can
// reach zero here even though every call site guards the ORIGINAL amount
// being positive, since a reduction can bring it down after the fact.
// isCombat is threaded through the identical reason dealPermanentDamage's
// own doc comment gives. table is dealPermanentDamage's own doc comment,
// the identical contract for a player-shaped target.
func (g *Game) dealPlayerDamage(controller PlayerController, source CardID, target PlayerID, amount int, isCombat bool, table *damageTable) {
	preventable := g.canDamagePrevented(source, isCombat)
	if preventable && g.damagePreventedPlayer(source, target, isCombat, amount) {
		return
	}
	if preventable {
		if amount = g.applyPreventShields(PlayerEntity(target), amount); amount <= 0 {
			return
		}
	}
	amount, redirect, redirected := g.damageReplacedPlayer(controller, source, target, isCombat, amount)
	g.dealRedirectedDamage(controller, source, redirect, redirected, isCombat, table)
	if amount <= 0 {
		return
	}
	g.Card(source).damageDealtThisTurn += amount
	g.Player(target).DamageReceivedThisTurn += amount
	kind := 0
	if isCombat {
		kind = 1
	}
	g.Player(target).damageThisTurn[kind] += amount
	g.recordPlayerDamage(source, target, amount, isCombat)
	var flags EventFlags
	if isCombat {
		flags = FlagCombat
	}
	// CR 120.3b: infect damage to a player is that many poison counters
	// instead of life loss, and CR 702.164c's toxic adds its magnitude in
	// poison counters to combat damage (Player.addDamageAfterPrevention).
	poison := 0
	lost := 0
	if g.Card(source).HasKeyword("Infect") {
		poison += amount
	} else if !g.cantLoseLife(target) {
		// Player.addDamageAfterPrevention's loseLife(damage, true): the
		// Event$ LifeReduced replacement run, whose answer is what is lost.
		lost = g.lifeReduced(controller, target, amount, true)
		g.Player(target).Life -= lost
	}
	if isCombat {
		poison += toxicMagnitude(g.Card(source))
	}
	g.sink.Emit(Event{Kind: DamageDealt, Source: source, Target: PlayerEntity(target), Amount: int32(amount), Flags: flags})
	if !g.Card(source).HasKeyword("Infect") && lost > 0 {
		g.sink.Emit(Event{Kind: LifeChanged, Source: source, Target: PlayerEntity(target), Amount: int32(-lost), Flags: flags})
		g.noteLifeLost(controller, target, lost)
	}
	if poison > 0 {
		if n := g.countersReplaced(controller, g.Card(source).Controller(), PlayerEntity(target), Poison, poison); n > 0 {
			g.Player(target).Counters.Add(Poison, n)
			emitCounterChanged(g, source, PlayerEntity(target), Poison, n)
		}
	}
	g.checkDamageDoneTriggersToPlayer(controller, source, target, amount, isCombat)
	if table != nil {
		*table = append(*table, damageEntry{Source: source, Target: PlayerEntity(target), Amount: amount})
	}
}

// dealRedirectedDamage deals the half of a damage event ReplaceSplitDamage
// split off (replaceeffect.go) to its new target: a separate event with its
// own prevention and replacements, what ReplacementHandler's damage map
// records under the new target. Nothing happens when amount is not
// positive.
func (g *Game) dealRedirectedDamage(controller PlayerController, source CardID, to EntityID, amount int, isCombat bool, table *damageTable) {
	if amount <= 0 {
		return
	}
	if cid, ok := to.AsCard(); ok {
		g.dealPermanentDamage(controller, source, cid, amount, g.Card(source).HasKeyword("Deathtouch"), isCombat, table)
		return
	}
	if pid, ok := to.AsPlayer(); ok {
		g.dealPlayerDamage(controller, source, pid, amount, isCombat, table)
	}
}

// witherDamage is Card.isWitherDamage (CR 120.3d): a source with wither or
// infect deals damage to a creature as -1/-1 counters. Mode$ WitherDamage, the
// static that grants it, is not read.
func (g *Game) witherDamage(source CardID) bool {
	c := g.Card(source)
	return c.HasKeyword("Wither") || c.HasKeyword("Infect")
}

// toxicMagnitude is Card.getKeywordMagnitude(Keyword.TOXIC): the sum of every
// Toxic:N line c currently carries, each its own instance (CR 702.164b).
func toxicMagnitude(c *Card) int {
	total := 0
	for _, line := range c.KeywordLines() {
		kw := keyword.Parse(line)
		if kw.Name != "Toxic" {
			continue
		}
		if args := kw.Args(); len(args) > 0 {
			if n, err := strconv.Atoi(args[0]); err == nil {
				total += n
			}
		}
	}
	return total
}
