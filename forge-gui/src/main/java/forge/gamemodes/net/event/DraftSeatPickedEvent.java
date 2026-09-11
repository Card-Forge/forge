package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;
import forge.item.PaperCard;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Server -> all clients: a seat has made their pick.
 * No card name revealed. Includes per-seat queue depths for the picker window,
 * and every seat's face-up cards, which are public.
 */
public final class DraftSeatPickedEvent implements NetEvent {
    private static final long serialVersionUID = 1L;
    private final int seatIndex;
    private final int[] seatQueueDepths;
    private final List<List<PaperCard>> faceUpBySeat;

    public DraftSeatPickedEvent(int seatIndex, int[] seatQueueDepths, List<List<PaperCard>> faceUpBySeat) {
        this.seatIndex = seatIndex;
        this.seatQueueDepths = seatQueueDepths.clone();
        this.faceUpBySeat = faceUpBySeat.stream().map(List::copyOf).collect(Collectors.toList());
    }

    public int getSeatIndex() { return seatIndex; }
    public int[] getSeatQueueDepths() { return seatQueueDepths; }
    public List<List<PaperCard>> getFaceUpBySeat() { return faceUpBySeat; }

    @Override
    public void updateForClient(RemoteClient client) { }
}
