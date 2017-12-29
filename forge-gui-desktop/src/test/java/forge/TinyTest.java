package forge;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * This test mostly exists to test TestNG itself.
 * 
 * @author Forge
 * @version $Id: TinyTest.java 11726 2011-11-03 16:16:33Z jendave $
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
