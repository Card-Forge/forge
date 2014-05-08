package forge.model;

import forge.error.ExceptionHandler;
import forge.properties.ForgePreferences;
import forge.util.BuildInfo;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Tests FModel.
 */
@Test(enabled = false)
public class FModelTest {
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
        try {
            ExceptionHandler.unregisterErrorHandling();
        } catch (final Throwable ignored) {
            // ignore exceptions during close.
        }
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
        ExceptionHandler.unregisterErrorHandling();

        System.err.println("log test");
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
        final ForgePreferences prefs = FModel.getPreferences();
        Assert.assertNotNull(prefs, "prefs instance is not null");
    }

}
