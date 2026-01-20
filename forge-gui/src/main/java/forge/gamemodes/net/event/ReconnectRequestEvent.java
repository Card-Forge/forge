package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;

/**
 * Event sent by client to request reconnection to an existing game session.
 * Contains the session ID and authentication token received during initial connection.
 */
public class ReconnectRequestEvent implements NetEvent {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final String token;

    /**
     * Create a reconnection request event.
     * @param sessionId the game session identifier
     * @param token the authentication token for this player
     */
    public ReconnectRequestEvent(final String sessionId, final String token) {
        this.sessionId = sessionId;
        this.token = token;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
        // No client-specific updates needed
    }

    /**
     * Get the session ID.
     * @return the session identifier
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Get the authentication token.
     * @return the session token
     */
    public String getToken() {
        return token;
    }
}
