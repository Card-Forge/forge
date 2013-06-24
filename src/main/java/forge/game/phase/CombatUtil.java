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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.Command;
import forge.Constant;
import forge.GameEntity;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.card.ability.ApiType;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.TriggerType;
import forge.game.Game;
import forge.game.GlobalRuleChange;
import forge.game.combat.AttackingBand;
import forge.game.player.Player;
import forge.game.player.PlayerController.ManaPaymentPurpose;
import forge.game.zone.ZoneType;
import forge.util.Expressions;
import forge.util.TextUtil;


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
        final Game game = blocker.getGame();

        for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
            for (final String keyword : c.getKeyword()) {
                if (keyword.equals("No more than two creatures can block each combat.")
                        && (combat.getAllBlockers().size() > 1)) {
                    return false;
                }
            }
        }

        if (combat.getAllBlockers().size() > 0
                && game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.onlyOneBlocker)) {
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
    public static boolean canBeBlocked(final Card attacker, final Combat combat, Player defendingPlayer) {

        if (attacker == null) {
            return true;
        }

        if (attacker.hasStartOfKeyword("CantBeBlockedByAmount GT") && !combat.getBlockers(attacker).isEmpty()) {
            return false;
        }
        return CombatUtil.canBeBlocked(attacker, defendingPlayer);
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
    public static boolean canBeBlocked(final Card attacker, Player defender) {

        if (attacker == null) {
            return true;
        }

        if (attacker.hasKeyword("Unblockable")) {
            return false;
        }

        // Landwalk
        if (isUnblockableFromLandwalk(attacker, defender)) {
            return false;
        }

        return true;
    }


    public static boolean isUnblockableFromLandwalk(final Card attacker, Player defendingPlayer) {
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
            } else if (keyword.endsWith("walk")) {
                final String landtype = keyword.replace("walk", "");
                if (CardType.isALandType(landtype)) {
                    if (!walkTypes.contains(landtype)) {
                        walkTypes.add(landtype);
                    }
                }
            }
        }

        if (walkTypes.isEmpty()) {
            return false;
        }

        String valid = StringUtils.join(walkTypes, ",");
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
        int blocks = 0;
        for (final Card blocker : blockers) {
            if (CombatUtil.canBeBlocked(attacker, blocker.getController()) && CombatUtil.canBlock(attacker, blocker)) {
                blocks++;
            }
        }
        
        return canAttackerBeBlockedWithAmount(attacker, blocks);
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

        if (attacker.hasKeyword("CantBeBlockedByAmount LT2")) {
            return 2;
        } else if (attacker.hasKeyword("CantBeBlockedByAmount LT3")) {
            return 3;
        } else
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
    public static String validateBlocks(final Combat combat, final Player defending) {

        final List<Card> defendersArmy = defending.getCreaturesInPlay();
        final List<Card> attackers = combat.getAttackers();
        final List<Card> blockers = CardLists.filterControlledBy(combat.getAllBlockers(), defending); 

        // if a creature does not block but should, return false
        for (final Card blocker : defendersArmy) {
            // lure effects
            if (!blockers.contains(blocker) && CombatUtil.mustBlockAnAttacker(blocker, combat)) {
                return String.format("%s must block an attacker, but has not been assigned to block any.", blocker);
            }

            // "CARDNAME blocks each turn if able."
            if (!blockers.contains(blocker) && blocker.hasKeyword("CARDNAME blocks each turn if able.")) {
                for (final Card attacker : attackers) {
                    if (CombatUtil.canBlock(attacker, blocker, combat)) {
                        boolean must = true;
                        if (attacker.hasStartOfKeyword("CantBeBlockedByAmount LT")) {
                            final List<Card> possibleBlockers = Lists.newArrayList(defendersArmy);
                            possibleBlockers.remove(blocker);
                            if (!CombatUtil.canBeBlocked(attacker, possibleBlockers)) {
                                must = false;
                            }
                        }
                        if (must) {
                            return String.format("%s must block each turn, but was not assigned to block any attacker now", blocker);
                        }
                    }
                }
            }
        }

        for (final Card attacker : attackers) {
            int cntBlockers = combat.getBlockers(attacker).size();
            // don't accept blocker amount for attackers with keyword defining valid blockers amount
            if (cntBlockers > 0 && !canAttackerBeBlockedWithAmount(attacker, cntBlockers))
                return String.format("%s cannot be blocked with %d creatures you've assigned", attacker, cntBlockers);
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

        final List<Card> attackers = combat.getAttackers();
        final List<Card> attackersWithLure = new ArrayList<Card>();
        for (final Card attacker : attackers) {
            if (attacker.hasStartOfKeyword("All creatures able to block CARDNAME do so.")
                    || (attacker.hasStartOfKeyword("All Walls able to block CARDNAME do so.") && blocker.isType("Wall"))
                    || (attacker.hasStartOfKeyword("All creatures with flying able to block CARDNAME do so.") && blocker.hasKeyword("Flying"))
                    || (attacker.hasStartOfKeyword("CARDNAME must be blocked if able.") 
                            && combat.getBlockers(attacker).isEmpty())) {
                attackersWithLure.add(attacker);
            }
        }

        final Player defender = blocker.getController();
        for (final Card attacker : attackersWithLure) {
            if (CombatUtil.canBeBlocked(attacker, combat, defender) && CombatUtil.canBlock(attacker, blocker)) {
                boolean canBe = true;
                if (attacker.hasStartOfKeyword("CantBeBlockedByAmount LT")) {
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
                if (CombatUtil.canBeBlocked(attacker, combat, defender) && CombatUtil.canBlock(attacker, blocker)
                        && combat.isAttacking(attacker)) {
                    boolean canBe = true;
                    if (attacker.hasStartOfKeyword("CantBeBlockedByAmount LT")) {
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
        if (!CombatUtil.canBeBlocked(attacker, combat, blocker.getController())) {
            return false;
        }

        // if the attacker has no lure effect, but the blocker can block another
        // attacker with lure, the blocker can't block the former
        if (!attacker.hasKeyword("All creatures able to block CARDNAME do so.")
                && !(attacker.hasStartOfKeyword("All Walls able to block CARDNAME do so.") && blocker.isType("Wall"))
                && !(attacker.hasStartOfKeyword("All creatures with flying able to block CARDNAME do so.") && blocker.hasKeyword("Flying"))
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
        if (!CombatUtil.canBeBlocked(attacker, blocker.getController())) {
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

        if (attacker.hasKeyword("Creatures with power less than CARDNAME's power can't block it.") && attacker.getNetAttack() > blocker.getNetAttack()) {
            return false;
        }


        if (attacker.hasStartOfKeyword("CantBeBlockedBy ")) {
            final int keywordPosition = attacker.getKeywordPosition("CantBeBlockedBy ");
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

        if (blocker.hasKeyword("CARDNAME can block only creatures with flying.") && !attacker.hasKeyword("Flying")) {
            return false;
        }

        if (attacker.hasKeyword("Flying") && !blocker.hasKeyword("Flying") && !blocker.hasKeyword("Reach")) {
            return false;
        }

        if (attacker.hasKeyword("Horsemanship") && !blocker.hasKeyword("Horsemanship")) {
            return false;
        }

        if (attacker.hasKeyword("Fear") && !blocker.isArtifact() && !blocker.isBlack()) {
            return false;
        }

        if (attacker.hasKeyword("Intimidate") && !blocker.isArtifact() && !blocker.sharesColorWith(attacker)) {
            return false;
        }

        return true;
    } // canBlock()

    public static void checkAttackOrBlockAlone(Combat combat) {
        // Handles removing cards like Mogg Flunkies from combat if group attack
        // didn't occur
        for (Card c1 : combat.getAttackers()) {
            if (c1.hasKeyword("CARDNAME can't attack or block alone.")) {
                if (combat.getAttackers().size() < 2) {
                    combat.removeFromCombat(c1);
                }
            }
        }
    }

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
    public static boolean canAttack(final Card c, final GameEntity def, final Combat combat) {
        int cntAttackers = combat.getAttackers().size();
        final Game game = c.getGame();

        for (final Card card : game.getCardsIn(ZoneType.Battlefield)) {
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
                && game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.onlyOneAttackerACombat)) {
            return false;
        }

        if ((cntAttackers > 0 || c.getController().getAttackedWithCreatureThisTurn())
                && game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.onlyOneAttackerATurn)) {
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
        return canAttack(c) && canAttackNextTurn(c, defender);
    }
    
    public static boolean canAttack(final Card c) {
        final Game game = c.getGame();
        if (c.isTapped() || c.isPhasedOut()
                || (c.hasSickness() && !c.hasKeyword("CARDNAME can attack as though it had haste."))
                || game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return false;
        }
        return true;
    }
    
    public static boolean canAttackerBeBlockedWithAmount(Card attacker, int amount) {
        if( amount == 0 )
            return false; // no block
        
        List<String> restrictions = Lists.newArrayList(); 
        for ( String kw : attacker.getKeyword() ) {
            if ( kw.startsWith("CantBeBlockedByAmount") )
                restrictions.add(TextUtil.split(kw, ' ', 2)[1]);
        }
        for ( String res : restrictions ) {
            int operand = Integer.parseInt(res.substring(2));
            String operator = res.substring(0,2);
            if (Expressions.compare(amount, operator, operand) )
                return false;
        }

        return true;
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
        String cantAttackKw = null;

 
        for( String kw : c.getKeyword()) {
            if (kw.startsWith("CARDNAME can't attack if defending player controls an untapped creature with power")) {
                cantAttackKw = kw;
            }
        }

        // The keyword
        // "CARDNAME can't attack if defending player controls an untapped creature with power"
        // ... is present
        if (cantAttackKw != null) {
            final String[] asSeparateWords = cantAttackKw.trim().split(" ");

            if (asSeparateWords.length >= 15) {
                if (StringUtils.isNumeric(asSeparateWords[12])) {
                    powerLimit[0] = Integer.parseInt((asSeparateWords[12]).trim());

                    List<Card> list = defendingPlayer.getCreaturesInPlay();
                    list = CardLists.filter(list, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card ct) {
                            return (ct.isUntapped()
                                    && ((ct.getNetAttack() >= powerLimit[0] && asSeparateWords[14].contains("greater")) 
                                    ||  (ct.getNetAttack() <= powerLimit[0] && asSeparateWords[14].contains("less"))));
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
                if (!Iterables.any(list, CardPredicates.isColor(MagicColor.BLUE)))
                    return false;
            }
        }

        // The creature won't untap next turn
        if (c.isTapped() && !Untap.canUntap(c)) {
            return false;
        }
        final Game game = c.getGame();
        // CantBeActivated static abilities
        for (final Card ca : game.getCardsIn(ZoneType.listValueOf("Battlefield,Command"))) {
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
     * <p>
     * checkPropagandaEffects.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param bLast
     *            a boolean.
     */
    public static boolean checkPropagandaEffects(final Game game, final Card c, final Combat combat) {
        Cost attackCost = new Cost(ManaCost.ZERO, true);
        // Sort abilities to apply them in proper order
        for (Card card : game.getCardsIn(ZoneType.listValueOf("Battlefield,Command"))) {
            final ArrayList<StaticAbility> staticAbilities = card.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                Cost additionalCost = stAb.getAttackCost(c, combat.getDefenderByAttacker(c));
                if ( null != additionalCost )
                    attackCost.add(additionalCost);
            }
        }
        
        boolean isFree = attackCost.getTotalMana().isZero() && attackCost.isOnlyManaCost(); // true if needless to pay
        return isFree || c.getController().getController().payManaOptional(c, attackCost, null, "Pay additional cost to declare " + c + " an attacker", ManaPaymentPurpose.DeclareAttacker);
    }

    /**
     * <p>
     * This method checks triggered effects of attacking creatures, right before
     * defending player declares blockers.
     * </p>
     * @param game 
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public static void checkDeclaredAttacker(final Game game, final Card c, final Combat combat) {
        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Attacker", c);
        final List<Card> otherAttackers = combat.getAttackers();
        otherAttackers.remove(c);
        runParams.put("OtherAttackers", otherAttackers);
        runParams.put("Attacked", combat.getDefenderByAttacker(c));
        game.getTriggerHandler().runTrigger(TriggerType.Attacks, runParams, false);

        // Annihilator:
        if (!c.getDamageHistory().getCreatureAttackedThisCombat()) {
            for (final String key : c.getKeyword()) {
                if( !key.startsWith("Annihilator ") ) continue;
                final String[] k = key.split(" ", 2);
                final int a = Integer.valueOf(k[1]);

                final Ability ability = new Ability(c, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        this.api = ApiType.Sacrifice;
                        final Player opponent = combat.getDefendingPlayerRelatedTo(c);
                        //List<Card> list = AbilityUtils.filterListByType(opponent.getCardsIn(ZoneType.Battlefield), "Permanent", this);
                        final List<Card> list = opponent.getCardsIn(ZoneType.Battlefield);
                        List<Card> toSac = opponent.getController().choosePermanentsToSacrifice(this, a, a, list, "Card");

                        for(Card sacd : toSac) {
                            game.getAction().sacrifice(sacd, this);
                        }
                        
                    }
                };
                String sb = String.format("Annihilator - Defending player sacrifices %d permanents.", a);
                ability.setStackDescription(sb);
                ability.setDescription(sb);
                ability.setActivatingPlayer(c.getController());
                ability.setTrigger(true);

                game.getStack().add(ability);

            } // for
        } // creatureAttacked
          // Annihilator


        if (c.getName().equals("Witch-Maw Nephilim") && !c.getDamageHistory().getCreatureAttackedThisCombat()
                && (c.getNetAttack() >= 10)) {
            final Card charger = c;
            final Ability ability2 = new Ability(c, ManaCost.ZERO) {
                @Override
                public void resolve() {

                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -1703473800920781454L;

                        @Override
                        public void run() {
                            if (charger.isInPlay()) {
                                charger.removeIntrinsicKeyword("Trample");
                            }
                        }
                    }; // Command

                    if (charger.isInPlay()) {
                        charger.addIntrinsicKeyword("Trample");

                        game.getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve
            }; // ability

            final StringBuilder sb2 = new StringBuilder();
            sb2.append(c.getName()).append(" - gains trample until end of turn if its power is 10 or greater.");
            ability2.setStackDescription(sb2.toString());

            game.getStack().add(ability2);

        } // Witch-Maw Nephilim

        c.getDamageHistory().setCreatureAttackedThisCombat(true);
        c.getController().setAttackedWithCreatureThisTurn(true);
        c.getController().incrementAttackersDeclaredThisTurn();
    } // checkDeclareAttackers

    /**
     * <p>
     * checkDeclareBlockers.
     * </p>
     * @param game 
     * 
     * @param cl
     *            a {@link forge.CardList} object.
     */
    public static void checkDeclareBlockers(Game game, final List<Card> cl, Combat combat) {
        for (final Card c : cl) {
            if (!c.getDamageHistory().getCreatureBlockedThisCombat()) {
                for (final Ability ab : CardFactoryUtil.getBushidoEffects(c)) {
                    game.getStack().add(ab);
                }
                // Run triggers
                final HashMap<String, Object> runParams = new HashMap<String, Object>();
                runParams.put("Blocker", c);
                final Card attacker = combat.getAttackersBlockedBy(c).get(0);
                runParams.put("Attacker", attacker);
                game.getTriggerHandler().runTrigger(TriggerType.Blocks, runParams, false);
            }

            c.getDamageHistory().setCreatureBlockedThisCombat(true);
        } // for

    } // checkDeclareBlockers

    /**
     * <p>
     * checkBlockedAttackers.
     * </p>
     * @param game 
     * 
     * @param a
     *            a {@link forge.Card} object.
     * @param b
     *            a {@link forge.Card} object.
     */
    public static void checkBlockedAttackers(final Game game, final Card a, final List<Card> blockers) {
        if (blockers.isEmpty()) {
            return;
        }

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Attacker", a);
        runParams.put("Blockers", blockers);
        runParams.put("NumBlockers", blockers.size());
        game.getTriggerHandler().runTrigger(TriggerType.AttackerBlocked, runParams, false);

        if (!a.getDamageHistory().getCreatureGotBlockedThisCombat()) {
            // Bushido
            for (final Ability ab : CardFactoryUtil.getBushidoEffects(a)) {
                game.getStack().add(ab);
            }

            // Rampage
            final List<String> keywords = a.getKeyword();
            final Pattern p = Pattern.compile("Rampage [0-9]+");
            Matcher m;
            for (final String keyword : keywords) {
                m = p.matcher(keyword);
                if (m.find()) {
                    final String[] k = keyword.split(" ");
                    final int magnitude = Integer.valueOf(k[1]);
                    final int numBlockers = blockers.size();
                    if (numBlockers > 1) {
                        CombatUtil.executeRampageAbility(game, a, magnitude, numBlockers);
                    }
                } // find
            } // end Rampage
        }

        for (Card b : blockers) {
            if (a.hasKeyword("Flanking") && !b.hasKeyword("Flanking")) {
                int flankingMagnitude = 0;
    
                for (String kw : a.getKeyword()) {
                    if (kw.equals("Flanking")) {
                        flankingMagnitude++;
                    }
                }
                final int mag = flankingMagnitude;
                final Card blocker = b;
                final Ability ability2 = new Ability(b, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
    
                        final Command untilEOT = new Command() {
    
                            private static final long serialVersionUID = 7662543891117427727L;
    
                            @Override
                            public void run() {
                                if (blocker.isInPlay()) {
                                    blocker.addTempAttackBoost(mag);
                                    blocker.addTempDefenseBoost(mag);
                                }
                            }
                        }; // Command
    
                        if (blocker.isInPlay()) {
                            blocker.addTempAttackBoost(-mag);
                            blocker.addTempDefenseBoost(-mag);
    
                            game.getEndOfTurn().addUntil(untilEOT);
                            System.out.println("Flanking!");
                        }
                    } // resolve
    
                }; // ability
    
                final StringBuilder sb2 = new StringBuilder();
                sb2.append(b.getName()).append(" - gets -").append(mag).append("/-").append(mag).append(" until EOT.");
                ability2.setStackDescription(sb2.toString());
                ability2.setDescription(sb2.toString());
    
                game.getStack().add(ability2);
                Log.debug("Adding Flanking!");
            } // flanking

            b.addBlockedThisTurn(a);
            a.addBlockedByThisTurn(b);
        }

        a.getDamageHistory().setCreatureGotBlockedThisCombat(true);
    }

    /**
     * <p>
     * executeExaltedAbility.
     * </p>
     * @param game 
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param magnitude
     *            a int.
     */
    public static void executeExaltedAbility(final Game game, final Card c, final int magnitude, final Card host) {
        final Card crd = c;
        Ability ability;
        // This really should be a trigger on the stack

        for (int i = 0; i < magnitude; i++) {
            ability = new Ability(host, ManaCost.ZERO) {
                @Override
                public void resolve() {
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = 1497565871061029469L;

                        @Override
                        public void run() {
                            if (crd.isInPlay()) {
                                crd.addTempAttackBoost(-1);
                                crd.addTempDefenseBoost(-1);
                            }
                        }
                    }; // Command

                    if (crd.isInPlay()) {
                        crd.addTempAttackBoost(1);
                        crd.addTempDefenseBoost(1);

                        game.getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve

            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append(host).append(" - Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)");
            
            sb.append(" [Attacker: ").append(c).append("]");
            ability.setStackDescription(sb.toString());
            ability.setDescription(sb.toString());
            ability.setActivatingPlayer(host.getController());
            ability.setTrigger(true);

            game.getStack().addSimultaneousStackEntry(ability);
        }
    }

    /**
     * executes Rampage abilities for a given card.
     * @param game 
     * 
     * @param c
     *            the card to add rampage bonus to
     * @param magnitude
     *            the magnitude of rampage (ie Rampage 2 means magnitude should
     *            be 2)
     * @param numBlockers
     *            - the number of creatures blocking this rampaging creature
     */
    private static void executeRampageAbility(final Game game, final Card c, final int magnitude, final int numBlockers) {
        final Card crd = c;
        final int pump = magnitude;
        Ability ability;

        // numBlockers -1 since it is for every creature beyond the first
        for (int i = 0; i < (numBlockers - 1); i++) {
            ability = new Ability(c, ManaCost.ZERO) {
                @Override
                public void resolve() {
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -3215615538474963181L;

                        @Override
                        public void run() {
                            if (crd.isInPlay()) {
                                crd.addTempAttackBoost(-pump);
                                crd.addTempDefenseBoost(-pump);
                            }
                        }
                    }; // Command

                    if (crd.isInPlay()) {
                        crd.addTempAttackBoost(pump);
                        crd.addTempDefenseBoost(pump);

                        game.getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve

            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append(c).append(" - (Rampage) gets +").append(pump).append("/+").append(pump).append(" until EOT.");
            ability.setStackDescription(sb.toString());

            game.getStack().add(ability);
        }
    }

} // end class CombatUtil
