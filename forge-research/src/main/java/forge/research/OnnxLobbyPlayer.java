package forge.research;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.research.onnx.OnnxInferenceEngine;

/**
 * LobbyPlayer for ONNX-based opponents. Creates OnnxPlayerController instances
 * that run inference locally on the game thread with zero network overhead.
 */
public class OnnxLobbyPlayer extends LobbyPlayer implements IGameEntitiesFactory {

    private final OnnxInferenceEngine engine;
    private final int playerIndex;

    public OnnxLobbyPlayer(String name, OnnxInferenceEngine engine, int playerIndex) {
        super(name);
        this.engine = engine;
        this.playerIndex = playerIndex;
    }

    @Override
    public Player createIngamePlayer(Game game, int id) {
        Player p = new Player(getName(), game, id);
        p.setFirstController(new OnnxPlayerController(game, p, this, playerIndex, engine));
        return p;
    }

    @Override
    public PlayerController createMindSlaveController(Player master, Player slave) {
        return new OnnxPlayerController(slave.getGame(), slave, this, playerIndex, engine);
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        // ONNX agent doesn't need chat messages
    }
}
