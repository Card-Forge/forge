package forge.gamesimulationtests.util.playeractions;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.game.Game;
import forge.game.player.Player;
import forge.gamesimulationtests.util.player.PlayerSpecificationHandler;
import forge.gamesimulationtests.util.playeractions.testactions.TestAction;

public class PlayerActions {
	private final List<BasePlayerAction> playerActions;
	
	public PlayerActions( List<? extends BasePlayerAction> playerActions ) {
		this.playerActions = new LinkedList<>(playerActions);
	}
	
	public PlayerActions( BasePlayerAction... basePlayerActions ) {
		this( Arrays.asList( basePlayerActions ) );
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getNextActionIfApplicable( Player player, Game game, Class<T> actionType ) {
		if( playerActions.isEmpty() ) {
			return null;
		}
		final BasePlayerAction nextAction = playerActions.get( 0 );
		
		if( nextAction instanceof TestAction && isRightPlayer( player, game, nextAction ) && isApplicable( nextAction, game ) ) {
			playerActions.remove( 0 );
			( ( TestAction ) nextAction ).perform( game, player );
			return getNextActionIfApplicable( player, game, actionType );
		}
		
		if( actionType.isAssignableFrom( nextAction.getClass() ) && isRightPlayer( player, game, nextAction ) && isApplicable( nextAction, game ) ) {
			playerActions.remove( 0 );
			return ( T ) nextAction;
		}
		
		return null;
	}
	
	private boolean isApplicable( BasePlayerAction action, Game game ) {
		return action != null && action.isApplicable( game );
	}
	
	private boolean isRightPlayer( Player player, Game game, BasePlayerAction action ) {
		return action.getPlayer() == null || player.equals( PlayerSpecificationHandler.INSTANCE.find( game, action.getPlayer() ) );
	}
	
	public boolean isEmpty() {
		return playerActions.isEmpty();
	}
	
	@Override
	public String toString() {
		return "PlayerActions : [" + StringUtils.join( playerActions, ", " ) + "]";
	}
}
