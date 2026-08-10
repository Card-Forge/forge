package forge.view;

import forge.game.decision.DeterminismTrace;
import forge.game.decision.DiagnosticOutputPaths;
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

    @Test
    public void completeWorkerNamespaceSelectsAuditRandomWithoutAnExplicitTraceDirectory() {
        final String previousRoot = System.getProperty(DiagnosticOutputPaths.OUTPUT_ROOT_PROPERTY);
        final String previousRun = System.getProperty(DiagnosticOutputPaths.RUN_ID_PROPERTY);
        final String previousWorker = System.getProperty(DiagnosticOutputPaths.WORKER_ID_PROPERTY);
        final String previousTrace = System.getProperty(DeterminismTrace.OUTPUT_DIRECTORY_PROPERTY);
        try {
            System.clearProperty(DeterminismTrace.OUTPUT_DIRECTORY_PROPERTY);
            System.setProperty(DiagnosticOutputPaths.OUTPUT_ROOT_PROPERTY, "audit-root");
            System.setProperty(DiagnosticOutputPaths.RUN_ID_PROPERTY, "run-alpha");
            System.setProperty(DiagnosticOutputPaths.WORKER_ID_PROPERTY, "0");

            assertTrue(SimulateMatch.seededRandom(20260810L) instanceof DeterminismAuditRandom);
        } finally {
            restore(DiagnosticOutputPaths.OUTPUT_ROOT_PROPERTY, previousRoot);
            restore(DiagnosticOutputPaths.RUN_ID_PROPERTY, previousRun);
            restore(DiagnosticOutputPaths.WORKER_ID_PROPERTY, previousWorker);
            restore(DeterminismTrace.OUTPUT_DIRECTORY_PROPERTY, previousTrace);
        }
    }

    @Test
    public void explicitAuditRandomSwitchDoesNotRequireTheTraceCollector() {
        final String previousAudit = System.getProperty(DeterminismTrace.AUDIT_RANDOM_PROPERTY);
        final String previousTrace = System.getProperty(DeterminismTrace.OUTPUT_DIRECTORY_PROPERTY);
        try {
            System.clearProperty(DeterminismTrace.OUTPUT_DIRECTORY_PROPERTY);
            System.setProperty(DeterminismTrace.AUDIT_RANDOM_PROPERTY, "true");

            assertTrue(SimulateMatch.seededRandom(20260810L) instanceof DeterminismAuditRandom);
        } finally {
            restore(DeterminismTrace.AUDIT_RANDOM_PROPERTY, previousAudit);
            restore(DeterminismTrace.OUTPUT_DIRECTORY_PROPERTY, previousTrace);
        }
    }

    private static void restore(final String property, final String value) {
        if (value == null) {
            System.clearProperty(property);
        } else {
            System.setProperty(property, value);
        }
    }
}
