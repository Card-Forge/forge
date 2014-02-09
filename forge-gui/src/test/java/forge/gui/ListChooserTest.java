package forge.gui;

import org.testng.annotations.Test;

import java.util.Arrays;

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
        final ListChooser<String> c = new ListChooser<String>("choose a or b", 0, 2, Arrays.asList("a", "b"), null);
        System.out.println(c.show());
        for (final String s : c.getSelectedValues()) {
            System.out.println(s);
        }
    }
}
