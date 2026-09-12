package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;

/**
 * Server -> all clients: a seat has made their pick.
 * No card name revealed. Includes per-seat queue depths for the picker window.
 */
public final class DraftSeatPickedEvent implements NetEvent {
    private static final long serialVersionUID = 1L;
    private final int seatIndex;
    private final int[] seatQueueDepths;

    public DraftSeatPickedEvent(int seatIndex, int[] seatQueueDepths) {
        this.seatIndex = seatIndex;
        this.seatQueueDepths = seatQueueDepths.clone();
    }

    public int getSeatIndex() { return seatIndex; }
    public int[] getSeatQueueDepths() { return seatQueueDepths; }

    @Override
    public void updateForClient(RemoteClient client) { }
}
