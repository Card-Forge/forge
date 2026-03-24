package forge.sim;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SimVerboseConfigTest {

    @Test
    public void parseCountOptionSupportsSpecialValues() {
        Assert.assertEquals(SimVerboseConfig.parseCountOption("-1"), Integer.valueOf(-1));
        Assert.assertNull(SimVerboseConfig.parseCountOption("0"));
        Assert.assertEquals(SimVerboseConfig.parseCountOption("5"), Integer.valueOf(5));
    }

    @Test
    public void parseCountOptionHandlesCommentsAndWhitespace() {
        Assert.assertEquals(SimVerboseConfig.parseCountOption("  7   # top seven"), Integer.valueOf(7));
        Assert.assertEquals(SimVerboseConfig.parseCountOption(" -1 # full"), Integer.valueOf(-1));
    }

    @Test
    public void parseCountOptionRejectsInvalidValues() {
        Assert.assertNull(SimVerboseConfig.parseCountOption(null));
        Assert.assertNull(SimVerboseConfig.parseCountOption(""));
        Assert.assertNull(SimVerboseConfig.parseCountOption("  "));
        Assert.assertNull(SimVerboseConfig.parseCountOption("-2"));
        Assert.assertNull(SimVerboseConfig.parseCountOption("abc"));
    }
}
