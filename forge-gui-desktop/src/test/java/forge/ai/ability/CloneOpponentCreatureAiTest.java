package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static org.testng.AssertJUnit.assertEquals;

/**
 * A creature is evaluated under its current controller, so a characteristic-defining body is worth
 * a different amount once the copy arrives under us.
 */
public class CloneOpponentCreatureAiTest extends AITest {

    private Card upkeep(Game game, Player ai, Player opp) {
        Card crypto = addCard("Cryptoplasm", ai);
        crypto.setSickness(false);
        addCards("Island", 5, ai);
        for (Player p : game.getPlayers()) {
            fillLibrary(p, 20);
        }
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        game.getAction().checkStateEffects(true);
        return crypto;
    }

    private void run(Game game) {
        playUntilPhase(game, PhaseType.MAIN1);
        playUntilStackClear(game);
    }

    @Test
    public void doesNotCopyABodyThatOnlyExistsOnTheirBoard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        // 8/8 for them, 0/0 for us: copying it is suicide
        addCard("Nightmare", opp);
        addCards("Swamp", 8, opp);
        Card crypto = upkeep(game, ai, opp);

        run(game);

        assertEquals("still ours and alive", ZoneType.Battlefield, crypto.getZone().getZoneType());
        assertEquals(0, countCardsWithName(game, "Nightmare", ZoneType.Graveyard));
    }

    @Test
    public void takesTheNextBestWhenTheTopPickIsATrap() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCard("Nightmare", opp);
        addCards("Swamp", 8, opp);
        addCard("Serra Angel", opp);
        Card crypto = upkeep(game, ai, opp);

        run(game);

        assertEquals("Serra Angel", crypto.getName());
        assertEquals(4, crypto.getNetPower());
        assertEquals(ZoneType.Battlefield, crypto.getZone().getZoneType());
    }

    @Test
    public void ordinaryBoardStillCopiesTheBest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCard("Serra Angel", opp);
        addCard("Grizzly Bears", opp);
        Card crypto = upkeep(game, ai, opp);

        run(game);

        assertEquals("Serra Angel", crypto.getName());
    }
}
