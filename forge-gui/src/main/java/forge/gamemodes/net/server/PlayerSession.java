package forge.gamemodes.net.server;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Represents a player's session within a game session.
 * Tracks connection state and provides tokens for reconnection authentication.
 */
public class PlayerSession {

    /**
     * Connection states for a player.
     */
    public enum ConnectionState {
        /** Player is connected and active */
        CONNECTED,
        /** Player has disconnected */
        DISCONNECTED,
        /** Player is in the process of reconnecting */
        RECONNECTING
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;

    private final int playerIndex;
    private final GameSession gameSession;
    private final String sessionToken;
    private ConnectionState connectionState;
    private long disconnectTime;
    private String playerName;

    /**
     * Create a new player session.
     * @param playerIndex the player's index in the game
     * @param gameSession the parent game session
     */
    public PlayerSession(int playerIndex, GameSession gameSession) {
        this.playerIndex = playerIndex;
        this.gameSession = gameSession;
        this.sessionToken = generateToken();
        this.connectionState = ConnectionState.CONNECTED;
        this.disconnectTime = 0;
    }

    /**
     * Generate a secure random token for session authentication.
     * @return a Base64-encoded token string
     */
    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public int getPlayerIndex() {
        return playerIndex;
    }

    public GameSession getGameSession() {
        return gameSession;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    /**
     * Set the connection state.
     * Automatically updates disconnect time when transitioning to DISCONNECTED.
     * @param state the new connection state
     */
    public void setConnectionState(ConnectionState state) {
        ConnectionState oldState = this.connectionState;
        this.connectionState = state;

        if (state == ConnectionState.DISCONNECTED && oldState != ConnectionState.DISCONNECTED) {
            this.disconnectTime = System.currentTimeMillis();
        } else if (state == ConnectionState.CONNECTED) {
            this.disconnectTime = 0;
        }
    }

    public long getDisconnectTime() {
        return disconnectTime;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    /**
     * Validate a token for reconnection.
     * @param token the token to validate
     * @return true if the token matches
     */
    public boolean validateToken(String token) {
        if (token == null || sessionToken == null) {
            return false;
        }
        // Use constant-time comparison to prevent timing attacks
        return constantTimeEquals(sessionToken, token);
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Check if this player is currently disconnected.
     * @return true if disconnected
     */
    public boolean isDisconnected() {
        return connectionState == ConnectionState.DISCONNECTED;
    }

    /**
     * Check if this player is currently connected.
     * @return true if connected
     */
    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED;
    }

    /**
     * Get the time elapsed since disconnect in milliseconds.
     * @return elapsed time, or 0 if not disconnected
     */
    public long getTimeSinceDisconnect() {
        if (connectionState != ConnectionState.DISCONNECTED || disconnectTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - disconnectTime;
    }

    @Override
    public String toString() {
        return String.format("PlayerSession[index=%d, name=%s, state=%s]",
                playerIndex, playerName, connectionState);
    }
}
