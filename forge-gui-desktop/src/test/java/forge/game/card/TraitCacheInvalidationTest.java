package forge.game.card;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.player.Player;

import static junit.framework.Assert.assertEquals;

/**
 * The cached trait lists have to be dropped by every path that changes an input to them.
 * clearCounters is the awkward one: it empties the field directly instead of going through
 * either setCounters, so it is the one counter path that has to invalidate for itself.
 */
public class TraitCacheInvalidationTest extends AITest {

    @Test
    public void clearCountersDropsTheShieldReplacements() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card c = addCard("Grizzly Bears", p);

        int bare = c.getCurrentState().getReplacementEffects().size();

        c.addCounterInternal(CounterEnumType.SHIELD, 1, p, false, null, null);
        int shielded = c.getCurrentState().getReplacementEffects().size();
        assertEquals("a shield counter should add its two replacements", bare + 2, shielded);

        c.clearCounters();
        assertEquals("clearing it should take them away again", bare,
                c.getCurrentState().getReplacementEffects().size());
    }
}
