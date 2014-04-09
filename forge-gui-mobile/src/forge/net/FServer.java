package forge.net;

import com.google.common.base.Supplier;

import forge.game.player.LobbyPlayer;
import forge.player.LobbyPlayerHuman;

public class FServer {
    private FServer() { } //don't allow creating instance

    private static Lobby lobby;
    
    public static Lobby getLobby() {
        if (lobby == null) {
            //not a very good solution still
            lobby = new Lobby(new Supplier<LobbyPlayer>() {
                @Override
                public LobbyPlayer get() {
                    return new LobbyPlayerHuman("Human");
                }
            });
        }
        return lobby;
    }

    /*private final NetServer server = new NetServer();

    public NetServer getServer() {
        // TODO Auto-generated method stub
        return server;
    }*/
}
