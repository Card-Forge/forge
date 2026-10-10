package forge.gamemodes.net.event;

import forge.gamemodes.limited.DraftAction;

/** Client -> server: use the draft ability of a face-up card in the seat's pool. */
public final class DraftActivateEvent implements NetEvent {
    private static final long serialVersionUID = 1L;
    private final int seatIndex;
    private final DraftAction action;

    public DraftActivateEvent(int seatIndex, DraftAction action) {
        this.seatIndex = seatIndex;
        this.action = action;
    }

    public int getSeatIndex() { return seatIndex; }
    public DraftAction getAction() { return action; }
}
