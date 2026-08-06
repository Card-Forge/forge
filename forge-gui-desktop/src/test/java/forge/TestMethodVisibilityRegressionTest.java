/*
 * REFORGE COMMANDER EXTENSION
 *
 * Regression test for TestNG method visibility.
 *
 * Guards against TestNG @Test methods reverting to package-private visibility
 * (which TestNG 7.x silently skips instead of running).
 */
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
    public void testCardRequestTestComposeCardRequestWithCardNameAndFoilIsPublic() {
        assertAllTestAnnotatedMethodsArePublic(CardRequestTest.class);
    }

    @Test
    public void testCardDbCardMockTestCaseMethodsArePublic() {
        assertAllTestAnnotatedMethodsArePublic(CardDbCardMockTestCase.class);
    }

    @Test
    public void testDeckHintsTestAllTestMethodsArePublic() {
        assertAllTestAnnotatedMethodsArePublic(DeckHintsTest.class);
    }

    @Test
    public void testDeckRecognizerTestAllTestMethodsArePublic() {
        assertAllTestAnnotatedMethodsArePublic(DeckRecognizerTest.class);
    }

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

    private static class ClassWithNonPublicTestMethod {
        @Test
        void packagePrivateTestMethod() {
        }
    }
}