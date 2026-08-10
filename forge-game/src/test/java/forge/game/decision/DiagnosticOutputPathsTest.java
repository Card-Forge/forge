package forge.game.decision;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;

public class DiagnosticOutputPathsTest {
    private static final String ROOT = "forge.diagnostics.outputRoot";
    private static final String RUN_ID = "forge.diagnostics.runId";
    private static final String WORKER_ID = "forge.diagnostics.workerId";
    private static final String PRIORITY = "forge.priority.metricsFile";
    private static final String MULLIGAN = "forge.mulligan.metricsFile";
    private static final String DETERMINISM = "forge.determinism.traceDir";
    private static final String[] PROPERTIES = { ROOT, RUN_ID, WORKER_ID, PRIORITY, MULLIGAN, DETERMINISM };

    private final Map<String, String> previous = new LinkedHashMap<>();

    @BeforeMethod
    public void preserveProperties() {
        for (final String property : PROPERTIES) {
            previous.put(property, System.getProperty(property));
            System.clearProperty(property);
        }
    }

    @AfterMethod
    public void restoreProperties() {
        for (final String property : PROPERTIES) {
            final String value = previous.get(property);
            if (value == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, value);
            }
        }
        previous.clear();
    }

    @Test
    public void completeWorkerNamespaceEnablesAllThreeDerivedOutputs() {
        System.setProperty(ROOT, "audit-root");
        System.setProperty(RUN_ID, "run-alpha");
        System.setProperty(WORKER_ID, "7");

        final DiagnosticOutputPaths.Resolved paths = DiagnosticOutputPaths.resolve();

        final Path worker = Path.of("audit-root", "run-alpha", "worker-007");
        assertEquals(paths.workerDirectory().orElseThrow(), worker);
        assertEquals(paths.priorityMetricsFile().orElseThrow(), worker.resolve("priority.csv"));
        assertEquals(paths.mulliganMetricsFile().orElseThrow(), worker.resolve("mulligan.csv"));
        assertEquals(paths.determinismTraceDirectory().orElseThrow(), worker.resolve("determinism"));
    }

    @Test
    public void explicitPathsOverrideOnlyTheirOwnDerivedSink() {
        System.setProperty(ROOT, "audit-root");
        System.setProperty(RUN_ID, "run-alpha");
        System.setProperty(WORKER_ID, "0");
        System.setProperty(PRIORITY, "special-priority.csv");

        final DiagnosticOutputPaths.Resolved paths = DiagnosticOutputPaths.resolve();

        final Path worker = Path.of("audit-root", "run-alpha", "worker-000");
        assertEquals(paths.priorityMetricsFile().orElseThrow(), Path.of("special-priority.csv"));
        assertEquals(paths.mulliganMetricsFile().orElseThrow(), worker.resolve("mulligan.csv"));
        assertEquals(paths.determinismTraceDirectory().orElseThrow(), worker.resolve("determinism"));
    }

    @Test
    public void partialWorkerNamespaceFailsEvenWhenOneSinkHasAnExplicitPath() {
        System.setProperty(ROOT, "audit-root");
        System.setProperty(RUN_ID, "run-alpha");
        System.setProperty(PRIORITY, "special-priority.csv");

        assertThrows(IllegalStateException.class, DiagnosticOutputPaths::resolve);
    }

    @Test
    public void blankWorkerNamespacePartFailsFastInsteadOfSilentlyDisablingDiagnostics() {
        System.setProperty(ROOT, "");

        assertThrows(IllegalStateException.class, DiagnosticOutputPaths::resolve);
    }

    @Test
    public void invalidWorkerIdentityAndTraversalRunIdsFailFast() {
        System.setProperty(ROOT, "audit-root");
        System.setProperty(RUN_ID, "../shared");
        System.setProperty(WORKER_ID, "0");
        assertThrows(IllegalArgumentException.class, DiagnosticOutputPaths::resolve);

        System.setProperty(RUN_ID, "run-alpha");
        System.setProperty(WORKER_ID, "-1");
        assertThrows(IllegalArgumentException.class, DiagnosticOutputPaths::resolve);
    }

    @Test
    public void differentWorkerIdsHaveDisjointDerivedPaths() {
        System.setProperty(ROOT, "audit-root");
        System.setProperty(RUN_ID, "run-alpha");
        System.setProperty(WORKER_ID, "0");
        final DiagnosticOutputPaths.Resolved worker0 = DiagnosticOutputPaths.resolve();
        System.setProperty(WORKER_ID, "1");
        final DiagnosticOutputPaths.Resolved worker1 = DiagnosticOutputPaths.resolve();

        assertFalse(worker0.allOutputPaths().stream().anyMatch(worker1.allOutputPaths()::contains));
    }

    @Test
    public void noConfigurationLeavesAllDiagnosticsDisabled() {
        final DiagnosticOutputPaths.Resolved paths = DiagnosticOutputPaths.resolve();

        assertFalse(paths.workerDirectory().isPresent());
        assertFalse(paths.priorityMetricsFile().isPresent());
        assertFalse(paths.mulliganMetricsFile().isPresent());
        assertFalse(paths.determinismTraceDirectory().isPresent());
    }
}
