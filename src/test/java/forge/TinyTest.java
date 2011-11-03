package forge;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * This test mostly exists to test TestNG itself.
 * 
 * @author Forge
 * @version $Id$
 */
@Test(groups = { "UnitTest" })
public class TinyTest {
    /**
     * Just a quick test to see if TestNG and Assert are working.
     */
    @Test(groups = { "UnitTest", "fast" })
    public void test_true() {
        Assert.assertTrue(true);
    }
}
