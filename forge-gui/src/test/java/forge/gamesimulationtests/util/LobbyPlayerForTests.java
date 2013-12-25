package forge.gamesimulationtests.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.game.Game;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.gamesimulationtests.util.playeractions.PlayerActions;

/**
 * Default harmless implementation for tests.
 * Test-specific behaviour can easily be added by mocking (parts of) this class.
 */
public class LobbyPlayerForTests extends LobbyPlayer {
	private final Map<Player, PlayerControllerForTests> playerControllers;
	private final PlayerActions playerActions;
	
	public LobbyPlayerForTests( String name, PlayerActions playerActions ) {
		super( name );
		playerControllers = new HashMap<Player, PlayerControllerForTests>();
		this.playerActions = playerActions;
	}
	
	@Override
	protected PlayerType getType() {
		//Don't really want to use COMPUTER here, as that might cause to much automatic behaviour by AI code embedded in the current rules code
		//Trying HUMAN for now, which might cause issues if it triggers GUI from the rules code.  If that happens, we'll need to refactor or use something else
		return PlayerType.HUMAN;
	}

	@Override
	public Player getPlayer( Game gameState ) {
		Player dummyPlayer = new Player( getName(), gameState );
		dummyPlayer.setFirstController( createControllerFor( dummyPlayer ) );
		return dummyPlayer;
	}

	@Override
	public PlayerController createControllerFor( Player player ) {
		if( !playerControllers.containsKey( player ) ) {
			PlayerControllerForTests dummyPlayerControllerForTests = new PlayerControllerForTests( player.getGame(), player, this );
			dummyPlayerControllerForTests.setPlayerActions( playerActions );
			playerControllers.put( player, dummyPlayerControllerForTests );
		}
		return playerControllers.get( player );
	}

	@Override
	public void hear( LobbyPlayer player, String message ) {
		//Do nothing
	}
	
	public PlayerControllerForTests getPlayerController() {
		if( playerControllers.size() == 1 ) {
			return playerControllers.values().iterator().next();
		}
		throw new IllegalStateException( "Can't determine correct controller " + StringUtils.join( playerControllers.entrySet(), ", " ) );
	}
}
