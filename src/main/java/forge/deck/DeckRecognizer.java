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
            card = knownCard;
            number = count;
            type = type1;
            text = message;
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
            if (type1 == TokenType.KnownCard || type1 == TokenType.UnknownCard) {
                throw new IllegalArgumentException("Use factory methods for recognized card lines");
            }
        }

        /**
         * Gets the text.
         * 
         * @return the text
         */
        public final String getText() {
            return text;
        }

        /**
         * Gets the card.
         * 
         * @return the card
         */
        public final CardPrinted getCard() {
            return card;
        }

        /**
         * Gets the type.
         * 
         * @return the type
         */
        public final TokenType getType() {
            return type;
        }

        /**
         * Gets the number.
         * 
         * @return the number
         */
        public final int getNumber() {
            return number;
        }
    }

    // Let's think about it numbers in the back later
    // private static final Pattern searchNumbersInBack =
    // Pattern.compile("(.*)[^A-Za-wyz]*\\s+([\\d]{1,2})");
    private static final Pattern searchNumbersInFront = Pattern.compile("([\\d]{1,2})[^A-Za-wyz]*\\s+(.*)");

    /**
     * Recognize line.
     * 
     * @param raw_line
     *            the raw_line
     * @return the token
     */
    public static Token recognizeLine(final String raw_line) {
        if (StringUtils.isBlank(raw_line)) {
            return new Token(TokenType.Comment, 0, raw_line);
        }
        String line = raw_line.trim();

        Token result = null;
        Matcher foundNumbersInFront = searchNumbersInFront.matcher(line);
        // Matcher foundNumbersInBack = searchNumbersInBack.matcher(line);
        if (foundNumbersInFront.matches()) {
            String cardName = foundNumbersInFront.group(2);
            int amount = Integer.parseInt(foundNumbersInFront.group(1));
            result = recognizePossibleNameAndNumber(cardName, amount);
        } /*
           * else if (foundNumbersInBack.matches()) { String cardName =
           * foundNumbersInBack.group(1); int amount =
           * Integer.parseInt(foundNumbersInBack.group(2)); return new
           * Token(cardName, amount); }
           */
        else {
            if (CardDb.instance().isCardSupported(line)) {
                return Token.knownCard(CardDb.instance().getCard(line), 1);
            }
            result = recognizeNonCard(line, 1);
        }
        return result != null ? result : new Token(TokenType.UnknownText, 0, line);
    }

    private static Token recognizePossibleNameAndNumber(final String name, final int n) {
        if (CardDb.instance().isCardSupported(name)) {
            return Token.knownCard(CardDb.instance().getCard(name), n);
        }

        Token known = recognizeNonCard(name, n);
        return null == known ? Token.unknownCard(name, n) : known;
    }

    private static Token recognizeNonCard(final String text, final int n) {
        if (isDecoration(text)) {
            return new Token(TokenType.Comment, n, text);
        }
        if (isSectionName(text)) {
            return new Token(TokenType.SectionName, n, text);
        }
        return null;
    }

    private static final String[] knownComments = new String[] { "land", "lands", "creatures", "creature", "spells",
            "enchancements", "other spells", "artifacts" };
    private static final String[] knownCommentParts = new String[] { "card" };

    private static boolean isDecoration(final String lineAsIs) {
        String line = lineAsIs.toLowerCase();
        for (String s : knownCommentParts) {
            if (line.contains(s)) {
                return true;
            }
        }
        for (String s : knownComments) {
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
