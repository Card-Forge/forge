package forge;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.quest.gui.QuestOptions;

/**
 * Created by IntelliJ IDEA. User: dhudson
 */
@Test(groups = { "UnitTest" }, enabled = false)
public class GuiQuestOptionsTest {

    /**
     * Gui quest options test1.
     */
    @Test(groups = { "UnitTest", "fast" }, enabled = false)
    public void guiQuestOptionsTest1() {
        final QuestOptions dialog = new QuestOptions();
        dialog.setVisible(true);
        Assert.assertNotNull(dialog);
        dialog.dispose();
    }
}
