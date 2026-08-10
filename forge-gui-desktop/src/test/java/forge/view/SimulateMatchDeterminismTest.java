package forge.view;

import forge.game.decision.DeterminismTrace;
import forge.util.DeterminismAuditRandom;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SimulateMatchDeterminismTest {

    @Test
    public void traceDirectorySelectsAuditRandomWithoutChangingDisabledRuns() {
        final String previous = System.getProperty(DeterminismTrace.OUTPUT_DIRECTORY_PROPERTY);
        try {
            System.clearProperty(DeterminismTrace.OUTPUT_DIRECTORY_PROPERTY);
            final Random disabled = SimulateMatch.seededRandom(20260810L);
            System.setProperty(DeterminismTrace.OUTPUT_DIRECTORY_PROPERTY, "audit-output");
            final Random enabled = SimulateMatch.seededRandom(20260810L);

            assertFalse(disabled instanceof DeterminismAuditRandom);
            assertTrue(enabled instanceof DeterminismAuditRandom);
            assertTrue(disabled.nextLong() == enabled.nextLong());
        } finally {
            if (previous == null) {
                System.clearProperty(DeterminismTrace.OUTPUT_DIRECTORY_PROPERTY);
            } else {
                System.setProperty(DeterminismTrace.OUTPUT_DIRECTORY_PROPERTY, previous);
            }
        }
    }
}
