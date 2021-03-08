package forge.gamesimulationtests;

import forge.ImageCache;
import forge.Singletons;
import forge.gamesimulationtests.util.CardDatabaseHelper;
import forge.gamesimulationtests.util.GameWrapper;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecificationHandler;
import forge.gamesimulationtests.util.playeractions.testactions.AssertAction;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;

import javax.imageio.ImageIO;

@PrepareForTest(value = { FModel.class, Singletons.class, ImageCache.class, ImageIO.class })
public class BaseGameSimulationTest extends PowerMockTestCase {
	//Can't run this with @BeforeTest or something like that, because of static voodoo
	protected void initializeMocks() throws Exception {
		//Loading a card also automatically loads the image, which we do not want (even if it wouldn't cause exceptions).
		//The static initializer block in ImageCache can't fully be mocked (https://code.google.com/p/powermock/issues/detail?id=256), so we also need to mess with ImageIO...
        //TODO: make sure that loading images only happens in a GUI environment, so we no longer need to mock this
        PowerMockito.mockStatic(ImageIO.class);
        PowerMockito.mockStatic(ImageCache.class);
        
        //Mocking some more static stuff
		ForgePreferences forgePreferences = new ForgePreferences();
		PowerMockito.when(FModel.getPreferences()).thenReturn(forgePreferences);
		PowerMockito.mockStatic(Singletons.class);
		PowerMockito.mockStatic(FModel.class);
		PowerMockito.when(FModel.getMagicDb()).thenReturn(CardDatabaseHelper.getStaticDataToPopulateOtherMocks());
	}
	
	@ObjectFactory
	public IObjectFactory getObjectFactory() {
		return new org.powermock.modules.testng.PowerMockObjectFactory();
	}
	
	protected void runGame(GameWrapper game, PlayerSpecification expectedWinner, int finalTurn, AssertAction... postGameAssertActions) {
		try {
			initializeMocks();
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
		Assert.assertEquals(game.getGame().getOutcome().getWinningPlayer(), PlayerSpecificationHandler.INSTANCE.find(game.getGame(), expectedWinner));
		Assert.assertTrue(game.getPlayerActions() == null || game.getPlayerActions().isEmpty());
	}
}
