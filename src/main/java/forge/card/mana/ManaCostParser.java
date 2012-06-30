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

        int atoms = 0;
        for (int iChar = 0; iChar < unparsed.length(); iChar++) {
            switch (unparsed.charAt(iChar)) {
            case 'W':
                atoms |= ManaCostShard.Atom.WHITE;
                break;
            case 'U':
                atoms |= ManaCostShard.Atom.BLUE;
                break;
            case 'B':
                atoms |= ManaCostShard.Atom.BLACK;
                break;
            case 'R':
                atoms |= ManaCostShard.Atom.RED;
                break;
            case 'G':
                atoms |= ManaCostShard.Atom.GREEN;
                break;
            case '2':
                atoms |= ManaCostShard.Atom.OR_2_COLORLESS;
                break;
            case 'P':
                atoms |= ManaCostShard.Atom.OR_2_LIFE;
                break;
            case 'S':
                atoms |= ManaCostShard.Atom.IS_SNOW;
                break;
            case 'X':
                atoms |= ManaCostShard.Atom.IS_X;
                break;
            default:
                break;
            }
        }
        return ManaCostShard.valueOf(atoms);
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
