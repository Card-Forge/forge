package forge.web;

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.web.protocol.ToBrowser.ServerSettings;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class OptionsTest {
    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    private static Map<FPref, String> hostPrefs() {
        final Map<FPref, String> out = new EnumMap<>(FPref.class);
        Arrays.stream(FPref.values()).forEach(p -> out.put(p, FModel.getPreferences().getPref(p)));
        return out;
    }

    // Fails if a guest can change the rules of the host's table, a guest's own change is not kept for that guest, or either reaches the host's preferences or another guest
    @Test
    public void aGuestsChangeIsItsOwnAndLeavesTheTablesRules() {
        final Map<FPref, String> host = hostPrefs();
        final PlayerSettings guest = PlayerSettings.fresh();
        final ServerSettings before = Options.values(guest);
        Options.set(guest, null, "filteredHands", String.valueOf(!guest.getBoolean(FPref.FILTERED_HANDS)));
        Options.set(guest, null, "mulliganRule", "Original");
        Options.set(guest, null, "undoMode", "off".equals(guest.get(FPref.UI_UNDO_MODE)) ? "free" : "off");
        Options.set(guest, null, "ante", String.valueOf(!guest.getBoolean(FPref.UI_ANTE)));
        Options.set(guest, null, "aiProfile", "Reckless");
        Assert.assertEquals(Options.values(guest), before, "a guest changed the table's rules");

        final boolean attackers = guest.getBoolean(FPref.YIELD_INTERRUPT_ON_ATTACKERS);
        Options.set(guest, null, "interruptAttackers", String.valueOf(!attackers));
        Options.set(guest, null, "soundVolume", "37");
        Options.set(guest, null, "devMode", "true");
        Assert.assertEquals(guest.getBoolean(FPref.YIELD_INTERRUPT_ON_ATTACKERS), !attackers);
        Assert.assertEquals(guest.get(FPref.UI_VOL_SOUNDS), "37");
        Assert.assertTrue(guest.getBoolean(FPref.DEV_MODE_ENABLED));
        Assert.assertEquals(hostPrefs(), host, "a guest's setting changed the host's preferences");
        Assert.assertFalse(PlayerSettings.fresh().getBoolean(FPref.DEV_MODE_ENABLED), "one guest's setting reached another guest");
    }

    // Fails if a value outside what the preference allows, or a key that names no setting, changes anything
    @Test
    public void badValuesAndUnknownKeysAreRefused() {
        final PlayerSettings guest = PlayerSettings.fresh();
        final ServerSettings before = Options.values(guest);
        Options.set(guest, null, "soundVolume", "101");
        Options.set(guest, null, "musicVolume", "-1");
        Options.set(guest, null, "soundVolume", "loud");
        Options.set(guest, null, FPref.SHORTCUT_SHOWSTACK.name(), "ctrl+s");
        Options.set(guest, null, "PLAYER_NAME", "Mallory");
        Options.set(guest, null, "nonsense", "true");
        Assert.assertEquals(Options.values(guest), before);
        Assert.assertNotEquals(guest.get(FPref.PLAYER_NAME), "Mallory", "a key that names a preference but no setting set it");
    }

    // Fails if the host's change is not saved to the preferences file, or the host can set a table rule Forge does not know
    @Test
    public void theHostsChangeIsSaved() {
        final ForgePreferences prefs = FModel.getPreferences();
        final String volume = prefs.getPref(FPref.UI_VOL_SOUNDS);
        final String undo = prefs.getPref(FPref.UI_UNDO_MODE);
        try {
            final String changed = "37".equals(volume) ? "38" : "37";
            Options.set(PlayerSettings.saved(), null, "soundVolume", changed);
            Assert.assertEquals(new ForgePreferences().getPref(FPref.UI_VOL_SOUNDS), changed);
            Options.set(PlayerSettings.saved(), null, "undoMode", "sometimes");
            Assert.assertEquals(prefs.getPref(FPref.UI_UNDO_MODE), undo);
        } finally {
            prefs.setPref(FPref.UI_VOL_SOUNDS, volume);
            prefs.setPref(FPref.UI_UNDO_MODE, undo);
            prefs.save();
        }
    }
}
