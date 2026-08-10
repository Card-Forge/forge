package forge.view;

import forge.game.decision.DeterminismTrace;
import forge.game.decision.PriorityReferenceProjection;
import forge.game.decision.ReferenceGameplayObserver;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FullGameCollectorNeutralityTest {

    @Test(timeOut = 600_000L)
    public void fullGameCollectorOffAndOnAreExactlyEqualThroughIndependentChannels() throws Exception {
        final Path root = Files.createTempDirectory("frl02k0-collector-full-game-");
        try {
            final RunResult offA = run(root, "off-a", false);
            final RunResult offB = run(root, "off-b", false);
            final RunResult onA = run(root, "on-a", true);
            final RunResult onB = run(root, "on-b", true);

            offA.printEvidence();
            offB.printEvidence();
            onA.printEvidence();
            onB.printEvidence();

            assertEquivalent(offA, offB);
            assertEquivalent(onA, onB);
            assertEquivalent(offA, onA);
            assertEquivalent(offB, onB);
        } finally {
            deleteTree(root);
        }
    }

    private static RunResult run(final Path root, final String name, final boolean collectorOn) throws Exception {
        final Path run = root.resolve(name);
        final Path reference = run.resolve("reference");
        final Path priority = run.resolve("priority.csv");
        final Path console = run.resolve("console.log");
        Files.createDirectories(run);
        final List<String> command = new ArrayList<>();
        command.add(ChildJvmSupport.javaExecutable().toString());
        command.add("-D" + DeterminismTrace.AUDIT_RANDOM_PROPERTY + "=true");
        command.add("-D" + ReferenceGameplayObserver.OUTPUT_DIRECTORY_PROPERTY + "=" + reference);
        command.add("-Dforge.priority.metricsFile=" + priority);
        if (collectorOn) {
            command.add("-D" + DeterminismTrace.OUTPUT_DIRECTORY_PROPERTY + "=" + run.resolve("determinism"));
        }
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

        final Process process = new ProcessBuilder(command)
                .directory(repositoryRoot().resolve("forge-gui").toFile()).redirectErrorStream(true)
                .redirectOutput(console.toFile()).start();
        assertTrue(process.waitFor(180, TimeUnit.SECONDS), "controlled full-game child JVM timed out");
        assertEquals(process.exitValue(), 0, Files.readString(console, StandardCharsets.UTF_8));
        return RunResult.read(name, run, reference, priority, process.pid());
    }

    private static Path repositoryRoot() {
        final Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        return workingDirectory.getFileName().toString().equals("forge-gui-desktop")
                ? workingDirectory.getParent() : workingDirectory;
    }

    private static void assertEquivalent(final RunResult left, final RunResult right) {
        assertEquals(left.referenceRecords, right.referenceRecords, "reference gameplay records");
        assertEquals(left.referenceHash, right.referenceHash, "reference gameplay hash");
        assertEquals(left.priorityRecords, right.priorityRecords, "priority semantic records");
        assertEquals(left.priorityHash, right.priorityHash, "priority semantic hash");
        assertEquals(left.rngRecords, right.rngRecords, "independent RNG records");
        assertEquals(left.rngHash, right.rngHash, "independent RNG hash");
        assertEquals(left.rngDrawCount, right.rngDrawCount, "independent RNG draw count");
        assertEquals(left.finalStateHash, right.finalStateHash, "final FORGE_STATE_V1 hash");
        assertEquals(left.outcome, right.outcome, "semantic outcome");
    }

    private static Map<String, String> properties(final Path file) throws IOException {
        final Map<String, String> result = new LinkedHashMap<>();
        for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            final int separator = line.indexOf('=');
            if (separator > 0) {
                result.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
        return result;
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

    private static final class RunResult {
        private final String name;
        private final long processId;
        private final List<String> referenceRecords;
        private final String referenceHash;
        private final List<String> priorityRecords;
        private final String priorityHash;
        private final List<String> rngRecords;
        private final String rngHash;
        private final long rngDrawCount;
        private final String finalStateHash;
        private final String outcome;

        private RunResult(final String name, final long processId, final List<String> referenceRecords,
                final String referenceHash,
                final List<String> priorityRecords, final String priorityHash, final List<String> rngRecords,
                final String rngHash, final long rngDrawCount, final String finalStateHash, final String outcome) {
            this.name = name;
            this.processId = processId;
            this.referenceRecords = referenceRecords;
            this.referenceHash = referenceHash;
            this.priorityRecords = priorityRecords;
            this.priorityHash = priorityHash;
            this.rngRecords = rngRecords;
            this.rngHash = rngHash;
            this.rngDrawCount = rngDrawCount;
            this.finalStateHash = finalStateHash;
            this.outcome = outcome;
        }

        private static RunResult read(final String name, final Path run, final Path reference, final Path priority,
                final long processId) throws IOException {
            final List<String> referenceRecords = Files.readAllLines(reference.resolve("game-001.reference.trace"),
                    StandardCharsets.UTF_8);
            final Map<String, String> referenceSummary = properties(
                    reference.resolve("game-001.reference-summary.properties"));
            final List<String> priorityRecords = PriorityReferenceProjection.readAndProject(priority);
            final Path rngFile = reference.resolve("game-001.reference-rng.trace");
            final List<String> rngRecords = Files.readAllLines(rngFile, StandardCharsets.UTF_8);
            final Map<String, String> rngSummary = properties(
                    reference.resolve("game-001.reference-rng-summary.properties"));
            final String outcome = Files.readString(reference.resolve("game-001.reference-outcome.txt"),
                    StandardCharsets.UTF_8).trim();
            return new RunResult(name, processId, referenceRecords, referenceSummary.get("referenceGameplayHash"),
                    priorityRecords, PriorityReferenceProjection.hash(priorityRecords), rngRecords,
                    rngSummary.get("rngHash"), Long.parseLong(rngSummary.get("rngDrawCount")),
                    referenceSummary.get("finalStateHash"), outcome);
        }

        private void printEvidence() {
            System.out.println("FRL02K0_COLLECTOR_SELF_NEUTRALITY run=" + name
                    + " pid=" + processId
                    + " reference_version=" + ReferenceGameplayObserver.TRACE_VERSION
                    + " reference_hash=" + referenceHash
                    + " priority_version=" + PriorityReferenceProjection.VERSION
                    + " priority_hash=" + priorityHash
                    + " rng_version=" + DeterminismTrace.RNG_TRACE_VERSION
                    + " rng_hash=" + rngHash
                    + " rng_draws=" + rngDrawCount
                    + " final_state_hash=" + finalStateHash
                    + " outcome=" + outcome);
        }
    }
}
