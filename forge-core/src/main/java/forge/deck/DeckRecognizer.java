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
        LEGAL_CARD_REQUEST,
        ILLEGAL_CARD_REQUEST,
        INVALID_CARD_REQUEST,
        UNKNOWN_CARD_REQUEST,
        DECK_NAME,
        DECK_SECTION_NAME,
        COMMENT,
        UNKNOWN_TEXT,
        CARD_TYPE
    }

    /**
     * The Class Token.
     */
    public static class Token {
        private final TokenType type;
        private final PaperCard card;
        private final int number;
        private final String text;

        public static Token KnownCard(final PaperCard theCard, final int count) {
            return new Token(theCard, TokenType.LEGAL_CARD_REQUEST, count, null);
        }

        public static Token IllegalCard(final String cardName, final String setCode, final int count) {
            String ttext = setCode == null || setCode.equals("") ? cardName :
                           String.format("%s [%s]", cardName, setCode);
            return new Token(null, TokenType.ILLEGAL_CARD_REQUEST, count, ttext);
        }

        public static Token InvalidCard(final String cardName, final String setCode, final int count) {
            String ttext = setCode == null || setCode.equals("") ? cardName :
                    String.format("%s [%s]", cardName, setCode);
            return new Token(null, TokenType.INVALID_CARD_REQUEST, count, ttext);
        }

        public static Token UnknownCard(final String cardName, final String setCode, final int count) {
            String ttext = setCode == null || setCode.equals("") ? cardName :
                    String.format("%s [%s]", cardName, setCode);
            return new Token(null, TokenType.UNKNOWN_CARD_REQUEST, count, ttext);
        }

        public static Token DeckSection(final String sectionName){
            if (sectionName.equals("avatar"))
                return new Token(TokenType.DECK_SECTION_NAME, DeckSection.Avatar.name());
            if (sectionName.equals("commander"))
                return new Token(TokenType.DECK_SECTION_NAME, DeckSection.Commander.name());
            if (sectionName.equals("schemes"))
                return new Token(TokenType.DECK_SECTION_NAME, DeckSection.Schemes.name());
            if (sectionName.equals("conspiracy"))
                return new Token(TokenType.DECK_SECTION_NAME, DeckSection.Conspiracy.name());
            if (sectionName.equals("side") || sectionName.contains("sideboard"))
                return new Token(TokenType.DECK_SECTION_NAME, DeckSection.Sideboard.name());
            if (sectionName.equals("main") || sectionName.contains("card")
                    || sectionName.equals("mainboard") || sectionName.equals("deck"))
                return new Token(TokenType.DECK_SECTION_NAME, DeckSection.Main.name());
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
            if ((type1 == TokenType.LEGAL_CARD_REQUEST) || (type1 == TokenType.UNKNOWN_CARD_REQUEST)) {
                throw new IllegalArgumentException("Use factory methods for recognized " + REGRP_CARD + " lines");
            }
        }

        public Token(final TokenType type1, final String message) {
            this(type1, 0, message);
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

    // Utility Constants
    private static final Pattern SEARCH_SINGLE_SLASH = Pattern.compile("(?<=[^/])\\s*/\\s*(?=[^/])");
    private static final String DOUBLE_SLASH = "//";
    private static final String LINE_COMMENT_DELIMITER_OR_MD_HEADER = "#";
    private static final String ASTERISK = "* ";  // Note the blank space after asterisk!

//    private static final Pattern EDITION_AFTER_CARD_NAME = Pattern.compile("([\\d]{1,2})?\\s*([a-zA-Z',\\/\\-\\s]+)\\s*((\\(|\\[)([a-zA-Z0-9]{3})(\\)|\\])\\s*)?([\\d]{1,3})?");

    // Core Matching Patterns (initialised in Constructor)
    public static final String REGRP_DECKNAME = "deckName";
    public static final String REX_DECK_NAME =
            String.format("^(//\\s*)?(?<pre>(deck|name))(\\:|\\s)\\s*(?<%s>[a-zA-Z0-9',\\/\\-\\s]+)\\s*(.*)$",
                    REGRP_DECKNAME);
    public static final Pattern DECK_NAME_PATTERN = Pattern.compile(REX_DECK_NAME, Pattern.CASE_INSENSITIVE);

    public static final String REGRP_NOCARD = "token";
    public static final String REX_NOCARD = String.format("^(?<pre>[^a-zA-Z]*)\\s*(?<title>(\\w+[:]\\s*))?(?<%s>[a-zA-Z]+)(?<post>[^a-zA-Z]*)?$", REGRP_NOCARD);
    public static final Pattern NONCARD_PATTERN = Pattern.compile(REX_NOCARD, Pattern.CASE_INSENSITIVE);

    public static final String REGRP_SET = "setcode";
    public static final String REGRP_COLLNR = "collnr";
    public static final String REGRP_CARD = "cardname";
    public static final String REGRP_CARDNO = "count";

    public static final String REX_CARD_NAME = String.format("(\\[)?(?<%s>[a-zA-Z0-9',\\.:!\\+\\\"\\/\\-\\s]+)(\\])?", REGRP_CARD);
    public static final String REX_SET_CODE = String.format("(?<%s>[a-zA-Z0-9_]{2,7})", REGRP_SET);
    public static final String REX_COLL_NUMBER = String.format("(?<%s>\\*?[0-9A-Z]+\\S?[A-Z]*)", REGRP_COLLNR);
    public static final String REX_CARD_COUNT = String.format("(?<%s>[\\d]{1,2})(?<mult>x)?", REGRP_CARDNO);

    // EXTRA
    public static final String REGRP_FOIL_GFISH = "foil";
    private static final String REX_FOIL_MTGGOLDFISH = String.format(
            "(?<%s>\\(F\\))?", REGRP_FOIL_GFISH);

    // 1. Card-Set Request (Amount?, CardName, Set)
    public static final String REX_CARD_SET_REQUEST = String.format(
            "(%s\\s)?\\s*%s\\s*(\\s|\\||\\(|\\[|\\{)%s(\\s|\\)|\\]|\\})?\\s*%s",
            REX_CARD_COUNT, REX_CARD_NAME, REX_SET_CODE, REX_FOIL_MTGGOLDFISH);
    public static final Pattern CARD_SET_PATTERN = Pattern.compile(REX_CARD_SET_REQUEST);

    // 2. Set-Card Request (Amount?, Set, CardName)
    public static final String REX_SET_CARD_REQUEST = String.format(
            "(%s\\s)?\\s*(\\(|\\[|\\{)?%s(\\s+|\\)|\\]|\\}|\\|)\\s*%s\\s*%s\\s*",
            REX_CARD_COUNT, REX_SET_CODE, REX_CARD_NAME, REX_FOIL_MTGGOLDFISH);
    public static final Pattern SET_CARD_PATTERN = Pattern.compile(REX_SET_CARD_REQUEST);

    // 3. Full-Request (Amount?, CardName, Set, Collector Number|Art Index) - MTGArena Format
    public static final String REX_FULL_REQUEST_CARD_SET = String.format(
            "(%s\\s)?\\s*%s\\s*(\\||\\(|\\[|\\{|\\s)%s(\\s|\\)|\\]|\\})?\\s+%s\\s*%s\\s*",
            REX_CARD_COUNT, REX_CARD_NAME, REX_SET_CODE, REX_COLL_NUMBER, REX_FOIL_MTGGOLDFISH);
    public static final Pattern CARD_SET_COLLNO_PATTERN = Pattern.compile(REX_FULL_REQUEST_CARD_SET);

    // 4. Full-Request (Amount?, Set, CardName, Collector Number|Art Index) - Alternative for flexibility
    public static final String REX_FULL_REQUEST_SET_CARD = String.format(
            "^(%s\\s)?\\s*(\\(|\\[|\\{)?%s(\\s+|\\)|\\]|\\}|\\|)\\s*%s\\s+%s\\s*%s$",
            REX_CARD_COUNT, REX_SET_CODE, REX_CARD_NAME, REX_COLL_NUMBER, REX_FOIL_MTGGOLDFISH);
    public static final Pattern SET_CARD_COLLNO_PATTERN = Pattern.compile(REX_FULL_REQUEST_SET_CARD);

    // 5. (MTGGoldfish mostly) (Amount?, Card Name, <Collector Number>, Set)
    public static final String REX_FULL_REQUEST_CARD_COLLNO_SET = String.format(
            "^(%s\\s)?\\s*%s\\s+(\\<%s\\>)\\s*(\\(|\\[|\\{)?%s(\\s+|\\)|\\]|\\}|\\|)\\s*%s$",
            REX_CARD_COUNT, REX_CARD_NAME, REX_COLL_NUMBER, REX_SET_CODE, REX_FOIL_MTGGOLDFISH);
    public static final Pattern CARD_COLLNO_SET_PATTERN = Pattern.compile(REX_FULL_REQUEST_CARD_COLLNO_SET);

    // 6. Card-Only Request (Amount?)
    public static final String REX_CARDONLY = String.format(
            "(%s\\s)?\\s*%s\\s*%s", REX_CARD_COUNT, REX_CARD_NAME, REX_FOIL_MTGGOLDFISH);
    public static final Pattern CARD_ONLY_PATTERN = Pattern.compile(REX_CARDONLY);


    // CoreTypes (to recognise Tokens of type CardType
    private static CharSequence[] CARD_TYPES = allCardTypes();

    private static final CharSequence[] DECK_SECTION_NAMES = {"avatar", "commander",
            "schemes", "conspiracy", "planes", "deck",
            "main", "card", "mainboard", "side", "sideboard"};

    private final CardDb db;
    private final CardDb altDb;
    private Date releaseDateConstraint = null;
    // This two parameters are controlled only via setter methods
    private List<String> allowedSetCodes = null;  // as imposed by current format
    private DeckFormat deckFormat = null;  //
    
    public DeckRecognizer(CardDb db, CardDb altDb) {
        this.db = db;
        this.altDb = altDb;
    }

    public Token recognizeLine(final String rawLine) {
        if (rawLine == null)
            return null;
        if (StringUtils.isBlank(rawLine.trim()))
            return null;

        final char smartQuote = (char) 8217;
        String line = rawLine.trim().replace(smartQuote, '\'');
        // Remove any link (e.g. Markdown Export format from TappedOut)
        line = purgeAllLinks(line);

        if (StringUtils.startsWith(line, LINE_COMMENT_DELIMITER_OR_MD_HEADER))
            line = line.replaceAll(LINE_COMMENT_DELIMITER_OR_MD_HEADER, "");

        // Some websites export split card names with a single slash. Replace with double slash.
        line = SEARCH_SINGLE_SLASH.matcher(line).replaceFirst(" // ");
        line = line.trim();  // Remove any trailing formattings
        if (StringUtils.startsWith(line, DOUBLE_SLASH))
            line = line.substring(2);  // In this case, we are sure to support split cards

        if (StringUtils.startsWith(line, ASTERISK))
            line = line.substring(2);

        // In some format, cards in the Sideboard have an SB: prefix.
        // We won't support that as it is, due to how the recognition process works, so a section must be
        // specified (e.g. Sideboard or Side) to allow that those cards will be imported in sideboard.
        if (StringUtils.startsWith(line.trim(), "SB:"))
            line = StringUtils.replace(line, "SB:", "").trim();

        Token result = recogniseCardToken(line);
        if (result == null)
            result = recogniseNonCardToken(line);
        return result != null ? result : new Token(TokenType.UNKNOWN_TEXT, 0, line);
    }

    public static String purgeAllLinks(String line){
        String urlPattern = "(?<protocol>((https|ftp|file|http):))(?<sep>((//|\\\\)+))(?<url>([\\w\\d:#@%/;$~_?\\+-=\\\\\\.&]*))";
        Pattern p = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(line);

        while (m.find()) {
            line = line.replaceAll(m.group(), "").trim();
        }
        if (StringUtils.endsWith(line, "()"))
            return line.substring(0, line.length()-2);
        return line;
    }

    public Token recogniseCardToken(final String text) {
        String line = text.trim();
        Token uknonwnCardToken = null;

        // TODO: recognize format: http://topdeck.ru/forum/index.php?showtopic=12711
        // @leriomaggio: DONE!
        List<Matcher> cardMatchers = getRegExMatchers(line);
        for (Matcher matcher : cardMatchers) {
            String cardName = getRexGroup(matcher, REGRP_CARD);
            if (cardName == null)
                continue;
            cardName = cardName.trim();
            //Avoid hit the DB - check whether cardName is contained in the DB
            CardDb.CardRequest cr = CardDb.CardRequest.fromString(cardName.trim());  // to account for any FOIL request
            if (!foundInCardDb(cr.cardName))
                continue;  // skip to the next matcher!
            String ccount = getRexGroup(matcher, REGRP_CARDNO);
            String setCode = getRexGroup(matcher, REGRP_SET);
            String collNo = getRexGroup(matcher, REGRP_COLLNR);
            String foilGr = getRexGroup(matcher, REGRP_FOIL_GFISH);
            if (foilGr != null)
                cr.isFoil = true;
            int cardCount = ccount != null ? Integer.parseInt(ccount) : 1;
            // if any, it will be tried to convert specific collector number to art index (useful for lands).
            String collectorNumber = collNo != null ? collNo : IPaperCard.NO_COLLECTOR_NUMBER;
            int artIndex;
            try {
                artIndex = Integer.parseInt(collectorNumber);
            } catch (NumberFormatException ex){
                artIndex = IPaperCard.NO_ART_INDEX;
            }

            if (setCode != null) {
                // Ok Now we're sure the cardName is correct. Now check for setCode
                CardEdition edition = StaticData.instance().getEditions().get(setCode);
                if (edition == null) {
                    // set the case for unknown card (in case) and continue to the next for any better matching
                    uknonwnCardToken = Token.UnknownCard(cardName, setCode, cardCount);
                    continue;
                }
                if (isNotCompliantWithReleaseDateRestrictions(edition))
                    return Token.InvalidCard(cr.cardName, edition.getCode(), cardCount);

                // we now name is ok, set is ok - we just need to be sure about collector number (if any)
                // and if that card can be actually found in the requested set.
                // IOW: we should account for wrong request, e.g. Counterspell|FEM - just doesn't exist!
                PaperCard pc = this.getCardFromSet(cr.cardName, edition, collectorNumber, artIndex, cr.isFoil);
                if (pc != null) {
                    // ok so the card has been found - let's see if there's any restriction on the set
                    if (isIllegalSetInGameFormat(edition.getCode()) || isIllegalCardInDeckFormat(pc))
                        // Mark as illegal card
                        return Token.IllegalCard(pc.getName(), pc.getEdition(), cardCount);
                    return Token.KnownCard(pc, cardCount);
                }
                // UNKNOWN card as in the Counterspell|FEM case
                return Token.UnknownCard(cardName, setCode, cardCount);
            }
            // ok so we can simply ignore everything but card name - as set code does not exist
            // At this stage, we know the card name exists in the DB so a Card MUST be found
            // unless it is illegal for current format.
            // In that case, an illegalCard token will be returned!
            PaperCard pc = this.getCardFromSupportedEditions(cr.cardName, cr.isFoil);
            if (pc != null){
                if (isIllegalCardInDeckFormat(pc))
                    return Token.IllegalCard(pc.getName(), pc.getEdition(), cardCount);
                return Token.KnownCard(pc, cardCount);
            }
            return Token.IllegalCard(cardName, "", cardCount);
        }
        return uknonwnCardToken;  // either null or unknown card
    }

    private String getRexGroup(Matcher matcher, String groupName){
        String rexGroup;
        try{
            rexGroup = matcher.group(groupName);
        } catch (IllegalArgumentException ex) {
            rexGroup = null;
        }
        return rexGroup;
    }

    private boolean isIllegalCardInDeckFormat(PaperCard pc) {
        return this.deckFormat != null && !deckFormat.isLegalCard(pc);
    }

    private boolean isIllegalSetInGameFormat(String setCode) {
        return this.allowedSetCodes != null && !this.allowedSetCodes.contains(setCode);
    }

    private boolean isNotCompliantWithReleaseDateRestrictions(CardEdition edition){
        return this.releaseDateConstraint != null && edition.getDate().compareTo(this.releaseDateConstraint) >= 0;
    }

    private boolean foundInCardDb(String cardName){
        return this.db.contains(cardName) || this.altDb.contains(cardName);
    }

    private List<Matcher> getRegExMatchers(String line) {
        List<Matcher> matchers = new ArrayList<>();
        Pattern[] patternsWithCollNumber = new Pattern[] {
                CARD_SET_COLLNO_PATTERN,
                SET_CARD_COLLNO_PATTERN,
                CARD_COLLNO_SET_PATTERN
        };
        for (Pattern pattern : patternsWithCollNumber) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches() && getRexGroup(matcher, REGRP_SET) != null &&
                    getRexGroup(matcher, REGRP_COLLNR) != null)
                matchers.add(matcher);
        }
        Pattern[] OtherPatterns = new Pattern[] {  // Order counts
                CARD_SET_PATTERN,
                SET_CARD_PATTERN,
                CARD_ONLY_PATTERN
        };
        for (Pattern pattern : OtherPatterns) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches())
                matchers.add(matcher);
        }
        return matchers;
    }

    private PaperCard getCardFromSet(final String cardName, final CardEdition edition,
                                     final String collectorNumber, final int artIndex,
                                     final boolean isFoil) {
        CardDb targetDb = this.db.contains(cardName) ? this.db : this.altDb;
        // Try with collector number first
        PaperCard result = targetDb.getCardFromSet(cardName, edition, collectorNumber, isFoil);
        if (result == null && !collectorNumber.equals(IPaperCard.NO_COLLECTOR_NUMBER)) {
            if (artIndex != IPaperCard.NO_ART_INDEX) {
                // So here we know cardName exists (checked before invoking this method)
                // and also a Collector Number was specified.
                // The only case we would reach this point is either due to a wrong edition-card match
                // (later resulting in Unknown card - e.g. "Counterspell|FEM") or due to the fact that
                // art Index was specified instead of collector number! Let's give it a go with that
                // but only if artIndex is not NO_ART_INDEX (e.g. collectorNumber = "*32")
                int maxArtForCard = targetDb.getMaxArtIndex(cardName);
                if (artIndex <= maxArtForCard) {
                    // if collNr was "78", it's hardly an artIndex. It was just the wrong collNr for the requested card
                    result = targetDb.getCardFromSet(cardName, edition, artIndex, isFoil);
                }
            }
            if (result == null){
                // Last chance, try without collector number and see if any match is found
                result = targetDb.getCardFromSet(cardName, edition, isFoil);
            }
        }
        return result;
    }

    private PaperCard getCardFromSupportedEditions(final String cardName, boolean isFoil){
        Predicate<PaperCard> filter = null;
        if (this.allowedSetCodes != null && this.allowedSetCodes.size() > 0)
            filter = (Predicate<PaperCard>) this.db.isLegal(this.allowedSetCodes);
        String reqInfo = CardDb.CardRequest.compose(cardName, isFoil);
        PaperCard result;
        if (this.releaseDateConstraint != null){
            result = this.db.getCardFromEditionsReleasedBefore(reqInfo,
                    this.releaseDateConstraint, filter);
            if (result == null)
                result = this.altDb.getCardFromEditionsReleasedBefore(reqInfo,
                        this.releaseDateConstraint, filter);
        }
        else {
            result = this.db.getCardFromEditions(reqInfo, filter);
            if (result == null)
                result = this.altDb.getCardFromEditions(reqInfo, filter);
        }
        return result;
    }

    public Token recogniseNonCardToken(final String text) {
        if (isDeckSectionName(text)) {
            String tokenText = getNonCardTokenText(text.toLowerCase().trim());
            return Token.DeckSection(tokenText);
        }
        if (isCardType(text)) {
            String tokenText = getNonCardTokenText(text);
            return new Token(TokenType.CARD_TYPE, tokenText);
        }
        if (isDeckName(text)) {
            String deckName = getDeckName(text);
            return new Token(TokenType.DECK_NAME, deckName);
        }
        return null;
    }

    private static String getNonCardTokenText(final String line){
        Matcher noncardMatcher = NONCARD_PATTERN.matcher(line);
        if (!noncardMatcher.matches())
            return "";
        return noncardMatcher.group(REGRP_NOCARD);
    }

    private static CharSequence[] allCardTypes(){
        List<String> cardTypesList = new ArrayList<>();
        // CoreTypesNames
        List<CardType.CoreType> coreTypes = Lists.newArrayList(CardType.CoreType.values());
        for (CardType.CoreType coreType : coreTypes)
            cardTypesList.add(coreType.name().toLowerCase());
        // Manual Additions:
        // NOTE: "sorceries" is also included as it can be found in exported deck, even if it's incorrect.
        // Example: https://deckstats.net/decks/70852/556925-artifacts/en - see Issue 1010
        cardTypesList.add("sorceries");  // Sorcery is the only name with different plural form
        cardTypesList.add("aura");  // in case.
        cardTypesList.add("mana");  // "Mana" (see Issue 1010)
        cardTypesList.add("spell");
        cardTypesList.add("other spell");
        cardTypesList.add("planeswalker");
        return cardTypesList.toArray(new CharSequence[cardTypesList.size()]);
    }

    // NOTE: Card types recognition is ONLY used for style formatting in the Import Editor
    // This won't affect the import process of cards in any way !-)
    public static boolean isCardType(final String lineAsIs) {
        if (lineAsIs == null)
            return false;
        String line = lineAsIs.toLowerCase().trim();
        Matcher noncardMatcher = NONCARD_PATTERN.matcher(line);
        if (!noncardMatcher.matches())
            return false;
        String nonCardToken = noncardMatcher.group(REGRP_NOCARD);
        return StringUtils.containsAny(nonCardToken, CARD_TYPES);
    }

    public static boolean isDeckName(final String lineAsIs) {
        if (lineAsIs == null)
            return false;
        final String line = lineAsIs.trim();
        final Matcher deckNameMatcher = DECK_NAME_PATTERN.matcher(line);
        boolean matches = deckNameMatcher.matches();
        return matches;
    }

    public static String getDeckName(final String text) {
        if (text == null)
            return "";
        String line = text.trim();
        final Matcher deckNamePattern = DECK_NAME_PATTERN.matcher(line);
        if (deckNamePattern.matches())
            return deckNamePattern.group(REGRP_DECKNAME);  // Deck name is at match 7
        return "";
    }

    public static boolean isDeckSectionName(final String text) {
        if (text == null)
            return false;
        String line = text.toLowerCase().trim();
        Matcher noncardMatcher = NONCARD_PATTERN.matcher(line);
        if (!noncardMatcher.matches())
            return false;
        String nonCardToken = noncardMatcher.group(REGRP_NOCARD);
        return StringUtils.equalsAnyIgnoreCase(nonCardToken, DECK_SECTION_NAMES);
    }

    public void setDateConstraint(int year, int month) {
        Calendar ca = Calendar.getInstance();
        ca.set(year, month, 1);
        releaseDateConstraint = ca.getTime();
    }

    public void setGameFormatConstraint(List<String> allowedSetCodes){
        this.allowedSetCodes = allowedSetCodes;
    }

    public void setDeckFormatConstraint(DeckFormat deckFormat0){
        this.deckFormat = deckFormat0;
    }
}
