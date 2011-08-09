package forge;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.swing.*;

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
    @Test()
    public void guiFilterTest1() {
        try {
            GuiFilter dialog = new GuiFilter(null, null);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
            Assert.assertNotNull(dialog);
            dialog.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
