package forge.gui.interfaces;

import java.util.List;

import forge.deck.Deck;
import forge.gamemodes.limited.DraftPrompt;
import forge.gamemodes.net.event.DraftAutoPickedEvent;
import forge.gamemodes.net.event.DraftLogEvent;
import forge.gamemodes.net.event.DraftPackArrivedEvent;
import forge.gamemodes.net.event.DraftPromptEvent;
import forge.gamemodes.net.event.DraftSeatPickedEvent;
import forge.gamemodes.net.event.DraftSeatStateEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gamemodes.net.event.ReceiveEventPoolEvent;
import forge.item.PaperCard;

public interface IDraftEventHandler {
    void draftPackArrived(int seatIndex, List<PaperCard> pack,
            int packNumber, int pickNumber, int timerDurationSeconds, int seq, int hiddenCount);
    void draftSeatPicked(int seatIndex, int[] seatQueueDepths, List<List<PaperCard>> faceUpBySeat);
    void draftAutoPicked(int seatIndex, PaperCard card, int packNumber, int pickInPack);
    void receiveEventPool(String eventId, Deck pool);
    void draftSeatState(DraftSeatStateEvent event);
    void draftLog(DraftLogEvent event);
    void draftPrompt(DraftPrompt prompt);

    /** Returns true if {@code event} was a recognized draft/event-pool event and was dispatched. */
    default boolean dispatch(NetEvent event) {
        if (event instanceof DraftPackArrivedEvent e) {
            draftPackArrived(e.getSeatIndex(), e.getPack(),
                    e.getPackNumber(), e.getPickNumber(), e.getTimerDurationSeconds(),
                    e.getSeq(), e.getHiddenCount());
            return true;
        } else if (event instanceof DraftSeatPickedEvent e) {
            draftSeatPicked(e.getSeatIndex(), e.getSeatQueueDepths(), e.getFaceUpBySeat());
            return true;
        } else if (event instanceof DraftAutoPickedEvent e) {
            draftAutoPicked(e.getSeatIndex(), e.getCard(),
                    e.getPackNumber(), e.getPickInPack());
            return true;
        } else if (event instanceof ReceiveEventPoolEvent e) {
            receiveEventPool(e.getEventId(), e.getPool());
            return true;
        } else if (event instanceof DraftSeatStateEvent e) {
            draftSeatState(e);
            return true;
        } else if (event instanceof DraftLogEvent e) {
            draftLog(e);
            return true;
        } else if (event instanceof DraftPromptEvent e) {
            draftPrompt(e.getPrompt());
            return true;
        }
        return false;
    }
}
