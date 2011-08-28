package forge.model;

import static org.testng.Assert.*;

import java.io.FileNotFoundException;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import forge.Singletons;
import forge.properties.ForgePreferences;

/**
 * Tests FModel.
 */
public class FModelTest {


    private FModel model;

    /**
     * Set up before each test, creating a default model.
     * 
     * @throws FileNotFoundException indirectly
     */
    @BeforeTest
    public final void setUp() throws FileNotFoundException {
        model = new FModel(null);
    }

    /**
     * Close the model after each test if it isn't null.
     */
    @AfterTest
    public final void tearDown() {
        if (model != null) {
            try {
                model.close();
            }
            catch (Throwable ignored) {
                // ignore exceptions during close.
            }
        }

        model = null;
    }


    /**
     * Test constructor (via setUp), close, and construct again.
     * @throws FileNotFoundException if something is really wrong
     */
    @Test
    public final void test_ctor_close_ctor() throws FileNotFoundException { // NOPMD by Braids on 8/12/11 10:36 AM
        assertNotNull(model, "model is not null");
        model.close();

        System.err.println("log test"); // NOPMD by Braids on 8/12/11 10:36 AM

        model = new FModel(null);
        assertNotNull(model, "model is not null");
    }

    /**
     * Test getVersion.
     * @throws FileNotFoundException if something is really wrong
     */
    @Test
    public final void test_getVersion() throws FileNotFoundException {
        final String version = model.getBuildInfo().getVersion();

        assertEquals(version, "SVN", "version is default");
    }

    /**
     * Test getBuildID.
     * @throws FileNotFoundException if something is really wrong
     */
    @Test
    public final void test_getBuildID() throws FileNotFoundException { // NOPMD by Braids on 8/12/11 10:36 AM
        // Just test for an unexpected exception.
        model.getBuildInfo().getBuildID();
    }

    /**
     * Test getPreferences.
     * @throws FileNotFoundException indirectly
     */
    @Test
    public final void test_getPreferences() throws FileNotFoundException {
        ForgePreferences prefs = model.getPreferences();
        assertNotNull(prefs, "prefs instance is not null");
    }

    /**
     * Test resetGameState and getGameState.
     */
    @Test
    public final void test_resetGameState_getGameState() {
        Singletons.setModel(model);
        assertNull(model.getGameState(), "game state has not yet been initialized");

        FGameState state1 = model.resetGameState();
        assertNotNull(state1, "first state is OK");

        FGameState state2 = model.resetGameState();
        assertNotNull(state1, "first state is OK");
        assertNotEquals(state1, state2, "first and second states are different");

    }

}

