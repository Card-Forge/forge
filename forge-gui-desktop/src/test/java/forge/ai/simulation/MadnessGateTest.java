package forge.ai.simulation;

import org.testng.SkipException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import forge.ai.AiPlayDecision;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The single choke point for madness timing: AiController.canPlaySa refuses a discard outlet whenever a castable
 * madness card would be the one thrown away and the moment is wrong (ComboPlan.outletMustWait).
 * <p>
 * Whatever the heuristics are (PumpAi, DiscardAi, fuel ordering, Monte Carlo revalidation), they all end in canPlaySa,
 * so these tests hold for every path at once. Written after MadnessTest.integrationAiNeverThrowsACardAwayDuringItsOwnTurn
 * failed in the AI's own Main 2 with Monte Carlo off.
 */
public class MadnessGateTest extends FlatMcTestBase {

    // PhaseType localizes its labels when the class initializes: build the arrays lazily, not in static fields.
    private static PhaseType[] phases() {
        return new PhaseType[] { PhaseType.UPKEEP, PhaseType.DRAW, PhaseType.MAIN1, PhaseType.COMBAT_BEGIN,
                PhaseType.COMBAT_DECLARE_ATTACKERS, PhaseType.COMBAT_DECLARE_BLOCKERS, PhaseType.COMBAT_DAMAGE,
                PhaseType.MAIN2, PhaseType.END_OF_TURN };
    }

    /** Mongrel on the battlefield, Rootwalla ({0} madness) in hand, three Forests: the discard is a free cast. */
    private SpellAbility readyBoard() {
        addMadnessDeck();
        put("Forest", 3, ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        put("Basking Rootwalla", ai, ZoneType.Hand);
        return discardAbilityOf(mongrel);
    }

    private void requireMadnessPlan() {
        if (!brain().deckPlan(ai).combos.has(ComboPlan.Kind.MADNESS)) {
            throw new SkipException("deck plan is switched off in the test AI profile (FLAT_MC_DECK_PLAN)");
        }
    }

    // ===================================================================================== outletMustWait

    @Test
    public void outletMustWaitOnTheAisOwnTurn() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        final ComboPlan plan = ComboPlan.scan(ai);
        for (final PhaseType phase : phases()) {
            at(phase, ai);
            assertTrue(plan.outletMustWait(ai, mongrel), "own " + phase);
        }
    }

    @Test
    public void outletMustWaitOnTheOpponentsTurnUntilTheEndStep() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        final ComboPlan plan = ComboPlan.scan(ai);
        for (final PhaseType phase : phases()) {
            at(phase, opp);
            assertEquals(plan.outletMustWait(ai, mongrel), phase != PhaseType.END_OF_TURN, "opponent's " + phase);
        }
    }

    @Test
    public void outletNeedsNoWaitingWhenNoMadnessCardIsAtStake() {
        setUpGame();
        addMadnessDeck();
        put("Forest", ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        put("Arrogant Wurm", ai, ZoneType.Hand); // {2}{G}: cannot be cast with one Forest
        at(PhaseType.MAIN1, ai);
        assertFalse(ComboPlan.scan(ai).outletMustWait(ai, discardAbilityOf(mongrel)),
                "no castable madness card: the normal rules decide, as before the patch");
    }

    @Test
    public void nothingElseIsEverHeldBack() {
        setUpGame();
        readyBoard();
        final Card rootwalla = put("Basking Rootwalla", ai, ZoneType.Hand);
        final Card forest = put("Forest", ai, ZoneType.Battlefield);
        at(PhaseType.MAIN1, ai);
        final ComboPlan plan = ComboPlan.scan(ai);
        final SpellAbility spell = rootwalla.getFirstSpellAbility();
        spell.setActivatingPlayer(ai);
        assertFalse(plan.outletMustWait(ai, spell), "casting a creature is not an outlet play");
        assertFalse(plan.outletMustWait(ai, forest.getManaAbilities().get(0)), "neither is tapping a land");
        assertFalse(plan.outletMustWait(ai, null));
    }

    @Test
    public void aDeckWithoutMadnessIsNeverAffected() {
        setUpGame();
        addPlainDeck();
        put("Forest", 3, ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        put("Basking Rootwalla", ai, ZoneType.Hand);
        at(PhaseType.MAIN1, ai);
        assertFalse(ComboPlan.scan(ai).outletMustWait(ai, discardAbilityOf(mongrel)));
    }

    // ===================================================================================== the gate itself

    @Test
    public void canPlaySaRefusesTheOutletWhereverTheWindowIsClosed() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        requireMadnessPlan();
        for (final PhaseType phase : phases()) {
            at(phase, ai);
            assertNotEquals(brain().canPlaySa(mongrel), AiPlayDecision.WillPlay, "own " + phase);
            if (phase != PhaseType.END_OF_TURN) {
                at(phase, opp);
                assertNotEquals(brain().canPlaySa(mongrel), AiPlayDecision.WillPlay, "opponent's " + phase);
            }
        }
    }

    @Test
    public void theRefusalNamesTheReason() {
        setUpGame();
        final SpellAbility mongrel = readyBoard();
        requireMadnessPlan();
        at(PhaseType.MAIN2, ai);
        assertEquals(brain().canPlaySa(mongrel), AiPlayDecision.AnotherTime,
                "AnotherTime = wait for a better moment; it is not a refusal for good");
    }

    @Test
    public void canPlaySaDoesNotBlockOtherAbilities() {
        setUpGame();
        readyBoard();
        requireMadnessPlan();
        final Card rootwalla = put("Basking Rootwalla", ai, ZoneType.Hand);
        final SpellAbility spell = rootwalla.getFirstSpellAbility();
        spell.setActivatingPlayer(ai);
        at(PhaseType.MAIN1, ai);
        assertNotEquals(brain().canPlaySa(spell), AiPlayDecision.AnotherTime, "the gate only looks at discard outlets");
    }
}