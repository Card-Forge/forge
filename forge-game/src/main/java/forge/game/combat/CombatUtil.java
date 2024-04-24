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
package forge.game.combat;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GlobalRuleChange;
import forge.game.ability.AbilityKey;
import forge.game.card.*;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerController.ManaPaymentPurpose;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityCantAttackBlock;
import forge.game.staticability.StaticAbilityMustBlock;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.maps.MapToAmount;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Static class containing utility methods related to combat.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class CombatUtil {

    public static FCollectionView<GameEntity> getAllPossibleDefenders(final Player playerWhoAttacks) {
        // Opponents, opposing planeswalkers, and any battle you don't protect
        final FCollection<GameEntity> defenders = new FCollection<>();
        for (final Player defender : playerWhoAttacks.getOpponents()) {
            defenders.add(defender);
            defenders.addAll(defender.getPlaneswalkersInPlay());
        }

        // Relevant battles (protected by the attacking player's opponents)
        final Game game = playerWhoAttacks.getGame();
        final CardCollection battles = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.BATTLES);
        for (Card battle : battles) {
            if (battle.getType().hasSubtype("Siege") && battle.getProtectingPlayer().isOpponentOf(playerWhoAttacks)) {
                defenders.add(battle);
            }
        }

        return defenders;
    }

    // ////////////////////////////////////
    // ////////// ATTACK METHODS //////////
    // ////////////////////////////////////

    public static boolean validateAttackers(final Combat combat) {
        final AttackConstraints constraints = combat.getAttackConstraints();
        final int myViolations = constraints.countViolations(combat.getAttackersAndDefenders());
        if (myViolations == -1) {
            return false;
        }
        final Pair<Map<Card, GameEntity>, Integer> bestAttack = constraints.getLegalAttackers();
        return myViolations <= bestAttack.getRight().intValue();
    }

    /**
     * Check if attacker could attack without violating any constraints.
     */
    public static boolean couldAttackButNotAttacking(Combat combat, final Card attacker) {
        // If the player didn't declare attackers, combat here will be null
        if (combat == null) {
            combat = new Combat(attacker.getController());
        } else if (combat.isAttacking(attacker)) {
            return false;
        }

        final AttackConstraints constraints = combat.getAttackConstraints();
        final Pair<Map<Card, GameEntity>, Integer> bestAttack = constraints.getLegalAttackers();
        final Map<Card, GameEntity> attackers = new HashMap<>(combat.getAttackersAndDefenders());
        final Game game = attacker.getGame();

        return Iterables.any(getAllPossibleDefenders(attacker.getController()), new Predicate<GameEntity>() {
            @Override
            public boolean apply(final GameEntity defender) {
                if (!canAttack(attacker, defender) || getAttackCost(game, attacker, defender) != null) {
                    return false;
                }
                attackers.put(attacker, defender);
                final int myViolations = constraints.countViolations(attackers);
                if (myViolations == -1) {
                    return false;
                }
                return myViolations <= bestAttack.getRight().intValue();
            }
        });
    }

    /**
     * <p>
     * Check whether a player should be given the chance to attack this combat.
     * </p>
     *
     * @param p
     *            a {@link Player}.
     * @return {@code true} if and only if the player controls any creatures and
     *         has any opponents or planeswalkers controlled by opponents to
     *         attack.
     */
    public static boolean canAttack(final Player p) {
        final CardCollection possibleAttackers = getPossibleAttackers(p);
        return !possibleAttackers.isEmpty();
    }

    /**
     * Obtain a {@link CardCollection} of all creatures a {@link Player}
     * controls that could attack any possible defending {@link GameEntity}.
     * Note that this only performs primitive checks (see
     * {@link #canAttack(Card)}).
     *
     * @param p
     *            the attacking {@link Player}.
     * @return a {@link CardCollection}.
     */
    public static CardCollection getPossibleAttackers(final Player p) {
        return CardLists.filter(p.getCreaturesInPlay(), new Predicate<Card>() {
            @Override
            public boolean apply(final Card attacker) {
                return canAttack(attacker);
            }
        });
    }

    /**
     * Check whether a {@link Card} can attack any {@link GameEntity} that's legal for its controller to attack.
     * @param attacker
     *            the attacking {@link Card}.
     * @return a boolean.
     * @see #canAttack(Card, GameEntity)
     */
    public static boolean canAttack(final Card attacker) {
        return Iterables.any(getAllPossibleDefenders(attacker.getController()), new Predicate<GameEntity>() {
            @Override
            public boolean apply(final GameEntity defender) {
                return canAttack(attacker, defender);
            }
        });
    }

    /**
     * <p>
     * Check whether a {@link Card} is affected by any <i>attacking
     * restrictions</i>. This is <b>not</b> the case if all of the following are
     * true:
     * <ul>
     * <li>It's a creature.</li>
     * <li>It's untapped.</li>
     * <li>It's not phased out.</li>
     * <li>It's not summoning sick.</li>
     * <li>It has no abilities or keywords that prevent it from attacking.</li>
     * <li>It is not affected by any static abilities that prevent it from
     * attacking.</li>
     * </ul>
     * </p>
     * <p>
     * This method doesn't check effects related to other creatures attacking
     * </p>
     * <p>
     * Note that a creature affected by any attacking restrictions may never be
     * declared as an attacker.
     * </p>
     *
     * @param attacker
     *            the attacking {@link Card}.
     * @param defender
     *            the defending {@link GameEntity}.
     * @return a boolean.
     */
    public static boolean canAttack(final Card attacker, final GameEntity defender) {
        return canAttack(attacker, defender, false);
    }
    public static boolean canAttackNextTurn(final Card attacker, final GameEntity defender) {
        return canAttack(attacker, defender, true);
    }

    private static boolean canAttack(final Card attacker, final GameEntity defender, final boolean forNextTurn) {
        final Game game = attacker.getGame();

        // Basic checks (unless is for next turn)
        if (!forNextTurn && (
                   !attacker.isCreature()
                || attacker.isTapped() || attacker.isPhasedOut()
                || isAttackerSick(attacker, defender)
                || game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS))) {
            return false;
        }

        // Goad logic
        // a goaded creature does need to attack a player which does not goaded her
        // or if not possible a planeswalker or a player which does goaded her
        if (attacker.isGoaded()) {
            final boolean goadedByDefender = defender instanceof Player && attacker.isGoadedBy((Player) defender);
            // attacker got goaded by defender or defender is not player
            if (goadedByDefender || !(defender instanceof Player)) {
                for (GameEntity ge : getAllPossibleDefenders(attacker.getController())) {
                    if (!ge.equals(defender) && ge instanceof Player) {
                        // found a player which does not goad that creature
                        // and creature can attack this player or planeswalker
                        if (!attacker.isGoadedBy((Player) ge) && !ge.hasKeyword("Creatures your opponents control attack a player other than you if able.") && canAttack(attacker, ge)) {
                            return false;
                        }
                    }
                }
            }
        }

        // Quasi-goad logic for "Kardur, Doomscourge" etc. that isn't goad but behaves the same
        if (defender != null && defender.hasKeyword("Creatures your opponents control attack a player other than you if able.")) {
            for (GameEntity ge : getAllPossibleDefenders(attacker.getController())) {
                if (!ge.equals(defender) && ge instanceof Player) {
                    if (!ge.hasKeyword("Creatures your opponents control attack a player other than you if able.") && canAttack(attacker, ge)) {
                        return false;
                    }
                }
            }
        }

        // CantAttack static abilities
        if (StaticAbilityCantAttackBlock.cantAttack(attacker, defender)) {
            return false;
        }

        return true;
    }

    public static boolean isAttackerSick(final Card attacker, final GameEntity defender) {
        return !StaticAbilityCantAttackBlock.canAttackHaste(attacker, defender);
    }

    /**
     * <p>
     * checkPropagandaEffects.
     * </p>
     *
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     */
    public static boolean checkPropagandaEffects(final Game game, final Card attacker, final Combat combat, final List<Card> attackersWithOptionalCost) {
        final Cost attackCost = getAttackCost(game, attacker,  combat.getDefenderByAttacker(attacker), attackersWithOptionalCost);
        if (attackCost == null) {
            return true;
        }

        // Not a great solution, but prevents a crash by passing a fake SA for Propaganda payments
        // If there's a better way of handling this somewhere deeper in the code, feel free to remove
        final SpellAbility fakeSA = new SpellAbility.EmptySa(attacker, attacker.getController());
        fakeSA.setCardState(attacker.getCurrentState());
        // need to set this for "CostContainsX" restriction
        fakeSA.setPayCosts(attackCost);
        // prevent recalculating X
        fakeSA.setSVar("X", "0");
        return attacker.getController().getController().payManaOptional(attacker, attackCost, fakeSA,
                "Pay additional cost to declare " + attacker + " an attacker", ManaPaymentPurpose.DeclareAttacker);
    }

    public static Cost getAttackCost(final Game game, final Card attacker, final GameEntity defender) {
        return getAttackCost(game, attacker, defender, ImmutableList.of());
    }
    /**
     * Get the cost that has to be paid for a creature to attack a certain
     * defender.
     *
     * @param game
     *            the {@link Game}.
     * @param attacker
     *            the attacking creature.
     * @param defender
     *            the defending {@link GameEntity}.
     * @return the {@link Cost} of attacking, or {@code null} if there is no
     *         cost.
     */
    public static Cost getAttackCost(final Game game, final Card attacker, final GameEntity defender, final List<Card> attackersWithOptionalCost) {
        final Cost attackCost = new Cost(ManaCost.ZERO, true);
        boolean hasCost = false;
        // Sort abilities to apply them in proper order
        for (final Card card : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : card.getStaticAbilities()) {
                final Cost additionalCost = stAb.getAttackCost(attacker, defender, attackersWithOptionalCost);
                if (null != additionalCost) {
                    attackCost.add(additionalCost);
                    hasCost = true;
                }
            }
        }

        if (!hasCost) {
            return null;
        }
        return attackCost;
    }

    public static CardCollection getOptionalAttackCostCreatures(final CardCollection attackers, Class<? extends CostPart> costType) {
        final CardCollection attackersWithCost = new CardCollection();
        for (final Card card : attackers) {
            for (final StaticAbility stAb : card.getStaticAbilities()) {
                if (stAb.hasAttackCost(card, costType)) {
                    attackersWithCost.add(card);
                }
            }
        }

        return attackersWithCost;
    }

    public static boolean payRequiredBlockCosts(Game game, Card blocker, Card attacker) {
        Cost blockCost = getBlockCost(game, blocker, attacker);
        if (blockCost == null) {
            return true;
        }

        SpellAbility fakeSA = new SpellAbility.EmptySa(blocker, blocker.getController());
        fakeSA.setCardState(blocker.getCurrentState());
        fakeSA.setPayCosts(blockCost);
        fakeSA.setSVar("X", "0");
        return blocker.getController().getController().payManaOptional(blocker, blockCost, fakeSA, "Pay cost to declare " + blocker + " a blocker. ", ManaPaymentPurpose.DeclareBlocker);
    }

    public static Cost getBlockCost(Game game, Card blocker, Card attacker) {
        Cost blockCost = new Cost(ManaCost.ZERO, true);
        // Sort abilities to apply them in proper order
        boolean noCost = true;
        for (Card card : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : card.getStaticAbilities()) {
                Cost c1 = stAb.getBlockCost(blocker, attacker);
                if (c1 != null) {
                    blockCost.add(c1);
                    noCost = false;
                }
            }
        }

        if (noCost) {
            return null;
        }
        return blockCost;
    }

    /**
     * <p>
     * This method checks triggered effects of attacking creatures, right before
     * defending player declares blockers.
     * </p>
     * @param game
     *
     * @param c
     *            a {@link forge.game.card.Card} object.
     */
    public static void checkDeclaredAttacker(final Game game, final Card c, final Combat combat, boolean triggers) {
        final GameEntity defender = combat.getDefenderByAttacker(c);
        final List<Card> otherAttackers = combat.getAttackers();

        // Run triggers
        if (triggers) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Attacker, c);
            otherAttackers.remove(c);
            runParams.put(AbilityKey.OtherAttackers, otherAttackers);
            runParams.put(AbilityKey.Attacked, defender);
            runParams.put(AbilityKey.DefendingPlayer, combat.getDefenderPlayerByAttacker(c));
            // only add defenders that were attacked
            final FCollection<GameEntity> defenders = new FCollection<>();
            for (GameEntity e : combat.getDefenders()) {
                if (!combat.getAttackersOf(e).isEmpty()) {
                    defenders.add(e);
                }
            }
            runParams.put(AbilityKey.Defenders, defenders);
            game.getTriggerHandler().runTrigger(TriggerType.Attacks, runParams, false);
        }

        c.getDamageHistory().setCreatureAttackedThisCombat(defender, otherAttackers.size());
        c.getDamageHistory().clearNotAttackedSinceLastUpkeepOf();
        c.getController().addCreaturesAttackedThisTurn(CardCopyService.getLKICopy(c), defender);
    }

    /**
     * Create a {@link Map} mapping each possible attacker for the attacking
     * {@link Player} this {@link Combat} (see
     * {@link #getPossibleAttackers(Player)}) to a {@link MapToAmount}. This map
     * then maps each {@link GameEntity}, for which an attack requirement
     * exists, to the number of requirements on attacking that entity. Absent
     * entries, including an empty map, indicate no requirements exist.
     *
     * @param combat
     *            a {@link Combat}.
     * @return a {@link Map}.
     */
    public static AttackConstraints getAllRequirements(final Combat combat) {
        return new AttackConstraints(combat);
    }

    // ///////////////////////////////////
    // ////////// BLOCK METHODS //////////
    // ///////////////////////////////////

    // can the creature block given the combat state?
    /**
     * <p>
     * canBlock.
     * </p>
     *
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a boolean.
     */
    public static boolean canBlock(final Card blocker, final Combat combat) {
        if (blocker == null) {
            return false;
        }
        if (combat == null) {
            return canBlock(blocker);
        }

        if (!canBlockMoreCreatures(blocker, combat.getAttackersBlockedBy(blocker))) {
            return false;
        }
        final Game game = blocker.getGame();
        final int blockers = combat.getAllBlockers().size();

        if (blockers > 1 && game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.onlyTwoBlockers)) {
            return false;
        }

        if (blockers > 0 && game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.onlyOneBlocker)) {
            return false;
        }

        CardCollection allOtherBlockers = combat.getAllBlockers();
        allOtherBlockers.remove(blocker);
        final int blockersFromOnePlayer = CardLists.count(allOtherBlockers, CardPredicates.isController(blocker.getController()));
        if (blockersFromOnePlayer > 0 && game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.onlyOneBlockerPerOpponent)) {
            return false;
        }

        return canBlock(blocker);
    }

    // can the creature block at all?
    /**
     * <p>
     * canBlock.
     * </p>
     *
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean canBlock(final Card blocker) {
        return canBlock(blocker, false);
    }

    // can the creature block at all?
    /**
     * <p>
     * canBlock.
     * </p>
     *
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean canBlock(final Card blocker, final boolean nextTurn) {
        if (blocker == null) {
            return false;
        }

        if (!nextTurn && blocker.isTapped() && !blocker.hasKeyword("CARDNAME can block as though it were untapped.")) {
            return false;
        }

        if (blocker.hasKeyword("CARDNAME can't block.") || blocker.hasKeyword("CARDNAME can't attack or block.")
                || blocker.isPhasedOut()) {
            return false;
        }

        boolean cantBlockAlone = blocker.hasKeyword("CARDNAME can't attack or block alone.") || blocker.hasKeyword("CARDNAME can't block alone.");

        final List<Card> list = blocker.getController().getCreaturesInPlay();
        return list.size() >= 2 || !cantBlockAlone;
    }

    public static boolean canBlockMoreCreatures(final Card blocker, final CardCollectionView blockedBy) {
        if (blockedBy.isEmpty() || blocker.canBlockAny()) {
            return true;
        }
        int canBlockMore = blocker.canBlockAdditional();
        return canBlockMore >= blockedBy.size();
    }

    // can the attacker be blocked at all?
    /**
     * <p>
     * canBeBlocked.
     * </p>
     *
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a boolean.
     */
    public static boolean canBeBlocked(final Card attacker, final Combat combat, Player defendingPlayer) {
        if (attacker == null) {
            return true;
        }

        if (combat != null) {
            if (StaticAbilityCantAttackBlock.getMinMaxBlocker(attacker, defendingPlayer).getRight() == combat.getBlockers(attacker).size()) {
                return false;
            }

            // Rule 802.4a: A player can block only creatures attacking him/her or a planeswalker he/she controls
            Player attacked = combat.getDefendingPlayerRelatedTo(attacker);
            if (attacked != null && attacked != defendingPlayer) {
                return false;
            }
        }

        // Unblockable check
        if (StaticAbilityCantAttackBlock.cantBlockBy(attacker, null)) {
            return false;
        }

        return true;
    }

    /**
     * canBlockAtLeastOne.
     *
     * @param blocker
     *            the blocker
     * @param attackers
     *            the attackers
     * @return true, if one can be blocked
     */
    public static boolean canBlockAtLeastOne(final Card blocker, final Iterable<Card> attackers) {
        for (Card attacker : attackers) {
            if (canBlock(attacker, blocker)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Can be blocked.
     *
     * @param attacker
     *            the attacker
     * @param blockers
     *            the blockers
     * @return true, if successful
     */
    public static boolean canBeBlocked(final Card attacker, final List<Card> blockers, final Combat combat) {
        int blocks = 0;
        for (final Card blocker : blockers) {
            if (canBlock(attacker, blocker)) {
                blocks++;
            }
        }

        return canAttackerBeBlockedWithAmount(attacker, blocks, combat);
    }

    public static List<Card> getPotentialBestBlockers(final Card attacker, final List<Card> blockers, final Combat combat) {
        List<Card> potentialBlockers = Lists.newArrayList();
        if (blockers.isEmpty() || attacker == null) {
            return potentialBlockers;
        }

        for (final Card blocker : blockers) {
            if (canBlock(attacker, blocker)) {
                potentialBlockers.add(blocker);
            }
        }

        int minBlockers = getMinNumBlockersForAttacker(attacker, blockers.get(0).getController());

        CardLists.sortByPowerDesc(potentialBlockers);

        List<Card> minBlockerList = Lists.newArrayList();
        for (int i = 0; i < minBlockers && i < potentialBlockers.size(); i++) {
            minBlockerList.add(potentialBlockers.get(i));
        }

        return minBlockerList;
    }

    // return all creatures that could help satisfy a blocking requirement without breaking another
    // TODO according to 509.1c, this should really check if the maximum possible is already fulfilled
    public static List<Card> findFreeBlockers(List<Card> defendersArmy, Combat combat) {
        final CardCollection freeBlockers = new CardCollection();
        for (Card blocker : defendersArmy) {
            if (canBlock(blocker) && !mustBlockAnAttacker(blocker, combat, null)) {
                CardCollection blockedAttackers = combat.getAttackersBlockedBy(blocker);
                boolean blockChange = blockedAttackers.isEmpty();
                for (Card attacker : blockedAttackers) {
                    // check if we could unblock something
                    List<Card> blockersReduced = combat.getBlockers(attacker);
                    blockersReduced.remove(blocker);
                    if (canBlockMoreCreatures(blocker, blockedAttackers) || canBeBlocked(attacker, blockersReduced, combat)) {
                        blockChange = true;
                        break;
                    }
                }
                if (blockChange) {
                    freeBlockers.add(blocker);
                }
            }
        }
        return freeBlockers;
    }

    // Has the human player chosen all mandatory blocks?
    /**
     * <p>
     * finishedMandatotyBlocks.
     * </p>
     *
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a boolean.
     */
    public static String validateBlocks(final Combat combat, final Player defending) {
        final List<Card> defendersArmy = defending.getCreaturesInPlay();
        final List<Card> attackers = combat.getAttackers();
        final List<Card> blockers = CardLists.filterControlledBy(combat.getAllBlockers(), defending);
        final List<Card> freeBlockers = findFreeBlockers(defendersArmy, combat);

        // if a creature does not block but should, return false
        for (final Card blocker : defendersArmy) {
            if (!blocker.getMustBlockCards().isEmpty()) {
                final CardCollectionView blockedSoFar = combat.getAttackersBlockedBy(blocker);
                for (Card cardToBeBlocked : blocker.getMustBlockCards()) {
                    // If a creature can't block unless a player pays a cost, that player is not required to pay that cost
                    if (getBlockCost(blocker.getGame(), blocker, cardToBeBlocked) != null) {
                        continue;
                    }

                    int additionalBlockers = getMinNumBlockersForAttacker(cardToBeBlocked, defending) -1;
                    int potentialBlockers = 0;
                    // if the attacker can only be blocked with multiple creatures check if that's possible
                    for (int i = 0; i < additionalBlockers; i++) {
                        for (Card freeBlocker: new CardCollection(freeBlockers)) {
                            if (freeBlocker != blocker && canBlock(cardToBeBlocked, freeBlocker)) {
                                freeBlockers.remove(freeBlocker);
                                potentialBlockers++;
                            }
                        }
                    }
                    if (potentialBlockers >= additionalBlockers && !blockedSoFar.contains(cardToBeBlocked)
                            && (canBlockMoreCreatures(blocker, blockedSoFar) || freeBlockers.contains(blocker))
                            && combat.isAttacking(cardToBeBlocked) && canBlock(cardToBeBlocked, blocker)) {
                        return TextUtil.concatWithSpace(blocker.toString(), "must still block", TextUtil.addSuffix(cardToBeBlocked.toString(),"."));
                    }
                }
            }
            // lure effects
            if (mustBlockAnAttacker(blocker, combat, freeBlockers)) {
                return TextUtil.concatWithSpace(blocker.toString(), "must block an attacker, but has not been assigned to block", blockers.contains(blocker) ? "the right ones." : "any.");
            }

            // "CARDNAME blocks each turn/combat if able."
            if (!blockers.contains(blocker) && StaticAbilityMustBlock.blocksEachCombatIfAble(blocker)) {
                for (final Card attacker : attackers) {
                    if (getBlockCost(blocker.getGame(), blocker, attacker) != null) {
                        continue;
                    }

                    if (canBlock(attacker, blocker, combat)) {
                        boolean must = true;
                        if (getMinNumBlockersForAttacker(attacker, defending) > 1) {
                            final List<Card> possibleBlockers = Lists.newArrayList(freeBlockers);
                            possibleBlockers.addAll(combat.getBlockers(attacker));
                            if (!canBeBlocked(attacker, possibleBlockers, combat)) {
                                must = false;
                            }
                        }
                        if (must) {
                            return TextUtil.concatWithSpace(blocker.toString(), "must block each combat but was not assigned to block any attacker now.");
                        }
                    }
                }
            }
        }

        // Creatures that aren't allowed to block unless certain restrictions are met.
        for (final Card blocker : blockers) {
            boolean cantBlockAlone = blocker.hasKeyword("CARDNAME can't attack or block alone.") || blocker.hasKeyword("CARDNAME can't block alone.");
            if (blockers.size() < 2 && cantBlockAlone) {
                return TextUtil.concatWithSpace(blocker.toString(), "can't block alone.");
            } else if (blockers.size() < 3 && blocker.hasKeyword("CARDNAME can't block unless at least two other creatures block.")) {
                return TextUtil.concatWithSpace(blocker.toString(), "can't block unless at least two other creatures block.");
            } else if (blocker.hasKeyword("CARDNAME can't block unless a creature with greater power also blocks.")) {
                boolean found = false;
                int power = blocker.getNetPower();
                // Note: This is O(n^2), but there shouldn't generally be many creatures with the above keyword.
                for (Card blocker2 : blockers) {
                    if (blocker2.getNetPower() > power) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return TextUtil.concatWithSpace(blocker.toString(), "can't block unless a creature with greater power also blocks.");
                }
            }
        }

        for (final Card attacker : attackers) {
            int cntBlockers = combat.getBlockers(attacker).size();
            // don't accept blocker amount for attackers with keyword defining valid blockers amount
            if (cntBlockers > 0 && !canAttackerBeBlockedWithAmount(attacker, cntBlockers, combat))
                return TextUtil.concatWithSpace(attacker.toString(), "cannot be blocked with", String.valueOf(cntBlockers), "creatures you've assigned");
        }

        return null;
    }

    // can the blocker block an attacker with a lure effect?
    /**
     * <p>
     * mustBlockAnAttacker.
     * </p>
     *
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a boolean.
     */
    public static boolean mustBlockAnAttacker(final Card blocker, final Combat combat, final List<Card> freeBlockers) {
        if (blocker == null || combat == null) {
            return false;
        }

        final CardCollectionView attackers = combat.getAttackers();

        final CardCollection requirementCards = new CardCollection();
        final Player defender = blocker.getController();
        for (final Card attacker : attackers) {
            if (getBlockCost(blocker.getGame(), blocker, attacker) != null) {
                continue;
            }

            if (attackerLureSatisfied(attacker, blocker, combat.getBlockers(attacker))) {
                continue;
            }

            if (canBeBlocked(attacker, combat, defender) && canBlock(attacker, blocker)) {
                boolean canBe = true;
                Player defendingPlayer = combat.getDefenderPlayerByAttacker(attacker);

                if (getMinNumBlockersForAttacker(attacker, defendingPlayer) > 1) {
                    final List<Card> blockers = defendingPlayer.getCreaturesInPlay();
                    blockers.remove(blocker);
                    if (!canBeBlocked(attacker, blockers, combat)) {
                        canBe = false;
                    }
                }
                if (canBe) {
                    requirementCards.add(attacker);
                }
            }
        }

        for (final Card attacker : blocker.getMustBlockCards()) {
            if (getBlockCost(blocker.getGame(), blocker, attacker) != null) {
                continue;
            }

            if (canBeBlocked(attacker, combat, defender) && canBlock(attacker, blocker)
                    && combat.isAttacking(attacker)) {
                boolean canBe = true;
                Player defendingPlayer = combat.getDefenderPlayerByAttacker(attacker);

                if (getMinNumBlockersForAttacker(attacker, defendingPlayer) > 1) {
                    final List<Card> blockers = freeBlockers != null ? new CardCollection(freeBlockers) : defendingPlayer.getCreaturesInPlay();
                    blockers.remove(blocker);
                    if (!canBeBlocked(attacker, blockers, combat)) {
                        canBe = false;
                    }
                }
                if (canBe) {
                    requirementCards.add(attacker);
                }
            }
        }

        if (requirementCards.isEmpty()) {
            return false;
        }

        if (combat.getAttackersBlockedBy(blocker).containsAll(requirementCards)) {
            return false;
        }

        if (!canBlock(blocker, combat)) {
            // the blocker can't block more but is he even part of another requirement?
            for (final Card attacker : attackers) {
                final boolean requirementSatisfied = attackerLureSatisfied(attacker, blocker, combat.getBlockers(attacker));
                final CardCollection reducedBlockers = combat.getBlockers(attacker);
                if (requirementSatisfied && reducedBlockers.contains(blocker)) {
                    reducedBlockers.remove(blocker);
                    if (!attackerLureSatisfied(attacker, blocker, reducedBlockers)) {
                        return false;
                    }
                }
            }
        }

        return Collections.disjoint(combat.getAttackersBlockedBy(blocker), requirementCards);
    }

    private static boolean attackerLureSatisfied(final Card attacker, final Card blocker, final CardCollection blockers) {
        if (attacker.hasStartOfKeyword("All creatures able to block CARDNAME do so.")
                || (attacker.hasStartOfKeyword("CARDNAME must be blocked if able.")
                        && blockers.isEmpty())
                || (attacker.hasStartOfKeyword("CARDNAME must be blocked by exactly one creature if able.")
                        && blockers.size() != 1)
                || (attacker.hasStartOfKeyword("CARDNAME must be blocked by two or more creatures if able.")
                        && blockers.size() < 2)) {
            return false;
        }

        // TODO replace with Hidden Keyword or Static Ability
        for (KeywordInterface inst : attacker.getKeywords()) {
            String keyword = inst.getOriginal();
            // MustBeBlockedBy <valid>
            if (keyword.startsWith("MustBeBlockedBy ")) {
                final String valid = keyword.substring("MustBeBlockedBy ".length());
                if (blocker.isValid(valid, null, null, null) &&
                        CardLists.getValidCardCount(blockers, valid, null, null, null) == 0) {
                    return false;

                }
            }
            // MustBeBlockedByAll:<valid>
            if (keyword.startsWith("MustBeBlockedByAll")) {
                final String valid = keyword.split(":")[1];
                if (blocker.isValid(valid, null, null, null)) {
                    return false;
                }
            }
        }

        return true;
    }

    // can a player block with one or more creatures at the moment?
    /**
     * <p>
     * canAttack.
     * </p>
     *
     * @param p
     *            a {@link forge.game.player} object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a boolean.
     */
    public static boolean canBlock(Player p, Combat combat) {
        List<Card> creatures = p.getCreaturesInPlay();
        if (creatures.isEmpty()) { return false; }

        List<Card> attackers = combat.getAttackers();
        if (attackers.isEmpty()) { return false; }

        for (Card c : creatures) {
            for (Card a : attackers) {
                if (canBlock(a, c, combat)) {
                    return true;
                }
            }
        }
        return false;
    }

    // can the blocker block the attacker given the combat state?
    /**
     * <p>
     * canBlock.
     * </p>
     *
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a boolean.
     */
    public static boolean canBlock(final Card attacker, final Card blocker, final Combat combat) {
        if (attacker == null || blocker == null) {
            return false;
        }

        if (!canBlock(blocker, combat)) {
            return false;
        }
        if (!canBeBlocked(attacker, combat, blocker.getController())) {
            return false;
        }
        if (combat != null && combat.isBlocking(blocker, attacker)) { // Can't block if already blocking the attacker
            return false;
        }

        // TODO remove with HiddenKeyword or Static Ability
        boolean mustBeBlockedBy = false;
        for (KeywordInterface inst : attacker.getKeywords()) {
            String keyword = inst.getOriginal();
            // MustBeBlockedBy <valid>
            if (keyword.startsWith("MustBeBlockedBy ")) {
                final String valid = keyword.substring("MustBeBlockedBy ".length());
                if (blocker.isValid(valid, null, null, null) &&
                        CardLists.getValidCardCount(combat.getBlockers(attacker), valid, null, null, null) == 0) {
                    mustBeBlockedBy = true;
                    break;
                }
            }
            // MustBeBlockedByAll:<valid>
            if (keyword.startsWith("MustBeBlockedByAll")) {
                final String valid = keyword.split(":")[1];
                if (blocker.isValid(valid, null, null, null)) {
                    mustBeBlockedBy = true;
                    break;
                }
            }
        }

        // if the attacker has no lure effect, but the blocker can block another
        // attacker with lure, the blocker can't block the former
        if (!attacker.hasKeyword("All creatures able to block CARDNAME do so.")
                && !(attacker.hasKeyword("CARDNAME must be blocked if able.") && combat.getBlockers(attacker).isEmpty())
                && !(attacker.hasKeyword("CARDNAME must be blocked by exactly one creature if able.") && combat.getBlockers(attacker).size() != 1)
                && !(attacker.hasKeyword("CARDNAME must be blocked by two or more creatures if able.") && combat.getBlockers(attacker).size() < 2)
                && !blocker.getMustBlockCards().contains(attacker)
                && !mustBeBlockedBy
                && mustBlockAnAttacker(blocker, combat, null)) {
            return false;
        }

        return canBlock(attacker, blocker);
    }

    // can the blocker block the attacker?
    /**
     * <p>
     * canBlock.
     * </p>
     *
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean canBlock(final Card attacker, final Card blocker) {
        return canBlock(attacker, blocker, false);
    }

    // can the blocker block the attacker?
    /**
     * <p>
     * canBlock.
     * </p>
     *
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean canBlock(final Card attacker, final Card blocker, final boolean nextTurn) {
        if (attacker == null || blocker == null) {
            return false;
        }

        if (!canBlock(blocker, nextTurn)) {
            return false;
        }

        // rare case:
        if (blocker.hasKeyword(Keyword.SHADOW)
                && blocker.hasKeyword("CARDNAME can block creatures with shadow as though they didn't have shadow.")) {
            return false;
        }

        if (attacker.hasKeyword(Keyword.SHADOW) && !blocker.hasKeyword(Keyword.SHADOW)
                && !blocker.hasKeyword("CARDNAME can block creatures with shadow as though they didn't have shadow.")) {
            return false;
        }

        if (!attacker.hasKeyword(Keyword.SHADOW) && blocker.hasKeyword(Keyword.SHADOW)) {
            return false;
        }

        // CantBlockBy static abilities
        if (StaticAbilityCantAttackBlock.cantBlockBy(attacker, blocker)) {
            return false;
        }

        return true;
    }

    public static boolean canAttackerBeBlockedWithAmount(Card attacker, int amount, Combat combat) {
        return canAttackerBeBlockedWithAmount(attacker, amount, combat != null ? combat.getDefenderPlayerByAttacker(attacker) : null);
    }
    public static boolean canAttackerBeBlockedWithAmount(Card attacker, int amount, Player defender) {
        if (amount == 0)
            return false; // no block

        Pair<Integer, Integer> minMaxBlock = StaticAbilityCantAttackBlock.getMinMaxBlocker(attacker, defender);

        if (minMaxBlock.getLeft() > amount || minMaxBlock.getRight() < amount) {
            return false;
        }

        return true;
    }

    public static int getMinNumBlockersForAttacker(Card attacker, Player defender) {
        return StaticAbilityCantAttackBlock.getMinMaxBlocker(attacker, defender).getLeft();
    }
}
