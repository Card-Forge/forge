package forge.game.ability;

import com.google.common.collect.Maps;
import junit.framework.TestCase;

import java.util.Map;

public class AbilityKeyTest extends TestCase {
    public void testFromStringWorksForAllKeys() {
        for (AbilityKey key : AbilityKey.values()) {
            assertEquals(key, AbilityKey.fromString(key.toString()));
        }
    }

    public void testCopyingEmptyMapWorks() {
        Map<AbilityKey, Object> map = Maps.newHashMap();

        Map<AbilityKey, Object> newMap = AbilityKey.newMap(map);

        // An actual copy should be made.
        assertNotSame(map, newMap);
    }
}
