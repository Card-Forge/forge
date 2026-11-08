package forge.gamesimulationtests.priority;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import forge.game.card.Card;
import forge.game.phase.PhaseType;

/**
 * CR 117.2c, 117.3a: turn-based actions are dealt with before anyone receives priority, then the active player receives
 * it. A spell cast during a turn-based action (combat damage) does not give its caster priority (CR 601.2i).
 */
public class TurnBasedActionCastPriorityTest extends PriorityTestBase {

    /**
     * a's Grizzly Bears attacks and b's Vampire Nighthawk blocks. As combat damage is dealt, Nighthawk's lifelink would
     * gain b life; b's Lich turns that into draws and Archmage Ascension into a search, during which b casts Panglacial
     * Wurm. The active player a still receives priority first in the combat damage step.
     */
    @Test(timeOut = 60000)
    public void castDuringCombatDamageByNonActivePlayer() {
        setUpGame();
        final Card bears = addCard("Grizzly Bears", a);
        bears.setSickness(false);
        final Card nighthawk = addCard("Vampire Nighthawk", b);
        wurmEngine(b);
        ctrl(a).attackWith = bears;
        ctrl(b).blockWith = nighthawk;
        ctrl(b).blocked = bears;

        final Priority damage = runUntilPriorityIn(1, PhaseType.COMBAT_DAMAGE);
        assertEquals(damage.stackTop(), WURM, "b cast the Wurm as combat damage was dealt\n" + log);
        assertEquals(damage.player(), a, "the active player gets priority first after the turn-based action, not the Wurm's caster\n" + log);
    }
}
