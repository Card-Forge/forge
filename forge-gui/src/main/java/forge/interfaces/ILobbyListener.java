package forge.interfaces;

import forge.match.GameLobby.GameLobbyData;
import forge.net.client.ClientGameLobby;

public interface ILobbyListener {
    void message(String source, String message);
    void update(GameLobbyData state, int slot);
    void close();
    ClientGameLobby getLobby();
}
