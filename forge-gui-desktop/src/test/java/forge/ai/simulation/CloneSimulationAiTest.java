package forge.ai.simulation;

import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Without a simulation the AI has no way to price a creature whose body is defined by its
 * controller's board, so it leaves it alone. With one it can tell the good copy from the trap.
 */
public class CloneSimulationAiTest extends SimulationTest {

    private Card setUp(Game game, Player ai, Player opp, int oursSwamps) {
        Card crypto = addCard("Cryptoplasm", ai);
        crypto.setSickness(false);
        addCards("Swamp", oursSwamps, ai);
        addCard("Nightmare", opp);
        addCards("Swamp", 8, opp);
        for (Player p : game.getPlayers()) {
            fillLibrary(p, 20);
        }
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        game.getAction().checkStateEffects(true);
        playUntilPhase(game, PhaseType.MAIN1);
        playUntilStackClear(game);
        return crypto;
    }

    @Test
    public void simulationTakesTheCopyThatIsGoodUnderUsToo() {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        // eight Swamps of our own, so their Nightmare is an 8/8 for us as well
        Card crypto = setUp(game, ai, opp, 8);

        assertEquals("Nightmare", crypto.getName());
        assertEquals(8, crypto.getNetPower());
    }

    @Test
    public void simulationStillRefusesTheOneThatShrinks() {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        // no Swamps of our own, so it would arrive as a 0/0
        Card crypto = setUp(game, ai, opp, 0);

        assertEquals("Cryptoplasm", crypto.getName());
    }
}
