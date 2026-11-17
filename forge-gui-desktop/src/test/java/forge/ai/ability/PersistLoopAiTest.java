package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

// Melira + Murderous Redcap + free sacrifice outlet = endless damage
public class PersistLoopAiTest extends AITest {

    private Player loopGame(String outlet, boolean melira) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        if (melira) {
            addCard("Melira, Sylvok Outcast", ai);
        }
        addCard("Murderous Redcap", ai);
        addCard(outlet, ai);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 20);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);
        return opponent;
    }

    @Test
    public void testMeliraRedcapSeerLoops() {
        Player opponent = loopGame("Viscera Seer", true);
        Game game = opponent.getGame();
        assertTrue(opponent.getLife() <= 0, "life " + opponent.getLife());
        assertTrue(game.isGameOver());
    }

    @Test
    public void testMeliraRedcapHuskLoops() {
        Player opponent = loopGame("Nantuko Husk", true);
        assertTrue(opponent.getLife() <= 0, "life " + opponent.getLife());
    }

    // without Melira the Redcap comes back once with a counter: no loop, the AI does not throw it away
    @Test
    public void testRedcapSeerWithoutMeliraDoesNotLoop() {
        Player opponent = loopGame("Viscera Seer", false);
        Game game = opponent.getGame();
        assertTrue(opponent.getLife() >= 16, "life " + opponent.getLife());
        assertEquals(countCardsWithName(game, "Murderous Redcap"), 1);
    }

    // two Auriok Champions gain back the 2 damage of each Redcap: the loop stops instead of running until the time limit
    @Test(timeOut = 120000)
    public void testLoopStopsWhenOpponentGainsItBack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCard("Auriok Champion", opponent);
        addCard("Auriok Champion", opponent);
        addCard("Melira, Sylvok Outcast", ai);
        addCard("Murderous Redcap", ai);
        addCard("Viscera Seer", ai);
        fillLibrary(ai, 60);
        fillLibrary(opponent, 20);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);
        assertTrue(opponent.getLife() > 0, "life " + opponent.getLife());
        assertEquals(game.getPhaseHandler().getPlayerTurn(), opponent);
    }
}
