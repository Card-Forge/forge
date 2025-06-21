package forge.gamesimulationtests.util.playeractions;

import forge.game.IGame;
import forge.gamesimulationtests.util.player.PlayerSpecification;

public abstract class BasePlayerAction {
	private final PlayerSpecification player;
	private ActionPreCondition preCondition;

	protected BasePlayerAction( PlayerSpecification player ) {
		this.player = player;
	}

	public PlayerSpecification getPlayer() {
		return player;
	}

	public boolean isApplicable( IGame game ) {
		return preCondition == null || preCondition.isApplicable( game );
	}

	public BasePlayerAction when( ActionPreCondition preCondition ) {
		this.preCondition = preCondition;
		return this;
	}
}
