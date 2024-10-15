package forge.game.ability;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Maps;


public class AbilityKeyTest {

    @Test
    public void testFromStringWorksForAllKeys() {
        for (AbilityKey key : AbilityKey.values()) {
            assertEquals(key, AbilityKey.fromString(key.toString()));
        }
    }

    @Test
    public void testCopyingEmptyMapWorks() {
        Map<AbilityKey, Object> map = Maps.newHashMap();
        Map<AbilityKey, Object> newMap = AbilityKey.newMap(map);

        // An actual copy should be made.
        assertNotSame(map, newMap);
    }
}
