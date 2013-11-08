package forge.card.mana;

import org.apache.commons.lang3.StringUtils;


/**
 * The Class ParserCardnameTxtManaCost.
 */
public class ManaCostParser implements IParserManaCost {
    private final String[] cost;
    private int nextToken;
    private int colorlessCost;

    /**
     * Instantiates a new parser cardname txt mana cost.
     * 
     * @param cost
     *            the cost
     */
    public ManaCostParser(final String cost) {
        this.cost = cost.split(" ");
        // System.out.println(cost);
        this.nextToken = 0;
        this.colorlessCost = 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.CardManaCost.ManaParser#getTotalColorlessCost()
     */
    @Override
    public final int getTotalColorlessCost() {
        if (this.hasNext()) {
            throw new RuntimeException("Colorless cost should be obtained after iteration is complete");
        }
        return this.colorlessCost;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public final boolean hasNext() {
        return this.nextToken < this.cost.length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
    @Override
    public final ManaCostShard next() {

        final String unparsed = this.cost[this.nextToken++];
        // System.out.println(unparsed);
        if (StringUtils.isNumeric(unparsed)) {
            this.colorlessCost += Integer.parseInt(unparsed);
            return null;
        }

        return ManaCostShard.parseNonGeneric(unparsed);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
    } // unsuported
}
