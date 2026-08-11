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

/** Fresh-JVM audit for FRL-02K-C1 ChangesZone trigger projection boundaries. */
public class FRL02KChangesZoneProjectionAuditTest {
    private static final String AUDIT_PROPERTY = "forge.changesZone.auditFile";

    @Test(timeOut = 900_000L)
    public void canonicalWorkloadCorrelatesChangesZoneProjectionWithoutMutation() throws Exception {
        final Path root = Files.createTempDirectory("frl02k-c1-audit-");
        try {
            final AuditRun audit = run(root, "audit", true);
            assertTrue(Files.exists(audit.changesZoneMetrics), "C1 audit workload must produce ChangesZone metrics");
            final List<AuditRow> rows = readRows(audit.changesZoneMetrics);
            assertTrue(rows.size() >= 5 * 3,
                    "the five named trigger occurrences must have enter, result, and exit records");
            assertProjectionSafety(rows);
            assertSourceAndScriptShapes(rows);
            assertLifecycleOrdering(rows);

            final AuditRun control = run(root, "control", false);
            assertEquals(hashTree(audit.determinism), hashTree(control.determinism),
                    "C1 audit instrumentation must not change deterministic reactive traces");
        } finally {
            deleteTree(root);
        }
    }

    private static void assertProjectionSafety(final List<AuditRow> rows) throws IOException {
        final String text = rows.stream().map(AuditRow::toString).reduce("", (left, right) -> left + right);
        assertFalse(text.matches("(?s).*\\b(?:SpellAbility|CardLKI|WrappedAbility|GameEntity)@[0-9a-fA-F]+.*"),
                "C1 audit must not expose raw engine object values");
        assertFalse(text.contains("Localized["), "C1 audit must not expose localized prompts");
        for (final AuditRow row : rows) {
            assertEquals(row.get("raw_card_exported"), "false", "Card values must remain typed projections");
            assertEquals(row.get("raw_lki_exported"), "false", "CardLKI values must remain metadata-only");
            assertTrue(Set.of("Card", "CardLKI", "NONE").contains(row.get("decision_context_type")),
                    "decision context type must be explicit");
            assertTrue(Set.of("PUBLIC", "HIDDEN", "NONE").contains(row.get("decision_context_visibility")),
                    "decision context visibility must be typed");
            assertTrue(Set.of("PUBLIC", "HIDDEN", "NONE").contains(row.get("lki_visibility")),
                    "LKI visibility must be typed");
            assertTrue(Set.of("true", "false", "UNKNOWN").contains(row.get("hidden_at_decision")),
                    "hidden-at-decision state must be explicit");
            assertTrue(Set.of("true", "false").contains(row.get("previously_hidden")),
                    "previously-hidden state must be explicit");
            assertEquals(row.get("action_continuation"), "false",
                    "C1 must not run under an ActionContinuation");
            assertEquals(row.get("state_neutral"), "true", "audit observations must be state-neutral");
            assertEquals(row.get("rng_delta"), "0", "audit observations must not consume RNG");
            assertTrue(row.get("triggering_object_keys").contains("Card"),
                    "triggering Card projection must be typed");
            assertTrue(row.get("triggering_object_keys").contains("CardLKI"),
                    "triggering CardLKI projection must be typed");
            if ("TRIGGER_ENTER".equals(row.get("event"))) {
                assertEquals(row.get("source_visibility"), "PUBLIC",
                        "the canonical named sources must be public to the deciding AI");
                assertEquals(row.get("decision_context_visibility"), "PUBLIC",
                        "the canonical changed-card context must be public at decision time");
                assertEquals(row.get("hidden_at_decision"), "false",
                        "the canonical C1 occurrences must not manufacture hidden context");
            }
        }
    }

    private static void assertSourceAndScriptShapes(final List<AuditRow> rows) throws IOException {
        final Map<String, AuditRow> firstByProfile = new HashMap<>();
        for (final AuditRow row : rows) {
            if ("TRIGGER_ENTER".equals(row.get("event"))) {
                firstByProfile.putIfAbsent(row.get("profile"), row);
            }
        }
        assertEquals(firstByProfile.size(), 2, "both named C1 profiles must be observed");

        final AuditRow blood = firstByProfile.get("BLOOD_OPERATIVE");
        assertEquals(blood.get("source_name"), "Blood Operative");
        assertEquals(blood.get("trigger_mode"), "ChangesZone");
        assertEquals(blood.get("origin"), "Any");
        assertEquals(blood.get("destination"), "Battlefield");
        assertEquals(blood.get("valid_card"), "Card.Self");
        assertEquals(blood.get("execute"), "TrigChangeZone");
        assertEquals(blood.get("live_api"), "ChangeZone");
        assertEquals(blood.get("optional_trigger"), "true");
        assertEquals(blood.get("optional_effect"), "false");

        final AuditRow lazav = firstByProfile.get("LAZAV");
        assertEquals(lazav.get("source_name"), "Lazav, Dimir Mastermind");
        assertEquals(lazav.get("trigger_mode"), "ChangesZone");
        assertEquals(lazav.get("origin"), "Any");
        assertEquals(lazav.get("destination"), "Graveyard");
        assertEquals(lazav.get("valid_card"), "Creature.!token+OppOwn");
        assertEquals(lazav.get("execute"), "LazavCopy");
        assertEquals(lazav.get("live_api"), "Clone");
        assertEquals(lazav.get("optional_trigger"), "true");
        assertEquals(lazav.get("optional_effect"), "true");
        assertEquals(lazav.get("defined_context"), "TriggeredCardLKICopy");

        final Path root = repositoryRoot();
        final String bloodScript = Files.readString(root.resolve("forge-gui/res/cardsfolder/b/blood_operative.txt"));
        assertTrue(bloodScript.contains("Execute$ TrigChangeZone"), "Blood must use the audited ChangeZone effect");
        assertTrue(bloodScript.contains("ValidTgts$ Card"), "Blood downstream target must remain card-typed");
        final String lazavScript = Files.readString(root.resolve("forge-gui/res/cardsfolder/l/lazav_dimir_mastermind.txt"));
        assertTrue(lazavScript.contains("Execute$ LazavCopy"), "Lazav must use the audited Clone effect");
        assertTrue(lazavScript.contains("Defined$ TriggeredCardLKICopy"),
                "Lazav must consume the current triggered Card projection");
        assertTrue(lazavScript.contains("Optional$ True"), "Lazav clone action must remain optional");
    }

    private static void assertLifecycleOrdering(final List<AuditRow> rows) {
        final Map<Long, List<AuditRow>> byToken = new LinkedHashMap<>();
        for (final AuditRow row : rows) {
            byToken.computeIfAbsent(Long.parseLong(row.get("token")), ignored -> new ArrayList<>()).add(row);
        }
        assertEquals(byToken.size(), 5, "one deterministic token must identify each C1 occurrence");
        final Map<String, Integer> profileCounts = new HashMap<>();
        for (final List<AuditRow> lifecycle : byToken.values()) {
            final String profile = lifecycle.get(0).get("profile");
            profileCounts.merge(profile, 1, Integer::sum);
            final List<String> events = lifecycle.stream().map(row -> row.get("event")).toList();
            assertEquals(events.get(0), "TRIGGER_ENTER", "trigger occurrence must start the lifecycle");
            assertEquals(events.get(events.size() - 1), "TRIGGER_EXIT", "trigger occurrence must close the lifecycle");
            assertEquals(count(events, "CONFIRM_TRIGGER_RESULT"), 1, "each trigger must have one confirmTrigger result");
            assertOrdered(events, "TRIGGER_ENTER", "CONFIRM_TRIGGER_RESULT", "TRIGGER_EXIT");

            if ("BLOOD_OPERATIVE".equals(profile)) {
                assertEquals(count(events, "AI_TARGET_EVALUATION"), 1,
                        "Blood target evaluation must be observable before native confirmation returns");
                assertOrdered(events, "TRIGGER_ENTER", "AI_TARGET_EVALUATION", "CONFIRM_TRIGGER_RESULT");
                final boolean accepted = Boolean.parseBoolean(valueFor(lifecycle, "CONFIRM_TRIGGER_RESULT",
                        "trigger_result"));
                if (accepted) {
                    assertEquals(count(events, "CHANGE_ZONE_EFFECT_ENTER"), 1);
                    assertEquals(count(events, "CHANGE_ZONE_EFFECT_EXIT"), 1);
                    assertOrdered(events, "CONFIRM_TRIGGER_RESULT", "CHANGE_ZONE_EFFECT_ENTER",
                            "CHANGE_ZONE_EFFECT_EXIT", "TRIGGER_EXIT");
                } else {
                    assertEquals(count(events, "CHANGE_ZONE_EFFECT_ENTER"), 0,
                            "declined Blood triggers must not enter ChangeZoneEffect");
                }
            } else {
                final boolean accepted = Boolean.parseBoolean(valueFor(lifecycle, "CONFIRM_TRIGGER_RESULT",
                        "trigger_result"));
                if (accepted) {
                    assertEquals(count(events, "CLONE_EFFECT_ENTER"), 1);
                    assertEquals(count(events, "CLONE_EFFECT_EXIT"), 1);
                    assertEquals(count(events, "CLONE_CONFIRM_ACTION_ENTER"), 1);
                    assertEquals(count(events, "CLONE_CONFIRM_ACTION_RESULT"), 1);
                    assertOrdered(events, "CONFIRM_TRIGGER_RESULT", "CLONE_EFFECT_ENTER",
                            "CLONE_CONFIRM_ACTION_ENTER", "CLONE_CONFIRM_ACTION_RESULT", "CLONE_EFFECT_EXIT",
                            "TRIGGER_EXIT");
                    final boolean cloneAccepted = Boolean.parseBoolean(valueFor(lifecycle,
                            "CLONE_CONFIRM_ACTION_RESULT", "confirm_action_result"));
                    if (cloneAccepted) {
                        assertEquals(count(events, "CLONE_STATE_CHANGED"), 1,
                                "accepted Lazav Clone must record clone-state mutation");
                        assertOrdered(events, "CLONE_CONFIRM_ACTION_RESULT", "CLONE_STATE_CHANGED",
                                "CLONE_EFFECT_EXIT");
                        final AuditRow state = first(lifecycle, "CLONE_STATE_CHANGED");
                        assertTrue(Integer.parseInt(state.get("clone_state_after"))
                                > Integer.parseInt(state.get("clone_state_before")),
                                "clone state count must increase after accepted Clone");
                    } else {
                        assertEquals(count(events, "CLONE_STATE_CHANGED"), 0,
                                "declined Lazav Clone must not mutate clone state");
                    }
                } else {
                    assertEquals(count(events, "CLONE_EFFECT_ENTER"), 0,
                            "declined Lazav triggers must not enter CloneEffect");
                }
            }
            System.out.println("FRL02K_C1_LIFECYCLE token=" + lifecycle.get(0).get("token")
                    + " profile=" + profile + " events=" + String.join(">", events));
        }
        assertEquals(profileCounts.getOrDefault("BLOOD_OPERATIVE", 0), 2);
        assertEquals(profileCounts.getOrDefault("LAZAV", 0), 3);
    }

    private static String valueFor(final List<AuditRow> rows, final String event, final String column) {
        return first(rows, event).get(column);
    }

    private static AuditRow first(final List<AuditRow> rows, final String event) {
        return rows.stream().filter(row -> event.equals(row.get("event"))).findFirst()
                .orElseThrow(() -> new AssertionError("missing event " + event + " in " + rows));
    }

    private static int count(final List<String> events, final String event) {
        return (int) events.stream().filter(event::equals).count();
    }

    private static void assertOrdered(final List<String> events, final String... ordered) {
        int previous = -1;
        for (final String event : ordered) {
            final int current = events.indexOf(event);
            assertTrue(current >= 0, "missing lifecycle event " + event + " in " + events);
            assertTrue(current > previous, "lifecycle order violated by " + event + " in " + events);
            previous = current;
        }
    }

    private static AuditRun run(final Path root, final String name, final boolean auditEnabled) throws Exception {
        final Path run = root.resolve(name);
        Files.createDirectories(run);
        final Path changesZoneMetrics = run.resolve("changes-zone.csv");
        final Path booleanMetrics = run.resolve("boolean-callback.csv");
        final Path confirmationMetrics = run.resolve("confirmation.csv");
        final Path trace = run.resolve("determinism");
        final Path console = run.resolve("console.log");
        final List<String> command = new ArrayList<>();
        command.add(ChildJvmSupport.javaExecutable().toString());
        if (auditEnabled) {
            command.add("-D" + AUDIT_PROPERTY + "=" + changesZoneMetrics);
        }
        command.add("-Dforge.booleanCallback.metricsFile=" + booleanMetrics);
        command.add("-Dforge.confirmation.metricsFile=" + confirmationMetrics);
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
            fail("controlled workload child JVM timed out: " + name);
        }
        assertEquals(process.exitValue(), 0, Files.readString(console, StandardCharsets.UTF_8));
        return new AuditRun(changesZoneMetrics, trace);
    }

    private static List<AuditRow> readRows(final Path path) throws IOException {
        final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue(lines.size() > 1, "C1 audit must contain lifecycle records");
        final List<String> header = csv(lines.get(0));
        final Map<String, Integer> indexes = new HashMap<>();
        for (int index = 0; index < header.size(); index++) {
            indexes.put(header.get(index), index);
        }
        final List<AuditRow> rows = new ArrayList<>();
        for (final String line : lines.subList(1, lines.size())) {
            final List<String> columns = csv(line);
            assertEquals(columns.size(), header.size(), "C1 audit column count");
            rows.add(new AuditRow(columns, indexes));
        }
        return rows;
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

    private record AuditRun(Path changesZoneMetrics, Path determinism) {
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
