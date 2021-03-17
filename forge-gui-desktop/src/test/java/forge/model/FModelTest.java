package forge.model;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import forge.error.ExceptionHandler;
import forge.localinstance.properties.ForgePreferences;
import forge.util.BuildInfo;

/**
 * Tests FModel.
 */
@Test(enabled = false)
public class FModelTest {
    /**
     * Set up before each test, creating a default model.
     *
     */
    @BeforeTest
    public final void setUp() {
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
     */
    @Test(enabled = false)
    public final void test_getVersion() {
        final String version = BuildInfo.getVersionString();

        Assert.assertEquals(version, "GIT", "version is default");
    }

    /**
     * Test getPreferences.
     *
     */
    @Test(enabled = false)
    public final void test_getPreferences() {
        final ForgePreferences prefs = FModel.getPreferences();
        Assert.assertNotNull(prefs, "prefs instance is not null");
    }

}
