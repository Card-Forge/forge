package forge.gamesimulationtests;

import forge.ImageCache;
import forge.ImageKeys;
import forge.Singletons;
import forge.card.ForgeCardMockTestCase;
import forge.game.GameLogFormatter;
import forge.gamesimulationtests.util.GameWrapper;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecificationHandler;
import forge.gamesimulationtests.util.playeractions.testactions.AssertAction;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.Lang;
import forge.util.Localizer;
import io.sentry.Sentry;
import io.sentry.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import javax.imageio.ImageIO;
import java.util.ResourceBundle;

@PrepareForTest(value = {FModel.class, Singletons.class, ResourceBundle.class,
		ImageCache.class, ImageIO.class, ImageKeys.class,
		ForgeConstants.class, Localizer.class, Sentry.class, GameLogFormatter.class})
@SuppressStaticInitializationFor({"forge.ImageCache", "forge.localinstance.properties.ForgeConstants"})
public class BaseGameSimulationTest extends ForgeCardMockTestCase {

	@BeforeMethod
	@Override
	protected void initMocks() throws Exception {
		super.initMocks();
		PowerMockito.mockStatic(Sentry.class);
		PowerMockito.mockStatic(GameLogFormatter.class);
		PowerMockito.when(Sentry.getContext()).thenReturn(new Context());
		Lang.createInstance("en-US");
	}

	protected void runGame(GameWrapper game, PlayerSpecification expectedWinner, int finalTurn, AssertAction... postGameAssertActions) {
		try {
			initMocks();
			game.runGame();
			verifyThatTheGameHasFinishedAndThatPlayerHasWonOnTurn(game, expectedWinner, finalTurn);
			if(postGameAssertActions != null && postGameAssertActions.length > 0) {
				for(AssertAction assertAction : postGameAssertActions) {
					assertAction.performAssertion(game.getGame());
				}
			}
		} catch (Throwable t) {
			System.out.println(game.toString());
			throw new RuntimeException(t);
		}
	}
	
	protected void verifyThatTheGameHasFinishedAndThatPlayerHasWonOnTurn(GameWrapper game, PlayerSpecification expectedWinner, int finalTurn) {
		Assert.assertTrue(game.getGame().isGameOver());
		Assert.assertEquals(game.getGame().getOutcome().getLastTurnNumber(), finalTurn);
		Assert.assertEquals(game.getGame().getOutcome().getWinningPlayer().getPlayer().getName(),
						    PlayerSpecificationHandler.INSTANCE.find(game.getGame(), expectedWinner).getName());
		Assert.assertTrue(game.getPlayerActions() == null || game.getPlayerActions().isEmpty());
	}
}
