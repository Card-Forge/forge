package forge.ai;

import java.util.Arrays;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.ai.simulation.FlatMcTestBase;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

/** Removal picks a permanent that stops the AI from attacking (Moat) over a creature when the AI has creatures to attack with. */
public class AttackLockRemovalTest extends FlatMcTestBase {

    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    @Test
    public void moatIsTheTargetWhenTheAiHasCreaturesToAttackWith() {
        put("Grizzly Bears", 2, ai, ZoneType.Battlefield);
        final Card moat = put("Moat", opp, ZoneType.Battlefield);
        final Card knight = put("Knight of the White Orchid", opp, ZoneType.Battlefield);
        AssertJUnit.assertSame(moat, ComputerUtilCard.getBestRemovalTargetAI(ai, Arrays.asList(knight, moat)));
    }

    @Test
    public void moatIsNotSpecialWhenTheAiHasNothingToAttackWith() {
        final Card moat = put("Moat", opp, ZoneType.Battlefield);
        final Card angel = put("Serra Angel", opp, ZoneType.Battlefield);
        AssertJUnit.assertSame(angel, ComputerUtilCard.getBestRemovalTargetAI(ai, Arrays.asList(angel, moat)));
    }
}
