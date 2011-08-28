package forge.card;

/**
 * <p>CardInSet class.</p>
 *
 * @author Forge
 * @version $Id: CardInSet.java 9708 2011-08-09 19:34:12Z jendave $
 */

public class CardInSet {
    private final CardRarity rarity;
    private final int numCopies;

    public CardInSet(final CardRarity rarity, final int cntCopies) {
        this.rarity = rarity;
        this.numCopies = cntCopies;
    }

    public final int getCopiesCount() { return numCopies; }
    public final CardRarity getRarity() { return rarity; }
}
