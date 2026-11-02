package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

// Melira + Kitchen Finks + Altar of Dementia mills the whole library. Forge's runaway check
// stopped the loop at random after 10 activations (42 of 50 cards milled against a Soul Warden deck).
public class MillLoopAiTest extends AITest {

    private Game millGame(boolean melira) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        if (melira) {
            addCard("Melira, Sylvok Outcast", ai);
        }
        addCard("Kitchen Finks", ai);
        addCard("Altar of Dementia", ai);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 60);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);
        return game;
    }

    @Test(timeOut = 120000)
    public void testMeliraFinksAltarMillsEverything() {
        Game game = millGame(true);
        Player opponent = game.getPlayers().get(0);
        assertEquals(opponent.getCardsIn(ZoneType.Library).size(), 0);
    }

    // without Melira, Finks comes back once with a counter: two activations at most
    @Test(timeOut = 120000)
    public void testFinksAltarWithoutMeliraStops() {
        Game game = millGame(false);
        Player opponent = game.getPlayers().get(0);
        assertTrue(opponent.getCardsIn(ZoneType.Library).size() >= 55, "library " + opponent.getCardsIn(ZoneType.Library).size());
    }

    // A Pod deck against a lifegain deck: with Soul Warden on the other side, the Altar activations piled up on the
    // stack without resolving until it passed 999 objects and Forge called the game a draw
    @Test(timeOut = 120000)
    public void testMillLoopStopsWhenStackedMillCoversLibrary() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCard("Melira, Sylvok Outcast", ai);
        addCard("Safehold Elite", ai);
        addCard("Altar of Dementia", ai);
        addCard("Soul Warden", opponent);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 40);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);
        assertTrue(!game.isGameOver() || game.getOutcome().getWinningPlayer() == ai.getRegisteredPlayer(),
                "game over: " + (game.isGameOver() ? game.getOutcome().isDraw() : false));
        assertEquals(opponent.getCardsIn(ZoneType.Library).size(), 0);
    }
}
