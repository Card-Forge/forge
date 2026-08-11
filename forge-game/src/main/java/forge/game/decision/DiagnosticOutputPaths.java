package forge.game.decision;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/** Resolves explicit and worker-scoped diagnostic output locations. */
public final class DiagnosticOutputPaths {
    public static final String OUTPUT_ROOT_PROPERTY = "forge.diagnostics.outputRoot";
    public static final String RUN_ID_PROPERTY = "forge.diagnostics.runId";
    public static final String WORKER_ID_PROPERTY = "forge.diagnostics.workerId";
    public static final String PRIORITY_FILE_PROPERTY = "forge.priority.metricsFile";
    public static final String MULLIGAN_FILE_PROPERTY = "forge.mulligan.metricsFile";
    public static final String CONFIRMATION_FILE_PROPERTY = "forge.confirmation.metricsFile";
    public static final String DETERMINISM_DIRECTORY_PROPERTY = "forge.determinism.traceDir";

    private DiagnosticOutputPaths() {
    }

    public static Resolved resolve() {
        final String outputRoot = System.getProperty(OUTPUT_ROOT_PROPERTY);
        final String runId = System.getProperty(RUN_ID_PROPERTY);
        final String workerId = System.getProperty(WORKER_ID_PROPERTY);
        final boolean workerConfigurationPresent = outputRoot != null || runId != null || workerId != null;
        final int configuredWorkerParts = countNonBlank(outputRoot, runId, workerId);
        if (workerConfigurationPresent && configuredWorkerParts != 3) {
            throw new IllegalStateException("Worker diagnostics require outputRoot, runId, and workerId");
        }

        final Path workerDirectory;
        if (configuredWorkerParts == 3) {
            validateRunId(runId);
            final int worker = parseWorkerId(workerId);
            workerDirectory = Path.of(outputRoot, runId, String.format("worker-%03d", worker));
        } else {
            workerDirectory = null;
        }

        return new Resolved(
                optionalPath(System.getProperty(PRIORITY_FILE_PROPERTY), workerDirectory, "priority.csv"),
                optionalPath(System.getProperty(MULLIGAN_FILE_PROPERTY), workerDirectory, "mulligan.csv"),
                optionalPath(System.getProperty(CONFIRMATION_FILE_PROPERTY), workerDirectory, "confirmation.csv"),
                optionalPath(System.getProperty(DETERMINISM_DIRECTORY_PROPERTY), workerDirectory, "determinism"),
                Optional.ofNullable(workerDirectory));
    }

    private static int countNonBlank(final String... values) {
        int count = 0;
        for (final String value : values) {
            if (value != null && !value.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private static void validateRunId(final String runId) {
        if (!runId.matches("[A-Za-z0-9][A-Za-z0-9._-]*") || runId.contains("..")) {
            throw new IllegalArgumentException("Invalid diagnostics runId: " + runId);
        }
    }

    private static int parseWorkerId(final String workerId) {
        final int parsed;
        try {
            parsed = Integer.parseInt(workerId);
        } catch (final NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid diagnostics workerId: " + workerId, exception);
        }
        if (parsed < 0) {
            throw new IllegalArgumentException("Diagnostics workerId must be non-negative: " + workerId);
        }
        return parsed;
    }

    private static Optional<Path> optionalPath(final String explicitPath, final Path workerDirectory,
            final String derivedName) {
        if (explicitPath != null && !explicitPath.isBlank()) {
            return Optional.of(Path.of(explicitPath));
        }
        return workerDirectory == null ? Optional.empty() : Optional.of(workerDirectory.resolve(derivedName));
    }

    public static final class Resolved {
        private final Optional<Path> priorityMetricsFile;
        private final Optional<Path> mulliganMetricsFile;
        private final Optional<Path> confirmationMetricsFile;
        private final Optional<Path> determinismTraceDirectory;
        private final Optional<Path> workerDirectory;

        private Resolved(final Optional<Path> priorityMetricsFile, final Optional<Path> mulliganMetricsFile,
                final Optional<Path> confirmationMetricsFile,
                final Optional<Path> determinismTraceDirectory, final Optional<Path> workerDirectory) {
            this.priorityMetricsFile = priorityMetricsFile;
            this.mulliganMetricsFile = mulliganMetricsFile;
            this.confirmationMetricsFile = confirmationMetricsFile;
            this.determinismTraceDirectory = determinismTraceDirectory;
            this.workerDirectory = workerDirectory;
        }

        public Optional<Path> priorityMetricsFile() {
            return priorityMetricsFile;
        }

        public Optional<Path> mulliganMetricsFile() {
            return mulliganMetricsFile;
        }

        public Optional<Path> confirmationMetricsFile() {
            return confirmationMetricsFile;
        }

        public Optional<Path> determinismTraceDirectory() {
            return determinismTraceDirectory;
        }

        public Optional<Path> workerDirectory() {
            return workerDirectory;
        }

        public Set<Path> allOutputPaths() {
            final Set<Path> paths = new LinkedHashSet<>();
            priorityMetricsFile.ifPresent(paths::add);
            mulliganMetricsFile.ifPresent(paths::add);
            confirmationMetricsFile.ifPresent(paths::add);
            determinismTraceDirectory.ifPresent(paths::add);
            return paths;
        }
    }
}
