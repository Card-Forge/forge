package forge;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"})
public class GuiProgressBarWindowTest {
    /**
     *
     *
     */
    @Test(groups = {"UnitTest", "fast"})
    public void GuiProgressBarWindowTest1() {
        try {
            GuiProgressBarWindow dialog = new GuiProgressBarWindow();
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
            Assert.assertNotNull(dialog);
            dialog.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
