package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

// Goblin Bombardment (sacrifice a creature: 1 damage) is no longer removed from AI decks
public class SacOutletDamageAiTest extends AITest {

    // three creatures, opponent at 3 life: the AI sacrifices them all and wins
    @Test
    public void testBombardmentFinishesOpponent() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Grizzly Bears", 3, ai);
        addCard("Goblin Bombardment", ai);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 20);
        opponent.setLife(3, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        assertTrue(opponent.getLife() <= 0, "life " + opponent.getLife());
        assertTrue(game.isGameOver());
    }

    // not lethal: the AI keeps its creatures
    @Test
    public void testBombardmentKeepsCreaturesWhenNotLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Grizzly Bears", 3, ai);
        addCard("Goblin Bombardment", ai);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 20);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        assertEquals(countCardsWithName(game, "Grizzly Bears"), 3);
        assertEquals(opponent.getLife(), 20);
    }

    // Melira + Kitchen Finks + Goblin Bombardment: endless damage, the AI loops and wins
    @Test
    public void testMeliraFinksBombardmentLoops() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Melira, Sylvok Outcast", ai);
        addCard("Kitchen Finks", ai);
        addCard("Goblin Bombardment", ai);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 20);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        assertTrue(opponent.getLife() <= 0, "life " + opponent.getLife());
        assertEquals(countCardsWithName(game, "Kitchen Finks"), 1);
        assertEquals(countCardsWithName(game, "Melira, Sylvok Outcast"), 1);
    }

    // Solemnity + Young Wolf (undying) + Goblin Bombardment: no +1/+1 counter, the wolf comes back each time
    @Test
    public void testSolemnityUndyingBombardmentLoops() {
        assertLoops("Solemnity", "Young Wolf");
    }

    // Vizier of Remedies + Kitchen Finks + Goblin Bombardment: the persist counter is replaced by none
    @Test
    public void testVizierFinksBombardmentLoops() {
        assertLoops("Vizier of Remedies", "Kitchen Finks");
    }

    private void assertLoops(final String enabler, final String looper) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard(enabler, ai);
        addCard(looper, ai);
        addCard("Goblin Bombardment", ai);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 20);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        assertTrue(opponent.getLife() <= 0, "life " + opponent.getLife());
        assertEquals(countCardsWithName(game, looper), 1);
    }
}
