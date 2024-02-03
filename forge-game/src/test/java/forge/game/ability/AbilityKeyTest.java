package forge.game.ability;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Maps;


public class AbilityKeyTest {

    @Test
    public void testFromStringWorksForAllKeys() {
        for (AbilityKey key : AbilityKey.values()) {
            Assert.assertEquals(key, AbilityKey.fromString(key.toString()));
        }
    }

    @Test
    public void testCopyingEmptyMapWorks() {
        Map<AbilityKey, Object> map = Maps.newHashMap();
        Map<AbilityKey, Object> newMap = AbilityKey.newMap(map);

        // An actual copy should be made.
        Assert.assertNotSame(map, newMap);
    }
}
