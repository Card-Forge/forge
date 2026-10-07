package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;
import forge.item.PaperCard;

public final class DraftAutoPickedEvent implements NetEvent {
    private static final long serialVersionUID = 1L;
    private final int seatIndex;
    private final PaperCard card;
    private final int packNumber;
    private final int pickInPack;

    public DraftAutoPickedEvent(int seatIndex, PaperCard card, int packNumber, int pickInPack) {
        this.seatIndex = seatIndex;
        this.card = card;
        this.packNumber = packNumber;
        this.pickInPack = pickInPack;
    }

    public int getSeatIndex() { return seatIndex; }
    public PaperCard getCard() { return card; }
    public int getPackNumber() { return packNumber; }
    public int getPickInPack() { return pickInPack; }

    @Override
    public void updateForClient(RemoteClient client) { }
}
