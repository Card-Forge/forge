package forge.card.cardFactory;

import java.util.Iterator;

import forge.Card;
import forge.CardList;
import forge.Player;
import forge.card.spellability.SpellAbility;

public interface CardFactoryInterface extends Iterable<Card>{

    /**
     * Iterate over all full-fledged cards in the database; these cards are 
     * owned by the human player by default.
     * 
     * @return an Iterator that does NOT support the remove method 
     */
	public Iterator<Card> iterator();

	/**
	 * Typical size method. 
	 * 
	 * @return an estimate of the number of items encountered by this object's 
	 * iterator
	 * 
	 * @see #iterator
	 */
	public int size();
	
    /**
     * <p>copyCard.</p>
     *
     * @param in a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public Card copyCard(Card in);
    
    /**
     * <p>copyCardintoNew.</p>
     *
     * @param in a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public Card copyCardintoNew(Card in);

    /**
     * <p>copySpellontoStack.</p>
     *
     * @param source a {@link forge.Card} object.
     * @param original a {@link forge.Card} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param bCopyDetails a boolean.
     */
    public void copySpellontoStack(Card source, Card original, SpellAbility sa, boolean bCopyDetails);

    /**
     * <p>copySpellontoStack.</p>
     *
     * @param source a {@link forge.Card} object.
     * @param original a {@link forge.Card} object.
     * @param bCopyDetails a boolean.
     */
    public void copySpellontoStack(Card source, Card original, boolean bCopyDetails);

    /**
     * <p>getCard.</p>
     *
     * @param cardName a {@link java.lang.String} object.
     * 
     * @param owner a {@link forge.Player} object.
     * 
     * @return a {@link forge.Card} instance, owned by owner; or the special 
     * blankCard
     * 
     * @throws RuntimeException if cardName isn't in the Card map
     */
    public Card getCard(String cardName, Player owner);

    /**
     * Fetch a random combination of cards without any duplicates.
     * 
     * This algorithm is reasonably fast if numCards is small. If it is larger
     * than, say, size()/10, it starts to get noticeably slow.
     * 
     * @param numCards
     *            the number of cards to return
     * 
     * @return a list of fleshed-out card instances
     * 
     * @throws IllegalArgumentException if numCards >= size()/4
     */
    public CardList getRandomCombinationWithoutRepetition(int numCards);

}
