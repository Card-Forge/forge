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

import forge.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.closures.Predicate;

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
     * @return a CardList with all cards currently in a graveyard
     */
    public static CardList getCardsIn(final ZoneType zone) {
        final CardList cards = new CardList();
        getCardsIn(zone, cards);
        return cards;
    }

    private static void getCardsIn(final ZoneType zone, final CardList cards) {
        if (zone == ZoneType.Stack) {
            cards.addAll(AllZone.getStackZone().getCards());
        } else {
            for (final Player p : AllZone.getPlayersInGame()) {
                cards.addAll(p.getZone(zone).getCards());
            }
        }
    }

    public static CardList getCardsIn(final Iterable<ZoneType> zones) {
        final CardList cards = new CardList();
        for (final ZoneType z : zones) {
            getCardsIn(z, cards);
        }
        return cards;
    }

    public static CardList getCardsIn(final ZoneType[] zones) {
        final CardList cards = new CardList();
        for (final ZoneType z : zones) {
            getCardsIn(z, cards);
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
     * @return a CardList with all cards currently in a graveyard
     */
    public static CardList getCardsIn(final ZoneType zone, final String cardName) {
        return AllZoneUtil.getCardsIn(zone).filter(CardPredicates.nameEquals(cardName));
    }

    // ////////// Creatures

    /**
     * use to get a CardList of all creatures on the battlefield for both.
     * players
     * 
     * @return a CardList of all creatures on the battlefield on both sides
     */
    public static CardList getCreaturesInPlay() {
        final CardList creats = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        return creats.filter(Presets.CREATURES);
    }

    /**
     * use to get a list of creatures in play for a given player.
     * 
     * @param player
     *            the player to get creatures for
     * @return a CardList containing all creatures a given player has in play
     */
    public static CardList getCreaturesInPlay(final Player player) {
        final CardList creats = player.getCardsIn(ZoneType.Battlefield);
        return creats.filter(Presets.CREATURES);
    }

    // /////////////// Lands

    /**
     * use to get a list of all lands a given player has on the battlefield.
     * 
     * @param player
     *            the player whose lands we want to get
     * @return a CardList containing all lands the given player has in play
     */
    public static CardList getPlayerLandsInPlay(final Player player) {
        return player.getCardsIn(ZoneType.Battlefield).filter(Presets.LANDS);
    }

    /**
     * gets a list of all lands in play.
     * 
     * @return a CardList of all lands on the battlefield
     */
    public static CardList getLandsInPlay() {
        return AllZoneUtil.getCardsIn(ZoneType.Battlefield).filter(Presets.LANDS);
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
        for (Card card : AllZoneUtil.getCardsIn(ZoneType.Battlefield)) {
            if (card.getName().equals(cardName)) {
                return true;
            }
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
        for (Card card : player.getCardsIn(ZoneType.Battlefield)) {
            if (card.getName().equals(cardName)) {
                return true;
            }
        }
        return false;
    }

    // ////////////// getting all cards of a given color

    /**
     * gets a list of all Cards of a given color on the battlefield.
     * 
     * @param color
     *            the color of cards to get
     * @return a CardList of all cards in play of a given color
     */
    public static CardList getColorInPlay(final String color) {
        final CardList cards = AllZoneUtil.getPlayerColorInPlay(AllZone.getComputerPlayer(), color);
        cards.addAll(AllZoneUtil.getPlayerColorInPlay(AllZone.getHumanPlayer(), color));
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
     * @return a CardList of all cards in play of a given color
     */
    public static CardList getPlayerColorInPlay(final Player player, final String color) {
        CardList cards = player.getCardsIn(ZoneType.Battlefield);
        cards = cards.filter(new Predicate<Card>() {
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
        final CardList playerList = player.getCardsIn(ZoneType.Battlefield).getType(type);
        final CardList opponentList = opponent.getCardsIn(ZoneType.Battlefield).getType(type);
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
        final CardList playerList = player.getCardsIn(ZoneType.Graveyard).getType(type);
        final CardList opponentList = opponent.getCardsIn(ZoneType.Graveyard).getType(type);
        return (playerList.size() - opponentList.size());
    }

    /**
     * a CardListFilter to get all cards that are a part of this game.
     * 
     * @return a {@link forge.CardList} with all cards in all Battlefields,
     *         Hands, Graveyards, Libraries, and Exiles.
     */
    public static CardList getCardsInGame() {
        final CardList all = new CardList();
        for (final Player player : AllZone.getPlayersInGame()) {
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
    public static int getDoublingSeasonMagnitude(final Player player) {
        final int doublingSeasons = player.getCardsIn(ZoneType.Battlefield, "Doubling Season").size();
        return (int) Math.pow(2, doublingSeasons); // pow(a,0) = 1; pow(a,1) = a
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

    /**
     * <p>
     * matchesValid.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     * @param valids
     *            an array of {@link java.lang.String} objects.
     * @param srcCard
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    /*public static boolean matchesValid(final Object o, final String[] valids, final Card srcCard) {
        if (o instanceof GameEntity) {
            final GameEntity c = (GameEntity) o;
            return c.isValid(valids, srcCard.getController(), srcCard);
        }

        return false;
    }*/

} // end class AllZoneUtil
