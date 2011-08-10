package forge;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import net.slightlymagic.braids.util.ClumsyRunnable;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static net.slightlymagic.braids.util.testng.BraidsAssertFunctions.assertThrowsException;

/**
 * Created by hand to test the CardReader class.
 */
@Test(groups = {"UnitTest"})
public class CardReaderTest implements NewConstants {

    /** The default test-timeout. */
    public static final int TEST_TIMEOUT = 1000;

    /** The estimated number of cards in the cardsfolder. */
    public static final int ESTIMATED_CARDS_IN_FOLDER = 9001;


    @Test(groups = {"UnitTest", "fast"}, timeOut = TEST_TIMEOUT)
    public final void test_ReadCard_nullMap() {
        final ClumsyRunnable withScissors = new ClumsyRunnable() {
            public void run() throws Exception {
                new CardReader(ForgeProps.getFile(CARDSFOLDER), null);
            }
        };

        assertThrowsException(NullPointerException.class, withScissors);
    }

    @Test(groups = {"UnitTest", "fast"}, timeOut = TEST_TIMEOUT)
    public final void test_ReadCard_nullCardsFolder() {
        final ClumsyRunnable withScissors = new ClumsyRunnable() {
            public void run() throws Exception {
                Map<String, Card> map = new HashMap<String, Card>();
                new CardReader(null, map);
            }
        };

        assertThrowsException(NullPointerException.class, withScissors);
    }

    @Test(groups = {"UnitTest", "fast"}, timeOut = TEST_TIMEOUT)
    public final void test_ReadCard_nonexistentCardsFolder() {
        final ClumsyRunnable withScissors = new ClumsyRunnable() {
            public void run() throws Exception {
                Map<String, Card> map = new HashMap<String, Card>();
                new CardReader(new File(
                        "this_does_not_exist_fjksdjfsdjfkdjslkfksdlajfikajfklsdhfksdalfhjklsdahfeakslfdsfdsfdsfdsfdssfc"
                        ), map);
            }
        };

        assertThrowsException(RuntimeException.class, withScissors);
    }

    @Test(groups = {"UnitTest", "fast"}, timeOut = TEST_TIMEOUT)
    public final void test_ReadCard_fileNotFolder() throws IOException {

        final File tmpFile = File.createTempFile("just-a-file", ".testng.tmp");
        tmpFile.deleteOnExit();  // request VM to delete later

        final ClumsyRunnable withScissors = new ClumsyRunnable() {
            public void run() throws Exception {
                Map<String, Card> map = new HashMap<String, Card>();
                new CardReader(tmpFile, map);
            }
        };

        assertThrowsException(RuntimeException.class, withScissors);
    }

    @Test(groups = {"UnitTest", "fast"}, timeOut = TEST_TIMEOUT)
    public final void test_ReadCard_findCard_zip() {
        final Map<String, Card> map = new HashMap<String, Card>();
        final File cardsfolder = ForgeProps.getFile(CARDSFOLDER);
        final CardReader cardReader = new CardReader(cardsfolder, map);

        final File zipFile = new File(cardsfolder, "cardsfolder.zip");

        Assert.assertTrue(zipFile.exists(), "zip file exists");

        final Card elvishWarrior = cardReader.findCard("Elvish Warrior");

        Assert.assertNotNull(elvishWarrior);
        Assert.assertEquals(elvishWarrior.getName(), "Elvish Warrior", "name is correct");
    }

    @Test(groups = {"UnitTest", "fast"}, timeOut = TEST_TIMEOUT)
    public final void test_ReadCard_findCard_nonzip() {
        final Map<String, Card> map = new HashMap<String, Card>();
        final File cardsfolder = ForgeProps.getFile(CARDSFOLDER);
        final CardReader cardReader = new CardReader(cardsfolder, map, false);

        final Card savannahLions = cardReader.findCard("Savannah Lions");

        Assert.assertNotNull(savannahLions);
        Assert.assertEquals(savannahLions.getName(), "Savannah Lions", "name is correct");
    }

    @Test(groups = {"slow"})
    public final void test_ReadCard_run_nonzip() {
        final Map<String, Card> map = new HashMap<String, Card>(2 * ESTIMATED_CARDS_IN_FOLDER);
        final File cardsfolder = ForgeProps.getFile(CARDSFOLDER);
        final CardReader cardReader = new CardReader(cardsfolder, map, false);
        cardReader.run();
        Assert.assertNotNull(map.get("Elvish Warrior"), "Elvish Warrior was loaded");
        Assert.assertNotNull(map.get("Savannah Lions"), "Savannah Lions were loaded");
    }

    @Test(groups = {"slow"})
    public final void test_ReadCard_run_zip() {
        final Map<String, Card> map = new HashMap<String, Card>(2 * ESTIMATED_CARDS_IN_FOLDER);
        final File cardsfolder = ForgeProps.getFile(CARDSFOLDER);
        final CardReader cardReader = new CardReader(cardsfolder, map);
        cardReader.run();
        Assert.assertNotNull(map.get("Elvish Warrior"), "Elvish Warrior was loaded");
        Assert.assertNotNull(map.get("Savannah Lions"), "Savannah Lions were loaded");
    }

}
