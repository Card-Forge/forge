package forge.view;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** Fresh-JVM canonical acceptance gate for FRL-02L2A capture diagnostics. */
public class FRL02L2ASurveilPartitionAuditTest {
    public static void main(final String[] args) {
        if (args.length > 0 && "run-workload".equals(args[0])) {
            Main.main(Arrays.copyOfRange(args, 1, args.length));
        }
    }

    @Test(timeOut = 900_000L)
    public void canonicalWorkloadPreservesTraceAndProducesSurveilAudit() throws Exception {
        final Path run = Files.createTempDirectory("frl02l2a-audit-");
        final Path auditRoot = run.resolve("audit");
        final Path controlRoot = run.resolve("control");
        Files.createDirectories(auditRoot);
        Files.createDirectories(controlRoot);
        final Path auditOutput = auditRoot.resolve("audit.properties");
        final Path auditTrace = auditRoot.resolve("trace");
        final Path auditConsole = auditRoot.resolve("console.log");
        final Path controlTrace = controlRoot.resolve("trace");
        final Path controlConsole = controlRoot.resolve("console.log");
        boolean passed = false;
        try {
            runChild(command(auditOutput, auditTrace, true), auditConsole);
            runChild(command(null, controlTrace, false), controlConsole);
            assertEquals(hashTraceTree(auditTrace), hashTraceTree(controlTrace));
            assertExpected(auditOutput);
            passed = true;
        } finally {
            if (passed) {
                deleteTree(run);
            } else {
                System.err.println("FRL-02L2A audit artifacts retained at " + run);
            }
        }
    }

    private static List<String> command(final Path auditOutput, final Path trace, final boolean audit) {
        final List<String> common = List.of("sim",
                "-d", "Izzet Guild Kit", "Dimir Guild Kit", "-n", "10", "-s", "20260810", "-q");
        final List<String> command = new ArrayList<>();
        command.add(ChildJvmSupport.javaExecutable().toString());
        if (audit) {
            command.add("-Dforge.surveil.partition.audit.enabled=true");
            command.add("-Dforge.surveil.partition.audit.output=" + auditOutput);
        }
        command.add("-Dforge.determinism.traceDir=" + trace);
        command.add("-Dforge.determinism.auditRandom=true");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(FRL02L2ASurveilPartitionAuditTest.class.getName());
        command.add("run-workload");
        command.addAll(common);
        return command;
    }

    private static void runChild(final List<String> command, final Path console) throws Exception {
        final Process child = new ProcessBuilder(command)
                .directory(repositoryRoot().resolve("forge-gui").toFile())
                .redirectErrorStream(true)
                .redirectOutput(console.toFile())
                .start();
        if (!child.waitFor(300, TimeUnit.SECONDS)) {
            child.destroyForcibly();
            throw new AssertionError("FRL-02L2A child timed out; see " + console);
        }
        assertEquals(child.exitValue(), 0, Files.readString(console, StandardCharsets.UTF_8));
    }

    private static void assertExpected(final Path output) throws IOException {
        assertTrue(Files.isRegularFile(output));
        final Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(output, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        final Set<String> expectedKeys = new TreeSet<>(List.of(
                "schema", "profile", "workload_first_deck", "workload_second_deck", "games", "seed",
                "raw_arrange_for_surveil_invocations", "capture_admission_failures", "non_empty_sessions",
                "n_bucket_0", "n_bucket_1", "n_bucket_2", "n_bucket_ge3", "native_callback_invocations",
                "native_callback_failures", "valid_partition_mappings", "mapping_failures",
                "membership_request_count", "membership_result_count", "candidate_count", "forced_request_count",
                "external_attempts", "trace_incomplete_count", "public_symmetry_conflicts",
                "teacher_eligibility_not_applicable_count", "teacher_eligibility_bc_eligible_count",
                "teacher_eligibility_bc_excluded_public_symmetry_count", "n2_graveyard_0_retained_2",
                "n2_graveyard_1_retained_1", "n2_graveyard_2_retained_0"));
        assertEquals(new TreeSet<>(properties.stringPropertyNames()), expectedKeys);
        final Map<String, String> expected = Map.ofEntries(
                Map.entry("schema", "FRL02L2A_SURVEIL_AUDIT_V1"),
                Map.entry("profile", "SURVEIL_PARTITION"),
                Map.entry("workload_first_deck", "Izzet Guild Kit"),
                Map.entry("workload_second_deck", "Dimir Guild Kit"),
                Map.entry("games", "10"),
                Map.entry("seed", "20260810"),
                Map.entry("non_empty_sessions", "16"),
                Map.entry("n_bucket_0", "0"), Map.entry("n_bucket_1", "6"), Map.entry("n_bucket_2", "10"),
                Map.entry("n_bucket_ge3", "0"), Map.entry("raw_arrange_for_surveil_invocations", "16"),
                Map.entry("capture_admission_failures", "0"), Map.entry("native_callback_invocations", "16"),
                Map.entry("native_callback_failures", "0"), Map.entry("valid_partition_mappings", "16"),
                Map.entry("mapping_failures", "0"), Map.entry("membership_request_count", "26"),
                Map.entry("membership_result_count", "26"), Map.entry("candidate_count", "52"),
                Map.entry("forced_request_count", "0"), Map.entry("external_attempts", "0"),
                Map.entry("trace_incomplete_count", "0"), Map.entry("n2_graveyard_0_retained_2", "5"),
                Map.entry("n2_graveyard_1_retained_1", "2"), Map.entry("n2_graveyard_2_retained_0", "3"),
                Map.entry("teacher_eligibility_not_applicable_count", "26"),
                Map.entry("teacher_eligibility_bc_eligible_count", "0"),
                Map.entry("teacher_eligibility_bc_excluded_public_symmetry_count", "0"));
        for (final Map.Entry<String, String> entry : expected.entrySet()) {
            assertEquals(properties.getProperty(entry.getKey()), entry.getValue(), entry.getKey());
        }
        final String serialized = Files.readString(output, StandardCharsets.UTF_8);
        assertFalse(serialized.contains("cardId"));
        assertFalse(serialized.contains("gameTimestamp"));
        assertFalse(serialized.contains("nativeObject"));
        for (final String forbidden : List.of("Card", "CardView", "CardLKI", "SpellAbility", "Player", "Game",
                "ownerId", "controllerId", "ZoneType", "zone", "RNG", "AI", "shuffle", "Island", "Forest",
                "Mountain", "Swamp", "Plains")) {
            assertFalse(serialized.contains(forbidden), forbidden);
        }
        assertTrue(Long.parseLong(properties.getProperty("public_symmetry_conflicts")) >= 0);
    }

    private static Path repositoryRoot() {
        final Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        return workingDirectory.getFileName().toString().equals("forge-gui-desktop")
                ? workingDirectory.getParent() : workingDirectory;
    }

    private static String hashTraceTree(final Path root) throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (Stream<Path> paths = Files.walk(root)) {
            for (final Path file : paths.filter(Files::isRegularFile)
                    .sorted(java.util.Comparator.comparing(path -> root.relativize(path).toString())).toList()) {
                digest.update(root.relativize(file).toString().replace('\\', '/')
                        .getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(Files.readAllBytes(file));
                digest.update((byte) 0);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void deleteTree(final Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (final Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
