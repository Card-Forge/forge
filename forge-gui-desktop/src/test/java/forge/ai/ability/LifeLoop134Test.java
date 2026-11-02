package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

import static org.testng.Assert.assertTrue;

// Melira + a persist creature that gains life when it enters (Kitchen Finks) + a free sacrifice
// outlet = as much life as wanted (tournament v20, Pod: Melira + Finks + Bloodthrone Vampire on the battlefield for
// 10 turns, never looped, lost at 8 life)
public class LifeLoop134Test extends AITest {

    private Game loopGame(String outlet, boolean melira, int life, boolean blocker) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        if (melira) {
            addCard("Melira, Sylvok Outcast", ai);
        }
        addCard("Kitchen Finks", ai);
        addCard(outlet, ai).setSickness(false);
        if (blocker) {
            addCard("Grizzly Bears", opponent);
        }
        ai.setLife(life, null);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 20);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);
        return game;
    }

    @Test(timeOut = 120000)
    public void testSeerGainsLife() {
        Game game = loopGame("Viscera Seer", true, 8, true);
        Player ai = game.getPlayers().get(1);
        assertTrue(ai.getLife() >= 50, "life " + ai.getLife() + "\n" + gameStateToString(game));
    }

    @Test(timeOut = 120000)
    public void testBloodthroneGainsLife() {
        Game game = loopGame("Bloodthrone Vampire", true, 8, true);
        Player ai = game.getPlayers().get(1);
        assertTrue(ai.getLife() >= 50, "life " + ai.getLife() + "\n" + gameStateToString(game));
    }

    // the outlet that pumps itself (Nantuko Husk) grows with every loop: no blocker, the opponent dies
    @Test(timeOut = 120000)
    public void testHuskLoopKills() {
        Game game = loopGame("Nantuko Husk", true, 20, false);
        assertTrue(game.isGameOver(), gameStateToString(game));
    }

    // without Melira the Finks comes back once with a -1/-1 counter: no loop
    @Test(timeOut = 120000)
    public void testNoMeliraNoLoop() {
        Game game = loopGame("Viscera Seer", false, 8, true);
        Player ai = game.getPlayers().get(1);
        assertTrue(ai.getLife() <= 12, "life " + ai.getLife() + "\n" + gameStateToString(game));
    }
}
