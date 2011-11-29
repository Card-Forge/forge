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
package forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import forge.Constant.Zone;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.AbilityFactoryAttach;
import forge.card.abilityfactory.AbilityFactoryCharm;
import forge.card.cardfactory.CardFactoryInterface;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostPayment;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaPool;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRequirements;
import forge.card.spellability.Target;
import forge.card.spellability.TargetSelection;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.Trigger;
import forge.deck.Deck;
import forge.game.GameEndReason;
import forge.game.GameSummary;
import forge.game.GameType;
import forge.gui.GuiUtils;
import forge.gui.input.InputMulligan;
import forge.gui.input.InputPayManaCost;
import forge.gui.input.InputPayManaCostAbility;
import forge.gui.input.InputPayManaCostUtil;
import forge.item.CardPrinted;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang.GameAction.GameActionText;
import forge.quest.gui.QuestWinLoseHandler;
import forge.quest.gui.main.QuestEvent;
import forge.view.match.ViewTopLevel;
import forge.view.toolbox.WinLoseFrame;

/**
 * <p>
 * GameAction class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GameAction {

    /**
     * This variable prevents WinLose dialog from popping several times, ie on
     * each state effect check after a win.
     */
    private boolean canShowWinLose = true;

    /**
     * <p>
     * resetActivationsPerTurn.
     * </p>
     */
    public final void resetActivationsPerTurn() {
        final CardList all = AllZoneUtil.getCardsInGame();

        // Reset Activations per Turn
        for (final Card card : all) {
            for (final SpellAbility sa : card.getAllSpellAbilities()) {
                sa.getRestrictions().resetTurnActivations();
            }
        }
    }

    /**
     * <p>
     * changeZone.
     * </p>
     * 
     * @param prev
     *            a {@link forge.PlayerZone} object.
     * @param zone
     *            a {@link forge.PlayerZone} object.
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public static Card changeZone(final PlayerZone prev, final PlayerZone zone, final Card c) {
        if ((prev == null) && !c.isToken()) {
            zone.add(c);
            return c;
        }

        boolean suppress;
        if ((prev == null) && !c.isToken()) {
            suppress = true;
        } else if (c.isToken()) {
            suppress = false;
        } else {
            suppress = prev.equals(zone);
        }

        Card copied = null;
        Card lastKnownInfo = null;

        // Don't copy Tokens, Cards staying in same zone, or cards entering
        // Battlefield
        if (c.isToken() || suppress || zone.is(Constant.Zone.Battlefield) || zone.is(Constant.Zone.Stack)) {
            lastKnownInfo = c;
            copied = c;
        } else {
            if (c.isInAlternateState()) {
                c.setState("Original");
            }
            if (c.isCloned()) {
                c.switchStates("Cloner", "Original");
                c.setState("Original");
            }

            copied = AllZone.getCardFactory().copyCard(c);
            lastKnownInfo = CardUtil.getLKICopy(c);

            copied.setUnearthed(c.isUnearthed());

            copied.setTapped(false);
        }

        if (c.wasSuspendCast()) {
            copied = GameAction.addSuspendTriggers(c);
        }

        for (final Trigger trigger : c.getTriggers()) {
            trigger.setHostCard(copied);
        }

        if (suppress) {
            AllZone.getTriggerHandler().suppressMode("ChangesZone");
        }

        zone.add(copied);

        // Tokens outside the battlefield disappear immideately.
        if (copied.isToken() && !zone.is(Constant.Zone.Battlefield)) {
            zone.remove(copied);
        }

        if (prev != null) {
            if (prev.is(Constant.Zone.Battlefield) && c.isCreature()) {
                AllZone.getCombat().removeFromCombat(c);
            }

            prev.remove(c);
        }

        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", lastKnownInfo);
        if (prev != null) {
            runParams.put("Origin", prev.getZoneType().name());
        } else {
            runParams.put("Origin", null);
        }
        runParams.put("Destination", zone.getZoneType().name());
        AllZone.getTriggerHandler().runTrigger("ChangesZone", runParams);
        // AllZone.getStack().chooseOrderOfSimultaneousStackEntryAll();

        if (suppress) {
            AllZone.getTriggerHandler().clearSuppression("ChangesZone");
        }

        /*
         * if (!(c.isToken() || suppress || zone.is(Constant.Zone.Battlefield))
         * && !zone.is(Constant.Zone.Battlefield)) copied =
         * AllZone.getCardFactory().copyCard(copied);
         */
        // remove all counters from the card if destination is not the
        // battlefield
        // UNLESS we're dealing with Skullbriar, the Walking Grave
        if (!zone.is(Constant.Zone.Battlefield)
                && !(c.getName().equals("Skullbriar, the Walking Grave") && !zone.is(Constant.Zone.Hand) && !zone
                        .is(Constant.Zone.Library))) {
            copied.clearCounters();
        }

        copied.setTimestamp(AllZone.getNextTimestamp());

        return copied;
    }

    /**
     * <p>
     * moveTo.
     * </p>
     * 
     * @param zone
     *            a {@link forge.PlayerZone} object.
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveTo(final PlayerZone zone, Card c) {
        // Ideally move to should never be called without a prevZone
        // Remove card from Current Zone, if it has one
        final PlayerZone prev = AllZone.getZoneOf(c);
        // String prevName = prev != null ? prev.getZoneName() : "";

        if (c.hasKeyword("If CARDNAME would leave the battlefield, exile it instead of putting it anywhere else.")
                && !zone.is(Constant.Zone.Exile)) {
            final PlayerZone removed = c.getOwner().getZone(Constant.Zone.Exile);
            c.removeExtrinsicKeyword("If CARDNAME would leave the battlefield, "
                    + "exile it instead of putting it anywhere else.");
            return this.moveTo(removed, c);
        }

        // Card lastKnownInfo = c;

        c = GameAction.changeZone(prev, zone, c);

        if (c.isAura() && zone.is(Constant.Zone.Battlefield) && ((prev == null) || !prev.is(Constant.Zone.Stack))) {
            // TODO Need a way to override this for Abilities that put Auras
            // into play attached to things
            AbilityFactoryAttach.attachAuraOnIndirectEnterBattlefield(c);
        }

        return c;
    }

    /**
     * <p>
     * moveToPlayFromHand.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToPlayFromHand(Card c) {
        // handles the case for Clone, etc where prev was null

        final PlayerZone hand = c.getOwner().getZone(Constant.Zone.Hand);
        final PlayerZone play = c.getController().getZone(Constant.Zone.Battlefield);

        c = GameAction.changeZone(hand, play, c);

        return c;
    }

    /*
     * public void changeController(CardList list, Player oldController, Player
     * newController) { if (oldController.equals(newController)) return;
     * 
     * // Consolidating this code for now. In the future I want moveTo to handle
     * this garbage PlayerZone oldBattlefield =
     * oldController.getZone(Constant.Zone.Battlefield); PlayerZone
     * newBattlefield = newController.getZone(Constant.Zone.Battlefield);
     * 
     * AllZone.getTriggerHandler().suppressMode("ChangesZone");
     * ((PlayerZone_ComesIntoPlay)
     * AllZone.getHumanPlayer().getZone(Zone.Battlefield)).setTriggers(false);
     * ((PlayerZone_ComesIntoPlay)
     * AllZone.getComputerPlayer().getZone(Zone.Battlefield
     * )).setTriggers(false); //so "enters the battlefield" abilities don't
     * trigger
     * 
     * for (Card c : list) { int turnInZone = c.getTurnInZone();
     * oldBattlefield.remove(c); c.setController(newController);
     * newBattlefield.add(c); //set summoning sickness c.setSickness(true);
     * c.setTurnInZone(turnInZone); // The number of turns in the zone should
     * not change if (c.isCreature()) AllZone.getCombat().removeFromCombat(c); }
     * 
     * AllZone.getTriggerHandler().clearSuppression("ChangesZone");
     * ((PlayerZone_ComesIntoPlay)
     * AllZone.getHumanPlayer().getZone(Zone.Battlefield)).setTriggers(true);
     * ((PlayerZone_ComesIntoPlay)
     * AllZone.getComputerPlayer().getZone(Zone.Battlefield)).setTriggers(true);
     * }
     */

    /**
     * Controller change zone correction.
     * 
     * @param c
     *            a Card object
     */
    public final void controllerChangeZoneCorrection(final Card c) {
        System.out.println("Correcting zone for " + c.toString());
        final PlayerZone oldBattlefield = AllZone.getZoneOf(c);
        final PlayerZone newBattlefield = c.getController().getZone(oldBattlefield.getZoneType());

        if ((oldBattlefield == null) || (newBattlefield == null)) {
            return;
        }

        AllZone.getTriggerHandler().suppressMode("ChangesZone");
        ((PlayerZoneComesIntoPlay) AllZone.getHumanPlayer().getZone(Zone.Battlefield)).setTriggers(false);
        ((PlayerZoneComesIntoPlay) AllZone.getComputerPlayer().getZone(Zone.Battlefield)).setTriggers(false);

        final int tiz = c.getTurnInZone();

        oldBattlefield.remove(c);
        newBattlefield.add(c);
        c.setSickness(true);
        if (c.hasStartOfKeyword("Echo")) {
            c.addIntrinsicKeyword("(Echo unpaid)");
        }
        AllZone.getCombat().removeFromCombat(c);

        c.setTurnInZone(tiz);

        AllZone.getTriggerHandler().clearSuppression("ChangesZone");
        ((PlayerZoneComesIntoPlay) AllZone.getHumanPlayer().getZone(Zone.Battlefield)).setTriggers(true);
        ((PlayerZoneComesIntoPlay) AllZone.getComputerPlayer().getZone(Zone.Battlefield)).setTriggers(true);
    }

    /**
     * <p>
     * moveToStack.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToStack(final Card c) {
        final PlayerZone stack = AllZone.getStackZone();
        return this.moveTo(stack, c);
    }

    /**
     * <p>
     * moveToGraveyard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToGraveyard(Card c) {
        final PlayerZone origZone = AllZone.getZoneOf(c);
        final Player owner = c.getOwner();
        final PlayerZone grave = owner.getZone(Constant.Zone.Graveyard);
        final PlayerZone exile = owner.getZone(Constant.Zone.Exile);
        final CardList ownerBoard = owner.getCardsIn(Constant.Zone.Battlefield);
        final CardList opponentsBoard = owner.getOpponent().getCardsIn(Constant.Zone.Battlefield);

        if (c.getName().equals("Nissa's Chosen") && origZone.is(Constant.Zone.Battlefield)) {
            return this.moveToLibrary(c, -1);
        }

        for (final Card card : opponentsBoard) {
            if (card.hasKeyword("If a card would be put into an opponent's "
                    + "graveyard from anywhere, exile it instead.")) {
                return this.moveTo(exile, c);
            }
        }

        for (final Card card : ownerBoard) {
            if (card.hasKeyword("If a card would be put into your graveyard from anywhere, exile it instead.")) {
                return this.moveTo(exile, c);
            }
        }

        if (c.hasKeyword("If CARDNAME would be put into a graveyard, exile it instead.")) {
            return this.moveTo(exile, c);
        }

        if (c.hasKeyword("If CARDNAME is put into a graveyard this turn, its controller gets a poison counter.")) {
            c.getController().addPoisonCounters(1);
        }

        // must put card in OWNER's graveyard not controller's
        c = this.moveTo(grave, c);

        // Recover keyword
        if (c.isCreature() && origZone.is(Constant.Zone.Battlefield)) {
            for (final Card recoverable : c.getOwner().getCardsIn(Zone.Graveyard)) {
                if (recoverable.hasStartOfKeyword("Recover") && !recoverable.equals(c)) {
                    final SpellAbility abRecover = new Ability(recoverable, "0") {
                        @Override
                        public void resolve() {
                            AllZone.getGameAction().moveToHand(recoverable);
                        }

                        @Override
                        public String getStackDescription() {
                            final StringBuilder sd = new StringBuilder(recoverable.getName());
                            sd.append(" - Recover.");

                            return sd.toString();
                        }
                    };

                    final Command notPaid = new Command() {
                        private static final long serialVersionUID = 5812397026869965462L;

                        @Override
                        public void execute() {
                            AllZone.getGameAction().exile(recoverable);
                        }
                    };

                    abRecover.setCancelCommand(notPaid);
                    abRecover.setTrigger(true);

                    final String recoverCost = recoverable.getKeyword().get(recoverable.getKeywordPosition("Recover"))
                            .split(":")[1];
                    final Cost abCost = new Cost(recoverCost, recoverable.getName(), false);
                    abRecover.setPayCosts(abCost);

                    final StringBuilder question = new StringBuilder("Recover ");
                    question.append(recoverable.getName());
                    question.append("(");
                    question.append(recoverable.getUniqueNumber());
                    question.append(")");
                    question.append("?");

                    boolean shouldRecoverForAI = false;
                    boolean shouldRecoverForHuman = false;

                    if (owner.isHuman()) {
                        shouldRecoverForHuman = GameActionUtil.showYesNoDialog(recoverable, question.toString());
                    } else if (c.getOwner().isComputer()) {
                        shouldRecoverForAI = ComputerUtil.canPayCost(abRecover);
                    }

                    if (shouldRecoverForHuman) {
                        AllZone.getStack().addSimultaneousStackEntry(abRecover);
                        // AllZone.getGameAction().playSpellAbility(abRecover);
                    } else if (shouldRecoverForAI) {
                        AllZone.getStack().addSimultaneousStackEntry(abRecover);
                        // ComputerUtil.playStack(abRecover);
                    }

                    if (!grave.hasChanged()) {
                        // If the controller declined Recovery or didn't pay the
                        // cost, exile the recoverable
                    }
                }
            }
        }
        return c;
    }

    /**
     * <p>
     * moveToHand.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToHand(final Card c) {
        final PlayerZone hand = c.getOwner().getZone(Constant.Zone.Hand);
        return this.moveTo(hand, c);
    }

    /**
     * <p>
     * moveToPlay.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToPlay(final Card c) {
        final PlayerZone play = c.getOwner().getZone(Constant.Zone.Battlefield);
        return this.moveTo(play, c);
    }

    /**
     * <p>
     * moveToPlay.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param p
     *            a {@link forge.Player} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToPlay(final Card c, final Player p) {
        // move to a specific player's Battlefield
        final PlayerZone play = p.getZone(Constant.Zone.Battlefield);
        return this.moveTo(play, c);
    }

    /**
     * <p>
     * moveToBottomOfLibrary.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToBottomOfLibrary(final Card c) {
        return this.moveToLibrary(c, -1);
    }

    /**
     * <p>
     * moveToLibrary.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToLibrary(final Card c) {
        return this.moveToLibrary(c, 0);
    }

    /**
     * <p>
     * moveToLibrary.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param libPosition
     *            a int.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToLibrary(Card c, int libPosition) {
        final PlayerZone p = AllZone.getZoneOf(c);
        final PlayerZone library = c.getOwner().getZone(Constant.Zone.Library);

        if (c.hasKeyword("If CARDNAME would leave the battlefield, exile it instead of putting it anywhere else.")) {
            final PlayerZone removed = c.getOwner().getZone(Constant.Zone.Exile);
            c.removeExtrinsicKeyword("If CARDNAME would leave the battlefield, "
                    + "exile it instead of putting it anywhere else.");
            return this.moveTo(removed, c);
        }

        if (p != null) {
            p.remove(c);
        }

        if (c.isToken()) {
            return c;
        }

        if (c.isInAlternateState()) {
            c.setState("Original");
        }

        if ((p != null) && p.is(Constant.Zone.Battlefield)) {
            c = AllZone.getCardFactory().copyCard(c);
        }

        c.clearCounters(); // remove all counters

        if ((libPosition == -1) || (libPosition > library.size())) {
            libPosition = library.size();
        }

        library.add(c, libPosition);
        return c;
    }

    /**
     * <p>
     * exile.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card exile(final Card c) {
        if (AllZoneUtil.isCardExiled(c)) {
            return c;
        }

        final PlayerZone removed = c.getOwner().getZone(Constant.Zone.Exile);

        return AllZone.getGameAction().moveTo(removed, c);
    }

    /**
     * Move to.
     * 
     * @param name
     *            the name
     * @param c
     *            the c
     * @return the card
     */
    public final Card moveTo(final Zone name, final Card c) {
        return this.moveTo(name, c, 0);
    }

    /**
     * <p>
     * moveTo.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param libPosition
     *            a int.
     * @return a {@link forge.Card} object.
     */
    public final Card moveTo(final Zone name, final Card c, final int libPosition) {
        // Call specific functions to set PlayerZone, then move onto moveTo
        if (name.equals(Constant.Zone.Hand)) {
            return this.moveToHand(c);
        } else if (name.equals(Constant.Zone.Library)) {
            return this.moveToLibrary(c, libPosition);
        } else if (name.equals(Constant.Zone.Battlefield)) {
            return this.moveToPlay(c);
        } else if (name.equals(Constant.Zone.Graveyard)) {
            return this.moveToGraveyard(c);
        } else if (name.equals(Constant.Zone.Exile)) {
            return this.exile(c);
        } else {
            return this.moveToStack(c);
        }
    }

    /**
     * <p>
     * discard_PutIntoPlayInstead.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void discardPutIntoPlayInstead(final Card c) {
        this.moveToPlay(c);

        if (c.getName().equals("Dodecapod")) {
            c.setCounter(Counters.P1P1, 2, false);
        }
    }

    /**
     * <p>
     * discard_madness.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void discardMadness(final Card c) {
        // Whenever a card with madness is discarded, you may cast it for it's
        // madness cost
        if (!c.hasMadness()) {
            return;
        }

        final Card madness = c;
        final Ability cast = new Ability(madness, madness.getMadnessCost()) {
            @Override
            public void resolve() {
                GameAction.this.playCardNoCost(madness);
                System.out.println("Madness cost paid");
            }
        };

        final StringBuilder sb = new StringBuilder();
        sb.append(madness.getName()).append(" - Cast via Madness");
        cast.setStackDescription(sb.toString());

        final Ability activate = new Ability(madness, "0") {
            @Override
            public void resolve() {
                // pay madness cost here.
                if (madness.getOwner().isHuman()) {
                    if (GameActionUtil.showYesNoDialog(madness, madness + " - Discarded. Pay Madness Cost?")) {
                        if (cast.getManaCost().equals("0")) {
                            AllZone.getStack().add(cast);
                        } else {
                            AllZone.getInputControl().setInput(new InputPayManaCost(cast));
                        }
                    }
                } else {
                    // computer will ALWAYS pay a madness cost if he has the
                    // mana.
                    ComputerUtil.playStack(cast);
                }
            }
        };

        final StringBuilder sbAct = new StringBuilder();
        sbAct.append(madness.getName()).append(" - Discarded. Pay Madness Cost?");
        activate.setStackDescription(sbAct.toString());

        AllZone.getStack().add(activate);
    }

    /**
     * <p>
     * checkEndGameSate.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean checkEndGameSate() {
        // Win / Lose
        final GameSummary game = AllZone.getGameInfo();
        boolean humanWins = false;
        boolean computerWins = false;
        final Player computer = AllZone.getComputerPlayer();
        final Player human = AllZone.getHumanPlayer();

        if (human.hasWon() || computer.hasLost()) { // Winning Conditions can be
                                                    // worth more than losing
                                                    // conditions
            // Human wins
            humanWins = true;

            if (human.getAltWin()) {
                game.end(GameEndReason.WinsGameSpellEffect, human.getName(), human.getWinConditionSource());
            } else {
                game.end(GameEndReason.AllOpponentsLost, human.getName(), null);
            }
        }

        if (computer.hasWon() || human.hasLost()) {
            if (humanWins) {
                // both players won/lost at the same time.
                game.end(GameEndReason.Draw, null, null);
            } else {
                computerWins = true;

                if (computer.getAltWin()) {
                    game.end(GameEndReason.WinsGameSpellEffect, computer.getName(), computer.getWinConditionSource());
                } else {
                    game.end(GameEndReason.AllOpponentsLost, computer.getName(), null);
                }

            }
        }

        final boolean isGameDone = humanWins || computerWins;
        if (isGameDone) {
            game.getPlayerRating(computer.getName()).setLossReason(computer.getLossState(),
                    computer.getLossConditionSource());
            game.getPlayerRating(human.getName()).setLossReason(human.getLossState(), human.getLossConditionSource());
            AllZone.getMatchState().addGamePlayed(game);
        }

        return isGameDone;
    }

    /**
     * <p>
     * checkStateEffects.
     * </p>
     */
    public final void checkStateEffects() {
        this.checkStateEffects(false);
    }

    /**
     * <p>
     * checkStateEffects.
     * </p>
     * 
     * @param force
     *            a boolean. States wether or not state effect checking should
     *            be forced, even if a spell is in the middle of resolving.
     */
    public final void checkStateEffects(final boolean force) {

        // sol(10/29) added for Phase updates, state effects shouldn't be
        // checked during Spell Resolution (except when persist-returning
        if (AllZone.getStack().getResolving() && !force) {
            return;
        }

        final boolean refreeze = AllZone.getStack().isFrozen();
        AllZone.getStack().setFrozen(true);

        if (Constant.Runtime.OLDGUI[0]) {
            final JFrame frame = (JFrame) AllZone.getDisplay();
            if (!frame.isDisplayable()) {
                return;
            }
        } else {
            final ViewTopLevel frame = (ViewTopLevel) AllZone.getDisplay();
            if (!frame.isDisplayable()) {
                return;
            }
        }

        if (this.canShowWinLose && this.checkEndGameSate()) {
            AllZone.getDisplay().savePrefs();
            // frame.setEnabled(false);
            // frame.dispose();

            // Gui_WinLose gwl = new Gui_WinLose(AllZone.getMatchState(),
            // AllZone.getQuestData(), AllZone.getQuestChallenge());

            // New WinLoseFrame below. Old Gui_WinLose above.
            // Old code should still work if any problems with new. Doublestrike
            // 02-10-11
            WinLoseFrame gwl;

            if (AllZone.getQuestData() != null) {
                gwl = new WinLoseFrame(new QuestWinLoseHandler());
            } else {
                gwl = new WinLoseFrame();
            }

            // gwl.setAlwaysOnTop(true);
            gwl.toFront();
            this.canShowWinLose = false;
            return;
        }

        // do this twice, sometimes creatures/permanents will survive when they
        // shouldn't
        for (int q = 0; q < 9; q++) {

            boolean checkAgain = false;

            // remove old effects
            AllZone.getStaticEffects().clearStaticEffects();

            // search for cards with static abilities
            final CardList allCards = AllZoneUtil.getCardsInGame();
            final CardList cardsWithStAbs = new CardList();
            for (final Card card : allCards) {
                final ArrayList<StaticAbility> staticAbilities = card.getStaticAbilities();
                if (!staticAbilities.isEmpty() && !card.isFaceDown()) {
                    cardsWithStAbs.add(card);
                }
            }

            cardsWithStAbs.reverse(); // roughly timestamp order

            // apply continuous effects
            for (int layer = 4; layer < 11; layer++) {
                for (final Card card : cardsWithStAbs) {
                    final ArrayList<StaticAbility> staticAbilities = card.getStaticAbilities();
                    for (final StaticAbility stAb : staticAbilities) {
                        if (stAb.getLayer() == layer) {
                            stAb.applyAbility("Continuous");
                        }
                    }
                }
            }

            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            AllZone.getTriggerHandler().runTrigger("Always", runParams);

            // card state effects like Glorious Anthem
            for (final String effect : AllZone.getStaticEffects().getStateBasedMap().keySet()) {
                final Command com = GameActionUtil.getCommands().get(effect);
                com.execute();
            }

            final CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
            Card c;

            final Iterator<Card> it = list.iterator();

            while (it.hasNext()) {
                c = it.next();

                if (c.isEquipped()) {
                    final CardList equipments = new CardList(c.getEquippedBy());
                    for (final Card equipment : equipments) {
                        if (!AllZoneUtil.isCardInPlay(equipment)) {
                            equipment.unEquipCard(c);
                            checkAgain = true;
                        }
                    }
                } // if isEquipped()

                if (c.isEquipping()) {
                    final Card equippedCreature = c.getEquipping().get(0);
                    if (!equippedCreature.isCreature() || !AllZoneUtil.isCardInPlay(equippedCreature)) {
                        c.unEquipCard(equippedCreature);
                        checkAgain = true;
                    }

                    // make sure any equipment that has become a creature stops
                    // equipping
                    if (c.isCreature()) {
                        c.unEquipCard(equippedCreature);
                        checkAgain = true;
                    }
                } // if isEquipping()

                if (c.isAura()) {
                    // Check if Card Aura is attached to is a legal target
                    final GameEntity entity = c.getEnchanting();
                    final SpellAbility sa = c.getSpellPermanent();
                    Target tgt = null;
                    if (sa != null) {
                        tgt = sa.getTarget();
                    }

                    if (entity instanceof Card) {
                        final Card perm = (Card) entity;
                        if (!AllZoneUtil.isCardInPlay(perm) || perm.hasProtectionFrom(c)
                                || perm.hasKeyword("CARDNAME can't be enchanted.")
                                || ((tgt != null) && !perm.isValid(tgt.getValidTgts(), c.getController(), c))) {
                            c.unEnchantEntity(perm);
                            this.moveToGraveyard(c);
                            checkAgain = true;
                        }
                    } else if (entity instanceof Player) {
                        final Player pl = (Player) entity;
                        boolean invalid = false;

                        if (tgt.canOnlyTgtOpponent() && !c.getController().getOpponent().isPlayer(pl)) {
                            invalid = true;
                        } else {
                            if (pl.hasProtectionFrom(c)) {
                                invalid = true;
                            }
                        }
                        if (invalid) {
                            c.unEnchantEntity(pl);
                            this.moveToGraveyard(c);
                            checkAgain = true;
                        }
                    }

                } // if isAura

                if (c.isCreature()) {
                    if ((c.getNetDefense() <= c.getDamage()) && !c.hasKeyword("Indestructible")) {
                        this.destroy(c);
                        // this is untested with instants and abilities but
                        // required for First Strike combat phase
                        AllZone.getCombat().removeFromCombat(c);
                        checkAgain = true;
                    } else if (c.getNetDefense() <= 0) {
                        // TODO This shouldn't be a destroy, and should happen
                        // before the damage check probably
                        this.destroy(c);
                        AllZone.getCombat().removeFromCombat(c);
                        checkAgain = true;
                    }
                }

            } // while it.hasNext()

            if (!checkAgain) {
                break; // do not continue the loop
            }

        } // for q=0;q<2

        this.destroyLegendaryCreatures();
        this.destroyPlaneswalkers();

        GameActionUtil.getStLandManaAbilities().execute();

        if (!refreeze) {
            AllZone.getStack().unfreezeStack();
        }
    } // checkStateEffects()

    /**
     * <p>
     * destroyPlaneswalkers.
     * </p>
     */
    private void destroyPlaneswalkers() {
        // get all Planeswalkers
        final CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield).getType("Planeswalker");

        Card c;
        for (int i = 0; i < list.size(); i++) {
            c = list.get(i);

            if (c.getCounters(Counters.LOYALTY) <= 0) {
                AllZone.getGameAction().moveToGraveyard(c);
            }

            final ArrayList<String> types = c.getType();
            for (final String type : types) {
                if (!CardUtil.isAPlaneswalkerType(type)) {
                    continue;
                }

                final CardList cl = list.getType(type);

                if (cl.size() > 1) {
                    for (final Card crd : cl) {
                        AllZone.getGameAction().moveToGraveyard(crd);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * destroyLegendaryCreatures.
     * </p>
     */
    private void destroyLegendaryCreatures() {
        final CardList a = AllZoneUtil.getCardsIn(Zone.Battlefield).getType("Legendary");

        while (!a.isEmpty() && !AllZoneUtil.isCardInPlay("Mirror Gallery")) {
            CardList b = AllZoneUtil.getCardsIn(Zone.Battlefield, a.get(0).getName());
            b = b.getType("Legendary");
            b = b.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return !c.isFaceDown();
                }
            });
            a.remove(0);
            if (1 < b.size()) {
                for (int i = 0; i < b.size(); i++) {
                    AllZone.getGameAction().sacrificeDestroy(b.get(i));
                }
            }
        }
    } // destroyLegendaryCreatures()

    /**
     * <p>
     * sacrifice.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean sacrifice(final Card c) {
        if (c.getName().equals("Mana Pool")) {
            System.out.println("Trying to sacrifice mana pool...");
            return false;
        }
        this.sacrificeDestroy(c);

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", c);
        AllZone.getTriggerHandler().runTrigger("Sacrificed", runParams);

        return true;
    }

    /**
     * <p>
     * destroyNoRegeneration.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean destroyNoRegeneration(final Card c) {
        if (!AllZoneUtil.isCardInPlay(c) || c.hasKeyword("Indestructible")) {
            return false;
        }

        if (c.isEnchanted()) {
            CardList list = new CardList(c.getEnchantedBy().toArray());
            list = list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card crd) {
                    return crd.hasKeyword("Totem armor");
                }
            });
            CardListUtil.sortCMC(list);

            if (list.size() != 0) {
                final Card crd;
                if (list.size() == 1) {
                    crd = list.get(0);
                } else {
                    if (c.getController().isHuman()) {
                        crd = GuiUtils.getChoiceOptional("Select totem armor to destroy", list.toArray());
                    } else {
                        crd = list.get(0);
                    }
                }

                final Card card = c;
                final AbilityStatic ability = new AbilityStatic(crd, "0") {
                    @Override
                    public void resolve() {
                        GameAction.this.destroy(crd);
                        card.setDamage(0);

                    }
                };

                final StringBuilder sb = new StringBuilder();
                sb.append(crd).append(" - Totem armor: destroy this aura.");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().add(ability);
                return false;
            }
        } // totem armor

        return this.sacrificeDestroy(c);
    }

    /**
     * <p>
     * addSuspendTriggers.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public static Card addSuspendTriggers(final Card c) {
        c.setSVar("HasteFromSuspend", "True");

        final Command intoPlay = new Command() {
            private static final long serialVersionUID = -4514610171270596654L;

            @Override
            public void execute() {
                if (AllZoneUtil.isCardInPlay(c) && c.isCreature()) {
                    c.addExtrinsicKeyword("Haste");
                }
            } // execute()
        };

        c.addComesIntoPlayCommand(intoPlay);

        final Command loseControl = new Command() {
            private static final long serialVersionUID = -4514610171270596654L;

            @Override
            public void execute() {
                if (c.getSVar("HasteFromSuspend").equals("True")) {
                    c.setSVar("HasteFromSuspend", "False");
                    c.removeExtrinsicKeyword("Haste");
                }
            } // execute()
        };

        c.addChangeControllerCommand(loseControl);
        c.addLeavesPlayCommand(loseControl);
        return c;
    }

    /**
     * <p>
     * sacrificeDestroy.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean sacrificeDestroy(final Card c) {
        if (!AllZoneUtil.isCardInPlay(c)) {
            return false;
        }

        final Player owner = c.getOwner();
        if (!(owner.isComputer() || owner.isHuman())) {
            throw new RuntimeException("GameAction : destroy() invalid card.getOwner() - " + c + " " + owner);
        }

        final boolean persist = (c.hasKeyword("Persist") && (c.getCounters(Counters.M1M1) == 0)) && !c.isToken();

        final Card newCard = this.moveToGraveyard(c);

        // Destroy needs to be called with Last Known Information
        c.destroy();

        // System.out.println("Card " + c.getName() +
        // " is getting sent to GY, and this turn it got damaged by: ");
        for (final Card crd : c.getReceivedDamageFromThisTurn().keySet()) {
            if (c.getReceivedDamageFromThisTurn().get(crd) > 0) {
                // System.out.println(crd.getName() );
                GameActionUtil.executeVampiricEffects(crd);
            }
        }

        if (persist) {
            final Card persistCard = newCard;
            final Ability persistAb = new Ability(persistCard, "0") {

                @Override
                public void resolve() {
                    if (AllZone.getZoneOf(persistCard).is(Constant.Zone.Graveyard)) {
                        final PlayerZone ownerPlay = persistCard.getOwner().getZone(Constant.Zone.Battlefield);
                        final Card card = GameAction.this.moveTo(ownerPlay, persistCard);
                        card.addCounter(Counters.M1M1, 1);
                    }
                }
            };
            persistAb.setStackDescription(newCard.getName() + " - Returning from Persist");
            AllZone.getStack().add(persistAb);
        }
        return true;
    } // sacrificeDestroy()

    /**
     * <p>
     * destroy.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean destroy(final Card c) {
        if (!AllZoneUtil.isCardInPlay(c)
                || (c.hasKeyword("Indestructible") && (!c.isCreature() || (c.getNetDefense() > 0)))) {
            return false;
        }

        if (c.canBeShielded() && (c.getShield() > 0)) {
            c.subtractShield();
            c.setDamage(0);
            c.tap();
            c.addRegeneratedThisTurn();
            AllZone.getCombat().removeFromCombat(c);
            return false;
        }

        if (c.isEnchanted()) {
            CardList list = new CardList(c.getEnchantedBy().toArray());
            list = list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card crd) {
                    return crd.hasKeyword("Totem armor");
                }
            });
            CardListUtil.sortCMC(list);

            if (list.size() != 0) {
                final Card crd;
                if (list.size() == 1) {
                    crd = list.get(0);
                } else {
                    if (c.getController().isHuman()) {
                        crd = GuiUtils.getChoiceOptional("Select totem armor to destroy", list.toArray());
                    } else {
                        crd = list.get(0);
                    }
                }

                c.setDamage(0);
                this.destroy(crd);
                System.out.println("Totem armor destroyed instead of original card");
                return false;
            }
        } // totem armor

        return this.sacrificeDestroy(c);
    }

    /**
     * <p>
     * newGame.
     * </p>
     * for Quest fantasy mode
     * 
     * @param humanDeck
     *            a {@link forge.deck.Deck} object.
     * @param computerDeck
     *            a {@link forge.deck.Deck} object.
     * @param human
     *            a {@link forge.CardList} object.
     * @param computer
     *            a {@link forge.CardList} object.
     * @param humanLife
     *            a int.
     * @param computerLife
     *            a int.
     * @param qe
     *            the qe
     */
    public final void newGame(final Deck humanDeck, final Deck computerDeck, final CardList human,
            final CardList computer, final int humanLife, final int computerLife, final QuestEvent qe) {
        this.newGame(humanDeck, computerDeck);

        AllZone.getComputerPlayer().setLife(computerLife, null);
        AllZone.getHumanPlayer().setLife(humanLife, null);

        for (final Card c : human) {

            AllZone.getHumanPlayer().getZone(Zone.Battlefield).add(c);
            c.setSickness(true);
        }

        for (final Card c : computer) {

            AllZone.getComputerPlayer().getZone(Zone.Battlefield).add(c);
            c.setSickness(true);
        }
        Constant.Quest.FANTASY_QUEST[0] = true;
    }

    private boolean startCut = false;

    /**
     * <p>
     * newGame.
     * </p>
     * 
     * @param humanDeck
     *            a {@link forge.deck.Deck} object.
     * @param computerDeck
     *            a {@link forge.deck.Deck} object.
     */
    public final void newGame(final Deck humanDeck, final Deck computerDeck) {
        // AllZone.getComputer() = new ComputerAI_Input(new
        // ComputerAI_General());
        Constant.Quest.FANTASY_QUEST[0] = false;

        AllZone.newGameCleanup();
        this.canShowWinLose = true;
        forge.card.trigger.Trigger.resetIDs();
        AllZone.getTriggerHandler().clearTriggerSettings();
        AllZone.getTriggerHandler().clearDelayedTrigger();

        // friendliness
        final CardFactoryInterface c = AllZone.getCardFactory();
        Card.resetUniqueNumber();
        final boolean canRandomFoil = Constant.Runtime.RANDOM_FOIL[0]
                && Constant.Runtime.getGameType().equals(GameType.Constructed);
        final Random generator = MyRandom.getRandom();
        for (final Entry<CardPrinted, Integer> stackOfCards : humanDeck.getMain()) {
            final CardPrinted cardPrinted = stackOfCards.getKey();
            for (int i = 0; i < stackOfCards.getValue(); i++) {

                final Card card = c.getCard(cardPrinted.getName(), AllZone.getHumanPlayer());
                card.setCurSetCode(cardPrinted.getSet());

                final int cntVariants = cardPrinted.getCard().getSetInfo(cardPrinted.getSet()).getCopiesCount();
                if (cntVariants > 1) {
                    card.setRandomPicture(generator.nextInt(cntVariants - 1) + 1);
                }

                card.setImageFilename(CardUtil.buildFilename(card));

                // Assign random foiling on approximately 1:20 cards
                if (cardPrinted.isFoil() || (canRandomFoil && MyRandom.percentTrue(5))) {
                    final int iFoil = MyRandom.getRandom().nextInt(9) + 1;
                    card.setFoil(iFoil);
                }

                AllZone.getHumanPlayer().getZone(Zone.Library).add(card);

                if (card.hasAlternateState()) {
                    if (card.isDoubleFaced()) {
                        card.setState("Transformed");
                    }
                    if (card.isFlip()) {
                        card.setState("Flipped");
                    }

                    card.setImageFilename(CardUtil.buildFilename(card));

                    card.setState("Original");
                }
            }
        }
        final ArrayList<String> rAICards = new ArrayList<String>();
        for (final Entry<CardPrinted, Integer> stackOfCards : computerDeck.getMain()) {
            final CardPrinted cardPrinted = stackOfCards.getKey();
            for (int i = 0; i < stackOfCards.getValue(); i++) {

                final Card card = c.getCard(cardPrinted.getName(), AllZone.getComputerPlayer());
                card.setCurSetCode(cardPrinted.getSet());

                final int cntVariants = cardPrinted.getCard().getSetInfo(cardPrinted.getSet()).getCopiesCount();
                if (cntVariants > 1) {
                    card.setRandomPicture(generator.nextInt(cntVariants - 1) + 1);
                }

                card.setImageFilename(CardUtil.buildFilename(card));

                // Assign random foiling on approximately 1:20 cards
                if (cardPrinted.isFoil() || (canRandomFoil && MyRandom.percentTrue(5))) {
                    final int iFoil = MyRandom.getRandom().nextInt(9) + 1;
                    card.setFoil(iFoil);
                }

                AllZone.getComputerPlayer().getZone(Zone.Library).add(card);

                if (card.getSVar("RemAIDeck").equals("True")) {
                    rAICards.add(card.getName());
                    // get card picture so that it is in the image cache
                    // ImageCache.getImage(card);
                }

                if (card.hasAlternateState()) {
                    if (card.isDoubleFaced()) {
                        card.setState("Transformed");
                    }
                    if (card.isFlip()) {
                        card.setState("Flipped");
                    }

                    card.setImageFilename(CardUtil.buildFilename(card));

                    card.setState("Original");
                }
            }
        }
        if (rAICards.size() > 0) {
            final StringBuilder sb = new StringBuilder(
                    "AI deck contains the following cards that it can't play or may be buggy:\n");
            for (int i = 0; i < rAICards.size(); i++) {
                sb.append(rAICards.get(i));
                if (((i % 4) == 0) && (i > 0)) {
                    sb.append("\n");
                } else if (i != (rAICards.size() - 1)) {
                    sb.append(", ");
                }
            }

            JOptionPane.showMessageDialog(null, sb.toString(), "", JOptionPane.INFORMATION_MESSAGE);

        }
        for (int i = 0; i < 100; i++) {
            AllZone.getHumanPlayer().shuffle();
        }

        // do this instead of shuffling Computer's deck
        final boolean smoothLand = Constant.Runtime.SMOOTH[0];

        if (smoothLand) {
            final Card[] c1 = this.smoothComputerManaCurve(AllZone.getComputerPlayer().getCardsIn(Zone.Library)
                    .toArray());
            AllZone.getComputerPlayer().getZone(Zone.Library).setCards(c1);
        } else {
            // WTF? (it was so before refactor)
            AllZone.getComputerPlayer().getZone(Zone.Library)
                    .setCards(AllZone.getComputerPlayer().getCardsIn(Zone.Library).toArray());
            AllZone.getComputerPlayer().shuffle();
        }

        // Only cut/coin toss if it's the first game of the match
        if (AllZone.getMatchState().getGamesPlayedCount() == 0) {
            // New code to determine who goes first. Delete this if it doesn't
            // work properly
            if (this.isStartCut()) {
                this.seeWhoPlaysFirst();
            } else {
                this.seeWhoPlaysFirstCoinToss();
            }
        } else if (AllZone.getMatchState().hasWonLastGame(AllZone.getHumanPlayer().getName())) {
            // if player won last, AI starts
            this.computerStartsGame();
        }

        for (int i = 0; i < 7; i++) {
            AllZone.getHumanPlayer().drawCard();
            AllZone.getComputerPlayer().drawCard();
        }

        // TODO ManaPool should be moved to Player and be represented in the
        // player panel
        final ManaPool mp = AllZone.getHumanPlayer().getManaPool();
        mp.setImageFilename("mana_pool");
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).add(mp);

        AllZone.getInputControl().setInput(new InputMulligan());
        Phase.setGameBegins(1);
    } // newGame()

    // this is where the computer cheats
    // changes AllZone.getComputerPlayer().getZone(Zone.Library)

    /**
     * <p>
     * smoothComputerManaCurve.
     * </p>
     * 
     * @param in
     *            an array of {@link forge.Card} objects.
     * @return an array of {@link forge.Card} objects.
     */
    final Card[] smoothComputerManaCurve(final Card[] in) {
        final CardList library = new CardList(in);
        library.shuffle();

        // remove all land, keep non-basicland in there, shuffled
        CardList land = library.getType("Land");
        for (int i = 0; i < land.size(); i++) {
            if (land.get(i).isLand()) {
                library.remove(land.get(i));
            }
        }

        // non-basic lands are removed, because the computer doesn't seem to
        // effectively use them very well
        land = this.threadLand(land);

        try {
            // mana weave, total of 7 land
            // The Following have all been reduced by 1, to account for the
            // computer starting first.
            library.add(6, land.get(0));
            library.add(7, land.get(1));
            library.add(8, land.get(2));
            library.add(9, land.get(3));
            library.add(10, land.get(4));

            library.add(12, land.get(5));
            library.add(15, land.get(6));
        } catch (final IndexOutOfBoundsException e) {
            System.err.println("Error: cannot smooth mana curve, not enough land");
            return in;
        }

        // add the rest of land to the end of the deck
        for (int i = 0; i < land.size(); i++) {
            if (!library.contains(land.get(i))) {
                library.add(land.get(i));
            }
        }

        // check
        for (int i = 0; i < library.size(); i++) {
            System.out.println(library.get(i));
        }

        return library.toArray();
    } // smoothComputerManaCurve()

    // non-basic lands are removed, because the computer doesn't seem to
    // effectively used them very well

    /**
     * <p>
     * threadLand.
     * </p>
     * 
     * @param in
     *            a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList threadLand(final CardList in) {
        // String[] basicLand = {"Forest", "Swamp", "Mountain", "Island",
        // "Plains"}; //unused

        // Thread stuff with as large a spread of colors as possible:
        final String[] allLand = { "Bayou", "Volcanic Island", "Savannah", "Badlands", "Tundra", "Taiga",
                "Underground Sea", "Plateau", "Tropical Island", "Scrubland", "Overgrown Tomb", "Steam Vents",
                "Temple Garden", "Blood Crypt", "Hallowed Fountain", "Stomping Ground", "Watery Grave",
                "Sacred Foundry", "Breeding Pool", "Godless Shrine", "Pendelhaven", "Flagstones of Trokair", "Forest",
                "Swamp", "Mountain", "Island", "Plains", "Tree of Tales", "Vault of Whispers", "Great Furnace",
                "Seat of the Synod", "Ancient Den", "Treetop Village", "Ghitu Encampment", "Faerie Conclave",
                "Forbidding Watchtower", "Savage Lands", "Arcane Sanctum", "Jungle Shrine", "Crumbling Necropolis",
                "Seaside Citadel", "Elfhame Palace", "Coastal Tower", "Salt Marsh", "Kher Keep",
                "Library of Alexandria", "Dryad Arbor" };

        final ArrayList<CardList> land = new ArrayList<CardList>();

        // get different CardList of all Forest, Swamps, etc...
        CardList check;
        for (final String element : allLand) {
            check = in.getName(element);

            if (!check.isEmpty()) {
                land.add(check);
            }
        }
        /*
         * //get non-basic land CardList check = in.filter(new CardListFilter()
         * { public boolean addCard(Card c) { return c.isLand() &&
         * !c.isBasicLand(); } }); if(! check.isEmpty()) land.add(check);
         */

        // thread all separate CardList's of land together to get something like
        // Mountain, Plains, Island, Mountain, Plains, Island
        final CardList out = new CardList();

        int i = 0;
        while (!land.isEmpty()) {
            i = (i + 1) % land.size();

            check = land.get(i);
            if (check.isEmpty()) {
                // System.out.println("removed");
                land.remove(i);
                i--;
                continue;
            }

            out.add(check.get(0));
            check.remove(0);
        } // while

        return out;
    } // threadLand()

    /**
     * <p>
     * getDifferentLand.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param land
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    @SuppressWarnings("unused")
    // getDifferentLand
    private int getDifferentLand(final CardList list, final String land) {
        final int out = 0;

        return out;
    }

    // decides who goes first when starting another game, used by newGame()

    /**
     * <p>
     * seeWhoPlaysFirst_CoinToss.
     * </p>
     */
    public final void seeWhoPlaysFirstCoinToss() {
        final Object[] possibleValues = { ForgeProps.getLocalized(GameActionText.HEADS),
                ForgeProps.getLocalized(GameActionText.TAILS) };
        final Object q = JOptionPane.showOptionDialog(null, ForgeProps.getLocalized(GameActionText.HEADS_OR_TAILS),
                ForgeProps.getLocalized(GameActionText.COIN_TOSS), JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, possibleValues, possibleValues[0]);

        final int flip = MyRandom.getRandom().nextInt(2);
        String humanFlip = " ";
        String computerFlip = " ";
        // JOptionPane.showMessageDialog(null, q, "",
        // JOptionPane.INFORMATION_MESSAGE);
        if (q.equals(0)) {
            humanFlip = ForgeProps.getLocalized(GameActionText.HEADS);
            computerFlip = ForgeProps.getLocalized(GameActionText.TAILS);
        } else {
            humanFlip = ForgeProps.getLocalized(GameActionText.TAILS);
            computerFlip = ForgeProps.getLocalized(GameActionText.HEADS);
        }

        if (((flip == 0) && q.equals(0)) || ((flip == 1) && q.equals(1))) {
            JOptionPane.showMessageDialog(null, humanFlip + "\r\n" + ForgeProps.getLocalized(GameActionText.HUMAN_WIN),
                    "", JOptionPane.INFORMATION_MESSAGE);
        } else {
            this.computerStartsGame();
            JOptionPane.showMessageDialog(null,
                    computerFlip + "\r\n" + ForgeProps.getLocalized(GameActionText.COMPUTER_WIN), "",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    } // seeWhoPlaysFirst_CoinToss()

    private Card humanCut = null;
    private Card computerCut = null;

    /**
     * <p>
     * seeWhoPlaysFirst.
     * </p>
     */
    public final void seeWhoPlaysFirst() {

        CardList hLibrary = AllZone.getHumanPlayer().getCardsIn(Zone.Library);
        hLibrary = hLibrary.filter(CardListFilter.NON_LANDS);
        CardList cLibrary = AllZone.getComputerPlayer().getCardsIn(Zone.Library);
        cLibrary = cLibrary.filter(CardListFilter.NON_LANDS);

        final boolean starterDetermined = false;
        int cutCount = 0;
        final int cutCountMax = 20;
        for (int i = 0; i < cutCountMax; i++) {
            if (starterDetermined) {
                break;
            }

            if (hLibrary.size() > 0) {
                this.setHumanCut(hLibrary.get(MyRandom.getRandom().nextInt(hLibrary.size())));
            } else {
                this.computerStartsGame();
                JOptionPane.showMessageDialog(null, ForgeProps.getLocalized(GameActionText.HUMAN_MANA_COST) + "\r\n"
                        + ForgeProps.getLocalized(GameActionText.COMPUTER_STARTS), "", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            if (cLibrary.size() > 0) {
                this.setComputerCut(cLibrary.get(MyRandom.getRandom().nextInt(cLibrary.size())));
            } else {
                JOptionPane.showMessageDialog(null, ForgeProps.getLocalized(GameActionText.COMPUTER_MANA_COST) + "\r\n"
                        + ForgeProps.getLocalized(GameActionText.HUMAN_STARTS), "", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            cutCount = cutCount + 1;
            AllZone.getGameAction().moveTo(AllZone.getHumanPlayer().getZone(Constant.Zone.Library),
                    AllZone.getGameAction().getHumanCut());
            AllZone.getGameAction().moveTo(AllZone.getComputerPlayer().getZone(Constant.Zone.Library),
                    AllZone.getGameAction().getComputerCut());

            final StringBuilder sb = new StringBuilder();
            sb.append(ForgeProps.getLocalized(GameActionText.HUMAN_CUT) + this.getHumanCut().getName() + " ("
                    + this.getHumanCut().getManaCost() + ")" + "\r\n");
            sb.append(ForgeProps.getLocalized(GameActionText.COMPUTER_CUT) + this.getComputerCut().getName() + " ("
                    + this.getComputerCut().getManaCost() + ")" + "\r\n");
            sb.append("\r\n" + "Number of times the deck has been cut: " + cutCount + "\r\n");
            if (CardUtil.getConvertedManaCost(this.getComputerCut().getManaCost()) > CardUtil.getConvertedManaCost(this
                    .getHumanCut().getManaCost())) {
                this.computerStartsGame();
                JOptionPane.showMessageDialog(null, sb + ForgeProps.getLocalized(GameActionText.COMPUTER_STARTS), "",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            } else if (CardUtil.getConvertedManaCost(this.getComputerCut().getManaCost()) < CardUtil
                    .getConvertedManaCost(this.getHumanCut().getManaCost())) {
                JOptionPane.showMessageDialog(null, sb + ForgeProps.getLocalized(GameActionText.HUMAN_STARTS), "",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            } else {
                sb.append(ForgeProps.getLocalized(GameActionText.EQUAL_CONVERTED_MANA) + "\r\n");
                if (i == (cutCountMax - 1)) {
                    sb.append(ForgeProps.getLocalized(GameActionText.RESOLVE_STARTER));
                    if (MyRandom.getRandom().nextInt(2) == 1) {
                        JOptionPane.showMessageDialog(null, sb + ForgeProps.getLocalized(GameActionText.HUMAN_WIN), "",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        this.computerStartsGame();
                        JOptionPane.showMessageDialog(null, sb + ForgeProps.getLocalized(GameActionText.COMPUTER_WIN),
                                "", JOptionPane.INFORMATION_MESSAGE);
                    }
                    return;
                } else {
                    sb.append(ForgeProps.getLocalized(GameActionText.CUTTING_AGAIN));
                }
                JOptionPane.showMessageDialog(null, sb, "", JOptionPane.INFORMATION_MESSAGE);
            }
        } // for-loop for multiple card cutting

    } // seeWhoPlaysFirst()

    /**
     * <p>
     * computerStartsGame.
     * </p>
     */
    public final void computerStartsGame() {
        final Player computer = AllZone.getComputerPlayer();
        AllZone.getPhase().setPlayerTurn(computer);
        // AllZone.getGameInfo().setPlayerWhoGotFirstTurn(computer.getName());
    }

    // if Card had the type "Aura" this method would always return true, since
    // local enchantments are always attached to something
    // if Card is "Equipment", returns true if attached to something

    /**
     * <p>
     * isAttachee.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean isAttacheeByMindsDesire(final Card c) {
        final CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);

        for (int i = 0; i < list.size(); i++) {
            final Card[] cc = list.getCard(i).getAttachedCardsByMindsDesire();
            if (Arrays.binarySearch(cc, c) >= 0) {
                return true;
            }
        }

        return false;
    } // isAttached(Card c)

    /**
     * <p>
     * playCard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean playCard(final Card c) {
        // this can only be called by the Human
        final HashMap<String, SpellAbility> map = new HashMap<String, SpellAbility>();
        final SpellAbility[] abilities = this.canPlaySpellAbility(c.getSpellAbility());
        final ArrayList<String> choices = new ArrayList<String>();
        final Player human = AllZone.getHumanPlayer();

        if (c.isLand() && human.canPlayLand()) {
            final PlayerZone zone = AllZone.getZoneOf(c);

            if (zone.is(Zone.Hand) || ((!zone.is(Zone.Battlefield)) && c.hasStartOfKeyword("May be played"))) {
                choices.add("Play land");
            }
        }

        for (final SpellAbility sa : abilities) {
            // for uncastables like lotus bloom, check if manaCost is blank
            sa.setActivatingPlayer(human);
            if (sa.canPlay() && (!sa.isSpell() || !sa.getManaCost().equals(""))) {

                boolean flashb = false;

                // check for flashback keywords
                if (c.isInZone(Constant.Zone.Graveyard) && sa.isSpell() && (c.isInstant() || c.isSorcery())) {
                    for (final String keyword : c.getKeyword()) {
                        if (keyword.startsWith("Flashback")) {
                            final SpellAbility flashback = sa.copy();
                            flashback.setFlashBackAbility(true);
                            if (!keyword.equals("Flashback")) { // there is a
                                                                // flashback
                                                                // cost
                                                                // (and not the
                                                                // cards cost)
                                final Cost fbCost = new Cost(keyword.substring(10), c.getName(), false);
                                flashback.setPayCosts(fbCost);
                            }
                            choices.add(flashback.toString());
                            map.put(flashback.toString(), flashback);
                            flashb = true;
                        }
                    }
                }
                if (!flashb || c.hasStartOfKeyword("May be played")) {
                    choices.add(sa.toString());
                    map.put(sa.toString(), sa);
                }
            }
        }

        String choice;
        if (choices.size() == 0) {
            return false;
        } else if (choices.size() == 1) {
            choice = choices.get(0);
        } else {
            choice = (String) GuiUtils.getChoiceOptional("Choose", choices.toArray());
        }

        if (choice == null) {
            return false;
        }

        if (choice.equals("Play land")) {
            AllZone.getHumanPlayer().playLand(c);
            return true;
        }

        final SpellAbility ability = map.get(choice);
        if (ability != null) {
            this.playSpellAbility(ability);
            return true;
        }
        return false;
    }

    /**
     * <p>
     * playCardNoCost.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void playCardNoCost(final Card c) {
        final ArrayList<SpellAbility> choices = c.getBasicSpells();
        SpellAbility sa;

        // TODO add Buyback, Kicker, ... , spells here
        if (choices.size() == 0) {
            return;
        } else if (choices.size() == 1) {
            sa = choices.get(0);
        } else {
            sa = (SpellAbility) GuiUtils.getChoiceOptional("Choose", choices.toArray());
        }

        if (sa == null) {
            return;
        }

        // Ripple causes a crash because it doesn't set the activatingPlayer in
        // this entrance
        if (sa.getActivatingPlayer() == null) {
            sa.setActivatingPlayer(AllZone.getHumanPlayer());
        }
        this.playSpellAbilityForFree(sa);
    }

    /**
     * <p>
     * playSpellAbilityForFree.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void playSpellAbilityForFree(final SpellAbility sa) {
        if (sa.getPayCosts() != null) {
            final TargetSelection ts = new TargetSelection(sa.getTarget(), sa);
            final CostPayment payment = new CostPayment(sa.getPayCosts(), sa);

            final SpellAbilityRequirements req = new SpellAbilityRequirements(sa, ts, payment);
            req.setFree(true);
            req.fillRequirements();
        } else if (sa.getBeforePayMana() == null) {
            if (sa.isSpell()) {
                final Card c = sa.getSourceCard();
                if (!c.isCopiedSpell()) {
                    sa.setSourceCard(AllZone.getGameAction().moveToStack(c));
                }
            }
            boolean x = false;
            if (sa.getSourceCard().getManaCost().contains("X")) {
                x = true;
            }

            if (sa.isKickerAbility()) {
                final Command paid1 = new Command() {
                    private static final long serialVersionUID = -6531785460264284794L;

                    @Override
                    public void execute() {
                        AllZone.getStack().add(sa);
                    }
                };
                AllZone.getInputControl().setInput(new InputPayManaCostAbility(sa.getAdditionalManaCost(), paid1));
            } else {
                AllZone.getStack().add(sa, x);
            }
        } else {
            sa.setManaCost("0"); // Beached As
            if (sa.isKickerAbility()) {
                sa.getBeforePayMana().setFree(false);
                sa.setManaCost(sa.getAdditionalManaCost());
            } else {
                sa.getBeforePayMana().setFree(true);
            }
            AllZone.getInputControl().setInput(sa.getBeforePayMana());
        }
    }

    /** The Cost cutting_ get multi micker mana cost paid. */
    private int costCuttingGetMultiMickerManaCostPaid = 0;

    /** The Cost cutting_ get multi micker mana cost paid_ colored. */
    private String costCuttingGetMultiMickerManaCostPaidColored = "";

    /**
     * <p>
     * getSpellCostChange.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param originalCost
     *            a {@link forge.card.mana.ManaCost} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public final ManaCost getSpellCostChange(final SpellAbility sa, final ManaCost originalCost) {
        // Beached
        final Card originalCard = sa.getSourceCard();
        final Player controller = originalCard.getController();
        final SpellAbility spell = sa;
        String mana = originalCost.toString();
        ManaCost manaCost = new ManaCost(mana);
        if (sa.isXCost() && !originalCard.isCopiedSpell()) {
            originalCard.setXManaCostPaid(0);
        }

        if (Phase.getGameBegins() != 1) {
            return manaCost;
        }

        if (spell.isSpell()) {
            if (originalCard.getName().equals("Avatar of Woe")) {
                final Player player = AllZone.getPhase().getPlayerTurn();
                final Player opponent = player.getOpponent();
                CardList playerCreatureList = player.getCardsIn(Zone.Graveyard);
                playerCreatureList = playerCreatureList.getType("Creature");
                CardList opponentCreatureList = opponent.getCardsIn(Zone.Graveyard);
                opponentCreatureList = opponentCreatureList.getType("Creature");
                if ((playerCreatureList.size() + opponentCreatureList.size()) >= 10) {
                    manaCost = new ManaCost("B B");
                } // Avatar of Woe
            } else if (originalCard.getName().equals("Avatar of Will")) {
                final Player opponent = AllZone.getPhase().getPlayerTurn().getOpponent();
                final CardList opponentHandList = opponent.getCardsIn(Zone.Hand);
                if (opponentHandList.size() == 0) {
                    manaCost = new ManaCost("U U");
                } // Avatar of Will
            } else if (originalCard.getName().equals("Avatar of Fury")) {
                final Player opponent = AllZone.getPhase().getPlayerTurn().getOpponent();
                final CardList opponentLand = AllZoneUtil.getPlayerLandsInPlay(opponent);
                if (opponentLand.size() >= 7) {
                    manaCost = new ManaCost("R R");
                } // Avatar of Fury
            } else if (originalCard.getName().equals("Avatar of Might")) {
                final Player player = AllZone.getPhase().getPlayerTurn();
                final Player opponent = player.getOpponent();
                final CardList playerCreature = AllZoneUtil.getCreaturesInPlay(player);
                final CardList opponentCreature = AllZoneUtil.getCreaturesInPlay(opponent);
                if ((opponentCreature.size() - playerCreature.size()) >= 4) {
                    manaCost = new ManaCost("G G");
                } // Avatar of Might
            } else if (spell.getIsDelve()) {
                final int cardsInGrave = originalCard.getController().getCardsIn(Zone.Graveyard).size();
                final ArrayList<Integer> choiceList = new ArrayList<Integer>();
                for (int i = 0; i <= cardsInGrave; i++) {
                    choiceList.add(i);
                }

                if (originalCard.getController().isHuman()) {

                    final int chosenAmount = (Integer) GuiUtils
                            .getChoice("Exile how many cards?", choiceList.toArray());
                    System.out.println("Delve for " + chosenAmount);
                    final CardList choices = AllZone.getHumanPlayer().getCardsIn(Zone.Graveyard);
                    final CardList chosen = new CardList();
                    for (int i = 0; i < chosenAmount; i++) {
                        final Card nowChosen = GuiUtils.getChoiceOptional("Exile which card?", choices.toArray());

                        if (nowChosen == null) {
                            // User canceled,abort delving.
                            chosen.clear();
                            break;
                        }

                        choices.remove(nowChosen);
                        chosen.add(nowChosen);
                    }

                    for (final Card c : chosen) {
                        this.exile(c);
                    }

                    manaCost = new ManaCost(originalCost.toString());
                    manaCost.decreaseColorlessMana(chosenAmount);
                } else {
                    // AI
                    int numToExile = 0;
                    final int colorlessCost = originalCost.getColorlessManaAmount();

                    if (cardsInGrave <= colorlessCost) {
                        numToExile = cardsInGrave;
                    } else {
                        numToExile = colorlessCost;
                    }

                    for (int i = 0; i < numToExile; i++) {
                        final CardList grave = new CardList(AllZone.getComputerPlayer().getZone(Zone.Graveyard)
                                .getCards());
                        Card chosen = null;
                        for (final Card c : grave) { // Exile noncreatures first
                                                     // in
                            // case we can revive. Might
                            // wanna do some additional
                            // checking here for Flashback
                            // and the like.
                            if (!c.isCreature()) {
                                chosen = c;
                                break;
                            }
                        }
                        if (chosen == null) {
                            chosen = CardFactoryUtil.getWorstCreatureAI(grave);
                        }

                        if (chosen == null) {
                            // Should never get here but... You know how it is.
                            chosen = grave.get(0);
                        }

                        this.exile(chosen);
                    }
                    manaCost = new ManaCost(originalCost.toString());
                    manaCost.decreaseColorlessMana(numToExile);
                }
            } else if (spell.getSourceCard().hasKeyword("Convoke")) {
                CardList untappedCreats = spell.getActivatingPlayer().getCardsIn(Zone.Battlefield).getType("Creature");
                untappedCreats = untappedCreats.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return !c.isTapped();
                    }
                });

                if (untappedCreats.size() != 0) {
                    final ArrayList<Object> choices = new ArrayList<Object>();
                    for (final Card c : untappedCreats) {
                        choices.add(c);
                    }
                    choices.add("DONE");
                    ArrayList<String> usableColors = new ArrayList<String>();
                    ManaCost newCost = new ManaCost(originalCost.toString());
                    Object tapForConvoke = null;
                    if (sa.getActivatingPlayer().isHuman()) {
                        tapForConvoke = GuiUtils.getChoiceOptional("Tap for Convoke? " + newCost.toString(),
                                choices.toArray());
                    } else {
                        // TODO: AI to choose a creature to tap would go here
                        // Probably along with deciding how many creatures to
                        // tap
                    }
                    while ((tapForConvoke != null) && (tapForConvoke instanceof Card) && (untappedCreats.size() != 0)) {
                        final Card workingCard = (Card) tapForConvoke;
                        usableColors = CardUtil.getConvokableColors(workingCard, newCost);

                        if (usableColors.size() != 0) {
                            String chosenColor = usableColors.get(0);
                            if (usableColors.size() > 1) {
                                if (sa.getActivatingPlayer().isHuman()) {
                                    chosenColor = (String) GuiUtils.getChoice("Convoke for which color?",
                                            usableColors.toArray());
                                } else {
                                    // TODO: AI for choosing which color to
                                    // convoke goes here.
                                }
                            }

                            if (chosenColor.equals("colorless")) {
                                newCost.decreaseColorlessMana(1);
                            } else {
                                String newCostStr = newCost.toString();
                                newCostStr = newCostStr.replaceFirst(
                                        InputPayManaCostUtil.getShortColorString(chosenColor), "");
                                newCost = new ManaCost(newCostStr.trim());
                            }

                            sa.addTappedForConvoke(workingCard);
                            choices.remove(workingCard);
                            untappedCreats.remove(workingCard);
                            if ((choices.size() < 2) || (newCost.getConvertedManaCost() == 0)) {
                                break;
                            }
                        } else {
                            untappedCreats.remove(workingCard);
                        }

                        if (sa.getActivatingPlayer().isHuman()) {
                            tapForConvoke = GuiUtils.getChoiceOptional("Tap for Convoke? " + newCost.toString(),
                                    choices.toArray());
                        } else {
                            // TODO: AI to choose a creature to tap would go
                            // here
                        }
                    }

                    // will only be null if user cancelled.
                    if (tapForConvoke != null) {
                        // Convoked creats are tapped here with triggers
                        // suppressed,
                        // Then again when payment is done(In
                        // InputPayManaCost.done()) with suppression cleared.
                        // This is to make sure that triggers go off at the
                        // right time
                        // AND that you can't use mana tapabilities of convoked
                        // creatures
                        // to pay the convoked cost.
                        AllZone.getTriggerHandler().suppressMode("Taps");
                        for (final Card c : sa.getTappedForConvoke()) {
                            c.tap();
                        }
                        AllZone.getTriggerHandler().clearSuppression("Taps");

                        manaCost = newCost;
                    }
                }

            }
        } // isSpell

        // Get Cost Reduction
        CardList cardsInPlay = AllZoneUtil.getCardsIn(Zone.Battlefield);
        cardsInPlay = cardsInPlay.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                if (c.getKeyword().toString().contains("CostChange")) {
                    return true;
                }
                return false;
            }
        });
        cardsInPlay.add(originalCard);
        final CardList playerPlay = controller.getCardsIn(Zone.Battlefield);
        final CardList playerHand = controller.getCardsIn(Zone.Hand);
        int xBonus = 0;
        final int max = 25;
        if (sa.isMultiKicker()) {
            this.setCostCuttingGetMultiMickerManaCostPaidColored("");
        }

        if (mana.toString().length() == 0) {
            mana = "0";
        }
        for (int i = 0; i < cardsInPlay.size(); i++) {
            final Card card = cardsInPlay.get(i);
            final ArrayList<String> a = card.getKeyword();
            int costKeywords = 0;
            final int[] costKeywordNumber = new int[a.size()];
            for (int x = 0; x < a.size(); x++) {
                if (a.get(x).toString().startsWith("CostChange")) {
                    costKeywordNumber[costKeywords] = x;
                    costKeywords = costKeywords + 1;
                }
            }
            for (int cKeywords = 0; cKeywords < costKeywords; cKeywords++) {
                final String parse = card.getKeyword().get(costKeywordNumber[cKeywords]).toString();
                final String[] k = parse.split(":");
                if (card.equals(originalCard)) {
                    if (!k[4].equals("Self")) {
                        k[2] = "Owned";
                    }
                }
                if (k[6].equals("ChosenType")) {
                    k[6] = card.getChosenType();
                }
                if (k[2].equals("More")) {
                    if (k[7].equals("OnlyOneBonus")) { // Only Works for Color
                                                       // and Type
                        for (int stringNo = 5; stringNo < 7; stringNo++) {
                            final String spilt = k[stringNo];
                            final String[] colorSpilt = spilt.split("/");

                            for (final String element : colorSpilt) {
                                k[stringNo] = element;
                                if ((stringNo == 5) && CardUtil.getColors(originalCard).contains(k[5])) {
                                    break;
                                }
                                if ((stringNo == 6) && (originalCard.isType(k[6]))) {
                                    break;
                                }
                            }
                        }
                    }
                    if (k[7].contains("All Conditions")) { // Only Works for
                                                           // Color and Type
                        for (int stringNo = 5; stringNo < 7; stringNo++) {
                            final String spilt = k[stringNo];
                            final String[] colorSpilt = spilt.split("/");
                            for (final String element : colorSpilt) {
                                k[stringNo] = element;
                                if (stringNo == 5) {
                                    if (CardUtil.getColors(originalCard).contains(k[5]) || k[5].equals("All")) {
                                    } else {
                                        k[5] = "Nullified";
                                        break;
                                    }
                                }
                                if (stringNo == 6) {
                                    if (originalCard.isType(k[6]) || k[6].equals("All")) {
                                    } else {
                                        k[6] = "Nullified";
                                        break;
                                    }
                                }
                            }
                        }
                        if (!k[5].equals("Nullified")) {
                            k[5] = "All";
                        }
                        if (!k[6].equals("Nullified")) {
                            k[6] = "All";
                        }
                    }
                    if (((k[1].equals("Player") && card.getController().equals(controller))
                            || (k[1].equals("Opponent") && card.getController().equals(controller.getOpponent())) || k[1]
                                .equals("All"))
                            && ((k[4].equals("Spell") && (sa.isSpell()))
                                    || (k[4].equals("Ability") && (sa.isAbility()))
                                    || (k[4].startsWith("Ability_Cycling") && sa.isCycling())
                                    || (k[4].equals("Self") && originalCard.equals(card))
                                    || (k[4].equals("Enchanted") && originalCard.getEnchantedBy().contains(card)) || k[4]
                                        .equals("All"))
                            && ((CardUtil.getColors(originalCard).contains(k[5])) || k[5].equals("All"))
                            && ((originalCard.isType(k[6]))
                                    || (!(originalCard.isType(k[6])) && k[7].contains("NonType")) || k[6].equals("All"))) {
                        if (k[7].contains("CardIsTapped")) {
                            if (!card.isTapped()) {
                                k[3] = "0";
                            }
                        }
                        if (k[7].contains("TargetInPlay")) {
                            if (!playerPlay.contains(originalCard)) {
                                k[3] = "0";
                            }
                        }
                        if (k[7].contains("TargetInHand")) {
                            if (!playerHand.contains(originalCard)) {
                                k[3] = "0";
                            }
                        }
                        if (k[7].contains("NonType")) {
                            if (originalCard.isType(k[6])) {
                                k[3] = "0";
                            }
                        }
                        if (k[7].contains("OpponentTurn")) {
                            if (AllZone.getPhase().isPlayerTurn(controller)) {
                                k[3] = "0";
                            }
                        }
                        if (k[7].contains("Affinity")) {
                            final String spilt = k[7];
                            final String[] colorSpilt = spilt.split("/");
                            k[7] = colorSpilt[1];
                            CardList playerList = controller.getCardsIn(Zone.Battlefield);
                            playerList = playerList.getType(k[7]);
                            k[3] = String.valueOf(playerList.size());
                        }
                        final String[] numbers = new String[max];
                        if ("X".equals(k[3])) {
                            for (int no = 0; no < max; no++) {
                                numbers[no] = String.valueOf(no);
                            }
                            String numberManaCost = " ";
                            if (mana.toString().length() == 1) {
                                numberManaCost = mana.toString().substring(0, 1);
                            } else if (mana.toString().length() == 0) {
                                numberManaCost = "0"; // Should Never Occur
                            } else {
                                numberManaCost = mana.toString().substring(0, 2);
                            }
                            numberManaCost = numberManaCost.trim();
                            for (int check = 0; check < max; check++) {
                                if (numberManaCost.equals(numbers[check])) {
                                    final int xValue = CardFactoryUtil.xCount(card, card.getSVar("X"));
                                    // if((spell.isXCost()) ||
                                    // (spell.isMultiKicker()) && (check -
                                    // Integer.valueOf(k[3])) < 0) XBonus =
                                    // XBonus - check + Integer.valueOf(k[3]);
                                    mana = mana.replaceFirst(String.valueOf(check), String.valueOf(check + xValue));
                                }
                                if (mana.equals("")) {
                                    mana = "0";
                                }
                                manaCost = new ManaCost(mana);
                            }
                        } else if (!"WUGRB".contains(k[3])) {
                            for (int no = 0; no < max; no++) {
                                numbers[no] = String.valueOf(no);
                            }
                            String numberManaCost = " ";
                            if (mana.toString().length() == 1) {
                                numberManaCost = mana.toString().substring(0, 1);
                            } else if (mana.toString().length() == 0) {
                                numberManaCost = "0"; // Should Never Occur
                            } else {
                                numberManaCost = mana.toString().substring(0, 2);
                            }
                            numberManaCost = numberManaCost.trim();

                            for (int check = 0; check < max; check++) {
                                if (numberManaCost.equals(numbers[check])) {
                                    mana = mana.replaceFirst(String.valueOf(check),
                                            String.valueOf(check + Integer.valueOf(k[3])));
                                }
                                if (mana.equals("")) {
                                    mana = "0";
                                }
                                manaCost = new ManaCost(mana);
                            }
                            if (!manaCost.toString().contains("0") && !manaCost.toString().contains("1")
                                    && !manaCost.toString().contains("2") && !manaCost.toString().contains("3")
                                    && !manaCost.toString().contains("4") && !manaCost.toString().contains("5")
                                    && !manaCost.toString().contains("6") && !manaCost.toString().contains("7")
                                    && !manaCost.toString().contains("8") && !manaCost.toString().contains("9")) {
                                mana = k[3] + " " + mana;
                                manaCost = new ManaCost(mana);
                            }
                        } else {
                            mana = mana + " " + k[3];
                            manaCost = new ManaCost(mana);
                        }
                    }
                }
            }
        }

        if (mana.equals("0") && spell.isAbility()) {
        } else {
            for (int i = 0; i < cardsInPlay.size(); i++) {
                final Card card = cardsInPlay.get(i);
                final ArrayList<String> a = card.getKeyword();
                int costKeywords = 0;
                final int[] costKeywordNumber = new int[a.size()];
                for (int x = 0; x < a.size(); x++) {
                    if (a.get(x).toString().startsWith("CostChange")) {
                        costKeywordNumber[costKeywords] = x;
                        costKeywords = costKeywords + 1;
                    }
                }
                for (int cKeywords = 0; cKeywords < costKeywords; cKeywords++) {
                    final String parse = card.getKeyword().get(costKeywordNumber[cKeywords]).toString();
                    final String[] k = parse.split(":");
                    if (card.equals(originalCard)) {
                        if (!k[4].equals("Self")) {
                            k[2] = "Owned";
                        }
                    }
                    if (k[6].equals("ChosenType")) {
                        k[6] = card.getChosenType();
                    }
                    if (k[2].equals("Less")) {
                        if (k[7].equals("OnlyOneBonus")) { // Only Works for
                                                           // Color and Type
                            for (int stringNo = 5; stringNo < 7; stringNo++) {
                                final String spilt = k[stringNo];
                                final String[] colorSpilt = spilt.split("/");

                                for (final String element : colorSpilt) {
                                    k[stringNo] = element;
                                    if ((stringNo == 5) && CardUtil.getColors(originalCard).contains(k[5])) {
                                        break;
                                    }
                                    if ((stringNo == 6) && (originalCard.isType(k[6]))) {
                                        break;
                                    }
                                }
                            }
                        }
                        if (k[7].contains("All Conditions")) { // Only Works for
                                                               // Color and Type
                            for (int stringNo = 5; stringNo < 7; stringNo++) {
                                final String spilt = k[stringNo];
                                final String[] colorSpilt = spilt.split("/");
                                for (final String element : colorSpilt) {
                                    k[stringNo] = element;
                                    if (stringNo == 5) {
                                        if (CardUtil.getColors(originalCard).contains(k[5]) || k[5].equals("All")) {
                                        } else {
                                            k[5] = "Nullified";
                                            break;
                                        }
                                    }
                                    if (stringNo == 6) {
                                        if (originalCard.isType(k[6]) || k[6].equals("All")) {
                                        } else {
                                            k[6] = "Nullified";
                                            break;
                                        }
                                    }
                                }
                            }
                            if (!k[5].equals("Nullified")) {
                                k[5] = "All";
                            }
                            if (!k[6].equals("Nullified")) {
                                k[6] = "All";
                            }
                        }
                        if (((k[1].equals("Player") && card.getController().equals(controller))
                                || (k[1].equals("Opponent") && card.getController().equals(controller.getOpponent())) || k[1]
                                    .equals("All"))
                                && ((k[4].equals("Spell") && sa.isSpell())
                                        || (k[4].equals("Ability") && sa.isAbility())
                                        || (k[4].startsWith("Ability_Cycling") && sa.isCycling())
                                        || (k[4].equals("Self") && originalCard.equals(card))
                                        || (k[4].equals("Enchanted") && originalCard.getEnchantedBy().contains(card)) || k[4]
                                            .equals("All"))
                                && ((CardUtil.getColors(originalCard).contains(k[5])) || k[5].equals("All"))
                                && ((originalCard.isType(k[6]))
                                        || (!(originalCard.isType(k[6])) && k[7].contains("NonType")) || k[6]
                                            .equals("All"))) {
                            if (k[7].contains("CardIsTapped")) {
                                if (!card.isTapped()) {
                                    k[3] = "0";
                                }
                            }
                            if (k[7].contains("TargetInPlay")) {
                                if (!playerPlay.contains(originalCard)) {
                                    k[3] = "0";
                                }
                            }
                            if (k[7].contains("TargetInHand")) {
                                if (!playerHand.contains(originalCard)) {
                                    k[3] = "0";
                                }
                            }
                            if (k[7].contains("NonType")) {
                                if (originalCard.isType(k[6])) {
                                    k[3] = "0";
                                }
                            }
                            if (k[7].contains("OpponentTurn")) {
                                if (AllZone.getPhase().isPlayerTurn(controller)) {
                                    k[3] = "0";
                                }
                            }
                            if (k[7].contains("Affinity")) {
                                final String spilt = k[7];
                                final String[] colorSpilt = spilt.split("/");
                                k[7] = colorSpilt[1];
                                CardList playerList = controller.getCardsIn(Zone.Battlefield);
                                playerList = playerList.getType(k[7]);
                                k[3] = String.valueOf(playerList.size());
                            }

                            final String[] numbers = new String[max];
                            if (!"WUGRB".contains(k[3])) {

                                int value = 0;
                                if ("X".equals(k[3])) {
                                    value = CardFactoryUtil.xCount(card, card.getSVar("X"));
                                } else {
                                    value = Integer.valueOf(k[3]);
                                }

                                for (int no = 0; no < max; no++) {
                                    numbers[no] = String.valueOf(no);
                                }
                                String numberManaCost = " ";
                                if (mana.toString().length() == 1) {
                                    numberManaCost = mana.toString().substring(0, 1);
                                } else if (mana.toString().length() == 0) {
                                    numberManaCost = "0"; // Should Never Occur
                                } else {
                                    numberManaCost = mana.toString().substring(0, 2);
                                }
                                numberManaCost = numberManaCost.trim();

                                for (int check = 0; check < max; check++) {
                                    if (numberManaCost.equals(numbers[check])) {
                                        if ((spell.isXCost()) || ((spell.isMultiKicker()) && ((check - value) < 0))) {
                                            xBonus = (xBonus - check) + value;
                                        }
                                        if ((check - value) < 0) {
                                            value = check;
                                        }
                                        mana = mana.replaceFirst(String.valueOf(check), String.valueOf(check - value));
                                    }
                                    if (mana.equals("")) {
                                        mana = "0";
                                    }
                                    manaCost = new ManaCost(mana);
                                }
                            } else {
                                // JOptionPane.showMessageDialog(null, Mana +
                                // " " + Mana.replaceFirst(k[3],""), "",
                                // JOptionPane.INFORMATION_MESSAGE);
                                if (mana.equals(mana.replaceFirst(k[3], ""))) {
                                    // if(sa.isXCost())
                                    // sa.getSourceCard().addXManaCostPaid(1);
                                    // Not Included as X Costs are not in
                                    // Colored Mana
                                    if (sa.isMultiKicker()) {
                                        this.setCostCuttingGetMultiMickerManaCostPaidColored(this
                                                .getCostCuttingGetMultiMickerManaCostPaidColored() + k[3]);
                                        // JOptionPane.showMessageDialog(null,
                                        // CostCutting_GetMultiMickerManaCostPaid_Colored,
                                        // "", JOptionPane.INFORMATION_MESSAGE);
                                    }
                                } else {
                                    mana = mana.replaceFirst(k[3], "");
                                    mana = mana.trim();
                                    if (mana.equals("")) {
                                        mana = "0";
                                    }
                                    manaCost = new ManaCost(mana);
                                }
                            }
                        }
                        mana = mana.trim();
                        if ((mana.length() == 0) || mana.equals("0")) {
                            if (sa.isSpell() || sa.isCycling()) {
                                mana = "0";
                            } else {
                                mana = "1";
                            }
                        }
                    }
                    manaCost = new ManaCost(mana);
                }
            }
        }
        if (sa.isXCost()) {
            for (int xPaid = 0; xPaid < xBonus; xPaid++) {
                originalCard.addXManaCostPaid(1);
            }
        }
        if (sa.isMultiKicker()) {
            this.setCostCuttingGetMultiMickerManaCostPaid(0);
            for (int xPaid = 0; xPaid < xBonus; xPaid++) {
                this.setCostCuttingGetMultiMickerManaCostPaid(this.getCostCuttingGetMultiMickerManaCostPaid() + 1);
            }
        }

        if (originalCard.getName().equals("Khalni Hydra") && spell.isSpell()) {
            final Player player = AllZone.getPhase().getPlayerTurn();
            CardList playerCreature = AllZoneUtil.getCreaturesInPlay(player);
            playerCreature = playerCreature.filter(CardListFilter.GREEN);
            String manaC = manaCost + " ";
            if (playerCreature.size() > 0) {
                for (int i = 0; i < playerCreature.size(); i++) {
                    manaC = manaC.replaceFirst("G ", "");
                }
                manaC = manaC.trim();
                if (manaC.equals("")) {
                    manaC = "0";
                }
                manaCost = new ManaCost(manaC);
            }
        } // Khalni Hydra
        return manaCost;
    } // GetSpellCostChange

    /**
     * <p>
     * playSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void playSpellAbility(final SpellAbility sa) {
        sa.setActivatingPlayer(AllZone.getHumanPlayer());

        AbilityFactoryCharm.setupCharmSAs(sa);

        // Need to check PayCosts, and Ability + All SubAbilities for Target
        boolean newAbility = sa.getPayCosts() != null;
        SpellAbility ability = sa;
        while ((ability != null) && !newAbility) {
            final Target tgt = ability.getTarget();

            newAbility |= tgt != null;
            ability = ability.getSubAbility();
        }

        if (newAbility) {
            final TargetSelection ts = new TargetSelection(sa.getTarget(), sa);
            CostPayment payment = null;
            if (sa.getPayCosts() == null) {
                payment = new CostPayment(new Cost("0", sa.getSourceCard().getName(), sa.isAbility()), sa);
            } else {
                payment = new CostPayment(sa.getPayCosts(), sa);
            }

            if (!sa.isTrigger()) {
                payment.changeCost();
            }

            final SpellAbilityRequirements req = new SpellAbilityRequirements(sa, ts, payment);
            req.fillRequirements();
        } else {
            ManaCost manaCost = new ManaCost(sa.getManaCost());
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                manaCost = new ManaCost("0");
            } else {

                manaCost = this.getSpellCostChange(sa, new ManaCost(sa.getManaCost()));
            }
            if (manaCost.isPaid() && (sa.getBeforePayMana() == null)) {
                if (sa.getAfterPayMana() == null) {
                    final Card source = sa.getSourceCard();
                    if (sa.isSpell() && !source.isCopiedSpell()) {
                        sa.setSourceCard(AllZone.getGameAction().moveToStack(source));
                    }

                    AllZone.getStack().add(sa);
                    if (sa.isTapAbility() && !sa.wasCancelled()) {
                        sa.getSourceCard().tap();
                    }
                    if (sa.isUntapAbility()) {
                        sa.getSourceCard().untap();
                    }
                    return;
                } else {
                    AllZone.getInputControl().setInput(sa.getAfterPayMana());
                }
            } else if (sa.getBeforePayMana() == null) {
                AllZone.getInputControl().setInput(new InputPayManaCost(sa, manaCost));
            } else {
                AllZone.getInputControl().setInput(sa.getBeforePayMana());
            }
        }
    }

    /**
     * <p>
     * playSpellAbility_NoStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param skipTargeting
     *            a boolean.
     */
    public final void playSpellAbilityNoStack(final SpellAbility sa, final boolean skipTargeting) {
        sa.setActivatingPlayer(AllZone.getHumanPlayer());

        if (sa.getPayCosts() != null) {
            final TargetSelection ts = new TargetSelection(sa.getTarget(), sa);
            final CostPayment payment = new CostPayment(sa.getPayCosts(), sa);

            if (!sa.isTrigger()) {
                payment.changeCost();
            }

            final SpellAbilityRequirements req = new SpellAbilityRequirements(sa, ts, payment);
            req.setSkipStack(true);
            req.fillRequirements(skipTargeting);
        } else {
            ManaCost manaCost = new ManaCost(sa.getManaCost());
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                manaCost = new ManaCost("0");
            } else {

                manaCost = this.getSpellCostChange(sa, new ManaCost(sa.getManaCost()));
            }
            if (manaCost.isPaid() && (sa.getBeforePayMana() == null)) {
                if (sa.getAfterPayMana() == null) {
                    AbilityFactory.resolve(sa, false);
                    if (sa.isTapAbility() && !sa.wasCancelled()) {
                        sa.getSourceCard().tap();
                    }
                    if (sa.isUntapAbility()) {
                        sa.getSourceCard().untap();
                    }
                    return;
                } else {
                    AllZone.getInputControl().setInput(sa.getAfterPayMana());
                }
            } else if (sa.getBeforePayMana() == null) {
                AllZone.getInputControl().setInput(new InputPayManaCost(sa, true));
            } else {
                AllZone.getInputControl().setInput(sa.getBeforePayMana());
            }
        }
    }

    /**
     * <p>
     * canPlaySpellAbility.
     * </p>
     * 
     * @param sa
     *            an array of {@link forge.card.spellability.SpellAbility}
     *            objects.
     * @return an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    public final SpellAbility[] canPlaySpellAbility(final SpellAbility[] sa) {
        final ArrayList<SpellAbility> list = new ArrayList<SpellAbility>();

        for (final SpellAbility element : sa) {
            element.setActivatingPlayer(AllZone.getHumanPlayer());
            if (element.canPlay()) {
                list.add(element);
            }
        }

        final SpellAbility[] array = new SpellAbility[list.size()];
        list.toArray(array);
        return array;
    } // canPlaySpellAbility()

    /**
     * <p>
     * setComputerCut.
     * </p>
     * 
     * @param computerCut
     *            a {@link forge.Card} object.
     */
    public final void setComputerCut(final Card computerCut) {
        this.computerCut = computerCut;
    }

    /**
     * <p>
     * getComputerCut.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getComputerCut() {
        return this.computerCut;
    }

    /**
     * <p>
     * setStartCut.
     * </p>
     * 
     * @param startCutIn
     *            a boolean.
     */
    public final void setStartCut(final boolean startCutIn) {
        this.startCut = startCutIn;
    }

    /**
     * <p>
     * isStartCut.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isStartCut() {
        return this.startCut;
    }

    /**
     * <p>
     * setHumanCut.
     * </p>
     * 
     * @param humanCut
     *            a {@link forge.Card} object.
     */
    public final void setHumanCut(final Card humanCut) {
        this.humanCut = humanCut;
    }

    /**
     * <p>
     * getHumanCut.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getHumanCut() {
        return this.humanCut;
    }

    /**
     * Gets the cost cutting get multi micker mana cost paid.
     * 
     * @return the costCuttingGetMultiMickerManaCostPaid
     */
    public int getCostCuttingGetMultiMickerManaCostPaid() {
        return this.costCuttingGetMultiMickerManaCostPaid;
    }

    /**
     * Sets the cost cutting get multi micker mana cost paid.
     * 
     * @param costCuttingGetMultiMickerManaCostPaid
     *            the costCuttingGetMultiMickerManaCostPaid to set
     */
    public void setCostCuttingGetMultiMickerManaCostPaid(final int costCuttingGetMultiMickerManaCostPaid) {
        this.costCuttingGetMultiMickerManaCostPaid = costCuttingGetMultiMickerManaCostPaid; // TODO:
                                                                                            // Add
                                                                                            // 0
                                                                                            // to
                                                                                            // parameter's
                                                                                            // name.
    }

    /**
     * Gets the cost cutting get multi micker mana cost paid colored.
     * 
     * @return the costCuttingGetMultiMickerManaCostPaidColored
     */
    public String getCostCuttingGetMultiMickerManaCostPaidColored() {
        return this.costCuttingGetMultiMickerManaCostPaidColored;
    }

    /**
     * Sets the cost cutting get multi micker mana cost paid colored.
     * 
     * @param costCuttingGetMultiMickerManaCostPaidColored
     *            the costCuttingGetMultiMickerManaCostPaidColored to set
     */
    public void setCostCuttingGetMultiMickerManaCostPaidColored(
            final String costCuttingGetMultiMickerManaCostPaidColored) {
        this.costCuttingGetMultiMickerManaCostPaidColored = costCuttingGetMultiMickerManaCostPaidColored; // TODO:
                                                                                                          // Add
                                                                                                          // 0
                                                                                                          // to
                                                                                                          // parameter's
                                                                                                          // name.
    }
}
