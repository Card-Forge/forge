package forge.card;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.card.CardManaCost.ManaParser;

/**
 * <p>
 * CardReader class.
 * </p>
 * 
 * Forked from forge.CardReader at rev 10010.
 * 
 * @version $Id$
 */
public class CardRulesReader {

    private CardRuleCharacteristics[] characteristics = new CardRuleCharacteristics[] { new CardRuleCharacteristics(),
            null };
    private int curCharacteristics = 0;

    private boolean isFlipCard = false;
    private boolean isDoubleFacedCard = false;

    private boolean removedFromAIDecks = false;
    private boolean removedFromRandomDecks = false;

    // Reset all fields to parse next card (to avoid allocating new
    // CardRulesReader N times)
    /**
     * Reset.
     */
    public final void reset() {
        characteristics = new CardRuleCharacteristics[] { new CardRuleCharacteristics(), null };
        curCharacteristics = 0;
        removedFromAIDecks = false;
        removedFromRandomDecks = false;
        isDoubleFacedCard = false;
        isFlipCard = false;
    }

    /**
     * Gets the card.
     * 
     * @return the card
     */
    public final CardRules[] getCard() {
        CardRules[] ret = new CardRules[] {
                new CardRules(characteristics[0], isDoubleFacedCard, false, removedFromRandomDecks, removedFromAIDecks),
                null };
        if (characteristics[1] != null) {
            ret[1] = new CardRules(characteristics[1], isDoubleFacedCard, true, removedFromRandomDecks,
                    removedFromAIDecks);
        }

        return ret;
    }

    /**
     * Parses the line.
     * 
     * @param line
     *            the line
     */
    public final void parseLine(final String line) {
        if (line.startsWith("Name:")) {
            characteristics[curCharacteristics].setCardName(getValueAfterKey(line, "Name:"));
            if (characteristics[curCharacteristics].getCardName() == null
                    || characteristics[curCharacteristics].getCardName().isEmpty()) {
                throw new RuntimeException("Card name is empty");
            }

        } else if (line.startsWith("ManaCost:")) {
            String sCost = getValueAfterKey(line, "ManaCost:");
            characteristics[curCharacteristics].setManaCost("no cost".equals(sCost) ? CardManaCost.empty
                    : new CardManaCost(new ParserCardnameTxtManaCost(sCost)));

        } else if (line.startsWith("Types:")) {
            characteristics[curCharacteristics].setCardType(CardType.parse(getValueAfterKey(line, "Types:")));

        } else if (line.startsWith("Oracle:")) {
            characteristics[curCharacteristics].setCardRules(getValueAfterKey(line, "Oracle:").split("\\n"));

        } else if (line.startsWith("PT:")) {
            characteristics[curCharacteristics].setPtLine(getValueAfterKey(line, "PT:"));
        } else if (line.startsWith("Loyalty:")) {
            characteristics[curCharacteristics].setPtLine(getValueAfterKey(line, "Loyalty:"));

        } else if (line.startsWith("SVar:RemAIDeck:")) {
            removedFromAIDecks = "True".equalsIgnoreCase(getValueAfterKey(line, "SVar:RemAIDeck:"));

        } else if (line.startsWith("SVar:RemRandomDeck:")) {
            removedFromRandomDecks = "True".equalsIgnoreCase(getValueAfterKey(line, "SVar:RemRandomDeck:"));

        } else if (line.startsWith("SetInfo:")) {
            parseSetInfoLine(line, characteristics[curCharacteristics].getSetsData());

        } else if (line.startsWith("AlternateMode:")) {
            isDoubleFacedCard = "DoubleFaced".equalsIgnoreCase(getValueAfterKey(line, "AlternateMode:"));
            isFlipCard = "Flip".equalsIgnoreCase(getValueAfterKey(line, "AlternateMode:"));
        } else if (line.equals("ALTERNATE")) {
            characteristics[1] = new CardRuleCharacteristics();
            curCharacteristics = 1;
        }

    }

    /**
     * Parse a SetInfo line from a card txt file.
     * 
     * @param line
     *            must begin with "SetInfo:"
     * @param setsData
     *            the current mapping of set names to CardInSet instances
     */
    public static void parseSetInfoLine(final String line, final Map<String, CardInSet> setsData) {
        final int setCodeIx = 0;
        final int rarityIx = 1;
        final int numPicIx = 3;

        // Sample SetInfo line:
        // SetInfo:POR|Land|http://magiccards.info/scans/en/po/203.jpg|4

        final String value = line.substring("SetInfo:".length());
        final String[] pieces = value.split("\\|");

        if (pieces.length <= rarityIx) {
            throw new RuntimeException("SetInfo line <<" + value + ">> has insufficient pieces");
        }

        final String setCode = pieces[setCodeIx];
        final String txtRarity = pieces[rarityIx];
        // pieces[2] is the magiccards.info URL for illustration #1, which we do
        // not need.
        int numIllustrations = 1;

        if (setsData.containsKey(setCode)) {
            throw new RuntimeException("Found multiple SetInfo lines for set code <<" + setCode + ">>");
        }

        if (pieces.length > numPicIx) {
            try {
                numIllustrations = Integer.parseInt(pieces[numPicIx]);
            } catch (NumberFormatException nfe) {
                throw new RuntimeException("Fourth item of SetInfo is not an integer in <<" + value + ">>");
            }

            if (numIllustrations < 1) {
                throw new RuntimeException("Fourth item of SetInfo is not a positive integer, but" + numIllustrations);
            }
        }

        CardRarity rarity = null;
        if ("Land".equals(txtRarity)) {
            rarity = CardRarity.BasicLand;
        } else if ("Common".equals(txtRarity)) {
            rarity = CardRarity.Common;
        } else if ("Uncommon".equals(txtRarity)) {
            rarity = CardRarity.Uncommon;
        } else if ("Rare".equals(txtRarity)) {
            rarity = CardRarity.Rare;
        } else if ("Mythic".equals(txtRarity)) {
            rarity = CardRarity.MythicRare;
        } else if ("Special".equals(txtRarity)) {
            rarity = CardRarity.Special;
        } else {
            throw new RuntimeException("Unrecognized rarity string <<" + txtRarity + ">>");
        }

        CardInSet cardInSet = new CardInSet(rarity, numIllustrations);

        setsData.put(setCode, cardInSet);
    }

    /**
     * Gets the value after key.
     * 
     * @param line
     *            the line
     * @param fieldNameWithColon
     *            the field name with colon
     * @return the value after key
     */
    public static String getValueAfterKey(final String line, final String fieldNameWithColon) {
        final int startIx = fieldNameWithColon.length();
        final String lineAfterColon = line.substring(startIx);
        return lineAfterColon.trim();
    }

    /**
     * The Class ParserCardnameTxtManaCost.
     */
    public static class ParserCardnameTxtManaCost implements ManaParser {
        private final String[] cost;
        private int nextToken;
        private int colorlessCost;

        /**
         * Instantiates a new parser cardname txt mana cost.
         * 
         * @param cost
         *            the cost
         */
        public ParserCardnameTxtManaCost(final String cost) {
            this.cost = cost.split(" ");
            // System.out.println(cost);
            nextToken = 0;
            colorlessCost = 0;
        }

        /*
         * (non-Javadoc)
         * 
         * @see forge.card.CardManaCost.ManaParser#getTotalColorlessCost()
         */
        public final int getTotalColorlessCost() {
            if (hasNext()) {
                throw new RuntimeException("Colorless cost should be obtained after iteration is complete");
            }
            return colorlessCost;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public final boolean hasNext() {
            return nextToken < cost.length;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#next()
         */
        @Override
        public final CardManaCostShard next() {

            String unparsed = cost[nextToken++];
            // System.out.println(unparsed);
            if (StringUtils.isNumeric(unparsed)) {
                colorlessCost += Integer.parseInt(unparsed);
                return null;
            }

            int atoms = 0;
            for (int iChar = 0; iChar < unparsed.length(); iChar++) {
                switch (unparsed.charAt(iChar)) {
                case 'W':
                    atoms |= CardManaCostShard.Atom.WHITE;
                    break;
                case 'U':
                    atoms |= CardManaCostShard.Atom.BLUE;
                    break;
                case 'B':
                    atoms |= CardManaCostShard.Atom.BLACK;
                    break;
                case 'R':
                    atoms |= CardManaCostShard.Atom.RED;
                    break;
                case 'G':
                    atoms |= CardManaCostShard.Atom.GREEN;
                    break;
                case '2':
                    atoms |= CardManaCostShard.Atom.OR_2_COLORLESS;
                    break;
                case 'P':
                    atoms |= CardManaCostShard.Atom.OR_2_LIFE;
                    break;
                case 'X':
                    atoms |= CardManaCostShard.Atom.IS_X;
                    break;
                default:
                    break;
                }
            }
            return CardManaCostShard.valueOf(atoms);
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

}
