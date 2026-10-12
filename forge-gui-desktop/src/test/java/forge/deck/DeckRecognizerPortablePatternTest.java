package forge.deck;

import org.testng.annotations.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DeckRecognizerPortablePatternTest {
    @Test
    public void testPortablePatternFallback() {
        String[] importerPatterns = {
                DeckRecognizer.REX_DECK_NAME,
                DeckRecognizer.REX_NOCARD,
                DeckRecognizer.REX_CMC,
                DeckRecognizer.REX_RARITY,
                DeckRecognizer.REX_MANA,
                DeckRecognizer.REX_MANA_SYMBOLS,
                DeckRecognizer.REX_CARD_SET_REQUEST,
                DeckRecognizer.REX_SET_CARD_REQUEST,
                DeckRecognizer.REX_FULL_REQUEST_CARD_SET,
                DeckRecognizer.REX_FULL_REQUEST_SET_CARD,
                DeckRecognizer.REX_FULL_REQUEST_CARD_COLLNO_SET,
                DeckRecognizer.REX_FULL_REQUEST_XMAGE,
                DeckRecognizer.REX_CARDONLY
        };
        for (String regex : importerPatterns) {
            DeckRecognizer.compilePortablePattern(regex, Pattern.CASE_INSENSITIVE);
        }

        Pattern deckNamePattern = DeckRecognizer.compilePortablePattern(
                DeckRecognizer.REX_DECK_NAME, Pattern.CASE_INSENSITIVE);
        Matcher deckNameMatcher = deckNamePattern.matcher("Deck: Red Green Aggro");
        assertTrue(deckNameMatcher.matches());
        assertEquals(DeckRecognizer.getRexGroup(deckNameMatcher, DeckRecognizer.REGRP_DECKNAME),
                "Red Green Aggro");

        Pattern cardPattern = DeckRecognizer.compilePortablePattern(
                DeckRecognizer.REX_FULL_REQUEST_CARD_SET, 0);
        Matcher cardMatcher = cardPattern.matcher("SB:4x Power Sink+ (TMP) 78 *F*");
        assertTrue(cardMatcher.matches());
        assertEquals(DeckRecognizer.getRexGroup(cardMatcher, DeckRecognizer.REGRP_DECK_SEC_XMAGE_STYLE), "SB");
        assertEquals(DeckRecognizer.getRexGroup(cardMatcher, DeckRecognizer.REGRP_CARDNO), "4");
        assertEquals(DeckRecognizer.getRexGroup(cardMatcher, DeckRecognizer.REGRP_CARD), "Power Sink+ ");
        assertEquals(DeckRecognizer.getRexGroup(cardMatcher, DeckRecognizer.REGRP_SET), "TMP");
        assertEquals(DeckRecognizer.getRexGroup(cardMatcher, DeckRecognizer.REGRP_COLLNR), "78");
        assertEquals(DeckRecognizer.getRexGroup(cardMatcher, DeckRecognizer.REGRP_FOIL_GFISH), "*F*");
    }
}
