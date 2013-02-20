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
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

import forge.card.mana.ManaCostShard;
import forge.card.mana.IParserManaCost;
import forge.card.mana.ManaCost;


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

    // fields to build 
    private List<String> originalScript = new ArrayList<String>();

    private CardFace[] faces = new CardFace[] { null, null };
    private String[] pictureUrl = new String[] { null, null };
    private int curFace = 0;
    private CardSplitType altMode;
    private String handLife = null; 
    
    // fields to build CardAiHints
    private boolean removedFromAIDecks = false;
    private boolean removedFromRandomDecks = false;
    private DeckHints hints = null;
    private DeckHints needs = null;
    


    // Reset all fields to parse next card (to avoid allocating new
    // CardRulesReader N times)
    /**
     * Reset.
     */
    public final void reset() {
        originalScript.clear();

        this.curFace = 0;
        this.faces[0] = null;
        this.faces[1] = null;
        this.pictureUrl[0] = null;
        this.pictureUrl[1] = null;

        this.handLife = null;
        this.altMode = CardSplitType.None;

        this.removedFromAIDecks = false;
        this.removedFromRandomDecks = false;
        this.needs = null;
        this.hints = null;
    }

    /**
     * Gets the card.
     * 
     * @return the card
     */
    public final CardRules getCard() {
        CardAiHints cah = new CardAiHints(removedFromAIDecks, removedFromRandomDecks, hints, needs );
        faces[0].calculateColor();
        if ( null != faces[1] ) faces[1].calculateColor();
        final CardRules result = new CardRules(faces, altMode, cah, originalScript);
        result.setDlUrls(pictureUrl);
        if ( StringUtils.isNotBlank(handLife))
            result.setVanguardProperties(handLife);
        return result;
    }

    /**
     * Parses the line.
     * 
     * @param line
     *            the line
     */
    public final void parseLine(final String line) {

        originalScript.add(line);
        
        int colonPos = line.indexOf(':');
        String key = colonPos > 0 ? line.substring(0, colonPos) : line;
        String value = colonPos > 0 ? line.substring(1+colonPos).trim() : null;


        switch(key.charAt(0)) {
            case 'A':
                if ("AlternateMode".equals(key)) {
                    //System.out.println(faces[curFace].getName());
                    this.altMode = CardSplitType.smartValueOf(value);
                } else if ("ALTERNATE".equals(key)) {
                    this.curFace = 1;
                }
            break;

            case 'C':
                if ("Colors".equals(key)) {
                    // This is forge.card.CardColor not forge.CardColor.
                    // Why do we have two classes with the same name?
                    ColorSet newCol = ColorSet.fromNames(value.split(","));
                    this.faces[this.curFace].setColor(newCol);
                }
                break;

            case 'D':
                if ("DeckHints".equals(key)) {
                    hints = new DeckHints(value);
                } else if ("DeckNeeds".equals(key)) {
                    needs = new DeckHints(value);
                }
                break;

            case 'H':
                if ("HandLifeModifier".equals(key)) {
                    handLife = value;
                }
                break;

            case 'K':
                if ("K".equals(key)) {
                    this.faces[this.curFace].addKeyword(value);
                }
                break;

            case 'L':
                if ("Loyalty".equals(key)) {
                    this.faces[this.curFace].setInitialLoyalty(Integer.valueOf(value));
                }
                break;

            case 'M':
                if ("ManaCost".equals(key)) {
                    this.faces[this.curFace].setManaCost("no cost".equals(value) ? ManaCost.NO_COST 
                            : new ManaCost(new ParserCardnameTxtManaCost(value)));
                }
                break;

            case 'N':
                if ("Name".equals(key)) {
                    this.faces[this.curFace] = new CardFace(value);
                }
                break;

            case 'O':
                if ("Oracle".equals(key)) {
                    this.faces[this.curFace].setOracleText(value);

                }
                break;

            case 'P':
                if ("PT".equals(key)) {
                    this.faces[this.curFace].setPtText(value);
                }
                break;

            case 'S':
                if ( "SVar".equals(key) ) {
                    if ( null == value ) throw new IllegalArgumentException("SVar has no variable name");

                    colonPos = value.indexOf(':');
                    String variable = colonPos > 0 ? value.substring(0, colonPos) : value;
                    value = colonPos > 0 ? value.substring(1+colonPos) : null;

                    if ( "RemAIDeck".equals(variable) ) {
                        this.removedFromAIDecks = "True".equalsIgnoreCase(value);
                    } else if ( "RemRandomDeck".equals(variable) ) {
                        this.removedFromRandomDecks = "True".equalsIgnoreCase(value);
                    } else if ( "Picture".equals(variable) ) {
                        this.pictureUrl[this.curFace] = value;
                    }
                } else if (line.startsWith("SetInfo:")) {
                    CardRulesReader.parseSetInfoLine(value, this.faces[this.curFace].getSetsData());
                }
                break;

            case 'T':
                if ("Types".equals(key)) {
                    this.faces[this.curFace].setType(CardType.parse(value));
                }
                break;
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
    public static void parseSetInfoLine(final String value, final Map<String, CardInSet> setsData) {
        final int setCodeIx = 0;
        final int rarityIx = 1;
        final int numPicIx = 3;

        // Sample SetInfo line:
        // SetInfo:POR|Land|http://magiccards.info/scans/en/po/203.jpg|4

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
            } catch (final NumberFormatException nfe) {
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

        final CardInSet cardInSet = new CardInSet(rarity, numIllustrations, pieces[2]);

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
    public static class ParserCardnameTxtManaCost implements IParserManaCost {
        private final StringTokenizer st; 
        private int colorlessCost;

        public ParserCardnameTxtManaCost(final String cost) {
            st = new StringTokenizer(cost, " ");
            this.colorlessCost = 0;
        }

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
            return st.hasMoreTokens();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#next()
         */
        @Override
        public final ManaCostShard next() {

            final String unparsed = st.nextToken();
            // System.out.println(unparsed);
            try {
                int iVal = Integer.parseInt(unparsed);
                this.colorlessCost += iVal;
                return null;
            }
            catch (NumberFormatException nex) { }

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
