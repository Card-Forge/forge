package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SacOutletLoopAiTest extends AITest {

    // Melira + Kitchen Finks + Altar of Dementia: the AI should loop and mill the opponent out
    @Test
    public void testMeliraFinksAltarMillsOpponent() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Melira, Sylvok Outcast", ai);
        addCard("Kitchen Finks", ai);
        addCard("Altar of Dementia", ai);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 30);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);

        assertEquals(opponent.getCardsIn(ZoneType.Library).size(), 0);
        assertEquals(ai.getCardsIn(ZoneType.Library).size(), 20);
        assertEquals(countCardsWithName(game, "Kitchen Finks"), 1);
    }

    // Without a way to get the creature back, the AI should not throw its only creature into the Altar
    @Test
    public void testAltarAloneDoesNotSacrificeGoodCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Grizzly Bears", ai);
        addCard("Altar of Dementia", ai);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 30);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);

        assertEquals(countCardsWithName(game, "Grizzly Bears"), 1);
        assertEquals(ai.getCardsIn(ZoneType.Library).size(), 20);
        assertEquals(opponent.getCardsIn(ZoneType.Library).size(), 30);
    }

    // Melira + Kitchen Finks + Viscera Seer: the AI loops up to PersistLoopAi.LIFE_GOAL life, then stops
    @Test
    public void testMeliraFinksSeerDoesNotLoop() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Melira, Sylvok Outcast", ai);
        addCard("Kitchen Finks", ai);
        addCard("Viscera Seer", ai);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 30);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, opponent);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        // each activation sacrifices the Finks, which comes back and gains 2 life: up to the goal, no runaway loop
        assertTrue(ai.getLife() >= forge.ai.PersistLoopAi.LIFE_GOAL - 2 && ai.getLife() <= forge.ai.PersistLoopAi.LIFE_GOAL + 4,
                "life " + ai.getLife());
        assertEquals(countCardsWithName(game, "Kitchen Finks", ZoneType.Battlefield), 1);
        assertEquals(countCardsWithName(game, "Viscera Seer", ZoneType.Battlefield), 1);
    }

    // Viscera Seer is no longer removed from AI decks: the AI casts it
    @Test
    public void testAiCastsVisceraSeer() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Swamp", ai);
        addCardToZone("Viscera Seer", ai, ZoneType.Hand);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 30);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);

        assertEquals(countCardsWithName(game, "Viscera Seer", ZoneType.Battlefield), 1);
    }
}
