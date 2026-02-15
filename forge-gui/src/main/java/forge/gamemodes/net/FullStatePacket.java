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

    private static int computeChecksum(GameView gameView) {
        if (gameView == null) {
            return 0;
        }
        int phaseOrdinal = gameView.getPhase() != null ? gameView.getPhase().ordinal() : -1;
        return NetworkChecksumUtil.computeStateChecksum(gameView.getTurn(), phaseOrdinal, gameView.getPlayers());
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
