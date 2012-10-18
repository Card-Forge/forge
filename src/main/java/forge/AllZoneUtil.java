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
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.zone.ZoneType;


/**
 * AllZoneUtil contains static functions used to get CardLists of various cards
 * in various zones.
 * 
 * @author dennis.r.friedrichsen (slapshot5 on slightlymagic.net)
 * @version $Id$
 */
public abstract class AllZoneUtil {

    // ////////// Creatures

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
            return AllZone.getStackZone().getCards();
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
        return CardLists.filter(AllZoneUtil.getCardsIn(zone), CardPredicates.nameEquals(cardName));
    }

    // ////////// Creatures

    /**
     * use to get a List<Card> of all creatures on the battlefield for both.
     * players
     * 
     * @return a List<Card> of all creatures on the battlefield on both sides
     */
    public static List<Card> getCreaturesInPlay() {
        final List<Card> creats = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
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

    // /////////////// Lands

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
        return CardLists.filter(AllZoneUtil.getCardsIn(ZoneType.Battlefield), Presets.LANDS);
    }

    // =============================================================================
    //
    // These functions handle getting all cards for a given player
    // and all cards with a given name for either or both players
    //
    // =============================================================================

    /**
     * answers the question "is the given card in any exile zone?".
     * 
     * @param c
     *            the card to look for in Exile
     * @return true is the card is in Human or Computer's Exile zone
     */
    public static boolean isCardExiled(final Card c) {
        return AllZoneUtil.getCardsIn(ZoneType.Exile).contains(c);
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

    // ////////////// getting all cards of a given color

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

        for (final Card c : AllZoneUtil.getCardsInGame()) {
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
        all.addAll(AllZone.getStackZone().getCards());
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

    /**
     * gets a list of all opponents of a given player.
     * 
     * @param p
     *            the player whose opponents to get
     * @return a list of all opponents
     */
    public static ArrayList<Player> getOpponents(final Player p) {
        final ArrayList<Player> list = new ArrayList<Player>();
        list.add(p.getOpponent());
        return list;
    }

    /**
     * <p>
     * compare.
     * </p>
     * 
     * @param leftSide
     *            a int.
     * @param comp
     *            a {@link java.lang.String} object.
     * @param rightSide
     *            a int.
     * @return a boolean.
     * @since 1.0.15
     */
    public static boolean compare(final int leftSide, final String comp, final int rightSide) {
        // should this function be somewhere else?
        // leftSide COMPARED to rightSide:
        if (comp.contains("LT")) {
            return leftSide < rightSide;
        } else if (comp.contains("LE")) {
            return leftSide <= rightSide;
        } else if (comp.contains("EQ")) {
            return leftSide == rightSide;
        } else if (comp.contains("GE")) {
            return leftSide >= rightSide;
        } else if (comp.contains("GT")) {
            return leftSide > rightSide;
        } else if (comp.contains("NE")) {
            return leftSide != rightSide; // not equals
        } else if (comp.contains("M2")) {
            return (leftSide % 2) == (rightSide % 2); // they are equal modulo 2
        }

        return false;
    }

} // end class AllZoneUtil
