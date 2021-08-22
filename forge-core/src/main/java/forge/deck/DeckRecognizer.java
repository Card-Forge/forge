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
package forge.deck;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardType;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * DeckRecognizer class.
 * </p>
 * 
 * @author Forge
 * @version $Id: DeckRecognizer.java 10499 2011-09-17 15:08:47Z Max mtg $
 * 
 */
public class DeckRecognizer {
    /**
     * The Enum TokenType.
     */
    public enum TokenType {
        KnownCard,
        UnknownCard,
        IllegalCard,
        DeckName,
        DeckSectionName,
        Comment,
        UnknownText,
        CardType
    }

    /**
     * The Class Token.
     */
    public static class Token {
        private final TokenType type;
        private final PaperCard card;
        private final int number;
        private final String text;

        public static Token knownCard(final PaperCard theCard, final int count) {
            return new Token(theCard, TokenType.KnownCard, count, null);
        }

        public static Token illegalCard(final String cardName, final String setCode, final int count) {
            String ttext = setCode == null || setCode.equals("") ? cardName :
                           String.format("%s [%s]", cardName, setCode);
            return new Token(null, TokenType.IllegalCard, count, ttext);
        }

        public static Token unknownCard(final String cardName, final String setCode, final int count) {
            String ttext = setCode == null || setCode.equals("") ? cardName :
                    String.format("%s [%s]", cardName, setCode);
            return new Token(null, TokenType.UnknownCard, count, ttext);
        }

        public static Token deckSection(final String sectionName){
            if (sectionName.contains("avatar"))
                return new Token(TokenType.DeckSectionName, 1, DeckSection.Avatar.name());
            if (sectionName.contains("commander"))
                return new Token(TokenType.DeckSectionName, 1, DeckSection.Commander.name());
            if (sectionName.contains("schemes"))
                return new Token(TokenType.DeckSectionName, 1, DeckSection.Schemes.name());
            if (sectionName.contains("conspiracy"))
                return new Token(TokenType.DeckSectionName, 1, DeckSection.Conspiracy.name());
            if (sectionName.contains("side") || sectionName.contains("sideboard"))
                return new Token(TokenType.DeckSectionName, 1, DeckSection.Sideboard.name());
            if (sectionName.contains("main") || sectionName.contains("card"))
                return new Token(TokenType.DeckSectionName, 1, DeckSection.Main.name());
            return null;
        }

        private Token(final PaperCard knownCard, final TokenType type1, final int count, final String message) {
            this.card = knownCard;
            this.number = count;
            this.type = type1;
            this.text = message;
        }

        public Token(final TokenType type1, final int count, final String message) {
            this(null, type1, count, message);
            if ((type1 == TokenType.KnownCard) || (type1 == TokenType.UnknownCard)) {
                throw new IllegalArgumentException("Use factory methods for recognized " + REGRP_CARD + " lines");
            }
        }

        public final String getText() {
            return this.text;
        }

        public final PaperCard getCard() {
            return this.card;
        }

        public final TokenType getType() {
            return this.type;
        }

        public final int getNumber() {
            return this.number;
        }
    }

//    // Let's think about it numbers in the back later
//    // private static final Pattern searchNumbersInBack =
//    // Pattern.compile("(.*)[^A-Za-wyz]*\\s+([\\d]{1,2})");
//    private static final Pattern SEARCH_NUMBERS_IN_FRONT = Pattern.compile("([\\d]{1,2})[^A-Za-wyz]*\\s+(.*)");
//    //private static final Pattern READ_SEPARATED_EDITION = Pattern.compile("[[\\(\\{]([a-zA-Z0-9]){1,3})[]*\\s+(.*)");
//    private static final Pattern SEARCH_SINGLE_SLASH = Pattern.compile("(?<=[^/])\\s*/\\s*(?=[^/])");

    // Utility Constants
    private final Pattern SEARCH_SINGLE_SLASH = Pattern.compile("(?<=[^/])\\s*/\\s*(?=[^/])");
    private static final String LINE_COMMENT_DELIMITER = "#";
    private static final String DOUBLE_SLASH = "//";

//    private static final Pattern EDITION_AFTER_CARD_NAME = Pattern.compile("([\\d]{1,2})?\\s*([a-zA-Z',\\/\\-\\s]+)\\s*((\\(|\\[)([a-zA-Z0-9]{3})(\\)|\\])\\s*)?([\\d]{1,3})?");

    // Core Matching Patterns (initialised in Constructor)
    private static final String REGRP_DECKNAME = "deckName";
    private static final String DECK_NAME_REGEX =
            String.format("(?<pre>(deck(\\s+name)?)|((deck\\s+)?name))(\\:)?\\s*(?<%s>[a-zA-Z',\\/\\-\\s]+)\\s*",
                    REGRP_DECKNAME);
    private static final Pattern DECK_NAME_PATTERN = Pattern.compile(DECK_NAME_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String REGRP_CARDNO = "count";
    private static final String REGRP_SET = "setCode";
    private static final String REGRP_COLLNO = "collectorNumber";
    private static final String REGRP_CARD = "card";

    private static final String CARD_REGEX_SET_NAME = String.format(
            "(?<%s>[\\d]{1,2})?\\s*((\\(|\\[)?(?<%s>[a-zA-Z0-9]{3})(\\)|\\]|\\|)?)?" +
                    "(?<%s>[a-zA-Z0-9',\\/\\-\\s]+)\\s*((\\|)(?<%s>[0-9A-Z]+\\S?[A-Z]*))?",
                    REGRP_CARDNO, REGRP_SET, REGRP_CARD, REGRP_COLLNO);
    private static final String CARD_REGEX_NAME_SET =
            String.format("(?<%s>[\\d]{1,2})?\\s*(?<%s>[a-zA-Z0-9',\\/\\-\\s]+)\\s*((\\(|\\[|\\|)" +
                    "(?<%s>[a-zA-Z0-9]{3})(\\)|\\])?)?\\s*(?<%s>[0-9A-Z]+\\S?[A-Z]*)?",
                    REGRP_CARDNO, REGRP_CARD, REGRP_SET, REGRP_COLLNO);

    private static final Pattern CARD_SET_NAME_PATTERN = Pattern.compile(CARD_REGEX_SET_NAME);
    private static final Pattern CARD_NAME_SET_PATTERN = Pattern.compile(CARD_REGEX_NAME_SET);
    // CoreTypes (to recognise Tokens of type CardType
    private static List<String> cardTypes = null;
    // Note: Planes section is not included, as it is checked against "Plainswalker" (see `isDeckSectionName` method)
    private static final CharSequence[] DECK_SECTION_NAMES = {"avatar", "commander", "schemes", "conspiracy",
            "main", "card",
            "side", "sideboard"};

    private CardDb db;
    private CardDb altDb;
    private Date recognizeCardsPrintedBefore = null;
    // This two parameters are controlled only via setter methods
    private List<String> allowedSetCodes = null;  // as imposed by current format
    private DeckFormat deckFormat = null;  //
    
    public DeckRecognizer(CardDb db, CardDb altDb) {
        this.db = db;
        this.altDb = altDb;

        if (cardTypes == null) {
            // CoreTypesNames
            List<CardType.CoreType> coreTypes = Lists.newArrayList(CardType.CoreType.values());
            cardTypes = new ArrayList<String>();
            coreTypes.forEach(new Consumer<CardType.CoreType>() {
                @Override
                public void accept(CardType.CoreType ctype) {
                    cardTypes.add(ctype.name().toLowerCase());
                }
            });
            // Manual Additions:
            // NOTE: "sorceries" is also included as it can be found in exported deck, even if it's incorrect.
            // Example: https://deckstats.net/decks/70852/556925-artifacts/en - see Issue 1010
            cardTypes.add("sorceries");  // Sorcery is the only name with different plural form
            cardTypes.add("aura");  // in case.
            cardTypes.add("mana");  // "Mana" (see Issue 1010)
            cardTypes.add("spell");
            cardTypes.add("other spell");
        }
    }

    public Token recognizeLine(final String rawLine) {
        if (StringUtils.isBlank(rawLine.trim()))
            return null;
        if (StringUtils.startsWith(rawLine.trim(), LINE_COMMENT_DELIMITER)) {
            return new Token(TokenType.Comment, 0, rawLine);
        }
        final char smartQuote = (char) 8217;
        String line = rawLine.trim().replace(smartQuote, '\'');

        // Some websites export split card names with a single slash. Replace with double slash.
        line = SEARCH_SINGLE_SLASH.matcher(line).replaceFirst(" // ");
        line = line.trim();  // Remove any trailing formattings
        if (StringUtils.startsWith(line, DOUBLE_SLASH))
            line = line.substring(2);  // In this case, we are sure to support split cards

        // In some format, cards in the Sideboard have an SB: prefix.
        // We won't support that as it is, due to how the recognition process works, so a section must be
        // specified (e.g. Sideboard or Side) to allow that those cards will be imported in sideboard.
        if (StringUtils.startsWith(line.trim(), "SB:"))
            line = StringUtils.replace(line, "SB:", "").trim();

        Token result = recogniseCardToken(line);
        if (result == null)
            result = recogniseNonCardToken(line, 1);
        return result != null ? result : new Token(TokenType.UnknownText, 0, line);
    }

    private Token recogniseCardToken(final String text) {
        String line = text.trim();
        // TODO: recognize format: http://topdeck.ru/forum/index.php?showtopic=12711
        // @leriomaggio: DONE!
        List<Matcher> cardMatchers = getRegexMatchers(line);
        for (Matcher cardMatcher : cardMatchers) {
            String cardName = cardMatcher.group(REGRP_CARD);
            String ccount = cardMatcher.group(REGRP_CARDNO);
            String setCode = cardMatcher.group(REGRP_SET);
            String collNo = cardMatcher.group(REGRP_COLLNO);
            int cardCount = ccount != null ? Integer.parseInt(ccount) : 1;
            // if any, it will be tried to convert specific collector number to art index (useful for lands).
            String collectorNumber = collNo != null ? collNo : IPaperCard.NO_COLLECTOR_NUMBER;

            //Avoid hit the DB - check whether cardName is contained in the DB
            CardDb.CardRequest cr = CardDb.CardRequest.fromString(cardName.trim());  // to account for any FOIL request
            if (!foundInCardDb(cr.cardName))
                continue;  // skip to the next matcher!
            // Ok Now we're sure the cardName is correct. Now check for setCode
            CardEdition edition = StaticData.instance().getEditions().get(setCode);
            if (edition != null){
                // we now name is ok, set is ok - we just need to be sure about collector number (if any)
                // and if that card can be actually found in the requested set.
                // IOW: we should account for wrong request, e.g. Counterspell|FEM - just doesn't exist!
                PaperCard pc = getCardFromSet(cr.cardName, edition, collectorNumber, cr.isFoil);
                if (pc != null) {
                    // ok so the card has been found - let's see if there's any restriction on the set
                    if (this.allowedSetCodes != null && !this.allowedSetCodes.contains(setCode))
                        // Mark as illegal card
                        return Token.illegalCard(pc.getName(), pc.getEdition(), cardCount);
                    if (this.deckFormat != null && !deckFormat.isLegalCard(pc))
                        return Token.illegalCard(pc.getName(), pc.getEdition(), cardCount);
                    return Token.knownCard(pc, cardCount);
                }
                // UNKNOWN card as in the Counterspell|FEM case
                return Token.unknownCard(cardName, setCode, cardCount);
            }
            // ok so we can simply ignore everything but card name - as set code does not exist
            // At this stage, we know the card name exists in the DB so a Card MUST be found
            // unless it is illegal for current format.
            // In that case, an illegalCard token will be returned!
            PaperCard pc = this.getCardFromSupportedEditions(cr.cardName, cr.isFoil);
            if (pc != null){
                if (this.deckFormat != null && !deckFormat.isLegalCard(pc))
                    return Token.illegalCard(pc.getName(), pc.getEdition(), cardCount);
                return Token.knownCard(pc, cardCount);
            }
            return Token.illegalCard(cardName, "", cardCount);
        }
        return null;
    }

    private boolean foundInCardDb(String cardName){
        return (this.db.contains(cardName.trim()) || this.altDb.contains(cardName.trim()));
    }

    private List<Matcher> getRegexMatchers(String line){
        final Matcher setBeforeNameMatcher = CARD_SET_NAME_PATTERN.matcher(line);
        final Matcher setAfterNameMatcher = CARD_NAME_SET_PATTERN.matcher(line);
        List<Matcher> matchers = new ArrayList<>();
        if (setBeforeNameMatcher.find())
            matchers.add(setAfterNameMatcher);
        if (setAfterNameMatcher.find())
            matchers.add(setBeforeNameMatcher);
        return matchers;

//        if (fullMatchSetAfter.length() == 0 && fullMatchSetBefore.length() == 0)
//            return null;
//        if (fullMatchSetBefore.equals(line))
//            return setBeforeNameMatcher;
//        if (fullMatchSetAfter.equals(line))
//            return setAfterNameMatcher;
//        return fullMatchSetAfter.length() > fullMatchSetBefore.length() ? setAfterNameMatcher : setBeforeNameMatcher;
    }

    private PaperCard getCardFromSet(final String cardName, final CardEdition edition,
                                     final String collectorNumber, final boolean isFoil) {
        PaperCard result = this.db.getCardFromSet(cardName, edition, collectorNumber, isFoil);
        if (result == null)
            result = this.altDb.getCardFromSet(cardName, edition, collectorNumber, isFoil);
        return result;
    }

    private PaperCard getCardFromSupportedEditions(final String cardName, boolean isFoil){
        Predicate<PaperCard> filter = null;
        if (this.allowedSetCodes != null && this.allowedSetCodes.size() > 0)
            filter = (Predicate<PaperCard>) this.db.isLegal(this.allowedSetCodes);
        String reqInfo = CardDb.CardRequest.compose(cardName, isFoil);
        PaperCard result = this.db.getCardFromEditions(reqInfo, filter);
        if (result == null)
            result = this.altDb.getCardFromEditions(reqInfo, filter);
        return result;
    }

    private static Token recogniseNonCardToken(final String text, final int n) {
        if (isDeckSectionName(text)) {
            return Token.deckSection(text.toLowerCase().trim());
        }
        if (isCardType(text)) {
            return new Token(TokenType.CardType, n, text);
        }
        if (isDeckName(text)) {
            String deckName = getDeckName(text);
            return new Token(TokenType.DeckName, n, deckName);
        }
        return null;
    }

    // NOTE: Card types recognition is ONLY used for style formatting in the Import Editor
    // This won't affect the import process of cards in any way !-)
    private static boolean isCardType(final String lineAsIs) {
        final String line = lineAsIs.toLowerCase().trim();
        for (final String cardType : cardTypes) {
            if (line.startsWith(cardType))
                return true;
        }
        return false;
    }

    private static boolean isDeckName(final String lineAsIs) {
        final String line = lineAsIs.trim();
        final Matcher deckNameMatcher = DECK_NAME_PATTERN.matcher(line);
        return (deckNameMatcher.find());
    }

    private static String getDeckName(final String text) {
        String line = text.trim();
        final Matcher deckNamePattern = DECK_NAME_PATTERN.matcher(line);
        if (deckNamePattern.find())
            return deckNamePattern.group(REGRP_DECKNAME);  // Deck name is at match 7
        return line;
    }

    private static boolean isDeckSectionName(final String text) {
        String line = text.toLowerCase().trim();
        if (StringUtils.containsAny(line, DECK_SECTION_NAMES))
            return true;
        return line.contains("planes") && !line.contains("planeswalker");
    }

    public void setDateConstraint(int month, Integer year) {
        Calendar ca = Calendar.getInstance();
        ca.set(year, month, 1);
        recognizeCardsPrintedBefore = ca.getTime();
    }

    public void setCardEditionConstrain(List<String> allowedSetCodes){
        this.allowedSetCodes = allowedSetCodes;
    }

    public void setDeckFormatConstraint(DeckFormat deckFormat0){
        this.deckFormat = deckFormat0;
    }
    
//    private Token recognizePossibleNameAndNumber(final String name, final int n) {
//        PaperCard pc = tryGetCard(name);
//        if (null != pc) {
//            return Token.knownCard(pc, n);
//        }
//
//        // TODO: recognize format: http://topdeck.ru/forum/index.php?showtopic=12711
//        //final Matcher foundEditionName = READ_SEPARATED_EDITION.matcher(name);
//
//        final Token known = DeckRecognizer.recognizeNonCard(name, n);
//        return null == known ? Token.unknownCard(name, n) : known;
//    }
//
//    private static Token recognizeNonCard(final String text, final int n) {
//        if (DeckRecognizer.isDecoration(text)) {
//            return new Token(TokenType.Comment, n, text);
//        }
//        if (DeckRecognizer.isSectionName(text)) {
//            return new Token(TokenType.SectionName, n, text);
//        }
//        return null;
//    }

//    private static final String[] KNOWN_COMMENTS = new String[] { "land", "lands", "creatures", "creature", "spells",
//            "enchantments", "other spells", "artifacts" };
//    private static final String[] KNOWN_COMMENT_PARTS = new String[] { "card" };
//
//    private static boolean isDecoration(final String lineAsIs) {
//        final String line = lineAsIs.toLowerCase();
//        for (final String s : DeckRecognizer.KNOWN_COMMENT_PARTS) {
//            if (line.contains(s)) {
//                return true;
//            }
//        }
//        for (final String s : DeckRecognizer.KNOWN_COMMENTS) {
//            if (line.equalsIgnoreCase(s)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private static boolean isSectionName(final String line) {
//        if (line.toLowerCase().contains("side")) {
//            return true;
//        }
//        if (line.toLowerCase().contains("main")) {
//            return true;
//        }
//        if (line.toLowerCase().contains("commander")) {
//            return true;
//        }
//        if (line.toLowerCase().contains("planes")) {
//            return true;
//        }
//        if (line.toLowerCase().contains("schemes")) {
//            return true;
//        }
//        if (line.toLowerCase().contains("vanguard")) {
//            return true;
//        }
//        return false;
//    }
//
//    public void setDateConstraint(int month, Integer year) {
//        Calendar ca = Calendar.getInstance();
//        ca.set(year, month, 1);
//        recognizeCardsPrintedBefore = ca.getTime();
//    }
}
