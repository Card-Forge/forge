
package forge;

/**
 * AllZoneUtil contains static functions used to get CardLists of various
 * cards in various zones.
 * 
 * @author dennis.r.friedrichsen (slapshot5 on slightlymagic.net)
 *
 */
public class AllZoneUtil {
	
	/**
	 * use to get a list of creatures in play for a given player
	 * 
	 * @param player the player to get creatures for
	 * @return a CardList containing all creatures a give player has in play
	 */
	public static CardList getCreaturesInPlay(final String player) {
		CardList creatures = AllZoneUtil.getCardsInPlay(player);
		return creatures.filter(AllZoneUtil.creatures);
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
	
	private static CardListFilter creatures = new CardListFilter() {
		public boolean addCard(Card c) {
			return c.isCreature();
		}
	};
	
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
		cards.add(getPlayerCardsInPlay(Constant.Player.Human));
		cards.add(getPlayerCardsInPlay(Constant.Player.Computer));
		if( cardName != null && cardName != "" ) {
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
	public static CardList getPlayerCardsInPlay(final String player) {
		CardList cards = new CardList();
		if( player.equals(Constant.Player.Human) || player.equals(Constant.Player.Computer) ){
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
	public static CardList getPlayerCardsInPlay(final String player, final String cardName) {
		CardList cards = new CardList();
		if( player.equals(Constant.Player.Human) || player.equals(Constant.Player.Computer) ){
			cards = getPlayerCardsInPlay(player);
			cards = cards.getName(cardName);
		}
		return cards;
	}
	
	//////////GRAVEYARD
	
	/**
	 * gets all cards in given player's graveyard
	 * 
	 * @param player the player whose graveyard we want to get
	 * @return a CardList containing all cards in that player's graveyard
	 */
	public static CardList getPlayerGraveyard(final String player) {
		CardList cards = new CardList();
		if( player.equals(Constant.Player.Human) || player.equals(Constant.Player.Computer) ){
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
	public static CardList getPlayerGraveyard(final String player, final String cardName) {
		CardList cards = new CardList();
		cards = getPlayerGraveyard(player);
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
	public static CardList getPlayerHand(final String player) {
		CardList cards = new CardList();
		if( player.equals(Constant.Player.Human) || player.equals(Constant.Player.Computer) ){
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
		cards.add(getPlayerCardsRemovedFromGame(Constant.Player.Computer));
		cards.add(getPlayerCardsRemovedFromGame(Constant.Player.Human));
		return cards;
	}
	
	/**
	 * gets a list of all cards removed from the game for a given player
	 * 
	 * @param player the player whose cards we want that are removed from the game
	 * @return a CardList with all cards removed from the game for a given player
	 */
	public static CardList getPlayerCardsRemovedFromGame(final String player) {
		CardList cards = new CardList();
		if( player.equals(Constant.Player.Human) || player.equals(Constant.Player.Computer) ){
			PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, player);
			cards.addAll(removed.getCards());
		}
		return cards;
	}
	
	//////////////////////// LIBRARY
	
	/**
	 * gets a list of all cards in a given player's library
	 * 
	 * @return a CardList with all the cards currently in that player's library
	 */
	public static CardList getPlayerCardsInLibrary(final String player) {
		CardList cards = new CardList();
		if( player.equals(Constant.Player.Human) || player.equals(Constant.Player.Computer) ){
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
	public static CardList getPlayerCardsInLibrary(final String player, final String cardName) {
		CardList cards = new CardList();
		cards = getPlayerCardsInLibrary(player);
		return cards.getName(cardName);
	}
	
	
	///Check if a certain card is in play
	
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
	public static boolean isCardInPlay(final String cardName, final String player) {
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
	public static CardList getPlayerTypeInPlay(final String player, final String cardType) {
		CardList cards = getPlayerCardsInPlay(player);
		cards = cards.getType(cardType);
		return cards;
	}
	
}