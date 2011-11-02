package forge;

import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"}, timeOut = 1000, enabled = false)
public class GuiMigrateLocalMWSSetPicturesHQTest {


    /**
     *
     */
    @Test(enabled = false, timeOut = 1000)
    public  void GuiMigrateLocalMWSSetPicturesHQ1() {
        GuiMigrateLocalMWSSetPicturesHQ.startDownload(null);
    }
}
