package forge.view;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** Fresh-JVM regression for the exact FRL-02K-B1 controlled workloads. */
public class GelectrodeConfirmationWorkloadTest {

    @Test(timeOut = 600_000L)
    public void canonicalWorkloadsPreserveTheExactSeventeenOfTwentySixBoundary() throws Exception {
        final Path root = Files.createTempDirectory("frl02k-b1-workload-");
        try {
            final Metrics reactive = run(root, "reactive", "Izzet Guild Kit", "Dimir Guild Kit", 10, 20260810L);
            assertEquals(reactive.callbacks, 26, "reactive raw trigger callbacks");
            assertEquals(reactive.statusCounts.getOrDefault("ADMITTED", 0), 19,
                    "admitted Gelectrode plus Blood callbacks");
            assertEquals(reactive.profileStatusCounts.getOrDefault(
                    "GELECTRODE_SPELL_CAST_UNTAP_SELF|ADMITTED", 0), 17,
                    "admitted Gelectrode callbacks");
            assertEquals(reactive.profileStatusCounts.getOrDefault(
                    "BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD|ADMITTED", 0), 2,
                    "admitted Blood ETB callbacks");
            assertEquals(reactive.statusCounts.getOrDefault("UNSUPPORTED_PROFILE", 0), 3,
                    "other normal optional profiles");
            assertEquals(reactive.statusCounts.getOrDefault("UNSUPPORTED_COST", 0), 1,
                    "cost-bearing callback");
            assertEquals(reactive.statusCounts.getOrDefault("UNSUPPORTED_PROVENANCE", 0), 3,
                    "provenance-untrusted callbacks");
            assertEquals(reactive.results, 19, "one result per admitted callback");
            assertEquals(reactive.candidateCounts.getOrDefault("ACCEPT", 0)
                    + reactive.candidateCounts.getOrDefault("DECLINE", 0), 19);
            assertEquals(reactive.confirmationRequests, 19, "DECISION_TRACE_V2 confirmation requests");
            assertEquals(reactive.confirmationResults, 19, "DECISION_TRACE_V2 confirmation results");

            final Metrics proactive = run(root, "proactive", "Dead and Alive", "Air Forces", 10, 20260809L);
            assertEquals(proactive.callbacks, 0, "proactive raw trigger callbacks");
            assertEquals(proactive.results, 0, "proactive confirmation results");
            assertEquals(proactive.confirmationRequests, 0, "proactive confirmation requests");
        } finally {
            deleteTree(root);
        }
    }

    private static Metrics run(final Path root, final String name, final String firstDeck, final String secondDeck,
            final int games, final long seed) throws Exception {
        final Path run = root.resolve(name);
        Files.createDirectories(run);
        final Path metrics = run.resolve("confirmation.csv");
        final Path trace = run.resolve("determinism");
        final Path console = run.resolve("console.log");
        final List<String> command = new ArrayList<>();
        command.add(ChildJvmSupport.javaExecutable().toString());
        command.add("-Dforge.confirmation.metricsFile=" + metrics);
        command.add("-Dforge.determinism.traceDir=" + trace);
        command.add("-Dforge.determinism.auditRandom=true");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("forge.view.Main");
        command.add("sim");
        command.add("-d");
        command.add(firstDeck);
        command.add(secondDeck);
        command.add("-n");
        command.add(Integer.toString(games));
        command.add("-s");
        command.add(Long.toString(seed));
        command.add("-q");

        final Process process = new ProcessBuilder(command)
                .directory(repositoryRoot().resolve("forge-gui").toFile())
                .redirectErrorStream(true)
                .redirectOutput(console.toFile())
                .start();
        assertTrue(process.waitFor(300, TimeUnit.SECONDS), "controlled workload child JVM timed out");
        assertEquals(process.exitValue(), 0, Files.readString(console, StandardCharsets.UTF_8));
        return Metrics.read(metrics, trace);
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

    private static final class Metrics {
        private final int callbacks;
        private final int results;
        private final Map<String, Integer> statusCounts;
        private final Map<String, Integer> profileStatusCounts;
        private final Map<String, Integer> candidateCounts;
        private final int confirmationRequests;
        private final int confirmationResults;

        private Metrics(final int callbacks, final int results, final Map<String, Integer> statusCounts,
                final Map<String, Integer> profileStatusCounts0,
                final Map<String, Integer> candidateCounts, final int confirmationRequests,
                final int confirmationResults) {
            this.callbacks = callbacks;
            this.results = results;
            this.statusCounts = statusCounts;
            this.profileStatusCounts = profileStatusCounts0;
            this.candidateCounts = candidateCounts;
            this.confirmationRequests = confirmationRequests;
            this.confirmationResults = confirmationResults;
        }

        private static Metrics read(final Path metrics, final Path trace) throws IOException {
            int callbacks = 0;
            int results = 0;
            final Map<String, Integer> statuses = new HashMap<>();
            final Map<String, Integer> profileStatuses = new HashMap<>();
            final Map<String, Integer> candidates = new HashMap<>();
            if (Files.exists(metrics)) {
                final List<String> lines = Files.readAllLines(metrics, StandardCharsets.UTF_8);
                for (final String line : lines.subList(1, lines.size())) {
                    final List<String> columns = csv(line);
                    if ("CALLBACK".equals(columns.get(0))) {
                        callbacks++;
                        statuses.merge(columns.get(6), 1, Integer::sum);
                        final String profile = columns.size() > 16 ? columns.get(16) : "";
                        profileStatuses.merge(profile + "|" + columns.get(6), 1, Integer::sum);
                    } else if ("RESULT".equals(columns.get(0))) {
                        results++;
                        candidates.merge(columns.get(11), 1, Integer::sum);
                    }
                }
            }
            int confirmationRequests = 0;
            int confirmationResults = 0;
            if (Files.exists(trace)) {
                try (var files = Files.walk(trace)) {
                    for (final Path file : files.filter(value -> value.getFileName().toString().endsWith(".decision.trace"))
                            .toList()) {
                        for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                            if (line.contains("|REQUEST|") && line.contains("|CONFIRMATION|")) {
                                confirmationRequests++;
                            }
                            if (line.contains("|RESULT|") && (line.contains("|CHOSEN|ACCEPT|")
                                    || line.contains("|CHOSEN|DECLINE|"))) {
                                confirmationResults++;
                            }
                        }
                    }
                }
            }
            return new Metrics(callbacks, results, statuses, profileStatuses, candidates,
                    confirmationRequests, confirmationResults);
        }

        private static List<String> csv(final String line) {
            final List<String> columns = new ArrayList<>();
            final StringBuilder value = new StringBuilder();
            boolean quoted = false;
            for (int index = 0; index < line.length(); index++) {
                final char character = line.charAt(index);
                if (character == '"') {
                    if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                        value.append('"');
                        index++;
                    } else {
                        quoted = !quoted;
                    }
                } else if (character == ',' && !quoted) {
                    columns.add(value.toString());
                    value.setLength(0);
                } else {
                    value.append(character);
                }
            }
            columns.add(value.toString());
            return columns;
        }
    }
}
