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
        // Only set the slot if the client has been assigned a valid slot.
        // This prevents sending slot=0 (which is the host slot) to newly connected
        // clients before they've been assigned their actual slot.
        if (client.hasValidSlot()) {
            this.slot = client.getIndex();
        }
        // Otherwise, slot remains UNASSIGNED_SLOT (-1) to indicate pending assignment
    }

    public GameLobbyData getState() {
        return state;
    }

    public int getSlot() {
        return slot;
    }
}
