/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.ai;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CounterType;
import forge.GameEntity;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;


/**
 * <p>
 * ComputerUtil_Block2 class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AiBlockController {
    
    private final Player ai;
    /** Constant <code>attackers</code>. */
    private List<Card> attackers = new ArrayList<Card>(); // all attackers
    /** Constant <code>attackersLeft</code>. */
    private List<Card> attackersLeft = new ArrayList<Card>(); // keeps track of
                                                            // all currently
                                                            // unblocked
                                                            // attackers
    /** Constant <code>blockedButUnkilled</code>. */
    private List<Card> blockedButUnkilled = new ArrayList<Card>(); // blocked
                                                                 // attackers
                                                                 // that
                                                                 // currently
                                                                 // wouldn't be
                                                                 // destroyed
    /** Constant <code>blockersLeft</code>. */
    private List<Card> blockersLeft = new ArrayList<Card>(); // keeps track of all
                                                           // unassigned
                                                           // blockers
    private int diff = 0;

    private boolean lifeInDanger = false;
    public AiBlockController(Player aiPlayer) {
        this.ai = aiPlayer;
    }
    
    // finds the creatures able to block the attacker
    private List<Card> getPossibleBlockers(final Combat combat, final Card attacker, final List<Card> blockersLeft, final boolean solo) {
        final List<Card> blockers = new ArrayList<Card>();

        for (final Card blocker : blockersLeft) {
            // if the blocker can block a creature with lure it can't block a
            // creature without
            if (CombatUtil.canBlock(attacker, blocker, combat)) {
                if (solo && blocker.hasKeyword("CARDNAME can't attack or block alone.")) {
                    continue;
                }
                blockers.add(blocker);
            }
        }

        return blockers;
    }

    // finds blockers that won't be destroyed
    private List<Card> getSafeBlockers(final Combat combat, final Card attacker, final List<Card> blockersLeft) {
        final List<Card> blockers = new ArrayList<Card>();

        for (final Card b : blockersLeft) {
            if (!ComputerUtilCombat.canDestroyBlocker(ai, b, attacker, combat, false)) {
                blockers.add(b);
            }
        }

        return blockers;
    }

    // finds blockers that destroy the attacker
    private List<Card> getKillingBlockers(final Combat combat, final Card attacker, final List<Card> blockersLeft) {
        final List<Card> blockers = new ArrayList<Card>();

        for (final Card b : blockersLeft) {
            if (ComputerUtilCombat.canDestroyAttacker(ai, attacker, b, combat, false)) {
                blockers.add(b);
            }
        }

        return blockers;
    }



    private List<List<Card>> sortAttackerByDefender(final Combat combat) {
        List<GameEntity> defenders = combat.getDefenders();
        final ArrayList<List<Card>> attackers = new ArrayList<List<Card>>(defenders.size());
        for (GameEntity defender : defenders) {
            attackers.add(combat.getAttackersOf(defender));
        }
        return attackers;
    }
    
    private List<Card> sortPotentialAttackers(final Combat combat) {
        final List<List<Card>> attackerLists = sortAttackerByDefender(combat);
        final List<Card> sortedAttackers = new ArrayList<Card>();
        final List<Card> firstAttacker = attackerLists.get(0);

        final List<GameEntity> defenders = combat.getDefenders();


        // Begin with the attackers that pose the biggest threat
        CardLists.sortByEvaluateCreature(firstAttacker);
        CardLists.sortByPowerDesc(firstAttacker);

        // If I don't have any planeswalkers than sorting doesn't really matter
        if (defenders.size() == 1) {
            return firstAttacker;
        }

        final boolean bLifeInDanger = ComputerUtilCombat.lifeInDanger(ai, combat);

        // TODO Add creatures attacking Planeswalkers in order of which we want
        // to protect
        // defend planeswalkers with more loyalty before planeswalkers with less
        // loyalty
        // if planeswalker will be too difficult to defend don't even bother
        for (List<Card> attacker : attackerLists) {
            // Begin with the attackers that pose the biggest threat
            CardLists.sortByPowerDesc(attacker);
            for (final Card c : attacker) {
                sortedAttackers.add(c);
            }
        }

        if (bLifeInDanger) {
            // add creatures attacking the Player to the front of the list
            for (final Card c : firstAttacker) {
                sortedAttackers.add(0, c);
            }

        } else {
            // add creatures attacking the Player to the back of the list
            for (final Card c : firstAttacker) {
                sortedAttackers.add(c);
            }
        }

        return sortedAttackers;
    }

    // ======================= block assignment functions
    // ================================

    // Good Blocks means a good trade or no trade
    private void makeGoodBlocks(final Combat combat) {

        List<Card> currentAttackers = new ArrayList<Card>(attackersLeft);

        for (final Card attacker : attackersLeft) {

            if (attacker.hasStartOfKeyword("CantBeBlockedByAmount LT")) {
                continue;
            }

            Card blocker = null;

            final List<Card> blockers = getPossibleBlockers(combat, attacker, blockersLeft, true);

            final List<Card> safeBlockers = getSafeBlockers(combat, attacker, blockers);
            List<Card> killingBlockers;

            if (safeBlockers.size() > 0) {
                // 1.Blockers that can destroy the attacker but won't get
                // destroyed
                killingBlockers = getKillingBlockers(combat, attacker, safeBlockers);
                if (killingBlockers.size() > 0) {
                    blocker = ComputerUtilCard.getWorstCreatureAI(killingBlockers);
                } else if (!attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
                    blocker = ComputerUtilCard.getWorstCreatureAI(safeBlockers);
                    blockedButUnkilled.add(attacker);
                }
            } // no safe blockers
            else {
                // 3.Blockers that can destroy the attacker and have an upside when dying
                killingBlockers = getKillingBlockers(combat, attacker, blockers);
                for (Card b : killingBlockers) {
                    if ((b.hasKeyword("Undying") && b.getCounters(CounterType.P1P1) == 0)
                            || !b.getSVar("SacMe").equals("")) {
                        blocker = b;
                        break;
                    }
                }
                // 4.Blockers that can destroy the attacker and are worth less
                if (blocker == null && killingBlockers.size() > 0) {
                    final Card worst = ComputerUtilCard.getWorstCreatureAI(killingBlockers);

                    if ((ComputerUtilCard.evaluateCreature(worst) + diff) < ComputerUtilCard
                            .evaluateCreature(attacker)) {
                        blocker = worst;
                    }
                }
            }
            if (blocker != null) {
                currentAttackers.remove(attacker);
                combat.addBlocker(attacker, blocker);
            }
        }
        attackersLeft = (new ArrayList<Card>(currentAttackers));
    }

    // Good Gang Blocks means a good trade or no trade
    /**
     * <p>
     * makeGangBlocks.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a {@link forge.game.combat.Combat} object.
     */
    static final Predicate<Card> rampagesOrNeedsManyToBlock = Predicates.or(CardPredicates.containsKeyword("Rampage"), CardPredicates.containsKeyword("CantBeBlockedByAmount GT"));

    private void makeGangBlocks(final Combat combat) {
        List<Card> currentAttackers = CardLists.filter(attackersLeft, Predicates.not(rampagesOrNeedsManyToBlock));
        List<Card> blockers;

        // Try to block an attacker without first strike with a gang of first strikers
        for (final Card attacker : attackersLeft) {
            if (!attacker.hasKeyword("First Strike") && !attacker.hasKeyword("Double Strike")) {
                blockers = getPossibleBlockers(combat, attacker, blockersLeft, false);
                final List<Card> firstStrikeBlockers = new ArrayList<Card>();
                final List<Card> blockGang = new ArrayList<Card>();
                for (int i = 0; i < blockers.size(); i++) {
                    if (blockers.get(i).hasFirstStrike() || blockers.get(i).hasDoubleStrike()) {
                        firstStrikeBlockers.add(blockers.get(i));
                    }
                }

                if (firstStrikeBlockers.size() > 1) {
                    CardLists.sortByPowerDesc(firstStrikeBlockers);
                    for (final Card blocker : firstStrikeBlockers) {
                        final int damageNeeded = ComputerUtilCombat.getDamageToKill(attacker)
                                + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, false);
                        // if the total damage of the blockgang was not enough
                        // without but is enough with this blocker finish the
                        // blockgang
                        if (ComputerUtilCombat.totalDamageOfBlockers(attacker, blockGang) < damageNeeded
                                || CombatUtil.needsBlockers(attacker) > blockGang.size()) {
                            blockGang.add(blocker);
                            if (ComputerUtilCombat.totalDamageOfBlockers(attacker, blockGang) >= damageNeeded) {
                                currentAttackers.remove(attacker);
                                for (final Card b : blockGang) {
                                    if (CombatUtil.canBlock(attacker, blocker, combat)) {
                                        combat.addBlocker(attacker, b);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        attackersLeft = (new ArrayList<Card>(currentAttackers));
        currentAttackers = new ArrayList<Card>(attackersLeft);

        // Try to block an attacker with two blockers of which only one will die
        for (final Card attacker : attackersLeft) {
            blockers = getPossibleBlockers(combat, attacker, blockersLeft, false);
            List<Card> usableBlockers;
            final List<Card> blockGang = new ArrayList<Card>();
            int absorbedDamage = 0; // The amount of damage needed to kill the
                                    // first blocker
            int currentValue = 0; // The value of the creatures in the blockgang

            // Try to add blockers that could be destroyed, but are worth less
            // than the attacker
            // Don't use blockers without First Strike or Double Strike if
            // attacker has it
            usableBlockers = CardLists.filter(blockers, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if ((attacker.hasKeyword("First Strike") || attacker.hasKeyword("Double Strike"))
                            && !(c.hasKeyword("First Strike") || c.hasKeyword("Double Strike"))) {
                        return false;
                    }
                    return lifeInDanger || (ComputerUtilCard.evaluateCreature(c) + diff) < ComputerUtilCard.evaluateCreature(attacker);
                }
            });
            if (usableBlockers.size() < 2) {
                return;
            }

            final Card leader = ComputerUtilCard.getBestCreatureAI(usableBlockers);
            blockGang.add(leader);
            usableBlockers.remove(leader);
            absorbedDamage = ComputerUtilCombat.getEnoughDamageToKill(leader, attacker.getNetCombatDamage(), attacker, true);
            currentValue = ComputerUtilCard.evaluateCreature(leader);

            for (final Card blocker : usableBlockers) {
                // Add an additional blocker if the current blockers are not
                // enough and the new one would deal the remaining damage
                final int currentDamage = ComputerUtilCombat.totalDamageOfBlockers(attacker, blockGang);
                final int additionalDamage = ComputerUtilCombat.dealsDamageAsBlocker(attacker, blocker);
                final int absorbedDamage2 = ComputerUtilCombat.getEnoughDamageToKill(blocker, attacker.getNetCombatDamage(), attacker, true);
                final int addedValue = ComputerUtilCard.evaluateCreature(blocker);
                final int damageNeeded = ComputerUtilCombat.getDamageToKill(attacker)
                        + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, false);
                if ((damageNeeded > currentDamage || CombatUtil.needsBlockers(attacker) > blockGang.size())
                        && !(damageNeeded > currentDamage + additionalDamage)
                        // The attacker will be killed
                        && (absorbedDamage2 + absorbedDamage > attacker.getNetCombatDamage()
                        // only one blocker can be killed
                        || currentValue + addedValue - 50 <= ComputerUtilCard.evaluateCreature(attacker)
                        // or attacker is worth more
                        || (lifeInDanger && ComputerUtilCombat.lifeInDanger(ai, combat)))
                        // or life is in danger
                        && CombatUtil.canBlock(attacker, blocker, combat)) {
                    // this is needed for attackers that can't be blocked by
                    // more than 1
                    currentAttackers.remove(attacker);
                    combat.addBlocker(attacker, blocker);
                    if (CombatUtil.canBlock(attacker, leader, combat)) {
                        combat.addBlocker(attacker, leader);
                    }
                    break;
                }
            }
        }

        attackersLeft = (new ArrayList<Card>(currentAttackers));
    }

    // Bad Trade Blocks (should only be made if life is in danger)
    /**
     * <p>
     * makeTradeBlocks.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a {@link forge.game.combat.Combat} object.
     */
    private void makeTradeBlocks(final Combat combat) {

        List<Card> currentAttackers = new ArrayList<Card>(attackersLeft);
        List<Card> killingBlockers;

        for (final Card attacker : attackersLeft) {

            if (attacker.hasStartOfKeyword("CantBeBlockedByAmount LT")) {
                continue;
            }

            List<Card> possibleBlockers = getPossibleBlockers(combat, attacker, blockersLeft, true);
            killingBlockers = getKillingBlockers(combat, attacker, possibleBlockers);
            if ((killingBlockers.size() > 0) && ComputerUtilCombat.lifeInDanger(ai, combat)) {
                final Card blocker = ComputerUtilCard.getWorstCreatureAI(killingBlockers);
                combat.addBlocker(attacker, blocker);
                currentAttackers.remove(attacker);
            }
        }
        attackersLeft = (new ArrayList<Card>(currentAttackers));

    }

    // Chump Blocks (should only be made if life is in danger)
    private void makeChumpBlocks(final Combat combat) {

        List<Card> currentAttackers = new ArrayList<Card>(attackersLeft);

        makeChumpBlocks(combat, currentAttackers);
        
        if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
            makeMultiChumpBlocks(combat);
        }
    }
    
    private void makeChumpBlocks(final Combat combat, List<Card> attackers) {
        
        if (attackers.isEmpty() || !ComputerUtilCombat.lifeInDanger(ai, combat)) {
            return;
        }
        
        Card attacker = attackers.get(0);
        
        if (attacker.hasStartOfKeyword("CantBeBlockedByAmount LT") || attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
            attackers.remove(0);
            makeChumpBlocks(combat, attackers);
            return;
        }

        List<Card> chumpBlockers = getPossibleBlockers(combat, attacker, blockersLeft, true);
        if (!chumpBlockers.isEmpty()) {
            final Card blocker = ComputerUtilCard.getWorstCreatureAI(chumpBlockers);
            
            // check if it's better to block a creature with lower power and without trample
            if (attacker.hasKeyword("Trample")) {
                final int damageAbsorbed = blocker.getLethalDamage();
                if (attacker.getNetCombatDamage() > damageAbsorbed) {
                    for (Card other : attackers) {
                        if (other.equals(attacker)) {
                            continue;
                        }
                        if (other.getNetCombatDamage() >= damageAbsorbed 
                                && !other.hasKeyword("Trample")
                                && CombatUtil.canBlock(other, blocker, combat)) {
                            combat.addBlocker(other, blocker);
                            attackersLeft.remove(other);
                            blockedButUnkilled.add(other);
                            attackers.remove(other);
                            makeChumpBlocks(combat, attackers);
                            return;
                        }
                    }
                }
            }

            combat.addBlocker(attacker, blocker);
            attackersLeft.remove(attacker);
            blockedButUnkilled.add(attacker);
        }
        attackers.remove(0);
        makeChumpBlocks(combat, attackers);
    }

    // Block creatures with "can't be blocked except by two or more creatures"
    private void makeMultiChumpBlocks(final Combat combat) {

        List<Card> currentAttackers = new ArrayList<Card>(attackersLeft);
        
        for (final Card attacker : currentAttackers) {

            if (!attacker.hasStartOfKeyword("CantBeBlockedByAmount LT")) {
                continue;
            }
            List<Card> possibleBlockers = getPossibleBlockers(combat, attacker, blockersLeft, true);
            if (!CombatUtil.canAttackerBeBlockedWithAmount(attacker, possibleBlockers.size())) {
                continue;
            }
            List<Card> usedBlockers = new ArrayList<Card>();
            for (Card blocker : possibleBlockers) {
                if (CombatUtil.canBlock(attacker, blocker, combat)) {
                    combat.addBlocker(attacker, blocker);
                    usedBlockers.add(blocker);
                    if (CombatUtil.canAttackerBeBlockedWithAmount(attacker, usedBlockers.size())) {
                        break;
                    }
                }
            }
            if (CombatUtil.canAttackerBeBlockedWithAmount(attacker, usedBlockers.size())) {
                attackersLeft.remove(attacker);
            } else {
                for (Card blocker : usedBlockers) {
                    combat.removeBlockAssignment(attacker, blocker);
                }
            }
        }
    }

    /** Reinforce blockers blocking attackers with trample (should only be made if life is in danger) */
    private void reinforceBlockersAgainstTrample(final Combat combat) {

        List<Card> chumpBlockers;

        List<Card> tramplingAttackers = CardLists.getKeyword(attackers, "Trample");
        tramplingAttackers = CardLists.filter(tramplingAttackers, Predicates.not(rampagesOrNeedsManyToBlock));

        // TODO - should check here for a "rampage-like" trigger that replaced
        // the keyword:
        // "Whenever CARDNAME becomes blocked, it gets +1/+1 until end of turn for each creature blocking it."

        for (final Card attacker : tramplingAttackers) {

            if ((attacker.hasStartOfKeyword("CantBeBlockedByAmount LT") && !combat.isBlocked(attacker))
                    || attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
                continue;
            }

            chumpBlockers = getPossibleBlockers(combat, attacker, blockersLeft, false);
            chumpBlockers.removeAll(combat.getBlockers(attacker));
            for (final Card blocker : chumpBlockers) {
                // Add an additional blocker if the current blockers are not
                // enough and the new one would suck some of the damage
                if (ComputerUtilCombat.getAttack(attacker) > ComputerUtilCombat.totalShieldDamage(attacker, combat.getBlockers(attacker))
                        && ComputerUtilCombat.shieldDamage(attacker, blocker) > 0
                        && CombatUtil.canBlock(attacker, blocker, combat) && ComputerUtilCombat.lifeInDanger(ai, combat)) {
                    combat.addBlocker(attacker, blocker);
                }
            }
        }
    }

    /** Support blockers not destroying the attacker with more blockers to try to kill the attacker */
    private void reinforceBlockersToKill(final Combat combat) {

        List<Card> safeBlockers;
        List<Card> blockers;

        List<Card> targetAttackers = CardLists.filter(blockedButUnkilled, Predicates.not(rampagesOrNeedsManyToBlock));

        // TODO - should check here for a "rampage-like" trigger that replaced
        // the keyword:
        // "Whenever CARDNAME becomes blocked, it gets +1/+1 until end of turn for each creature blocking it."

        for (final Card attacker : targetAttackers) {
            blockers = getPossibleBlockers(combat, attacker, blockersLeft, false);
            blockers.removeAll(combat.getBlockers(attacker));

            // Try to use safe blockers first
            safeBlockers = getSafeBlockers(combat, attacker, blockers);
            for (final Card blocker : safeBlockers) {
                final int damageNeeded = ComputerUtilCombat.getDamageToKill(attacker)
                        + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, false);
                // Add an additional blocker if the current blockers are not
                // enough and the new one would deal additional damage
                if ((damageNeeded > ComputerUtilCombat.totalDamageOfBlockers(attacker, combat.getBlockers(attacker)))
                        && ComputerUtilCombat.dealsDamageAsBlocker(attacker, blocker) > 0
                        && CombatUtil.canBlock(attacker, blocker, combat)) {
                    combat.addBlocker(attacker, blocker);
                }
                blockers.remove(blocker); // Don't check them again next
            }

            // Try to add blockers that could be destroyed, but are worth less
            // than the attacker
            // Don't use blockers without First Strike or Double Strike if
            // attacker has it
            if (attacker.hasKeyword("First Strike") || attacker.hasKeyword("Double Strike")) {
                safeBlockers = CardLists.getKeyword(blockers, "First Strike");
                safeBlockers.addAll(CardLists.getKeyword(blockers, "Double Strike"));
            } else {
                safeBlockers = new ArrayList<Card>(blockers);
            }

            for (final Card blocker : safeBlockers) {
                final int damageNeeded = ComputerUtilCombat.getDamageToKill(attacker)
                        + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, false);
                // Add an additional blocker if the current blockers are not
                // enough and the new one would deal the remaining damage
                final int currentDamage = ComputerUtilCombat.totalDamageOfBlockers(attacker, combat.getBlockers(attacker));
                final int additionalDamage = ComputerUtilCombat.dealsDamageAsBlocker(attacker, blocker);
                if ((damageNeeded > currentDamage)
                        && !(damageNeeded > (currentDamage + additionalDamage))
                        && ((ComputerUtilCard.evaluateCreature(blocker) + diff) < ComputerUtilCard
                                .evaluateCreature(attacker)) && CombatUtil.canBlock(attacker, blocker, combat)) {
                    combat.addBlocker(attacker, blocker);
                    blockersLeft.remove(blocker);
                }
            }
        }
    }

    private void clearBlockers(final Combat combat, final List<Card> possibleBlockers) {

        final List<Card> oldBlockers = combat.getAllBlockers();
        for (final Card blocker : oldBlockers) {
            if ( blocker.getController() == ai ) // don't touch other player's blockers
                combat.removeFromCombat(blocker);
        }

        attackersLeft = new ArrayList<Card>(attackers); // keeps track of all currently unblocked attackers
        blockersLeft = new ArrayList<Card>(possibleBlockers); // keeps track of all unassigned blockers
        blockedButUnkilled = new ArrayList<Card>(); // keeps track of all blocked attackers that currently wouldn't be destroyed
    }

    /** Assigns blockers for the provided combat instance (in favor of player passes to ctor) */
    public void assignBlockers(final Combat combat) {
        
        final List<Card> possibleBlockers = ai.getCreaturesInPlay();

        attackers = sortPotentialAttackers(combat);

        if (attackers.isEmpty()) {
            return;
        }

        clearBlockers(combat, possibleBlockers);
        
        List<Card> blockers;
        List<Card> chumpBlockers;

        diff = (ai.getLife() * 2) - 5; // This is the minimal gain for an unnecessary trade

        // remove all attackers that can't be blocked anyway
        for (final Card a : attackers) {
            if (!CombatUtil.canBeBlocked(a, ai)) {
                attackersLeft.remove(a);
            }
        }

        // remove all blockers that can't block anyway
        for (final Card b : possibleBlockers) {
            if (!CombatUtil.canBlock(b, combat)) {
                blockersLeft.remove(b);
            }
        }

        if (attackersLeft.isEmpty()) {
            return;
        }

        // Begin with the weakest blockers
        CardLists.sortByPowerAsc(blockersLeft);

        // == 1. choose best blocks first ==
        makeGoodBlocks(combat);
        makeGangBlocks(combat);
        if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
            makeTradeBlocks(combat); // choose necessary trade blocks
        }
        // if life is in danger
        if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
            makeChumpBlocks(combat); // choose necessary chump blocks
        }
        // if life is still in danger
        // Reinforce blockers blocking attackers with trample if life is still
        // in danger
        if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
            reinforceBlockersAgainstTrample(combat);
        }
        // Support blockers not destroying the attacker with more blockers to
        // try to kill the attacker
        if (!ComputerUtilCombat.lifeInDanger(ai, combat)) {
            reinforceBlockersToKill(combat);
        }

        // == 2. If the AI life would still be in danger make a safer approach ==
        if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
            lifeInDanger = true;
            clearBlockers(combat, possibleBlockers); // reset every block assignment
            makeTradeBlocks(combat); // choose necessary trade blocks
            // if life is in danger
            makeGoodBlocks(combat);
            // choose necessary chump blocks if life is still in danger
            if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
                makeChumpBlocks(combat);
            }
            // Reinforce blockers blocking attackers with trample if life is
            // still in danger
            if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
                reinforceBlockersAgainstTrample(combat);
            }
            makeGangBlocks(combat);
            reinforceBlockersToKill(combat);
        }

        // == 3. If the AI life would be in serious danger make an even safer approach ==
        if (lifeInDanger && ComputerUtilCombat.lifeInSeriousDanger(ai, combat)) {
            clearBlockers(combat, possibleBlockers); // reset every block assignment
            makeChumpBlocks(combat); // choose chump blocks
            if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
                makeTradeBlocks(combat); // choose necessary trade
            }

            if (!ComputerUtilCombat.lifeInDanger(ai, combat)) {
                makeGoodBlocks(combat);
            }
            // Reinforce blockers blocking attackers with trample if life is
            // still in danger
            else {
                reinforceBlockersAgainstTrample(combat);
            }
            makeGangBlocks(combat);
            // Support blockers not destroying the attacker with more blockers
            // to try to kill the attacker
            reinforceBlockersToKill(combat);
        }

        // assign blockers that have to block
        chumpBlockers = CardLists.getKeyword(blockersLeft, "CARDNAME blocks each turn if able.");
        // if an attacker with lure attacks - all that can block
        for (final Card blocker : blockersLeft) {
            if (CombatUtil.mustBlockAnAttacker(blocker, combat)) {
                chumpBlockers.add(blocker);
            }
        }
        if (!chumpBlockers.isEmpty()) {
            CardLists.shuffle(attackers);
            for (final Card attacker : attackers) {
                blockers = getPossibleBlockers(combat, attacker, chumpBlockers, false);
                for (final Card blocker : blockers) {
                    if (CombatUtil.canBlock(attacker, blocker, combat) && blockersLeft.contains(blocker)
                            && (CombatUtil.mustBlockAnAttacker(blocker, combat)
                                    || blocker.hasKeyword("CARDNAME blocks each turn if able."))) {
                        combat.addBlocker(attacker, blocker);
                        blockersLeft.remove(blocker);
                    }
                }
            }
        }
    }

    public static List<Card> orderBlockers(Card attacker, List<Card> blockers) {
        // ordering of blockers, sort by evaluate, then try to kill the best
        int damage = attacker.getNetCombatDamage();
        CardLists.sortByEvaluateCreature(blockers);
        final List<Card> first = new ArrayList<Card>();
        final List<Card> last = new ArrayList<Card>();
        for (Card blocker : blockers) {
            int lethal = ComputerUtilCombat.getEnoughDamageToKill(blocker, damage, attacker, true);
            if (lethal > damage) {
                last.add(blocker);
            } else {
                first.add(blocker);
                damage -= lethal;
            }
        }
        first.addAll(last);

        // TODO: Take total damage, and attempt to maximize killing the greatest evaluation of creatures
        // It's probably generally better to kill the largest creature, but sometimes its better to kill a few smaller ones

        return first;
    }

    public static List<Card> orderAttackers(Card blocker, List<Card> attackers) {
        // This shouldn't really take trample into account, but otherwise should be pretty similar to orderBlockers
        // ordering of blockers, sort by evaluate, then try to kill the best
        int damage = blocker.getNetCombatDamage();
        CardLists.sortByEvaluateCreature(attackers);
        final List<Card> first = new ArrayList<Card>();
        final List<Card> last = new ArrayList<Card>();
        for (Card attacker : attackers) {
            int lethal = ComputerUtilCombat.getEnoughDamageToKill(attacker, damage, blocker, true);
            if (lethal > damage) {
                last.add(attacker);
            } else {
                first.add(attacker);
                damage -= lethal;
            }
        }
        first.addAll(last);

        // TODO: Take total damage, and attempt to maximize killing the greatest evaluation of creatures
        // It's probably generally better to kill the largest creature, but sometimes its better to kill a few smaller ones

        return first;
    }
}
