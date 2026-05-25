package forge.gui.interfaces;

import forge.deck.Deck;
import forge.interfaces.IPlayerChangeListener;
import forge.interfaces.IUpdateable;
import forge.item.PaperCard;

import java.util.List;

public interface ILobbyView extends IUpdateable {
    void setPlayerChangeListener(IPlayerChangeListener iPlayerChangeListener);

    default void onDraftPackArrived(int seatIndex, List<PaperCard> pack,
            int packNumber, int pickNumber, int timerDurationSeconds) { }
    /** Fires for every pod seat (including the viewing player) so views can refresh queue depths uniformly. */
    default void onDraftSeatPicked(int seatIndex, int[] seatQueueDepths) { }
    /** Fires when the pick timer expires and the server auto-selects a card. */
    default void onDraftAutoPicked(int seatIndex, PaperCard card, int packNumber, int pickInPack) { }
    /** Fires once at the end of the draft with the player's pool. */
    default void onReceiveEventPool(String eventId, Deck pool) { }
}
