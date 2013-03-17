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
import forge.game.phase.Combat;
import forge.game.phase.CombatUtil;
import forge.game.player.Player;


/**
 * <p>
 * ComputerUtil_Block2 class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ComputerUtilBlock {
    /** Constant <code>attackers</code>. */
    private static List<Card> attackers = new ArrayList<Card>(); // all attackers
    /** Constant <code>attackersLeft</code>. */
    private static List<Card> attackersLeft = new ArrayList<Card>(); // keeps track of
                                                            // all currently
                                                            // unblocked
                                                            // attackers
    /** Constant <code>blockedButUnkilled</code>. */
    private static List<Card> blockedButUnkilled = new ArrayList<Card>(); // blocked
                                                                 // attackers
                                                                 // that
                                                                 // currently
                                                                 // wouldn't be
                                                                 // destroyed
    /** Constant <code>blockersLeft</code>. */
    private static List<Card> blockersLeft = new ArrayList<Card>(); // keeps track of all
                                                           // unassigned
                                                           // blockers
    /** Constant <code>diff=0</code>. */
    private static int diff = 0;

    private static boolean lifeInDanger = false;

    /**
     * <p>
     * Getter for the field <code>attackers</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private static List<Card> getAttackers() {
        return ComputerUtilBlock.attackers;
    }

    /**
     * <p>
     * Setter for the field <code>attackers</code>.
     * </p>
     * 
     * @param cardList
     *            a {@link forge.CardList} object.
     */
    private static void setAttackers(final List<Card> cardList) {
        ComputerUtilBlock.attackers = (cardList);
    }

    /**
     * <p>
     * Getter for the field <code>attackersLeft</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private static List<Card> getAttackersLeft() {
        return ComputerUtilBlock.attackersLeft;
    }

    /**
     * <p>
     * Setter for the field <code>attackersLeft</code>.
     * </p>
     * 
     * @param cardList
     *            a {@link forge.CardList} object.
     */
    private static void setAttackersLeft(final List<Card> cardList) {
        ComputerUtilBlock.attackersLeft = (cardList);
    }

    /**
     * <p>
     * Getter for the field <code>blockedButUnkilled</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private static List<Card> getBlockedButUnkilled() {
        return ComputerUtilBlock.blockedButUnkilled;

    }

    /**
     * <p>
     * Setter for the field <code>blockedButUnkilled</code>.
     * </p>
     * 
     * @param cardList
     *            a {@link forge.CardList} object.
     */
    private static void setBlockedButUnkilled(final List<Card> cardList) {
        ComputerUtilBlock.blockedButUnkilled = (cardList);
    }

    /**
     * <p>
     * Getter for the field <code>blockersLeft</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private static List<Card> getBlockersLeft() {
        return ComputerUtilBlock.blockersLeft;
    }

    /**
     * <p>
     * Setter for the field <code>blockersLeft</code>.
     * </p>
     * 
     * @param cardList
     *            a {@link forge.CardList} object.
     */
    private static void setBlockersLeft(final List<Card> cardList) {
        ComputerUtilBlock.blockersLeft = (cardList);
    }

    /**
     * <p>
     * Getter for the field <code>diff</code>.
     * </p>
     * 
     * @return a int.
     */
    private static int getDiff() {
        return ComputerUtilBlock.diff;
    }

    /**
     * <p>
     * Setter for the field <code>diff</code>.
     * </p>
     * 
     * @param diff
     *            a int.
     */
    private static void setDiff(final int diff) {
        ComputerUtilBlock.diff = (diff);
    }

    // finds the creatures able to block the attacker
    /**
     * <p>
     * getPossibleBlockers.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param blockersLeft
     *            a {@link forge.CardList} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a {@link forge.CardList} object.
     */
    private static List<Card> getPossibleBlockers(final Card attacker, final List<Card> blockersLeft, final Combat combat
            , final boolean solo) {
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
    /**
     * <p>
     * getSafeBlockers.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param blockersLeft
     *            a {@link forge.CardList} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a {@link forge.CardList} object.
     */
    private static List<Card> getSafeBlockers(final Player ai, final Card attacker, final List<Card> blockersLeft, final Combat combat) {
        final List<Card> blockers = new ArrayList<Card>();

        for (final Card b : blockersLeft) {
            if (!ComputerUtilCombat.canDestroyBlocker(ai, b, attacker, combat, false)) {
                blockers.add(b);
            }
        }

        return blockers;
    }

    // finds blockers that destroy the attacker
    /**
     * <p>
     * getKillingBlockers.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param blockersLeft
     *            a {@link forge.CardList} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a {@link forge.CardList} object.
     */
    private static List<Card> getKillingBlockers(final Player ai, final Card attacker, final List<Card> blockersLeft, final Combat combat) {
        final List<Card> blockers = new ArrayList<Card>();

        for (final Card b : blockersLeft) {
            if (ComputerUtilCombat.canDestroyAttacker(ai, attacker, b, combat, false)) {
                blockers.add(b);
            }
        }

        return blockers;
    }

    /**
     * <p>
     * sortPotentialAttackers.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> sortPotentialAttackers(final Player ai, final Combat combat) {
        final List<List<Card>> attackerLists = combat.sortAttackerByDefender();
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
    /**
     * <p>
     * makeGoodBlocks.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a {@link forge.game.phase.Combat} object.
     */
    private static Combat makeGoodBlocks(final Player ai, final Combat combat) {

        List<Card> currentAttackers = new ArrayList<Card>(ComputerUtilBlock.getAttackersLeft());

        for (final Card attacker : ComputerUtilBlock.getAttackersLeft()) {

            if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")) {
                continue;
            }

            Card blocker = null;

            final List<Card> blockers = ComputerUtilBlock.getPossibleBlockers(attacker,
                    ComputerUtilBlock.getBlockersLeft(), combat, true);

            final List<Card> safeBlockers = ComputerUtilBlock.getSafeBlockers(ai, attacker, blockers, combat);
            List<Card> killingBlockers;

            if (safeBlockers.size() > 0) {
                // 1.Blockers that can destroy the attacker but won't get
                // destroyed
                killingBlockers = ComputerUtilBlock.getKillingBlockers(ai, attacker, safeBlockers, combat);
                if (killingBlockers.size() > 0) {
                    blocker = ComputerUtilCard.getWorstCreatureAI(killingBlockers);
                } else if (!attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
                    blocker = ComputerUtilCard.getWorstCreatureAI(safeBlockers);
                    ComputerUtilBlock.getBlockedButUnkilled().add(attacker);
                }
            } // no safe blockers
            else {
                // 3.Blockers that can destroy the attacker and have an upside when dying
                killingBlockers = ComputerUtilBlock.getKillingBlockers(ai, attacker, blockers, combat);
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

                    if ((ComputerUtilCard.evaluateCreature(worst) + ComputerUtilBlock.getDiff()) < ComputerUtilCard
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
        ComputerUtilBlock.setAttackersLeft(new ArrayList<Card>(currentAttackers));
        return combat;
    }

    // Good Gang Blocks means a good trade or no trade
    /**
     * <p>
     * makeGangBlocks.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a {@link forge.game.phase.Combat} object.
     */
    static final Predicate<Card> rampagesOrNeedsManyToBlock = Predicates.or(CardPredicates.containsKeyword("Rampage"), CardPredicates.containsKeyword("CARDNAME can't be blocked by more than one creature."));

    private static Combat makeGangBlocks(final Player ai, final Combat combat) {
        List<Card> currentAttackers = CardLists.filter(ComputerUtilBlock.getAttackersLeft(), Predicates.not(rampagesOrNeedsManyToBlock));
        List<Card> blockers;

        // Try to block an attacker without first strike with a gang of first strikers
        for (final Card attacker : ComputerUtilBlock.getAttackersLeft()) {
            if (!attacker.hasKeyword("First Strike") && !attacker.hasKeyword("Double Strike")) {
                blockers = ComputerUtilBlock.getPossibleBlockers(attacker, ComputerUtilBlock.getBlockersLeft(), combat, false);
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

        ComputerUtilBlock.setAttackersLeft(new ArrayList<Card>(currentAttackers));
        currentAttackers = new ArrayList<Card>(ComputerUtilBlock.getAttackersLeft());

        // Try to block an attacker with two blockers of which only one will die
        for (final Card attacker : ComputerUtilBlock.getAttackersLeft()) {
            blockers = ComputerUtilBlock.getPossibleBlockers(attacker, ComputerUtilBlock.getBlockersLeft(), combat, false);
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
                    return lifeInDanger || (ComputerUtilCard.evaluateCreature(c) + ComputerUtilBlock.getDiff()) < ComputerUtilCard
                            .evaluateCreature(attacker);
                }
            });
            if (usableBlockers.size() < 2) {
                return combat;
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

        ComputerUtilBlock.setAttackersLeft(new ArrayList<Card>(currentAttackers));
        return combat;
    }

    // Bad Trade Blocks (should only be made if life is in danger)
    /**
     * <p>
     * makeTradeBlocks.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a {@link forge.game.phase.Combat} object.
     */
    private static Combat makeTradeBlocks(final Player ai, final Combat combat) {

        List<Card> currentAttackers = new ArrayList<Card>(ComputerUtilBlock.getAttackersLeft());
        List<Card> killingBlockers;

        for (final Card attacker : ComputerUtilBlock.getAttackersLeft()) {

            if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")) {
                continue;
            }

            killingBlockers = ComputerUtilBlock.getKillingBlockers(ai, attacker,
                    ComputerUtilBlock.getPossibleBlockers(attacker, ComputerUtilBlock.getBlockersLeft(), combat, true),
                    combat);
            if ((killingBlockers.size() > 0) && ComputerUtilCombat.lifeInDanger(ai, combat)) {
                final Card blocker = ComputerUtilCard.getWorstCreatureAI(killingBlockers);
                combat.addBlocker(attacker, blocker);
                currentAttackers.remove(attacker);
            }
        }
        ComputerUtilBlock.setAttackersLeft(new ArrayList<Card>(currentAttackers));
        return combat;
    }

    // Chump Blocks (should only be made if life is in danger)
    /**
     * <p>
     * makeChumpBlocks.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a {@link forge.game.phase.Combat} object.
     */
    private static Combat makeChumpBlocks(final Player ai, final Combat combat) {

        List<Card> currentAttackers = new ArrayList<Card>(ComputerUtilBlock.getAttackersLeft());
        List<Card> chumpBlockers;

        for (final Card attacker : ComputerUtilBlock.getAttackersLeft()) {

            if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")
                    || attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
                continue;
            }

            chumpBlockers = ComputerUtilBlock
                    .getPossibleBlockers(attacker, ComputerUtilBlock.getBlockersLeft(), combat, true);
            if ((chumpBlockers.size() > 0) && ComputerUtilCombat.lifeInDanger(ai, combat)) {
                final Card blocker = ComputerUtilCard.getWorstCreatureAI(chumpBlockers);
                combat.addBlocker(attacker, blocker);
                currentAttackers.remove(attacker);
                ComputerUtilBlock.getBlockedButUnkilled().add(attacker);
            }
        }
        ComputerUtilBlock.setAttackersLeft(new ArrayList<Card>(currentAttackers));
        return combat;
    }

    // Reinforce blockers blocking attackers with trample (should only be made
    // if life is in danger)
    /**
     * <p>
     * reinforceBlockersAgainstTrample.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a {@link forge.game.phase.Combat} object.
     */
    private static Combat reinforceBlockersAgainstTrample(final Player ai, final Combat combat) {

        List<Card> chumpBlockers;

        List<Card> tramplingAttackers = CardLists.getKeyword(ComputerUtilBlock.getAttackers(), "Trample");
        tramplingAttackers = CardLists.filter(tramplingAttackers, Predicates.not(rampagesOrNeedsManyToBlock));

        // TODO - should check here for a "rampage-like" trigger that replaced
        // the keyword:
        // "Whenever CARDNAME becomes blocked, it gets +1/+1 until end of turn for each creature blocking it."

        for (final Card attacker : tramplingAttackers) {

            if ((attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")
                    && !combat.isBlocked(attacker))
                    || attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
                continue;
            }

            chumpBlockers = ComputerUtilBlock
                    .getPossibleBlockers(attacker, ComputerUtilBlock.getBlockersLeft(), combat, false);
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

        return combat;
    }

    // Support blockers not destroying the attacker with more blockers to try to
    // kill the attacker
    /**
     * <p>
     * reinforceBlockersToKill.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a {@link forge.game.phase.Combat} object.
     */
    private static Combat reinforceBlockersToKill(final Player ai, final Combat combat) {

        List<Card> safeBlockers;
        List<Card> blockers;

        List<Card> targetAttackers = CardLists.filter(ComputerUtilBlock.getBlockedButUnkilled(), Predicates.not(rampagesOrNeedsManyToBlock));

        // TODO - should check here for a "rampage-like" trigger that replaced
        // the keyword:
        // "Whenever CARDNAME becomes blocked, it gets +1/+1 until end of turn for each creature blocking it."

        for (final Card attacker : targetAttackers) {
            blockers = ComputerUtilBlock.getPossibleBlockers(attacker, ComputerUtilBlock.getBlockersLeft(), combat, false);
            blockers.removeAll(combat.getBlockers(attacker));

            // Try to use safe blockers first
            safeBlockers = ComputerUtilBlock.getSafeBlockers(ai, attacker, blockers, combat);
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
                        && ((ComputerUtilCard.evaluateCreature(blocker) + ComputerUtilBlock.getDiff()) < ComputerUtilCard
                                .evaluateCreature(attacker)) && CombatUtil.canBlock(attacker, blocker, combat)) {
                    combat.addBlocker(attacker, blocker);
                    ComputerUtilBlock.getBlockersLeft().remove(blocker);
                }
            }
        }

        return combat;
    }

    /**
     * <p>
     * resetBlockers.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @param possibleBlockers
     *            a {@link forge.CardList} object.
     * @return a {@link forge.game.phase.Combat} object.
     */
    private static Combat resetBlockers(final Combat combat, final List<Card> possibleBlockers) {

        final List<Card> oldBlockers = combat.getAllBlockers();
        for (final Card blocker : oldBlockers) {
            combat.removeFromCombat(blocker);
        }

        ComputerUtilBlock.setAttackersLeft(new ArrayList<Card>(ComputerUtilBlock.getAttackers())); // keeps
                                                                                                      // track
        // of all
        // currently
        // unblocked
        // attackers
        ComputerUtilBlock.setBlockersLeft(new ArrayList<Card>(possibleBlockers)); // keeps
        // track of
        // all
        // unassigned
        // blockers
        ComputerUtilBlock.setBlockedButUnkilled(new ArrayList<Card>()); // keeps track
                                                                 // of all
                                                                 // blocked
        // attackers that currently
        // wouldn't be destroyed

        return combat;
    }

    // Main function
    /**
     * <p>
     * getBlockers.
     * </p>
     * 
     * @param originalCombat
     *            a {@link forge.game.phase.Combat} object.
     * @param possibleBlockers
     *            a {@link forge.CardList} object.
     * @return a {@link forge.game.phase.Combat} object.
     */
    public static Combat getBlockers(final Player ai, final Combat originalCombat, final List<Card> possibleBlockers) {

        Combat combat = originalCombat;

        ComputerUtilBlock.setAttackers(ComputerUtilBlock.sortPotentialAttackers(ai, combat));

        if (ComputerUtilBlock.getAttackers().size() == 0) {
            return combat;
        }

        // keeps track of all currently unblocked attackers
        ComputerUtilBlock.setAttackersLeft(new ArrayList<Card>(ComputerUtilBlock.getAttackers()));
        // keeps track of all unassigned blockers
        ComputerUtilBlock.setBlockersLeft(new ArrayList<Card>(possibleBlockers));
        // keeps track of all blocked attackers that currently wouldn't be destroyed
        ComputerUtilBlock.setBlockedButUnkilled(new ArrayList<Card>());
        List<Card> blockers;
        List<Card> chumpBlockers;

        ComputerUtilBlock.setDiff((ai.getLife() * 2) - 5); // This
                                                                                    // is
                                                                                    // the
        // minimal gain
        // for an
        // unnecessary
        // trade

        // remove all attackers that can't be blocked anyway
        for (final Card a : ComputerUtilBlock.getAttackers()) {
            if (!CombatUtil.canBeBlocked(a)) {
                ComputerUtilBlock.getAttackersLeft().remove(a);
            }
        }

        // remove all blockers that can't block anyway
        for (final Card b : possibleBlockers) {
            if (!CombatUtil.canBlock(b, combat)) {
                ComputerUtilBlock.getBlockersLeft().remove(b);
            }
        }

        if (ComputerUtilBlock.getAttackersLeft().size() == 0) {
            return combat;
        }

        // Begin with the weakest blockers
        CardLists.sortByPowerAsc(ComputerUtilBlock.getBlockersLeft());

        // == 1. choose best blocks first ==
        combat = ComputerUtilBlock.makeGoodBlocks(ai, combat);
        combat = ComputerUtilBlock.makeGangBlocks(ai, combat);
        if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
            combat = ComputerUtilBlock.makeTradeBlocks(ai, combat); // choose
                                                                // necessary
                                                                // trade blocks
        }
        // if life is in danger
        if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
            combat = ComputerUtilBlock.makeChumpBlocks(ai, combat); // choose
                                                                // necessary
                                                                // chump blocks
        }
        // if life is still in danger
        // Reinforce blockers blocking attackers with trample if life is still
        // in danger
        if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
            combat = ComputerUtilBlock.reinforceBlockersAgainstTrample(ai, combat);
        }
        // Support blockers not destroying the attacker with more blockers to
        // try to kill the attacker
        if (!ComputerUtilCombat.lifeInDanger(ai, combat)) {
            combat = ComputerUtilBlock.reinforceBlockersToKill(ai, combat);
        }

        // == 2. If the AI life would still be in danger make a safer approach
        // ==
        if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
            lifeInDanger = true;
            combat = ComputerUtilBlock.resetBlockers(combat, possibleBlockers); // reset
                                                                                // every
            // block
            // assignment
            combat = ComputerUtilBlock.makeTradeBlocks(ai, combat); // choose
                                                                // necessary
                                                                // trade blocks
            // if life is in danger
            combat = ComputerUtilBlock.makeGoodBlocks(ai, combat);
            if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
                combat = ComputerUtilBlock.makeChumpBlocks(ai, combat); // choose
                                                                    // necessary
                                                                    // chump
            }
            // blocks if life is still in
            // danger
            // Reinforce blockers blocking attackers with trample if life is
            // still in danger
            if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
                combat = ComputerUtilBlock.reinforceBlockersAgainstTrample(ai, combat);
            }
            combat = ComputerUtilBlock.makeGangBlocks(ai, combat);
            combat = ComputerUtilBlock.reinforceBlockersToKill(ai, combat);
        }

        // == 3. If the AI life would be in serious danger make an even safer
        // approach ==
        if (ComputerUtilCombat.lifeInSeriousDanger(ai, combat)) {
            combat = ComputerUtilBlock.resetBlockers(combat, possibleBlockers); // reset
                                                                                // every
            // block
            // assignment
            combat = ComputerUtilBlock.makeChumpBlocks(ai, combat); // choose chump
                                                                // blocks
            if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
                combat = ComputerUtilBlock.makeTradeBlocks(ai, combat); // choose
                                                                    // necessary
                                                                    // trade
            }

            if (!ComputerUtilCombat.lifeInDanger(ai, combat)) {
                combat = ComputerUtilBlock.makeGoodBlocks(ai, combat);
            }
            // Reinforce blockers blocking attackers with trample if life is
            // still in danger
            else {
                combat = ComputerUtilBlock.reinforceBlockersAgainstTrample(ai, combat);
            }
            combat = ComputerUtilBlock.makeGangBlocks(ai, combat);
            // Support blockers not destroying the attacker with more blockers
            // to try to kill the attacker
            combat = ComputerUtilBlock.reinforceBlockersToKill(ai, combat);
        }

        // assign blockers that have to block
        chumpBlockers = CardLists.getKeyword(ComputerUtilBlock.getBlockersLeft(), "CARDNAME blocks each turn if able.");
        // if an attacker with lure attacks - all that can block
        for (final Card blocker : ComputerUtilBlock.getBlockersLeft()) {
            if (CombatUtil.mustBlockAnAttacker(blocker, combat)) {
                chumpBlockers.add(blocker);
            }
        }
        if (!chumpBlockers.isEmpty()) {
            CardLists.shuffle(ComputerUtilBlock.getAttackers());
            for (final Card attacker : ComputerUtilBlock.getAttackers()) {
                blockers = ComputerUtilBlock.getPossibleBlockers(attacker, chumpBlockers, combat, false);
                for (final Card blocker : blockers) {
                    if (CombatUtil.canBlock(attacker, blocker, combat) && ComputerUtilBlock.getBlockersLeft().contains(blocker)
                            && (CombatUtil.mustBlockAnAttacker(blocker, combat)
                                    || blocker.hasKeyword("CARDNAME blocks each turn if able."))) {
                        combat.addBlocker(attacker, blocker);
                        ComputerUtilBlock.getBlockersLeft().remove(blocker);
                    }
                }
            }
        }

        return combat;
    }

    public static List<Card> orderBlockers(Card attacker, List<Card> blockers) {
        // very very simple ordering of blockers, sort by evaluate, then sort by attack
        //final int damage = attacker.getNetCombatDamage();
        CardLists.sortByEvaluateCreature(blockers);
        CardLists.sortByPowerDesc(blockers);

        // TODO: Take total damage, and attempt to maximize killing the greatest evaluation of creatures
        // It's probably generally better to kill the largest creature, but sometimes its better to kill a few smaller ones

        return blockers;
    }

    public static List<Card> orderAttackers(Card attacker, List<Card> blockers) {
        // This shouldn't really take trample into account, but otherwise should be pretty similar to orderBlockers
        // very very simple ordering of attackers, sort by evaluate, then sort by attack
        //final int damage = attacker.getNetCombatDamage();
        CardLists.sortByEvaluateCreature(blockers);
        CardLists.sortByPowerDesc(blockers);

        // TODO: Take total damage, and attempt to maximize killing the greatest evaluation of creatures
        // It's probably generally better to kill the largest creature, but sometimes its better to kill a few smaller ones

        return blockers;
    }
}
