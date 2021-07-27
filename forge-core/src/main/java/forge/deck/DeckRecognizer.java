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

import forge.card.CardDb;
import forge.card.CardDb.CardArtPreference;
import forge.card.ICardDatabase;
import forge.item.PaperCard;
import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;
import java.util.Date;
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
        SectionName,
        Comment,
        UnknownText
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

        public static Token unknownCard(final String cardNme, final int count) {
            return new Token(null, TokenType.UnknownCard, count, cardNme);
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
                throw new IllegalArgumentException("Use factory methods for recognized card lines");
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

    // Let's think about it numbers in the back later
    // private static final Pattern searchNumbersInBack =
    // Pattern.compile("(.*)[^A-Za-wyz]*\\s+([\\d]{1,2})");
    private static final Pattern SEARCH_NUMBERS_IN_FRONT = Pattern.compile("([\\d]{1,2})[^A-Za-wyz]*\\s+(.*)");
    //private static final Pattern READ_SEPARATED_EDITION = Pattern.compile("[[\\(\\{]([a-zA-Z0-9]){1,3})[]*\\s+(.*)");
    private static final Pattern SEARCH_SINGLE_SLASH = Pattern.compile("(?<=[^/])\\s*/\\s*(?=[^/])");

    private final CardArtPreference useLastSet;
    private final ICardDatabase db;
    private Date recognizeCardsPrintedBefore = null;
    
    public DeckRecognizer(boolean fromLatestSet, boolean onlyCoreAndExp, CardDb db) {
        if (!fromLatestSet) {
            useLastSet = null;
        }
        else if (onlyCoreAndExp) {
            useLastSet = CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        }
        else {
            useLastSet = CardArtPreference.LATEST_ART_ALL_EDITIONS;
        }
        this.db = db;
    }
    
    public Token recognizeLine(final String rawLine) {
        if (StringUtils.isBlank(rawLine)) {
            return new Token(TokenType.Comment, 0, rawLine);
        }
        final char smartQuote = (char) 8217;
        String line = rawLine.trim().replace(smartQuote, '\'');

        // Some websites export split card names with a single slash. Replace with double slash.
        line = SEARCH_SINGLE_SLASH.matcher(line).replaceFirst(" // ");

        Token result = null;
        final Matcher foundNumbersInFront = DeckRecognizer.SEARCH_NUMBERS_IN_FRONT.matcher(line);
        // Matcher foundNumbersInBack = searchNumbersInBack.matcher(line);
        if (foundNumbersInFront.matches()) {
            final String cardName = foundNumbersInFront.group(2);
            final int amount = Integer.parseInt(foundNumbersInFront.group(1));
            result = recognizePossibleNameAndNumber(cardName, amount);
        } /*
           * else if (foundNumbersInBack.matches()) { String cardName =
           * foundNumbersInBack.group(1); int amount =
           * Integer.parseInt(foundNumbersInBack.group(2)); return new
           * Token(cardName, amount); }
           */
        else {
            PaperCard pc = tryGetCard(line);
            if (null != pc) {
                return Token.knownCard(pc, 1);
            }
            result = DeckRecognizer.recognizeNonCard(line, 1);
        }
        return result != null ? result : new Token(TokenType.UnknownText, 0, line);
    }

    private PaperCard tryGetCard(String text) {
        return db.getCardFromEditionsReleasedBefore(text, useLastSet, recognizeCardsPrintedBefore);
    }
    
    private Token recognizePossibleNameAndNumber(final String name, final int n) {
        PaperCard pc = tryGetCard(name);
        if (null != pc) {
            return Token.knownCard(pc, n);
        }

        // TODO: recognize format: http://topdeck.ru/forum/index.php?showtopic=12711
        //final Matcher foundEditionName = READ_SEPARATED_EDITION.matcher(name);

        final Token known = DeckRecognizer.recognizeNonCard(name, n);
        return null == known ? Token.unknownCard(name, n) : known;
    }

    private static Token recognizeNonCard(final String text, final int n) {
        if (DeckRecognizer.isDecoration(text)) {
            return new Token(TokenType.Comment, n, text);
        }
        if (DeckRecognizer.isSectionName(text)) {
            return new Token(TokenType.SectionName, n, text);
        }
        return null;
    }

    private static final String[] KNOWN_COMMENTS = new String[] { "land", "lands", "creatures", "creature", "spells",
            "enchantments", "other spells", "artifacts" };
    private static final String[] KNOWN_COMMENT_PARTS = new String[] { "card" };

    private static boolean isDecoration(final String lineAsIs) {
        final String line = lineAsIs.toLowerCase();
        for (final String s : DeckRecognizer.KNOWN_COMMENT_PARTS) {
            if (line.contains(s)) {
                return true;
            }
        }
        for (final String s : DeckRecognizer.KNOWN_COMMENTS) {
            if (line.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSectionName(final String line) {
        if (line.toLowerCase().contains("side")) {
            return true;
        }
        if (line.toLowerCase().contains("main")) {
            return true;
        }
        if (line.toLowerCase().contains("commander")) {
            return true;
        }
        if (line.toLowerCase().contains("planes")) {
            return true;
        }
        if (line.toLowerCase().contains("schemes")) {
            return true;
        }
        if (line.toLowerCase().contains("vanguard")) {
            return true;
        }
        return false;
    }

    public void setDateConstraint(int month, Integer year) {
        Calendar ca = Calendar.getInstance();
        ca.set(year, month, 1);
        recognizeCardsPrintedBefore = ca.getTime();
    }
}
