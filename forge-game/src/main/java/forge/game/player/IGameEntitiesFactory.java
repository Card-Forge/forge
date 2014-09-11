package forge.game.player;

import forge.game.Game;

public interface IGameEntitiesFactory
{
	PlayerController createControllerFor(Player p);
	Player createIngamePlayer(Game game, int id);
}
