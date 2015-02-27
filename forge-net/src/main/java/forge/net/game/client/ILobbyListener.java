package forge.net.game.client;

import forge.net.game.LobbyState;

public interface ILobbyListener {
    void message(String source, String message);
    void update(LobbyState state);
}
