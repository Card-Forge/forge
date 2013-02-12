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

import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.Command;
import forge.Constant;
import forge.GameEntity;
import forge.Singletons;
import forge.card.SpellManaCost;
import forge.card.ability.effects.SacrificeEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityStatic;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.TriggerType;
import forge.game.GameActionUtil;
import forge.game.GameState;
import forge.game.GlobalRuleChange;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilBlock;
import forge.game.ai.ComputerUtilCost;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
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

        for (final Card c : Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield)) {
            for (final String keyword : c.getKeyword()) {
                if (keyword.equals("No more than two creatures can block each combat.")
                        && (combat.getAllBlockers().size() > 1)) {
                    return false;
                }
            }
        }

        if (combat.getAllBlockers().size() > 0
                && Singletons.getModel().getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.onlyOneBlocker)) {
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

        final List<Card> list = blocker.getController().getCreaturesInPlay();
        if (list.size() < 2 && blocker.hasKeyword("CARDNAME can't attack or block alone.")) {
            return false;
        }

        return true;
    }

    public static boolean canBlockMoreCreatures(final Card blocker, final List<Card> blockedBy) {
        // TODO(sol) expand this for the additional blocking keyword
        if (blockedBy.isEmpty() || blocker.hasKeyword("CARDNAME can block any number of creatures.")) {
            return true;
        }

        return blocker.getKeywordAmount("CARDNAME can block an additional creature.") >= blockedBy.size();
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
        final Player defendingPlayer = Singletons.getModel().getGame().getCombat().getDefendingPlayerRelatedTo(attacker);
        List<Card> defendingLands = defendingPlayer.getCardsIn(ZoneType.Battlefield);
        for (Card c : defendingLands) {
            if (c.isValid(valid.split(","), defendingPlayer, attacker)) {
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
    public static boolean canBlockAtLeastOne(final Card blocker, final Iterable<Card> attackers) {
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
    public static boolean canBeBlocked(final Card attacker, final List<Card> blockers) {
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
    public static boolean finishedMandatoryBlocks(final Combat combat, final Player defending) {

        final List<Card> blockers = Singletons.getControl().getPlayer().getCreaturesInPlay();
        final List<Card> attackers = combat.getAttackerList();

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
                            final List<Card> possibleBlockers = CardLists.filter(defending.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);
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
        final List<Card> attackers = combat.getAttackerList();
        for (final Card attacker : attackers) {
            List<Card> blockers = combat.getBlockers(attacker);
            if (blockers.size() <= 1) {
                continue;
            }

            List<Card> orderedBlockers = null;
            if (player.isHuman()) {
                GuiUtils.setPanelSelection(attacker);
                List<Card> ordered = GuiChoose.order("Choose Blocking Order", "Damaged First", 0, blockers, null, attacker);

                orderedBlockers = new ArrayList<Card>();
                for (Object o : ordered) {
                    orderedBlockers.add((Card) o);
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
        for (final Card blocker : combat.getAllBlockers()) {
            List<Card> attackers = combat.getAttackersBlockedBy(blocker);
            if (attackers.size() <= 1) {
                continue;
            }

            List<Card> orderedAttacker = null;
            if (blocker.getController().isHuman()) {
                GuiUtils.setPanelSelection(blocker);
                List<Card> ordered = GuiChoose.order("Choose Blocking Order", "Damaged First", 0, attackers, null, blocker);

                orderedAttacker = new ArrayList<Card>();
                for (Object o : ordered) {
                    orderedAttacker.add((Card) o);
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

        final List<Card> attackers = combat.getAttackerList();
        final List<Card> attackersWithLure = new ArrayList<Card>();
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
                    final List<Card> blockers = combat.getDefenderPlayerByAttacker(attacker).getCreaturesInPlay();
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
                if (CombatUtil.canBeBlocked(attacker, combat) && CombatUtil.canBlock(attacker, blocker)
                        && combat.isAttacking(attacker)) {
                    boolean canBe = true;
                    if (attacker.hasKeyword("CARDNAME can't be blocked except by two or more creatures.")) {
                        final List<Card> blockers = combat.getDefenderPlayerByAttacker(attacker).getCreaturesInPlay();
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
        
        if (attacker.hasKeyword("CARDNAME can't be blocked by creature tokens.") && blocker.isToken()) {
            return false;
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
        final GameEntity def = combat.getDefender();
        for (final Card card : Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield)) {
            for (final String keyword : card.getKeyword()) {
                if (keyword.equals("No more than two creatures can attack each combat.") && cntAttackers > 1) {
                    return false;
                }
                if (keyword.equals("No more than two creatures can attack you each combat.") && cntAttackers > 1
                        && card.getController().getOpponent().equals(c.getController())) {
                    return false;
                }
                if (keyword.equals("CARDNAME can only attack alone.") && combat.isAttacking(card)) {
                    return false;
                }
            }
        }

        final List<Card> list = c.getController().getCreaturesInPlay();
        if (list.size() < 2 && c.hasKeyword("CARDNAME can't attack or block alone.")) {
            return false;
        }

        if (cntAttackers > 0 && c.hasKeyword("CARDNAME can only attack alone.")) {
            return false;
        }

        if (cntAttackers > 0
                && Singletons.getModel().getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.onlyOneAttackerACombat)) {
            return false;
        }

        if ((cntAttackers > 0 || c.getController().getAttackedWithCreatureThisTurn())
                && Singletons.getModel().getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.onlyOneAttackerATurn)) {
            return false;
        }

        return CombatUtil.canAttack(c, def);
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
    public static boolean canAttack(final Card c, final GameEntity defender) {
        if (c.isTapped() || c.isPhasedOut()
                || (c.hasSickness() && !c.hasKeyword("CARDNAME can attack as though it had haste."))
                || Singletons.getModel().getGame().getPhaseHandler().getPhase()
                    .isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return false;
        }
        return CombatUtil.canAttackNextTurn(c, defender);
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
        return canAttackNextTurn(c, c.getController().getOpponent());
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
    public static boolean canAttackNextTurn(final Card c, final GameEntity defender) {
        if (!c.isCreature()) {
            return false;
        }

        Player defendingPlayer = null;
        if (defender instanceof Card) {
            defendingPlayer = ((Card) defender).getController();
        } else {
            defendingPlayer = (Player) defender;
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

                    List<Card> list = defendingPlayer.getCreaturesInPlay();
                    list = CardLists.filter(list, new Predicate<Card>() {
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

        final List<Card> list = defendingPlayer.getCardsIn(ZoneType.Battlefield);
        List<Card> temp;
        for (String keyword : c.getKeyword()) {
            if (keyword.equals("CARDNAME can't attack.") || keyword.equals("CARDNAME can't attack or block.")) {
                return false;
            } else if (keyword.equals("Defender") && !c.hasKeyword("CARDNAME can attack as though it didn't have defender.")) {
                return false;
            } else if (keyword.equals("CARDNAME can't attack unless defending player controls an Island.")) {
                temp = CardLists.getType(list, "Island");
                if (temp.isEmpty()) {
                    return false;
                }
            } else if (keyword.equals("CARDNAME can't attack unless defending player controls a Forest.")) {
                temp = CardLists.getType(list, "Forest");
                if (temp.isEmpty()) {
                    return false;
                }
            } else if (keyword.equals("CARDNAME can't attack unless defending player controls a Swamp.")) {
                temp = CardLists.getType(list, "Swamp");
                if (temp.isEmpty()) {
                    return false;
                }
            } else if (keyword.equals("CARDNAME can't attack unless defending player controls a Mountain.")) {
                temp = CardLists.getType(list, "Mountain");
                if (temp.isEmpty()) {
                    return false;
                }
            } else if (keyword.equals("CARDNAME can't attack unless defending player controls a snow land.")) {
                temp = CardLists.filter(list, CardPredicates.Presets.SNOW_LANDS);
                if (temp.isEmpty()) {
                    return false;
                }
            } else if (keyword.equals("CARDNAME can't attack unless defending player controls a blue permanent.")) {
                temp = CardLists.getColor(list, Constant.Color.BLUE);
                if (temp.isEmpty()) {
                    return false;
                }
            }
        }

        // The creature won't untap next turn
        if (c.isTapped() && !Untap.canUntap(c)) {
            return false;
        }

        // CantBeActivated static abilities
        for (final Card ca : Singletons.getModel().getGame().getCardsIn(ZoneType.listValueOf("Battlefield,Command"))) {
            final ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.applyAbility("CantAttack", c, defender)) {
                    return false;
                }
            }
        }

        return true;
    } // canAttack()


    /**
     * gets a string for the GameLog regarding attackers.
     * 
     * @return a String
     */
    public static String getCombatAttackForLog() {
        final StringBuilder sb = new StringBuilder();

        // Loop through Defenders
        // Append Defending Player/Planeswalker
        final Combat combat = Singletons.getModel().getGame().getCombat();
        final List<GameEntity> defenders = combat.getDefenders();
        final List<List<Card>> attackers = combat.sortAttackerByDefender();

        // Not a big fan of the triple nested loop here
        for (int def = 0; def < defenders.size(); def++) {
            List<Card> attacker = attackers.get(def);
            if ((attacker == null) || (attacker.size() == 0)) {
                continue;
            }

            sb.append(combat.getAttackingPlayer()).append(" declared ");
            for (final Card atk : attacker) {
                sb.append(atk).append(" ");
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

        List<Card> defend = null;

        // Loop through Defenders
        // Append Defending Player/Planeswalker
        final Combat combat = Singletons.getModel().getGame().getCombat();
        final List<GameEntity> defenders = combat.getDefenders();
        final List<List<Card>> attackers = combat.sortAttackerByDefender();

        // Not a big fan of the triple nested loop here
        for (int def = 0; def < defenders.size(); def++) {
            final List<Card> list = attackers.get(def);

            for (final Card attacker : list) {


                defend = Singletons.getModel().getGame().getCombat().getBlockers(attacker);
                sb.append(combat.getDefenderByAttacker(attacker)).append(" assigned ");

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

    private static String getCombatDescription(Combat combat) {
        final StringBuilder display = new StringBuilder();

        // Loop through Defenders
        // Append Defending Player/Planeswalker
        final List<GameEntity> defenders = combat.getDefenders();
        final List<List<Card>> attackers = combat.sortAttackerByDefender();

        // Not a big fan of the triple nested loop here
        for (int def = 0; def < defenders.size(); def++) {
            List<Card> atk = attackers.get(def);
            if ((atk == null) || (atk.size() == 0)) {
                continue;
            }

            if (def > 0) {
                display.append("\n");
            }

            display.append("Defender - ");
            display.append(defenders.get(def).toString());
            display.append("\n");

            for (final Card c : atk) {
                // loop through attackers
                display.append("-> ");
                display.append(CombatUtil.combatantToString(c)).append("\n");

                List<Card> blockers = combat.getBlockers(c);

                // loop through blockers
                for (final Card element : blockers) {
                    display.append(" [ ");
                    display.append(CombatUtil.combatantToString(element)).append("\n");
                }
            } // loop through attackers
        }
        return display.toString().trim();
    }


    /**
     * <p>
     * showCombat.
     * </p>
     */
    public static void showCombat() {
        // TODO(sol) ShowCombat seems to be resetting itself when switching away and switching back?
        String text = "";
        if (Singletons.getModel().getGame().getPhaseHandler().inCombat()) {
            text = getCombatDescription(Singletons.getModel().getGame().getCombat());
            SDisplayUtil.showTab(EDocID.REPORT_COMBAT.getDoc());
        }
        VCombat.SINGLETON_INSTANCE.updateCombat(text);
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
        final GameState game = Singletons.getModel().getGame();
        // Sort abilities to apply them in proper order
        for (Card card : Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield)) {
            final ArrayList<StaticAbility> staticAbilities = card.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                Cost additionalCost = stAb.getCostAbility("CantAttackUnless", c, game.getCombat().getDefenderByAttacker(c));
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

        
        final PhaseType phase = game.getPhaseHandler().getPhase();

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
                    game.getCombat().removeFromCombat(crd);

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

            ability.setActivatingPlayer(c.getController());
            if (c.getController().isHuman()) {
                GameActionUtil.payCostDuringAbilityResolve(c.getController(), ability, attackCost, paidCommand, unpaidCommand, null, game);
            } else { // computer
                if (ComputerUtilCost.canPayCost(ability, c.getController())) {
                    ComputerUtil.playNoStack((AIPlayer)c.getController(), ability, game);
                    if (!crd.hasKeyword("Vigilance")) {
                        crd.tap();
                    }
                } else {
                    // TODO remove the below line after Propaganda occurs
                    // during Declare_Attackers
                    game.getCombat().removeFromCombat(crd);
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
        final List<Card> otherAttackers = Singletons.getModel().getGame().getCombat().getAttackerList();
        otherAttackers.remove(c);
        runParams.put("OtherAttackers", otherAttackers);
        runParams.put("Attacked", Singletons.getModel().getGame().getCombat().getDefenderByAttacker(c));
        Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.Attacks, runParams, false);

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

                    final Ability ability = new Ability(c, SpellManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            final Player cp = crd.getController();
                            final Player opponent = Singletons.getModel().getGame().getCombat().getDefendingPlayerRelatedTo(c);
                            if (cp.isHuman()) {
                                final List<Card> list = opponent.getCardsIn(ZoneType.Battlefield);
                                ComputerUtil.sacrificePermanents(opponent,  a, list, false, this);
                            } else {
                                SacrificeEffect.sacrificeHuman(opponent, a, "Permanent", this, false, false);
                            }
                        }
                    };
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Annihilator - Defending player sacrifices ").append(a).append(" permanents.");
                    ability.setStackDescription(sb.toString());
                    ability.setDescription(sb.toString());
                    ability.setActivatingPlayer(c.getController());
                    ability.setTrigger(true);

                    Singletons.getModel().getGame().getStack().add(ability);
                } // find
            } // for
        } // creatureAttacked
          // Annihilator

        // Mijae Djinn
        if (c.getName().equals("Mijae Djinn")) {
            if (!GuiDialog.flipCoin(c.getController(), c)) {
                Singletons.getModel().getGame().getCombat().removeFromCombat(c);
                c.tap();
            }
        } // Mijae Djinn
        else if (c.getName().equals("Witch-Maw Nephilim") && !c.getDamageHistory().getCreatureAttackedThisCombat()
                && (c.getNetAttack() >= 10)) {
            final Card charger = c;
            final Ability ability2 = new Ability(c, SpellManaCost.ZERO) {
                @Override
                public void resolve() {

                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -1703473800920781454L;

                        @Override
                        public void execute() {
                            if (charger.isInPlay()) {
                                charger.removeIntrinsicKeyword("Trample");
                            }
                        }
                    }; // Command

                    if (charger.isInPlay()) {
                        charger.addIntrinsicKeyword("Trample");

                        Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve
            }; // ability

            final StringBuilder sb2 = new StringBuilder();
            sb2.append(c.getName()).append(" - gains trample until end of turn if its power is 10 or greater.");
            ability2.setStackDescription(sb2.toString());

            Singletons.getModel().getGame().getStack().add(ability2);

        } // Witch-Maw Nephilim

        else if (c.getName().equals("Sapling of Colfenor") && !c.getDamageHistory().getCreatureAttackedThisCombat()) {
            final Player player = c.getController();

            final PlayerZone lib = player.getZone(ZoneType.Library);

            if (lib.size() > 0) {
                final List<Card> cl = new ArrayList<Card>();
                cl.add(lib.get(0));
                GuiChoose.oneOrNone("Top card", cl);
                final Card top = lib.get(0);
                if (top.isCreature()) {
                    player.gainLife(top.getBaseDefense(), c);
                    player.loseLife(top.getBaseAttack());

                    Singletons.getModel().getGame().getAction().moveToHand(top);
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
        Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.AttackerUnblocked, runParams, false);
    }

    /**
     * <p>
     * checkDeclareBlockers.
     * </p>
     * 
     * @param cl
     *            a {@link forge.CardList} object.
     */
    public static void checkDeclareBlockers(final List<Card> cl) {
        for (final Card c : cl) {
            if (!c.getDamageHistory().getCreatureBlockedThisCombat()) {
                for (final Ability ab : CardFactoryUtil.getBushidoEffects(c)) {
                    Singletons.getModel().getGame().getStack().add(ab);
                }
                // Run triggers
                final HashMap<String, Object> runParams = new HashMap<String, Object>();
                runParams.put("Blocker", c);
                final Card attacker = Singletons.getModel().getGame().getCombat().getAttackersBlockedBy(c).get(0);
                runParams.put("Attacker", attacker);
                Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.Blocks, runParams, false);
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
        //Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.Blocks, runParams, false);

        if (!a.getDamageHistory().getCreatureGotBlockedThisCombat()) {
            final int blockers = Singletons.getModel().getGame().getCombat().getBlockers(a).size();
            runParams.put("NumBlockers", blockers);
            Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.AttackerBlocked, runParams, false);

            // Bushido
            for (final Ability ab : CardFactoryUtil.getBushidoEffects(a)) {
                Singletons.getModel().getGame().getStack().add(ab);
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
                    final int numBlockers = Singletons.getModel().getGame().getCombat().getBlockers(a).size();
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
            final Ability ability2 = new Ability(b, SpellManaCost.ZERO) {
                @Override
                public void resolve() {

                    final Command untilEOT = new Command() {

                        private static final long serialVersionUID = 7662543891117427727L;

                        @Override
                        public void execute() {
                            if (blocker.isInPlay()) {
                                blocker.addTempAttackBoost(mag);
                                blocker.addTempDefenseBoost(mag);
                            }
                        }
                    }; // Command

                    if (blocker.isInPlay()) {
                        blocker.addTempAttackBoost(-mag);
                        blocker.addTempDefenseBoost(-mag);

                        Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
                        System.out.println("Flanking!");
                    }
                } // resolve

            }; // ability

            final StringBuilder sb2 = new StringBuilder();
            sb2.append(b.getName()).append(" - gets -").append(mag).append("/-").append(mag).append(" until EOT.");
            ability2.setStackDescription(sb2.toString());
            ability2.setDescription(sb2.toString());

            Singletons.getModel().getGame().getStack().add(ability2);
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
            ability = new Ability(c, SpellManaCost.ZERO) {
                @Override
                public void resolve() {
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = 1497565871061029469L;

                        @Override
                        public void execute() {
                            if (crd.isInPlay()) {
                                crd.addTempAttackBoost(-1);
                                crd.addTempDefenseBoost(-1);
                            }
                        }
                    }; // Command

                    if (crd.isInPlay()) {
                        crd.addTempAttackBoost(1);
                        crd.addTempDefenseBoost(1);

                        Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve

            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append(c).append(" - (Exalted) gets +1/+1 until EOT.");
            ability.setStackDescription(sb.toString());
            ability.setDescription(sb.toString());
            ability.setActivatingPlayer(c.getController());

            Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);
        }

        final Player phasingPlayer = c.getController();

        if (phasingPlayer.getCardsIn(ZoneType.Battlefield, "Sovereigns of Lost Alara").size() > 0) {
            for (int i = 0; i < phasingPlayer.getCardsIn(ZoneType.Battlefield, "Sovereigns of Lost Alara").size(); i++) {
                final Card attacker = c;
                final Ability ability4 = new Ability(c, SpellManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        List<Card> enchantments =
                                CardLists.filter(attacker.getController().getCardsIn(ZoneType.Library), new Predicate<Card>() {
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
                            final Object check = GuiChoose.oneOrNone(
                                    "Select enchantment to enchant exalted creature", target);
                            if (check != null) {
                                enchantment = ((Card) check);
                            }
                        } else {
                            enchantment = CardFactoryUtil.getBestEnchantmentAI(enchantments, this, false);
                        }
                        if ((enchantment != null) && attacker.isInPlay()) {
                            Singletons.getModel().getGame().getAction().changeZone(Singletons.getModel().getGame().getZoneOf(enchantment),
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

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability4);
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
            ability = new Ability(c, SpellManaCost.ZERO) {
                @Override
                public void resolve() {
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -3215615538474963181L;

                        @Override
                        public void execute() {
                            if (crd.isInPlay()) {
                                crd.addTempAttackBoost(-pump);
                                crd.addTempDefenseBoost(-pump);
                            }
                        }
                    }; // Command

                    if (crd.isInPlay()) {
                        crd.addTempAttackBoost(pump);
                        crd.addTempDefenseBoost(pump);

                        Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve

            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append(c).append(" - (Rampage) gets +").append(pump).append("/+").append(pump).append(" until EOT.");
            ability.setStackDescription(sb.toString());

            Singletons.getModel().getGame().getStack().add(ability);
        }
    }

} // end class CombatUtil
