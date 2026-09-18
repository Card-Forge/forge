package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;

/**
 * Server tells a reconnecting client that its seat is gone — either the 300s
 * disconnect window expired, or the match was never one this user was in.
 * Replaces the prior "infer seat-lost from any LobbyUpdateEvent during
 * RECONNECTING" heuristic, which fired spuriously on the lobby update that
 * normally accompanies a successful resume.
 */
public final class SeatLostEvent implements NetEvent {
    private static final long serialVersionUID = 1L;

    @Override
    public void updateForClient(final RemoteClient client) {
    }

    @Override
    public String toString() {
        return "SeatLost";
    }
}
