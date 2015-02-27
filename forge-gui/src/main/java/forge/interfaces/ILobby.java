package forge.interfaces;

import forge.net.game.LobbyState;
import forge.net.game.server.RemoteClient;

public interface ILobby {
    LobbyState getState();
    int login(RemoteClient client);
    void logout(RemoteClient client);
}
