package forge.interfaces;

import forge.match.GameLobby;
import forge.net.game.server.RemoteClient;

public interface ILobby {
    GameLobby getState();
    int login(RemoteClient client);
    void logout(RemoteClient client);
}
