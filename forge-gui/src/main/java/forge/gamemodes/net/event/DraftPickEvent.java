package forge.gamemodes.net.event;

import forge.gamemodes.limited.DraftAction;
import forge.gamemodes.net.server.RemoteClient;
import forge.item.PaperCard;

public final class DraftPickEvent implements NetEvent {
    private static final long serialVersionUID = 1L;
    private final int seatIndex;
    private final int seq;
    /** Ignored for a hidden pack, where the host draws the card itself. */
    private final PaperCard card;
    private final DraftAction variant;

    public DraftPickEvent(int seatIndex, int seq, PaperCard card, DraftAction variant) {
        this.seatIndex = seatIndex;
        this.seq = seq;
        this.card = card;
        this.variant = variant;
    }

    public int getSeatIndex() { return seatIndex; }
    public int getSeq() { return seq; }
    public PaperCard getCard() { return card; }
    public DraftAction getVariant() { return variant; }

    @Override
    public void updateForClient(RemoteClient client) { }
}
