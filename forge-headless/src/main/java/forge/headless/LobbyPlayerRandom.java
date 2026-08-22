package forge.headless;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;

import java.util.Random;

/**
 * LobbyPlayer that creates RandomController players for benchmarking.
 * Much faster than full AI since it doesn't do deep game tree analysis.
 */
public class LobbyPlayerRandom extends LobbyPlayer implements IGameEntitiesFactory {

    private final Random random;

    public LobbyPlayerRandom(String name) {
        this(name, new Random());
    }

    public LobbyPlayerRandom(String name, long seed) {
        this(name, new Random(seed));
    }

    public LobbyPlayerRandom(String name, Random random) {
        super(name);
        this.random = random;
    }

    @Override
    public PlayerController createMindSlaveController(Player master, Player slave) {
        return new RandomController(slave.getGame(), slave, this, random);
    }

    @Override
    public Player createIngamePlayer(Game game, final int id) {
        Player player = new Player(getName(), game, id);
        player.setFirstController(new RandomController(game, player, this, random));
        return player;
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        // Random player doesn't listen to chat
    }
}
