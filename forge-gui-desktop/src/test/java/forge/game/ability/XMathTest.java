package forge.game.ability;

import java.util.List;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;

import static junit.framework.Assert.assertEquals;

/** The arithmetic behind Count$ expressions: doXMath operators and the player-wide Highest/Lowest counts. */
public class XMathTest extends AITest {

    /** DivideEvenly* answer 0 for a zero operand; Mod has to do the same instead of dividing by zero. */
    @Test
    public void modByZeroIsZero() {
        Game game = initAndCreateGame();
        Card host = addCard("Grizzly Bears", game.getPlayers().get(1));

        assertEquals(0, AbilityUtils.doXMath(7, "Mod.0", host, null));
    }

    @Test
    public void modWithNonZeroOperandStillWorks() {
        Game game = initAndCreateGame();
        Card host = addCard("Grizzly Bears", game.getPlayers().get(1));

        assertEquals(1, AbilityUtils.doXMath(7, "Mod.3", host, null));
    }

    /** The trailing /Plus.1 belongs to the Highest count, so it is added once, not once per player and again after. */
    @Test
    public void highestAppliesItsOperatorOnce() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        game.getPlayers().get(0).setLife(10, null);
        p.setLife(20, null);

        List<Player> all = game.getPlayers();
        assertEquals(21, AbilityUtils.playerXCount(all, "HighestLifeTotal/Plus.1", host, null));
    }

    @Test
    public void lowestAppliesItsOperatorOnce() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        game.getPlayers().get(0).setLife(10, null);
        p.setLife(20, null);

        List<Player> all = game.getPlayers();
        assertEquals(11, AbilityUtils.playerXCount(all, "LowestLifeTotal/Plus.1", host, null));
    }
}
