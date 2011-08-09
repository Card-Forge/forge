package forge;

import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"}, timeOut = 10000, enabled = false)
public class ReadBoosterPackTest {
    /**
     *
     *
     */
    @Test(timeOut = 10000, enabled = false)
    public void ReadBoosterPackTest1() {
        //testing
        ReadBoosterPack r = new ReadBoosterPack();


        for (int i = 0; i < 1000; i++) {
            r.getBoosterPack5();
        }
    }//main()
}
