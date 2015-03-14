package forge.game.event;

import forge.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.PlayerController;

public class GameEventPlayerControl extends GameEvent {
    public final Player player;
    public final LobbyPlayer oldLobbyPlayer;
    public final PlayerController oldController;
    public final LobbyPlayer newLobbyPlayer;
    public final PlayerController newController;

    public GameEventPlayerControl(final Player p, final LobbyPlayer oldLobbyPlayer, final PlayerController oldController, final LobbyPlayer newLobbyPlayer, final PlayerController newController) {
        this.player = p;
        this.oldLobbyPlayer = oldLobbyPlayer;
        this.oldController = oldController;
        this.newLobbyPlayer = newLobbyPlayer;
        this.newController = newController;
    }

    @Override
    public <T> T visit(final IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}