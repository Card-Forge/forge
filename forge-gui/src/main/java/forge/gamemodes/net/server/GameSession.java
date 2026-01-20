package forge.gamemodes.net.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a network game session that can survive temporary disconnections.
 * Manages session state and player connections for reconnection support.
 */
public class GameSession {
    /** Default timeout for reconnection attempts (5 minutes) */
    public static final long DEFAULT_DISCONNECT_TIMEOUT_MS = 5 * 60 * 1000;

    private final String sessionId;
    private final long creationTime;
    private final Map<Integer, PlayerSession> playerSessions;
    private long disconnectTimeoutMs;
    private boolean gameInProgress;
    private boolean paused;
    private String pauseMessage;

    /**
     * Create a new game session.
     */
    public GameSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.creationTime = System.currentTimeMillis();
        this.playerSessions = new ConcurrentHashMap<>();
        this.disconnectTimeoutMs = DEFAULT_DISCONNECT_TIMEOUT_MS;
        this.gameInProgress = false;
        this.paused = false;
        this.pauseMessage = null;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getDisconnectTimeoutMs() {
        return disconnectTimeoutMs;
    }

    public void setDisconnectTimeoutMs(long timeoutMs) {
        this.disconnectTimeoutMs = timeoutMs;
    }

    public boolean isGameInProgress() {
        return gameInProgress;
    }

    public void setGameInProgress(boolean gameInProgress) {
        this.gameInProgress = gameInProgress;
    }

    public boolean isPaused() {
        return paused;
    }

    public String getPauseMessage() {
        return pauseMessage;
    }

    /**
     * Register a player for this session.
     * @param playerIndex the player's index
     * @return the player session
     */
    public PlayerSession registerPlayer(int playerIndex) {
        PlayerSession session = new PlayerSession(playerIndex, this);
        playerSessions.put(playerIndex, session);
        return session;
    }

    /**
     * Get a player session by index.
     * @param playerIndex the player's index
     * @return the player session, or null if not found
     */
    public PlayerSession getPlayerSession(int playerIndex) {
        return playerSessions.get(playerIndex);
    }

    /**
     * Remove a player session.
     * @param playerIndex the player's index
     */
    public void removePlayerSession(int playerIndex) {
        playerSessions.remove(playerIndex);
    }

    /**
     * Mark a player as disconnected.
     * @param playerIndex the player's index
     */
    public void markPlayerDisconnected(int playerIndex) {
        PlayerSession session = playerSessions.get(playerIndex);
        if (session != null) {
            session.setConnectionState(PlayerSession.ConnectionState.DISCONNECTED);
        }
    }

    /**
     * Mark a player as reconnecting.
     * @param playerIndex the player's index
     */
    public void markPlayerReconnecting(int playerIndex) {
        PlayerSession session = playerSessions.get(playerIndex);
        if (session != null) {
            session.setConnectionState(PlayerSession.ConnectionState.RECONNECTING);
        }
    }

    /**
     * Mark a player as connected.
     * @param playerIndex the player's index
     */
    public void markPlayerConnected(int playerIndex) {
        PlayerSession session = playerSessions.get(playerIndex);
        if (session != null) {
            session.setConnectionState(PlayerSession.ConnectionState.CONNECTED);
        }
    }

    /**
     * Pause the game due to a player disconnect.
     * @param message the pause message to display
     */
    public void pauseGame(String message) {
        this.paused = true;
        this.pauseMessage = message;
    }

    /**
     * Resume the game after reconnection.
     */
    public void resumeGame() {
        this.paused = false;
        this.pauseMessage = null;
    }

    /**
     * Check if any player is disconnected.
     * @return true if any player is disconnected
     */
    public boolean hasDisconnectedPlayers() {
        return playerSessions.values().stream()
                .anyMatch(ps -> ps.getConnectionState() == PlayerSession.ConnectionState.DISCONNECTED);
    }

    /**
     * Check if all players are connected.
     * @return true if all players are connected
     */
    public boolean allPlayersConnected() {
        return playerSessions.values().stream()
                .allMatch(ps -> ps.getConnectionState() == PlayerSession.ConnectionState.CONNECTED);
    }

    /**
     * Validate a reconnection attempt.
     * @param playerIndex the player index
     * @param token the session token
     * @return true if the reconnection is valid
     */
    public boolean validateReconnection(int playerIndex, String token) {
        PlayerSession session = playerSessions.get(playerIndex);
        if (session == null) {
            return false;
        }
        return session.validateToken(token);
    }

    /**
     * Check if a player's reconnection timeout has expired.
     * @param playerIndex the player's index
     * @return true if the timeout has expired
     */
    public boolean isReconnectionTimeoutExpired(int playerIndex) {
        PlayerSession session = playerSessions.get(playerIndex);
        if (session == null) {
            return true;
        }
        if (session.getConnectionState() != PlayerSession.ConnectionState.DISCONNECTED) {
            return false;
        }
        long disconnectTime = session.getDisconnectTime();
        return System.currentTimeMillis() - disconnectTime > disconnectTimeoutMs;
    }

    /**
     * Get the remaining time for reconnection.
     * @param playerIndex the player's index
     * @return remaining time in milliseconds, or 0 if timeout expired
     */
    public long getRemainingReconnectionTime(int playerIndex) {
        PlayerSession session = playerSessions.get(playerIndex);
        if (session == null || session.getConnectionState() != PlayerSession.ConnectionState.DISCONNECTED) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - session.getDisconnectTime();
        return Math.max(0, disconnectTimeoutMs - elapsed);
    }

    @Override
    public String toString() {
        return String.format("GameSession[id=%s, players=%d, inProgress=%b, paused=%b]",
                sessionId, playerSessions.size(), gameInProgress, paused);
    }
}
