package forge;

import java.util.ArrayList;
import java.util.List;

import forge.Constant.Zone;

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
     * @param zone Constant.Zone
     * @return a CardList with all cards currently in a graveyard
     */
    public static CardList getCardsIn(final Constant.Zone zone) {
        CardList cards = new CardList();
        if (zone == Zone.Stack) {
            cards.addAll(AllZone.getStackZone().getCards());
        } else {
            for (Player p : AllZone.getPlayersInGame()) {
                cards.addAll(p.getZone(zone).getCards());
            }
        }
        return cards;
    }

    /**
     * 
     * getCardsIn.
     * @param zones a List<Constant.Zone>
     * @return CardList
     */
    public static CardList getCardsIn(final List<Constant.Zone> zones) {
        CardList cards = new CardList();
        for (Zone z : zones) {
            if (z == Zone.Stack) {
                cards.addAll(AllZone.getStackZone().getCards());
                continue;
            }

            for (Player p : AllZone.getPlayersInGame()) {
                cards.addAll(p.getZone(z).getCards());
            }
        }
        return cards;
    }

    /**
     * gets a list of all cards owned by both players that have are currently in
     * the given zone.
     * @param zone a Constant.Zone
     * @param cardName a String
     * @return a CardList with all cards currently in a graveyard
     */
    public static CardList getCardsIn(final Constant.Zone zone, final String cardName) {
        return getCardsIn(zone).getName(cardName);
    }

    // ////////// Creatures

    /**
     * use to get a CardList of all creatures on the battlefield for both.
     * players
     * 
     * @return a CardList of all creatures on the battlefield on both sides
     */
    public static CardList getCreaturesInPlay() {
        CardList creats = getCardsIn(Zone.Battlefield);
        return creats.filter(CardListFilter.creatures);
    }

    /**
     * use to get a list of creatures in play for a given player.
     * 
     * @param player
     *            the player to get creatures for
     * @return a CardList containing all creatures a given player has in play
     */
    public static CardList getCreaturesInPlay(final Player player) {
        CardList creats = player.getCardsIn(Zone.Battlefield);
        return creats.filter(CardListFilter.creatures);
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
        return player.getCardsIn(Zone.Battlefield).filter(CardListFilter.lands);
    }

    /**
     * gets a list of all lands in play.
     * 
     * @return a CardList of all lands on the battlefield
     */
    public static CardList getLandsInPlay() {
        return getCardsIn(Zone.Battlefield).filter(CardListFilter.lands);
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
        return getCardsIn(Zone.Exile).contains(c);
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
        return getCardsIn(Zone.Battlefield).contains(card);
    }

    /**
     * Answers the question: "Is <card name> in play?".
     * 
     * @param cardName
     *            the name of the card to look for
     * @return true is the card is in play, false otherwise
     */
    public static boolean isCardInPlay(final String cardName) {
        return getCardsIn(Zone.Battlefield, cardName).size() > 0;
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
        return player.getCardsIn(Zone.Battlefield, cardName).size() > 0;
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
        CardList cards = getPlayerColorInPlay(AllZone.getComputerPlayer(), color);
        cards.addAll(getPlayerColorInPlay(AllZone.getHumanPlayer(), color));
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
        CardList cards = player.getCardsIn(Zone.Battlefield);
        cards = cards.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                ArrayList<String> colorList = CardUtil.getColors(c);
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

        for (Card c : AllZoneUtil.getCardsInGame()) {
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
     *            a {@link forge.Player} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int compareTypeAmountInPlay(final Player player, final String type) {
        // returns the difference between player's
        Player opponent = player.getOpponent();
        CardList playerList = player.getCardsIn(Zone.Battlefield).getType(type);
        CardList opponentList = opponent.getCardsIn(Zone.Battlefield).getType(type);
        return (playerList.size() - opponentList.size());
    }

    /**
     * <p>
     * compareTypeAmountInGraveyard.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int compareTypeAmountInGraveyard(final Player player, final String type) {
        // returns the difference between player's
        Player opponent = player.getOpponent();
        CardList playerList = player.getCardsIn(Zone.Graveyard).getType(type);
        CardList opponentList = opponent.getCardsIn(Zone.Graveyard).getType(type);
        return (playerList.size() - opponentList.size());
    }

    /**
     * a CardListFilter to get all cards that are a part of this game.
     * 
     * @return a {@link forge.CardList} with all cards in all Battlefields,
     *         Hands, Graveyards, Libraries, and Exiles.
     */
    public static CardList getCardsInGame() {
        CardList all = new CardList();
        for (Player player : AllZone.getPlayersInGame()) {
            all.addAll(player.getZone(Zone.Graveyard).getCards());
            all.addAll(player.getZone(Zone.Hand).getCards());
            all.addAll(player.getZone(Zone.Library).getCards()); // not sure if
                                                                 // library
                                                                 // should be
                                                                 // included.
            all.addAll(player.getZone(Zone.Battlefield).getCards(false));
            all.addAll(player.getZone(Zone.Exile).getCards()); // Spawnsire of
                                                               // Ulamog plays
                                                               // spells from
                                                               // here?
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
     *            the {@link forge.Player} player to determine if is affected by
     *            Doubling Season
     * @return a int.
     */
    public static int getDoublingSeasonMagnitude(final Player player) {
        int doublingSeasons = player.getCardsIn(Zone.Battlefield, "Doubling Season").size();
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
     *            the {@link forge.Player} player to determine if is affected by
     *            Doubling Season
     * @return a int.
     */
    public static int getTokenDoublersMagnitude(final Player player) {
        int tokenDoublers = player.getCardsIn(Zone.Battlefield, "Parallel Lives").size()
                + player.getCardsIn(Zone.Battlefield, "Doubling Season").size();
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
        ArrayList<Player> list = new ArrayList<Player>();
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
        }

        return false;
    }

} // end class AllZoneUtil
