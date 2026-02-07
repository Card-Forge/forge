package forge.gamemodes.net;

import forge.game.GameView;
import forge.gamemodes.net.server.RemoteClient;
import forge.gamemodes.net.event.NetEvent;

/**
 * Packet containing the full game state.
 * Used for initial synchronization when a client connects.
 */
public final class FullStatePacket implements NetEvent {
    private static final long serialVersionUID = 1L;

    private final long sequenceNumber;
    private final GameView gameView;
    private final int stateChecksum;

    /**
     * Create a full state packet.
     * @param sequenceNumber the current sequence number
     * @param gameView the complete game view
     */
    public FullStatePacket(long sequenceNumber, GameView gameView) {
        this.sequenceNumber = sequenceNumber;
        this.gameView = gameView;
        this.stateChecksum = computeChecksum(gameView);
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public GameView getGameView() {
        return gameView;
    }

    public int getStateChecksum() {
        return stateChecksum;
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

    @Override
    public void updateForClient(final RemoteClient client) {
        // No client-specific updates needed
    }

    @Override
    public String toString() {
        return String.format("FullStatePacket[seq=%d, checksum=%d]",
                sequenceNumber, stateChecksum);
    }
}
