package forge.net.event;

import forge.match.GameLobby.GameLobbyData;
import forge.net.server.RemoteClient;

public class LobbyUpdateEvent implements NetEvent {
    private static final long serialVersionUID = 7114918637727047985L;

    private final GameLobbyData state;
    private int slot;
    public LobbyUpdateEvent(final GameLobbyData state) {
        this.state = state;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
        this.slot = client.getIndex();
    }

    public GameLobbyData getState() {
        return state;
    }

    public int getSlot() {
        return slot;
    }
}
