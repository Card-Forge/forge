package forge.card.mana;

import org.apache.commons.lang3.StringUtils;


/**
 * The Class ParserCardnameTxtManaCost.
 */
public class ManaCostParser implements IParserManaCost {
    private final String[] cost;
    private int nextToken;
    private int genericCost;

    /**
     * Parse the given cost and output formatted cost string
     * 
     * @param cost
     */
    public static String parse(final String cost) {
    	final ManaCostParser parser = new ManaCostParser(cost);
    	final ManaCost manaCost = new ManaCost(parser);
    	return manaCost.toString();
    }

    /**
     * Instantiates a new parser cardname txt mana cost.
     * 
     * @param cost
     *            the cost
     */
    public ManaCostParser(final String cost) {
        this.cost = cost.split(" ");
        this.nextToken = 0;
        this.genericCost = 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.CardManaCost.ManaParser#getTotalGenericCost()
     */
    @Override
    public final int getTotalGenericCost() {
        if (this.hasNext()) {
            throw new RuntimeException("Generic cost should be obtained after iteration is complete");
        }
        return this.genericCost;
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
        if (StringUtils.isNumeric(unparsed)) {
            this.genericCost += Integer.parseInt(unparsed);
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
