package forge.game.decision;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

public class TriggeredTargetIntegrityExceptionApiTest {

    @Test
    public void reasonIsTheOnlyMachineReadableClassification() throws Exception {
        assertMissingNestedType("Status");
        assertMissingField("status");
        assertMissingMethod("getStatus");

        final TriggeredTargetIntegrityException exception = new TriggeredTargetIntegrityException(
                TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        assertEquals("UNSUPPORTED_PROFILE", exception.getReason());
    }

    private static void assertMissingNestedType(final String nestedTypeName) throws Exception {
        try {
            Class.forName(TriggeredTargetIntegrityException.class.getName() + "$" + nestedTypeName);
            fail("TriggeredTargetIntegrityException must not expose nested " + nestedTypeName);
        } catch (final ClassNotFoundException expected) {
            // Expected API-removal result.
        }
    }

    private static void assertMissingField(final String fieldName) throws Exception {
        try {
            TriggeredTargetIntegrityException.class.getDeclaredField(fieldName);
            fail("TriggeredTargetIntegrityException must not expose field " + fieldName);
        } catch (final NoSuchFieldException expected) {
            // Expected API-removal result.
        }
    }

    private static void assertMissingMethod(final String methodName) throws Exception {
        try {
            TriggeredTargetIntegrityException.class.getDeclaredMethod(methodName);
            fail("TriggeredTargetIntegrityException must not expose method " + methodName);
        } catch (final NoSuchMethodException expected) {
            // Expected API-removal result.
        }
    }
}
