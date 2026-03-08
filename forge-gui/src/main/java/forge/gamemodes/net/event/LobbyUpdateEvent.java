package forge.gamemodes.net.event;

import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.net.server.RemoteClient;

public class LobbyUpdateEvent implements NetEvent {
    private static final long serialVersionUID = 7114918637727047985L;

    private final GameLobbyData state;
    private int slot = RemoteClient.UNASSIGNED_SLOT;  // Default to unassigned
    public LobbyUpdateEvent(final GameLobbyData state) {
        this.state = state;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
        this.slot = client.hasValidSlot() ? client.getIndex() : RemoteClient.UNASSIGNED_SLOT;
    }

    public GameLobbyData getState() {
        return state;
    }

    public int getSlot() {
        return slot;
    }
}
