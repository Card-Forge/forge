package forge;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"})
public class GuiFilterTest {
    /**
     *
     *
     */
    @Test(groups = {"UnitTest", "fast"})
    public void guiFilterTest1() {
        try {
            GuiFilter dialog = new GuiFilter(null, null);
            dialog.setVisible(true);
            Assert.assertNotNull(dialog);
            dialog.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
