package forge.game.ability;

import junit.framework.TestCase;

public class AbilityKeyTest extends TestCase {
    public void testFromStringWorksForAllKeys() {
        for (AbilityKey key : AbilityKey.values()) {
            assertEquals(key, AbilityKey.fromString(key.toString()));
        }
    }
}
