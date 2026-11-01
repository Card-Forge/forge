package forge.game.ability;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Random;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

import static junit.framework.Assert.assertEquals;

/**
 * Spark Fiend notes its entry roll and stops rolling once an upkeep roll matches it. The roll is
 * stored as a plain number, and StoreSVar's CountSVar copies it through xCount.
 */
public class SparkFiendTest extends AITest {
    private final Random saved = MyRandom.getRandom();

    @AfterMethod
    public void restoreRandom() {
        MyRandom.setRandom(saved);
    }

    // MyRandom reads nextInt() >>> 1, which a six-sided die takes modulo 6
    private static Random dice(Integer... faces) {
        final Deque<Integer> q = new ArrayDeque<>(Arrays.asList(faces));
        return new Random() {
            @Override
            public int nextInt() {
                return q.isEmpty() ? super.nextInt() : (q.poll() - 1) << 1;
            }
        };
    }

    private void roll(Card fiend, Player p, String svar, Integer... faces) {
        MyRandom.setRandom(dice(faces));
        SpellAbility sa = AbilityFactory.getAbility(fiend.getSVar(svar), fiend);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
    }

    private int svar(Card fiend, String name) {
        return AbilityUtils.calculateAmount(fiend, name, null);
    }

    @Test
    public void keepsRollingUntilItRollsTheNotedTotal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card fiend = addCard("Spark Fiend", p);

        roll(fiend, p, "TrigRollETB", 3, 5);
        assertEquals(8, svar(fiend, "Noted"));

        roll(fiend, p, "TrigRoll", 2, 4);
        assertEquals(-2, svar(fiend, "Safe"));

        roll(fiend, p, "TrigRoll", 4, 4);
        assertEquals(0, svar(fiend, "Safe"));
    }
}
