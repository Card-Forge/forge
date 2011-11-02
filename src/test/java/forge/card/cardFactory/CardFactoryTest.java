package forge.card.cardFactory;

import forge.Card;
import forge.CardList;
import forge.card.cardfactory.CardFactoryInterface;
import forge.card.cardfactory.LazyCardFactory;
import forge.card.cardfactory.PreloadingCardFactory;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.swing.OldGuiNewGame;
import net.slightlymagic.braids.util.ClumsyRunnable;
import net.slightlymagic.braids.util.testng.BraidsAssertFunctions;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;
import java.util.TreeSet;

//import net.slightlymagic.braids.testng.BraidsAssertFunctions;

/**
 * <p>Mana_PartTest class.</p>
 *
 * @author Forge
 * @version $Id$
 */
@Test(groups = {"UnitTest" }, timeOut = CardFactoryTest.DEFAULT_TEST_TIMEOUT_MS)
public class CardFactoryTest implements NewConstants {

    /** The default time to allow a test to run before TestNG ignores it. */
    public static final int DEFAULT_TEST_TIMEOUT_MS = 5000; // NOPMD by Braids on 8/18/11 11:43 PM

    private transient CardFactoryInterface factory;

    /**
     * Executed before each test method, as in JUnit.
     */
    @BeforeMethod
    public final void setUp() {
        OldGuiNewGame.loadDynamicGamedata();
        factory = new LazyCardFactory(ForgeProps.getFile(CARDSFOLDER));
    }

    /**
     * Just a quick test to see if Arc-Slogger is in the database, and if it
     * has the correct owner.
     */
    @Test(enabled = true, timeOut = DEFAULT_TEST_TIMEOUT_MS)
    public final void test_getCard_1() { // NOPMD by Braids on 8/18/11 11:39 PM
        final Card card = factory.getCard("Arc-Slogger", null);
        Assert.assertNotNull(card, "card is not null");
        Assert.assertNull(card.getOwner(), "card has correct owner");
    }

    /**
     * Make sure the method throws an exception when it's supposed to.
     */
    @Test(enabled = true, timeOut = DEFAULT_TEST_TIMEOUT_MS)
    public final void test_getRandomCombinationWithoutRepetition_tooLarge() { // NOPMD by Braids on 8/18/11 11:39 PM
        BraidsAssertFunctions.assertThrowsException(IllegalArgumentException.class,
                new ClumsyRunnable() {
            public void run() throws Exception { // NOPMD by Braids on 8/18/11 11:40 PM
                factory.getRandomCombinationWithoutRepetition(factory.size());
            }
        });

        BraidsAssertFunctions.assertThrowsException(IllegalArgumentException.class,
                new ClumsyRunnable() {
            public void run() throws Exception { // NOPMD by Braids on 8/18/11 11:40 PM
                final int largeDivisorForRandomCombo = 4; // NOPMD by Braids on 8/18/11 11:41 PM
                factory.getRandomCombinationWithoutRepetition(factory.size() / largeDivisorForRandomCombo);
            }
        });
    }

    /**
     * Make sure the method works.
     *
     * This doesn't work with LazyCardFactory, so it is too slow to enable by default.
     */
    @Test(enabled = false, timeOut = DEFAULT_TEST_TIMEOUT_MS)
    public final void test_getRandomCombinationWithoutRepetition_oneTenth() { // NOPMD by Braids on 8/18/11 11:39 PM
        factory = new PreloadingCardFactory(ForgeProps.getFile(CARDSFOLDER));
        final int divisor = 10; // NOPMD by Braids on 8/18/11 11:41 PM
        final CardList actual = factory.getRandomCombinationWithoutRepetition(factory.size() / divisor);

        final Set<String> cardNames = new TreeSet<String>();

        for (Card card : actual) {
            Assert.assertNotNull(card);
            cardNames.add(card.getName());
        }

        // Make sure we got a unique set of card names and that all are
        // accounted for.
        Assert.assertEquals(actual.size(), cardNames.size());
    }
}
