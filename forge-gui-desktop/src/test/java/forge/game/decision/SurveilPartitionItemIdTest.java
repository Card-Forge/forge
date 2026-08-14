package forge.game.decision;

import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class SurveilPartitionItemIdTest {
    @Test
    public void opaqueItemIdIsDeterministicForTheSameCanonicalRank() {
        assertEquals(SurveilPartitionItemId.opaqueItemId(1), -1956407806741107680L);
        assertEquals(SurveilPartitionItemId.opaqueItemId(2), -7541218347953203506L);
        assertEquals(SurveilPartitionItemId.opaqueItemId(3), -7995527694508729151L);
        assertEquals(SurveilPartitionItemId.opaqueItemId(4), -4511274789031428550L);
    }

    @Test
    public void opaqueItemIdIsDistinctForTheCanonicalRanksUsedByOneSession() {
        final Set<Long> itemIds = new HashSet<>();
        for (int rank = 1; rank <= 4; rank++) {
            itemIds.add(SurveilPartitionItemId.opaqueItemId(rank));
        }

        assertEquals(itemIds.size(), 4);
    }

    @Test
    public void opaqueItemIdRejectsNonPositiveCanonicalRank() {
        expectThrows(IllegalArgumentException.class,
                () -> SurveilPartitionItemId.opaqueItemId(0));
        expectThrows(IllegalArgumentException.class,
                () -> SurveilPartitionItemId.opaqueItemId(-1));
    }

    @Test
    public void opaqueItemIdIgnoresNativeSessionAndCardInputs() {
        assertEquals(SurveilPartitionItemId.class.getDeclaredFields().length, 0);
        assertEquals(SurveilPartitionItemId.class.getDeclaredMethods().length, 1);
        final Method method;
        try {
            method = SurveilPartitionItemId.class.getDeclaredMethod("opaqueItemId", int.class);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
        assertEquals(method.getReturnType(), long.class);
        assertEquals(Arrays.asList(method.getParameterTypes()), Arrays.asList(int.class));
        assertTrue(Modifier.isStatic(method.getModifiers()));
        assertFalse(Modifier.isPublic(method.getModifiers()));
        assertFalse(Modifier.isPublic(SurveilPartitionItemId.class.getModifiers()));
        assertEquals(SurveilPartitionItemId.class.getConstructors().length, 0);
    }
}
