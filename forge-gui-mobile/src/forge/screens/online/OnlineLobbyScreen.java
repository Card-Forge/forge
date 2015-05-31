package forge.screens.online;

import forge.interfaces.ILobbyView;
import forge.match.GameLobby;
import forge.net.IOnlineLobby;
import forge.net.client.FGameClient;
import forge.screens.constructed.LobbyScreen;

public class OnlineLobbyScreen extends LobbyScreen implements IOnlineLobby {
    public OnlineLobbyScreen() {
        super(null, OnlineMenu.getMenu(), null); //must wait for lobby to be set
    }

    @Override
    public ILobbyView setLobby(GameLobby lobby0) {
        initLobby(lobby0);
        return this;
    }

    @Override
    public void setClient(FGameClient client) {
        // TODO Auto-generated method stub
        
    }
}
