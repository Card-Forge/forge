package forge.gamemodes.net;

import forge.gamemodes.match.GameLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gui.interfaces.ILobbyView;

public interface IOnlineLobby {
    ILobbyView setLobby(GameLobby lobby);
    void setClient(FGameClient client);
    void closeConn(String msg);
}
