package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;

import java.io.Serializable;

public interface NetEvent extends Serializable {
    /**
     * Called by the server's broadcast path once per recipient, just
     * before the event is encoded and sent to that client. Override to
     * embed per-recipient data into the event (e.g. {@link LobbyUpdateEvent}
     * stores the recipient's slot index this way).
     *
     * <p>The event is mutated in place and reused across recipients —
     * encoding completes synchronously between iterations, so the prior
     * recipient's value is on the wire before the next iteration
     * overwrites it. Don't override unless your event has per-recipient
     * state; default is no-op.
     */
    default void updateForClient(RemoteClient client) {}
}
