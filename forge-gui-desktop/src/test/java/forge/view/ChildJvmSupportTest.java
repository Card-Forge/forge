package forge.view;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.testng.Assert.assertEquals;

public class ChildJvmSupportTest {
    @DataProvider
    public Object[][] platformExecutables() {
        return new Object[][] {
                {"Windows 11", "java.exe"},
                {"Linux", "java"},
                {"Mac OS X", "java"}
        };
    }

    @Test(dataProvider = "platformExecutables")
    public void derivesJavaExecutableNameFromOperatingSystem(final String osName,
            final String executableName) throws Exception {
        final Path javaHome = Files.createTempDirectory("frl02k0-java-home-");
        final Path bin = Files.createDirectories(javaHome.resolve("bin"));
        Files.createFile(bin.resolve("java"));
        Files.createFile(bin.resolve("java.exe"));
        try {
            assertEquals(ChildJvmSupport.executableFor(javaHome, osName),
                    javaHome.resolve("bin").resolve(executableName));
        } finally {
            try (var paths = Files.walk(javaHome)) {
                for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
