package forge.headless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MainHelpTest {
    @Test
    public void topLevelHelpExplainsCommandsAndExamples() {
        SystemOutputCapture output = new SystemOutputCapture();
        output.startCapture();
        int exitCode;
        try {
            exitCode = Main.run(new String[] {"--help"});
        } finally {
            output.stopCapture();
        }

        assertEquals(0, exitCode);
        assertTrue(output.getOutput().contains("Run Forge games without starting"));
        assertTrue(output.getOutput().contains("sim    Run automated games"));
        assertTrue(output.getOutput().contains("tui    Play or observe a game"));
        assertTrue(output.getOutput().contains("bridge  Connect Forge AI"));
        assertTrue(output.getOutput().contains("Examples:"));
        assertFalse(output.getOutput().contains("Unknown command"));
    }

    @Test
    public void simulationHelpExplainsRequiredDecksAndOptions() {
        SystemOutputCapture output = new SystemOutputCapture();
        output.startCapture();
        try {
            SimulateMatch.simulate(new String[] {"sim", "--help"});
        } finally {
            output.stopCapture();
        }

        assertTrue(output.getOutput().contains("Run automated Forge games"));
        assertTrue(output.getOutput().contains("At least two are required"));
        assertTrue(output.getOutput().contains("-s <seed>"));
        assertTrue(output.getOutput().contains("Examples:"));
    }
}
