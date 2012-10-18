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
package forge.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.CardUtil;
import forge.Counters;
import forge.GameLog;
import forge.Singletons;
import forge.StaticEffects;
import forge.card.replacement.ReplacementHandler;
import forge.card.trigger.TriggerHandler;
import forge.game.phase.Cleanup;
import forge.game.phase.Combat;
import forge.game.phase.EndOfCombat;
import forge.game.phase.EndOfTurn;
import forge.game.phase.PhaseHandler;
import forge.game.phase.Untap;
import forge.game.phase.Upkeep;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.MagicStack;
import forge.game.zone.ZoneType;

/**
 * Represents the state of a <i>single game</i> and is
 * "cleaned up" at each new game.
 */
public class GameState {

    /** The Constant HUMAN_PLAYER_NAME. */
    public static final String HUMAN_PLAYER_NAME = "Human";

    /** The Constant AI_PLAYER_NAME. */
    public static final String AI_PLAYER_NAME = "Computer";

    private final List<Player> roPlayers;
    private final Cleanup cleanup = new Cleanup();
    private final EndOfTurn endOfTurn = new EndOfTurn();
    private final EndOfCombat endOfCombat = new EndOfCombat();
    private final Untap untap = new Untap();
    private final Upkeep upkeep = new Upkeep();
    private PhaseHandler phaseHandler = new PhaseHandler();
    private final MagicStack stack = new MagicStack();
    private final StaticEffects staticEffects = new StaticEffects();
    private final TriggerHandler triggerHandler = new TriggerHandler();
    private final ReplacementHandler replacementHandler = new ReplacementHandler();
    private Combat combat = new Combat();
    private final GameLog gameLog = new GameLog();
    private boolean gameOver = false;

    private final PlayerZone stackZone = new PlayerZone(ZoneType.Stack, null);

    private long timestamp = 0;
    private int nTurn = 0;
    
    /**
     * Constructor.
     * @param players2 
     */
    public GameState(Iterable<LobbyPlayer> players2) { /* no more zones to map here */
        List<Player> players = new ArrayList<Player>();
        for(LobbyPlayer p : players2) {
            players.add(p.getIngamePlayer());
        }
        roPlayers = Collections.unmodifiableList(players);
    }

    /**
     * Gets the players.
     * 
     * @return the players
     */
    public final List<Player> getPlayers() {
        return roPlayers;
    }

    /**
     * Gets the cleanup step.
     * 
     * @return the cleanup step
     */
    public final Cleanup getCleanup() {
        return this.cleanup;
    }

    /**
     * Gets the end of turn.
     * 
     * @return the endOfTurn
     */
    public final EndOfTurn getEndOfTurn() {
        return this.endOfTurn;
    }

    /**
     * Gets the end of combat.
     * 
     * @return the endOfCombat
     */
    public final EndOfCombat getEndOfCombat() {
        return this.endOfCombat;
    }

    /**
     * Gets the upkeep.
     * 
     * @return the upkeep
     */
    public final Upkeep getUpkeep() {
        return this.upkeep;
    }

    /**
     * Gets the untap.
     * 
     * @return the upkeep
     */
    public final Untap getUntap() {
        return this.untap;
    }

    /**
     * Gets the phaseHandler.
     * 
     * @return the phaseHandler
     */
    public final PhaseHandler getPhaseHandler() {
        return this.phaseHandler;
    }

    /**
     * Gets the stack.
     * 
     * @return the stack
     */
    public final MagicStack getStack() {
        return this.stack;
    }

    /**
     * Gets the static effects.
     * 
     * @return the staticEffects
     */
    public final StaticEffects getStaticEffects() {
        return this.staticEffects;
    }

    /**
     * Gets the trigger handler.
     * 
     * @return the triggerHandler
     */
    public final TriggerHandler getTriggerHandler() {
        return this.triggerHandler;
    }

    /**
     * Gets the combat.
     * 
     * @return the combat
     */
    public final Combat getCombat() {
        return this.combat;
    }

    /**
     * Sets the combat.
     * 
     * @param combat0
     *            the combat to set
     */
    public final void setCombat(final Combat combat0) {
        this.combat = combat0;
    }

    /**
     * Gets the game log.
     * 
     * @return the game log
     */
    public final GameLog getGameLog() {
        return this.gameLog;
    }

    /**
     * Gets the stack zone.
     * 
     * @return the stackZone
     */
    public final PlayerZone getStackZone() {
        return this.stackZone;
    }

    /**
     * Create and return the next timestamp.
     * 
     * @return the next timestamp
     */
    public final long getNextTimestamp() {
        this.setTimestamp(this.getTimestamp() + 1);
        return this.getTimestamp();
    }

    /**
     * Gets the timestamp.
     * 
     * @return the timestamp
     */
    public final long getTimestamp() {
        return this.timestamp;
    }

    /**
     * Sets the timestamp.
     * 
     * @param timestamp0
     *            the timestamp to set
     */
    protected final void setTimestamp(final long timestamp0) {
        this.timestamp = timestamp0;
    }


    /**
     * @return the replacementHandler
     */
    public ReplacementHandler getReplacementHandler() {
        return replacementHandler;
    }

    /**
     * @return the gameOver
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * @param go the gameOver to set
     */
    public void setGameOver() {
        this.gameOver = true;
        for(Player p : roPlayers) {
            p.onGameOver();
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public int getTurnNumber() {
        return nTurn;
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void notifyNextTurn() {
        nTurn++;
    }

    
    // THESE WERE MOVED HERE FROM AllZoneUtil 
    // They must once become non-static members of this class 
    
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
     * gets a list of all cards owned by both players that have are currently in
     * the given zone.
     * 
     * @param zone
     *            Constant.Zone
     * @return a List<Card> with all cards currently in a graveyard
     */
    public static List<Card> getCardsIn(final ZoneType zone) {
        if (zone == ZoneType.Stack) {
            return Singletons.getModel().getGameState().getStackZone().getCards();
        } else {
            List<Card> cards = null;
            for (final Player p : Singletons.getModel().getGameState().getPlayers()) {
                if ( cards == null ) 
                    cards = p.getZone(zone).getCards();
                else
                    cards.addAll(p.getZone(zone).getCards());
            }
            return cards;
        }
    }

    public static List<Card> getCardsIn(final Iterable<ZoneType> zones) {
        final List<Card> cards = new ArrayList<Card>();
        for (final ZoneType z : zones) {
            cards.addAll(getCardsIn(z));
        }
        return cards;
    }

    /**
     * gets a list of all cards owned by both players that have are currently in
     * the given zone.
     * 
     * @param zone
     *            a Constant.Zone
     * @param cardName
     *            a String
     * @return a List<Card> with all cards currently in a graveyard
     */
    public static List<Card> getCardsIn(final ZoneType zone, final String cardName) {
        return CardLists.filter(GameState.getCardsIn(zone), CardPredicates.nameEquals(cardName));
    }

    /**
     * use to get a List<Card> of all creatures on the battlefield for both.
     * players
     * 
     * @return a List<Card> of all creatures on the battlefield on both sides
     */
    public static List<Card> getCreaturesInPlay() {
        final List<Card> creats = GameState.getCardsIn(ZoneType.Battlefield);
        return CardLists.filter(creats, Presets.CREATURES);
    }

    /**
     * use to get a list of creatures in play for a given player.
     * 
     * @param player
     *            the player to get creatures for
     * @return a List<Card> containing all creatures a given player has in play
     */
    public static List<Card> getCreaturesInPlay(final Player player) {
        final List<Card> creats = player.getCardsIn(ZoneType.Battlefield);
        return CardLists.filter(creats, Presets.CREATURES);
    }

    /**
     * use to get a list of all lands a given player has on the battlefield.
     * 
     * @param player
     *            the player whose lands we want to get
     * @return a List<Card> containing all lands the given player has in play
     */
    public static List<Card> getPlayerLandsInPlay(final Player player) {
        return CardLists.filter(player.getCardsIn(ZoneType.Battlefield), Presets.LANDS);
    }

    /**
     * gets a list of all lands in play.
     * 
     * @return a List<Card> of all lands on the battlefield
     */
    public static List<Card> getLandsInPlay() {
        return CardLists.filter(GameState.getCardsIn(ZoneType.Battlefield), Presets.LANDS);
    }

    /**
     * answers the question "is the given card in any exile zone?".
     * 
     * @param c
     *            the card to look for in Exile
     * @return true is the card is in Human or Computer's Exile zone
     */
    public static boolean isCardExiled(final Card c) {
        return GameState.getCardsIn(ZoneType.Exile).contains(c);
    }

    // /Check if a certain card is in play
    /**
     * <p>
     * isCardInPlay.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isCardInPlay(final Card card) {
        if (card.getController() == null) {
            return false;
        }
        return card.getController().getCardsIn(ZoneType.Battlefield).contains(card);
    }

    /**
     * Answers the question: "Is <card name> in play?".
     * 
     * @param cardName
     *            the name of the card to look for
     * @return true is the card is in play, false otherwise
     */
    public static boolean isCardInPlay(final String cardName) {
        for (final Player p : Singletons.getModel().getGameState().getPlayers()) {
            if (isCardInPlay(cardName, p))
                return true;
        }
        return false;
    }

    /**
     * Answers the question: "Does <player> have <card name> in play?".
     * 
     * @param cardName
     *            the name of the card to look for
     * @param player
     *            the player whose battlefield we want to check
     * @return true if that player has that card in play, false otherwise
     */
    public static boolean isCardInPlay(final String cardName, final Player player) {
        return Iterables.any(player.getZone(ZoneType.Battlefield), CardPredicates.nameEquals(cardName));
    }

    /**
     * gets a list of all Cards of a given color on the battlefield.
     * 
     * @param color
     *            the color of cards to get
     * @return a List<Card> of all cards in play of a given color
     */
    public static List<Card> getColorInPlay(final String color) {
        final List<Card> cards = new ArrayList<Card>();
        for(Player p : Singletons.getModel().getGameState().getPlayers()) {
            cards.addAll(getPlayerColorInPlay(p, color));
        }
        return cards;
    }

    /**
     * gets a list of all Cards of a given color a given player has on the
     * battlefield.
     * 
     * @param player
     *            the player's cards to get
     * @param color
     *            the color of cards to get
     * @return a List<Card> of all cards in play of a given color
     */
    public static List<Card> getPlayerColorInPlay(final Player player, final String color) {
        List<Card> cards = player.getCardsIn(ZoneType.Battlefield);
        cards = CardLists.filter(cards, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                final List<String> colorList = CardUtil.getColors(c);
                return colorList.contains(color);
            }
        });
        return cards;
    }

    /**
     * <p>
     * getCardState.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCardState(final Card card) {
    
        for (final Card c : GameState.getCardsInGame()) {
            if (card.equals(c)) {
                return c;
            }
        }
    
        return card;
    }

    /**
     * <p>
     * compareTypeAmountInPlay.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int compareTypeAmountInPlay(final Player player, final String type) {
        // returns the difference between player's
        final Player opponent = player.getOpponent();
        final List<Card> playerList = CardLists.getType(player.getCardsIn(ZoneType.Battlefield), type);
        final List<Card> opponentList = CardLists.getType(opponent.getCardsIn(ZoneType.Battlefield), type);
        return (playerList.size() - opponentList.size());
    }

    /**
     * <p>
     * compareTypeAmountInGraveyard.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int compareTypeAmountInGraveyard(final Player player, final String type) {
        // returns the difference between player's
        final Player opponent = player.getOpponent();
        final List<Card> playerList = CardLists.getType(player.getCardsIn(ZoneType.Graveyard), type);
        final List<Card> opponentList = CardLists.getType(opponent.getCardsIn(ZoneType.Graveyard), type);
        return (playerList.size() - opponentList.size());
    }

    /**
     * a CardListFilter to get all cards that are a part of this game.
     * 
     * @return a {@link forge.CardList} with all cards in all Battlefields,
     *         Hands, Graveyards, Libraries, and Exiles.
     */
    public static List<Card> getCardsInGame() {
        final List<Card> all = new ArrayList<Card>();
        for (final Player player : Singletons.getModel().getGameState().getPlayers()) {
            all.addAll(player.getZone(ZoneType.Graveyard).getCards());
            all.addAll(player.getZone(ZoneType.Hand).getCards());
            all.addAll(player.getZone(ZoneType.Library).getCards());
            all.addAll(player.getZone(ZoneType.Battlefield).getCards(false));
            all.addAll(player.getZone(ZoneType.Exile).getCards());
        }
        all.addAll(Singletons.getModel().getGameState().getStackZone().getCards());
        return all;
    }

    /**
     * <p>
     * getDoublingSeasonMagnitude.
     * </p>
     * 
     * @param player
     *            the {@link forge.game.player.Player} player to determine if is affected by
     *            Doubling Season
     * @return a int.
     */
    public static int getCounterDoublersMagnitude(final Player player, Counters type) {
        int counterDoublers = player.getCardsIn(ZoneType.Battlefield, "Doubling Season").size();
        if(type == Counters.P1P1) {
            counterDoublers += player.getCardsIn(ZoneType.Battlefield, "Corpsejack Menace").size();
        }
        return (int) Math.pow(2, counterDoublers); // pow(a,0) = 1; pow(a,1) = a
                                                   // ... no worries about size
                                                   // = 0
    }

    /**
     * <p>
     * getTokenDoublersMagnitude.
     * </p>
     * 
     * @param player
     *            the {@link forge.game.player.Player} player to determine if is affected by
     *            Doubling Season
     * @return a int.
     */
    public static int getTokenDoublersMagnitude(final Player player) {
        final int tokenDoublers = player.getCardsIn(ZoneType.Battlefield, "Parallel Lives").size()
                + player.getCardsIn(ZoneType.Battlefield, "Doubling Season").size();
        return (int) Math.pow(2, tokenDoublers); // pow(a,0) = 1; pow(a,1) = a
                                                 // ... no worries about size =
                                                 // 0
    }
}
