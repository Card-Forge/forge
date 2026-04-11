package forge.interfaces;

import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.client.ClientGameLobby;

public interface ILobbyListener {
    // type may be null from older peers — implementations should fall back to source-based inference.
    void message(String source, String message, ChatMessage.MessageType type);

    default void message(String source, String message) {
        message(source, message, null);
    }

    void update(GameLobbyData state, int slot);
    void close();
    ClientGameLobby getLobby();
}
