package forge.interfaces;

import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.client.ClientGameLobby;

public interface ILobbyListener {
    void message(String source, String message, ChatMessage.MessageType type);
    void update(GameLobbyData state, int slot);
    void close();
    ClientGameLobby getLobby();
}
