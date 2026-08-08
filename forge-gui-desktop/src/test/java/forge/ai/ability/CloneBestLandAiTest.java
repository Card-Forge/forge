package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

import static junit.framework.Assert.assertEquals;

/**
 * Thespian's Stage was AI:RemoveDeck:All because CloneAi had no way to pick a land to copy.
 */
public class CloneBestLandAiTest extends AITest {

    @Test
    public void copiesTheBetterLandItControls() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card stage = addCard("Thespian's Stage", ai);
        addCard("Ancient Tomb", ai);
        addCards("Swamp", 4, ai);
        // scores higher than the Tomb once creatures are out, but copying it would just make us
        // sacrifice one of the two
        addCard("Gaea's Cradle", ai);
        addCards("Grizzly Bears", 3, ai);

        // end of the opponent's turn, so ours is next
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);

        assertEquals("Ancient Tomb", stage.getName());
    }

    @Test
    public void waitsForTheTurnBeforeOurs() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card stage = addCard("Thespian's Stage", ai);
        addCard("Ancient Tomb", ai);
        addCards("Swamp", 4, ai);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);

        assertEquals("Thespian's Stage", stage.getName());
    }

    @Test
    public void leavesItAloneWithNothingBetterAround() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card stage = addCard("Thespian's Stage", ai);
        addCards("Swamp", 4, ai);
        // the only upgrade on the board is theirs, and it would be scored under them rather than
        // under us, so it is not one to read
        addCard("Ancient Tomb", opp);

        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);

        assertEquals("Thespian's Stage", stage.getName());
    }
}
