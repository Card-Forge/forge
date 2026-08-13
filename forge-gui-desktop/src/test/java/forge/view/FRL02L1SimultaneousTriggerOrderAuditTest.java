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
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/** Fresh-JVM canonical acceptance gate for FRL-02L1 admission and request counts. */
public class FRL02L1SimultaneousTriggerOrderAuditTest {
    private static final String AUDIT_PROPERTY = "forge.simultaneousTriggerOrder.auditFile";

    @Test(timeOut = 900_000L)
    public void canonicalWorkloadAdmitsEveryL1ProfileSessionAndPreservesTrace() throws Exception {
        final Path root = Files.createTempDirectory("frl02l1-order-audit-");
        boolean completed = false;
        try {
            final AuditRun audit = run(root, "audit", true);
            AssertionError acceptanceFailure = null;
            try {
                assertExpectedCounts(audit);
            } catch (final AssertionError failure) {
                acceptanceFailure = failure;
            }

            final AuditRun control = run(root, "control", false);
            assertEquals(hashTree(audit.trace), hashTree(control.trace),
                    "order audit instrumentation must not change deterministic traces");
            if (acceptanceFailure != null) {
                throw acceptanceFailure;
            }
            completed = true;
        } finally {
            if (completed) {
                deleteTree(root);
            } else {
                System.out.println("FRL02L1 audit failure artifacts: " + root);
            }
        }
    }

    private static void assertExpectedCounts(final AuditRun run) throws IOException {
        assertTrue(Files.exists(run.auditFile), "canonical audit must produce its diagnostics file: "
                + run.auditFile + "\nconsole=" + run.console);
        final Map<String, String> values = readProperties(run.auditFile);
        assertEquals(values.get("version"), "FRL_02L1_ORDER_AUDIT_V2");
        assertEquals(values.get("orderSimultaneousSa.total"), "116");
        assertEquals(values.get("orderSimultaneousSa.n0"), "0");
        assertEquals(values.get("orderSimultaneousSa.n1"), "96");
        assertEquals(values.get("orderSimultaneousSa.n2"), "14");
        assertEquals(values.get("orderSimultaneousSa.n3"), "5");
        assertEquals(values.get("orderSimultaneousSa.n4"), "1");
        assertEquals(values.get("orderSimultaneousSa.nOther"), "0");
        assertEquals(values.get("rawMultiItemCallbacks"), "20");
        assertEquals(values.get("simultaneousTriggerProfileSessions"), "19");
        assertEquals(values.get("admittedSimultaneousTriggerSessions"), "19");
        assertEquals(values.get("nonL1MultiItemCallbacks"), "1");
        assertEquals(values.get("orderRequests"), "26");
        assertEquals(values.get("candidateSize2"), "19");
        assertEquals(values.get("candidateSize3"), "6");
        assertEquals(values.get("candidateSize4"), "1");
        assertEquals(values.get("forcedRequests"), "0");
        assertEquals(values.get("l1UnsupportedFallbacks"), "0");
        assertEquals(values.get("outsideL1NativeFallbacks"), "1");
        assertEquals(values.get("integrityFailures"), "0");
        assertEquals(values.get("l1UnsupportedFailures"), "0");
        assertEquals(values.get("invalidExternalCandidates"), "0");
        assertEquals(values.get("nativeCallbackFailures"), "0");
        assertEquals(values.get("mappingFailures"), "0");
        assertEquals(values.get("traceIncomplete"), "0");
    }

    private static AuditRun run(final Path root, final String name, final boolean auditEnabled) throws Exception {
        final Path run = root.resolve(name);
        Files.createDirectories(run);
        final Path auditFile = run.resolve("simultaneous-trigger-order.properties");
        final Path trace = run.resolve("determinism");
        final Path console = run.resolve("console.log");
        final List<String> command = new ArrayList<>();
        command.add(ChildJvmSupport.javaExecutable().toString());
        if (auditEnabled) {
            command.add("-D" + AUDIT_PROPERTY + "=" + auditFile);
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
            fail("canonical FRL-02L1 child JVM timed out: " + name);
        }
        assertEquals(process.exitValue(), 0, Files.readString(console, StandardCharsets.UTF_8));
        return new AuditRun(auditFile, trace, console);
    }

    private static Map<String, String> readProperties(final Path file) throws IOException {
        final Map<String, String> values = new HashMap<>();
        for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            final int separator = line.indexOf('=');
            assertTrue(separator > 0, "audit line must contain a key/value separator: " + line);
            values.put(line.substring(0, separator), line.substring(separator + 1));
        }
        return values;
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

    private static final class AuditRun {
        private final Path auditFile;
        private final Path trace;
        private final Path console;

        private AuditRun(final Path auditFile0, final Path trace0, final Path console0) {
            auditFile = auditFile0;
            trace = trace0;
            console = console0;
        }
    }
}
