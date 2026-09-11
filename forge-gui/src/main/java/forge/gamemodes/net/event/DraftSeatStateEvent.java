package forge.gamemodes.net.event;

import forge.gamemodes.limited.DraftAction;
import forge.item.PaperCard;

import java.util.List;

/**
 * Server -> one seat: changes to that seat's pool and its current offers. A full
 * event carries the whole pool in {@code poolAdded} and replaces the client's pool.
 */
public final class DraftSeatStateEvent implements NetEvent {
    private static final long serialVersionUID = 1L;
    private final int seatIndex;
    private final boolean full;
    private final List<PaperCard> poolAdded;
    private final List<PaperCard> poolRemoved;
    private final List<DraftAction> actions;

    public DraftSeatStateEvent(int seatIndex, boolean full, List<PaperCard> poolAdded,
            List<PaperCard> poolRemoved, List<DraftAction> actions) {
        this.seatIndex = seatIndex;
        this.full = full;
        this.poolAdded = List.copyOf(poolAdded);
        this.poolRemoved = List.copyOf(poolRemoved);
        this.actions = List.copyOf(actions);
    }

    public int getSeatIndex() { return seatIndex; }
    public boolean isFull() { return full; }
    public List<PaperCard> getPoolAdded() { return poolAdded; }
    public List<PaperCard> getPoolRemoved() { return poolRemoved; }
    public List<DraftAction> getActions() { return actions; }
}
