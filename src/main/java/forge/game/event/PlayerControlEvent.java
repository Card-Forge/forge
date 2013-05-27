package forge.game.event;

import forge.game.player.LobbyPlayer;
import forge.game.player.Player;

public class PlayerControlEvent extends Event { 
    private final Player player;
    private final LobbyPlayer oldController;
    private final LobbyPlayer newController;
    
    public PlayerControlEvent(Player p, LobbyPlayer old, LobbyPlayer new1) {
        player = p;
        oldController = old;
        newController = new1;
    }

    public final Player getPlayer() {
        return player;
    }

    public final LobbyPlayer getOldController() {
        return oldController;
    }

    public final LobbyPlayer getNewController() {
        return newController;
    }
}