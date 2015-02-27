package forge.net.game;

import forge.net.game.server.RemoteClient;

public class LobbyUpdateEvent implements NetEvent {
    private static final long serialVersionUID = -3176971304173703949L;

    private final LobbyState state;
    public LobbyUpdateEvent(final LobbyState state) {
        this.state = state;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
        state.setLocalPlayer(client.getIndex());
    }

    public LobbyState getState() {
        return state;
    }
}
