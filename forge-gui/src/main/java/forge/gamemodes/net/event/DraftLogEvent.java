package forge.gamemodes.net.event;

import forge.item.PaperCard;

/** Server -> every seat when {@code seatIndex} is -1, otherwise to that seat only: one draft log line. */
public final class DraftLogEvent implements NetEvent {
    private static final long serialVersionUID = 1L;
    private final int seatIndex;
    private final String message;
    /** The card the line names, or null. */
    private final PaperCard card;

    public DraftLogEvent(int seatIndex, String message, PaperCard card) {
        this.seatIndex = seatIndex;
        this.message = message;
        this.card = card;
    }

    public int getSeatIndex() { return seatIndex; }
    public String getMessage() { return message; }
    public PaperCard getCard() { return card; }
}
