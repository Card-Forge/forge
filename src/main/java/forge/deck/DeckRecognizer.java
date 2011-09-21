package forge.deck;

import java.util.Map.Entry;

import forge.item.CardPrinted;

/** 
 * <p>DeckRecognizer class.</p>
 *
 * @author Forge
 * @version $Id: DeckRecognizer.java 10499 2011-09-17 15:08:47Z Max mtg $
 *
 */
public class DeckRecognizer {
    public enum TokenType {
        KnownCardWithNumber,
        UnknownCardWithNumber,
        SectionName,
        Comment,
        Unknown
    }
    
    public static class Token {
        private final TokenType type;
        private final CardPrinted card;
        private final int number;
        private final String text;
        
        public Token(CardPrinted knownCard, int count) { 
            card = knownCard;
            text = null;
            number =  count;
            type = TokenType.KnownCardWithNumber;
        }

        public Token(String unknownCard, int count) { 
            card = null;
            text = unknownCard;
            number = count;
            type = TokenType.UnknownCardWithNumber;
        }

        public Token(TokenType type1, int count, String message)
        {
            if (type1 == TokenType.KnownCardWithNumber || type1 == TokenType.UnknownCardWithNumber) {
                throw new IllegalArgumentException("Use specialized constructors for recognized card lines");
            }

            card = null;
            number = count;
            type = type1;
            text = message;
        }

        public String getText() { return text; }
        public CardPrinted getCard() { return card; }
        public TokenType getType() { return type; }
        public int getNumber() { return number; }
    }

    public static Token recognizeLine(String line)
    {
        return new Token(TokenType.Unknown, 0, line);
    }

}
