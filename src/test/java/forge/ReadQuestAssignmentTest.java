package forge;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"}, timeOut = 1000)
public class ReadQuestAssignmentTest implements NewConstants {
    /**
     *
     *
     */
    @Test(groups = {"UnitTest", "fast"}, timeOut = 1000)
    public void ReadQuestAssignmentTest1() {
        try {
            ReadQuest_Assignment read = new ReadQuest_Assignment(ForgeProps.getFile(QUEST.QUESTS), null);

            javax.swing.SwingUtilities.invokeAndWait(read);
            //    read.run();

            Quest_Assignment qa[] = new Quest_Assignment[read.allQuests.size()];
            read.allQuests.toArray(qa);
            for (int i = 0; i < qa.length; i++) {
                System.out.println(qa[i].getId());
                System.out.println(qa[i].getName());
                System.out.println(qa[i].getDesc());
                System.out.println(qa[i].getDifficulty());
                System.out.println(qa[i].isRepeatable());
                System.out.println(qa[i].getRequiredNumberWins());
            }
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            System.out.println("Error reading file " + ex);
        }
    }
}
