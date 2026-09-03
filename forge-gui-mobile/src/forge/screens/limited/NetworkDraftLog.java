package forge.screens.limited;

import forge.deck.FDeckEditor.FDraftLog;
import forge.gamemodes.net.EventParticipant;
import forge.item.PaperCard;
import forge.util.Localizer;

import java.util.List;

/**
 * Pushes network draft log entries into the mobile {@link FDraftLog} panel,
 * caching each pending self-pick until the server echoes its queue depth.
 */
public final class NetworkDraftLog {
    private static final Localizer localizer = Localizer.getInstance();

    private final int ownSeat;
    private FDraftLog sink;

    private record PendingSelfPick(String cardName, int packNumber, int pickInPack, boolean auto) { }
    private PendingSelfPick pending;

    public NetworkDraftLog(int ownSeat) {
        this.ownSeat = ownSeat;
    }

    public void setSink(FDraftLog sink) {
        this.sink = sink;
    }

    /**
     * Called when any seat confirms a pick (server's DraftSeatPickedEvent).
     * Flushes the pending self-pick entry for our own seat with server-authoritative
     * queue depth; logs an "other picked" entry for all other seats.
     */
    public void recordSeatPicked(int seat, int[] seatQueueDepths,
            List<EventParticipant> participants) {
        int depth = (seat >= 0 && seat < seatQueueDepths.length) ? seatQueueDepths[seat] : 0;
        if (seat == ownSeat) {
            flushPending(depth);
        } else {
            String name = EventParticipant.resolveName(seat, participants, null);
            log(localizer.getMessage("lblDraftLogOtherPick", name) + waitingSuffix(depth));
        }
    }

    /**
     * Called when the server auto-picks a card for our seat (DraftAutoPickedEvent).
     * Caches a pending entry; flushed with authoritative queue depth on the next
     * recordSeatPicked for our seat.
     */
    public void recordAutoPicked(int seat, PaperCard card, int packNumber, int pickInPack,
            List<EventParticipant> participants) {
        if (seat != ownSeat) return;
        pending = new PendingSelfPick(card.getName(), packNumber, pickInPack, true);
    }

    /**
     * Caches a pending entry for a manual pick by the local player. Flushed when
     * the server's DraftSeatPickedEvent echoes our seat back with queue-depth data.
     */
    public void recordPendingSelfPick(PaperCard card, int packNumber, int pickInPack) {
        pending = new PendingSelfPick(card.getName(), packNumber, pickInPack, false);
    }

    private void flushPending(int queueDepth) {
        PendingSelfPick p = pending;
        if (p == null) return;
        pending = null;
        String displayName = p.auto ? p.cardName + " (auto)" : p.cardName;
        String msg = localizer.getMessage("lblDraftLogMyPick", displayName,
                String.valueOf(p.packNumber), String.valueOf(p.pickInPack));
        log(msg + waitingSuffix(queueDepth));
    }

    private static String waitingSuffix(int depth) {
        if (depth <= 0) return "";
        return " " + localizer.getMessage("lblDraftLogWaiting", String.valueOf(depth));
    }

    private void log(String message) {
        if (sink != null) {
            sink.addLogEntry(message);
        }
    }
}
