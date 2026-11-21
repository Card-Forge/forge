package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.FlatMonteCarloPicker;
import forge.ai.simulation.Forecast;
import forge.ai.simulation.HandAbilities;
import forge.ai.simulation.SpecialActions;
import forge.ai.simulation.TutorPlan;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Hand tutors (transmute) and special actions (plot, foretell, suspend, turning a morph face up): what the AI
 * recognizes, what it may search for and when it holds an action back (see {@link TutorPlan} and {@link SpecialActions}).
 */
public class FlatMonteCarloSpecialActionTest extends AITest {
    private Game game;
    private Player ai;

    private void setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
    }

    /** The first ability of the card in its zone that satisfies the test. */
    private SpellAbility find(final Card c, final java.util.function.Predicate<SpellAbility> test) {
        for (final SpellAbility sa : c.getAllPossibleAbilities(ai, true)) {
            if (test.test(sa)) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no such ability on " + c);
    }

    @Test
    public void transmuteIsALibraryTutorThatFindsTheSameManaValue() {
        setUp();
        final Card infiltrator = addCardToZone("Dimir Infiltrator", ai, ZoneType.Hand);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        final Card giant = addCardToZone("Hill Giant", ai, ZoneType.Library);
        final SpellAbility transmute = find(infiltrator, sa -> sa.getApi() == forge.game.ability.ApiType.ChangeZone);
        AssertJUnit.assertTrue(TutorPlan.isHandAbility(transmute));
        AssertJUnit.assertTrue(TutorPlan.isLibraryTutor(transmute));
        final CardCollection pool = TutorPlan.pool(ai, transmute);
        AssertJUnit.assertTrue("a card of the same mana value", pool.contains(bears));
        AssertJUnit.assertFalse("not another one", pool.contains(giant));
    }

    @Test
    public void transmuteIsNotWorthACardAndManaWithoutAPlanTarget() {
        setUp();
        final Card infiltrator = addCardToZone("Dimir Infiltrator", ai, ZoneType.Hand);
        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        addCards("Island", 3, ai);
        final SpellAbility transmute = find(infiltrator, sa -> sa.getApi() == forge.game.ability.ApiType.ChangeZone);
        AssertJUnit.assertFalse("a card for a card, and the mana", TutorPlan.hasUse(ai, transmute));
    }

    @Test
    public void aSpellTutorWithAnythingToFindIsStillWorthIt() {
        setUp();
        final Card tutor = addCardToZone("Demonic Tutor", ai, ZoneType.Hand);
        addCardToZone("Hill Giant", ai, ZoneType.Library);
        final SpellAbility cast = find(tutor, SpellAbility::isSpell);
        AssertJUnit.assertFalse(TutorPlan.isHandAbility(cast));
        AssertJUnit.assertTrue(TutorPlan.hasUse(ai, cast));
    }

    @Test
    public void theDeferralsSaveWhatTheLaterCastWillNotPay() {
        setUp();
        final Card behold = addCardToZone("Behold the Multiverse", ai, ZoneType.Hand);
        final Card bolt = addCardToZone("Rift Bolt", ai, ZoneType.Hand);
        final Card show = addCardToZone("Slickshot Show-Off", ai, ZoneType.Hand);
        final SpellAbility foretell = find(behold, SpellAbility::isForetelling);
        final SpellAbility suspend = find(bolt, sa -> sa.isKeyword(forge.game.keyword.Keyword.SUSPEND));
        final SpellAbility plot = find(show, SpellAbility::isPlotting);
        AssertJUnit.assertTrue(SpecialActions.isDeferral(foretell));
        AssertJUnit.assertEquals("{3}{U} for {1}{U}", 2, SpecialActions.saving(foretell));
        AssertJUnit.assertEquals("a free cast", 2, SpecialActions.saving(plot));
        AssertJUnit.assertEquals("a free cast one turn later", 3, SpecialActions.saving(suspend));
    }

    @Test
    public void aDeferralIsOfferedInTheSecondMainWithManaToSpare() {
        setUp();
        final Card behold = addCardToZone("Behold the Multiverse", ai, ZoneType.Hand);
        addCards("Island", 4, ai);
        final SpellAbility foretell = find(behold, SpellAbility::isForetelling);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse("the first main phase is for what the mana is wanted for", SpecialActions.isDiscoverable(ai, foretell));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertTrue(SpecialActions.isDiscoverable(ai, foretell));
    }

    @Test
    public void aDeferralWaitsForACardThatTheManaIsWantedFor() {
        setUp();
        final Card behold = addCardToZone("Behold the Multiverse", ai, ZoneType.Hand);
        addCardToZone("Merfolk Looter", ai, ZoneType.Hand);
        addCards("Island", 2, ai); // {2} for the foretell leaves nothing for the 2-drop, which costs {1}{U}
        final SpellAbility foretell = find(behold, SpellAbility::isForetelling);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertFalse(SpecialActions.isDiscoverable(ai, foretell));
    }

    @Test
    public void aMorphWaitsForTheBlocksWhileItCouldStillAttack() {
        setUp();
        final Card angel = addCardToZone("Exalted Angel", ai, ZoneType.Battlefield);
        angel.turnFaceDown();
        angel.setSickness(false);
        final SpellAbility up = find(angel, SpellAbility::isTurnFaceUp);
        AssertJUnit.assertTrue(SpecialActions.isTurnFaceUp(up));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertTrue("the opponent would see it before the attack", SpecialActions.waitsForCombat(ai, up));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertFalse("after combat there is nothing to surprise", SpecialActions.waitsForCombat(ai, up));
    }

    @Test
    public void aMorphThatCannotAttackIsTurnedUpAsBefore() {
        setUp();
        final Card angel = addCardToZone("Exalted Angel", ai, ZoneType.Battlefield);
        angel.turnFaceDown();
        angel.setSickness(true);
        final SpellAbility up = find(angel, SpellAbility::isTurnFaceUp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse(SpecialActions.waitsForCombat(ai, up));
    }

    @Test
    public void landcyclingIsOfferedToAHandWithoutALand() {
        setUp();
        final Card vis = addCardToZone("Absorb Vis", ai, ZoneType.Hand);
        addCardToZone("Swamp", ai, ZoneType.Library);
        addCards("Swamp", 2, ai);
        final SpellAbility cycle = find(vis, sa -> sa.getApi() == forge.game.ability.ApiType.ChangeZone);
        AssertJUnit.assertTrue(HandAbilities.isLandcycling(cycle));
        AssertJUnit.assertFalse("not a spell tutor", TutorPlan.isLibraryTutor(cycle) && !TutorPlan.isHandAbility(cycle));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertTrue(HandAbilities.isDiscoverable(ai, cycle));
    }

    @Test
    public void landcyclingWaitsWhenALandIsInHand() {
        setUp();
        final Card vis = addCardToZone("Absorb Vis", ai, ZoneType.Hand);
        addCardToZone("Swamp", ai, ZoneType.Library);
        addCardToZone("Swamp", ai, ZoneType.Hand);
        addCards("Swamp", 2, ai);
        final SpellAbility cycle = find(vis, sa -> sa.getApi() == forge.game.ability.ApiType.ChangeZone);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse(HandAbilities.isDiscoverable(ai, cycle));
    }

    @Test
    public void reinforceIsATrickForAfterTheBlocks() {
        setUp();
        final Card krushok = addCardToZone("Bannerhide Krushok", ai, ZoneType.Hand);
        final SpellAbility reinforce = find(krushok, SpellAbility::isActivatedAbility);
        AssertJUnit.assertTrue(HandAbilities.isReinforce(reinforce));
        AssertJUnit.assertTrue(forge.ai.simulation.FlatMonteCarloPicker.isCombatTrick(reinforce));
        AssertJUnit.assertFalse("not a main phase play", forge.ai.simulation.FlatMonteCarloPicker.isDiscoverable(reinforce));
    }

    private SpellAbility forecastOfSkyHussar() {
        final Card hussar = addCardToZone("Sky Hussar", ai, ZoneType.Hand);
        // not through getAllPossibleAbilities: it leaves out an ability whose restrictions (the upkeep, once a turn) fail now
        for (final SpellAbility sa : hussar.getSpellAbilities()) {
            if (sa.hasParam("Forecast")) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no forecast ability on " + hussar);
    }

    @Test
    public void aForecastAbilityIsRecognizedInTheHandInTheUpkeep() {
        setUp();
        final SpellAbility forecast = forecastOfSkyHussar();
        AssertJUnit.assertTrue(Forecast.isForecast(forecast));
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        AssertJUnit.assertTrue(Forecast.beforeOwnMain(ai));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse(Forecast.beforeOwnMain(ai));
    }

    @Test
    public void aForecastThatTapsCreaturesThatWouldAttackIsKept() {
        setUp();
        final SpellAbility forecast = forecastOfSkyHussar();
        for (int i = 0; i < 2; i++) {
            addCardToZone("Savannah Lions", ai, ZoneType.Battlefield).setSickness(false);
        }
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        AssertJUnit.assertFalse("two attackers against an empty board: they are needed in the first main phase",
                Forecast.worthIt(ai, forecast));
    }

    @Test
    public void aForecastThatTapsCreaturesThatCannotAttackIsUsed() {
        setUp();
        final SpellAbility forecast = forecastOfSkyHussar();
        for (int i = 0; i < 2; i++) {
            addCardToZone("Savannah Lions", ai, ZoneType.Battlefield).setSickness(true);
        }
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        AssertJUnit.assertTrue("two creatures that came this turn have nothing else to do", Forecast.worthIt(ai, forecast));
    }

    @Test
    public void aForecastNeedsTheCreaturesItTaps() {
        setUp();
        final SpellAbility forecast = forecastOfSkyHussar();
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        AssertJUnit.assertFalse("no creatures to tap", Forecast.worthIt(ai, forecast));
    }

    // ------------------------------------------------------------------------------------------------ copy targets

    /** The first spell ability of the card, aimed at the target and put on the stack for the player. */
    private SpellAbility aimedOnTheStack(final String name, final Player owner, final forge.game.GameObject target) {
        final Card card = addCardToZone(name, owner, ZoneType.Hand);
        owner.getZone(ZoneType.Hand).remove(card);
        final SpellAbility sa = card.getFirstSpellAbility();
        sa.setActivatingPlayer(owner);
        if (target != null) {
            sa.resetTargets();
            sa.getTargets().add(target);
        }
        game.getStack().add(sa);
        return sa;
    }

    private SpellAbility copyAimedAt(final SpellAbility original, final forge.game.GameObject target) {
        final SpellAbility copy = original.copy(ai);
        copy.resetTargets();
        copy.getTargets().add(target);
        return copy;
    }

    @Test
    public void aCopyOfABounceSpellLeavesTheTargetOfTheOriginal() {
        setUp();
        final Card giant = addCardToZone("Hill Giant", game.getPlayers().get(0), ZoneType.Battlefield);
        final Card bears = addCardToZone("Grizzly Bears", game.getPlayers().get(0), ZoneType.Battlefield);
        final SpellAbility original = aimedOnTheStack("Unsummon", ai, giant);
        final SpellAbility copy = copyAimedAt(original, giant);
        AssertJUnit.assertTrue(CopyTargets.awayFromTheOthers(ai, copy));
        AssertJUnit.assertEquals("the other creature of the opponent", bears, copy.getTargets().getFirstTargetedCard());
    }

    @Test
    public void aCopyKeepsTheTargetWhenThereIsNoOther() {
        setUp();
        final Card giant = addCardToZone("Hill Giant", game.getPlayers().get(0), ZoneType.Battlefield);
        final SpellAbility original = aimedOnTheStack("Unsummon", ai, giant);
        final SpellAbility copy = copyAimedAt(original, giant);
        AssertJUnit.assertFalse(CopyTargets.awayFromTheOthers(ai, copy));
        AssertJUnit.assertEquals(giant, copy.getTargets().getFirstTargetedCard());
    }

    @Test
    public void aCopyOfBurnStaysOnTheTargetBecauseDamageAdds() {
        setUp();
        final Card giant = addCardToZone("Hill Giant", game.getPlayers().get(0), ZoneType.Battlefield);
        addCardToZone("Grizzly Bears", game.getPlayers().get(0), ZoneType.Battlefield);
        final SpellAbility original = aimedOnTheStack("Shock", ai, giant);
        final SpellAbility copy = copyAimedAt(original, giant);
        AssertJUnit.assertFalse(CopyTargets.repeatsDoNothing(copy));
        AssertJUnit.assertFalse(CopyTargets.awayFromTheOthers(ai, copy));
    }

    @Test
    public void aCopyOfACounterspellGoesForAnotherSpell() {
        setUp();
        final Player opp = game.getPlayers().get(0);
        final SpellAbility ring = aimedOnTheStack("Sol Ring", opp, null);
        final SpellAbility stone = aimedOnTheStack("Mind Stone", opp, null);
        final SpellAbility original = aimedOnTheStack("Consign to Memory", ai, ring);
        final SpellAbility copy = copyAimedAt(original, ring);
        AssertJUnit.assertTrue(CopyTargets.awayFromTheOthers(ai, copy));
        AssertJUnit.assertEquals(stone, copy.getTargets().getFirstTargetedSpell());
    }

    // ---------------------------------------------------------------------------------------- one trick, several targets

    @Test
    public void aTrickForTwoIsAimedAtTwoFightsAndAtOneForOne() {
        setUp();
        final Player opp = game.getPlayers().get(0);
        final Card bears1 = addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        final Card bears2 = addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        final Card block1 = addCardToZone("Grizzly Bears", opp, ZoneType.Battlefield);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai);
        final forge.game.combat.Combat combat = new forge.game.combat.Combat(ai);
        combat.addAttacker(bears1, opp);
        combat.addAttacker(bears2, opp);
        combat.addBlocker(bears1, block1);
        combat.setBlocked(bears1, true);
        game.getPhaseHandler().setCombat(combat);
        final SpellAbility trick = addCardToZone("Appeal to Eirdu", ai, ZoneType.Hand).getFirstSpellAbility();
        trick.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("two attackers, one blocked and one not: both are in the combat", FlatMonteCarloPicker.multiCombatTargets(ai, trick));
        AssertJUnit.assertEquals(2, trick.getTargets().size());
        trick.resetTargets();
        // one attacker only: the single-target path (combatTarget) is the one that decides
        combat.removeFromCombat(bears2);
        AssertJUnit.assertFalse(FlatMonteCarloPicker.multiCombatTargets(ai, trick));
        AssertJUnit.assertEquals(bears1, FlatMonteCarloPicker.combatTarget(ai, trick));
    }
}
