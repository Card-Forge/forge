package forge.model;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.FileNotFoundException;

import org.testng.annotations.Test;

import forge.properties.ForgePreferences;

/**
 * Tests FModel.
 */
public class FModelTest {


    /**
     * Test constructor, close, and construct again.
     * @throws FileNotFoundException if something is really wrong
     */
    @Test
    public final void test_ctor_close_ctor() throws FileNotFoundException { // NOPMD by Braids on 8/12/11 10:36 AM
        final FModel modelA = new FModel(null);
        assertNotNull(modelA, "modelA is not null");
        modelA.close();

        System.err.println("log test"); // NOPMD by Braids on 8/12/11 10:36 AM

        final FModel modelB = new FModel(null);
        assertNotNull(modelB, "modelB is not null");
    }

    /**
     * Test getVersion.
     * @throws FileNotFoundException if something is really wrong
     */
    @Test
    public final void test_getVersion() throws FileNotFoundException { // NOPMD by Braids on 8/12/11 10:36 AM
        final FModel model = new FModel(null);
        final String version = model.getBuildInfo().getVersion();
        model.close();

        assertEquals(version, "SVN", "version is default");
    }

    /**
     * Test getBuildID.
     * @throws FileNotFoundException if something is really wrong
     */
    @Test
    public final void test_getBuildID() throws FileNotFoundException { // NOPMD by Braids on 8/12/11 10:36 AM
        final FModel model = new FModel(null);

        // Just test for an unexpected exception.
        model.getBuildInfo().getBuildID();

        model.close();
    }

    /**
     * Test getPreferences.
     * @throws FileNotFoundException indirectly
     */
    @Test
    public final void test_getPreferences() throws FileNotFoundException {
        final FModel model = new FModel(null);
        try {
            ForgePreferences prefs = model.getPreferences();
            assertNotNull(prefs, "prefs instance is not null");
        } finally {
            model.close();
        }
    }
}
