
package forge;


public class PlayerUtil {
	public static boolean worshipFlag(Player player) {
		// Instead of hardcoded Ali from Cairo like cards, it is now a Keyword
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
		list = list.getKeyword("Damage that would reduce your life total to less than 1 reduces it to 1 instead.");
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return !c.isFaceDown();
			}
		});
		
		return list.size() > 0;
    }
}