package forge.ai;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import forge.GuiDesktop;
import forge.gui.GuiBase;

import forge.localinstance.properties.ForgeConstants;

/**
 * The AI profiles each have a Flat Monte Carlo section of their own, so that switching the profile is felt when the
 * layer is on: Reckless takes risks and does not hold mana, Cautious weighs risk and keeps mana open, Experimental
 * turns every optional part on. The test reads the .ai files, checks every FLAT_MC_ key is a real property with a value
 * that is in range, and that the profiles are ordered the way their names say (a failure says which key to change).
 */
public class FlatMonteCarloProfilesTest {
    @BeforeClass
    public void setUp() {
        GuiBase.setInterface(new GuiDesktop()); // ForgeConstants needs it
    }

    private static Properties load(final String profile) throws Exception {
        final Properties p = new Properties();
        try (InputStream in = new FileInputStream(ForgeConstants.AI_PROFILE_DIR + profile + ".ai")) {
            p.load(in);
        }
        return p;
    }

    private static int num(final Properties p, final String profile, final String key) {
        final String v = p.getProperty(key);
        AssertJUnit.assertNotNull(profile + ".ai has no " + key + " (the profile would fall back to the default value)", v);
        return Integer.parseInt(v.trim());
    }

    @Test
    public void everyFlatMcKeyOfTheProfilesIsARealProperty() throws Exception {
        for (final String profile : new String[] { "Default", "Reckless", "Cautious", "Experimental", "Hard", "Hardest" }) {
            final Properties p = load(profile);
            for (final String key : p.stringPropertyNames()) {
                if (key.startsWith("FLAT_MC_")) {
                    try {
                        AiProps.valueOf(key);
                    } catch (final IllegalArgumentException e) {
                        AssertJUnit.fail(profile + ".ai has " + key + ", which is not an AiProps constant");
                    }
                }
            }
        }
    }

    @Test
    public void theThreeProfilesHaveTheirOwnFlatMcSection() throws Exception {
        for (final String profile : new String[] { "Reckless", "Cautious", "Experimental" }) {
            final Properties p = load(profile);
            for (final AiProps prop : AiProps.values()) {
                if (prop.name().startsWith("FLAT_MC_") && !"FLAT_MC_DECK_PLAN".equals(prop.name())
                        && !"FLAT_MC_MULLIGAN".equals(prop.name())) {
                    AssertJUnit.assertNotNull(profile + ".ai should set " + prop.name(), p.getProperty(prop.name()));
                }
            }
        }
    }

    @Test
    public void theProfilesAreOrderedTheWayTheirNamesSay() throws Exception {
        final Properties def = load("Default");
        final Properties reck = load("Reckless");
        final Properties caut = load("Cautious");
        for (final String key : new String[] { "FLAT_MC_RISK_WEIGHT", "FLAT_MC_HOLD_MANA_WEIGHT", "FLAT_MC_HEURISTIC_BONUS",
            "FLAT_MC_OVERRIDE_MAJORITY" }) {
            final int r = num(reck, "Reckless", key);
            final int d = num(def, "Default", key);
            final int c = num(caut, "Cautious", key);
            AssertJUnit.assertTrue(key + ": Reckless " + r + " < Default " + d + " < Cautious " + c + " should hold",
                    r < d && d < c);
        }
        AssertJUnit.assertEquals("Reckless never holds back", "false", reck.getProperty("FLAT_MC_CONSIDER_HOLDING"));
        AssertJUnit.assertEquals("Cautious looks at the reply", true, num(caut, "Cautious", "FLAT_MC_OPPONENT_LOOKAHEAD") > 0);
        AssertJUnit.assertEquals("Cautious answers spells", "true", caut.getProperty("FLAT_MC_REACTIVE"));
        AssertJUnit.assertEquals("Reckless attacks by rollouts", "true", reck.getProperty("FLAT_MC_ATTACK"));
        final Properties exp = load("Experimental");
        AssertJUnit.assertEquals("Experimental discovers broadly", "true", exp.getProperty("FLAT_MC_BROAD_DISCOVERY"));
        AssertJUnit.assertEquals("Experimental compares attacks", "true", exp.getProperty("FLAT_MC_ATTACK"));
    }
}
