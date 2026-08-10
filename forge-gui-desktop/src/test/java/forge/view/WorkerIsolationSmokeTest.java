package forge.view;

import forge.game.decision.DiagnosticOutputPaths;
import forge.game.decision.DeterminismTrace;
import forge.game.decision.DeterminismTraceHasher;
import forge.game.decision.PriorityReferenceProjection;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class WorkerIsolationSmokeTest {

    @Test(timeOut = 300_000L)
    public void simultaneousSameSeedWorkersHaveDisjointFilesAndIdenticalCanonicalTraces() throws Exception {
        final Path root = Files.createTempDirectory("frl02k0-workers-");
        try {
            final String runId = "same-seed-smoke";
            final Child worker0 = start(root, runId, 0);
            final Child worker1 = start(root, runId, 1);
            worker0.await();
            worker1.await();

            final WorkerResult result0 = WorkerResult.read(root.resolve(runId).resolve("worker-000"));
            final WorkerResult result1 = WorkerResult.read(root.resolve(runId).resolve("worker-001"));
            assertTrue(result0.paths.stream().noneMatch(result1.paths::contains), "worker paths must be disjoint");
            assertEquals(result0.priorityProcessIds, Set.of(Long.toString(worker0.process.pid())));
            assertEquals(result0.mulliganProcessIds, Set.of(Long.toString(worker0.process.pid())));
            assertEquals(result1.priorityProcessIds, Set.of(Long.toString(worker1.process.pid())));
            assertEquals(result1.mulliganProcessIds, Set.of(Long.toString(worker1.process.pid())));
            assertEquals(result0.gameplay, result1.gameplay, "GAMEPLAY_TRACE_V1 bytes");
            assertEquals(result0.rng, result1.rng, "RNG_TRACE_V1 bytes");
            assertEquals(result0.decision, result1.decision, "DECISION_TRACE_V2 bytes");
            assertEquals(result0.priorityProjection, result1.priorityProjection,
                    "PRIORITY_REFERENCE_V1 records");
            assertEquals(result0.parseErrors, 0);
            assertEquals(result1.parseErrors, 0);
            result0.printEvidence(runId, 0, worker0);
            result1.printEvidence(runId, 1, worker1);
            System.out.println("FRL02K0_WORKER_EQUALITY gameplay=IDENTICAL rng=IDENTICAL"
                    + " decision=IDENTICAL priority=IDENTICAL collisions=0 parse_errors=0");
        } finally {
            deleteTree(root);
        }
    }

    private static Child start(final Path root, final String runId, final int workerId) throws IOException {
        final List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString());
        command.add("-D" + DiagnosticOutputPaths.OUTPUT_ROOT_PROPERTY + "=" + root);
        command.add("-D" + DiagnosticOutputPaths.RUN_ID_PROPERTY + "=" + runId);
        command.add("-D" + DiagnosticOutputPaths.WORKER_ID_PROPERTY + "=" + workerId);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("forge.view.Main");
        command.add("sim");
        command.add("-d");
        command.add("Izzet Guild Kit");
        command.add("Dimir Guild Kit");
        command.add("-n");
        command.add("1");
        command.add("-s");
        command.add("20260810");
        command.add("-q");
        final Path console = root.resolve(runId).resolve(String.format("worker-%03d.console.log", workerId));
        Files.createDirectories(console.getParent());
        final Process process = new ProcessBuilder(command).directory(repositoryRoot().resolve("forge-gui").toFile())
                .redirectErrorStream(true).redirectOutput(console.toFile()).start();
        return new Child(process, console);
    }

    private static Path repositoryRoot() {
        final Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        return workingDirectory.getFileName().toString().equals("forge-gui-desktop")
                ? workingDirectory.getParent() : workingDirectory;
    }

    private static void deleteTree(final Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static final class Child {
        private final Process process;
        private final Path console;

        private Child(final Process process, final Path console) {
            this.process = process;
            this.console = console;
        }

        private void await() throws Exception {
            assertTrue(process.waitFor(180, TimeUnit.SECONDS), "worker JVM timed out: " + process.pid());
            assertEquals(process.exitValue(), 0, Files.readString(console, StandardCharsets.UTF_8));
        }
    }

    private static final class WorkerResult {
        private final Path workerDirectory;
        private final Set<Path> paths;
        private final List<String> gameplay;
        private final List<String> rng;
        private final List<String> decision;
        private final List<String> priorityProjection;
        private final Set<String> priorityProcessIds;
        private final Set<String> mulliganProcessIds;
        private final int parseErrors;

        private WorkerResult(final Path workerDirectory, final Set<Path> paths,
                final List<String> gameplay, final List<String> rng,
                final List<String> decision, final List<String> priorityProjection,
                final Set<String> priorityProcessIds, final Set<String> mulliganProcessIds,
                final int parseErrors) {
            this.workerDirectory = workerDirectory;
            this.paths = paths;
            this.gameplay = gameplay;
            this.rng = rng;
            this.decision = decision;
            this.priorityProjection = priorityProjection;
            this.priorityProcessIds = priorityProcessIds;
            this.mulliganProcessIds = mulliganProcessIds;
            this.parseErrors = parseErrors;
        }

        private static WorkerResult read(final Path worker) throws IOException {
            final Path determinism = worker.resolve("determinism");
            final Path priority = worker.resolve("priority.csv");
            final Path mulligan = worker.resolve("mulligan.csv");
            assertTrue(Files.exists(priority));
            assertTrue(Files.exists(mulligan));
            assertTrue(Files.isDirectory(determinism));
            int parseErrors = 0;
            List<String> priorityProjection = List.of();
            Set<String> priorityProcessIds = Set.of();
            Set<String> mulliganProcessIds = Set.of();
            try {
                priorityProjection = PriorityReferenceProjection.readAndProject(priority);
                PriorityReferenceProjection.readAndProject(mulligan);
                priorityProcessIds = PriorityReferenceProjection.readColumnValues(priority, "process_id");
                mulliganProcessIds = PriorityReferenceProjection.readColumnValues(mulligan, "process_id");
            } catch (final IllegalArgumentException exception) {
                parseErrors++;
            }
            final Set<Path> paths = new HashSet<>();
            try (var produced = Files.walk(worker)) {
                produced.filter(Files::isRegularFile).map(Path::toAbsolutePath).forEach(paths::add);
            }
            return new WorkerResult(worker, paths,
                    Files.readAllLines(determinism.resolve("game-001.gameplay.trace"), StandardCharsets.UTF_8),
                    Files.readAllLines(determinism.resolve("game-001.rng.trace"), StandardCharsets.UTF_8),
                    Files.readAllLines(determinism.resolve("game-001.decision.trace"), StandardCharsets.UTF_8),
                    priorityProjection, priorityProcessIds, mulliganProcessIds, parseErrors);
        }

        private void printEvidence(final String runId, final int workerId, final Child child) {
            final List<String> producedFiles = paths.stream().sorted()
                    .map(workerDirectory::relativize).map(Path::toString).toList();
            System.out.println("FRL02K0_WORKER run_id=" + runId
                    + " worker_id=" + workerId
                    + " pid=" + child.process.pid()
                    + " exit=" + child.process.exitValue()
                    + " path=" + workerDirectory
                    + " files=" + producedFiles
                    + " gameplay_version=" + DeterminismTrace.GAMEPLAY_TRACE_VERSION
                    + " gameplay_hash=" + DeterminismTraceHasher.sha256(gameplay)
                    + " rng_version=" + DeterminismTrace.RNG_TRACE_VERSION
                    + " rng_hash=" + DeterminismTraceHasher.sha256(rng)
                    + " decision_version=" + DeterminismTrace.DECISION_TRACE_VERSION
                    + " decision_hash=" + DeterminismTraceHasher.sha256(decision)
                    + " priority_version=" + PriorityReferenceProjection.VERSION
                    + " priority_hash=" + PriorityReferenceProjection.hash(priorityProjection));
        }
    }
}
