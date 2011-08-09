package forge;

import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"}, timeOut = 1000, enabled = false)
public class GuiDownloadPicturesLQTest {


    /**
     *
     */
    @Test(enabled = false, timeOut = 1000)
    public  void GuiDownloadPicturesTest1() {
        Gui_DownloadPictures_LQ.startDownload(null);
    }
}
