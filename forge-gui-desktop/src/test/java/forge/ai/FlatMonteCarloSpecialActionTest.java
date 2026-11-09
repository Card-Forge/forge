package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

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
}
