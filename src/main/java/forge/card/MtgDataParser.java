package forge.card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.FileUtil;
import forge.card.CardManaCost.ManaParser;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/** This class can read CardRules from Arch's mtg-data.txt file. */
public final class MtgDataParser implements Iterator<CardRules> {

    private final Iterator<String> it;
    private final List<String> mtgDataLines;

    /**
     * Instantiates a new mtg data parser.
     */
    public MtgDataParser() {
        this.mtgDataLines = FileUtil.readFile(ForgeProps.getFile(NewConstants.MTG_DATA));
        this.it = this.mtgDataLines.iterator();
        this.skipSetList();
    }

    private static List<String> setsToSkipPrefixes = new ArrayList<String>();
    private static List<String> unSets = new ArrayList<String>(); // take only
                                                                  // lands from
                                                                  // there
    static {
        MtgDataParser.setsToSkipPrefixes.add("VG"); // Vanguard
        MtgDataParser.setsToSkipPrefixes.add("ME"); // Mtgo master's editions
        MtgDataParser.setsToSkipPrefixes.add("FV"); // From the vaults

        // Duel decks... or... should I keep them?
        MtgDataParser.setsToSkipPrefixes.add("DVD");
        MtgDataParser.setsToSkipPrefixes.add("EVT");
        MtgDataParser.setsToSkipPrefixes.add("EVG");
        MtgDataParser.setsToSkipPrefixes.add("GVL");
        MtgDataParser.setsToSkipPrefixes.add("JVC");
        MtgDataParser.setsToSkipPrefixes.add("DDG");
        MtgDataParser.setsToSkipPrefixes.add("PVC");

        // Archenemy - we cannot play it now anyway
        MtgDataParser.setsToSkipPrefixes.add("ARC");

        // Planechase - this too
        MtgDataParser.setsToSkipPrefixes.add("HOP");

        // Reprints
        MtgDataParser.setsToSkipPrefixes.add("BRB");
        MtgDataParser.setsToSkipPrefixes.add("BTD");
        MtgDataParser.setsToSkipPrefixes.add("DKM");
        // setsToSkipPrefixes.add("ATH"); // No need to skip it really.
        // On gatherer's opinion this cards were released twice in original set

        // Promo sets - all cards have been issued in other sets
        MtgDataParser.setsToSkipPrefixes.add("SDC");
        MtgDataParser.setsToSkipPrefixes.add("ASTRAL");

        // Premium decks
        MtgDataParser.setsToSkipPrefixes.add("H09");
        MtgDataParser.setsToSkipPrefixes.add("H10");

        // Un-sets are weird, but lands from there are valuable
        MtgDataParser.unSets.add("UNH");
        MtgDataParser.unSets.add("UGL");
    }

    private boolean weHaveNext;

    private void skipSetList() {
        String nextLine = this.it.next();
        while ((nextLine.length() > 0) && this.it.hasNext()) {
            nextLine = this.it.next();
        }
        this.weHaveNext = this.it.hasNext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        return this.weHaveNext;
    }

    private final CardRuleCharacteristics[] chars = new CardRuleCharacteristics[2];

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
    @Override
    public CardRules next() {
        if (this.chars[1] != null) {
            final CardRules ret = new CardRules(this.chars[1], false, true, false, false);
            return ret;
        }
        this.chars[0] = new CardRuleCharacteristics();
        final Map<String, CardInSet> sets = new HashMap<String, CardInSet>();

        String nextline = this.readSingleCard(this.chars[0]);
        if (nextline != null) {
            if (nextline.equals("----")) {
                this.chars[1] = new CardRuleCharacteristics();
                nextline = this.readSingleCard(this.chars[1]);
            }
            if (!nextline.isEmpty()) {
                final String setsLine = nextline;
                final boolean isBasicLand = this.chars[0].getCardType().isLand()
                        && this.chars[0].getCardType().isBasic();
                this.chars[0].setSetsData(this.getValidEditions(setsLine, isBasicLand));
                if (this.chars[1] != null) {
                    this.chars[1].setSetsData(this.getValidEditions(setsLine, isBasicLand));
                }
            }
        }

        // feel free to return null after this line
        if (sets.isEmpty()) {
            return null;
        } // that was a bad card - it won't be added by invoker
        if (this.chars[0] == null) {
            return null;
        }

        return new CardRules(this.chars[0], false, false, false, false);
    }

    private String readSingleCard(final CardRuleCharacteristics ret) {

        if (!this.it.hasNext()) {
            this.weHaveNext = false;
            return null;
        }
        ret.setCardName(this.it.next());

        if (!this.it.hasNext()) {
            this.weHaveNext = false;
            return null;
        }

        String manaCost = this.it.next();
        ret.setManaCost(CardManaCost.EMPTY);
        CardType type = null;
        if (manaCost.startsWith("{")) {
            ret.setManaCost(new CardManaCost(new ManaParserMtgData(manaCost)));
            if (!this.it.hasNext()) {
                this.weHaveNext = false;
                return null;
            }
            type = CardType.parse(this.it.next());
        } else { // Land?
            type = CardType.parse(manaCost);
            manaCost = null;
        }
        ret.setPtLine(null);
        if (type.isCreature() || type.isPlaneswalker()) {
            if (!this.it.hasNext()) {
                this.weHaveNext = false;
                return null;
            }
            ret.setPtLine(this.it.next());
        }

        final String nextline = this.it.next();
        final ArrayList<String> rules = new ArrayList<String>();
        while ((nextline != null)
                && !nextline.isEmpty()
                && !nextline.equals("----")
                && !java.util.regex.Pattern.matches(
                        "([A-Z0-9][A-Z0-9][A-Z0-9] [CURM], )*[A-Z0-9][A-Z0-9][A-Z0-9] [CURM]", nextline)) {
            rules.add(nextline);
        }
        ret.setCardRules((String[]) rules.toArray());

        return nextline;
    }

    private Map<String, CardInSet> getValidEditions(final String sets, final boolean isBasicLand) {
        final String[] setsData = sets.split(", ");
        final Map<String, CardInSet> result = new HashMap<String, CardInSet>();
        for (final String element : setsData) {
            final int spacePos = element.indexOf(' ');
            final String setCode = element.substring(0, spacePos);
            boolean shouldSkip = false;
            for (final String s : MtgDataParser.setsToSkipPrefixes) {
                if (setCode.startsWith(s)) {
                    shouldSkip = true;
                    break;
                }
            }
            for (final String s : MtgDataParser.unSets) {
                if (setCode.startsWith(s) && !isBasicLand) {
                    shouldSkip = true;
                    break;
                }
            }
            if (shouldSkip) {
                continue;
            }
            result.put(setCode, MtgDataParser.parseCardInSet(element, spacePos));
        }
        return result;
    }

    /**
     * Parses the card in set.
     * 
     * @param unparsed
     *            the unparsed
     * @param spaceAt
     *            the space at
     * @return the card in set
     */
    public static CardInSet parseCardInSet(final String unparsed, final int spaceAt) {
        final char rarity = unparsed.charAt(spaceAt + 1);
        CardRarity rating;
        switch (rarity) {
        case 'L':
            rating = CardRarity.BasicLand;
            break;
        case 'C':
            rating = CardRarity.Common;
            break;
        case 'U':
            rating = CardRarity.Uncommon;
            break;
        case 'R':
            rating = CardRarity.Rare;
            break;
        case 'M':
            rating = CardRarity.MythicRare;
            break;
        case 'S':
            rating = CardRarity.Special;
            break;
        default:
            rating = CardRarity.MythicRare;
            break;
        }

        int number = 1;
        final int bracketAt = unparsed.indexOf('(', spaceAt);
        if (-1 != bracketAt) {
            final String sN = unparsed.substring(bracketAt + 2, unparsed.indexOf(')', bracketAt));
            number = Integer.parseInt(sN);
        }
        return new CardInSet(rating, number);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
    }

    /**
     * This is a mana-parser for mana written in curly braces like {2}{R}{B}.
     */
    public static final class ManaParserMtgData implements ManaParser {
        private final String cost;

        private int nextBracket;
        private int colorlessCost;

        /**
         * Instantiates a new mana parser mtg data.
         * 
         * @param cost0
         *            the cost0
         */
        public ManaParserMtgData(final String cost0) {
            this.cost = cost0;
            // System.out.println(cost);
            this.nextBracket = cost0.indexOf('{');
            this.colorlessCost = 0;
        }

        /*
         * (non-Javadoc)
         * 
         * @see forge.card.CardManaCost.ManaParser#getTotalColorlessCost()
         */
        @Override
        public int getTotalColorlessCost() {
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
        public boolean hasNext() {
            return this.nextBracket != -1;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#next()
         */
        @Override
        public CardManaCostShard next() {
            final int closeBracket = this.cost.indexOf('}', this.nextBracket);
            final String unparsed = this.cost.substring(this.nextBracket + 1, closeBracket);
            this.nextBracket = this.cost.indexOf('{', closeBracket + 1);

            // System.out.println(unparsed);
            if (StringUtils.isNumeric(unparsed)) {
                this.colorlessCost += Integer.parseInt(unparsed);
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
