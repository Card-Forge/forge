package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * A permanent whose enters trigger can aim at the permanent itself (Merfolk Skydiver) is not "without a target" just
 * because nothing else is in play; a hostile one (Journey to Nowhere) still is.
 */
public class LegalityGuardSelfTargetTest extends FlatMcTestBase {

    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    @Test
    public void aCreatureThatCanTargetItselfIsNotHeld() {
        final Card skydiver = put("Merfolk Skydiver", ai, ZoneType.Hand);
        final SpellAbility sa = skydiver.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertNull(LegalityGuard.problem(ai, sa));
    }

    @Test
    public void journeyToNowhereIsStillHeldWithNoCreature() {
        final Card journey = put("Journey to Nowhere", ai, ZoneType.Hand);
        final SpellAbility sa = journey.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("etb-no-target", LegalityGuard.problem(ai, sa));
    }
}