package forge;

import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA. User: dhudson
 */
@Test(groups = { "UnitTest" }, timeOut = 1000, enabled = false)
public class GuiDownloadPicturesLQTest {

    /**
     * Gui download pictures test1.
     */
    @Test(enabled = false, timeOut = 1000)
    public void guiDownloadPicturesTest1() {
        new GuiDownloadPicturesLQ(null);
    }
}
