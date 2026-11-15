package forge.web.match;

import forge.game.card.CardView;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventZone;
import forge.game.zone.ZoneType;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.web.WebTestSupport;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class BrowserSoundsTest {
    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    // Fails if a land's sound is not found when the host plays no sound itself, or when the host has its own sounds off
    @Test
    public void aLandMakesItsColoursSound() {
        final CardView forest = CardView.getCardForUi(FModel.getMagicDb().getCommonCards().getCard("Forest"));
        final GameEventZone played = new GameEventZone(ZoneType.Battlefield, null, EventValueChangeType.Added, forest, null);
        final boolean hostSounds = FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_SOUNDS);
        try {
            for (final boolean on : new boolean[] { true, false }) {
                FModel.getPreferences().setPref(FPref.UI_ENABLE_SOUNDS, on);
                Assert.assertEquals(new BrowserSounds(() -> null).soundFor(played).name(), "green_land");
            }
        } finally {
            FModel.getPreferences().setPref(FPref.UI_ENABLE_SOUNDS, hostSounds);
        }
    }
}
