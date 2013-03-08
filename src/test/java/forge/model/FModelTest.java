package forge.model;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import forge.Singletons;
import forge.properties.ForgePreferences;

/**
 * Tests FModel.
 */
@Test(enabled = false)
public class FModelTest {

    private FModel model;

    /**
     * Set up before each test, creating a default model.
     * 
     * @throws FileNotFoundException
     *             indirectly
     */
    @BeforeTest
    public final void setUp() throws FileNotFoundException {
       // this.model = new FModel();
    }

    /**
     * Close the model after each test if it isn't null.
     */
    @AfterTest
    public final void tearDown() {
        if (this.model != null) {
            try {
                this.model.close();
            } catch (final Throwable ignored) {
                // ignore exceptions during close.
            }
        }

        this.model = null;
    }

    /**
     * Test constructor (via setUp), close, and construct again.
     * 
     * @throws FileNotFoundException
     *             if something is really wrong
     */
    @Test(enabled = false)
    public final void test_ctor_close_ctor() throws IOException {
        // by
        // Braids
        // on
        // 8/12/11
        // 10:36
        // AM
        Assert.assertNotNull(this.model, "model is not null");
        this.model.close();

        System.err.println("log test");

       // this.model = new FModel();
        Assert.assertNotNull(this.model, "model is not null");
    }

    /**
     * Test getVersion.
     * 
     * @throws FileNotFoundException
     *             if something is really wrong
     */
    @Test(enabled = false)
    public final void test_getVersion() throws FileNotFoundException {
        final String version = BuildInfo.getVersionString();

        Assert.assertEquals(version, "SVN", "version is default");
    }

    /**
     * Test getPreferences.
     * 
     * @throws FileNotFoundException
     *             indirectly
     */
    @Test(enabled = false)
    public final void test_getPreferences() throws FileNotFoundException {
        final ForgePreferences prefs = this.model.getPreferences();
        Assert.assertNotNull(prefs, "prefs instance is not null");
    }

    /**
     * Test resetGameState and getGameState.
     */
    @Test(enabled = false)
    public final void test_resetGameState_getGameState() {
        Singletons.setModel(this.model);
        Assert.assertNull(this.model.getGame(), "game state has not yet been initialized");

        /*final GameState state1 = this.model.resetGameState();
        Assert.assertNotNull(state1, "first state is OK");

        final GameState state2 = this.model.resetGameState();
        Assert.assertNotNull(state1, "first state is OK");
        Assert.assertNotEquals(state1, state2, "first and second states are different");*/

    }

}
