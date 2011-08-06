
package forge;


import java.util.ArrayList;

/**
 * AllZoneUtil contains static functions used to get CardLists of various
 * cards in various zones.
 * 
 * @author dennis.r.friedrichsen (slapshot5 on slightlymagic.net)
 *
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
                return c.getKeyword().contains(keyword);
            }
        });
        return list;
	}
	
	/**
	 * use to get a CardList of all creatures in play for both players
	 * 
	 * @return a CardList of all creatures in play on both sides
	 */
	public static CardList getCreaturesInPlay() {
		CardList creatures = getCardsInPlay();
		return creatures.filter(AllZoneUtil.creatures);
	}
	
	///////////////// Lands
	
	/**
	 * use to get a list of all lands a given player has in play
	 * 
	 * @param player the player whose lands we want to get
	 * @return a CardList containing all lands the given player has in play
	 */
	public static CardList getPlayerLandsInPlay(final Player player) {
		CardList cards = getPlayerCardsInPlay(player);
		return cards.filter(lands);
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
		cards.add(getCardsInPlay(null));
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
		cards.add(getPlayerCardsInPlay(AllZone.HumanPlayer));
		cards.add(getPlayerCardsInPlay(AllZone.ComputerPlayer));
		if( cardName != null && !"".equals(cardName) ) {
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
		if( player.equals(AllZone.HumanPlayer) || player.equals(AllZone.ComputerPlayer) ){
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
			cards.addAll(play.getCards());
		}
		return cards;
	}
	
	/**
	 * gets a list of all cards with a given name a given player has in play
	 * 
	 * @param player the player whose cards in play you want to get
	 * @param cardName the card name to look for in that zone
	 * @return a CardList with all cards of a given name the player has in play
	 */
	public static CardList getPlayerCardsInPlay(final Player player, final String cardName) {
		CardList cards = new CardList();
		
		PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
		cards.addAll(play.getCards());
		
		if( cardName != null && !"".equals(cardName) ) {
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
		cards.add(getPlayerGraveyard(AllZone.HumanPlayer));
		cards.add(getPlayerGraveyard(AllZone.ComputerPlayer));
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
		if( player.isHuman() || player.isComputer() ){
			PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
			cards.addAll(grave.getCards());
		}
		return cards;
	}
	
	/**
	 * gets a list of all cards with a given name in a certain player's graveyard
	 * 
	 * @param player the player whose graveyard we want to get
	 * @param cardName the card name to find in the graveyard
	 * @return a CardList containing all cards with that name in the target graveyard
	 */
	public static CardList getPlayerGraveyard(final Player player, final String cardName) {
		CardList cards = getPlayerGraveyard(player);
		cards = cards.getName(cardName);
		return cards;
	}
	
	// Get a Cards in All Graveyards with a certain name
	public static CardList getCardsInGraveyard(final String cardName) {
		CardList cards = new CardList();
		cards.add(getPlayerGraveyard(AllZone.HumanPlayer));
		cards.add(getPlayerGraveyard(AllZone.ComputerPlayer));
		cards = cards.getName(cardName);
		return cards;
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
		if( player.isHuman() || player.isComputer() ){
			PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
			cards.addAll(hand.getCards());
		}
		return cards;
	}
	
	////////////// REMOVED FROM GAME
	
	/**
	 * gets a list of all cards owned by both players that have been removed from the game
	 * 
	 * @return a CardList with all cards removed from the game
	 */
	public static CardList getCardsRemovedFromGame() {
		CardList cards = new CardList();
		cards.add(getPlayerCardsRemovedFromGame(AllZone.ComputerPlayer));
		cards.add(getPlayerCardsRemovedFromGame(AllZone.HumanPlayer));
		return cards;
	}
	
	/**
	 * gets a list of all cards removed from the game for a given player
	 * 
	 * @param player the player whose cards we want that are removed from the game
	 * @return a CardList with all cards removed from the game for a given player
	 */
	public static CardList getPlayerCardsRemovedFromGame(final Player player) {
		CardList cards = new CardList();
		if( player.equals(AllZone.HumanPlayer) || player.equals(AllZone.ComputerPlayer) ){
			PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, player);
			cards.addAll(removed.getCards());
		}
		return cards;
	}
	
	/**
	 * gets a list of all cards with a given name in a certain player's exile
	 * 
	 * @param player the player whose exile we want to get
	 * @param cardName the card name to find in the exile
	 * @return a CardList containing all cards with that name in the target exile
	 */
	public static CardList getPlayerCardsRemovedFromGame(final Player player, final String cardName) {
		CardList cards = getPlayerCardsRemovedFromGame(player);
		cards = cards.getName(cardName);
		return cards;
	}
	
	//////////////////////// LIBRARY
	
	/**
	 * gets a list of all cards in a given player's library
	 * 
	 * @return a CardList with all the cards currently in that player's library
	 */
	public static CardList getPlayerCardsInLibrary(final Player player) {
		CardList cards = new CardList();
		if( player.equals(AllZone.HumanPlayer) || player.equals(AllZone.ComputerPlayer) ){
			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
			cards.addAll(lib.getCards());
		}
		return cards;
	}
	
	/**
	 * gets a list of all cards with a certain name in a given player's library
	 * 
	 * @param player the player's library one is interested in
	 * @param cardName the card's name that one is interested in
	 * @return a CardList of all cards of the given name in the given player's library
	 */
	public static CardList getPlayerCardsInLibrary(final Player player, final String cardName) {
		CardList cards = getPlayerCardsInLibrary(player);
		return cards.getName(cardName);
	}
	
	
	///Check if a certain card is in play
	
	public static boolean isCardInPlay(Card card) {
		return PlayerZoneUtil.isCardInZone(AllZone.Computer_Play, card)
        	|| PlayerZoneUtil.isCardInZone(AllZone.Human_Play, card);
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
	 * @param player the player whose battlefield we want to check
	 * @return true if that player has that card in play, false otherwise
	 */
	public static boolean isCardInPlay(final String cardName, final Player player) {
		return getPlayerCardsInPlay(player, cardName).size() > 0;
	}
	
	///get a list of certain types are in play (like Mountain, Elf, etc...)
	
	/**
	 * gets a list of all cards with a certain type (Mountain, Elf, etc...) in play
	 * 
	 * @param the type to find in play
	 * @return a CardList with all cards of the given type in play
	 */
	public static CardList getTypeInPlay(final String cardType) {
		CardList cards = getCardsInPlay();
		cards = cards.getType(cardType);
		return cards;
	}
	
	/**
	 * gets a list of all cards of a certain type that a given player has in play
	 * @param player the player to check for cards in play
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
	 * @param player the player to check for cards in play
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
	 * @param player the player to check for cards in play
	 * @param cardType the card type to check for
	 * @return a CardList with all cards of a certain type the player has in graveyard
	 */
	public static CardList getPlayerTypeInGraveyard(final Player player, final String cardType) {
		CardList cards = getPlayerGraveyard(player);
		cards = cards.getType(cardType);
		return cards;
	}
	
	//////////////// getting all cards of a given color
	
	public static CardList getColorInPlay(final String color) {
		CardList cards = getPlayerColorInPlay(AllZone.ComputerPlayer, color);
		cards.add(getPlayerColorInPlay(AllZone.HumanPlayer, color));
		return cards;
	}
	
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
	
	
	public static CardList getCardsInZone(String zone){	
        return getCardsInZone(zone, null);
	}
	
	public static CardList getCardsInZone(String zone, Player player){	
        CardList all = new CardList();

		if (zone.equals(Constant.Zone.Graveyard)){
			if (player == null || player.isHuman())
				all.addAll(AllZone.Human_Graveyard.getCards());
			if (player == null || player.isComputer())
				all.addAll(AllZone.Computer_Graveyard.getCards());
		}
		else if (zone.equals(Constant.Zone.Hand)){
			if (player == null || player.isHuman())
				all.addAll(AllZone.Human_Hand.getCards());
			if (player == null || player.isComputer())
				all.addAll(AllZone.Computer_Hand.getCards());
		}
		else if (zone.equals(Constant.Zone.Play)){
			if (player == null || player.isHuman())
				all.addAll(AllZone.Human_Play.getCards());
			if (player == null || player.isComputer())
				all.addAll(AllZone.Computer_Play.getCards());
		}
		else if (zone.equals(Constant.Zone.Removed_From_Play)){
			if (player == null || player.isHuman())
				all.addAll(AllZone.Human_Removed.getCards());
			if (player == null || player.isComputer())
				all.addAll(AllZone.Computer_Removed.getCards());
		}
		else if (zone.equals(Constant.Zone.Library)){
			if (player == null || player.isHuman())
				all.addAll(AllZone.Human_Library.getCards());
			if (player == null || player.isComputer())
				all.addAll(AllZone.Computer_Library.getCards());
		}
		
        return all;
	}
	
	
	//zone manipulation, maybe be better off in GameAction.java...
	/**
	 * use this when Human needs to rearrange the top X cards in a player's library.  You
	 * may also specify a shuffle when done
	 * 
	 * @param player the player to target
	 * @param numCards the number of cards from the top to rearrange
	 * @param shuffle true if a shuffle is desired at the end, false otherwise
	 */
	public static void rearrangeTopOfLibrary(final Player player, final int numCards, boolean shuffle) {
		PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
		int maxCards = lib.size();
		maxCards = Math.min(maxCards, numCards);
		if(maxCards == 0) return;
		CardList topCards =   new CardList();
		//show top n cards:
		for(int j = 0; j < maxCards; j++ ) {
			topCards.add(lib.get(j));
		}
		for(int i = 1; i <= maxCards; i++) {
			String suffix = "";
			switch(i) {
			case 1:	suffix="st"; break;
			case 2: suffix="nd"; break;
			case 3: suffix="rd"; break;
			default: suffix="th";
			}
			String title = "Put "+i+suffix+" in Library: ";
			Object o = AllZone.Display.getChoiceOptional(title, topCards.toArray());
			if(o == null) break;
			Card c_1 = (Card) o;
			topCards.remove(c_1);
			lib.remove(c_1);
			lib.add(c_1, i - 1);
		}
		if(shuffle) {
			player.shuffle();
		}
	}
	
	public static void exileNCardsFromZone(final PlayerZone zone, final CardListFilter filter, final int n, final boolean shuffle) {
		CardList cards = new CardList(zone.getCards());
		if(null != filter) {
			cards = cards.filter(filter);
		}
		int maxCards = n;
		int numCards = cards.size();
		maxCards = Math.min(maxCards, numCards);
		for(int i = 1; i <= maxCards; i++) {
			String title = "Select card to exile: " + i + "/" + maxCards;
			Object o = AllZone.Display.getChoiceOptional(title, cards.toArray());
			if(o == null) break;
			Card card = (Card) o;
			AllZone.GameAction.exile(card);
		}
		if(shuffle) zone.getPlayer().shuffle();
	}
	
	public static int compareTypeAmountInPlay(final Player player, String type)
	{
		// returns the difference between player's
		Player opponent = player.getOpponent();
		CardList playerList = getPlayerTypeInPlay(player, type);
		CardList opponentList = getPlayerTypeInPlay(opponent, type);
		return (playerList.size() - opponentList.size());
	}
	
	public static int compareTypeAmountInGraveyard(final Player player, String type)
	{
		// returns the difference between player's
		Player opponent = player.getOpponent();
		CardList playerList = getPlayerTypeInGraveyard(player, type);
		CardList opponentList = getPlayerTypeInGraveyard(opponent, type);
		return (playerList.size() - opponentList.size());
	}
	
	
	/**
	 * a CardListFilter to get all cards that are tapped
	 */
	public static CardListFilter tapped = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isTapped();
		}
	};
	
	/**
	 * a CardListFilter to get all cards that are untapped
	 */
	public static CardListFilter untapped = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isUntapped();
		}
	};
	
	/**
	 * a CardListFilter to get all creatures
	 */
	public static CardListFilter creatures = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isCreature();
		}
	};
	
	/**
	 * a CardListFilter to get all enchantments
	 */
	public static CardListFilter enchantments = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isCreature();
		}
	};
	
	/**
	 * a CardListFilter to get all unenchanted cards in a list
	 */
	public static CardListFilter unenchanted = new CardListFilter() {
		public boolean addCard(Card c) {
			return !c.isEnchanted();
		}
	};	
	
	/**
	 * a CardListFilter to get all enchanted cards in a list
	 */
	public static CardListFilter enchanted = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isEnchanted();
		}
	};
	
	public static CardListFilter nonToken = new CardListFilter() {
		public boolean addCard(Card c) {
			return !c.isToken();
		}
	};
	
	public static CardListFilter artifacts = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isArtifact();
		}
	};
	
	public static CardListFilter nonartifacts = new CardListFilter() {
		public boolean addCard(Card c) {
			return !c.isArtifact();
		}
	};
	
	public static CardListFilter lands = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isLand();
		}
	};
	
	public static CardListFilter nonlands = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isLand();
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
				return c.getKeyword().contains(keyword);
			}
		};
		return filter;
	}
	
	public static CardListFilter black = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isBlack();
		}
	};
	
	public static CardListFilter blue = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isBlue();
		}
	};
	
	public static CardListFilter green = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isGreen();
		}
	};
	
	public static CardListFilter red = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isRed();
		}
	};
	
	public static CardListFilter white = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isWhite();
		}
	};
	
	public static CardList getCardsInGame(){
        CardList all = new CardList();
        all.addAll(AllZone.Human_Graveyard.getCards());
        all.addAll(AllZone.Human_Hand.getCards());
        all.addAll(AllZone.Human_Library.getCards());
        all.addAll(AllZone.Human_Play.getCards());
        all.addAll(AllZone.Human_Removed.getCards());
        
        all.addAll(AllZone.Computer_Graveyard.getCards());
        all.addAll(AllZone.Computer_Hand.getCards());
        all.addAll(AllZone.Computer_Library.getCards());
        all.addAll(AllZone.Computer_Play.getCards());
        all.addAll(AllZone.Computer_Removed.getCards());
        
        return all;
    }
	
	public static int getDoublingSeasonMagnitude(Player player) {
		int multiplier = 1;
        int doublingSeasons = getPlayerCardsInPlay(player, "Doubling Season").size();
        if(doublingSeasons > 0) multiplier = (int) Math.pow(2, doublingSeasons);
        return multiplier;
	}
	
	public static boolean hasAllUrzas(Player player) {
		return isCardInPlay("Urza's Power Plant", player) && isCardInPlay("Urza's Mine", player)
			&& isCardInPlay("Urza's Tower", player);
	}

	public static CardList getLandsInPlay() {
		CardList lands = new CardList();
		lands.add(getPlayerLandsInPlay(AllZone.HumanPlayer));
		lands.add(getPlayerLandsInPlay(AllZone.ComputerPlayer));
		return lands;
	}
}