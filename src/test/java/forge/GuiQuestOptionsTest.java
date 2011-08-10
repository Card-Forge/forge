package forge;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"})
public class GuiQuestOptionsTest {
    /**
     *
     *
     */
    @Test(groups = {"UnitTest", "fast"})
    public void GuiQuestOptionsTest1() {
        Gui_QuestOptions dialog = new Gui_QuestOptions();
        dialog.setVisible(true);
        Assert.assertNotNull(dialog);
        dialog.dispose();
    }
}
