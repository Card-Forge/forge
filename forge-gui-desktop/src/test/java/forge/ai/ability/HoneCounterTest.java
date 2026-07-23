package forge.ai.ability;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Hone counters (Hobbit / #11281): each hone counter on an Equipment grants +1/+0 to the equipped
 * creature. The effect is intrinsic to the counter, so any Equipment picks it up.
 */
public class HoneCounterTest extends AITest {

    @Test
    public void honeCountersPumpEquippedCreature() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card bear = addCard("Grizzly Bears", p);   // 2/2
        bear.setSickness(false);
        // Bonesplitter gives a flat +2/+0 of its own; use it to prove hone stacks with normal boosts.
        Card equip = addCard("Bonesplitter", p);
        equip.attachToEntity(bear, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // Baseline: 2/2 + Bonesplitter's +2/+0
        AssertJUnit.assertEquals("Bonesplitter baseline", 4, bear.getNetPower());
        AssertJUnit.assertEquals(2, bear.getNetToughness());

        // Add 3 hone counters to the Equipment -> equipped creature gets +3/+0
        equip.addCounterInternal(CounterEnumType.HONE, 3, p, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("Bonesplitter +2 and 3 hone counters", 7, bear.getNetPower());
        AssertJUnit.assertEquals("hone is power only", 2, bear.getNetToughness());

        // Remove 2 hone counters -> +1/+0 from hone remains
        equip.subtractCounter(CounterEnumType.HONE, 2, p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("one hone counter left", 5, bear.getNetPower());

        // Remove the last hone counter -> back to just Bonesplitter
        equip.subtractCounter(CounterEnumType.HONE, 1, p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("no hone counters", 4, bear.getNetPower());
    }
}
