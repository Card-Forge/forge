package forge.ai.ability;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.player.Player;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class BecomeMonarchAiTest extends AITest {

    /**
     * Throne of the High City sacrifices itself to make you the monarch. Becoming the monarch while
     * already the monarch does nothing (GameAction.becomeMonarch returns early), so the AI must not
     * throw the land away for no effect.
     */
    @Test
    public void doesNotCrownItselfTwice() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Throne of the High City", ai);
        for (int i = 0; i < 4; i++) {
            addCard("Swamp", ai);
        }
        game.getAction().becomeMonarch(ai, null);
        AssertJUnit.assertEquals(ai, game.getMonarch());

        playUntilStackClear(game);

        AssertJUnit.assertEquals("AI should still be the monarch", ai, game.getMonarch());
        AssertJUnit.assertEquals("Throne of the High City should not have been sacrificed", 1,
                countCardsWithName(game, "Throne of the High City"));
    }

    /** When somebody else holds the crown the ability is worth activating. */
    @Test
    public void takesTheCrownFromAnOpponent() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Throne of the High City", ai);
        for (int i = 0; i < 4; i++) {
            addCard("Swamp", ai);
        }
        game.getAction().becomeMonarch(opponent, null);
        AssertJUnit.assertEquals(opponent, game.getMonarch());

        playUntilStackClear(game);

        AssertJUnit.assertEquals("AI should have taken the crown", ai, game.getMonarch());
    }

    /** With no monarch at all the ability is still worth activating. */
    @Test
    public void claimsAnUnclaimedCrown() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Throne of the High City", ai);
        for (int i = 0; i < 4; i++) {
            addCard("Swamp", ai);
        }
        AssertJUnit.assertNull(game.getMonarch());

        playUntilStackClear(game);

        AssertJUnit.assertEquals("AI should have become the monarch", ai, game.getMonarch());
    }
}
