package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.ai.ComputerUtilCombat;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Plays the AI must not make: spells the game forbids right now (Grand Abolisher), enters-the-battlefield removal and
 * reanimation with nothing to aim at (Journey to Nowhere, Oblivion Ring, Necromancy), and the damage arithmetic under
 * Doran, the Siege Tower (creatures assign combat damage equal to toughness).
 */
public class LegalityGuardTest extends FlatMcTestBase {

    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    private SpellAbility spellOf(final Card c) {
        final SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    // ------------------------------------------------------------------------------------------ targetless enters

    @Test
    public void journeyToNowhereWithNoCreatureIsHeld() {
        final SpellAbility journey = spellOf(put("Journey to Nowhere", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("etb-no-target", LegalityGuard.problem(ai, journey));
    }

    @Test
    public void journeyToNowhereWithOnlyItsOwnCreatureIsHeld() {
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        final SpellAbility journey = spellOf(put("Journey to Nowhere", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("etb-no-target", LegalityGuard.problem(ai, journey));
    }

    @Test
    public void journeyToNowhereWithAnOpposingCreatureIsFine() {
        put("Grizzly Bears", opp, ZoneType.Battlefield);
        final SpellAbility journey = spellOf(put("Journey to Nowhere", ai, ZoneType.Hand));
        AssertJUnit.assertNull(LegalityGuard.problem(ai, journey));
    }

    @Test
    public void oblivionRingWithNothingElseInPlayIsHeld() {
        final SpellAbility ring = spellOf(put("Oblivion Ring", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("etb-no-target", LegalityGuard.problem(ai, ring));
    }

    @Test
    public void banisherPriestNeedsAnOpposingCreature() {
        final SpellAbility priest = spellOf(put("Banisher Priest", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("etb-no-target", LegalityGuard.problem(ai, priest));
        put("Grizzly Bears", opp, ZoneType.Battlefield);
        AssertJUnit.assertNull(LegalityGuard.problem(ai, priest));
    }

    @Test
    public void necromancyNeedsACreatureInAGraveyard() {
        final SpellAbility necro = spellOf(put("Necromancy", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("etb-no-target", LegalityGuard.problem(ai, necro));
        put("Grizzly Bears", opp, ZoneType.Graveyard);
        AssertJUnit.assertNull(LegalityGuard.problem(ai, necro));
    }

    // ------------------------------------------------------------------------------------------ cast restrictions

    @Test
    public void grandAbolisherStopsTheAiFromCastingOnTheOpponentsTurn() {
        put("Grand Abolisher", opp, ZoneType.Battlefield);
        put("Island", 2, ai, ZoneType.Battlefield);
        final SpellAbility negate = spellOf(put("Negate", ai, ZoneType.Hand));
        at(PhaseType.MAIN1, opp);
        AssertJUnit.assertEquals("cast-restricted", LegalityGuard.problem(ai, negate));
    }

    @Test
    public void withoutGrandAbolisherNegateIsLegal() {
        put("Island", 2, ai, ZoneType.Battlefield);
        final SpellAbility negate = spellOf(put("Negate", ai, ZoneType.Hand));
        at(PhaseType.MAIN1, opp);
        AssertJUnit.assertNull(LegalityGuard.problem(ai, negate));
    }

    // ------------------------------------------------------------------------------------------ Doran

    @Test
    public void doranMakesToughnessTheCombatDamage() {
        final Card doran = put("Doran, the Siege Tower", ai, ZoneType.Battlefield); // 0/5
        final Card wall = put("Wall of Stone", ai, ZoneType.Battlefield); // 0/8
        AssertJUnit.assertEquals(8, wall.getNetCombatDamage());
        AssertJUnit.assertEquals(8, ComputerUtilCombat.damageIfUnblocked(wall, opp, null, true));
        AssertJUnit.assertEquals(13, ComputerUtilCombat.totalCombatDamage(java.util.Arrays.asList(wall, doran)));
    }

    @Test
    public void aToughnessPumpIsDamageUnderDoran() {
        put("Doran, the Siege Tower", ai, ZoneType.Battlefield);
        final Card wall = put("Wall of Stone", ai, ZoneType.Battlefield);
        AssertJUnit.assertTrue(wall.toughnessAssignsDamage());
        AssertJUnit.assertTrue(ComputerUtilCombat.attackerDamageBonus(wall, null, true) >= 0);
    }
}
