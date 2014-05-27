package forge;

import forge.download.GuiDownloadSetPicturesLQ;
import forge.download.GuiDownloader;

import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA. User: dhudson
 */
@Test(groups = { "UnitTest" }, timeOut = 1000, enabled = false)
public class GuiDownloadSetPicturesLQTest {

    /**
     * Gui download set pictures lq test1.
     */
    @Test(enabled = false, timeOut = 1000)
    public void g() {
        new GuiDownloader(new GuiDownloadSetPicturesLQ());
    }
}
