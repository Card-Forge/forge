package forge.screens.home.sanctioned;

import java.lang.reflect.Method;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.deck.Deck;
import forge.deck.DeckGroup;

/**
 * Unit tests for CSubmenuDraft private parsing logic.
 */
public class CSubmenuDraftTest {

    private int invokeParse(final String duelType, final DeckGroup dg) throws Exception {
        Method m = CSubmenuDraft.class.getDeclaredMethod("parseGauntletRounds", String.class, DeckGroup.class);
        m.setAccessible(true);
        return (Integer) m.invoke(CSubmenuDraft.SINGLETON_INSTANCE, duelType, dg);
    }

    @Test(groups = { "UnitTest", "fast" })
    public void testParseGauntletRoundsValid() throws Exception {
        DeckGroup dg = new DeckGroup();
        dg.addAiDeck(new Deck("AI1"));
        dg.addAiDeck(new Deck("AI2"));
        dg.addAiDeck(new Deck("AI3"));
        int rounds = invokeParse("2", dg);
        Assert.assertEquals(rounds, 2);
    }

    @Test(groups = { "UnitTest", "fast" })
    public void testParseGauntletRoundsTooLow() throws Exception {
        DeckGroup dg = new DeckGroup();
        dg.addAiDeck(new Deck("AI1"));
        try {
            invokeParse("0", dg);
            Assert.fail("Expected IllegalStateException");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Assert.assertTrue(ite.getCause() instanceof IllegalStateException,
                    "Expected cause to be IllegalStateException");
        }
    }

    @Test(groups = { "UnitTest", "fast" })
    public void testParseGauntletRoundsTooHigh() throws Exception {
        DeckGroup dg = new DeckGroup();
        dg.addAiDeck(new Deck("AI1"));
        dg.addAiDeck(new Deck("AI2"));
        try {
            invokeParse("5", dg);
            Assert.fail("Expected IllegalStateException");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Assert.assertTrue(ite.getCause() instanceof IllegalStateException,
                    "Expected cause to be IllegalStateException");
        }
    }

    @Test(groups = { "UnitTest", "fast" })
    public void testParseGauntletRoundsNonNumeric() throws Exception {
        DeckGroup dg = new DeckGroup();
        dg.addAiDeck(new Deck("AI1"));
        try {
            invokeParse("not-a-number", dg);
            Assert.fail("Expected NumberFormatException");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Assert.assertTrue(ite.getCause() instanceof NumberFormatException,
                    "Expected cause to be NumberFormatException");
        }
    }
}
