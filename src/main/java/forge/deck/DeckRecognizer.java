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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import forge.item.CardDb;
import forge.item.CardPrinted;

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

        /** The Known card. */
        KnownCard,

        /** The Unknown card. */
        UnknownCard,

        /** The Section name. */
        SectionName,

        /** The Comment. */
        Comment,

        /** The Unknown text. */
        UnknownText
    }

    /**
     * The Class Token.
     */
    public static class Token {
        private final TokenType type;
        private final CardPrinted card;
        private final int number;
        private final String text;

        /**
         * Known card.
         * 
         * @param theCard
         *            the the card
         * @param count
         *            the count
         * @return the token
         */
        public static Token knownCard(final CardPrinted theCard, final int count) {
            return new Token(theCard, TokenType.KnownCard, count, null);
        }

        /**
         * Unknown card.
         * 
         * @param cardNme
         *            the card nme
         * @param count
         *            the count
         * @return the token
         */
        public static Token unknownCard(final String cardNme, final int count) {
            return new Token(null, TokenType.UnknownCard, count, cardNme);
        }

        private Token(final CardPrinted knownCard, final TokenType type1, final int count, final String message) {
            this.card = knownCard;
            this.number = count;
            this.type = type1;
            this.text = message;
        }

        /**
         * Instantiates a new token.
         * 
         * @param type1
         *            the type1
         * @param count
         *            the count
         * @param message
         *            the message
         */
        public Token(final TokenType type1, final int count, final String message) {
            this(null, type1, count, message);
            if ((type1 == TokenType.KnownCard) || (type1 == TokenType.UnknownCard)) {
                throw new IllegalArgumentException("Use factory methods for recognized card lines");
            }
        }

        /**
         * Gets the text.
         * 
         * @return the text
         */
        public final String getText() {
            return this.text;
        }

        /**
         * Gets the card.
         * 
         * @return the card
         */
        public final CardPrinted getCard() {
            return this.card;
        }

        /**
         * Gets the type.
         * 
         * @return the type
         */
        public final TokenType getType() {
            return this.type;
        }

        /**
         * Gets the number.
         * 
         * @return the number
         */
        public final int getNumber() {
            return this.number;
        }
    }

    // Let's think about it numbers in the back later
    // private static final Pattern searchNumbersInBack =
    // Pattern.compile("(.*)[^A-Za-wyz]*\\s+([\\d]{1,2})");
    private static final Pattern SEARCH_NUMBERS_IN_FRONT = Pattern.compile("([\\d]{1,2})[^A-Za-wyz]*\\s+(.*)");
    //private static final Pattern READ_SEPARATED_EDITION = Pattern.compile("[[\\(\\{]([a-zA-Z0-9]){1,3})[]*\\s+(.*)");

    /**
     * Recognize line.
     * 
     * @param rawLine
     *            the raw_line
     * @param newestEdition
     *            get the newest available edition?
     *
     * @return the token
     */
    public static Token recognizeLine(final String rawLine, final boolean newestEdition) {
        if (StringUtils.isBlank(rawLine)) {
            return new Token(TokenType.Comment, 0, rawLine);
        }
        final String line = rawLine.trim();

        Token result = null;
        final Matcher foundNumbersInFront = DeckRecognizer.SEARCH_NUMBERS_IN_FRONT.matcher(line);
        // Matcher foundNumbersInBack = searchNumbersInBack.matcher(line);
        if (foundNumbersInFront.matches()) {
            final String cardName = foundNumbersInFront.group(2);
            final int amount = Integer.parseInt(foundNumbersInFront.group(1));
            result = DeckRecognizer.recognizePossibleNameAndNumber(cardName, amount, newestEdition);
        } /*
           * else if (foundNumbersInBack.matches()) { String cardName =
           * foundNumbersInBack.group(1); int amount =
           * Integer.parseInt(foundNumbersInBack.group(2)); return new
           * Token(cardName, amount); }
           */
        else {
            if (CardDb.instance().isCardSupported(line)) {
                return Token.knownCard(CardDb.instance().getCard(line, newestEdition), 1);
            }
            result = DeckRecognizer.recognizeNonCard(line, 1);
        }
        return result != null ? result : new Token(TokenType.UnknownText, 0, line);
    }

    private static Token recognizePossibleNameAndNumber(final String name, final int n, final boolean newestEdition) {
        if (CardDb.instance().isCardSupported(name)) {
            return Token.knownCard(CardDb.instance().getCard(name, newestEdition), n);
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
            "enchancements", "other spells", "artifacts" };
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
        return false;
    }

}
