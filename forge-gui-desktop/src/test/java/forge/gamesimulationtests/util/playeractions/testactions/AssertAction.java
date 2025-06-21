package forge.gamesimulationtests.util.playeractions.testactions;

import forge.game.IGame;
import forge.game.player.Player;

public abstract class AssertAction extends TestAction {
	public AssertAction() {
		super( null );//AssertActions may be about a player, but they're not really actions taken by a player...
	}

	public abstract void performAssertion( IGame game );

	@Override
	public void perform(IGame game, Player player ) {
		performAssertion( game );
	}
}
