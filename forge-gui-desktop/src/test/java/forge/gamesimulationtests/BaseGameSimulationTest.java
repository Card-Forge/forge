package forge.gamesimulationtests;

import forge.card.CardMockTestCase;
import forge.game.GameLogFormatter;
import forge.gamesimulationtests.util.GameWrapper;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecificationHandler;
import forge.gamesimulationtests.util.playeractions.testactions.AssertAction;
import io.sentry.Sentry;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class BaseGameSimulationTest extends CardMockTestCase {

    private MockedStatic<Sentry> sentryMock;
    private MockedStatic<GameLogFormatter> gameLogFormatterMock;

    @BeforeMethod
    @Override
    public void initMocks() throws Exception {
        super.initMocks();
        sentryMock = Mockito.mockStatic(Sentry.class);
        gameLogFormatterMock = Mockito.mockStatic(GameLogFormatter.class);
    }

    @AfterMethod(alwaysRun = true)
    @Override
    public void releaseMocks() {
        if (gameLogFormatterMock != null) {
            gameLogFormatterMock.close();
            gameLogFormatterMock = null;
        }
        if (sentryMock != null) {
            sentryMock.close();
            sentryMock = null;
        }
        super.releaseMocks();
    }

    protected void runGame(GameWrapper game, PlayerSpecification expectedWinner, int finalTurn,
            AssertAction... postGameAssertActions) {
        try {
            initMocks();
            game.runGame();
            verifyThatTheGameHasFinishedAndThatPlayerHasWonOnTurn(game, expectedWinner, finalTurn);
            if (postGameAssertActions != null && postGameAssertActions.length > 0) {
                for (AssertAction assertAction : postGameAssertActions) {
                    assertAction.performAssertion(game.getGame());
                }
            }
        } catch (Throwable t) {
            System.out.println(game.toString());
            throw new RuntimeException(t);
        }
    }

    protected void verifyThatTheGameHasFinishedAndThatPlayerHasWonOnTurn(GameWrapper game,
            PlayerSpecification expectedWinner, int finalTurn) {
        Assert.assertTrue(game.getGame().isGameOver());
        Assert.assertEquals(game.getGame().getOutcome().getLastTurnNumber(), finalTurn);
        Assert.assertEquals(game.getGame().getOutcome().getWinningPlayer().getPlayer().getName(),
                PlayerSpecificationHandler.INSTANCE.find(game.getGame(), expectedWinner).getName());
        Assert.assertTrue(game.getPlayerActions() == null || game.getPlayerActions().isEmpty());
    }
}
