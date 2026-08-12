package forge.game.decision;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class TriggeredTargetContinuationProcessTest {
    private static final List<String> EXPECTED_NULL_RESOLVER_OUTPUT = List.of(
            "reason=UNSUPPORTED_ACTION_CONTINUATION",
            "provider_requests=0",
            "resolver_present=false",
            "resolver_calls=0",
            "native_calls=0");
    private static final List<String> EXPECTED_EXTERNAL_RESOLVER_OUTPUT = List.of(
            "reason=UNSUPPORTED_ACTION_CONTINUATION",
            "provider_requests=0",
            "resolver_present=true",
            "resolver_calls=0",
            "native_calls=0");

    @Test
    public void freshJvmRejectsTriggeredTargetContinuationBeforeDownstreamCallbacksWithNullResolver()
            throws Exception {
        assertChildOutput(EXPECTED_NULL_RESOLVER_OUTPUT);
    }

    @Test
    public void freshJvmRejectsTriggeredTargetContinuationBeforeDownstreamCallbacksWithExternalResolver()
            throws Exception {
        assertChildOutput(EXPECTED_EXTERNAL_RESOLVER_OUTPUT, "external");
    }

    private static void assertChildOutput(final List<String> expectedOutput, final String... arguments)
            throws Exception {
        final Path temporaryDirectory = Files.createTempDirectory("frl02k-c2a-process-");
        final Path output = temporaryDirectory.resolve("child-output.txt");
        Process process = null;
        try {
            final List<String> command = new ArrayList<>(List.of(
                    javaExecutable().toString(),
                    "-Djava.io.tmpdir=" + temporaryDirectory,
                    "-cp",
                    System.getProperty("java.class.path"),
                    TriggeredTargetContinuationChildMain.class.getName()));
            command.addAll(List.of(arguments));
            process = new ProcessBuilder(command)
                    .directory(repositoryRoot().resolve("forge-gui").toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(output.toFile())
                    .start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                fail("continuation child JVM timed out");
            }

            final List<String> childOutput = Files.readAllLines(output, StandardCharsets.UTF_8);
            assertEquals(process.exitValue(), 0, String.join(System.lineSeparator(), childOutput));
            assertEquals(childOutput, expectedOutput);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
            deleteTree(temporaryDirectory);
        }
    }

    private static Path javaExecutable() {
        final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        final String executableName = osName.startsWith("windows") ? "java.exe" : "java";
        final Path executable = Path.of(System.getProperty("java.home"), "bin", executableName);
        if (!Files.isRegularFile(executable)) {
            throw new IllegalStateException("Java executable does not exist: " + executable);
        }
        return executable;
    }

    private static Path repositoryRoot() {
        final Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        return workingDirectory.getFileName().toString().equals("forge-gui-desktop")
                ? workingDirectory.getParent() : workingDirectory;
    }

    private static void deleteTree(final Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
