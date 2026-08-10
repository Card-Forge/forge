package forge.view;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Shared support for launching the current JVM directly from child-JVM tests. */
final class ChildJvmSupport {
    private ChildJvmSupport() {
    }

    static Path javaExecutable() {
        return executableFor(Path.of(System.getProperty("java.home")), System.getProperty("os.name"));
    }

    static Path executableFor(final Path javaHome, final String osName) {
        final String executableName = isWindows(osName) ? "java.exe" : "java";
        final Path executable = javaHome.resolve("bin").resolve(executableName);
        if (!Files.isRegularFile(executable)) {
            throw new IllegalStateException("Java executable does not exist: " + executable);
        }
        return executable;
    }

    private static boolean isWindows(final String osName) {
        return osName != null && osName.toLowerCase(Locale.ROOT).startsWith("windows");
    }
}
