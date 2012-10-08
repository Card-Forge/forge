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

import java.util.List;

import forge.card.cardfactory.CardFactory;
import forge.card.cardfactory.CardFactoryInterface;
import forge.card.replacement.ReplacementHandler;
import forge.card.trigger.TriggerHandler;
import forge.control.input.InputControl;
import forge.game.GameState;
import forge.game.limited.GauntletMini;
import forge.game.phase.Combat;
import forge.game.phase.EndOfTurn;
import forge.game.player.Player;
import forge.game.player.PlayerType;
import forge.game.zone.MagicStack;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.QuestController;
import forge.util.Aggregates;


/**
 * Please use public getters and setters instead of direct field access.
 * <p/>
 * If you need a setter, by all means, add it.
 * 
 * @author Forge
 * @version $Id$
 */
public final class AllZone {
    // only for testing, should read decks from local directory
    // public static final IO IO = new IO("all-decks");

    /**
     * Do not instantiate.
     */
    private AllZone() {
        // blank
    }

    /** Global <code>questData</code>. */
    private static forge.quest.QuestController quest = null;

    /** Global <code>gauntletData</code>. */
    private static forge.game.limited.GauntletMini gauntlet = null;

    /** Constant <code>COLOR_CHANGER</code>. */
    private static final ColorChanger COLOR_CHANGER = new ColorChanger();

    // Phase is now a prerequisite for CardFactory
    /** Global <code>cardFactory</code>. */
    private static CardFactoryInterface cardFactory = null;

    /** Constant <code>inputControl</code>. */
    private static InputControl inputControl = null;

    // initialized at Runtime since it has to be the last object constructed

    // shared between Input_Attack, Input_Block, Input_CombatDamage ,
    // InputState_Computer

    /**
     * <p>
     * getHumanPlayer.
     * </p>
     * 
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.game.player.Player} object.
     * @since 1.0.15
     */
    @Deprecated
    public static Player getHumanPlayer() {
        if (Singletons.getModel() == null) 
            return null;

        return Aggregates.firstFieldEquals(Singletons.getModel().getGameState().getPlayers(), Player.Accessors.FN_GET_TYPE, PlayerType.HUMAN);
    }

    /**
     * <p>
     * getComputerPlayer.
     * </p>
     * 
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.game.player.Player} object.
     * @since 1.0.15
     */
    @Deprecated
    public static Player getComputerPlayer() {
        List<Player> players = Singletons.getModel().getGameState().getPlayers();
        return Aggregates.firstFieldEquals(players, Player.Accessors.FN_GET_TYPE, PlayerType.COMPUTER);
    }

    /**
     * get a list of all players participating in this game.
     * 
     * @return a list of all player participating in this game
     */
    public static List<Player> getPlayersInGame() {
        return Singletons.getModel().getGameState().getPlayers();
    }

    /**
     * <p>
     * getQuestData.
     * </p>
     * 
     * @return a {@link forge.quest.data.QuestData} object.
     * @since 1.0.15
     */
    public static forge.quest.QuestController getQuest() {
        if (null == quest) {
            quest = new QuestController();
        }
        return AllZone.quest;
    }

    /**
     * <p>
     * getGauntletData.
     * </p>
     * 
     * @return a {@link forge.quest.data.QuestData} object.
     * @since 1.0.15
     */
    public static forge.game.limited.GauntletMini getGauntlet() {

        if (gauntlet == null) {
            gauntlet = new GauntletMini();
        }
        return AllZone.gauntlet;
    }

    /**
     * <p>
     * getEndOfTurn.
     * </p>
     * 
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.game.phase.EndOfTurn} object.
     * @since 1.0.15
     */
    public static EndOfTurn getEndOfTurn() {
        return Singletons.getModel().getGameState().getEndOfTurn();
    }

    /**
     * <p>
     * getEndOfCombat.
     * </p>
     * 
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.game.phase.EndOfCombat} object.
     * @since 1.0.15
     */
    public static forge.game.phase.EndOfCombat getEndOfCombat() {
        return Singletons.getModel().getGameState().getEndOfCombat();
    }


    /**
     * <p>
     * getGameLog.
     * </p>
     * 
     * @return a {@link forge.GameLog} object; may be null.
     * @since 1.2.0
     */
    public static GameLog getGameLog() {
        final GameState gameState = Singletons.getModel().getGameState();

        if (gameState != null) {
            return gameState.getGameLog();
        }

        return null;
    }

    /**
     * <p>
     * getCardFactory.
     * </p>
     * 
     * @return a {@link forge.card.cardfactory.CardFactoryInterface} object.
     * @since 1.0.15
     */
    public static CardFactoryInterface getCardFactory() {
        if (AllZone.cardFactory == null) {
            // setCardFactory(new
            // LazyCardFactory(ForgeProps.getFile(CARDSFOLDER)));
            AllZone.setCardFactory(new CardFactory(ForgeProps.getFile(NewConstants.CARDSFOLDER)));
        }
        return AllZone.cardFactory;
    }

    /**
     * Setter for cardFactory.
     * 
     * @param factory
     *            the factory to set
     */
    private static void setCardFactory(final CardFactoryInterface factory) {
        AllZone.cardFactory = factory;
    }

    /**
     * <p>
     * getStack.
     * </p>
     * 
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.game.zone.MagicStack} object.
     * @since 1.0.15
     */
    public static MagicStack getStack() {
        if (Singletons.getModel() != null) {
            return Singletons.getModel().getGameState().getStack();
        }

        return null;
    }

    /**
     * <p>
     * getInputControl.
     * </p>
     * 
     * @return a {@link forge.control.input.InputControl} object.
     * @since 1.0.15
     */
    public static InputControl getInputControl() {
        return AllZone.inputControl;
    }

    /** @param i0 &emsp; {@link forge.control.input.InputControl} */
    public static void setInputControl(InputControl i0) {
        AllZone.inputControl = i0;
    }

    /**
     * <p>
     * getStaticEffects.
     * </p>
     * 
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.StaticEffects} object.
     * @since 1.0.15
     */
    public static StaticEffects getStaticEffects() {
        final GameState gameState = Singletons.getModel().getGameState();

        if (gameState != null) {
            return gameState.getStaticEffects();
        }

        return null;
    }

    /**
     * <p>
     * getTriggerHandler.
     * </p>
     * 
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.card.trigger.TriggerHandler} object.
     * @since 1.0.15
     */
    public static TriggerHandler getTriggerHandler() {
        return Singletons.getModel().getGameState().getTriggerHandler();
    }

    /**
     * Gets the replacement handler.
     *
     * @return the replacement handler
     */
    public static ReplacementHandler getReplacementHandler() {
        return Singletons.getModel().getGameState().getReplacementHandler();
    }

    /**
     * <p>
     * getCombat.
     * </p>
     * 
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.game.phase.Combat} object.
     * @since 1.0.15
     */
    public static Combat getCombat() {
        return Singletons.getModel().getGameState().getCombat();
    }

    /**
     * <p>
     * setCombat.
     * </p>
     * 
     * Will eventually be marked deprecated.
     * 
     * @param attackers
     *            a {@link forge.game.phase.Combat} object.
     * @since 1.0.15
     */
    public static void setCombat(final Combat attackers) {
        Singletons.getModel().getGameState().setCombat(attackers);
    }

    /**
     * <p>
     * getStackZone.
     * </p>
     * 
     * Will eventually be marked deprecated.
     * 
     * @return a {@link forge.game.zone.PlayerZone} object.
     * @since 1.0.15
     */
    public static PlayerZone getStackZone() {
        return Singletons.getModel().getGameState().getStackZone();
    }

    /**
     * <p>
     * getZone.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.game.zone.PlayerZone} object.
     */
    public static PlayerZone getZoneOf(final Card c) {
        final GameState gameState = Singletons.getModel().getGameState();
        if (gameState == null) {
            return null;
        }

        if (gameState.getStackZone().contains(c)) {
            return gameState.getStackZone();
        }

        for (final Player p : gameState.getPlayers()) {
            for (final ZoneType z : Player.ALL_ZONES) {
                final PlayerZone pz = p.getZone(z);
                if (pz.contains(c)) {
                    return pz;
                }
            }
        }

        return null;
    }

    /**
     * 
     * isCardInZone.
     * 
     * @param c
     *            Card
     * @param zone
     *            Constant.Zone
     * @return boolean
     */
    public static boolean isCardInZone(final Card c, final ZoneType zone) {
        final GameState gameState = Singletons.getModel().getGameState();
        if (gameState == null) {
            return false;
        }

        if (zone.equals(ZoneType.Stack)) {
            if (gameState.getStackZone().contains(c)) {
                return true;
            }
        } else {
            for (final Player p : gameState.getPlayers()) {
                if (p.getZone(zone).contains(c)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * <p>
     * resetZoneMoveTracking.
     * </p>
     */
    public static void resetZoneMoveTracking() {
        final GameState gameState = Singletons.getModel().getGameState();
        if (gameState == null) {
            return;
        }
        for (final Player p : gameState.getPlayers()) {
            for (final ZoneType z : Player.ALL_ZONES) {
                p.getZone(z).resetCardsAddedThisTurn();
            }
        }
    }

    /**
     * Create and return the next timestamp.
     * 
     * Will eventually be marked deprecated.
     * 
     * @return the next timestamp
     */
    public static long getNextTimestamp() {
        return Singletons.getModel().getGameState().getNextTimestamp();
    }

    /**
     * Getter for colorChanger.
     * 
     * @return the colorChanger
     */
    public static ColorChanger getColorChanger() {
        return AllZone.COLOR_CHANGER;
    }


} // AllZone
