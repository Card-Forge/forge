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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/** Fresh-JVM attribution regression for the remaining boolean callback surfaces. */
public class FRL02KRemainingConfirmationAuditTest {
    private static final String BOOLEAN_METRICS_PROPERTY = "forge.booleanCallback.metricsFile";

    @Test(timeOut = 900_000L)
    public void canonicalWorkloadsReconcileBooleanCallbacksAndPreserveB1() throws Exception {
        final Path root = Files.createTempDirectory("frl02k-c-audit-");
        try {
            final AuditRun reactiveAudit = run(root, "reactive-audit", "Izzet Guild Kit", "Dimir Guild Kit", 10,
                    20260810L, true);
            assertTrue(Files.exists(reactiveAudit.booleanMetrics),
                    "audit-enabled workload must produce boolean callback metrics");
            assertReactiveB1(reactiveAudit);
            assertReactiveBooleanCounts(reactiveAudit);
            assertReactiveSemanticShapes(reactiveAudit);
            assertSafeProjection(reactiveAudit);
            printClusters(reactiveAudit);

            final AuditRun reactiveControl = run(root, "reactive-control", "Izzet Guild Kit", "Dimir Guild Kit", 10,
                    20260810L, false);
            assertEquals(hashTree(reactiveAudit.determinism), hashTree(reactiveControl.determinism),
                    "audit instrumentation must not change deterministic reactive traces");

            final AuditRun proactiveAudit = run(root, "proactive-audit", "Dead and Alive", "Air Forces", 10,
                    20260809L, true);
            assertEquals(proactiveAudit.confirmationCallbacks, 0, "proactive raw trigger callbacks");
            assertEquals(proactiveAudit.confirmationResults, 0, "proactive confirmation results");
            assertProactiveBooleanCounts(proactiveAudit);
            assertSafeProjection(proactiveAudit);
            printClusters(proactiveAudit);
        } finally {
            deleteTree(root);
        }
    }

    private static void assertReactiveB1(final AuditRun run) {
        assertEquals(run.confirmationCallbacks, 26, "reactive raw trigger callbacks");
        assertEquals(run.confirmationStatusCounts.getOrDefault("ADMITTED", 0), 19,
                "admitted B1 plus D1 callbacks");
        assertEquals(run.confirmationProfileStatusCounts.getOrDefault(
                "GELECTRODE_SPELL_CAST_UNTAP_SELF|ADMITTED", 0), 17,
                "admitted Gelectrode callbacks");
        assertEquals(run.confirmationProfileStatusCounts.getOrDefault(
                "BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD|ADMITTED", 0), 2,
                "admitted Blood ETB callbacks");
        assertEquals(run.confirmationStatusCounts.getOrDefault("UNSUPPORTED_PROFILE", 0), 3,
                "other normal optional profiles");
        assertEquals(run.confirmationStatusCounts.getOrDefault("UNSUPPORTED_COST", 0), 1,
                "cost-bearing callback");
        assertEquals(run.confirmationStatusCounts.getOrDefault("UNSUPPORTED_PROVENANCE", 0), 3,
                "provenance-untrusted callbacks");
        assertEquals(run.confirmationResults, 19, "one result per admitted callback");
    }

    private static void assertReactiveBooleanCounts(final AuditRun run) {
        assertFamilyCount(run, "confirmTrigger", 26);
        assertFamilyCount(run, "confirmAction", 8);
        assertFamilyCount(run, "chooseBinary", 2);
        assertFamilyCount(run, "payCostToPreventEffect", 5);
        assertFamilyCount(run, "confirmPayment", 0);
        assertFamilyCount(run, "confirmBidAction", 0);
        assertFamilyCount(run, "confirmReplacementEffect", 0);
        assertFamilyCount(run, "confirmStaticApplication", 0);
    }

    private static void assertReactiveSemanticShapes(final AuditRun run) {
        assertRows(run, "confirmTrigger", "Gelectrode", "Untap", "SpellCast", 17);
        assertRows(run, "confirmTrigger", "Lazav, Dimir Mastermind", "Clone", "ChangesZone", 3);
        assertRows(run, "confirmTrigger", "Blood Operative", "ChangeZone", "ChangesZone", 2);
        assertRows(run, "confirmTrigger", "Blood Operative", "ChangeZone", "Surveil", 1);
        assertRows(run, "confirmTrigger", "Nightveil Specter", "Play", "DamageDone", 1);
        assertRows(run, "confirmTrigger", "Tibor and Lumia", "Play", "DamageDone", 2);
        assertRows(run, "confirmAction", "Stolen Identity", "Encode", "", 4);
        assertRows(run, "confirmAction", "Call of the Nightwing", "Encode", "", 1);
        assertRows(run, "confirmAction", "Lazav, Dimir Mastermind", "Clone", "", 3);
        assertRows(run, "chooseBinary", "Stitch in Time", "FlipCoin", "", 2);
        assertRows(run, "payCostToPreventEffect", "Syncopate", "Counter", "", 5);
        assertValue(run, "confirmTrigger", "Blood Operative", "ChangeZone", "Surveil", 9, "false");
        assertValue(run, "confirmTrigger", "Blood Operative", "ChangeZone", "Surveil", 10, "YES");
        assertValue(run, "confirmTrigger", "Gelectrode", "Untap", "SpellCast", 10, "NO");
        assertValue(run, "confirmTrigger", "Lazav, Dimir Mastermind", "Clone", "ChangesZone", 10, "NO");
        assertValue(run, "confirmTrigger", "Blood Operative", "ChangeZone", "ChangesZone", 10, "NO");
        assertValue(run, "confirmTrigger", "Nightveil Specter", "Play", "DamageDone", 10, "NO");
        assertValue(run, "confirmTrigger", "Tibor and Lumia", "Play", "DamageDone", 10, "NO");
    }

    private static void assertRows(final AuditRun run, final String family, final String source,
            final String api, final String mode, final int expected) {
        final long count = run.booleanRows.stream().filter(columns -> family.equals(columns.get(0)))
                .filter(columns -> source.equals(columns.get(3))).filter(columns -> api.equals(columns.get(5)))
                .filter(columns -> mode.equals(columns.get(6)))
                .count();
        assertEquals(count, expected, family + " / " + source + " / " + api + " / " + mode + " cluster count");
    }

    private static void assertValue(final AuditRun run, final String family, final String source,
            final String api, final String mode, final int column, final String expected) {
        final List<List<String>> rows = run.booleanRows.stream().filter(columns -> family.equals(columns.get(0)))
                .filter(columns -> source.equals(columns.get(3))).filter(columns -> api.equals(columns.get(5)))
                .filter(columns -> mode.equals(columns.get(6))).toList();
        assertTrue(!rows.isEmpty(), family + " / " + source + " / " + api + " / " + mode + " must be observed");
        assertTrue(rows.stream().allMatch(columns -> expected.equals(columns.get(column))),
                family + " / " + source + " / " + api + " / " + mode + " has unexpected column " + column
                        + " rows=" + rows);
    }

    private static void assertProactiveBooleanCounts(final AuditRun run) {
        assertFamilyCount(run, "confirmTrigger", 0);
        assertFamilyCount(run, "confirmAction", 0);
        assertFamilyCount(run, "chooseBinary", 0);
        assertFamilyCount(run, "payCostToPreventEffect", 24);
        assertFamilyCount(run, "confirmPayment", 0);
        assertFamilyCount(run, "confirmBidAction", 0);
        assertFamilyCount(run, "confirmReplacementEffect", 0);
        assertFamilyCount(run, "confirmStaticApplication", 0);
    }

    private static void assertFamilyCount(final AuditRun run, final String family, final int expected) {
        assertEquals(run.familyCounts.getOrDefault(family, 0), expected, family + " callback count");
    }

    private static void assertSafeProjection(final AuditRun run) throws IOException {
        final String text = Files.readString(run.booleanMetrics, StandardCharsets.UTF_8);
        assertFalse(text.matches("(?s).*\\b(?:SpellAbility|CardLKI|WrappedAbility|GameEntity)@[0-9a-fA-F]+.*"),
                "boolean audit must not expose raw engine object values");
        assertFalse(text.contains("Localized["), "boolean audit must not expose localized prompt values");

        final List<String> lines = Files.readAllLines(run.booleanMetrics, StandardCharsets.UTF_8);
        assertTrue(lines.size() > 1, "boolean audit must contain callback records");
        for (final String line : lines.subList(1, lines.size())) {
            final List<String> columns = csv(line);
            assertEquals(columns.size(), 28, "boolean audit column count");
            assertFalse(columns.get(1).isBlank(), "caller attribution must be present");
            assertFalse(columns.get(2).isBlank(), "semantic owner hint must be present");
            assertFalse(columns.get(15).isBlank(), "candidate/result shape must be present");
            assertTrue("true".equals(columns.get(16)) || "false".equals(columns.get(16)),
                    "ActionContinuation state must be explicit");
            assertFalse(columns.get(17).isBlank(), "provenance must be explicit");
            assertTrue("PUBLIC".equals(columns.get(4)) || "HIDDEN".equals(columns.get(4))
                    || "NONE".equals(columns.get(4)), "visibility must be a typed public marker");
            if ("confirmTrigger".equals(columns.get(0))) {
                assertFalse(columns.get(19).isBlank(), "trigger card state must be projected");
                assertFalse(columns.get(20).isBlank(), "trigger type must be projected");
                assertFalse(columns.get(21).isBlank(), "normalized trigger params must be projected");
                assertFalse(columns.get(22).isBlank(), "trigger Execute must be projected");
                assertFalse(columns.get(23).isBlank(), "live wrapped effect must be projected");
                assertTrue("true".equals(columns.get(24)) || "false".equals(columns.get(24)),
                        "trigger intrinsic state must be explicit");
                assertTrue("PRESENT".equals(columns.get(25)) || "ABSENT".equals(columns.get(25)),
                        "spawning ability state must be explicit");
                assertFalse(columns.get(26).isBlank(), "triggering-object keys must be projected");
                assertFalse(columns.get(27).isBlank(), "source controller must be projected");
            } else {
                for (int index = 19; index < 28; index++) {
                    assertEquals(columns.get(index), "NOT_APPLICABLE",
                            "non-trigger rows must not project trigger-only fields");
                }
            }
        }
    }

    private static void printClusters(final AuditRun run) {
        System.out.println("FRL02K_C_CALLBACK_TOTAL name=" + run.name + " total=" + run.booleanRows.size());
        for (final Map.Entry<String, Integer> entry : run.familyCounts.entrySet()) {
            System.out.println("FRL02K_C_FAMILY name=" + run.name + " family=" + entry.getKey()
                    + " count=" + entry.getValue());
        }
        for (final Map.Entry<String, Integer> entry : run.clusterCounts.entrySet()) {
            System.out.println("FRL02K_C_CLUSTER name=" + run.name + " key=" + entry.getKey()
                    + " count=" + entry.getValue());
        }
        for (final List<String> row : run.booleanRows) {
            if ("confirmTrigger".equals(row.get(0))) {
                System.out.println("FRL02K_C_TRIGGER name=" + run.name + " source=" + row.get(3)
                        + " state=" + row.get(19) + " type=" + row.get(20) + " params=" + row.get(21)
                        + " execute=" + row.get(22) + " effect=" + row.get(23) + " intrinsic=" + row.get(24)
                        + " spawning=" + row.get(25) + " keys=" + row.get(26) + " sourceController=" + row.get(27)
                        + " decider=" + row.get(11) + " affected=" + row.get(12)
                        + " active=" + row.get(13) + " triggering=" + row.get(14)
                        + " continuation=" + row.get(16) + " native=" + row.get(18));
            }
        }
        assertEquals(run.clusterCounts.values().stream().mapToInt(Integer::intValue).sum(), run.booleanRows.size(),
                "semantic clusters must reconcile every callback row");
    }

    private static AuditRun run(final Path root, final String name, final String firstDeck, final String secondDeck,
            final int games, final long seed, final boolean auditEnabled) throws Exception {
        final Path run = root.resolve(name);
        Files.createDirectories(run);
        final Path booleanMetrics = run.resolve("boolean-callback.csv");
        final Path confirmationMetrics = run.resolve("confirmation.csv");
        final Path trace = run.resolve("determinism");
        final Path console = run.resolve("console.log");
        final List<String> command = new ArrayList<>();
        command.add(ChildJvmSupport.javaExecutable().toString());
        command.add("-Dforge.confirmation.metricsFile=" + confirmationMetrics);
        if (auditEnabled) {
            command.add("-D" + BOOLEAN_METRICS_PROPERTY + "=" + booleanMetrics);
        }
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
        if (!process.waitFor(300, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("controlled workload child JVM timed out: " + name);
        }
        assertEquals(process.exitValue(), 0, Files.readString(console, StandardCharsets.UTF_8));
        return AuditRun.read(name, booleanMetrics, confirmationMetrics, trace);
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

    private static final class AuditRun {
        private final String name;
        private final Path booleanMetrics;
        private final Path determinism;
        private final List<List<String>> booleanRows;
        private final Map<String, Integer> familyCounts;
        private final Map<String, Integer> clusterCounts;
        private final int confirmationCallbacks;
        private final int confirmationResults;
        private final Map<String, Integer> confirmationStatusCounts;
        private final Map<String, Integer> confirmationProfileStatusCounts;

        private AuditRun(final String name, final Path booleanMetrics, final Path determinism,
                final List<List<String>> booleanRows, final Map<String, Integer> familyCounts,
                final Map<String, Integer> clusterCounts, final int confirmationCallbacks,
                final int confirmationResults, final Map<String, Integer> confirmationStatusCounts,
                final Map<String, Integer> confirmationProfileStatusCounts0) {
            this.name = name;
            this.booleanMetrics = booleanMetrics;
            this.determinism = determinism;
            this.booleanRows = booleanRows;
            this.familyCounts = familyCounts;
            this.clusterCounts = clusterCounts;
            this.confirmationCallbacks = confirmationCallbacks;
            this.confirmationResults = confirmationResults;
            this.confirmationStatusCounts = confirmationStatusCounts;
            this.confirmationProfileStatusCounts = confirmationProfileStatusCounts0;
        }

        private static AuditRun read(final String name, final Path booleanMetrics,
                final Path confirmationMetrics, final Path determinism) throws IOException {
            final List<List<String>> booleanRows = new ArrayList<>();
            final Map<String, Integer> familyCounts = new HashMap<>();
            final Map<String, Integer> clusterCounts = new HashMap<>();
            if (Files.exists(booleanMetrics)) {
                final List<String> lines = Files.readAllLines(booleanMetrics, StandardCharsets.UTF_8);
                for (final String line : lines.subList(1, lines.size())) {
                    final List<String> columns = csv(line);
                    booleanRows.add(columns);
                    familyCounts.merge(columns.get(0), 1, Integer::sum);
                    final String cluster = String.join("|", columns.get(0), columns.get(1), columns.get(2),
                            columns.get(3), columns.get(4), columns.get(5), columns.get(6), columns.get(7),
                            columns.get(8), columns.get(9), columns.get(10), columns.get(15), columns.get(16),
                            columns.get(17));
                    clusterCounts.merge(cluster, 1, Integer::sum);
                }
            }

            int confirmationCallbacks = 0;
            int confirmationResults = 0;
            final Map<String, Integer> confirmationStatusCounts = new HashMap<>();
            final Map<String, Integer> confirmationProfileStatusCounts = new HashMap<>();
            if (Files.exists(confirmationMetrics)) {
                final List<String> lines = Files.readAllLines(confirmationMetrics, StandardCharsets.UTF_8);
                for (final String line : lines.subList(1, lines.size())) {
                    final List<String> columns = csv(line);
                    if ("CALLBACK".equals(columns.get(0))) {
                        confirmationCallbacks++;
                        confirmationStatusCounts.merge(columns.get(6), 1, Integer::sum);
                        final String profile = columns.size() > 16 ? columns.get(16) : "";
                        confirmationProfileStatusCounts.merge(profile + "|" + columns.get(6), 1,
                                Integer::sum);
                    } else if ("RESULT".equals(columns.get(0))) {
                        confirmationResults++;
                    }
                }
            }
            return new AuditRun(name, booleanMetrics, determinism, booleanRows, familyCounts, clusterCounts,
                    confirmationCallbacks, confirmationResults, confirmationStatusCounts,
                    confirmationProfileStatusCounts);
        }
    }
}
