package forge.view;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/** Fresh-JVM audit for FRL-02K-C2 triggered target ownership and timing. */
public class FRL02KTriggeredTargetOwnershipAuditTest {
    private static final String AUDIT_PROPERTY = "forge.triggeredTarget.auditFile";

    @Test(timeOut = 900_000L)
    public void canonicalWorkloadCorrelatesTriggeredTargetOwnershipWithoutMutation() throws Exception {
        final Path root = Files.createTempDirectory("frl02k-c2-audit-");
        try {
            final AuditRun audit = run(root, "audit", true);
            assertTrue(Files.exists(audit.triggeredTargetMetrics),
                    "enabled C2 audit workload must produce triggered-target metrics");
            final List<AuditRow> rows = readRows(audit.triggeredTargetMetrics);
            assertProjectionSafety(rows);
            assertLifecycleOrdering(rows);

            final AuditRun control = run(root, "control", false);
            assertEquals(hashTree(audit.determinism), hashTree(control.determinism),
                    "C2 audit instrumentation must not change deterministic reactive traces");
        } finally {
            deleteTree(root);
        }
    }

    private static void assertProjectionSafety(final List<AuditRow> rows) {
        final String text = rows.stream().map(AuditRow::toString).reduce("", (left, right) -> left + right);
        assertFalse(text.matches("(?s).*\\b(?:SpellAbility|CardLKI|WrappedAbility|GameEntity)@[0-9a-fA-F]+.*"),
                "C2 audit must not expose raw engine object values");
        assertFalse(text.contains("Localized["), "C2 audit must not expose localized prompts");
        final Set<String> events = Set.of("TRIGGER_CONSTRUCTED", "TRIGGER_QUEUED", "TARGET_PREPARATION",
                "TARGET_STORED", "STACK_BEFORE_PUSH", "STACK_AFTER_PUSH", "RESOLVE_ENTER",
                "CONFIRM_TRIGGER_ENTER", "TARGET_A_BEFORE_CONFIRM", "TARGET_B_EVALUATION",
                "CONFIRM_TRIGGER_RESULT", "EFFECT_ENTER", "EFFECT_EXIT", "RESOLVE_EXIT");
        for (final AuditRow row : rows) {
            assertEquals(row.get("profile"), "BLOOD_OPERATIVE");
            assertTrue(events.contains(row.get("event")), "unexpected C2 event " + row.get("event"));
            assertEquals(row.get("action_continuation"), "false",
                    "triggered target preparation must not run under ActionContinuation");
            assertEquals(row.get("state_neutral"), "true", "C2 observations must be state-neutral");
            assertEquals(row.get("rng_delta"), "0", "C2 observations must not consume RNG");
            assertTrue(Set.of("NONE", "PUBLIC", "HIDDEN").contains(row.get("target_visibility")),
                    "target visibility must be typed");
            if (Integer.parseInt(row.get("target_count")) > 0) {
                assertEquals(row.get("target_kinds"), "TARGET_CARD");
                assertTrue(Set.of("Graveyard", "Exile").contains(row.get("target_zone")),
                        "Blood target zone must remain typed");
                assertEquals(row.get("target_visibility"), "PUBLIC");
            }
        }
    }

    private static void assertLifecycleOrdering(final List<AuditRow> rows) {
        final Map<Long, List<AuditRow>> byToken = new LinkedHashMap<>();
        for (final AuditRow row : rows) {
            byToken.computeIfAbsent(Long.parseLong(row.get("token")), ignored -> new ArrayList<>()).add(row);
        }
        assertEquals(byToken.size(), 2, "the canonical workload must expose two Blood occurrences");

        int storedMatchesB = 0;
        int storedDiffersFromB = 0;
        int accepted = 0;
        for (final List<AuditRow> lifecycle : byToken.values()) {
            final List<String> events = lifecycle.stream().map(row -> row.get("event")).toList();
            assertEquals(events.get(0), "TRIGGER_CONSTRUCTED");
            assertEquals(events.get(events.size() - 1), "RESOLVE_EXIT");
            assertEquals(count(events, "TARGET_STORED"), 2,
                    "the second stored target must come from the temporary B evaluation");
            assertEquals(count(events, "TARGET_A_BEFORE_CONFIRM"), 1);
            assertEquals(count(events, "TARGET_B_EVALUATION"), 1);
            assertEquals(count(events, "CONFIRM_TRIGGER_RESULT"), 1);
            assertOrdered(events, "TRIGGER_CONSTRUCTED", "TRIGGER_QUEUED", "TARGET_STORED",
                    "TARGET_PREPARATION", "STACK_BEFORE_PUSH", "STACK_AFTER_PUSH", "RESOLVE_ENTER",
                    "CONFIRM_TRIGGER_ENTER", "TARGET_A_BEFORE_CONFIRM", "TARGET_B_EVALUATION",
                    "CONFIRM_TRIGGER_RESULT", "RESOLVE_EXIT");

            final AuditRow targetAAtPreparation = first(lifecycle, "TARGET_STORED");
            final AuditRow targetAAtConfirmation = first(lifecycle, "TARGET_A_BEFORE_CONFIRM");
            final AuditRow targetB = first(lifecycle, "TARGET_B_EVALUATION");
            assertEquals(targetAAtPreparation.get("target_count"), "1");
            assertEquals(targetAAtConfirmation.get("target_count"), "1");
            assertEquals(targetAAtPreparation.get("target_order"), targetAAtConfirmation.get("target_order"));
            assertEquals(targetAAtPreparation.get("decider_seat"), targetAAtPreparation.get("activating_player_seat"));
            assertEquals(targetAAtPreparation.get("targeting_player_seat"), "NONE",
                    "current AI trigger preparation does not expose a separate targeting-player seam");

            final boolean didConfirm = Boolean.parseBoolean(valueFor(lifecycle, "CONFIRM_TRIGGER_RESULT", "result"));
            if (!didConfirm) {
                assertEquals(count(events, "EFFECT_ENTER"), 0,
                        "a declined Blood trigger must not resolve its ChangeZone effect");
                continue;
            }
            accepted++;
            assertEquals(count(events, "EFFECT_ENTER"), 1);
            assertEquals(count(events, "EFFECT_EXIT"), 1);
            assertOrdered(events, "CONFIRM_TRIGGER_RESULT", "EFFECT_ENTER", "EFFECT_EXIT", "RESOLVE_EXIT");
            final AuditRow effect = first(lifecycle, "EFFECT_ENTER");
            assertEquals(effect.get("target_count"), "1");
            assertEquals(effect.get("target_zone"), "Graveyard");
            assertEquals(targetProjection(targetAAtConfirmation), targetProjection(effect),
                    "ChangeZoneEffect must consume the stack-time target A");
            if (targetProjection(targetAAtConfirmation).equals(targetProjection(targetB))) {
                storedMatchesB++;
            } else {
                storedDiffersFromB++;
            }
        }
        assertEquals(accepted, 2, "both canonical Blood triggers must reach the effect in this workload");
        assertEquals(storedMatchesB, 1,
                "one canonical Blood occurrence must retain the same target in A and B");
        assertEquals(storedDiffersFromB, 1,
                "one canonical Blood occurrence must diverge between stored A and temporary B");
    }

    private static String targetProjection(final AuditRow row) {
        return row.get("target_count") + "|" + row.get("target_values") + "|" + row.get("target_order");
    }

    private static void assertOrdered(final List<String> events, final String... ordered) {
        int previous = -1;
        for (final String event : ordered) {
            final int current = events.indexOf(event);
            assertTrue(current >= 0, "missing event " + event + " in " + events);
            assertTrue(current > previous, "event order violated by " + event + " in " + events);
            previous = current;
        }
    }

    private static int count(final List<String> events, final String event) {
        return (int) events.stream().filter(event::equals).count();
    }

    private static String valueFor(final List<AuditRow> rows, final String event, final String column) {
        return first(rows, event).get(column);
    }

    private static AuditRow first(final List<AuditRow> rows, final String event) {
        return rows.stream().filter(row -> event.equals(row.get("event"))).findFirst()
                .orElseThrow(() -> new AssertionError("missing event " + event + " in " + rows));
    }

    private static AuditRun run(final Path root, final String name, final boolean auditEnabled) throws Exception {
        final Path run = root.resolve(name);
        Files.createDirectories(run);
        final Path audit = run.resolve("triggered-target.csv");
        final Path trace = run.resolve("determinism");
        final Path console = run.resolve("console.log");
        final List<String> command = new ArrayList<>();
        final String java = ChildJvmSupport.javaExecutable().toString();
        command.add(java);
        if (auditEnabled) {
            command.add("-D" + AUDIT_PROPERTY + "=" + audit);
        }
        command.add("-Dforge.determinism.traceDir=" + trace);
        command.add("-Dforge.determinism.auditRandom=true");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("forge.view.Main");
        command.add("sim");
        command.add("-d");
        command.add("Izzet Guild Kit");
        command.add("Dimir Guild Kit");
        command.add("-n");
        command.add("10");
        command.add("-s");
        command.add("20260810");
        command.add("-q");

        final Process process = new ProcessBuilder(command)
                .directory(repositoryRoot().resolve("forge-gui").toFile())
                .redirectErrorStream(true)
                .redirectOutput(console.toFile())
                .start();
        if (!process.waitFor(300, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("controlled C2 workload child JVM timed out: " + name);
        }
        assertEquals(process.exitValue(), 0, Files.readString(console, StandardCharsets.UTF_8));
        return new AuditRun(audit, trace);
    }

    private static List<AuditRow> readRows(final Path path) throws IOException {
        final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue(lines.size() > 1, "C2 audit must contain lifecycle records");
        final List<String> header = csv(lines.get(0));
        final Map<String, Integer> indexes = new HashMap<>();
        for (int index = 0; index < header.size(); index++) {
            indexes.put(header.get(index), index);
        }
        for (final String required : List.of("token", "sequence", "event", "target_order", "target_visibility",
                "target_kinds", "action_continuation", "state_neutral", "rng_delta")) {
            assertTrue(indexes.containsKey(required), "C2 audit header must contain " + required);
        }
        final List<AuditRow> rows = new ArrayList<>();
        for (final String line : lines.subList(1, lines.size())) {
            final List<String> columns = csv(line);
            assertEquals(columns.size(), header.size(), "C2 audit column count");
            rows.add(new AuditRow(columns, indexes));
        }
        return rows;
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

    private static Path repositoryRoot() {
        final Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        return workingDirectory.getFileName().toString().equals("forge-gui-desktop")
                ? workingDirectory.getParent() : workingDirectory;
    }

    private static String hashTree(final Path root) throws IOException, NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        if (!Files.exists(root)) {
            return "";
        }
        try (var paths = Files.walk(root)) {
            for (final Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                digest.update(root.relativize(path).toString().getBytes(StandardCharsets.UTF_8));
                digest.update(Files.readAllBytes(path));
            }
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
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

    private record AuditRun(Path triggeredTargetMetrics, Path determinism) {
    }

    private static final class AuditRow {
        private final List<String> columns;
        private final Map<String, Integer> indexes;

        private AuditRow(final List<String> columns, final Map<String, Integer> indexes) {
            this.columns = columns;
            this.indexes = indexes;
        }

        private String get(final String name) {
            return columns.get(indexes.get(name));
        }

        @Override
        public String toString() {
            return String.join(",", columns);
        }
    }
}
