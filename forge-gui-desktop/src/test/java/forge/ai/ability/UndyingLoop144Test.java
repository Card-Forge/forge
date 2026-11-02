package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

import static org.testng.Assert.assertTrue;

// Solemnity (no +1/+1 counters) makes undying loop like persist with Melira: Geralf's
// Messenger + Cartel Aristocrat (free sacrifice outlet) = endless life loss.
public class UndyingLoop144Test extends AITest {

    private Player loopGame(boolean solemnity) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        if (solemnity) {
            addCard("Solemnity", ai);
        }
        addCard("Geralf's Messenger", ai);
        addCard("Cartel Aristocrat", ai);
        for (int i = 0; i < 4; i++) {
            addCard("Wall of Stone", opponent);
        }
        fillLibrary(ai, 20);
        fillLibrary(opponent, 20);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);
        return opponent;
    }

    @Test(timeOut = 120000)
    public void testSolemnityMessengerAristocratLoops() {
        Player opponent = loopGame(true);
        assertTrue(opponent.getLife() <= 0, "life " + opponent.getLife());
    }

    // without Solemnity the Messenger comes back once with a counter: no endless loop
    @Test(timeOut = 120000)
    public void testWithoutSolemnityNoLoop() {
        Player opponent = loopGame(false);
        assertTrue(opponent.getLife() >= 14, "life " + opponent.getLife());
    }
}
