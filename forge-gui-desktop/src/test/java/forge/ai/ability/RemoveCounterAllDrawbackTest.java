package forge.ai.ability;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;

/**
 * RemoveCounterAll has no AI, so as a sub-ability it used to refuse and take its parent down with
 * it. Oblivion Stone's wipe ends by clearing the fate counters it just checked, which meant the
 * wipe itself could never be activated.
 */
public class RemoveCounterAllDrawbackTest extends AITest {

    @Test
    public void bookkeepingSubAbilityDoesNotVetoTheWipe() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);

        for (int i = 0; i < 8; i++) {
            addCard("Wastes", ai);
        }
        fillLibrary(ai, 15);
        fillLibrary(opp, 15);
        // nothing of the AI's own is in range, so the wipe is pure upside
        addCard("Grizzly Bears", opp);
        addCard("Grizzly Bears", opp);
        addCard("Grizzly Bears", opp);
        addCard("Oblivion Stone", ai);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        assertEquals("it sacrificed Oblivion Stone to wipe", 1,
                countCardsWithName(game, "Oblivion Stone", ZoneType.Graveyard));
        assertEquals("the opponent's board is gone", 0,
                countCardsWithName(game, "Grizzly Bears", ZoneType.Battlefield));
    }
}
