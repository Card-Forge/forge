package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class GameStateEvaluatorTest extends SimulationTest {
    private final GameStateEvaluator evaluator = new GameStateEvaluator();

    @Test
    public void testAlliedResourcesImproveTeamScore() {
        Game game = initAndCreateThreePlayerGame();
        Player ai = game.getPlayers().get(1);
        Player ally = game.getPlayers().get(2);
        game.getPlayers().get(0).setTeam(1);
        ai.setTeam(0);
        ally.setTeam(0);
        moveToMain2(game, ai);

        int score = evaluate(game, ai);
        addCard("Shivan Dragon", ally);
        AssertJUnit.assertTrue("An allied permanent should improve the team's position",
                evaluate(game, ai) > score);

        score = evaluate(game, ai);
        ally.setLife(ally.getLife() + 2, null);
        AssertJUnit.assertTrue("Allied life should improve the team's position",
                evaluate(game, ai) > score);

        score = evaluate(game, ai);
        addCardToZone("Runeclaw Bear", ally, ZoneType.Hand);
        AssertJUnit.assertTrue("An allied card should improve the team's position",
                evaluate(game, ai) > score);
    }

    @Test
    public void testPhasedPermanentRetainsLongTermValue() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card mountain = addCard("Mountain", ai);
        addCardToZone("Shivan Dragon", ai, ZoneType.Hand);
        moveToMain2(game, ai);

        int score = evaluate(game, ai);
        mountain.setPhasedOut(ai);

        AssertJUnit.assertEquals("Phasing should not make a permanent a long-term loss",
                score, evaluate(game, ai));
    }

    @Test
    public void testLostPlayerGetsTerminalScoreInMultiplayerGame() {
        Game game = initAndCreateThreePlayerGame();
        Player ai = game.getPlayers().get(1);
        moveToMain2(game, ai);
        ai.concede();

        AssertJUnit.assertFalse("The multiplayer game should continue after one player loses",
                game.isGameOver());
        AssertJUnit.assertEquals(Integer.MIN_VALUE, evaluate(game, ai));
    }

    private int evaluate(Game game, Player ai) {
        return evaluator.getScoreForGameState(game, ai).value;
    }
}
