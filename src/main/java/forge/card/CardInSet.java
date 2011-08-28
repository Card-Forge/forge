package forge.card;

/**
 * <p>CardInSet class.</p>
 *
 * @author Forge
 * @version $Id: CardInSet.java 9708 2011-08-09 19:34:12Z jendave $
 */

public class CardInSet {
    private CardRarity rarity;
    private int numCopies;

    public CardInSet(final CardRarity rarity, final int cntCopies) {
        this.rarity = rarity;
        this.numCopies = cntCopies;
    }

    public static CardInSet parse(final String unparsed) {
        int spaceAt = unparsed.indexOf(' ');
        char rarity = unparsed.charAt(spaceAt + 1);
        CardRarity rating;
        switch (rarity) {
            case 'L': rating = CardRarity.BasicLand; break;
            case 'C': rating = CardRarity.Common; break;
            case 'U': rating = CardRarity.Uncommon; break;
            case 'R': rating = CardRarity.Rare; break;
            case 'M': rating = CardRarity.MythicRare; break;
            case 'S': rating = CardRarity.Special; break;
            default: rating = CardRarity.MythicRare; break;
        }

        int number = 1;
        int bracketAt = unparsed.indexOf('(');
        if (-1 != bracketAt) {
            String sN = unparsed.substring(bracketAt + 2, bracketAt + 3);
            number = Integer.parseInt(sN);
        }
        return new CardInSet(rating, number);

    }

    public final int getCopiesCount() { return numCopies; }
    public final CardRarity getRarity() { return rarity; }
}
