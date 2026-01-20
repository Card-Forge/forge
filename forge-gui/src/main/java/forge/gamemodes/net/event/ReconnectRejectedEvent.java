package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;

/**
 * Event sent by server to reject a reconnection request.
 * Contains the reason for rejection.
 */
public class ReconnectRejectedEvent implements NetEvent {
    private static final long serialVersionUID = 1L;

    private final String reason;

    /**
     * Create a reconnection rejected event.
     * @param reason the reason the reconnection was rejected
     */
    public ReconnectRejectedEvent(final String reason) {
        this.reason = reason;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
        // No client-specific updates needed
    }

    /**
     * Get the reason for rejection.
     * @return the rejection reason
     */
    public String getReason() {
        return reason;
    }
}
