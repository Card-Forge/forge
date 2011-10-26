package forge.card;

/**
 * <p>
 * CardInSet class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardInSet.java 9708 2011-08-09 19:34:12Z jendave $
 */

public class CardInSet {
    private final CardRarity rarity;
    private final int numCopies;

    /**
     * Instantiates a new card in set.
     * 
     * @param rarity
     *            the rarity
     * @param cntCopies
     *            the cnt copies
     */
    public CardInSet(final CardRarity rarity, final int cntCopies) {
        this.rarity = rarity;
        this.numCopies = cntCopies;
    }

    /**
     * Gets the copies count.
     * 
     * @return the copies count
     */
    public final int getCopiesCount() {
        return numCopies;
    }

    /**
     * Gets the rarity.
     * 
     * @return the rarity
     */
    public final CardRarity getRarity() {
        return rarity;
    }
}
