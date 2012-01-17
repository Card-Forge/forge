package forge;

import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA. User: dhudson
 */
@Test(groups = { "UnitTest" }, timeOut = 5000, enabled = false)
public class PhaseHandlerTest {

    /**
     * PhaseHandler test1.
     */
    @Test(groups = { "UnitTest", "fast" }, timeOut = 5000, enabled = false)
    public void phaseTest1() {
        final PhaseHandler phase = new PhaseHandler();
        for (int i = 0; i < phase.getPhaseOrder().length; i++) {
            System.out.println(phase.getPlayerTurn() + " " + phase.getPhase());
            phase.nextPhase();
        }
    }
}
