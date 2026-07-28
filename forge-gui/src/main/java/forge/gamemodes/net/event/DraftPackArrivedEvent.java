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

    public DraftPackArrivedEvent(int seatIndex, List<PaperCard> pack,
            int packNumber, int pickNumber, int timerDurationSeconds) {
        this.seatIndex = seatIndex;
        this.pack = List.copyOf(pack);
        this.packNumber = packNumber;
        this.pickNumber = pickNumber;
        this.timerDurationSeconds = timerDurationSeconds;
    }

    public int getSeatIndex() { return seatIndex; }
    public List<PaperCard> getPack() { return pack; }
    public int getPackNumber() { return packNumber; }
    public int getPickNumber() { return pickNumber; }
    public int getTimerDurationSeconds() { return timerDurationSeconds; }

    @Override
    public void updateForClient(RemoteClient client) { }
}
