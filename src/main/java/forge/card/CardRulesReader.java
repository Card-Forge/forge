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

import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import forge.card.mana.IParserManaCost;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;


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
    public final static EditionCollection editions = new EditionCollection(); // create a copy here, Singletons.model... is not initialized yet.
    
    // fields to build 
    private CardFace[] faces = new CardFace[] { null, null };
    private String[] pictureUrl = new String[] { null, null };
    private int curFace = 0;
    private CardSplitType altMode = CardSplitType.None;
    private String handLife = null; 
    
    private Map<String,CardInSet> sets = new TreeMap<String, CardInSet>(String.CASE_INSENSITIVE_ORDER);
    
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
        this.curFace = 0;
        this.faces[0] = null;
        this.faces[1] = null;
        this.pictureUrl[0] = null;
        this.pictureUrl[1] = null;
        
        this.sets.clear();

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
        faces[0].assignMissingFields();
        if (null != faces[1]) faces[1].assignMissingFields();
        final CardRules result = new CardRules(faces, altMode, cah, sets);
        result.setDlUrls(pictureUrl);
        if (StringUtils.isNotBlank(handLife))
            result.setVanguardProperties(handLife);
        return result;
    }

    public final CardRules readCard(final Iterable<String> script) {
        this.reset();
        for (String line : script) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            this.parseLine(line);
        }
        return this.getCard();
    }
    
    
    /**
     * Parses the line.
     * 
     * @param line
     *            the line
     */
    public final void parseLine(final String line) {
        int colonPos = line.indexOf(':');
        String key = colonPos > 0 ? line.substring(0, colonPos) : line;
        String value = colonPos > 0 ? line.substring(1+colonPos).trim() : null;

        switch(key.charAt(0)) {
            case 'A':
                if ("A".equals(key))
                    this.faces[curFace].addAbility(value);
                else if ("AlternateMode".equals(key)) {
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

            case 'R':
                if ("R".equals(key)) {
                    this.faces[this.curFace].addReplacementEffect(value);
                }
                break;

            case 'S':
                if ("S".equals(key)) {
                    this.faces[this.curFace].addStaticAbility(value);
                } else if ( "SVar".equals(key) ) {
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
                    } else if ( "Rarity".equals(variable) ) {
                        // discard that, they should supply it in SetInfo
                    } else
                        this.faces[curFace].addSVar(variable, value);
                } else if ("SetInfo".equals(key)) {
                    parseSetInfoLine(value);
                }
                break;

            case 'T':
                if ("T".equals(key)) {
                    this.faces[this.curFace].addTrigger(value);
                } else if ("Types".equals(key)) {
                    this.faces[this.curFace].setType(CardType.parse(value));
                } else if ("Text".equals(key) && !"no text".equals(value) && StringUtils.isNotBlank(value)) {
                    this.faces[this.curFace].setNonAbilityText(value);
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
    private void parseSetInfoLine(final String value) {
        // Sample SetInfo line:
        // SetInfo:POR Land x4

        int i = 0;
        String setCode = null;
        String txtRarity = "Common";
        String txtCount = "x1";
        
        StringTokenizer stt = new StringTokenizer(value, " ");
        while(stt.hasMoreTokens()) {
            if( i == 0 ) setCode = stt.nextToken();
            if( i == 1 ) txtRarity = stt.nextToken();
            if( i == 2 ) txtCount = stt.nextToken();
            i++;
        }


        int numIllustrations = 1;
        if ( i > 2 )
            numIllustrations = Integer.parseInt(txtCount.substring(1));

        if (sets.containsKey(setCode)) {
            System.err.print(faces[0].getName());
            throw new RuntimeException("Found multiple SetInfo lines for set code <<" + setCode + ">>");
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

        final CardInSet cardInSet = new CardInSet(rarity, numIllustrations);

        sets.put(setCode, cardInSet);
    }

    /**
     * Instantiates class, reads a card. Do not use for batch operations.
     * @param script
     * @return
     */
    public static CardRules parseSingleCard(Iterable<String> script) {
        CardRulesReader crr = new CardRulesReader();
        for(String line : script) {
            crr.parseLine(line);
        }
        return crr.getCard();
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
