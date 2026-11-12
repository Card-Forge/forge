package forge.gamesimulationtests.priority;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.Test;

import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.zone.ZoneType;

/**
 * CR 725.1 (ending the turn) and CR 514.3a: during the cleanup step after an effect ends the turn, players only get
 * priority if abilities triggered during the process (or state-based actions are performed in the cleanup step).
 * A spell that reached the stack without anybody having priority does not give anyone priority.
 */
public class EndTurnCleanupPriorityTest extends PriorityTestBase {

    private static final String SUNDIAL = "Sundial of the Infinite";

    /** a activates Sundial of the Infinite ({1}, {T}: end the turn) the next time they get priority. */
    private void endTheTurnWithSundial() {
        final Card sundial = addCard(SUNDIAL, a);
        lands("Plains", a, 1);
        activate(sundial, a);
    }

    /**
     * a's Doomed Traveler, equipped with Hammer of Nazahn (indestructible), takes 3 damage from Lightning Bolt and
     * survives. Then a casts Ultima: it destroys the Hammer, so in the 725.1c state-based action check the Traveler
     * dies to the damage marked on it.
     */
    private Card travelerDiesInTheProcess() {
        final Card traveler = addCard("Doomed Traveler", a);
        lands("Plains", a, 4);
        cast("Hammer of Nazahn", a);
        resolveAll();
        assertTrue(traveler.isEquipped(), "Hammer of Nazahn's trigger attached it to the Traveler\n" + log);
        lands("Mountain", a, 1);
        cast("Lightning Bolt", a, traveler);
        resolveAll();
        assertEquals(traveler.getDamage(), 3, "the indestructible Traveler survived the Bolt with lethal damage marked\n" + log);
        lands("Plains", a, 5);
        cast("Ultima", a);
        return traveler;
    }

    private int spirits() {
        return count("Spirit Token", ZoneType.Battlefield) + count("Spirit", ZoneType.Battlefield);
    }

    private void assertNoPriorityInCleanup() {
        assertTrue(priorities(1, PhaseType.CLEANUP).isEmpty(),
                "no player may receive priority in the cleanup step (CR 725.1f), but: " + priorities(1, PhaseType.CLEANUP) + "\n" + log);
    }

    // ------------------------------------------------------------------------------------------------- baselines

    /** Nothing triggers: no priority in cleanup, the next turn begins. */
    @Test(timeOut = 60000)
    public void noTriggersNoPriorityInCleanup() {
        setUpGame();
        endTheTurnWithSundial();
        runUntilPhase(2, PhaseType.UPKEEP);
        assertNoPriorityInCleanup();
    }

    /** A creature dies in the 725.1c state-based action check: its dies trigger gives the active player priority in cleanup. */
    @Test(timeOut = 60000)
    public void diesTriggerDuringTheProcessGivesActivePlayerPriority() {
        setUpGame();
        travelerDiesInTheProcess();
        final Priority first = runUntilPriorityIn(1, PhaseType.CLEANUP);
        assertEquals(first.player(), a, "the active player gets priority first (CR 725.1f)\n" + log);
        assertEquals(first.stackTop(), "Doomed Traveler", "the dies trigger is on the stack\n" + log);
        runUntilPhase(2, PhaseType.UPKEEP);
        assertEquals(spirits(), 1, "the trigger resolved\n" + log);
    }

    /**
     * Ultima destroys the Traveler itself (no Hammer): CR 725.1a, its dies trigger triggered before the end-the-turn
     * process began, so it ceases to exist and nobody gets priority in cleanup.
     */
    @Test(timeOut = 60000)
    public void endTheTurnAsSubAbilityGivesNoPriorityInCleanup() {
        setUpGame();
        addCard("Doomed Traveler", a);
        lands("Plains", a, 5);
        cast("Ultima", a);
        runUntilPhase(2, PhaseType.UPKEEP);
        assertEquals(count("Doomed Traveler", ZoneType.Graveyard), 1, "Ultima destroyed it\n" + log);
        assertEquals(spirits(), 0, "the dies trigger ceased to exist\n" + log);
        assertNoPriorityInCleanup();
    }

    /** Madness from discarding to hand size in the cleanup step triggers: players get priority. */
    @Test(timeOut = 60000)
    public void madnessDiscardInCleanupGivesPriority() {
        setUpGame();
        for (int i = 0; i < 9; i++) {
            addCardToZone("Basking Rootwalla", a, ZoneType.Hand);
        }
        endTheTurnWithSundial();
        final Priority first = runUntilPriorityIn(1, PhaseType.CLEANUP);
        assertEquals(first.player(), a, log.toString());
    }

    /** CR 725.1d: ending the turn during the cleanup step starts a new cleanup step, without priority if nothing triggers. */
    @Test(timeOut = 60000)
    public void endingTheTurnDuringCleanupStartsANewCleanup() {
        setUpGame();
        final Card sundial = addCard(SUNDIAL, a);
        travelerDiesInTheProcess();
        // added after the casts so that only Sundial uses it
        lands("Plains", a, 1);
        ctrl(a).actIn = PhaseType.CLEANUP;
        activate(sundial, a);
        runUntilPhase(2, PhaseType.UPKEEP);
        final List<Priority> cleanup = priorities(1, PhaseType.CLEANUP);
        assertTrue(cleanup.size() >= 2, "a got priority for the dies trigger and activated Sundial\n" + log);
        assertEquals(cleanup.get(cleanup.size() - 1).stackTop(), SUNDIAL,
                "nobody may get priority after Sundial resolved\n" + log);
        assertEquals(spirits(), 0, "Sundial exiled the dies trigger\n" + log);
    }

    // ------------------------------------------------------------------------------------------------- bugs

    /**
     * a is at 0 life with Marina Vendrell's Grimoire and has cast Stunning Reversal. a's Ultima destroys the Grimoire, so
     * in the 725.1c state-based action check a would lose; Stunning Reversal draws seven instead, Archmage Ascension
     * turns the draws into searches and a casts Panglacial Wurm. Nothing triggers.
     */
    @Test(timeOut = 60000)
    public void wurmCastDuringTheProcessByActivePlayer() {
        setUpGame();
        stunningReversalEngine(a);
        lands("Plains", a, 5);
        cast("Ultima", a);
        assertWurmWaitsForNextUpkeep();
    }

    /** Same, with b at 0 life behind the Grimoire: b casts the Wurm during a's Ultima. */
    @Test(timeOut = 60000)
    public void wurmCastDuringTheProcessByNonActivePlayer() {
        setUpGame();
        stunningReversalEngine(b);
        lands("Plains", a, 5);
        cast("Ultima", a);
        assertWurmWaitsForNextUpkeep();
    }

    private void assertWurmWaitsForNextUpkeep() {
        final Priority upkeep = runUntilPriorityIn(2, PhaseType.UPKEEP);
        assertNoPriorityInCleanup();
        assertTrue(priorities(2, PhaseType.UNTAP).isEmpty(), "no priority in the untap step\n" + log);
        assertEquals(upkeep.player(), b, "the new active player gets priority first in upkeep\n" + log);
        assertEquals(upkeep.stackTop(), WURM, "the Wurm is still on the stack in the next upkeep\n" + log);
        runUntil(() -> count(WURM, ZoneType.Battlefield) == 1, "the Wurm resolving");
        assertEquals(turn(), 2, log.toString());
        assertEquals(game.getPhaseHandler().getPhase(), PhaseType.UPKEEP, "the Wurm resolves in the next upkeep\n" + log);
    }

    /**
     * As above with b casting the Wurm, and b's Up the Beanstalk triggers on it (mana value 5 or greater): the trigger
     * gives priority in cleanup, and the active player a gets it first, not the Wurm's caster.
     */
    @Test(timeOut = 60000)
    public void wurmCastTriggerGivesActivePlayerPriorityFirst() {
        setUpGame();
        stunningReversalEngine(b);
        addCard("Up the Beanstalk", b);
        lands("Plains", a, 5);
        cast("Ultima", a);
        final Priority first = runUntilPriorityIn(1, PhaseType.CLEANUP);
        assertEquals(first.stackTop(), "Up the Beanstalk", "Up the Beanstalk's trigger is on the stack\n" + log);
        assertEquals(first.player(), a, "the active player gets priority first (CR 725.1f), not the Wurm's caster\n" + log);
    }
}
