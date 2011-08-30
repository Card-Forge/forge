
package forge.card;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
//import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipFile;

import forge.card.CardManaCost.ManaParser;
import forge.error.ErrorViewer;
import forge.properties.NewConstants;


/**
 * <p>CardReader class.</p>
 *
 * Forked from forge.CardReader at rev 10010.
 * 
 * @version $Id$
 */
public class CardRulesReader {
    
    private String cardName = null;
    private CardType cardType = null;
    private CardManaCost manaCost = CardManaCost.empty;
    private String ptLine = null;
    private String[] cardRules = null;
    private Map<String, CardInSet> setsData = new TreeMap<String, CardInSet>();
    private boolean removedFromAIDecks = false;
    private boolean removedFromRandomDecks = false;

    // Reset all fields to parse next card (to avoid allocating new CardRulesReader N times)
    public final void reset() {
        cardName = null;
        cardType = null;
        manaCost = CardManaCost.empty;
        ptLine = null;
        cardRules = null;
        setsData = new TreeMap<String, CardInSet>();
        removedFromAIDecks = false;
        removedFromRandomDecks = false;
    }

    public final CardRules getCard() {
        return new CardRules(cardName, cardType, manaCost, ptLine, cardRules, setsData, removedFromRandomDecks, removedFromAIDecks);
    }
    

    public final void parseLine(final String line) {
        if (line.startsWith("Name:")) {
            cardName = getValueAfterKey(line, "Name:");
            if (cardName == null || cardName.isEmpty()) {
                throw new RuntimeException("Card name is empty");
            }

        } else if (line.startsWith("ManaCost:")) {
            String sCost = getValueAfterKey(line, "ManaCost:");
            manaCost = "no cost".equals(sCost) ? CardManaCost.empty : new CardManaCost(new ParserCardnameTxtManaCost(sCost));

        } else if (line.startsWith("Types:")) {
            cardType = CardType.parse(getValueAfterKey(line, "Types:"));

        } else if (line.startsWith("Oracle:")) {
            cardRules = getValueAfterKey(line, "Oracle:").split("\\n");

        } else if (line.startsWith("PT:")) {
            ptLine = getValueAfterKey(line, "PT:");
        } else if (line.startsWith("Loyalty:")) {
            ptLine = getValueAfterKey(line, "Loyalty:");

        } else if (line.startsWith("SVar:RemAIDeck:")) {
            removedFromAIDecks = "True".equalsIgnoreCase(getValueAfterKey(line, "SVar:RemAIDeck:"));

        } else if (line.startsWith("SVar:RemRandomDeck:")) {
            removedFromRandomDecks = "True".equalsIgnoreCase(getValueAfterKey(line, "SVar:RemRandomDeck:"));

        } else if (line.startsWith("SetInfo:")) {
            parseSetInfoLine(line, setsData);
        }
    }

    /**
     * Parse a SetInfo line from a card txt file.
     * 
     * @param txtFileLocator  used in error messages
     * @param lineNum  used in error messages
     * @param line  must begin with "SetInfo:"
     * @param setsData  the current mapping of set names to CardInSet instances
     * 
     * @throws CardParsingException  if there is a problem parsing the line
     */
    public static void parseSetInfoLine(final String line, final Map<String, CardInSet> setsData)
    {
        final int setCodeIx = 0;
        final int rarityIx = 1;
        final int numPicIx = 3;

        // Sample SetInfo line:
        //SetInfo:POR|Land|http://magiccards.info/scans/en/po/203.jpg|4

        final String value = line.substring("SetInfo:".length());
        final String[] pieces = value.split("\\|");

        if (pieces.length <= rarityIx) {
            throw new RuntimeException(
                    "SetInfo line <<" + value + ">> has insufficient pieces");
        }

        final String setCode = pieces[setCodeIx];
        final String txtRarity = pieces[rarityIx];
        // pieces[2] is the magiccards.info URL for illustration #1, which we do not need.
        int numIllustrations = 1;

        if (setsData.containsKey(setCode)) {
            throw new RuntimeException(
                    "Found multiple SetInfo lines for set code <<" + setCode + ">>");
        }

        if (pieces.length > numPicIx) {
            try {
                numIllustrations = Integer.parseInt(pieces[numPicIx]);
            }
            catch (NumberFormatException nfe) {
                throw new RuntimeException(
                        "Fourth item of SetInfo is not an integer in <<"
                        + value + ">>");
            }

            if (numIllustrations < 1) {
                throw new RuntimeException(
                        "Fourth item of SetInfo is not a positive integer, but"
                        + numIllustrations);
            }
        }

        CardRarity rarity = null;
        if ("Land".equals(txtRarity)) {
            rarity = CardRarity.BasicLand;
        }
        else if ("Common".equals(txtRarity)) {
            rarity = CardRarity.Common;
        }
        else if ("Uncommon".equals(txtRarity)) {
            rarity = CardRarity.Uncommon;
        }
        else if ("Rare".equals(txtRarity)) {
            rarity = CardRarity.Rare;
        }
        else if ("Mythic".equals(txtRarity)) {
            rarity = CardRarity.MythicRare;
        }
        else if ("Special".equals(txtRarity)) {
            rarity = CardRarity.Special;
        }
        else {
            throw new RuntimeException("Unrecognized rarity string <<" + txtRarity + ">>");
        }

        CardInSet cardInSet = new CardInSet(rarity, numIllustrations);

        setsData.put(setCode, cardInSet);
    }

    public static String getValueAfterKey(final String line, final String fieldNameWithColon) {
        final int startIx = fieldNameWithColon.length();
        final String lineAfterColon = line.substring(startIx);
        return lineAfterColon.trim();
    }


    
    public static class ParserCardnameTxtManaCost implements ManaParser {
        private final String[] cost;
        private int nextToken;
        private int colorlessCost;


        public ParserCardnameTxtManaCost(final String cost) {
            this.cost = cost.split(" ");
            // System.out.println(cost);
            nextToken = 0;
            colorlessCost = 0;
        }

        public int getTotalColorlessCost() { 
            if ( hasNext() ) { 
                throw new RuntimeException("Colorless cost should be obtained after iteration is complete");
            }
            return colorlessCost;
        }

        @Override
        public boolean hasNext() { return nextToken < cost.length; }

        @Override
        public CardManaCostShard next() {
            
            String unparsed = cost[nextToken++];;

            // System.out.println(unparsed);
            if (StringUtils.isNumeric(unparsed)) {
                colorlessCost += Integer.parseInt(unparsed);
                return null;
            }

            int atoms = 0;
            for (int iChar = 0; iChar < unparsed.length(); iChar++) {
                switch (unparsed.charAt(iChar)) {
                    case 'W': atoms |= CardManaCostShard.Atom.WHITE; break;
                    case 'U': atoms |= CardManaCostShard.Atom.BLUE; break;
                    case 'B': atoms |= CardManaCostShard.Atom.BLACK; break;
                    case 'R': atoms |= CardManaCostShard.Atom.RED; break;
                    case 'G': atoms |= CardManaCostShard.Atom.GREEN; break;
                    case '2': atoms |= CardManaCostShard.Atom.OR_2_COLORLESS; break;
                    case 'P': atoms |= CardManaCostShard.Atom.OR_2_LIFE; break;
                    case 'X': atoms |= CardManaCostShard.Atom.IS_X; break;
                    default: break;
                }
            }
            return CardManaCostShard.valueOf(atoms);
        }

        @Override
        public void remove() { } // unsuported
    }    


}

