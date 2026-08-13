package forge.headless;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;

/** Creates bridge-aware Forge controllers and retains the active game controller. */
final class LobbyPlayerBridge extends LobbyPlayer implements IGameEntitiesFactory {
    private final int seat;
    private final boolean forgeAiSeat;
    private BridgeController controller;

    LobbyPlayerBridge(String name, int seat, boolean forgeAiSeat) {
        super(name);
        this.seat = seat;
        this.forgeAiSeat = forgeAiSeat;
    }

    BridgeController getController() {
        if (controller == null) {
            throw new IllegalStateException("Bridge controller has not been attached to a game");
        }
        return controller;
    }

    @Override
    public PlayerController createMindSlaveController(Player master, Player slave) {
        return new BridgeController(slave.getGame(), slave, this, seat, forgeAiSeat);
    }

    @Override
    public Player createIngamePlayer(Game game, int id) {
        Player player = new Player(getName(), game, id);
        controller = new BridgeController(game, player, this, seat, forgeAiSeat);
        player.setFirstController(controller);
        return player;
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        // The stdio protocol, not Forge chat, carries bridge communication.
    }
}
