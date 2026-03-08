package forge.net;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import forge.game.event.GameEvent;
import forge.gamemodes.net.GameEventProxy;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Verifies that all GameEvent record types can be serialized by GameEventProxy.
 * This catches non-serializable fields in new or modified events, which would
 * otherwise only fail at runtime during network play.
 */
@Test(groups = { "UnitTest" })
public class GameEventProxyTest {

    /**
     * Discovers all GameEvent record classes in the event package and verifies
     * each can be wrapped by GameEventProxy without serialization errors.
     *
     * This catches real bugs: if someone adds a new GameEvent with a
     * non-Serializable field (e.g., a raw Game reference), this test fails
     * immediately rather than waiting for a network game to hit it.
     */
    @Test
    @SuppressWarnings("UnstableApiUsage")
    public void testAllGameEventTypesAreWrappable() throws Exception {
        ImmutableSet<ClassPath.ClassInfo> classInfos = ClassPath.from(getClass().getClassLoader())
                .getTopLevelClasses("forge.game.event");

        List<Class<?>> eventRecords = new ArrayList<>();
        for (ClassPath.ClassInfo info : classInfos) {
            Class<?> clazz = info.load();
            if (GameEvent.class.isAssignableFrom(clazz) && clazz.isRecord()) {
                eventRecords.add(clazz);
            }
        }

        Assert.assertFalse(eventRecords.isEmpty(), "Should discover at least one GameEvent record");

        List<String> failures = new ArrayList<>();
        int tested = 0;

        for (Class<?> clazz : eventRecords) {
            try {
                GameEvent event = constructWithDefaults(clazz);
                GameEventProxy.wrap(event);
                tested++;
            } catch (Exception e) {
                failures.add(clazz.getSimpleName() + ": " + e.getMessage());
            }
        }

        Assert.assertTrue(tested > 0, "Should have tested at least one event type");
        if (!failures.isEmpty()) {
            Assert.fail("The following GameEvent types failed to wrap (non-serializable fields?):\n  "
                    + String.join("\n  ", failures));
        }
    }

    /**
     * Constructs a record instance using default values for each component type.
     * Uses null for objects, 0 for numeric types, false for booleans.
     */
    private GameEvent constructWithDefaults(Class<?> recordClass) throws Exception {
        RecordComponent[] components = recordClass.getRecordComponents();
        Class<?>[] paramTypes = new Class<?>[components.length];
        Object[] args = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            Class<?> type = components[i].getType();
            paramTypes[i] = type;
            args[i] = defaultFor(type);
        }

        return (GameEvent) recordClass.getDeclaredConstructor(paramTypes).newInstance(args);
    }

    private Object defaultFor(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0.0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return ' ';
        if (type == short.class) return (short) 0;
        if (type == String.class) return "";
        if (type == List.class) return List.of();
        if (type == Collection.class) return List.of();
        if (type == Iterable.class) return List.of();
        if (type == Map.class) return Map.of();
        return null;
    }
}
