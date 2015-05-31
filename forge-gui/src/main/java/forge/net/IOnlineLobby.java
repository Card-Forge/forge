package forge.net;

import forge.interfaces.ILobbyView;
import forge.match.GameLobby;
import forge.net.client.FGameClient;

public interface IOnlineLobby {
    ILobbyView setLobby(GameLobby lobby);
    void setClient(FGameClient client);
}
