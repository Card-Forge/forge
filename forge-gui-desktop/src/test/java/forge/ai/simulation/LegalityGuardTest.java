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

    // ------------------------------------------------------------------------------------------ sacrifice unless

    @Test
    public void faerieImpostorAloneWouldOnlySacrificeItself() {
        final SpellAbility impostor = spellOf(put("Faerie Impostor", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("etb-self-sacrifice", LegalityGuard.problem(ai, impostor));
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull(LegalityGuard.problem(ai, impostor));
    }

    // ------------------------------------------------------------------------------------------ X of nothing

    @Test
    public void anXCreatureWithNoManaForXIsHeld() {
        put("Forest", ai, ZoneType.Battlefield);
        final SpellAbility scourge = spellOf(put("Wildwood Scourge", ai, ZoneType.Hand)); // {X}{G}: one land is X = 0
        AssertJUnit.assertEquals("x-zero", LegalityGuard.problem(ai, scourge));
        put("Forest", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull(LegalityGuard.problem(ai, scourge));
    }

    // ------------------------------------------------------------------------------------------ lands

    @Test
    public void templeOfTheFalseGodIsDormantUntilFourOtherLandsAreInPlay() {
        final Card temple = put("Temple of the False God", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(LegalityGuard.isDormantLand(ai, temple));
        put("Forest", 3, ai, ZoneType.Battlefield);
        AssertJUnit.assertTrue(LegalityGuard.isDormantLand(ai, temple));
        put("Forest", ai, ZoneType.Battlefield);
        AssertJUnit.assertFalse(LegalityGuard.isDormantLand(ai, temple)); // the fifth land: it works at once
        AssertJUnit.assertFalse(LegalityGuard.isDormantLand(ai, put("Forest", ai, ZoneType.Hand)));
    }

    // ------------------------------------------------------------------------------------------ chosen numbers

    @Test
    public void aNumberThatFeedsATokenForTheAiIsTheHighestOne() {
        final Card pia = put("Pia Nalaar, Chief Mechanic", ai, ZoneType.Battlefield);
        SpellAbility choose = null;
        for (final forge.game.trigger.Trigger t : pia.getTriggers()) {
            final SpellAbility a = t.ensureAbility();
            if (a != null && a.getApi() == forge.game.ability.ApiType.ChooseNumber) {
                choose = a;
            }
        }
        AssertJUnit.assertNotNull(choose);
        choose.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(4, forge.ai.NumberChoice.choose(choose, java.util.Arrays.asList(1, 2, 3, 4), ai));
    }

    @Test
    public void aNumberNothingReadsIsTheFirstOption() {
        final Card void_ = put("Void", ai, ZoneType.Hand);
        final SpellAbility sa = spellOf(void_);
        AssertJUnit.assertEquals(0, forge.ai.NumberChoice.choose(sa, java.util.Arrays.asList(0, 1, 2), ai));
    }

    // ------------------------------------------------------------------------------------------ a second copy

    @Test
    public void aSecondResonatingLuteIsHeldAndTheFirstIsNot() {
        final SpellAbility first = spellOf(put("Resonating Lute", ai, ZoneType.Hand));
        AssertJUnit.assertNull(LegalityGuard.problem(ai, first));
        put("Resonating Lute", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals("redundant-copy", LegalityGuard.problem(ai, first));
    }

    @Test
    public void aSecondCopyOfAStackingEffectIsFine() {
        put("Glorious Anthem", ai, ZoneType.Battlefield);
        final SpellAbility second = spellOf(put("Glorious Anthem", ai, ZoneType.Hand));
        AssertJUnit.assertNull(LegalityGuard.problem(ai, second));
    }

    // ------------------------------------------------------------------------------------------ repeating what is done

    private static void counters(final Card c, final String type, final int n, final forge.game.player.Player ai) {
        c.addCounterInternal(forge.game.card.CounterType.getType(type), n, ai, false, null, null);
    }

    private SpellAbility adapt(final Card emperor) {
        for (final SpellAbility sa : emperor.getSpellAbilities()) {
            if (sa.hasParam("Adapt")) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new AssertionError("no Adapt ability");
    }

    @Test
    public void adaptIsNotActivatedAgainWhileTheCreatureHasCounters() {
        final Card emperor = put("Emperor of Bones", ai, ZoneType.Battlefield);
        final SpellAbility adapt = adapt(emperor);
        AssertJUnit.assertNull(ActivationGuard.problem(ai, adapt));
        counters(emperor, "P1P1", 2, ai);
        AssertJUnit.assertEquals("nothing-to-affect", ActivationGuard.problem(ai, adapt));
    }

    @Test
    public void monstrosityIsNotActivatedAgain() {
        final Card lion = put("Fleecemane Lion", ai, ZoneType.Battlefield);
        SpellAbility monstrosity = null;
        for (final SpellAbility sa : lion.getSpellAbilities()) {
            if (sa.hasParam("Monstrosity")) {
                sa.setActivatingPlayer(ai);
                monstrosity = sa;
            }
        }
        AssertJUnit.assertNotNull(monstrosity);
        AssertJUnit.assertNull(ActivationGuard.problem(ai, monstrosity));
        lion.setMonstrous(true);
        AssertJUnit.assertEquals("nothing-to-affect", ActivationGuard.problem(ai, monstrosity));
    }

    @Test
    public void aKeywordTheCreatureHasIsNotGrantedAgain() {
        final Card angel = put("Serra Angel", ai, ZoneType.Battlefield); // flying
        final SpellAbility fly = forge.game.ability.AbilityFactory.getAbility("AB$ Pump | Cost$ 1 | Defined$ Self | KW$ Flying", angel);
        fly.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("nothing-to-affect", ActivationGuard.problem(ai, fly));
        final SpellAbility trample = forge.game.ability.AbilityFactory.getAbility("AB$ Pump | Cost$ 1 | Defined$ Self | KW$ Trample", angel);
        trample.setActivatingPlayer(ai);
        AssertJUnit.assertNull(ActivationGuard.problem(ai, trample));
    }

    // ------------------------------------------------------------------------------------------ defeating a battle

    @Test
    public void aBattleIsNotDefeatedForALegendThatIsAlreadyInPlay() {
        final Card battle = put("Invasion of Ikoria", ai, ZoneType.Battlefield);
        // a Siege may already come with its defense counters: work from what is there
        if (battle.getCounters(forge.game.card.CounterEnumType.DEFENSE) == 0) {
            counters(battle, "DEFENSE", 6, ai);
        }
        final int defense = battle.getCounters(forge.game.card.CounterEnumType.DEFENSE);
        AssertJUnit.assertFalse("no Zilortha yet", LegalityGuard.redundantDefeat(ai, battle, defense + 4));
        // the legend is the back face of a second Siege (a card is not found by its back name)
        final Card zilortha = put("Invasion of Ikoria", ai, ZoneType.Battlefield);
        zilortha.setState(forge.card.CardStateName.Backside, true);
        AssertJUnit.assertEquals("Zilortha, Apex of Ikoria", zilortha.getName());
        AssertJUnit.assertTrue(LegalityGuard.redundantDefeat(ai, battle, defense + 4));
        AssertJUnit.assertFalse("the attack cannot take the last counter", LegalityGuard.redundantDefeat(ai, battle, defense - 1));
        counters(zilortha, "STUN", 1, ai);
        AssertJUnit.assertFalse("Zilortha does nothing", LegalityGuard.redundantDefeat(ai, battle, defense + 4));
    }

    // ------------------------------------------------------------------------------------------ Birthing Ritual

    private boolean ritualWouldSacrifice() {
        final Card ritual = put("Birthing Ritual", ai, ZoneType.Battlefield);
        final SpellAbility sac = forge.game.ability.AbilityFactory.getAbility(ritual.getSVar("DBSacrifice"), ritual);
        sac.setActivatingPlayer(ai);
        return forge.ai.ability.SacrificeAi.birthingRitualPaysOff(ai, sac);
    }

    @Test
    public void birthingRitualDoesNotSacrificeWithNothingToPutInItsPlace() {
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        put("Forest", 7, ai, ZoneType.Library);
        AssertJUnit.assertFalse(ritualWouldSacrifice());
    }

    @Test
    public void birthingRitualDoesNotSacrificeForACreatureThatIsTooExpensive() {
        put("Grizzly Bears", ai, ZoneType.Battlefield); // mana value 2: up to 3
        put("Forest", 6, ai, ZoneType.Library);
        put("Serra Angel", ai, ZoneType.Library);
        AssertJUnit.assertFalse(ritualWouldSacrifice());
    }

    @Test
    public void birthingRitualSacrificesForABetterCreatureItCanPut() {
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        put("Forest", 6, ai, ZoneType.Library);
        put("Centaur Courser", ai, ZoneType.Library); // mana value 3, 3/3
        AssertJUnit.assertTrue(ritualWouldSacrifice());
    }
}
