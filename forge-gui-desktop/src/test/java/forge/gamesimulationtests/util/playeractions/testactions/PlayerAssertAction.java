package forge.gamesimulationtests.util.playeractions.testactions;

import org.testng.Assert;

import forge.game.Game;
import forge.game.player.Player;
import forge.gamesimulationtests.util.IntegerConstraint;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecificationBuilder;
import forge.gamesimulationtests.util.player.PlayerSpecificationHandler;

public class PlayerAssertAction extends AssertAction {
	private final PlayerSpecification playerRequirements;
	
	public PlayerAssertAction( final PlayerSpecification playerRequirements ) {
		this.playerRequirements = playerRequirements;
	}
	
	public PlayerAssertAction( final PlayerSpecificationBuilder playerRequirements ) {
		this( playerRequirements.build() );
	}

	@Override
	public void performAssertion( Game game ) {
		final Player player = PlayerSpecificationHandler.INSTANCE.find( game, playerRequirements, IntegerConstraint.ZERO_OR_ONE );
		Assert.assertNotNull( player );
	}
}
