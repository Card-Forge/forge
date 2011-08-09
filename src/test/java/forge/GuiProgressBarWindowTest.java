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
    @Test()
    public void GuiProgressBarWindowTest1() {
        try {
            Gui_ProgressBarWindow dialog = new Gui_ProgressBarWindow();
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
            Assert.assertNotNull(dialog);
            dialog.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
