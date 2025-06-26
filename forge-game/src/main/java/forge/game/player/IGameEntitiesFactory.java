package forge.game.player;

import forge.game.IGame;

public interface IGameEntitiesFactory {
	PlayerController createMindSlaveController(Player master, Player slave);
	Player createIngamePlayer(IGame game, int id);
}
