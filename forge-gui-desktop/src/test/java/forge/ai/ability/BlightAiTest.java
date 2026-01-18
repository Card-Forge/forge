package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertEquals;

public class BlightAiTest extends AITest {

    // This also documents that the AI, when using Champions of the Weird,
    // will keep activating the ability as long as there are creatures that it can blight
    @Test
    public void testBlightAiTargetsOpponentWithCreatures() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Champion of the Weird", ai);
        addCard("Blooming Stinger", opponent);
        addCard("Blooming Stinger", opponent);
        addCard("Blooming Stinger", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        assertEquals(1, countCardsWithName(game, "Champion of the Weird"));
        assertEquals(3, countCardsWithName(game, "Blooming Stinger"));

        // Let the AI play until it passes priority
        gameLoopUntilNextPhase(game);

        assertEquals(3,  countCardsWithName(game, "Blooming Stinger", ZoneType.Graveyard));
        // Champion (5/5) should be dead from paying Blight 2 three times (6 counters)
        assertEquals(1,  countCardsWithName(game, "Champion of the Weird", ZoneType.Graveyard));
    }

    @Test
    public void testBlightAiDoesNotActivateWithoutOpponentCreatures() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card champion = addCard("Champion of the Weird", ai);
        int initialAiLife = ai.getLife();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        game.getPhaseHandler().mainLoopStep();

        // AI should not activate since human has no creatures to blight
        int countersOnChampion = champion.getCounters(CounterEnumType.M1M1);
        assertEquals(0, countersOnChampion);
        assertEquals(initialAiLife, ai.getLife());
    }
}
