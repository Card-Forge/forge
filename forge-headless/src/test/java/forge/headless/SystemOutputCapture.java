package forge.headless;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for capturing System.out and System.err during tests.
 * Helps detect error messages that aren't logged to the game log.
 */
public class SystemOutputCapture {
    private final ByteArrayOutputStream outputStream;
    private final ByteArrayOutputStream errorStream;
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    private boolean capturing = false;

    public SystemOutputCapture() {
        this.outputStream = new ByteArrayOutputStream();
        this.errorStream = new ByteArrayOutputStream();
        this.originalOut = System.out;
        this.originalErr = System.err;
    }

    /**
     * Start capturing System.out and System.err.
     */
    public void startCapture() {
        if (!capturing) {
            System.setOut(new PrintStream(outputStream));
            System.setErr(new PrintStream(errorStream));
            capturing = true;
        }
    }

    /**
     * Stop capturing and restore original streams.
     */
    public void stopCapture() {
        if (capturing) {
            System.setOut(originalOut);
            System.setErr(originalErr);
            capturing = false;
        }
    }

    /**
     * Get the captured output.
     */
    public String getOutput() {
        return outputStream.toString();
    }

    /**
     * Get the captured error output.
     */
    public String getErrorOutput() {
        return errorStream.toString();
    }

    /**
     * Get all captured output (both stdout and stderr).
     */
    public String getAllOutput() {
        return getOutput() + getErrorOutput();
    }

    /**
     * Check if any error patterns were captured.
     * NOTE: "Did not have activator set" is treated as a warning, not a fatal error,
     * since it's self-correcting and comes from deep in the game engine.
     */
    public boolean hasErrorPatterns() {
        String allOutput = getAllOutput();
        return checkForErrors(allOutput);
    }

    /**
     * Get list of error patterns found in output.
     */
    public List<String> getErrorPatterns() {
        List<String> errors = new ArrayList<>();
        String allOutput = getAllOutput();

        if (allOutput.contains("cost was not paid for")) {
            errors.add("Mana cost payment failure detected");
        }
        if (allOutput.contains("Couldn't add to stack, failed to target")) {
            errors.add("Targeting failure detected");
        }
        if (allOutput.contains("AI failed to play")) {
            errors.add("AI play failure detected");
        }
        // Note: "Did not have activator set" warnings are excluded - they're self-correcting

        return errors;
    }

    private boolean checkForErrors(String output) {
        // Only check for actual errors, not self-correcting warnings
        return output.contains("cost was not paid for") ||
               output.contains("Couldn't add to stack, failed to target") ||
               output.contains("AI failed to play");
    }

    /**
     * Clear captured output.
     */
    public void clear() {
        outputStream.reset();
        errorStream.reset();
    }
}
