package forge;

import forge.card.CardDbCardMockTestCase;
import forge.card.CardRequestTest;
import forge.deck.DeckRecognizerTest;
import forge.item.DeckHintsTest;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Regression tests for a CI fix where several TestNG {@code @Test} methods were declared
 * with package-private (default) visibility instead of {@code public}. TestNG 7.x silently
 * ignores non-public {@code @Test} methods rather than failing the build, so those tests
 * were never actually executed. This class verifies that the specific methods fixed by that
 * change (see the "make test methods public" commit) remain public, and includes a
 * self-check proving the detection logic itself works.
 */
public class TestMethodVisibilityRegressionTest {

    @Test
    public void testCardRankerTestRankIsPublic() throws NoSuchMethodException {
        assertMethodIsPublic(CardRankerTest.class, "testRank");
    }

    @Test
    public void testFCollectionTestCompletableFutureIsPublic() throws NoSuchMethodException {
        assertMethodIsPublic(FCollectionTest.class, "testCompletableFuture");
    }

    @Test
    public void testRunTestTestIsPublic() throws NoSuchMethodException {
        assertMethodIsPublic(RunTest.class, "test");
    }

    @Test
    public void testCardRequestTestComposeCardRequestWithCardNameAndFoilIsPublic() throws NoSuchMethodException {
        assertMethodIsPublic(CardRequestTest.class, "testComposeCardRequestWithCardNameAndFoil");
    }

    @Test
    public void testCardDbCardMockTestCaseMethodsArePublic() throws NoSuchMethodException {
        String[] methodNames = {
                "testGetAllCardsOfaGivenNameAndPrintedInSets",
                "testGetAllCardsOfaGivenNameAndLegalInSets",
                "testCardRequestWithSetCodeAllInLowercase",
                "testThatWithCardPreferenceSetAndNoRequestForSpecificEditionAlwaysReturnsPreferredArt",
                "testGetDualAndDoubleCards"
        };
        for (String methodName : methodNames) {
            assertMethodIsPublic(CardDbCardMockTestCase.class, methodName);
        }
    }

    @Test
    public void testDeckHintsTestAllTestMethodsArePublic() {
        assertAllTestAnnotatedMethodsArePublic(DeckHintsTest.class);
    }

    @Test
    public void testDeckRecognizerTestAllTestMethodsArePublic() {
        assertAllTestAnnotatedMethodsArePublic(DeckRecognizerTest.class);
    }

    /**
     * Sanity check for the detection logic used above: it must actually flag a
     * package-private {@code @Test} method instead of silently passing, otherwise the
     * checks above would give a false sense of security.
     */
    @Test
    public void testDetectionLogicFlagsNonPublicTestMethod() {
        List<String> nonPublicTestMethods = findNonPublicTestMethods(ClassWithNonPublicTestMethod.class);
        assertFalse(nonPublicTestMethods.isEmpty(), "Detection logic failed to flag a package-private @Test method");
        assertTrue(nonPublicTestMethods.contains("packagePrivateTestMethod"));
    }

    private void assertMethodIsPublic(Class<?> testClass, String methodName) throws NoSuchMethodException {
        Method method = testClass.getDeclaredMethod(methodName);
        assertTrue(Modifier.isPublic(method.getModifiers()),
                testClass.getName() + "#" + methodName
                        + "() must be public, otherwise TestNG silently skips it instead of running it");
    }

    private void assertAllTestAnnotatedMethodsArePublic(Class<?> testClass) {
        List<String> nonPublicTestMethods = findNonPublicTestMethods(testClass);
        assertTrue(nonPublicTestMethods.isEmpty(),
                "Found @Test method(s) in " + testClass.getName()
                        + " that are not public (TestNG silently skips these instead of running them): "
                        + nonPublicTestMethods);
    }

    private List<String> findNonPublicTestMethods(Class<?> testClass) {
        List<String> violations = new ArrayList<>();
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Test.class) && !Modifier.isPublic(method.getModifiers())) {
                violations.add(method.getName());
            }
        }
        return violations;
    }

    /** Fixture class used only to verify that {@link #findNonPublicTestMethods} works correctly. */
    private static class ClassWithNonPublicTestMethod {
        @Test
        void packagePrivateTestMethod() {
            // intentionally package-private to exercise the detection logic
        }
    }
}