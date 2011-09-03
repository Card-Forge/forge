package forge;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.quest.gui.QuestOptions;

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
        QuestOptions dialog = new QuestOptions();
        dialog.setVisible(true);
        Assert.assertNotNull(dialog);
        dialog.dispose();
    }
}
