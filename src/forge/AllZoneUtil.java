package forge;


import forge.card.cardFactory.CardFactoryUtil;

import java.util.ArrayList;

/**
 * AllZoneUtil contains static functions used to get CardLists of various
 * cards in various zones.
 *
 * @author dennis.r.friedrichsen (slapshot5 on slightlymagic.net)
 * @version $Id: $
 */
public class AllZoneUtil {

    //////////// Creatures

    /**
     * use to get a list of creatures in play for a given player
     *
     * @param player the player to get creatures for
     * @return a CardList containing all creatures a given player has in play
     */
    public static CardList getCreaturesInPlay(final Player player) {
        CardList creatures = AllZoneUtil.getPlayerCardsInPlay(player);
        return creatures.filter(AllZoneUtil.creatures);
    }

    /**
     * use to get a list of creatures in play with a given keyword
     *
     * @param keyword the keyword to get creatures for
     * @return a CardList containing all creatures in play with a given keyword
     */
    public static CardList getCreaturesInPlayWithKeyword(final String keyword) {
        CardList list = getCreaturesInPlay();
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.hasKeyword(keyword);
            }
        });
        return list;
    }

    /**
     * use to get a CardList of all creatures on the battlefield for both players
     *
     * @return a CardList of all creatures on the battlefield on both sides
     */
    public static CardList getCreaturesInPlay() {
        CardList creatures = getCardsInPlay();
        return creatures.filter(AllZoneUtil.creatures);
    }

    ///////////////// Lands

    /**
     * use to get a list of all lands a given player has on the battlefield
     *
     * @param player the player whose lands we want to get
     * @return a CardList containing all lands the given player has in play
     */
    public static CardList getPlayerLandsInPlay(final Player player) {
        CardList cards = getPlayerCardsInPlay(player);
        return cards.filter(lands);
    }

    /**
     * gets a list of all lands in play
     *
     * @return a CardList of all lands on the battlefield
     */
    public static CardList getLandsInPlay() {
        CardList lands = new CardList();
        lands.addAll(getPlayerLandsInPlay(AllZone.getHumanPlayer()));
        lands.addAll(getPlayerLandsInPlay(AllZone.getComputerPlayer()));
        return lands;
    }

    //=============================================================================
    //
    // These functions handle getting all cards for a given player
    // and all cards with a given name for either or both players
    //
    //=============================================================================

    /**
     * gets a list of all cards in play on both sides
     *
     * @return a CardList of all cards in play on both sides
     */
    public static CardList getCardsInPlay() {
        CardList cards = new CardList();
        cards.addAll(getCardsInPlay(null));
        return cards;
    }

    /**
     * gets a list of all cards in play with a given card name
     *
     * @param cardName the name of the card to search for
     * @return a CardList with all cards in play of the given name
     */
    public static CardList getCardsInPlay(final String cardName) {
        CardList cards = new CardList();
        cards.addAll(getPlayerCardsInPlay(AllZone.getHumanPlayer()));
        cards.addAll(getPlayerCardsInPlay(AllZone.getComputerPlayer()));
        if (cardName != null && !"".equals(cardName)) {
            cards = cards.getName(cardName);
        }
        return cards;
    }

    /**
     * gets a list of all cards that a given Player has in play
     *
     * @param player the player's cards to get
     * @return a CardList with all cards in the Play zone for the given player
     */
    public static CardList getPlayerCardsInPlay(final Player player) {
        CardList cards = new CardList();
        if (player.isHuman() || player.isComputer()) {
            PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);
            cards.addAll(play.getCards());
        }
        return cards;
    }

    /**
     * gets a list of all cards with a given name a given player has in play
     *
     * @param player   the player whose cards in play you want to get
     * @param cardName the card name to look for in that zone
     * @return a CardList with all cards of a given name the player has in play
     */
    public static CardList getPlayerCardsInPlay(final Player player, final String cardName) {
        CardList cards = new CardList();

        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);
        cards.addAll(play.getCards());

        if (cardName != null && !"".equals(cardName)) {
            cards = cards.getName(cardName);
        }
        return cards;
    }

    //////////GRAVEYARD


    /**
     * gets a list of all cards owned by both players that have are currently in the graveyard
     *
     * @return a CardList with all cards currently in a graveyard
     */
    public static CardList getCardsInGraveyard() {
        CardList cards = new CardList();
        cards.addAll(getPlayerGraveyard(AllZone.getHumanPlayer()));
        cards.addAll(getPlayerGraveyard(AllZone.getComputerPlayer()));
        return cards;
    }


    /**
     * gets all cards in given player's graveyard
     *
     * @param player the player whose graveyard we want to get
     * @return a CardList containing all cards in that player's graveyard
     */
    public static CardList getPlayerGraveyard(final Player player) {
        CardList cards = new CardList();
        if (player.isHuman() || player.isComputer()) {
            PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
            cards.addAll(grave.getCards());
        }
        return cards;
    }

    /**
     * gets a list of all cards with a given name in a certain player's graveyard
     *
     * @param player   the player whose graveyard we want to get
     * @param cardName the card name to find in the graveyard
     * @return a CardList containing all cards with that name in the target graveyard
     */
    public static CardList getPlayerGraveyard(final Player player, final String cardName) {
        CardList cards = getPlayerGraveyard(player);
        cards = cards.getName(cardName);
        return cards;
    }

    // Get a Cards in All Graveyards with a certain name

    /**
     * gets a CardList of all cards with a given name in all graveyards
     *
     * @param cardName the card name to look for
     * @return a CardList of all cards with the given name in all graveyards
     */
    public static CardList getCardsInGraveyard(final String cardName) {
        CardList cards = new CardList();
        cards.addAll(getPlayerGraveyard(AllZone.getHumanPlayer()));
        cards.addAll(getPlayerGraveyard(AllZone.getComputerPlayer()));
        cards = cards.getName(cardName);
        return cards;
    }

    /**
     * answers the question "is a certain, specific card in this player's graveyard?"
     *
     * @param player the player's hand to check
     * @param card   the specific card to look for
     * @return true if the card is present in this player's hand; false otherwise
     */
    public static boolean isCardInPlayerGraveyard(Player player, Card card) {
        return isCardInZone(AllZone.getZone(Constant.Zone.Graveyard, player), card);
    }

    //////// HAND

    /**
     * gets a list of all cards in a given player's hand
     *
     * @param player the player's hand to target
     * @return a CardList containing all cards in target player's hand
     */
    public static CardList getPlayerHand(final Player player) {
        CardList cards = new CardList();
        if (player.isHuman() || player.isComputer()) {
            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
            cards.addAll(hand.getCards());
        }
        return cards;
    }

    /**
     * answers the question "is a certain, specific card in this player's hand?"
     *
     * @param player the player's hand to check
     * @param card   the specific card to look for
     * @return true if the card is present in this player's hand; false otherwise
     */
    public static boolean isCardInPlayerHand(Player player, Card card) {
        return isCardInZone(AllZone.getZone(Constant.Zone.Hand, player), card);
    }

    /**
     * answers the question "is a specific card in this player's library?"
     *
     * @param player the player's library to check
     * @param card   the specific card to look for
     * @return true if the card is present in this player's library; false otherwise
     */
    public static boolean isCardInPlayerLibrary(Player player, Card card) {
        return isCardInZone(AllZone.getZone(Constant.Zone.Library, player), card);
    }

    /**
     * answers the question "is a specific card in the specified zone?"
     *
     * @param pz   the PlayerZone to check
     * @param card the specific card to look for
     * @return true if the card is present in this zone; false otherwise
     */
    public static boolean isCardInZone(PlayerZone pz, Card card) {
        if (card == null)
            return false;

        CardList cl = getCardsInZone(pz);

        for (int i = 0; i < cl.size(); i++)
            if (cl.get(i).equals(card))
                return true;

        return false;
    }

    ////////////// EXILE

    /**
     * gets a list of all cards owned by both players that are in Exile
     *
     * @return a CardList with all cards in Exile
     */
    public static CardList getCardsInExile() {
        CardList cards = new CardList();
        cards.addAll(getPlayerCardsInExile(AllZone.getComputerPlayer()));
        cards.addAll(getPlayerCardsInExile(AllZone.getHumanPlayer()));
        return cards;
    }

    /**
     * gets a list of all cards in Exile for a given player
     *
     * @param player the player whose cards we want that are in Exile
     * @return a CardList with all cards in Exile for a given player
     */
    public static CardList getPlayerCardsInExile(final Player player) {
        CardList cards = new CardList();
        if (player.isHuman() || player.isComputer()) {
            PlayerZone removed = AllZone.getZone(Constant.Zone.Exile, player);
            cards.addAll(removed.getCards());
        }
        return cards;
    }

    /**
     * gets a list of all cards with a given name in a certain player's exile
     *
     * @param player   the player whose exile we want to get
     * @param cardName the card name to find in the exile
     * @return a CardList containing all cards with that name in the target exile
     */
    public static CardList getPlayerCardsInExile(final Player player, final String cardName) {
        CardList cards = getPlayerCardsInExile(player);
        cards = cards.getName(cardName);
        return cards;
    }

    //////////////////////// LIBRARY

    /**
     * gets a list of all cards in a given player's library
     *
     * @return a CardList with all the cards currently in that player's library
     * @param player a {@link forge.Player} object.
     */
    public static CardList getPlayerCardsInLibrary(final Player player) {
        CardList cards = new CardList();
        if (player.isHuman() || player.isComputer()) {
            PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
            cards.addAll(lib.getCards());
        }
        return cards;
    }

    /**
     * gets a list of all cards with a certain name in a given player's library
     *
     * @param player   the player's library one is interested in
     * @param cardName the card's name that one is interested in
     * @return a CardList of all cards of the given name in the given player's library
     */
    public static CardList getPlayerCardsInLibrary(final Player player, final String cardName) {
        CardList cards = getPlayerCardsInLibrary(player);
        return cards.getName(cardName);
    }

    /**
     * gets a list of a given number of cards from the top of given player's library
     *
     * @param player   the player's library one is interested in
     * @param numCards the number of cards to get from the top
     * @return a CardList of the top number of cards in the given player's library
     */
    public static CardList getPlayerCardsInLibrary(final Player player, int numCards) {
        CardList cards = new CardList();
        if (player.isHuman() || player.isComputer()) {
            PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);

            if (lib.size() <= numCards)
                cards.addAll(lib.getCards());
            else {
                for (int i = 0; i < numCards; i++)
                    cards.add(lib.get(i));
            }
        }
        return cards;
    }

    /**
     * answers the question "is the given card in any exile zone?"
     *
     * @param c the card to look for in Exile
     * @return true is the card is in Human or Computer's Exile zone
     */
    public static boolean isCardExiled(Card c) {
        return getCardsInExile().contains(c);
    }

    /**
     * <p>isCardInGrave.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isCardInGrave(Card c) {
        return getCardsInGraveyard().contains(c);
    }

    ///Check if a certain card is in play

    /**
     * <p>isCardInPlay.</p>
     *
     * @param card a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isCardInPlay(Card card) {
        return getCardsInPlay().contains(card);
    }

    /**
     * Answers the question: "Is <card name> in play?"
     *
     * @param cardName the name of the card to look for
     * @return true is the card is in play, false otherwise
     */
    public static boolean isCardInPlay(final String cardName) {
        return getCardsInPlay(cardName).size() > 0;
    }

    /**
     * Answers the question: "Does <player> have <card name> in play?"
     *
     * @param cardName the name of the card to look for
     * @param player   the player whose battlefield we want to check
     * @return true if that player has that card in play, false otherwise
     */
    public static boolean isCardInPlay(final String cardName, final Player player) {
        return getPlayerCardsInPlay(player, cardName).size() > 0;
    }
    
	/**
	 * Answers the question: "Does <player> have <given card> in play?"
	 *
	 * @param card the card to look for
	 * @param player the player whose battlefield we want to check
	 * @return true if that player has that card in play, false otherwise
	 * @since 1.0.15
	 */
	public static boolean isCardInPlay(final Card card, final Player player) {
		return getPlayerCardsInPlay(player).contains(card);
	}

    ///get a list of certain types are in play (like Mountain, Elf, etc...)

    /**
     * gets a list of all cards with a certain type (Mountain, Elf, etc...) in play
     *
     * @param cardType the type to find in play
     * @return a CardList with all cards of the given type in play
     */
    public static CardList getTypeInPlay(final String cardType) {
        CardList cards = getCardsInPlay();
        cards = cards.getType(cardType);
        return cards;
    }

    /**
     * gets a list of all cards of a certain type that a given player has in play
     *
     * @param player   the player to check for cards in play
     * @param cardType the card type to check for
     * @return a CardList with all cards of a certain type the player has in play
     */
    public static CardList getPlayerTypeInPlay(final Player player, final String cardType) {
        CardList cards = getPlayerCardsInPlay(player);
        cards = cards.getType(cardType);
        return cards;
    }

    /**
     * gets a list of all cards of a certain type that a given player has in his library
     *
     * @param player   the player to check for cards in play
     * @param cardType the card type to check for
     * @return a CardList with all cards of a certain type the player has in his library
     */
    public static CardList getPlayerTypeInLibrary(final Player player, final String cardType) {
        CardList cards = getPlayerCardsInLibrary(player);
        cards = cards.getType(cardType);
        return cards;
    }

    /**
     * gets a list of all cards of a certain type that a given player has in graveyard
     *
     * @param player   the player to check for cards in play
     * @param cardType the card type to check for
     * @return a CardList with all cards of a certain type the player has in graveyard
     */
    public static CardList getPlayerTypeInGraveyard(final Player player, final String cardType) {
        CardList cards = getPlayerGraveyard(player);
        cards = cards.getType(cardType);
        return cards;
    }

    //////////////// getting all cards of a given color

    /**
     * gets a list of all Cards of a given color on the battlefield
     *
     * @param color the color of cards to get
     * @return a CardList of all cards in play of a given color
     */
    public static CardList getColorInPlay(final String color) {
        CardList cards = getPlayerColorInPlay(AllZone.getComputerPlayer(), color);
        cards.addAll(getPlayerColorInPlay(AllZone.getHumanPlayer(), color));
        return cards;
    }

    /**
     * gets a list of all Cards of a given color a given player has on the battlefield
     *
     * @param player the player's cards to get
     * @param color  the color of cards to get
     * @return a CardList of all cards in play of a given color
     */
    public static CardList getPlayerColorInPlay(final Player player, final String color) {
        CardList cards = getPlayerCardsInPlay(player);
        cards = cards.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                ArrayList<String> colorList = CardUtil.getColors(c);
                return colorList.contains(color);
            }
        });
        return cards;
    }

    /**
     * <p>getCardState.</p>
     *
     * @param card a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCardState(Card card) {
        PlayerZone zone = AllZone.getZone(card);
        if (zone == null)    // for tokens
            return null;

        CardList list = getCardsInZone(zone.getZoneName());
        for (Card c : list) {
            if (card.equals(c))
                return c;
        }

        return card;
    }

    /**
     * <p>getCardsInZone.</p>
     *
     * @param zone a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getCardsInZone(String zone) {
        return getCardsInZone(zone, null);
    }

    /**
     * <p>getCardsInZone.</p>
     *
     * @param zone a {@link forge.PlayerZone} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getCardsInZone(PlayerZone zone) {
        return new CardList(zone.getCards());
    }

    /**
     * <p>getCardsInZone.</p>
     *
     * @param zone a {@link java.lang.String} object.
     * @param player a {@link forge.Player} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getCardsInZone(String zone, Player player) {
        CardList all = new CardList();

        if (zone.contains(Constant.Zone.Graveyard)) {
            if (player == null || player.isHuman())
                all.addAll(AllZone.getHumanGraveyard().getCards());
            if (player == null || player.isComputer())
                all.addAll(AllZone.getComputerGraveyard().getCards());
        }
        if (zone.contains(Constant.Zone.Hand)) {
            if (player == null || player.isHuman())
                all.addAll(AllZone.getHumanHand().getCards());
            if (player == null || player.isComputer())
                all.addAll(AllZone.getComputerHand().getCards());
        }
        if (zone.contains(Constant.Zone.Battlefield)) {
            if (player == null || player.isHuman())
                all.addAll(AllZone.getHumanBattlefield().getCards());
            if (player == null || player.isComputer())
                all.addAll(AllZone.getComputerBattlefield().getCards());
        }
        if (zone.contains(Constant.Zone.Exile)) {
            if (player == null || player.isHuman())
                all.addAll(AllZone.getHumanExile().getCards());
            if (player == null || player.isComputer())
                all.addAll(AllZone.getComputerExile().getCards());
        }
        if (zone.contains(Constant.Zone.Library)) {
            if (player == null || player.isHuman())
                all.addAll(AllZone.getHumanLibrary().getCards());
            if (player == null || player.isComputer())
                all.addAll(AllZone.getComputerLibrary().getCards());
        }

        return all;
    }

    /**
     * <p>compareTypeAmountInPlay.</p>
     *
     * @param player a {@link forge.Player} object.
     * @param type a {@link java.lang.String} object.
     * @return a int.
     */
    public static int compareTypeAmountInPlay(final Player player, String type) {
        // returns the difference between player's
        Player opponent = player.getOpponent();
        CardList playerList = getPlayerTypeInPlay(player, type);
        CardList opponentList = getPlayerTypeInPlay(opponent, type);
        return (playerList.size() - opponentList.size());
    }

    /**
     * <p>compareTypeAmountInGraveyard.</p>
     *
     * @param player a {@link forge.Player} object.
     * @param type a {@link java.lang.String} object.
     * @return a int.
     */
    public static int compareTypeAmountInGraveyard(final Player player, String type) {
        // returns the difference between player's
        Player opponent = player.getOpponent();
        CardList playerList = getPlayerTypeInGraveyard(player, type);
        CardList opponentList = getPlayerTypeInGraveyard(opponent, type);
        return (playerList.size() - opponentList.size());
    }


    /**
     * a CardListFilter to get all cards that are tapped
     */
    public static final CardListFilter tapped = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isTapped();
        }
    };

    /**
     * a CardListFilter to get all cards that are untapped
     */
    public static final CardListFilter untapped = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isUntapped();
        }
    };

    /**
     * a CardListFilter to get all creatures
     */
    public static final CardListFilter creatures = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isCreature();
        }
    };

    /**
     * a CardListFilter to get all enchantments
     */
    public static final CardListFilter enchantments = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isEnchantment();
        }
    };

    /**
     * a CardListFilter to get all equipment
     */
    public static final CardListFilter equipment = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isEquipment();
        }
    };

    /**
     * a CardListFilter to get all unenchanted cards in a list
     */
    public static final CardListFilter unenchanted = new CardListFilter() {
        public boolean addCard(Card c) {
            return !c.isEnchanted();
        }
    };

    /**
     * a CardListFilter to get all enchanted cards in a list
     */
    public static final CardListFilter enchanted = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isEnchanted();
        }
    };

    /**
     * a CardListFilter to get all nontoken cards
     */
    public static final CardListFilter nonToken = new CardListFilter() {
        public boolean addCard(Card c) {
            return !c.isToken();
        }
    };

    /**
     * a CardListFilter to get all token cards
     */
    public static final CardListFilter token = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isToken();
        }
    };

    /**
     * a CardListFilter to get all nonbasic lands
     */
    public static final CardListFilter nonBasicLand = new CardListFilter() {
        public boolean addCard(Card c) {
            return !c.isBasicLand();
        }
    };

    /**
     * a CardListFilter to get all basicLands
     */
    public static final CardListFilter basicLands = new CardListFilter() {
        public boolean addCard(Card c) {
            //the isBasicLand() check here may be sufficient...
            return c.isLand() && c.isBasicLand();
        }
    };

    /**
     * a CardListFilter to get all artifacts
     */
    public static final CardListFilter artifacts = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isArtifact();
        }
    };

    /**
     * a CardListFilter to get all nonartifacts
     */
    public static final CardListFilter nonartifacts = new CardListFilter() {
        public boolean addCard(Card c) {
            return !c.isArtifact();
        }
    };

    /**
     * a CardListFilter to get all lands
     */
    public static final CardListFilter lands = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isLand();
        }
    };

    /**
     * a CardListFilter to get all nonlands
     */
    public static final CardListFilter nonlands = new CardListFilter() {
        public boolean addCard(Card c) {
            return !c.isLand();
        }
    };

    /**
     * get a CardListFilter to filter in only cards that can be targeted
     *
     * @param source - the card to be the source for the target
     * @return a CardListFilter to only add cards that can be targeted
     */
    public static CardListFilter getCanTargetFilter(final Card source) {
        CardListFilter canTarget = new CardListFilter() {
            public boolean addCard(Card c) {
                return CardFactoryUtil.canTarget(source, c);
            }
        };
        return canTarget;
    }

    /**
     * get a CardListFilter to filter a CardList for a given keyword
     *
     * @param keyword - the keyword to look for
     * @return a CardListFilter to only add cards with the given keyword
     */
    public static CardListFilter getKeywordFilter(final String keyword) {
        CardListFilter filter = new CardListFilter() {
            public boolean addCard(Card c) {
                return c.hasKeyword(keyword);
            }
        };
        return filter;
    }

    /**
     * get a CardListFilter to filter a CardList for a given type
     *
     * @param type - the type to check for
     * @return a CardListFilter to only add cards of the given type
     */
    public static CardListFilter getTypeFilter(final String type) {
        CardListFilter filter = new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isType(type);
            }
        };
        return filter;
    }

    /**
     * a CardListFilter to get all cards that are black
     */
    public static final CardListFilter black = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isBlack();
        }
    };

    /**
     * a CardListFilter to get all cards that are blue
     */
    public static final CardListFilter blue = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isBlue();
        }
    };

    /**
     * a CardListFilter to get all cards that are green
     */
    public static final CardListFilter green = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isGreen();
        }
    };

    /**
     * a CardListFilter to get all cards that are red
     */
    public static final CardListFilter red = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isRed();
        }
    };

    /**
     * a CardListFilter to get all cards that are white
     */
    public static final CardListFilter white = new CardListFilter() {
        public boolean addCard(Card c) {
            return c.isWhite();
        }
    };

    /**
     * a CardListFilter to get all cards that are a part of this game
     *
     * @return a {@link forge.CardList} with all cards in all Battlefields, Hands, Graveyards, Libraries, and Exiles.
     */
    public static CardList getCardsInGame() {
        CardList all = new CardList();
        all.addAll(AllZone.getHumanGraveyard().getCards());
        all.addAll(AllZone.getHumanHand().getCards());
        all.addAll(AllZone.getHumanLibrary().getCards());
        all.addAll(AllZone.getHumanBattlefield().getCards());
        all.addAll(AllZone.getHumanExile().getCards());
        //should this include Human_Command ?
        //all.addAll(AllZone.getHumanSideboard().getCards());

        all.addAll(AllZone.getComputerGraveyard().getCards());
        all.addAll(AllZone.getComputerHand().getCards());
        all.addAll(AllZone.getComputerLibrary().getCards());
        all.addAll(AllZone.getComputerBattlefield().getCards());
        all.addAll(AllZone.getComputerExile().getCards());
        //should this include Computer_Command ?
        //all.addAll(AllZone.getComputerSideboard().getCards());

        return all;
    }

    /**
     * <p>getDoublingSeasonMagnitude.</p>
     *
     * @param player the {@link forge.Player} player to determine if is affected by Doubling Season
     * @return a int.
     */
    public static int getDoublingSeasonMagnitude(Player player) {
        int multiplier = 1;
        int doublingSeasons = getPlayerCardsInPlay(player, "Doubling Season").size();
        if (doublingSeasons > 0) multiplier = (int) Math.pow(2, doublingSeasons);
        return multiplier;
    }

    /**
     * get a list of all players participating in this game
     *
     * @return a list of all player participating in this game
     */
    public static ArrayList<Player> getPlayersInGame() {
        ArrayList<Player> list = new ArrayList<Player>();
        list.add(AllZone.getHumanPlayer());
        list.add(AllZone.getComputerPlayer());
        return list;
    }

    /**
     * gets a list of all opponents of a given player
     *
     * @param p the player whose opponents to get
     * @return a list of all opponents
     */
    public static ArrayList<Player> getOpponents(Player p) {
        ArrayList<Player> list = new ArrayList<Player>();
        list.add(p.getOpponent());
        return list;
    }

    /**
     * <p>compare.</p>
     *
     * @param leftSide a int.
     * @param comp a {@link java.lang.String} object.
     * @param rightSide a int.
     * @return a boolean.
     * @since 1.0.15
     */
    public static boolean compare(int leftSide, String comp, int rightSide) {
        // should this function be somewhere else?
        // leftSide COMPARED to rightSide:
        if (comp.contains("LT")) return leftSide < rightSide;

        else if (comp.contains("LE")) return leftSide <= rightSide;

        else if (comp.contains("EQ")) return leftSide == rightSide;

        else if (comp.contains("GE")) return leftSide >= rightSide;

        else if (comp.contains("GT")) return leftSide > rightSide;

        else if (comp.contains("NE")) return leftSide != rightSide; // not equals

        return false;
    }

}//end class AllZoneUtil
