package forge.util;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class LocalizerTest {

    private Localizer localizer;

    private static class MapResourceBundle extends ResourceBundle {
        private final Map<String, Object> map;

        public MapResourceBundle(Map<String, Object> map) {
            this.map = map;
        }

        @Override
        protected Object handleGetObject(String key) {
            return map.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return java.util.Collections.enumeration(map.keySet());
        }
    }

    @BeforeMethod
    public void setUp() {
        localizer = Localizer.getInstance();
        localizer.setEnglish(false);
    }

    @AfterMethod
    public void tearDown() {
        localizer.setBundlesForTesting(null, null, null);
    }

    @Test
    public void testGetInstanceIsSingleton() {
        Localizer instance1 = Localizer.getInstance();
        Localizer instance2 = Localizer.getInstance();
        AssertJUnit.assertSame(instance1, instance2);
    }

    @Test
    public void testGetMessageFromActiveBundle() {
        Map<String, Object> active = new HashMap<>();
        active.put("hello", "Hello World");
        localizer.setBundlesForTesting(new MapResourceBundle(active), null, null);

        AssertJUnit.assertEquals("Hello World", localizer.getMessage("hello"));
    }

    @Test
    public void testGetMessageFallbackToEnglishBundle() {
        Map<String, Object> active = new HashMap<>();
        Map<String, Object> english = new HashMap<>();
        english.put("dialog_intro", "Welcome to the adventure!");
        localizer.setBundlesForTesting(new MapResourceBundle(active), new MapResourceBundle(english), null);

        AssertJUnit.assertEquals("Welcome to the adventure!", localizer.getMessage("dialog_intro"));
    }

    @Test
    public void testGetMessageAdventureBundleTakesPrecedence() {
        Map<String, Object> active = new HashMap<>();
        active.put("npc_greeting", "Base greeting");
        Map<String, Object> adventure = new HashMap<>();
        adventure.put("npc_greeting", "Adventure greeting");
        localizer.setBundlesForTesting(new MapResourceBundle(active), null, new MapResourceBundle(adventure));

        AssertJUnit.assertEquals("Adventure greeting", localizer.getMessage("npc_greeting"));
    }

    @Test
    public void testGetMessageMissingPropertyReturnsInvalid() {
        Map<String, Object> english = new HashMap<>();
        localizer.setBundlesForTesting(null, new MapResourceBundle(english), null);

        String result = localizer.getMessage("non_existent_key");
        AssertJUnit.assertTrue(result.contains("INVALID PROPERTY: 'non_existent_key'"));
    }

    @Test
    public void testGetMessageOrUseDefaultWithExistingKey() {
        Map<String, Object> english = new HashMap<>();
        english.put("character_race_elf", "Elf");
        localizer.setBundlesForTesting(null, new MapResourceBundle(english), null);

        AssertJUnit.assertEquals("Elf", localizer.getMessageorUseDefault("character_race_elf", "Unknown"));
    }

    @Test
    public void testGetMessageOrUseDefaultWithMissingKey() {
        Map<String, Object> english = new HashMap<>();
        localizer.setBundlesForTesting(null, new MapResourceBundle(english), null);

        AssertJUnit.assertEquals("Default Fallback", localizer.getMessageorUseDefault("character_race_missing", "Default Fallback"));
    }

    @Test
    public void testForcedEnglishUsesEnglishBundle() {
        Map<String, Object> active = new HashMap<>();
        active.put("greeting", "Hola");
        Map<String, Object> english = new HashMap<>();
        english.put("greeting", "Hello");
        localizer.setBundlesForTesting(new MapResourceBundle(active), new MapResourceBundle(english), null);

        AssertJUnit.assertEquals("Hello", localizer.getEnglishMessage("greeting"));
    }
}
