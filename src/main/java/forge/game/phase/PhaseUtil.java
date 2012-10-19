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

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates.Presets;
import forge.Singletons;
import forge.card.trigger.TriggerType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;
import forge.gui.match.controllers.CMessage;
import forge.gui.match.nonsingleton.VField.PhaseLabel;


/**
 * <p>
 * PhaseUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class PhaseUtil {
    // ******* UNTAP PHASE *****
    /**
     * <p>
     * skipUntap.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    private static boolean skipUntap(final Player p) {

        if (p.hasKeyword("Skip your next untap step.")) {
            p.removeKeyword("Skip your next untap step.");
            return true;
        }
        if (p.hasKeyword("Skip the untap step of this turn.")) {
            return true;
        }

        if (Singletons.getModel().getGame().isCardInPlay("Sands of Time") || Singletons.getModel().getGame().isCardInPlay("Stasis")) {
            return true;
        }

        if (p.skipNextUntap()) {
            p.setSkipNextUntap(false);
            return true;
        }

        return false;
    }

    /**
     * <p>
     * handleUntap.
     * </p>
     */
    public static void handleUntap() {
        final PhaseHandler ph = Singletons.getModel().getGame().getPhaseHandler();
        final Player turn = ph.getPlayerTurn();

        Singletons.getModel().getGame().notifyNextTurn();
        CMessage.SINGLETON_INSTANCE.updateGameInfo(Singletons.getModel().getMatch());

        Singletons.getModel().getGame().getCombat().reset();
        Singletons.getModel().getGame().getCombat().setAttackingPlayer(turn);
        Singletons.getModel().getGame().getCombat().setDefendingPlayer(turn.getOpponent());

        // Tokens starting game in play now actually suffer from Sum. Sickness again
        final List<Card> list = turn.getCardsIncludePhasingIn(ZoneType.Battlefield);
        for (final Card c : list) {
            if (turn.getTurn() > 0 || !c.isStartsGameInPlay()) {
                c.setSickness(false);
            }
        }
        turn.incrementTurn();

        Singletons.getModel().getGame().getAction().resetActivationsPerTurn();

        final List<Card> lands = CardLists.filter(turn.getLandsInPlay(), Presets.UNTAPPED);
        turn.setNumPowerSurgeLands(lands.size());

        // anything before this point happens regardless of whether the Untap
        // phase is skipped

        if (PhaseUtil.skipUntap(turn)) {
            Singletons.getModel().getGame().getPhaseHandler().setNeedToNextPhase(true);
            return;
        }

        Singletons.getModel().getGame().getUntap().executeUntil(turn);
        Singletons.getModel().getGame().getUntap().executeAt();

        // otherwise land seems to stay tapped when it is really untapped
        //AllZone.getHumanPlayer().getZone(ZoneType.Battlefield).updateObservers();

        Singletons.getModel().getGame().getPhaseHandler().setNeedToNextPhase(true);
    }

    // ******* UPKEEP PHASE *****
    /**
     * <p>
     * handleUpkeep.
     * </p>
     */
    public static void handleUpkeep() {
        final Player turn = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        if (PhaseUtil.skipUpkeep()) {
            // Slowtrips all say "on the next turn's upkeep" if there is no
            // upkeep next turn, the trigger will never occur.
            turn.clearSlowtripList();
            turn.getOpponent().clearSlowtripList();
            Singletons.getModel().getGame().getPhaseHandler().setNeedToNextPhase(true);
            return;
        }

        Singletons.getModel().getGame().getUpkeep().executeUntil(turn);
        Singletons.getModel().getGame().getUpkeep().executeAt();
    }

    /**
     * <p>
     * skipUpkeep.
     * </p>
     * 
     * @return a boolean.
     */
    public static boolean skipUpkeep() {
        if (Singletons.getModel().getGame().isCardInPlay("Eon Hub")) {
            return true;
        }

        final Player turn = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        if ((turn.getCardsIn(ZoneType.Hand).size() == 0) && turn.isCardInPlay("Gibbering Descent")) {
            return true;
        }

        return false;
    }

    // ******* DRAW PHASE *****
    /**
     * <p>
     * handleDraw.
     * </p>
     */
    public static void handleDraw() {
        final Player playerTurn = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        if (PhaseUtil.skipDraw(playerTurn)) {
            Singletons.getModel().getGame().getPhaseHandler().setNeedToNextPhase(true);
            return;
        }

        playerTurn.drawCards(1, true);
    }

    /**
     * <p>
     * skipDraw.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    private static boolean skipDraw(final Player player) {
        // starting player skips his draw
        if (Singletons.getModel().getGame().getPhaseHandler().getTurn() == 1) {
            return true;
        }

        if (player.hasKeyword("Skip your next draw step.")) {
            player.removeKeyword("Skip your next draw step.");
            return true;
        }

        if (player.hasKeyword("Skip your draw step.")) {
            return true;
        }

        return false;
    }

    // ********* Declare Attackers ***********

    /**
     * <p>
     * verifyCombat.
     * </p>
     */
    public static void verifyCombat() {
        Singletons.getModel().getGame().getCombat().verifyCreaturesInPlay();
    }

    /**
     * <p>
     * handleCombatBegin.
     * </p>
     */
    public static void handleCombatBegin() {
        final Player playerTurn = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        if (PhaseUtil.skipCombat(playerTurn)) {
            Singletons.getModel().getGame().getPhaseHandler().setNeedToNextPhase(true);
            return;
        }
    }

    /**
     * <p>
     * handleCombatDeclareAttackers.
     * </p>
     */
    public static void handleCombatDeclareAttackers() {
        final Player playerTurn = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        if (PhaseUtil.skipCombat(playerTurn)) {
            Singletons.getModel().getGame().getPhaseHandler().setNeedToNextPhase(true);
            playerTurn.removeKeyword("Skip your next combat phase.");
            return;
        }
    }

    /**
     * <p>
     * skipCombat.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    private static boolean skipCombat(final Player player) {

        if (player.hasKeyword("Skip your next combat phase.")) {
            return true;
        }
        if (player.hasKeyword("Skip your combat phase.")) {
            return true;
        }
        if (player.hasKeyword("Skip all combat phases of your next turn.")) {
            player.removeKeyword("Skip all combat phases of your next turn.");
            player.addKeyword("Skip all combat phases of this turn.");
            return true;
        }
        if (player.hasKeyword("Skip all combat phases of this turn.")) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * handleDeclareAttackers.
     * </p>
     */
    public static void handleDeclareAttackers() {
        PhaseUtil.verifyCombat();

        // Handles removing cards like Mogg Flunkies from combat if group attack didn't occur
        final List<Card> filterList = Singletons.getModel().getGame().getCombat().getAttackerList();
        for (Card c : filterList) {
            if (c.hasKeyword("CARDNAME can't attack or block alone.") && c.isAttacking()) {
                if (Singletons.getModel().getGame().getCombat().getAttackers().size() < 2) {
                    Singletons.getModel().getGame().getCombat().removeFromCombat(c);
                }
            }
        }

        final List<Card> list = Singletons.getModel().getGame().getCombat().getAttackerList();

        // TODO move propaganda to happen as the Attacker is Declared
        // Remove illegal Propaganda attacks first only for attacking the Player

        final int size = list.size();
        for (int i = 0; i < size; i++) {
            final Card c = list.get(i);
            final boolean last = (i == (size - 1));
            CombatUtil.checkPropagandaEffects(c, last);
        }
    }

    /**
     * <p>
     * handleAttackingTriggers.
     * </p>
     */
    public static void handleAttackingTriggers() {
        final List<Card> list = Singletons.getModel().getGame().getCombat().getAttackerList();
        Singletons.getModel().getGame().getStack().freezeStack();
        // Then run other Attacker bonuses
        // check for exalted:
        if (list.size() == 1) {
            final Player attackingPlayer = Singletons.getModel().getGame().getCombat().getAttackingPlayer();
            int exaltedMagnitude = 0;
            for (Card card : attackingPlayer.getCardsIn(ZoneType.Battlefield)) {
                exaltedMagnitude += card.getKeywordAmount("Exalted");
            }

            if (exaltedMagnitude > 0) {
                CombatUtil.executeExaltedAbility(list.get(0), exaltedMagnitude);
                // Make sure exalted effects get applied only once per combat
            }

        }

        Singletons.getModel().getGame().getGameLog().add("Combat", CombatUtil.getCombatAttackForLog(), 1);

        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Attackers", list);
        runParams.put("AttackingPlayer", Singletons.getModel().getGame().getCombat().getAttackingPlayer());
        Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.AttackersDeclared, runParams);

        for (final Card c : list) {
            CombatUtil.checkDeclareAttackers(c);
        }
        Singletons.getModel().getGame().getStack().unfreezeStack();
    }

    /**
     * <p>
     * handleDeclareBlockers.
     * </p>
     */
    public static void handleDeclareBlockers() {
        PhaseUtil.verifyCombat();

        // Handles removing cards like Mogg Flunkies from combat if group block didn't occur
        final List<Card> filterList = Singletons.getModel().getGame().getCombat().getAllBlockers();
        for (Card c : filterList) {
            if (c.hasKeyword("CARDNAME can't attack or block alone.") && c.isBlocking()) {
                if (Singletons.getModel().getGame().getCombat().getAllBlockers().size() < 2) {
                    Singletons.getModel().getGame().getCombat().undoBlockingAssignment(c);
                }
            }
        }

        Singletons.getModel().getGame().getStack().freezeStack();

        Singletons.getModel().getGame().getCombat().setUnblocked();

        List<Card> list = new ArrayList<Card>();
        list.addAll(Singletons.getModel().getGame().getCombat().getAllBlockers());

        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !c.getDamageHistory().getCreatureBlockedThisCombat();
            }
        });

        final List<Card> attList = Singletons.getModel().getGame().getCombat().getAttackerList();

        CombatUtil.checkDeclareBlockers(list);

        for (final Card a : attList) {
            final List<Card> blockList = Singletons.getModel().getGame().getCombat().getBlockers(a);
            for (final Card b : blockList) {
                CombatUtil.checkBlockedAttackers(a, b);
            }
        }

        Singletons.getModel().getGame().getStack().unfreezeStack();

        Singletons.getModel().getGame().getGameLog().add("Combat", CombatUtil.getCombatBlockForLog(), 1);
    }

    // ***** Combat Utility **********
    // TODO: the below functions should be removed and the code blocks that use
    // them should instead use SpellAbilityRestriction
    /**
     * <p>
     * isBeforeAttackersAreDeclared.
     * </p>
     * 
     * @return a boolean.
     */
    public static boolean isBeforeAttackersAreDeclared() {
        final PhaseType phase = Singletons.getModel().getGame().getPhaseHandler().getPhase();
        return phase == PhaseType.UNTAP || phase == PhaseType.UPKEEP || phase == PhaseType.DRAW
            || phase == PhaseType.MAIN1 || phase == PhaseType.COMBAT_BEGIN;
    }

    /**
     * Retrieves and visually activates phase label for appropriate phase and
     * player.
     * 
     * @param s
     *            &emsp; Phase state
     */
    public static void visuallyActivatePhase(final PhaseType s) {
        PhaseLabel lbl = null;
        final Player p = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        final CMatchUI t = CMatchUI.SINGLETON_INSTANCE;

        // Index of field; computer is 1, human is 0
        int i = p.isComputer() ? 1 : 0;

        switch(s) {
            case UPKEEP:
                lbl = t.getFieldControls().get(i).getView().getLblUpkeep();
                break;
           case DRAW:
                lbl = t.getFieldControls().get(i).getView().getLblDraw();
                break;
           case MAIN1:
                lbl = t.getFieldControls().get(i).getView().getLblMain1();
                break;
           case COMBAT_BEGIN:
                lbl = t.getFieldControls().get(i).getView().getLblBeginCombat();
                break;
           case COMBAT_DECLARE_ATTACKERS:
                lbl = t.getFieldControls().get(i).getView().getLblDeclareAttackers();
                break;
           case COMBAT_DECLARE_BLOCKERS:
                lbl = t.getFieldControls().get(i).getView().getLblDeclareBlockers();
                break;
           case COMBAT_DAMAGE:
                lbl = t.getFieldControls().get(i).getView().getLblCombatDamage();
                break;
           case COMBAT_FIRST_STRIKE_DAMAGE:
                lbl = t.getFieldControls().get(i).getView().getLblFirstStrike();
                break;
           case COMBAT_END:
                lbl = t.getFieldControls().get(i).getView().getLblEndCombat();
                break;
           case MAIN2:
                lbl = t.getFieldControls().get(i).getView().getLblMain2();
                break;
           case END_OF_TURN:
                lbl = t.getFieldControls().get(i).getView().getLblEndTurn();
                break;
           case CLEANUP:
                lbl = t.getFieldControls().get(i).getView().getLblCleanup();
                break;
            default:
                return;
        }

        t.resetAllPhaseButtons();
        lbl.setActive(true);
    }
}
