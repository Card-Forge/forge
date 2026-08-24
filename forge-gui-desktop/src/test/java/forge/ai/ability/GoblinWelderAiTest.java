package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Goblin Welder targets the artifact that gets sacrificed and the graveyard card that replaces it,
 * so the AI has to read both halves as one trade before it picks either one.
 */
public class GoblinWelderAiTest extends AITest {

    private Card welder(Player ai) {
        Card welder = addCard("Goblin Welder", ai);
        welder.setSickness(false);
        return welder;
    }

    private void endOfOpponentsTurn(Game game, Player opp) {
        // Chromatic Star draws a card on the way out, and decking ends the game before we can look
        for (Player p : game.getPlayers()) {
            fillLibrary(p, 10);
        }
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);
    }

    @Test
    public void weldsOurWorstArtifactIntoOurBest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        welder(ai);
        addCard("Chromatic Star", ai);
        addCard("Sol Ring", ai);
        addCardToZone("Wurmcoil Engine", ai, ZoneType.Graveyard);

        endOfOpponentsTurn(game, opp);

        assertEquals(1, countCardsWithName(game, "Wurmcoil Engine"));
        assertEquals(1, countCardsWithName(game, "Chromatic Star", ZoneType.Graveyard));
        assertEquals("kept the artifact worth keeping", 1, countCardsWithName(game, "Sol Ring"));
    }

    @Test
    public void leavesTheOpponentNoBetterOffThanTheyWere() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        welder(ai);
        addCard("Chromatic Star", ai);
        addCardToZone("Ornithopter", ai, ZoneType.Graveyard);
        // welding for the opponent here would trade their Wurmcoil up into a Blightsteel Colossus
        addCard("Wurmcoil Engine", opp);
        addCardToZone("Blightsteel Colossus", opp, ZoneType.Graveyard);

        endOfOpponentsTurn(game, opp);

        assertEquals(0, countCardsWithName(game, "Blightsteel Colossus"));
        assertEquals(1, countCardsWithName(game, "Wurmcoil Engine"));
        assertEquals("nothing of ours was spent either", 1, countCardsWithName(game, "Chromatic Star"));
    }

    @Test
    public void tradesTheOpponentsArtifactDownForTheirOwnJunk() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        welder(ai);
        addCard("Wurmcoil Engine", opp);
        addCardToZone("Ornithopter", opp, ZoneType.Graveyard);

        endOfOpponentsTurn(game, opp);

        assertEquals(1, countCardsWithName(game, "Wurmcoil Engine", ZoneType.Graveyard));
        assertTrue("the Ornithopter came back instead", countCardsWithName(game, "Ornithopter") == 1);
    }

    @Test
    public void leavesItAloneWithNothingToGain() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        welder(ai);
        addCard("Sol Ring", ai);
        addCardToZone("Ornithopter", ai, ZoneType.Graveyard);

        endOfOpponentsTurn(game, opp);

        assertEquals(1, countCardsWithName(game, "Sol Ring"));
        assertEquals(0, countCardsWithName(game, "Ornithopter"));
    }

    @Test
    public void bringsBackTheirWorstNotTheirBest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        welder(ai);
        addCard("Wurmcoil Engine", opp);
        addCardToZone("Ornithopter", opp, ZoneType.Graveyard);
        addCardToZone("Blightsteel Colossus", opp, ZoneType.Graveyard);

        endOfOpponentsTurn(game, opp);

        assertEquals("their Wurmcoil is gone", 1, countCardsWithName(game, "Wurmcoil Engine", ZoneType.Graveyard));
        assertEquals("and the junk came back, not the bomb", 1, countCardsWithName(game, "Ornithopter", ZoneType.Battlefield));
        assertEquals(0, countCardsWithName(game, "Blightsteel Colossus", ZoneType.Battlefield));
    }

    @Test
    public void takesTheBiggerSwingNotTheFirstOneFound() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        welder(ai);
        // ours is a real upgrade, +5, but stripping their Colossus for an Ornithopter is +12
        addCard("Chromatic Star", ai);
        addCardToZone("Wurmcoil Engine", ai, ZoneType.Graveyard);
        addCard("Blightsteel Colossus", opp);
        addCardToZone("Ornithopter", opp, ZoneType.Graveyard);

        endOfOpponentsTurn(game, opp);

        assertEquals("their Colossus is off the battlefield", 0,
                countCardsWithName(game, "Blightsteel Colossus"));
        assertEquals("and they got the Ornithopter back", 1, countCardsWithName(game, "Ornithopter"));
        assertEquals("our own trade was left on the table", 1, countCardsWithName(game, "Chromatic Star"));
        assertEquals(1, countCardsWithName(game, "Wurmcoil Engine", ZoneType.Graveyard));
    }
}
