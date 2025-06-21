package forge.gamesimulationtests.util.player;

import forge.game.IGame;
import forge.game.player.Player;
import forge.gamesimulationtests.util.IntegerConstraint;
import forge.gamesimulationtests.util.SpecificationHandler;

public class PlayerSpecificationHandler extends SpecificationHandler<Player, PlayerSpecification> {
	public static final PlayerSpecificationHandler INSTANCE = new PlayerSpecificationHandler();

	public Player find(IGame game, final PlayerSpecification playerSpecification) {
		return find(game.getRegisteredPlayers(), playerSpecification);
	}

	public Player find(IGame game, final PlayerSpecification playerSpecification, final IntegerConstraint expectedNumberOfResults) {
		return find(game.getRegisteredPlayers(), playerSpecification, expectedNumberOfResults);
	}

	@Override
	public boolean matches(Player player, final PlayerSpecification playerSpecification) {
		return player.getName().equals( playerSpecification.getName())
				&& (playerSpecification.getLife() == null || playerSpecification.getLife().equals(player.getLife()));
	}
}
