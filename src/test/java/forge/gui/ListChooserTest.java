package forge.gui;

import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"}, timeOut = 1000, enabled = false)
public class ListChooserTest {
    /**
     *
     *
     */
    @Test(timeOut = 1000)
    public void ListChooserTest1() {
        ListChooser<String> c = new ListChooser<String>("test", "choose a or b", 0, 2, "a", "b");
        System.out.println(c.show());
        for (String s : c.getSelectedValues()) {
            System.out.println(s);
        }
    }
}
