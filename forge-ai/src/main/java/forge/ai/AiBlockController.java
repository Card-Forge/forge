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
package forge.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardStateName;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.collect.FCollectionView;


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
    private List<Card> attackers = new ArrayList<>(); // all attackers
    /** Constant <code>attackersLeft</code>. */
    private List<Card> attackersLeft = new ArrayList<>(); // keeps track of all currently unblocked attackers
    /** Constant <code>blockedButUnkilled</code>. */
    private List<Card> blockedButUnkilled = new ArrayList<>(); // blocked attackers that currently wouldn't be destroyed
    /** Constant <code>blockersLeft</code>. */
    private List<Card> blockersLeft = new ArrayList<>(); // keeps track of all unassigned blockers
    private int diff = 0;

    private boolean lifeInDanger = false;

    public AiBlockController(Player aiPlayer) {
        ai = aiPlayer;
    }

    // finds the creatures able to block the attacker
    private static List<Card> getPossibleBlockers(final Combat combat, final Card attacker, final List<Card> blockersLeft, final boolean solo) {
        final List<Card> blockers = new ArrayList<>();

        for (final Card blocker : blockersLeft) {
            // if the blocker can block a creature with lure it can't block a creature without
            if (CombatUtil.canBlock(attacker, blocker, combat)) {
                boolean cantBlockAlone = blocker.hasKeyword("CARDNAME can't attack or block alone.") || blocker.hasKeyword("CARDNAME can't block alone.");
                if (solo && cantBlockAlone) {
                    continue;
                }
                blockers.add(blocker);
            }
        }

        return blockers;
    }

    // finds blockers that won't be destroyed
    private List<Card> getSafeBlockers(final Combat combat, final Card attacker, final List<Card> blockersLeft) {
        final List<Card> blockers = new ArrayList<>();

        // We don't check attacker static abilities at this point since the attackers have already attacked and, thus,
        // their P/T modifiers are active and are counted as a part of getNetPower/getNetToughness
        for (final Card b : blockersLeft) {
            if (!ComputerUtilCombat.canDestroyBlocker(ai, b, attacker, combat, false, true)) {
                blockers.add(b);
            }
        }

        return blockers;
    }

    // finds blockers that destroy the attacker
    private List<Card> getKillingBlockers(final Combat combat, final Card attacker, final List<Card> blockersLeft) {
        final List<Card> blockers = new ArrayList<>();

        // We don't check attacker static abilities at this point since the attackers have already attacked and, thus,
        // their P/T modifiers are active and are counted as a part of getNetPower/getNetToughness
        for (final Card b : blockersLeft) {
            if (ComputerUtilCombat.canDestroyAttacker(ai, attacker, b, combat, false, true)) {
                blockers.add(b);
            }
        }

        return blockers;
    }

    private List<Card> sortPotentialAttackers(final Combat combat) {
        final CardCollection sortedAttackers = new CardCollection();
        CardCollection firstAttacker = new CardCollection();

        final FCollectionView<GameEntity> defenders = combat.getDefenders();

        // If I don't have any planeswalkers then sorting doesn't really matter
        if (defenders.size() == 1) {
            final CardCollection attackers = combat.getAttackersOf(defenders.get(0));
            // Begin with the attackers that pose the biggest threat
            ComputerUtilCard.sortByEvaluateCreature(attackers);
            CardLists.sortByPowerDesc(attackers);
            //move cards like Phage the Untouchable to the front
            Collections.sort(attackers, new Comparator<Card>() {
                @Override
                public int compare(final Card o1, final Card o2) {
                    if (o1.hasSVar("MustBeBlocked") && !o2.hasSVar("MustBeBlocked")) {
                        return -1;
                    } else if (!o1.hasSVar("MustBeBlocked") && o2.hasSVar("MustBeBlocked")) {
                        return 1;
                    }
                    return 0;
                }
            });
            return attackers;
        }

        final boolean bLifeInDanger = ComputerUtilCombat.lifeInDanger(ai, combat);

        // TODO Add creatures attacking Planeswalkers in order of which we want to protect
        // defend planeswalkers with more loyalty before planeswalkers with less loyalty
        // if planeswalker will be too difficult to defend don't even bother
        for (GameEntity defender : defenders) {
            if (defender instanceof Card) {
                final CardCollection attackers = combat.getAttackersOf(defender);
                // Begin with the attackers that pose the biggest threat
                CardLists.sortByPowerDesc(attackers);
                sortedAttackers.addAll(attackers);
            } else if (defender instanceof Player && defender.equals(ai)) {
                firstAttacker = combat.getAttackersOf(defender);
            }
        }

        if (bLifeInDanger) {
            // add creatures attacking the Player to the front of the list
            for (final Card c : firstAttacker) {
                sortedAttackers.add(0, c);
            }
        } else {
            // add creatures attacking the Player to the back of the list
            sortedAttackers.addAll(firstAttacker);
        }
        return sortedAttackers;
    }

    // ======================= block assignment functions
    // ================================

    // Good Blocks means a good trade or no trade
    private void makeGoodBlocks(final Combat combat) {

        List<Card> currentAttackers = new ArrayList<>(attackersLeft);

        for (final Card attacker : attackersLeft) {

            if (attacker.hasStartOfKeyword("CantBeBlockedByAmount LT")
                    || attacker.hasKeyword("CARDNAME can't be blocked unless all creatures defending player controls block it.")
                    || attacker.hasKeyword(Keyword.MENACE)) {
                continue;
            }

            Card blocker = null;

            final List<Card> blockers = getPossibleBlockers(combat, attacker, blockersLeft, true);

            final List<Card> safeBlockers = getSafeBlockers(combat, attacker, blockers);
            List<Card> killingBlockers;

            if (!safeBlockers.isEmpty()) {
                // 1.Blockers that can destroy the attacker but won't get destroyed
                killingBlockers = getKillingBlockers(combat, attacker, safeBlockers);
                if (!killingBlockers.isEmpty()) {
                    if (ComputerUtilCombat.attackerHasThreateningAfflict(attacker, ai)) {
                        continue;
                    }
                    blocker = ComputerUtilCard.getWorstCreatureAI(killingBlockers);
                // 2.Blockers that won't get destroyed
                } else if (!attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                    && !ComputerUtilCombat.attackerHasThreateningAfflict(attacker, ai)) {
                    blocker = ComputerUtilCard.getWorstCreatureAI(safeBlockers);
                    // check whether it's better to block a creature without trample to absorb more damage
                    if (attacker.hasKeyword(Keyword.TRAMPLE)) {
                        boolean doNotBlock = false;
                        for (Card other : attackersLeft) {
                            if (other.equals(attacker) || !CombatUtil.canBlock(other, blocker)
                                    || other.hasKeyword(Keyword.TRAMPLE)
                                    || ComputerUtilCombat.attackerHasThreateningAfflict(other, ai)
                                    || ComputerUtilCombat.canDestroyBlocker(ai, blocker, other, combat, false)
                                    || other.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
                                continue;
                            }

                            if (other.getNetCombatDamage() > blocker.getLethalDamage()) {
                                doNotBlock = true;
                                break;
                            }
                        }
                        if (doNotBlock) {
                            continue;
                        }
                    }
                    blockedButUnkilled.add(attacker);
                }
            } // no safe blockers
            else {
                // 3.Blockers that can destroy the attacker and have an upside when dying
                killingBlockers = getKillingBlockers(combat, attacker, blockers);
                for (Card b : killingBlockers) {
                    if ((b.hasKeyword(Keyword.UNDYING) && b.getCounters(CounterEnumType.P1P1) == 0) || b.hasSVar("SacMe")
                            || (b.hasKeyword(Keyword.VANISHING) && b.getCounters(CounterEnumType.TIME) == 1)
                            || (b.hasKeyword(Keyword.FADING) && b.getCounters(CounterEnumType.FADE) == 0)
                            || b.hasSVar("EndOfTurnLeavePlay")) {
                        blocker = b;
                        break;
                    }
                }
                // 4.Blockers that have a big upside when dying
                for (Card b : blockers) {
                    if (b.hasSVar("SacMe") && Integer.parseInt(b.getSVar("SacMe")) > 3) {
                        blocker = b;
                        if (!ComputerUtilCombat.canDestroyAttacker(ai, attacker, blocker, combat, false)) {
                            blockedButUnkilled.add(attacker);
                        }
                        break;
                    }
                }
                // 5.Blockers that can destroy the attacker and are worth less
                if (!killingBlockers.isEmpty()) {
                    final Card worst = ComputerUtilCard.getWorstCreatureAI(killingBlockers);
                    int value = ComputerUtilCard.evaluateCreature(attacker);

                    // check for triggers when unblocked
                    for (Trigger trigger : attacker.getTriggers()) {
                        final Map<String, String> trigParams = trigger.getMapParams();
                        TriggerType mode = trigger.getMode();

                        if (!trigger.requirementsCheck(attacker.getGame())) {
                            continue;
                        }

                        if (mode == TriggerType.DamageDone) {
                            if ((!trigParams.containsKey("ValidSource")
                                        || trigger.matchesValid(attacker, trigParams.get("ValidSource").split(",")))
                                    && attacker.getNetCombatDamage() > 0
                                    && (!trigParams.containsKey("ValidTarget")
                                            || trigger.matchesValid(combat.getDefenderByAttacker(attacker), trigParams.get("ValidTarget").split(",")))) {
                                value += 50;
                            }
                        } else if (mode == TriggerType.AttackerUnblocked) {
                            if (trigger.matchesValid(attacker, trigParams.get("ValidCard").split(","))) {
                                value += 50;
                            }
                        }
                    }

                    if (ComputerUtilCard.evaluateCreature(worst) + diff < value) {
                        blocker = worst;
                    }
                }
            }
            if (blocker != null) {
                currentAttackers.remove(attacker);
                combat.addBlocker(attacker, blocker);
            }
        }
        attackersLeft = (new ArrayList<>(currentAttackers));

        // 6. Blockers that don't survive until the next turn anyway
        for (final Card attacker : attackersLeft) {
            if (attacker.hasStartOfKeyword("CantBeBlockedByAmount LT") || attacker.hasKeyword(Keyword.MENACE)
                    || attacker.hasKeyword("CARDNAME can't be blocked unless all creatures defending player controls block it.")) {
                continue;
            }

            Card blocker = null;

            final List<Card> blockers = getPossibleBlockers(combat, attacker, blockersLeft, true);

            for (Card b : blockers) {
                if ((b.hasKeyword(Keyword.VANISHING) && b.getCounters(CounterEnumType.TIME) == 1)
                        || (b.hasKeyword(Keyword.FADING) && b.getCounters(CounterEnumType.FADE) == 0)
                        || b.hasSVar("EndOfTurnLeavePlay")) {
                    blocker = b;
                    if (!ComputerUtilCombat.canDestroyAttacker(ai, attacker, blocker, combat, false)) {
                        blockedButUnkilled.add(attacker);
                    }
                    break;
                }
            }
            if (blocker != null) {
                currentAttackers.remove(attacker);
                combat.addBlocker(attacker, blocker);
            }
        }
        attackersLeft = (new ArrayList<>(currentAttackers));
    }

    static final Predicate<Card> rampagesOrNeedsManyToBlock = Predicates.or(CardPredicates.containsKeyword("Rampage"), CardPredicates.containsKeyword("CantBeBlockedByAmount GT"));

    // Good Gang Blocks means a good trade or no trade
    /**
     * <p>
     * makeGangBlocks.
     * </p>
     *
     * @param combat a {@link forge.game.combat.Combat} object.
     */
    private void makeGangBlocks(final Combat combat) {
        List<Card> currentAttackers = CardLists.filter(attackersLeft, Predicates.not(rampagesOrNeedsManyToBlock));
        List<Card> blockers;

        // Try to block an attacker without first strike with a gang of first strikers
        for (final Card attacker : attackersLeft) {
            if (ComputerUtilCombat.attackerCantBeDestroyedInCombat(ai, attacker)) {
                // don't bother with gang blocking if the attacker will regenerate or is indestructible
                continue;
            }
            if (!ComputerUtilCombat.dealsFirstStrikeDamage(attacker, false, combat)) {
                blockers = getPossibleBlockers(combat, attacker, blockersLeft, false);
                final List<Card> firstStrikeBlockers = new ArrayList<>();
                final List<Card> blockGang = new ArrayList<>();
                for (Card blocker : blockers) {
                    if (ComputerUtilCombat.canDestroyBlockerBeforeFirstStrike(blocker, attacker, false)) {
                        continue;
                    }
                    if (blocker.hasFirstStrike() || blocker.hasDoubleStrike()) {
                        firstStrikeBlockers.add(blocker);
                    }
                }

                if (firstStrikeBlockers.size() > 1) {
                    CardLists.sortByPowerDesc(firstStrikeBlockers);
                    for (final Card blocker : firstStrikeBlockers) {
                        final int damageNeeded = ComputerUtilCombat.getDamageToKill(attacker)
                                + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, false);
                        // if the total damage of the blockgang was not enough
                        // without but is enough with this blocker finish the blockgang
                        if (ComputerUtilCombat.totalFirstStrikeDamageOfBlockers(attacker, blockGang) < damageNeeded
                                || CombatUtil.getMinNumBlockersForAttacker(attacker, ai) > blockGang.size()) {
                            blockGang.add(blocker);
                            if (ComputerUtilCombat.totalFirstStrikeDamageOfBlockers(attacker, blockGang) >= damageNeeded) {
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

        attackersLeft = (new ArrayList<>(currentAttackers));
        currentAttackers = new ArrayList<>(attackersLeft);

        boolean considerTripleBlock = true;

        // Try to block an attacker with two blockers of which only one will die
        for (final Card attacker : attackersLeft) {
            if (ComputerUtilCombat.attackerCantBeDestroyedInCombat(ai, attacker)) {
                // don't bother with gang blocking if the attacker will regenerate or is indestructible
                continue;
            }

            int evalAttackerValue = ComputerUtilCard.evaluateCreature(attacker);

            blockers = getPossibleBlockers(combat, attacker, blockersLeft, false);
            List<Card> usableBlockers;
            final List<Card> blockGang = new ArrayList<>();
            int absorbedDamage; // The amount of damage needed to kill the first blocker
            int currentValue; // The value of the creatures in the blockgang
            boolean foundDoubleBlock = false; // if true, a good double block is found

            // AI can't handle good blocks with more than three creatures yet
            if (CombatUtil.getMinNumBlockersForAttacker(attacker, ai) > (considerTripleBlock ? 3 : 2)) {
                continue;
            }

            // Try to add blockers that could be destroyed, but are worth less than the attacker
            // Don't use blockers without First Strike or Double Strike if attacker has it
            usableBlockers = CardLists.filter(blockers, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (ComputerUtilCombat.dealsFirstStrikeDamage(attacker, false, combat)
                            && !ComputerUtilCombat.dealsFirstStrikeDamage(c, false, combat)) {
                        return false;
                    }
                    final boolean randomTrade = wouldLikeToRandomlyTrade(attacker, c, combat);
                    return lifeInDanger || ComputerUtilCard.evaluateCreature(c) + diff < ComputerUtilCard.evaluateCreature(attacker) || randomTrade;
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

            // consider a double block
            for (final Card blocker : usableBlockers) {
                // Add an additional blocker if the current blockers are not
                // enough and the new one would deal the remaining damage
                final int currentDamage = ComputerUtilCombat.totalDamageOfBlockers(attacker, blockGang);
                final int additionalDamage = ComputerUtilCombat.dealsDamageAsBlocker(attacker, blocker);
                final int absorbedDamage2 = ComputerUtilCombat.getEnoughDamageToKill(blocker, attacker.getNetCombatDamage(), attacker, true);
                final int addedValue = ComputerUtilCard.evaluateCreature(blocker);
                final int damageNeeded = ComputerUtilCombat.getDamageToKill(attacker)
                        + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, false);
                if ((damageNeeded > currentDamage || CombatUtil.getMinNumBlockersForAttacker(attacker, ai) > blockGang.size())
                        && !(damageNeeded > currentDamage + additionalDamage)
                        // The attacker will be killed
                        && (absorbedDamage2 + absorbedDamage > attacker.getNetCombatDamage()
                        // only one blocker can be killed
                        || currentValue + addedValue - 50 <= evalAttackerValue
                        // or attacker is worth more
                        || (lifeInDanger && ComputerUtilCombat.lifeInDanger(ai, combat)))
                        // or life is in danger
                        && CombatUtil.canBlock(attacker, blocker, combat)) {
                    // this is needed for attackers that can't be blocked by more than 1
                    currentAttackers.remove(attacker);
                    combat.addBlocker(attacker, blocker);
                    if (CombatUtil.canBlock(attacker, leader, combat)) {
                        combat.addBlocker(attacker, leader);
                    }
                    foundDoubleBlock = true;
                    break;
                }
                if (!foundDoubleBlock && (currentDamage + additionalDamage >= damageNeeded)) {
                    // a double block was tested which resulted in a potential kill but it was dismissed,
                    // no need to test for a triple block then to avoid suboptimal plays.
                    considerTripleBlock = false;
                }
            }

            if (foundDoubleBlock || !considerTripleBlock) {
                continue;
            }

            // consider a triple block if a double block was not found
            blockerLoop:
            for (final Card secondBlocker : usableBlockers) {
                // consider the properties of the second blocker
                final int currentDamage = ComputerUtilCombat.totalDamageOfBlockers(attacker, blockGang);
                final int additionalDamage2 = ComputerUtilCombat.dealsDamageAsBlocker(attacker, secondBlocker);
                final int absorbedDamage2 = ComputerUtilCombat.getEnoughDamageToKill(secondBlocker, attacker.getNetCombatDamage(), attacker, true);
                final int addedValue2 = ComputerUtilCard.evaluateCreature(secondBlocker);
                final int damageNeeded = ComputerUtilCombat.getDamageToKill(attacker)
                        + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, secondBlocker, combat, false);

                List<Card> usableBlockersAsThird = new ArrayList<>(usableBlockers);
                usableBlockersAsThird.remove(secondBlocker);

                // loop over the remaining blockers in search of a good third blocker candidate
                for (Card thirdBlocker : usableBlockersAsThird) {
                    final int additionalDamage3 = ComputerUtilCombat.dealsDamageAsBlocker(attacker, thirdBlocker);
                    final int absorbedDamage3 = ComputerUtilCombat.getEnoughDamageToKill(thirdBlocker, attacker.getNetCombatDamage(), attacker, true);
                    final int addedValue3 = ComputerUtilCard.evaluateCreature(secondBlocker);
                    final int netCombatDamage = attacker.getNetCombatDamage();

                    if ((damageNeeded > currentDamage || CombatUtil.getMinNumBlockersForAttacker(attacker, ai) > blockGang.size())
                            && !(damageNeeded > currentDamage + additionalDamage2 + additionalDamage3)
                            // The attacker will be killed
                            && ((absorbedDamage2 + absorbedDamage > netCombatDamage && absorbedDamage3 + absorbedDamage > netCombatDamage
                            && absorbedDamage3 + absorbedDamage2 > netCombatDamage)
                            // only one blocker can be killed
                            || currentValue + addedValue2 + addedValue3 - 50 <= evalAttackerValue
                            // or attacker is worth more
                            || (thirdBlocker.isToken() && absorbedDamage2 + absorbedDamage > netCombatDamage)
                            // or third blocker is a token and no more than two blockers will die, one of which is the third blocker (token)
                            || (lifeInDanger && ComputerUtilCombat.lifeInDanger(ai, combat)))
                            // or life is in danger
                            && CombatUtil.canBlock(attacker, secondBlocker, combat)
                            && CombatUtil.canBlock(attacker, thirdBlocker, combat)) {
                        // this is needed for attackers that can't be blocked by more than 1
                        currentAttackers.remove(attacker);
                        combat.addBlocker(attacker, thirdBlocker);
                        if (CombatUtil.canBlock(attacker, secondBlocker, combat)) {
                            combat.addBlocker(attacker, secondBlocker);
                        }
                        if (CombatUtil.canBlock(attacker, leader, combat)) {
                            combat.addBlocker(attacker, leader);
                        }
                        break blockerLoop;
                    }
                }
            }
        }

        attackersLeft = (new ArrayList<>(currentAttackers));
    }

    private void makeGangNonLethalBlocks(final Combat combat) {
        List<Card> currentAttackers = new ArrayList<>(attackersLeft);
        List<Card> blockers;

        // Try to block a Menace attacker with two blockers, neither of which will die
        for (final Card attacker : attackersLeft) {
            if (!attacker.hasKeyword(Keyword.MENACE) && !attacker.hasStartOfKeyword("CantBeBlockedByAmount LT2")) {
                continue;
            }

            blockers = getPossibleBlockers(combat, attacker, blockersLeft, false);
            List<Card> usableBlockers;
            final List<Card> blockGang = new ArrayList<>();
            int absorbedDamage; // The amount of damage needed to kill the first blocker

            usableBlockers = CardLists.filter(blockers, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.getNetToughness() > attacker.getNetCombatDamage();
                }
            });
            if (usableBlockers.size() < 2) {
                return;
            }

            final Card leader = ComputerUtilCard.getWorstCreatureAI(usableBlockers);
            blockGang.add(leader);
            usableBlockers.remove(leader);
            absorbedDamage = ComputerUtilCombat.getEnoughDamageToKill(leader, attacker.getNetCombatDamage(), attacker, true);

            // consider a double block
            for (final Card blocker : usableBlockers) {
                final int absorbedDamage2 = ComputerUtilCombat.getEnoughDamageToKill(blocker, attacker.getNetCombatDamage(), attacker, true);
                // only do it if neither blocking creature will die
                if (absorbedDamage > attacker.getNetCombatDamage() && absorbedDamage2 > attacker.getNetCombatDamage()) {
                    currentAttackers.remove(attacker);
                    combat.addBlocker(attacker, blocker);
                    if (CombatUtil.canBlock(attacker, leader, combat)) {
                        combat.addBlocker(attacker, leader);
                    }
                    break;
                }
            }
        }

        attackersLeft = (new ArrayList<>(currentAttackers));
    }

    // Bad Trade Blocks (should only be made if life is in danger)
    // Random Trade Blocks (performed randomly if enabled in profile and only when in favorable conditions)
    /**
     * <p>
     * makeTradeBlocks.
     * </p>
     *
     * @param combat a {@link forge.game.combat.Combat} object.
     */
    private void makeTradeBlocks(final Combat combat) {

        List<Card> currentAttackers = new ArrayList<>(attackersLeft);
        List<Card> killingBlockers;

        for (final Card attacker : attackersLeft) {

            if (attacker.hasStartOfKeyword("CantBeBlockedByAmount LT")
                    || attacker.hasKeyword(Keyword.MENACE)
                    || attacker.hasKeyword("CARDNAME can't be blocked unless all creatures defending player controls block it.")) {
                continue;
            }
            if (ComputerUtilCombat.attackerHasThreateningAfflict(attacker, ai)) {
                continue;
            }

            List<Card> possibleBlockers = getPossibleBlockers(combat, attacker, blockersLeft, true);
            killingBlockers = getKillingBlockers(combat, attacker, possibleBlockers);

            if (!killingBlockers.isEmpty()) {
                final Card blocker = ComputerUtilCard.getWorstCreatureAI(killingBlockers);
                boolean doTrade = false;

                if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
                    // Always trade when life in danger
                    doTrade = true;
                } else {
                    // Randomly trade creatures with lower power and [hopefully] worse abilities, if enabled in profile
                    doTrade = wouldLikeToRandomlyTrade(attacker, blocker, combat);
                }

                if (doTrade) {
                    combat.addBlocker(attacker, blocker);
                    currentAttackers.remove(attacker);
                }
            }
        }
        attackersLeft = (new ArrayList<>(currentAttackers));
    }

    // Chump Blocks (should only be made if life is in danger)
    private void makeChumpBlocks(final Combat combat) {

        List<Card> currentAttackers = new ArrayList<>(attackersLeft);

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

        if (attacker.hasStartOfKeyword("CantBeBlockedByAmount LT") 
            || attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
            || attacker.hasKeyword("CARDNAME can't be blocked unless all creatures defending player controls block it.")
            || attacker.hasKeyword(Keyword.MENACE)
            || ComputerUtilCombat.attackerHasThreateningAfflict(attacker, ai)) {
            attackers.remove(0);
            makeChumpBlocks(combat, attackers);
            return;
        }

        List<Card> chumpBlockers = getPossibleBlockers(combat, attacker, blockersLeft, true);
        if (!chumpBlockers.isEmpty()) {
            final Card blocker = ComputerUtilCard.getWorstCreatureAI(chumpBlockers);

            // check if it's better to block a creature with lower power and without trample
            if (attacker.hasKeyword(Keyword.TRAMPLE)) {
                final int damageAbsorbed = blocker.getLethalDamage();
                if (attacker.getNetCombatDamage() > damageAbsorbed) {
                    for (Card other : attackers) {
                        if (other.equals(attacker)) {
                            continue;
                        }
                        if (other.getNetCombatDamage() >= damageAbsorbed
                                && !other.hasKeyword(Keyword.TRAMPLE)
                                && !other.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                                && !ComputerUtilCombat.attackerHasThreateningAfflict(other, ai)
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

        List<Card> currentAttackers = new ArrayList<>(attackersLeft);

        for (final Card attacker : currentAttackers) {

            if (!attacker.hasStartOfKeyword("CantBeBlockedByAmount LT")
                    && !attacker.hasKeyword(Keyword.MENACE)
                    && !attacker.hasKeyword("CARDNAME can't be blocked unless all creatures defending player controls block it.")) {
                continue;
            }
            List<Card> possibleBlockers = getPossibleBlockers(combat, attacker, blockersLeft, true);
            if (!CombatUtil.canAttackerBeBlockedWithAmount(attacker, possibleBlockers.size(), combat)) {
                continue;
            }
            List<Card> usedBlockers = new ArrayList<>();
            for (Card blocker : possibleBlockers) {
                if (CombatUtil.canBlock(attacker, blocker, combat)) {
                    combat.addBlocker(attacker, blocker);
                    usedBlockers.add(blocker);
                    if (CombatUtil.canAttackerBeBlockedWithAmount(attacker, usedBlockers.size(), combat)) {
                        break;
                    }
                }
            }
            if (CombatUtil.canAttackerBeBlockedWithAmount(attacker, usedBlockers.size(), combat)) {
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

        List<Card> tramplingAttackers = CardLists.getKeyword(attackers, Keyword.TRAMPLE);
        tramplingAttackers = CardLists.filter(tramplingAttackers, Predicates.not(rampagesOrNeedsManyToBlock));

        // TODO - should check here for a "rampage-like" trigger that replaced the keyword:
        // "Whenever CARDNAME becomes blocked, it gets +1/+1 until end of turn for each creature blocking it."

        for (final Card attacker : tramplingAttackers) {

            if (((attacker.hasStartOfKeyword("CantBeBlockedByAmount LT") || attacker.hasKeyword(Keyword.MENACE)) && !combat.isBlocked(attacker))
                    || attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                    || attacker.hasKeyword("CARDNAME can't be blocked unless all creatures defending player controls block it.")) {
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
        // the keyword: "Whenever CARDNAME becomes blocked, it gets +1/+1 until end of turn for each creature blocking it."

        for (final Card attacker : targetAttackers) {
            blockers = getPossibleBlockers(combat, attacker, blockersLeft, false);
            blockers.removeAll(combat.getBlockers(attacker));

            // Don't add any blockers that won't kill the attacker because the damage would be prevented by a static effect
            blockers = CardLists.filter(blockers, new Predicate<Card>() {
                @Override
                public boolean apply(Card blocker) {
                    return !ComputerUtilCombat.isCombatDamagePrevented(blocker, attacker, blocker.getNetCombatDamage());
                }
            });

            // Try to use safe blockers first
            if (blockers.size() > 0) {
                safeBlockers = getSafeBlockers(combat, attacker, blockers);
                for (final Card blocker : safeBlockers) {
                    final int damageNeeded = ComputerUtilCombat.getDamageToKill(attacker)
                            + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, false);
                    // Add an additional blocker if the current blockers are not
                    // enough and the new one would deal additional damage
                    if (damageNeeded > ComputerUtilCombat.totalDamageOfBlockers(attacker, combat.getBlockers(attacker))
                            && ComputerUtilCombat.dealsDamageAsBlocker(attacker, blocker) > 0
                            && CombatUtil.canBlock(attacker, blocker, combat)) {
                        combat.addBlocker(attacker, blocker);
                    }
                    blockers.remove(blocker); // Don't check them again next
                }
            }
            // don't try to kill what can't be killed
            if (attacker.hasKeyword(Keyword.INDESTRUCTIBLE) || ComputerUtil.canRegenerate(ai, attacker)) {
                continue;
            }

            // Try to add blockers that could be destroyed, but are worth less than the attacker
            // Don't use blockers without First Strike or Double Strike if attacker has it
            if (ComputerUtilCombat.dealsFirstStrikeDamage(attacker, false, combat)) {
                safeBlockers = CardLists.getKeyword(blockers, Keyword.FIRST_STRIKE);
                safeBlockers.addAll(CardLists.getKeyword(blockers, Keyword.DOUBLE_STRIKE));
            } else {
                safeBlockers = new ArrayList<>(blockers);
            }

            for (final Card blocker : safeBlockers) {
                final int damageNeeded = ComputerUtilCombat.getDamageToKill(attacker)
                        + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, false);
                // Add an additional blocker if the current blockers are not
                // enough and the new one would deal the remaining damage
                final int currentDamage = ComputerUtilCombat.totalDamageOfBlockers(attacker, combat.getBlockers(attacker));
                final int additionalDamage = ComputerUtilCombat.dealsDamageAsBlocker(attacker, blocker);
                if (damageNeeded > currentDamage
                        && damageNeeded <= currentDamage + additionalDamage
                        && ComputerUtilCard.evaluateCreature(blocker) + diff < ComputerUtilCard.evaluateCreature(attacker)
                        && CombatUtil.canBlock(attacker, blocker, combat)
                        && !ComputerUtilCombat.canDestroyBlockerBeforeFirstStrike(blocker, attacker, false)) {
                    combat.addBlocker(attacker, blocker);
                    blockersLeft.remove(blocker);
                }
            }
        }
    }

    private void makeChumpBlocksToSavePW(Combat combat) {
        if (ComputerUtilCombat.lifeInDanger(ai, combat) || ai.getLife() <= ai.getStartingLife() / 5) {
            // most likely not worth trying to protect planeswalkers when at threateningly low life or in
            // dangerous combat which threatens lethal or severe damage to face
            return;
        }

        AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
        final int evalThresholdToken = aic.getIntProperty(AiProps.THRESHOLD_TOKEN_CHUMP_TO_SAVE_PLANESWALKER);
        final int evalThresholdNonToken = aic.getIntProperty(AiProps.THRESHOLD_TOKEN_CHUMP_TO_SAVE_PLANESWALKER);
        final boolean onlyIfLethal = aic.getBooleanProperty(AiProps.CHUMP_TO_SAVE_PLANESWALKER_ONLY_ON_LETHAL);

        if (evalThresholdToken > 0 || evalThresholdNonToken > 0) {
            // detect how much damage is threatened to each of the planeswalkers, see which ones would be
            // worth protecting according to the AI profile properties
            CardCollection threatenedPWs = new CardCollection();
            for (final Card attacker : attackers) {
                GameEntity def = combat.getDefenderByAttacker(attacker);
                if (def instanceof Card) {
                    if (!onlyIfLethal) {
                        threatenedPWs.add((Card) def);
                    } else {
                        int damageToPW = 0;
                        for (final Card pwatkr : combat.getAttackersOf(def)) {
                            if (!combat.isBlocked(pwatkr)) {
                                damageToPW += ComputerUtilCombat.predictDamageTo((Card) def, pwatkr.getNetCombatDamage(), pwatkr, true);
                            }
                        }
                        if ((!onlyIfLethal && damageToPW > 0) || damageToPW >= def.getCounters(CounterEnumType.LOYALTY)) {
                            threatenedPWs.add((Card) def);
                        }
                    }
                }
            }

            CardCollection pwsWithChumpBlocks = new CardCollection();
            CardCollection chosenChumpBlockers = new CardCollection();
            CardCollection chumpPWDefenders = CardLists.filter(new CardCollection(this.blockersLeft), new Predicate<Card>() {
                @Override
                public boolean apply(Card card) {
                    return ComputerUtilCard.evaluateCreature(card) <= (card.isToken() ? evalThresholdToken
                            : evalThresholdNonToken);
                }
            });
            CardLists.sortByPowerAsc(chumpPWDefenders);
            if (!chumpPWDefenders.isEmpty()) {
                for (final Card attacker : attackers) {
                    GameEntity def = combat.getDefenderByAttacker(attacker);
                    if (def instanceof Card && threatenedPWs.contains(def)) {
                        if (attacker.hasKeyword(Keyword.TRAMPLE)) {
                            // don't bother trying to chump a trampling creature
                            continue;
                        }
                        if (!combat.getBlockers(attacker).isEmpty()) {
                            // already blocked by something, no need to chump
                            continue;
                        }
                        Card blockerDecided = null;
                        for (final Card blocker : chumpPWDefenders) {
                            if (CombatUtil.canBlock(attacker, blocker, combat)) {
                                combat.addBlocker(attacker, blocker);
                                pwsWithChumpBlocks.add((Card) combat.getDefenderByAttacker(attacker));
                                chosenChumpBlockers.add(blocker);
                                blockerDecided = blocker;
                                blockersLeft.remove(blocker);
                                break;
                            }
                        }
                        chumpPWDefenders.remove(blockerDecided);
                    }
                }
                // check to see if we managed to cover all the blockers of the planeswalker; if not, bail
                for (final Card pw : pwsWithChumpBlocks) {
                    CardCollection pwAttackers = combat.getAttackersOf(pw);
                    CardCollection pwDefenders = new CardCollection();
                    boolean isFullyBlocked = true;
                    if (!pwAttackers.isEmpty()) {
                        int damageToPW = 0;
                        for (Card pwAtk : pwAttackers) {
                            if (!combat.getBlockers(pwAtk).isEmpty()) {
                                pwDefenders.addAll(combat.getBlockers(pwAtk));
                            } else {
                                isFullyBlocked = false;
                                damageToPW += ComputerUtilCombat.predictDamageTo(pw, pwAtk.getNetCombatDamage(), pwAtk, true);
                            }
                        }
                        if (!isFullyBlocked && damageToPW >= pw.getCounters(CounterEnumType.LOYALTY)) {
                            for (Card chump : pwDefenders) {
                                if (chosenChumpBlockers.contains(chump)) {
                                    combat.removeFromCombat(chump);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void clearBlockers(final Combat combat, final List<Card> possibleBlockers) {

        final List<Card> oldBlockers = combat.getAllBlockers();
        for (final Card blocker : oldBlockers) {
            if (blocker.getController() == ai) // don't touch other player's blockers
                combat.removeFromCombat(blocker);
        }

        attackersLeft = new ArrayList<>(attackers); // keeps track of all currently unblocked attackers
        blockersLeft = new ArrayList<>(possibleBlockers); // keeps track of all unassigned blockers
        blockedButUnkilled = new ArrayList<>(); // keeps track of all blocked attackers that currently wouldn't be destroyed
    }

    /** Assigns blockers for the provided combat instance (in favor of player passes to ctor) */
    public void assignBlockersForCombat(final Combat combat) {
        List<Card> possibleBlockers = ai.getCreaturesInPlay();
        attackers = sortPotentialAttackers(combat);
        assignBlockers(combat, possibleBlockers);
    }

    /**
     * assignBlockersForCombat() with additional and possibly "virtual" blockers.
     * @param combat combat instance
     * @param blockers blockers to add in addition to creatures already in play
     */
    public void assignAdditionalBlockers(final Combat combat, CardCollectionView blockers) {
        List<Card> possibleBlockers = ai.getCreaturesInPlay();
        for (Card c : blockers) {
            if (!possibleBlockers.contains(c)) {
                possibleBlockers.add(c);
            }
        }
        attackers = sortPotentialAttackers(combat);
        assignBlockers(combat, possibleBlockers);
    }

    /**
     * assignBlockersForCombat() with specific and possibly "virtual" attackers. No other creatures, even if
     * they have already been declared in the combat instance, will be considered.
     * @param combat combat instance
     * @param givenAttackers specific attackers to consider
     */
    public void assignBlockersGivenAttackers(final Combat combat, List<Card> givenAttackers) {
        List<Card> possibleBlockers = ai.getCreaturesInPlay();
        attackers = givenAttackers;
        assignBlockers(combat, possibleBlockers);
    }

    /**
     * Core blocker assignment algorithm.
     * @param combat combat instance
     * @param possibleBlockers list of blockers to be considered
     */
    private void assignBlockers(final Combat combat, List<Card> possibleBlockers) {
        if (attackers.isEmpty()) {
            return;
        }

        clearBlockers(combat, possibleBlockers);

        List<Card> blockers;
        List<Card> chumpBlockers;

        diff = (ai.getLife() * 2) - 5; // This is the minimal gain for an unnecessary trade
        if (ai.getController().isAI() && diff > 0 && ((PlayerControllerAi) ai.getController()).getAi().getBooleanProperty(AiProps.PLAY_AGGRO)) {
            diff = 0;
        }

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

        // When the AI holds some Fog effect, don't bother about lifeInDanger
        if (!ComputerUtil.hasAFogEffect(ai)) {
            lifeInDanger = ComputerUtilCombat.lifeInDanger(ai, combat);
            makeTradeBlocks(combat); // choose necessary trade blocks

            // if life is still in danger
            if (lifeInDanger && ComputerUtilCombat.lifeInDanger(ai, combat)) {
                makeChumpBlocks(combat); // choose necessary chump blocks
            } else {
                lifeInDanger = false;
            }
            // Reinforce blockers blocking attackers with trample if life is still in danger
            if (lifeInDanger && ComputerUtilCombat.lifeInDanger(ai, combat)) {
                reinforceBlockersAgainstTrample(combat);
            } else {
                lifeInDanger = false;
            }
            // Support blockers not destroying the attacker with more blockers
            // to try to kill the attacker
            if (!lifeInDanger) {
                reinforceBlockersToKill(combat);
            }

            // == 2. If the AI life would still be in danger make a safer approach ==
            if (lifeInDanger && ComputerUtilCombat.lifeInDanger(ai, combat)) {
                clearBlockers(combat, possibleBlockers); // reset every block assignment
                makeTradeBlocks(combat); // choose necessary trade blocks
                // if life is in danger
                makeGoodBlocks(combat);
                // choose necessary chump blocks if life is still in danger
                if (ComputerUtilCombat.lifeInDanger(ai, combat)) {
                    makeChumpBlocks(combat);
                } else {
                    lifeInDanger = false;
                }
                // Reinforce blockers blocking attackers with trample if life is still in danger
                if (lifeInDanger && ComputerUtilCombat.lifeInDanger(ai, combat)) {
                    reinforceBlockersAgainstTrample(combat);
                } else {
                    lifeInDanger = false;
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
                // Reinforce blockers blocking attackers with trample if life is still in danger
                else {
                    reinforceBlockersAgainstTrample(combat);
                }
                makeGangBlocks(combat);
                // Support blockers not destroying the attacker with more
                // blockers to try to kill the attacker
                reinforceBlockersToKill(combat);
            }
        }

        // assign blockers that have to block
        chumpBlockers = CardLists.getKeyword(blockersLeft, "CARDNAME blocks each turn if able.");
        chumpBlockers.addAll(CardLists.getKeyword(blockersLeft, "CARDNAME blocks each combat if able."));
        // if an attacker with lure attacks - all that can block
        for (final Card blocker : blockersLeft) {
            if (CombatUtil.mustBlockAnAttacker(blocker, combat, null)) {
                chumpBlockers.add(blocker);
            }
        }
        if (!chumpBlockers.isEmpty()) {
            CardLists.shuffle(attackers);
            for (final Card attacker : attackers) {
                blockers = getPossibleBlockers(combat, attacker, chumpBlockers, false);
                for (final Card blocker : blockers) {
                    if (CombatUtil.canBlock(attacker, blocker, combat) && blockersLeft.contains(blocker)
                            && (CombatUtil.mustBlockAnAttacker(blocker, combat, null)
                                    || blocker.hasKeyword("CARDNAME blocks each turn if able.")
                                    || blocker.hasKeyword("CARDNAME blocks each combat if able."))) {
                        combat.addBlocker(attacker, blocker);
                        if (blocker.getMustBlockCards() != null) {
                            int mustBlockAmt = blocker.getMustBlockCards().size();
                            final CardCollectionView blockedSoFar = combat.getAttackersBlockedBy(blocker);
                            boolean canBlockAnother = CombatUtil.canBlockMoreCreatures(blocker, blockedSoFar);
                            if (!canBlockAnother || mustBlockAmt == blockedSoFar.size()) {
                                blockersLeft.remove(blocker);
                            }
                        } else {
                            blockersLeft.remove(blocker);
                        }
                    }
                }
            }
        }

        // check to see if it's possible to defend a Planeswalker under attack with a chump block,
        // unless life is low enough to be more worried about saving preserving the life total
        if (ai.getController().isAI() && !ComputerUtilCombat.lifeInDanger(ai, combat)) {
            makeChumpBlocksToSavePW(combat);
        }

        // if there are still blockers left, see if it's possible to block Menace creatures with
        // non-lethal blockers that won't kill the attacker but won't die to it as well
        makeGangNonLethalBlocks(combat);

        //Check for validity of blocks in case something slipped through
        for (Card attacker : attackers) {
            if (!CombatUtil.canAttackerBeBlockedWithAmount(attacker, combat.getBlockers(attacker).size(), combat)) {
                for (final Card blocker : combat.getBlockers(attacker)) {
                    if (blocker.getController() == ai) // don't touch other player's blockers
                        combat.removeFromCombat(blocker);
                }
            }
        }
    }

    public static CardCollection orderBlockers(Card attacker, CardCollection blockers) {
        // ordering of blockers, sort by evaluate, then try to kill the best
        int damage = attacker.getNetCombatDamage();
        ComputerUtilCard.sortByEvaluateCreature(blockers);
        final CardCollection first = new CardCollection();
        final CardCollection last = new CardCollection();
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

    /**
     * Orders a blocker that put onto the battlefield blocking. Depends heavily
     * on the implementation of orderBlockers().
     */
    public static CardCollection orderBlocker(final Card attacker, final Card blocker,
            final CardCollection oldBlockers) {
        // add blocker to existing ordering
        // sort by evaluate, then insert it appropriately
        // relies on current implementation of orderBlockers()
        final CardCollection allBlockers = new CardCollection(oldBlockers);
        allBlockers.add(blocker);
        ComputerUtilCard.sortByEvaluateCreature(allBlockers);
        final int newBlockerIndex = allBlockers.indexOf(blocker);

        int damage = attacker.getNetCombatDamage();

        final CardCollection result = new CardCollection();
        boolean newBlockerIsAdded = false;
        // The new blocker comes right after this one
        final Card newBlockerRightAfter = (newBlockerIndex == 0 ? null : allBlockers.get(newBlockerIndex - 1));
        if (newBlockerRightAfter == null
                && damage >= ComputerUtilCombat.getEnoughDamageToKill(blocker, damage, attacker, true)) {
            result.add(blocker);
            newBlockerIsAdded = true;
        }
        // Don't bother to keep damage up-to-date after the new blocker is
        // added, as we can't modify the order of the other cards anyway
        for (final Card c : oldBlockers) {
            final int lethal = ComputerUtilCombat.getEnoughDamageToKill(c, damage, attacker, true);
            damage -= lethal;
            result.add(c);
            if (!newBlockerIsAdded && c == newBlockerRightAfter
                    && damage <= ComputerUtilCombat.getEnoughDamageToKill(blocker, damage, attacker, true)) {
                // If blocker is right after this card in priority and we have
                // sufficient damage to kill it, add it here
                result.add(blocker);
                newBlockerIsAdded = true;
            }
        }
        // We don't have sufficient damage, just add it at the end!
        if (!newBlockerIsAdded) {
            result.add(blocker);
        }

        return result;
    }

    public static CardCollection orderAttackers(Card blocker, CardCollection attackers) {
        // This shouldn't really take trample into account, but otherwise should be pretty similar to orderBlockers
        // ordering of blockers, sort by evaluate, then try to kill the best
        int damage = blocker.getNetCombatDamage();
        ComputerUtilCard.sortByEvaluateCreature(attackers);
        final CardCollection first = new CardCollection();
        final CardCollection last = new CardCollection();
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

    private boolean wouldLikeToRandomlyTrade(Card attacker, Card blocker, Combat combat) {
        // Determines if the AI would like to randomly trade its blocker for the attacker in given combat
        boolean enableRandomTrades = false;
        boolean randomTradeIfBehindOnBoard = false;
        boolean randomTradeIfCreatInHand = false;
        int chanceModForEmbalm = 0;
        int chanceToTradeToSaveWalker = 0;
        int chanceToTradeDownToSaveWalker = 0;
        int minRandomTradeChance = 0;
        int maxRandomTradeChance = 0;
        int maxCreatDiff = 0;
        int maxCreatDiffWithRepl = 0;
        int aiCreatureCount = 0;
        int oppCreatureCount = 0;
        if (ai.getController().isAI()) {
            AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
            enableRandomTrades = aic.getBooleanProperty(AiProps.ENABLE_RANDOM_FAVORABLE_TRADES_ON_BLOCK);
            randomTradeIfBehindOnBoard = aic.getBooleanProperty(AiProps.RANDOMLY_TRADE_EVEN_WHEN_HAVE_LESS_CREATS);
            randomTradeIfCreatInHand = aic.getBooleanProperty(AiProps.ALSO_TRADE_WHEN_HAVE_A_REPLACEMENT_CREAT);
            minRandomTradeChance = aic.getIntProperty(AiProps.MIN_CHANCE_TO_RANDOMLY_TRADE_ON_BLOCK);
            maxRandomTradeChance = aic.getIntProperty(AiProps.MAX_CHANCE_TO_RANDOMLY_TRADE_ON_BLOCK);
            chanceModForEmbalm = aic.getIntProperty(AiProps.CHANCE_DECREASE_TO_TRADE_VS_EMBALM);
            maxCreatDiff = aic.getIntProperty(AiProps.MAX_DIFF_IN_CREATURE_COUNT_TO_TRADE);
            maxCreatDiffWithRepl = aic.getIntProperty(AiProps.MAX_DIFF_IN_CREATURE_COUNT_TO_TRADE_WITH_REPL);
            chanceToTradeToSaveWalker = aic.getIntProperty(AiProps.CHANCE_TO_TRADE_TO_SAVE_PLANESWALKER);
            chanceToTradeDownToSaveWalker = aic.getIntProperty(AiProps.CHANCE_TO_TRADE_DOWN_TO_SAVE_PLANESWALKER);
        }

        if (!enableRandomTrades) {
            return false;
        }

        aiCreatureCount = ComputerUtil.countUsefulCreatures(ai);

        if (!attackersLeft.isEmpty()) {
            oppCreatureCount = ComputerUtil.countUsefulCreatures(attackersLeft.get(0).getController());
        }

        if (attacker != null && attacker.getOwner() != null)
            if (attacker.getOwner().equals(ai) && "6".equals(attacker.getSVar("SacMe"))) {
            // Temporarily controlled object - don't trade with it
            // TODO: find a more reliable way to figure out that control will be reestablished next turn
            return false;
        }

        int numSteps = ai.getStartingLife() - 5; // e.g. 15 steps between 5 life and 20 life
        float chanceStep = (maxRandomTradeChance - minRandomTradeChance) / numSteps;
        int chance = (int)Math.max(minRandomTradeChance, (maxRandomTradeChance - (Math.max(5, ai.getLife() - 5)) * chanceStep));
        if (chance > maxRandomTradeChance) {
            chance = maxRandomTradeChance;
        }

        int evalAtk = ComputerUtilCard.evaluateCreature(attacker, true, false);
        int evalBlk = ComputerUtilCard.evaluateCreature(blocker, true, false);
        boolean atkEmbalm = (attacker.hasStartOfKeyword("Embalm") || attacker.hasStartOfKeyword("Eternalize")) && !attacker.isToken();
        boolean blkEmbalm = (blocker.hasStartOfKeyword("Embalm") || blocker.hasStartOfKeyword("Eternalize")) && !blocker.isToken();

        if (atkEmbalm && !blkEmbalm) {
            // The opponent will eventually get his creature back, while the AI won't
            chance = Math.max(0, chance - chanceModForEmbalm);
        }

        if (blocker.isFaceDown() && blocker.getState(CardStateName.Original).getType().isCreature()) {
            // if the blocker is a face-down creature (e.g. cast via Morph, Manifest), evaluate it
            // in relation to the original state, not to the Morph state
            evalBlk = ComputerUtilCard.evaluateCreature(Card.fromPaperCard(blocker.getPaperCard(), ai), false, true);
        }
        int chanceToSavePW = chanceToTradeDownToSaveWalker > 0 && evalAtk + 1 < evalBlk ? chanceToTradeDownToSaveWalker : chanceToTradeToSaveWalker;
        boolean powerParityOrHigher = blocker.getNetPower() <= attacker.getNetPower();
        boolean creatureParityOrAllowedDiff = aiCreatureCount
                + (randomTradeIfBehindOnBoard ? maxCreatDiff : 0) >= oppCreatureCount;
        boolean wantToTradeWithCreatInHand = randomTradeIfCreatInHand
                && !CardLists.filter(ai.getCardsIn(ZoneType.Hand), CardPredicates.Presets.CREATURES).isEmpty()
                && aiCreatureCount + maxCreatDiffWithRepl >= oppCreatureCount;
        boolean wantToSavePlaneswalker = MyRandom.percentTrue(chanceToSavePW)
                && combat.getDefenderByAttacker(attacker) instanceof Card
                && ((Card) combat.getDefenderByAttacker(attacker)).isPlaneswalker();
        boolean wantToTradeDownToSavePW = chanceToTradeDownToSaveWalker > 0;

        return ((evalBlk <= evalAtk + 1) || (wantToSavePlaneswalker && wantToTradeDownToSavePW)) // "1" accounts for tapped.
                && powerParityOrHigher
                && (creatureParityOrAllowedDiff || wantToTradeWithCreatInHand)
                && (MyRandom.percentTrue(chance) || wantToSavePlaneswalker);
    }
}
