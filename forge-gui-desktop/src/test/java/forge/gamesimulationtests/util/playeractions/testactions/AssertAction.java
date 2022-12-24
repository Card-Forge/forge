package forge.gamesimulationtests.util.playeractions.testactions;

import forge.game.Game;
import forge.game.player.Player;

public abstract class AssertAction extends TestAction {
	public AssertAction() {
		super( null );//AssertActions may be about a player, but they're not really actions taken by a player...
	}

	public abstract void performAssertion( Game game );

	@Override
	public void perform( Game game, Player player ) {
		performAssertion( game );
	}
}
