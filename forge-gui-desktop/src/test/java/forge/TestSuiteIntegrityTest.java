package forge;

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.annotations.Test;

/**
 * Guards against tests that disappear without anyone noticing.
 *
 * <p>
 * Issue #11183: eight test classes stopped running in September 2024 and the build stayed
 * green for a year, because TestNG reports a class it cannot inspect as holding zero tests
 * rather than as a failure. Two independent defects produced that, and this class checks
 * for both:
 * </p>
 * <ol>
 * <li><b>A test class TestNG cannot inspect.</b> Every subclass of PowerMock's
 * {@code PowerMockTestCase} became uninspectable when TestNG 7.10 removed
 * {@code org.testng.IObjectFactory}, which PowerMock 2.0.9 still names in the signature of
 * an inherited method. Walking a class's methods here reproduces exactly the resolution
 * TestNG performs, so the same breakage now fails the build.</li>
 * <li><b>A {@code @Test} method that is not public.</b> TestNG only collects public test
 * methods and says nothing about the rest; 101 methods had quietly become invisible.</li>
 * </ol>
 */
public class TestSuiteIntegrityTest {

    /** Surefire's default include patterns, which decide what is a test class at all. */
    private static boolean isTestClassName(String simpleName) {
        return simpleName.startsWith("Test")
                || simpleName.endsWith("Test")
                || simpleName.endsWith("Tests")
                || simpleName.endsWith("TestCase");
    }

    @Test
    public void testEveryTestClassCanBeInspectedByTestNG() throws Exception {
        List<String> broken = new ArrayList<>();
        for (String className : compiledTestClassNames()) {
            String simpleName = className.substring(className.lastIndexOf('.') + 1);
            if (!isTestClassName(simpleName)) {
                continue;
            }
            try {
                // initialize=false: we want the class resolved the way TestNG resolves it,
                // without paying for static initialisers.
                Class<?> clazz = Class.forName(className, false, getClass().getClassLoader());
                // getMethods() walks the hierarchy and resolves every inherited signature.
                // This is the call that used to throw NoClassDefFoundError.
                clazz.getMethods();
            } catch (Throwable t) {
                broken.add(className + " -> " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
        assertTrue(broken.isEmpty(),
                "TestNG silently reports zero tests for a class it cannot inspect, so these classes "
                        + "would never run and the build would still pass:\n  " + String.join("\n  ", broken));
    }

    @Test
    public void testEveryTestMethodIsPublic() throws Exception {
        List<String> nonPublic = new ArrayList<>();
        for (String className : compiledTestClassNames()) {
            final Class<?> clazz;
            try {
                clazz = Class.forName(className, false, getClass().getClassLoader());
            } catch (Throwable t) {
                // Reported by testEveryTestClassCanBeInspectedByTestNG instead.
                continue;
            }
            final Method[] methods;
            try {
                methods = clazz.getDeclaredMethods();
            } catch (Throwable t) {
                continue;
            }
            for (Method m : methods) {
                if (m.isAnnotationPresent(Test.class) && !Modifier.isPublic(m.getModifiers())) {
                    nonPublic.add(className + "#" + m.getName());
                }
            }
        }
        assertTrue(nonPublic.isEmpty(),
                "TestNG only collects public @Test methods, so these would never run:\n  "
                        + String.join("\n  ", nonPublic));
    }

    private List<String> compiledTestClassNames() throws IOException, URISyntaxException {
        Path root = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!Files.isDirectory(root)) {
            throw new IOException("Expected test classes in a directory, found " + root);
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".class"))
                    .filter(name -> !name.contains("$"))
                    .map(name -> name.substring(0, name.length() - ".class".length()))
                    .map(name -> name.replace(File.separatorChar, '.'))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}
