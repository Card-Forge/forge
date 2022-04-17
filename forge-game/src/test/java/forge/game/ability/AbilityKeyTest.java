package forge.game.ability;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import java.util.Map;

import com.google.common.collect.Maps;

public class AbilityKeyTest {

	@Test
	public void testFromStringWorksForAllKeys() {
		for (AbilityKey key : AbilityKey.values()) {
			AssertJUnit.assertEquals(key, AbilityKey.fromString(key.toString()));
		}
	}

	@Test
	public void testCopyingEmptyMapWorks() {
		Map<AbilityKey, Object> map = Maps.newHashMap();
		Map<AbilityKey, Object> newMap = AbilityKey.newMap(map);

		// An actual copy should be made.
		AssertJUnit.assertNotSame(map, newMap);
	}
}
