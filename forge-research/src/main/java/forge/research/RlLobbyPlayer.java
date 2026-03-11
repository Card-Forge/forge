package forge.research;

import java.util.concurrent.SynchronousQueue;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;

/**
 * LobbyPlayer for RL agents. Follows LobbyPlayerAi pattern.
 * Creates RlPlayerController instances for in-game players.
 */
public class RlLobbyPlayer extends LobbyPlayer implements IGameEntitiesFactory {

    private final SynchronousQueue<DecisionContext> decisionQueue;
    private final SynchronousQueue<ActionResponse> responseQueue;
    private final int playerIndex;

    public RlLobbyPlayer(String name,
            SynchronousQueue<DecisionContext> decisionQueue,
            SynchronousQueue<ActionResponse> responseQueue,
            int playerIndex) {
        super(name);
        this.decisionQueue = decisionQueue;
        this.responseQueue = responseQueue;
        this.playerIndex = playerIndex;
    }

    @Override
    public Player createIngamePlayer(Game game, int id) {
        Player p = new Player(getName(), game, id);
        p.setFirstController(new RlPlayerController(game, p, this, decisionQueue, responseQueue, playerIndex));
        return p;
    }

    @Override
    public PlayerController createMindSlaveController(Player master, Player slave) {
        // Mind slaver effects: fall back to a new RL controller
        return new RlPlayerController(slave.getGame(), slave, this, decisionQueue, responseQueue, playerIndex);
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        // RL agent doesn't need chat messages
    }
}
