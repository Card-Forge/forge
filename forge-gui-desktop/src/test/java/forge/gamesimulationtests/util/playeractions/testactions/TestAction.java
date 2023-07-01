package forge.gamesimulationtests.util.playeractions.testactions;

import forge.game.Game;
import forge.game.player.Player;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.playeractions.BasePlayerAction;

public abstract class TestAction extends BasePlayerAction {
	public TestAction( PlayerSpecification player ) {
		super( player );
	}

	public abstract void perform( Game game, Player player );
}
