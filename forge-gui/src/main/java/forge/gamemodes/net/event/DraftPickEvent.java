package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;
import forge.item.PaperCard;

public final class DraftPickEvent implements NetEvent {
    private static final long serialVersionUID = 1L;
    private final int seatIndex;
    private final PaperCard card;

    public DraftPickEvent(int seatIndex, PaperCard card) {
        this.seatIndex = seatIndex;
        this.card = card;
    }

    public int getSeatIndex() { return seatIndex; }
    public PaperCard getCard() { return card; }

    @Override
    public void updateForClient(RemoteClient client) { }
}
