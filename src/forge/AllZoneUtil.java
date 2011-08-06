
package forge;

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
	
	public static CardList getPlayerGraveyard(final String player) {
		CardList cards = new CardList();
		if( player.equals(Constant.Player.Human) || player.equals(Constant.Player.Computer) ){
			PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
			cards.addAll(grave.getCards());
		}
		return cards;
	}
	
	public static CardList getPlayerGraveyard(final String player, final String cardName) {
		CardList cards = new CardList();
		cards = getPlayerGraveyard(player);
		cards = cards.getName(cardName);
		return cards;
	}
}