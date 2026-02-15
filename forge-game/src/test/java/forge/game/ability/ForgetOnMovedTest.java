package forge.game.ability;

import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.trigger.Trigger;
import forge.util.Lang;
import forge.util.Localizer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;

public class ForgetOnMovedTest {

    @BeforeClass
    public void initLocalization() {
        File file = new File("../forge-gui/res/languages");
        if (!file.exists()) {
            file = new File("forge-gui/res/languages");
        }
        Localizer.getInstance().initialize("en-US", file.getAbsolutePath());
        Lang.createInstance("en-US");
    }

    @Test
    public void addsChangesZoneTriggerWithExcludedDestinations() {
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, new ArrayList<>(), "Test");
        Game game = new Game(new ArrayList<>(), rules, match);

        Card host = new Card(game.nextCardId(), game);
        SpellAbilityEffect.addForgetOnMovedTrigger(host, "Exile");

        boolean foundChangesZone = false;
        boolean foundExiled = false;
        for (Trigger t : host.getTriggers()) {
            String mode = t.getParam("Mode");
            if ("ChangesZone".equals(mode)) {
                foundChangesZone = true;
                String excluded = t.getParam("ExcludedDestinations");
                Assert.assertNotNull(excluded, "ExcludedDestinations should be present");
                Assert.assertTrue(excluded.contains("Stack") && excluded.contains("Exile"),
                        "ExcludedDestinations must contain Stack and Exile, got: " + excluded);
            }
            if ("Exiled".equals(mode)) {
                foundExiled = true;
            }
        }
        Assert.assertTrue(foundChangesZone, "Expected a ChangesZone trigger for ForgetOnMoved");
        Assert.assertTrue(foundExiled, "Expected an Exiled trigger for ForgetOnMoved");
    }
}
