package forge;


import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * AllZoneUtil contains static functions used to get CardLists of various
 * cards in various zones.
 *
 * @author dennis.r.friedrichsen (slapshot5 on slightlymagic.net)
 * @version $Id$
 */
public final class AllZoneUtil {

    private AllZoneUtil() {
        throw new AssertionError();
    }

    //////////// Creatures

    /**
     * use to get a list of creatures in play for a given player.
     *
     * @param player the player to get creatures for
     * @return a CardList containing all creatures a given player has in play
     */
    public static CardList getCreaturesInPlay(final Player player) {
        CardList creats = player.getCardsIn(Zone.Battlefield);
        return creats.filter(AllZoneUtil.creatures);
    }

    /**
     * use to get a list of creatures in play with a given keyword.
     *
     * @param keyword the keyword to get creatures for
     * @return a CardList containing all creatures in play with a given keyword
     */
    public static CardList getCreaturesInPlayWithKeyword(final String keyword) {
        CardList list = getCreaturesInPlay();
        list = list.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                return c.hasKeyword(keyword);
            }
        });
        return list;
    }

    
    /**
     * gets a list of all cards of a certain type that a given player has in given zone.
     *
     * @param player   the player to check for cards in play
     * @param cardType the card type to check for
     * @return a CardList with all cards of a certain type the player has in play
     */
    public static CardList getPlayerTypeIn(final Player player, final Constant.Zone zone, final String cardType) {
        CardList cards = player.getCardsIn(zone);
        cards = cards.getType(cardType);
        return cards;
    }
    
    /**
     * gets a list of all cards owned by both players that have are currently in the given zone.
     *
     * @return a CardList with all cards currently in a graveyard
     */
    public static CardList getCardsIn(final Constant.Zone zone) {
        CardList cards = new CardList();
        for (Player p : Singletons.getModel().getGameState().getPlayers()) {
            cards.addAll(p.getZone(zone).getCards());    
        }
        return cards;
    }

    public static CardList getCardsIn(final List<Constant.Zone> zones) {
        CardList cards = new CardList();
        for (Zone z: zones) {
            for (Player p : Singletons.getModel().getGameState().getPlayers()) {
                cards.addAll(p.getZone(z).getCards());    
            }            
        }
        return cards;
    }

    /**
     * gets a list of all cards owned by both players that have are currently in the given zone.
     *
     * @return a CardList with all cards currently in a graveyard
     */
    public static CardList getCardsIn(final Constant.Zone zone, String cardName) {
        CardList cards = getCardsIn(zone);
        return cards.getName(cardName);
    }    

    /**
     * use to get a CardList of all creatures on the battlefield for both players
     *
     * @return a CardList of all creatures on the battlefield on both sides
     */
    public static CardList getCreaturesInPlay() {
        CardList creats = getCardsIn(Zone.Battlefield);
        return creats.filter(AllZoneUtil.creatures);
    }

    ///////////////// Lands

    /**
     * use to get a list of all lands a given player has on the battlefield.
     *
     * @param player the player whose lands we want to get
     * @return a CardList containing all lands the given player has in play
     */
    public static CardList getPlayerLandsInPlay(final Player player) {
        CardList cards = player.getCardsIn(Zone.Battlefield);
        return cards.filter(lands);
    }

    /**
     * gets a list of all lands in play.
     *
     * @return a CardList of all lands on the battlefield
     */
    public static CardList getLandsInPlay() {
        CardList cards = getCardsIn(Zone.Battlefield);
        return cards.filter(lands);
    }

    //=============================================================================
    //
    // These functions handle getting all cards for a given player
    // and all cards with a given name for either or both players
    //
    //=============================================================================



    /**
     * answers the question "is a certain, specific card in this player's graveyard?".
     *
     * @param player the player's hand to check
     * @param card   the specific card to look for
     * @return true if the card is present in this player's hand; false otherwise
     */
    public static boolean isCardInPlayerGraveyard(final Player player, final Card card) {
        return isCardInZone(player.getZone(Constant.Zone.Graveyard), card);
    }

    //////// HAND



    /**
     * answers the question "is a specific card in the specified zone?".
     *
     * @param pz   the PlayerZone to check
     * @param card the specific card to look for
     * @return true if the card is present in this zone; false otherwise
     */
    public static boolean isCardInZone(final PlayerZone pz, final Card card) {
        if (card == null) {
            return false;
        }

        for (Card c : pz.getCards()) {
            if (c.equals(card)) {
                return true;
            }
        }

        return false;
    }

    /**
     * answers the question "is the given card in any exile zone?".
     *
     * @param c the card to look for in Exile
     * @return true is the card is in Human or Computer's Exile zone
     */
    public static boolean isCardExiled(final Card c) {
        return getCardsIn(Zone.Exile).contains(c);
    }


    ///Check if a certain card is in play

    /**
     * <p>isCardInPlay.</p>
     *
     * @param card a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isCardInPlay(final Card card) {
        return getCardsIn(Zone.Battlefield).contains(card);
    }

    /**
     * Answers the question: "Is <card name> in play?".
     *
     * @param cardName the name of the card to look for
     * @return true is the card is in play, false otherwise
     */
    public static boolean isCardInPlay(final String cardName) {
        return getCardsIn(Zone.Battlefield, cardName).size() > 0;
    }

    /**
     * Answers the question: "Does <player> have <card name> in play?".
     *
     * @param cardName the name of the card to look for
     * @param player   the player whose battlefield we want to check
     * @return true if that player has that card in play, false otherwise
     */
    public static boolean isCardInPlay(final String cardName, final Player player) {
        return player.getCardsIn(Zone.Battlefield, cardName).size() > 0;
    }

	/**
	 * Answers the question: "Does <player> have <given card> in play?".
	 *
	 * @param card the card to look for
	 * @param player the player whose battlefield we want to check
	 * @return true if that player has that card in play, false otherwise
	 * @since 1.0.15
	 */
	public static boolean isCardInPlay(final Card card, final Player player) {
		return player.getCardsIn(Zone.Battlefield).contains(card);
	}

    ///get a list of certain types are in play (like Mountain, Elf, etc...)

    /**
     * gets a list of all cards with a certain type (Mountain, Elf, etc...) in play.
     *
     * @param cardType the type to find in play
     * @return a CardList with all cards of the given type in play
     */
    public static CardList getTypeIn(final Zone zone, final String cardType) {
        return getCardsIn(zone).getType(cardType);
    }


    //////////////// getting all cards of a given color

    /**
     * gets a list of all Cards of a given color on the battlefield.
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
     * gets a list of all Cards of a given color a given player has on the battlefield.
     *
     * @param player the player's cards to get
     * @param color  the color of cards to get
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
     * <p>getCardState.</p>
     *
     * @param card a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCardState(final Card card) {
        PlayerZone zone = AllZone.getZoneOf(card);
        //for tokens
        if (zone == null) {
            return null;
        }

        CardList list = getCardsInZone(zone);
        for (Card c : list) {
            if (card.equals(c)) {
                return c;
            }
        }

        return card;
    }

    /**
     * <p>getCardsInZone.</p>
     *
     * @param zone a {@link forge.PlayerZone} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getCardsInZone(final PlayerZone zone) {
        return new CardList(zone.getCards());
    }


    /**
     * <p>compareTypeAmountInPlay.</p>
     *
     * @param player a {@link forge.Player} object.
     * @param type a {@link java.lang.String} object.
     * @return a int.
     */
    public static int compareTypeAmountInPlay(final Player player, final String type) {
        // returns the difference between player's
        Player opponent = player.getOpponent();
        CardList playerList = getPlayerTypeIn(player, Zone.Battlefield, type);
        CardList opponentList = getPlayerTypeIn(opponent, Zone.Battlefield, type);
        return (playerList.size() - opponentList.size());
    }

    /**
     * <p>compareTypeAmountInGraveyard.</p>
     *
     * @param player a {@link forge.Player} object.
     * @param type a {@link java.lang.String} object.
     * @return a int.
     */
    public static int compareTypeAmountInGraveyard(final Player player, final String type) {
        // returns the difference between player's
        Player opponent = player.getOpponent();
        CardList playerList = getPlayerTypeIn(player, Zone.Graveyard, type);
        CardList opponentList = getPlayerTypeIn(opponent, Zone.Graveyard, type);
        return (playerList.size() - opponentList.size());
    }


    /**
     * a CardListFilter to get all cards that are tapped.
     */
    public static final CardListFilter tapped = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isTapped();
        }
    };

    /**
     * a CardListFilter to get all cards that are untapped.
     */
    public static final CardListFilter untapped = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isUntapped();
        }
    };

    /**
     * a CardListFilter to get all creatures.
     */
    public static final CardListFilter creatures = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isCreature();
        }
    };

    /**
     * a CardListFilter to get all enchantments.
     */
    public static final CardListFilter enchantments = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isEnchantment();
        }
    };

    /**
     * a CardListFilter to get all equipment.
     */
    public static final CardListFilter equipment = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isEquipment();
        }
    };

    /**
     * a CardListFilter to get all unenchanted cards in a list.
     */
    public static final CardListFilter unenchanted = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isEnchanted();
        }
    };

    /**
     * a CardListFilter to get all enchanted cards in a list.
     */
    public static final CardListFilter enchanted = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isEnchanted();
        }
    };

    /**
     * a CardListFilter to get all nontoken cards.
     */
    public static final CardListFilter nonToken = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isToken();
        }
    };

    /**
     * a CardListFilter to get all token cards.
     */
    public static final CardListFilter token = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isToken();
        }
    };

    /**
     * a CardListFilter to get all nonbasic lands.
     */
    public static final CardListFilter nonBasicLand = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isBasicLand();
        }
    };

    /**
     * a CardListFilter to get all basicLands.
     */
    public static final CardListFilter basicLands = new CardListFilter() {
        public boolean addCard(final Card c) {
            //the isBasicLand() check here may be sufficient...
            return c.isLand() && c.isBasicLand();
        }
    };

    /**
     * a CardListFilter to get all artifacts.
     */
    public static final CardListFilter artifacts = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isArtifact();
        }
    };

    /**
     * a CardListFilter to get all nonartifacts.
     */
    public static final CardListFilter nonartifacts = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isArtifact();
        }
    };

    /**
     * a CardListFilter to get all lands.
     */
    public static final CardListFilter lands = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isLand();
        }
    };

    /**
     * a CardListFilter to get all nonlands.
     */
    public static final CardListFilter nonlands = new CardListFilter() {
        public boolean addCard(final Card c) {
            return !c.isLand();
        }
    };

    /**
     * get a CardListFilter to filter in only cards that can be targeted.
     *
     * @param source - the card to be the source for the target
     * @return a CardListFilter to only add cards that can be targeted
     */
    public static CardListFilter getCanTargetFilter(final Card source) {
        CardListFilter canTarget = new CardListFilter() {
            public boolean addCard(final Card c) {
                return CardFactoryUtil.canTarget(source, c);
            }
        };
        return canTarget;
    }

    /**
     * get a CardListFilter to filter a CardList for a given keyword.
     *
     * @param keyword - the keyword to look for
     * @return a CardListFilter to only add cards with the given keyword
     */
    public static CardListFilter getKeywordFilter(final String keyword) {
        CardListFilter filter = new CardListFilter() {
            public boolean addCard(final Card c) {
                return c.hasKeyword(keyword);
            }
        };
        return filter;
    }

    /**
     * get a CardListFilter to filter a CardList for a given type.
     *
     * @param type - the type to check for
     * @return a CardListFilter to only add cards of the given type
     */
    public static CardListFilter getTypeFilter(final String type) {
        CardListFilter filter = new CardListFilter() {
            public boolean addCard(final Card c) {
                return c.isType(type);
            }
        };
        return filter;
    }

    /**
     * a CardListFilter to get all cards that are black.
     */
    public static final CardListFilter black = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isBlack();
        }
    };

    /**
     * a CardListFilter to get all cards that are blue.
     */
    public static final CardListFilter blue = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isBlue();
        }
    };

    /**
     * a CardListFilter to get all cards that are green.
     */
    public static final CardListFilter green = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isGreen();
        }
    };

    /**
     * a CardListFilter to get all cards that are red.
     */
    public static final CardListFilter red = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isRed();
        }
    };

    /**
     * a CardListFilter to get all cards that are white.
     */
    public static final CardListFilter white = new CardListFilter() {
        public boolean addCard(final Card c) {
            return c.isWhite();
        }
    };

    /**
     * a CardListFilter to get all cards that are a part of this game.
     *
     * @return a {@link forge.CardList} with all cards in all Battlefields, Hands, Graveyards, Libraries, and Exiles.
     */
    public static CardList getCardsInGame() {
        CardList all = new CardList();
        getCardsInGame(AllZone.getHumanPlayer(), all);
        getCardsInGame(AllZone.getComputerPlayer(), all);
        return all;
    }

    private static CardList getCardsInGame(Player player, CardList toAdd)
    {
        CardList all = toAdd == null ? new CardList() : toAdd;
        all.addAll(player.getZone(Zone.Graveyard).getCards());
        all.addAll(player.getZone(Zone.Hand).getCards());
        all.addAll(player.getZone(Zone.Library).getCards());
        all.addAll(player.getZone(Zone.Battlefield).getCards());
        all.addAll(player.getZone(Zone.Exile).getCards());
        //should this include Human_Command ?
        //all.addAll(AllZone.getHumanSideboard().getCards());
        return all;
    }
    
    /**
     * <p>getDoublingSeasonMagnitude.</p>
     *
     * @param player the {@link forge.Player} player to determine if is affected by Doubling Season
     * @return a int.
     */
    public static int getDoublingSeasonMagnitude(final Player player) {
        int multiplier = 1;
        int doublingSeasons = player.getCardsIn(Zone.Battlefield, "Doubling Season").size();
        if (doublingSeasons > 0) {
            multiplier = (int) Math.pow(2, doublingSeasons);
        }
        return multiplier;
    }

    /**
     * get a list of all players participating in this game.
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
     * gets a list of all opponents of a given player.
     *
     * @param p the player whose opponents to get
     * @return a list of all opponents
     */
    public static ArrayList<Player> getOpponents(final Player p) {
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

}//end class AllZoneUtil
