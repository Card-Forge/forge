/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.card.mana.ManaCostShard;
import forge.card.mana.IParserManaCost;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.FileUtil;

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
        boolean hasOtherPart = false;
        this.chars[0] = new CardRuleCharacteristics();
        final Map<String, CardInSet> sets = new HashMap<String, CardInSet>();

        String nextline = this.readSingleCard(this.chars[0]);
        if (nextline != null) {
            if (nextline.equals("----")) {
                hasOtherPart = true;
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

        final CardRules otherPart = hasOtherPart ? new CardRules(this.chars[1], null, hasOtherPart, null, false, false)
                : null;
        return new CardRules(this.chars[0], null, hasOtherPart, otherPart, false, false);
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
        ret.setManaCost(SpellManaCost.NO_COST);
        CardType type = null;
        if (manaCost.startsWith("{")) {
            ret.setManaCost(new SpellManaCost(new ManaParserMtgData(manaCost)));
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
        ret.setCardRules(StringUtils.join(rules, '\n'));

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
        return new CardInSet(rating, number, null);
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
    public static final class ManaParserMtgData implements IParserManaCost {
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
        public ManaCostShard next() {
            final int closeBracket = this.cost.indexOf('}', this.nextBracket);
            final String unparsed = this.cost.substring(this.nextBracket + 1, closeBracket);
            this.nextBracket = this.cost.indexOf('{', closeBracket + 1);

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
}
