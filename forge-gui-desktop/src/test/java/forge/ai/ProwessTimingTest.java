package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.ai.simulation.FlatMcTestBase;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** A burn spell is cast before combat when a prowess creature is going to attack, and not otherwise. */
public class ProwessTimingTest extends FlatMcTestBase {

    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    private SpellAbility spellOf(final Card c) {
        final SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    @Test
    public void boltwaveIsCastInMain1WithAnAttackingProwessCreature() {
        put("Monastery Swiftspear", ai, ZoneType.Battlefield);
        final SpellAbility boltwave = spellOf(put("Boltwave", ai, ZoneType.Hand));
        at(PhaseType.MAIN1, ai);
        AssertJUnit.assertTrue(ProwessTiming.worthBeforeCombat(ai, boltwave));
    }

    @Test
    public void boltwaveAfterCombatGetsNoSuchPush() {
        put("Monastery Swiftspear", ai, ZoneType.Battlefield);
        final SpellAbility boltwave = spellOf(put("Boltwave", ai, ZoneType.Hand));
        at(PhaseType.MAIN2, ai);
        AssertJUnit.assertFalse(ProwessTiming.worthBeforeCombat(ai, boltwave));
    }

    @Test
    public void noProwessCreatureNoPush() {
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        final SpellAbility boltwave = spellOf(put("Boltwave", ai, ZoneType.Hand));
        at(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse(ProwessTiming.worthBeforeCombat(ai, boltwave));
    }

    @Test
    public void aTappedProwessCreatureGetsNoPush() {
        final Card swiftspear = put("Monastery Swiftspear", ai, ZoneType.Battlefield);
        swiftspear.setTapped(true);
        final SpellAbility boltwave = spellOf(put("Boltwave", ai, ZoneType.Hand));
        at(PhaseType.MAIN1, ai);
    }
}