package forge.ai.ability;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

/**
 * Thespian's Stage was AI:RemoveDeck:All because CloneAi had no way to pick a land to copy.
 */
public class CloneBestLandAiTest extends AITest {

    /** The Stage is taken at the end of the turn before ours, so run the game from there. */
    private void runAtOppEndStep(Game game, Player opp) {
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);
    }

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

        runAtOppEndStep(game, opp);

        AssertJUnit.assertEquals("Ancient Tomb", stage.getName());
    }

    @Test
    public void copiesAnOpponentsLandToo() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card stage = addCard("Thespian's Stage", ai);
        addCards("Swamp", 4, ai);
        addCard("Ancient Tomb", opp);

        runAtOppEndStep(game, opp);

        AssertJUnit.assertEquals("Ancient Tomb", stage.getName());
    }

    @Test
    public void ignoresALandThatOnlyScoresOnTheirBoard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card stage = addCard("Thespian's Stage", ai);
        addCards("Swamp", 4, ai);
        // huge for them, worthless for us: the creatures it counts are on their side
        addCard("Gaea's Cradle", opp);
        addCards("Grizzly Bears", 3, opp);

        runAtOppEndStep(game, opp);

        AssertJUnit.assertEquals("Thespian's Stage", stage.getName());
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

        AssertJUnit.assertEquals("Thespian's Stage", stage.getName());
    }
}
