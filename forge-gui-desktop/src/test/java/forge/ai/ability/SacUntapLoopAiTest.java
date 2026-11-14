package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

import static org.testng.Assert.assertTrue;

// Blasting Station (untaps when a creature enters) + Pitiless Plunderer (Treasure on death)
// + Stimulus Package (sacrifice a Treasure: 1/1 Citizen) = endless damage without mana.
public class SacUntapLoopAiTest extends AITest {

    private Player loopGame(boolean converter) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        if (converter) {
            addCard("Stimulus Package", ai);
        }
        addCard("Blasting Station", ai);
        addCard("Pitiless Plunderer", ai);
        addCard("Llanowar Elves", ai);
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
    public void testStationPlundererStimulusLoops() {
        Player opponent = loopGame(true);
        assertTrue(opponent.getLife() <= 0, "life " + opponent.getLife());
    }

    // without Stimulus Package the Treasure stays a Treasure: one or two pings at most
    @Test(timeOut = 120000)
    public void testWithoutConverterNoLoop() {
        Player opponent = loopGame(false);
        assertTrue(opponent.getLife() >= 17, "life " + opponent.getLife());
    }
}
