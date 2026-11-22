package forge.gamesimulationtests.priority;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

/**
 * CR 734.1: a cast that can't be completed is reversed, and with it the abilities its payment would have triggered.
 * Abilities that triggered earlier in the resolution the cast happened during are not part of it.
 */
public class CancelledCastDuringResolutionTest extends PriorityTestBase {

    /**
     * a's Transforming Flourish destroys b's Doomed Traveler (its dies trigger waits until the resolution finishes).
     * b exiles Lightning Bolt from the top of their library, starts casting it for free and cancels while choosing its
     * target, so the cast is reversed. That must not delete the waiting dies trigger.
     */
    @Test(timeOut = 60000)
    public void cancelledCastDuringResolutionKeepsTheResolutionsTriggers() {
        setUpGame();
        ctrl(a).declineCopies = true; // Transforming Flourish's Demonstrate
        ctrl(b).humanPaths = true;
        ctrl(b).cancelTargeting = true;
        final Card traveler = addCard("Doomed Traveler", b);
        b.getZone(ZoneType.Library).add(createCard("Lightning Bolt", b), 0);
        lands("Mountain", a, 3);
        cast("Transforming Flourish", a, traveler);

        runUntil(() -> spirits() == 1 || turn() > 1, "the dies trigger resolving");
        assertEquals(count("Doomed Traveler", ZoneType.Graveyard), 1, "Transforming Flourish destroyed it\n" + log);
        assertTrue(count("Lightning Bolt", ZoneType.Exile) == 1, "b's cast of Lightning Bolt was cancelled\n" + log);
        assertEquals(spirits(), 1, "the dies trigger survives the cancelled cast\n" + log);
    }

    private int spirits() {
        return count("Spirit Token", ZoneType.Battlefield) + count("Spirit", ZoneType.Battlefield);
    }
}
