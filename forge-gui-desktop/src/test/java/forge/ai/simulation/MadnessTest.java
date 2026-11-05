package forge.ai.simulation;

import java.util.Arrays;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import forge.ai.AiController;
import forge.ai.ComputerUtilCost;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Madness (PR #12508): the AI throws a card away to a discard outlet only when it is "safe and sure":
 * <ul>
 *   <li>the moment is right: instant-speed outlets (Wild Mongrel, Aquamoeba) at the opponent's end step,
 *       sorcery-speed outlets (Careful Study) in the AI's own second main phase, never anywhere else;</li>
 *   <li>the outlet's own mana cost AND the madness cost can be paid together, colours included;</li>
 *   <li>the card discarded is a madness card that can really be cast.</li>
 * </ul>
 * The tests name the behaviour they protect, so a failure says what went wrong. The window and payability tests call
 * ComboPlan directly (they do not depend on the AI profile); the "Integration" tests go through the AI the way the
 * game does and skip themselves when the profile has Monte Carlo or the deck plan switched off.
 */
public class MadnessTest extends FlatMcTestBase {

    @Test
    public void testAiRunsLikeARealFlatMcPlayer() {
        setUpGame();
        assertFalse(brain().usesFullSimulation());
    }

    // ===================================================================================== detection

    @Test
    public void deckWithMadnessCardsAndOutletsIsRecognised() {
        setUpGame();
        addMadnessDeck();
        assertTrue(ComboPlan.scan(ai).has(ComboPlan.Kind.MADNESS), "madness deck was not recognised");
    }

    @Test
    public void twoMadnessCardsAreNotAMadnessDeck() {
        setUpGame();
        put("Basking Rootwalla", 2, ai, ZoneType.Library);
        put("Wild Mongrel", 3, ai, ZoneType.Library);
        put("Grizzly Bears", 10, ai, ZoneType.Library);
        assertFalse(ComboPlan.scan(ai).has(ComboPlan.Kind.MADNESS), "2 madness cards are a stray, not a plan");
    }

    @Test
    public void twoOutletsAreNotAMadnessDeck() {
        setUpGame();
        put("Basking Rootwalla", 3, ai, ZoneType.Library);
        put("Wild Mongrel", 2, ai, ZoneType.Library);
        put("Grizzly Bears", 10, ai, ZoneType.Library);
        assertFalse(ComboPlan.scan(ai).has(ComboPlan.Kind.MADNESS), "madness cards without outlets are just creatures");
    }

    @Test
    public void madnessCardIsNeverProtectedFromDiscard() {
        setUpGame();
        addMadnessDeck();
        final Card rootwalla = put("Basking Rootwalla", ai, ZoneType.Hand);
        final CardCollection safe = ComboPlan.scan(ai).withoutProtected(collection(rootwalla));
        assertTrue(safe.contains(rootwalla), "a madness card is meant to be discarded");
    }

    @Test
    public void madnessCostIsReadFromTheCard() {
        setUpGame();
        assertEquals(ComboPlan.madnessCost(put("Basking Rootwalla", ai, ZoneType.Hand)), 0);
        assertEquals(ComboPlan.madnessCost(put("Arrogant Wurm", ai, ZoneType.Hand)), 3);
        assertEquals(ComboPlan.madnessCost(put("Grizzly Bears", ai, ZoneType.Hand)), -1, "no madness: no opinion");
    }

    // ===================================================================================== the window (when)

    /** Mongrel (free, instant speed) on the battlefield, Rootwalla (madness {0}) in hand, three Forests. */
    private SpellAbility readyBoard() {
        addMadnessDeck();
        put("Forest", 3, ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        put("Basking Rootwalla", ai, ZoneType.Hand);
        return discardAbilityOf(mongrel);
    }

    // Build this lazily: PhaseType localizes its labels during class initialization, so referencing its constants
    // in a static field can initialize it before Forge's Localizer has loaded its ResourceBundle.
    private static PhaseType[] allPhases() {
        return new PhaseType[] { PhaseType.UPKEEP, PhaseType.DRAW, PhaseType.MAIN1,
                PhaseType.COMBAT_BEGIN, PhaseType.COMBAT_DECLARE_ATTACKERS, PhaseType.COMBAT_DECLARE_BLOCKERS,
                PhaseType.COMBAT_DAMAGE, PhaseType.MAIN2, PhaseType.END_OF_TURN };
    }

    @Test
    public void instantSpeedOutletIsNeverUsedOnTheAisOwnTurn() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        final ComboPlan plan = ComboPlan.scan(ai);
        for (final PhaseType phase : allPhases()) {
            at(phase, ai);
            assertFalse(plan.madnessWindowOpen(ai, mongrel), "Mongrel must not discard on the AI's own turn: " + phase);
        }
    }

    @Test
    public void instantSpeedOutletOpensOnlyAtTheOpponentsEndStep() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        final ComboPlan plan = ComboPlan.scan(ai);
        for (final PhaseType phase : allPhases()) {
            at(phase, opp);
            final boolean expected = phase == PhaseType.END_OF_TURN;
            assertEquals(plan.madnessWindowOpen(ai, mongrel), expected, "opponent's " + phase);
        }
    }

    @Test
    public void sorcerySpeedOutletOpensOnlyInTheAisOwnSecondMain() {
        setUpGame();
        addMadnessDeck();
        put("Island", 4, ai, ZoneType.Battlefield);
        final Card study = put("Careful Study", ai, ZoneType.Hand);
        put("Basking Rootwalla", ai, ZoneType.Hand);
        final SpellAbility sa = study.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        final ComboPlan plan = ComboPlan.scan(ai);
        for (final PhaseType phase : allPhases()) {
            at(phase, ai);
            assertEquals(plan.madnessWindowOpen(ai, sa), phase == PhaseType.MAIN2, "own " + phase);
            at(phase, opp);
            assertFalse(plan.madnessWindowOpen(ai, sa), "a sorcery is not cast on the opponent's turn: " + phase);
        }
    }

    @Test
    public void windowStaysClosedWithSomethingOnTheStack() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        final ComboPlan plan = ComboPlan.scan(ai);
        at(PhaseType.END_OF_TURN, opp);
        assertTrue(plan.madnessWindowOpen(ai, mongrel), "precondition: the window is open");
        final Card bolt = put("Lightning Bolt", opp, ZoneType.Hand);
        final SpellAbility boltSa = bolt.getFirstSpellAbility();
        boltSa.setActivatingPlayer(opp);
        boltSa.getTargets().add(ai);
        game.getStack().add(boltSa);
        assertFalse(plan.madnessWindowOpen(ai, mongrel), "never start an outlet play with a spell on the stack");
    }

    // ===================================================================================== payability (what)

    @Test
    public void freeMadnessCardIsCastableWithAFreeOutletAndNoLands() {
        setUpGame();
        addMadnessDeck();
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        final Card rootwalla = put("Basking Rootwalla", ai, ZoneType.Hand);
        final CardCollection castable = ComboPlan.scan(ai).castableMadness(ai, discardAbilityOf(mongrel));
        assertTrue(castable.contains(rootwalla), "{0} madness needs no mana at all");
    }

    @Test
    public void madnessCostNeedsEnoughMana() {
        setUpGame();
        addMadnessDeck();
        put("Forest", 2, ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        final Card wurm = put("Arrogant Wurm", ai, ZoneType.Hand);
        assertFalse(ComboPlan.scan(ai).castableMadness(ai, discardAbilityOf(mongrel)).contains(wurm),
                "{2}{G} madness with two lands is a wasted card");
        put("Forest", ai, ZoneType.Battlefield);
        assertTrue(ComboPlan.scan(ai).castableMadness(ai, discardAbilityOf(mongrel)).contains(wurm),
                "with the third land it can be paid");
    }

    @Test
    public void madnessCostNeedsTheRightColour() {
        setUpGame();
        addMadnessDeck();
        put("Island", 3, ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        final Card wurm = put("Arrogant Wurm", ai, ZoneType.Hand);
        assertFalse(ComboPlan.scan(ai).castableMadness(ai, discardAbilityOf(mongrel)).contains(wurm),
                "three Islands cannot pay {2}{G}: three mana is not enough, the colour decides");
    }

    @Test
    public void outletManaCostIsCountedOnTopOfTheMadnessCost() {
        setUpGame();
        addMadnessDeck();
        put("Island", ai, ZoneType.Battlefield);
        put("Forest", 2, ai, ZoneType.Battlefield);
        final Card study = put("Careful Study", ai, ZoneType.Hand); // {U}
        final Card wurm = put("Arrogant Wurm", ai, ZoneType.Hand);  // madness {2}{G}
        final SpellAbility sa = study.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        assertFalse(ComboPlan.scan(ai).castableMadness(ai, sa).contains(wurm),
                "{U} for the outlet plus {2}{G} for madness is four mana; the AI has three lands");
        put("Forest", ai, ZoneType.Battlefield);
        assertTrue(ComboPlan.scan(ai).castableMadness(ai, sa).contains(wurm), "four lands pay both");
    }

    @Test
    public void theOutletIsNeverItsOwnMadnessTarget() {
        setUpGame();
        addMadnessDeck();
        put("Island", 2, ai, ZoneType.Battlefield);
        final Card study = put("Careful Study", ai, ZoneType.Hand);
        final SpellAbility sa = study.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        assertTrue(ComboPlan.scan(ai).castableMadness(ai, sa).isEmpty(), "no madness card in hand: nothing to plan");
    }

    // ===================================================================================== which card

    @Test
    public void madnessDiscardPicksACastableCardAndSkipsTheOthers() {
        setUpGame();
        addMadnessDeck();
        put("Forest", ai, ZoneType.Battlefield);
        final Card rootwalla = put("Basking Rootwalla", ai, ZoneType.Hand);
        final Card wurm = put("Arrogant Wurm", ai, ZoneType.Hand);
        final Card chosen = ComboPlan.scan(ai).madnessDiscard(ai, collection(rootwalla, wurm));
        assertSame(chosen, rootwalla, "with one land only the {0} madness card can be cast");
    }

    @Test
    public void madnessDiscardPicksTheBestCastableCard() {
        setUpGame();
        addMadnessDeck();
        put("Forest", 3, ai, ZoneType.Battlefield);
        final Card rootwalla = put("Basking Rootwalla", ai, ZoneType.Hand);
        final Card wurm = put("Arrogant Wurm", ai, ZoneType.Hand);
        final Card chosen = ComboPlan.scan(ai).madnessDiscard(ai, collection(rootwalla, wurm));
        assertSame(chosen, wurm, "both can be cast: the 6/4 is worth more than the 2/2");
    }

    @Test
    public void madnessDiscardHasNoOpinionWhenNothingCanBeCast() {
        setUpGame();
        addMadnessDeck();
        put("Forest", ai, ZoneType.Battlefield);
        final Card wurm = put("Arrogant Wurm", ai, ZoneType.Hand);
        final Card bears = put("Grizzly Bears", ai, ZoneType.Hand);
        assertNull(ComboPlan.scan(ai).madnessDiscard(ai, collection(wurm, bears)),
                "a madness card that cannot be paid for is a plain discard: the normal choice decides");
    }

    @Test
    public void madnessDiscardIgnoresCardsWithoutMadness() {
        setUpGame();
        addMadnessDeck();
        final Card bears = put("Grizzly Bears", ai, ZoneType.Hand);
        assertNull(ComboPlan.scan(ai).madnessDiscard(ai, collection(bears)));
    }

    // ===================================================================================== fuel / ordering

    @Test
    public void outletIsNoFuelPlayOutsideTheWindow() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        final ComboPlan plan = ComboPlan.scan(ai);
        at(PhaseType.MAIN1, ai);
        assertFalse(plan.isFuelPlay(mongrel), "the old code put the outlet first in Main 1: that was the reckless play");
        at(PhaseType.MAIN2, ai);
        assertFalse(plan.isFuelPlay(mongrel), "an instant-speed outlet waits for the opponent's end step");
        at(PhaseType.END_OF_TURN, opp);
        assertTrue(plan.isFuelPlay(mongrel), "inside the window it is the play");
    }

    @Test
    public void outletIsNoFuelPlayWhenTheMadnessCardCannotBePaidFor() {
        setUpGame();
        addMadnessDeck();
        put("Forest", ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        put("Arrogant Wurm", ai, ZoneType.Hand);
        at(PhaseType.END_OF_TURN, opp);
        assertFalse(ComboPlan.scan(ai).isFuelPlay(discardAbilityOf(mongrel)),
                "right moment, but {2}{G} cannot be paid with one Forest");
    }

    @Test
    public void fuelPlaysAreOnlyBroughtForwardInTheAisSecondMain() {
        setUpGame();
        readyBoard();
        final ComboPlan plan = ComboPlan.scan(ai);
        at(PhaseType.MAIN1, ai);
        assertFalse(plan.fuelFirst(ai), "no reordering of the plays in Main 1");
        at(PhaseType.COMBAT_DECLARE_ATTACKERS, ai);
        assertFalse(plan.fuelFirst(ai));
        at(PhaseType.END_OF_TURN, opp);
        assertFalse(plan.fuelFirst(ai), "the opponent's turn is not a main-phase ordering question");
    }

    @Test
    public void sureMadnessPlayAtTheEndStep() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        final ComboPlan plan = ComboPlan.scan(ai);
        at(PhaseType.END_OF_TURN, opp);
        assertSame(plan.sureMadnessPlay(ai, Arrays.asList(mongrel)), mongrel);
    }

    @Test
    public void sureMadnessPlayIsNullEverywhereElse() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        final ComboPlan plan = ComboPlan.scan(ai);
        for (final PhaseType phase : allPhases()) {
            at(phase, ai);
            assertNull(plan.sureMadnessPlay(ai, Arrays.asList(mongrel)), "own " + phase);
            if (phase != PhaseType.END_OF_TURN) {
                at(phase, opp);
                assertNull(plan.sureMadnessPlay(ai, Arrays.asList(mongrel)), "opponent's " + phase);
            }
        }
    }

    @Test
    public void sureMadnessPlayIsNullWithoutAMadnessCardInHand() {
        setUpGame();
        addMadnessDeck();
        put("Forest", 3, ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        put("Grizzly Bears", ai, ZoneType.Hand);
        at(PhaseType.END_OF_TURN, opp);
        assertNull(ComboPlan.scan(ai).sureMadnessPlay(ai, Arrays.asList(discardAbilityOf(mongrel))),
                "discarding a plain card for a +1/+1 at the end step is not madness");
    }

    // ===================================================================================== the discard cost

    /** The cost check is what made every outlet look affordable once a madness card was in hand. */
    @Test
    public void discardCostForAMadnessCardIsRefusedOutsideTheWindow() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        requireMadnessPlan();
        at(PhaseType.MAIN1, ai);
        assertFalse(ComputerUtilCost.checkDiscardCost(ai, mongrel.getPayCosts(), mongrel.getHostCard(), mongrel),
                "Main 1: throwing the madness card away now is the reckless play");
        at(PhaseType.COMBAT_DECLARE_BLOCKERS, opp);
        assertFalse(ComputerUtilCost.checkDiscardCost(ai, mongrel.getPayCosts(), mongrel.getHostCard(), mongrel),
                "during the opponent's combat the pump is not what madness is for");
    }

    @Test
    public void discardCostForAMadnessCardIsAcceptedInsideTheWindow() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        requireMadnessPlan();
        at(PhaseType.END_OF_TURN, opp);
        assertTrue(ComputerUtilCost.checkDiscardCost(ai, mongrel.getPayCosts(), mongrel.getHostCard(), mongrel));
    }

    @Test
    public void discardCostWithoutAMadnessDeckIsUnchanged() {
        setUpGame();
        addPlainDeck();
        put("Forest", 3, ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        put("Grizzly Bears", ai, ZoneType.Hand);
        at(PhaseType.END_OF_TURN, opp);
        final SpellAbility sa = discardAbilityOf(mongrel);
        assertFalse(ComputerUtilCost.checkDiscardCost(ai, sa.getPayCosts(), mongrel, sa),
                "no DiscardMe card and no madness: the AI does not throw cards away, as before the patch");
    }

    // ===================================================================================== integration

    private void requireMadnessPlan() {
        if (!brain().deckPlan(ai).combos.has(ComboPlan.Kind.MADNESS)) {
            throw new SkipException("deck plan is switched off in the test AI profile (FLAT_MC_DECK_PLAN)");
        }
    }

    private List<SpellAbility> choose(final boolean flatMc) {
        final AiController brain = brain();
        brain.setFlatMonteCarloOverride(flatMc);
        if (flatMc && !brain.flatMonteCarloActive()) {
            throw new SkipException("Flat Monte Carlo is switched off in the test AI profile (FLAT_MC_ENABLED)");
        }
        return brain.chooseSpellAbilityToPlay();
    }

    private static boolean choosesAnOutlet(final List<SpellAbility> chosen) {
        if (chosen != null) {
            for (final SpellAbility sa : chosen) {
                if (isDiscardAbility(sa)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The regression from the video: the AI discarded in the middle of its own turn, with or without Monte Carlo. */
    @Test
    public void integrationAiNeverThrowsACardAwayDuringItsOwnTurn() {
        for (final boolean flatMc : new boolean[] { false, true }) {
            setUpGame();
            readyBoard();
            requireMadnessPlan();
            for (final PhaseType phase : new PhaseType[] { PhaseType.MAIN1, PhaseType.COMBAT_DECLARE_ATTACKERS,
                    PhaseType.COMBAT_DECLARE_BLOCKERS, PhaseType.MAIN2 }) {
                at(phase, ai);
                final List<SpellAbility> chosen = choose(flatMc);
                assertFalse(choosesAnOutlet(chosen), "flatMc=" + flatMc + ", own " + phase + ", the AI chose " + chosen
                        + " (stack " + game.getStack().size() + ", plan " + brain().deckPlan(ai).combos + ")");
            }
        }
    }

    @Test
    public void integrationAiNeverThrowsACardAwayDuringTheOpponentsTurnBeforeTheEndStep() {
        setUpGame();
        readyBoard();
        requireMadnessPlan();
        for (final PhaseType phase : new PhaseType[] { PhaseType.UPKEEP, PhaseType.DRAW, PhaseType.MAIN1,
                PhaseType.COMBAT_BEGIN, PhaseType.COMBAT_DECLARE_ATTACKERS, PhaseType.COMBAT_DECLARE_BLOCKERS,
                PhaseType.MAIN2 }) {
            at(phase, opp);
            assertFalse(choosesAnOutlet(choose(true)), "opponent's " + phase);
        }
    }

    @Test
    public void integrationAiUsesTheOutletAtTheOpponentsEndStep() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        requireMadnessPlan();
        at(PhaseType.END_OF_TURN, opp);
        final List<SpellAbility> chosen = choose(true);
        assertTrue(choosesAnOutlet(chosen), "mana open, madness card castable, right moment: that is the play");
        assertSame(chosen.get(0).getHostCard(), mongrel.getHostCard());
    }

    @Test
    public void integrationAiDoesNotDiscardAnUncastableMadnessCardAtTheEndStep() {
        setUpGame();
        addMadnessDeck();
        put("Forest", ai, ZoneType.Battlefield);
        put("Wild Mongrel", ai, ZoneType.Battlefield);
        put("Arrogant Wurm", ai, ZoneType.Hand);
        requireMadnessPlan();
        at(PhaseType.END_OF_TURN, opp);
        assertFalse(choosesAnOutlet(choose(true)), "{2}{G} cannot be paid: the Wurm would just be lost");
    }

    // ===================================================================================== tutors

    @Test
    public void tutorFetchesAnOutletWhenAMadnessCardWaitsInHand() {
        setUpGame();
        addMadnessDeck();
        put("Basking Rootwalla", ai, ZoneType.Hand);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Library);
        final Card bears = put("Grizzly Bears", ai, ZoneType.Library);
        assertSame(ComboPlan.scan(ai).tutorPick(ai, collection(bears, mongrel)), mongrel);
    }

    @Test
    public void tutorFetchesAMadnessCardWhenAnOutletIsOut() {
        setUpGame();
        addMadnessDeck();
        put("Wild Mongrel", ai, ZoneType.Battlefield);
        final Card rootwalla = put("Basking Rootwalla", ai, ZoneType.Library);
        final Card bears = put("Grizzly Bears", ai, ZoneType.Library);
        assertSame(ComboPlan.scan(ai).tutorPick(ai, collection(bears, rootwalla)), rootwalla);
    }

    @Test
    public void intuitionAsksForThreeDifferentUsefulCards() {
        setUpGame();
        addMadnessDeck();
        final Card r1 = put("Basking Rootwalla", ai, ZoneType.Library);
        final Card r2 = put("Basking Rootwalla", ai, ZoneType.Library);
        final Card wurm = put("Arrogant Wurm", ai, ZoneType.Library);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Library);
        final Card bears = put("Grizzly Bears", ai, ZoneType.Library);
        final CardCollection picks = ComboPlan.scan(ai).tutorPicks(ai, collection(r1, r2, wurm, mongrel, bears), 3);
        assertTrue(picks != null && picks.size() == 3);
        assertFalse(picks.contains(bears), "a vanilla creature is not worth a slot");
        assertFalse(picks.contains(r1) && picks.contains(r2), "two copies of one card leave the opponent an easy choice");
    }

    @Test
    public void tutorHasNoOpinionWithoutAMadnessOrAtogDeck() {
        setUpGame();
        addPlainDeck();
        final Card bears = put("Grizzly Bears", ai, ZoneType.Library);
        assertNull(ComboPlan.scan(ai).tutorPick(ai, collection(bears)));
    }

    @Test
    public void forcedDiscardThrowsAwayTheCastableMadnessCard() {
        setUpGame();
        addMadnessDeck();
        final Card rootwalla = put("Basking Rootwalla", ai, ZoneType.Hand);
        final Card bears = put("Grizzly Bears", ai, ZoneType.Hand);
        final CardCollection chosen = ComboPlan.scan(ai).preferredDiscards(ai, collection(bears, rootwalla), 1);
        assertTrue(chosen != null && chosen.contains(rootwalla), "madness {0}: the discard is a free cast");
    }

    // ===================================================================================== end to end

    /**
     * The whole chain in a simulated game: discard to Wild Mongrel, madness trigger, the AI pays and casts. If the
     * Rootwalla ends in the graveyard, the madness trigger is not being paid (PlayerControllerAi.playSaFromPlayEffect).
     */
    @Test
    public void endToEndMadnessCardIsCastNotLost() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        at(PhaseType.END_OF_TURN, opp);
        final GameSimulator sim = createSimulator(ai);
        sim.simulateSpellAbility(mongrel);
        final Game after = sim.getSimulatedGameState();
        boolean onBattlefield = false;
        boolean inGraveyard = false;
        for (final Card c : after.getCardsIn(ZoneType.Battlefield)) {
            onBattlefield |= "Basking Rootwalla".equals(c.getName()) && c.getController().getName().equals(ai.getName());
        }
        for (final Card c : after.getCardsIn(ZoneType.Graveyard)) {
            inGraveyard |= "Basking Rootwalla".equals(c.getName());
        }
        assertTrue(onBattlefield, "the madness card should have been cast");
        assertFalse(inGraveyard, "the madness card must not be lost to the graveyard");
        assertNotNull(after);
    }
}