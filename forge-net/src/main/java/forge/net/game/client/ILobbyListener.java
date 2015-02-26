package forge.net.game.client;

import forge.net.game.server.RemoteClient;

public interface ILobbyListener {
    void login(RemoteClient client);
    void logout(RemoteClient client);
    void message(String source, String message);
}
