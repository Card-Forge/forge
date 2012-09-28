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
package forge.game.phase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Predicate;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListUtil;
import forge.CardPredicates;
import forge.Command;
import forge.Constant;
import forge.Counters;
import forge.GameAction;
import forge.GameActionUtil;
import forge.GameEntity;
import forge.Singletons;
import forge.card.TriggerReplacementBase;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.AbilityFactorySacrifice;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.SpellAbility;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.card.trigger.TriggerType;
import forge.game.player.ComputerUtil;
import forge.game.player.ComputerUtilBlock;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;
import forge.gui.framework.EDocID;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.views.VCombat;


/**
 * <p>
 * CombatUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CombatUtil {

    // can the creature block given the combat state?
    /**
     * <p>
     * canBlock.
     * </p>
     * 
     * @param blocker
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean canBlock(final Card blocker, final Combat combat) {

        if (blocker == null) {
            return false;
        }
        if (combat == null) {
            return CombatUtil.canBlock(blocker);
        }
        
        if (!CombatUtil.canBlockMoreCreatures(blocker, combat.getAttackersBlockedBy(blocker))) {
            return false;
        }

        for (final Card c : AllZoneUtil.getCardsIn(ZoneType.Battlefield)) {
            for (final String keyword : c.getKeyword()) {
                if (keyword.equals("No more than one creature can block each combat.")
                        && (combat.getAllBlockers().size() > 0)) {
                    return false;
                }
                if (keyword.equals("No more than two creatures can block each combat.")
                        && (combat.getAllBlockers().size() > 1)) {
                    return false;
                }
            }
        }

        if (combat.getAllBlockers().size() > 0 && AllZoneUtil.isCardInPlay("Dueling Grounds")) {
            return false;
        }

        return CombatUtil.canBlock(blocker);
    }

    // can the creature block at all?
    /**
     * <p>
     * canBlock.
     * </p>
     * 
     * @param blocker
     *            a {@link forge.Card} object.
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
     *            a {@link forge.Card} object.
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

        final CardList list = AllZoneUtil.getCreaturesInPlay(blocker.getController());
        if (list.size() < 2 && blocker.hasKeyword("CARDNAME can't attack or block alone.")) {
            return false;
        }

        return true;
    }
    
    public static boolean canBlockMoreCreatures(final Card blocker, final CardList blockedBy) {
        // TODO(sol) expand this for the additional blocking keyword
        int size = blockedBy.size();
        
        if (size == 0 || blocker.hasKeyword("CARDNAME can block any number of creatures.")) {
            return true;
        }
        
        return blocker.getKeywordAmount("CARDNAME can block an additional creature.") >= size;
    }

    // can the attacker be blocked at all?
    /**
     * <p>
     * canBeBlocked.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean canBeBlocked(final Card attacker, final Combat combat) {

        if (attacker == null) {
            return true;
        }

        if (attacker.hasKeyword("CARDNAME can't be blocked by more than one creature.")
                && (combat.getBlockers(attacker).size() > 0)) {
            return false;
        }

        return CombatUtil.canBeBlocked(attacker);
    }

    // can the attacker be blocked at all?
    /**
     * <p>
     * canBeBlocked.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canBeBlocked(final Card attacker) {

        if (attacker == null) {
            return true;
        }

        if (attacker.hasKeyword("Unblockable")) {
            return false;
        }

        // Landwalk
        if (isUnblockableFromLandwalk(attacker)) {
            return false;
        }

        return true;
    }


    public static boolean isUnblockableFromLandwalk(final Card attacker) {
        //May be blocked as though it doesn't have landwalk. (Staff of the Ages)
        if (attacker.hasKeyword("May be blocked as though it doesn't have landwalk.")) {
            return false;
        }

        ArrayList<String> walkTypes = new ArrayList<String>();

        for (String basic : Constant.Color.BASIC_LANDS) {
            StringBuilder sbLand = new StringBuilder();
            sbLand.append(basic);
            sbLand.append("walk");
            String landwalk = sbLand.toString();

            StringBuilder sbSnow = new StringBuilder();
            sbSnow.append("Snow ");
            sbSnow.append(landwalk.toLowerCase());
            String snowwalk = sbSnow.toString();

            sbLand.insert(0, "May be blocked as though it doesn't have "); //Deadfall, etc.
            sbLand.append(".");

            String mayBeBlocked = sbLand.toString();

            if (attacker.hasKeyword(landwalk) && !attacker.hasKeyword(mayBeBlocked)) {
                walkTypes.add(basic);
            }

            if (attacker.hasKeyword(snowwalk)) {
                StringBuilder sbSnowType = new StringBuilder();
                sbSnowType.append(basic);
                sbSnowType.append(".Snow");
                walkTypes.add(sbSnowType.toString());
            }
        }

        for (String keyword : attacker.getKeyword()) {
            if (keyword.equals("Legendary landwalk")) {
                walkTypes.add("Land.Legendary");
            } else if (keyword.equals("Desertwalk")) {
                walkTypes.add("Desert");
            } else if (keyword.equals("Nonbasic landwalk")) {
                walkTypes.add("Land.nonBasic");
            } else if (keyword.equals("Snow landwalk")) {
                walkTypes.add("Land.Snow");
            }
        }

        if (walkTypes.isEmpty()) {
            return false;
        }

        String valid = StringUtils.join(walkTypes, ",");
        final Player defendingPlayer = attacker.getController().getOpponent();
        CardList defendingLands = defendingPlayer.getCardsIn(ZoneType.Battlefield);
        for (Card c : defendingLands) {
            if (c.isValid(valid, defendingPlayer, attacker)) {
                return true;
            }
        }

        return false;
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
    public static boolean canBlockAtLeastOne(final Card blocker, final CardList attackers) {
        for (Card attacker : attackers) {
            if (CombatUtil.canBlock(attacker, blocker)) {
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
    public static boolean canBeBlocked(final Card attacker, final CardList blockers) {
        if (!CombatUtil.canBeBlocked(attacker)) {
            return false;
        }

        if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")) {
            int blocks = 0;
            for (final Card blocker : blockers) {
                if (CombatUtil.canBlock(attacker, blocker)) {
                    blocks += 1;
                    if (blocks > 1) {
                        return true;
                    }
                }
            }
        } else {
            for (final Card blocker : blockers) {
                if (CombatUtil.canBlock(attacker, blocker)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>
     * needsMoreBlockers.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static int needsBlockers(final Card attacker) {

        if (attacker == null) {
            return 0;
        }

        if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")) {
            return 2;
        }

        return 1;
    }

    // Has the human player chosen all mandatory blocks?
    /**
     * <p>
     * finishedMandatotyBlocks.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean finishedMandatoryBlocks(final Combat combat) {

        final CardList blockers = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
        final CardList attackers = combat.getAttackerList();

        // if a creature does not block but should, return false
        for (final Card blocker : blockers) {
            // lure effects
            if (!combat.getAllBlockers().contains(blocker) && CombatUtil.mustBlockAnAttacker(blocker, combat)) {
                return false;
            }

            // "CARDNAME blocks each turn if able."
            if (!combat.getAllBlockers().contains(blocker) && blocker.hasKeyword("CARDNAME blocks each turn if able.")) {
                for (final Card attacker : attackers) {
                    if (CombatUtil.canBlock(attacker, blocker, combat)) {
                        boolean must = true;
                        if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")) {
                            final CardList possibleBlockers = combat.getDefendingPlayer().getCardsIn(ZoneType.Battlefield)
                                    .filter(CardPredicates.Presets.CREATURES);
                            possibleBlockers.remove(blocker);
                            if (!CombatUtil.canBeBlocked(attacker, possibleBlockers)) {
                                must = false;
                            }
                        }
                        if (must) {
                            return false;
                        }
                    }
                }
            }
        }

        for (final Card attacker : attackers) {
            // don't accept one blocker for attackers with this keyword
            if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")
                    && (combat.getBlockers(attacker).size() == 1)) {
                return false;
            }
        }

        return true;
    }
    
    public static void orderMultipleCombatants(final Combat combat) {
        CombatUtil.orderMultipleBlockers(combat);
        CombatUtil.orderBlockingMultipleAttackers(combat);
    }
    
    private static void orderMultipleBlockers(final Combat combat) {
        // If there are multiple blockers, the Attacker declares the Assignment Order
        final Player player = combat.getAttackingPlayer();
        final CardList attackers = combat.getAttackerList();
        for (final Card attacker : attackers) {
            CardList blockers = combat.getBlockers(attacker);
            if (blockers.size() <= 1) {
                continue;
            }
         
            CardList orderedBlockers = null;
            if (player.isHuman()) {
                List<Object> ordered = GuiUtils.getOrderChoices("Choose Blocking Order", "Damaged First", 0, blockers.toArray(), null);
                
                orderedBlockers = new CardList();
                for(Object o : ordered) {
                    orderedBlockers.add((Card)o);
                }
            }
            else {
                orderedBlockers = ComputerUtilBlock.orderBlockers(attacker, blockers);
            }
            combat.setBlockerList(attacker,  orderedBlockers);
        }
        CombatUtil.showCombat();
        // Refresh Combat Panel
    }
    
    private static void orderBlockingMultipleAttackers(final Combat combat) {
        // If there are multiple blockers, the Attacker declares the Assignment Order
        final Player player = combat.getDefendingPlayer();
        final CardList blockers = combat.getAllBlockers();
        for (final Card blocker : blockers) {
            CardList attackers = combat.getAttackersBlockedBy(blocker);
            if (attackers.size() <= 1) {
                continue;
            }
         
            CardList orderedAttacker = null;
            if (player.isHuman()) {
                List<Object> ordered = GuiUtils.getOrderChoices("Choose Blocking Order", "Damaged First", 0, attackers.toArray(), null);
                
                orderedAttacker = new CardList();
                for(Object o : ordered) {
                    orderedAttacker.add((Card)o);
                }
            }
            else {
                orderedAttacker = ComputerUtilBlock.orderAttackers(blocker, attackers);
            }
            combat.setAttackersBlockedByList(blocker,  orderedAttacker);
        }
        CombatUtil.showCombat();
        // Refresh Combat Panel
    }
    

    // can the blocker block an attacker with a lure effect?
    /**
     * <p>
     * mustBlockAnAttacker.
     * </p>
     * 
     * @param blocker
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean mustBlockAnAttacker(final Card blocker, final Combat combat) {

        if (blocker == null || combat == null) {
            return false;
        }

        if (!CombatUtil.canBlock(blocker, combat)) {
            return false;
        }

        final CardList attackers = combat.getAttackerList();
        final CardList attackersWithLure = new CardList();
        for (final Card attacker : attackers) {
            if (attacker.hasStartOfKeyword("All creatures able to block CARDNAME do so.")
                    || (attacker.hasStartOfKeyword("CARDNAME must be blocked if able.") && combat.getBlockers(attacker)
                            .isEmpty())) {
                attackersWithLure.add(attacker);
            }
        }

        for (final Card attacker : attackersWithLure) {
            if (CombatUtil.canBeBlocked(attacker, combat) && CombatUtil.canBlock(attacker, blocker)) {
                boolean canBe = true;
                if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")) {
                    final CardList blockers = combat.getDefendingPlayer().getCardsIn(ZoneType.Battlefield)
                            .filter(CardPredicates.Presets.CREATURES);
                    blockers.remove(blocker);
                    if (!CombatUtil.canBeBlocked(attacker, blockers)) {
                        canBe = false;
                    }
                }
                if (canBe) {
                    return true;
                }
            }
        }

        if (blocker.getMustBlockCards() != null) {
            for (final Card attacker : blocker.getMustBlockCards()) {
                if (CombatUtil.canBeBlocked(attacker, combat) && CombatUtil.canBlock(attacker, blocker)) {
                    boolean canBe = true;
                    if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")) {
                        final CardList blockers = combat.getDefendingPlayer().getCardsIn(ZoneType.Battlefield)
                                .filter(CardPredicates.Presets.CREATURES);
                        blockers.remove(blocker);
                        if (!CombatUtil.canBeBlocked(attacker, blockers)) {
                            canBe = false;
                        }
                    }
                    if (canBe) {
                        return true;
                    }
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
     *            a {@link forge.Card} object.
     * @param blocker
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean canBlock(final Card attacker, final Card blocker, final Combat combat) {

        if ((attacker == null) || (blocker == null)) {
            return false;
        }

        if (!CombatUtil.canBlock(blocker, combat)) {
            return false;
        }
        if (!CombatUtil.canBeBlocked(attacker, combat)) {
            return false;
        }

        // if the attacker has no lure effect, but the blocker can block another
        // attacker with lure, the blocker can't block the former
        if (!attacker.hasKeyword("All creatures able to block CARDNAME do so.")
                && !(attacker.hasKeyword("CARDNAME must be blocked if able.") && combat.getBlockers(attacker).isEmpty())
                && !(blocker.getMustBlockCards() != null && blocker.getMustBlockCards().contains(attacker))
                && CombatUtil.mustBlockAnAttacker(blocker, combat)) {
            return false;
        }

        return CombatUtil.canBlock(attacker, blocker);
    }
    
 // can the blocker block the attacker?
    /**
     * <p>
     * canBlock.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param blocker
     *            a {@link forge.Card} object.
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
     *            a {@link forge.Card} object.
     * @param blocker
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canBlock(final Card attacker, final Card blocker, final boolean nextTurn) {
        if ((attacker == null) || (blocker == null)) {
            return false;
        }

        if (!CombatUtil.canBlock(blocker, nextTurn)) {
            return false;
        }
        if (!CombatUtil.canBeBlocked(attacker)) {
            return false;
        }

        if (CardFactoryUtil.hasProtectionFrom(blocker, attacker)) {
            return false;
        }

        if (blocker.hasStartOfKeyword("CARDNAME can't block ")) {
            for (final String kw : blocker.getKeyword()) {
                if (kw.startsWith("CARDNAME can't block ")) {
                    final String unblockableCard = kw.substring(21);
                    final int id = Integer.parseInt(unblockableCard.substring(unblockableCard.lastIndexOf("(") + 1,
                            unblockableCard.length() - 1));
                    if (attacker.getUniqueNumber() == id) {
                        return false;
                    }
                }
            }
        }

        // rare case:
        if (blocker.hasKeyword("Shadow")
                && blocker.hasKeyword("CARDNAME can block creatures with shadow as though they didn't have shadow.")) {
            return false;
        }

        if (attacker.hasKeyword("Shadow") && !blocker.hasKeyword("Shadow")
                && !blocker.hasKeyword("CARDNAME can block creatures with shadow as though they didn't have shadow.")) {
            return false;
        }

        if (!attacker.hasKeyword("Shadow") && blocker.hasKeyword("Shadow")) {
            return false;
        }

        if (attacker.hasKeyword("Creatures with power less than CARDNAME's power can't block it.")
                && (attacker.getNetAttack() > blocker.getNetAttack())) {
            return false;
        }
        if ((blocker.getNetAttack() > attacker.getNetAttack())
                && blocker.hasKeyword("CARDNAME can't be blocked by creatures "
                        + "with power greater than CARDNAME's power.")) {
            return false;
        }
        if ((blocker.getNetAttack() >= attacker.getNetDefense())
                && blocker.hasKeyword("CARDNAME can't be blocked by creatures with "
                        + "power equal to or greater than CARDNAME's toughness.")) {
            return false;
        }

        if (attacker.hasStartOfKeyword("CantBeBlockedBy")) {
            final int keywordPosition = attacker.getKeywordPosition("CantBeBlockedBy");
            final String parse = attacker.getKeyword().get(keywordPosition).toString();
            final String[] k = parse.split(" ", 2);
            final String[] restrictions = k[1].split(",");
            if (blocker.isValid(restrictions, attacker.getController(), attacker)) {
                return false;
            }
        }

        if (blocker.hasStartOfKeyword("CantBlock")) {
            final int keywordPosition = blocker.getKeywordPosition("CantBlock");
            final String parse = blocker.getKeyword().get(keywordPosition).toString();
            final String[] k = parse.split(" ", 2);
            final String[] restrictions = k[1].split(",");
            if (attacker.isValid(restrictions, blocker.getController(), blocker)) {
                return false;
            }
        }

        if (attacker.hasKeyword("CARDNAME can't be blocked by black creatures.") && blocker.isBlack()) {
            return false;
        }
        if (attacker.hasKeyword("CARDNAME can't be blocked by blue creatures.") && blocker.isBlue()) {
            return false;
        }
        if (attacker.hasKeyword("CARDNAME can't be blocked by green creatures.") && blocker.isGreen()) {
            return false;
        }
        if (attacker.hasKeyword("CARDNAME can't be blocked by red creatures.") && blocker.isRed()) {
            return false;
        }
        if (attacker.hasKeyword("CARDNAME can't be blocked by white creatures.") && blocker.isWhite()) {
            return false;
        }

        if (blocker.hasKeyword("CARDNAME can block only creatures with flying.") && !attacker.hasKeyword("Flying")) {
            return false;
        }

        if (attacker.hasKeyword("Flying")
                || attacker.hasKeyword("CARDNAME can't be blocked except by creatures with flying or reach.")) {
            if (!blocker.hasKeyword("Flying") && !blocker.hasKeyword("Reach")) {
                return false;
            }
        }

        if (attacker.hasKeyword("CARDNAME can't be blocked except by creatures with defender.") && !blocker.hasKeyword("Defender")) {
            return false;
        }

        if (attacker.hasKeyword("Horsemanship")) {
            if (!blocker.hasKeyword("Horsemanship")) {
                return false;
            }
        }

        if (attacker.hasKeyword("Fear")) {
            if (!blocker.isArtifact() && !blocker.isBlack()) {
                return false;
            }
        }

        if (attacker.hasKeyword("Intimidate")) {
            if (!blocker.isArtifact() && !blocker.sharesColorWith(attacker)) {
                return false;
            }
        }

        if (attacker.hasKeyword("CARDNAME can't be blocked by Walls.") && blocker.isWall()) {
            return false;
        }

        if (attacker.hasKeyword("CARDNAME can't be blocked except by Walls.") && !blocker.isWall()) {
            return false;
        }

        if (attacker.hasKeyword("CARDNAME can't be blocked except by black creatures.") && !blocker.isBlack()) {
            return false;
        }

        if (AllZoneUtil.isCardInPlay("Shifting Sliver")) {
            if (attacker.isType("Sliver") && !blocker.isType("Sliver")) {
                return false;
            }
        }

        return true;
    } // canBlock()

    // can a creature attack given the combat state
    /**
     * <p>
     * canAttack.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean canAttack(final Card c, final Combat combat) {

        int cntAttackers = combat.getAttackers().size();
        for (final Card card : AllZoneUtil.getCardsIn(ZoneType.Battlefield)) {
            for (final String keyword : card.getKeyword()) {
                if (keyword.equals("No more than one creature can attack each combat.") && cntAttackers > 0) {
                    return false;
                }
                if (keyword.equals("No more than two creatures can attack each combat.") && cntAttackers > 1) {
                    return false;
                }
                if (keyword.equals("No more than two creatures can attack you each combat.") && cntAttackers > 1
                        && card.getController().getOpponent().isPlayer(c.getController())) {
                    return false;
                }
                if (keyword.equals("CARDNAME can only attack alone.") && combat.isAttacking(card)) {
                    return false;
                }
            }
        }

        final CardList list = AllZoneUtil.getCreaturesInPlay(c.getController());
        if (list.size() < 2 && c.hasKeyword("CARDNAME can't attack or block alone.")) {
            return false;
        }

        if (cntAttackers > 0 && c.hasKeyword("CARDNAME can only attack alone.")) {
            return false;
        }

        if (cntAttackers > 0 && AllZoneUtil.isCardInPlay("Dueling Grounds")) {
            return false;
        }

        return CombatUtil.canAttack(c);
    }

    // can a creature attack at the moment?
    /**
     * <p>
     * canAttack.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canAttack(final Card c) {
        if (c.isTapped() || c.isPhasedOut()
                || (c.hasSickness() && !c.hasKeyword("CARDNAME can attack as though it had haste."))
                || Singletons.getModel().getGameState().getPhaseHandler().getPhase()
                    .isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return false;
        }
        return CombatUtil.canAttackNextTurn(c);
    }

    // can a creature attack if untapped and without summoning sickness?
    /**
     * <p>
     * canAttackNextTurn.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canAttackNextTurn(final Card c) {
        if (!c.isCreature()) {
            return false;
        }

        // CARDNAME can't attack if defending player controls an untapped
        // creature with power ...
        final int[] powerLimit = { 0 };
        int keywordPosition = 0;
        boolean hasKeyword = false;

        final ArrayList<String> attackerKeywords = c.getKeyword();
        for (int i = 0; i < attackerKeywords.size(); i++) {
            if (attackerKeywords.get(i).toString()
                    .startsWith("CARDNAME can't attack if defending player controls an untapped creature with power")) {
                hasKeyword = true;
                keywordPosition = i;
            }
        }

        // The keyword
        // "CARDNAME can't attack if defending player controls an untapped creature with power"
        // ... is present
        if (hasKeyword) {
            final String tmpString = c.getKeyword().get(keywordPosition).toString();
            final String[] asSeparateWords = tmpString.trim().split(" ");

            if (asSeparateWords.length >= 15) {
                if (asSeparateWords[12].matches("[0-9][0-9]?")) {
                    powerLimit[0] = Integer.parseInt((asSeparateWords[12]).trim());

                    CardList list = AllZoneUtil.getCreaturesInPlay(c.getController().getOpponent());
                    list = list.filter(new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card ct) {
                            return ((ct.isUntapped() && (ct.getNetAttack() >= powerLimit[0]) && asSeparateWords[14]
                                    .contains("greater")) || (ct.isUntapped() && (ct.getNetAttack() <= powerLimit[0]) && asSeparateWords[14]
                                    .contains("less")));
                        }
                    });
                    if (!list.isEmpty()) {
                        return false;
                    }
                }
            }
        } // hasKeyword = CARDNAME can't attack if defending player controls an
          // untapped creature with power ...

        final CardList list = c.getController().getOpponent().getCardsIn(ZoneType.Battlefield);
        CardList temp;
        for (String keyword : c.getKeyword()) {
            if (keyword.equals("CARDNAME can't attack.") || keyword.equals("CARDNAME can't attack or block.")) {
                return false;
            } else if (keyword.equals("Defender") && !c.hasKeyword("CARDNAME can attack as though it didn't have defender.")) {
                return false;
            } else if (keyword.equals("CARDNAME can't attack unless defending player controls an Island.")) {
                temp = list.getType("Island");
                if (temp.isEmpty()) {
                    return false;
                }
            } else if (keyword.equals("CARDNAME can't attack unless defending player controls a Forest.")) {
                temp = list.getType("Forest");
                if (temp.isEmpty()) {
                    return false;
                }
            } else if (keyword.equals("CARDNAME can't attack unless defending player controls a Swamp.")) {
                temp = list.getType("Swamp");
                if (temp.isEmpty()) {
                    return false;
                }
            } else if (keyword.equals("CARDNAME can't attack unless defending player controls a Mountain.")) {
                temp = list.getType("Mountain");
                if (temp.isEmpty()) {
                    return false;
                }
            } else if (keyword.equals("CARDNAME can't attack unless defending player controls a snow land.")) {
                temp = list.filter(CardPredicates.Presets.SNOW_LANDS);
                if (temp.isEmpty()) {
                    return false;
                }
            } else if (keyword.equals("CARDNAME can't attack unless defending player controls a blue permanent.")) {
                temp = CardListUtil.getColor(list, Constant.Color.BLUE);
                if (temp.isEmpty()) {
                    return false;
                }
            }
        }

        // The creature won't untap next turn
        if (c.isTapped() && !Untap.canUntap(c)) {
            return false;
        }

        return true;
    } // canAttack()

    /**
     * <p>
     * getTotalFirstStrikeBlockPower.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int getTotalFirstStrikeBlockPower(final Card attacker, final Player player) {
        final Card att = attacker;

        CardList list = AllZoneUtil.getCreaturesInPlay(player);
        list = list.filter(new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return CombatUtil.canBlock(att, c) && (c.hasFirstStrike() || c.hasDoubleStrike());
            }
        });

        return CombatUtil.totalDamageOfBlockers(attacker, list);

    }

    // This function takes Doran and Double Strike into account
    /**
     * <p>
     * getAttack.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int getAttack(final Card c) {
        int n = c.getNetCombatDamage();

        if (c.hasDoubleStrike()) {
            n *= 2;
        }

        return n;
    }

    // Returns the damage an unblocked attacker would deal
    /**
     * <p>
     * damageIfUnblocked.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param attacked
     *            a {@link forge.game.player.Player} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a int.
     */
    public static int damageIfUnblocked(final Card attacker, final Player attacked, final Combat combat) {
        int damage = attacker.getNetCombatDamage();
        int sum = 0;
        damage += CombatUtil.predictPowerBonusOfAttacker(attacker, null, combat);
        if (!attacker.hasKeyword("Infect")) {
            sum = attacked.predictDamage(damage, attacker, true);
            if (attacker.hasKeyword("Double Strike")) {
                sum += attacked.predictDamage(damage, attacker, true);
            }
        }
        return sum;
    }

    // Returns the poison an unblocked attacker would deal
    /**
     * <p>
     * poisonIfUnblocked.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param attacked
     *            a {@link forge.game.player.Player} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a int.
     */
    public static int poisonIfUnblocked(final Card attacker, final Player attacked, final Combat combat) {
        int damage = attacker.getNetCombatDamage();
        int poison = 0;
        damage += CombatUtil.predictPowerBonusOfAttacker(attacker, null, null);
        if (attacker.hasKeyword("Infect")) {
            poison += attacked.predictDamage(damage, attacker, true);
            if (attacker.hasKeyword("Double Strike")) {
                poison += attacked.predictDamage(damage, attacker, true);
            }
        }
        if (attacker.hasKeyword("Poisonous") && (damage > 0)) {
            poison += attacker.getKeywordMagnitude("Poisonous");
        }
        return poison;
    }

    // Returns the damage unblocked attackers would deal
    /**
     * <p>
     * sumDamageIfUnblocked.
     * </p>
     * 
     * @param attackers
     *            a {@link forge.CardList} object.
     * @param attacked
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int sumDamageIfUnblocked(final CardList attackers, final Player attacked) {
        int sum = 0;
        for (final Card attacker : attackers) {
            sum += CombatUtil.damageIfUnblocked(attacker, attacked, null);
        }
        return sum;
    }

    // Returns the number of poison counters unblocked attackers would deal
    /**
     * <p>
     * sumPoisonIfUnblocked.
     * </p>
     * 
     * @param attackers
     *            a {@link forge.CardList} object.
     * @param attacked
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int sumPoisonIfUnblocked(final CardList attackers, final Player attacked) {
        int sum = 0;
        for (final Card attacker : attackers) {
            sum += CombatUtil.poisonIfUnblocked(attacker, attacked, null);
        }
        return sum;
    }

    // calculates the amount of life that will remain after the attack
    /**
     * <p>
     * lifeThatWouldRemain.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a int.
     */
    public static int lifeThatWouldRemain(final Combat combat) {

        int damage = 0;

        final CardList attackers = combat.getAttackersByDefenderSlot(0);
        final CardList unblocked = new CardList();

        for (final Card attacker : attackers) {

            final CardList blockers = combat.getBlockers(attacker);

            if ((blockers.size() == 0)
                    || attacker.hasKeyword("You may have CARDNAME assign its combat damage "
                            + "as though it weren't blocked.")) {
                unblocked.add(attacker);
            } else if (attacker.hasKeyword("Trample")
                    && (CombatUtil.getAttack(attacker) > CombatUtil.totalShieldDamage(attacker, blockers))) {
                if (!attacker.hasKeyword("Infect")) {
                    damage += CombatUtil.getAttack(attacker) - CombatUtil.totalShieldDamage(attacker, blockers);
                }
            }
        }

        damage += CombatUtil.sumDamageIfUnblocked(unblocked, AllZone.getComputerPlayer());

        if (!AllZone.getComputerPlayer().canLoseLife()) {
            damage = 0;
        }

        return AllZone.getComputerPlayer().getLife() - damage;
    }

    // calculates the amount of poison counters after the attack
    /**
     * <p>
     * resultingPoison.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a int.
     */
    public static int resultingPoison(final Combat combat) {

        int poison = 0;

        final CardList attackers = combat.getAttackersByDefenderSlot(0);
        final CardList unblocked = new CardList();

        for (final Card attacker : attackers) {

            final CardList blockers = combat.getBlockers(attacker);

            if ((blockers.size() == 0)
                    || attacker.hasKeyword("You may have CARDNAME assign its combat damage"
                            + " as though it weren't blocked.")) {
                unblocked.add(attacker);
            } else if (attacker.hasKeyword("Trample")
                    && (CombatUtil.getAttack(attacker) > CombatUtil.totalShieldDamage(attacker, blockers))) {
                if (attacker.hasKeyword("Infect")) {
                    poison += CombatUtil.getAttack(attacker) - CombatUtil.totalShieldDamage(attacker, blockers);
                }
                if (attacker.hasKeyword("Poisonous")) {
                    poison += attacker.getKeywordMagnitude("Poisonous");
                }
            }
        }

        poison += CombatUtil.sumPoisonIfUnblocked(unblocked, AllZone.getComputerPlayer());

        return AllZone.getComputerPlayer().getPoisonCounters() + poison;
    }

    // Checks if the life of the attacked Player/Planeswalker is in danger
    /**
     * <p>
     * lifeInDanger.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean lifeInDanger(final Combat combat) {
        // life in danger only cares about the player's life. Not about a
        // Planeswalkers life
        if (AllZone.getComputerPlayer().cantLose()) {
            return false;
        }

        // check for creatures that must be blocked
        final CardList attackers = combat.getAttackersByDefenderSlot(0);

        for (final Card attacker : attackers) {

            final CardList blockers = combat.getBlockers(attacker);

            if (blockers.size() == 0) {
                if (!attacker.getSVar("MustBeBlocked").equals("")) {
                    return true;
                }
            }
        }

        if ((CombatUtil.lifeThatWouldRemain(combat) < Math.min(4, AllZone.getComputerPlayer().getLife()))
                && !AllZone.getComputerPlayer().cantLoseForZeroOrLessLife()) {
            return true;
        }

        return (CombatUtil.resultingPoison(combat) > Math.max(7, AllZone.getComputerPlayer().getPoisonCounters()));
    }

    // Checks if the life of the attacked Player would be reduced
    /**
     * <p>
     * wouldLoseLife.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean wouldLoseLife(final Combat combat) {

        return (CombatUtil.lifeThatWouldRemain(combat) < AllZone.getComputerPlayer().getLife());
    }

    // Checks if the life of the attacked Player/Planeswalker is in danger
    /**
     * <p>
     * lifeInSeriousDanger.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean lifeInSeriousDanger(final Combat combat) {
        // life in danger only cares about the player's life. Not about a
        // Planeswalkers life
        if (AllZone.getComputerPlayer().cantLose()) {
            return false;
        }

        // check for creatures that must be blocked
        final CardList attackers = combat.getAttackersByDefenderSlot(0);

        for (final Card attacker : attackers) {

            final CardList blockers = combat.getBlockers(attacker);

            if (blockers.size() == 0) {
                if (!attacker.getSVar("MustBeBlocked").equals("")) {
                    return true;
                }
            }
        }

        if ((CombatUtil.lifeThatWouldRemain(combat) < 1) && !AllZone.getComputerPlayer().cantLoseForZeroOrLessLife()) {
            return true;
        }

        return (CombatUtil.resultingPoison(combat) > 9);
    }

    // This calculates the amount of damage a blockgang can deal to the attacker
    // (first strike not supported)
    /**
     * <p>
     * totalDamageOfBlockers.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defenders
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int totalDamageOfBlockers(final Card attacker, final CardList defenders) {
        int damage = 0;

        for (final Card defender : defenders) {
            damage += CombatUtil.dealsDamageAsBlocker(attacker, defender);
        }
        return damage;
    }

    // This calculates the amount of damage a blocker in a blockgang can deal to
    // the attacker
    /**
     * <p>
     * dealsDamageAsBlocker.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int dealsDamageAsBlocker(final Card attacker, final Card defender) {

        if (attacker.getName().equals("Sylvan Basilisk") && !defender.hasKeyword("Indestructible")) {
            return 0;
        }

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetDefense()) {
                return 0;
            }
            if ((flankingMagnitude >= (defender.getNetDefense() - defender.getDamage()))
                    && !defender.hasKeyword("Indestructible")) {
                return 0;
            }

        } // flanking
        if (attacker.hasKeyword("Indestructible") && !(defender.hasKeyword("Wither") || defender.hasKeyword("Infect"))) {
            return 0;
        }

        int defenderDamage = defender.getNetAttack() + CombatUtil.predictPowerBonusOfBlocker(attacker, defender, true);
        if (AllZoneUtil.isCardInPlay("Doran, the Siege Tower")) {
            defenderDamage = defender.getNetDefense() + CombatUtil.predictToughnessBonusOfBlocker(attacker, defender, true);
        }

        // consider static Damage Prevention
        defenderDamage = attacker.predictDamage(defenderDamage, defender, true);

        if (defender.hasKeyword("Double Strike")) {
            defenderDamage += attacker.predictDamage(defenderDamage, defender, true);
        }

        return defenderDamage;
    }

    // This calculates the amount of damage a blocker in a blockgang can take
    // from the attacker (for trampling attackers)
    /**
     * <p>
     * totalShieldDamage.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defenders
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int totalShieldDamage(final Card attacker, final CardList defenders) {

        int defenderDefense = 0;

        for (final Card defender : defenders) {
            defenderDefense += CombatUtil.shieldDamage(attacker, defender);
        }

        return defenderDefense;
    }

    // This calculates the amount of damage a blocker in a blockgang can take
    // from the attacker (for trampling attackers)
    /**
     * <p>
     * shieldDamage.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int shieldDamage(final Card attacker, final Card defender) {

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetDefense()) {
                return 0;
            }
            if ((flankingMagnitude >= (defender.getNetDefense() - defender.getDamage()))
                    && !defender.hasKeyword("Indestructible")) {
                return 0;
            }

        } // flanking

        final int defBushidoMagnitude = defender.getKeywordMagnitude("Bushido");

        final int defenderDefense = (defender.getLethalDamage() - flankingMagnitude) + defBushidoMagnitude;

        return defenderDefense;
    } // shieldDamage

    // For AI safety measures like Regeneration
    /**
     * <p>
     * combatantWouldBeDestroyed.
     * </p>
     * 
     * @param combatant
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean combatantWouldBeDestroyed(final Card combatant) {

        if (combatant.isAttacking()) {
            return CombatUtil.attackerWouldBeDestroyed(combatant);
        }
        if (combatant.isBlocking()) {
            return CombatUtil.blockerWouldBeDestroyed(combatant);
        }
        return false;
    }

    // For AI safety measures like Regeneration
    /**
     * <p>
     * attackerWouldBeDestroyed.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean attackerWouldBeDestroyed(final Card attacker) {
        final CardList blockers = AllZone.getCombat().getBlockers(attacker);

        for (final Card defender : blockers) {
            if (CombatUtil.canDestroyAttacker(attacker, defender, AllZone.getCombat(), true)
                    && !(defender.hasKeyword("Wither") || defender.hasKeyword("Infect"))) {
                return true;
            }
        }

        return CombatUtil.totalDamageOfBlockers(attacker, blockers) >= attacker.getKillDamage();
    }

    // Will this trigger trigger?
    /**
     * <p>
     * combatTriggerWillTrigger.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @param trigger
     *            a {@link forge.card.trigger.Trigger} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean combatTriggerWillTrigger(final Card attacker, final Card defender, final Trigger trigger,
            Combat combat) {
        final HashMap<String, String> trigParams = trigger.getMapParams();
        boolean willTrigger = false;
        final Card source = trigger.getHostCard();
        if (combat == null) {
            combat = AllZone.getCombat();
        }

        if (!trigger.zonesCheck(AllZone.getZoneOf(trigger.getHostCard()))) {
            return false;
        }
        if (!trigger.requirementsCheck()) {
            return false;
        }

        TriggerType mode = trigger.getMode();
        if (mode == TriggerType.Attacks) {
            willTrigger = true;
            if (attacker.isAttacking()) {
                return false; // The trigger should have triggered already
            }
            if (trigParams.containsKey("ValidCard")) {
                if (!TriggerReplacementBase.matchesValid(attacker, trigParams.get("ValidCard").split(","), source)
                        && !(combat.isAttacking(source) && TriggerReplacementBase.matchesValid(source,
                                trigParams.get("ValidCard").split(","), source)
                            && !trigParams.containsKey("Alone"))) {
                    return false;
                }
            }
        }

        // defender == null means unblocked
        if ((defender == null) && mode == TriggerType.AttackerUnblocked) {
            willTrigger = true;
            if (trigParams.containsKey("ValidCard")) {
                if (!TriggerReplacementBase.matchesValid(attacker, trigParams.get("ValidCard").split(","), source)) {
                    return false;
                }
            }
        }

        if (defender == null) {
            return willTrigger;
        }

        if (mode == TriggerType.Blocks) {
            willTrigger = true;
            if (trigParams.containsKey("ValidBlocked")) {
                if (!TriggerReplacementBase.matchesValid(attacker, trigParams.get("ValidBlocked").split(","), source)) {
                    return false;
                }
            }
            if (trigParams.containsKey("ValidCard")) {
                if (!TriggerReplacementBase.matchesValid(defender, trigParams.get("ValidCard").split(","), source)) {
                    return false;
                }
            }
        } else if (mode == TriggerType.AttackerBlocked) {
            willTrigger = true;
            if (trigParams.containsKey("ValidBlocker")) {
                if (!TriggerReplacementBase.matchesValid(defender, trigParams.get("ValidBlocker").split(","), source)) {
                    return false;
                }
            }
            if (trigParams.containsKey("ValidCard")) {
                if (!TriggerReplacementBase.matchesValid(attacker, trigParams.get("ValidCard").split(","), source)) {
                    return false;
                }
            }
        } else if (mode == TriggerType.DamageDone) {
            willTrigger = true;
            if (trigParams.containsKey("ValidSource")) {
                if (TriggerReplacementBase.matchesValid(defender, trigParams.get("ValidSource").split(","), source)
                        && defender.getNetCombatDamage() > 0
                        && (!trigParams.containsKey("ValidTarget")
                                || TriggerReplacementBase.matchesValid(attacker, trigParams.get("ValidTarget").split(","), source))) {
                    return true;
                }
                if (TriggerReplacementBase.matchesValid(attacker, trigParams.get("ValidSource").split(","), source)
                        && attacker.getNetCombatDamage() > 0
                        && (!trigParams.containsKey("ValidTarget")
                                || TriggerReplacementBase.matchesValid(defender, trigParams.get("ValidTarget").split(","), source))) {
                    return true;
                }
            }
            return false;
        }

        return willTrigger;
    }

    // Predict the Power bonus of the blocker if blocking the attacker
    // (Flanking, Bushido and other triggered abilities)
    /**
     * <p>
     * predictPowerBonusOfBlocker.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int predictPowerBonusOfBlocker(final Card attacker, final Card defender, boolean withoutAbilities) {
        int power = 0;

        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {
            power -= attacker.getAmountOfKeyword("Flanking");
        }

        // if the attacker has first strike and wither the blocker will deal
        // less damage than expected
        if ((attacker.hasKeyword("First Strike") || attacker.hasKeyword("Double Strike"))
                && (attacker.hasKeyword("Wither") || attacker.hasKeyword("Infect"))
                && !(defender.hasKeyword("First Strike") || defender.hasKeyword("Double Strike") || defender
                        .hasKeyword("CARDNAME can't have counters placed on it."))) {
            power -= attacker.getNetCombatDamage();
        }

        power += defender.getKeywordMagnitude("Bushido");

        // look out for continuous static abilities that only care for blocking
        // creatures
        final CardList cardList = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        for (final Card card : cardList) {
            for (final StaticAbility stAb : card.getStaticAbilities()) {
                final HashMap<String, String> params = stAb.getMapParams();
                if (!params.get("Mode").equals("Continuous")) {
                    continue;
                }
                if (!params.containsKey("Affected") || !params.get("Affected").contains("blocking")) {
                    continue;
                }
                final String valid = params.get("Affected").replace("blocking", "Creature");
                if (!defender.isValid(valid, card.getController(), card)) {
                    continue;
                }
                if (params.containsKey("AddPower")) {
                    if (params.get("AddPower").equals("X")) {
                        power += CardFactoryUtil.xCount(card, card.getSVar("X"));
                    } else if (params.get("AddPower").equals("Y")) {
                        power += CardFactoryUtil.xCount(card, card.getSVar("Y"));
                    } else {
                        power += Integer.valueOf(params.get("AddPower"));
                    }
                }
            }
        }

        final ArrayList<Trigger> theTriggers = new ArrayList<Trigger>();
        for (Card card : AllZoneUtil.getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        theTriggers.addAll(attacker.getTriggers());
        for (final Trigger trigger : theTriggers) {
            final HashMap<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!CombatUtil.combatTriggerWillTrigger(attacker, defender, trigger, null)
                    || !trigParams.containsKey("Execute")) {
                continue;
            }
            final String ability = source.getSVar(trigParams.get("Execute"));
            final HashMap<String, String> abilityParams = AbilityFactory.getMapParams(ability, source);
            if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump")) {
                continue;
            }
            if (abilityParams.containsKey("DB") && !abilityParams.get("DB").equals("Pump")) {
                continue;
            }
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt")) {
                continue; // targeted pumping not supported
            }
            final ArrayList<Card> list = AbilityFactory.getDefinedCards(source, abilityParams.get("Defined"), null);
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredBlocker")) {
                list.add(defender);
            }
            if (list.isEmpty()) {
                continue;
            }
            if (!list.contains(defender)) {
                continue;
            }
            if (!abilityParams.containsKey("NumAtt")) {
                continue;
            }

            String att = abilityParams.get("NumAtt");
            if (att.startsWith("+")) {
                att = att.substring(1);
            }
            try {
                power += Integer.parseInt(att);
            } catch (final NumberFormatException nfe) {
                // can't parse the number (X for example)
                power += 0;
            }
        }
        if (withoutAbilities) {
            return power;
        }
        for (SpellAbility ability : defender.getAllSpellAbilities()) {
            if (!(ability instanceof AbilityActivated) || ability.getAbilityFactory() == null
                    || ability.getPayCosts() == null) {
                continue;
            }
            AbilityFactory af = ability.getAbilityFactory();

            if (!af.getAPI().equals("Pump")) {
                continue;
            }
            HashMap<String, String> params = af.getMapParams();
            if (!params.containsKey("NumAtt")) {
                continue;
            }

            if (ComputerUtil.canPayCost(ability, defender.getController())) {
                int pBonus = AbilityFactory.calculateAmount(ability.getSourceCard(), params.get("NumAtt"), ability);
                if (pBonus > 0) {
                    power += pBonus;
                }
            }
        }
        return power;
    }

    // Predict the Toughness bonus of the blocker if blocking the attacker
    // (Flanking, Bushido and other triggered abilities)
    /**
     * <p>
     * predictToughnessBonusOfBlocker.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int predictToughnessBonusOfBlocker(final Card attacker, final Card defender, boolean withoutAbilities) {
        int toughness = 0;

        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {
            toughness -= attacker.getAmountOfKeyword("Flanking");
        }

        toughness += defender.getKeywordMagnitude("Bushido");

        final ArrayList<Trigger> theTriggers = new ArrayList<Trigger>();
        for (Card card : AllZoneUtil.getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        theTriggers.addAll(attacker.getTriggers());
        for (final Trigger trigger : theTriggers) {
            final HashMap<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!CombatUtil.combatTriggerWillTrigger(attacker, defender, trigger, null)
                    || !trigParams.containsKey("Execute")) {
                continue;
            }
            final String ability = source.getSVar(trigParams.get("Execute"));
            final HashMap<String, String> abilityParams = AbilityFactory.getMapParams(ability, source);

            // DealDamage triggers
            if ((abilityParams.containsKey("AB") && abilityParams.get("AB").equals("DealDamage"))
                    || (abilityParams.containsKey("DB") && abilityParams.get("DB").equals("DealDamage"))) {
                if (!abilityParams.containsKey("Defined") || !abilityParams.get("Defined").equals("TriggeredBlocker")) {
                    continue;
                }
                int damage = 0;
                try {
                    damage = Integer.parseInt(abilityParams.get("NumDmg"));
                } catch (final NumberFormatException nfe) {
                    // can't parse the number (X for example)
                    continue;
                }
                toughness -= defender.predictDamage(damage, 0, source, false);
                continue;
            }

            // Pump triggers
            if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump")) {
                continue;
            }
            if (abilityParams.containsKey("DB") && !abilityParams.get("DB").equals("Pump")) {
                continue;
            }
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt")) {
                continue; // targeted pumping not supported
            }
            final ArrayList<Card> list = AbilityFactory.getDefinedCards(source, abilityParams.get("Defined"), null);
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredBlocker")) {
                list.add(defender);
            }
            if (list.isEmpty()) {
                continue;
            }
            if (!list.contains(defender)) {
                continue;
            }
            if (!abilityParams.containsKey("NumDef")) {
                continue;
            }

            String def = abilityParams.get("NumDef");
            if (def.startsWith("+")) {
                def = def.substring(1);
            }
            try {
                toughness += Integer.parseInt(def);
            } catch (final NumberFormatException nfe) {
                // can't parse the number (X for example)

            }
        }
        if (withoutAbilities) {
            return toughness;
        }
        for (SpellAbility ability : defender.getAllSpellAbilities()) {
            if (!(ability instanceof AbilityActivated) || ability.getAbilityFactory() == null
                    || ability.getPayCosts() == null) {
                continue;
            }
            AbilityFactory af = ability.getAbilityFactory();

            if (!af.getAPI().equals("Pump")) {
                continue;
            }
            HashMap<String, String> params = af.getMapParams();
            if (!params.containsKey("NumDef")) {
                continue;
            }

            if (ComputerUtil.canPayCost(ability, defender.getController())) {
                int tBonus = AbilityFactory.calculateAmount(ability.getSourceCard(), params.get("NumDef"), ability);
                if (tBonus > 0) {
                    toughness += tBonus;
                }
            }
        }
        return toughness;
    }

    // Predict the Power bonus of the blocker if blocking the attacker
    // (Flanking, Bushido and other triggered abilities)
    /**
     * <p>
     * predictPowerBonusOfAttacker.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a int.
     */
    public static int predictPowerBonusOfAttacker(final Card attacker, final Card defender, final Combat combat) {
        int power = 0;

        power += attacker.getKeywordMagnitude("Bushido");
        //check Exalted only for the first attacker
        if (combat != null && combat.getAttackers().isEmpty()) {
            for (Card card : attacker.getController().getCardsIn(ZoneType.Battlefield)) {
                power += card.getKeywordAmount("Exalted");
            }
        }

        final ArrayList<Trigger> theTriggers = new ArrayList<Trigger>();
        for (Card card : AllZoneUtil.getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        // if the defender has first strike and wither the attacker will deal
        // less damage than expected
        if (null != defender) {
            if ((defender.hasKeyword("First Strike") || defender.hasKeyword("Double Strike"))
                    && (defender.hasKeyword("Wither") || defender.hasKeyword("Infect"))
                    && !(attacker.hasKeyword("First Strike") || attacker.hasKeyword("Double Strike") || attacker
                            .hasKeyword("CARDNAME can't have counters placed on it."))) {
                power -= defender.getNetCombatDamage();
            }
            theTriggers.addAll(defender.getTriggers());
        }

        // look out for continuous static abilities that only care for attacking
        // creatures
        final CardList cardList = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        for (final Card card : cardList) {
            for (final StaticAbility stAb : card.getStaticAbilities()) {
                final HashMap<String, String> params = stAb.getMapParams();
                if (!params.get("Mode").equals("Continuous")) {
                    continue;
                }
                if (!params.containsKey("Affected") || !params.get("Affected").contains("attacking")) {
                    continue;
                }
                final String valid = params.get("Affected").replace("attacking", "Creature");
                if (!attacker.isValid(valid, card.getController(), card)) {
                    continue;
                }
                if (params.containsKey("AddPower")) {
                    if (params.get("AddPower").equals("X")) {
                        power += CardFactoryUtil.xCount(card, card.getSVar("X"));
                    } else if (params.get("AddPower").equals("Y")) {
                        power += CardFactoryUtil.xCount(card, card.getSVar("Y"));
                    } else {
                        power += Integer.valueOf(params.get("AddPower"));
                    }
                }
            }
        }

        for (final Trigger trigger : theTriggers) {
            final HashMap<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!CombatUtil.combatTriggerWillTrigger(attacker, defender, trigger, combat)
                    || !trigParams.containsKey("Execute")) {
                continue;
            }
            final String ability = source.getSVar(trigParams.get("Execute"));
            final HashMap<String, String> abilityParams = AbilityFactory.getMapParams(ability, source);
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt")) {
                continue; // targeted pumping not supported
            }
            if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump")
                    && !abilityParams.get("AB").equals("PumpAll")) {
                continue;
            }
            if (abilityParams.containsKey("DB") && !abilityParams.get("DB").equals("Pump")
                    && !abilityParams.get("DB").equals("PumpAll")) {
                continue;
            }
            ArrayList<Card> list = new ArrayList<Card>();
            if (!abilityParams.containsKey("ValidCards")) {
                list = AbilityFactory.getDefinedCards(source, abilityParams.get("Defined"), null);
            }
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredAttacker")) {
                list.add(attacker);
            }
            if (abilityParams.containsKey("ValidCards")) {
                if (attacker.isValid(abilityParams.get("ValidCards").split(","), source.getController(), source)
                        || attacker.isValid(abilityParams.get("ValidCards").replace("attacking+", "").split(","),
                                source.getController(), source)) {
                    list.add(attacker);
                }
            }
            if (list.isEmpty()) {
                continue;
            }
            if (!list.contains(attacker)) {
                continue;
            }
            if (!abilityParams.containsKey("NumAtt")) {
                continue;
            }

            String att = abilityParams.get("NumAtt");
            if (att.startsWith("+")) {
                att = att.substring(1);
            }
            if (att.matches("[0-9][0-9]?") || att.matches("-" + "[0-9][0-9]?")) {
                power += Integer.parseInt(att);
            } else {
                String bonus = new String(source.getSVar(att));
                if (bonus.contains("TriggerCount$NumBlockers")) {
                    bonus = bonus.replace("TriggerCount$NumBlockers", "Number$1");
                }
                power += CardFactoryUtil.xCount(source, bonus);

            }
        }
        return power;
    }

    // Predict the Toughness bonus of the attacker if blocked by the blocker
    // (Flanking, Bushido and other triggered abilities)
    /**
     * <p>
     * predictToughnessBonusOfAttacker.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a int.
     */
    public static int predictToughnessBonusOfAttacker(final Card attacker, final Card defender, final Combat combat) {
        int toughness = 0;

        //check Exalted only for the first attacker
        if (combat != null && combat.getAttackers().isEmpty()) {
            for (Card card : attacker.getController().getCardsIn(ZoneType.Battlefield)) {
                toughness += card.getKeywordAmount("Exalted");
            }
        }

        final ArrayList<Trigger> theTriggers = new ArrayList<Trigger>();
        for (Card card : AllZoneUtil.getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        if (defender != null) {
            toughness += attacker.getKeywordMagnitude("Bushido");
            theTriggers.addAll(defender.getTriggers());
        }

        // look out for continuous static abilities that only care for attacking
        // creatures
        final CardList cardList = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        for (final Card card : cardList) {
            for (final StaticAbility stAb : card.getStaticAbilities()) {
                final HashMap<String, String> params = stAb.getMapParams();
                if (!params.get("Mode").equals("Continuous")) {
                    continue;
                }
                if (!params.containsKey("Affected") || !params.get("Affected").contains("attacking")) {
                    continue;
                }
                final String valid = params.get("Affected").replace("attacking", "Creature");
                if (!attacker.isValid(valid, card.getController(), card)) {
                    continue;
                }
                if (params.containsKey("AddToughness")) {
                    if (params.get("AddToughness").equals("X")) {
                        toughness += CardFactoryUtil.xCount(card, card.getSVar("X"));
                    } else if (params.get("AddToughness").equals("Y")) {
                        toughness += CardFactoryUtil.xCount(card, card.getSVar("Y"));
                    } else {
                        toughness += Integer.valueOf(params.get("AddToughness"));
                    }
                }
            }
        }

        for (final Trigger trigger : theTriggers) {
            final HashMap<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!CombatUtil.combatTriggerWillTrigger(attacker, defender, trigger, combat)
                    || !trigParams.containsKey("Execute")) {
                continue;
            }
            final String ability = source.getSVar(trigParams.get("Execute"));
            final HashMap<String, String> abilityParams = AbilityFactory.getMapParams(ability, source);
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt")) {
                continue; // targeted pumping not supported
            }

            // DealDamage triggers
            if ((abilityParams.containsKey("AB") && abilityParams.get("AB").equals("DealDamage"))
                    || (abilityParams.containsKey("DB") && abilityParams.get("DB").equals("DealDamage"))) {
                if (!abilityParams.containsKey("Defined") || !abilityParams.get("Defined").equals("TriggeredAttacker")) {
                    continue;
                }
                int damage = 0;
                try {
                    damage = Integer.parseInt(abilityParams.get("NumDmg"));
                } catch (final NumberFormatException nfe) {
                    // can't parse the number (X for example)
                    continue;
                }
                toughness -= attacker.predictDamage(damage, 0, source, false);
                continue;
            }

            // Pump triggers
            if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump")
                    && !abilityParams.get("AB").equals("PumpAll")) {
                continue;
            }
            if (abilityParams.containsKey("DB") && !abilityParams.get("DB").equals("Pump")
                    && !abilityParams.get("DB").equals("PumpAll")) {
                continue;
            }
            ArrayList<Card> list = new ArrayList<Card>();
            if (!abilityParams.containsKey("ValidCards")) {
                list = AbilityFactory.getDefinedCards(source, abilityParams.get("Defined"), null);
            }
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredAttacker")) {
                list.add(attacker);
            }
            if (abilityParams.containsKey("ValidCards")) {
                if (attacker.isValid(abilityParams.get("ValidCards").split(","), source.getController(), source)
                        || attacker.isValid(abilityParams.get("ValidCards").replace("attacking+", "").split(","),
                                source.getController(), source)) {
                    list.add(attacker);
                }
            }
            if (list.isEmpty()) {
                continue;
            }
            if (!list.contains(attacker)) {
                continue;
            }
            if (!abilityParams.containsKey("NumDef")) {
                continue;
            }

            String def = abilityParams.get("NumDef");
            if (def.startsWith("+")) {
                def = def.substring(1);
            }
            if (def.matches("[0-9][0-9]?") || def.matches("-" + "[0-9][0-9]?")) {
                toughness += Integer.parseInt(def);
            } else {
                String bonus = new String(source.getSVar(def));
                if (bonus.contains("TriggerCount$NumBlockers")) {
                    bonus = bonus.replace("TriggerCount$NumBlockers", "Number$1");
                }
                toughness += CardFactoryUtil.xCount(source, bonus);
            }
        }
        return toughness;
    }

    // Sylvan Basilisk and friends
    /**
     * <p>
     * checkDestroyBlockerTrigger.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean checkDestroyBlockerTrigger(final Card attacker, final Card defender) {
        final ArrayList<Trigger> theTriggers = new ArrayList<Trigger>();
        for (Card card : AllZoneUtil.getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        for (Trigger trigger : theTriggers) {
            HashMap<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!CombatUtil.combatTriggerWillTrigger(attacker, defender, trigger, null)) {
                continue;
            }
            //consider delayed triggers
            if (trigParams.containsKey("DelayedTrigger")) {
                String sVarName = trigParams.get("DelayedTrigger");
                trigger = TriggerHandler.parseTrigger(source.getSVar(sVarName), trigger.getHostCard(), true);
                trigParams = trigger.getMapParams();
            }
            if (!trigParams.containsKey("Execute")) {
                continue;
            }
            String ability = source.getSVar(trigParams.get("Execute"));
            final HashMap<String, String> abilityParams = AbilityFactory.getMapParams(ability, source);
            // Destroy triggers
            if ((abilityParams.containsKey("AB") && abilityParams.get("AB").equals("Destroy"))
                    || (abilityParams.containsKey("DB") && abilityParams.get("DB").equals("Destroy"))) {
                if (!abilityParams.containsKey("Defined")) {
                    continue;
                }
                if (abilityParams.get("Defined").equals("TriggeredBlocker")) {
                    return true;
                }
                if (abilityParams.get("Defined").equals("Self") && source.equals(defender)) {
                    return true;
                }
                if (abilityParams.get("Defined").equals("TriggeredTarget") && source.equals(attacker)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Cockatrice and friends
    /**
     * <p>
     * checkDestroyBlockerTrigger.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean checkDestroyAttackerTrigger(final Card attacker, final Card defender) {
        final ArrayList<Trigger> theTriggers = new ArrayList<Trigger>();
        for (Card card : AllZoneUtil.getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        for (Trigger trigger : theTriggers) {
            HashMap<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!CombatUtil.combatTriggerWillTrigger(attacker, defender, trigger, null)) {
                continue;
            }
            //consider delayed triggers
            if (trigParams.containsKey("DelayedTrigger")) {
                String sVarName = trigParams.get("DelayedTrigger");
                trigger = TriggerHandler.parseTrigger(source.getSVar(sVarName), trigger.getHostCard(), true);
                trigParams = trigger.getMapParams();
            }
            if (!trigParams.containsKey("Execute")) {
                continue;
            }
            String ability = source.getSVar(trigParams.get("Execute"));
            final HashMap<String, String> abilityParams = AbilityFactory.getMapParams(ability, source);
            // Destroy triggers
            if ((abilityParams.containsKey("AB") && abilityParams.get("AB").equals("Destroy"))
                    || (abilityParams.containsKey("DB") && abilityParams.get("DB").equals("Destroy"))) {
                if (!abilityParams.containsKey("Defined")) {
                    continue;
                }
                if (abilityParams.get("Defined").equals("TriggeredAttacker")) {
                    return true;
                }
                if (abilityParams.get("Defined").equals("Self") && source.equals(attacker)) {
                    return true;
                }
                if (abilityParams.get("Defined").equals("TriggeredTarget") && source.equals(defender)) {
                    return true;
                }
            }
        }
        return false;
    }

    // can the blocker destroy the attacker?
    /**
     * <p>
     * canDestroyAttacker.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @param withoutAbilities
     *            a boolean.
     * @return a boolean.
     */
    public static boolean canDestroyAttacker(final Card attacker, final Card defender, final Combat combat,
            final boolean withoutAbilities) {

        if (attacker.getName().equals("Sylvan Basilisk") && !defender.hasKeyword("Indestructible")) {
            return false;
        }

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetDefense()) {
                return false;
            }
            if ((flankingMagnitude >= (defender.getNetDefense() - defender.getDamage()))
                    && !defender.hasKeyword("Indestructible")) {
                return false;
            }
        } // flanking

        if (((attacker.hasKeyword("Indestructible") || (ComputerUtil.canRegenerate(attacker) && !withoutAbilities)) && !(defender
                .hasKeyword("Wither") || defender.hasKeyword("Infect")))
                || (attacker.hasKeyword("Persist") && !attacker.canHaveCountersPlacedOnIt(Counters.M1M1) && (attacker
                        .getCounters(Counters.M1M1) == 0))
                || (attacker.hasKeyword("Undying") && !attacker.canHaveCountersPlacedOnIt(Counters.P1P1) && (attacker
                        .getCounters(Counters.P1P1) == 0))) {
            return false;
        }
        if (checkDestroyAttackerTrigger(attacker, defender) && !attacker.hasKeyword("Indestructible")) {
            return true;
        }

        int defenderDamage = defender.getNetAttack()
                + CombatUtil.predictPowerBonusOfBlocker(attacker, defender, withoutAbilities);
        int attackerDamage = attacker.getNetAttack()
                + CombatUtil.predictPowerBonusOfAttacker(attacker, defender, combat);
        if (AllZoneUtil.isCardInPlay("Doran, the Siege Tower")) {
            defenderDamage = defender.getNetDefense()
                    + CombatUtil.predictToughnessBonusOfBlocker(attacker, defender, withoutAbilities);
            attackerDamage = attacker.getNetDefense()
                    + CombatUtil.predictToughnessBonusOfAttacker(attacker, defender, combat);
        }

        int possibleDefenderPrevention = 0;
        int possibleAttackerPrevention = 0;
        if (!withoutAbilities) {
            possibleDefenderPrevention = ComputerUtil.possibleDamagePrevention(defender);
            possibleAttackerPrevention = ComputerUtil.possibleDamagePrevention(attacker);
        }

        // consider Damage Prevention/Replacement
        defenderDamage = attacker.predictDamage(defenderDamage, possibleAttackerPrevention, defender, true);
        attackerDamage = defender.predictDamage(attackerDamage, possibleDefenderPrevention, attacker, true);

        final int defenderLife = defender.getKillDamage()
                + CombatUtil.predictToughnessBonusOfBlocker(attacker, defender, withoutAbilities);
        final int attackerLife = attacker.getKillDamage()
                + CombatUtil.predictToughnessBonusOfAttacker(attacker, defender, combat);

        if (defender.hasKeyword("Double Strike")) {
            if (defenderDamage > 0 && (defender.hasKeyword("Deathtouch")
                        || attacker.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                return true;
            }
            if (defenderDamage >= attackerLife) {
                return true;
            }

            // Attacker may kill the blocker before he can deal normal
            // (secondary) damage
            if ((attacker.hasKeyword("Double Strike") || attacker.hasKeyword("First Strike"))
                    && !defender.hasKeyword("Indestructible")) {
                if (attackerDamage >= defenderLife) {
                    return false;
                }
                if (attackerDamage > 0 && (attacker.hasKeyword("Deathtouch")
                        || defender.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                    return false;
                }
            }
            if (attackerLife <= (2 * defenderDamage)) {
                return true;
            }
        } // defender double strike

        else { // no double strike for defender
               // Attacker may kill the blocker before he can deal any damage
            if ((attacker.hasKeyword("Double Strike") || attacker.hasKeyword("First Strike"))
                    && !defender.hasKeyword("Indestructible")
                    && !defender.hasKeyword("First Strike")) {

                if (attackerDamage >= defenderLife) {
                    return false;
                }
                if (attackerDamage > 0 && (attacker.hasKeyword("Deathtouch")
                        || defender.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                    return false;
                }
            }

            if (defenderDamage > 0 && (defender.hasKeyword("Deathtouch")
                        || attacker.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                return true;
            }

            return defenderDamage >= attackerLife;

        } // defender no double strike
        return false; // should never arrive here
    } // canDestroyAttacker

    // For AI safety measures like Regeneration
    /**
     * <p>
     * blockerWouldBeDestroyed.
     * </p>
     * 
     * @param blocker
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean blockerWouldBeDestroyed(final Card blocker) {
        // TODO THis function only checks if a single attacker at a time would destroy a blocker
        // This needs to expand to tally up damage
        final CardList attackers = AllZone.getCombat().getAttackersBlockedBy(blocker);

        for(Card attacker : attackers) {
            if (CombatUtil.canDestroyBlocker(blocker, attacker, AllZone.getCombat(), true)
                    && !(attacker.hasKeyword("Wither") || attacker.hasKeyword("Infect"))) {
                return true;
            }
        }
        return false;
    }

    // can the attacker destroy this blocker?
    /**
     * <p>
     * canDestroyBlocker.
     * </p>
     * 
     * @param defender
     *            a {@link forge.Card} object.
     * @param attacker
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @param withoutAbilities
     *            a boolean.
     * @return a boolean.
     */
    public static boolean canDestroyBlocker(final Card defender, final Card attacker, final Combat combat,
            final boolean withoutAbilities) {

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetDefense()) {
                return true;
            }
            if ((flankingMagnitude >= defender.getKillDamage()) && !defender.hasKeyword("Indestructible")) {
                return true;
            }
        } // flanking

        if (((defender.hasKeyword("Indestructible") || (ComputerUtil.canRegenerate(defender) && !withoutAbilities)) && !(attacker
                .hasKeyword("Wither") || attacker.hasKeyword("Infect")))
                || (defender.hasKeyword("Persist") && !defender.canHaveCountersPlacedOnIt(Counters.M1M1) && (defender
                        .getCounters(Counters.M1M1) == 0))
                || (defender.hasKeyword("Undying") && !defender.canHaveCountersPlacedOnIt(Counters.P1P1) && (defender
                        .getCounters(Counters.P1P1) == 0))) {
            return false;
        }

        if (checkDestroyBlockerTrigger(attacker, defender) && !defender.hasKeyword("Indestructible")) {
            return true;
        }

        int defenderDamage = defender.getNetAttack()
                + CombatUtil.predictPowerBonusOfBlocker(attacker, defender, withoutAbilities);
        int attackerDamage = attacker.getNetAttack()
                + CombatUtil.predictPowerBonusOfAttacker(attacker, defender, combat);
        if (AllZoneUtil.isCardInPlay("Doran, the Siege Tower")) {
            defenderDamage = defender.getNetDefense()
                    + CombatUtil.predictToughnessBonusOfBlocker(attacker, defender, withoutAbilities);
            attackerDamage = attacker.getNetDefense()
                    + CombatUtil.predictToughnessBonusOfAttacker(attacker, defender, combat);
        }

        int possibleDefenderPrevention = 0;
        int possibleAttackerPrevention = 0;
        if (!withoutAbilities) {
            possibleDefenderPrevention = ComputerUtil.possibleDamagePrevention(defender);
            possibleAttackerPrevention = ComputerUtil.possibleDamagePrevention(attacker);
        }

        // consider Damage Prevention/Replacement
        defenderDamage = attacker.predictDamage(defenderDamage, possibleAttackerPrevention, defender, true);
        attackerDamage = defender.predictDamage(attackerDamage, possibleDefenderPrevention, attacker, true);

        if (combat != null) {
            for (Card atkr : combat.getAttackersBlockedBy(defender)) {
                if (!atkr.equals(attacker)) {
                    attackerDamage += defender.predictDamage(atkr.getNetCombatDamage(), 0, atkr, true);
                }
            }
        }

        final int defenderLife = defender.getKillDamage()
                + CombatUtil.predictToughnessBonusOfBlocker(attacker, defender, withoutAbilities);
        final int attackerLife = attacker.getKillDamage()
                + CombatUtil.predictToughnessBonusOfAttacker(attacker, defender, combat);

        if (attacker.hasKeyword("Double Strike")) {
            if (attackerDamage > 0 && (attacker.hasKeyword("Deathtouch") 
                    || defender.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                return true;
            }
            if (attackerDamage >= defenderLife) {
                return true;
            }

            // Attacker may kill the blocker before he can deal normal
            // (secondary) damage
            if ((defender.hasKeyword("Double Strike") || defender.hasKeyword("First Strike"))
                    && !attacker.hasKeyword("Indestructible")) {
                if (defenderDamage >= attackerLife) {
                    return false;
                }
                if (defenderDamage > 0 && (defender.hasKeyword("Deathtouch") 
                        || attacker.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                    return false;
                }
            }
            if (defenderLife <= (2 * attackerDamage)) {
                return true;
            }
        } // attacker double strike

        else { // no double strike for attacker
               // Defender may kill the attacker before he can deal any damage
            if (defender.hasKeyword("Double Strike")
                    || (defender.hasKeyword("First Strike") && !attacker.hasKeyword("Indestructible") && !attacker
                            .hasKeyword("First Strike"))) {

                if (defenderDamage >= attackerLife) {
                    return false;
                }
                if (defenderDamage > 0 && (defender.hasKeyword("Deathtouch") 
                        || attacker.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                    return false;
                }
            }

            if (attackerDamage > 0 && (attacker.hasKeyword("Deathtouch") 
                    || defender.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                return true;
            }

            return attackerDamage >= defenderLife;

        } // attacker no double strike
        return false; // should never arrive here
    } // canDestroyBlocker

    /**
     * <p>
     * removeAllDamage.
     * </p>
     */
    public static void removeAllDamage() {
        final CardList cl = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        for (final Card c : cl) {
            c.setDamage(0);
        }
    }

    /**
     * gets a string for the GameLog regarding attackers.
     * 
     * @return a String
     */
    public static String getCombatAttackForLog() {
        final StringBuilder sb = new StringBuilder();

        // Loop through Defenders
        // Append Defending Player/Planeswalker
        final Combat combat = AllZone.getCombat();
        final List<GameEntity> defenders = combat.getDefenders();
        final CardList[] attackers = combat.sortAttackerByDefender();

        // Not a big fan of the triple nested loop here
        for (int def = 0; def < defenders.size(); def++) {
            if ((attackers[def] == null) || (attackers[def].size() == 0)) {
                continue;
            }

            sb.append(combat.getAttackingPlayer()).append(" declared ");
            for (final Card attacker : attackers[def]) {
                sb.append(attacker).append(" ");
            }

            sb.append("attacking ").append(defenders.get(def).toString()).append(".");
        }

        return sb.toString();
    }

    /**
     * gets a string for the GameLog regarding assigned blockers.
     * 
     * @return a String
     */
    public static String getCombatBlockForLog() {
        final StringBuilder sb = new StringBuilder();

        CardList defend = null;

        // Loop through Defenders
        // Append Defending Player/Planeswalker
        final Combat combat = AllZone.getCombat();
        final List<GameEntity> defenders = combat.getDefenders();
        final CardList[] attackers = combat.sortAttackerByDefender();

        // Not a big fan of the triple nested loop here
        for (int def = 0; def < defenders.size(); def++) {
            final CardList list = attackers[def];

            for (final Card attacker : list) {
                sb.append(combat.getDefendingPlayer()).append(" assigned ");

                defend = AllZone.getCombat().getBlockers(attacker);

                if (!defend.isEmpty()) {
                    // loop through blockers
                    for (final Card blocker : defend) {
                        sb.append(blocker).append(" ");
                    }
                } else {
                    sb.append("<nothing> ");
                }

                sb.append("to block ").append(attacker).append(". ");
            } // loop through attackers
        }

        return sb.toString();
    }

    /**
     * <p>
     * showCombat.
     * </p>
     */
    public static void showCombat() {
        // TODO(sol) ShowCombat seems to be resetting itself when switching away and switching back?
        final StringBuilder display = new StringBuilder();
        
        if (!Singletons.getModel().getGameState().getPhaseHandler().inCombat()) {
            VCombat.SINGLETON_INSTANCE.updateCombat(display.toString().trim());
            return;
        }

        // Loop through Defenders
        // Append Defending Player/Planeswalker
        final List<GameEntity> defenders = AllZone.getCombat().getDefenders();
        final CardList[] attackers = AllZone.getCombat().sortAttackerByDefender();

        // Not a big fan of the triple nested loop here
        for (int def = 0; def < defenders.size(); def++) {
            if ((attackers[def] == null) || (attackers[def].size() == 0)) {
                continue;
            }

            if (def > 0) {
                display.append("\n");
            }

            display.append("Defender - ");
            display.append(defenders.get(def).toString());
            display.append("\n");

            final CardList list = attackers[def];

            for (final Card c : list) {
                // loop through attackers
                display.append("-> ");
                display.append(CombatUtil.combatantToString(c)).append("\n");

                CardList blockers = AllZone.getCombat().getBlockers(c);

                // loop through blockers
                for (final Card element : blockers) {
                    display.append(" [ ");
                    display.append(CombatUtil.combatantToString(element)).append("\n");
                }
            } // loop through attackers
        }

        SDisplayUtil.showTab(EDocID.REPORT_COMBAT.getDoc());
        VCombat.SINGLETON_INSTANCE.updateCombat(display.toString().trim());
    } // showBlockers()

    /**
     * <p>
     * combatantToString.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.lang.String} object.
     */
    private static String combatantToString(final Card c) {
        final StringBuilder sb = new StringBuilder();

        final String name = (c.isFaceDown()) ? "Morph" : c.getName();

        sb.append(name);
        sb.append(" (").append(c.getUniqueNumber()).append(") ");
        sb.append(c.getNetAttack()).append("/").append(c.getNetDefense());

        return sb.toString();
    }

    /**
     * <p>
     * checkPropagandaEffects.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param bLast
     *            a boolean.
     */
    public static void checkPropagandaEffects(final Card c, final boolean bLast) {
        Cost attackCost = new Cost(c, "0", true);
        // Sort abilities to apply them in proper order
        for (Card card : AllZoneUtil.getCardsIn(ZoneType.Battlefield)) {
            final ArrayList<StaticAbility> staticAbilities = card.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                Cost additionalCost = stAb.getCostAbility("CantAttackUnless", c, AllZone.getCombat().getDefenderByAttacker(c));
                attackCost = CostUtil.combineCosts(attackCost, additionalCost);
            }
        }
        if (attackCost.toSimpleString().equals("")) {
            if (!c.hasKeyword("Vigilance")) {
                c.tap();
            }

            if (bLast) {
                PhaseUtil.handleAttackingTriggers();
            }
            return;
        }

        final Card crd = c;

        final PhaseType phase = Singletons.getModel().getGameState().getPhaseHandler().getPhase();

        if (phase == PhaseType.COMBAT_DECLARE_ATTACKERS || phase == PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY) {
            final Ability ability = new AbilityStatic(c, attackCost, null) {
                @Override
                public void resolve() {

                }
            };

            final Command unpaidCommand = new Command() {

                private static final long serialVersionUID = -6483405139208343935L;

                @Override
                public void execute() {
                    AllZone.getCombat().removeFromCombat(crd);

                    if (bLast) {
                        PhaseUtil.handleAttackingTriggers();
                    }
                }
            };

            final Command paidCommand = new Command() {
                private static final long serialVersionUID = -8303368287601871955L;

                @Override
                public void execute() {
                    // if Propaganda is paid, tap this card
                    if (!crd.hasKeyword("Vigilance")) {
                        crd.tap();
                    }

                    if (bLast) {
                        PhaseUtil.handleAttackingTriggers();
                    }
                }
            };

            if (c.getController().isHuman()) {
                GameActionUtil.payCostDuringAbilityResolve(ability, attackCost, paidCommand, unpaidCommand, null);
            } else { // computer
                if (ComputerUtil.canPayCost(ability)) {
                    ComputerUtil.playNoStack(ability);
                    if (!crd.hasKeyword("Vigilance")) {
                        crd.tap();
                    }
                } else {
                    // TODO remove the below line after Propaganda occurs
                    // during Declare_Attackers
                    AllZone.getCombat().removeFromCombat(crd);
                }
                if (bLast) {
                    PhaseUtil.handleAttackingTriggers();
                }
            }
        }
    }

    /**
     * <p>
     * This method checks triggered effects of attacking creatures, right before
     * defending player declares blockers.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public static void checkDeclareAttackers(final Card c) {
        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Attacker", c);
        final CardList otherAttackers = AllZone.getCombat().getAttackerList();
        otherAttackers.remove(c);
        runParams.put("OtherAttackers", otherAttackers);
        runParams.put("Attacked", AllZone.getCombat().getDefenderByAttacker(c));
        AllZone.getTriggerHandler().runTrigger(TriggerType.Attacks, runParams);

        // Annihilator:
        if (!c.getDamageHistory().getCreatureAttackedThisCombat()) {
            final ArrayList<String> kws = c.getKeyword();
            final Pattern p = Pattern.compile("Annihilator [0-9]+");
            Matcher m;
            for (final String key : kws) {
                m = p.matcher(key);
                if (m.find()) {
                    final String[] k = key.split(" ");
                    final int a = Integer.valueOf(k[1]);
                    final Card crd = c;

                    final Ability ability = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (crd.getController().isHuman()) {
                                final CardList list = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
                                ComputerUtil.sacrificePermanents(a, list, false, this);
                            } else {
                                AbilityFactorySacrifice.sacrificeHuman(AllZone.getHumanPlayer(), a, "Permanent", this,
                                        false, false);
                            }

                        }
                    };
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Annihilator - Defending player sacrifices ").append(a).append(" permanents.");
                    ability.setStackDescription(sb.toString());
                    ability.setDescription(sb.toString());
                    ability.setActivatingPlayer(c.getController());

                    AllZone.getStack().add(ability);
                } // find
            } // for
        } // creatureAttacked
          // Annihilator

        // Mijae Djinn
        if (c.getName().equals("Mijae Djinn")) {
            if (!GameActionUtil.flipACoin(c.getController(), c)) {
                AllZone.getCombat().removeFromCombat(c);
                c.tap();
            }
        } // Mijae Djinn
        else if (c.getName().equals("Witch-Maw Nephilim") && !c.getDamageHistory().getCreatureAttackedThisCombat()
                && (c.getNetAttack() >= 10)) {
            final Card charger = c;
            final Ability ability2 = new Ability(c, "0") {
                @Override
                public void resolve() {

                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -1703473800920781454L;

                        @Override
                        public void execute() {
                            if (AllZoneUtil.isCardInPlay(charger)) {
                                charger.removeIntrinsicKeyword("Trample");
                            }
                        }
                    }; // Command

                    if (AllZoneUtil.isCardInPlay(charger)) {
                        charger.addIntrinsicKeyword("Trample");

                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve
            }; // ability

            final StringBuilder sb2 = new StringBuilder();
            sb2.append(c.getName()).append(" - gains trample until end of turn if its power is 10 or greater.");
            ability2.setStackDescription(sb2.toString());

            AllZone.getStack().add(ability2);

        } // Witch-Maw Nephilim

        else if (c.getName().equals("Sapling of Colfenor") && !c.getDamageHistory().getCreatureAttackedThisCombat()) {
            final Player player = c.getController();

            final PlayerZone lib = player.getZone(ZoneType.Library);

            if (lib.size() > 0) {
                final CardList cl = new CardList();
                cl.add(lib.get(0));
                GuiUtils.chooseOneOrNone("Top card", cl);
                final Card top = lib.get(0);
                if (top.isCreature()) {
                    player.gainLife(top.getBaseDefense(), c);
                    player.loseLife(top.getBaseAttack(), c);

                    Singletons.getModel().getGameAction().moveToHand(top);
                }
            }
        } // Sapling of Colfenor

        c.getDamageHistory().setCreatureAttackedThisCombat(true);
        c.getController().setAttackedWithCreatureThisTurn(true);
        c.getController().incrementAttackersDeclaredThisTurn();
    } // checkDeclareAttackers

    /**
     * <p>
     * checkUnblockedAttackers.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public static void checkUnblockedAttackers(final Card c) {

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", c);
        AllZone.getTriggerHandler().runTrigger(TriggerType.AttackerUnblocked, runParams);
    }

    /**
     * <p>
     * checkDeclareBlockers.
     * </p>
     * 
     * @param cl
     *            a {@link forge.CardList} object.
     */
    public static void checkDeclareBlockers(final CardList cl) {
        for (final Card c : cl) {
            if (!c.getDamageHistory().getCreatureBlockedThisCombat()) {
                for (final Ability ab : CardFactoryUtil.getBushidoEffects(c)) {
                    AllZone.getStack().add(ab);
                }
            }

            c.getDamageHistory().setCreatureBlockedThisCombat(true);
        } // for

    } // checkDeclareBlockers

    /**
     * <p>
     * checkBlockedAttackers.
     * </p>
     * 
     * @param a
     *            a {@link forge.Card} object.
     * @param b
     *            a {@link forge.Card} object.
     */
    public static void checkBlockedAttackers(final Card a, final Card b) {
        // System.out.println(a.getName() + " got blocked by " + b.getName());

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Attacker", a);
        runParams.put("Blocker", b);
        AllZone.getTriggerHandler().runTrigger(TriggerType.Blocks, runParams);

        if (!a.getDamageHistory().getCreatureGotBlockedThisCombat()) {
            final int blockers = AllZone.getCombat().getBlockers(a).size();
            runParams.put("NumBlockers", blockers);
            AllZone.getTriggerHandler().runTrigger(TriggerType.AttackerBlocked, runParams);

            // Bushido
            for (final Ability ab : CardFactoryUtil.getBushidoEffects(a)) {
                AllZone.getStack().add(ab);
            }

            // Rampage
            final ArrayList<String> keywords = a.getKeyword();
            final Pattern p = Pattern.compile("Rampage [0-9]+");
            Matcher m;
            for (final String keyword : keywords) {
                m = p.matcher(keyword);
                if (m.find()) {
                    final String[] k = keyword.split(" ");
                    final int magnitude = Integer.valueOf(k[1]);
                    final int numBlockers = AllZone.getCombat().getBlockers(a).size();
                    if (numBlockers > 1) {
                        CombatUtil.executeRampageAbility(a, magnitude, numBlockers);
                    }
                } // find
            } // end Rampage
        }

        if (a.hasKeyword("Flanking") && !b.hasKeyword("Flanking")) {
            int flankingMagnitude = 0;
            String kw = "";
            final ArrayList<String> list = a.getKeyword();

            for (int i = 0; i < list.size(); i++) {
                kw = list.get(i);
                if (kw.equals("Flanking")) {
                    flankingMagnitude++;
                }
            }
            final int mag = flankingMagnitude;
            final Card blocker = b;
            final Ability ability2 = new Ability(b, "0") {
                @Override
                public void resolve() {

                    final Command untilEOT = new Command() {

                        private static final long serialVersionUID = 7662543891117427727L;

                        @Override
                        public void execute() {
                            if (AllZoneUtil.isCardInPlay(blocker)) {
                                blocker.addTempAttackBoost(mag);
                                blocker.addTempDefenseBoost(mag);
                            }
                        }
                    }; // Command

                    if (AllZoneUtil.isCardInPlay(blocker)) {
                        blocker.addTempAttackBoost(-mag);
                        blocker.addTempDefenseBoost(-mag);

                        AllZone.getEndOfTurn().addUntil(untilEOT);
                        System.out.println("Flanking!");
                    }
                } // resolve

            }; // ability

            final StringBuilder sb2 = new StringBuilder();
            sb2.append(b.getName()).append(" - gets -").append(mag).append("/-").append(mag).append(" until EOT.");
            ability2.setStackDescription(sb2.toString());
            ability2.setDescription(sb2.toString());

            AllZone.getStack().add(ability2);
            Log.debug("Adding Flanking!");

        } // flanking

        a.getDamageHistory().setCreatureGotBlockedThisCombat(true);
        b.addBlockedThisTurn(a);
        a.addBlockedByThisTurn(b);
    }

    /**
     * <p>
     * executeExaltedAbility.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param magnitude
     *            a int.
     */
    public static void executeExaltedAbility(final Card c, final int magnitude) {
        final Card crd = c;
        Ability ability;

        for (int i = 0; i < magnitude; i++) {
            ability = new Ability(c, "0") {
                @Override
                public void resolve() {
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = 1497565871061029469L;

                        @Override
                        public void execute() {
                            if (AllZoneUtil.isCardInPlay(crd)) {
                                crd.addTempAttackBoost(-1);
                                crd.addTempDefenseBoost(-1);
                            }
                        }
                    }; // Command

                    if (AllZoneUtil.isCardInPlay(crd)) {
                        crd.addTempAttackBoost(1);
                        crd.addTempDefenseBoost(1);

                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve

            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append(c).append(" - (Exalted) gets +1/+1 until EOT.");
            ability.setStackDescription(sb.toString());
            ability.setDescription(sb.toString());
            ability.setActivatingPlayer(c.getController());

            AllZone.getStack().addSimultaneousStackEntry(ability);
        }

        final Player phasingPlayer = c.getController();
        // Finest Hour untaps the creature on the first combat phase
        if ((phasingPlayer.getCardsIn(ZoneType.Battlefield, "Finest Hour").size() > 0)
                && Singletons.getModel().getGameState().getPhaseHandler().isFirstCombat()) {
            // Untap the attacking creature
            final Ability fhUntap = new Ability(c, "0") {
                @Override
                public void resolve() {
                    crd.untap();
                }
            };

            final StringBuilder sbUntap = new StringBuilder();
            sbUntap.append(c).append(" - (Finest Hour) untap.");
            fhUntap.setDescription(sbUntap.toString());
            fhUntap.setStackDescription(sbUntap.toString());

            AllZone.getStack().addSimultaneousStackEntry(fhUntap);

            // If any Finest Hours, queue up a new combat phase
            for (int ix = 0; ix < phasingPlayer.getCardsIn(ZoneType.Battlefield, "Finest Hour").size(); ix++) {
                final Ability fhAddCombat = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        Singletons.getModel().getGameState().getPhaseHandler().addExtraCombat();
                    }
                };

                final StringBuilder sbACom = new StringBuilder();
                sbACom.append(c).append(" - (Finest Hour) ").append(phasingPlayer).append(" gets Extra Combat Phase.");
                fhAddCombat.setDescription(sbACom.toString());
                fhAddCombat.setStackDescription(sbACom.toString());

                AllZone.getStack().addSimultaneousStackEntry(fhAddCombat);
            }
        }

        if (phasingPlayer.getCardsIn(ZoneType.Battlefield, "Sovereigns of Lost Alara").size() > 0) {
            for (int i = 0; i < phasingPlayer.getCardsIn(ZoneType.Battlefield, "Sovereigns of Lost Alara").size(); i++) {
                final Card attacker = c;
                final Ability ability4 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardList enchantments = attacker.getController().getCardsIn(ZoneType.Library);
                        enchantments = enchantments.filter(new Predicate<Card>() {
                            @Override
                            public boolean apply(final Card c) {
                                if (attacker.hasKeyword("Protection from enchantments")
                                        || (attacker.hasKeyword("Protection from everything"))) {
                                    return false;
                                }
                                return (c.isEnchantment() && c.hasKeyword("Enchant creature") && !CardFactoryUtil
                                        .hasProtectionFrom(c, attacker));
                            }
                        });
                        final Player player = attacker.getController();
                        Card enchantment = null;
                        if (player.isHuman()) {
                            final Card[] target = new Card[enchantments.size()];
                            for (int j = 0; j < enchantments.size(); j++) {
                                final Card crd = enchantments.get(j);
                                target[j] = crd;
                            }
                            final Object check = GuiUtils.chooseOneOrNone(
                                    "Select enchantment to enchant exalted creature", target);
                            if (check != null) {
                                enchantment = ((Card) check);
                            }
                        } else {
                            enchantment = CardFactoryUtil.getBestEnchantmentAI(enchantments, this, false);
                        }
                        if ((enchantment != null) && AllZoneUtil.isCardInPlay(attacker)) {
                            GameAction.changeZone(AllZone.getZoneOf(enchantment),
                                    enchantment.getOwner().getZone(ZoneType.Battlefield), enchantment, null);
                            enchantment.enchantEntity(attacker);
                        }
                        attacker.getController().shuffle();
                    } // resolve
                }; // ability4

                final StringBuilder sb4 = new StringBuilder();
                sb4.append(c).append(
                        " - (Exalted) searches library for an Aura card that could enchant that creature, ");
                sb4.append("put it onto the battlefield attached to that creature, then shuffles library.");
                ability4.setDescription(sb4.toString());
                ability4.setStackDescription(sb4.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability4);
            } // For
        }
    }

    /**
     * executes Rampage abilities for a given card.
     * 
     * @param c
     *            the card to add rampage bonus to
     * @param magnitude
     *            the magnitude of rampage (ie Rampage 2 means magnitude should
     *            be 2)
     * @param numBlockers
     *            - the number of creatures blocking this rampaging creature
     */
    private static void executeRampageAbility(final Card c, final int magnitude, final int numBlockers) {
        final Card crd = c;
        final int pump = magnitude;
        Ability ability;

        // numBlockers -1 since it is for every creature beyond the first
        for (int i = 0; i < (numBlockers - 1); i++) {
            ability = new Ability(c, "0") {
                @Override
                public void resolve() {
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -3215615538474963181L;

                        @Override
                        public void execute() {
                            if (AllZoneUtil.isCardInPlay(crd)) {
                                crd.addTempAttackBoost(-pump);
                                crd.addTempDefenseBoost(-pump);
                            }
                        }
                    }; // Command

                    if (AllZoneUtil.isCardInPlay(crd)) {
                        crd.addTempAttackBoost(pump);
                        crd.addTempDefenseBoost(pump);

                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve

            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append(c).append(" - (Rampage) gets +").append(pump).append("/+").append(pump).append(" until EOT.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().add(ability);
        }
    }

} // end class CombatUtil
