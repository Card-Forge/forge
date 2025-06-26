package forge.gamesimulationtests.util.playeractions.testactions;

import forge.game.IGame;
import forge.game.player.Player;
import forge.gamesimulationtests.util.player.PlayerSpecification;

public class EndTestAction extends TestAction {
	public EndTestAction( PlayerSpecification player ) {
		super( player );
	}

	public void endTest( Player player ) {
		player.concede();
	}

	@Override
	public void perform(IGame game, Player player ) {
		endTest( player );
	}
}
