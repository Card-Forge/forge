package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;

public final class HeartbeatEvent implements NetEvent {
    private static final long serialVersionUID = 1L;

    @Override
    public void updateForClient(final RemoteClient client) {
    }

    @Override
    public String toString() {
        return "Heartbeat";
    }
}
