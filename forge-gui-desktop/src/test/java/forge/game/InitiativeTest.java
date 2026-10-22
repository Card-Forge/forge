package forge.game;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.player.GameLossReason;
import forge.game.player.Player;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

/** CR 723.4: the initiative cannot stay with, or go to, a player who has left the game. */
public class InitiativeTest extends AITest {

    /** Taking the initiative on behalf of a lost player hands it to the next player, and only to them. */
    @Test
    public void theInitiativeGoesPastAPlayerWhoHasLost() {
        Game game = initAndCreateThreePlayerGame();
        Player lost = game.getPlayers().get(0);
        lost.loseConditionMet(GameLossReason.SpellEffect, null);

        game.getAction().takeInitiative(lost, "CLB");

        Player holder = game.getHasInitiative();
        assertFalse("the holder is still in the game", holder == null || holder.hasLost());
        assertEquals("only the holder has the initiative card", 1,
                game.getPlayers().stream().filter(p -> p.getInitiativeSet() != null).count());
    }
}
