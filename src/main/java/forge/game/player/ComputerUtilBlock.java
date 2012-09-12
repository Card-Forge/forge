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
package forge.game.player;

import java.util.List;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.Counters;
import forge.GameEntity;
import forge.card.cardfactory.CardFactoryUtil;
import forge.game.phase.Combat;
import forge.game.phase.CombatUtil;

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
    private static CardList attackers = new CardList(); // all attackers
    /** Constant <code>attackersLeft</code>. */
    private static CardList attackersLeft = new CardList(); // keeps track of
                                                            // all currently
                                                            // unblocked
                                                            // attackers
    /** Constant <code>blockedButUnkilled</code>. */
    private static CardList blockedButUnkilled = new CardList(); // blocked
                                                                 // attackers
                                                                 // that
                                                                 // currently
                                                                 // wouldn't be
                                                                 // destroyed
    /** Constant <code>blockersLeft</code>. */
    private static CardList blockersLeft = new CardList(); // keeps track of all
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
    private static CardList getAttackers() {
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
    private static void setAttackers(final CardList cardList) {
        ComputerUtilBlock.attackers = (cardList);
    }

    /**
     * <p>
     * Getter for the field <code>attackersLeft</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private static CardList getAttackersLeft() {
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
    private static void setAttackersLeft(final CardList cardList) {
        ComputerUtilBlock.attackersLeft = (cardList);
    }

    /**
     * <p>
     * Getter for the field <code>blockedButUnkilled</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private static CardList getBlockedButUnkilled() {
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
    private static void setBlockedButUnkilled(final CardList cardList) {
        ComputerUtilBlock.blockedButUnkilled = (cardList);
    }

    /**
     * <p>
     * Getter for the field <code>blockersLeft</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private static CardList getBlockersLeft() {
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
    private static void setBlockersLeft(final CardList cardList) {
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
    private static CardList getPossibleBlockers(final Card attacker, final CardList blockersLeft, final Combat combat) {
        final CardList blockers = new CardList();

        for (final Card blocker : blockersLeft) {
            // if the blocker can block a creature with lure it can't block a
            // creature without
            if (CombatUtil.canBlock(attacker, blocker, combat)) {
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
    private static CardList getSafeBlockers(final Card attacker, final CardList blockersLeft, final Combat combat) {
        final CardList blockers = new CardList();

        for (final Card b : blockersLeft) {
            if (!CombatUtil.canDestroyBlocker(b, attacker, combat, false)) {
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
    private static CardList getKillingBlockers(final Card attacker, final CardList blockersLeft, final Combat combat) {
        final CardList blockers = new CardList();

        for (final Card b : blockersLeft) {
            if (CombatUtil.canDestroyAttacker(attacker, b, combat, false)) {
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
    public static CardList sortPotentialAttackers(final Combat combat) {
        final CardList[] attackerLists = combat.sortAttackerByDefender();
        final CardList sortedAttackers = new CardList();

        final List<GameEntity> defenders = combat.getDefenders();

        // Begin with the attackers that pose the biggest threat
        CardListUtil.sortByEvaluateCreature(attackerLists[0]);
        CardListUtil.sortAttack(attackerLists[0]);

        // If I don't have any planeswalkers than sorting doesn't really matter
        if (defenders.size() == 1) {
            return attackerLists[0];
        }

        final boolean bLifeInDanger = CombatUtil.lifeInDanger(combat);

        // TODO Add creatures attacking Planeswalkers in order of which we want
        // to protect
        // defend planeswalkers with more loyalty before planeswalkers with less
        // loyalty
        // if planeswalker will be too difficult to defend don't even bother
        for (int i = 1; i < attackerLists.length; i++) {
            // Begin with the attackers that pose the biggest threat
            CardListUtil.sortAttack(attackerLists[i]);
            for (final Card c : attackerLists[i]) {
                sortedAttackers.add(c);
            }
        }

        if (bLifeInDanger) {
            // add creatures attacking the Player to the front of the list
            for (final Card c : attackerLists[0]) {
                sortedAttackers.add(0, c);
            }

        } else {
            // add creatures attacking the Player to the back of the list
            for (final Card c : attackerLists[0]) {
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
    private static Combat makeGoodBlocks(final Combat combat) {

        CardList currentAttackers = new CardList(ComputerUtilBlock.getAttackersLeft());

        for (final Card attacker : ComputerUtilBlock.getAttackersLeft()) {

            if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")) {
                continue;
            }

            Card blocker = null;

            final CardList blockers = ComputerUtilBlock.getPossibleBlockers(attacker,
                    ComputerUtilBlock.getBlockersLeft(), combat);

            final CardList safeBlockers = ComputerUtilBlock.getSafeBlockers(attacker, blockers, combat);
            CardList killingBlockers;

            if (safeBlockers.size() > 0) {
                // 1.Blockers that can destroy the attacker but won't get
                // destroyed
                killingBlockers = ComputerUtilBlock.getKillingBlockers(attacker, safeBlockers, combat);
                if (killingBlockers.size() > 0) {
                    blocker = CardFactoryUtil.getWorstCreatureAI(killingBlockers);
                } else if (!attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
                    blocker = CardFactoryUtil.getWorstCreatureAI(safeBlockers);
                    ComputerUtilBlock.getBlockedButUnkilled().add(attacker);
                }
            } // no safe blockers
            else {
                // 3.Blockers that can destroy the attacker and have an upside when dying
                killingBlockers = ComputerUtilBlock.getKillingBlockers(attacker, blockers, combat);
                for (Card b : killingBlockers) {
                    if ((b.hasKeyword("Undying") && b.getCounters(Counters.P1P1) == 0)
                            || !b.getSVar("SacMe").equals("")) {
                        blocker = b;
                        break;
                    }
                }
                // 4.Blockers that can destroy the attacker and are worth less
                if (blocker == null && killingBlockers.size() > 0) {
                    final Card worst = CardFactoryUtil.getWorstCreatureAI(killingBlockers);

                    if ((CardFactoryUtil.evaluateCreature(worst) + ComputerUtilBlock.getDiff()) < CardFactoryUtil
                            .evaluateCreature(attacker)) {
                        blocker = worst;
                    }
                }
            }
            if (blocker != null) {
                currentAttackers.remove(attacker);
                ComputerUtilBlock.getBlockersLeft().remove(blocker);
                combat.addBlocker(attacker, blocker);
            }
        }
        ComputerUtilBlock.setAttackersLeft(new CardList(currentAttackers));
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
    private static Combat makeGangBlocks(final Combat combat) {

        CardList currentAttackers = new CardList(ComputerUtilBlock.getAttackersLeft());
        currentAttackers = currentAttackers.getKeywordsDontContain("Rampage");
        currentAttackers = currentAttackers
                .getKeywordsDontContain("CARDNAME can't be blocked by more than one creature.");
        CardList blockers;

        // Try to block an attacker without first strike with a gang of first strikers
        for (final Card attacker : ComputerUtilBlock.getAttackersLeft()) {
            if (!attacker.hasKeyword("First Strike") && !attacker.hasKeyword("Double Strike")) {
                blockers = ComputerUtilBlock.getPossibleBlockers(attacker, ComputerUtilBlock.getBlockersLeft(), combat);
                final CardList firstStrikeBlockers = new CardList();
                final CardList blockGang = new CardList();
                for (int i = 0; i < blockers.size(); i++) {
                    if (blockers.get(i).hasFirstStrike() || blockers.get(i).hasDoubleStrike()) {
                        firstStrikeBlockers.add(blockers.get(i));
                    }
                }

                if (firstStrikeBlockers.size() > 1) {
                    CardListUtil.sortAttack(firstStrikeBlockers);
                    for (final Card blocker : firstStrikeBlockers) {
                        final int damageNeeded = attacker.getKillDamage()
                                + CombatUtil.predictToughnessBonusOfAttacker(attacker, blocker, combat);
                        // if the total damage of the blockgang was not enough
                        // without but is enough with this blocker finish the
                        // blockgang
                        if (CombatUtil.totalDamageOfBlockers(attacker, blockGang) < damageNeeded
                                || CombatUtil.needsBlockers(attacker) > blockGang.size()) {
                            blockGang.add(blocker);
                            if (CombatUtil.totalDamageOfBlockers(attacker, blockGang) >= damageNeeded) {
                                currentAttackers.remove(attacker);
                                for (final Card b : blockGang) {
                                    if (CombatUtil.canBlock(attacker, blocker, combat)) {
                                        ComputerUtilBlock.getBlockersLeft().remove(b);
                                        combat.addBlocker(attacker, b);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        ComputerUtilBlock.setAttackersLeft(new CardList(currentAttackers));
        currentAttackers = new CardList(ComputerUtilBlock.getAttackersLeft());

        // Try to block an attacker with two blockers of which only one will die
        for (final Card attacker : ComputerUtilBlock.getAttackersLeft()) {
            blockers = ComputerUtilBlock.getPossibleBlockers(attacker, ComputerUtilBlock.getBlockersLeft(), combat);
            CardList usableBlockers;
            final CardList blockGang = new CardList();
            int absorbedDamage = 0; // The amount of damage needed to kill the
                                    // first blocker
            int currentValue = 0; // The value of the creatures in the blockgang

            // Try to add blockers that could be destroyed, but are worth less
            // than the attacker
            // Don't use blockers without First Strike or Double Strike if
            // attacker has it
            usableBlockers = blockers.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    if ((attacker.hasKeyword("First Strike") || attacker.hasKeyword("Double Strike"))
                            && !(c.hasKeyword("First Strike") || c.hasKeyword("Double Strike"))) {
                        return false;
                    }
                    return lifeInDanger || (CardFactoryUtil.evaluateCreature(c) + ComputerUtilBlock.getDiff()) < CardFactoryUtil
                            .evaluateCreature(attacker);
                }
            });
            if (usableBlockers.size() < 2) {
                return combat;
            }

            final Card leader = CardFactoryUtil.getBestCreatureAI(usableBlockers);
            blockGang.add(leader);
            usableBlockers.remove(leader);
            absorbedDamage = leader.getEnoughDamageToKill(attacker.getNetCombatDamage(), attacker, true);
            currentValue = CardFactoryUtil.evaluateCreature(leader);

            for (final Card blocker : usableBlockers) {
                // Add an additional blocker if the current blockers are not
                // enough and the new one would deal the remaining damage
                final int currentDamage = CombatUtil.totalDamageOfBlockers(attacker, blockGang);
                final int additionalDamage = CombatUtil.dealsDamageAsBlocker(attacker, blocker);
                final int absorbedDamage2 = blocker
                        .getEnoughDamageToKill(attacker.getNetCombatDamage(), attacker, true);
                final int addedValue = CardFactoryUtil.evaluateCreature(blocker);
                final int damageNeeded = attacker.getKillDamage()
                        + CombatUtil.predictToughnessBonusOfAttacker(attacker, blocker, combat);
                if ((damageNeeded > currentDamage || CombatUtil.needsBlockers(attacker) > blockGang.size())
                        && (!(damageNeeded > currentDamage + additionalDamage)
                        // The attacker will be killed
                        && (absorbedDamage2 + absorbedDamage > attacker.getNetCombatDamage()
                        // only one blocker can be killed
                        || currentValue + addedValue - 50 <= CardFactoryUtil.evaluateCreature(attacker)))
                        // or attacker is worth more
                        || (lifeInDanger && CombatUtil.lifeInDanger(combat))
                        && CombatUtil.canBlock(attacker, blocker, combat)) {
                    // this is needed for attackers that can't be blocked by
                    // more than 1
                    currentAttackers.remove(attacker);
                    combat.addBlocker(attacker, blocker);
                    ComputerUtilBlock.getBlockersLeft().remove(blocker);
                    if (CombatUtil.canBlock(attacker, leader, combat)) {
                        combat.addBlocker(attacker, leader);
                        ComputerUtilBlock.getBlockersLeft().remove(leader);
                    }
                    break;
                }
            }
        }

        ComputerUtilBlock.setAttackersLeft(new CardList(currentAttackers));
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
    private static Combat makeTradeBlocks(final Combat combat) {

        CardList currentAttackers = new CardList(ComputerUtilBlock.getAttackersLeft());
        CardList killingBlockers;

        for (final Card attacker : ComputerUtilBlock.getAttackersLeft()) {

            if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")) {
                continue;
            }

            killingBlockers = ComputerUtilBlock.getKillingBlockers(attacker,
                    ComputerUtilBlock.getPossibleBlockers(attacker, ComputerUtilBlock.getBlockersLeft(), combat),
                    combat);
            if ((killingBlockers.size() > 0) && CombatUtil.lifeInDanger(combat)) {
                final Card blocker = CardFactoryUtil.getWorstCreatureAI(killingBlockers);
                combat.addBlocker(attacker, blocker);
                currentAttackers.remove(attacker);
                ComputerUtilBlock.getBlockersLeft().remove(blocker);
            }
        }
        ComputerUtilBlock.setAttackersLeft(new CardList(currentAttackers));
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
    private static Combat makeChumpBlocks(final Combat combat) {

        CardList currentAttackers = new CardList(ComputerUtilBlock.getAttackersLeft());
        CardList chumpBlockers;

        for (final Card attacker : ComputerUtilBlock.getAttackersLeft()) {

            if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")
                    || attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
                continue;
            }

            chumpBlockers = ComputerUtilBlock
                    .getPossibleBlockers(attacker, ComputerUtilBlock.getBlockersLeft(), combat);
            if ((chumpBlockers.size() > 0) && CombatUtil.lifeInDanger(combat)) {
                final Card blocker = CardFactoryUtil.getWorstCreatureAI(chumpBlockers);
                combat.addBlocker(attacker, blocker);
                currentAttackers.remove(attacker);
                ComputerUtilBlock.getBlockedButUnkilled().add(attacker);
                ComputerUtilBlock.getBlockersLeft().remove(blocker);
            }
        }
        ComputerUtilBlock.setAttackersLeft(new CardList(currentAttackers));
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
    private static Combat reinforceBlockersAgainstTrample(final Combat combat) {

        CardList chumpBlockers;

        CardList tramplingAttackers = ComputerUtilBlock.getAttackers().getKeyword("Trample");
        tramplingAttackers = tramplingAttackers.getKeywordsDontContain("Rampage"); // Don't
                                                                                   // make
                                                                                   // it
                                                                                   // worse
        tramplingAttackers = tramplingAttackers
                .getKeywordsDontContain("CARDNAME can't be blocked by more than one creature.");
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
                    .getPossibleBlockers(attacker, ComputerUtilBlock.getBlockersLeft(), combat);
            for (final Card blocker : chumpBlockers) {
                // Add an additional blocker if the current blockers are not
                // enough and the new one would suck some of the damage
                if ((CombatUtil.getAttack(attacker) > CombatUtil.totalShieldDamage(attacker,
                        combat.getBlockers(attacker)))
                        && (CombatUtil.shieldDamage(attacker, blocker) > 0)
                        && CombatUtil.canBlock(attacker, blocker, combat) && CombatUtil.lifeInDanger(combat)) {
                    combat.addBlocker(attacker, blocker);
                    ComputerUtilBlock.getBlockersLeft().remove(blocker);
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
    private static Combat reinforceBlockersToKill(final Combat combat) {

        CardList safeBlockers;
        CardList blockers;
        CardList targetAttackers = ComputerUtilBlock.getBlockedButUnkilled().getKeywordsDontContain("Rampage"); // Don't
        // make
        // it
        // worse
        targetAttackers = targetAttackers
                .getKeywordsDontContain("CARDNAME can't be blocked by more than one creature.");
        // TODO - should check here for a "rampage-like" trigger that replaced
        // the keyword:
        // "Whenever CARDNAME becomes blocked, it gets +1/+1 until end of turn for each creature blocking it."

        for (final Card attacker : targetAttackers) {
            blockers = ComputerUtilBlock.getPossibleBlockers(attacker, ComputerUtilBlock.getBlockersLeft(), combat);

            // Try to use safe blockers first
            safeBlockers = ComputerUtilBlock.getSafeBlockers(attacker, blockers, combat);
            for (final Card blocker : safeBlockers) {
                final int damageNeeded = attacker.getKillDamage()
                        + CombatUtil.predictToughnessBonusOfAttacker(attacker, blocker, combat);
                // Add an additional blocker if the current blockers are not
                // enough and the new one would deal additional damage
                if ((damageNeeded > CombatUtil.totalDamageOfBlockers(attacker, combat.getBlockers(attacker)))
                        && (CombatUtil.dealsDamageAsBlocker(attacker, blocker) > 0)
                        && CombatUtil.canBlock(attacker, blocker, combat)) {
                    combat.addBlocker(attacker, blocker);
                    ComputerUtilBlock.getBlockersLeft().remove(blocker);
                }
                blockers.remove(blocker); // Don't check them again next
            }

            // Try to add blockers that could be destroyed, but are worth less
            // than the attacker
            // Don't use blockers without First Strike or Double Strike if
            // attacker has it
            if (attacker.hasKeyword("First Strike") || attacker.hasKeyword("Double Strike")) {
                safeBlockers = blockers.getKeyword("First Strike");
                safeBlockers.addAll(blockers.getKeyword("Double Strike"));
            } else {
                safeBlockers = new CardList(blockers);
            }

            for (final Card blocker : safeBlockers) {
                final int damageNeeded = attacker.getKillDamage()
                        + CombatUtil.predictToughnessBonusOfAttacker(attacker, blocker, combat);
                // Add an additional blocker if the current blockers are not
                // enough and the new one would deal the remaining damage
                final int currentDamage = CombatUtil.totalDamageOfBlockers(attacker, combat.getBlockers(attacker));
                final int additionalDamage = CombatUtil.dealsDamageAsBlocker(attacker, blocker);
                if ((damageNeeded > currentDamage)
                        && !(damageNeeded > (currentDamage + additionalDamage))
                        && ((CardFactoryUtil.evaluateCreature(blocker) + ComputerUtilBlock.getDiff()) < CardFactoryUtil
                                .evaluateCreature(attacker)) && CombatUtil.canBlock(attacker, blocker, combat)) {
                    combat.addBlocker(attacker, blocker);
                    ComputerUtilBlock.getBlockersLeft().removeAll(blocker);
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
    private static Combat resetBlockers(final Combat combat, final CardList possibleBlockers) {

        final CardList oldBlockers = combat.getAllBlockers();
        for (final Card blocker : oldBlockers) {
            combat.removeFromCombat(blocker);
        }

        ComputerUtilBlock.setAttackersLeft(new CardList(ComputerUtilBlock.getAttackers())); // keeps
                                                                                                      // track
        // of all
        // currently
        // unblocked
        // attackers
        ComputerUtilBlock.setBlockersLeft(new CardList(possibleBlockers)); // keeps
        // track of
        // all
        // unassigned
        // blockers
        ComputerUtilBlock.setBlockedButUnkilled(new CardList()); // keeps track
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
    public static Combat getBlockers(final Combat originalCombat, final CardList possibleBlockers) {

        Combat combat = originalCombat;

        ComputerUtilBlock.setAttackers(ComputerUtilBlock.sortPotentialAttackers(combat));

        if (ComputerUtilBlock.getAttackers().size() == 0) {
            return combat;
        }

        // keeps track of all currently unblocked attackers
        ComputerUtilBlock.setAttackersLeft(new CardList(ComputerUtilBlock.getAttackers())); 
        // keeps track of all unassigned blockers
        ComputerUtilBlock.setBlockersLeft(new CardList(possibleBlockers)); 
        // keeps track of all blocked attackers that currently wouldn't be destroyed
        ComputerUtilBlock.setBlockedButUnkilled(new CardList());
        CardList blockers;
        CardList chumpBlockers;

        ComputerUtilBlock.setDiff((AllZone.getComputerPlayer().getLife() * 2) - 5); // This
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
        CardListUtil.sortAttackLowFirst(ComputerUtilBlock.getBlockersLeft());

        // == 1. choose best blocks first ==
        combat = ComputerUtilBlock.makeGoodBlocks(combat);
        combat = ComputerUtilBlock.makeGangBlocks(combat);
        if (CombatUtil.lifeInDanger(combat)) {
            combat = ComputerUtilBlock.makeTradeBlocks(combat); // choose
                                                                // necessary
                                                                // trade blocks
        }
        // if life is in danger
        if (CombatUtil.lifeInDanger(combat)) {
            combat = ComputerUtilBlock.makeChumpBlocks(combat); // choose
                                                                // necessary
                                                                // chump blocks
        }
        // if life is still in danger
        // Reinforce blockers blocking attackers with trample if life is still
        // in danger
        if (CombatUtil.lifeInDanger(combat)) {
            combat = ComputerUtilBlock.reinforceBlockersAgainstTrample(combat);
        }
        // Support blockers not destroying the attacker with more blockers to
        // try to kill the attacker
        if (!CombatUtil.lifeInDanger(combat)) {
            combat = ComputerUtilBlock.reinforceBlockersToKill(combat);
        }

        // == 2. If the AI life would still be in danger make a safer approach
        // ==
        if (CombatUtil.lifeInDanger(combat)) {
            lifeInDanger = true;
            combat = ComputerUtilBlock.resetBlockers(combat, possibleBlockers); // reset
                                                                                // every
            // block
            // assignment
            combat = ComputerUtilBlock.makeTradeBlocks(combat); // choose
                                                                // necessary
                                                                // trade blocks
            // if life is in danger
            combat = ComputerUtilBlock.makeGoodBlocks(combat);
            if (CombatUtil.lifeInDanger(combat)) {
                combat = ComputerUtilBlock.makeChumpBlocks(combat); // choose
                                                                    // necessary
                                                                    // chump
            }
            // blocks if life is still in
            // danger
            // Reinforce blockers blocking attackers with trample if life is
            // still in danger
            if (CombatUtil.lifeInDanger(combat)) {
                combat = ComputerUtilBlock.reinforceBlockersAgainstTrample(combat);
            }
            combat = ComputerUtilBlock.makeGangBlocks(combat);
            combat = ComputerUtilBlock.reinforceBlockersToKill(combat);
        }

        // == 3. If the AI life would be in serious danger make an even safer
        // approach ==
        if (CombatUtil.lifeInSeriousDanger(combat)) {
            combat = ComputerUtilBlock.resetBlockers(combat, possibleBlockers); // reset
                                                                                // every
            // block
            // assignment
            combat = ComputerUtilBlock.makeChumpBlocks(combat); // choose chump
                                                                // blocks
            if (CombatUtil.lifeInDanger(combat)) {
                combat = ComputerUtilBlock.makeTradeBlocks(combat); // choose
                                                                    // necessary
                                                                    // trade
            }
            // blocks if life is in danger
            if (!CombatUtil.lifeInDanger(combat)) {
                combat = ComputerUtilBlock.makeGoodBlocks(combat);
            }
            // Reinforce blockers blocking attackers with trample if life is
            // still in danger
            if (CombatUtil.lifeInDanger(combat)) {
                combat = ComputerUtilBlock.reinforceBlockersAgainstTrample(combat);
            }
            combat = ComputerUtilBlock.makeGangBlocks(combat);
            // Support blockers not destroying the attacker with more blockers
            // to try to kill the attacker
            combat = ComputerUtilBlock.reinforceBlockersToKill(combat);
        }

        // assign blockers that have to block
        chumpBlockers = ComputerUtilBlock.getBlockersLeft().getKeyword("CARDNAME blocks each turn if able.");
        // if an attacker with lure attacks - all that can block
        for (final Card blocker : ComputerUtilBlock.getBlockersLeft()) {
            if (CombatUtil.mustBlockAnAttacker(blocker, combat)) {
                chumpBlockers.add(blocker);
            }
        }
        if (!chumpBlockers.isEmpty()) {
            ComputerUtilBlock.getAttackers().shuffle();
            for (final Card attacker : ComputerUtilBlock.getAttackers()) {
                blockers = ComputerUtilBlock.getPossibleBlockers(attacker, chumpBlockers, combat);
                for (final Card blocker : blockers) {
                    if (CombatUtil.canBlock(attacker, blocker, combat) && ComputerUtilBlock.getBlockersLeft().contains(blocker)
                            && (CombatUtil.mustBlockAnAttacker(blocker, combat)
                                    || blocker.hasKeyword("CARDNAME blocks each turn if able."))) {
                        combat.addBlocker(attacker, blocker);
                        ComputerUtilBlock.getBlockersLeft().removeAll(blocker);
                    }
                }
            }
        }

        return combat;
    }
    
    public static CardList orderBlockers(Card attacker, CardList blockers) {
        // very very simple ordering of blockers, sort by evaluate, then sort by attack
        //final int damage = attacker.getNetCombatDamage();
        CardListUtil.sortByEvaluateCreature(blockers);
        CardListUtil.sortAttack(blockers);
        
        // TODO: Take total damage, and attempt to maximize killing the greatest evaluation of creatures
        // It's probably generally better to kill the largest creature, but sometimes its better to kill a few smaller ones
        
        return blockers;
    }
    
    public static CardList orderAttackers(Card attacker, CardList blockers) {
        // This shouldn't really take trample into account, but otherwise should be pretty similar to orderBlockers
        // very very simple ordering of attackers, sort by evaluate, then sort by attack
        //final int damage = attacker.getNetCombatDamage();
        CardListUtil.sortByEvaluateCreature(blockers);
        CardListUtil.sortAttack(blockers);
        
        // TODO: Take total damage, and attempt to maximize killing the greatest evaluation of creatures
        // It's probably generally better to kill the largest creature, but sometimes its better to kill a few smaller ones
        
        return blockers;
    }
}
