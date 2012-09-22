package forge.gui;

import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA. User: dhudson
 */
@Test(groups = { "UnitTest" }, timeOut = 1000, enabled = false)
public class ListChooserTest {

    /**
     * List chooser test1.
     */
    @Test(groups = { "UnitTest", "fast" }, timeOut = 1000, enabled = false)
    public void listChooserTest1() {
        final ListChooser<String> c = new ListChooser<String>("test", "choose a or b", 0, 2, new String[] {"a", "b"});
        System.out.println(c.show());
        for (final String s : c.getSelectedValues()) {
            System.out.println(s);
        }
    }
}
