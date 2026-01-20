package forge.gamemodes.net;

import forge.game.GameView;
import forge.gamemodes.net.server.RemoteClient;
import forge.gamemodes.net.event.NetEvent;

/**
 * Packet containing the full game state.
 * Used for initial synchronization when a client connects and for reconnection.
 */
public final class FullStatePacket implements NetEvent {
    private static final long serialVersionUID = 1L;

    private final long sequenceNumber;
    private final long timestamp;
    private final GameView gameView;
    private final int stateChecksum;
    private final boolean isReconnect;
    private final String sessionId;
    private final String sessionToken;

    /**
     * Create a full state packet for initial connection.
     * @param sequenceNumber the current sequence number
     * @param gameView the complete game view
     */
    public FullStatePacket(long sequenceNumber, GameView gameView) {
        this(sequenceNumber, gameView, false, null, null);
    }

    /**
     * Create a full state packet for reconnection.
     * @param sequenceNumber the current sequence number
     * @param gameView the complete game view
     * @param sessionId session identifier for reconnection
     * @param sessionToken security token for reconnection
     */
    public FullStatePacket(long sequenceNumber, GameView gameView, String sessionId, String sessionToken) {
        this(sequenceNumber, gameView, true, sessionId, sessionToken);
    }

    private FullStatePacket(long sequenceNumber, GameView gameView, boolean isReconnect,
                            String sessionId, String sessionToken) {
        this.sequenceNumber = sequenceNumber;
        this.timestamp = System.currentTimeMillis();
        this.gameView = gameView;
        this.stateChecksum = computeChecksum(gameView);
        this.isReconnect = isReconnect;
        this.sessionId = sessionId;
        this.sessionToken = sessionToken;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public GameView getGameView() {
        return gameView;
    }

    public int getStateChecksum() {
        return stateChecksum;
    }

    public boolean isReconnect() {
        return isReconnect;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    /**
     * Compute a checksum for the game state.
     * Used for validation after delta application.
     * @param gameView the game view to checksum
     * @return checksum value
     */
    private static int computeChecksum(GameView gameView) {
        if (gameView == null) {
            return 0;
        }
        // Simple checksum based on key game state
        int hash = 17;
        hash = 31 * hash + gameView.getId();
        hash = 31 * hash + (gameView.getTurn());
        if (gameView.getPhase() != null) {
            hash = 31 * hash + gameView.getPhase().hashCode();
        }
        if (gameView.getPlayers() != null) {
            hash = 31 * hash + gameView.getPlayers().size();
        }
        return hash;
    }

    /**
     * Verify that a local game state matches this packet's checksum.
     * @param localGameView the local game view to verify
     * @return true if checksums match
     */
    public boolean verifyChecksum(GameView localGameView) {
        return computeChecksum(localGameView) == stateChecksum;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
        // No client-specific updates needed
    }

    @Override
    public String toString() {
        return String.format("FullStatePacket[seq=%d, reconnect=%b, checksum=%d]",
                sequenceNumber, isReconnect, stateChecksum);
    }
}
