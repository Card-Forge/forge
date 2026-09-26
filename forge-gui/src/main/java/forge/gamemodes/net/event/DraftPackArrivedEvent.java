package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;
import forge.item.PaperCard;
import java.util.List;

/**
 * Server -> specific client: a draft pack has arrived for picking.
 * Includes the timer duration (fire-and-forget, client runs local countdown).
 */
public final class DraftPackArrivedEvent implements NetEvent {
    private static final long serialVersionUID = 1L;
    private final int seatIndex;
    private final List<PaperCard> pack;
    private final int packNumber;
    private final int pickNumber;
    private final int timerDurationSeconds;
    private final int seq;
    /** When above zero, {@code pack} is empty and the client shows that many face-down placeholders. */
    private final int hiddenCount;

    public DraftPackArrivedEvent(int seatIndex, List<PaperCard> pack,
            int packNumber, int pickNumber, int timerDurationSeconds, int seq, int hiddenCount) {
        this.seatIndex = seatIndex;
        this.pack = List.copyOf(pack);
        this.packNumber = packNumber;
        this.pickNumber = pickNumber;
        this.timerDurationSeconds = timerDurationSeconds;
        this.seq = seq;
        this.hiddenCount = hiddenCount;
    }

    public int getSeatIndex() { return seatIndex; }
    public List<PaperCard> getPack() { return pack; }
    public int getPackNumber() { return packNumber; }
    public int getPickNumber() { return pickNumber; }
    public int getTimerDurationSeconds() { return timerDurationSeconds; }
    public int getSeq() { return seq; }
    public int getHiddenCount() { return hiddenCount; }

    @Override
    public void updateForClient(RemoteClient client) { }
}
