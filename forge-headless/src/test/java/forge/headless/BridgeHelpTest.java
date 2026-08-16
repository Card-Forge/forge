package forge.headless;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import org.junit.Test;

import picocli.CommandLine;

public class BridgeHelpTest {
    @Test
    public void publicHelpExplainsCoordinatorTransportAndSeatOrdering() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CommandLine command = new CommandLine(new BridgeOptions());
        command.usage(new PrintWriter(output, true));
        String help = output.toString();

        assertTrue(help.contains("Run one Forge AI player under an external game coordinator"));
        assertTrue(help.contains("standard input"));
        assertTrue(help.contains("ordered by one-based seat"));
        assertTrue(help.contains("number"));
        assertTrue(help.contains("normally launched by a coordinator"));
        assertFalse(help.contains("Task B"));
        assertFalse(help.contains("--skeleton"));
    }
}
