package forge;

import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"}, timeOut = 5000, enabled = false)
public class PhaseTest {
    /**
     *
     *
     */
    @Test(timeOut = 5000, enabled = false)
    public void PhaseTest1() {
        Phase phase = new Phase();
        for (int i = 0; i < phase.phaseOrder.length; i++) {
            System.out.println(phase.getPlayerTurn() + " " + phase.getPhase());
            phase.nextPhase();
        }
    }
}
