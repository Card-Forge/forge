package forge;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"})
public class GuiQuestFilterTest {
    /**
     *
     *
     */
    @Test(groups = {"UnitTest", "fast"})
    public void guiQuestFilterTest1() {
        try {
            GuiQuestFilter dialog = new GuiQuestFilter(null, null);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
            Assert.assertNotNull(dialog);
            dialog.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
